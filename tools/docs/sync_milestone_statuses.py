#!/usr/bin/env python3
"""Mirror MILESTONE_XX.md status YAML headers into ROADMAP.md status cells.

This tool only copies explicit labels. It never infers approval from tests or CI.
Usage:
  python3 tools/docs/sync_milestone_statuses.py --check
  python3 tools/docs/sync_milestone_statuses.py --write
"""
import argparse
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
NAME = re.compile(r"MILESTONE_(\d{2})\.md\Z")
FRONT = re.compile(r"\A---\r?\n(.*?)\r?\n---(?:\r?\n|\Z)", re.S)
ROAD_ROW = re.compile(r"^(\|\s*(M\d{2})\s*\|[^|\n]*\|\s*)\*\*([^*\n]+)\*\*(\s*\|.*)$")
ROAD_PREFIX = re.compile(r"^\|\s*M\d{2}\s*\|")


class StatusError(ValueError):
    """Milestone metadata or roadmap structure is not valid."""


def read_milestone_statuses(docs: pathlib.Path) -> dict[str, str]:
    policy = json.loads((docs / "REPORTING_POLICY.json").read_text(encoding="utf-8"))
    allowed = set(policy["milestone_status_schema"]["allowed_values"])
    statuses = {}
    for path in sorted(docs.glob("MILESTONE_??.md")):
        m = NAME.fullmatch(path.name)
        if m is None:
            raise StatusError(f"Unexpected milestone filename: {path.name}")
        mid = "M" + m.group(1)
        contents = path.read_text(encoding="utf-8")
        front = FRONT.match(contents)
        if front is None:
            raise StatusError(f"{path.name}: missing initial --- YAML header")
        fields = {}
        for line in front.group(1).splitlines():
            if not line.strip():
                continue
            if ":" not in line:
                raise StatusError(f"{path.name}: invalid header line {line!r}")
            key, value = [part.strip() for part in line.split(":", 1)]
            if key not in ("milestone", "status") or key in fields or not value:
                raise StatusError(f"{path.name}: invalid, duplicate or empty field {key!r}")
            fields[key] = value
        if set(fields) != {"milestone", "status"} or fields["milestone"] != mid:
            raise StatusError(f"{path.name}: milestone header must match {mid}")
        if fields["status"] not in allowed:
            raise StatusError(f"{path.name}: unsupported status {fields['status']!r}")
        statuses[mid] = fields["status"]
    if not statuses:
        raise StatusError("No milestone files found")
    return statuses


def rewrite_roadmap(source: str, statuses: dict[str, str]) -> str:
    """Replace ONLY bold status cells of existing rows. Preserve all other bytes."""
    seen = set()
    out = []
    for line in source.splitlines(keepends=True):
        stripped = line.rstrip("\r\n")
        eol = line[len(stripped):]
        if not ROAD_PREFIX.match(stripped):
            out.append(line)
            continue
        row = ROAD_ROW.fullmatch(stripped)
        if row is None:
            raise StatusError(f"Malformed milestone roadmap row: {stripped[:120]}")
        prefix, mid, old, suffix = row.groups()
        if mid in seen:
            raise StatusError(f"Duplicate roadmap row: {mid}")
        if mid not in statuses:
            raise StatusError(f"Roadmap {mid} has no milestone metadata")
        seen.add(mid)
        out.append(prefix + "**" + statuses[mid] + "**" + suffix + eol)
    missing = sorted(set(statuses) - seen)
    if missing:
        raise StatusError("Missing roadmap rows: " + ", ".join(missing))
    return "".join(out)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument("--write", action="store_true", help="update only ROADMAP status cells")
    mode.add_argument("--check", action="store_true", help="check consistency (default)")
    parser.add_argument("--docs", type=pathlib.Path, default=ROOT / "docs")
    args = parser.parse_args(argv)
    try:
        statuses = read_milestone_statuses(args.docs)
        path = args.docs / "ROADMAP.md"
        current = path.read_text(encoding="utf-8")
        generated = rewrite_roadmap(current, statuses)
        if current != generated:
            if args.write:
                path.write_text(generated, encoding="utf-8")
                print("UPDATED roadmap milestone statuses")
                return 0
            print("FAIL roadmap statuses differ from milestone headers (run --write)")
            return 1
        print("PASS milestone and roadmap statuses match")
        return 0
    except (OSError, ValueError, KeyError) as exc:
        print(f"FAIL status synchronization: {exc}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
