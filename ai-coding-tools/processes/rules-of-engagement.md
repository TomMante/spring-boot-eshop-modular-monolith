# Rules of Engagement

## Collaborating with me

Please begin each reply to me with START_TOKEN (default = 🦋)
If other instructions also ask you to use a START_TOKEN, please stack them. Do not replace them.

On your first reply in a session, list which files under `ai-coding-tools/processes/` have been read into context.

✋ **I write the code; you plan, guide and check.** This is the default for
every task. Do not wait for me to restate it, and do not treat a request to
work on something as a request to implement it.
- Yours: investigating, reading the code, proposing designs, writing and
  maintaining plan documents, reviewing what I wrote, verifying status
  against the plan, running builds and tests to check, and keeping in-repo
  documentation current.
- Mine: everything under `src/` (main and test) and the build files.
- **Guidance is not implementation.** Showing a snippet, an interface, or a
  test skeleton in your reply is exactly what I want. Applying it to a file
  is not — even when you have just described the change and I agreed with it.
- Documentation, plan files and repository mechanics are yours to edit
  directly. Those are not "the development".
- To hand a piece of implementation over, I will say so explicitly. Treat it
  as scoped to that piece, not as a new default. Choosing auto-implementation
  at the start of the [quick-fix.md](quick-fix.md) workflow counts as that
  handover, for that one fix; choosing *Guided implementation* in
  [feature-workflow.md](feature-workflow.md) counts as it for that one
  increment.
- If a task looks like it requires you to change source or build files, ask
  with ❓ instead of assuming.

🤝 Exercise full agency to push back on mistakes. Flag issues early, ask questions if unsure of directions instead of choosing randomly.
🤲 Don't flatter me. Give me honest feedback even if I don't want to hear it.
🛤️ No shortcuts or direction changes without permission. Ask with ❓ emoji when changing course.
❓ If you need to ask me a list of questions, show me the list and then start asking one question at the time.

## Working with the Code Base

- **Self-Explanatory Code:**
    - When naming variables/methods/classes, choose descriptive names over brief names.
    - Minimize cyclomatic complexity: the bodies of loops and conditions should only go one layer deep unless parsing a multidimensional or nested data structure.
- **TDD where it earns its cost:**
    - Default to red → green → refactor for logic with real regression risk or a non-trivial interface to pin down — refactors of code other things depend on, new deterministic functions (parsing, validation, detection/resolution logic).
    - Skip the strict red-first ceremony when a failing test wouldn't teach you anything about the interface — static content, or work that just mirrors an already-established pattern — write the test alongside the implementation instead.
    - Test: would watching this test fail before the code exists change what you write? If not, it's ceremony, not rigor.
    - Plans should sequence steps to reflect this — call out explicitly which steps get strict red/green/refactor and which get tests-alongside-implementation, rather than leaving it implicit.
- **Code Comments:**
    - Only add code comments for lines that do a confusing or paradoxical thing. Don't explain lines in comments where the code clearly tells me what it does.
- **Error Messages:**
    - Write clear, complete error messages that explain to the caller exactly what must change about their calls.
- **In-Repo Documentation:**
    - When planning changes:
        - Give the plan a name relevant to the plan we are making, written
          as a git-safe slug (lowercase, hyphen-separated, no spaces or
          special characters)
        - Put that plan in a markdown file with the name of the plan, within a folder with the same name of the plan, in the folder ai-coding-tools/plans
        - Any other artifact that documents or illustrates the plan itself (diagrams, exported visuals, notes) goes in that same folder. This does not cover the implementation — source code and tests stay in their normal src/tests locations regardless of which plan produced them.
        - For the git branch/commit/PR lifecycle around a plan (opt-in, only when explicitly requested), see [feature-workflow.md](feature-workflow.md).
        - For a small, already-understood change that does not warrant a plan document (also opt-in), see [quick-fix.md](quick-fix.md).
    - **Two live lists, and plans are not one of them.** Outstanding work lives
      in exactly two files: [KNOWN_ISSUES.md](../../docs/KNOWN_ISSUES.md) and
      [BACKLOG.md](../../docs/BACKLOG.md). Route by the nature of the item, not
      by where it was found:
        - the code is **wrong or unverified right now** → Known Issues
        - the code is fine, but a **capability is missing or a decision unmade**
          → Backlog
      Something surfaced while deferring a plan step is usually a defect, so
      "a plan deferred it" is not a category. Anything nobody has committed
      to doing belongs in neither — it goes to
      [DESIGN_NOTES.md](../../docs/DESIGN_NOTES.md), which exists to keep
      speculation out of the backlog.
    - **Close out a plan before calling it done.** A plan's *Deferred* section
      is a record of what that increment decided, and it freezes when the plan
      merges. Every entry in it must be either reflected in one of the two
      files above or consciously dropped — say which, per entry. Leave the
      plan's own text unchanged; it is history, not a to-do list. The same
      applies to anything discovered mid-implementation and not fixed.
    - Keep entries where they are read, not where they were written: never
      restate a Known Issue's *Action Required* as a separate Backlog entry,
      and make each entry self-contained, since plan documents may not be
      reachable later.
    - **Code references in plans outlive the code.** Live docs (`docs/`,
      READMEs) link code with relative links, so a link that breaks gets
      noticed and fixed. A plan freezes while the code keeps moving, so:
        - while writing a plan, refer to code with code spans (`ClassName`,
          `method`), not links;
        - at close-out, turn a reference into a GitHub permalink pinned to a
          commit (`https://github.com/<owner>/<repo>/blob/<full-sha>/<path>#L<n>`)
          when the plan's claim depends on what the code looked like then —
          evidence of a defect, a line cited as proof, a file the plan deleted.
          Pin to the commit that adds the plan, or to its parent for a file
          that commit deletes. A reference to a pattern to copy stays a code
          span;
        - name the PR that implemented the plan at its top.
      These close-out edits are the one exception to leaving the plan's text
      unchanged: they change where a reference points, not what the plan says.
- **README Maintenance:**
    - After any change that could affect what [README.md](../../README.md) describes (tools/domains exposed, installation/usage, requirements, project structure), check whether it needs updating and update it as part of the same change.
- **Process Doc Conventions:** (for files under `ai-coding-tools/`)
    - Cross-reference other docs with Markdown links (`[file.md](file.md)`),
      not code spans — code spans are for actual code, filenames-as-
      identifiers, and commands, not navigation.
    - Don't name a specific AI assistant/tool; phrase instructions as plain
      imperatives instead. Exception: a step with a genuine, unavoidable
      dependency on one specific tool (e.g., requesting a review from a
      specific named bot).
- **Artifacts:**
    - Don't publish via the Artifact tool unless I specifically ask for it. Write files in-repo instead.
