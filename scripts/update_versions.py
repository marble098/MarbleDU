#!/usr/bin/env python3
"""
به‌روزرسانی هفتگی نسخه‌ها به آخرین نسخه‌های منتشرشده (در صورت وجود: بتا).

- Gradle: آخرین نسخه‌ی غیر-RC از services.gradle.org
- Android Gradle Plugin: آخرین نسخه از maven-metadata گوگل
- Kotlin: از maven-metadata مرکزی
- AndroidX / Compose: از maven-metadata گوگل (شامل بتا/آلفا؛ در صورت وجود بتا)
- Vazirmatn: از npm

خروجی: gradle/libs.versions.toml، gradle/wrapper/gradle-wrapper.properties،
       gradle/versions-lock.json
"""
from __future__ import annotations

import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CATALOG = ROOT / "gradle" / "libs.versions.toml"
WRAPPER = ROOT / "gradle" / "wrapper" / "gradle-wrapper.properties"
LOCK = ROOT / "gradle" / "versions-lock.json"

GOOGLE = "https://dl.google.com/dl/android/maven2/{group}/{artifact}/maven-metadata.xml"
CENTRAL = "https://repo1.maven.org/maven2/{group}/{artifact}/maven-metadata.xml"


def fetch(url: str) -> str | None:
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return response.read().decode("utf-8")
    except Exception as exc:  # noqa: BLE001
        print(f"  ! {url}: {exc}")
        return None


def all_versions(xml: str) -> list[str]:
    return re.findall(r"<version>([^<]+)</version>", xml)


def latest(versions: list[str], allow_prerelease: bool = True) -> str | None:
    if not versions:
        return None
    cleaned = [v for v in versions if v and "SNAPSHOT" not in v]
    if not allow_prerelease:
        cleaned = [v for v in cleaned if not re.search(r"(alpha|beta|rc|RC|dev|M\d)", v)]
    return cleaned[-1] if cleaned else None


def maven(group: str, artifact: str, base: str = GOOGLE) -> list[str]:
    xml = fetch(base.format(group=group.replace(".", "/"), artifact=artifact))
    return all_versions(xml) if xml else []


def update_catalog(version_map: dict[str, str]) -> bool:
    text = CATALOG.read_text(encoding="utf-8")
    changed = False
    for key, value in version_map.items():
        if not value:
            continue
        pattern = re.compile(rf'^{re.escape(key)}\s*=\s*"[^"]*"', re.MULTILINE)
        new_line = f'{key} = "{value}"'
        if pattern.search(text):
            updated = pattern.sub(new_line, text)
            if updated != text:
                changed = True
                text = updated
    CATALOG.write_text(text, encoding="utf-8")
    return changed


def main() -> int:
    print("== Gradle ==")
    gradle_versions = []
    page = fetch("https://services.gradle.org/versions/all")
    if page:
        data = json.loads(page)
        gradle_versions = [d["version"] for d in data if not d.get("snapshot") and not d.get("rcFor")]
    newest_gradle = latest(gradle_versions, allow_prerelease=False)
    print(f"  newest stable: {newest_gradle}")

    if newest_gradle and WRAPPER.exists():
        wrapper = WRAPPER.read_text(encoding="utf-8")
        new_wrapper = re.sub(
            r"gradle-[0-9.]+-bin\.zip", f"gradle-{newest_gradle}-bin.zip", wrapper
        )
        if new_wrapper != wrapper:
            WRAPPER.write_text(new_wrapper, encoding="utf-8")
            print("  wrapper updated")

    targets = {
        "agp": ("com.android.tools.build", "gradle", GOOGLE, True),
        "kotlin": ("org.jetbrains.kotlin", "kotlin-gradle-plugin", CENTRAL, False),
        "composeBom": ("androidx.compose", "compose-bom", GOOGLE, False),
        "material3": ("androidx.compose.material3", "material3", GOOGLE, True),
        "coreKtx": ("androidx.core", "core-ktx", GOOGLE, False),
        "activityCompose": ("androidx.activity", "activity-compose", GOOGLE, False),
        "lifecycle": ("androidx.lifecycle", "lifecycle-runtime-compose", GOOGLE, False),
        "datastore": ("androidx.datastore", "datastore-preferences", GOOGLE, False),
        "coroutines": ("org.jetbrains.kotlinx", "kotlinx-coroutines-android", CENTRAL, False),
    }

    resolved: dict[str, str] = {"gradle": newest_gradle or ""}
    for key, (group, artifact, base, allow_pre) in targets.items():
        versions = maven(group, artifact, base)
        newest = latest(versions, allow_prerelease=allow_pre)
        print(f"== {key}: {newest}")
        resolved[key] = newest or ""

    changed = update_catalog(resolved)
    LOCK.write_text(json.dumps(resolved, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("catalog changed" if changed else "catalog already up to date")
    return 0


if __name__ == "__main__":
    sys.exit(main())
