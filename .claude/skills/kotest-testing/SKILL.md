---
name: kotest-testing
description: Guides unit and integration testing with Kotest for Kotlin/Android. Use when writing tests for Kotlin code, testing ViewModels, coroutines, or Compose UI. Covers test styles, assertions, property testing, and Android-specific testing.
tags: ["kotlin", "kotest", "testing", "unit-test", "android", "coroutines", "compose"]
difficulty: intermediate
category: testing
version: "1.0.0"
last_updated: "2025-01-29"
---

# Kotest Testing

## Quick Start

Add dependencies to `build.gradle`:

```kotlin
dependencies {
    // Core
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    
    // Optional modules
    testImplementation("io.kotest:kotest-property:5.8.0")  // Property testing
    testImplementation("io.kotest:kotest-extensions-robolectric:1.2.1")  // Android
}
```

Enable JUnit 5:

```kotlin
tasks.test {
    useJUnitPlatform()
}
```

Test styles:

| Style | Structure | Best For |
|-------|-----------|----------|
| `FunSpec` | `test("name") { }` | Simple unit tests |
| `StringSpec` | `"name" { }` | Minimal syntax |
| `ShouldSpec` | `should("name") { }` | BDD-style |
| `DescribeSpec` | `describe/it` | Nested contexts |
| `BehaviorSpec` | `given/when/then` | BDD/Gherkin |
| `FreeSpec` | `- "name" { }` | Flexible nesting |

## Core Patterns

### Basic Test Styles

**FunSpec (most common):**
```kotlin
class CalculatorTest : FunSpec({
    
    test("addition should return correct sum") {
        val calc = Calculator()
        calc.add(2, 3) shouldBe 5
    }
    
    test("division by zero should throw exception") {
        val calc = Calculator()
        shouldThrow<ArithmeticException> {
            calc.divide(10, 0)
        }
    }
})
```

**BehaviorSpec (BDD style):**
```kotlin
class LoginBehaviorTest : BehaviorSpec({
    
    given("a user repository") {
        val repo = mockk<UserRepository>()
        
        and("valid credentials") {
            val email = "user@test.com"
            val password = "password123"
            
            `when`("login is called") {
                coEvery { repo.login(email, password) } returns User(email)
                val result = repo.login(email, password)
                
                then("user should be returned") {
                    result.email shouldBe email
                }
            }
        }
        
        and("invalid credentials") {
            `when`("login is called") {
                coEvery { repo.login(any(), any()) } throws InvalidCredentialsException()
                
                then("exception should be thrown") {
                    shouldThrow<InvalidCredentialsException> {
                        repo.login("bad", "credentials")
                    }
                }
            }
        }
    }
})
```

**DescribeSpec (grouped tests):**
```kotlin
class UserServiceTest : DescribeSpec({
    
    describe("getUser") {
        val service = UserService(mockk())
        
        it("returns user when found") {
            // Test
        }
        
        it("returns null when not found") {
            // Test
        }
    }
    
    describe("saveUser") {
        it("saves user successfully") {
            // Test
        }
        
        it("throws when user is invalid") {
            // Test
        }
    }
})
```

### Assertions

**Basic assertions:**
```kotlin
test("assertion examples") {
    // Equality
    result shouldBe expected
    result shouldNotBe otherValue
    
    // Nullability
    value shouldBe null
    value shouldNotBe null
    
    // Booleans
    condition shouldBe true
    condition.shouldBeTrue()
    list.shouldBeEmpty()
    list.shouldNotBeEmpty()
    
    // Types
    obj.shouldBeTypeOf<String>()
    obj.shouldBeInstanceOf<Number>()
    
    // Collections
    list shouldHaveSize 3
    list shouldContain "item"
    list shouldContainAll listOf("a", "b")
    list.shouldBeSorted()
    
    // Strings
    text shouldContain "substring"
    text shouldStartWith "prefix"
    text shouldEndWith "suffix"
    text.shouldMatch(Regex("[a-z]+"))
    
    // Exceptions
    shouldThrow<IllegalArgumentException> {
        validateInput(-1)
    }.message shouldContain "must be positive"
}
```

