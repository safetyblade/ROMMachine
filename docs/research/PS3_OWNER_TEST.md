# PS3 owner-testing candidate

Use the manually dispatched **CARTDECK PS3 owner testing APK** workflow on master.
It reuses source workflow run 44012020331 initially (both pinned PS3 engine inputs
verified and not expired at registration). Select a fresh successful PS3 source
run if these expire; never weaken the pin/receipt/ELF checks. The ordinary CARTDECK
APK workflow and native recipes are unchanged.

The repository is public; downloadable Actions artifacts are public even if called
debug/internal. For owner testing only, add repository Actions secret
`CARTDECK_OWNER_TEST_PASSPHRASE` with a unique strong password (24+ characters).
Keep the password private. Run the workflow with the verified input run ID.
It assembles the normal package/signing/version path, verifies both PS3 payloads
and the existing engine inventory, and uploads only a password-encrypted 7z
containing APK and evidence, not a publicly installable combined APK.
Download the outer Actions ZIP, open the enclosed 7z with the password, install
the APK over the existing CARTDECK without uninstalling. No user game, BIOS or
PS3 firmware is bundled; real firmware/gameplay acceptance remains pending.

This limits plaintext artifact exposure; it is not legal clearance or a licence
compatibility finding. Do not share the password or decrypted APK. Public release
and any distribution requiring clearance remain controlled by REL-ENG-01 #307.
The source/receipt checks and stable console workflow remain mandatory.

## Automatic master build integration

Every successful standard master APK build now automatically selects a trusted
successful PS3 source workflow with both nonexpired native inputs, then builds
the PS3-inclusive owner candidate at the same app revision and console-host
recipe. Standard unencrypted APK remains a separate verified fallback; it
does not contain PS3 and is not the owner-test candidate. Pull requests do not
automatically package PS3. Download CARTDECK_PS3_OWNER_TEST_ENCRYPTED from
the same master Actions run after that job succeeds. The password secret
remains necessary for delivery; absence fails the PS3 job explicitly instead
of presenting a PS3-less APK as a PS3 build. Native artifact expiration needs
a fresh PS3 source producer, not a full console rebuild. The standalone
owner-test workflow remains available as a manual retry.
