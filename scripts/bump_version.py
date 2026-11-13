#!/usr/bin/env python3
"""
مدیریت خودکار نسخه MarbleDU هنگام merge روی main.

قوانین (عمدی و کند):
- versionCode همیشه دقیقاً ۱ واحد بالا می‌رود (هرگز بیشتر).
- versionName فقط وقتی تغییر می‌کند که در CHANGELOG.md یک بخش جدیدِ بالاتر
  از نسخه‌ی فعلی اعلام شده باشد (مثلاً «## 1.4.0 – ...»). یعنی نسخه‌ی اصلی
  برنامه دستِ ایجنت‌ها/نگهدارنده‌هاست و با هر merge بی‌دلیل بالا نمی‌رود.

خروجی: فایل version.properties را درجا اصلاح می‌کند و در stdout چاپ می‌کند
چه چیزی تغییر کرد. برای اجرا روی main توسط workflow «Build APK» است.
"""

from __future__ import annotations

import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VERSION_FILE = ROOT / "version.properties"
CHANGELOG = ROOT / "CHANGELOG.md"

SEMVER_RE = re.compile(r"(\d+)\.(\d+)\.(\d+)")
CHANGELOG_HEADING_RE = re.compile(r"^##\s+(.*)$", re.MULTILINE)
FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"


def normalize_digits(text: str) -> str:
    """اعداد فارسی/عربی را به ASCII تبدیل می‌کند (CHANGELOG با ارقام فارسی نوشته شده)."""
    for index, digit in enumerate(FA_DIGITS):
        text = text.replace(digit, str(index))
    for index, digit in enumerate(AR_DIGITS):
        text = text.replace(digit, str(index))
    return text


def read_version_file() -> dict[str, str]:
    values: dict[str, str] = {}
    for raw in VERSION_FILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        key, sep, value = line.partition("=")
        if sep:
            values[key.strip()] = value.strip()
    return values


def write_version_file(values: dict[str, str], comment: list[str]) -> None:
    lines = list(comment)
    for key, value in values.items():
        lines.append(f"{key}={value}")
    VERSION_FILE.write_text("\n".join(lines) + "\n", encoding="utf-8")


def top_changelog_version() -> str | None:
    if not CHANGELOG.exists():
        return None
    text = CHANGELOG.read_text(encoding="utf-8")
    match = CHANGELOG_HEADING_RE.search(text)
    if not match:
        return None
    semver = SEMVER_RE.search(normalize_digits(match.group(1)))
    return semver.group(0) if semver else None


def semver_tuple(version: str) -> tuple[int, int, int]:
    match = SEMVER_RE.fullmatch(version.strip())
    if not match:
        raise ValueError(f"not a semver: {version}")
    return int(match.group(1)), int(match.group(2)), int(match.group(3))


def main() -> int:
    if not VERSION_FILE.exists():
        print("version.properties not found", file=sys.stderr)
        return 1

    current = read_version_file()
    name = current.get("versionName", "1.0.0")
    try:
        code = int(current.get("versionCode", "1"))
    except ValueError:
        print(f"versionCode is not an integer: {current.get('versionCode')}", file=sys.stderr)
        return 1

    comment = [
        "# نسخه‌ی فعلی اپ – این فایل تنها منبع حقیقت برای versionName و versionCode است.",
        "# versionCode با هر merge روی main توسط workflow «Build APK» دقیقاً ۱ واحد زیاد می‌شود.",
        "# versionName فقط زمانی زیاد می‌شود که CHANGELOG.md یک بخش جدیدِ بالاتر معرفی کند؛",
        "# یعنی بالا رفتن نسخه آگاهانه و کند است و خودبه‌خود با هر merge رخ نمی‌دهد.",
    ]

    changelog_top = top_changelog_version()
    new_name = name
    adopted = False
    if changelog_top:
        try:
            if semver_tuple(changelog_top) > semver_tuple(name):
                new_name = changelog_top
                adopted = True
                print(f"versionName adopted from CHANGELOG.md: {name} -> {new_name}")
            else:
                print(f"versionName stays {name} (CHANGELOG top section: {changelog_top})")
        except ValueError:
            print(f"warning: ignoring malformed CHANGELOG version: {changelog_top}")
    else:
        print("warning: no versioned section found in CHANGELOG.md; versionName stays as is")

    # برای workflow: یعنی این merge یک بخش تغییرنگاری جدید داشت یا نه.
    github_output = os.environ.get("GITHUB_OUTPUT")
    if github_output:
        with open(github_output, "a", encoding="utf-8") as handle:
            handle.write(f"adopted={'true' if adopted else 'false'}\n")
    print(f"ADOPTED={'true' if adopted else 'false'}")

    new_code = code + 1
    values = dict(current)
    values["versionName"] = new_name
    values["versionCode"] = str(new_code)
    write_version_file(values, comment)
    print(f"versionCode {code} -> {new_code}; versionName={new_name}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
