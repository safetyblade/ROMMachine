# Cartridge front and Setup recovery

Related: #143, #155. UI package C; no native engine recovery claim.

The detail view previously placed engine and version controls before Open. The
front now keeps the existing animated cartridge, title, system and ARC badge,
with Open and Setup outside the cover scroller. Setup progressively opens
Emulator, Controls, Cartridge artwork or Advanced / Troubleshooting.

The existing preference, cover, controller and version stores remain authoritative.
PS1/PSP bundled support-asset preparation runs before launch too: launching must
not depend on visiting the optional emulator UI. Wii controller choices and
pointer guidance remain in Controls. Engine state files and player saves are not
moved or converted.

Only eligible installed choices are selectable. A single eligible engine is a
status, not a fabricated alternatives list. Retained previous versions remain
separate from other engines; rollback is shown only for a distinct verified
previous installation. A failed engine can be explicitly retried.

## Automated acceptance

`CartridgeExperienceTest` exercises the front/Setup boundary, direct Open,
artwork picker/cancellation/reset, narrow landscape, enlarged text, navigation
and shelf restoration. The synthetic Xbox fixture checks that unavailable
alternatives and previous versions are not advertised. Its native-graphics
capture test writes `build/reports/cartridge-setup/{front,setup}.png`; the existing
APK workflow collects those images alongside test results.

`DebugCoreVersionStoreTest` checks real previous-version availability, corruption
fallback and save-independent version-store operations. Source preflight remains
the existing #150 gate; no test or native build gate is removed.

## Local verification limitation

Production and test sources can compile with the completed cartdeck3 host AAR.
Compose/Robolectric test execution requires the official
`org.robolectric:nativeruntime-dist-compat:1.0.16` artifact. Its Maven download
redirects to `gcp.mirrors.maven.org`, which this workspace denies with CONNECT 403.
No runtime test or screenshot is claimed until CI runs those tests. The pure
version-store JUnit tests run independently without Android/Robolectric.

## Phone acceptance (Jonathan and Marcus)

- Install over the existing application. Confirm shelf positions, covers, saves,
  console preferences and per-cartridge engine choices remain intact.
- Open cartridges using each appearance and enlarged text, portrait and landscape.
  Confirm the animated cover and Open are obvious; no technical controls appear
  until Setup. Dismiss Setup without changes and confirm the same cartridge.
- In Setup, change/reset artwork; check persistence after reopening. Select
  Standard, GripArc and PivotArc and verify existing mappings are retained.
- Check Wii sideways, Classic and Nunchuk selection through Controls. Verify the
  established Wii launch path and return to shelf.
- Check a single-engine console shows status and only real retained-version
  rollback. Check PS1/PSP alternatives, console inheritance and cartridge
  overrides when both actual implementations are available.
- Open without first visiting Setup; verify existing working engine paths and
  save/reopen. PS2 and Xbox still require their separate native recovery work.

Issue #143 stays open. This change does not establish PS2 gameplay, Xbox visible
guest rendering, or complete Dreamcast alternate-format discovery and staging.
