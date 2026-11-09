# DC-02 — developer-local Flycast core-version rollback

This is debug-only executable code stacked on [PR #95](https://github.com/safetyblade/ROMMachine/pull/95), not public core provisioning.

- `DebugCoreVersionStore` verifies SHA-256 against manifest for every selection; previous version remains physically stored and is selected automatically if active checksum fails.
- A versioned layout in `files/dreamcast-lab/cores/flycast/versions/<version>/core.so` is staged by `scripts/ci/dc01_device_probe.sh`, with `active.version`, `previous.version`, and a version-local `sha256.txt`. Use `DC01_CORE_VERSION=flycast-pinned-1b7c7d63` to label private builds. No core is committed or delivered.
- Three local tests check version selection/corruption, valid rollback without deleting user progress, invalid paths/checksums. Device script now captures `core-selection.txt` evidence. The previous core and all VMU/save directories are retained; no existing Lemuroid core is touched.
- The default single-file debug probe path remains a fallback for the previous PR #91 workflow while the versioned selector is introduced.

**Remaining gates:** Validate Gradle/JUnit CI, real physical device staging and rollback, VMU/NVRAM persistence after force stop, 16 KiB phone, Daniel's exact GPL and asset/source correspondence, Marcus's lawful gameplay and save roundtrip, Cassia's product integration. A SHA-256 match is only file integrity, not source authentication or licensing approval.
