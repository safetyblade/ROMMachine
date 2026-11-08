# CARTDECK — visible brand migration (P1-BR1)

**Status:** Feature branch only; Android compile, install, launch and save persistence not yet verified for this change.
**Source branch:** `feature/cartdeck-visible-rebrand`.
**Owner:** Cassia Rowan (experience); Elliot Shaw (engineering); Daniel Mercer (migration review); Sienna Vale (art); Marcus Reid (QA).

## Approved identity

- Public brand: **CARTDECK by prestwich.dev**.
- Approved source: `design/cartdeck-v03/brand/cslot-master.svg`, `brand/logo-lockup-master.svg` and `editions/original.json`.
- Original edition app background `#303B49`, foreground `#EDF6F2`, accent `#4ED8C4`. Pink edition remains a future theme option and has **not** been applied globally to the launcher.
- The launcher icon vectors reuse the **exact C-slot master path geometry**. The 320:180 Android TV banner translates the approved vector logo lockup paths. Both are editable XML, not baked-in bitmap replacements.

## Completed in this branch

- Main manifest launcher `@mipmap/cartdeck_launcher`, round icon, and TV banner `@drawable/cartdeck_tv_banner`.
- Adaptive icon foreground and monochrome for modern Android; fallback vector resources for older Android.
- Visible application label from Gradle build type: `CARTDECK` or `CARTDECK Debug`. Legacy `lemuroid_name` resource key remains as an alias with CARTDECK value in case code still references it.
- Main default English UI strings: reset, help, notifications, ROM import/loading and recovery wording.
- Root README and draft Fastlane English title and description are now CARTDECK-first.
- Upstream credits retained in `THIRD_PARTY_NOTICES.md` and existing licence text.
- All original emulation, touch-control hit zones, saves, core bindings and ROM scanner code unchanged.

## Deliberately NOT renamed

- `com.swordfish.lemuroid` (Android application ID / namespace), `.debug` test variant, inherited internal classes, `lemuroid_*` resource identifiers and modules. **Changing these during the existing v1015→v1016 phone-test chain could orphan data, URI grants, ROM library records, saves and install/upgrade continuity.**
- Existing deep-link scheme `lemuroid:` (part of the compatibility contract). Existing shortcuts must be inventoried and migrated before changing it.
- Binary native core assets, source packages, upstream copyright notices, `COPYING` and historical commits/PRs. Upstream attribution cannot be scrubbed for visual consistency.
- Existing unverified third-party screenshots, Fastlane image resources and other locales. These require a dedicated inventory and replacement using approved original visual material before any store publication.
- Virtual D-pad/buttons and in-game shell artwork. Sienna's v0.3.1 corrected D-pad source remains in `design/cartdeck-v03/`; **do not** replace native input code or touch hit zones merely to rebrand.

## Acceptance checklist

1. `python3 scripts/audit_cartdeck_brand.py` passes in CI; review any reported source/visible-text leftovers.
2. Android builds both freeBundleDebug and the P0 phone ARM64 variant and APK resources show CARTDECK logo/label/banner.
3. Install branded APK **over** a prior P0 debug build with the same package and signing certificate, higher versionCode; never uninstall. Confirm saved games, manual states, games list and persisted ROM permissions survive.
4. Verify Android 8–latest launcher adaptive/fallback appearance, Android TV banner resolution and overscan, light/dark/system monochrome icon support.
5. Check user-visible help, settings, crash messages, notifications, startup/home, installed-app label, shortcuts and in-game pause/title surfaces. Check non-English locales and dynamic overlays.
6. Verify C-slot matches approved source and Pink is not accidentally treated as approved global default. Confirm legal attribution remains accessible through distribution/source notices.
7. Inspect actual APK using `aapt2 dump resources` or Android inspection, screenshot both phone and TV. No experience sign-off from source review alone.

## P1-BR2 — separate identity migration

Only after Marcus's upgrade/save QA, make a separate PR to adopt `dev.prestwich.cartdeck`, new signing identity, namespace/package refactors (if necessary), notification and provider authority migrations, deep-link/shortcut continuity and explicit save/import/export migration. A package name/signing change is **not** an Android in-place upgrade. Do not create a new production key in source control.

## Current blockers

- No live compile or APK inspection performed in this tool environment.
- Independent device/game/save verification remains a release gate.
- Full repository-wide branding and third-party image inventory required for public shipping.
- Per-core licence compliance and app store metadata approval remain outstanding.
