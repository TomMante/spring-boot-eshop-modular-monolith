# Stripe Integration

Researched between 2026-10-21 and 2026-10-26, for
[tokenized-card-payment](../plans/tokenized-card-payment/tokenized-card-payment.md), the
increment that closes Known Bug 4 (raw card data stored and transmitted in clear).
Background on why card data must not reach the server is in
[card-data-and-pci-dss.md](card-data-and-pci-dss.md).

**How far this was checked:** every page in *Sources* marked "read" was fetched and read on
the date given. The SDK details come from stripe-java's own source code. The behaviour of
`stripe-mock` was measured by running it, not taken from its README alone. Nothing here was
run against real Stripe: the project has no Stripe account, and the adapter is tested
against WireMock stubs written from the pages below.

## 1. The flow this project implements

Stripe documents it as
[card payments without bank authentication](https://docs.stripe.com/payments/without-card-authentication):

1. The client collects the card in a Stripe-hosted field (Elements) and gets a
   **PaymentMethod** id (`pm_…`) from Stripe. The card never reaches the shop's server.
2. The server creates **and confirms** a **PaymentIntent** in one call
   (`confirm=true`), with `error_on_requires_action=true`.
3. Stripe answers synchronously: `status: succeeded`, or an error. A card that needs bank
   authentication (3-D Secure) fails with `code: authentication_required`,
   `decline_code: authentication_not_handled`.

The project follows this guide's server step. Bruno plays the client in step 1, sending
Stripe's test ids (`pm_card_visa`, …) instead of creating a PaymentMethod in a browser.

**Additions beyond the guide's example**, each from the
[PaymentIntent API reference](https://docs.stripe.com/api/payment_intents/create):
- `automatic_payment_methods[enabled]=true` with `allow_redirects=never`. With `never`,
  "Payment methods that require redirect will be filtered" and a `return_url` is not
  required to confirm. The guide's example does not send it.
- `metadata[checkout_id]`, so a PaymentIntent, and through it its refunds, can be found from
  the checkout id.
- `expand[]=payment_method`, so the card's brand, last four digits and expiry come back in
  the same response.
- An idempotency key per request (the checkout id for the charge,
  `refund-<PaymentIntent id>` for the refund), set through stripe-java's `RequestOptions`.

## 2. The flow is limited to the US and Canada

The guide states that it supports businesses accepting cards from the US and Canada only, and
that it does not scale once customers are international. In regions such as Europe and India,
banks often require two-factor authentication, and this flow declines every such payment
instead of completing it.

This project charges in `eur`, so in a real European shop many legitimate cards would be
declined. It is a deliberate scope choice for a showcase: the parts that carry over
unchanged to a production integration are the ones this increment is about (no card data on
the server, a provider token, an idempotent server-side charge, decline handling, refund as
compensation).

**What a European production shop uses instead:** Stripe's
[global integration](https://docs.stripe.com/payments/accept-a-payment), where the
**Payment Element** confirms in the browser and handles 3-D Secure, and the server learns the
outcome through **webhooks**.

## 3. Features used, and not used

| Used | For |
|---|---|
| PaymentMethod | the card token the client sends |
| PaymentIntent, created and confirmed server-side | the charge |
| `error_on_requires_action` | 3-D Secure becomes a decline instead of a pending state |
| `automatic_payment_methods` with `allow_redirects=never` | only payment methods that complete without a redirect |
| Metadata | tracing a PaymentIntent back to its checkout |
| Expand | the card summary in the charge response |
| Idempotency keys | a retried request never charges or refunds twice |
| Refunds | compensation when the basket transaction fails after the charge |
| Test payment method ids and the error model | the WireMock stubs and the error mapping |

**Not used:** Customers (needed for saved cards, subscriptions, charging later), Stripe
Checkout and the Payment Element (Stripe's recommended default, see section 2), webhooks (not
needed while the result is synchronous), manual capture (nothing in the system ships an
order).

## 4. Why WireMock and not stripe-mock

[stripe-mock](https://github.com/stripe/stripe-mock) is Stripe's official mock server. It
validates requests against Stripe's OpenAPI specification, but answers from fixed sample
objects, and its README says it is meant for basic sanity checks, not realistic behaviour.
Measured on 2026-10-21 with version 0.206.0: every PaymentIntent, including one confirmed
with a declining test card, came back `200` with `status: requires_payment_method`. A correct
adapter would read every payment as not completed, so it can show neither a successful charge
nor a decline. The project therefore serves Stripe-shaped stubs from WireMock, written from
the error and testing pages below.

## Sources

Read on 2026-10-26 unless noted:
- Stripe, [Card payments without bank authentication](https://docs.stripe.com/payments/without-card-authentication). The flow, its server request, the `authentication_required` / `authentication_not_handled` error, and the US/Canada limitation.
- Stripe, [Create a PaymentIntent (API reference)](https://docs.stripe.com/api/payment_intents/create), including [`automatic_payment_methods`](https://docs.stripe.com/api/payment_intents/create?query=automatic_payment_methods). Parameters `confirm`, `error_on_requires_action`, `metadata`, `allow_redirects`.
- Stripe, [Errors (API reference)](https://docs.stripe.com/api/errors). Error types and HTTP statuses. Read 2026-10-21.
- Stripe, [Error codes](https://docs.stripe.com/error-codes). `card_declined`, `expired_card`, `incorrect_cvc`, `authentication_required`, `resource_missing`. Read 2026-10-21.
- Stripe, [Testing](https://docs.stripe.com/testing). Test payment method ids and the codes they produce. Read 2026-10-21.
- Stripe, [Accept a payment (global integration)](https://docs.stripe.com/payments/accept-a-payment). Linked from the guide above as the international alternative; not read in full.
- stripe-java, [README](https://github.com/stripe/stripe-java/blob/master/README.md) and source: [`StripeClient`](https://github.com/stripe/stripe-java/blob/master/src/main/java/com/stripe/StripeClient.java) (`setApiBase`, `v1()`), [`CardException`](https://github.com/stripe/stripe-java/blob/master/src/main/java/com/stripe/exception/CardException.java), [`RateLimitException`](https://github.com/stripe/stripe-java/blob/master/src/main/java/com/stripe/exception/RateLimitException.java). Version 34.0.0. Read 2026-10-21 to 2026-10-26.
- Stripe, [stripe-mock README](https://github.com/stripe/stripe-mock). Read 2026-10-21; behaviour measured the same day.
