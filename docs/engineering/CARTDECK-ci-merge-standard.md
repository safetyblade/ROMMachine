# CARTDECK — CI, native build and merge standard

**Scope:** `safetyblade/ROMMachine`, all CARTDECK engineering PRs.
**Owner:** Engineering (Elliot); architecture review (Daniel); independent verification (Marcus).
**Purpose:** Fail invalid application changes before expensive native builds, prevent stale-branch regressions, and keep installed-user continuity.

## Non-negotiable flow

1. **Latest-master guard:** A PR head must contain the latest base branch commit. Source preflight checks ancestry; rebase/merge `master` into an out-of-date PR before native jobs.
2. **Source preflight:** Python/contract tests, Android workflow dependency contract tests and Kotlin lint.
3. **Patched Android host:** Build the pinned host AAR once and make it available as a checked workflow artifact; host construction precedes app compilation.
4. **Mandatory Android gate:** With the pinned patched host and `FreeBundleDebug` app configuration, compile production Kotlin, compile JVM test Kotlin, then run JVM unit tests. **All emulation-engine builds depend on success.**
5. **Native engines:** Build pinned PS1, PSP, Dreamcast, GameCube, PS2 and separately gated Xbox/Xbox 360 backends as specified in each workflow. A native engine build alone is not an APK pass.
6. **Final variants:** Build Wii, Wii+Xbox and opt-in Wii+Xbox 360 when configured. Retain independent native bundle checks, package/signing, upgrade identity, runtime and artifact evidence, plus variant-sensitive tests. Do not replace these gates with the early test.
7. **Independent QA:** Marcus verifies real install-over, saved progress, shelf retention, controller behavior and actual engine paths. CI cannot claim hardware playback by itself.

A failure must display the first actionable compiler/test diagnostics with the failing job and commit SHA. Do not rerun an unchanged failing commit. No release or unverified QA snapshot may be described as a passed build.

## Enforcing merges in GitHub

The workflow gates prevent expensive native jobs from starting after failed Kotlin compilation. **Required status checks and branch update requirements are separately enforced by GitHub Rulesets or Branch Protection, not by workflow YAML.** A repository administrator must enable these controls for `master`:

- Settings → Rules → Rulesets → New branch ruleset; target `master`, activate it, and require pull requests for changes.
- Require the **CARTDECK APK** complete engineering workflow to succeed at the current proposed merge commit, including the full APK artifact/identity verification jobs. Require the **Android compilation and JVM tests — mandatory pre-native gate** check as an explicitly named gate.
- Enable **Require branches to be up to date before merging** (or the equivalent merge-queue requirement). Reject stale-head checks.
- Do not bypass failed checks or permit force-push/direct pushes to `master`. Any exceptional owner-approved emergency process must have written risk, rollback and follow-up QA evidence.
- Keep the `arch-lib-01-room-schema.yml` schema/migration gate required for migration-bearing PRs; it does not replace Android compilation.

**Status check name for the reusable workflow:** `Complete CARTDECK ARM64 APK / Android compilation and JVM tests — mandatory pre-native gate`. Confirm the precise displayed context in the first PR checks list before saving repository rules, as GitHub displays reusable workflow job paths.

## Rollback and maintenance

- Workflow rollback is a dedicated revert PR with a passing baseline gate and owner review; do not silently remove required checks.
- `scripts/ci/test_ci_kotlin_gate.py` asserts the dependency graph. Any new `native-*` engine or `apk-*` variant automatically inherits the gate requirement in this contract test.
- The app package ID, debug signature, monotonically increasing version code, database, saves, existing engine fallback and installed assets must not change as part of CI-only fixes.
- The first pass keeps existing final-APK Kotlin/test verification in place. Consolidating duplicate *variant-sensitive* tests requires a separate audited evidence/contract change; this CI correction prioritises reliability over faster green builds.
