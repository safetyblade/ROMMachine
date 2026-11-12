# PS2 launch failures and native-engine recovery

The pinned Play! core requests **OpenGL ES 3.2** and uses its matching shaders. It is not a BIOS-based engine. An ARM64 device alone does not establish graphics compatibility. The loader now checks the advertised GLES version before starting Play!, retains the native current-context validation, and reports the specific PS2 requirement rather than a generic failed game.

The previous host treated a graphics-context mismatch as a failed engine binary. That disabled the same engine revision on subsequent Open attempts, including after a host-only update. Recovery now separates verified-install failures from runtime failures:

- Integrity/install failures remain disabled across host updates.
- Runtime library failures are scoped to the host version. Repeating Open with the same failed host/engine does not start a crash loop; a corrected host gets one attempt.
- Earlier conflated failure markers are archived once under `native-engine-versions/<core>/legacy-failure-history`, preserving the history while allowing a verified engine to recover under the new policy.
- The cartridge's existing **Use current engine** button explicitly clears a failed attempt and retries the installed engine. **Use previous engine** pins a retained, verified previous binary when one exists.
- Graphics incompatibility no longer blacklists the engine. It remains a visible graphics error on subsequent attempts.

These operations do not delete or replace saves, game images, NAND, memory cards or custom artwork. The host version is passed through the shared build configuration so a graphics-host fix can recover an unchanged native core. Loader exceptions retain their underlying causes in diagnostic logs.

Regression coverage exercises same-host blocking, recovery after a host change, legacy-marker migration, persistent integrity-failure blocking, explicit retry and the Play! GLES requirement. The owner identifies SM-S721B and two distinct prepare/return versus Android-crash outcomes; a native trace and exact failing graphics stage remain unavailable. Actual Android Play! boot, video/audio, control input and authentic memory-card save round trips remain **NOT TESTED** in this environment. This recovery fix does not claim universal Play! compatibility or establish the owner's complete failure cause.

## Issue #143: contained PS2 launch supervision

The existing game activity and service already run in CARTDECK's private `:game`
process. Therefore the owner's report of an app crash is not evidence that the
shelf's process itself received a native signal. Neither issue #143 nor its
comments contains a PS2 logcat or tombstone. The SM-S721B device is identified,
but the failing native instruction and graphics stage remain unknown.

A PS2-only recovery activity now stays in the main process while the unchanged
Play! game activity runs in `:game`. Canceled/crashed activity results return to
a recoverable screen with a copyable diagnostic. A foreground-only startup watchdog bounds
initial staging at ten minutes per frontend stage and host initialization at
three minutes. It terminates only the journaled PID when Android confirms that
PID is this UID's exact `:game` process. It never relaunches the engine, replaces
an engine or deletes a save. Large disc staging and native startup have separate
budgets. The existing `FrameRendered` event remains only a frontend draw; it never
marks an engine successful. Older hosts without core-frame reporting stop startup
supervision at that frontend draw to preserve existing gameplay. The additive patched-host3
`CoreFrameRendered` event requires a nonnull, positive-sized core video-refresh
callback completed during the host step. That event ends startup supervision and
records engine selection success. It establishes core frame submission, **not
correct gameplay, audible sound, mapped input or save persistence**.

The private atomic journal records only the last verified frontend stage:
REQUESTED, ACTIVITY_STARTED, CORE_PROVISION, ROM_STAGE, HOST_INIT, FRONTEND_FRAME, CORE_FRAME,
CLEAN_EXIT. HOST_INIT includes native dlopen, retro_init, context negotiation and
content initialization; these are **not** falsely represented as individually
observed stages. A fatal native signal cannot be caught by Kotlin. Android 11+
process-exit records provide the native/Java crash reason and exit status after
such a failure. Older Android versions explicitly report that evidence as
unavailable. The report also includes app/core/host revision, core checksum,
model, ABI, Android version and advertised GLES version. It excludes game titles,
content URIs, paths and exception messages. Native tombstone/logcat collection
from the affected phone remains necessary to identify the Play! engine fault.

The Java uncaught-error finish is dispatched onto the UI thread for Play! only.
Wii, GameCube, Dreamcast CDI and all other hosts retain their existing routing,
rendering, controllers and storage. The supervisor forwards successful game
results so the existing shelf/Continue accounting still runs.

### Alternative-engine boundary

ARMSX2 is actively maintained (release 2.9.3 reviewed during this recovery), with
Android APK variants for different ARM/API requirements and Exynos/Xclipse driver
work. Its Android APK is not an Android libretro core; its Linux artifacts cannot
be dlopened into CARTDECK. A lawful integrated candidate requires a source-built
Android JNI host, renderer/input/storage adapters, BIOS/resource provisioning,
complete corresponding-source/licence review and isolated saves. No external APK
launch, repackaged branding or unverified Linux binary is introduced here.
Play! remains packaged and reversible. This change contains failures and gathers
evidence; PS2 gameplay, sound, controls and memory-card persistence on the owner's
phone are still pending, and the underlying native crash is not claimed fixed.

Latest owner observations distinguish a prepare/return-to-shelf path and a
prepare/Android crash-notification path. They are separate failure results to
record; neither establishes a native cause or justifies clearing player cache.
