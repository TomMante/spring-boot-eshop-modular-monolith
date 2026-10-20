# fresh-clone-startup

Close [Known Bug 8](../../../docs/KNOWN_ISSUES.md): make one documented command
sequence take a clean checkout to an authenticated request, and verify it by
actually running it against a clean checkout.

## Goal

From `git clone` to a successful authenticated call against a documented
endpoint, using only committed files and the commands in
[SETUP.md](../../../docs/SETUP.md). No untracked file, no manual Keycloak
click-through, no step that only works because the machine already ran the
project before.

## Decisions taken (2026-09-09)

| Question | Decision |
| --- | --- |
| Where does the working configuration live? | The committed `application.yaml` boots against `compose.yaml` with no profile active. `application-local.yaml` is deleted; an untracked personal profile remains possible but nothing depends on one. |
| How is the Mongo URI supplied? | `spring.mongodb.uri` stays exactly as it is — it is the correct Spring Boot 4 property name. `MongoConfiguration` is **not** changed. The defect is only that nothing committed defines the value. |
| How does the quickstart get a token? | Add a demo user and a literal client secret to the committed realm import, so the existing Bruno password-grant request works straight after `docker compose up`. |
| What does "runs" mean? | Dependencies in compose, application on the host via Maven. The commented-out `sb-eshop` compose service stays commented; the fully containerised route is out of scope. |
| How does demo catalog data get there? | A **separate Flyway instance** with its own `flyway_seed_history` table, created only when `eshop.catalog.seed-demo-data` is true. `catalogFlyway` is untouched, so the schema's history never records the seed and the flag can be turned off without breaking validation. |

## Ownership

Implementation of `src/` and build files is the owner's. This plan, the setup
documentation, and running the verification are not.

**Resolved 2026-09-09:** `docker/sb-eshop/compose.yaml` and
`docker/sb-eshop/keycloak-init/eshop-realm.json` were handed over, and steps 4
and 5 are done. Nothing is committed yet.

## The six defects

Three are as recorded in Known Bug 8. Three were found while investigating it on
2026-09-09 and belong to the same story — a fresh clone still cannot reach an
authenticated request without them.

1. **No committed Mongo URI.** `MongoConfiguration.mongoClient` reads
   `@Value("${spring.mongodb.uri}")`; the only definition is in the untracked
   `application-local.yaml`. Boot's `MongoProperties` default of
   `mongodb://localhost/test` lives on the `@ConfigurationProperties` bean and
   never enters the `Environment`, so a bare `@Value` on the same key cannot see
   it and placeholder resolution fails at startup.
2. **Postgres password disagrees with compose.** `application.yaml` says
   `password`; `compose.yaml` starts Postgres with `postgres`. The untracked
   file silently corrects it.
3. **SETUP.md documents Keycloak steps that `--import-realm` already performs**,
   and misdescribes what the running realm contains.
4. **The realm import has no user to authenticate as.** It contains exactly one
   principal, `service-account-eshop-app`. `bruno/environments/local.bru` points
   at `username: john`, whose password and the client secret are uncommitted
   secret vars, and the client secret in the export is masked as `**********`.
   The author's local Keycloak has a hand-made `john` — visible in Mongo, where
   `shopping-cart._id` is `"john"` — which is precisely why this survived.
5. **Two more steps that only work on a warm machine.** `./mvnw spring-boot:run`
   (SETUP.md step 3) runs from the aggregator root and therefore fires for every
   module rather than the application; and `keycloak` has no `depends_on` in
   `compose.yaml`, so on a cold `docker compose up` it races Postgres and the
   `identity` schema created by `postgres-init`.
6. **Nothing seeds the catalog, so a booted application has nothing to do.**
   This is load-bearing rather than cosmetic: `AddItemIntoBasketHandler` and
   `CheckoutBasketHandler` both resolve products through `ProductService` into
   catalog, so with an empty `catalog.products` table there is nothing to put in
   a basket and no path to an order. A reader who gets everything else right
   still sees an empty `GET /products` and no way forward.

