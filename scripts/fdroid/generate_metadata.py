#!/usr/bin/env python3

from __future__ import annotations

import dataclasses
import glob
import os
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterable


@dataclasses.dataclass(frozen=True)
class ApkInfo:
    path: Path
    version_code: int
    version_name: str
    mtime: float


_PKG_RE = re.compile(
    r"^package:\s+name='(?P<name>[^']+)'\s+versionCode='(?P<vc>[^']+)'\s+versionName='(?P<vn>[^']*)'"
)


def _run_aapt_badging(apk_path: Path) -> str:
    try:
        proc = subprocess.run(
            ["aapt", "dump", "badging", str(apk_path)],
            check=True,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
        )
        return proc.stdout
    except subprocess.CalledProcessError as e:
        raise RuntimeError(f"aapt failed for {apk_path}: {e.stdout}") from e


def _parse_apk_info(apk_path: Path) -> ApkInfo | None:
    out = _run_aapt_badging(apk_path)
    pkg_line = next((line for line in out.splitlines() if line.startswith("package: ")), None)
    if pkg_line is None:
        return None
    m = _PKG_RE.match(pkg_line)
    if not m:
        return None
    if m.group("name") != "wtf.uhoh.newsoftkeyboard":
        return None
    version_code = int(m.group("vc"))
    version_name = m.group("vn") or ""
    stat = apk_path.stat()
    return ApkInfo(path=apk_path, version_code=version_code, version_name=version_name, mtime=stat.st_mtime)


def _find_apks() -> Iterable[Path]:
    patterns = [
        "repo/wtf.uhoh.newsoftkeyboard_*.apk",
        "archive/wtf.uhoh.newsoftkeyboard_*.apk",
    ]
    for pattern in patterns:
        for p in glob.glob(pattern):
            yield Path(p)


def _dedupe_by_version_code(apks: Iterable[ApkInfo]) -> list[ApkInfo]:
    by_code: dict[int, ApkInfo] = {}
    for apk in apks:
        existing = by_code.get(apk.version_code)
        if existing is None or apk.mtime > existing.mtime:
            by_code[apk.version_code] = apk
    return sorted(by_code.values(), key=lambda a: a.version_code, reverse=True)


def _render_builds(apks: list[ApkInfo]) -> str:
    if not apks:
        return "Builds: []\n"
    lines: list[str] = ["Builds:"]
    for apk in apks:
        lines.extend(
            [
                f"  - versionName: {apk.version_name}",
                f"    versionCode: {apk.version_code}",
                "    commit: main",
                "    subdir: .",
                "    gradle:",
                "      - :ime:app:assembleRelease",
                "    output: ime/app/build/outputs/apk/release/app-release.apk",
            ]
        )
    return "\n".join(lines) + "\n"


def _update_or_insert_line(text: str, key: str, value: str) -> str:
    pattern = re.compile(rf"^{re.escape(key)}:.*$", re.MULTILINE)
    replacement = f"{key}: {value}"
    if pattern.search(text):
        return pattern.sub(replacement, text, count=1)
    # Insert near other top-level keys if possible.
    return text.rstrip() + "\n" + replacement + "\n"


def _replace_builds_block(text: str, builds_block: str) -> str:
    # Replace everything from "Builds:" to EOF if present (our metadata keeps builds at the end).
    m = re.search(r"^Builds:.*$", text, flags=re.MULTILINE)
    if not m:
        return text.rstrip() + "\n" + builds_block
    start = m.start()
    return text[:start].rstrip() + "\n" + builds_block


def main() -> int:
    metadata_path = Path("metadata/wtf.uhoh.newsoftkeyboard.yml")
    if metadata_path.exists():
        text = metadata_path.read_text(encoding="utf-8")
    else:
        # Minimal fallback if metadata was never created.
        text = (
            "Categories:\n"
            "- System\n"
            "License: Apache-2.0\n"
            "SourceCode: https://github.com/pepperpepperpepper/NewSoftKeyboard\n"
            "Name: New Soft Keyboard\n"
            "Summary: NewSoftKeyboard (AnySoftKeyboard-compatible)\n"
            "Description: |\n"
            "  NewSoftKeyboard is a hard fork of AnySoftKeyboard with add-on compatibility.\n"
            "AuthorName: Uh-Oh WTF\n"
            "AutoUpdateMode: None\n"
            "UpdateCheckMode: Static\n"
            "ArchivePolicy: KeepAll\n"
            "Builds: []\n"
        )

    apk_infos: list[ApkInfo] = []
    for apk_path in _find_apks():
        try:
            info = _parse_apk_info(apk_path)
        except Exception as e:
            print(f"warn: {e}", file=sys.stderr)
            continue
        if info is not None:
            apk_infos.append(info)

    apks = _dedupe_by_version_code(apk_infos)
    if not apks:
        raise SystemExit("No wtf.uhoh.newsoftkeyboard APKs found in repo/ or archive/.")

    current = apks[0]
    text = _update_or_insert_line(text, "AutoUpdateMode", "None")
    text = _update_or_insert_line(text, "UpdateCheckMode", "Static")
    text = _update_or_insert_line(text, "ArchivePolicy", "KeepAll")
    text = _update_or_insert_line(text, "CurrentVersion", current.version_name)
    text = _update_or_insert_line(text, "CurrentVersionCode", str(current.version_code))
    text = _replace_builds_block(text, _render_builds(apks))

    metadata_path.parent.mkdir(parents=True, exist_ok=True)
    metadata_path.write_text(text, encoding="utf-8")

    print(
        f"Updated {metadata_path} CurrentVersion={current.version_name} CurrentVersionCode={current.version_code} Builds={len(apks)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

