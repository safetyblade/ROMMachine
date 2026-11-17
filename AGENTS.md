# CARTDECK engineering instructions

## Kotlin and APK readiness (all lanes)

- Use the pinned Gradle wrapper and ktlint configuration. Do not hand-format code as a substitute for the actual formatter.
- Before opening or updating any Kotlin PR, run `./gradlew ktlintFormat --continue`, review the diff, then run `./gradlew ktlintCheck --continue`. Repeat after resolving conflicts or updating from master. Commit only intended changes.
- Fix non-auto-correctable violations and rerun until whole-repository lint passes. Never suppress rules, skip lint or mark a failure passing to obtain an APK.
- Run relevant unit tests. Keep saves, profiles, native receipts and unrelated features intact.
- Require the **Kotlin formatting (whole repository)** check on the current proposed merge revision before merging. The dedicated workflow produces a formatter patch on failure; review and apply it, then rerun checks.
- If local Gradle is unavailable, disclose the blocker and use the PR check plus formatter artifact. Do not claim local validation or merge a failing revision.