## Sequencing

### Step 1 — Pin the requirement with a failing test — **done 2026-09-09**

**Written as `starter`'s `DefaultConfigurationTest`, two tests, and the strict
red-first ordering below was not followed** — step 2 was handed over and applied
first, so the test could only be born green. The substitute for a red phase was
a mutation check: deleting `spring.mongodb.uri` from `application.yaml` failed
exactly the Mongo test, deleting `spring.datasource.password` failed exactly the
datasource test, and the file was restored byte-identically afterwards.

**Known limitation.** These assert *presence*, so they guard defect 1 (a
property missing entirely) but not defect 2 (a property present with a value
that disagrees with `compose.yaml`). The password was wrong, not absent, and
this test would not have caught it. Closing that needs the compose-agreement
check recorded under *Considered and not taken*.

### Original plan for this step *(strict red → green)*

This is the step where watching the test fail changes what gets written, so do
it first and watch it fail.

A test in `starter` that loads the application's own configuration with **no
profile active** and asserts the properties a fresh clone needs are resolvable.
It should fail today for defect 1.

Sketch — the seam is "default configuration is complete", so load config data
rather than the whole context:

```java
@Test
void givenNoProfileActive_whenDefaultConfigurationIsLoaded_thenMongoUriIsDefined() {
    new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .run(context -> assertThat(context.getEnvironment().getProperty("spring.mongodb.uri"))
                    .as("a fresh clone must boot without an untracked profile")
                    .isNotNull());
}
```

Cover `spring.datasource.password` in the same class. Assert **presence and
resolvability**, not specific values — the values belong to the environment, and
a test that restates them is a second copy of the configuration that drifts.

Agreement with `compose.yaml` is deliberately *not* tested here; see *Considered
and not taken*.

### Step 2 — Make the committed configuration boot *(implementation)*

Fold `application-local.yaml` into `application.yaml`. Nothing in it is secret —
it is a local connection string, a database name and a container password — so
there is no reason for it to be uncommitted.

The merge is a two-line delta, because the local file is a near-total copy of
the default rather than an override of the one machine-specific value: it
repeats `spring.application.name`, the datasource url and username, and the
whole `jpa` block identically. Only two lines are not already committed:

- add `spring.mongodb.uri: mongodb://localhost:27020`, matching compose's mapped
  port;
- change `spring.datasource.password` from `password` to `postgres`.

The `oauth2` block and the two Mongo `representation` lines exist only in
`application.yaml` and stay as they are.

That the local file was a whole-file copy is worth recording: an override that
restates everything cannot show which value is actually environment-specific,
which is exactly how the password drifted apart from `compose.yaml` unnoticed.

Then delete the untracked `application-local.yaml`. Nothing should reference it
afterwards. Step 1 turns green here.

Note on the URI: compose runs Mongo as replica set `rs0` whose member advertises
`mongodb:27017`, a name that does not resolve from the host. This is fine — the
driver uses SINGLE topology for a one-host URI with no `replicaSet` parameter, so
it connects directly and transactions still work. Do not add `?replicaSet=rs0`;
that would switch it to discovery and break it.

### Step 3 — Seed the catalog, without seeding the tests *(tests alongside)*

The seed rows themselves are static content, so writing them red-first teaches
nothing. The *conditional wiring* is the part with a real invariant — "catalog
tests must not see these rows" is the entire reason this option was chosen over
a plain `V2__` migration — so pin that with a test written alongside the change.

**Design revised 2026-09-09** after testing the original single-instance idea.
Adding the seed as a second *location* on `catalogFlyway` makes the seed a row
in `catalog.flyway_schema_history`, and turning the flag off afterwards then
fails startup outright:

```
Validate failed: Migrations have failed validation
Detected applied migration not resolved locally: demo products.
```

