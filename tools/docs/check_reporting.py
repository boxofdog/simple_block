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
from sync_milestone_statuses import read_milestone_statuses, rewrite_roadmap

ROOT = pathlib.Path(__file__).resolve().parents[2]
DOCS = ROOT / "docs"
HEADING = re.compile(r"^## Implementation and verification — (\d{4}-\d{2}-\d{2})\s*$", re.M)
SHA40 = re.compile(r"`([0-9a-f]{7,40})`")
EXPECTED = read_milestone_statuses(DOCS)
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

# All milestones are discovered; no future M08+ exemptions. A planned milestone
# can have no appendix, while PLAYTESTING/SHIPPED requires one.
for path in sorted(DOCS.glob("MILESTONE_??.md")):
    mid = "M" + path.stem[-2:]
    content = path.read_text(encoding="utf-8")
    heads = list(HEADING.finditer(content))
    if not heads:
        if EXPECTED[mid] in ("PLAYTESTING", "SHIPPED"):
            fail(f"{path.name}: {EXPECTED[mid]} requires a dated verification appendix")
        continue
    for i, head in enumerate(heads):
        end = heads[i+1].start() if i+1 < len(heads) else len(content)
        section = content[head.start():end]
        if re.search(r"^#{1,2} (?!Implementation and verification —)", section[3:], re.M):
            fail(f"{path.name}: appendix must be the last substantive section")
        body = section.split("\n",1)[1] if "\n" in section else ""
        if words(body)>max_words:
            fail(f"{path.name}: appendix {i+1} has {words(body)} words (> {max_words})")
        pos=-1
        for s in SECTIONS:
            p=body.find(s)
            if p<0 or p<pos:
                fail(f"{path.name}: appendix {i+1}: {s} missing or out of order")
            pos=max(pos,p)
        meta=re.search(r"\*\*Metadata:\*\*\s*(M\d{2})\s*\|\s*([^|]+?)\s*\|",body)
        if not meta or meta.group(1)!=mid:
            fail(f"{path.name}: appendix {i+1}: wrong/missing milestone metadata")
        elif meta.group(2) not in allowed:
            fail(f"{path.name}: appendix {i+1}: unsupported status {meta.group(2)}")
        for c in CATEGORIES:
            if not re.search(rf"^\| {re.escape(c)} \| (PASS|FAIL|NOT RUN) \|",body,re.M):
                fail(f"{path.name}: appendix {i+1}: missing/invalid {c} test row")
        for label in ("- Commits:","- Source SHA:"):
            line=next((line for line in body.splitlines() if line.startswith(label)),None)
            if line is None:
                fail(f"{path.name}: appendix {i+1}: missing {label}")
                continue
            shas=SHA40.findall(line)
            if not shas and "UNAVAILABLE" not in line:
                fail(f"{path.name}: appendix {i+1}: missing SHA or UNAVAILABLE")
            for sha in shas:
                if len(sha)!=40:
                    fail(f"{path.name}: appendix {i+1}: truncated SHA {sha}")
        gaps=re.search(r"\*\*Known gaps\*\*\n(.*?)\n\*\*Owner playtest\*\*",body,re.S)
        if gaps and len(re.findall(r"^- ",gaps.group(1),re.M))>5:
            fail(f"{path.name}: appendix {i+1}: too many gaps")
        if "**Owner playtest**" in body:
            plays=body.split("**Owner playtest**",1)[-1]
            if len(re.findall(r"^- ",plays,re.M))>5:
                fail(f"{path.name}: appendix {i+1}: too many playtest steps")

# Validate roadmap using the current milestone metadata, never a hardcoded snapshot.
roadmap = (DOCS / "ROADMAP.md").read_text(encoding="utf-8")
try:
    if rewrite_roadmap(roadmap, EXPECTED) != roadmap:
        fail("ROADMAP.md: status cells differ from milestone metadata")
except ValueError as exc:
    fail(f"ROADMAP.md: {exc}")

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
