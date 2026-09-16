# Order Endpoints Scoped to Customer

## Context

[Known Bug 12](../../../docs/KNOWN_ISSUES.md): `SecurityFilterChainConfig` only requires a valid token, and none of ordering's three endpoints takes the current user. `GET /orders` pages through every customer's orders, `GET /order/{id}` returns any order, and `DELETE /order/{id}` hard-deletes any order and answers `204` whether or not it existed. The payload carries shipping and billing addresses and, through Known Bug 4, full card data.

Everything the fix needs already exists. `Order.customer` is set by `BasketCheckoutIntegrationEventListener` from `event.userName()`, which basket takes from the JWT's `preferred_username`. `CurrentUserArgumentResolver` (security) reads that same claim, and every basket controller already takes `@AuthenticatedUser CurrentUser` (e.g. `GetBasketController` → `GetBasketQuery(username)`).

Intended outcome: every order endpoint acts only on the caller's own orders, and another customer's order is indistinguishable from one that does not exist.

Found during planning:
- **The page count leaks too.** `MongoOrderQueryRepository.paginatedOrders` counts with `new Query()` — the whole collection. Scoping only the `find` would still expose how many orders exist across all customers through `totalItems`/`totalPages`.
- **`OrderDB.customer` has no index**, so a scoped list would scan the collection.
- **Ordering cannot test an authenticated request.** Its pom has no `security` dependency on the test classpath, so its controller tests run with no filter chain and no `CurrentUserArgumentResolver`. A controller taking `@AuthenticatedUser CurrentUser` needs the same three test-scope dependencies `basket-core/pom.xml` declares.
- **Checked, not a concern here:** ownership keys on `preferred_username` rather than the immutable `sub`. The realm export sets `"editUsernameAllowed": false`, so a user cannot rename themselves into or out of someone's orders. Basket keys on the same claim.

## Design decisions

