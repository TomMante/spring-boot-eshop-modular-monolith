# Kubernetes Sample

**A sample, not a deployment.** These manifests show how the application goes into a Kubernetes
deployment. They are not applied anywhere and not tested: the hosts, the image and the
credentials are placeholders, and the datastores and Keycloak they point at aren't included.

To run the whole stack, use compose: [Run everything in Docker](../docs/SETUP.md#run-everything-in-docker).

## What it shows

| File | Contents |
| --- | --- |
| [deployment.yaml](deployment.yaml) | The application container: startup, liveness and readiness probes on the actuator's probe groups; a non-root, read-only security context with all capabilities dropped and a writable `/tmp`; resource requests and limits; the ConfigMap mounted as a file that Spring imports through `SPRING_CONFIG_IMPORT`, and the Secret as environment variables. |
| [configmap.yaml](configmap.yaml) | An `application.yaml` with every setting that differs per environment: Postgres, MongoDB, the payment provider, and the token issuer with its key set. It overrides the same keys in the application's packaged `application.yaml`. |
| [secret.yaml](secret.yaml) | The database credentials and the payment provider's API key, as environment variables, which take precedence over any config file. |
| [service.yaml](service.yaml) | A ClusterIP Service on port 8080. |

## Before it could run

- An image built from [the Dockerfile](../SpringBoot-EshopModularMonolith/Dockerfile) and pushed
  to a registry the cluster pulls from.
- PostgreSQL with the `EShopDb` database, MongoDB running as a replica set, and Keycloak with the
  `eshop` realm, all reachable from the pod.
- Real values in the ConfigMap, and credentials provided by the cluster's secret management
  rather than by [secret.yaml](secret.yaml).
- Keycloak's public hostname fixed, so every token carries the issuer in `ISSUERURI` whichever
  address it was requested on. Compose does this with `KC_HOSTNAME`; the settings table in
  [SETUP.md](../docs/SETUP.md#run-everything-in-docker) explains why.

The settings are the same ones compose's `app` service overrides. A new setting that differs per
environment goes in [SETUP.md](../docs/SETUP.md#run-everything-in-docker)'s table, on the `app`
service, and in [configmap.yaml](configmap.yaml) or [secret.yaml](secret.yaml).
