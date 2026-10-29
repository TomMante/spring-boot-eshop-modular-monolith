# Kubernetes Sample Scope

Draft, 2026-10-29. The decision below was settled with the user on 2026-10-29. The user handed
step 1 to the assistant explicitly, and chose `k8s-samples/` at the repository root.

## Status
- [x] Step 1 — `Kubernetes/simple-deployment` replaced by `k8s-samples/`: a Deployment, a Service,
  a ConfigMap and a Secret, one file each, kept split the way the old folder was. Besides what the
  design below lists, a startup probe was added: the old probes started after 3 seconds, so a slow
  start would have had the pod restarted by its liveness probe. Checked as YAML only;
  `kubectl --dry-run=client` needs a reachable cluster, and no schema validator was run.
- [x] Step 2 — Docs: `k8s-samples/README.md`, the root README row, the SETUP.md settings rule, and
  the reference in `actuator-health-probes.md` turned into a permalink.

## Context

`Kubernetes/simple-deployment` holds a Deployment and a ConfigMap written before MongoDB,
Keycloak and the payment provider existed. It has drifted:
- the ConfigMap sets only `spring.datasource.*`, so MongoDB, the issuer and the payment provider
  fall back to the `localhost` defaults in `application.yaml`, which inside a pod is the pod
  itself;
- `imagePullPolicy: Always` on the local image `sb-eshop:test` fails unless the image is in a
  registry;
- `readOnlyRootFilesystem: true` with no writable `/tmp`, which the embedded Tomcat needs for its
  work directory.

The root README describes the folder as manifests "for running the application on a local
Minikube", which it can't do. Backlog 23 proposed fixing that by running the whole stack in the
cluster: StatefulSets for Postgres and MongoDB, Keycloak, a Job for the replica set, kustomize
overlays, and possibly the end-to-end suite against the cluster.

## Decision

**This repository keeps one sample deployment manifest, documented as not runnable as it is.
Running the stack on Kubernetes, and GitOps, belong to a separate DevOps project.**

Why:
- **The repository's subject is the modular monolith.** Its claims are about module isolation,
  the outbox, and enforced boundaries. A cluster setup shows deployment skill, not that design.
- **Running the full stack is already proven.** Compose builds the image from source and runs the
  application with every dependency, and GitHub Actions runs the end-to-end suite against that
  stack after every merge. The same stack as StatefulSets would repeat compose in YAML.
- **Unmaintained deployment files drift.** The current folder is the evidence: it stopped working
  as the application grew, and nothing noticed. A sample that states it isn't runnable can't
  silently stop running.
- **A separate project shows Kubernetes better.** There it is the subject, and it can use any
  workload image. The usual GitOps layout keeps deployment config apart from application code
  anyway.

What the sample is for: showing how this application goes into a deployment — the actuator
probes, the non-root and read-only security context, resource requests and limits, settings from a
ConfigMap and credentials from a Secret.

Rejected:
- **The full stack on Minikube** (Backlog 23). Reasons above.
- **Deleting the folder.** Loses the one place that shows how the application's probes and
  settings map onto a deployment, which is cheap to keep as a sample.
- **A published image from CI (GHCR)**, which a runnable cluster setup would need. Not needed for
  a sample, which names a placeholder image.

## Design

### The sample (step 1)

A Deployment, a Service, a ConfigMap and a Secret, one file each, as the old folder split them.
No kustomize: overlays only pay off with more than one environment.

- **A header comment** saying it is a sample: not applied anywhere, not tested, hosts and the
  image are placeholders. Point at the settings table in `docs/SETUP.md` as the list of settings
  an environment must provide.
- **Every setting the application needs, with placeholder values.** The rows of SETUP.md's
  settings table, so nothing falls back to a `localhost` default without anyone seeing it:
  - ConfigMap: `spring.datasource.url`, `spring.mongodb.uri`,
    `eshop.basket.payment.stripe.api-base`, `spring.security.oauth2.resourceserver.jwt.issuer-uri`,
    `spring.security.oauth2.resourceserver.jwt.jwk-set-uri`.
  - Secret: `spring.datasource.username`, `spring.datasource.password`,
    `eshop.basket.payment.stripe.api-key`. Placeholder values, never the compose ones.
  - Placeholder hosts that can't be mistaken for real ones, e.g. `postgres.example.internal`.
  - The `issuer-uri` / `jwk-set-uri` pair the same way compose uses it: the issuer is the public
    Keycloak URL that tokens carry, the key set the address the pod can reach.
- **Image:** a placeholder such as `registry.example.com/eshop-modular-monolith:1.0.0`, with
  `imagePullPolicy: IfNotPresent`.
- **Keep from the current Deployment:** the liveness and readiness probes on
  `/actuator/health/liveness` and `/actuator/health/readiness`, the `securityContext` (uid/gid
  1002 to match the image's `apiuser`, no privilege escalation, read-only root filesystem, all
  capabilities dropped), and the resource requests and limits.
- **Add:** an `emptyDir` mounted at `/tmp`, since the root filesystem is read-only; a ClusterIP
  Service on port 8080.
- **Settings as environment variables or as a mounted `application.yaml`:** either works. The
  current file mounts a ConfigMap as `application.yml` through `SPRING_CONFIG_IMPORT`; environment
  variables match how compose's `app` service does it, so the two read alike. Chosen by the user:
  the mounted `application.yaml`, as the most convenient way to configure a Spring application.
  The import is no longer `optional:`, so a missing mount fails at startup instead of falling
  back to the `localhost` defaults. Credentials stay in the Secret as environment variables.

### Docs (step 2)

- `k8s-samples/README.md`: what the sample shows, that it isn't runnable as it is and why,
  that the full stack runs with compose (link SETUP.md's *Run everything in Docker*).
- Root `README.md`, *Repository Structure*: replace the `Kubernetes/` row, which today claims the
  manifests run the application.
- `docs/SETUP.md`, under the settings table: a new setting that differs per environment also needs
  an entry in the sample.
- `actuator-health-probes.md` cites `Kubernetes/simple-deployment/backend/deployment.yaml`, lines
  50–63, as evidence. That file goes in step 1, so turn the reference into a permalink pinned to
  the last commit holding it.

## Steps and verification

Nothing under `src/` changes, so there is no red/green cycle.

1. **Sample manifest** (handed to the assistant).
   - Verify: the file parses as Kubernetes resources, e.g. `kubectl apply --dry-run=client -f`
     or `kubeconform`, and every row of SETUP.md's settings table appears in it.
2. **Docs** (assistant), once step 1 has landed, so they describe the file that exists.
   - Verify: relative links resolve; the README row no longer claims the manifest runs.

## Open questions

1. **A CI check on the sample.** A schema validation step (`kubeconform`) keeps the file
   well-formed as Kubernetes versions move. It doesn't catch a missing application setting; the
   SETUP.md rule does that, by convention only. Worth a workflow step for a sample, or not.

## Deferred

- The full stack on Kubernetes and GitOps: a separate DevOps project, not this repository.
  Backlog 23 is removed, and so are the Kustomize and Argo CD ideas in DESIGN_NOTES'
  *Directions Worth Exploring*: both are decided now, not open directions.
- Observability, chosen over Kubernetes as the next showcase for this repository: Backlog 27.
