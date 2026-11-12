# PivotArc + GripArc joint deck — issue #135

This combined-only implementation follows the V2 coordinated-deck contract in
`design/cartdeck-p2/griparc-v02`. Standard + GripArc and both standalone controller
solvers remain unchanged. Initial native support is GB, GBC, GBA and SNES, exactly
as declared by `PivotArcLayoutProfile.activeFor`; no other console is advertised.

`CompositeControlDeckSolver` reserves the permanent L arms and all four fixed
GripArc slot targets before placing the PivotArc constellation, Hold A/B, the
utility bank and future X/Y envelope. It returns a typed fit/failure result, the
unchanged viewport, measured safe deck, input rectangles, dock footprint and a
notched island contour. All touch targets remain at least 48dp. Future X/Y are
reserved but are absent from rendered/input controls when inactive.

The compact island uses measured baseline target sizes and shared constellation
translation/scaling. The selected dock side mirrors the joint deck without
swapping key or slot identities; the standalone PivotArc hand preference remains
saved and unchanged. Insets, viewport, orientation, profiles and offsets are read
on every layout pass, so no stale geometry cache crosses mode or device changes.
Saved-side offset attempts precede the default position on that same side.
Trying the opposite side requires an explicit session choice.
Failed attempts never update cartridge or global settings.

The joint renderer consumes these exact contours and targets. It suppresses the
solo PivotArc plate and the accessory's separate housing pass, paints the notched
island and complete L once, and retains the existing individual input controls,
Hold indicators, theme tokens and shared hold/chord ownership gate. The artwork
has no pointer handlers. A joint failure exposes neither partial module, disables
both virtual hold/chord paths, and offers an effective Standard override for the session with settings
preserved. Physical-controller-only GripArc behavior remains intact.

## Fast validation

Controller-path tests cover GB/GBC/GBA/SNES at 393/360/320dp fixture widths on both
sides, all reserved GripArc slots, future X/Y, shell/target/viewport exclusions,
minimum target sizes, identity-preserving mirroring, gesture insets, offset reset,
and atomic rejection. Standard combined and solo PivotArc/GripArc regression
suites run alongside the new tests. Input ownership tests exercise simultaneous
touch, physical, hold and GripArc owners, all four slot releases and fallback
cleanup, without requiring a commercial title.

Geometry and input tests run directly on production Kotlin sources in an isolated
JUnit harness (Android asset-loader/side-enum scaffolding and Android SDK key
constants). Targeted KtLint checks cover changed Kotlin files. A touch-input module
compilation checks the joint solver, housing and existing renderers with temporary
profile/palette type scaffolding; this is not a full app build or device result.

CI is handed off through the existing PR workflow. The acceptance checklist is
`docs/engineering/griparc-device-acceptance.md`. APK completion, install-over,
Marcus device QA and owner acceptance are a separate session. Shorter decks or
larger system insets can legitimately require the Standard session fallback;
fixture fit is not a claim that every 320dp handset fits. Issue #135 stays open.
