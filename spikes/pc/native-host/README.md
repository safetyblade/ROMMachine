# Isolated PC native host experiment

The standard CARTDECK APK reuses the verified LibretroDroid console host recipe from
5d661b1535bde0825a3ddd4ed9f6498a1e507c35. Its Play JavaVM bridge, rendering fixes,
core frame signal and console input contract remain intact. PC import, guest disks,
Windows setup, Wine groundwork and the Kotlin PC input implementation remain in the app.

The three files here retain the PC keyboard/mouse native source experiment exactly as
merged before isolation. The combined patch failed against pinned LibretroDroid
8835c3098514390a271e36983957f7bb5f40abf1 in input.cpp and environment.h. It is
not a verified build or an active console-host patch.

Use the dedicated PC native host laboratory workflow to validate and repair this
experiment. It applies the experimental overlay only to a disposable runner checkout,
builds only the host, and publishes only a distinct PC laboratory artifact. No release
APK or console native receipt is published and no production native artifact is replaced.

The stable app deliberately does not send PC-only native input opcodes to the verified
console host. Do not enable NATIVE_PROTOCOL_AVAILABLE until the isolated runtime
has its own checked integration and packaging contract. This is not a completed PC runtime.
PC media, workspaces, save data and installations must remain intact.

Future PC execution should own a runtime capability boundary (DOSBox/Wine/translation)
with independently versioned native outputs and a separate laboratory install ID.
Reject unknown PC runtime protocols; do not change existing console inputs or host
receipts in order to implement PC-specific features.

## Controlling roadmap

Read [PC-00 #299](https://github.com/safetyblade/ROMMachine/issues/299) and the [PC-00 charter](../../../docs/research/pc/PC00_LANE_CHARTER_AND_FEASIBILITY_GATES.md). PC-R01 is the next increment; the current lab is not yet an independently integrated PC runtime. Use separate PC artifact identity, protocol capability, install ID and storage, without modifying or replacing the production console host or blocking standard APK assembly. PC-R02–R07 govern later guest execution, product integration, Wine and promotion.

## New independent laboratory recipe

Use build_pc_lab_host.sh, apply_pc_input.py and the frozen base-patches directory; the preserved older scripts and combined patch are historical evidence. No copy-over of the production script or Maven coordinate. Workflow cartdeck-pc-native-lab.yml builds the host and pinned core independently, then a synthetic lab APK with package dev.cartdeck.pc.lab. Normal CARTDECK does not consume its outputs. Only synthetic DOS COM input is provided; Windows support is not established. The old shared-host recovery workflow is retired. Remove the lab app independently to roll back; do not uninstall CARTDECK.
