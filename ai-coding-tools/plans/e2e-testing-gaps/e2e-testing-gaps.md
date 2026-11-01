# E2E Testing Gaps

Approved by the user on 2026-10-30. Implementation: guided (the assistant implements each
batch after the user's "ok"), per [feature-workflow.md](../../processes/feature-workflow.md).

## Status
- [x] Step 1 — One way to send a request: token always applied, setup checked, response under test recorded (Testing Gaps 9, 16, 17)
  - **Results, 2026-10-30,** from the host against the application running in IntelliJ:
    - **Whole suite:** green, 8 tests, the same count as before the change.
    - **Gap 9:** with the baseline's temporary "And Alice asks for Alice's order by its id",
      "Another customer can't see the order" now passes. Bob's own response is checked.
    - **Gap 16:** the planned edit (adding 0 of a product) didn't fail, because the API accepts
      quantity 0 and checkout placed an order with a 0.00 total. That's a defect, recorded under
      *Deferred*. Replaced, with the user's agreement, by dropping `CreateTheBasket` from
      `BasketSteps.hasInTheBasket`. Both checkout scenarios then failed in their *Given*, in
      under 2 s, with `POST /basket/items answered 500: …`. Before the change, such a failure
      showed up on a later step, after `eventually`'s 30 s. The `500` instead of `404` is a
      second defect, also under *Deferred*.
    - **Misuse:** a temporary scenario with "Alice's request is accepted" and no *When* failed
      with "Can't check that the request is accepted: Alice sent no request under test. …".
    - **Gap 17:** the Visitor (`401`) and Alice (`200`) scenarios stay green. `seeThatResponse`,
      `UseABearerToken.authorize` and Serenity's `Get`/`Post` no longer appear outside `api/`.
    - **Deviation:** all three tasks a *When* uses (`CheckOut`, `ListTheProducts`,
      `AskForTheOrder`) got the `asTheRequestUnderTest()` flag, including `AskForTheOrder`,
      which only a *When* uses, so every *When* step reads the same way.
  - **Baseline, 2026-10-30, before any change:** with "And Alice asks for Alice's order by its
    id" inserted between Bob's request and "Bob's request is refused as not found", "Another
    customer can't see the order" failed with `Expected status code <404> but was <200>` (the
    step checked Alice's response), and the other scenarios passed. Run from the host against
    the application running in IntelliJ: the `app` container couldn't bind port 8080 while that
    process held it. The temporary line was then removed.
- [x] Step 2 — Product lookup across every page of the catalog (Testing Gap 19)
  - **Results, 2026-11-01,** from the host against the application in Docker (`--profile app`,
    a fresh stack, since the IntelliJ run was gone):
    - **Baseline on the old lookup,** with `ListTheProducts`'s `pageSize` set to `1`: both
      checkout scenarios failed with "No product named 'Galaxy Smartphone' in the catalog", and
      `catalog.feature` listed only `[Aurora Laptop]`. "Another customer can't see the order"
      also failed, with `POST /basket/checkout answered 500`, for an unrelated reason (a fresh
      database, recorded under *Deferred*). Step 1's 2xx check reported it on its own step.
    - **New lookup with `PAGE_SIZE = 1`:** green, 8 tests. The Serenity report shows
      `pageIndex` 0, 1 and 2 requested once, in one scenario: one read per run. `catalog.feature`
      passed too, which was not what the plan's batch description predicted. It goes through
      `ListTheProducts`, which this step leaves at page 0 / size 10.
    - **Restored to `PAGE_SIZE = 10`:** green, 8 tests.
    - **Deviation:** the map is a static field, against end-to-end-tests-with-serenity's "no
      static fields" rule. That rule keeps scenarios from sharing mutable state. This field is
      written once, under `synchronized`, holds the read-only seeded catalog, and is the same
      for every scenario. The lookup calls `Api.get` directly rather than `ListTheProducts`,
      which stays the *When* "lists the products".
- [x] Step 3 — The declined-checkout "no order" check (Testing Gap 18)
  - **Results, 2026-11-01,** against the application in Docker:
    - The scenario, renamed "A declined checkout keeps the basket and places no order", now
      checks out again with `pm_card_visa` after the decline and waits for exactly one order:
      1 Galaxy Smartphone, 550.00. It reuses the existing steps, so no new step code.
      `hasNoOrders` and `PUBLISHING_WINDOW` are gone.
    - **Whole suite:** green, 8 tests, in 5.7 s instead of about 12.5 s.
    - **Failing for the right reason:** with 2 phones and 1100.00 expected, the scenario failed
      after `eventually`'s 30 s, printing the single paid order (quantity 1, 550.00).
    - The feature description ("A declined one changes nothing") still fits and is unchanged.
- [x] Step 4 — Generically worded response steps (Testing Gap 20)
  - **Results, 2026-11-01,** against the application in Docker:
    - "{actor}'s request is accepted" checks for any 2xx. "{actor}'s request is refused as not
      found" became "{actor} is told the order doesn't exist" (`isToldTheOrderDoesNotExist`),
      still checking `404` with `order.not-found`.
    - **Whole suite:** green, 8 tests.
    - **Failing for the right reason:** with "Visitor's request is accepted" in place of the
      unauthenticated refusal, the scenario failed with `Expected status code (a value equal to
      or greater than <200> and a value less than <300>) but … <401> was greater than <300>`.
      Hamcrest's `allOf` makes the mismatch half of the message clumsy, but it names the range
      and the status. **Deviation:** the plan named a request answering `404`. The existing
      Visitor scenario's `401` shows the same thing without a new scenario.
- [x] Step 5 — Docs and live lists
  - `e2e/README.md` has a *Writing a scenario* section. `docs/KNOWN_ISSUES.md` no longer has
    Testing Gaps 9 and 16–20. Backlog 24 and 25 now describe the suite as it is. `README.md`
    needed no change.
  - **Close-out of *Deferred*:** the gap 18 blind spot became Testing Gap 24. The zero-or-less
    quantity became Known Bug 21, the add/remove 500 became Known Bug 22 (widened to
    `CatalogProductNotFound`, found while writing it up), and the fresh-database
    `WriteConflict` became Known Bug 23. Nothing was dropped.

## Context

The end-to-end suite under `e2e/` (added in end-to-end-tests-with-serenity) works, and every
scenario checks what it says. A review on 2026-10-29 found six ways it can pass or fail for the
wrong reason once it grows. They are Testing Gaps 9 and 16–20 in `docs/KNOWN_ISSUES.md`:

| Gap | Problem |
|---|---|
| 9 | Response steps read `SerenityRest.lastResponse()`, the thread's last response, not the named actor's. A question asked in between (e.g. `TheCatalog`) moves it too. |
| 16 | Setup tasks (`CreateTheBasket`, `AddToTheBasket`, `CheckOut` in a *Given*) never check their status, so a failed setup surfaces later on an unrelated step. |
| 17 | Each request opts in to the token by calling `UseABearerToken.authorize`. A task that forgets sends no token, silently. |
| 18 | `OrderSteps.hasNoOrders` sleeps a fixed 10 s (`PUBLISHING_WINDOW`), tied to the outbox's poll interval by a constant in the suite. |
| 19 | `TheCatalog.idOfTheProductNamed` reads only page 0 (size 10) of `GET /products`. |
| 20 | "{actor}'s request is refused as not found" asserts `order.not-found`, and "{actor}'s request is accepted" asserts `200`, so neither is as reusable as its wording says. |

9, 16 and 17 have one cause: every task and question builds its request by hand (`Get`/`Post`,
then `authorize`, then someone reads the last response). They get one fix. 18, 19 and 20 are
small and independent.

Nothing under `SpringBoot-EshopModularMonolith/src` changes in this plan.

**Out of scope:**
- Backlog 24, which reorganises the suite by capability. It moves every file, which would bury
  these fixes in the review. It also depends on gap 9's fix, so it comes after this plan.
  Backlog 25 and 26 are separate too.
- Known Bug 15 (root-owned files in `e2e/target`). It lives in the compose file, not the suite.

## Decisions

### Settled
- **One plan, one branch:** `test/e2e-testing-gaps`, since it closes Testing Gaps.
- **"The request under test" is a named concept.** It is the request a *When* step sends, whose
  answer a *Then* step checks. Only that request skips the status check and has its response
  recorded. Every other request (setup tasks and questions) must answer 2xx and fails at once
  with its status and body when it doesn't. This one flag closes both halves of gap 9: a
  question asked between the *When* and the *Then* no longer moves the response being checked,
  because questions don't record theirs.

- **Gap 18: show that the outbox drained, instead of waiting a fixed time** (settled with the
  user, 2026-10-30). After the decline, the same customer checks out with a valid card, and the
  scenario waits for exactly one order, the paid one. The user's reasoning: the strength of an
  end-to-end test is the cross-module check. Checking only the basket would be too narrow; what
  must hold is that no event left basket's outbox and no order exists in ordering.
  - **How this shows the outbox:** the suite only talks HTTP, and nothing in the API exposes the
    outbox, so the outbox part is inferred, not read. Basket's outbox is Mongo, and
    `MongoOutboxEventStore` hands out due events sorted by `occurredOn`. Any event the decline
    had written is therefore older than the paid checkout's, and becomes an order no later than
    the paid one. So "exactly one order, the paid one" means the decline produced none.
  - **Known blind spot:** a decline event whose first publish *failed* would be retried after
    the outbox backoff, possibly after the paid order arrived. The check would miss it. Recorded
    rather than closed, since closing it needs to see the outbox itself.
  - **Considered and not chosen:**
    - *Dropping the line*, leaning on `CheckoutBasketHandlerTest`'s "no event is recorded":
      too narrow for an end-to-end test, per the user.
    - *Reading basket's outbox collection from the runner*: breaks the black box and ties the
      suite to a storage schema. end-to-end-tests-with-serenity rejected the same thing for
      clean-up.
  - **Cost:** a *When* after a *Then*, which is two behaviours in one scenario. That is accepted
    here, because the second checkout is how the first one's outcome is observed.

- **Gap 20: mixed** (settled with the user, 2026-10-30). "{actor}'s request is accepted"
  becomes generic in fact: it checks for any 2xx, since it reads naturally after any request.
  "{actor}'s request is refused as not found" gets domain wording, e.g. "{actor} is told the
  order doesn't exist", and keeps checking `404` with `order.not-found`, since the error code is
  the part worth checking.
  - Considered and not chosen: domain wording for both (the authentication "accepted" has no
    domain to name), and generic for both (it splits each refusal into two steps for no reader's
    benefit).

## Design

### Sending a request (gaps 9, 16, 17)

The tasks and questions stop using `Get`/`Post` directly. They use a small factory that returns
one interaction:

```java
public final class Api {
    public static Send get(String path, RestQueryFunction... customisations) { ... }
    public static Send post(String path, RestQueryFunction... customisations) { ... }
}

public class Send implements Interaction {
    static final String RESPONSE_UNDER_TEST = "response under test";

    // fields: method, path, customisations, boolean underTest

    public Send asTheRequestUnderTest() { ... }   // no status check; response recorded

    @Override
    public <T extends Actor> void performAs(T actor) {
        // build Get.resource(path) / Post.to(path) here, where the actor is known:
        //   .with(request -> withTheTokenOf(actor, request)).with(customisations)
        actor.attemptsTo(restInteraction);
        var response = SerenityRest.lastResponse();   // safe: read right after this actor's own call
        if (underTest) {
            actor.remember(RESPONSE_UNDER_TEST, response);
        } else if (!isSuccessful(response)) {
            throw new AssertionError(method + " " + path + " answered " + status + ": " + body
                    + ". This request is setup for the scenario, not the request under test, so it must succeed.");
        }
    }
}
```

- **The token is applied in `Send` only.** `UseABearerToken` stays a plain ability: the
  `authorize` helper moves into `Send` as a private method, so there is no public way to attach
  a token outside it. "No token" can then only mean the actor holds none, which is how Visitor
  sends its request (gap 17). Nothing stops a task from using Serenity's `Get` directly; the e2e
  README states the rule (step 5). An ArchUnit guard would be more than a 24-file suite needs.
- **Response steps check the recorded response,** through a consequence that takes the same
  `Consumer<ValidatableResponse>` as `seeThatResponse`, so step bodies barely change:

  ```java
  actor.should(seeThatTheResponseUnderTest("the order is not found",
          response -> response.statusCode(404).body("code", equalTo("order.not-found"))));
  ```

  `ResponseConsequence`'s constructor is package-private, so this is a class of our own
  extending `BaseConsequence<Response>` (public no-arg constructor, checked with `javap` on
  `serenity-screenplay` 5.4.1). It reads `actor.recall(RESPONSE_UNDER_TEST)` from the named
  actor. If nothing is recorded, it fails with a message saying the step checks the response to
  a request under test, and that this actor sent none in an earlier *When* step.
- **Questions read their own response** with `SerenityRest.lastResponse()` straight after their
  own `Send`. That is already correct, since nothing runs in between on that thread. The
  questions also get the 2xx check, so a question answered with `404` fails with the status and
  body instead of a JSON-path error.
- **Tasks used in both a *Given* and a *When*** (`CheckOut`, `ListTheProducts`, `AskForTheOrder`)
  get a variant that marks their request as under test, e.g. `CheckOut.payingWith(card)` for setup
  and `CheckOut.payingWith(card).asTheRequestUnderTest()` for the *When*. With `Tasks.instrumented`
  the flag is one more constructor argument.

Where each request ends up:

| Sent by | Under test? |
|---|---|
| `CatalogSteps.listsTheProducts`, `CheckoutSteps.checksOut`, `OrderSteps.asksForTheOrderOf` (*When*) | yes |
| `BasketSteps.noLongerHasABasket` (a *Then* that sends its own `GET /basket` and expects `404`) | yes, checked straight after |
| `BasketSteps.hasInTheBasket`, `OrderSteps.hasPlacedAnOrder` (*Given*) | no: must succeed. `hasPlacedAnOrder`'s explicit `202` check goes, since the 2xx check covers it |
| `TheBasket`, `TheCatalog`, `TheOrders` (questions) | no: must succeed |

`seeThatResponse` and `SerenityRest.lastResponse()` then no longer appear in step classes.

### Product lookup (gap 19)

`TheCatalog.idOfTheProductNamed` reads the name-to-id map from a holder that loads every page of
`GET /products` once per run. It follows `totalPages` from `PaginatedResults` and requests each
page with the asking actor's token. Scenarios run on 4 threads, so the load is guarded (a
`synchronized` lazy load, or an `AtomicReference` set once) and runs only once. The map covers
the seeded, read-only catalog only. A product that a scenario creates is known by the id its
create request returned, as the e2e README already asks. The "No product named …" message says
so.

