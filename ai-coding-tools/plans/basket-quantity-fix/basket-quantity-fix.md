# Basket Quantity Fix

## Status

- [x] Step 1 — `color` removed from the basket add-item path. Full suite green.
- [x] Step 2 — `totalPrice` fixed to use current catalog prices. Full suite green.
- [x] Step 3 — `addItem` merges duplicate products by `productId`. Full suite green.

## Context

[Known Bug 1](../../../docs/KNOWN_ISSUES.md) names two defects in `ShoppingCart.checkout(Map<UUID, BigDecimal> currentPrices, ...)`: the total-price calculation ignores `quantity`, and `CheckoutBasketHandler`'s `Collectors.toMap(ProductInfo::id, ProductInfo::price)` throws `IllegalStateException` when the same product was added twice, because `addItem` never merges duplicates. [Backlog 1](../../../docs/BACKLOG.md) tied fixing this to first deciding `Order`'s pricing basis (add-time vs. checkout-time) and adding `totalPrice` end to end. [Testing Gap 1](../../../docs/KNOWN_ISSUES.md) asks for an isolated `ShoppingCartTest` on `checkout`.

Discussing the plan went through two revisions before landing here:

1. First pass: remove `totalPrice` from basket entirely, since it's computed and then never read (`BasketCheckoutIntegrationEventListener` never consults `event.totalPrice()`), and defer the whole pricing-basis question to Backlog.
2. Second pass, on reflection: that framed the gap too narrowly. Checkout genuinely is the moment a total should be confirmed against current prices — real checkout systems (Stripe, BigCommerce) compute the charge at commit time from live prices, not from whatever was true when the item was added to the cart. Deleting the calculation would have avoided the bug rather than fixed it, and thrown away the one piece of this codebase that already attempts real-time cross-module communication between basket and catalog at checkout.

Landed design: **fix `totalPrice`'s calculation to use current catalog prices** (`Σ currentPrice × quantity`, current price fetched from catalog via `ProductService` at checkout — the same fetch that already exists, just currently miscalculated). `priceWhenAdded` stays on `Item` — it isn't being replaced, it exists for a future add-time-vs-current-price delta view, which is exactly why the two are named differently rather than one field being overloaded. This plan does *not* build that delta view or reconcile it into `Order` — see Explicitly Deferred.

Two things are deliberately left unfixed here, by explicit choice: the priority is demonstrating the current-price-at-checkout mechanism and the fact that add-time and current price are different concepts worth tracking separately — not shipping a fully hardened checkout. Both are documented as open items rather than silently left broken:

- **Missing-price handling.** If `ProductService.fetchProductInfo` can't find a product, `CheckoutBasketHandler`'s `flatMap(Optional::stream)` silently drops it from `currentPrices` — so `ShoppingCart.checkout` can still throw a `NullPointerException` looking up that product's price. This is Known Bug 1's second defect, unfixed, narrowed and re-recorded rather than closed.
- **The event's own numbers won't reconcile.** `BasketCheckedOut`/`BasketCheckoutIntegrationEvent` will carry a `totalPrice` computed from current prices, while `items` on the same event still show `priceWhenAdded`. Summing the item lines on the event will not equal its `totalPrice`. This is a real, visible rough edge — recorded as a Backlog decision (a shape carrying both prices per item, e.g. a dedicated `CheckedOutItem`) rather than solved now.

