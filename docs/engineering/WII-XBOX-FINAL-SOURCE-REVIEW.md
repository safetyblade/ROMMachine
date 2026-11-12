# Independent native integration review

This review distinguishes implementation evidence from gameplay acceptance. It supplements `WII-XBOX-INDEPENDENT-QA.md` and issue #120. The owner reports **PS2 fails to launch; GameCube reaches a black screen**. Neither symptom is declared resolved by compilation.

## Independently exercised checks

On the retained managed Linux executor:

- 27 native/APK verification regressions passed, including rejection of incomplete engines, missing original fallbacks, wrong host bytes, disabled registry, missing Xbox host classes/activity registration, modified native bytes, wrong architecture and unaligned ELF files.
- Six staging regressions passed, including missing native dependencies, missing host ABI, changed SDL bytes, unsafe manifest paths and Wii without its matching Dolphin package.
- The actual seven-library hakuX package passed ARM64 shared-ELF, 16 KB load alignment, dynamic dependency closure and required JNI/SDL export verification. SDL default-version exports such as `JNI_OnLoad@@SDL3_0.0.0` are correctly treated as exported ABI names, not missing symbols.
- `libhakuxcore.so`: 6,853,256 bytes, SHA-256 `82ce7eadf32917c915d6ec7542ccad0a6e52c016a6feed34876e7e9583b42012`.
- `libSDL3.so`: 2,513,640 bytes, SHA-256 `40832efc3cb9f0cdcd0a1f5b7c575bc249ba8cf7ffbf925cfeeea38fe6bbfe87`.

These local binary hashes identify reviewed inputs. Hosted rebuilds may produce different hashes; their manifests and APK evidence remain authoritative for each artifact.

Fresh device inspection returned an empty `adb devices -l` list. No `/dev/kvm` or Android emulator executable was available. Real frames/audio, touch or gamepad response, NAND/HDD in-game save round trips, runtime 16 KB compatibility, thermals and upgrade preservation are **NOT TESTED**.

## Source findings and closures

The Xbox adapter is an internal activity under the CARTDECK package and a separate `:xbox` process. It uses the actual pinned JNI namespace; it does not pretend to implement Libretro.

| Finding | Source review outcome |
| --- | --- |
| Empty Libretro core list reached core selection/download or falsely appeared unsupported | Native backend branches bypass Libretro selection/update; supported-games detection checks hakuX and SDL separately. |
| JNI called after SDL library loading failed | `mBrokenLibraries` returns before pad/pause/poll JNI calls. |
| Failed SDL initialization reported successful play | Found during this review; adapter now records a load failure and reports unsolicited `finish()` as an error. Successful explicit return sets the normal result contract first. |
| Continue metadata and cancelled background work were not restored | Activity uses `REQUEST_PLAY_GAME`, returns game/session metadata, forwards the result through XBE relaunch, and reaches the existing `handleGameFinish` path. That path reschedules background work even for cancelled/error results. |
| Native host could continue without owning the game lock | Host now requires `acquireExclusive` success before opening console data. Preflight/upgrade backup also takes an exclusive file lock. |
| Repeated native XBE relaunch could loop | Activity limits relaunches to five within 30 seconds and offers return to shelf. |
| Engine rollback could mix incompatible native dependencies | Immutable versions hash all required libraries; selected SDL/core are loaded from one verified package. Rollback restarts the native process rather than loading a second FEX runtime into the current process. |
| Engine replacement could reset console data | HDD/EEPROM/UDATA/TDATA are outside engine versions. A version change backs up the complete HDD before use and fails closed if protection cannot complete. This is file protection, not proof of authentic game-save persistence. |

Wii source review confirms Dolphin's actual Classic/sideways/Nunchuk IDs `1025/513/769`, matching button labels and separate persistent `saves/native/wii/User` storage. GameCube remains under `saves/native/gc/User`. The engine uses `GET_SAVE_DIRECTORY/User`, so the separation reaches Dolphin's NAND/user configuration path.

The pointer fix maps coordinates through the actual rotated quad and clears input on touch cancellation. Its regression evidence is recorded by the host test scripts. The graphics-state fix isolates frontend VAO/VBO and presentation state from hardware cores; it addresses a real rendering-boundary defect but is not physical confirmation of the owner's GameCube black screen.

PS2 recovery source review confirms that runtime failure markers are scoped to the graphics host identity, while integrity failures remain separately blocked. Legacy conflated failure history is archived once before a verified retry. Explicit current/previous-engine actions remain available. Play's actual GLES 3.2 requirement is surfaced before launch on a device advertising an older version. The native current-context check is still authoritative.

## Remaining acceptance work

Full combined Android build/test results and hosted artifact hashes must be attached after integration. Independently review the final committed tree, including any changes made after this snapshot.

Upstream hakuX currently ends its dedicated process after SDL quit because its core thread has no stop API. The adapter contains the process exit, but this must not be described as graceful core-thread shutdown or a proven HDD flush. Authentic in-game save, close/reopen and in-place APK upgrade tests remain required on physical ARM64 Android hardware.

Public distribution is not cleared by this review. Preserve the complete corresponding source archive, exact dependency revisions, build scripts, patches and notices; the per-file licence/version ambiguity recorded in `wii-xbox-source-obligations.md` remains an owner review item. No proprietary firmware or game content is part of this evidence.

**Independent Marcus QA status: source/package checks passed as recorded; final integrated artifact review and physical gameplay acceptance pending.**
