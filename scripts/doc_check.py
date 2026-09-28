#!/usr/bin/env python3
"""Deterministic docs quality check for help-desk.

Emits:
    CHECK <name>: PASS|FAIL
    file:line: <name>: <detail>   for each issue
    METRIC issues=<n>
    METRIC score=<pass fraction>

Checks (higher score = healthier docs):
  structure  — per-file markdown hygiene
  refs       — backticked/link file references resolve
  canonical  — spec statements agree across docs and with code
"""

import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

MD_GLOBS = [
    "*.md",
    "docs/**/*.md",
    "hd-web/**/*.md",
    "hd-service/**/*.md",
    ".github/**/*.md",
]
EXCLUDE_DIRS = ("docs/plans", "hd-web/docs/plans", "hd-service/docs/plans")
FILE_EXT = re.compile(r"\.(md|ts|tsx|js|java|kt|kts|yaml|yml|json|css|sql|xml|html|toml)$")
KNOWN_ENUM_VALUES = {
    "status": ["open", "in_progress", "resolved", "closed"],
    "priority": ["low", "medium", "high"],
    "category": ["hardware", "software", "access", "other"],
    "role": ["requester", "agent"],
}
SEED_EMAILS = ("agent@b.co", "requester@b.co")
LEGIT_DOUBLE_FILES = ("build.gradle.kts",)

issues = []
checks = {}


def add(check, file, line, detail):
    issues.append((check, file, line, detail))


def md_files():
    out = []
    for g in MD_GLOBS:
        out += glob.glob(os.path.join(ROOT, g), recursive=True)
    files = sorted(
        os.path.relpath(p, ROOT)
        for p in set(out)
        if os.path.isfile(p)
        and not os.path.relpath(p, ROOT).startswith(EXCLUDE_DIRS)
        and "node_modules" not in p
    )
    return files


def read(rel):
    with open(os.path.join(ROOT, rel), encoding="utf-8") as f:
        return f.read()


def code_spans(text):
    """Line numbers inside fenced code blocks."""
    inside, lines = False, set()
    for i, ln in enumerate(text.splitlines(), 1):
        if re.match(r"^\s*```", ln):
            inside = not inside
        elif inside:
            lines.add(i)
    return lines


# ---------- structural checks (per file) ----------

def check_structure(rel, text):
    """Per-file markdown hygiene: fences, headings, tables, whitespace."""
    n = 0
    codelines = code_spans(text)
    h1 = 0
    prev_level = 0
    in_table = False
    in_code = False
    for i, ln in enumerate(text.splitlines(), 1):
        if re.match(r"^\s*```", ln):
            in_code = not in_code
            continue
        if i in codelines or in_code:
            continue
        if ln != ln.rstrip() and ln.strip():
            add("structure", rel, i, "trailing whitespace")
            n += 1
        m = re.match(r"^(#{1,6})\s+(.*)$", ln)
        if m:
            level = len(m.group(1))
            if level == 1:
                h1 += 1
            if prev_level and level > prev_level + 1:
                add("structure", rel, i, f"heading jumps h{prev_level}->h{level}")
                n += 1
            if m.group(2).rstrip().endswith((".", ",", ";", ":")):
                add("structure", rel, i, f"heading ends with punctuation: {m.group(2).strip()}")
                n += 1
            prev_level = level
        if ln.strip().startswith("|"):
            if not in_table:
                in_table = True
                continue
            # same column count as header row? count outer pipes
            cells = ln.strip().strip("|").split("|")
            if len(cells) < 2:
                add("structure", rel, i, "table row has fewer than 2 columns")
                n += 1
        elif in_table:
            in_table = False
    if ".github/" not in rel:
        if h1 == 0:
            add("structure", rel, 1, "missing H1 title")
            n += 1
        if h1 > 1:
            add("structure", rel, 1, f"{h1} H1 headings")
            n += 1
    if text.count("```") % 2 != 0:
        add("structure", rel, len(text.splitlines()), "unbalanced code fences")
        n += 1
    checks[rel + ":structure"] = n == 0


