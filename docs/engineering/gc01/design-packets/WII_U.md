# WII_U — controller design packet

**Batch:** E · **Hardware family:** Specialist · **Status: DESIGN PREPARATION ONLY**.

**PivotArc:** review_candidate; owner physical pending.  
**GripArc:** to_design; owner physical pending.  
**Joint:** DEFERRED until all solo controllers are designed, built and accepted.

**Console-specific input consideration:** Native host and GamePad control proof needed.

## WU-PA-01A/B — GamePad and Pro Controller review candidates (26 Nov 2026)

Inherit exact approved PSX-PA-03 448×400 reference chassis and side-mounted sticks, central D-pad, radial face seats Square=X, Triangle=A, Cross=B, Circle=Y (Nintendo compass rotated as approved SNES), fixed independent Holds X/A/B/Y, mid-arc 2×2 L/R upper and ZL/ZR lower, L/R stick-click seats only with verified guest dispatch. Rail Menu/Minus/Plus, native Home only if separately verified. A GamePad mode must protect independent GamePad interactive screen and TV viewport, support true guest touchscreen coordinate mapping and release, dual-screen switching/layout where available, and genuine gyro only via proven backend. Never treat TV touch as GamePad input without explicit mapped mode. B Pro Controller uses same geometry without GamePad touch or motion claims; actual guest device selection required. No fabricated Wii Remote support in Wii U mode. Engineering NativeGameSystems.kt registers Wii U as nativeBackend=cemu, hasTouchScreen=true and empty systemCoreConfigs; WiiuEngine.kt blob 98736e8db022f34d34471e83779fe41c4d0273f9 requires engineering build. These declarations do not prove working native input bridge, dual-display or gyro. Verify Cemu host actual capabilities before enabling each profile; absent support is explicit engineering gap, not a visual pass. Challenge games proposed Nintendo Land, Splatoon, ZombiU, Super Mario Maker, The Wind Waker HD; verify game-specific device requirements, screen dependencies, touch, gyro and simultaneous inputs. Require >=48dp independent discrete targets, full analogue travel, mirror, safe viewport, held ownership/release, native mode, and enhanced handset acceptance. Geometry inherited, not measured. Owner approval pending; no implementation/build. GripArc/Joint deferred.

## PivotArc — phase 2
- [ ] Inspect installed native/PadKit input inventory and document exact source SHA, input/axis identities and required button count
- [ ] Specify console-specific labels, paired face buttons, analogue/trigger geometry, menu/hold controls and physical artwork
- [ ] Match approved CARTDECK PivotArc material, shell joints, rim, inset and button collars; **do not change owner-approved GBA or PSP solo art**
- [ ] Specify every control's normalized position, at least 48dp touch target, compact grouping, game viewport/safe area, left/right and rotation
- [ ] Approve physical design independently from implementation; keep unmapped native host integrations explicitly unsupported

## GripArc — phase 3
- [ ] Use the approved full-size **GBA GripArc base chassis** as shared hardware foundation, not the detached L-dock
- [ ] Design this console's native button constellation inside the full panel; no reuse of scattered Standard positions
- [ ] Integrate G1–G4 as one deliberate 2×2 programmable bank; independent 48dp+ hitboxes, visual caps may be smaller
- [ ] Preserve mapping identity, held/simultaneous touch, safe display area, mirroring and actual native controller semantics
- [ ] Approve GripArc standalone visual and input separately from PivotArc

## Joint — final phase only
- [ ] Mount BOTH finished solo modules using rigid placement; change no visual styling, native key, legend or relative button position
- [ ] No overlapping manufactured shells or duplicated live inputs; explicitly unavailable when a device cannot fit the complete assembly

## Acceptance evidence
| Gate | Result |
|---|---|
| Design prepared | PENDING |
| Solo PivotArc visual | PENDING |
| Solo PivotArc input/CI | PENDING |
| Solo GripArc visual | PENDING |
| Solo GripArc input/CI | PENDING |
| Integrated family APK | PENDING — one build covers the whole cohort |
| Device acceptance | PENDING — per console, from the same cohort APK |
| Joint | DEFERRED |

*Packet preparation is not runtime support, complete styling or owner acceptance.*
