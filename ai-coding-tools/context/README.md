# Context

Background facts that decisions in this project rest on: what an external standard,
tool or service actually requires or does, found out once and written down with its
source.

Each file here:
- **is dated and cites its sources**, and says which claims were checked against a
  primary source and which only against secondary ones;
- **holds facts only.** Defects go to [KNOWN_ISSUES.md](../../docs/KNOWN_ISSUES.md),
  missing capabilities and unmade decisions to [BACKLOG.md](../../docs/BACKLOG.md),
  options to [DESIGN_NOTES.md](../../docs/DESIGN_NOTES.md), decisions to a plan;
- **is frozen once written**, like a plan. If the facts change, add a new dated file
  rather than editing the old one.

A fact that explains one specific part of the system belongs next to it instead, for
example in [COVERAGE.md](../../docs/COVERAGE.md). This folder is for background that
several documents depend on.

| File | Background for |
|---|---|
| [card-data-and-pci-dss.md](card-data-and-pci-dss.md) | What PCI DSS allows a merchant to store about a payment card, and why encrypting the data would not have fixed Known Bug 4 (closed by tokenized-card-payment). |
| [stripe-integration.md](stripe-integration.md) | Which Stripe flow checkout uses, its US/Canada limitation, the Stripe features it relies on, why WireMock stands in for Stripe, and the Stripe documentation the code was checked against. |
