# Kotest Testing - Agent Guide

## Quick Start

This skill helps you write comprehensive tests for Kotlin code using the Kotest framework.

**When to use this skill:**
- Writing unit tests for Kotlin classes
- Testing ViewModels with coroutines
- Testing Compose UI components
- Setting up property-based testing
- Converting from JUnit to Kotest

## Common Tasks

### 1. Create a Basic Test Class

**Input:** "Create tests for a UserRepository class"

**Action:** Use the `FunSpec` style for straightforward unit tests:

```kotlin
class UserRepositoryTest : FunSpec({
    // Test setup
    beforeEach {
        // Initialize mocks and test data
    }
    
    // Individual tests
    test("should return user when found in cache") {
        // Arrange
        val userId = "123"
        val expectedUser = User(userId, "John")
        
        // Mock setup
        coEvery { cache.get(userId) } returns expectedUser
        
        // Act
        val result = repository.getUser(userId)
        
        // Assert
        result shouldBe expectedUser
    }
    
    test("should fetch from API when not in cache") {
        // Test implementation
    }
})
```

### 2. Test Coroutines

**Input:** "Test a suspend function that fetches data"

**Action:** Use `coroutineTestScope = true` and `runTest`:

```kotlin
class CoroutineTest : FunSpec({
    coroutineTestScope = true
    
    test("should fetch data successfully") {
        runTest {
            val viewModel = MyViewModel()
            
            viewModel.loadData()
            advanceUntilIdle()  // Wait for coroutines
            
            viewModel.data.value shouldNotBe null
        }
    }
})
```

### 3. Mock with MockK

**Input:** "Mock a repository dependency"

**Action:** Use MockK for Kotlin-friendly mocking:

```kotlin
// Create mock
val repository = mockk<UserRepository>()

// Stub suspend function
coEvery { repository.fetchUser(any()) } returns User("1", "John")
coEvery { repository.saveUser(any()) } throws DatabaseException()

// Verify calls
coVerify { repository.fetchUser("123") }
coVerify(exactly = 1) { repository.saveUser(any()) }
```

### 4. Test Compose UI

**Input:** "Test a Compose component"

**Action:** Use Compose test rules:

```kotlin
class ComposeTest : FunSpec({
    test("button click increments counter") {
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
})
```

## Key Patterns

### Test Styles

| Style | Use Case | Syntax |
|-------|----------|--------|
| `FunSpec` | Simple unit tests | `test("name") { }` |
| `BehaviorSpec` | BDD style | `given/when/then` |
| `DescribeSpec` | Grouped tests | `describe/it` |
| `StringSpec` | Minimal | `"name" { }` |

### Common Assertions

```kotlin
// Equality
result shouldBe expected
result shouldNotBe other

// Nullability
value shouldBe null
value shouldNotBe null

// Types
obj.shouldBeTypeOf<String>()

// Collections
list shouldHaveSize 3
list shouldContain "item"
list.shouldBeSorted()

// Exceptions
shouldThrow<IllegalArgumentException> {
    invalidOperation()
}
```

### Soft Assertions

Use `assertSoftly` to run all assertions even if some fail:

```kotlin
assertSoftly(user) {
    id shouldBe 1
    name shouldBe "John"
    email shouldContain "@"
}
```

## Running Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "UserRepositoryTest"

# Run with Kotest plugin
./gradlew kotest
```

## Project Structure

```
src/
├── test/
│   └── kotlin/
│       └── com/example/
│           ├── UserRepositoryTest.kt
│           ├── UserViewModelTest.kt
│           └── ComposeTest.kt
└── androidTest/
    └── kotlin/
        └── com/example/
            └── UIInstrumentationTest.kt
```

## Dependencies

```kotlin
dependencies {
    testImplementation("io.kotest:kotest-runner-junit5:5.8.0")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation("io.kotest:kotest-property:5.8.0")
    testImplementation("io.mockk:mockk:1.13.8")
}
```

## Tips

1. **Use `shouldBe` for equality**, not `==`
2. **Mock suspend functions** with `coEvery` and `coVerify`
3. **Use `runTest`** for coroutine tests, not `runBlocking`
4. **Group related tests** with `context` or `describe`
5. **Use `assertSoftly`** for multiple assertions on same object
6. **Enable `coroutineTestScope`** for automatic test dispatcher injection

## Resources

- [Kotest Documentation](https://kotest.io/)
- [MockK Documentation](https://mockk.io/)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
