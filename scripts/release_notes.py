#!/usr/bin/env python3
"""
ساخت یادداشت‌های release به زبان ساده از CHANGELOG.md.

- اولین بخش «## ...» در CHANGELOG.md را برمی‌دارد (جدیدترین تغییرات).
- تغییرنگار باید به زبان ساده نوشته شده باشد (قانون AGENTS.md).
- اگر بخش پیدا نشد یا با versionName فعلی هم‌خوان نبود، یک متن ساده‌ی
  جایگزین می‌سازد و با کد خروجی ۰ ادامه می‌دهد تا release گیر نکند،
  ولی ::warning:: چاپ می‌کند تا کم‌بودی در یادداشت‌ها به چشم بیاید.
"""

from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CHANGELOG = ROOT / "CHANGELOG.md"
VERSION_FILE = ROOT / "version.properties"

SEMVER_RE = re.compile(r"(\d+)\.(\d+)\.(\d+)")
FA_DIGITS = "۰۱۲۳۴۵۶۷۸۹"
AR_DIGITS = "٠١٢٣٤٥٦٧٨٩"


def normalize_digits(text: str) -> str:
    """اعداد فارسی/عربی را به ASCII تبدیل می‌کند (CHANGELOG با ارقام فارسی نوشته شده)."""
    for index, digit in enumerate(FA_DIGITS):
        text = text.replace(digit, str(index))
    for index, digit in enumerate(AR_DIGITS):
        text = text.replace(digit, str(index))
    return text


def read_version_name() -> str:
    for raw in VERSION_FILE.read_text(encoding="utf-8").splitlines():
        line = raw.strip()
        if line.startswith("versionName="):
            return line.partition("=")[2].strip()
    return "0.0.0"


def extract_top_section(text: str) -> tuple[str, str] | None:
    """اولین بخش تغییرنگار را برمی‌گرداند: (عنوان بدون ##، بدنه). پارسر خطی، بدون regex روی متن چندخطی."""
    lines = text.splitlines()
    start = None
    for index, line in enumerate(lines):
        if line.startswith("## "):
            start = index
            break
    if start is None:
        return None
    end = len(lines)
    for index in range(start + 1, len(lines)):
        if lines[index].startswith("## "):
            end = index
            break
    title = lines[start][3:].strip()
    body = "\n".join(lines[start + 1:end]).strip()
    return title, body


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="release-notes.md")
    args = parser.parse_args()

    version_name = read_version_name()
    out_path = Path(args.out)

    section = extract_top_section(CHANGELOG.read_text(encoding="utf-8")) if CHANGELOG.exists() else None

    if section is None:
        out_path.write_text(
            f"نسخه‌ی {version_name} منتشر شد. این نسخه شامل رفع اشکال و بهبودهای عمومی است.\n\n"
            "فهرست کامل تغییرات در CHANGELOG.md مخزن موجود است.\n",
            encoding="utf-8",
        )
        print("::warning::CHANGELOG.md هیچ بخش تغییرنگاری نداشت؛ یادداشت عمومی ساخته شد. "
              "قوانین AGENTS.md را ببینید: هر merge باید تغییرات را به زبان ساده بنویسد.")
        return 0

    title, body = section
    top_version = SEMVER_RE.search(normalize_digits(title))
    top_version = top_version.group(0) if top_version else None

    if top_version and top_version != version_name:
        try:
            top_tuple = tuple(int(p) for p in SEMVER_RE.fullmatch(top_version).groups())  # type: ignore[union-attr]
            name_tuple = tuple(int(p) for p in SEMVER_RE.fullmatch(version_name).groups())  # type: ignore[union-attr]
            if top_tuple < name_tuple:
                print(f"::warning::نسخه‌ی CHANGELOG ({top_version}) از versionName ({version_name}) عقب‌تر است؛ "
                      "یعنی این merge احتمالاً بخش تغییرنگاری جدید نداشته. قوانین AGENTS.md را ببینید.")
        except AttributeError:
            pass

    if not body:
        print("::warning::بخش بالایی CHANGELOG خالی است؛ یادداشت عمومی ساخته شد.")
        body = "این نسخه شامل رفع اشکال و بهبودهای عمومی است."

    plain = (
        f"{body}\n\n"
        f"---\n"
        f"نسخه: {version_name}\n"
        f"فهرست کامل تغییرات نسخه‌های قبلی: فایل CHANGELOG.md مخزن\n"
    )
    out_path.write_text(plain, encoding="utf-8")
    print(f"release notes written to {out_path} ({len(plain)} bytes), from section: {title}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
