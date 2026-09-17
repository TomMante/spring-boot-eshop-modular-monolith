# test-dependency-scope-fix

Closes **Known Bug 10** in [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md): `ordering` and
`basket-core` each declare `spring-boot-starter-test`, `spring-boot-starter-webmvc-test` and
`spring-boot-starter-data-mongodb-test` with no `<scope>`, so Maven resolves all six at
`compile`. Test infrastructure is therefore on those modules' production classpath and
transitive to everything depending on them.

Scope is these six declarations. The regression guard that would stop this recurring is
deliberately out of scope and recorded under *Deferred* below.

## Verified before planning (2026-09-10)

Every claim the plan rests on was checked rather than taken from the issue text.

1. **Exactly six declarations, in exactly the two named modules.** A scope audit across all
   fourteen poms found no other unscoped test dependency. `catalog-core`, `outbox-jpa`,
   `outbox-mongo`, `security`, `shared` and `starter` all scope theirs correctly.
2. **The leak reaches the packaged artifact.** `starter/target/starter.jar` carries, under
   `BOOT-INF/lib/`: `spring-test-7.0.3` (1080 KB), `spring-boot-test-4.0.2` (202 KB),
   `spring-boot-test-autoconfigure-4.0.2` (24.5 KB) and `spring-boot-data-mongodb-test-4.0.2`
   (6.5 KB) — 1313 KB total, matching the issue's estimate.
3. **JUnit, Mockito and AssertJ are *on ordering's compile classpath*** (`dependency:tree`
   confirms junit-jupiter 6.0.2, mockito-core 5.20.0, assertj-core 3.27.6 all at `compile`)
   **but are absent from the jar.** `starter` declares `spring-boot-starter-test` itself at
   `test` scope, which shadows most of the subtree during resolution. The four artifacts in
   point 2 are the ones that survive at `compile` anyway — `spring-boot-data-mongodb-test`
   because `starter` declares no counterpart to shadow it. So the issue's two claims are
   both true and describe different classpaths; neither is a restatement of the other.
4. **No production source exploits the leak.** No file under `modules/ordering/src/main`,
   `modules/basket/basket-core/src/main` or `starter/src/main` imports a JUnit, Mockito,
   AssertJ, Testcontainers or Spring test type.
5. **Nothing downstream will stop compiling.** Only `starter` and `sonar` depend on
   `ordering`/`basket-core`. `sonar` has no `src` directory at all — it exists to aggregate
   Jacoco. `starter` has two test classes, and every test type they import
   (`org.junit.jupiter.*`, `org.springframework.boot.test.context.*`,
   `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`,
   `org.springframework.test.*`, `org.testcontainers.*`, `org.assertj.*`) is served by
   `starter`'s own test-scoped declarations.
6. **`starter`'s Mongo `@ServiceConnection` does not come from the leaked jar.**
   `spring-boot-data-mongodb-test` contains only the `@DataMongoTest` slice machinery, which
   `starter` does not use. The `MongoDBContainer` connection-details factory lives in
   `spring-boot-mongodb`, which `starter` gets at `compile` via
   `spring-boot-starter-data-mongodb` and keeps. This was the one plausible way the change
   could have broken `ApplicationWiringTest`; it does not.

## Sequencing

**No red-first step.** There is no interface to pin down and no logic to design — the change
is a scope keyword on six declarations, and a test written to fail first would be asserting
Maven's own documented behaviour. Per
[rules-of-engagement.md](../../processes/rules-of-engagement.md), that is ceremony rather
than rigor. What replaces it is a **before/after measurement on the built artifact**, which
is the real evidence and cannot be faked by a green suite.

### Step 0 — Record the baseline

Build the jar as it stands and capture what is in it, so step 3 has something to compare
against:

```bash
cd SpringBoot-EshopModularMonolith && unset JAVA_HOME && ./mvnw -q clean package -DskipTests && unzip -l starter/target/starter.jar 'BOOT-INF/lib/*' | grep -Ei 'spring-test|spring-boot-test|mongodb-test'
```

Expect the four jars listed in *Verified* point 2.

### Step 1 — Scope the three declarations in `ordering`

`modules/ordering/pom.xml`, lines 43–54. Add `<scope>test</scope>` to each of the three:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-mongodb-test</artifactId>
    <scope>test</scope>
