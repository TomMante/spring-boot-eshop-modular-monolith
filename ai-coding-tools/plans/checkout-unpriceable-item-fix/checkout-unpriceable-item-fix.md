# Checkout Unpriceable Item Fix

## Status
- [x] Step 1 — `ShoppingCart.checkout` rejects an unpriceable item with `UnpriceableCartItem` instead of NPE'ing. Full suite green.
- [x] Step 2 — `UnpriceableCartItem` and `BasketNotFound` surfaced as 4xx at `/basket/checkout`. Full suite green.

## Context

[Known Bug 1](../../../docs/KNOWN_ISSUES.md) (basket module): `CheckoutBasketHandler` builds its `currentPrices` map by calling `ProductService.fetchProductInfo` per cart item, then `.flatMap(Optional::stream)` silently drops any product that can't be found (deleted/discontinued between add and checkout). `ShoppingCart.checkout` then does `currentPrices.get(item.productId()).multiply(...)` — a `null` lookup NPEs instead of failing predictably.

The doc's own Action Required already settled the reject-vs-skip question during the prior [basket-quantity-fix](../basket-quantity-fix/basket-quantity-fix.md) plan's design discussion: reject the whole checkout, surfaced as a 4xx. This plan implements that.

While investigating, found `CheckoutBasketController` has no error handler at all — not even for the pre-existing `BasketNotFound` (checkout on a non-existent cart currently falls through to Spring's default 500, not a 404, unlike `GetBasketController` which maps it correctly). Bundling that fix in here, confirmed with the user, since the new error handler class is the natural place for it.

### Design decisions

- **New domain exception**, not a handler-level check: `ShoppingCart.checkout` is the aggregate computing the total — it should refuse to compute one it can't trust, same principle as the pricing fix in `basket-quantity-fix`. Named `UnpriceableCartItem`, in a new `basket.basket.domain.exceptions` package, mirroring `catalog...domain.exceptions.ProductNotFound` and `ordering...domain.exceptions.OrderNotFound` (both message style and package placement).
- **No `CheckoutBasketHandler` change needed.** Once the domain throws, the existing handler naturally propagates it — the handler's map-building logic doesn't need to change, only what happens when the aggregate is handed an incomplete map.
- **One new `CheckoutBasketErrorHandler`**, following the exact shape of `GetBasketErrorHandler` (package-scoped `@RestControllerAdvice`), mapping both:
  - `UnpriceableCartItem` → 400 via `ApiErrors.badRequest`
  - `BasketNotFound` → 404 via `ApiErrors.notFound` (same code/message shape `GetBasketErrorHandler` already uses for the same exception)

### Testing rigor per step

- **Step 1** (domain fix): strict TDD red → green → refactor. This changes existing checkout calculation logic with real regression risk.
- **Step 2** (error handler): mirrors an already-established pattern (`GetBasketErrorHandler`) — test alongside implementation, no strict red-first ceremony needed.

## Todo List

### 1. Fix `ShoppingCart.checkout` to reject an unpriceable item
- [x] Red first: in `ShoppingCartTest`, a cart item whose `productId` is missing from `currentPrices` → `checkout` throws `UnpriceableCartItem` (assert type via `assertThatExceptionOfType`), and no domain event is recorded.
- [x] Green: added `com.github.eshopmodularmonolith.basket.basket.domain.exceptions.UnpriceableCartItem extends RuntimeException` (constructor `(UUID productId)`, message `"Product with id [" + productId + "] could not be priced for checkout."`). `ShoppingCart.checkout` now throws it when a cart item's price is missing from `currentPrices`, before any multiplication.
- [x] Refactor: not needed — the null-check fit cleanly in the stream lambda.
- [x] Full suite green.

### 2. Surface `UnpriceableCartItem` and `BasketNotFound` as 4xx at `/basket/checkout`
- [x] Added `CheckoutBasketErrorHandler` (`@RestControllerAdvice(basePackages = "com.github.eshopmodularmonolith.basket.basket.application.features.checkoutbasket")`):
  - `UnpriceableCartItem` → `ApiErrors.badRequest("basket.checkout.unpriceable-item", exception.getLocalizedMessage())`
  - `BasketNotFound` → `ApiErrors.notFound("basket.not-found", exception.getLocalizedMessage())`
- [x] `CheckoutBasketControllerTest`: added `CheckoutBasketErrorHandler.class` to the `@Import`, plus two new tests mocking `CheckoutBasketHandler` to throw each exception and asserting the resulting status (400 / 404).
- [x] `CheckoutBasketHandlerTest`: added a test where `FakeProductQueryService` doesn't have the cart's product — asserts `handler.handle(cmd)` throws `UnpriceableCartItem`. Covers the originally-reported scenario (product removed from catalog between add and checkout), which the domain unit test alone didn't reach.
- [x] Full suite green (43/43 in basket-core; full reactor `mvn test` also green).

## Verification
- [x] `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green.
- [x] Updated `docs/KNOWN_ISSUES.md`: removed Known Bug 1 entirely (both the crash and the "surfaced as a 4xx" requirement are now satisfied).
- [x] Checked `docs/BACKLOG.md` item 1's reference to "closed Known Bug 1" — it refers to the *pricing* fix from `basket-quantity-fix`, not this one; still accurate, no change needed.
- [x] Checked [README.md](../../../README.md) for anything describing checkout error behavior — backend-only change, nothing there needed updating.

## Explicitly deferred
None anticipated. If anything surfaces mid-implementation, route it per the closeout convention (Known Issues / Backlog / consciously dropped) before opening the PR.
