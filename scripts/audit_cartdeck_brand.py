#!/usr/bin/env python3
"""Validate the CARTDECK Android-visible brand contract without altering runtime code.

This static audit is NOT an APK/device, licence or save-migration acceptance test.
"""
from __future__ import annotations

import re
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "lemuroid-app/src/main/res"
A = "{http://schemas.android.com/apk/res/android}"


def content(rel: str) -> str:
    return (ROOT / rel).read_text(encoding="utf-8")


def icon_resource(path: Path, required_tag: str) -> ET.Element:
    tree = ET.parse(path).getroot()
    assert tree.tag == required_tag, f"{path} has unexpected drawable type {tree.tag}"
    return tree


def check() -> None:
    manifest = ET.fromstring(content("lemuroid-app/src/main/AndroidManifest.xml"))
    app = manifest.find("application")
    assert app is not None
    expectations = {
        "label": "@string/cartdeck_name",
        "icon": "@mipmap/cartdeck_launcher",
        "roundIcon": "@mipmap/cartdeck_launcher_round",
        "banner": "@drawable/cartdeck_tv_banner",
    }
    for key, expected in expectations.items():
        assert app.get(A + key) == expected, f"Manifest {key} must be {expected}"

    gradle = content("lemuroid-app/build.gradle.kts")
    assert 'applicationId = "com.swordfish.lemuroid"' in gradle, "P0 test update ID changed"
    assert 'namespace = "com.swordfish.lemuroid"' in gradle
    assert 'resValue("string", "cartdeck_name", "CARTDECK")' in gradle
    assert 'resValue("string", "cartdeck_name", "CARTDECK Debug")' in gradle
    assert 'resValue("string", "lemuroid_name", "CARTDECK")' in gradle

    strings = content("lemuroid-app/src/main/res/values/strings.xml")
    assert '<string name="cartdeck_launch_title">CARTDECK</string>' in strings
    assert '<string name="cartdeck_launch_byline">by prestwich.dev</string>' in strings
    intro = content(
        "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/main/CartdeckLaunchSignature.kt"
    )
    assert "R.string.cartdeck_launch_title" in intro
    assert "R.string.cartdeck_launch_byline" in intro
    main_activity = content(
        "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/feature/main/MainActivity.kt"
    )
    assert "CartdeckLaunchSignature()" in main_activity
    assert "delay(650)" in main_activity
    assert not re.search(r"\bLemuroid\b", strings), "Main English UI still displays Lemuroid"
    for translated in RES.glob("values*/strings.xml"):
        local_text = translated.read_text(encoding="utf-8")
        assert not re.search(r"\bLemuroid\b", local_text), (
            f"Old public product name remains in translated UI: {translated}"
        )

    base_icon = icon_resource(RES / "mipmap-anydpi/cartdeck_launcher.xml", "vector")
    round_icon = icon_resource(RES / "mipmap-anydpi/cartdeck_launcher_round.xml", "vector")
    adaptive = icon_resource(RES / "mipmap-anydpi-v26/cartdeck_launcher.xml", "adaptive-icon")
    round_adaptive = icon_resource(RES / "mipmap-anydpi-v26/cartdeck_launcher_round.xml", "adaptive-icon")
    assert len(base_icon) >= 3 and len(round_icon) >= 3, "Legacy icon missing foreground"
    for node in (adaptive, round_adaptive):
        assert node.find("foreground").get(A + "drawable") == "@drawable/cartdeck_mark_foreground"
        assert node.find("monochrome").get(A + "drawable") == "@drawable/cartdeck_mark_monochrome"
        assert node.find("background").get(A + "drawable") == "@color/cartdeck_launcher_background"

    master = ET.fromstring(content("design/cartdeck-v03/brand/cslot-master.svg"))
    official_paths = [e.get("d") for e in master.iter() if e.tag.endswith("path")]
    assert len(official_paths) == 2
    def norm(s: str) -> str:
        return re.sub(r"[\s,]+", "", s or "")
    for path in (
        "drawable/cartdeck_mark_foreground.xml",
        "drawable/cartdeck_mark_monochrome.xml",
        "mipmap-anydpi/cartdeck_launcher.xml",
        "mipmap-anydpi/cartdeck_launcher_round.xml",
    ):
        node = ET.parse(RES / path).getroot()
        found = [e.get(A + "pathData") for e in node.iter() if e.tag == "path"]
        assert [norm(v) for v in found[-2:]] == [norm(v) for v in official_paths], (
            f"{path} differs from the approved C-slot mark geometry"
        )

    # Every foreground/scan/core/save notification must use a safe monochrome
    # status-bar icon. The legacy upstream tiny icon must never be referenced.
    notification_source = content(
        "lemuroid-app/src/main/java/com/swordfish/lemuroid/app/mobile/shared/NotificationsManager.kt"
    )
    assert notification_source.count(
        ".setSmallIcon(R.drawable.ic_cartdeck_notification)"
    ) == 4, "All four notification types must use the CARTDECK mark"
    assert "ic_lemuroid_tiny" not in notification_source
    for source in (ROOT / "lemuroid-app/src/main").rglob("*.kt"):
        assert "R.drawable.ic_lemuroid_tiny" not in source.read_text(encoding="utf-8"), (
            f"Upstream tiny notification icon still referenced by: {source}"
        )
    notification_icon = icon_resource(RES / "drawable/ic_cartdeck_notification.xml", "vector")
    assert notification_icon.get(A + "width") == "24dp"
    assert notification_icon.get(A + "height") == "24dp"
    small_paths = [el for el in notification_icon if el.tag == "path"]
    assert len(small_paths) == 2
    assert [norm(el.get(A + "pathData")) for el in small_paths] == [
        norm(path) for path in official_paths
    ], "Notification icon must use approved original C-slot path geometry"
    for el in small_paths:
        assert el.get(A + "fillColor") == "#00000000", "Small icon must be transparent"
        assert el.get(A + "strokeColor") == "#FFFFFFFF", "Small icon must be white for system tint"

    banner = icon_resource(RES / "drawable/cartdeck_tv_banner.xml", "vector")
    banner_paths = [e.get(A + "pathData") for e in banner.iter() if e.tag == "path"]
    lockup = ET.fromstring(content("design/cartdeck-v03/brand/logo-lockup-master.svg"))
    official_banner = [e.get("d") for e in lockup.iter() if e.tag.endswith("path")]
    assert len(official_banner) == 10
    assert [norm(v) for v in banner_paths[-10:]] == [norm(v) for v in official_banner], (
        "TV banner is not a vector translation of approved CARTDECK lockup"
    )

    brand = ET.parse(RES / "values/cartdeck_brand.xml").getroot()
    values = {v.get("name"): (v.text or "").strip() for v in brand}
    assert values["cartdeck_launcher_background"] == "#303B49"
    assert values["cartdeck_brand_accent"] == "#4ED8C4"

    assert content("README.md").startswith("# CARTDECK")
    assert "Lemuroid" in content("THIRD_PARTY_NOTICES.md"), "Upstream attribution lost"
    assert (ROOT / "COPYING").is_file(), "GPL text must not be removed"
    assert content("fastlane/metadata/android/en-US/title.txt").strip() == "CARTDECK"
    assert "CARTDECK" in content("fastlane/metadata/android/en-US/full_description.txt").splitlines()[0]

    print("PASS — app label, source geometry, adaptive icons, TV banner, English UI copy,")
    print("       metadata, P0 package continuity, original palette and source attribution.")
    print("WARNING — no APK compiled or inspected; no save upgrade or external-core licence test.")
    leftovers = []
    for folder in (ROOT / "lemuroid-app/src/main", ROOT / "fastlane/metadata"):
        for path in folder.rglob("*"):
            if not path.is_file() or path.suffix.lower() not in {".xml", ".kt", ".java", ".txt", ".md"}:
                continue
            try:
                value = path.read_text(encoding="utf-8")
            except UnicodeError:
                continue
            # A human review list, not an erasure mandate: many internal identifiers are deliberate.
            if re.search(r"\bLemuroid\b", value):
                leftovers.append(str(path.relative_to(ROOT)))
    print(f"Review candidate upstream visible-text files: {len(leftovers)}")
    for path in leftovers[:35]:
        print(f"  REVIEW: {path}")
    if len(leftovers) > 35:
        print(f"  ... {len(leftovers) - 35} more. Check source locally.")


if __name__ == "__main__":
    check()