`ListTheProducts` as the *When* of `catalog.feature` keeps page 0 / size 10: that scenario checks
the seeded products, not the lookup.

## Steps

The suite tests the application, and nothing tests the suite, so there is no red/green in the
usual sense. As in end-to-end-tests-with-serenity, each step shows its change **failing for the
right reason** once, with a temporary edit that is reverted afterwards, and records the output
here. Every step ends with the whole suite green: `cd e2e && ./mvnw verify` against
`--profile app`.

1. **`Api` / `Send`, the response-under-test consequence, and every task, question and step
   moved onto them** (gaps 9, 16, 17). This is the mechanical bulk of the diff, done alongside
   the existing scenarios. Shown failing for the right reason:
   - *Gap 16:* in a temporary copy of "A paid checkout becomes an order", the *Given* adds 0 of
     a product. It must fail in the *Given* step with `POST /basket/items answered 400: …`, not
     on a later *Then*.
   - *Gap 9:* in "Another customer can't see the order", a temporary line "And Alice asks for
     Alice's order by its id" goes between Bob's request and "Bob's request is refused as not
     found". It must still pass, because Bob's response is checked. Before the change the same
     edit fails, since Alice's `200` is checked. Run it once on `main` first to show that.
   - *Misuse message:* a temporary scenario with a response step and no *When*. It must fail with
     the "sent no request under test" message.
   - *Gap 17:* the existing Visitor (`401`) and Alice (`200`) scenarios staying green is the check.
