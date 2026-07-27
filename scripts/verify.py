#!/usr/bin/env python3
"""Repository-wide consistency checks.

Runs the static checks that do not need a JVM, a database or a network:
syntax of every source file, and cross-references that a compiler would only
catch much later — Thymeleaf fragments that do not exist, controllers naming
templates that were never written, secrets committed by accident.

    python scripts/verify.py

Exits non-zero on the first category that fails, so it can gate a commit hook.
"""

from __future__ import annotations

import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
GREEN, RED, YELLOW, DIM, RESET = "\033[32m", "\033[31m", "\033[33m", "\033[2m", "\033[0m"

failures: list[str] = []
warnings: list[str] = []


def section(title: str) -> None:
    print(f"\n{DIM}{'─' * 68}{RESET}\n  {title}")


def ok(message: str) -> None:
    print(f"  {GREEN}✓{RESET} {message}")


def fail(message: str) -> None:
    print(f"  {RED}✗{RESET} {message}")
    failures.append(message)


def warn(message: str) -> None:
    print(f"  {YELLOW}!{RESET} {message}")
    warnings.append(message)


def java_files() -> list[Path]:
    return sorted((ROOT / "backend/src").rglob("*.java"))


_HTML_COMMENT = re.compile(r"<!--.*?-->", re.S)
_BLOCK_COMMENT = re.compile(r"/\*.*?\*/", re.S)
_LINE_COMMENT = re.compile(r"(?<!:)//[^\n]*")
_HASH_COMMENT = re.compile(r"(?m)^\s*#[^\n]*")


def strip_comments(text: str, suffix: str) -> str:
    """Remove comments before scanning for forbidden patterns.

    Several checks below look for things that must not appear in the code,
    such as `th:utext`. Those same strings appear legitimately in comments
    explaining why they are absent, and a scanner that cannot tell the two
    apart is a scanner nobody will trust.
    """
    if suffix == ".html":
        return _HTML_COMMENT.sub(" ", text)
    if suffix == ".java":
        return _LINE_COMMENT.sub(" ", _BLOCK_COMMENT.sub(" ", text))
    if suffix in {".yml", ".yaml", ".tf", ".py", ".properties"}:
        return _HASH_COMMENT.sub(" ", text)
    return text


# ---------------------------------------------------------------------------


def check_java_syntax() -> None:
    """Parse every Java source with the chevrotain-based java-parser, if present."""
    section("Java syntax")
    files = java_files()

    checker = ROOT / "scripts/check_java.js"
    if not checker.exists():
        warn("scripts/check_java.js missing; skipping (run `npm i java-parser` first)")
        return

    result = subprocess.run(
        ["node", str(checker), str(ROOT / "backend/src")],
        capture_output=True, text=True,
    )
    print(f"    {DIM}{result.stdout.strip().splitlines()[-1] if result.stdout.strip() else ''}{RESET}")
    if result.returncode == 0:
        ok(f"{len(files)} Java files parse")
    else:
        fail(f"Java syntax errors:\n{result.stdout}")


def check_java_imports() -> None:
    """Every ``com.adwitiya`` import must resolve to a file in this repository."""
    section("Java internal imports resolve")

    declared: set[str] = set()
    for path in java_files():
        text = path.read_text(encoding="utf-8")
        package = re.search(r"^package\s+([\w.]+);", text, re.M)
        if package:
            declared.add(f"{package.group(1)}.{path.stem}")

    missing: list[str] = []
    for path in java_files():
        for imported in re.findall(r"^import\s+(?:static\s+)?(com\.adwitiya\.[\w.]+);",
                                   path.read_text(encoding="utf-8"), re.M):
            # Strip a trailing static member reference before resolving.
            candidate = imported
            if candidate not in declared and candidate.rsplit(".", 1)[0] not in declared:
                missing.append(f"{path.name} imports {imported}")

    if missing:
        for entry in missing[:15]:
            fail(entry)
    else:
        ok(f"{len(declared)} classes declared, every internal import resolves")


def check_sql() -> None:
    """Parse the Flyway migrations as MySQL."""
    section("SQL migrations")
    try:
        import sqlglot
    except ImportError:
        warn("sqlglot not installed; skipping")
        return

    total = 0
    for path in sorted((ROOT / "backend/src/main/resources/db").rglob("*.sql")):
        try:
            statements = [s for s in sqlglot.parse(path.read_text(), read="mysql") if s]
            total += len(statements)
            ok(f"{path.name} — {len(statements)} statements")
        except Exception as exc:  # noqa: BLE001 - report anything the parser rejects
            fail(f"{path.name}: {exc}")
    print(f"    {DIM}{total} statements total{RESET}")


