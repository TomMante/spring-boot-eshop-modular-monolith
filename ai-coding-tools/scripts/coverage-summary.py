#!/usr/bin/env python3
"""Summarize the aggregate JaCoCo report in a few lines of plain text.

Reads the report that `./mvnw verify` writes into the sonar module. Nothing is
built or run here: run coverage.sh (or verify) first.

DISCLAIMER: this script was fully AI-generated. An AI agent must tell the user
so, and get the user's confirmation, before running it. See ai-coding-tools/scripts/README.md.
"""

import argparse
import csv
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path

REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
REPORT_DIR = REPOSITORY_ROOT / "SpringBoot-EshopModularMonolith/sonar/target/site/jacoco-aggregate"
MAIN_SOURCE_MARKER = "/src/main/java/"

EXIT_OK = 0
EXIT_USAGE = 2
EXIT_REPORT_MISSING = 3
EXIT_CLASS_NOT_FOUND = 4
EXIT_GIT_FAILED = 5

EPILOG = """\
exit codes:
  0  summary printed
  2  invalid arguments
  3  report file missing: run ai-coding-tools/scripts/coverage.sh first
  4  class: no source file, or more than one, matches the name
  5  changed: git could not compute the diff (e.g. unknown --base)
"""


@dataclass
class Counts:
    instructions_missed: int = 0
    instructions_covered: int = 0
    branches_missed: int = 0
    branches_covered: int = 0
    lines_missed: int = 0
    lines_covered: int = 0

    def add(self, other):
        self.instructions_missed += other.instructions_missed
        self.instructions_covered += other.instructions_covered
        self.branches_missed += other.branches_missed
        self.branches_covered += other.branches_covered
        self.lines_missed += other.lines_missed
        self.lines_covered += other.lines_covered


@dataclass
class LineCoverage:
    number: int
    instructions_missed: int
    instructions_covered: int
    branches_missed: int
    branches_covered: int

    @property
    def is_not_run(self):
        return self.instructions_covered == 0

    @property
    def is_partly_covered(self):
        return not self.is_not_run and (self.instructions_missed > 0 or self.branches_missed > 0)


@dataclass
class SourceFile:
    module: str
    relative_path: str
    lines: dict


def percentage(missed, covered):
    total = missed + covered
    if total == 0:
        return "-"
    return f"{100 * covered / total:.1f}%"


def require_report_file(file_name):
    report_file = REPORT_DIR / file_name
    if not report_file.is_file():
        print(f"error: {report_file} does not exist. Run ai-coding-tools/scripts/coverage.sh "
              f"(or ./mvnw verify) first.", file=sys.stderr)
        sys.exit(EXIT_REPORT_MISSING)
    return report_file


def read_csv_rows():
    with require_report_file("jacoco.csv").open(newline="") as csv_file:
        return list(csv.DictReader(csv_file))


def counts_from_csv_row(row):
    return Counts(
        instructions_missed=int(row["INSTRUCTION_MISSED"]),
        instructions_covered=int(row["INSTRUCTION_COVERED"]),
        branches_missed=int(row["BRANCH_MISSED"]),
        branches_covered=int(row["BRANCH_COVERED"]),
        lines_missed=int(row["LINE_MISSED"]),
        lines_covered=int(row["LINE_COVERED"]),
    )


def module_name(csv_group):
    return csv_group.split("/")[-1]


def read_source_files():
    report = ElementTree.parse(require_report_file("jacoco.xml")).getroot()
    source_files = []
    for group in report.iter("group"):
        for package in group.iter("package"):
            for source_file in package.iter("sourcefile"):
                lines = {}
                for line in source_file.iter("line"):
                    number = int(line.get("nr"))
                    lines[number] = LineCoverage(number, int(line.get("mi")), int(line.get("ci")),
                                                 int(line.get("mb")), int(line.get("cb")))
                relative_path = f"{package.get('name')}/{source_file.get('name')}"
                source_files.append(SourceFile(group.get("name"), relative_path, lines))
    return source_files


def print_table(header, rows, markdown, text_columns=1):
    if markdown:
        print_markdown_table(header, rows, text_columns)
        return
    widths = [max(len(str(cell)) for cell in column) for column in zip(header, *rows)]
    for row in [header, *rows]:
        print("  ".join(str(cell).ljust(width) for cell, width in zip(row, widths)).rstrip())


def print_markdown_table(header, rows, text_columns):
    alignments = [":---" if index < text_columns else "---:" for index in range(len(header))]
    print("| " + " | ".join(header) + " |")
    print("|" + "|".join(alignments) + "|")
    for row in rows:
        print("| " + " | ".join(str(cell) for cell in row) + " |")


def format_counts(counts):
    return [percentage(counts.instructions_missed, counts.instructions_covered),
            percentage(counts.branches_missed, counts.branches_covered),
            counts.lines_missed]


