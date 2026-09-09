# Coverage Aggregation

How one coverage number is produced across fifteen Maven modules, and the two
things about it that are easy to get wrong.

This file describes the **plumbing**. What is worth testing, and why line count
is not the goal, is [TESTING_CONVENTIONS.md](TESTING_CONVENTIONS.md).

## The pipeline

```
each module          sonar module              scanner
  jacoco.exec   →    jacoco-aggregate/    →    per-module
  (binary)           jacoco.xml                coverage in SonarCloud
```

1. **Record.** `jacoco:prepare-agent` is declared once in the root pom's
   `<plugins>`, so every module inherits it. It sets `argLine`, Surefire runs
   with the JaCoCo agent, and each module writes `target/jacoco.exec` — raw
   "which lines executed" data, with no notion of which module owns them.

2. **Aggregate.** The `sonar` module runs `jacoco:report-aggregate` at `verify`.
   It reads the `.exec` files *and* the classes and sources of the modules it
   depends on, and merges them into one
   `sonar/target/site/jacoco-aggregate/jacoco.xml`.

   This merge is the point of the exercise: a test in `starter` that exercises
   `catalog-core` code counts toward `catalog-core`, because attribution comes
   from combined execution data rather than from fifteen separate reports.

3. **Import.** The scanner runs once per module. For each, it reads
   `sonar.coverage.jacoco.xmlReportPaths`, opens that XML, and takes the entries
   matching *that module's* files. Every module points at the same aggregate;
   each finds its own slice.

## Constraint 1: `report-aggregate` sees direct dependencies only

Transitive dependencies are not included. `sonar/pom.xml` must therefore name
every code-bearing module explicitly, and that list is a hand-maintained
duplicate of the root pom's `<modules>`.

Nothing compares the two. A module missing from it is not reported as
uncovered — it is **excluded from the ratio**, so the number goes *up* when
reporting breaks. `outbox-mongo` was absent for the whole life of the module and
the build stayed green throughout. Closing that hole is
[Backlog item 10](BACKLOG.md).

After adding a module, check that
`sonar/target/site/jacoco-aggregate/` contains one directory per code-bearing
module. There are ten.

## Constraint 2: the report path must resolve in every project

`sonar.coverage.jacoco.xmlReportPaths` is resolved per project, so a path
written relative to `${basedir}` has to match that project's nesting depth.
Counting `../` per module is how this was done originally, and four modules
counted wrong — silently, because a path that does not resolve produces no
coverage rather than an error.

It is now declared once, in the root pom, with two complementary entries:

```xml
<sonar.coverage.jacoco.xmlReportPaths>${project.parent.basedir}/${aggregate.report.dir},${project.basedir}/${aggregate.report.dir}</sonar.coverage.jacoco.xmlReportPaths>
```

| Project | `${project.parent.basedir}` | `${project.basedir}` |
|---|---|---|
| any module | Maven root ✅ | wrong |
| root reactor | unresolvable | Maven root ✅ |

Every module in this build declares the root pom as its **direct** parent, at
every depth, so the first entry is correct everywhere except the root itself —
which the second entry covers. The scanner accepts a comma-separated list and is
satisfied when one entry resolves, so each project silently uses whichever
applies. Nothing counts directories, so a module added at any depth inherits a
correct path.

## Where it lives

| File | Declares |
|---|---|
| root `pom.xml` | `prepare-agent`, and the one `xmlReportPaths` property |
| `sonar/pom.xml` | `report-aggregate`, and the dependency list that scopes it |
| every other pom | nothing coverage-related |

`report-aggregate` belongs to `sonar` alone. Declaring it in the root
`<plugins>` makes every module run it, producing fourteen partial reports that
nothing reads.
