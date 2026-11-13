# INPUT-02 — Marcus independent QA matrix v0.1
**Status of all cases: NOT TESTED** · Tracking: [#181](https://github.com/safetyblade/ROMMachine/issues/181)

## Core semantic-mapping acceptance
| ID | Platform / game | Mode | Original game input | Player action on modern physical controller | Expected core output / result | Gate |
| --- | --- | --- | --- | --- | --- | --- |
| M01 | SNES — Super Mario World | Modern Function (verified) | Original B = Jump | SOUTH | Original SNES B pressed/released; controlled jump; run remains available | Device |
| M02 | SNES — Super Mario Kart | Modern Function (verified) | Original B = Accelerate | RT | Original SNES B held on squeeze, released on lift; NOT global B→RT in M01 | Device |
| M03 | SNES — Super Mario Kart | Modern Function (verified) | Original Y = Brake | LT | Original SNES Y held/released; hop/drift preserved | Device |
| M04 | GBA — Mario Kart Super Circuit | Modern Function (verified) | Original A = Accelerate | RT | Actual GBA A core key and hold; A not generally remapped for non-racing GBA | Device |
| M05 | GBA — Mario Kart Super Circuit | Modern Function (verified) | Original B = Brake | LT | Actual GBA B core key; no phantom A/B or Select | Device |
| M06 | N64 — Mario Kart 64 | Modern Function (verified) | A/B accel/brake, left stick steer | RT/LT/stick | Core receives original A/B; original C/Z/R remain usable | Device |
| M07 | GC — Mario Kart Double Dash | Modern Function (verified) | A/B accel/brake | RT/LT | Exact native A/B; L/R drift, Z and C-stick preserved | Device |
| M08 | PS2 — Gran Turismo 3 | Modern Function (verified) | Cross/Square accel/brake | RT/LT | Actual native Cross/Square; pressure indicated DIGITAL unless proven | Device |
| M09 | Dreamcast — Crazy Taxi | Modern Function (verified) | Native analog R/L triggers | RT/LT | Genuine 0..1 analog pair; no doubling digital keys | Device |
| M10 | Original Xbox — Forza Motorsport | Modern Function (verified) | Native R/L triggers | RT/LT | Correct native trigger axes when Xbox engine actually playable | Device |
| M11 | PS2 — FIFA 2005 | Modern Function (verified) | Cross short pass, Circle shoot | SOUTH/EAST | Cross/Circle correct in attacking play; defense/menu contextual reuse disclosed | Device |
| M12 | Unknown title/genre | Modern Function chosen | Unknown | Any | Console Authentic fallback; no auto profiles applied | Unit+device |
| M13 | Confirmed genre, unreviewed title | Modern Function chosen | Suggested only | User explicitly applies | Clearly shows suggestion and Undo; no unasked silent remap | UI |
| M14 | Any supported title | Console Authentic | Existing input map | Same gamepad inputs | Previous gameplay controls unchanged | Device |
| M15 | Custom per-cartridge | Custom | User assigned | Chosen physical input | Custom overrides published profile; saves/reopens | Device |

**Profile evidence checkpoint:** the research matrix/manual citation only makes a row a *candidate*; do not move its QA state to PASS without owner/Marcus gameplay evidence.

## Device and system tests
| ID | What | Required evidence |
| --- | --- | --- |
| D01 | Xbox 360-style USB pad | FACE SOUTH/EAST/WEST/NORTH, DPAD, both sticks, L3/R3, bumpers/triggers, Menu/pause |
| D02 | Switch Pro Bluetooth or USB | Nintendo labels correct while physical South maps consistently; real pairing support recorded |
| D03 | PS3/DualShock 3-style | Connect only if recognised by Android; record unsupported USB/Bluetooth separately from mapping QA |
| D04 | Generic Android gamepad | Axis/right-stick variants and fallback mapping; no ghost inputs |
| D05 | Two identical controllers | Physical device identity distinct, P1/P2 ports stable, no cross-player input |
| D06 | Trigger digital fallback | Threshold, hysteresis, continuous hold/release; no chatter or dropped release |
| D07 | Analog accelerator | 0..1 proportional value only where core supports; no duplicate digital event |
| D08 | Shared original key owners | Press physical original + RT mapped to same native key; release one leaves other pressed until both release |
| D09 | Gameplay menu | Menu shortcut never accidentally injected as game action; no trapped Back/Exit |
| D10 | Hotplug/focus | Disconnect/reconnect, pause/resume, profile swap, quit and reset release held input |
| D11 | Handheld portrait | Console shell/GripArc unchanged when using touch; controller mapping retains existing settings |
| D12 | Tablet/phone landscape | Physical pad hides on-screen buttons and shell; touch restored when last pad disconnects |
| D13 | Android TV | No virtual game controls even without pad; controller/remote menu accessible, Fill/Native independent |
| D14 | DS portrait, Wii variants | Stylus/dual screen, Wii remote pointer modes preserved and clearly unsupported mapping disabled |
| D15 | Install-over and saves | Prior cartridge identity, ROM library, saved states, GripArc/custom/controller settings and game progress persist |

## Test report template
- Case ID:
- Controller physical name, hardware ID/descriptor, transport:
- Android device, OS, screen/orientation:
- Game + region/edition + ROM identity/serial/hash (where feasible):
- System/core/controller variant and RetroPad/native output:
- Profile source, confidence, settings mode:
- APK version, commit and install-over origin:
- Expected native input and observed gameplay action:
- Result: **PASS / FAIL / NOT TESTED**
- Owner evidence/QA reviewer, screenshot/video/log:
- Related defect/issues:

**Close #181 only after integrated build and real-device evidence, not from static fixtures.**
