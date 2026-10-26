# Card Data and PCI DSS

Researched on 2026-10-21, against PCI DSS v4.0.1. Background for
Known Bug 4 (raw card data stored and transmitted in clear), closed by [tokenized-card-payment](../plans/tokenized-card-payment/tokenized-card-payment.md).

**How far this was checked:** the CVV rule (section 2) was read on the PCI Security
Standards Council's own FAQ. The other requirement numbers and their wording come from
the Council's FAQ listings and from secondary compliance guides, not from the standard's
text. Read the standard before relying on an exact requirement.

## 1. Two kinds of card data

PCI DSS splits the data on a payment card into two groups, with different rules:

| Group | Contains | May be stored? |
|---|---|---|
| **Account data: cardholder data** | PAN (the card number), cardholder name, expiry date, service code | Yes, if protected (section 3) |
| **Account data: sensitive authentication data (SAD)** | Card verification code (CVV, CVC, CID), full track data, PIN and PIN block | Not after authorization (section 2) |

This project carries a PAN, a cardholder name, an expiry date and a CVV.

## 2. CVV may not be kept after authorization, encrypted or not

- **Requirement 3.3.1.2:** the card verification code is not retained after
  authorization. The Council's FAQ states that this holds "even if encrypted".
- **Card-on-file and recurring payments get no exception.** The code may not be kept
  for the next purchase, only for the one it was collected for.
- **Before authorization (Requirement 3.3.2):** SAD stored electronically before
  authorization completes must be encrypted with strong cryptography. This was a best
  practice until 31 March 2025 and is now required. Whether a merchant may store it
  before authorization at all is up to the card brands and acquirers, not PCI DSS.

What this means for the options:
- **A vault, or encryption at rest, does not make storing a CVV compliant.** It only
  changes how the data is stored, and the rule is about whether it is stored at all
  after authorization.
- The CVV is valid only from the moment it is collected until it is sent for
  authorization. Once that happens, every copy has to be gone.
- **This project never authorizes a payment.** No step charges the card, so no point
  exists after which the CVV should be gone. Today it stays indefinitely in two places:
  the basket outbox document and `PaymentDB` in the order database.

> **Update, 2026-10-26:** no longer true. Since
> [tokenized-card-payment](../plans/tokenized-card-payment/tokenized-card-payment.md), checkout
> receives a Stripe payment method token instead of card details and charges it through the
> provider; no card number, expiry or CVV reaches the application, its outbox or its databases.
> The flow is described in [stripe-integration.md](stripe-integration.md). The research text above
> is left as written.

## 3. The PAN may be stored, but not as it is

- **Requirement 3.5.1:** a stored PAN must be unreadable. The accepted ways are:
  - a keyed cryptographic hash of the whole PAN (v4.0.1 made "keyed" explicit);
  - truncation, removing part of the number for good;
  - index tokens, a value that stands in for the PAN, with the real PAN held elsewhere;
  - strong cryptography with managed keys (Requirements 3.6 and 3.7).
- **Requirement 3.4.1:** a displayed PAN is masked, showing at most the BIN (the
  leading digits that identify the issuer) and the last four digits.
- **Masking and truncation are different.** Masking hides digits on a screen, a
  receipt or a report, while the full number may still be stored. Truncation removes
  the digits from the stored value. How many digits truncation may keep depends on
  the card brand.
- **"Brand plus last four digits"**, as Known Bug 4 puts it, is truncation that keeps
  less than the maximum allowed. That is enough to show the customer which card paid.

## 4. How a shop keeps card data off its servers

The usual approach is to never receive the card data at all. The card fields on the
checkout page are served by a payment provider, embedded as an iframe or reached by a
redirect. The card details go from the browser straight to the provider, and the shop
receives only a **token**: a reference it can pass back to the provider to charge the
card, which is worthless to anyone else.

- **SAQ A** is the shortest PCI self-assessment. A merchant qualifies when every
  part of the payment page that handles card data comes from a PCI DSS-validated
  provider, and the merchant does not store, process or transmit cardholder data.
- **Since v4,** a merchant that embeds the provider's form must also either protect the
  page against malicious scripts (Requirements 6.4.3 and 11.6.1) or get the provider's
  written confirmation that its embedded form does so. A full redirect is not
  affected.
- **Collecting card fields in the merchant's own page** and posting them to the
  provider with the merchant's own JavaScript qualifies for SAQ A-EP, which is longer.
  Receiving the card data on the merchant's own server, as this project's checkout
  endpoint does, puts that server in scope for the full assessment.

With a provider token in place:
- no CVV ever reaches the backend;
- the backend holds no PAN, only the token and, for display, brand, last four digits
  and expiry.

## Sources

- PCI SSC FAQ: [Are merchants allowed to request card verification codes from cardholders?](https://www.pcisecuritystandards.org/faqs/are-merchants-allowed-to-request-card-verification-codes-values-from-cardholders/). Primary; the source of the "even if encrypted" wording.
- PCI SSC FAQs on card-on-file retention and call recordings: [1280](https://www.pcisecuritystandards.org/faqs/1280/), [1319](https://www.pcisecuritystandards.org/faqs/1319/). Found in search results; not read in full.
- PCI SSC FAQ on masking and truncation with 8-digit BINs: [1492](https://www.pcisecuritystandards.org/faqs/1492/). Found in search results; not read in full.
- PCI SSC FAQ on SAQ A and iframes: [1438](https://www.pcisecuritystandards.org/faqs/1438/), and the Council's blog post [FAQ clarifies new SAQ A eligibility criteria](https://blog.pcisecuritystandards.org/faq-clarifies-new-saq-a-eligibility-criteria-for-e-commerce-merchants). Found in search results; not read in full.
- Secondary, for the requirement wording of 3.3.2, 3.4.1 and 3.5.1:
  - [SecurityMetrics, SAQ-D v4.0 self-assessment](https://www.securitymetrics.com/blog/performing-a-saq-d-version-40-merchant-self-assessment);
  - [Basis Theory, PCI DSS Requirement 3](https://blog.basistheory.com/pci-dss-requirement-3);
  - [VikingCloud, what it means to mask a PAN](https://www.vikingcloud.com/blog/what-does-it-mean-to-mask-pan).
- Secondary, for SAQ A and SAQ A-EP:
  - [Hyperproof, new SAQ A eligibility criteria](https://hyperproof.io/?p=33329);
  - [PCI Proxy, iframes and SAQ A](https://pci-proxy.com/blog-posts/iframes-an-saq-a-eligible-way-to-collect-credit-card-details).
