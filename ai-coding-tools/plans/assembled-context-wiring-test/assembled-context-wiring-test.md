# Assembled Context Wiring Test

Closes Testing Gap 5 in [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md): nothing loads the real module configurations, so the wiring the running application uses is exercised by no test.

**Status:** complete. Full suite green (with `ApplicationWiringTest` active). All five implementation steps and verification done; no findings needed routing to Known Issues or Backlog.

## Context

Every test in this suite builds its own `@Configuration`. Three configuration failures have reached a manual application run rather than a test — a bare `@Transactional` that could not resolve among three `TransactionManager` beans, an untested `DomainEventRegistry` contribution that poisoned the outbox at runtime, and catalog slice configs whose bean names drifted from production's.

### Loading the context is not enough

The obvious answer — one `@SpringBootTest` asserting the context loads — would have caught **none** of those three:

- `TransactionInterceptor` resolves the transaction manager on the *first invocation* of a transactional method, not at wiring time. The context starts clean.
- `@PostConstruct` registers whatever key it is handed. Misspelling or removing it still loads.
- Slice configs drifting from production is the *tests* being wrong; a test of production config cannot see it in that direction.

So loading is the floor. What closes the gap is loading the real context **and then exercising it**.

### Why this is an integration test and not end-to-end

The subject is assembly, not behaviour. End-to-end would catch these only as a side effect and report them as a 500 or a missing order — starting a debugging session instead of naming the ambiguous bean. It is also expensive here: Keycloak, a token, and an inherent multi-second wait on `BasketOutboxDispatcher`'s `fixedDelay`. A separate end-to-end test is worth having eventually, outside the normal test run, to answer whether the flow works for a user. It should never be what tells us a bean is ambiguous.

### Decisions settled while planning

- **Lives in `starter`.** It is the only module that owns `EshopModularMonolithStarter` and depends on all three module configs plus `security`. `starter/src/test/` does not exist yet.
- **Runs in `mvn test` with the other Testcontainers tests.** There are already six; this is not a new category. If suite time becomes a problem, the answer is moving all seven to failsafe — a decision about the set, not about this test.
- **Keycloak is not needed.** `issuer-uri` produces a `SupplierJwtDecoder` that defers OIDC discovery to the first token validation, so no token is ever validated during the test. To be confirmed in step 2; if wrong, override with a `jwk-set-uri` property rather than starting a container.
- **`@ServiceConnection` will not wire Mongo.** `MongoConfiguration` reads a non-standard `spring.mongodb.uri` with a silent `mongodb://localhost/test` default, which is not the property `@ServiceConnection` sets. Needs `@DynamicPropertySource`.
- **Catalog write only, for now.** The basket path is what forces a Mongo replica set; see Deferred.

## Todo List

### 1. Test dependencies — done
*Mechanical, mirrors four existing poms — no test of its own.*

- [x] `starter/pom.xml`: add `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`, `testcontainers-mongodb`, all test scope. Versions come from the same BOM the module poms use
- [x] Create `starter/src/test/java/com/github/eshopmodularmonolith/`

### 2. The context loads — done
*Tests-alongside. There is no interface to discover and no hypothesis worth watching fail — "the application starts" teaches nothing red-first.*

- [x] `ApplicationWiringTest`: `@SpringBootTest` + `@Testcontainers`, a `PostgreSQLContainer` with `@ServiceConnection`, a `MongoDBContainer`, and a `@DynamicPropertySource` supplying `spring.mongodb.uri`
- [x] Confirmed no Keycloak is required — no container, no property override, context loads clean
- [x] No surprises around Flyway ordering against `ddl-auto: validate` or `@EnableJpaAuditing` with a custom `catalogEmf` — clean pass first attempt; nothing to route to Known Issues or Backlog from this step

### 3. Each module contributes its domain events — done
*Strict red → green. This is the regression that poisoned the outbox, and it is worth seeing the test catch it.*

- [x] Red: commented out the `registerDomainEvent` line in `BasketModuleConfig.postConstruct()` — failed on `IllegalArgumentException: Unknown event type: BasketCheckedOut`, the exact production signal, not a plain assertion mismatch. Restored; `BasketModuleConfig` diffs clean against HEAD, no residue
- [x] Registry assertion is its own test (`givenAllModulesAssembled_whenResolvingDomainEvents_thenEachModuleContributedItsClass`), separate from the context-load test — a failure here won't be confused with a failure there
- [x] Asserts `DomainEventRegistry` resolves both `"ProductCreated"` and `"BasketCheckedOut"` to their classes

### 4. A transactional write resolves its manager — done
*Strict red → green — reproducing a defect that has already bitten once is exactly the case where watching it fail is the point.*

- [x] Red: temporarily stripped `JpaProductRepository.save` back to a bare `@Transactional` — failed with `NoUniqueBeanDefinitionException`, the exact ambiguity seen manually against three unqualified `TransactionManager` beans. Qualifier restored; `JpaProductRepository` diffs clean against HEAD, no residue
- [x] Green: `givenAllModulesAssembled_whenCreatingAProduct_thenTheWriteCommits` invokes `CreateProductHandler.handle(...)` through the assembled context and reads the product back via `ProductRepository.findProductById`

### 5. Measure the cost — done
- [x] Timed `mvn test` before (53s) and after (60s) — a ~7s/13% increase for two containers plus a real transactional write. Reasonable; no case for moving any test to a failsafe tier

## Verification

- [x] `mvn test -f SpringBoot-EshopModularMonolith/pom.xml` — full suite green
- [x] Testing Gap 5 removed from [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md); old Gaps 6–7 renumbered to 5–6, and the one cross-reference into that range ([BACKLOG.md](../../../docs/BACKLOG.md) item 3) updated to match. Gap 3 (`BasketCheckedOutHandler` / `ProductCreatedHandler` untested) re-read: left as-is — the new test triggers `ProductCreatedHandler` incidentally via `CreateProductHandler`'s in-process publish, but asserts nothing about it, so Gap 3's actual claim still holds
- [x] No findings from steps 2 or 4 to route — both landed clean on the first real attempt (step 2's note already says so); nothing new for Known Issues or Backlog
- [x] Checked [README.md](../../../README.md) — nothing to change; it doesn't describe test structure and this adds no tool or endpoint

## Explicitly deferred

- **The basket transactional write.** `CheckoutBasketHandler` is `@Transactional("basketTransactionManager")` and Mongo transactions require `withReplicaSet()`. Worth adding once the catalog path is green, as its own small step, rather than paying the replica-set startup before anything works.
- **`spring.mongodb.uri` is a non-standard property with a silent localhost default,** set nowhere in `application.yaml`. A misconfigured deployment connects to nothing and fails late. Surfaced here, not fixed here — decide whether it is a Known Issue on its own.
- **An end-to-end test.** Different question, different tier, outside `mvn test`. Bruno covers it manually today and caught the real bugs last increment.
- **The reverse drift.** This test cannot see slice configs diverging from production, which is one of the three original failures. Nothing here fixes that; a future consolidation of test configuration would.