- **Both reads are scoped to the caller.** *(User decision, 2026-09-15.)* Back-office access — a role that can see across customers — is a separate feature, recorded in the Backlog at close-out rather than built here.
- **"Not yours" is "not found".** *(User decision, 2026-09-15.)* `404` with the existing `OrderNotFound` / `order.not-found`, so the response never confirms that an id exists.
- **`DELETE /order/{id}` stays, scoped to the caller's own orders, and stays a hard delete.** *(User decision, 2026-09-15; the recommendation had been removal.)* Known consequence, accepted: deleting an order removes its `checkoutId` from `orders_checkoutId_unique`, so a redelivery of that checkout arriving afterwards places the order again. Redelivery is confined to the outbox retry window (a failed `markAsProcessed`, or two instances polling), so the window is narrow — but real, and recorded as a Known Issue at close-out. Soft delete would keep the index intact and was rejected as larger scope (state on the aggregate plus an update path). An id that is missing or belongs to someone else answers `404` *(user decision, 2026-09-15)*, the same as `GET /order/{id}`, replacing today's `204` regardless. A repeated delete of one's own order therefore also answers `404`. `GetOrderByIdErrorHandler` is scoped to its own package, so delete gets its own `DeleteOrderErrorHandler`, following the per-feature pattern.
- **The read port offers only viewer-scoped methods, and the viewer is `CurrentUser`.** *(User decision, 2026-09-16.)* `OrderQueryRepository` becomes `ordersVisibleTo(CurrentUser viewer, PageRequest)` and `findVisibleTo(CurrentUser viewer, UUID id)`; the unscoped `findById` goes, so a handler that forgets the viewer does not compile — the bug being fixed here is a forgotten check. Enforcement sits in the Mongo query for both reads; the list has no alternative anyway, since filtering after the fact makes pagination and count wrong. `CurrentUser` is the existing `shared.contract.identity` record the resolver already produces for basket's controllers — deliberately **not** a second identity type, which would leave two names for one concept.
- **`visibleTo` names a policy, and that is the point.** It leaves room for back-office access without a second port: `CurrentUser` gains roles later (populated from `realm_access.roles` in `CurrentUserArgumentResolver`), and "back-office sees every order" becomes one branch. Known cost, accepted now: that branch will live in `MongoOrderQueryRepository` **and** in `FakeOrderQueryRepository`, i.e. authorization policy in infrastructure, in two implementations that can drift — Backlog 11 is a live example of that drift. Harmless today, since both simply filter by the viewer's username and there is no branch. The alternative, recorded in DESIGN_NOTES at close-out rather than built: the handler computes an `OrderVisibility` value (`OnlyPlacedBy` / `AllOrders`) and the adapters only translate it, keeping policy in the application layer.
- **Queries and the delete command carry `CurrentUser`, not a bare username.** A deliberate step away from basket's `GetBasketQuery(String username)`: these records feed a port that now takes the viewer, and unwrapping to a string and re-wrapping would lose the roles the seam exists for.
- **Ownership stays explicit; no infrastructure-level filter.** *(User decision, 2026-09-16.)* The alternatives — Postgres row-level security (wrong engine: orders are in Mongo), a Hibernate filter (JPA only), an ambient current-user read inside the Mongo adapter, a shared owned-resource type with a common query fragment, or a policy engine with data filtering (Keycloak Authorization Services, OPA, OpenFGA) — all move ownership into infrastructure. Against, here: orders are the only owned resource, since basket is keyed by username as its `@Id` and catalog has no owner; ownership is a business rule, not plumbing; and an ambient filter would need a `SecurityContext` in `MongoOrderQueryRepositoryTest` and an imitation of it in the fake. (An ambient filter on the *read* port alone would need no bypass today — both callers are HTTP handlers — but back-office access, already scheduled, is the first case that would.) A scoped-only port gives the "cannot be forgotten" property without the implicitness. A guard test asserting every read-port method takes a customer was considered and left out: the port has two methods, and ArchUnit-style rules are Backlog 9's subject.
- **Delete keeps its ownership check in the handler.** `OrderRepository` is the aggregate's port — it loads by identity, and `DeleteOrderHandler` is its only customer-facing caller. The check-then-act gap is harmless: an order's `customer` never changes after placement, and there is no update path.
- **One compound index matching the list query**: `{customer: 1, placedAt: -1, _id: 1}`, the filter plus `GetOrdersHandler`'s sort. Declared with `@CompoundIndex` on `OrderDB`; created by the existing `MongoOrderRepository.createIndexes()`, which already resolves every annotated index on `OrderDB`. Non-unique, so existing local data cannot fail its creation.

Sketch of the shapes (guidance, not a diff):

```java
// GetOrdersController
@GetMapping("/orders")
public ResponseEntity<GetOrdersResponse> getPaginatedOrders(@AuthenticatedUser CurrentUser currentUser,
                                                            @ModelAttribute PaginationRequest request) {
    var result = handler.handle(new GetOrdersQuery(currentUser, request.pageIndex(), request.pageSize()));
    ...
}
```

```java
// OrderQueryRepository — no unscoped read remains
PageResult<OrderDTO> ordersVisibleTo(CurrentUser viewer, PageRequest pageRequest);
Optional<OrderDTO> findVisibleTo(CurrentUser viewer, UUID id);

// MongoOrderQueryRepository — one criteria for both the page and the count.
// When roles arrive, this is the single line that branches for back-office.
var visibleToViewer = Criteria.where("customer").is(viewer.username());
var pageQuery = new Query(visibleToViewer).skip(...).limit(...).with(sort);
var count = template.count(new Query(visibleToViewer), OrderDB.class);
```

```java
// GetOrderByIdHandler
var orderDto = repository.findVisibleTo(query.viewer(), query.id())
        .orElseThrow(() -> new OrderNotFound(query.id()));
```