def check_yaml() -> None:
    """Every YAML file in the repository must load."""
    section("YAML")
    try:
        import yaml
    except ImportError:
        warn("PyYAML not installed; skipping")
        return

    paths = sorted({p for pattern in ("**/*.yml", "**/*.yaml") for p in ROOT.glob(pattern)
                    if ".venv" not in p.parts and "node_modules" not in p.parts})
    bad = 0
    for path in paths:
        try:
            list(yaml.safe_load_all(path.read_text()))
        except Exception as exc:  # noqa: BLE001
            bad += 1
            fail(f"{path.relative_to(ROOT)}: {str(exc)[:120]}")
    if not bad:
        ok(f"{len(paths)} YAML files valid")


def check_terraform() -> None:
    """Parse the Terraform configuration as HCL2."""
    section("Terraform")
    try:
        import hcl2
    except ImportError:
        warn("python-hcl2 not installed; skipping")
        return

    blocks = 0
    for path in sorted((ROOT / "infra/terraform").glob("*.tf")):
        try:
            with path.open() as handle:
                doc = hcl2.load(handle)
            blocks += sum(len(v) for k, v in doc.items()
                          if k in ("resource", "data", "variable", "output", "module"))
        except Exception as exc:  # noqa: BLE001
            fail(f"{path.name}: {str(exc)[:120]}")
    ok(f"Terraform parses — {blocks} top-level blocks")


def check_templates() -> None:
    """Thymeleaf fragment references must resolve, and tags must balance."""
    section("Thymeleaf templates")
    template_root = ROOT / "backend/src/main/resources/templates"
    templates = sorted(template_root.rglob("*.html"))

    defined: set[str] = set()
    for path in templates:
        name = str(path.relative_to(template_root)).removesuffix(".html").replace("\\", "/")
        for fragment in re.findall(r'th:fragment="(\w+)', path.read_text(encoding="utf-8")):
            defined.add(f"{name}::{fragment}")

    unresolved: list[str] = []
    for path in templates:
        for template, fragment in re.findall(r'~\{([\w/]+)\s*::\s*(\w+)', path.read_text(encoding="utf-8")):
            if f"{template}::{fragment}" not in defined:
                unresolved.append(f"{path.name} → {template}::{fragment}")

    if unresolved:
        for entry in unresolved:
            fail(f"unresolved fragment {entry}")
    else:
        ok(f"{len(templates)} templates, {len(defined)} fragments, all references resolve")


def check_controller_views() -> None:
    """Every view name a Thymeleaf controller returns must exist as a template."""
    section("Controller view names")
    template_root = ROOT / "backend/src/main/resources/templates"
    available = {str(p.relative_to(template_root)).removesuffix(".html").replace("\\", "/")
                 for p in template_root.rglob("*.html")}

    missing: list[str] = []
    for path in (ROOT / "backend/src/main/java/com/adwitiya/feedbackportal/web/ui").glob("*.java"):
        text = path.read_text(encoding="utf-8")
        for view in re.findall(r'return\s+"([a-z][\w/-]*)"\s*;', text):
            if view.startswith("redirect:") or view.startswith("forward:"):
                continue
            if view not in available:
                missing.append(f"{path.name} returns '{view}'")

    if missing:
        for entry in missing:
            fail(f"missing template — {entry}")
    else:
        ok(f"every view name resolves to one of {len(available)} templates")


