# P1-EMU-X360 packaging continuation

Baseline: a3407cee11fbb8e78002200103f1b08aa2a4663d.
Status: source/native/APK gates passed at 7843208f; Samsung gameplay unverified.

Recovered/retained #219 and its native-engine, cartridge-runtime and verified-APK
branches. The baseline already contains the isolated Activity, native adapter,
24-entry controller ABI, persistent virtual storage, diagnostics, verified
versioned engine store and rollback. No old UI/database tree was merged.

The new lane now selects the existing canonical opt-in combined ARM64 APK
job. That proven pipeline includes the five existing engines, Wii, original
Xbox and the real source-built Xenia package, preserving current gaming paths.
It runs full app lint/JVM checks and verifies package identity, signing,
version, engine bytes and dependency closure. The independent native workflow
retains engine evidence only. Standard distribution remains gated; no X360-only
APK is installed over the player's working Wii/Xbox application.
The stager now permits a genuine X360-only package instead of requiring an
unrelated legacy engine argument. No native binary is substituted.

JNI verification now requires an actual defined, exported function; an undefined
JNI_OnLoad import or a similarly named symbol cannot satisfy packaging. Tests
exercise undefined/local/hidden/data symbols. The existing native source still
forces the stock Vulkan loader; no Adreno driver is selected for Xclipse.

Local targeted Xbox 360 JVM contracts and native diagnostic tests passed.
The complete native and APK gates passed at source
`7843208f2afa9fa71a2b22b71efa1a09fe0d5001`, run
https://github.com/safetyblade/ROMMachine/actions/runs/43598603237 .
The opt-in five-engine + Wii + original Xbox + Xbox 360 APK is artifact
https://github.com/safetyblade/ROMMachine/actions/runs/43598603237/artifacts/14115819982 .
APK SHA256 `c45caa14bd5a9a182a81d554a37091d0cb4ff1fd470b4a35cb2da274cdb613fc`,
141,673,927 bytes, versionCode 90606412, package `dev.prestwich.cartdeck.debug`,
existing debug signing identity. All 36 packaged ARM64 libraries passed 16 KB
ELF checks; source manifests, native bytes, five preserved engines and the
patched GLES host were verified. Independent native run43598603132 also passed.
The native artifact includes its own exact engine hashes, BuildID, complete
source archive and licence inventory. The follow-up receipt prints those hashes
in logs and binds the source archive and recipe to provenance; its new head must
pass the same gates before merge. No guest image, audible output or Samsung
performance is inferred from these build checks.

Physical #248 must eventually verify SM-S721B stock-driver feature checks,
independentBlend failures, JIT versus guest swaps versus visible images, audio,
all controls, persistent in-game save/reopen, menu return and rollback. Save
State/Load State remain unavailable. Original Xbox hakuX data is unchanged.


## Bulk physical QA procedure

Retain the baseline APK and back up CARTDECK app data before upgrading: this
opt-in APK shares the existing development package/key. Use verified lawful
Xbox 360 ISO/XEX content; an XEX import must include its companion directory.
Open from the shelf. Confirm a valid changing guest image (not just a JIT count,
load screen or white presentation), audible stereo, both sticks/clicks, D-pad,
ABXY, LB/RB, analogue triggers and Start/Back. Guide opens the CARTDECK menu.
Test physical disconnect/reconnect, Standard controls and GripArc; undefined
PivotArc geometry must preserve preferences and expose the existing session
fallback rather than omit required inputs. Check stock Xclipse device/driver
and independentBlend diagnostics. Save in-game, return to Shelf, reopen and
confirm persistence. Repeat after an engine update and explicit rollback.
Unsupported Save State/Load State must remain absent. Check original Xbox,
Wii and the five earlier engines using their existing data. A driver/JIT failure
must return safely to Shelf with the local diagnostic report retained.

Engine rollback selects the previous immutable, hash-verified native package
without moving virtual storage. An APK rollback to the retained stable build
may require Android's deliberate downgrade procedure; preserve app data and
never uninstall/clear data as a rollback instruction. Handset installation,
GPU feature availability, real frames/audio, save durability under interruption
and performance remain #248 acceptance, not a CI success claim.
