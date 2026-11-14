# P1-EMU-PS2 recovery

Baseline: a3407cee11fbb8e78002200103f1b08aa2a4663d.
Status: INCOMPLETE ENGINEERING. Play! remains the default.

Recovered private-APK staging and verification from commit
4449cdf0a496ecef4ed35c640b5ca918bd1b6b1a, whose historical native build
succeeded in run 43572451960 but whose app packaging failed. The existing
merged JNI callbacks and per-cartridge storage were retained.

New implementation adds physical analog L2/R2 pressure and neutral release
through the pinned DualShock ABI, with focused JVM assertions. The Rust
librashader source receives an explicit build-ID and 16 KiB linker patch
whose before/after hashes are recorded. Missing build IDs remain failures;
no verifier exception is carried forward. Private APK CI builds both upstream
page models and packages each separately, under a distinct debug package ID.

Offline Python tests cover the linker patch, drift rejection, missing build
IDs and native/packaging contracts. Actual native build, complete dependency
closure, full JVM/Room checks and both signed APKs remain mandatory. A local
Java auto-provisioning 403 was worked around using checksum-verified Temurin
21 without changing upstream runtime code. No source-build or gameplay
success is inferred from that setup.

Normal cartridge engine selection, resource/BIOS onboarding, shared controls
and menu, lifecycle/crash recovery and save/reopen acceptance remain missing.
Samsung Xclipse performance and the reported SIGSEGV remain unverified under
#248. The stable APK and all Play! save paths are preserved.
