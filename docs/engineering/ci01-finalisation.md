# CI-01 opt-in acceptance paths

These paths do not promote selective reuse to the normal APK workflow. Acceptance
requires the complete verified native inventory and APK evidence, followed by
Marcus Reid's independent review and Jonathan's GC-01C physical testing.

| Mode | Workflow | Behavior |
| --- | --- | --- |
| `FAST_TEST_APK` | `cartdeck-ci01-apk.yml` | Resolve ten independent successful native builds, verify current pinned receipts, stage complete engines, run lint/unit tests, assemble and verify the ARM64 APK. Gradle caches remain enabled. Missing/expired/invalid engines stop the build with an individual-engine action; no emulator compilation fallback. |
| `NATIVE_ENGINE_BUILD` | `cartdeck-ci01-native-engine.yml`, `engine=<component>` | Compile one selected pinned component, verify its package contract, issue/recheck its receipt, and retain its independent artifact. ARMSX2 includes both page models; Cemu includes the Vulkan host. |
| `FULL_INTEGRATION_APK` | `cartdeck-ci01-apk.yml` | Apply the same ten-receipt, inventory, unit-test and APK gates. Clean project outputs, disable the Gradle build cache, measure first APK assembly and same-workspace repeat, and require identical APK SHA-256 with unchanged native inputs. |

The timing JSON describes the measured command, wall seconds and exit status.
Dependency and toolchain caches remain available in both measurements. These are
workspace-cold and workspace-warm APK builds, not fresh dependency-download or
native-producer benchmarks. Native receipts are verified before staging; the
timing wrapper additionally detects input changes during assembly. Failed builds
retain failure timing evidence and never qualify as successful APK benchmarks.

PS3 remains in its separately controlled workflow. These opt-in modes do not enable
PS3 packaging or alter `distribution_licensing_cleared`. Package/signature checks,
complete engine checks and the existing rollback policy remain required. Source
or unit-test passes alone are not physical, gameplay or install-over acceptance.
