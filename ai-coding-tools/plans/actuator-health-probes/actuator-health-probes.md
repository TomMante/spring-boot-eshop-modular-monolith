# actuator-health-probes

Closes **Known Bug 9** in [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md): the
Kubernetes liveness and readiness probes both `httpGet` `/actuator/health`, which the
security filter chain rejects with 401 because it permits only the springdoc paths and
requires authentication for `anyRequest()`. As written, the manifests could never pass
readiness and the pod would be killed by the liveness probe on a loop. Invisible today
only because nothing deploys them.

Scope is this one defect. Testing Gap 2 and Testing Gap 5 were considered alongside it and
deliberately left out; they stay in [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md)
unchanged, so nothing needs re-filing.

## Decision made here

Liveness and readiness use the **separate health groups**, not the aggregate
`/actuator/health`.

Reasoning: the aggregate endpoint includes the datastore health indicators, so a Postgres
or Mongo blip fails the *liveness* probe and Kubernetes restarts a pod whose only problem
is downstream — a restart loop that cannot fix the cause. The `liveness` group defaults to
`livenessState` alone and the `readiness` group to `readinessState` alone, which is the
correct split for a single-datastore deployment: liveness answers "is this process
broken", readiness answers "should traffic come here".

Deliberately **not** adding `db`/`mongo` to the readiness group. With one shared Postgres
and one shared Mongo, every replica would go unready simultaneously on a datastore blip,
turning a degraded service into a total outage. Revisit only if replicas ever have
independent datastore connectivity.

## Sequencing

**Strict red → green** for the security rule. The failing test is informative here: a
`permitAll` matcher is exactly the kind of rule that regresses silently, and watching it
401 first proves the test reaches the real filter chain rather than a permissive test
context. The configuration steps that follow (3 and 4) have nothing to assert beyond what
step 1 already covers.

### Step 1 — Red: assert the probe endpoints are reachable unauthenticated

In `starter/src/test/java/com/github/eshopmodularmonolith/ApplicationWiringTest.java`,
which already stands up the real assembled context. Add `@AutoConfigureMockMvc` to the
class, inject `MockMvc`, and assert an anonymous `GET /actuator/health/liveness` and
`GET /actuator/health/readiness` both return 200. Expect 401 before step 2.

The context is already paid for by this test, so the two assertions cost no extra
startup. `@SpringBootTest`'s default MOCK web environment auto-configures the actuator
endpoints, and the `issuer-uri` JWT decoder is supplier-backed so nothing contacts
Keycloak for an anonymous request to a permitted path.

If `readiness` proves timing-sensitive under MOCK (the group reports UP only once
`ApplicationReadyEvent` has fired), assert liveness for the 200 and assert readiness is
merely not 401 — the security rule is what is under test, not the health computation.

### Step 2 — Green: permit the health endpoint

`SecurityFilterChainConfig.filterChain`:

```java
.requestMatchers("/actuator/health/**").permitAll()
```

One matcher covers the aggregate and both groups: under `PathPatternParser`, `/**`
matches zero or more segments. Everything else under `/actuator` stays authenticated,
which is the intent.

The typed `EndpointRequest.to(HealthEndpoint.class)` form was considered and rejected:
`security/pom.xml` carries no actuator dependency, and adding one to import a matcher
widens that module's surface for no behavioural gain.

Unauthenticated exposure is acceptable because `management.endpoint.health.show-details`
defaults to `never` — an anonymous caller sees `{"status":"UP"}` and no component
breakdown.

### Step 3 — State the management surface

`starter/src/main/resources/application.yaml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      probes:
        enabled: true
```

Neither line changes current behaviour — `include` already defaults to `health`, and
Spring Boot 4 enables the probe groups automatically. Both are written down so the
exposed surface is a stated decision rather than a default that a future upgrade can
move, which is what the Known Issue asked for.

### Step 4 — Point the manifests at the groups

[`Kubernetes/simple-deployment/backend/deployment.yaml`, lines 50–63](https://github.com/TomMante/SpringBoot-EshopModularMonolith/blob/812015648e96064c198c4564f256ef8128114322/Kubernetes/simple-deployment/backend/deployment.yaml#L50-L63):

- `livenessProbe.httpGet.path` → `/actuator/health/liveness`
- `readinessProbe.httpGet.path` → `/actuator/health/readiness`

## Not in scope

- **Graceful shutdown** (`server.shutdown=graceful`), the natural companion to a readiness
  probe. Separate concern, and nothing currently claims it.
- **Any actuator endpoint beyond health.** `info` was considered and left out: nothing
  populates it, so exposing it would publish an empty object and invite the assumption
  that build metadata is wired up.

## Close-out

- Remove Known Bug 9 from [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md).
- [README.md](../../../README.md) and [SETUP.md](../../../docs/SETUP.md) were checked while
  writing this plan: neither mentions actuator or the probe paths, so no documentation
  change is owed.
