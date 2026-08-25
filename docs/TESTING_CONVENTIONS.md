# Testing Conventions

Two separate questions: **what to test and where** (below), and **what to call it**
([Naming & organization](#naming--organization)).

## What to test, and where

### Test at the behavior seam

For each change, identify the narrowest entry point that is:

- **Stable** — its shape follows a requirement, not an implementation choice, so
  refactoring behind it does not move it.
- **Complete** — exercising it produces the observable outcome, with no need to
  assert on intermediate steps.

Test there. The unit under test is a **behavior**, not a class. Which layer the seam
sits in is a per-change question, not a standing rule — a plan should state where the
seam is for the change at hand and why, so the claim can be argued with.

Prefer the outermost seam that stays fast and deterministic. Reach for a Spring
context or HTTP only when the wiring itself is the thing being proven.

### Fake only at ports

Substitute at the architecture's real boundaries — the `domain/port` interfaces — and
use real collaborators inside the seam. See `FakeOrderRepository`,
`FakeBasketRepository`, `FakeProductQueryService`.

Assert the outcome a behavior produces, never that a particular collaborator was
called. `verify(x).method(arg)` couples a test to structure, and structural coupling
is what makes a single change ripple through many test suites. A hand-written fake
implements the port faithfully *and* can expose its state for assertions — that is its
advantage over a mock, and it is why fakes may carry inspection or seeding methods the
production interface does not (`FakeBasketRepository.domainEvents()`,
`FakeOrderQueryRepository.addOrder(...)`).

### Narrow the seam when its cases outgrow it

A seam test tends to cover one happy path per behavior, so combinatorial logic behind
it goes unenumerated by default and the edge cases silently never get written.

Add a test at a smaller seam when the logic behind the current one has more
interesting cases than you would enumerate at its setup cost — typically:

- a **calculation** (totals, discounts, tax, proration)
- an **invariant** that must hold regardless of caller — especially where no DTO
  boundary can enforce it, e.g. an aggregate reached from an event listener rather
  than a controller
- a **state machine** with transitions worth enumerating
- **merge / dedup / ordering** logic over a collection

Do not narrow for constructors, getters, or code that only shuffles arguments and
defaults. Apply the test from
[rules-of-engagement.md](../ai-coding-tools/processes/rules-of-engagement.md): if
watching it fail before the code exists would not change what you write, it is
ceremony, not rigor.

### Choose test data that can fail

A test proves nothing about a mapping or a calculation if its values cannot
distinguish a correct implementation from a wrong one. Quantity `1` and price `1`
cannot detect a swapped field; quantity `2` and price `12.50` can. One item cannot
detect a total that ignores quantity. Pick values that differ, and cover more than the
single-element case wherever a collection is involved.

Compare `BigDecimal` with `isEqualByComparingTo` rather than `isEqualTo` — `12.50` and
`12.5` are unequal but comparable, and the difference surfaces after a database
round-trip.

### Worked examples

| Code | Seam | Reasoning |
|------|------|-----------|
| `ShoppingCart.checkout` | Domain (`ShoppingCartTest`) | Calculation over a collection — quantity, missing prices, duplicate products. More cases than a handler test would ever enumerate. |
| `Order`, `Product`, `Address`, `Payment` | None of their own | Constructors, getters, and factories that shuffle arguments. Covered incidentally by the seams above them. |
| Basket checkout → order creation | `BasketCheckoutIntegrationEventListener` | No dedicated handler exists for this path; the listener is the module's stable entry point for the integration contract. A Spring context is justified because `@EventListener` wiring is part of what is being proven. |
| `CreateOrderHandler` | The handler | Stable entry point for the manual `POST /order` path; the persisted order is the complete outcome. |
| `DBMapper` / `DTOMapper` | The adapter tests (`MongoOrderRepositoryTest`) | Exercised through the real adapter, since a fake repository never touches them. Test data must include the shapes production actually produces, not only fully populated ones. |

## Naming & organization

### Current convention

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

### Trade-off and intended evolution

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
