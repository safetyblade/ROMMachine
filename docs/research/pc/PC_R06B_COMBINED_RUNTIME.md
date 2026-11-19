# PC-R06B: no-Windows execution checkpoints

R06A Box64 Android AArch64 PIE / 16 KiB ELF passed on 44454773356. The DOSBox integrated APK and normal regression at 48a604a6 passed. Linux Wine control still running at this checkpoint.

Two complementary gates:
1. A source-pinned Wine 10.0 (b073859675060c9211fcbccfd90e4e87520dc2c2, LGPL-2.1-or-later) new WoW64 build on x86_64 Linux. Actual 32-/64-bit fixtures then run under the same pinned Box64 release on an ARM64 Linux runner with independent prefixes, GDI/registry/local progress checks twice. This deliberately uses a glibc Linux control, not an Android compatibility assertion. Runner availability and actual execution are not assumed.
2. Separate Android APK dev.cartdeck.pc.winelab bundles the already-built Bionic Box64 executable and original static x86_64 guest. It launches the packaged runner with a private working directory, requires exact stdout/exit code and two guest-written file records, and reports binary digest. No Windows media, no Wine package, no arbitrary imported executable, no production saves. Device testing still needed. The candidate is not the final package or proof of Windows support.

A simple Bionic executable and Linux WoW64 archive alone are insufficient to integrate: need a coherent Android-compatible user-space (Bionic-native Hangover/WoW64 or verified glibc + Box64 runner), packaged source/receipt dependencies, guest executable mapping under Android target SDK/W^X policy, X11/Wayland and audio, supervised process tree and private prefix. Do not treat Linux success as Android success; no unreviewed Winlator binaries. No weakening shared host, signing, storage or production recipes. Preserve DOSBox as fallback. 
