#!/usr/bin/env python3
"""Simple Block documentation automation; standard library only, never grants approval."""
import argparse
from collections import Counter, defaultdict
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import urllib.request
ROOT = Path(__file__).resolve().parents[2]
START = "<!-- GENERATED SOURCE INVENTORY START -->"
END = "<!-- GENERATED SOURCE INVENTORY END -->"

def write_or_check(path, new, write):
    old = path.read_text(encoding="utf-8") if path.exists() else ""
    if new == old:
        print("PASS", path.relative_to(ROOT))
        return 0
    if not write:
        print("OUTDATED", path.relative_to(ROOT), "(use --write)")
        return 1
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(new, encoding="utf-8")
    print("UPDATED", path.relative_to(ROOT))
    return 0

def generated_block(content, section):
    if (START in content) != (END in content):
        raise ValueError("incomplete generated README markers")
    block = START + "\n" + section.rstrip() + "\n" + END
    if START in content:
        if content.count(START) != 1 or content.count(END) != 1:
            raise ValueError("duplicate README markers")
        a = content.index(START)
        b = content.index(END, a) + len(END)
        return content[:a] + block + content[b:]
    return content.rstrip() + "\n\n" + block + "\n"

def readme(root=ROOT, write=False):
    counts = Counter()
    for scope in ("main", "client", "gametest"):
        prefix = root / "src" / scope / "java/io/github/boxofdog/simpleblock"
        for p in prefix.rglob("*.java") if prefix.is_dir() else ():
            pieces = p.relative_to(prefix).parts
            counts[(scope, pieces[0] if len(pieces)>1 else "(root)")] += 1
    if not counts:
        raise ValueError("No Java source inventory exists")
    lines = ["## Generated Java source inventory", "",
             "Counts reflect source files only, not proof of implemented or tested behavior.",
             "", "| Source set | First-level package | Java files |", "|---|---|---:|"]
    for (scope, package), count in sorted(counts.items()):
        lines.append("| " + scope + " | " + package + " | " + str(count) + " |")
    lines += ["", "Regenerate using tools/docs/automation.py readme --write"]
    path = root / "README.md"
    return write_or_check(path, generated_block(path.read_text(encoding="utf-8"), "\n".join(lines)), write)

def evidence(root=ROOT, write=False):
    base = root / "docs/evidence"
    lines = ["# Evidence inventory — generated", "",
             "File counts and paths only. Presence does not prove execution, PASS, or owner approval.",
             "", "| Location | Images | Text/logs | Other |", "|---|---:|---:|---:|"]
    for d in sorted(p for p in base.iterdir() if p.is_dir()):
        files = [p for p in d.rglob("*") if p.is_file() and p.name != ".gitkeep"]
        imgs = sum(p.suffix.lower() in (".png", ".jpg", ".jpeg", ".webp") for p in files)
        logs = sum(p.suffix.lower() in (".txt", ".log", ".md") for p in files)
        lines.append("| [" + d.name + "](" + d.name + "/) | " +
                     str(imgs) + " | " + str(logs) + " | " + str(len(files)-imgs-logs) + " |")
    lines += ["", "Regenerate using tools/docs/automation.py evidence --write", ""]
    return write_or_check(base / "INDEX.md", "\n".join(lines), write)

def new_milestone(mid, title, write=False, root=ROOT):
    if not re.fullmatch(r"M\d{2}", mid) or not title.strip() or "|" in title or "\n" in title:
        raise ValueError("Use MNN and a one-line title without pipes")
    doc = root / "docs" / ("MILESTONE_" + mid[1:] + ".md")
    if doc.exists():
        raise ValueError("Milestone already exists; refusing overwrite")
    board = root / "docs/ROADMAP.md"
    lines = board.read_text(encoding="utf-8").splitlines(keepends=True)
    indices = [i for i,l in enumerate(lines) if re.match(r"^\| M\d{2} \|", l)]
    if not indices or any(re.match(r"^\| "+mid+r" \|", l) for l in lines):
        raise ValueError("Roadmap missing or duplicate milestone")
    lines.insert(indices[-1]+1, "| " + mid + " | " + title.strip() +
        " | **PLANNED - NOT READY** | [Brief](MILESTONE_" + mid[1:] +
        ".md) | Not implemented |\n")
    if not write:
        print("PREVIEW", doc.relative_to(root), "and ROADMAP.md row; no implementation")
        return 0
    doc.write_text("---\nmilestone: " + mid + "\nstatus: PLANNED - NOT READY\n---\n\n# " +
        mid + " — " + title.strip() + "\n\n## Scope and decisions\n\n- TBD. Owner approval required.\n\n"
        "## Acceptance\n\n- NOT RUN; implementation not authorized.\n", encoding="utf-8")
    board.write_text("".join(lines), encoding="utf-8")
    e = root / "docs/evidence" / ("milestone-"+mid[1:])
    e.mkdir(parents=True, exist_ok=True)
    (e/".gitkeep").touch()
    print("CREATED", mid, "as PLANNED - NOT READY")
    return 0

