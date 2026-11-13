# GripArc V2.2 implementation and acceptance handoff

Rollback baseline: master `c00dd29a19c168bb4e376f89930f47d3e9797dbf`, complete
[APK workflow 43333794250](https://github.com/safetyblade/ROMMachine/actions/runs/43333794250).
Keep that working APK/install as the rollback reference. No package, signing,
engine, ROM, save, viewport or standalone layout configuration is changed here.

## Compact material

Runtime now loads `griparc-v22-manifest.json`, schema `2.2.0`: a purpose-designed
160×160dp L with a 60dp vertical arm, 40dp bottom arm, 38×26dp visual capsules,
chamfered walls, mounting details, controlled reflections and italic **GripArc**.
This replaces the 272×308 source scaled down to 72–84%; the new assembly runs at
its designed size and never scales its touch targets below 48dp.

Right-hand slot centres are G1=(124,28), G2=(124,80), G3=(124,132), G4=(60,132).
Left mirrors x around the 160dp artboard. All four 48dp targets and the complete L
are reserved at every active count. Only the active buttons are drawn; no ghost
seats. Slot IDs, G mappings, chord ownership and cartridge preference schema stay
unchanged; revised centres are permanent within this hardware version.

## Native controller fidelity

The selected `TouchControllerID.Config` publishes `NativeControlInventory`.
Standard combined mode no longer guesses a button list from `SystemID` or drops
analogs through a separate count override. Metadata preserves Sony symbols,
GameCube A/B/X/Y + L/R/Z and two stick roles, Dreamcast ABXY + native L2/R2 trigger
codes and one stick, GB/GBA/SNES inventories, PSP and digital/DualShock variants.
Unmapped profiles keep an explicit Standard fallback rather than a made-up deck.
Wii auxiliary variants and Xbox SDL GripArc integration are not advertised as new
working combined profiles.

Native production controls publish a read-only input signature alongside their
existing bounds observer. Before fitting, observed native inputs must exactly
match the selected configuration and every required seat must be measured. Extra
or missing input signatures fail closed. After fitting, the complete key set,
visual set and native hardware seats must still match; the renderer uses required
bindings rather than silently skipping unknown keys.

Placement starts with those measured native positions. Each hand keeps its
constellation, face compass and primary/secondary stick/D-pad relationships.
Visuals compact by 10% where practical; targets stay at least 48dp. Dock-side
horizontal travel is bounded to 10–24dp, with only a bounded whole-hand safe-edge
correction. Upward candidates are tried from smallest first. All actual targets
and native paired/quad seat artwork must clear viewport, gestures and dock.
Hardware seats use their original Standard material path, not a PivotArc plate.

PivotArc combined mode reuses the smaller dock, its existing constellation and
notched island. Interior bevel/reflection and the native PivotArc wordmark retain
machined identity. Combined-only Hold A/B legends are explicit and illuminated;
solo PivotArc remains unchanged. Native GameCube half-trigger/stick codes already
present in the programming catalog now participate in chord ownership instead of
being filtered out.

## Validation and physical gates

Fast checks use production geometry/input code, selected-profile inventory,
representative native-bound fixtures, both sides, fixed G slots, proper
screen-derived viewports/insets, and negative incomplete/overflow cases. These are
not owner handset measurements. The existing integrated ARM64 pipeline remains
required for the signed install-over APK and all engines; no checks are removed.

Use [the owner/Marcus acceptance matrix](griparc-device-acceptance.md) for both
combined modes, counts 1–4, left/right, console inventories, symbols/labels,
pressed states, simultaneous holds/chords including GameCube half triggers,
cartridge reopen/reload, themes and install-over/save continuity. Record actual
handset dp dimensions/insets. **V2.2 owner fit/gameplay and install-over: NOT
TESTED in this coding pass.** The prior reported PivotArc failure remains a
physical acceptance gate, regardless of desktop geometry results. Keep #135 open.
