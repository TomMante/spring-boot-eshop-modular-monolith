# Feature Workflow

This process is **opt-in**: it only applies when explicitly requested for a
piece of work (e.g. "let's do this as a feature" / "start the feature
workflow for X"). Absent that request, work continues directly on `main` as
before. Entering plan mode by itself does not imply this workflow.

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
   refactor of existing code.
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
   from `feature/<plan-name>` into `main`. Copilot review has a cost, so
   ask before requesting it rather than requesting it by default — both
   when the PR is first opened and again after any later push to that PR,
   since a Copilot review doesn't automatically re-run against new
   commits. When requesting it, use `gh pr edit <number> --add-reviewer
   @copilot` (requires `gh` ≥2.88.0; `gh pr create --reviewer @copilot`
   works too, if requesting at creation time). No merge strategy is
   prescribed here — use whichever the repository/GitHub default is at
   merge time. Pushing the branch and opening the PR each require explicit
   confirmation in the moment, per standing rules — this workflow doesn't
   change that, it just marks where in the sequence it happens.