def drift(root=ROOT):
    docs = root / "docs"
    errors=[]
    patterns={"BACKLOG.md":r"^\|\s*(I-\d{3})\s*\|",
              "BUG_LOG.md":r"^\|\s*(BUG-\d{3})\s*\|",
              "UX_TEXT_LOG.md":r"^\|\s*(UX-\d{3})\s*\|",
              "DESIGN_REGISTER.md":r"^\|\s*([SGTMUA]-\d{2})\s*\|"}
    ids = {"M" + f.stem[-2:] for f in docs.glob("MILESTONE_??.md")}
    board = (docs/"ROADMAP.md").read_text(encoding="utf-8")
    for mid in sorted(ids):
        if len(re.findall(r"(?m)^\|\s*"+mid+r"\s*\|", board))!=1:
            errors.append("Missing or duplicate roadmap row "+mid)
    for filename, pat in patterns.items():
        seen=set()
        content=(docs/filename).read_text(encoding="utf-8")
        for line_no, line in enumerate(content.splitlines(), 1):
            m=re.match(pat, line)
            if m:
                if m[1] in seen:
                    errors.append(filename+":"+str(line_no)+" repeats "+m[1])
                seen.add(m[1])
        for n in set(re.findall(r"MILESTONE_(\d{2})\.md", content)):
            if "M"+n not in ids:
                errors.append(filename+" references missing M"+n)
    from sync_milestone_statuses import read_milestone_statuses
    read_milestone_statuses(docs)
    for item in errors:
        print("FAIL", item)
    if not errors:
        print("PASS structural documentation drift; semantic decisions still require owner review")
    return 1 if errors else 0

def impact(base="HEAD^", head="HEAD", root=ROOT):
    try:
        changed = subprocess.check_output(["git","-C",str(root),"diff","--name-only",base,head],
            text=True, stderr=subprocess.PIPE).splitlines()
    except (FileNotFoundError, subprocess.CalledProcessError):
        print("SKIP impact hints: no comparable Git history")
        return 0
    docs = list((root/"docs").glob("MILESTONE_??.md"))
    summary=["### Source-to-document review hints (advisory only)", ""]
    for filename in changed:
        if not filename.startswith(("src/","art/","tools/models/","build.gradle","gradle.properties")):
            continue
        stem=Path(filename).stem
        matched=[p.relative_to(root).as_posix() for p in docs
                 if len(stem)>=6 and re.search(r"\b"+re.escape(stem)+r"\b",
                     p.read_text(encoding="utf-8"))]
        summary.append("- "+filename+": "+(", ".join(matched) if matched else
            "No exact class match; assess scope manually"))
    if len(summary)==2:
        summary.append("- No relevant source paths in this diff.")
    result="\n".join(summary)+"\n"
    print(result)
    if os.getenv("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"],"a",encoding="utf-8") as f:
            f.write(result)
    return 0

def github_api(url, token):
    headers={"Accept":"application/vnd.github+json","User-Agent":"simple-block-docs"}
    if token:
        headers["Authorization"]="Bearer "+token
    with urllib.request.urlopen(urllib.request.Request(url,headers=headers),timeout=25) as f:
        return json.load(f)

