# Development Notes & Guidelines

## Read Model Strategy
Currently the read model (query) is simply returning DTOs. This is more of a convenience, as DTOs are for data transfer, not read models.
Ideally, there should be a read-model which is linked to database views, or dedicated collection/tables.

In an ideal world, when writing from the Write model (command) the write will trigger a domain event, which will trigger a database view update/refresh.
This can be done with standard databases in 2 ways:
1. Simple view (no need for domain event trigger). Since a view is a convenience query no sync is required.
2. Materialized view: in this case, since data is persisted a sync needs to be done, there are a couple of ways for this:
   1. Domain Event Listener syncs the data whenever a relevant event is published. This approach is reactive. Meaning that it reduces the time between data change and view update.
   2. Scheduled Sync: A scheduled sync that happens once every x seconds/minute/hours. This approach is simpler, but not reactive, meaning more time between data change and view update.

## Validation Strategy
- Add various levels of validation:
  - Serialization validation (controller) (introduced, missing spots)
  - Business validation (handler/domain) (introduced, missing spots)

## Testing Strategy
- Unit tests use Given/When/Then in the method name. See [Testing Conventions](TESTING_CONVENTIONS.md) for the current template and the intended later shift to GWT-as-structure.
- Cucumber / `.feature` files are reserved for acceptance tests, if the project grows that far.
- See [Known Issues](KNOWN_ISSUES.md) for known defects and testing gaps.