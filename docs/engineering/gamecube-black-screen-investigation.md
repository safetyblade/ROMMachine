# GameCube black-screen investigation

Owner symptom: GameCube opens a black gameplay screen; PS2 separately fails to load.

The pinned Dolphin core `32c61f9d0e933331bb7087ba57e96883241b4960` requests a GLES hardware context and supplies its video through Libretro's framebuffer callback. Its hardware renderer leaves its own vertex array, vertex buffers and raster state current. The inherited LibretroDroid presentation code used CPU-memory vertex pointers without isolating those buffers or the vertex array, and inherited blend/scissor/cull/stencil/color-mask state. Consequently its pointers can be interpreted as offsets inside a core buffer, the core's cached vertex state can be modified, or the presentation draw can be masked/clipped. Restoring only the GL program or framebuffer does not fix that boundary.

`libretrodroid-frontend-gl-state.patch` gives presentation and the immersive background their own VBO/VAO and restores the core's state after image upload and presentation. GLES2 retains its original attribute state; the GLES3 path restores separate read/draw framebuffers, unpack buffer/row length, samplers and VAO. GL resources are recreated after context loss, and lost-context names are never deleted in the replacement context.

Validation: `test_libretrodroid_gl_state.sh` compiles the actual patched helper against the official Android GLES/EGL declarations and a deterministic fake driver. It starts with a bound core VBO/VAO, enabled scissor/blend/stencil, masked color channels, different read/draw framebuffers, samplers and unpack buffers. It verifies frontend vertices are buffer offsets in an independent buffer, every owned core state is restored, GLES2 attributes survive, and replacement-context resources are reclaimed without deleting lost-context names.

This is a confirmed code-level rendering-boundary defect. It is not proof that this is the sole cause of the owner's black screen. Actual game frames, audio and saved-game round trips remain **NOT TESTED** without the affected ARM64 device and title. The rebuilt host and final APK must be checked on that device before the reported live issue is accepted as resolved.
