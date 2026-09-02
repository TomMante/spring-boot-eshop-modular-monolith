# Design Notes

Options and open questions being thought about. **Nothing here is a decision or a commitment** — some of it may never be worth doing, and saying so is part of the point.

Where things go when they stop being ideas:

- becomes a decision, and the work is real → [BACKLOG.md](BACKLOG.md)
- turns out to describe something broken or unverified → [KNOWN_ISSUES.md](KNOWN_ISSUES.md)
- becomes how this project actually works → [ARCHITECTURE.md](ARCHITECTURE.md)

Neither of the first two should ever point back here. This file exists so speculation has a home that isn't the backlog — a backlog holding things nobody committed to stops being read.

## Read Model Strategy

The query side currently returns Contract DTOs straight from the database (see [ARCHITECTURE.md](ARCHITECTURE.md) for the CQRS split as built). DTOs are a data-transfer shape rather than a read model, so this is a convenience: it works, and it leaves no place to put a projection that differs from the write model's shape.

A fuller separation would give the read side its own persisted or derived shape, driven off the write side:

1. **Plain view.** No synchronization needed — a view is a convenience query, always current by construction.
2. **Materialized view.** Data is persisted, so it needs a sync strategy:
    1. *Domain event listener* — refreshes whenever a relevant event is published. Reactive; minimizes the window between a write and the read model reflecting it.
    2. *Scheduled sync* — refreshes every N seconds/minutes/hours. Simpler, but the read model is stale for that interval.

**Open question, and the reason this stays a note:** whether a study project of this size ever justifies it. The current approach costs nothing and has not yet been the thing that hurt. Worth revisiting if a read ever needs a shape the write model genuinely cannot produce.
