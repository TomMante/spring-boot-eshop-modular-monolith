# Feature Workflow

This process is **opt-in**: it only applies when explicitly requested for a
piece of work (e.g. "let's do this as a feature" / "start the feature
workflow for X"). Absent that request, work continues directly on `main` as
before. Entering plan mode by itself does not imply this workflow.

For a small, already-understood change that does not warrant a plan
document, use the lighter [quick-fix.md](quick-fix.md) instead.

## Steps

1. **Start in plan mode.** Draft the plan following the planning conventions
   in [rules-of-engagement.md](rules-of-engagement.md) (naming — including
   the git-safe slug requirement, since it's reused as the branch name in
   step 2 — and folder location under `ai-coding-tools/plans/<plan-name>/`).
2. **Create the branch.** Once the plan name is settled, create
   `<prefix>/<plan-name>` from the latest `main` — same `<plan-name>` as the
   plan's folder, so the two are always easy to line up. Pick the prefix by
   what the plan closes, not by how big it is:
   - `fix/` — closes a [Known Bug](../../docs/KNOWN_ISSUES.md)
   - `test/` — closes a Testing Gap in the same file
   - `feature/` — implements a [Backlog](../../docs/BACKLOG.md) item, or any
     new capability not tracked in either file
   - `refactor/` — restructures or renames without changing behavior, and
     closes nothing in either file
   A plan spanning more than one still gets one branch; pick the prefix for
   what it primarily closes.
3. **Commit the plan.** Commit the initial plan draft to the branch. As the
   plan is discussed and revised, committing is manual — you decide when a
   revision is worth a commit. Suggest committing at natural checkpoints
   (e.g. after a substantive revision), but wait for the user's go-ahead
   rather than committing without it. When you do commit, use:
   `plan: <plan-name> — <what changed>`.
4. **Close out discussion.** Once the plan is approved, make a final commit
   marking that (`plan: <plan-name> approved`), then exit plan mode.
5. **Implement.** Follow the plan's sequencing — strict TDD red/green/refactor
   vs. tests-alongside, per step, as called out in the plan
   ([rules-of-engagement.md](rules-of-engagement.md)). Use
   [refactoring.md](refactoring.md)'s process for any step that's a
   refactor of existing code. By default the user writes the code; see
   *Guided implementation* below for the opt-in alternative.
6. **Commit as you go.** Refactor steps use
   [refactoring.md](refactoring.md)'s `- r <refactoring>` message format;
   other implementation commits are free-form but descriptive of the step
   completed.
7. **Review and iterate.** Periodically check progress against the plan. If
   scope or approach changes, update the plan doc and commit that update
   (same convention as step 3), then continue implementation — loop back to
   5–6 as needed.
8. **Close out the plan.** Before opening the PR, walk the plan's *Deferred*
   section and anything else the increment surfaced but did not fix, and
   route each entry into [KNOWN_ISSUES.md](../../docs/KNOWN_ISSUES.md) or
   [BACKLOG.md](../../docs/BACKLOG.md) — or state that it is being dropped.
   The routing rule is in [rules-of-engagement.md](rules-of-engagement.md).
   The plan's own text stays as written; it is the record of what this
   increment decided, not a live list. Commit this with the implementation,
   so the two files are never behind the branch.
9. **Open the PR.** Once the user agrees implementation is done, open a PR
   from `<prefix>/<plan-name>` into `main`. No merge strategy is
   prescribed here — use whichever the repository/GitHub default is at
   merge time. Pushing the branch and opening the PR each require explicit
   confirmation in the moment, per standing rules — this workflow doesn't
   change that, it just marks where in the sequence it happens.

## Guided implementation (opt-in)

An alternative to the user writing the code in step 5. The assistant
implements the approved plan, but applies nothing without the user's
approval of that exact change. It only applies when the user asks for it
for this increment, and counts as the implementation handover described
in [rules-of-engagement.md](rules-of-engagement.md), for this increment only.

1. **Work in small batches.** One batch is one file or a small set of
   files that belong to one move in the plan: compile-only scaffolding,
   the red tests, the green change, the tests updated alongside, a docs
   update. Follow the plan's order and its rigor per step.
2. **Show before applying.** For each batch, show the change as a diff,
   explain why it is needed and what it should do when run, and ask for
   **"ok"** or **"let's refine"**. Apply it only after "ok". On "let's
   refine", discuss, then show the revised batch again. Stage every applied
   change right away (`git add` on exactly the files in the batch), so the
   staged diff is always what the user has approved and the unstaged diff
   is empty.
3. **Make red mean something.** For a strict red/green/refactor step, the
   scaffolding batch compiles but keeps today's behavior, so the red tests
   fail on the behavior, not on compilation. After applying the red batch,
   run the tests and report why each one failed before proposing green.
4. **Run and report.** After each applied batch that changes code, run the
   narrowest command that proves it (one test class during red/green, the
   full `./mvnw clean verify` at the end of each plan step) and report the
   result before the next batch.
5. **Keep the plan honest.** When the code disagrees with the plan (a step
   that can't compile on its own, a file listed that needs no change), say
   so with ❓ before going on, and record the outcome in the plan. Tick the
   plan's items as part of the batches that complete them.
6. **Ask separately for side effects outside the diff.** Wiping local
   data, starting the application, or calling its endpoints are named and
   confirmed on their own, before they run.
7. **Leave the commit to the user.** When the plan is done, show the staged
   diff and end with a suggested one-line commit message. The user reviews
   and makes the commit; the assistant does not commit in this mode.