```java
// DeleteOrderHandler
var order = repository.findById(cmd.id())
        .filter(stored -> stored.customer().equals(cmd.requester().username()))
        .orElseThrow(() -> new OrderNotFound(cmd.id()));
repository.delete(order.id());
```

```java
// OrderDB
@CompoundIndex(name = "orders_customer_placedAt_id", def = "{'customer': 1, 'placedAt': -1, '_id': 1}")
```

## Testing rigor

- **Step 1 (controller tests authenticate)**: no red-first — test plumbing with no behavior change. Existing suite green is the check.
- **Step 2 (list scoped)**: strict red → green at `MongoOrderQueryRepositoryTest` — enforcement lives in the Mongo query, which only real Mongo proves, count included. Strict red → green at `GetOrdersHandlerTest` against the fake. Controller change is tests-alongside.
- **Step 3 (by id scoped)**: strict red → green at `MongoOrderQueryRepositoryTest`, where the filter now lives, and at `GetOrderByIdHandlerTest` against the fake. Both reds are real: another customer's order is returned today. Controller change is tests-alongside. *(2026-09-16: strict red skipped at the adapter — same mechanism as step 2, so the red would have taught nothing; the test's own positive control covers the vacuous-pass risk instead. Kept strict at the handler.)*
- **Step 4 (delete scoped)**: strict red → green at `DeleteOrderHandlerTest`. Both reds are real: another customer's order is deleted today, and a missing id is a silent no-op. Controller change and error mapping are tests-alongside.
- **Step 5 (index)**: no test of its own — an index has no behavior. `ApplicationWiringTest` and `MongoOrderRepositoryTest` run `createIndexes()`, so a malformed definition fails the suite.

Controller tests keep their current shape (mocked handler) but stub it with the query carrying the **authenticated** user: if the controller passes anything else, the stub misses and the response assertion fails — no `verify`.

## Todo List

### 1. Ordering's controller tests run behind security
- [x] `modules/ordering/pom.xml`: add `security`, `security` `test-jar`, and `spring-boot-starter-security-oauth2-resource-server-test`, all at **test** scope — copy `modules/basket/basket-core/pom.xml` lines 84–101. (Backlog 12 is exactly the mistake of leaving the scope off.)
- [x] `GetOrdersControllerTest`, `GetOrderByIdControllerTest`, `DeleteOrderControllerTest`: `@Import(SecurityTestConfig.class)` on the inner `Config`, and `.with(user("john_doe"))` on each request (`TestUsersUtils.user`). *(2026-09-16: also corrected a copy-pasted test name in `GetOrdersControllerTest` — it said "create product endpoint".)*
- [x] Full suite green. *(Confirmed by the user, 2026-09-16.)*

### 2. `GET /orders` returns only the caller's orders
- [x] `OrderMother.Builder.withCustomer(String)`. *(Also added `OrderMother.instance()` returning a `Builder`.)*
- [x] `OrderQueryRepository.paginatedOrders` → `ordersVisibleTo(CurrentUser viewer, PageRequest)`; adapter and fake accept the viewer and still ignore it, so the next tests compile and fail.
- [x] Red: `MongoOrderQueryRepositoryTest` — seed orders for two customers with **different** counts and interleaved `placedAt` (e.g. 3 for `john_doe`, 2 for `jane_roe`); a page for `john_doe` holds only his orders, and `totalItems`/`totalPages` count only his. Existing nested tests pass the seeded customer. *(Landed as the `PaginatedOrdersVisibleForCustomer` nested class, seeding inside the test so neither existing fixture leaks in; asserts page 0, page 1, and both totals.)*
- [x] Green: one `Criteria.where("customer").is(viewer.username())` for both the page query and the count.
- [x] Red: `GetOrdersHandlerTest` — same two-customer shape against the fake. Green: `FakeOrderQueryRepository` filters by the viewer's username before sorting and counting. *(Red confirmed by the user.)*
- [x] `GetOrdersQuery(CurrentUser viewer, int pageIndex, int pageSize)`; `GetOrdersController` takes `@AuthenticatedUser CurrentUser` and passes it through; `GetOrdersControllerTest` stubs the handler with the authenticated viewer.
- [x] Full suite green. *(Confirmed by the user, 2026-09-16.)*
- **Caught in review:** the handler and controller each passed `null` as the viewer at an intermediate commit, which the fake tolerated and no test covered — `GET /orders` would have thrown an NPE against Mongo. Fixed by wiring the viewer through in the same step; steps 3 and 4 wire the controller in the same pass for that reason.

