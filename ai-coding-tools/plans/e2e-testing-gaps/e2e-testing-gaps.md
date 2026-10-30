# E2E Testing Gaps

Draft, 2026-10-29; decisions settled with the user on 2026-10-30. Not yet approved.

## Status
- [ ] Step 1 — One way to send a request: token always applied, setup checked, response under test recorded (Testing Gaps 9, 16, 17)
- [ ] Step 2 — Product lookup across every page of the catalog (Testing Gap 19)
- [ ] Step 3 — The declined-checkout "no order" check (Testing Gap 18)
- [ ] Step 4 — Generically worded response steps (Testing Gap 20)
- [ ] Step 5 — Docs and live lists

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

Anything else this increment surfaces and doesn't fix is listed here and routed at close-out.
