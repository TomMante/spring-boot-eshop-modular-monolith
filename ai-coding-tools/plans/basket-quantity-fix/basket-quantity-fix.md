# Basket Quantity Fix

## Status

Draft — under discussion, not yet started.

## Context

[Known Bug 1](../../../docs/KNOWN_ISSUES.md) names two defects in `ShoppingCart.checkout(Map<UUID, BigDecimal> currentPrices, ...)`: the total-price calculation ignores `quantity`, and `CheckoutBasketHandler`'s `Collectors.toMap(ProductInfo::id, ProductInfo::price)` throws `IllegalStateException` when the same product was added twice, because `addItem` never merges duplicates. [Backlog 1](../../../docs/BACKLOG.md) tied fixing this to first deciding `Order`'s pricing basis (add-time vs. checkout-time) and adding `totalPrice` end to end. [Testing Gap 1](../../../docs/KNOWN_ISSUES.md) asks for an isolated `ShoppingCartTest` on `checkout`.

Discussing the plan surfaced that the pricing-basis decision is larger and less urgent than it looked, and that the current `totalPrice` is dead weight rather than a bug worth fixing in place:

- `totalPrice` is computed in `ShoppingCart.checkout`, carried across the module boundary in `BasketCheckoutIntegrationEvent`, and never read — `BasketCheckoutIntegrationEventListener` builds `CreateOrderCommand` without ever consulting `event.totalPrice()`. It is computed, sent, and dropped today.
- Item prices that do reach `Order` already come from `ItemDTO.priceWhenAdded` (add-time), regardless of what `checkout()` computes for the total. Making the total checkout-time-accurate would additionally require overwriting item prices before the event fires — new behavior nothing does today — plus a real decision on what happens when a product's current price can't be fetched (deleted/discontinued).
- `Order.totalPrice` should be a value `Order.create` derives from its own items (`Σ item.price × quantity`), not a number trusted from a caller — otherwise it's the same "computed once, possibly wrong, believed anyway" shape as today's bug. Deriving it inside `Order` also means the direct `POST /order` path and the basket-checkout path share one rule for free, with no extra field needed on `CreateOrderCommand`/`CreateOrderRequest`.