def check_refs(rel, text):
    """File references (backticked paths, md links) resolve on disk."""
    n = 0
    fdir = os.path.dirname(os.path.join(ROOT, rel))
    app_root = None
    if rel.startswith("hd-web/"):
        app_root = os.path.join(ROOT, "hd-web")
    elif rel.startswith("hd-service/"):
        app_root = os.path.join(ROOT, "hd-service")
    seen = set()

    for i, ln in enumerate(text.splitlines(), 1):
        for tok in re.findall(r"`([^`\s]+)`", ln) + re.findall(
            r"\[[^\]]*\]\(([^)\s]+)\)", ln
        ):
            t = tok.strip().rstrip(".,;)")
            if (
                t.startswith(("http", "#", "/"))
                or "<" in t
                or "/" not in t
            ):
                continue
            if not FILE_EXT.search(t) and not t.endswith("/**"):
                continue
            if t in seen:
                continue
            seen.add(t)
            cands = [
                os.path.normpath(os.path.join(fdir, t)),
                os.path.normpath(os.path.join(ROOT, t)),
            ]
            if app_root:
                cands.append(os.path.join(app_root, t))
                cands.append(os.path.join(app_root, "src", t))
                cands.append(os.path.join(app_root, "docs", t))
            else:
                for app in ("hd-web", "hd-service"):
                    cands.append(os.path.join(ROOT, app, "src", t))
            ok = False
            for c in cands:
                if "*" in t:
                    base = c.split("*")[0].rstrip("/")
                    if glob.glob(c) or os.path.isdir(base):
                        ok = True
                        break
                elif os.path.exists(c):
                    ok = True
                    break
            if not ok:
                add("refs", rel, i, f"unresolvable file reference `{t}`")
                n += 1
    checks[rel + ":refs"] = n == 0


# ---------- canonical checks (spec agreement) ----------

def check_error_shape(files):
    """Error body must be { code, message } everywhere (code truth: ErrorResponse)."""
    n = 0
    for rel in files:
        text = read(rel)
        for i, ln in enumerate(text.splitlines(), 1):
            if re.search(r'\{\s*"error"\s*:\s*', ln):
                add("error-shape", rel, i, 'documents `{ "error": "message" }`; actual shape is { "code", "message" }')
                n += 1
    checks["canonical:error-shape"] = n == 0


def check_enum_naming(files):
    """Status enum is in_progress; `in progress`/`in-progress` is a spec bug."""
    n = 0
    pat = re.compile(r"`in progress`|`in-progress`|IN PROGRESS|in progress →", re.I)
    for rel in files:
        for i, ln in enumerate(read(rel).splitlines(), 1):
            if pat.search(ln) and "in_progress" not in ln:
                add("enum-naming", rel, i, "status written as `in progress`; wire value is `in_progress`")
                n += 1
    checks["canonical:enum-naming"] = n == 0


def check_seed_emails(files):
    """Seeded accounts are agent@b.co / requester@b.co (SeedUsers.java)."""
    n = 0
    for rel in files:
        if "plans/" in rel:
            continue
        for i, ln in enumerate(read(rel).splitlines(), 1):
            if re.search(r"\b(a@b\.co|b@b\.co|Ada|Bea)\b", ln):
                add("seed-emails", rel, i, "old seed names/emails; seeds are agent@b.co and requester@b.co")
                n += 1
    checks["canonical:seed-emails"] = n == 0


def check_password_min(files):
    """No minimum password length on login (auth.md + LoginRequest)."""
    n = 0
    for rel in files:
        for i, ln in enumerate(read(rel).splitlines(), 1):
            if re.search(r"password.*min\s*8|min\s*8.*password", ln, re.I):
                add("password-min", rel, i, "states password min 8; login spec and code impose no minimum")
                n += 1
    checks["canonical:password-min"] = n == 0


