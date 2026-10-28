# End-to-End Tests With Serenity

Approved by the user on 2026-10-28. Implementation: guided (the assistant implements each
batch after the user's "ok"), per [feature-workflow.md](../../processes/feature-workflow.md).

## Status
- [x] Step 1 — Spike: the project skeleton runs two scenarios in parallel on JDK 25 and writes a report.
  Results, 2026-10-28, from the host against `--profile app`:
  - **JDK 25 works.** Serenity 5.4.1 with RestAssured 6.0.0 on Groovy 4.0.29 compiles and runs on
    Temurin 25.0.4.1, with no class-file version errors. No JDK 21 fallback needed.
  - **Parallel:** the two scenarios started 2 ms apart and ran about 870 ms each. The whole suite
    took 1.1 s, so they overlapped.
  - **`OnStage`** holds its stage in a `private static final ThreadLocal<Stage>` (checked with
    `javap` on `serenity-screenplay-5.4.1.jar`), so `setTheStage` in a `@Before` hook gives each
    scenario thread its own cast.
  - **Report:** `target/site/serenity/index.html` is written, also when scenarios fail.
  - **Red for the right reason:** with the expectations broken on purpose, the catalog scenario
    failed listing the actual products, and the anonymous one failed with
    `Expected status code <200> but was <401>`.
  - **Deviation:** every endpoint needs a token, catalog included, so `Given <actor> is a customer`
    signs in as the seeded `john` until step 2 creates customers per scenario. The feature text
    doesn't change in step 2.
  - **Needed beyond the plan:** `serenity-cucumber` declares `cucumber-junit-platform-engine` only
    at test scope, so the e2e pom declares it itself. Failsafe needs
    `<include>**/CucumberTestSuite.java</include>`, because the class name matches none of its
    default patterns.
- [x] Step 2 — A fresh customer per scenario; *Requests need a token*.
  Results, 2026-10-28, from the host against `--profile app`:
  - **Customers per scenario** are created and deleted through the admin API as designed; the
    `john` stand-in from step 1 is gone. After each run, no `e2e-*` user is left in the realm.
  - **Red for the right reason:** with the expectations inverted one at a time, the eshop-token
    scenario failed with `Expected status code <401> but was <200>`, and the other-realm one with
    `Expected status code <200> but was <401>`. Green: 4 tests, 4 passed.
  - **Two runs at once** (two copies of `e2e/`, so their `target/` folders don't collide) both
    passed against the same stack.
  - **Deviation:** the clean-up hook reaches the scenario's actors through a `Cast` kept in a
    field of `StageSetup`, against the "no step-definition fields" rule. `Stage` has no getter
    for its cast, and Cucumber creates the class once per scenario, so the field holds that
    scenario's cast only. It is clean-up bookkeeping, not step state; avoiding it would need a
    DI container (cucumber-picocontainer) for this alone.
  - **Added at the user's request:** logback (1.5.25, the version the application's Spring Boot
    uses) with `logback-test.xml` taking the application's `custom-logging` pattern, minus the
    PID and trace ids, which need Spring Boot or a request. A failed clean-up logs a warning
    through it.
- [x] Step 3 — *Checkout places an order*: paid and declined.
  Results, 2026-10-28, from the host against `--profile app`:
  - **The order is compared as one record,** `OrderSummary` (all its lines, the total, the card
    brand and last four digits), against a list holding only the expected one. That checks
    "exactly one order" and every line at once. Comparing only the first line was dropped
    because a duplicated line would have passed; mapping the response onto Jackson DTOs was
    dropped because it would copy the API's shape into the suite. Order lines carry only a
    product id, so steps look up the seeded product's id by name in the catalog.
  - **Red for the right reason:** with the order expected as 3 phones instead of 2, the paid
    scenario failed after `Eventually`'s full 30 seconds, printing both records, which differ
    only in the line's quantity. With the decline expected to answer `202`, the declined scenario
    failed with `Expected status code <202> but was <402>`. Green: 6 tests, 6 passed, in about
    15 seconds of wall time; the declined scenario's 10-second wait overlaps the others.
  - **Also changed:** the suite selects features with `@SelectPackages("features")`, as Cucumber's
    discovery warning asks, instead of `@SelectClasspathResource`.
- [x] Step 4 — *Orders belong to their customer*.
  Results, 2026-10-28, from the host against `--profile app`:
  - **A control scenario was added:** the owner fetches the order by its id and gets it back,
    checked on `order.id`. Another customer's `404 order.not-found` is the same answer as for an
    id that doesn't exist, so without the control a broken path or id would pass unnoticed.
  - **One actor's memory read through another:** in "Bob asks for Alice's order" the step takes
    both actors and reads the id from Alice's memory. The "has placed an order" step checks the
    checkout's `202` before waiting, so a failed checkout doesn't surface 30 seconds later as a
    wrong list size.
  - **Red for the right reason:** asking for a random id made the control scenario fail with
    `Expected status code <200> but was <404>`. Letting Alice ask where Bob should made the other
    one fail with `Expected status code <404> but was <200>`, the failure a leak between
    customers would give. Green: 8 tests, 8 passed, in about 16 seconds.
  - **Found by the second red run:** the steps worded "`<actor>`'s request is …" check the
    thread's last response (`SerenityRest.lastResponse()`), not that actor's. In that run, "Bob's
    request is refused" checked Alice's response. Every current scenario has the same actor make
    the request and then check it, so nothing is wrong today. A scenario that interleaves two
    actors' requests before checking would check the wrong one.
- [x] Step 5 — The `e2e` compose profile runs the suite in a container.
  Decided while implementing, 2026-10-28:
  - **No `e2e/Dockerfile`** (changed with the user). The `e2e` service runs the stock
    `eclipse-temurin:25-jdk-alpine-3.23` image, the application's build-stage image, with `e2e/`
    bind-mounted and a named volume (`e2e-maven-repository`) for `~/.m2`. Nothing ships the
    runner, so a custom image only added a rebuild after every scenario edit and the gaps of
    `dependency:go-offline`. The report lands in `e2e/target/site/serenity/` without a separate
    mount. `down -v` also drops the Maven volume, so the next run downloads dependencies again.
  - **Keycloak gets a healthcheck** (`KC_HEALTH_ENABLED=true`, `/health/ready` on the management
    port through bash's `/dev/tcp`, since the image has no curl or wget), and `e2e` waits for
    `keycloak: service_healthy` as well as `app: service_healthy`. `app` waits only for Keycloak
    to have started, and its readiness doesn't touch Keycloak, so from `down -v` the suite could
    otherwise ask for a token before the realm is imported.
  - **`mongo-init` made idempotent** (fixed with the user, 2026-10-29). Its `rs.initiate(...)` ran
    unconditionally, so a second `run`, or a second `--profile app up`, recreated the exited
    container, which failed with `MongoServerError: already initialized`. `app` waits on
    `service_completed_successfully`, so the stack stopped there. The defect dates from
    `compose-runs-the-application`. The entrypoint now initiates only when `rs.status()` throws.

  Results, 2026-10-28/29, with `docker compose --profile e2e run --rm --build e2e`:
  - **From `down -v`:** exit 0 in 88 seconds, 8 tests and 8 passed, with the report at
    `e2e/target/site/serenity/index.html`. The startup log showed `app` healthy before
    `keycloak`, which is the race the healthcheck guards against. This run downloaded the
    dependencies into the Maven volume.
  - **Second run** (the first try hit the `mongo-init` defect above; this is the run after the
    fix): exit 0 in 19 seconds, 8 tests and 8 passed, nothing downloaded. `mongo-init` ran
    against the initialised replica set and exited 0.
  - **Payment provider stopped** (run with `--no-deps`, so `run` doesn't start it again): exit 1.
    4 tests failed, the two checkout scenarios and both order-visibility scenarios, whose
    Background places an order, all with `503` instead of `202`/`402`. The run fails the build,
    not just the report. Starting `payment-provider` again afterwards restored the stack.
- [x] Step 6 — Docs and the live lists.
  Done 2026-10-29, as listed below, plus two places that still called Cucumber hypothetical:
  *Current convention* and *Trade-off and intended evolution* in `TESTING_CONVENTIONS.md`.
  The parallelism override was checked before documenting it: with
  `-Dcucumber.execution.parallel.config.fixed.parallelism=1` every scenario ran on one worker
  (23 seconds against 13), so Failsafe passes the Maven user property to the forked JVM.

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
