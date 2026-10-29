# End-to-end tests

Serenity BDD scenarios (Cucumber, Screenplay) that drive the running system from the outside:
the application's HTTP API, real Keycloak tokens, and the `payment-provider` container standing
in for Stripe. A separate Maven project, not part of the application's build; it knows only two
base URLs.

| Feature | What it checks |
| --- | --- |
| `catalog.feature` | The seeded products are listed |
| `authentication.feature` | No token, or a token from another realm, is refused; a customer's token is accepted |
| `checkout.feature` | A paid checkout becomes exactly one order with the right line, total and card; a declined one keeps the basket and creates no order |
| `order_visibility.feature` | A customer finds their own order by id; another customer gets `404` and an empty list |

## Running it

**In Docker** (no JDK needed), from the repository root:

```bash
docker compose -f SpringBoot-EshopModularMonolith/docker/sb-eshop/compose.yaml --profile e2e run --rm --build e2e
```

Compose builds and starts the application and its stack, waits until the application and
Keycloak report ready, then runs `./mvnw -B verify` in a stock JDK 25 container with this folder
mounted. The command exits with the suite's exit code. The first run downloads Maven and the
suite's dependencies into the `e2e-maven-repository` volume; later runs reuse it. The stack keeps
running afterwards: stop it with `down` and `--profile e2e`, and note that `-v` also drops the
Maven volume (see [Starting over](../docs/SETUP.md#starting-over)).

**From the host** (JDK 25), with the application already running, either under `--profile app`
or on the host (see [SETUP.md](../docs/SETUP.md)):

```bash
cd e2e && ./mvnw verify
```

**In CI**, the [end-to-end workflow](../.github/workflows/spring_boot_eshopmodularmonolith_e2e.yml)
runs the same compose command after every merge to `main` that touches the application, this
folder or the workflow, and can be started by hand from the Actions tab. It doesn't run on pull
requests. The report is uploaded as the `serenity-report` artifact, and a failed run prints the
stack's logs.

## Settings

| Variable | Default | In the `e2e` container |
| --- | --- | --- |
| `E2E_APP_URL` | `http://localhost:8080` | `http://app:8080` |
| `E2E_KEYCLOAK_URL` | `http://localhost:9090` | `http://keycloak:8080` |
| `E2E_KEYCLOAK_ADMIN_USER` | `admin` | default |
| `E2E_KEYCLOAK_ADMIN_PASSWORD` | `admin` | default |

The admin credentials are the Keycloak container's own, committed in `compose.yaml`. The suite
uses them to create a fresh customer for each actor in each scenario. An `@After` hook deletes
those customers, best-effort: a failed deletion is logged, not failed, since a leftover `e2e-*`
user harms nothing and `down -v` drops them all. Orders placed by deleted customers stay in
MongoDB; nothing reads them.

## Parallelism

Scenarios run on 4 threads (`src/test/resources/junit-platform.properties`). The count is fixed
rather than tied to CPU cores, since scenarios wait on HTTP and the outbox, not on the CPU.
Override it for one run:

```bash
./mvnw verify -Dcucumber.execution.parallel.config.fixed.parallelism=1
```

**Rule for new scenarios: shared data is read-only.** Today that's the seeded catalog. A
scenario that writes catalog data creates its own product and reads only that one; otherwise one
scenario's price change breaks another's total.

## The report

`target/site/serenity/index.html`, written by both ways of running the suite. In CI, download
the `serenity-report` artifact from the run.