None of that is worth doing to fix a crash. So this plan **removes `totalPrice` from basket entirely** rather than fixing its calculation, and defers building `Order.totalPrice` to its own Backlog item once the pricing basis is actually decided. What's left in scope is the part that's a live defect right now: the duplicate-product crash, fixed at its root (`addItem`'s own invariant, not the price-map construction downstream that happens to trip over it).

Discussing the merge key surfaced a second simplification: `color` is basket-only (ordering's `Item` has never had it — confirmed at [Item.java:9](../../../SpringBoot-EshopModularMonolith/modules/ordering/src/main/java/com/github/eshopmodularmonolith/ordering/orders/domain/Item.java:9) and `BasketCheckoutIntegrationEventListener.toDomain(ItemDTO)` already never reads it), it's threaded through ~12 files for no consumer beyond display, and in a real catalog a different color would be a different product id. Removing it turns "merge key" into a non-question: `productId` is the only identifier.

### Design decisions

- **Merge key is `productId` alone.** `color` is removed from the basket add-item path end to end, not just ignored for merge purposes.
- **`totalPrice` is removed, not fixed**, from `ShoppingCart.checkout`, `BasketCheckedOut`, and `BasketCheckoutIntegrationEvent`. `Order.totalPrice` does not exist yet; building it is deferred (see Explicitly Deferred).
- **`CheckoutBasketHandler` stops calling `ProductService` at checkout.** Its only use today (`currentPrices`) exists solely to feed the calculation being removed. This also makes the "missing price → NPE" half of Known Bug 1 moot by construction — there's no fetch left to fail.
- **Testing Gap 1 is retargeted, not carried over as-is.** Once `checkout()` computes nothing, it has no calculation left to test in isolation. The logic that now qualifies under [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md) is `addItem`'s merge behavior (a calculation over existing state), so the new `ShoppingCartTest` targets that instead.
- **Merging replaces, not mutates.** `Item` is a record; "merging" means constructing a new `Item` with summed quantity and replacing the old list entry, not calling a setter.

### Testing rigor per step

Per the "TDD where it earns its cost" rule in [rules-of-engagement.md](../../processes/rules-of-engagement.md): steps 1 and 2 are field/parameter deletions with no new behavior to pin down — tests alongside implementation, updating what breaks. Step 3 is new deterministic logic with real regression risk (this is the actual bug fix) — strict red → green → refactor.

---

## Todo List

### 1. Remove `color` from the basket add-item path
*Deletion following an established shape — tests alongside implementation.*

- [ ] `Item` (domain) — drop `color`
- [ ] `ShoppingCart.addItem` — drop the `color` parameter
- [ ] `AddItemIntoBasketCommand` — drop `color`
- [ ] `AddItemIntoBasketRequest` — drop `color`
- [ ] `AddItemIntoBasketController` — stop reading `request.color()`
- [ ] `AddItemIntoBasketHandler` — stop passing a color into `cart.addItem`
- [ ] `ItemDB` (mongo) — drop `color`
- [ ] `DBMapper` — drop `color` from `toDb(Item)`, `toDomain(ItemDB)`, `toDTO(ItemDB)`
- [ ] `ItemDTO` (basket-contract) — drop `color`
- [ ] `BasketOutboxDispatcher.toDto(Item)` — drop `color`
- [ ] `bruno/Basket/Add Item Into Basket.bru` — drop the `"color": "red"` line
- [ ] Update tests that construct items/requests with a color: `AddItemIntoBasketControllerTest`, `AddItemIntoBasketHandlerTest`, `BasketOutboxDispatcherTest`, `ShoppingCartMother` (and any others the compiler finds)
- [ ] Check [README.md](../../../README.md) for any basket API example mentioning `color`

### 2. Remove `totalPrice` from basket checkout
*Deletion of dead/miscalculated logic — tests alongside implementation.*

- [ ] `ShoppingCart.checkout` — drop the `totalPrice` calculation and the `currentPrices` parameter; new signature is `checkout(Address billingAddress, Payment payment, Address shippingAddress)`
- [ ] `BasketCheckedOut` — drop the `totalPrice` field
- [ ] `BasketCheckoutIntegrationEvent` (basket-contract) — drop the `totalPrice` field
- [ ] `BasketOutboxDispatcher.toIntegrationEvent` — stop passing `event.totalPrice()`
- [ ] `CheckoutBasketHandler` — remove the `currentPrices` construction (the `ProductService.fetchProductInfo` loop and `Collectors.toMap`); call `cart.checkout(cmd.billingAddress(), cmd.payment(), cmd.shippingAddress())` directly. `ProductService` has no other use in this class (confirmed while planning), so remove its field and constructor parameter too
- [ ] Update `CheckoutBasketHandlerTest` / `CheckoutBasketHandlerTransactionalTest` — drop `currentPrices`/`ProductService` stubbing and any `totalPrice` assertions
- [ ] Update `BasketOutboxDispatcherTest` — drop `totalPrice` from constructed events/assertions
- [ ] Confirm `BasketCheckoutIntegrationEventListener` compiles unchanged in behavior (it already never reads `totalPrice`)

### 3. Fix `ShoppingCart.addItem` to merge duplicate products
*New deterministic behavior with real regression risk — strict red → green → refactor. This is the actual fix for Known Bug 1's remaining defect and closes Testing Gap 1 (retargeted).*

- [ ] Red first: new `ShoppingCartTest` — adding the same `productId` twice sums quantity into one `Item` instead of appending a second; adding two different products keeps both as separate items (existing behavior, guards against an overly broad merge)
- [ ] Green: `ShoppingCart.addItem` looks up an existing item by `productId`; if found, replace it with a new `Item` carrying summed quantity; otherwise append as today
- [ ] Refactor: revisit only if the lookup/replace reads awkwardly once written — no refactor forced if it doesn't

---

## Verification

- [ ] `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green
- [ ] Manual smoke via bruno: `POST /basket/items` twice with the same `productId` — basket ends up with one line at quantity 2, not two lines and not a crash
- [ ] Manual smoke: checkout that basket — no crash, resulting outbox/integration event payload has no `totalPrice` field anywhere
- [ ] [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) — remove Known Bug 1 entirely (the calculation is deleted, not fixed, and duplicate products can no longer occur)
- [ ] [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) — remove Testing Gap 1 (the logic it named no longer exists; the logic that replaced it — `addItem`'s merge — gets a test as part of this plan, so there's no gap left to record)
- [ ] [BACKLOG.md](../../../docs/BACKLOG.md) — replace Backlog 1 with a revised entry: `Order.totalPrice` does not exist at all (not "is wrong"); building it needs the add-time-vs-checkout-time decision, and if checkout-time is chosen, a design for overwriting item prices at checkout and for a product that can't be priced (deleted/discontinued). Note that `Order.create` should derive the total from its own items rather than accept one from a caller
- [ ] Check [README.md](../../../README.md) for anything describing basket item `color` or a checkout total price

---

## Explicitly deferred (recorded, not fixed here)

- **`Order.totalPrice`.** Doesn't exist after this plan, same as before it — but now it's a missing capability, not a wrong calculation, so it belongs in [BACKLOG.md](../../../docs/BACKLOG.md) rather than as a Known Bug. See the revised Backlog entry above.
- **Missing-price-at-checkout handling.** Moot for now — `CheckoutBasketHandler` no longer fetches current prices at checkout at all. Resurfaces as a real question once `Order.totalPrice` is built on a checkout-time basis.
- **Every other Known Bug and Testing Gap** (Known Bugs 2–7, Gaps 2–7) — untouched by this plan, unaffected.
