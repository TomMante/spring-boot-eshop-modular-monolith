# Tokenized Card Payment

Approved by the user on 2026-10-21. Implemented in [TomMante/SpringBoot-EshopModularMonolith#32](https://github.com/TomMante/SpringBoot-EshopModularMonolith/pull/32).

## Status
- [x] Step 1 — `ShoppingCart` checks out only against a charge that matches its own total. Full suite green.
- [x] Step 2 — `CheckoutBasketHandler` charges through a `PaymentGateway` port, outside the transaction, and refunds when the transaction fails. Full suite green.
- [x] Step 3 — Card data leaves the request, the event, the contract and ordering. Full suite green.
- [x] Step 4 — `StripePaymentGateway` adapter, tested against the WireMock stubs. Full suite green.
- [x] Step 5 — Wiring, compose, Bruno, fixture, docs and live lists. Full suite green.

## Context

Closes [Known Bug 4](../../../docs/KNOWN_ISSUES.md) (*Raw Card Data Is Stored and Transmitted in Clear*).

Today `POST /basket/checkout` accepts `cardName`, `cardNumber`, `expiration` and `cvv`. They travel unchanged through [`CheckoutPayment`](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/0d4eb74c89c1133d4eef2aab2225ebdb840c5936/SpringBoot-EshopModularMonolith/modules/basket/basket-core/src/main/java/io/github/tommante/eshopmodularmonolith/basket/basket/application/features/checkoutbasket/CheckoutPayment.java) → basket's [`Payment`](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/0d4eb74c89c1133d4eef2aab2225ebdb840c5936/SpringBoot-EshopModularMonolith/modules/basket/basket-core/src/main/java/io/github/tommante/eshopmodularmonolith/basket/basket/domain/valueobjects/Payment.java) → [`BasketCheckedOut`](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/0d4eb74c89c1133d4eef2aab2225ebdb840c5936/SpringBoot-EshopModularMonolith/modules/basket/basket-core/src/main/java/io/github/tommante/eshopmodularmonolith/basket/basket/domain/events/BasketCheckedOut.java) (stored as JSON in the basket outbox) → `BasketCheckoutIntegrationEvent` ([`basket.contract.PaymentDTO`](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/0d4eb74c89c1133d4eef2aab2225ebdb840c5936/SpringBoot-EshopModularMonolith/modules/basket/basket-contract/src/main/java/io/github/tommante/eshopmodularmonolith/basket/basket/contract/PaymentDTO.java)) → ordering's [`Payment`](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/0d4eb74c89c1133d4eef2aab2225ebdb840c5936/SpringBoot-EshopModularMonolith/modules/ordering/src/main/java/io/github/tommante/eshopmodularmonolith/ordering/orders/domain/valueobjects/Payment.java) → [`PaymentDB`](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/0d4eb74c89c1133d4eef2aab2225ebdb840c5936/SpringBoot-EshopModularMonolith/modules/ordering/src/main/java/io/github/tommante/eshopmodularmonolith/ordering/orders/infrastructure/persistence/mongo/model/PaymentDB.java) (stored in the order document) → [`orders.contract.PaymentDTO`](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/0d4eb74c89c1133d4eef2aab2225ebdb840c5936/SpringBoot-EshopModularMonolith/modules/ordering/src/main/java/io/github/tommante/eshopmodularmonolith/ordering/orders/contract/PaymentDTO.java). That last one is part of [`OrderDTO`](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/0d4eb74c89c1133d4eef2aab2225ebdb840c5936/SpringBoot-EshopModularMonolith/modules/ordering/src/main/java/io/github/tommante/eshopmodularmonolith/ordering/orders/contract/OrderDTO.java), so `GET /orders` and `GET /order/{id}` **return the CVV to the client**. No step ever charges the card.

Background on what PCI DSS allows is in [card-data-and-pci-dss.md](../../context/card-data-and-pci-dss.md). The Stripe flow, the features used and the documentation it was checked against are in [stripe-integration.md](../../context/stripe-integration.md). In short: the CVV may not be kept after authorization, encrypted or not, and the usual way to stay out of scope is never to receive card data at all, only a provider token.

### Decisions (settled with the user, 2026-10-21)

- **The shop never receives card data.** The client gets a payment method id from the provider (in a browser: the provider's hosted card field; here: Bruno sends a Stripe test id such as `pm_card_visa`). Checkout carries only that id.
- **Payment is an external system behind a port** in basket's domain, like `ProductService`. No `payment` module in this increment.
- **The charge is synchronous, at checkout.** The customer gets a yes or a no from `POST /basket/checkout` itself, before anything is written.
- **The adapter is real Stripe code.** It uses the `stripe-java` SDK. Only the base URL and key change between environments.
- **The provider stand-in is WireMock serving Stripe-shaped stubs**, not stripe-mock. A spike on 2026-10-21 against `stripe/stripe-mock` 0.206.0 showed it validates requests strictly but returns a fixed PaymentIntent with `status: requires_payment_method` for every request, including a declining card. A correct adapter would read every payment as not completed. stripe-mock as an extra schema check in one adapter test was offered and not chosen.

### Further decisions (settled with the user, 2026-10-21)

Proposed during review and accepted as written, except 4, where the user chose to correct the event in place instead of the proposed throwing upcaster.

1. **Charge immediately, not authorize-then-capture.** Nothing in the system ships an order, so nothing would ever capture a held authorization; it would expire after 7 days. So the PaymentIntent uses automatic capture, and the compensation when the transaction fails is a refund. *Alternative:* manual capture with cancel as compensation, and capture routed to the Backlog as "capture on fulfilment".
2. **One configured currency.** Prices in this system have no currency. The adapter sends the currency from configuration (`eur`), and converts the amount to minor units exactly, rejecting an amount with more than two decimals instead of rounding it.
3. **A decline answers `402 Payment Required`** (`basket.checkout.payment-declined`), the provider being unreachable answers `503` (`basket.checkout.payment-unavailable`), and an unknown or unusable payment method id answers `400` (`basket.checkout.payment-method-invalid`). The cart is kept in all three cases.
4. **`BasketCheckedOut` is corrected in place at schema version 1, with no upcaster.** This departs from the policy in [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) (*Payload versioning*: every shape change bumps the version, existing fixtures are never edited), as [checkout-price-on-order](../checkout-price-on-order/checkout-price-on-order.md) did. The user's reasoning: the project isn't public yet, so no rows exist at rest outside local stacks, and a v1 row holds a card and no charge, so no upcaster could turn it into anything truthful. The policy text is left unchanged; this plan is the record of the exception.
   - *Considered and not chosen:* a v2 with an upcaster that throws, so pending v1 rows dead-letter with an explaining message.
   - **Consequence for a local database:** a `BasketCheckedOut` still *pending* in the local basket outbox would fail to read and dead-letter, and keep its card data until deleted. Clear the local stack (`docker compose down -v`) before running the new version.
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
  - The port, `ChargeRequest` and the three payment exceptions stay in the domain, although only the checkout slice uses them (settled with the user, 2026-10-24). `ProductService` is the same kind of port and lives in `domain.port`; moving only this one would give the codebase two placements for ports with no rule for choosing between them.
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
  - `charge`: `PaymentIntent` create with `amount` (minor units), `currency`, `payment_method`, `confirm=true`, `error_on_requires_action=true`, `automatic_payment_methods[enabled]=true` with `allow_redirects=never` (real Stripe refuses to confirm without a `return_url` otherwise), `metadata[checkout_id]`, `expand[]=payment_method`, and the checkout id as `Idempotency-Key`. Return a `Charge` only for `status=succeeded`, carrying the amount Stripe reports; any other status (e.g. `processing`) is `PaymentDeclined` with the status as reason. An amount that differs from the request is not checked here: `ShoppingCart.checkout` rejects it with `ChargeDoesNotMatchTotal` inside the transaction, and the handler refunds it.
  - `error_on_requires_action=true` is Stripe's documented switch for integrations that can't run 3-D Secure: instead of leaving a PaymentIntent in `requires_action`, Stripe fails the confirm with a `card_error`, code `authentication_required`. So a card that needs authentication is an ordinary decline here, and no half-finished PaymentIntent is left behind.
  - Error mapping, from Stripe's [error object](https://docs.stripe.com/api/errors) (`type`, `code`, `decline_code`) and [error codes](https://docs.stripe.com/error-codes). The SDK raises one exception class per `type`:

    | Stripe answer | SDK exception | Ours |
    |---|---|---|
    | `402`, `card_error`: `card_declined` (+ `decline_code`, e.g. `generic_decline`, `insufficient_funds`, `lost_card`), `expired_card`, `incorrect_cvc`, `authentication_required`, `processing_error` | `CardException` | `PaymentDeclined(code, declineCode)` |
    | `400`/`404`, `invalid_request_error`, `param` = `payment_method` (e.g. `resource_missing`, `payment_method_unexpected_state`) | `InvalidRequestException` | `InvalidPaymentMethod` |
    | any other `invalid_request_error` (e.g. `amount_too_small`, `parameter_unknown`) | `InvalidRequestException` | not translated: our request is wrong, a `500` with an ERROR log |
    | `429` `rate_limit`, `409` `idempotency_key_in_use`/`lock_timeout`, `5xx` `api_error`, no connection | `RateLimitException`, `IdempotencyException`, `ApiException`, `ApiConnectionException` | `PaymentProviderUnavailable` |
    | `401` (wrong key, `api_key_expired`) | `AuthenticationException` | not translated: configuration error |

  - `processing_error` is a `card_error` but means "try again later or use another card"; it stays a decline because Stripe classifies it as one, and the customer can retry with the same request.
  - Only `code` and `decline_code` cross into the domain. Stripe's `message` for card errors is meant to be shown to users, but it is the provider's text in the provider's language; ours comes from `basket-messages.properties`.
  - `refund`: `Refund` create for the PaymentIntent, idempotency key `refund-<charge reference>` (`refund(Charge)` has no checkout id). The idempotency key only deduplicates retries; a refund is traced through the PaymentIntent it points at, whose `metadata[checkout_id]` names the checkout.
  - Amount conversion in a small package-private function: `amount.setScale(2)` without rounding (`ArithmeticException` → `IllegalArgumentException` naming the amount), then `movePointRight(2).longValueExact()`.
- `BasketModuleConfig` builds the `StripeClient` from `eshop.basket.payment.stripe.{api-base, api-key}` with connect/read timeouts and `maxNetworkRetries(2)` (safe because every call carries an idempotency key), and the gateway from it plus `eshop.basket.payment.currency`.
- Nothing logs a payment method id, charge request or provider response body.

**Payload versioning.**
- `BasketCheckedOut` stays at schema version 1: `payment` becomes `charge` (`reference`, `amount`, `card`). No upcaster (decision 4).
- `BasketCheckedOut/v1.json` is edited by hand in step 3 so the suite stays green, and recaptured from the running application in step 5.

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
  - declines, `402`, `error.type: card_error`, using Stripe's own [test payment methods](https://docs.stripe.com/testing#declined-payments) and the codes they produce:
    - `pm_card_visa_chargeDeclined` → `code: card_declined`, `decline_code: generic_decline`;
    - `pm_card_visa_chargeDeclinedInsufficientFunds` → `code: card_declined`, `decline_code: insufficient_funds`;
    - `pm_card_chargeDeclinedExpiredCard` → `code: expired_card`;
    - `pm_card_chargeDeclinedIncorrectCvc` → `code: incorrect_cvc`;
    - `pm_card_authenticationRequired` → `code: authentication_required` (the answer Stripe gives with `error_on_requires_action=true`);
  - any other `payment_method` → `400`, `invalid_request_error`, `code: resource_missing`, `param: payment_method`;
  - the stubs match on the form body, so a request missing `error_on_requires_action=true` or the `Idempotency-Key` header gets no match (WireMock's `404`), and the adapter test fails instead of passing against a response real Stripe wouldn't give;
  - `POST /v1/refunds` → `200`, `status: succeeded`.
- Compose service `payment-provider`, image `wiremock/wiremock` pinned to a 3.x tag (3.13.2 was the newest found on 2026-10-21; check before pinning), mappings mounted read-only (no templating flag: each stub that uses placeholders declares `"transformers": ["response-template"]` itself, so the files behave the same in compose and in the JUnit extension), port `12111` on the host.
- `application.yaml`: `api-base: http://localhost:12111`, `api-key: sk_test_wiremock`, `currency: eur`. The key is a placeholder WireMock ignores, overridable by environment variable; a real test key from a Stripe account is the only other change needed to run against Stripe test mode.

### Testing seams and rigor per step

- **Step 1 — strict red → green → refactor.** Seam: `ShoppingCart.checkout` / `totalAt` (`ShoppingCartTest`). The charge-matches-total rule is an invariant that must hold whoever calls checkout, which is the narrowing criterion in [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md).
- **Step 2 — strict red → green → refactor at the handler seam.** Seam: `CheckoutBasketHandler`, with a new `FakePaymentGateway` at the port (scriptable to decline, to be unavailable, and recording charges and refunds as state, not as call verification). The outcomes are: a decline leaves the cart and writes no event; a success writes the event with the charge; a failed transaction leaves the cart and the charge refunded. The rollback case needs a real transaction, so it lives in `CheckoutBasketHandlerTransactionalTest` (outbox save fails → cart present, outbox empty, fake shows the charge refunded). Controller status mapping: tests alongside in `CheckoutBasketControllerTest`, since it mirrors the existing error-handler pattern.
- **Step 3 — tests alongside, except `Order`'s amount check.** Mapping across an established path (dispatcher → bus → listener → order) teaches nothing by failing first. `Order`'s new amount check gets red first in `CreateOrderHandlerTest`, matching how its other guards are tested.
- **Step 4 — strict red → green → refactor for the adapter.** Seam: `StripePaymentGateway`, against WireMock loaded with **the same mapping files compose uses**, so the test proves the stubs the demo runs on. The amount conversion gets its own small red/green first, as a deterministic function with edge cases (scale 0, scale 2, scale 3 rejected, large amounts).
- **Step 5 — no new tests;** full suite plus the manual run.

## Todo List

### 1. `ShoppingCart` checks out only against a matching charge
Red first, with only what the tests need to compile:
- [x] Add `Charge`, `CardSummary` (basket `domain.valueobjects`) and `ChargeDoesNotMatchTotal` (`domain.exceptions`).
- [x] Add `totalAt(Map)` returning `BigDecimal.ZERO`, and the new `checkout(UUID checkoutId, Map, Instant, Address, Payment, Charge, Address)` signature delegating to today's logic. Until step 2 adds the port, `CheckoutBasketHandler` passes a placeholder `Charge` built from `cart.totalAt(prices)`, so the existing handler tests stay green.
- [x] **Changed during implementation (2026-10-22):** `Payment` stays on `checkout` and on `BasketCheckedOut` next to the new `Charge`, instead of being set to `null` as first written. The handler tests and `CheckoutBasketHandlerTransactionalTest` assert the event's payment, so `null` would have turned them red for a reason unrelated to this step. Step 3 removes `Payment` everywhere, as it already planned to.
- [x] `BasketCheckedOut` lost its convenience constructor (random `eventId`); every caller now passes the id. The fixture test expects `charge` = `null`, since `v1.json` has none yet.

Red:
- [x] `ShoppingCartTest`: `totalAt` for the two-item, two-price cart equals `Σ(price × quantity)` and records no event; an item with no current price throws `UnpriceableCartItem`; an empty cart throws `EmptyCartCheckout`, so the handler never charges an empty cart.
- [x] `ShoppingCartTest`: checkout with a charge one cent short throws `ChargeDoesNotMatchTotal` and records no event.
- [x] `ShoppingCartTest`: checkout uses the given `checkoutId` as the event id, and the event carries the charge.

Green and refactor:
- [x] Implement; `totalAt` and `checkout` share the private `checkedOutItems` and `computeTotal`, so the two can't disagree.
- [x] Added after green: a charge equal to the total at a different scale (`159.0` vs `159.00`) is accepted, guarding the `compareTo`.
- [x] `./mvnw clean verify` green.

### 2. Handler charges outside the transaction
Scaffolding (compiles, behaviour unchanged):
- [x] `PaymentGateway` port, `ChargeRequest`, `PaymentDeclined`, `InvalidPaymentMethod`, `PaymentProviderUnavailable`.
- [x] `FakePaymentGateway` under `src/test/…/infrastructure/adapters/fake/`.
- [x] `CheckoutBasketCommand.paymentMethodId`; `CheckoutPayment` keeps its card fields until step 3, mapped to a placeholder id, so the controller still compiles.

**Changed during implementation (settled with the user, 2026-10-23):**
- [x] `CheckoutBasketCommand` keeps `Payment` next to the new `paymentMethodId` until step 3, for the same reason as in step 1: the event still carries `payment`. `CheckoutPayment.paymentMethodId()` returns a fixed `"pm_card_visa"` until step 3 makes it a record component of the same name, so the controller line doesn't change again.
- [x] A temporary `PlaceholderPaymentGateway` (`infrastructure.adapters.payment`) always charges and never refunds, so the application context and `ApplicationWiringTest` start before the Stripe adapter exists. Step 5 deletes it.
- [x] The handler receives a `TransactionOperations` (`basketTransactionOperations`, a `TransactionTemplate` on `basketTransactionManager`, declared in `BasketModuleConfig`) instead of building a `TransactionTemplate` itself. Unit tests pass `TransactionOperations.withoutTransaction()`.
- [x] Error texts come from the exception messages, as `CheckoutBasketErrorHandler` already does for `UnpriceableCartItem`, not from `basket-messages.properties`. `ApiErrors` (in `shared`) gained `paymentRequired` (402) and `serviceUnavailable` (503).
- [x] `FakePaymentGateway` has a `reset()`, called from `CheckoutBasketHandlerTransactionalTest`'s `@BeforeEach`: the Spring test context is cached, so the fake bean is shared across test methods.

Red:
- [x] `CheckoutBasketHandlerTest`: success → one charge for the cart's current total under the event's id (`chargeTakenFor(event.eventId())`); the event carries that charge; the cart is gone.
- [x] `CheckoutBasketHandlerTest`: decline → `PaymentDeclined`, no event, cart still there.
- [x] `CheckoutBasketHandlerTest`: unpriceable item or empty cart → no charge is attempted. Passes before green by construction; guards the order "price, then charge".
- [x] `CheckoutBasketHandlerTransactionalTest`: outbox save fails → cart present, outbox empty, the charge is refunded.
- [x] `CheckoutBasketHandlerTransactionalTest`: outbox save fails and the refund fails too → the original failure reaches the caller, cart present. Passes before green by construction.

Green and refactor:
- [x] Handler as in *Design*: charge before the try, `executeWithoutResult` around checkout and delete, `refundAfterFailedCheckout` (refund, `addSuppressed` and an ERROR log if the refund fails) in the catch. Method-level `@Transactional` dropped.
- [x] Alongside: `CheckoutBasketErrorHandler` mappings; `CheckoutBasketControllerTest` cases for 402, 400, 503 (the 503 case also checks the provider's cause doesn't leak into the response).
- [x] `./mvnw clean verify` green.

### 3. Card data out of the request, the event, the contract and ordering
- [x] `BasketCheckedOut.payment` → `charge`. Delete basket's `Payment` and `PaymentMother`.
- [x] `BasketOutboxPayloadFixturesTest`: expect a `Charge`. Edit `v1.json` by hand for now (`payment` → `charge` with a reference, amount and card summary) so the suite stays green; it is recaptured in step 5.
- [x] Alongside: `CheckoutPayment(paymentMethodId)`, messages, `CheckoutBasketControllerTest` request bodies and the blank-id validation case.
- [x] Alongside: `ChargeDTO`, `CardSummaryDTO`, `BasketCheckoutIntegrationEvent.charge`, `BasketOutboxDispatcher` and its test.
- [x] Red: `CreateOrderHandlerTest` — a payment whose amount differs from the order total is rejected and nothing is saved. Green: the check in `Order.placeFromCheckout`.
- [x] Alongside: ordering's `Payment`, `PaymentDB`, `PaymentDTO`, `DBMapper`, the test `DTOMapper`, `PaymentMother`, `OrderMother`, listener and its test, `MongoOrderRepositoryTest`, `MongoOrderQueryRepositoryTest`.
- [x] `PlaceholderPaymentGateway` returns a placeholder `CardSummary` instead of `null`: from this step on the dispatcher and ordering map the card, and a `null` would make every checkout in the running application fail to place its order. Added at step 3 (2026-10-25).
- [x] Test data: ordering's `PaymentMother` gains `paymentFor(List<Item>)`, so tests that place an order pay exactly the items' total. `OrderMother` builds through `reconstitute`, which doesn't check, so it needs no change. Added at step 3 (2026-10-25).
- [x] `grep -ri 'cvv\|cardNumber' --include=*.java` over `src/` returns nothing.
- [x] `./mvnw clean verify` green.

### 4. `StripePaymentGateway` against the WireMock stubs
- [x] Add `stripe-java` (latest; 34.0.0 on 2026-10-21) to `basket-core`, and `wiremock-standalone` (test scope; it shades its own Jetty) — build files, so the user's change. Versions as properties plus `<dependencyManagement>` entries in the root pom, like Lombok, springdoc and ArchUnit.
- [x] **Corrections accepted by the user (2026-10-26):** `automatic_payment_methods` with `allow_redirects=never`; no amount check in the adapter (the domain's `ChargeDoesNotMatchTotal` plus the handler's refund cover it); refund idempotency key `refund-<charge reference>`; templating opted into per stub instead of globally; versions managed in the root pom. All five are reflected in *Design* above.
- [x] Write the mapping files under `docker/sb-eshop/payment-provider/mappings/` (nine stubs: two successes, five declines, an unknown-method fallback, a refund).
- [x] Red/green: amount-to-minor-units conversion.
- [x] Red: `StripePaymentGatewayTest` (WireMock JUnit extension on those files): `pm_card_visa` → `Charge` with amount, reference and `visa`/`4242`; `pm_card_mastercard` → `mastercard`/`4444`; `pm_card_visa_chargeDeclined` → `PaymentDeclined` carrying `card_declined`/`generic_decline`; `pm_card_visa_chargeDeclinedInsufficientFunds` → `insufficient_funds`; `pm_card_chargeDeclinedExpiredCard` → `expired_card` with no decline code; `pm_card_authenticationRequired` → `authentication_required`; unknown id → `InvalidPaymentMethod`; WireMock answering `500` or a connection reset → `PaymentProviderUnavailable`; refund succeeds. Also assert, via the request WireMock received, that the idempotency key is the checkout id.
- [x] Green, refactor.
- [x] `./mvnw clean verify` green.

### 5. Wiring, compose, Bruno, fixture, docs, live lists
- [x] Delete `PlaceholderPaymentGateway` (added in step 2).
- [x] `BasketModuleConfig`: `StripeClient` and gateway beans; `application.yaml`: `eshop.basket.payment.*`. Check `ApplicationWiringTest` still starts (no call is made at startup).
- [x] Compose: `payment-provider` service (`wiremock/wiremock:3.13.2`, the same version as `wiremock.version`; mappings mounted read-only; host port `12111`).
- [x] Bruno: `Checkout Basket.bru` sends `{"paymentMethodId": "pm_card_visa"}`; add `Checkout Basket (Declined).bru` with `pm_card_visa_chargeDeclined`, expecting `402`. Document in both that a browser would get the id from the provider's card field.
- [x] ~~After `docker compose down -v`: recapture `BasketCheckedOut/v1.json` from a real checkout~~ **Changed (2026-10-26, the user's call):** the hand-edited fixture from step 3 is kept instead of a recapture. Checked instead: every key in `v1.json`, at every level, matches the components of `BasketCheckedOut`, `CheckedOutItem`, `Address`, `Charge` and `CardSummary`.
- [x] [ARCHITECTURE.md](../../../docs/ARCHITECTURE.md): request-shape section (*They already have reasons to diverge* — now they have diverged); *Payload versioning* unchanged (decision 4 is an exception recorded here, not a policy change); a short section on the payment port, why the call sits outside the transaction, and the refund compensation.
- [x] [SETUP.md](../../../docs/SETUP.md): the curl checkout body, the new compose service, and the decline example.
- [x] [README.md](../../../README.md): replace *Not production-safe: card data* with how payment works now (token in, charge out, card summary only).
- [x] State that the integration is Stripe's US/Canada server-side flow on purpose, and that a European production shop would use the Payment Element with 3-D Secure and webhooks: in the README section above and in ARCHITECTURE.md's payment section. Both link [stripe-integration.md](../../context/stripe-integration.md), which holds the Stripe documentation links the code was checked against (added 2026-10-26, at the user's request).
- [x] [bruno/README.md](../../../bruno/README.md): the two checkout requests and the test payment method ids.
- [x] [card-data-and-pci-dss.md](../../context/card-data-and-pci-dss.md): section 2 says the project never authorizes a payment; add a dated note that this changed, leaving the research text as it is.
- [x] [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md): remove Bug 4. Route the *Deferred* entries below: saved cards → Backlog 21; 3-D Secure → Backlog 20; a failed refund → Known Issue 14; authorize-then-capture dropped (decision 1 stood, and nothing fulfils orders); webhooks dropped (no asynchronous payment method); stripe-mock as a schema check dropped (not chosen).

## Verification
- [x] `./mvnw clean verify` green.
- [x] Manual run with the compose stack (done by the user, 2026-10-26): checkout with `pm_card_visa` → `202`, order shows `visa`/`4242` and the charged amount equal to its `totalPrice`; checkout with `pm_card_visa_chargeDeclined` → `402` and the basket is still there; stop `payment-provider` and check out → `503`, basket still there.
- [x] Mongo: the basket outbox document and the order document contain no card number, expiry-with-number or CVV.

## Deferred
- **Saved cards (card on file).** The provider keeps the card; the shop would store a customer reference and payment method id with the card summary. Route to the Backlog.
- **3-D Secure.** Needs a client step this API has no way to drive; cards that require it are declined with `authentication_required` for now (`error_on_requires_action=true`). In a real European shop most cards would need it, so this is the biggest gap between the showcase and production. Route to the Backlog, with the note that it needs the checkout to become a two-step flow.
- **Authorize-then-capture**, if proposed decision 1 stands. Route to the Backlog only together with order fulfilment; otherwise drop.
- **A refund that fails after a failed transaction** leaves the customer charged with no order, visible only as an ERROR log. Route to Known Issues: it needs a retry, e.g. a pending-refund outbox entry.
- **Webhooks** (asynchronous payment events from the provider). Not needed for synchronous card charges; drop unless an asynchronous payment method is added.
- **stripe-mock as a request-schema check** in the adapter test. Offered and not chosen; drop.
