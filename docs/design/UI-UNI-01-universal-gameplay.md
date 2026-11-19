# UI-UNI-01 — Universal gameplay appearance

Owner accepted UNI-A on device. UNI-B consolidates appearance resolution without changing engine or input geometry. Retain the accepted A checkpoint at 7007b177b289b6b713fbe66313b4f532aa77fac6.

## UNI-C — design approved by owner, implementation candidate

- C01: replace separately drawn screen side rail assets with flush, recessed bezel accent channels.
- C02: align left and right upper chassis accents with the central cartridge indicator, using shared mirrored geometry.
- C03: connect the upper accents and side channels to the existing lighting settings (off, static, supported game feedback and controller activity). Off is recessed and unlit; high contrast disables decorative light. Native hosts without real feedback must not simulate it.
- C04: consistent bezel thickness and finish, with measured video exclusion and dual-screen protection.
- C05: integrated speaker and indicator detailing.
- C06: verify all editions and custom appearances; no per-engine artwork branches.

Do not change aperture placement or control geometry in C; adaptive layout is UNI-D. Keep maximised, TV and borderless modes game-first and respect the existing renderer lifecycle. No native binaries, save data, or storage migrations for this UI lane.

## Landscape acceptance (owner approved)

With surround enabled, use recessed channels and shared materials in portrait and landscape. Add control housings only in measured residual chassis with actual touch inputs; compact branding must not displace the aperture. Maximised mode remains minimal and borderless remains undecorated. Reuse side-rail zone for crown and vertical channels, screen zone for horizontal channels and control-well zone for indicators. Native hosts use baseline lighting until genuine feedback is available. UNI-D owns positioning changes.

## Owner acceptance revision

Standard portrait accepted. Landscape hardware, a selectable minimal surround, and GripArc alignment require correction. Approved: Hardware / Minimal / Off for each orientation, preserving saved STANDARD as Hardware, OFF as Off and inherited per-cartridge choices. Physical controllers retain game-first presentation. Standard landscape hardware reserves measured pad areas before fitting the viewport. Minimal deliberately keeps the existing overlay policy and paints only a measured thin surround. TV remains game-first.

GripArc final approved direction: close-fitting L around the selected native hand group, preserving the Standard constellation with modest proportional scaling and shifts. All four programmable buttons G1–G4 are independently available on every supported controller; mirror for left-hand mode. Do not scatter controls with dock-first free-space packing, remove original inputs, shrink touch targets below their safe minimum, or silently change saved settings. Resolve a shared deck, controls, dock and viewport together, including native hosts. This is a separate solver implementation and owner acceptance gate, not delivered by the display-menu and standard landscape increment. PivotArc/combined modes and all engines remain explicit validation requirements.