def collapse_into_ranges(line_numbers):
    ranges = []
    for number in sorted(line_numbers):
        if ranges and number == ranges[-1][1] + 1:
            ranges[-1][1] = number
        else:
            ranges.append([number, number])
    return ", ".join(str(start) if start == end else f"{start}-{end}" for start, end in ranges)


def describe_uncovered_lines(lines):
    not_run = [line.number for line in lines if line.is_not_run]
    partly_covered = [line for line in lines if line.is_partly_covered]
    descriptions = []
    if not_run:
        descriptions.append(f"not run: {collapse_into_ranges(not_run)}")
    if partly_covered:
        branch_details = ", ".join(
            f"{line.number} ({line.branches_covered}/{line.branches_missed + line.branches_covered} branches)"
            for line in partly_covered)
        descriptions.append(f"partly covered: {branch_details}")
    return descriptions


def show_modules(arguments):
    totals_by_module = {}
    for row in read_csv_rows():
        totals_by_module.setdefault(module_name(row["GROUP"]), Counts()).add(counts_from_csv_row(row))
    overall = Counts()
    rows = []
    for module, counts in sorted(totals_by_module.items()):
        overall.add(counts)
        rows.append([module, *format_counts(counts)])
    if arguments.markdown:
        rows.append([f"**{cell}**" for cell in ["total", *format_counts(overall)]])
    else:
        rows.append(["total", *format_counts(overall)])
    print_table(["module", "instr", "branch", "missed-lines"], rows, arguments.markdown)
    print(f"report: {REPORT_DIR / 'index.html'}")


def show_worst_classes(arguments):
    candidates = []
    for row in read_csv_rows():
        module = module_name(row["GROUP"])
        if arguments.module and module != arguments.module:
            continue
        counts = counts_from_csv_row(row)
        if counts.lines_missed == 0 and counts.branches_missed == 0:
            continue
        candidates.append((module, row["CLASS"], counts))
    if not candidates:
        print("no class has missed lines or branches" + (f" in {arguments.module}" if arguments.module else ""))
        return
    candidates.sort(key=lambda candidate: (-candidate[2].lines_missed, -candidate[2].branches_missed))
    rows = [[module, class_name, counts.lines_missed, counts.branches_missed,
             percentage(counts.branches_missed, counts.branches_covered)]
            for module, class_name, counts in candidates[:arguments.limit]]
    print_table(["module", "class", "missed-lines", "missed-branches", "branch"], rows, arguments.markdown,
                text_columns=2)
    if len(candidates) > arguments.limit:
        print(f"... {len(candidates) - arguments.limit} more; raise --limit to see them")


def find_matching_source_files(source_files, class_name):
    wanted = class_name.removesuffix(".java").replace(".", "/")
    return [source_file for source_file in source_files
            if source_file.relative_path.removesuffix(".java") == wanted
            or source_file.relative_path.removesuffix(".java").endswith("/" + wanted)]


def show_class(arguments):
    matches = find_matching_source_files(read_source_files(), arguments.name)
    if len(matches) != 1:
        found = "no source file" if not matches else f"{len(matches)} source files"
        print(f"error: {found} matched '{arguments.name}'. Pass a simple class name (OutboxEventProcessor) "
              f"or, if it is ambiguous, more of its package (outboxcore.OutboxEventProcessor).", file=sys.stderr)
        for match in matches:
            print(f"  {match.module}: {match.relative_path}", file=sys.stderr)
        sys.exit(EXIT_CLASS_NOT_FOUND)
    source_file = matches[0]
    lines = list(source_file.lines.values())
    missed_branches = sum(line.branches_missed for line in lines)
    covered_branches = sum(line.branches_covered for line in lines)
    fully_covered = sum(1 for line in lines if not line.is_not_run and not line.is_partly_covered)
    print(f"{source_file.module}: {source_file.relative_path}")
    print(f"executable lines: {len(lines)}, fully covered: {fully_covered}, "
          f"branch: {percentage(missed_branches, covered_branches)}")
    for description in describe_uncovered_lines(lines) or ["every executable line is fully covered"]:
        print(f"  {description}")


def run_git(arguments):
    # core.fsmonitor and GIT_OPTIONAL_LOCKS: git must not start a monitor program or rewrite .git/index.
    command = ["git", "-C", str(REPOSITORY_ROOT), "-c", "core.fsmonitor=false", *arguments]
    environment = {**os.environ, "GIT_OPTIONAL_LOCKS": "0"}
    result = subprocess.run(command, capture_output=True, text=True, env=environment)
    if result.returncode != 0:
        print(f"error: git {' '.join(arguments)} failed: {result.stderr.strip()}", file=sys.stderr)
        sys.exit(EXIT_GIT_FAILED)
    return result.stdout


