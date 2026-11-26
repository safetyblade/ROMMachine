# SMS-GA-01P/L — Sega Digital 2 Power User (review candidate)
Inherit approved GB/GBC/NES GripARC hardware and styling; no fake shoulder or Select control. Source SMS.kt at reference head: Android BUTTON_B = guest 1, BUTTON_A = guest 2, D-pad, Android START = guest Pause (as specified by approved SMS PivotARC packet; verify actual selected core semantics), independent CARTDECK Menu. Keep native keys and existing saved bindings. SMS button 1 may perform start in individual games; do not create separate invented Start input. 

## Portrait (360×380 illustrative canvas)
D-pad (86,155), native 2 (278,129), native 1 (210,185), Hold 1 (210,60), Turbo 2 (318,195), Turbo 1 (236,252), 1+2 (147,260). Menu (115,330), Pause (245,330), no Select. Maintain full-body manufactured shell, engraved upper-left detail and accent/trim. Native 1 lower-left, 2 upper-right.

## Landscape (730×390 illustrative canvas)
Existing CARTDECK chassis: D-pad (82,159), 1+2 (114,253) left; Hold 1 (650,65), 2 (681,143), 1 (612,202), Turbo 2 (684,220), Turbo 1 (613,278), Pause (674,322) right; Menu (365,365). Unused left utility area gets noninteractive chassis detailing, no extra native key. Preserve actual core-reported aspect (normally 4:3 presentation), never stretch/crop. CARTDECK main wordmark centre, smaller GripArc lower right. Same assignments both orientations.

## Factory preset (proposed)
1 Hold 1 — tap to latch native BUTTON_B down until deselected, visible depressed state; release safely on lifecycle changes.
2 Turbo 2 — repeat native BUTTON_A while pressed.
3 Turbo 1 — repeat native BUTTON_B while pressed.
4 1+2 — simultaneous momentary native BUTTON_B and BUTTON_A.
All programmable; function labels reflect actual mapping. Universal Hold contract in README/GA-D00 applies; Turbo and Hold implementation pending; chord dispatch verify. Retain user mapping overrides, no active latch persistence. Independent 48dp targets, actual viewport and two-hand reach, orientation neutralization, safe release. Candidate gameplay Sonic the Hedgehog, Alex Kidd in Miracle World, Wonder Boy III: The Dragon's Trap; verify actual controls rather than assume title-specific semantics. No runtime changes or physical acceptance; owner review pending.
