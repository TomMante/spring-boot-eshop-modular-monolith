#!/usr/bin/env bash
# Run the full test suite with coverage, then print a short result instead of the Maven log.
#
# DISCLAIMER: this script was fully AI-generated. An AI agent must tell the user so, and get
# the user's confirmation, before running it. See ai-coding-tools/scripts/README.md.
set -uo pipefail

readonly EXIT_OK=0
readonly EXIT_USAGE=2
readonly EXIT_BUILD_FAILED=3
readonly EXIT_REPORT_MISSING=4
readonly EXIT_COVERAGE_INCOMPLETE=5

readonly SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPOSITORY_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
readonly APPLICATION_DIR="$REPOSITORY_ROOT/backend"
readonly REPORT_DIR="$APPLICATION_DIR/sonar/target/site/jacoco-aggregate"
# Outside target/: `clean` would delete the log while Maven is still writing it.
readonly BUILD_LOG="${TMPDIR:-/tmp}/eshop-coverage-build.log"

print_help() {
    cat <<EOF
Usage: $(basename "$0") [--no-build] [--open] [--markdown] [-h|--help]

DISCLAIMER: this script was fully AI-generated.

Runs ./mvnw -B clean verify (needs JDK 25 and Docker for Testcontainers), writes the
Maven log to $BUILD_LOG, and prints only:
  - on failure: the failing tests, or the first build errors if no test failed;
  - on success: test totals, per-module coverage, and the report paths.

Options:
  --no-build  skip the build and summarize the report from the last run
  --open      open the HTML report in the browser afterwards
  --markdown  print the per-module table as Markdown, ready to paste into a reply
  -h, --help  show this help

For IntelliJ, import the printed jacoco.xml once via Run > Manage Coverage Reports.
For more detail, run coverage-summary.py (worst, class, changed) next to this script.

Exit codes:
  0  build passed (or --no-build) and the summary was printed
  2  unknown option
  3  build failed; the failing tests or errors were printed
  4  no aggregate report found (with --no-build, run without it first)
  5  summary printed, but a module with *Test.java files wrote no jacoco.exec,
     so the coverage is incomplete; the modules are listed last
EOF
}

run_build=true
open_report=false
table_format=()
for argument in "$@"; do
    case "$argument" in
        --no-build) run_build=false ;;
        --open) open_report=true ;;
        --markdown) table_format=(--markdown) ;;
        -h|--help) print_help; exit "$EXIT_OK" ;;
        *) echo "error: unknown option '$argument'. Valid options: --no-build, --open, --markdown, --help." >&2
           exit "$EXIT_USAGE" ;;
    esac
done

print_build_failures() {
    local failing_tests
    failing_tests="$(grep -h '<<< \(FAILURE\|ERROR\)!' "$APPLICATION_DIR"/{,*/,*/*/,*/*/*/}target/surefire-reports/*.txt 2>/dev/null \
        | grep -v '^Tests run:' | sed 's/ -- Time elapsed.*<<< / <<< /')"
    if [[ -n "$failing_tests" ]]; then
        echo "failing tests:"
        echo "$failing_tests" | sed 's/^/  /'
        return
    fi
    echo "no test failed; first build errors:"
    # Maven's own advice (re-run with -e, help links) starts after the "-> [Help 1]" marker.
    awk '/^\[ERROR\] -> \[Help/ { exit } /^\[ERROR\] *$/ { next } /^\[ERROR\]/ { print }' "$BUILD_LOG" \
        | head -20 | sed 's/^/  /'
}

print_test_totals() {
    grep -ho '<testsuite [^>]*' "$APPLICATION_DIR"/{,*/,*/*/,*/*/*/}target/surefire-reports/TEST-*.xml 2>/dev/null \
        | awk '{
            for (field = 1; field <= NF; field++) {
                split($field, pair, "=");
                gsub(/"/, "", pair[2]);
                if (pair[1] == "tests") tests += pair[2];
                if (pair[1] == "skipped") skipped += pair[2];
            }
        } END { printf "tests run: %d, skipped: %d\n", tests, skipped }'
}

list_modules_without_execution_data() {
    local test_directory module_directory
    while IFS= read -r test_directory; do
        module_directory="${test_directory%/src/test/java}"
        if [[ ! -f "$module_directory/target/jacoco.exec" ]]; then
            echo "${module_directory#"$REPOSITORY_ROOT"/}"
        fi
    done < <(find "$APPLICATION_DIR" -path '*/target' -prune -o -type d -path '*/src/test/java' -print \
        | while IFS= read -r directory; do
            [[ -n "$(find "$directory" -name '*Test.java' -print -quit)" ]] && echo "$directory"
        done)
}

if [[ "$run_build" == true ]]; then
    echo "building: ./mvnw -B clean verify (log: $BUILD_LOG)"
    # A build that fails before reaching sonar never cleans it, which would leave the previous report looking current.
    rm -rf "$REPORT_DIR"
    if ! (cd "$APPLICATION_DIR" && ./mvnw -B clean verify) >"$BUILD_LOG" 2>&1; then
        echo "BUILD FAILED"
        print_build_failures
        echo "full log: $BUILD_LOG"
        exit "$EXIT_BUILD_FAILED"
    fi
    echo "BUILD PASSED"
    print_test_totals
fi

if [[ ! -f "$REPORT_DIR/jacoco.xml" ]]; then
    echo "error: no aggregate report at $REPORT_DIR. Run this script without --no-build first." >&2
    exit "$EXIT_REPORT_MISSING"
fi

python3 "$SCRIPT_DIR/coverage-summary.py" modules ${table_format[@]+"${table_format[@]}"}
echo "intellij: $REPORT_DIR/jacoco.xml"

if [[ "$open_report" == true ]]; then
    if command -v open >/dev/null; then open "$REPORT_DIR/index.html"; else xdg-open "$REPORT_DIR/index.html"; fi
fi

modules_without_execution_data="$(list_modules_without_execution_data)"
if [[ -n "$modules_without_execution_data" ]]; then
    echo "COVERAGE INCOMPLETE: these modules have *Test.java files but wrote no target/jacoco.exec, so their"
    echo "tests ran without the JaCoCo agent, or did not run. The coverage above is understated for these"
    echo "modules and for any other module whose code their tests exercise."
    echo "$modules_without_execution_data" | sed 's/^/  /'
    exit "$EXIT_COVERAGE_INCOMPLETE"
fi
exit "$EXIT_OK"