def ci_facts(repo, run_id=None, latest=False, test_evidence=None, api=github_api):
    if not re.fullmatch(r"[\w.-]+/[\w.-]+",repo):
        raise ValueError("Invalid GitHub repository")
    prefix="https://api.github.com/repos/"+repo
    token=os.getenv("GH_TOKEN") or os.getenv("GITHUB_TOKEN")
    if latest:
        runs=api(prefix+"/actions/runs?per_page=50&branch=main",token)["workflow_runs"]
        found=[r for r in runs if r.get("name")=="Build" and
               r.get("status")=="completed" and r.get("conclusion")=="success"]
        if not found:
            raise ValueError("No successful Build in last 50 runs")
        run_id=found[0]["id"]
    if not run_id or run_id<1:
        raise ValueError("Positive --run-id required")
    run=api(prefix+"/actions/runs/"+str(run_id),token)
    if run.get("name")!="Build":
        raise ValueError("Not a Build workflow run")
    artifacts=api(prefix+"/actions/runs/"+str(run_id)+"/artifacts?per_page=100",token)
    result={"build_number":run["run_number"],"run_id":run["id"],
        "url":run["html_url"],"head_sha":run["head_sha"],
        "result":("PASS" if run.get("conclusion")=="success" else
                  "FAIL" if run.get("status")=="completed" else "NOT RUN"),
        "artifacts":[{"name":a["name"],"id":a["id"],
            "available":not a.get("expired",True)} for a in artifacts.get("artifacts",[])],
        "tests":"UNAVAILABLE"}
    if test_evidence:
        evidence=json.loads(Path(test_evidence).read_text(encoding="utf-8"))
        if evidence.get("head_sha")!=result["head_sha"]:
            raise ValueError("Evidence SHA does not match Build")
        valid={"Server","Client","Dedicated server","Negative controls"}
        categories=evidence.get("categories")
        if not isinstance(categories,dict) or set(categories)!=valid:
            raise ValueError("All four test categories required")
        for key,value in categories.items():
            if value.get("result") not in ("PASS","FAIL","NOT RUN"):
                raise ValueError("Invalid status for "+key)
            p,t=value.get("passed"),value.get("total")
            if not isinstance(p,int) or not isinstance(t,int) or isinstance(p,bool) or isinstance(t,bool) or p<0 or t<p or (value["result"]=="PASS" and p!=t):
                raise ValueError("Invalid counts for "+key)
        result["tests"]=categories
    return result

def main(argv=None):
    parser=argparse.ArgumentParser(description=__doc__)
    sub=parser.add_subparsers(dest="command",required=True)
    for action in ("readme","evidence"):
        q=sub.add_parser(action)
        m=q.add_mutually_exclusive_group()
        m.add_argument("--write",action="store_true")
        m.add_argument("--check",action="store_true")
    q=sub.add_parser("new")
    q.add_argument("milestone");q.add_argument("title");q.add_argument("--write",action="store_true")
    sub.add_parser("drift")
    q=sub.add_parser("impact");q.add_argument("--base",default="HEAD^");q.add_argument("--head",default="HEAD")
    q=sub.add_parser("ci");q.add_argument("--repo",default="boxofdog/simple_block")
    g=q.add_mutually_exclusive_group(required=True)
    g.add_argument("--run-id",type=int);g.add_argument("--latest-successful",action="store_true")
    q.add_argument("--test-evidence",type=Path);q.add_argument("--format",choices=("json","markdown"),default="markdown")
    a=parser.parse_args(argv)
    try:
        if a.command=="readme":return readme(write=a.write)
        if a.command=="evidence":return evidence(write=a.write)
        if a.command=="new":return new_milestone(a.milestone,a.title,a.write)
        if a.command=="drift":return drift()
        if a.command=="impact":return impact(a.base,a.head)
        if a.command=="ci":
            facts=ci_facts(a.repo,a.run_id,a.latest_successful,a.test_evidence)
            if a.format=="json":
                print(json.dumps(facts,indent=2));return 0
            print("Build: #{} | Run ID: {} | {}".format(facts["build_number"],facts["run_id"],facts["result"]))
            print("Source SHA:",facts["head_sha"])
            print("URL:",facts["url"])
            print("Artifacts:",", ".join(x["name"]+" (ID "+str(x["id"])+", "+("available" if x["available"] else "EXPIRED")+")" for x in facts["artifacts"]) or "UNAVAILABLE")
            print("Tests:",json.dumps(facts["tests"]) if isinstance(facts["tests"],dict) else "UNAVAILABLE")
            print("NOTE: Build success alone cannot prove per-category game tests, client, dedicated server, negative controls, or owner playtest.")
            return 0
    except (ValueError,KeyError,OSError) as exc:
        print("FAIL",exc,file=sys.stderr);return 1
if __name__=="__main__":
    sys.exit(main())
