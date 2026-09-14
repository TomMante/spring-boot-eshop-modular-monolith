---
name: open-points
description: Read the project's two live work lists and produce a checklist of open items ranked 🔴/🟠/🟢, with a suggestion of where to start. Use when the user asks "what's open", "what needs to be done", "show me the priority list", or similar.
---

# Open Points

Produces the prioritized checklist this project's owner uses to decide what to pick up next. Do the ranking yourself each time — don't reuse a list from earlier in the conversation, since [KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) and [BACKLOG.md](../../../docs/BACKLOG.md) change between asks.

## Source of truth

Read exactly these two files — per [getting-started.md](../../processes/getting-started.md) they are "the only live lists of outstanding work":

- [docs/KNOWN_ISSUES.md](../../../docs/KNOWN_ISSUES.md) — Known Bugs + Testing Gaps
- [docs/BACKLOG.md](../../../docs/BACKLOG.md) — Ready to pick up + Gated on a trigger

Do not scan `ai-coding-tools/plans/` to reconstruct open work — plans are frozen history, and anything still open from one has already been routed into one of the two files above.

## Priority rubric

Apply this per item; don't just carry over a rank from a previous run, since a fix elsewhere can change an item's category.

- 🔴 **High** — the defect breaks or corrupts an ordinary, common-path user action right now (a crash, silently wrong data, silently wrong config), or it's the piece of work that unblocks fixing one that does.
- 🟠 **Medium** — a real defect or gap, but conditional: it needs a decision made first, it only bites once another thing happens (compounds another bug, next schema change, etc.), or it's a structural/test-coverage gap a recent increment explicitly left open.
- 🟢 **Low** — explicitly accepted or deferred by design in the doc's own text ("acceptable", "recorded so it is not rediscovered as a surprise", "not owed right now"), an edge case, or blocked on out-of-scope infrastructure.
- **Excluded from ranking, listed separately** — anything under Backlog's "⏳ Gated on a trigger" section. It's not actionable regardless of severity; say so rather than assigning it a color.

When a Known Bug and a Backlog item describe the same piece of work (the Backlog entry says "Closes Known Bug N" or similar), merge them into one line — don't rank the same fix twice.

## Output format

A checklist grouped by section header (`## 🔴 High`, `## 🟠 Medium`, `## 🟢 Low`), each line a markdown checkbox linking to the source file:

```markdown
- [ ] **[Known Bug N](docs/KNOWN_ISSUES.md) — one-line title.** One sentence on why this rank.
```

End with a one- or two-sentence **Suggestion** naming the single best item to start with and why (usually: the highest-ranked item that doesn't first need an unmade decision). Say which workflow fits it: ⚡ [quick-fix](../../processes/quick-fix.md) when the fix is small and already clear from reading the code, or [feature](../../processes/feature-workflow.md) when it spans several modules, needs a design decision recorded, or has steps that need ordering — the same test as quick-fix's *When it fits* section. Base that on having read the code the item points at, not on the entry's wording alone.

Match the checklist-not-prose style this project's owner prefers — no narrative paragraphs between items.