def check_impl_status(files):
    """Endpoints documented as 'planned' in service api docs must not exist in code."""
    n = 0
    impl = set()
    for c in glob.glob(os.path.join(ROOT, "hd-service/src/main/java/**/*.java"), recursive=True):
        src = open(c).read()
        base = re.search(r'@RequestMapping\(path = "([^"]+)"\)', src)
        prefix = base.group(1) if base else ""
        for m in re.finditer(r'@(Get|Post|Patch|Put|Delete)Mapping\(?("([^"]*)")?\)?', src):
            impl.add((m.group(1).upper(), prefix + (m.group(3) or "")))
    for rel in files:
        if not rel.startswith("hd-service/docs/api/"):
            continue
        text = read(rel)
        planned_block = re.search(r"implemented:? (.+?)(planned|\.|$)", text, re.I | re.S)
        declares = planned_block is not None
        for m in re.finditer(r"##\s*`?(GET|POST|PATCH|PUT|DELETE)\s+(/[^\s`]*)`?", text):
            method, path = m.group(1), m.group(2).replace("{id}", "{id}").replace(":id", "{id}")
            exists = any(
                mm == method and re.fullmatch(pp.replace("{id}", r"\{id\}"), path)
                for mm, pp in impl
            )
            if exists:
                continue
            if not declares:
                add("impl-status", rel, 1, f"{method} {path} not implemented and not declared planned")
                n += 1
            # declared planned is fine
    checks["canonical:impl-status"] = n == 0


def check_status_enum_set(files):
    """Any doc listing the status enum must include all four wire values."""
    n = 0
    pat = re.compile(r"open.*resolved.*closed", re.S)
    for rel in files:
        text = read(rel)
        for m in pat.finditer(text):
            seg = m.group(0)
            if "in_progress" not in seg and "in progress" not in seg:
                line = text[: m.start()].count("\n") + 1
                add("enum-set", rel, line, "status enum listed without in_progress")
                n += 1
    checks["canonical:enum-set"] = n == 0


def check_field_limits(files):
    """title 120 / description 4000 / comment 2000 must agree wherever stated."""
    n = 0
    limits = [
        (r"title[^.;|]*?max\s*(\d+)", "120", "title"),
        (r"description[^.;|]*?max\s*(\d+)", "4000", "description"),
        (r"body[^.;|]*?max\s*(\d+)", "2000", "comment body"),
    ]
    for rel in files:
        for i, ln in enumerate(read(rel).splitlines(), 1):
            for pat, want, name in limits:
                m = re.search(pat, ln, re.I)
                if m and m.group(1) != want:
                    add("field-limits", rel, i, f"{name} max {m.group(1)}, spec is {want}")
                    n += 1
    checks["canonical:field-limits"] = n == 0


def check_links(files):
    """Markdown links with relative targets resolve."""
    n = 0
    for rel in files:
        fdir = os.path.dirname(os.path.join(ROOT, rel))
        for i, ln in enumerate(read(rel).splitlines(), 1):
            for m in re.finditer(r"\[[^\]]*\]\(([^)\s]+)\)", ln):
                t = m.group(1)
                if t.startswith(("http", "#", "mailto:")):
                    continue
                t = t.split("#")[0]
                if not t:
                    continue
                p = os.path.normpath(os.path.join(fdir, t))
                if not os.path.exists(p):
                    add("links", rel, i, f"broken link {t}")
                    n += 1
    checks["canonical:links"] = n == 0


def check_todos(files):
    """No TODO/FIXME/XXX markers outside prompts/templates."""
    n = 0
    for rel in files:
        if "prompts/" in rel or "ISSUE_TEMPLATE" in rel or "pull_request_template" in rel:
            continue
        for i, ln in enumerate(read(rel).splitlines(), 1):
            if re.search(r"\b(TODO|FIXME|XXX|TBD)\b", ln):
                add("todos", rel, i, "TODO/FIXME marker left in doc")
                n += 1
    checks["canonical:todos"] = n == 0


def main():
    files = md_files()
    for rel in files:
        text = read(rel)
        check_structure(rel, text)
        check_refs(rel, text)

    check_error_shape(files)
    check_enum_naming(files)
    check_seed_emails(files)
    check_password_min(files)
    check_impl_status(files)
    check_status_enum_set(files)
    check_field_limits(files)
    check_links(files)
    check_todos(files)

    for c, rel, line, detail in issues:
        print(f"{rel}:{line}: {c}: {detail}")
    print()

    total = len(checks)
    passed = sum(1 for v in checks.values() if v)
    print(f"checks: {total}  passed: {passed}  issues: {len(issues)}")
    for name, ok in sorted(checks.items()):
        if not ok:
            print(f"FAIL {name}")
    print(f"METRIC issues={len(issues)}")
    print(f"METRIC score={passed / total:.4f}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
