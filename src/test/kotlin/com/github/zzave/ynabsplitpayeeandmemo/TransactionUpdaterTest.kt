package com.github.zzave.ynabsplitpayeeandmemo

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class TransactionUpdaterTest :
    FunSpec({

        fun transaction(
            payeeName: String? = null,
            importPayeeName: String? = null,
            memo: String? = null,
        ) = Transaction(
            id = "test-id",
            date = "2026-01-01",
            amount = -10000,
            cleared = "cleared",
            approved = true,
            accountId = "account-1",
            payeeName = payeeName,
            importPayeeName = importPayeeName,
            memo = memo,
        )

        context("payee and memo splitting") {

            test("splits payee on dash and puts remainder in memo when memo is null") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "John Doe - Maintenance spend at DIY store (50/50)",
                            importPayeeName = "John Doe - Maintenance spend at DIY store (50/50)",
                            memo = null,
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 1
                result[0].payeeName shouldBe "John Doe"
                result[0].memo shouldBe "Maintenance spend at DIY store (50/50)"
            }

            test("splits payee on dash when there's no memo in the import payee") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "John Doe -",
                            importPayeeName = "John Doe -",
                            memo = null,
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 1
                result[0].payeeName shouldBe "John Doe"
                result[0].memo shouldBe null
            }

            test("should not duplicate memo again") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "John Doe - Maintenance spend at DIY store (50/50)",
                            importPayeeName = "John Doe - Maintenance spend at DIY store (50/50)",
                            memo = "Maintenance spend at DIY store (50/50)",
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 1
                result[0].payeeName shouldBe "John Doe"
                result[0].memo shouldBe "Maintenance spend at DIY store (50/50)"
            }

            test("should not duplicate memo again 2") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "John Doe - Maintenance spend at DIY store (50/50)",
                            importPayeeName = "John Doe - Maintenance spend at DIY store (50/50)",
                            memo = "Maintenance spend at DIY store (50/50) - Maintenance spend at DIY store (50/50)",
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 1
                result[0].payeeName shouldBe "John Doe"
                result[0].memo shouldBe "Maintenance spend at DIY store (50/50)"
            }

            test("should not duplicate memo again 3") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "John Doe - Maintenance spend at DIY store (50/50)",
                            importPayeeName = "John Doe - Maintenance spend at DIY store (50/50)",
                            memo = "just - testing - my - testing - my - testing",
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 1
                result[0].payeeName shouldBe "John Doe"
                result[0].memo shouldBe "just - testing - my - testing"
            }

            test("splits payee and appends to existing memo") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "John Doe - Funding diy budget",
                            importPayeeName = "John Doe - Funding diy budget",
                            memo = "monthly transfer",
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 1
                result[0].payeeName shouldBe "John Doe"
                result[0].memo shouldBe "monthly transfer - Funding diy budget"
            }

            test("preserves multiple dashes after first split in memo") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "John Doe - Maintenance spend (paint, furniture, etc.) - IBAN: NL00BANK0123456789",
                            importPayeeName = "John Doe - Maintenance spend (paint, furniture, etc.) - IBAN: NL00BANK0123456789",
                            memo = null,
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 1
                result[0].payeeName shouldBe "John Doe"
                result[0].memo shouldBe "Maintenance spend (paint, furniture, etc.) - IBAN: NL00BANK0123456789"
            }

            test("full payee with description and IBAN extracts memo when original memo is null") {
                val payee = "John Doe - Maintenance spend (paint, furniture, etc.) (50/50 as agreed) - IBAN: NL00BANK0123456789"
                val transactions =
                    listOf(
                        transaction(
                            payeeName = payee,
                            importPayeeName = payee,
                            memo = null,
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 1
                result[0].payeeName shouldBe "John Doe"
                result[0].memo shouldBe "Maintenance spend (paint, furniture, etc.) (50/50 as agreed) - IBAN: NL00BANK0123456789"
            }
        }

        context("skipped transactions") {

            test("no dash in payee keeps original memo") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "Hardware Store",
                            importPayeeName = "Hardware Store",
                            memo = "some memo",
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result.shouldBeEmpty()
            }

            test("skips transaction when payee was already changed by user") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "Renamed Payee",
                            importPayeeName = "John Doe - Funding diy budget",
                            memo = null,
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result.shouldBeEmpty()
            }

            test("skips transaction when payeeName or importPayeeName is blank") {
                val transactions =
                    listOf(
                        transaction(payeeName = null, importPayeeName = "John Doe - some memo"),
                        transaction(payeeName = "John Doe - some memo", importPayeeName = null),
                    )

                val result = transactions.findTransactionsToUpdate()

                result.shouldBeEmpty()
            }

            test("skips transaction when payee contains - without surrounding whitespace") {
                val transactions =
                    listOf(
                        transaction(
                            payeeName = "Payee-with-dash",
                            importPayeeName = "Payee-with-dash",
                            memo = null,
                        ),
                    )

                val result = transactions.findTransactionsToUpdate()

                result shouldHaveSize 0
            }
        }
    })
