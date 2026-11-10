# CARTDECK PivotArc — v0.5 Interactive Control Research Prototype

**Status: design prototype; not an emulator feature or gameplay-verified ergonomics.**

**Approved direction:** provisional name **PivotArc**; simple **Adjust Position** moves the full cluster. Individual button editing is a different future advanced setting. A GLOBAL chosen layout applies to every game, with required buttons changing by system. Future long-press cartridge overrides, Racing/Fighting/Sports presets and opt-in genre rules remain out of scope.

## Faithful second-sketch topology
Right-hand source diagram:
- One central/right-side D-pad.
- Action positions **P1 (top-left), P2 (left-upper), P3 (left-lower), P4 (bottom-left)** surrounding D-pad asymmetrically. Do not turn into an ABXY diamond.
- Horizontal L above, vertical R upper-right.
- Left-hand view mirrors positions but **never the game/video or labels**.
- Actual system input ID mapping and primary A/B pair remain **an experiment**, not assumed.

## First geometry feasibility (NOT a phone ergonomics result)
Prototype hit targets: D-pad 96×96, P1–P4 48×48, L 66×48, R 48×66 in dp-proxy coordinates. Bounding rectangle **285×300**; minimum active target gap is **8** in the schematic. A 320dp-wide phone with 12dp left/right safe margins has only **11dp horizontal slack**; therefore a free-form shift across the whole display is not physically possible with the entire four-action cluster. The adjustment must be **clamped** to the valid range, with constrained/unsupported combinations explained. Larger screens offer more freedom.

The root must satisfy the measured game viewport exclusion and gesture/navigation insets before activating a placement. **DS dual-screen and large analog modules may not have adequate lower display space**; do not crop video, shrink below minimum targets, or silently abandon the global selected mode.

## Action contact question
Two mapping candidates are intentionally available:
- **A · Lower arc:** B=P3 and A=P4; P1/P2 optional other actions.
- **B · Middle arc:** B=P2 and A=P3; P1/P4 optional actions.

The HTML schematic can visualise touch-down ownership: pointer-down activates one enabled target, moving past another action does **not** activate that neighbour; on release pointer is cleared. This is a *candidate*, not a change to the existing PadKit gesture logic.

## Interactive design study
Open `prototype.html` in a modern browser (no build tooling or network required). Choose handset width/height, Right/Left, mapping candidate, system profile and whole-cluster position. Observe constraints and pressed inputs. Record real-device results in `study-template.csv`, rather than inferring comfort from browser diagrams.

## Implementation gate
Sienna/Cassia approve mapping and profile, Daniel validates actual measured input semantics, and only then Elliot implements a **separate opt-in native development PR**. Existing Standard-mode Start/Select positions and hit zones are protected. Pause/Start/Select remain in a separate utility zone, not in the frequent-action arc. No ROM/emulator/save/system migration in this pack.


## v0.6 PivotArc Button Lab — individual drag experiment (10 Nov 2026)

Owner requested editable button placement **before** locking shoulder ergonomics. The `prototype.html` has been extended so each D-pad, action position and L/R shoulder control can be grabbed and moved individually. The shoulder controls now have **two recallable comparison presets**: (1) original sketch with horizontal L above and vertical R upper-right, and (2) near A/B at the lower action arc. Both remain hypotheses, not a locked redesign.

Controls include hand and system input family, alternative A/B mappings, whole-group moves, target-clearance warnings and an exportable JSON record of relative positions. The entire HTML is independent of Kotlin and PadKit. Importing JSON, detailed phone-size visualization and additional editing affordances are also supplied in the standalone **PivotArc Button Lab v0.6 HTML** in the current owner design review conversation; GitHub `prototype.html` retains the simpler tool with direct drag and JSON export.

The first geometry comparison (393×852 simulated surface, lower A/B assignment) changes the mean Euclidean distance across the four A/B↔L/R centre pairs from approximately **271 dp** (original shoulder positions) to **107 dp** (near-action position). This is a modeled geometric result only. It does NOT prove physical thumb reach, game performance, ability to press simultaneously, or accidental-contact rates. At compact width, the close-shoulder preset can exceed the available safe area; the browser flags that condition rather than reducing target size.

**User experiment:** try both presets on a phone, drag L/R until every important control can be reached without changing grip, optionally activate all four action circles and extra-console controls, then **Export JSON**. Treat the exported JSON as a design handoff, not a configuration to import into CARTDECK Android. Standard-mode Start/Select remain locked and the release APK is unchanged by this task.
