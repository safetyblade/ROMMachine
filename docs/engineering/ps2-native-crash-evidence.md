# PS2 HOST_INIT native crash: exact trace and symbols

Refs #84, #143. PARTIAL: executable evidence capture, not PS2 gameplay recovery.

The owner report establishes SM-S721B/API 37, cartdeck3, Play revision
1f27232772e484bfb79364b751b84f41f4c87481, HOST_INIT, no GL strings and
Android native-crash reason 5/status 11. It supplies no faulting PC, module or
function. Therefore no EGL option, retro_init implementation, driver or engine
replacement can yet be selected as the demonstrated fix.

Copy report in the existing recovery activity now retrieves the matching own
:game process exit (PID and launch timestamp), on an IO dispatcher. On Android
12+ native exits it consumes at most 1 MiB of ApplicationExitInfo's protobuf
trace, decodes the AOSP debuggerd faulting thread and exports at most 24 frames.
Only module basenames, relative PCs, sanitized symbols, signal and ELF build IDs
are exported. Raw tombstones, memory maps, registers, memory dumps, command lines,
thread names and game paths are not saved. Malformed, absent or oversized traces
produce an explicit ADB fallback. A sanitized report remains available in Setup.

The Play and LibretroDroid builders retain unstripped DWARF separately from the
runtime package and compare build IDs against the exact stripped ELF/AAR. Release
optimization remains unchanged; -g preserves symbol evidence. CI keeps separate
ps2-native-symbols and libretrodroid-native-symbols artifacts for 14 days. The
symbol tool refuses mismatched builds, including a different binary from the same
upstream revision. Version-store copies named core.so match by exact ELF build ID.
No old stripped build is claimed symbolicated by a newly rebuilt engine.

## Required phone evidence and next correction

1. Install the matching development APK over the existing installation. Open a
   legal PS2 fixture once, choose Copy report after failure, and retain that text.
2. If Android omits the native trace, collect the native crash buffer over ADB
   while reproducing once: `adb logcat -b crash -v brief DEBUG:F '*:S'`.
   Keep raw output private; share only the faulting backtrace/module build IDs,
   signal and CARTDECK's sanitized launch report. Do not clear player data.
3. Download the symbol artifacts from that exact APK run. Combine their `.debug`
   files in a local directory; symbolicate only matching IDs:

   ```sh
   python3 scripts/ci/native_symbols.py --readelf /path/to/llvm-readelf symbolize \
     --report ps2-report.txt --symbols /path/to/exact-symbols \
     --symbolizer /path/to/llvm-symbolizer
   ```

4. Use the first relevant native frame and callers to isolate JNI/retro_init,
   EGL/GLES, hardware callbacks or content load. Patch that demonstrated function
   in a separate recovery PR. Alternatives still require a maintained compatible
   ARM64 host and licence review; no unavailable engine is added to the registry.
5. Jonathan/Marcus must then verify visible frames, audio, input, memory-card save,
   clean exit/reopen, rollback and install-over. Containment is not a gameplay pass.

No handset/ADB transport or native tombstone is available in this execution
workspace. The precise external blocker is the faulting-thread stack with module
build IDs from the owner device; source compilation cannot produce it. Work on
independent Xbox/Dreamcast packages continues while that evidence is pending.
