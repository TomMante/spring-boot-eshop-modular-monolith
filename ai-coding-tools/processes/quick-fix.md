# Quick-Fix Workflow

START_TOKEN = ⚡

This process is **opt-in**: it only applies when explicitly requested (e.g.
"quick-fix this" / "start the quick-fix workflow"). It is the lightweight
sibling of [feature-workflow.md](feature-workflow.md) — same care, less
ceremony. There is no plan document, no plan folder, and no PR step.

## When it fits

Use it when the change is small and already understood: the shape of the fix
is clear from reading the code, and writing a plan document would cost more
than it clarifies. Typical fits: a single Known Bug with an obvious cause, a
missing test for one behavior, a configuration or dependency correction, a
localized rename or extraction.

Use [feature-workflow.md](feature-workflow.md) instead when the change spans
several modules, needs a design decision recorded, or has sequencing worth
writing down.

**The analysis in step 2 is the escape hatch.** If it turns out the change is
larger than it looked, stop and say so with ❓ rather than pushing a plan-sized
change through this process. Switching costs one message; discovering it
halfway through implementation costs much more.

## Steps

1. **Ask the two setup questions.** One at a time, waiting for an answer
   before asking the next (per
   [rules-of-engagement.md](rules-of-engagement.md)):
   - *Who implements?* The default remains the user — the standing division of
     labour is unchanged, and this question is the only way to override it.
     An answer of "you do it" is scoped to **this quick fix only**; it does
     not carry to the next one.
   - *Branch or work on `main`?* If a branch is wanted, name it
     `<prefix>/<short-slug>` using the same prefix taxonomy as
     [feature-workflow.md](feature-workflow.md) — `fix/` for a
     [Known Bug](../../docs/KNOWN_ISSUES.md), `test/` for a Testing Gap in the
     same file, `feature/` for a [Backlog](../../docs/BACKLOG.md) item or any
     new capability, `refactor/` for a behavior-preserving restructure or
     rename that closes nothing in either file. The slug describes the fix, not a plan, since there is no
     plan folder to line it up with. Create it from the latest `main`.

2. **Analyse, then explain.** Read the relevant code before saying anything
   about the fix. Then give a **concise explanation** of the work — not a
   plan document, not a step list:
   - what is wrong (or missing) and where, with file references;
   - what the change will be, and why that one rather than an alternative
     that was considered and rejected;
   - which tests will prove it, and whether they come first (strict red/green)
     or alongside the implementation, per the TDD criteria in
     [rules-of-engagement.md](rules-of-engagement.md);
   - a short code example wherever prose is the longer way to say it.

   Keep it to what fits in a reply. If it does not fit, see the escape hatch
   above.

3. **On approval, take one of two paths.**
   - *Auto-implementation was chosen in step 1:* implement it. Stay inside
     what was explained in step 2 — anything discovered mid-change that
     widens the scope stops for a ❓ instead of being absorbed silently. Use
     [refactoring.md](refactoring.md)'s process for any part that is a
     refactor of existing code (minus its commit step, see step 4).
   - *The user implements:* provide an actionable TODO list — ordered,
     checkbox-per-step, each step naming the file it touches and small enough
     to be finished and verified on its own. Include the test steps in the
     order the TDD choice from step 2 implies. Snippets, signatures and test
     skeletons belong in the list; applying them to files does not.

4. **Do not commit.** This holds for both paths, and it overrides
   [refactoring.md](refactoring.md)'s per-refactor commit step for work done
   under this process. Committing is a separate, explicit request.

5. **Verify, then report.**
   - Run the relevant tests (`./mvnw test -Dtest=...` for a narrow change,
     `./mvnw clean verify` when the change could affect the build or
     coverage). Report failures with their output.
   - *If the change was auto-implemented:* summarise what changed, file by
     file, and what the tests now prove.
   - *If the user implemented it:* review the diff against the step-2
     explanation — what matches, what deviates, and whether each deviation is
     an improvement or a defect. Say plainly which. Do not correct the code
     directly.

   Then close out the two live lists, if the change touched them: tick or
   remove what it fixed in [KNOWN_ISSUES.md](../../docs/KNOWN_ISSUES.md) or
   [BACKLOG.md](../../docs/BACKLOG.md), and route anything the fix surfaced
   but did not address into whichever of the two fits (routing rule in
   [rules-of-engagement.md](rules-of-engagement.md)). Check
   [README.md](../../README.md) the same way. These are documentation edits,
   so they are made directly.

6. **Suggest a commit message.** One line, imperative, describing the change
   rather than the process — matching the style already in `git log`. No body
   unless the *why* is genuinely not inferable from the diff, and then one
   sentence. A refactor-only quick fix keeps
   [refactoring.md](refactoring.md)'s `- r <refactoring>` format instead.
   Offer the message; leave the committing to the user unless asked.
