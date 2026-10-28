# End-to-End Tests With Serenity

Approved by the user on 2026-10-28. Implementation: guided (the assistant implements each
batch after the user's "ok"), per [feature-workflow.md](../../processes/feature-workflow.md).

## Status
- [ ] Step 1 — Spike: the project skeleton runs two scenarios in parallel on JDK 25 and writes a report.
- [ ] Step 2 — A fresh customer per scenario; *Requests need a token*.
- [ ] Step 3 — *Checkout places an order*: paid and declined.
- [ ] Step 4 — *Orders belong to their customer*.
- [ ] Step 5 — The `e2e` compose profile runs the suite in a container.
- [ ] Step 6 — Docs and the live lists.

## Context

Picks up Backlog 22 (*Automated End-to-End Tests*), except its GitHub Actions part.

The suite stops today at an assembled Spring context: `ApplicationWiringTest` boots every module
against Testcontainers, with WireMock playing Stripe and a mocked JWT decoder. Nothing runs the
real image against real Keycloak tokens, the compose stack and the Stripe stand-in, and nothing
follows a checkout through the outbox to the order the way a client sees it.

The previous increment, `compose-runs-the-application`, made that possible: compose builds and
runs the application under an `app` profile, and Keycloak issues every token under the fixed
issuer `http://localhost:9090/realms/eshop`, whether it was requested from the host or from inside
the compose network. Both were verified by hand. This plan automates that check.

### Decisions (settled with the user, 2026-10-27)

- **A separate project in a top-level `e2e/` folder**, next to `bruno/`, with its own `pom.xml`
  and Maven wrapper. It is not a module of the application's reactor and depends on none of its
  code: it knows two URLs, the application's and Keycloak's. Bruno stays, for a person exploring
  the API by hand. This suite is the automated regression check. The two send similar requests,
  and that overlap is accepted.
- **Serenity BDD with Cucumber and Screenplay.** Scenarios are `.feature` files. Steps are
  written as actors performing tasks and asking questions, over Serenity's REST integration
  (RestAssured). Serenity's report then reads as living documentation of the features, with
  each step's HTTP exchanges attached.
- **The first scenarios:**

  | Feature | Scenarios |
  |---|---|
  | Catalog | The seeded products are listed |
  | Requests need a token | No token → `401`; a token from another realm → `401`; a token from the `eshop` realm → `200` |
  | Checkout places an order | A paid checkout (`pm_card_visa`) becomes an order with the right line, total and card summary, and the basket is gone. A declined checkout (`pm_card_visa_chargeDeclined`) answers `402`, keeps the basket and creates no order |
  | Orders belong to their customer | Another customer doesn't see the order in their list, and gets `404` on its id |

  Not in this increment: catalog writes and deleting orders. They're CRUD that crosses no module
  boundary, and the integration tests cover them.
- **Every scenario creates its own customers** in Keycloak, through the admin API. Baskets are
  keyed by username and orders by customer, so a fresh user starts with no basket and no orders.
  Assertions can then be exact ("has exactly one order"), scenarios never share state, and two
  whole runs against one stack don't interfere either.
  - Considered and not chosen:
    - *Self-healing setup on the seeded `john`/`jane`*: delete the basket in each *Given*, and
      find "my" order through a per-scenario marker in the request. It works against any stack,
      but orders pile up, and scenarios using the same user can't run in parallel.
    - *Cleaning up afterwards by deleting orders*: deleting an order soon after it was created is
      exactly the window of Known Bug 13, where a redelivered checkout re-creates the deleted
      order. The suite would become flaky because of a real defect.
    - *Fresh stack per run* only fixes state between runs, not between scenarios.
    - *Truncating the databases from the runner*: breaks the black box, and would tie the suite
      to three schemas.
  - Cost: the suite depends on Keycloak's admin API, which isn't the application's API. Keycloak
    is part of the system under test, and the local realm's admin credentials are already
    committed (`admin`/`admin`).
- **State between steps lives in actor memory** (`actor.remember` / `actor.recall`, behind key
  constants). Never in static fields or step-definition fields, which would be shared between
  parallel scenarios or between the two actors in one scenario.
- **Scenarios run in parallel.** End-to-end suites get slow fast. Here each checkout scenario
  waits for the outbox dispatcher (a 5-second poll), so its time is mostly waiting, which
  parallel runs overlap.
- **CI is out of this increment.** The suite runs locally first. Running it in GitHub Actions
  stays in Backlog 22.

## Design

### Project layout

```
e2e/
├── README.md
├── Dockerfile
├── mvnw, .mvn/wrapper/maven-wrapper.properties    copied from the application, same Maven version
├── pom.xml                                         standalone; Serenity, Cucumber, JUnit Platform, Failsafe
└── src/test/
    ├── java/io/github/tommante/eshopmodularmonolith/e2e/
    │   ├── CucumberTestSuite.java                  @Suite, @IncludeEngines("cucumber"), @SelectClasspathResource("features")
    │   ├── stepdefinitions/                        thin: Gherkin → tasks and questions
    │   ├── tasks/                                  e.g. StartsAShoppingBasket, AddsToBasket, ChecksOut
    │   ├── questions/                              e.g. TheOrders, TheBasket, TheLastResponseStatus
    │   └── keycloak/                               CustomerAccounts (admin API), Tokens
    └── resources/
        ├── features/                               catalog, authentication, checkout, order_visibility
        ├── junit-platform.properties
        └── serenity.conf
```

- **Dependencies:** `net.serenity-bdd:serenity-cucumber`, `serenity-screenplay-rest` and
  `serenity-rest-assured` at **5.4.1** (Maven Central, released 2026-10-13), with
  `io.cucumber:cucumber-junit-platform-engine` and `org.junit.platform:junit-platform-suite` at the
  versions Serenity 5.4.1 is built against. `serenity-maven-plugin` 5.4.1 aggregates the report
  in `verify`.
- **Failsafe, not Surefire,** runs the suite. Then `verify` still produces the report when
  scenarios fail, and fails the build afterwards. That's the layout Serenity's own templates use.
- **The two URLs come from the environment,** with defaults for running from the host against the
  `app` profile:

  | Variable | Default (from the host) | In the `e2e` container |
  |---|---|---|
  | `E2E_APP_URL` | `http://localhost:8080` | `http://app:8080` |
  | `E2E_KEYCLOAK_URL` | `http://localhost:9090` | `http://keycloak:8080` |
  | `E2E_KEYCLOAK_ADMIN_USER` / `_PASSWORD` | `admin` / `admin` | same |

  Tokens requested on `keycloak:8080` carry the same issuer as those from `localhost:9090`, which
  is what lets the containerized application accept them. The previous increment verified this.

### Parallelism

`src/test/resources/junit-platform.properties`:

```properties
cucumber.execution.parallel.enabled=true
cucumber.execution.parallel.config.strategy=fixed
cucumber.execution.parallel.config.fixed.parallelism=4
cucumber.plugin=net.serenitybdd.cucumber.core.plugin.SerenityReporterParallel
```

- **`fixed`, not `dynamic`:** scenarios wait on HTTP and the outbox, not on CPU, so tying threads
  to cores would under-use a small machine. The count can be overridden with
  `-Dcucumber.execution.parallel.config.fixed.parallelism=<n>`.
- **`SerenityReporterParallel`** is the reporter Serenity documents for parallel runs. Since
  Serenity 5.0 it lives in `net.serenitybdd.cucumber.core.plugin`, not `io.cucumber.core.plugin`.
- **Rule for future scenarios:** shared data is read-only. Today that's the seeded catalog. A
  scenario that writes catalog data creates its own product and reads only that one; otherwise
  one scenario's price change breaks another's total.

### Customers per scenario

- `CustomerAccounts.create()`:
  1. gets an admin token from the `master` realm (`admin-cli`, password grant);
  2. `POST /admin/realms/eshop/users` with a unique username (`e2e-<uuid>`), first and last name,
     a verified email and a non-temporary password. The profile is complete so Keycloak 26's user
     profile doesn't attach a required action, which would make the password grant fail with
     "Account is not fully set up";
  3. returns the credentials. `Tokens` then gets the customer's token with the same
     `eshop-app` password grant SETUP.md uses.
- The actor gets `CallAnApi.at(appUrl)` and remembers its token. Every request it sends carries
  `Authorization: Bearer <token>`.
- **Clean-up:** an `@After` hook deletes the scenario's users, best-effort. A failed deletion is
  logged, not failed: a leftover `e2e-*` user harms nothing, and a fresh stack drops them all.
- **"A token from another realm"** is a real Keycloak token from the `master` realm (the admin
  token above). Its issuer and signing key both differ from `eshop`'s, so the application must
  reject it. No JWT library or self-signed key needed.

### Waiting for the order

Checkout answers `202` and the order appears once the dispatcher has published the event. The
*Then* step polls with Serenity's `Eventually`, for up to 30 seconds: the order list has exactly
one order, with the expected line, total and card summary (`visa` / `4242`). The declined
scenario's "no order is created" waits a fixed 10 seconds before asserting, two dispatcher
cycles, since there's no event to wait for.

### The `e2e` compose profile

- `e2e/Dockerfile`: `eclipse-temurin:25-jdk-alpine-3.23`. Dependencies are resolved at build time
  into the image's own `~/.m2`, not a cache mount, so a run doesn't download them again. The
  container runs `./mvnw -B verify`.
- In `docker/sb-eshop/compose.yaml`:
  - `app` gets `profiles: ["app", "e2e"]`, so either profile starts it;
  - a new `e2e` service, `profiles: ["e2e"]`, built from `../../../e2e`, with `depends_on: app:
    service_healthy`, the container URLs from the table above, and `e2e/target` bind-mounted, so
    the report lands on the host.
- **The command:** `docker compose --profile e2e run --rm --build e2e`.
  `run` starts the dependencies, waits for their conditions, and exits with the suite's exit
  code. The alternative, `up --exit-code-from e2e`, implies `--abort-on-container-exit`, which
  stops everything as soon as any container exits, and `mongo-init` exits by design.
  To check in step 5: that `run` honours the `depends_on` conditions with this Compose version
  (v5.6).
- The report is written as root inside the container. On Docker Desktop for Mac that's
  invisible. On Linux (CI later) the bind-mounted files end up root-owned, which the CI increment
  will have to deal with (`user:` on the service).

## Steps

The system under test already works, so there is no red/green in the usual sense: a new scenario
is expected to pass at once. The risk with end-to-end tests is the opposite, one that passes
without checking anything. An `Eventually` that never runs its assertion, or a step that ignores
the response. So for every scenario, the guided batch that adds it also shows it **failing for
the right reason** once, by running it against a deliberately wrong expectation (e.g. expecting
`pm_card_visa_chargeDeclined` to answer `202`) or a broken stack (e.g. `payment-provider`
stopped). The failure output is reported, then the expectation is restored. Nothing under
`SpringBoot-EshopModularMonolith/src` changes.

1. **Spike.** `pom.xml`, the wrapper, the suite class, `junit-platform.properties`, a Screenplay
   cast, and two scenarios needing no customer: *the seeded products are listed* and *no token →
   `401`*.
   - Run from the host (`cd e2e && ./mvnw verify`) against `--profile app`.
   - **Proves or refutes, and the result is recorded here:**
     - Serenity 5.4.1 and RestAssured (on Groovy) run on JDK 25. Fallback: compile and run the
       e2e project on JDK 21, independently of the application's JDK;
     - the two scenarios run on different threads (Serenity's report shows the threads, or the
       start times overlap);
     - Screenplay's `OnStage` keeps one stage per thread under parallel Cucumber. If it doesn't,
       each scenario builds its own `Cast` instead of using the static stage;
     - `target/site/serenity/index.html` is written.
2. **Customers per scenario.** `CustomerAccounts`, `Tokens`, the clean-up hook, and the rest of
   *Requests need a token*.
   - Also verify: two runs of the suite at once against the same stack both pass.
3. **Checkout places an order.** Paid and declined.
4. **Orders belong to their customer.**
5. **`e2e` profile.** `e2e/Dockerfile` and the compose changes.
   - Verify from `down -v`: `docker compose --profile e2e run --rm --build e2e` passes and leaves
     the report in `e2e/target/site/serenity/`. Then stop `payment-provider` and check the run
     fails with a non-zero exit code.
6. **Docs and live lists.**
   - `e2e/README.md`: the two ways to run it, the environment variables, the parallelism
     setting, the shared-data rule, and where the report is.
   - `docs/SETUP.md`: a short *End-to-end tests* section pointing to it.
   - `README.md`: a row for `e2e/` in *Repository Structure*, and the suite next to the test
     description.
   - `docs/BACKLOG.md` item 22: narrowed to running the suite in GitHub Actions. That covers its
     own workflow on PRs, the report as an artifact, the image-build cache, the `sparse-checkout`
     and `paths` filters that need the new folder, and root-owned report files on Linux.
   - `docs/TESTING_CONVENTIONS.md`: where end-to-end tests sit relative to the behavior-seam
     guidance. They're the widest seam, for flows that cross every module, not a place to repeat
     what a narrower test already proves.

## Deferred

- Running the suite in GitHub Actions: stays in Backlog 22, narrowed in step 6.
- Catalog-write and order-deletion scenarios: not committed to. Add them when a change in those
  flows needs an end-to-end check.
