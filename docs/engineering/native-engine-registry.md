# CARTDECK native engine registry

The application registry describes only implemented engines. It is generated from the established console/core configurations, with additional packaged PS1/PSP candidates and the separate hakuX SDL3/Vulkan host. It does not manufacture a Libretro identity for Xbox or advertise an unbuilt ARMSX2 Android engine.

Each descriptor includes the console, formats, host, core/configuration identity, ABI/API/graphics prerequisites, firmware/support assets, controller profile, state namespace, save compatibility, CPU/audio requirements, performance limitations, provenance manifest and rollback policy. Versioned manifests and the active version pointer identify native engine revisions. Native provisioning independently verifies binary hashes, dependencies and rollback eligibility; the registry does not bypass those checks. Legacy cores retain their existing provisioning path.

## Selection and storage

`GameLauncher` resolves cartridge override → console preference → established successful automatic selection → existing core default. Existing opt-in PS1/PSP settings are honoured until the player chooses a new registry preference. The old DS migration/default and controller configurations are retained. Automatic selection has no unverified performance ranking and does not replace working defaults simply because a candidate is newer.

Console settings and cartridge details expose Automatic/reset, eligible engine choice, console inheritance and the previous successful engine. Existing native previous/current *version* controls remain available. An unavailable engine is not silently replaced. The controlled reason is shown to the player. Preferred choices use the existing application's private preferences and survive install-over. Cartridge keys use a private hash of the source URI plus console, so a refreshed database row ID does not discard the choice or alter game identity.

Runtime outcomes are separate, version/host-scoped records under `files/native-engine-outcomes`. Reads use the committed file under an interprocess lock; they do not call `AtomicFile.openRead`, which could remove another native process's in-flight file. Writes are atomic. No ROM path or title is stored in those records.

Only an observed guest graphical frame may call `recordSuccess`. Activity creation, GL context creation, a frontend draw and time elapsed do not qualify. The Libretro host must provide a real video-refresh signal; the previous unconditional frontend `FrameRendered` event is insufficient. A successful Xbox presentation must come from the guest Vulkan-present hook, with matching provenance. These source hooks do not claim phone gameplay acceptance.

## Failure and save safety

Failure records identify a symbolic failing stage and the actual selected engine/version/host. Every failed engine has its own entry, so a second failure cannot erase the first and cause cycling. The default policy asks the player to choose/retry in cartridge settings. A user may select no automatic switching, or choose a proven alternative on the next deliberate Open. The latter selects only a different, previously successful, still-eligible engine. It never recursively launches or blindly probes a list. Explicit cartridge selection/reset authorizes retry and clears the corresponding failure guard.

Changing engine identity disables automatic state resume for that launch. Each core's existing state namespace and native revision-scoped states remain isolated. No conversion or sharing of incompatible state formats is attempted. Existing battery saves, memory cards, Wii NAND, Dreamcast VMUs and Xbox HDD paths are not moved or erased by registry choices. Compatibility of a particular memory-card format remains engine-specific; the descriptor does not promise universal portability.

## Validation boundary and remaining implementation

Tests are organised by system/engine/execution path: registration coverage, Xbox native host, PS2 GLES/API/ABI rejection, Wii/GameCube configuration/state separation, PS1/PSP legacy/candidate identities, persisted cartridge/console precedence, stable automatic selection, explicit recovery, bounded manual fallback and prevention of cycling.

No new phone gameplay result is inferred from these tests. Wii and Dreamcast CDI owner-confirmed gameplay remain independent regression anchors. Device performance, audio, controller, save/reopen and install-over acceptance must be recorded per path. Additional engines require an actual Android-compatible host/binary, exact provenance, assets and licence review, hardware rules, isolated state/save handling and path tests before registration. ARMSX2's Android application and Linux ARM64 Libretro package are not interchangeable; it is not selectable until a legitimate integrated Android implementation exists.