Discussing the merge key surfaced a separate, unrelated simplification: `color` is basket-only (ordering's `Item` has never had it — confirmed at [Item.java:9](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/78a4383518244b3d834a13b9b558448b9a891b1c/SpringBoot-EshopModularMonolith/modules/ordering/src/main/java/com/github/eshopmodularmonolith/ordering/orders/domain/Item.java#L9) and `BasketCheckoutIntegrationEventListener.toDomain(ItemDTO)` already never reads it), it's threaded through ~12 files for no consumer beyond display, and in a real catalog a different color would be a different product id. Removing it turns "merge key" into a non-question: `productId` is the only identifier. Done in step 1.

### Design decisions

- **Merge key is `productId` alone.** `color` is removed from the basket add-item path end to end, not just ignored for merge purposes. *(Step 1, done.)*
- **`totalPrice` is fixed, not removed.** `ShoppingCart.checkout` keeps its `currentPrices` parameter and computes `Σ currentPrices.get(item.productId()) × item.quantity()`. `CheckoutBasketHandler` keeps calling `ProductService` at checkout — that real-time catalog fetch is the point, not incidental plumbing to delete.
- **`priceWhenAdded` is untouched and not consumed by this calculation.** It stays on `Item` for a future delta view; `totalPrice` is priced entirely off `currentPrices`, not blended with it.
- **Missing-price handling is out of scope, not silently ignored.** The existing silent-drop behavior stays as today's code has it; the resulting possible `NullPointerException` is re-recorded as a narrowed Known Bug rather than fixed, since deciding reject-vs-skip is real, separate design work.
- **The event's total-vs-items inconsistency is accepted, not designed away.** Making `items` reflect current price too (via a new type, since `Item` itself is persisted cart state and shouldn't carry a checkout-only field) is real, separate work — recorded as a Backlog decision, not built here.
- **`Order` still doesn't consume `totalPrice`.** `CreateOrderCommand`/`Order.create` are untouched by this plan. The gap between "basket computes a real total" and "ordering ignores it" persists and stays a Backlog item.
- **Testing Gap 1 closes as originally scoped.** The isolated `ShoppingCartTest` this plan adds covers `checkout()`'s calculation directly — the exact gap the doc named — plus `addItem`'s new merge behavior from step 3.
- **Merging (step 3) replaces, not mutates.** `Item` is a record; "merging" means constructing a new `Item` with summed quantity and replacing the old list entry, not calling a setter.

### Testing rigor per step

Per the "TDD where it earns its cost" rule in [rules-of-engagement.md](../../processes/rules-of-engagement.md): step 1 was a field deletion with no new behavior to pin down — tests alongside implementation. Steps 2 and 3 are both new deterministic calculations with real regression risk — strict red → green → refactor, both landing in the same new `ShoppingCartTest`.

---

## Todo List

### 1. Remove `color` from the basket add-item path — done
*Deletion following an established shape — tests alongside implementation.*

- [x] `Item` (domain) — drop `color`
- [x] `ShoppingCart.addItem` — drop the `color` parameter
- [x] `AddItemIntoBasketCommand` — drop `color`
- [x] `AddItemIntoBasketRequest` — drop `color`
- [x] `AddItemIntoBasketController` — stop reading `request.color()`
- [x] `AddItemIntoBasketHandler` — stop passing a color into `cart.addItem`
- [x] `ItemDB` (mongo) — drop `color`
- [x] `DBMapper` — drop `color` from `toDb(Item)`, `toDomain(ItemDB)`, `toDTO(ItemDB)`
- [x] `ItemDTO` (basket-contract) — drop `color`
- [x] `BasketOutboxDispatcher.toDto(Item)` — drop `color`
- [x] `bruno/Basket/Add Item Into Basket.bru` — drop the `"color": "red"` line
- [x] Updated tests: `AddItemIntoBasketControllerTest`, `AddItemIntoBasketHandlerTest`, `BasketOutboxDispatcherTest`, `CheckoutBasketHandlerTest`, `CheckoutBasketHandlerTransactionalTest`, `RemoveItemFromBasketHandlerTest`, `BasketCheckoutIntegrationEventListenerTest`
- [x] No remaining references to `color`/`Color` anywhere in `modules/` (confirmed by grep)
- [x] Checked [README.md](../../../README.md) for any basket API example mentioning `color` — no mentions of `color` or basket endpoints at all, nothing to update

### 2. Fix `totalPrice` to use current catalog prices
*New deterministic calculation with real regression risk — strict red → green → refactor.*

- [x] Red first: new `ShoppingCartTest` — `checkout()` with multiple items and quantities greater than 1 produces `totalPrice = Σ(currentPrices.get(productId) × quantity)`, using current prices that deliberately differ from each item's `priceWhenAdded`, and per-product subtotals that differ from each other (7×15.00 vs 3×25.00) so the total can't coincidentally match the old bug, a swapped-quantity mistake, or an accidental use of `priceWhenAdded`. Plus a second case: two `Item`s reconstituted with the same `productId` both get priced (pins the "duplicate keys tolerated" half of Known Bug 1's original text, independent of step 3's `addItem` fix)
- [x] Green: `ShoppingCart.checkout` — fixed the reduce to multiply by `quantity`; `currentPrices` parameter unchanged
- [x] Refactor: not warranted — left as written
- [x] `CheckoutBasketHandler` — confirmed no change needed; still builds `currentPrices` via `ProductService.fetchProductInfo` / `Collectors.toMap` exactly as before
- [x] `BasketCheckedOut` — confirmed no change; `totalPrice` field unchanged
- [x] `BasketCheckoutIntegrationEvent` — confirmed no change; `totalPrice` field unchanged
- [x] `BasketOutboxDispatcher` — confirmed no change; still passes `event.totalPrice()`
- [x] `CheckoutBasketHandlerTest` / `CheckoutBasketHandlerTransactionalTest` — checked, no update needed: both fixtures use quantity 1, so their `totalPrice` assertions are numerically unaffected by the fix. Per the seam decision above, these tests only need to prove one happy path each — the quantity cases are the domain test's job, not theirs
- [x] `BasketOutboxDispatcherTest` — checked, no update needed: it constructs `BasketCheckedOut` by hand rather than via `checkout()`, so it never exercised the calculation to begin with
- [x] Confirmed `BasketCheckoutIntegrationEventListener` unaffected (it already never reads `totalPrice`)
- [x] Full suite green (`basket-contract`, `basket-core`, `ordering`)

