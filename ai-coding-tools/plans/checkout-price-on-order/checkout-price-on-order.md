# Checkout Price on Order

Approved by the user on 2026-10-19.

## Status
- [ ] Step 1 — Checked-out lines carry the checkout price, from `BasketCheckedOut` through to the order's lines
- [ ] Step 2 — `Order` derives its total from its lines, and the read model returns it
- [ ] Step 3 — Fixture recaptured, docs and live lists updated

## Context

Closes [Backlog 8](../../../docs/BACKLOG.md) (*`BasketCheckedOut` Items Don't Reflect Current Price*) and [Backlog 1](../../../docs/BACKLOG.md) (*`Order.totalPrice` Doesn't Exist*).

Checkout already prices the cart at the current catalog price: `ShoppingCart.checkout` computes `totalPrice = Σ(current price × quantity)`. But each line on `BasketCheckedOut` is the cart's `Item`, which carries `priceWhenAdded`. So the event's lines don't add up to its own total, ordering builds every order `Item.price` from `priceWhenAdded`, and `Order` has no total at all. What the customer is charged and what the order records disagree.

### Decisions (settled with the user, 2026-10-19)

- **After checkout, the only price that matters is the one paid.** `priceWhenAdded` keeps its role on the cart's `Item` up to checkout and does not travel past it. Nothing downstream carries both prices.
  - This deliberately drops Backlog 8's original shape (`priceWhenAdded` *and* `currentPrice` per line) and its rationale (showing the customer the add-time vs. current delta). Nobody asked for that view; it was speculation, not a requirement.
