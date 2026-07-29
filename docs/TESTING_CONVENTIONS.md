# Test Naming & Organization Convention

## Structure
Use JUnit 5 `@Nested` classes to group tests by shared context ("given"). Use short
method names for the outcome ("then"). Do not encode given/when/then into a single
flat method name.

```java
class AccountServiceTest {

    @Nested
    class WhenAccountIsSuspended {

        @BeforeEach
        void setUp() {
            // shared "given" setup for this context
        }

        @Test
        void rejectsLogin() { }

        @Test
        void sendsSuspensionNotice() { }
    }

    @Nested
    class GivenEmptyCart {

        @Test
        void throwsValidationException() { }
    }

    // No meaningful shared context? Don't force nesting.
    @Test
    void returnsAccountById() { }
}
```

## Rules

1. **Nested class name = the context/condition**, in plain descriptive language.
    - Prefer `WhenX`, `GivenX`, or just a descriptive condition name — pick whichever
      reads most naturally. Do not force a rigid grammar template.
    - Example: `WhenPaymentFails`, `GivenEmptyCart`, `AccountAlreadyExists`.

2. **Method names describe only the outcome**, not the setup or condition.
    - Good: `rejectsLogin`, `throwsValidationException`, `returnsConflict`.
    - Bad: `shouldRejectLoginWhenAccountIsSuspended` (context belongs in the nested
      class, not repeated per method).
    - Dropping a leading `should`/`returns` verb is fine as long as the name reads
      as a clear statement of behavior.

3. **Don't force every test into a nested class.** If a test has no meaningful
   shared context, leave it as a top-level method with a short `shouldX`-style
   name. Consistency of shape is not the goal — clarity is.

4. **Shared setup for a context goes in that nested class's `@BeforeEach`**,
   not repeated across sibling test methods.

5. **If a test name is still hard to keep short, treat that as a signal** the
   test may be covering more than one behavior — split it rather than
   compressing the name further.

6. **`@DisplayName` is optional**, only add it when the class/method name alone
   isn't readable enough in test reports. Don't add it as a blanket rule on
   every class/method — it's not refactor-safe, so keep it minimal.

## Do not use
- Gherkin/Cucumber for unit tests. Reserve BDD-style scenario tests for
  integration/acceptance tests where non-technical readability is an actual
  requirement.