</dependency>
```

The three `testcontainers` declarations directly below already carry `<scope>test</scope>`
and are the pattern to match.

### Step 2 — Scope the three declarations in `basket-core`

`modules/basket/basket-core/pom.xml`, lines 54–65. The same three artifacts, the same
addition. Nothing else in that file changes.

### Step 3 — Verify

```bash
cd SpringBoot-EshopModularMonolith && unset JAVA_HOME && ./mvnw clean verify
```

Three things to check, in order of what each one proves:

1. **The suite is green.** Confirms the six dependencies still reach the *test* classpath of
   their own modules — moving a dependency to `test` scope does not remove it there.
   `catalog-core` already proves this shape works: it declares no `spring-boot-starter-test`
   at all and compiles its tests from what `spring-boot-starter-webmvc-test` brings in at
   test scope.
2. **The jar shrinks.** Re-run the `unzip -l` from step 0. All four artifacts from the
   baseline should be gone, and the jar about 1.3 MB smaller.
3. **`dependency:analyze` no longer names these two modules.** The pinned plugin version
   cannot run here — it inherits 3.7.0 from the Spring Boot BOM chain, whose bundled ASM
   rejects Java 25 bytecode with `Unsupported class file major version 69`. Invoke 3.9.0
   explicitly:

   ```bash
   cd SpringBoot-EshopModularMonolith && unset JAVA_HOME && ./mvnw org.apache.maven.plugins:maven-dependency-plugin:3.9.0:analyze
   ```

   `Non-test scoped test only dependencies found` must no longer appear for `ordering` or
   `basket-core`. It **will** still appear for `catalog-core` naming `spring-boot-persistence`
   — that is the documented false positive in Known Bug 10, not a missed instance. Ignore the
   plugin's `unused declared` / `used undeclared` output entirely; it reports every Spring
   Boot starter that way by construction.

## Deferred

- **A guard that fails the build on the next unscoped test dependency.** `dependency:analyze`
  detects exactly this, but wiring it into `verify` needs the plugin pinned to 3.9.0 *and*
  `failOnWarning` narrowed — far more narrowed than expected, see *Outcome* below. Its own
  increment with its own configuration decision, and the same shape of problem as Backlog
  item 10 (nothing checks the coverage aggregator). **Filed as
  [BACKLOG.md](../../../docs/BACKLOG.md) item 12.**

## Folded in: three unfiled build fixes

Added at the owner's request after the scope fix verified green, on the reasoning that one
commit should carry the Maven-related work. Each is mechanical and independently verifiable;
the three *filed* Maven items were deliberately left out because each carries an unmade
decision (see *Considered and left out* below).

### A. `sonar` and `modules` were building empty jars

Neither declared `<packaging>`, so both defaulted to `jar`, and every single build emitted
`JAR will be empty - no content was marked for inclusion!` twice. Both are aggregator-only:
`modules/pom.xml` has no sources and no `<modules>` of its own, and `sonar` exists solely to
host `jacoco:report-aggregate`. `modules/basket` and `modules/catalog` already declared
`<packaging>pom</packaging>` — this is the remaining two catching up.

The risk worth naming: `report-aggregate` is bound to `verify` in `sonar`, and its scope comes
from that module's dependencies. Changing packaging changes the default lifecycle, so this
needed proving rather than assuming — see *Outcome* point 4.

### B. `maven.compiler.source`/`target` → `maven.compiler.release`

`source` and `target` set the language level and the bytecode level but do **not** constrain
which JDK APIs are visible, so code can compile against a method that does not exist on the
declared target. `release` pins all three together and is the correct form on JDK 25. Two
properties collapse into one.

### C. Spring Initializr stubs removed from the root pom

`<url/>`, `<licenses><license/></licenses>` and `<developers><developer/></developers>` were
empty generator placeholders. No build effect. Removed because
[portfolio-readiness](../portfolio-readiness/portfolio-readiness.md) wants this repository read
as a public showcase, and an empty `<licenses>` block is a detail a reviewer notices.

## Considered and left out

- **Backlog 10 — coverage aggregator unchecked.** Verified correct as of 2026-09-10: ten
  dependencies in `sonar/pom.xml`, ten code-bearing reactor modules, ten directories in
  `jacoco-aggregate/`. There is no mismatch to fix, only a guard to design, and the choice
  between a `maven-enforcer-plugin` rule and a CI script is open.
- **Backlog 12 — scope regression guard.** Needs the `failOnWarning` ignore list designed
  first; see the measurement in *Outcome* below.
- **Backlog 11 — duplicated `FakeOutboxEventStore`.** The Maven half is four lines in
  `shared/pom.xml`; the rest is test sources and, as found here, a behavioural choice. The
  backlog entry was corrected — there are **three** copies, not two, and `shared`'s differs
  semantically from the other two (it deletes on `markAsProcessed`; they flag and retain).

### D. Surefire only ran `**/*Test.java`

A class named `FooTests` or `FooTestCase` was compiled, reported as uncovered, and **silently
never executed** — green build, lower count, no signal. Nothing was actually hidden (verified:
79 files under `src/test/java`, 23 fakes/mothers/helpers, 56 ending in `Test.java`, 53 run and
3 `abstract` Testcontainers base classes correctly skipped, 132 tests all accounted for), but
the convention holding was undocumented and enforced only by habit.

Widened to three explicit patterns: `**/*Test.java`, `**/*Tests.java`, `**/*TestCase.java`.

Two alternatives rejected:

- **Deleting the `<includes>` block for Surefire's defaults.** Those also include
  `**/Test*.java`, which matches `security`'s `TestUsersUtils` — a helper, not a test. The
  explicit list closes the gap without scanning `Test`-prefixed helpers.
- **Adopting `maven-failsafe-plugin` for `*IT.java`.** There is no `*IT` class in the
  repository and no Failsafe; every Testcontainers test already runs under Surefire as an
  ordinary `*Test`. Building a second lifecycle for a file that does not exist is speculative
  infrastructure, which this project rejects elsewhere on the same grounds (Backlog item 5).

That leaves one honest residual: a class named `FooIT` still would not run. Rather than leave
it as a backlog entry, the rule is now written where someone naming a test will read it —
[TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md), under *Class names Surefire
will actually run*: the three patterns that work, why `Test*` and `*IT` are excluded, an
instruction to settle the Failsafe question **before** naming the first `*IT`, and the
corollary that helper types must not end in any of the three.

### E. `spring-data-commons` was declared for every module, contracts included

It sat in the root pom's `<dependencies>` — not `<dependencyManagement>` — so every module
inherited it at compile scope, including `basket-contract` and `catalog-contract`, which
[ARCHITECTURE.md](../../../docs/ARCHITECTURE.md) requires to stay framework-agnostic. Nothing
violated that in code, but the compiler would have accepted a breach.

It could not simply be deleted: `shared` uses `org.springframework.data.annotation.CreatedBy`
/ `CreatedDate` / `LastModifiedBy` / `LastModifiedDate` in its audit metadata and declared
nothing itself, relying on the inherited entry. Moved into `shared/pom.xml`, where it is used.

Verified that nothing else needed it directly: `ordering`, `basket-core`, `catalog-core`,
`outbox-jpa` and `outbox-mongo` each declare their own `spring-boot-starter-data-jpa` or
`-data-mongodb`; `starter` inherits Spring Data through the three module dependencies;
`security` does not reference Spring Data at all.

## Outcome (2026-09-10)

Applied as planned: six `<scope>test</scope>` additions, nothing else touched.

1. **Suite green.** `./mvnw clean verify` exit 0, all fifteen reactor modules SUCCESS,
   132 tests across 53 test classes, 0 failures, 0 errors, 0 skipped. No module lost a test
   type; nothing downstream needed a new declaration, as *Verified* point 5 predicted.
2. **Jar clean.** `starter.jar` 78,300,248 → 76,955,025 bytes, **1,345,223 bytes smaller**.
   All four artifacts from the step-0 baseline are gone, and a wider grep for the whole
   JUnit/Mockito/AssertJ/Hamcrest/XMLUnit/JSONassert/Awaitility/Testcontainers family finds
   nothing left under `BOOT-INF/lib/`.
3. **`dependency:analyze` no longer names any declared test library.** Neither `ordering`
   nor `basket-core` reports one under `Non-test scoped test only dependencies found`.
4. **The build now emits zero warnings**, down from two, and the coverage aggregate is intact
   after the packaging change: `jacoco:report-aggregate` analysed all ten bundles and wrote
   `sonar/target/site/jacoco-aggregate/jacoco.xml` (311 KB) with one directory per
   code-bearing module. No empty jar is produced by `sonar` or `modules` any more.
5. **`starter.jar` is byte-identical in size across the scope fix and fixes A–C**
   (76,955,025 both times), confirming the packaging and compiler-property changes altered the
   build without altering the deployable artifact.
6. **After D and E the artifact is 17 bytes larger** (76,955,042) and carries the same 123
   libraries. The difference is ordering, not content: `BOOT-INF/classpath.idx` still lists 123
   entries, with `spring-data-commons` now sequenced after `shared` and `spring-tx` because the
   declaration moved. All three Spring Data jars are still packaged, as they must be.
7. **Both contract modules are now free of Spring entirely.** `dependency:tree` on
   `basket-contract` returns one line — `org.projectlombok:lombok:provided`. Before E it also
   carried `spring-data-commons` at compile scope.
8. **All three verification runs agree:** 15/15 modules SUCCESS, 132 tests across 53 classes,
   0 failures, 10 Jacoco bundles, and — from fix A onward — zero build warnings.

### Correction to what Known Bug 10 recorded

That entry said there was **one** known false positive — `spring-boot-persistence` on
`catalog-core`. Measured after the fix, the warning still fires for **six** modules across
**fourteen** entries: `outbox-jpa`, `catalog-core`, `ordering`, `outbox-mongo`, `basket-core`
and `starter`. Not one is a declared test library — they are transitive compile dependencies
referenced only from test classes (`spring-beans`, `spring-context`,
`spring-boot-autoconfigure`, `mongodb-driver-sync`, `jackson-databind`, `spring-core`, and
for `starter` its own sibling modules `shared`/`basket-core`/`catalog-core`).

This matters for the deferred guard and is why it stayed deferred rather than being bundled
in here: `failOnWarning` would fail on all six modules today. A usable guard has to match on
the *declaration* rather than the resolved graph, or carry an explicit ignore list. Carried
into Backlog item 12 so it survives this entry's removal from
[KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md).
