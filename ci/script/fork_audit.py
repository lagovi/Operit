#!/usr/bin/env python3
"""Audit the fork's invariants against a merged upstream revision.

The fork is an English-only distribution. That makes a handful of properties
non-negotiable, and each of them regresses silently the moment upstream lands a
change: a resource reference with no default-bucket definition falls back to a
locale the APK no longer ships, and a Chinese literal in a composable reaches
the user no matter how complete ``values/`` is.

This script is the single command to run after every upstream merge. It reads
the machine-readable registry in ``docs/FORK-REGISTRY/registry.json`` and reports
four classes of regression:

  ANCHOR-LOST      a fork change is no longer applied (its anchor symbol/string
                   disappeared, usually because upstream renamed or moved it)
  STRING-UNDEFINED an ``R.string.x`` referenced from Kotlin has no definition in
                   the default resource bucket
  CJK-NEW          a new Chinese string literal appeared in Kotlin outside the
                   reviewed allowlist
  UNREGISTERED     a shipped ``values-*`` directory is not declared in the
                   registry, or vice versa

Exit status is non-zero only when a check actually fails, so it can gate CI and
be run locally without ceremony.

Passing ``--ports`` runs a different, independent report instead: it cross-checks
the upstream commit ledger in
``docs/FORK-REGISTRY/upstream-port-ledger.json`` against the real git history and
lists every upstream commit after the fork point that the ledger does not
classify yet, plus stale ledger entries whose upstream sha matches no commit in
the range. That mode prints a report and exits zero unless the ledger itself is
missing or unparseable.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from collections import defaultdict
from dataclasses import dataclass, field
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
REGISTRY_PATH = REPO_ROOT / "docs" / "FORK-REGISTRY" / "registry.json"
LEDGER_PATH = REPO_ROOT / "docs" / "FORK-REGISTRY" / "upstream-port-ledger.json"
DEFAULT_UPSTREAM_REF = "upstream/main"

#: Modules that own their own resource package. Kotlin under these roots
#: resolves ``R.string.*`` against that module's own ``values/``, not the app's.
MODULE_RESOURCE_ROOTS = {
    "app/src/main/java": "app/src/main/res",
    "terminal/src/main/java": "terminal/src/main/res",
    "avator/dragonbones/src/main/java": "avator/dragonbones/src/main/res",
    "avator/mmd/src/main/java": "avator/mmd/src/main/res",
    "avator/fbx/src/main/java": "avator/fbx/src/main/res",
    "llm/mnn/src/main/java": "llm/mnn/src/main/res",
    "llm/llama/src/main/java": "llm/llama/src/main/res",
    "quickjs/src/main/java": "quickjs/src/main/res",
    "showerclient/src/main/java": "showerclient/src/main/res",
}

# ``android.R.string.*`` resolves against the platform, not the app, so the
# framework case is excluded while a fully qualified app R stays matchable.
STRING_REF_RE = re.compile(r"(?<!android\.)\bR\.string\.([A-Za-z0-9_]+)")
KOTLIN_STRING_RE = re.compile(r'"(?:[^"\\\n]|\\.)*"', re.S)
HAN_RE = re.compile(r"[㐀-䶿一-鿿豈-﫿]")
# Matches the whole argument list of a logging call, including newlines. Logger
# messages are diagnostics, not user-visible text, so a Chinese literal inside
# one of these is expected even in an English-only build.
LOGGER_CALL_RE = re.compile(
    r"\b(?:AppLogger|StreamLogger|Log)\s*[.(]\s*(?:TAG\s*,\s*)?(.*?)\)",
    re.S,
)
COMMENT_PREFIX_RE = re.compile(r"^\s*(?://|\*|/\*|#)")


def logger_spans(text: str) -> list[tuple[int, int]]:
    """Character ranges covered by logging-call argument lists."""
    return [(match.start(1), match.end(1)) for match in LOGGER_CALL_RE.finditer(text)]

RED = "\033[31m"
YELLOW = "\033[33m"
GREEN = "\033[32m"
BOLD = "\033[1m"
RESET = "\033[0m"


@dataclass
class Finding:
    check: str
    location: str
    message: str


@dataclass
class AuditResult:
    findings: list[Finding] = field(default_factory=list)
    notes: list[str] = field(default_factory=list)

    @property
    def failed(self) -> bool:
        return bool(self.findings)


def strip_ansi(text: str) -> str:
    return re.sub(r"\033\[[0-9;]*m", "", text)


def collect_module_for(path: Path) -> tuple[Path, str] | None:
    """Return (resource dir, source file) for a Kotlin file, or None."""
    repo_relative = path.relative_to(REPO_ROOT).as_posix()
    for source_root, resource_root in MODULE_RESOURCE_ROOTS.items():
        if repo_relative.startswith(source_root + "/"):
            return REPO_ROOT / resource_root, repo_relative
    return None


def read_default_string_names(resource_dir: Path) -> set[str] | None:
    """Resource names defined in the unqualified ``values/strings.xml``."""
    default_file = resource_dir / "values" / "strings.xml"
    if not default_file.exists():
        return None
    root = ET.fromstring(default_file.read_text(encoding="utf-8"))
    return {element.get("name") for element in root if element.get("name")}


def list_kotlin_files() -> list[Path]:
    files: list[Path] = []
    for source_root in MODULE_RESOURCE_ROOTS:
        root = REPO_ROOT / source_root
        if not root.is_dir():
            continue
        files.extend(root.rglob("*.kt"))
    return files


def check_anchors(result: AuditResult) -> None:
    """Every fork change must still be present in the tree."""
    if not REGISTRY_PATH.exists():
        result.findings.append(
            Finding("REGISTRY", REGISTRY_PATH.relative_to(REPO_ROOT).as_posix(), "registry is missing")
        )
        return

    registry = json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
    entries = registry.get("entries", [])
    if not entries:
        result.findings.append(
            Finding("REGISTRY", REGISTRY_PATH.relative_to(REPO_ROOT).as_posix(), "registry declares no entries")
        )
        return

    lost = 0
    planned = 0
    rejected = 0
    for entry in entries:
        entry_id = entry.get("id", "<no id>")
        status = entry.get("status", "planned")
        # A planned entry describes work that has not landed yet. Its anchors are
        # the contract to satisfy, not a claim about the current tree, so they are
        # reported as pending rather than as lost.
        if status == "rejected":
            # Decided against and kept only as a record; never applied, so it is
            # neither planned nor anchor-checked.
            rejected += 1
            continue
        if status != "applied":
            planned += 1
            continue
        for anchor in entry.get("anchors", []):
            path = REPO_ROOT / anchor["file"]
            if not path.exists():
                result.findings.append(
                    Finding("ANCHOR-LOST", anchor["file"], f"{entry_id}: file no longer exists")
                )
                lost += 1
                continue
            haystack = path.read_text(encoding="utf-8", errors="replace")
            needle = anchor["contains"]
            if needle not in haystack:
                result.findings.append(
                    Finding(
                        "ANCHOR-LOST",
                        anchor["file"],
                        f"{entry_id}: {anchor.get('why', 'anchor')} not found: {needle!r}",
                    )
                )
                lost += 1

    applied = len(entries) - planned - rejected
    result.notes.append(
        f"Registry entries: {applied} applied, {planned} planned, {rejected} rejected; "
        f"anchors checked: {sum(len(e.get('anchors', [])) for e in entries if e.get('status') == 'applied')}; lost: {lost}"
    )


def check_string_references(result: AuditResult) -> None:
    """``R.string.x`` must exist in the default bucket of its own module.

    This is the check that catches an upstream string landing in Kotlin before
    anyone writes the English text: without a default-bucket entry the reference
    either fails the link or resolves against a locale the English-only APK does
    not ship.
    """
    defined_cache: dict[Path, set[str] | None] = {}
    total_refs = 0
    undefined: list[Finding] = []
    modules_without_strings: set[str] = set()

    for kotlin_file in list_kotlin_files():
        module = collect_module_for(kotlin_file)
        if module is None:
            continue
        resource_dir, repo_relative = module
        if resource_dir not in defined_cache:
            defined_cache[resource_dir] = read_default_string_names(resource_dir)
        defined = defined_cache[resource_dir]
        if defined is None:
            modules_without_strings.add(resource_dir.relative_to(REPO_ROOT).as_posix())
            continue

        text = kotlin_file.read_text(encoding="utf-8", errors="replace")
        if not STRING_REF_RE.search(text):
            continue
        for match in STRING_REF_RE.finditer(text):
            total_refs += 1
            name = match.group(1)
            if name in defined:
                continue
            line = text.count("\n", 0, match.start()) + 1
            undefined.append(
                Finding("STRING-UNDEFINED", f"{repo_relative}:{line}", f"R.string.{name} is not in values/strings.xml")
            )

    # A module with no values/strings.xml can only be a problem if it actually
    # references string resources, which the per-file skip above already knows.
    for module_path in sorted(modules_without_strings):
        result.notes.append(f"Note: {module_path} declares no strings; its R.string references are not verifiable")

    result.findings.extend(undefined)
    result.notes.append(f"Kotlin string references checked: {total_refs}; undefined: {len(undefined)}")


def check_cjk_literals(result: AuditResult) -> None:
    """New Chinese literals in Kotlin must be reviewed before they ship.

    A checked-in allowlist records every occurrence that was examined and judged
    acceptable (model prompts, log messages, parsers, wire values). Anything not
    on the list is a new upstream literal that nobody has looked at yet.
    """
    allowlist_path = REGISTRY_PATH.parent / "cjk-allowlist.json"
    allowlist: list = []
    if allowlist_path.exists():
        allowlist = json.loads(allowlist_path.read_text(encoding="utf-8")).get("allowed", [])
    reviewed = {(entry[0], entry[1]) for entry in allowlist if len(entry) == 2}

    observed: dict[tuple[str, str], str] = {}
    for kotlin_file in list_kotlin_files():
        module = collect_module_for(kotlin_file)
        if module is None:
            continue
        repo_relative = module[1]
        text = kotlin_file.read_text(encoding="utf-8", errors="replace")
        spans = logger_spans(text)
        line_start = 0
        in_block_comment = False
        for index, line in enumerate(text.splitlines()):
            stripped = line.strip()
            if in_block_comment:
                if "*/" in stripped:
                    in_block_comment = False
                line_start += len(line) + 1
                continue
            if stripped.startswith("/*"):
                in_block_comment = "*/" not in stripped
                line_start += len(line) + 1
                continue
            if COMMENT_PREFIX_RE.match(line):
                line_start += len(line) + 1
                continue
            for literal in KOTLIN_STRING_RE.findall(line):
                if not HAN_RE.search(literal):
                    continue
                offset = line_start + line.index(literal)
                if any(start <= offset < end for start, end in spans):
                    break
                # Keyed by content, not line number: moving code inside a file must
                # not invalidate a reviewed entry, and the same literal appearing
                # twice is one review, not two.
                observed.setdefault((repo_relative, literal.strip()), f"{repo_relative}:{index + 1}")
                break
            line_start += len(line) + 1

    unreviewed = {
        (path, literal): location
        for (path, literal), location in observed.items()
        if (path, literal) not in reviewed
    }
    for (path, literal), location in sorted(unreviewed.items(), key=lambda item: item[1]):
        result.findings.append(
            Finding("CJK-NEW", location, f"unreviewed Chinese literal: {literal[:100]}")
        )
    result.notes.append(
        f"Chinese literals in Kotlin: {len(observed)} distinct, {len(reviewed)} allowlisted, "
        f"{len(unreviewed)} new"
    )


def check_label_concatenation(result: AuditResult) -> None:
    """Labels must be format resources, not code that joins two of them.

    A translator cannot reorder or re-spaced ``Title: %s`` when the colon lives
    in Kotlin. Both shapes this catches — ``+ ":"`` and ``"${stringResource(X)}: $v"``
    — were present in the tree and both are now gone, which makes this a gate.
    """
    patterns = (
        (re.compile(r"stringResource\([^)]*\)\s*\+"), 'stringResource(...) + "..."'),
        (re.compile(r"\$\{stringResource\("), '"${stringResource(...)}..."'),
    )
    for kotlin_file in list_kotlin_files():
        module = collect_module_for(kotlin_file)
        if module is None:
            continue
        text = kotlin_file.read_text(encoding="utf-8", errors="replace")
        for pattern, shape in patterns:
            for match in pattern.finditer(text):
                line = text.count("\n", 0, match.start()) + 1
                result.findings.append(
                    Finding(
                        "LABEL-CONCAT",
                        f"{module[1]}:{line}",
                        f"label built by joining resources in code ({shape})",
                    )
                )


def check_locale_dirs(result: AuditResult) -> None:
    """Shipped locale directories must match what the registry declares."""
    if not REGISTRY_PATH.exists():
        return
    registry = json.loads(REGISTRY_PATH.read_text(encoding="utf-8"))
    declared = registry.get("shipped_locales")
    if declared is None:
        return

    res_root = REPO_ROOT / "app" / "src" / "main" / "res"
    qualified = sorted(
        directory.name.removeprefix("values-")
        for directory in res_root.iterdir()
        if directory.is_dir()
        and directory.name.startswith("values-")
        and directory.name != "values-night"
        and (directory / "strings.xml").exists()
    )
    has_default = (res_root / "values" / "strings.xml").exists()

    # ``default`` is a sentinel for the unqualified bucket, whose language is the
    # registry's default_locale rather than something its directory name states.
    actual = (["default"] if has_default else []) + qualified
    expected = (["default"] if registry.get("default_locale") in declared else []) + sorted(
        tag for tag in declared if tag != registry.get("default_locale")
    )

    if sorted(actual) != sorted(expected):
        result.findings.append(
            Finding(
                "UNREGISTERED",
                "app/src/main/res",
                f"shipped string buckets {actual} do not match registry declaration {expected}",
            )
        )
    result.notes.append(
        f"Shipped string buckets: {actual} (default bucket holds {registry.get('default_locale')})"
    )


def run_git(*args: str) -> str | None:
    try:
        completed = subprocess.run(
            ["git", *args], cwd=REPO_ROOT, capture_output=True, text=True, check=True
        )
    except (subprocess.CalledProcessError, FileNotFoundError):
        return None
    return completed.stdout


def report_merge_hotspots(result: AuditResult, base: str | None) -> None:
    """Files the fork edited that upstream also edited since the merge base.

    These are where a re-merge will conflict, and they are the files to read
    first when deciding whether a fork change is still needed.
    """
    if base is None:
        result.notes.append("Merge hotspots: skipped (no --base and no upstream/* ref resolved)")
        return

    fork_files = run_git("diff", "--name-only", f"{base}..HEAD")
    if fork_files is None:
        result.notes.append("Merge hotspots: skipped (git unavailable)")
        return

    fork_set = {line for line in fork_files.splitlines() if line}
    if not fork_set:
        result.notes.append("Merge hotspots: fork has no changes since merge base")
        return

    upstream_files = run_git("diff", "--name-only", f"{base}..upstream/dev")
    if upstream_files is None:
        result.notes.append("Merge hotspots: upstream/dev not fetched; run `git fetch upstream`")
        return

    upstream_set = {line for line in upstream_files.splitlines() if line}
    overlap = sorted(fork_set & upstream_set)
    for path in overlap:
        result.notes.append(f"hotspot: {path}")
    result.notes.append(
        f"Merge hotspots: {len(overlap)} of {len(fork_set)} fork-touched files also changed upstream"
    )


@dataclass
class UpstreamCommit:
    sha: str
    subject: str


@dataclass
class LedgerEntry:
    upstream_sha: str
    upstream_subject: str
    status: str
    fork_commit: str | None
    reason: str


def load_ledger() -> dict | None:
    """Return the parsed ledger, or None when it is missing/unreadable/invalid."""
    try:
        raw = LEDGER_PATH.read_text(encoding="utf-8")
    except OSError:
        return None
    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        return None
    if not isinstance(data, dict):
        return None
    return data


def parse_ledger_entries(data: dict) -> tuple[list[LedgerEntry], list[str]]:
    """Normalise ledger entries; malformed ones are reported, never fatal."""
    entries: list[LedgerEntry] = []
    problems: list[str] = []
    raw_entries = data.get("entries", [])
    if not isinstance(raw_entries, list):
        return entries, ["ledger 'entries' is not a list; nothing to cross-check"]
    for index, item in enumerate(raw_entries):
        if not isinstance(item, dict):
            problems.append(f"entry #{index} is not an object; ignored")
            continue
        sha = item.get("upstream_sha")
        if not isinstance(sha, str) or not sha.strip():
            problems.append(f"entry #{index} has no usable upstream_sha; ignored")
            continue
        status = item.get("status")
        if not isinstance(status, str) or not status:
            status = "unknown"
        subject = item.get("upstream_subject")
        reason = item.get("reason")
        fork_commit = item.get("fork_commit")
        entries.append(
            LedgerEntry(
                upstream_sha=sha.strip().lower(),
                upstream_subject=subject if isinstance(subject, str) else "",
                status=status,
                fork_commit=fork_commit if isinstance(fork_commit, str) and fork_commit else None,
                reason=reason if isinstance(reason, str) else "",
            )
        )
    return entries, problems


def sha_prefix_matches(prefix: str, full: str) -> bool:
    """True when either side is a prefix of the other, up to the shorter length."""
    prefix = prefix.strip().lower()
    full = full.strip().lower()
    if not prefix or not full:
        return False
    length = min(len(prefix), len(full))
    return prefix[:length] == full[:length]


def find_covering_entry(commit: UpstreamCommit, entries: list[LedgerEntry]) -> LedgerEntry | None:
    for entry in entries:
        if sha_prefix_matches(entry.upstream_sha, commit.sha):
            return entry
    return None


def run_ports_report(color: bool) -> int:
    """Cross-check the upstream commit ledger against the real git history."""
    paint = _make_painter(color)

    data = load_ledger()
    if data is None:
        sys.stderr.write(
            f"port ledger missing or unparseable: {LEDGER_PATH.relative_to(REPO_ROOT).as_posix()}\n"
        )
        return 2

    entries, problems = parse_ledger_entries(data)

    ledger_fork_point = data.get("fork_point")
    ledger_fork_point = ledger_fork_point.strip().lower() if isinstance(ledger_fork_point, str) else ""
    upstream_ref = data.get("upstream_ref")
    upstream_ref = upstream_ref.strip() if isinstance(upstream_ref, str) and upstream_ref.strip() else DEFAULT_UPSTREAM_REF

    lines: list[str] = []
    header = "Port ledger report"
    lines.append(paint(BOLD, header))
    lines.append("=" * len(header))

    if not ledger_fork_point:
        lines.append(paint(YELLOW, "warning: ledger has no fork_point; relying on git merge-base only"))

    if run_git("rev-parse", "--verify", "--quiet", f"{upstream_ref}^{{commit}}") is None:
        lines.append("")
        lines.append(
            f"upstream ref {upstream_ref!r} is not available; run `git fetch upstream` and retry"
        )
        sys.stdout.write("\n".join(lines) + "\n")
        return 0

    merge_base = run_git("merge-base", "HEAD", upstream_ref)
    merge_base = merge_base.strip().lower() if merge_base else None
    if not merge_base:
        lines.append(paint(RED, f"error: could not compute merge-base of HEAD and {upstream_ref}"))
        sys.stdout.write("\n".join(lines) + "\n")
        return 1

    # Re-enumerate from the git merge-base even when the ledger disagrees, so the
    # report always reflects reality; the ledger value is only a consistency check.
    if ledger_fork_point and ledger_fork_point != merge_base:
        lines.append("")
        lines.append(
            paint(
                RED,
                f"WARNING: ledger fork_point is {ledger_fork_point} but git merge-base is {merge_base}; "
                f"enumerating from git.",
            )
        )

    log_output = run_git("log", "--no-merges", "--format=%H %s", f"{merge_base}..{upstream_ref}")
    if log_output is None:
        lines.append(paint(RED, f"error: could not enumerate commits in {merge_base}..{upstream_ref}"))
        sys.stdout.write("\n".join(lines) + "\n")
        return 1

    commits: list[UpstreamCommit] = []
    for raw in log_output.splitlines():
        raw = raw.strip()
        if not raw:
            continue
        sha, _, subject = raw.partition(" ")
        commits.append(UpstreamCommit(sha=sha.lower(), subject=subject.strip()))

    unclassified: list[UpstreamCommit] = []
    classified: list[UpstreamCommit] = []
    for commit in commits:
        if find_covering_entry(commit, entries) is None:
            unclassified.append(commit)
        else:
            classified.append(commit)

    matched_full_shas: set[str] = set()
    matched_prefixes: set[str] = set()
    stale_entries: list[LedgerEntry] = []
    for entry in entries:
        matches = [commit for commit in commits if sha_prefix_matches(entry.upstream_sha, commit.sha)]
        if not matches:
            stale_entries.append(entry)
            continue
        # Prefer the full-sha match; a short prefix may cover several commits.
        matched_prefixes.add(entry.upstream_sha)
        for commit in matches:
            matched_full_shas.add(commit.sha)

    by_status: dict[str, list[LedgerEntry]] = defaultdict(list)
    for entry in entries:
        by_status[entry.status].append(entry)

    def short(sha: str) -> str:
        return sha[:12]

    lines.append("")
    lines.append(paint(RED, f"UNCLASSIFIED ({len(unclassified)})"))
    if unclassified:
        lines.append("  upstream commits after the fork point with no ledger entry:")
        for commit in unclassified:
            lines.append(f"  {short(commit.sha)}  {commit.subject}")
    else:
        lines.append("  none: every upstream commit in range is classified")

    lines.append("")
    ported = by_status.get("ported", [])
    lines.append(paint(GREEN, f"PORTED ({len(ported)})"))
    if ported:
        for entry in ported:
            target = entry.fork_commit if entry.fork_commit else "(no fork commit recorded)"
            subject = entry.upstream_subject or entry.reason or "(no subject recorded)"
            lines.append(f"  {entry.upstream_sha} -> {target}  {subject}")
    else:
        lines.append("  none")

    for label, key in (("SKIPPED", "skipped"), ("DEFERRED", "deferred"), ("SUPERSEDED", "superseded")):
        bucket = by_status.get(key, [])
        lines.append("")
        lines.append(paint(YELLOW, f"{label} ({len(bucket)})"))
        if bucket:
            for entry in bucket:
                subject = entry.upstream_subject or entry.reason or "(no subject recorded)"
                lines.append(f"  {short(entry.upstream_sha)}  {subject}")
        else:
            lines.append("  none")

    other_statuses = sorted(status for status in by_status if status not in {"ported", "skipped", "deferred", "superseded"})
    if other_statuses:
        lines.append("")
        lines.append(paint(YELLOW, "OTHER STATUSES"))
        for status in other_statuses:
            for entry in by_status[status]:
                subject = entry.upstream_subject or entry.reason or "(no subject recorded)"
                lines.append(f"  [{status}] {short(entry.upstream_sha)}  {subject}")

    lines.append("")
    lines.append(paint(RED, f"STALE-ENTRY ({len(stale_entries)})"))
    if stale_entries:
        for entry in stale_entries:
            reason = entry.reason or "(no reason recorded)"
            lines.append(f"  {short(entry.upstream_sha)} [{entry.status}]  {reason}")
    else:
        lines.append("  none")

    lines.append("")
    total = len(commits)
    classified_count = len(classified)
    lines.append(
        f"Summary: {total} non-merge upstream commits after {short(merge_base)}; "
        f"classified: {classified_count}; UNCLASSIFIED: {len(unclassified)}; "
        f"ledger entries: {len(entries)}; stale entries: {len(stale_entries)}"
    )

    if problems:
        lines.append("")
        lines.append(paint(YELLOW, "Ledger problems"))
        for problem in problems:
            lines.append(f"  {problem}")

    sys.stdout.write("\n".join(lines) + "\n")
    return 0


def _make_painter(color: bool):
    def paint(code: str, text: str) -> str:
        if not color:
            return text
        return f"{code}{text}{RESET}"

    return paint


def render(result: AuditResult, color: bool) -> str:
    def paint(code: str, text: str) -> str:
        if not color:
            return text
        return f"{code}{text}{RESET}"

    lines: list[str] = []
    header = "Fork audit"
    lines.append(paint(BOLD, header))
    lines.append("=" * len(header))

    if result.findings:
        grouped: dict[str, list[Finding]] = defaultdict(list)
        for finding in result.findings:
            grouped[finding.check].append(finding)
        for check in sorted(grouped):
            items = grouped[check]
            lines.append("")
            lines.append(paint(RED, f"{check}  ({len(items)})"))
            for item in items[:80]:
                lines.append(f"  {item.location}")
                lines.append(f"      {item.message}")
            if len(items) > 80:
                lines.append(f"  ... and {len(items) - 80} more")
    else:
        lines.append("")
        lines.append(paint(GREEN, "No regressions found."))

    if result.notes:
        lines.append("")
        lines.append(paint(YELLOW, "Notes"))
        for note in result.notes:
            lines.append(f"  {note}")

    return "\n".join(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        "--base",
        default=None,
        help="merge base to compare against (default: merge-base with upstream/dev)",
    )
    parser.add_argument("--no-hotspots", action="store_true", help="skip the upstream overlap report")
    parser.add_argument(
        "--ports",
        action="store_true",
        help="cross-check the upstream port ledger against git history and report unclassified commits",
    )
    parser.add_argument("--no-color", action="store_true")
    args = parser.parse_args()

    color = not args.no_color and sys.stdout.isatty()
    if args.ports:
        # The port report is a focused, independent mode: it does not run the
        # invariant checks and its status does not gate CI on findings.
        return run_ports_report(color)

    result = AuditResult()
    check_anchors(result)
    check_string_references(result)
    check_cjk_literals(result)
    check_label_concatenation(result)
    check_locale_dirs(result)

    if args.no_hotspots:
        result.notes.append("Merge hotspots: skipped (--no-hotspots)")
    else:
        base = args.base
        if base is None:
            merge_base = run_git("merge-base", "HEAD", "upstream/dev")
            base = merge_base.strip() if merge_base else None
        report_merge_hotspots(result, base)

    sys.stdout.write(render(result, color=not args.no_color and sys.stdout.isatty()) + "\n")
    return 1 if result.failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