So the seed gets **its own Flyway instance with its own history table**, and
`catalogFlyway` is left exactly as it was. The schema's history never records
the seed, so disabling it is a non-event rather than a validation failure, and
no validation is weakened anywhere to achieve that.

```java
@Bean
@ConditionalOnProperty(name = "eshop.catalog.seed-demo-data", havingValue = "true")
Flyway catalogSeedFlyway(DataSource dataSource) {
    return Flyway.configure()
            .dataSource(dataSource)
            .schemas("catalog")
            .locations("classpath:db/catalog-seed")
            .table("flyway_seed_history")
            .baselineOnMigrate(true)
            .load();
}

@Bean
@ConditionalOnProperty(name = "eshop.catalog.seed-demo-data", havingValue = "true")
@DependsOn("catalogFlywayInitializer")
FlywayMigrationInitializer catalogSeedFlywayInitializer(
        @Qualifier("catalogSeedFlyway") Flyway catalogSeedFlyway) {
    return new FlywayMigrationInitializer(catalogSeedFlyway);
}
```

Three details that are not optional:

- **`.baselineOnMigrate(true)` is required**, not stylistic. By the time the
  seed instance runs, the `catalog` schema is already non-empty, and its own
  history table does not exist yet — without the baseline Flyway refuses with
  *"Found non-empty schema(s) 'catalog' but no schema history table"*.
- **`@DependsOn("catalogFlywayInitializer")`** — the seed inserts into tables
  the schema migration creates, so bean ordering has to be explicit.
- **`.table("flyway_seed_history")`** is the whole point; sharing the default
  table reintroduces exactly the problem this design avoids.

The property default now lives in the conditional rather than a `@Value`, so
absence means "no seed" for every consumer of the context, and only the
committed `application.yaml` turns it on.

**One limitation to document, not solve:** the flag controls whether the seed
*runs*, never whether the data *is there*. Turning it off leaves the seeded rows
in place. An genuinely empty catalog needs a fresh database
(`docker compose down -v`).

Put the data in `catalog-core/src/main/resources/db/catalog-seed/` as a
**repeatable** migration — `R__demo_products.sql` — with idempotent inserts
(`insert ... on conflict (id) do nothing`) and fixed UUIDs, so re-running against
an existing database is harmless and the ids can be quoted verbatim in SETUP.md.
A versioned `V2__` would instead become a permanent, non-optional part of the
schema history.

Caveat to state in the file's header comment: repeatable migrations share
`flyway_schema_history`, so turning the flag *off* against a database that
already applied them makes Flyway report a missing migration. That only affects
a long-lived local database — test databases are created fresh — but it will
look like a bug when it happens.

The tests, built the way `FlywayMigrationTest.setUp` already builds Flyway
instances, varying only locations and history table:

- migrating `db/catalog` alone leaves `catalog.products` empty — the guarantee
  that catalog tests never inherit demo rows;
- running the seed instance inserts the expected products;
- `catalog.flyway_schema_history` contains **no** row for the seed afterwards —
  this is the assertion that pins the chosen design, and the one that would fail
  if someone later folds the seed back in as an extra location.

**Verified 2026-09-09** against Postgres 18.1 with Flyway 11.14.1, outside the
application: schema migration, then the seed instance failing without
`baselineOnMigrate` and succeeding with it, then a schema-only run with the seed
instance absent — which reported `applied=0` and no validation error, leaving two
separate history tables.

### Step 4 — Make the realm import self-sufficient — **done 2026-09-09**

In `docker/sb-eshop/keycloak-init/eshop-realm.json`:

- add an enabled user `john` with a password credential, matching what
  `bruno/environments/local.bru` already expects;
- replace the masked `secret: "**********"` on `eshop-app` with a literal value.

`directAccessGrantsEnabled` and the audience mapper are already correct in the
export; leave them alone.

These are local-only demo credentials for a study project, in a file that
already commits `admin`/`admin` for the Keycloak container. State that plainly in
SETUP.md rather than leaving a reader guessing whether they are real.

