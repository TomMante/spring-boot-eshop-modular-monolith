# Scripts

Repeatable commands for working on this repository, written to be run by a person
or by a coding agent. Each one prints a short, stable summary instead of a raw
log, has a `--help` that lists its options and exit codes, and exits non-zero
when it fails.

Run them from anywhere in the repository; they locate the repository root from
their own path. They need `bash` and `python3` (standard library only).

## AI-generated

**Every script in this folder was fully AI-generated.** Read a script before
relying on it, as you would any code you did not write.

**For AI agents:** before running any script in this folder, tell the user that
it was AI-generated, say which script and with which arguments you intend to run,
and wait for the user's confirmation. Ask again for each new run; one approval does
not cover later runs. `coverage.sh` calls `coverage-summary.py` itself, so
confirming `coverage.sh` covers both.

| Script | What it does |
|---|---|
| [coverage.sh](coverage.sh) | Runs `./mvnw -B clean verify`, then prints test totals, per-module coverage and the report paths, or only the failing tests if the build fails. Fails with exit code 5 if a module with tests wrote no `jacoco.exec`, since its coverage would be understated. `--no-build` re-reads the last report; `--open` opens the HTML report; `--markdown` prints the table as Markdown. |
| [coverage-summary.py](coverage-summary.py) | Reads the aggregate JaCoCo report from the last run. Four modes: `modules`, `worst`, `class`, `changed`. |

## Coverage summary modes

| Mode | Answers | Reads |
|---|---|---|
| `modules [--markdown]` | How covered is each module, and overall? | `jacoco.csv` |
| `worst [--limit N] [--module M] [--markdown]` | Which classes have the most uncovered lines and branches? | `jacoco.csv` |
| `class <Name>` | Which lines of this class are not run, or only partly covered? | `jacoco.xml` |
| `changed [--base REF]` | Are the main-source lines I changed since `REF` (default `origin/main`) covered? Includes uncommitted changes. | `jacoco.xml` + `git diff` |

`changed` is the one to use when reviewing a change: a file can be well covered
overall and still have all its uncovered lines in the part that just changed. It
has two limits:
- **Untracked files are invisible to it**, because they are not in `git diff`.
  Stage a new file (`git add -N <file>`) to include it.
- **It trusts the report's line numbers.** If a file was edited after the last
  coverage run, it prints a warning.

### What it touches

`coverage-summary.py` is read-only and stays inside the repository:
- **Reads** only `jacoco.csv` and `jacoco.xml` from the fixed report folder
  `SpringBoot-EshopModularMonolith/sonar/target/site/jacoco-aggregate/`. There is
  no option to point it elsewhere.
- **Checks the modification time** (and nothing else) of the changed source files
  that `git diff` lists, to warn when the report is older than an edit.
- **Runs `git`** (`changed` only), with an argument list and no shell, scoped to the
  repository with `-C`. `--base` must not start with `-` and is passed after
  `--end-of-options`, so it is never read as a git option. `--no-ext-diff`,
  `--no-textconv` and `core.fsmonitor=false` stop git from starting external
  programs, and `GIT_OPTIONAL_LOCKS=0` stops it from rewriting `.git/index`.
- **Writes** nothing and makes no network calls.

These limits are in the code, not enforced by the operating system: the script
runs with the permissions of whoever starts it.

Coverage percentages are JaCoCo **instruction** and **branch** coverage, so they
will not match SonarCloud's line-and-condition `Coverage` metric exactly. See
[COVERAGE.md](../../docs/COVERAGE.md).

## Coverage in IntelliJ

`coverage.sh` prints the path of the aggregate `jacoco.xml`. Import it once with
*Run → Manage Coverage Reports → +*. After a later run, select it there and click
*Show selected*. The aggregate already merges every module's execution data, so no
separate merge step is needed.