2. **Lookup across every page** (gap 19). Shown failing for the right reason: with the lookup's
   page size forced to `1`, the current code fails with "No product named 'Aurora Laptop'" (the
   seeded products past the first page), and the new code passes. Then restore the page size.
   *(This replaces the strict red/green suggested on 2026-10-29: the suite has no unit tests of
   its own helpers, and forcing the page size gives the same evidence without adding that
   infrastructure.)*
3. **Gap 18: the outbox drained.** In "A declined checkout keeps the basket", replace "And
   Alice has no orders once the outbox has had time to publish" with a second checkout with a
   valid card, followed by the existing exactly-one-order step. Remove `hasNoOrders` and
   `PUBLISHING_WINDOW`. Exact wording is settled at implementation, e.g. "When Alice then checks
   out paying with "pm_card_visa"" / "Then Alice eventually has exactly one order: 1 "Galaxy
   Smartphone", 550.00 in total, paid with a visa card ending in 4242". The second checkout is a
   request under test, so the *Then* in between ("Alice's payment is declined") must be checked
   before it. Step 1's design gives that, because each *Then* reads the response recorded at
   that point. Shown failing for the right reason: expecting two orders fails, printing the
   single paid one.
4. **Gap 20: mixed wording.** `AuthenticationSteps.isAccepted` checks for any 2xx.
   `OrderSteps.isRefusedAsNotFound` is renamed to its domain wording in the step class and in
   `order_visibility.feature`. Shown failing for the right reason: the "accepted" step placed
   after a request that answers `404` must fail.
