# Test Naming & Organization

## Current convention

Tests in this repository name methods with Given / When / Then:

```java
@Test
@DisplayName("Given a basket with items, when checking out, then the cart is emptied")
void givenBasketWithItems_whenCheckingOut_thenCartIsEmptied() { }
```

The three beats are in the **name** so a single test reads as a scenario.
`@DisplayName` repeats that sentence in reports. This is a Java naming
template, not Cucumber — unit tests are not `.feature` files.

`@Nested` is unused for this today. Shared setup, when it exists, lives in
the test class rather than a Given/When type hierarchy.

## Trade-off and intended evolution

GWT is a useful way to **think** about a test (fixture → action → outcome).
Putting all three into every method name has a cost: names get long, the
outcome is easy to miss, and `@DisplayName` Gherkin drifts from the code.

A later pass may keep GWT as structure instead:

| Beat  | Where it would live                            |
|-------|------------------------------------------------|
| Given | `@Nested` class, only when a fixture is shared |
| When  | Test body                                      |
| Then  | Method name (`rejectsLogin`)                   |

Nesting would stay optional. A single test would not be wrapped in Given +
When types just to complete the grammar. Cucumber would still be reserved
for acceptance tests, if those appear.

Until that pass, the suite stays on the current name template so naming
stays consistent.
