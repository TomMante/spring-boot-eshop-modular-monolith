# Tokenized Card Payment

Draft, 2026-10-21. Not approved yet.

## Status
- [ ] Step 1 — `ShoppingCart` checks out only against a charge that matches its own total.
- [ ] Step 2 — `CheckoutBasketHandler` charges through a `PaymentGateway` port, outside the transaction, and refunds when the transaction fails.
- [ ] Step 3 — Card data leaves the request, the event, the contract and ordering.
- [ ] Step 4 — `StripePaymentGateway` adapter, tested against the WireMock stubs.
- [ ] Step 5 — Wiring, compose, Bruno, fixture, docs and live lists.

## Context

Closes [Known Bug 4](../../../docs/KNOWN_ISSUES.md) (*Raw Card Data Is Stored and Transmitted in Clear*).

Today `POST /basket/checkout` accepts `cardName`, `cardNumber`, `expiration` and `cvv`. They travel unchanged through `CheckoutPayment` → basket's `Payment` → `BasketCheckedOut` (stored as JSON in the basket outbox) → `BasketCheckoutIntegrationEvent` (`basket.contract.PaymentDTO`) → ordering's `Payment` → `PaymentDB` (stored in the order document) → `orders.contract.PaymentDTO`. That last one is part of `OrderDTO`, so `GET /orders` and `GET /order/{id}` **return the CVV to the client**. No step ever charges the card.

Background on what PCI DSS allows is in [card-data-and-pci-dss.md](../../context/card-data-and-pci-dss.md). In short: the CVV may not be kept after authorization, encrypted or not, and the usual way to stay out of scope is never to receive card data at all, only a provider token.

### Decisions (settled with the user, 2026-10-21)

