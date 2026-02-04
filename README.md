## TODO:
- **AuditableEntityInterceptor**: In C# project, a SaveChangesInterceptor is taking care of saving Auditable entity information. This delegates to the "db" infrastructure. I think it makes sense for this to be part of the infrastructure, but it should be abstracted from the DB. I don't want to have to re-implement this should I need to change db. Also, what if I have multiple databases? => Should find a way to add an  "auditing abstraction layer". => done with AuditMetadata
- Add various level of validation:
  - Serialization validation (controller)
  - business validation (handler/domain)
- Add Security with keycloak

## Things To Investigate:

- actuator tests?
- HATEOAS: https://www.youtube.com/watch?v=k5S3qcCFDiQ


## DDD definitions:

- An **Aggregate** is a conceptual consistency boundary that models a business concept.
It groups entities and value objects that must remain consistent together.
- An **Aggregate Root** is a specific Entity chosen as the sole external entry point to the aggregate.
It has a global identity and authorizes all state changes of the aggregate.
- **Entities** may exist inside the aggregate and represent concepts with identity and lifecycle, but they have no external authority.
- **Value Objects** represent immutable values without identity and are replaced, not modified.
- **Domain:** The subject matter, rules, policies, and constraints of the business problem the software exists to solve.
- **Domain model**: A conceptual model that captures important business concepts, rules, and relationships — expressed in code.
  - Insight: The domain model is not a data model and not a technical model.
    It is a business meaning model.
- **Domain event**: Something meaningful that has happened in the domain and matters to the business.
- **Subdomains** are business divisions
- **Bounded Contexts** are software boundaries where a model applies.
  - Insight: domain model only makes sense within a bounded context.


### Domain other definition:
The domain is the part of the business problem that the software models, including its rules, language, and constraints. The domain model is a representation of that business knowledge in code. Domain events represent business‑meaningful facts that have occurred within that domain.


### Rule of thumbs:
- Ask yourself: If the business rule changes tomorrow, would this code need to change:
  - Yes => domain
  - No => application or infrastructure