**As written:** user `john` / password `password`, enabled, `emailVerified`,
holding `default-roles-eshop`; client secret `local-demo-secret`. The username
matches what `bruno/environments/local.bru` already expects, and what the
existing basket documents use as their `_id`. No roles beyond the default were
needed — `SecurityFilterChainConfig` requires authentication, not authority.

### Step 5 — Fix the cold-start race — **done 2026-09-09**

Give `eshopdb` a healthcheck and make `keycloak` `depends_on` it with
`condition: service_healthy`, so the `identity` schema exists before Keycloak
tries to use it. `mongodb` already has a healthcheck and `mongo-init` already
gates on it; this brings Keycloak to the same standard.

**As written:** `pg_isready -h 127.0.0.1 -U postgres -d EShopDb`. The `-h` is
load-bearing and carries a comment saying so — on first boot the postgres
entrypoint runs a temporary server on the unix socket only, so a socket-based
check reports ready *before* `postgres-init` has created the `identity` schema,
which is the exact race being closed.

**Migration note for the existing local stack:** the running
`sb-eshop-eshopdb-1` container was created before this healthcheck existed, so
`docker compose start` now fails with *"container sb-eshop-eshopdb-1 has no
healthcheck configured"* — Keycloak's new `depends_on` cannot be satisfied by a
container that predates it. `docker compose up -d` recreates both services and
resolves it; anonymous volumes are carried over on recreate, so the data
survives (only `--renew-anon-volumes` would discard it).

