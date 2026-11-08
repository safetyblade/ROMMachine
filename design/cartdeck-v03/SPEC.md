# CARTDECK v0.3 Native Design Acceptance

The appearance assets here must be integrated only after independently verified P0 GB/GBC/GBA APK + save persistence gate.

- **Permanent identity:** C-slot path, uppercase CARTDECK name, chamfered shell, side rail, bezel, cartridge insertion, directional precision and tactile buttons; signature boot beats Wake (0-120ms) → Slot (120-280ms) → Mark (280-550ms) → Game when ready. These are maximum visual target windows, never artificial gameplay waits.
- **Changing edition:** edit only colours and material/cosmetic decoration inside `editions/<name>.json`; never copy the logo/controls into alternate brands. Green is a demonstration and not approved for shipment.
- **Physical feeling:** D-pad beveled cross with directional inlay; each arrow one faceted source path rotated 0/90/180/270, tintable; A/B are a face + separate glyph; Start/Select are machined pills. Implement press-depth and focus states with native rendering, never debounce/delay game input.
- **Gameplay:** Console Shell is default for phone/tablet; Fullscreen renders clean game to available bounds; Floating overlays adjustable alpha, scale, handedness and location with recoverable control positions. The choice is saved as a user preference per system/orientation, not embedded in game or cartridge data. TV prefers controller-first fullscreen. At least 48dp touch targets, support diagonal and simultaneous inputs, preserve video aspect (GB/GBC 160x144; GBA 240x160), orientation safe insets and unsuppressed pause.
- **Architecture:** Layer order Identity → System family → Cosmetic edition → User preferences. Native touch/gamepad hit regions are independent of scalable SVG graphics. Shell image is not a screenshot: render geometry as Compose Canvas/Path and/or Android vector layers with safe-screen constraints. Theme invalid? Restore Original cosmetics without interrupting active game or save files.
- **Compliance:** No bundled protected game artwork, manufacturer trade dress, ROMs, BIOS, sound or font binaries. Wordmark strokes are original candidate, final optical/trademark clearance pending.

**Review gates:** Cassia journey/layout review; Jonathan production asset signoff; Daniel theme implementation contract; verified P0; Elliot P1; Marcus independent regression.