5. **Docs and live lists.**
   - `e2e/README.md`: a short *Writing a scenario* section. Send every request through `Api`.
     Only a *When* sends a request under test. Response steps check that request, and setup
     fails on its own step.
   - `docs/KNOWN_ISSUES.md`: remove Testing Gaps 9, 16, 17, 18, 19 and 20.
   - `docs/BACKLOG.md` item 24: remove "Depends on the fix in Known Issue 9", and replace the
     `seeThatResponse` sentence with the response-under-test consequence.
   - `README.md`: check that nothing it says about the e2e suite changed. Expected: no edit.

## Deferred

- The gap 18 blind spot: a decline event whose first publish failed is retried after the
  outbox backoff, which can be later than the paid order. Seeing it needs the outbox itself,
  through a public surface such as Backlog 27's pending-events metric.

- **Found in step 1: the basket accepts a quantity of 0 or less.** `AddItemIntoBasketRequest`
  has no constraint on `quantity`, and neither `ShoppingCart.addItem` nor `Item` checks it.
  Adding 0 of a product answered 2xx, and checking out placed an order with that line at
  quantity 0 and a 0.00 total. A negative quantity would go through the same path, and merging
  it with an existing line lowers that line's quantity. Not reproduced with a negative number
  yet. A defect: route to Known Issues.
