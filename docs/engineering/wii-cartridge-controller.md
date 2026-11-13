# Wii controller choice per cartridge

Refs #85, #143. Reuses ControllerConfigsManager and the existing live input flow.

Previously the Wii choice was written to a system/core/port key, so choosing
GameCube for one cartridge changed every Wii cartridge. Wii reads now try the
cartridge-specific key first, then a valid existing console key, then the first
registered profile (Sideways). Other platforms retain their original keys.

The new key extends the established controller preference namespace with a
SHA-256 of system and exact cartridge URI. It survives display-name/record-ID
changes on refresh and does not expose game paths. No preference migration,
player data deletion or console-default rewrite occurs. Unknown profile names
fall through to a compatible existing choice; they are not erased during rollback.

Runtime initialisation, first-open choice (through its current controller state),
in-game switching and Cartridge Setup Controls all pass the same active game to
the same manager. Explicit choices write only that cartridge's port. The existing
observable state still drives LibretroDroid setControllerType and the native touch
layout together. Dolphin is not restarted and Wii NAND/save files are untouched.
The first-open prompt now correctly describes persistence and retains the current
selection until the player explicitly changes it. GameCube remains labelled as
supported Wii games only.

## Validation

Synthetic preference tests check two independent Wii cartridges, fresh-manager
reopen, all four real device IDs (513 Sideways, 1025 Classic, 769 Nunchuk, 1
GameCube), default choice, legacy console fallback without writes, invalid-choice
rejection and stable opaque keys after rename/reindex. Existing standalone
GameCube profile/device mapping tests remain intact. Android UI execution is
blocked locally by the retained workspace Robolectric-download restriction; no
Android interface or phone test is claimed from Kotlin compilation alone.

## Phone acceptance (Jonathan/Marcus)

- With no prior cartridge/console choice, open a synthetic/legal Wii cartridge:
  first-open and touch layout should select Sideways.
- Choose GameCube for cartridge A (using a compatible Wii fixture), Nunchuk for B.
  Close and reopen both. Verify Setup Controls, first-open selection when shown,
  and the in-game menu agree with each cartridge's stored selection.
- Switch among all four profiles from the in-game menu. Confirm native device
  input and touch art update together, no emulator restart, and saved progress
  survives. Verify held inputs release through the established input path.
- Install over the prior APK and check a legacy console preference still serves
  cartridges without an override, while a new override affects only its owner.
- Confirm standalone GameCube, GripArc/PivotArc and other consoles remain intact.

Source completion does not establish Wii GameCube-profile compatibility for every
game or final physical gameplay acceptance.
