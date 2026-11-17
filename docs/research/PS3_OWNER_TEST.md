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
