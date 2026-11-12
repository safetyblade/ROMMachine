# hakuX native Android backend

`scripts/ci/build_hakux_native.sh OUTPUT` builds the real upstream hakuX
SDL3/Vulkan backend, independently of LibretroDroid. Set `ANDROID_NDK_HOME`
to NDK `29.0.14206865`, with CMake 3.31.6, Ninja and Python 3.12 on `PATH`.
The ARM64 target is Android API 28, generic ARMv8 with CRC, Release/ThinLTO,
static libc++, shared SDL3, and 16 KB shared-library linker alignment.
`NATIVE_BUILD_JOBS` defaults to two. Use a fresh `HAKUX_BUILD_ROOT` when
rebuilding: the recipe refuses to overwrite modified sources, including a
previous build's applied compatibility patch.

The exact source is hakuX `a7cdb5a975e31730d488c47f6f10440b16c1e6f4`;
FEX is its pinned submodule `52018bf46a8279b22bce738548ab3f40db467064`.
All other revisions are explicit in `build_hakux_native.py` and emitted in
`source-manifest.json`. Two documented upstream short revisions could not be
resolved in the published repositories:

* nv2a_vsh_cpu: selected published
  `1115255708c10c4841b65dcd2223262e7a316598` instead of `561fe80`.
* libslirp: selected the official GitHub mirror's 4.7.0 release
  `3ad1710a96678fe79066b1469cead4058713a1d9` instead of `26be815b`.

The small retained `hakux-libslirp-4.7-ssize.patch` uses the release's
`ssize_t` callback type; upstream hakuX uses the later spelling
`slirp_ssize_t`. The adrenotools KGSL priority patch is applied exactly from
the selected hakuX source. Neither patch changes emulator behaviour.

The flat runtime package includes `libhakuxcore.so`, `libSDL3.so`, five
adrenotools hook libraries, `hakux.properties`, per-component notices,
`native-verification.json` and SHA-256 checksums. The verifier checks actual
ELF64/AArch64 shared-library headers, all load segment alignments, DT_NEEDED
closure, hakuX JNI exports and SDL's JNI entry point. No Libretro ABI is
invented for this backend.

Publish `source-compliance/hakux-corresponding-source.tar.xz` alongside the
engineering APK. It contains the actual patched sources, all dependency
snapshots including FEX's used submodules, the build recipe, verification
script, retained patch and revision manifest. Its checksum is separate from
the APK runtime list, so source snapshots are not duplicated into the APK.
Notices retain the upstream GPL/LGPL/BSD/MIT/Apache terms. The upstream
`native/dev/apu/vp/svf.h` GPL version ambiguity remains a per-file legal
review item; public distribution requires a separate owner decision.

Compilation and ELF checks do not prove physical Android boot, Vulkan/JIT
operation, 16 KB runtime compatibility, audio, controls or authentic HDD-save
persistence. Record those separately against a device and lawful game fixture.
