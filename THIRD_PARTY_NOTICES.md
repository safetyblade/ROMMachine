# CARTDECK — Third-party source and copyright notices

This repository is a modification of existing open-source emulator software. The user-facing CARTDECK name, C-slot icon and original device design do not remove the copyrights or licences of upstream contributors.

## Upstream applications and frameworks

- [Lemuroid](https://github.com/Swordfish90/Lemuroid) — original Android application foundation by Swordfish90 and contributors. The upstream project is licensed under GNU GPL version 3; the full GPL text is preserved in the root [COPYING](COPYING) file and upstream source notices remain applicable.
- [LibretroDroid](https://github.com/Swordfish90/LibretroDroid) — Android emulator frontend technology, subject to its own upstream GPL terms.
- [Libretro](https://github.com/libretro) — ecosystem of independently maintained emulation cores. Each distributed core must be assessed independently: its binary licence, upstream revision, source availability and redistribution terms may differ from the Android app.
- [LemuroidCores](https://github.com/Swordfish90/LemuroidCores) — existing native-core submodule used in this source tree. Binary and source provenance require verification at the precise commit included in each build.

## Important distribution gates

1. Preserve all upstream and dependency copyright notices and applicable source-availability obligations in any redistributed binary/source release.
2. Audit licences of every included native core and other third-party dependency. Some core variants have non-commercial restrictions that require explicit clearance or exclusion.
3. Do not include unauthorised commercial game ROMs, BIOS files, logos, screenshots or trade dress from console manufacturers.
4. Never publish with inherited release signing credentials. Separate the P0 test APK identity from prestwich.dev production signing and plan save export/import.
5. Public APK and store listing remain on hold until a complete licence matrix, device compatibility and independent QA sign-off exist.

The controlled research and licence register is:
https://docs.google.com/document/d/1GVPPHmMar0iSsPlDNYT1f9_lVWSKDqx4dLf4LorJWk0/edit

CARTDECK original visual artwork source:
`design/cartdeck-v03/`

This file is source attribution, not a legal opinion or an assertion that the planned release is compliant.
