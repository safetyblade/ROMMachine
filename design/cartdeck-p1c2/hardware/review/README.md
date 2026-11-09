# CARTDECK P1-C2A — Dimensioned Visual Review

**These are static production-layout proofs, not screenshots of the installed APK or a real emulated game.** The scenic video within the preview is original abstract test artwork. The owner’s actual screenshots were assessed in the design brief and project issue #74; the exact screenshot image bytes were not available as image files in this source branch.

### Comparison boards
- `control-profiles-board.svg`: two-face standard 4:3, four-face standard 4:3, PS1 with analog socket, and tall DS with independent mic/screen/menu/C-slot positioning.
- `ds-aux-board.svg`: DS Original/Green, featuring the distinct decorated compact C-slot above the right utility control instead of occupying a button slot.
- `editions-board.svg`: Original/Pink/Green all using precisely the same outline and physical hardware zones.
- `glow-off-on.svg`: hardware visually complete unlit versus selectively lit shoulders, bezel edge and lower seam. Only sourced masks—not runtime UI.
- Individual phone mockups are editable `.svg` and share the same 393×852 reference coordinate system.

### Measured baseline versus proposal
| Feature | Existing PR #78 runtime | Proposed P1-C2A source treatment |
| --- | --- | --- |
| Standard display | Working responsive 4:3, 7dp physical bezel | Keep geometry; add inset glass lip and matching structural-to-chassis transition |
| Nintendo DS | Working useful expanded combined display, small compact C-slot | Keep size/touch; add tall matching bezel and integrated DS auxiliary strip |
| Lower presentation | `PadContainer` `GlassSurface` transparent/float-like overlay | Solid factory deck with connected upper/lower seams and control seats |
| PS1 analog | Existing PadKit analog may appear visually detached | Conditional round machined socket follows actual analog group |
| Lower seam | Continuous line near bottom, only split for DS brand | Segments clipped around **all** functional/label/brand rectangles |
| Accent lights | Fixed decorative accent present | Separate unlit/on/mask components; all glow **off** by default |
| User faceplate art | Not part of P1-C2 | Reserve masked residual regions, implement optional decorations only in #75 |

Reference dimensions are illustrative, **not** Android dp. Actual positions derive from `GameScreenLayout.kt`, `MobileGameScreen.kt`, measured renderer bounds and PadKit groups. Review actual APK only after Elliot's post-approval work.
