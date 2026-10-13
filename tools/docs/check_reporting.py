#!/usr/bin/env python3
"""Checks the mechanically checkable rules of docs/REPORTING_POLICY.json.

Run from the repository root: python3 tools/docs/check_reporting.py
Standard library only. Exits non-zero and lists every failure.
"""
import json
import pathlib
import re
import sys
import unicodedata

ROOT = pathlib.Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs"
HEADING = re.compile(r"^## Implementation and verification — (\d{4}-\d{2}-\d{2})\s*$", re.M)
SHA40 = re.compile(r"`([0-9a-f]{7,40})`")
EXPECTED = {f"M{n:02d}": "SHIPPED" for n in range(1, 7)}
EXPECTED.update({"M07": "PLAYTESTING", "M08": "PLANNED - NOT READY",
                 "M09": "PLANNED - NOT READY", "M10": "PLANNED - NOT READY",
                 "M11": "PLANNED - READY"})
SECTIONS = ["**Metadata:**", "**Implementation**", "**CI**", "**Tests**",
            "**Known gaps**", "**Owner playtest**"]
CATEGORIES = ["Server", "Client", "Dedicated server", "Negative controls"]
LINK = re.compile(r"(?<!!)\[[^\]]*\]\(([^)\s]+)\)")

errors = []


def fail(msg):
    errors.append(msg)


def words(text):
    return len(re.findall(r"[A-Za-z0-9][A-Za-z0-9_./#'’-]*", text))


def slug(heading):
    s = heading.strip().lower()
    s = re.sub(r"[*_`]", "", s)
    s = "".join(c for c in s if c in " -_" or unicodedata.category(c)[0] in "LN")
    return s.replace(" ", "-")


def anchors(path):
    seen, out = {}, set()
    in_fence = False
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("```"):
            in_fence = not in_fence
        m = None if in_fence else re.match(r"^#{1,6}\s+(.*?)\s*#*\s*$", line)
        if m:
            base = slug(re.sub(r"\[([^\]]*)\]\([^)]*\)", r"\1", m.group(1)))
            n = seen.get(base, 0)
            seen[base] = n + 1
            out.add(base if n == 0 else f"{base}-{n}")
    return out


policy = json.loads((DOCS / "REPORTING_POLICY.json").read_text(encoding="utf-8"))
allowed = policy["milestone_status_schema"]["allowed_values"]
max_words = policy["documentation_policy"]["max_report_words"]

# Appended verification sections.
for n in range(1, 8):
    mid = f"M{n:02d}"
    path = DOCS / f"MILESTONE_{n:02d}.md"
    text = path.read_text(encoding="utf-8")
    heads = list(HEADING.finditer(text))
    if len(heads) != 1:
        fail(f"{path.name}: expected exactly one verification heading, found {len(heads)}")
        continue
    section = text[heads[0].start():]
    if re.search(r"^#{1,2} ", section[3:], re.M):
        fail(f"{path.name}: verification section is not the last section")
    body = section.split("\n", 1)[1]
    if words(body) > max_words:
        fail(f"{path.name}: verification section has {words(body)} words (> {max_words})")
    pos = -1
    for s in SECTIONS:
        p = body.find(s)
        if p < 0 or p < pos:
            fail(f"{path.name}: section {s} missing or out of order")
        pos = max(pos, p)
    meta = re.search(r"\*\*Metadata:\*\*\s*(M\d{2})\s*\|\s*([^|]+?)\s*\|", body)
    if not meta or meta.group(1) != mid:
        fail(f"{path.name}: metadata milestone id missing or not {mid}")
    elif meta.group(2) not in allowed:
        fail(f"{path.name}: status '{meta.group(2)}' is not an allowed label")
    elif meta.group(2) != EXPECTED[mid]:
        fail(f"{path.name}: status {meta.group(2)} != expected {EXPECTED[mid]}")
    for c in CATEGORIES:
        if not re.search(rf"^\| {re.escape(c)} \| (PASS|FAIL|NOT RUN) \|", body, re.M):
            fail(f"{path.name}: test category '{c}' missing or result invalid")
    for label in ("- Commits:", "- Source SHA:"):
        line = next((l for l in body.splitlines() if l.startswith(label)), None)
        if line is None:
            fail(f"{path.name}: '{label}' line missing")
            continue
        shas = SHA40.findall(line)
        if not shas and "UNAVAILABLE" not in line:
            fail(f"{path.name}: '{label}' has no SHA")
        for sha in shas:
            if len(sha) != 40:
                fail(f"{path.name}: '{label}' SHA {sha} is not 40 characters")
    gaps = re.search(r"\*\*Known gaps\*\*\n(.*?)\n\*\*Owner playtest\*\*", body, re.S)
    if gaps and len(re.findall(r"^- ", gaps.group(1), re.M)) > 5:
        fail(f"{path.name}: more than 5 known gaps")
    plays = body.split("**Owner playtest**", 1)[-1]
    if len(re.findall(r"^- ", plays, re.M)) > 5:
        fail(f"{path.name}: more than 5 owner playtest steps")

# Roadmap statuses.
roadmap = (DOCS / "ROADMAP.md").read_text(encoding="utf-8")
for mid, status in EXPECTED.items():
    m = re.search(rf"^\| {mid} \|[^|]*\| \*\*([^*]+)\*\* \|", roadmap, re.M)
    if not m:
        fail(f"ROADMAP.md: status row for {mid} not found")
    elif m.group(1) != status:
        fail(f"ROADMAP.md: {mid} is {m.group(1)}, expected {status}")

# No standalone reports outside the archive.
for p in DOCS.rglob("MILESTONE_*_REPORT.md"):
    if "archive" not in p.relative_to(DOCS).parts:
        fail(f"standalone report outside archive: {p.relative_to(ROOT)}")

# Relative links and anchors in active docs (archive is historical and skipped).
active = [ROOT / "README.md", ROOT / "CLAUDE.md"]
active += sorted(DOCS.glob("*.md"))
active += sorted((DOCS / "Joe's To-Do").glob("*.md"))
for src in active:
    in_fence = False
    for no, line in enumerate(src.read_text(encoding="utf-8").splitlines(), 1):
        if line.startswith("```"):
            in_fence = not in_fence
        if in_fence:
            continue
        for target in LINK.findall(line):
            if re.match(r"^[a-z]+:", target):
                continue
            path_part, _, frag = target.partition("#")
            path_part = path_part.replace("%20", " ")
            dest = (src.parent / path_part).resolve() if path_part else src
            where = f"{src.relative_to(ROOT)}:{no}"
            if not dest.exists():
                fail(f"{where}: broken link {target}")
            elif frag and dest.suffix == ".md" and frag.lower() not in anchors(dest):
                fail(f"{where}: missing anchor #{frag} in {dest.relative_to(ROOT)}")

if errors:
    print("\n".join(f"FAIL {e}" for e in errors))
    sys.exit(1)
print("PASS reporting policy checks")
