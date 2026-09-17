# Testing Conventions

Two separate questions: **what to test and where** (below), and **what to call it**
([Naming & organization](#naming--organization)).

How coverage is measured across modules is plumbing, not convention, and lives in
[COVERAGE.md](COVERAGE.md).

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
| `Order.placeFromCheckout` guards | `CreateOrderHandler` | Invariants (billing address, shipping address, payment, items, checkout id) that must hold however the handler is reached. The checkout listener is its only caller, but enumerating every rejection there would cost a Spring context per case; the handler proves them against a fake repository. |
| One order per checkout | `MongoOrderRepositoryTest` for enforcement, `BasketCheckoutIntegrationEventListenerTest` for the skip | The guarantee is a unique index on `checkoutId`, which only real Mongo can prove — the fake merely mirrors it, so enforcement gets its own adapter test. The listener tests prove what a redelivery must look like to the outbox: no exception escapes, one order exists, and distinct checkouts from the same user still produce distinct orders. |
| Order visibility (each customer sees and deletes only their own orders) | `MongoOrderQueryRepositoryTest` for both reads, the handler tests for each endpoint | Both reads filter inside the Mongo query — the list has to, or paging and `totalItems` would count other customers' orders — so only real Mongo proves enforcement. Each adapter test carries a positive control (the owner still sees the order), so an always-empty result cannot pass. The handler tests prove the same rule against `FakeOrderQueryRepository`. Delete checks ownership in `DeleteOrderHandler` against the write port, so its handler test is the seam, and asserts the other customer's order still exists afterwards. |
| Domain event routing (`ProductCreated`, `BasketCheckedOut` reach their listeners) | The outbox assertions in `ApplicationWiringTest` (product creation, basket checkout) | Routing is a property of the assembled Spring context, and the only listener with an observable outcome is `OutboxEventSubscriber`: an outbox row exists only if the event was delivered inside the committing transaction. `ProductCreatedHandler` and `BasketCheckedOutHandler` are demo listeners that only log, kept to show where a module would react to its own events; they stay untested by design, since the only thing to assert would be a log line or a call. |
| `DBMapper` / `DTOMapper` | The adapter tests (`MongoOrderRepositoryTest`) | Exercised through the real adapter, since a fake repository never touches them. Test data must include the shapes production actually produces, not only fully populated ones. |

## Naming & organization

### Class names Surefire will actually run

A test **class** name must end in `Test`, `Tests` or `TestCase`. Those are the
three patterns `maven-surefire-plugin` is configured with in the root pom, and a
class outside them is compiled, reported as uncovered, and **silently never
executed** — the build stays green and the test count simply comes out lower.
Every class in the suite today ends in `Test`; the other two patterns are
configured so that the obvious alternative spellings do not vanish.

Two things deliberately left out of that list:

- **`Test*` as a prefix.** It is one of Surefire's own defaults, but it would
  also match helpers like `security`'s `TestUsersUtils`, which are not tests.
- **`*IT`.** By convention an `*IT` class belongs to `maven-failsafe-plugin` at
  the `integration-test` phase, not to Surefire at `test`. This project has no
  `*IT` class and no Failsafe, and every Testcontainers test runs under Surefire
  as an ordinary `*Test`. **If you introduce the first `*IT`, settle the Failsafe
  question before naming it that** — otherwise it will not run, and nothing will
  say so.

Helper types — fakes, object mothers, test configuration — must **not** end in
any of the three, which is why the suite uses `FakeOrderRepository`,
`OrderMother` and `SecurityTestConfig`.

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

Cucumber and `.feature` files are reserved for acceptance tests, if this
project ever grows that far. Nothing in the current suite uses them.

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