**Collection assertions:**
```kotlin
test("collection matchers") {
    val list = listOf(1, 2, 3, 4, 5)
    
    list.shouldContainInOrder(2, 3, 4)
    list.shouldBeUnique()
    list.shouldHaveAtLeastSize(3)
    list.shouldHaveAtMostSize(10)
    list.shouldContainOnlyOddDigits()
    
    // Deep equality
    list.shouldContainExactly(1, 2, 3, 4, 5)
    list.shouldContainExactlyInAnyOrder(5, 4, 3, 2, 1)
}
```

**Soft assertions (all assertions run):**
```kotlin
test("soft assertions") {
    val user = fetchUser()
    
    assertSoftly(user) {
        id shouldBe 1
        name shouldBe "John"
        email shouldContain "@"
        age shouldBeGreaterThan 0
    }
}
```

### Mocking

**MockK:**
```kotlin
test("mocking with MockK") {
    // Create mock
    val repo = mockk<UserRepository>()
    
    // Stub
    every { repo.getUser(1) } returns User(1, "John")
    every { repo.getUser(any()) } returns User(0, "Unknown")
    
    // Stub with argument matching
    every { repo.saveUser(match { it.name.isNotEmpty() }) } returns true
    
    // Verify
    verify { repo.getUser(1) }
    verify(exactly = 1) { repo.getUser(any()) }
    verify(atLeast = 1) { repo.saveUser(any()) }
    verify(atMost = 2) { repo.getUser(any()) }
}
```

**CoMockK (coroutines):**
```kotlin
test("mocking suspend functions") {
    val api = mockk<ApiService>()
    
    // Stub suspend function
    coEvery { api.fetchData() } returns listOf("data")
    coEvery { api.postData(any()) } throws NetworkException()
    
    // Verify suspend calls
    coVerify { api.fetchData() }
    coVerify(timeout = 1000) { api.postData(any()) }
}
```

### Coroutine Testing

**TestDispatcher:**
```kotlin
class CoroutineTest : FunSpec({
    
    // Inject TestDispatcher
    coroutineTestScope = true
    
    test("test coroutines") {
        val viewModel = MyViewModel()
        
        // Trigger coroutine
        viewModel.loadData()
        
        // Advance time
        advanceTimeBy(1000)
        
        // Or run until idle
        advanceUntilIdle()
        
        // Assert
        viewModel.data.value shouldNotBe null
    }
})
```

**runTest:**
```kotlin
class ViewModelTest : FunSpec({
    
    test("viewModel loads data") {
        runTest {
            val viewModel = UserViewModel(fakeRepo)
            
            viewModel.loadUser("1")
            
            // Skip delay
            advanceUntilIdle()
            
            viewModel.uiState.value.user?.name shouldBe "John"
        }
    }
})
```

### Property Testing

**Basic properties:**
```kotlin
class PropertyTest : StringSpec({
    
    "reversing a string twice returns original" {
        checkAll<String> { str ->
            str.reversed().reversed() shouldBe str
        }
    }
    
    "addition is commutative" {
        checkAll<Int, Int> { a, b ->
            a + b shouldBe b + a
        }
    }
    
    "list size after adding element increases by 1" {
        checkAll(Arb.list(Arb.int()), Arb.int()) { list, element ->
            (list + element).size shouldBe list.size + 1
        }
    }
})
```

**Custom generators:**
```kotlin
val emailArb = arbitrary { rs ->
    val name = rs.random.nextInt(1000).toString()
    val domain = listOf("gmail.com", "test.com", "example.org").random(rs.random)
    "$name@$domain"
}

test("email validation") {
    checkAll(emailArb) { email ->
        isValidEmail(email) shouldBe true
    }
}
```

**Shrinking:**
```kotlin
test("finds minimal failing case") {
    checkAll<Int> { i ->
        // Kotest automatically shrinks to find smallest failing input
        i shouldBeLessThan 10000
    }
}
```

### Android Testing