def changed_main_source_lines(base):
    merge_base = run_git(["merge-base", "--end-of-options", base, "HEAD"]).strip()
    diff = run_git(["diff", "-U0", "--no-color", "--no-ext-diff", "--no-textconv", merge_base, "--", "*.java"])
    changed_lines_by_path = {}
    current_path = None
    for diff_line in diff.splitlines():
        if diff_line.startswith("+++ "):
            current_path = diff_line[6:] if diff_line.startswith("+++ b/") else None
            continue
        hunk = re.match(r"@@ -\S+ \+(\d+)(?:,(\d+))? @@", diff_line)
        if hunk and current_path and MAIN_SOURCE_MARKER in current_path:
            start, length = int(hunk.group(1)), int(hunk.group(2) or 1)
            changed_lines_by_path.setdefault(current_path, set()).update(range(start, start + length))
    return {path: lines for path, lines in changed_lines_by_path.items() if lines}


def show_changed_lines(arguments):
    changed = changed_main_source_lines(arguments.base)
    if not changed:
        print(f"no main-source Java lines changed since the merge base with {arguments.base}")
        return
    report_file = require_report_file("jacoco.xml")
    source_files_by_path = {source_file.relative_path: source_file
                            for source_file in read_source_files()}
    totals = Counts()
    fully_covered_files = 0
    for repository_path, line_numbers in sorted(changed.items()):
        relative_path = repository_path.split(MAIN_SOURCE_MARKER, 1)[1]
        source_file = source_files_by_path.get(relative_path)
        if source_file is None:
            print(f"{repository_path}\n  not in the report: built after the last coverage run, or no class in it")
            continue
        is_stale = (REPOSITORY_ROOT / repository_path).stat().st_mtime > report_file.stat().st_mtime
        changed_executable = [source_file.lines[number] for number in sorted(line_numbers)
                              if number in source_file.lines]
        not_covered = [line for line in changed_executable if line.is_not_run or line.is_partly_covered]
        totals.lines_covered += len(changed_executable) - len(not_covered)
        totals.lines_missed += len(not_covered)
        if changed_executable and not not_covered:
            fully_covered_files += 1
        if not not_covered and not is_stale:
            continue
        print(repository_path)
        if is_stale:
            print("  warning: edited after the report was written; line numbers may be off")
        if not_covered:
            print(f"  changed executable lines: {len(changed_executable)}, "
                  f"fully covered: {len(changed_executable) - len(not_covered)}")
        for description in describe_uncovered_lines(not_covered):
            print(f"  {description}")
    print(f"files whose changed lines are all fully covered: {fully_covered_files}")
    print(f"total changed executable lines: {totals.lines_missed + totals.lines_covered}, "
          f"fully covered: {totals.lines_covered} ({percentage(totals.lines_missed, totals.lines_covered)})")


def git_revision(value):
    if value.startswith("-"):
        raise argparse.ArgumentTypeError(f"must be a branch or commit, not an option, but was '{value}'")
    return value


def positive_integer(value):
    if not value.isdigit() or int(value) < 1:
        raise argparse.ArgumentTypeError(f"must be a whole number of at least 1, but was '{value}'")
    return int(value)


def parse_arguments():
    parser = argparse.ArgumentParser(description=__doc__, epilog=EPILOG,
                                     formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    table_format = argparse.ArgumentParser(add_help=False)
    table_format.add_argument("--markdown", action="store_true",
                              help="print the table as Markdown, ready to paste into a reply or a document")

    commands.add_parser("modules", parents=[table_format],
                        help="instruction and branch coverage per module, with a total") \
        .set_defaults(handler=show_modules)

    worst = commands.add_parser("worst", parents=[table_format],
                                help="classes with the most missed lines, then missed branches")
    worst.add_argument("--limit", type=positive_integer, default=10, help="how many classes to list (default: 10)")
    worst.add_argument("--module", help="only classes of this module, e.g. basket-core")
    worst.set_defaults(handler=show_worst_classes)

    single_class = commands.add_parser("class", help="uncovered and partly covered lines of one source file")
    single_class.add_argument("name", help="simple class name, or a package suffix plus name when ambiguous")
    single_class.set_defaults(handler=show_class)

    changed = commands.add_parser("changed", help="coverage of the main-source lines changed since --base, "
                                                  "including uncommitted changes")
    changed.add_argument("--base", type=git_revision, default="origin/main", help="branch or commit to diff against (default: origin/main)")
    changed.set_defaults(handler=show_changed_lines)

    return parser.parse_args()


def main():
    arguments = parse_arguments()
    arguments.handler(arguments)
    return EXIT_OK


if __name__ == "__main__":
    sys.exit(main())