**Verified 2026-09-09** on a throwaway stack (`-p eshop-fresh`, ports remapped,
fresh volumes, the author's stack untouched):

- ordering held — compose logged `eshopdb Waiting → Healthy → keycloak Starting`;
- the `identity` schema existed in the new database;
- the realm imported and `POST /protocol/openid-connect/token` with
  `grant_type=password`, `client_id=eshop-app`,
  `client_secret=local-demo-secret`, `john`/`password` returned a bearer token
  whose claims carry `aud: eshop-app` and `preferred_username: john` — so the
  audience mapper the resource server checks is intact.

One caveat on that evidence: the throwaway stack ran Keycloak on a different
port, so the token's `iss` was `http://localhost:59090/realms/eshop`. Against
the real stack it is `:9090`, which is what `application.yaml`'s `issuer-uri`
expects. The issuer check is port-sensitive; nothing here changes it.

### Step 5b — Make the Bruno collection usable from a fresh clone — **done 2026-09-09**

Added after the fact, when the realm import made the credentials committable.
Numbered 5b rather than renumbering the steps below, which are referenced above.

`bruno/environments/local.bru` is **not tracked** (confirmed against
`git ls-files`), so a fresh clone opened the collection with no environment at
all and no way to obtain a token — the same defect as Known Bug 8's first, in a
different file.

- **`bruno/environments/default.bru`**, committed, carrying the working demo
  credentials. `access_token` stays a secret var deliberately: Bruno writes
  non-secret var changes back into the file, and the token request would
  otherwise dirty the committed environment on every run.
- **`bruno/README.md`** rewritten. Its "One-time setup: Keycloak client secret
  and user password" section instructed the reader to copy a secret out of the
  Keycloak admin console — obsolete and actively wrong now that the secret is
  committed. Replaced with a credentials section stating why committing them is
  safe, and a table of the seeded product ids.
- **`Add Item Into Basket` and `Remove Item From Basket`** pointed at product
  ids that exist on no fresh clone (`ccdebfe4-…` and an all-zero placeholder).
  Both now use the seeded Galaxy Smartphone id.

The untracked `local.bru` was **deleted** rather than kept alongside the default,
by decision: everything it held is in `default.bru`, and leaving a second
environment in place would reproduce the original problem — a working setup that
exists only on one machine. The whole usage is now in git, which also removes the
`git add bruno/environments/` hazard that would otherwise have swept a personal
file into a commit.

### Step 6 — Rewrite SETUP.md as a verified quickstart — **done 2026-09-09**

Replace the current contents with the exact commands, in order, from clone to an
authenticated request:

- prerequisites (JDK 25, Docker);
- `docker compose -f SpringBoot-EshopModularMonolith/docker/sb-eshop/compose.yaml up -d`
  and how to tell when it is ready;
- `./mvnw clean install`;
- running the application — **with `-pl starter`**, since the current step 3 runs
  from the aggregator root and fires for every module;
- obtaining a token (curl equivalent of the Bruno password grant, with the demo
  credentials from step 4);
- `GET /products`, which returns the seeded catalog from step 3 — the first
  point at which the reader sees the application actually working;
- the flow through to an order: add a seeded product id to a basket, check out,
  then `GET /orders`. This is what makes the outbox and the cross-module
  integration event visible, and it is the only part of the quickstart that
  demonstrates the thing the project is about.

Delete the "Keycloak Setup" section — `--import-realm` does all three of its
steps. Keep the Swagger and Bruno pointers.

### Step 7 — Verify against a genuinely clean checkout — **done 2026-09-09**

Run against a checkout built from `git ls-files` (337 tracked files, no `target/`, no
`application-local.yaml`, no `bruno/environments/local.bru`), with a separate compose
project on fresh volumes and the standard ports. Every step below is the command
SETUP.md now documents:

| Step | Result |
| --- | --- |
| `docker compose up -d` | `eshopdb Waiting → Healthy → keycloak Starting` — ordering held on empty volumes |
| `identity` schema | present before Keycloak started |
| `./mvnw clean install` | BUILD SUCCESS |
| `./mvnw -pl starter spring-boot:run` | `Started EshopModularMonolithStarter in 2.761 seconds`, no profile active |
| logging | 128 INFO lines, none of the previous "no appenders present" warnings |
| token as `john` | issued, `aud: eshop-app`, `preferred_username: john` |
| `GET /products` | the three seeded products |
| `POST /basket` | 201 |
| `POST /basket/items` | 202; basket shows `Galaxy Smartphone` at `550.00`, resolved through catalog |
| `POST /basket/checkout` | 202 |
| `GET /orders` | order present after ~9s, carrying the right items, customer `john` and both addresses |

**One deviation, and it is a finding.** The first build failed with
`invalid target release: 25`: the JDK 25 pin lives in `.java-version`, which is
gitignored, so the clean checkout inherited the machine's default JDK 21. The
gitignore is deliberate and correct — the file names a local jenv alias — so the
verification continued with `JAVA_HOME` set to JDK 25, which is what a reader
meeting SETUP.md's stated prerequisite would have. The unhelpful error message is
recorded in [DESIGN_NOTES.md](../../../docs/DESIGN_NOTES.md); it is not a defect,
and no decision was taken.

### Original plan for this step

The whole bug exists because a warm machine hides it, so verification must not
run on one:

- clone the repository to a temporary directory (or use a fresh `git worktree`),
  so no untracked `application-local.yaml` is present;
- `docker compose down -v` the existing stack, or bring the new one up under a
  separate project name with fresh volumes;
- run the SETUP.md commands **verbatim**, changing nothing silently — anything
  that has to be adjusted is a defect in the document, not in the run;
- finish on a real authenticated response, not on "the context started".

### Step 8 — Close out — **done 2026-09-09**

- [x] Known Bug 8 removed from [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md);
      all six of its defects are fixed and verified end to end.
- [x] The JDK-error legibility question recorded in
      [DESIGN_NOTES.md](../../../docs/DESIGN_NOTES.md) — a note, not a backlog
      entry, because nobody has committed to doing it.
- [x] [README.md](../../../README.md) checked and deliberately left alone: it
      links SETUP.md from its documentation index, which is still accurate.
      Giving it a real quickstart is a separate piece
      of work about the README as a landing page.

**Raised during this plan, and closed 2026-09-09 rather than filed.** The
divergence was real: `spring.mongodb.representation.uuid` is a genuine Spring
Boot 4 property, but `MongoConfiguration` built its own `MongoClient` with a
hard-coded `UuidRepresentation.STANDARD` and never read it — while all three
Mongo test base classes (`BasketDatabaseTest`, `OrderingDatabaseTest`,
`MongoOutboxEventStoreTest`) let Boot's auto-configuration build the client and
therefore *did* honour their `java_legacy` setting. The persistence tests were
exercising a binary UUID format the application never writes.

Fixed by honouring the property with a `standard` default and setting `standard`
in `application.yaml` and in all three test classes. For production this is a
runtime no-op — the value is what the code already used — so existing documents
stay readable; the behaviour that actually changed is in the tests.

No comment was added to `application.yaml`: the property is standard Spring
Boot, and the project does not document framework behaviour in its own config.

## Discovered, not fixed here

❓ **`spring.mongodb.representation.uuid: java_legacy` in `application.yaml` does
not describe what the application does.** `MongoConfiguration` builds the
`MongoClient` itself with a hard-coded `UuidRepresentation.STANDARD`, so the
property has no path to take effect in production. Confirmed by inspection of
the running database: `eshop-basket-db.outbox._id` is stored as a subtype-4
(standard) UUID.

Three test classes — `MongoOutboxEventStoreTest`, `BasketDatabaseTest`,
`OrderingDatabaseTest` — set the same property. If Boot's own auto-configuration
creates the client in those slices, they are exercising the *legacy*
representation while production uses the standard one, which is the
slice-versus-production drift already recorded as Testing Gap 7. Unverified.

Proposal: record as a new Known Bug once the divergence is confirmed either way,
rather than folding a second investigation into this one. Not actioned pending
the owner's agreement.

## Considered and not taken

- **A test asserting `application.yaml` agrees with `compose.yaml`.** It would
  catch the exact drift of defect 2, and it is the same silent-drift shape as
  [Backlog item 10](../../../docs/BACKLOG.md). Left out because the test would
  have to reach out of the module to parse a compose file by relative path, which
  is brittle in a way that outlives the bug it guards. Revisit if the pair drifts
  a second time.
- **Committing `application-local.yaml` instead of fixing `application.yaml`.**
  Makes the default configuration correct only for whoever remembers the
  profile flag, which is the same failure one step further along.
- **Making the containerised route work.** Out of scope by decision; it needs
  in-network hostnames, an image build step and startup ordering, and would mean
  two documented paths to verify instead of one.
- **Seeding as a second location on the existing `catalogFlyway`.** The first
  version of this plan. Rejected on evidence: the seed lands in
  `catalog.flyway_schema_history`, so turning the flag off later fails startup
  with *"Detected applied migration not resolved locally"*.
  `.ignoreMigrationPatterns("repeatable:missing")` does fix it — verified — but
  it buys the fix by weakening validation for every repeatable migration the
  module will ever have, to protect one that is not schema at all.
- **Seeding via a plain `V2__seed_products.sql` in `db/catalog`.** No code
  change, but `FlywayMigrationTest` and `CatalogDatabaseTest` both migrate that
  location, so demo rows would become fixture data for every catalog test, and
  the seed would become a non-optional part of the schema history that any real
  deployment inherits.
- **Seeding from a `CommandLineRunner` in `starter`.** Catalog's contract
  exposes only the read side (`ProductQueryService`), so a seeder would have to
  reach into catalog's internal write handler from the composition root —
  precisely the boundary the project exists to demonstrate, and not worth
  blurring for demo data.
- **No seeding, with the quickstart creating products by `POST /products`.**
  Honest and zero-machinery, but leaves `GET /products` empty on a bare boot,
  which reads as a broken application to anyone who runs it before reading.
