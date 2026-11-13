# Xbox presentation evidence and engine selection

Refs #86, #143. Source-level fallback bookkeeping correction; no graphics repair.

The owner has an accepted guest Vulkan present with a white viewport. The Android
adapter nevertheless called EngineSelectionPreferences.recordSuccess, which
promoted lastSelected into successful, rotated previous engines and cleared
failure evidence. That callback establishes none of those compatibility claims.

A separate presentation outcome now records the selected engine identity and
PRESENT_ACCEPTED stage without modifying successful/previous, failure guards or
user preferences. Xbox uses that method explicitly. recordSuccess also routes
Xbox to presentation evidence, preventing an accidental old call site from
promoting a white image. Existing non-Xbox observed-core-video outcomes are intact.
Previously stored choices are not deleted or migrated; version rollback and
installed HDD/saves are untouched. This does not retroactively validate historical
Xbox presentation-derived successful records, or offer any nonexistent engine.

Added tests cover accepted presentation after an earlier proven engine, failure
preservation, and the Xbox legacy-method guard. They compile for the existing
Android test suite. Runtime execution remains blocked locally by the user-retained
Robolectric artifact restriction; CI must execute them before acceptance.

PR #168 independently supplies opt-in numerical guest/swapchain/SurfaceView probes.
A matching owner report is still required to identify and repair the white-output
stage. No gameplay, audio/input/HDD persistence or final-frame acceptance follows
from this source correction.