- **The shop never receives card data.** The client gets a payment method id from the provider (in a browser: the provider's hosted card field; here: Bruno sends a Stripe test id such as `pm_card_visa`). Checkout carries only that id.
- **Payment is an external system behind a port** in basket's domain, like `ProductService`. No `payment` module in this increment.
- **The charge is synchronous, at checkout.** The customer gets a yes or a no from `POST /basket/checkout` itself, before anything is written.
- **The adapter is real Stripe code.** It uses the `stripe-java` SDK. Only the base URL and key change between environments.
- **The provider stand-in is WireMock serving Stripe-shaped stubs**, not stripe-mock. A spike on 2026-10-21 against `stripe/stripe-mock` 0.206.0 showed it validates requests strictly but returns a fixed PaymentIntent with `status: requires_payment_method` for every request, including a declining card. A correct adapter would read every payment as not completed. stripe-mock as an extra schema check in one adapter test was offered and not chosen.

### Proposed decisions (to confirm in review)

Each of these has a recommendation in the design below. They are listed here so they can be accepted or changed in one place.

1. **Charge immediately, not authorize-then-capture.** Nothing in the system ships an order, so nothing would ever capture a held authorization; it would expire after 7 days. So the PaymentIntent uses automatic capture, and the compensation when the transaction fails is a refund. *Alternative:* manual capture with cancel as compensation, and capture routed to the Backlog as "capture on fulfilment".
2. **One configured currency.** Prices in this system have no currency. The adapter sends the currency from configuration (`eur`), and converts the amount to minor units exactly, rejecting an amount with more than two decimals instead of rounding it.
3. **A decline answers `402 Payment Required`** (`basket.checkout.payment-declined`), the provider being unreachable answers `503` (`basket.checkout.payment-unavailable`), and an unknown or unusable payment method id answers `400` (`basket.checkout.payment-method-invalid`). The cart is kept in all three cases.
4. **Pending v1 `BasketCheckedOut` rows are not upcast into orders.** A v1 row holds a card and no charge, so there is nothing truthful to turn it into. The v1→v2 upcaster throws an exception that says so; the row dead-letters like any other failure. No release has written v1 rows outside local stacks, and the local remedy is `docker compose down -v`.
5. **Orders already stored locally** keep their v1 `PaymentDB` shape and read back with `null` in the new fields. Same remedy: `docker compose down -v`. No migration script.
6. **The card summary comes from the provider, never from the client.** Brand, last four digits and expiry are read from the charge's payment method in the provider's response. The client can't make an order display a card it didn't pay with.

### Design

**Basket (domain).**
- New port `domain.port.PaymentGateway`:
  ```java
  public interface PaymentGateway {
      Charge charge(ChargeRequest request);   // throws PaymentDeclined, InvalidPaymentMethod, PaymentProviderUnavailable
      void refund(Charge charge);
  }
  ```
  - `ChargeRequest(UUID checkoutId, String paymentMethodId, BigDecimal amount)`. `checkoutId` is the idempotency key, so a retried request never charges twice.
  - `Charge(String reference, BigDecimal amount, CardSummary card)`, `CardSummary(String brand, String last4, int expiryMonth, int expiryYear)`.
  - Declines are exceptions, matching how the module already reports `EmptyCartCheckout` and `UnpriceableCartItem`. A sealed result type would make the handler branch on something it can only abort on anyway.
- `Payment` (card number, CVV, …) is removed. `Charge` takes its place on `BasketCheckedOut`.
- `ShoppingCart` gains `BigDecimal totalAt(Map<UUID, BigDecimal> currentPrices)`, using the same pricing as checkout (`UnpriceableCartItem` included), so the handler knows what to charge.
- `ShoppingCart.checkout(UUID checkoutId, Map<UUID, BigDecimal> currentPrices, Instant checkedOutAt, Address billingAddress, Charge charge, Address shippingAddress)`:
  - rejects a `charge` whose amount differs from the total it computes itself (`ChargeDoesNotMatchTotal`). This is the invariant that ties the money taken to the lines on the order, whatever the caller did between pricing and checkout.
  - uses `checkoutId` as `BasketCheckedOut.eventId`. Ordering already treats the event id as the checkout id, so the id the provider saw, the outbox row and the order's `checkoutId` are one value.

**Basket (application).**
- `CheckoutPayment(@NotBlank String paymentMethodId)` replaces the four card fields. Messages: drop the four `basket.checkout.payment.*.notBlank` keys, add `basket.checkout.payment.paymentMethodId.notBlank`.
- `CheckoutBasketCommand` carries `String paymentMethodId` instead of `Payment`.
- `CheckoutBasketHandler.handle` is no longer `@Transactional` as a whole. The provider call must not sit inside a Mongo transaction (it would hold the transaction open for a network round trip, and a rollback can't undo a charge). New order:
  1. load the cart, fetch current prices (unchanged);
  2. `checkoutId = UUID.randomUUID()`; `charge = paymentGateway.charge(new ChargeRequest(checkoutId, cmd.paymentMethodId(), cart.totalAt(prices)))`. A decline propagates; nothing has been written;
  3. in a `TransactionTemplate` on `basketTransactionManager`: `cart.checkout(checkoutId, prices, …, charge, …)` and `repository.delete(cart)`;
  4. if step 3 throws, `paymentGateway.refund(charge)` and rethrow. A refund that itself fails is logged at ERROR with the charge reference and checkout id, and the original exception is rethrown.
  - `TransactionTemplate`, not a second `@Transactional` method: a call from the handler to its own method bypasses the proxy.
  - Two concurrent checkouts of one cart get two charges; Mongo rejects one transaction with a write conflict on the delete, and that one is refunded. Covered by step 4, not by a dedicated test.
- `CheckoutBasketErrorHandler` maps `PaymentDeclined` → 402, `InvalidPaymentMethod` → 400, `PaymentProviderUnavailable` → 503. The response message never includes the provider's raw text beyond its decline code.

**Basket (infrastructure).**
- `infrastructure.adapters.payment.StripePaymentGateway implements PaymentGateway`, built on a `StripeClient`:
  - `charge`: `PaymentIntent` create with `amount` (minor units), `currency`, `payment_method`, `confirm=true`, `metadata[checkout_id]`, `expand[]=payment_method`, and the checkout id as `Idempotency-Key`. Return a `Charge` only for `status=succeeded` **and** a returned amount equal to the requested one; anything else (`requires_action`, `processing`, …) is `PaymentDeclined` with the status as reason, since this flow has no way to complete 3-D Secure.
  - `CardException` → `PaymentDeclined(declineCode)`; `InvalidRequestException` on the payment method → `InvalidPaymentMethod`; `ApiConnectionException`, `RateLimitException`, `ApiException` (5xx) → `PaymentProviderUnavailable`. `AuthenticationException` (wrong key) is a configuration error and is not translated.
  - `refund`: `Refund` create for the PaymentIntent, idempotency key `refund-<checkoutId>`.
  - Amount conversion in a small package-private function: `amount.setScale(2)` without rounding (`ArithmeticException` → `IllegalArgumentException` naming the amount), then `movePointRight(2).longValueExact()`.
- `BasketModuleConfig` builds the `StripeClient` from `eshop.basket.payment.stripe.{api-base, api-key}` with connect/read timeouts and `maxNetworkRetries(2)` (safe because every call carries an idempotency key), and the gateway from it plus `eshop.basket.payment.currency`.
- Nothing logs a payment method id, charge request or provider response body.

**Payload versioning.**
- `BasketCheckedOut` goes to schema version 2: `payment` becomes `charge` (`reference`, `amount`, `card`).
- `BasketCheckedOutV1ToV2` throws `IllegalStateException`: a v1 checkout carries card data and no charge, so it can't be placed as a paid order; it will be dead-lettered and must be discarded by hand. (Proposed decision 4.)
- New fixture `BasketCheckedOut/v2.json`, captured from the running application. `v1.json` stays as it is.

**Basket (contract) and the crossing.**
- `basket.contract.PaymentDTO` becomes `ChargeDTO(String reference, BigDecimal amount, CardSummaryDTO card)`; `CardSummaryDTO(String brand, String last4, int expiryMonth, int expiryYear)`. `BasketCheckoutIntegrationEvent.payment` → `charge`. Not stored, so no versioning.
- `BasketOutboxDispatcher` maps `Charge` → `ChargeDTO`. `application.mapper.DTOMapper.toDto(Payment)` goes (check for other callers when removing it).

**Ordering.**
- `domain.valueobjects.Payment` becomes `Payment(String chargeReference, BigDecimal amount, CardSummary card)`. `Order.placeFromCheckout` keeps rejecting a `null` payment, and also rejects a payment whose `amount` differs from `totalPrice()`: the order would otherwise record a total that wasn't what was charged.
- `PaymentDB` and `orders.contract.PaymentDTO` get the same shape, `DBMapper` follows. `OrderDTO` therefore exposes only the reference, the amount and the card summary.
- `BasketCheckoutIntegrationEventListener.toDomain` maps `ChargeDTO`.

**Provider stand-in.**
- WireMock stubs under `SpringBoot-EshopModularMonolith/docker/sb-eshop/payment-provider/mappings/`, modelled on Stripe's documented responses:
  - `POST /v1/payment_intents` with `payment_method=pm_card_visa` → `200`, `status: succeeded`, `amount`/`currency`/`metadata` echoed from the form body (response templating), `payment_method` expanded with card `visa`/`4242`;
  - `payment_method=pm_card_mastercard` → the same with `mastercard`/`4444`, so a second card shows the summary isn't hard-coded;
  - `payment_method=pm_card_chargeDeclined` → `402`, `error.type: card_error`, `code: card_declined`, `decline_code: generic_decline`;
  - `payment_method=pm_card_authenticationRequired` → `200`, `status: requires_action`, to exercise the "can't complete 3-D Secure" path;
  - any other `payment_method` → `400`, `invalid_request_error`, `code: resource_missing`, `param: payment_method`;
  - `POST /v1/refunds` → `200`, `status: succeeded`.
- Compose service `payment-provider`, image `wiremock/wiremock` pinned to a 3.x tag (3.13.2 was the newest found on 2026-10-21; check before pinning), started with `--global-response-templating`, mappings mounted read-only, port `12111` on the host.
- `application.yaml`: `api-base: http://localhost:12111`, `api-key: sk_test_wiremock`, `currency: eur`. The key is a placeholder WireMock ignores, overridable by environment variable; a real test key from a Stripe account is the only other change needed to run against Stripe test mode.

### Testing seams and rigor per step

- **Step 1 — strict red → green → refactor.** Seam: `ShoppingCart.checkout` / `totalAt` (`ShoppingCartTest`). The charge-matches-total rule is an invariant that must hold whoever calls checkout, which is the narrowing criterion in [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md).
- **Step 2 — strict red → green → refactor at the handler seam.** Seam: `CheckoutBasketHandler`, with a new `FakePaymentGateway` at the port (scriptable to decline, to be unavailable, and recording charges and refunds as state, not as call verification). The outcomes are: a decline leaves the cart and writes no event; a success writes the event with the charge; a failed transaction leaves the cart and the charge refunded. The rollback case needs a real transaction, so it lives in `CheckoutBasketHandlerTransactionalTest` (outbox save fails → cart present, outbox empty, fake shows the charge refunded). Controller status mapping: tests alongside in `CheckoutBasketControllerTest`, since it mirrors the existing error-handler pattern.
- **Step 3 — tests alongside, except the upcaster.** Mapping across an established path (dispatcher → bus → listener → order) teaches nothing by failing first. The upcaster gets red first in `BasketOutboxPayloadFixturesTest` (the v1 fixture must now fail with the explaining message), and `Order`'s new amount check gets red first in `CreateOrderHandlerTest`, matching how its other guards are tested.
- **Step 4 — strict red → green → refactor for the adapter.** Seam: `StripePaymentGateway`, against WireMock loaded with **the same mapping files compose uses**, so the test proves the stubs the demo runs on. The amount conversion gets its own small red/green first, as a deterministic function with edge cases (scale 0, scale 2, scale 3 rejected, large amounts).
- **Step 5 — no new tests;** full suite plus the manual run.

## Todo List

### 1. `ShoppingCart` checks out only against a matching charge
Red first, with only what the tests need to compile:
- [ ] Add `Charge`, `CardSummary` (basket `domain.valueobjects`) and `ChargeDoesNotMatchTotal` (`domain.exceptions`).
- [ ] Add `totalAt(Map)` returning `BigDecimal.ZERO`, and the new `checkout(UUID, Map, Instant, Address, Charge, Address)` signature delegating to today's logic. Keep `Payment` on the event for now: put `null` there, it is replaced in step 3. Until step 2 adds the port, `CheckoutBasketHandler` passes a placeholder `Charge` built from `cart.totalAt(prices)`, so the existing handler tests stay green.

Red:
- [ ] `ShoppingCartTest`: `totalAt` for the two-item, two-price cart equals `Σ(price × quantity)`; an item with no current price throws `UnpriceableCartItem`.
- [ ] `ShoppingCartTest`: checkout with a charge of a different amount throws `ChargeDoesNotMatchTotal` and records no event.
- [ ] `ShoppingCartTest`: checkout uses the given `checkoutId` as the event id, and the event carries the charge.

Green and refactor:
- [ ] Implement; `checkout` reuses `totalAt`'s pricing so the two can't disagree.
- [ ] `./mvnw test -pl modules/basket/basket-core -am` green.

### 2. Handler charges outside the transaction
Scaffolding (compiles, behaviour unchanged):
- [ ] `PaymentGateway` port, `ChargeRequest`, `PaymentDeclined`, `InvalidPaymentMethod`, `PaymentProviderUnavailable`.
- [ ] `FakePaymentGateway` under `src/test/…/infrastructure/adapters/fake/`.
- [ ] `CheckoutBasketCommand.paymentMethodId`; `CheckoutPayment` keeps its card fields until step 3, mapped to a placeholder id, so the controller still compiles.

Red:
- [ ] `CheckoutBasketHandlerTest`: success → one charge for the cart's current total with the event's id as checkout id; the event carries that charge; the cart is gone.
- [ ] `CheckoutBasketHandlerTest`: decline → `PaymentDeclined`, no event, cart still there.
- [ ] `CheckoutBasketHandlerTest`: unpriceable item or empty cart → no charge is attempted.
- [ ] `CheckoutBasketHandlerTransactionalTest`: outbox save fails → cart present, outbox empty, the charge is refunded.

Green and refactor:
- [ ] Handler as in *Design*; drop the method-level `@Transactional`, add the `TransactionTemplate`.
- [ ] Alongside: `CheckoutBasketErrorHandler` mappings and messages; `CheckoutBasketControllerTest` cases for 402, 400, 503.
- [ ] `./mvnw clean verify` green.

### 3. Card data out of the request, the event, the contract and ordering
- [ ] Red: `BasketOutboxPayloadFixturesTest` expects reading `v1.json` to fail with the upcaster's message.
- [ ] Green: `BasketCheckedOut.payment` → `charge`; register version 2 with `BasketCheckedOutV1ToV2` in `BasketOutboxEvents`. Delete basket's `Payment` and `PaymentMother`.
- [ ] Alongside: `CheckoutPayment(paymentMethodId)`, messages, `CheckoutBasketControllerTest` request bodies and the blank-id validation case.
- [ ] Alongside: `ChargeDTO`, `CardSummaryDTO`, `BasketCheckoutIntegrationEvent.charge`, `BasketOutboxDispatcher` and its test.
- [ ] Red: `CreateOrderHandlerTest` — a payment whose amount differs from the order total is rejected and nothing is saved. Green: the check in `Order.placeFromCheckout`.
- [ ] Alongside: ordering's `Payment`, `PaymentDB`, `PaymentDTO`, `DBMapper`, the test `DTOMapper`, `PaymentMother`, `OrderMother`, listener and its test, `MongoOrderRepositoryTest`, `MongoOrderQueryRepositoryTest`.
- [ ] `grep -ri 'cvv\|cardNumber' --include=*.java` over `src/` returns nothing.
- [ ] `./mvnw clean verify` green.

### 4. `StripePaymentGateway` against the WireMock stubs
- [ ] Add `stripe-java` (latest; 34.0.0 on 2026-10-21) to `basket-core`, and WireMock (test scope) — build files, so the user's change.
- [ ] Write the mapping files under `docker/sb-eshop/payment-provider/mappings/`.
- [ ] Red/green: amount-to-minor-units conversion.
- [ ] Red: `StripePaymentGatewayTest` (WireMock JUnit extension on those files): `pm_card_visa` → `Charge` with amount, reference and `visa`/`4242`; `pm_card_mastercard` → `mastercard`/`4444`; `pm_card_chargeDeclined` → `PaymentDeclined` carrying `generic_decline`; `pm_card_authenticationRequired` → `PaymentDeclined`; unknown id → `InvalidPaymentMethod`; WireMock answering `500` or a connection reset → `PaymentProviderUnavailable`; refund succeeds. Also assert, via the request WireMock received, that the idempotency key is the checkout id.
- [ ] Green, refactor.
- [ ] `./mvnw clean verify` green.

### 5. Wiring, compose, Bruno, fixture, docs, live lists
- [ ] `BasketModuleConfig`: `StripeClient` and gateway beans; `application.yaml`: `eshop.basket.payment.*`. Check `ApplicationWiringTest` still starts (no call is made at startup).
- [ ] Compose: `payment-provider` service.
- [ ] Bruno: `Checkout Basket.bru` sends `{"paymentMethodId": "pm_card_visa"}`; add `Checkout Basket (Declined).bru` with `pm_card_chargeDeclined`, expecting `402`. Document in both that a browser would get the id from the provider's card field.
- [ ] After `docker compose down -v`: capture `BasketCheckedOut/v2.json` from a real checkout and add its fixture test.
- [ ] [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md): request-shape section (*They already have reasons to diverge* — now they have diverged); *Payload versioning* gets the first real version bump as its example; a short section on the payment port, why the call sits outside the transaction, and the refund compensation.
- [ ] [SETUP.md](../../../docs/SETUP.md): the curl checkout body, the new compose service, and the decline example.
- [ ] [README.md](../../../README.md): replace *Not production-safe: card data* with how payment works now (token in, charge out, card summary only).
- [ ] [bruno/README.md](../../../bruno/README.md): the two checkout requests and the test payment method ids.
- [ ] [card-data-and-pci-dss.md](../../context/card-data-and-pci-dss.md): section 2 says the project never authorizes a payment; add a dated note that this changed, leaving the research text as it is.
- [ ] [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md): remove Bug 4. Route the *Deferred* entries below.

## Verification
- [ ] `./mvnw clean verify` green.
- [ ] Manual run with the compose stack: checkout with `pm_card_visa` → `202`, order shows `visa`/`4242` and the charged amount equal to its `totalPrice`; checkout with `pm_card_chargeDeclined` → `402` and the basket is still there; stop `payment-provider` and check out → `503`, basket still there.
- [ ] Mongo: the basket outbox document and the order document contain no card number, expiry-with-number or CVV.

## Deferred
- **Saved cards (card on file).** The provider keeps the card; the shop would store a customer reference and payment method id with the card summary. Route to the Backlog.
- **3-D Secure / `requires_action`.** Needs a client step this API has no way to drive; declined for now. Route to the Backlog, with the note that it needs the checkout to become a two-step flow.
- **Authorize-then-capture**, if proposed decision 1 stands. Route to the Backlog only together with order fulfilment; otherwise drop.
- **A refund that fails after a failed transaction** leaves the customer charged with no order, visible only as an ERROR log. Route to Known Issues: it needs a retry, e.g. a pending-refund outbox entry.
- **Webhooks** (asynchronous payment events from the provider). Not needed for synchronous card charges; drop unless an asynchronous payment method is added.
- **stripe-mock as a request-schema check** in the adapter test. Offered and not chosen; drop.