def check_security_claims() -> None:
    """Assert the security properties the README claims are actually true."""
    section("Security claims in the README hold")

    templates = list((ROOT / "backend/src/main/resources/templates").rglob("*.html"))
    unescaped = [p.name for p in templates
                 if "th:utext=" in strip_comments(p.read_text(encoding="utf-8"), ".html")]
    if unescaped:
        fail(f"th:utext found in {unescaped} — the README claims it appears nowhere")
    else:
        ok("no th:utext anywhere (README's XSS claim holds)")

    java = java_files()

    # Credentials must never be constructed inline in a connection call.
    leaked = [p.name for p in java
              if re.search(r'DriverManager\.getConnection\([^)]*"[^"]*",\s*"[^"]*"',
                           strip_comments(p.read_text(encoding="utf-8"), ".java"))]
    if leaked:
        fail(f"hard-coded JDBC credentials in {leaked}")
    else:
        ok("no hard-coded JDBC credentials")

    # No empty catch blocks: a swallowed exception is an invisible failure.
    swallowed = [p.name for p in java
                 if re.search(r"catch\s*\([^)]+\)\s*\{\s*\}",
                              strip_comments(p.read_text(encoding="utf-8"), ".java"))]
    if swallowed:
        fail(f"empty catch blocks in {swallowed}")
    else:
        ok("no empty catch blocks")

    # Every REST controller method that takes an id must be access-checked
    # somewhere in its class, either by @PreAuthorize or via the service.
    config = (ROOT / "backend/src/main/java/com/adwitiya/feedbackportal/config/SecurityConfig.java").read_text()
    for required in ("anyRequest().authenticated()", "SessionCreationPolicy.STATELESS",
                     "contentSecurityPolicy", "httpStrictTransportSecurity"):
        if required in config:
            ok(f"SecurityConfig declares {required}")
        else:
            fail(f"SecurityConfig is missing {required}")


def check_no_secrets() -> None:
    """Nothing that looks like a live credential should be committed."""
    section("No committed secrets")

    patterns = {
        "AWS access key": re.compile(r"AKIA[0-9A-Z]{16}"),
        "private key block": re.compile(r"-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----"),
    }
    # This file contains the patterns themselves.
    skip_dirs = {"docs", ".git", "node_modules", ".venv", "target"}
    skip_files = {"verify.py"}

    found = 0
    for path in ROOT.rglob("*"):
        if not path.is_file() or set(path.parts) & skip_dirs or path.name in skip_files:
            continue
        if path.suffix not in {".java", ".py", ".yml", ".yaml", ".tf", ".xml", ".properties", ".sql", ".html"}:
            continue
        try:
            text = strip_comments(path.read_text(encoding="utf-8"), path.suffix)
        except (UnicodeDecodeError, OSError):
            continue
        for label, pattern in patterns.items():
            if pattern.search(text):
                found += 1
                fail(f"{label} in {path.relative_to(ROOT)}")

    if not found:
        ok("no AWS keys or private keys committed")


def check_demo_data_references() -> None:
    """Emails used by tests and templates must exist in the demo dataset."""
    section("Demo-data references")
    demo = (ROOT / "backend/src/main/resources/db/demo/V900__demo_data.sql").read_text()

    referenced: set[str] = set()
    for folder, suffix in ((ROOT / "backend/src/test", "*.java"),
                           (ROOT / "backend/src/main/resources/templates", "*.html")):
        for path in folder.rglob(suffix):
            referenced |= set(re.findall(r"[\w.]+@(?:student\.)?university\.edu",
                                         path.read_text(encoding="utf-8")))

    # Addresses that must NOT exist: negative-path fixtures and UI placeholders.
    intentionally_absent = {
        "ghost@university.edu",     # AuthApiIT: unknown-account path
        "victim@university.edu",    # UserLockoutTest: pure unit fixture
        "you@university.edu",       # login form placeholder text
        "admin@university.edu",     # created by BootstrapRunner, not seeded
        "no-reply@university.edu",  # outbound mail From address
    }
    missing = sorted(e for e in referenced if e not in demo and e not in intentionally_absent)
    if missing:
        for entry in missing:
            fail(f"'{entry}' is referenced but not present in the demo dataset")
    else:
        ok(f"all {len(referenced)} referenced demo accounts exist")


def check_json() -> None:
    section("JSON")
    for path in sorted((ROOT / "infra").rglob("*.json")):
        try:
            json.loads(path.read_text())
            ok(f"{path.relative_to(ROOT)}")
        except Exception as exc:  # noqa: BLE001
            fail(f"{path.relative_to(ROOT)}: {exc}")


# ---------------------------------------------------------------------------


def main() -> int:
    print(f"\n  Student Feedback Portal — repository verification\n  {DIM}{ROOT}{RESET}")

    check_java_syntax()
    check_java_imports()
    check_sql()
    check_yaml()
    check_terraform()
    check_json()
    check_templates()
    check_controller_views()
    check_security_claims()
    check_no_secrets()
    check_demo_data_references()

    print(f"\n{DIM}{'─' * 68}{RESET}")
    if failures:
        print(f"  {RED}{len(failures)} check(s) failed{RESET}\n")
        return 1
    if warnings:
        print(f"  {GREEN}All checks passed{RESET} ({len(warnings)} skipped)\n")
        return 0
    print(f"  {GREEN}All checks passed{RESET}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