### 3. Fix `ShoppingCart.addItem` to merge duplicate products
*New deterministic behavior with real regression risk — strict red → green → refactor. Together with step 2, closes Testing Gap 1.*

**Caveat found during Copilot review, not fully closed:** this step stops `addItem` from ever *creating* a duplicate `productId` going forward, but `CheckoutBasketHandler`'s `Collectors.toMap(ProductInfo::id, ProductInfo::price)` still has no merge function — a cart that already contains a duplicate (e.g. reconstituted from data written before this fix) would still throw `IllegalStateException` there, before `ShoppingCart.checkout` is ever reached. `ShoppingCartTest`'s duplicate-item case proves `checkout()`'s own calculation is robust to duplicates; it doesn't cover the handler's map-building, which is where the crash actually lives. Known Bug 1's "duplicate-product crash" framing was closed based on the `addItem` fix alone — that claim is narrower than it reads.

- [x] Red first: `ShoppingCartTest` — adding the same `productId` twice sums quantity into one `Item` instead of appending a second, and adding a different product keeps both as separate items (guards against an overly broad merge), both asserted in one test
- [x] Green: `ShoppingCart.addItem` scans for an existing item by `productId`; if found, replaces it in place via `Item.increaseQuantity(amount)` (a new method on the record, keeping the original `id`, `priceWhenAdded`, and `productName` — only quantity changes); otherwise appends as today
- [x] Refactor: done — the new `Item` is now only constructed on the append path (`items.add(new Item(...))`), not unconditionally before the merge check. The nit noted here earlier no longer applies to the code as written.
- [x] Full suite green (confirmed by user; not independently re-run per their instruction)

---

## Verification

- [ ] `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green
- [ ] Manual smoke via bruno: `POST /basket/items` twice with the same `productId` — basket ends up with one line at quantity 2, not two lines and not a crash
- [ ] Manual smoke: checkout that basket — no crash, resulting outbox/integration event's `totalPrice` reflects current catalog price × quantity, not `priceWhenAdded`
- [x] [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) — narrowed Known Bug 1 to just the remaining defect (`ShoppingCart.checkout` can still NPE on a product that can't be priced at checkout). Quantity-ignored and the duplicate-product crash are both genuinely fixed and dropped from the bug's text
- [x] [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) — removed Testing Gap 1 (closed for real by `ShoppingCartTest`; remaining gaps keep their original numbers 2–7, not renumbered, matching this project's existing numbering convention)
- [x] [BACKLOG.md](../../../docs/BACKLOG.md) — revised item 1 into `Order.totalPrice` Doesn't Exist (resolved pricing-basis context, decision now framed as trust-vs-derive), and added a new item 8, `BasketCheckedOut` Items Don't Reflect Current Price
- [x] Checked [README.md](../../../README.md) for anything describing basket item `color` or a checkout total price — no mentions of either, nothing to update

---

## Explicitly deferred (recorded, not fixed here)

- **Missing-price-at-checkout handling.** `ShoppingCart.checkout` can still NPE if a product can't be priced. Re-recorded as a narrowed Known Bug 1 rather than fixed — deciding reject-vs-skip is its own design work, not an extension of the arithmetic fix.
- **`BasketCheckedOut` items not carrying current price.** New Backlog entry — needs a dedicated type, not a retrofit of `Item`.
- **`Order.totalPrice`.** Still doesn't exist; still a Backlog entry, now with a real (if unused) source value to eventually decide whether to trust.
- **The add-time-vs-current-price "delta" view itself** — showing the customer both numbers — isn't designed here at all, only motivated. It depends on the `CheckedOutItem`-shaped Backlog item above existing first.
- **Every other Known Bug and Testing Gap** (Known Bugs 2–7, Gaps 2–7) — untouched by this plan, unaffected.
