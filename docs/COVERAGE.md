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

`sonar.coverage.jacoco.xmlReportPaths` is resolved per project, so a path written
relative to `${project.basedir}` has to match that project's nesting depth. It is
declared once, in the root pom, as one entry per depth:

```xml
<sonar.coverage.jacoco.xmlReportPaths>${project.basedir}/${aggregate.report.dir},${project.basedir}/../${aggregate.report.dir},${project.basedir}/../../${aggregate.report.dir},${project.basedir}/../../../${aggregate.report.dir}</sonar.coverage.jacoco.xmlReportPaths>
```

Depth 0 is the root reactor, 1 the top-level modules, 2 `modules/*`, 3
`modules/*/*`. Every project matches exactly one; the scanner accepts a
comma-separated list and imports coverage as long as **one** entry resolves, so
the other three are harmlessly absent. **Adding a fourth nesting level means
adding a fifth entry** — that is the cost of this approach, and it is the reason
to keep the module tree shallow.

Keep it on one line. The value is split on commas and a multi-line value has been
a source of stray whitespace in entries here before.

### Why not one depth-independent expression

`${project.parent.basedir}` looks like it removes the counting — every module here
declares the root pom as its direct parent, so it would name the Maven root from
any depth. **It does not work, and it fails silently.** Maven's *model*
interpolator, which produces the property value the scanner reads, resolves
`${project.basedir}` as a special case but has no `basedir` for `project.parent`,
which is a `Parent` element rather than a project. The expression survives
uninterpolated into the effective POM, and Sonar receives the literal string:

```
No coverage report can be found with sonar.coverage.jacoco.xmlReportPaths=
'${project.parent.basedir}/sonar/target/site/jacoco-aggregate/jacoco.xml,…'
```

`${maven.multiModuleProjectDirectory}` is a second tempting answer with a
different flaw: it is set by the `mvn` launcher by walking **up** from the
working directory looking for `.mvn/`, so it is correct when the build is started
from the Maven root and wrong when started from the repository root with `-f`.

### Verifying a change to this property

Use `help:effective-pom`, never `help:evaluate`. They resolve expressions through
different mechanisms — `help:evaluate` uses the plugin parameter evaluator, which
resolves `${project.parent.basedir}` quite happily — and only the effective POM
shows what the scanner will actually see:

```bash
./mvnw -B -pl <module> help:effective-pom -Doutput=/tmp/eff.xml
grep -o '<sonar.coverage.jacoco.xmlReportPaths>[^<]*' /tmp/eff.xml
```

Check both that no entry still contains a `${...}` and that exactly one names a
file that exists. Do this for one project at each depth.

## Where it lives

| File | Declares |
|---|---|
| root `pom.xml` | `prepare-agent`, and the one `xmlReportPaths` property |
| `sonar/pom.xml` | `report-aggregate`, and the dependency list that scopes it |
| every other pom | nothing coverage-related |

`report-aggregate` belongs to `sonar` alone. Declaring it in the root
`<plugins>` makes every module run it, producing fourteen partial reports that
nothing reads.
