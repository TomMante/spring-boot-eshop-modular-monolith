# Backlog

Work this project has decided to do later, recorded with the reasoning that put it here — so a future session can pick something up without re-deriving why it was left.

**What belongs here vs. [KNOWN_ISSUES.md](KNOWN_ISSUES.md).** Ask one question: *is the code wrong or unverified right now?*

| The code is… | Goes in |
| --- | --- |
| broken, or behaving correctly with nothing proving it | [KNOWN_ISSUES.md](KNOWN_ISSUES.md) |
| fine as far as it goes, but a capability is missing or a decision is unmade | here |

Route by **nature, not by origin**. Something surfaced while deferring a plan step can still be a defect — most of this project's deferrals have been. "A plan deferred it" is not a category.

**Entry bar:** work actually decided on and deliberately postponed. Not ideas, not "would be nice". A backlog that accepts those stops being read, which is the only way a backlog fails.

**Lifecycle:** an entry leaves when it is done, when it is promoted into a plan under `ai-coding-tools/plans/`, or when it is consciously dropped. Entries here never duplicate a fix already written as a Known Issue's *Action Required* — two copies of one decision drift apart.

Entries are self-contained on purpose. Plan documents are frozen history and may not be reachable; nothing here should require reading one.

## 🎯 Ready to pick up

### 1. Pricing Basis and `Order.totalPrice`
- **Decision needed first:** the two price sources in the system disagree by design, and storing both without choosing persists a contradiction — `Σ(item.price × quantity)` will not equal `totalPrice` whenever a catalog price moves between add and checkout, and nothing marks which is authoritative.

  | Value | Source | Meaning |
  | --- | --- | --- |
  | ordering `Item.price` | basket's `priceWhenAdded` | price when the item was added to the cart |
  | event `totalPrice` | `ShoppingCart.checkout(currentPrices)`, refetched from the catalog | price at checkout |

  - *Checkout-time pricing* — conventional for e-commerce, price confirmed at purchase. Checkout overwrites item prices with current ones; total is `Σ(current price × quantity)`. Gives the basket's refetch an actual purpose.
  - *Add-time pricing* — the customer keeps the price they saw. Basket stops refetching; total is `Σ(priceWhenAdded × quantity)`, derivable from items, so storing it is an audit snapshot rather than new information.
- **Then, in one piece of work:** fix the `ShoppingCart.checkout` defects (Known Bug 1), and add `totalPrice` to `Order`, `OrderDB`, `OrderDTO`, both mappers, and `CreateOrderCommand`/`Request`/`Controller`.
- **Closes:** Known Bug 1 and Testing Gap 1 — the total-price calculation is the one piece of domain logic here that earns an isolated `ShoppingCartTest`.
- **Why deferred:** the previous increment would have had to persist a number already known to be wrong. Not persisting it yet was the better of the two.

### 2. Nested Validation on Address and Payment Contents
- Neither `CreateOrderRequest` nor `CheckoutBasketRequest` validates the fields *inside* an address or payment — only that the object itself is present. An address of all-blank strings is accepted by both entry points.
- **Why deferred:** kept at parity between the two requests deliberately, rather than making one stricter than the other in an increment that was about something else.

### 3. `MongoAuditMetadata`
- Ordering has no counterpart to `shared.infrastructure.JpaAuditMetadata`, so Mongo-backed documents carry no created/modified audit fields.
- **Blocked on:** adding `@Version` to the documents. Mongo auditing decides created-vs-modified purely from `isNew`, which is id-based, and every document in this codebase assigns its id up front — so without a version field auditing cannot distinguish the two cases at all.
- **Note:** whatever shape this takes should not repeat the defect recorded in Testing Gap 5 — a `save` that blind-overwrites a whole row from a projection that does not carry every column.

### 7. Extend the Wiring Test to the Basket Write Path
- [assembled-context-wiring-test](../ai-coding-tools/plans/assembled-context-wiring-test/assembled-context-wiring-test.md) proves a transactional write resolves its manager for catalog (`CreateProductHandler` through `JpaProductRepository`) but not for basket. `CheckoutBasketHandler` is `@Transactional("basketTransactionManager")` against Mongo, and Mongo transactions require `MongoDBContainer.withReplicaSet()`.
- **Why deferred:** that increment deliberately paid for one container shape (plain `MongoDBContainer`) to get the catalog path green first, rather than the replica-set startup cost before anything worked.
- **Action:** add a second write-and-read-back test in `ApplicationWiringTest`, switching `mongoDb` to `.withReplicaSet()`, exercising `CheckoutBasketHandler`.

## ⏳ Gated on a trigger

Nothing to do until the stated condition holds. Deliberate, not neglected.

### 4. Distinguishing an Incomplete Order
- Nothing in the read model marks an order as pending or incomplete.
- **Trigger:** a "complete your order" step, or any flow that can produce a partially specified order. Checkout currently produces complete orders and `Order.create` rejects missing address or payment, so there is no incomplete state to represent.

### 5. `OrderCreated` Domain Event and Outbox Wiring
- `Order` never calls `addDomainEvent`, and `OrderingModuleConfig` has no outbox wiring — unlike basket and catalog.
- **Trigger:** another module actually needing to react to order creation. Deliberate YAGNI; a speculative event plus outbox is real infrastructure to maintain in exchange for nothing.

### 6. Test Naming Pass — GWT as Structure
- Move the suite from `givenX_whenY_thenZ` method names to GWT-as-structure (`@Nested` for a shared given, method name = outcome only). Full reasoning and steps in [gwt-test-naming-later-refactor](../ai-coding-tools/plans/gwt-test-naming-later-refactor/gwt-test-naming-later-refactor.md), and the target style in [TESTING_CONVENTIONS.md](TESTING_CONVENTIONS.md).
- **Trigger:** the owner declaring the blueprint finished. Not during feature work — mixing styles mid-project is worse than a consistent noisy one.
