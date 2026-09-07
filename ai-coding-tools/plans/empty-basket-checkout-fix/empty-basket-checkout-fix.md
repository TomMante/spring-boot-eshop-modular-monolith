# Empty Basket Checkout Fix

## Status
- [ ] Step 1 — `ShoppingCart.checkout` rejects an empty cart.
- [ ] Step 2 — `Order.create` refuses an empty item list.
- [ ] Step 3 — `CreateOrderRequest.items` rejects null/empty at the `/order` entry point.

## Context

[Known Bug 5](../../../docs/KNOWN_ISSUES.md) (basket/ordering modules): nothing rejects a checkout with no items. `CheckoutBasketHandler` doesn't guard, `ShoppingCart.checkout` reduces an empty item stream to a `totalPrice` of zero, and `Order.create` validates address and payment but not the item list — so a persisted, zero-total, item-less order is indistinguishable from a real one. The doc's own Action Required already names both fixes needed: reject at the checkout entry point (400) *and* have `Order.create` refuse an empty item list as its own invariant — fixing only the aggregate would turn a bad request into a poison outbox event (Known Bug 2) instead of a synchronous HTTP error.

While investigating, found a related but distinct gap: `CreateOrderRequest.items` has no `@NotNull`/`@NotEmpty` (unlike every other field on that request). A `POST /order` with `items: null` or the field omitted NPEs in `CreateOrderController` on `request.items().stream()`, before validation or `Order.create` ever run. Bundling a fix in, confirmed with the user, since it's the same invariant on the same aggregate's other entry point.

### Design decisions

- **Basket side, same shape as [checkout-unpriceable-item-fix](../checkout-unpriceable-item-fix/checkout-unpriceable-item-fix.md):** a new domain exception (`EmptyCartCheckout`, no-arg, in the existing `basket.basket.domain.exceptions` package) thrown by `ShoppingCart.checkout` itself when `items` is empty — checked first, before any pricing work. Mapped to 400 in the **existing** `CheckoutBasketErrorHandler` (just one more `@ExceptionHandler` method — no new controller-advice class needed).
- **Ordering side, matches `Order.create`'s own existing style:** the method already throws plain `IllegalArgumentException` for missing billing address / payment / shipping address. Add a fourth check, `items == null || items.isEmpty()`, same style, no new exception type — introducing a named domain exception here would be inconsistent with its three siblings in the same method.
- **`CreateOrderRequest.items`:** add `@NotEmpty` (rejects both `null` and `[]` in one annotation), matching the `@NotBlank`/`@NotNull` already on `customer`/`billingAddress`/`payment`/`shippingAddress`. This makes `Order.create`'s new check unreachable from the direct `/order` REST path (caught earlier by validation) — it remains reachable, and is still needed, via `BasketCheckoutIntegrationEventListener`, which has no request-level validation.
- **Not touching `CreateOrderErrorHandler`** (currently empty — Testing Gap 4) or Known Bug 2's poison-event handling. If `Order.create`'s new invariant is ever violated via the integration-event path, that failure mode is already tracked there; out of scope here.

### Testing rigor per step

- **Step 1** (basket domain fix): strict TDD red → green → refactor — new deterministic invariant with real regression risk.
- **Step 2** (`Order.create` fix): tests alongside implementation — mirrors three sibling checks already in the same method.
- **Step 3** (`CreateOrderRequest` validation): tests alongside implementation — mirrors the `@NotBlank`/`@NotNull` pattern already on sibling fields of the same request.

## Todo List

### 1. `ShoppingCart.checkout` rejects an empty cart
- [ ] Red first: in `ShoppingCartTest`, `ShoppingCart.create(...)` with no items → `checkout` throws `EmptyCartCheckout`, and no domain event is recorded.
- [ ] Green: add `com.github.eshopmodularmonolith.basket.basket.domain.exceptions.EmptyCartCheckout extends RuntimeException` (no-arg, message `"Cannot checkout an empty cart."`). `ShoppingCart.checkout` throws it first, before building `currentPrices`/`totalPrice`, when `items.isEmpty()`.
- [ ] Refactor: none anticipated.
- [ ] Add to the existing `CheckoutBasketErrorHandler`: `EmptyCartCheckout` → `ApiErrors.badRequest("basket.checkout.empty-cart", exception.getLocalizedMessage())`.
- [ ] `CheckoutBasketControllerTest`: one test mocking `CheckoutBasketHandler` to throw `EmptyCartCheckout`, asserting 400 + `$.code == "basket.checkout.empty-cart"` (same shape as the existing unpriceable-item test).
- [ ] `CheckoutBasketHandlerTest`: one test — save a cart via `ShoppingCartMother.create().build()` (no items), assert `handler.handle(cmd)` throws `EmptyCartCheckout`. This is the originally-reported flow: create a basket, checkout without adding anything.
- [ ] Full suite green.

### 2. `Order.create` refuses an empty item list
- [ ] Add `if (items == null || items.isEmpty()) throw new IllegalArgumentException("Order must contain at least one item");` to `Order.create`, alongside its three existing checks.
- [ ] `CreateOrderHandlerTest`: add "Given create order command with no items, when handler is called, then order is rejected" (`List.of()`), mirroring the existing missing-billing/-payment/-shipping tests exactly (`assertThatExceptionOfType(IllegalArgumentException.class)...withMessageContaining(...)`).
- [ ] Full suite green.

### 3. `CreateOrderRequest.items` rejects null/empty at the `/order` entry point
- [ ] Add `@NotEmpty(message = "{ordering.order.items.notEmpty}")` to `CreateOrderRequest.items`.
- [ ] Add `ordering.order.items.notEmpty=At least one item is required.` to `modules/ordering/src/main/resources/messages.properties`, alongside the three existing keys.
- [ ] `CreateOrderControllerTest`: two tests mirroring the existing missing-field tests — `items: null` and `items: []` — both expecting 400 + `$.fieldErrors.items == "At least one item is required."`.
- [ ] Full suite green.

## Verification
- `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green.
- Update `docs/KNOWN_ISSUES.md`: remove Known Bug 5 entirely (both required rules now in place, plus the bundled `/order` request-validation gap).
- Check [README.md](../../../README.md) for anything describing checkout/order-creation validation that needs updating (backend-only change; confirm nothing to update).

## Explicitly deferred
None anticipated. If anything surfaces mid-implementation, route it per the closeout convention (Known Issues / Backlog / consciously dropped) before opening the PR.