- **Found in step 1: adding to a missing basket answers 500, not 404.** `AddItemIntoBasketHandler`
  throws `BasketNotFound`, but the only handlers for it are the `@RestControllerAdvice` classes
  scoped to the `getbasket` and `checkoutbasket` packages. `additemintobasket` has none, and
  neither has `removeitemfrombasket`, which throws the same exception. A defect: route to Known
  Issues.

- **Found in step 2: the first concurrent checkouts on a fresh database can fail with 500.**
  On a stack started on 2026-11-01 (Mongo container created at 14:36:01), a setup checkout at
  14:36:38 answered 500. The app log shows `TransactionSystemException` caused by MongoDB error
  112 `WriteConflict`, "Collection namespace 'eshop-basket-db.outbox' is already in use", labelled
  `TransientTransactionError`. Nothing creates the `outbox` collection up front, so the first
  checkouts create it implicitly inside their transactions. When two run at once, one of them
  conflicts, and nothing retries a transient transaction error. The customer was charged first,
  so this also takes the refund path of Known Bug 14. The end-to-end suite meets it on every
  fresh stack, which is what CI runs, so it can make the suite flaky there. A defect: route to
  Known Issues. Possible remedies: create the collection (and its indexes) at startup, or retry
  on `TransientTransactionError`.

Anything else this increment surfaces and doesn't fix is listed here and routed at close-out.
