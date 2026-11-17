# CARTDECK engineering instructions

## Kotlin and APK readiness (all lanes)

- Use the pinned Gradle wrapper and ktlint configuration. Do not hand-format code as a substitute for the actual formatter.
- Before opening or updating any Kotlin PR, run `./gradlew ktlintFormat --continue`, review the diff, then run `./gradlew ktlintCheck --continue`. Repeat after resolving conflicts or updating from master. Commit only intended changes.
- Fix non-auto-correctable violations and rerun until whole-repository lint passes. Never suppress rules, skip lint or mark a failure passing to obtain an APK.
- Run relevant unit tests. Keep saves, profiles, native receipts and unrelated features intact.
- Require the **Kotlin formatting (whole repository)** check on the current proposed merge revision before merging. The dedicated workflow produces a formatter patch on failure; review and apply it, then rerun checks.
- If local Gradle is unavailable, disclose the blocker and use the PR check plus formatter artifact. Do not claim local validation or merge a failing revision.

## Mandatory UI change and merge safety

- Start each UI correction from the latest verified master. Keep changes narrowly scoped; do not touch native engines, saves, dependency versions, signing or CI packaging to fix presentation.
- Before editing, identify existing interaction contracts and regression tests. Preserve import, resume, back, focus, accessibility, save/profile identity and all previous accepted behaviours. Layout improvements must not hide working actions merely to reclaim space.
- Exercise portrait and compact landscape, multiple named sections, empty/populated/filtered shelf, touch and controller action reachability at the affected boundary. Add a regression test that would fail against the original defect; do not weaken or delete a pre-existing test to make a change pass.
- For Kotlin changes run the pinned formatter/check and affected tests first. For UI changes require the full connected APK workflow (including the mandatory unit suite) and **UI merge gate (verified APK)**, in addition to the Kotlin source gate. Success must be for the current prospective merge tree. A cancelled, queued, skipped, failing or stale UI check is NOT approval.
- Keep PRs draft until verified. Before merging, re-read PR head and current master SHA; if either changed since verification, update the branch and obtain fresh prospective merge-tree results. Merge one verified change at a time, then require the exact resulting master APK build and artifact to pass before merging another UI change.
- Do not call a merged source revision a working APK. Publish a link only after successful workflow conclusion and confirmation that the matching ARM64 APK artifact exists. Preserve the last successful master artifact and SHA for recovery. On broken master, stop other merges and prioritise minimal repair or approved revert.
- Never bypass branch protections or required checks, including when asked for quick delivery. If full verification is unavailable, report the precise blocker and leave the candidate unmerged.