**ViewModel testing:**
```kotlin
class UserViewModelTest : FunSpec({
    
    coroutineTestScope = true
    
    test("load user updates state") {
        val repo = mockk<UserRepository>()
        coEvery { repo.getUser("1") } returns User("1", "John")
        
        val viewModel = UserViewModel(repo)
        
        viewModel.loadUser("1")
        advanceUntilIdle()
        
        viewModel.uiState.value shouldBe UserUiState(
            user = User("1", "John"),
            isLoading = false
        )
    }
    
    test("error state on failure") {
        val repo = mockk<UserRepository>()
        coEvery { repo.getUser(any()) } throws IOException("Network error")
        
        val viewModel = UserViewModel(repo)
        viewModel.loadUser("1")
        advanceUntilIdle()
        
        viewModel.uiState.value.error shouldBe "Network error"
    }
})
```

**Compose testing:**
```kotlin
class ComposeTest : FunSpec({
    
    test("button click updates count") {
        composeTestRule.setContent {
            MyAppTheme {
                Counter()
            }
        }
        
        // Find and interact
        composeTestRule.onNodeWithText("Count: 0").assertExists()
        composeTestRule.onNodeWithContentDescription("Increment").performClick()
        composeTestRule.onNodeWithText("Count: 1").assertExists()
    }
    
    test("lazy list scroll") {
        composeTestRule.setContent {
            ProductList(products = sampleProducts)
        }
        
        composeTestRule.onNodeWithTag("product_list")
            .performScrollToNode(hasText("Product 10"))
    }
})
```

## Common Patterns

### Test Lifecycle

```kotlin
class LifecycleTest : FunSpec({
    
    // Before each test
    beforeEach {
        // Setup
    }
    
    // After each test
    afterEach {
        // Cleanup
    }
    
    // Before all tests in this class
    beforeSpec {
        // One-time setup
    }
    
    // After all tests
    afterSpec {
        // One-time cleanup
    }
    
    // Per-test isolation
    isolationMode = IsolationMode.InstancePerTest
})
```

### Data Driven Tests

```kotlin
class ParameterizedTest : FunSpec({
    
    context("addition") {
        withData(
            Pair(1, 1) to 2,
            Pair(2, 3) to 5,
            Pair(0, 0) to 0,
            Pair(-1, 1) to 0
        ) { (input, expected) ->
            val (a, b) = input
            a + b shouldBe expected
        }
    }
    
    context("division") {
        withData(
            nameFn = { "${it.first} / ${it.second} = ${it.third}" },
            Triple(10, 2, 5),
            Triple(9, 3, 3),
            Triple(100, 10, 10)
        ) { (a, b, expected) ->
            a / b shouldBe expected
        }
    }
})
```

### Test Configuration

```kotlin
class ConfiguredTest : FunSpec({
    
    // Retry failed tests
    retry(3)
    
    // Timeout
    timeout = 5000.milliseconds
    
    // Tags for selective running
    tags(Tag.Slow, Tag.Integration)
    
    // Configure listeners
    listener(object : TestListener {
        override suspend fun beforeTest(testCase: TestCase) {
            println("Starting ${testCase.name}")
        }
    })
    
    test("slow test") {
        // Will retry 3 times on failure, timeout after 5s
    }
})
```

### Result Validation

```kotlin
test("result assertions") {
    val result = fetchData()
    
    // ShouldBe with custom matcher
    result.shouldBeSuccess {
        it.data shouldHaveSize 10
    }
    
    // ShouldNotBeFailure
    result.shouldNotBeFailureOfType<NetworkException>()
}
```

## Examples (Input → Output)

### Complete ViewModel Test

**Input request:** "Create comprehensive tests for a user ViewModel"