### 3. `GET /order/{id}` returns only the caller's order
- [x] `GetOrderByIdQuery(CurrentUser viewer, UUID id)`; existing tests pass a viewer with the stored order's customer.
- [x] `OrderQueryRepository.findById` → `findVisibleTo(CurrentUser viewer, UUID id)`; adapter and fake accept the viewer and still ignore it, so the next tests compile and fail. Removing the unscoped method is the point of the step — nothing outside the port may call it.
- [x] Red: `MongoOrderQueryRepositoryTest` — an order placed by another customer, fetched as `john_doe` → empty; fetched as its own customer → present. *(2026-09-16: written as a plain test beside the existing `findById` one rather than a nested class — no shared fixture to hold. **Strict red was deliberately skipped**: the same criteria mechanism had just been built in step 2, so watching it fail would not have changed the code. The test carries its own positive control — the present-for-the-right-viewer assertion — so it cannot pass vacuously, which is what the red would otherwise have proved.)*
- [x] Green: add `Criteria.where("customer").is(viewer.username())` to the id lookup (`template.findOne`, since `findById` takes no criteria).
- [x] Red: `GetOrderByIdHandlerTest` — same two-customer shape against the fake → `OrderNotFound`. Green: `FakeOrderQueryRepository.findVisibleTo` compares `customer` to the viewer. *(Caught in review: the first version of this test used a random id with nothing stored, duplicating the "not present" case, and passed with or without the fake's filter. Replaced with a stored order placed by another customer.)*
- [x] `GetOrderByIdController` takes `@AuthenticatedUser CurrentUser`; `GetOrderByIdControllerTest` stubs with the authenticated user (both the 200 and 404 cases).
- [x] Full suite green. *(Confirmed by the user, 2026-09-16.)*

### 4. `DELETE /order/{id}` acts only on the caller's order
- [x] `DeleteOrderCommand(CurrentUser requester, UUID id)`; the existing handler test passes a requester with the stored order's customer.
- [x] Red: `DeleteOrderHandlerTest` — an order placed by `jane_roe`, deleted by `john_doe` → `OrderNotFound`, **and** the order is still in `FakeOrderRepository`.
- [x] Red: `DeleteOrderHandlerTest` — a random id → `OrderNotFound`. *(Pins the `204` → `404` contract change for a delete that finds nothing.)*
- [x] Green: load, filter on `customer`, `orElseThrow`, then delete.
- [x] `DeleteOrderErrorHandler` (`@RestControllerAdvice(basePackages = "...deleteorder")`) maps `OrderNotFound` to `ApiErrors.notFound("order.not-found", ...)`.
- [x] `DeleteOrderController` takes `@AuthenticatedUser CurrentUser`. `DeleteOrderControllerTest`: `@Import` the new error handler; add a `404` case that stubs `doThrow(new OrderNotFound(id)).when(handler).handle(new DeleteOrderCommand(new CurrentUser("john_doe"), id))` — it answers `404` only if the controller passed the authenticated user. *(The `204` case still carries its original `verify`; see the open item below.)*
- [x] `DeleteOrderControllerTest`'s `204` case keeps `verify(handler, times(1)).handle(cmd)`. *(User decision, 2026-09-16: kept deliberately. The `404` case already pins the command contents through an outcome, so the `verify` is redundant and couples the test to the collaboration — the argument [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md) makes against it. Kept anyway because the happy path otherwise has no positive assertion that the handler was reached at all: a controller returning `204` unconditionally would pass without it.)*
- [x] Full suite green. *(Confirmed by the user, 2026-09-16.)*

### 5. Index for the scoped list
- [x] `@CompoundIndex(name = "orders_customer_placedAt_id", def = "{'customer': 1, 'placedAt': -1, '_id': 1}")` on `OrderDB`.
- [x] Full suite green, including `ApplicationWiringTest`. *(Confirmed by the user, 2026-09-16.)*

### 6. A second user in the realm export
*(Added 2026-09-16, user request: a seeded second user makes this manual pass — and every future multi-user feature — repeatable instead of hand-made in the admin console each time.)*
- [x] `docker/sb-eshop/keycloak-init/eshop-realm.json`: add a second entry to `users` beside `john` — fresh UUID `id`, `username` `jane`, name and email, `emailVerified` and `enabled` true, a non-temporary `password` credential, and `realmRoles: ["default-roles-eshop"]`, matching john's shape exactly. *(Done 2026-09-16; JSON re-parsed and both user ids confirmed unique.)*
- [x] **Re-import is not automatic.** `--import-realm` skips a realm that already exists, and Keycloak's data lives in Postgres' `identity` schema. No named volumes are declared in `compose.yaml`, so `docker compose down && docker compose up -d` drops the realm and re-imports it — which also wipes catalog, basket and order data (Flyway re-seeds catalog on the next app start). *(For the user to run as part of step 7.)*
- [x] [SETUP.md](../../../docs/SETUP.md): list both users in the credentials table (step 4), and note that the token request's `-d username=` selects which one. Also update the "Keycloak administration" line that says the `john` user is created by `--import-realm`. *(Done 2026-09-16; the re-import caveat above is documented there too, so it is not only in this frozen plan.)*
- [x] No automated test: the realm export is not on any test classpath, and module tests mint their own JWTs through `TestUsersUtils`. The check is getting a token for the new user. *(Confirmed by the user in the step 7 pass, 2026-09-16.)*
- [x] Bruno: `environments/default.bru` renamed to `default - john.bru` (via `git mv`, so history follows) and a `default - jane.bru` added, identical apart from `username`. [bruno/README.md](../../../bruno/README.md) and [SETUP.md](../../../docs/SETUP.md) name the new environments. *(Added 2026-09-16, user request. The README also still described `POST /order` and a **Create Order** request, both removed by [one-order-per-checkout](../one-order-per-checkout/one-order-per-checkout.md); corrected in the same pass.)*

### 7. Manual pass
- [x] Two users available from the realm export (step 6) — no admin-console setup.
- [x] Check out one basket as each user. Each user's `GET /orders` shows only their order, with `totalItems` 1; `GET /order/{other user's id}` → `404`.
- [x] `DELETE /order/{other user's id}` → `404`, and the other user still sees their order. `DELETE` of one's own order → `204`, then `GET /orders` no longer lists it; repeating the delete → `404`.
- [x] Bruno requests under `bruno/Ordering/` still work unchanged (paths are the same).
- *(Whole pass confirmed by the user, 2026-09-16.)*

## Verification
- [x] `./mvnw clean verify` — full suite green (run by the user; the assistant's shell forks the wrong JDK). *(Confirmed by the user, 2026-09-16.)*
- [x] *(2026-09-16: Known Bug 12 removed; added as Known Bug 13.)* [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md): remove Known Bug 12. Add a Known Bug: deleting an order drops its `checkoutId` from the unique index, so a later redelivery of that checkout places the order again — self-contained, naming the retry window and soft delete as the fix shape.
- [x] *(2026-09-16: added as item 13. One deviation: the entry describes the `OrderVisibility` alternative inline instead of linking to DESIGN_NOTES, because DESIGN_NOTES.md forbids Backlog and Known Issues from pointing back to it.)* [BACKLOG.md](../../../docs/BACKLOG.md): add item 13, back-office access to orders — no role exists; the Keycloak-vs-application roles question (README roadmap) is unmade; what a back-office needs across customers (list, read, delete or cancel); customer endpoints are scoped since this plan. Record the seam this plan leaves: the read port takes `CurrentUser`, so the work is roles on that record (from `realm_access.roles` in `CurrentUserArgumentResolver`) plus one branch in the Mongo adapter and the fake — and note that the branch puts policy in infrastructure in two places, with `OrderVisibility` (DESIGN_NOTES) as the alternative if that starts to hurt.
- [x] [TESTING_CONVENTIONS.md](../../../docs/TESTING_CONVENTIONS.md): add a row for order ownership — list enforcement at `MongoOrderQueryRepositoryTest`, single-order checks at the handlers.
- [x] [DESIGN_NOTES.md](../../../docs/DESIGN_NOTES.md): add "Ownership Filtering as Infrastructure" — an option, not a decision. Content:
    - **The shape to grow into:** a shared owned-resource type in `shared.infrastructure` carrying an `owner` field, with a common query fragment appended to each query — the same pattern `JpaAuditMetadata` already follows for audit fields (`ProductDB` attaches it with `@Embedded`).
    - **It must be composition, not inheritance.** `OrderDB` and `ShoppingCartDB` are records, and a record cannot extend a class. An embedded component, or an interface (`interface OwnedResource { String owner(); }`) a record can implement.
    - **The shared field is the cheap half.** Appending the filter does not generalize across the two engines: Hibernate's `@Filter` rewrites JPA queries automatically, while Spring Data Mongo has no equivalent hook, leaving either a helper each query must remember to call — the forgetting problem again — or a custom repository base class. One shared field, two unrelated enforcement mechanisms.
    - **`owner` is not `customer`.** The aggregate's `customer` is domain vocabulary (who placed the order); `owner` is access control (whose data this is). Equal today, free to diverge (an agent placing an order for a customer, a household account). Do not collapse them in the domain.
    - **Cost:** a shared kernel across modules is the coupling modules otherwise avoid — consistency in exchange for changes rippling everywhere at once.
    - **Triggers:** a second module with owned data, or ownership rules that stop meaning "equals the caller" (sharing, roles, delegated access). Multi-tenancy is a different problem with a different answer — push it down to RLS or `@TenantId` rather than hand-roll it.
    - **`OrderVisibility` as the policy-placement alternative:** with the viewer in the port, the back-office branch lands in each adapter and the fake. A sealed `OrderVisibility` (`OnlyPlacedBy` / `AllOrders`) computed by the handler keeps the decision in the application layer and leaves adapters translating a data description. Not built now — there is no branch to place yet.
- [x] [SETUP.md](../../../docs/SETUP.md): the `GET /orders` step still reads correctly; add that it shows only the caller's orders.
- [x] [README.md](../../../README.md): recheck — no endpoint list today. *(Rechecked 2026-09-16: no mention of orders or endpoints. Live docs also searched for the renamed port methods and the old Bruno environment name — nothing stale.)*
- [x] Route every *Explicitly deferred* entry *(2026-09-16)*:
    - **Back-office access to orders** → [BACKLOG.md](../../../docs/BACKLOG.md) item 13.
    - **A deleted order's checkout can be placed again** → [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) Known Bug 13.
    - **Ownership filtering as infrastructure**, including the shared owned-resource type and `OrderVisibility` → [DESIGN_NOTES.md](../../../docs/DESIGN_NOTES.md), "Ownership Filtering as Infrastructure".

## Explicitly deferred
- **Back-office access to orders** → Backlog item 13 at close-out.
- **A deleted order's checkout can be placed again** → Known Issues at close-out (see Verification).
- **Ownership filtering as infrastructure**, including the shared owned-resource type → DESIGN_NOTES at close-out, with its constraints and triggers (see Verification).
