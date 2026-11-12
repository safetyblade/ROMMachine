# PS2 launch failures and native-engine recovery

The pinned Play! core requests **OpenGL ES 3.2** and uses its matching shaders. It is not a BIOS-based engine. An ARM64 device alone does not establish graphics compatibility. The loader now checks the advertised GLES version before starting Play!, retains the native current-context validation, and reports the specific PS2 requirement rather than a generic failed game.

The previous host treated a graphics-context mismatch as a failed engine binary. That disabled the same engine revision on subsequent Open attempts, including after a host-only update. Recovery now separates verified-install failures from runtime failures:

- Integrity/install failures remain disabled across host updates.
- Runtime library failures are scoped to the host version. Repeating Open with the same failed host/engine does not start a crash loop; a corrected host gets one attempt.
- Earlier conflated failure markers are archived once under `native-engine-versions/<core>/legacy-failure-history`, preserving the history while allowing a verified engine to recover under the new policy.
- The cartridge's existing **Use current engine** button explicitly clears a failed attempt and retries the installed engine. **Use previous engine** pins a retained, verified previous binary when one exists.
- Graphics incompatibility no longer blacklists the engine. It remains a visible graphics error on subsequent attempts.

These operations do not delete or replace saves, game images, NAND, memory cards or custom artwork. The host version is passed through the shared build configuration so a graphics-host fix can recover an unchanged native core. Loader exceptions retain their underlying causes in diagnostic logs.

Regression coverage exercises same-host blocking, recovery after a host change, legacy-marker migration, persistent integrity-failure blocking, explicit retry and the Play! GLES requirement. The owner reports “PS2 fails”; the exact device, game and driver error have not been supplied. Actual Android Play! boot, video/audio, control input and authentic memory-card save round trips remain **NOT TESTED** in this environment. This recovery fix does not claim universal Play! compatibility or establish the owner's complete failure cause.
