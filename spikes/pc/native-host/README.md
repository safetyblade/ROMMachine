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