- **The order is the record of what was paid.** Its lines carry the checkout price, and its total is derived from them.
- **Backlog 1's "trust or derive" question is settled as derive.** Once each line carries the checkout price, `Σ(price × quantity)` over the order's lines equals basket's `totalPrice` by construction, so `Order` owns its own total and never reads the event's. `BasketCheckoutIntegrationEvent.totalPrice` stays on the event (it is basket's statement of the charge) but ordering does not read it.
- **`BasketCheckedOut` is corrected in place at schema version 1, with no upcaster.** This departs from the policy in [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) (*Payload versioning*: every shape change bumps the version, existing fixtures are never edited). The user's reasoning: the event has never been written by a release, so there are no rows at rest to upcast, and the v1 shape stated a wrong price and would not have been committed had that been noticed. The policy text is left unchanged; this plan is the record of the exception.
  - **Consequence for a local database:** a `BasketCheckedOut` still *pending* in the local basket outbox would read with `price` = `null` (or fail and dead-letter). Processed rows are never read again. Clear the local stack (`docker compose down -v`) or check nothing is pending before running the new version.
- **Showing the customer the checkout price before paying is not in scope.** `POST /basket/checkout` returns `202` with no body, and the customer only ever sees cart prices (`priceWhenAdded`). Placing the price at checkout is what makes a preview possible later; the preview itself is routed to the Backlog in Step 3.

### Design

**Basket (domain).**
- New record `CheckedOutItem(UUID id, UUID productId, int quantity, BigDecimal price, String productName)` in `basket.domain.events`, next to the event it belongs to. Not an `Entity`, and not the cart's `Item`: on `Item`, the price means the add-time price, which is exactly the ambiguity being removed.
- `BasketCheckedOut.items` becomes `List<CheckedOutItem>`.
- `ShoppingCart.checkout` first turns each cart item into a `CheckedOutItem` priced from `currentPrices` (still throwing `UnpriceableCartItem` when a price is missing), then sums `price × quantity` over those lines. One pass prices the lines; the total is computed from them, so the two cannot disagree.
- `id` stays the cart item's id; ordering already uses it as the order item's id.

**Basket (contract) and the crossing.**
- New record `CheckedOutItemDTO(UUID id, UUID productId, int quantity, BigDecimal price, String productName)` in `basket.contract.integrationevents`, next to `BasketCheckoutIntegrationEvent`.
- `BasketCheckoutIntegrationEvent.items` becomes `List<CheckedOutItemDTO>`. The cart's `ItemDTO` is untouched: it is also the item type of `ShoppingCartDTO`, the cart read API, which keeps showing `priceWhenAdded`.
- `BasketOutboxDispatcher` maps `CheckedOutItem` → `CheckedOutItemDTO`.
- The integration event is not stored (it travels over the in-memory `MessageBus`), so its shape change needs no versioning.

**Ordering.**
- `BasketCheckoutIntegrationEventListener` builds each order `Item` from `CheckedOutItemDTO.price`. `Item`, `ItemDB` and ordering's `ItemDTO` already have a neutral `price`, so their shapes don't change; only the value does.
- `Order.totalPrice()` is derived: `Σ(item.price × item.quantity)`. No field, so `reconstitute` doesn't take it and it can never disagree with the lines.
- `Order.placeFromCheckout` additionally rejects an item with a `null` price (`IllegalArgumentException`, matching the other checks there), since the total can't be derived without it.
- `OrderDB` gains `BigDecimal totalPrice`, written by `DBMapper.toDb` from `order.totalPrice()`. It is stored because the read side returns DTOs straight from the database and must not repeat domain logic. `DBMapper.toDomain` ignores it.
- `OrderDTO` gains `BigDecimal totalPrice`, filled by `DBMapper.toDto` from `OrderDB.totalPrice`, and by `FakeOrderQueryRepository` the same way.
- Orders already stored locally have no `totalPrice` and read back with `null`. Pre-release; same remedy as the outbox note above.

### Testing seams and rigor per step

- **Step 1 — strict red → green → refactor for the pricing, tests alongside for the crossing.** Basket's event and the integration event change in one step because the dispatcher maps one to the other: changing `BasketCheckedOut.items` alone would not compile without a throwaway bridge onto the cart's `ItemDTO`, which the user chose not to write (2026-10-19). The red→green cycle is therefore longer than usual, so keep its red narrow:
  - **Red/green/refactor on pricing.** Seam: `ShoppingCart.checkout` (`ShoppingCartTest`), already the narrowed seam for checkout pricing, because pricing has more cases than the handler's setup is worth.
  - **Alongside for the crossing.** Mapping across an established path (dispatcher → bus → listener); a failing test first would teach nothing about the interface. Seams: `BasketOutboxDispatcherTest` (what leaves basket) and `BasketCheckoutIntegrationEventListenerTest` (what ordering records).

## Todo List

### 1. Checked-out lines carry the checkout price, through to the order
Red first, with only what the new test needs to compile:
- [ ] Add `CheckedOutItem` in `basket.domain.events` and `CheckedOutItemDTO` in `basket.contract.integrationevents`. Change `BasketCheckedOut.items` to `List<CheckedOutItem>` and `BasketCheckoutIntegrationEvent.items` to `List<CheckedOutItemDTO>`.
- [ ] Map them through: `BasketOutboxDispatcher` (`CheckedOutItem` → `CheckedOutItemDTO`), and `BasketCheckoutIntegrationEventListener.toDomain` (order item `price` ← `CheckedOutItemDTO.price`).
- [ ] In `ShoppingCart.checkout`, build the lines with `item.priceWhenAdded()` for now: compiles, and is the bug being fixed.

Red:
- [ ] `ShoppingCartTest`: a cart with two items added at one price and checked out at others (different quantities, so a per-line mistake can't hide) → the event's lines `containsExactly` `CheckedOutItem`s carrying the checkout price. Fails on the price.
- [ ] `ShoppingCartTest`, separate test: for the duplicate-product cart, `Σ(price × quantity)` over the event's lines `isEqualByComparingTo` `totalPrice`. This is the invariant Backlog 8 is about; it fails today too.

Green and refactor:
- [ ] `ShoppingCart.checkout` prices each line from `currentPrices` (a `priceAtCheckout(Item, Map)` helper keeps the `UnpriceableCartItem` check one level deep), collects them with `.toList()`, and sums the lines for the total. The `.toList()` also stops the event sharing the cart's live, mutable `items` list.
- [ ] Refactor: leave the line total inline in `checkout`. A `CheckedOutItem.lineTotal()` would be one more accessor to check in the serialized outbox payload.

Alongside:
- [ ] `CheckoutBasketHandlerTest` (both checkout tests): pull the cart `Item` into a variable and expect `containsExactly(new CheckedOutItem(cartItem.id(), product.id(), 1, product.price(), product.name()))`. This proves at the handler seam that the catalog price (783), not the add-time price (400), reaches the line.
- [ ] `BasketOutboxDispatcherTest`: build the event with a `CheckedOutItem`, expect a `CheckedOutItemDTO`.
- [ ] `BasketCheckoutIntegrationEventListenerTest` and ordering's test `DTOMapper`: send `CheckedOutItemDTO`s; assert the placed order's item `price` is the line's `price`.
- [ ] `BasketOutboxPayloadFixturesTest`: expect a `CheckedOutItem`. Edit `v1.json` by hand for now (`priceWhenAdded` → `price`) so the suite stays green; it is recaptured in Step 3.
- [ ] `./mvnw clean verify` green (the contract change crosses modules).

### 2. `Order` derives its total; the read model returns it
- [ ] Red: `CreateOrderHandlerTest` — an order placed from two lines with different prices and quantities has `totalPrice()` equal to `Σ(price × quantity)`.
- [ ] Red: `CreateOrderHandlerTest` — an item with a `null` price is rejected with `IllegalArgumentException`, and nothing is saved.
- [ ] Green: `Order.totalPrice()` and the null-price check in `placeFromCheckout`.
- [ ] Alongside: `OrderDB.totalPrice`, `OrderDTO.totalPrice`, `DBMapper.toDb`/`toDto`, `FakeOrderQueryRepository`. Assert the stored total in `MongoOrderRepositoryTest` (read the document directly) and the returned total in `MongoOrderQueryRepositoryTest`.
- [ ] Check the controller tests (`GetOrderByIdControllerTest`, `GetOrdersControllerTest`) for JSON assertions that need the new field.
- [ ] `./mvnw clean verify` green.

### 3. Fixture, docs, live lists
- [ ] Recapture `BasketCheckedOut/v1.json` from the running application (after `docker compose down -v`), replacing the hand edit from Step 1, and update the expected values in `BasketOutboxPayloadFixturesTest`.
- [ ] Bruno: check the order requests' documentation/assertions for the response shape and add `totalPrice` where orders are described.
- [ ] [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md): *Payload versioning* says "the first real upcaster is expected with Backlog 8" — change it to the first change to a released event. Check the pricing description near *Why a synchronous read here* still holds.
- [ ] [BACKLOG.md](../../../docs/BACKLOG.md): remove items 1 and 8. Add an entry for a checkout price preview (the customer sees the checkout price before paying), with the reasoning from *Decisions* above.
- [ ] [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md): Testing Gap 6 says Backlog item 1 approaches a second write in order creation. It doesn't: the total is a field of the same document. Reword it to point at Backlog 5 only.
- [ ] Check [README.md](../../../README.md) for anything describing order or checkout payloads.

## Verification
- [ ] `./mvnw clean verify` green.
- [ ] Manual run: add an item, change its price in the catalog, check out; the placed order's line price and `totalPrice` show the new price.

## Deferred
- **Checkout price preview** — routed to the Backlog in Step 3.
- **Showing the add-time vs. checkout price delta** — consciously dropped (see *Decisions*).
- **Policy text for pre-release shape corrections** — consciously not written; the exception is recorded here only, by the user's choice.