**Output:**
```kotlin
class UserViewModelTest : FunSpec({
    
    coroutineTestScope = true
    
    lateinit var repository: UserRepository
    lateinit var viewModel: UserViewModel
    
    beforeEach {
        repository = mockk()
        viewModel = UserViewModel(repository)
    }
    
    context("loadUser") {
        
        test("emits loading then success state") {
            val user = User("1", "John", "john@test.com")
            coEvery { repository.getUser("1") } coAnswers {
                delay(100)
                user
            }
            
            viewModel.loadUser("1")
            
            // Initial loading state
            viewModel.uiState.value.isLoading shouldBe true
            
            // After delay
            advanceTimeBy(100)
            
            // Success state
            assertSoftly(viewModel.uiState.value) {
                isLoading shouldBe false
                user?.name shouldBe "John"
                error shouldBe null
            }
        }
        
        test("emits error state on failure") {
            coEvery { repository.getUser(any()) } throws 
                IOException("Network error")
            
            viewModel.loadUser("1")
            advanceUntilIdle()
            
            assertSoftly(viewModel.uiState.value) {
                isLoading shouldBe false
                user shouldBe null
                error shouldBe "Network error"
            }
        }
        
        test("cancels previous load on new request") {
            coEvery { repository.getUser("1") } coAnswers {
                delay(1000)
                User("1", "Old")
            }
            coEvery { repository.getUser("2") } returns 
                User("2", "New")
            
            viewModel.loadUser("1")
            viewModel.loadUser("2")
            advanceUntilIdle()
            
            // Should show result from second call
            viewModel.uiState.value.user?.name shouldBe "New"
        }
    }
    
    context("search") {
        
        test("debounces search query") {
            val users = listOf(User("1", "John"))
            coEvery { repository.searchUsers(any()) } returns users
            
            viewModel.setQuery("j")
            viewModel.setQuery("jo")
            viewModel.setQuery("john")
            
            // No calls yet (debounced)
            coVerify(exactly = 0) { repository.searchUsers(any()) }
            
            // Advance past debounce (300ms)
            advanceTimeBy(300)
            
            // Only one call with final query
            coVerify(exactly = 1) { repository.searchUsers("john") }
        }
    }
})
```

### Repository Test with MockK

**Input request:** "Write tests for a repository that fetches from network and cache"

**Output:**
```kotlin
class UserRepositoryTest : BehaviorSpec({
    
    val api = mockk<UserApi>()
    val dao = mockk<UserDao>()
    val repository = UserRepository(api, dao)
    
    given("getUser") {
        val userId = "123"
        val cachedUser = User(userId, "Cached User")
        val networkUser = User(userId, "Network User")
        
        `when`("user exists in cache") {
            coEvery { dao.getUser(userId) } returns cachedUser
            
            then("returns cached user without network call") {
                val result = repository.getUser(userId)
                
                result shouldBe cachedUser
                coVerify(exactly = 0) { api.fetchUser(any()) }
            }
        }
        
        `when`("user not in cache") {
            coEvery { dao.getUser(userId) } returns null
            coEvery { api.fetchUser(userId) } returns networkUser
            coEvery { dao.insert(networkUser) } just Runs
            
            then("fetches from network and caches") {
                val result = repository.getUser(userId)
                
                result shouldBe networkUser
                coVerify { api.fetchUser(userId) }
                coVerify { dao.insert(networkUser) }
            }
        }
        
        `when`("network fails") {
            coEvery { dao.getUser(userId) } returns null
            coEvery { api.fetchUser(any()) } throws IOException()
            
            then("throws exception") {
                shouldThrow<IOException> {
                    repository.getUser(userId)
                }
            }
        }
    }
})
```

## Best Practices

1. **Use appropriate test style**: FunSpec for unit tests, BehaviorSpec for BDD
2. **Group related tests**: Use `context` or `describe` for organization
3. **Use assertSoftly**: Run all assertions to see all failures
4. **Mock at boundaries**: Mock repositories, not data classes
5. **Test behavior, not implementation**: Assert on outcomes, not method calls
6. **Use property testing**: Test invariants that should always hold
7. **Isolate tests**: Use `isolationMode` to prevent test interference
8. **Coroutines**: Always use `runTest` and `advanceUntilIdle`
9. **Meaningful names**: Test names should describe behavior
10. **Test edge cases**: Empty collections, nulls, errors, boundaries

## Resources

- [Kotest documentation](https://kotest.io/)
- [Assertion docs](https://kotest.io/docs/assertions/assertions.html)
- [Property testing](https://kotest.io/docs/proptest/property-based-testing.html)
- [Android testing](https://kotest.io/docs/framework/android.html)
