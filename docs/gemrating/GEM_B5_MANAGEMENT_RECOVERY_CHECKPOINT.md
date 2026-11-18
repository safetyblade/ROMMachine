# GEM-B5 — catalogue management and personal recovery checkpoint

## Approved scope
P05/P07/P08: offline first; manual local package import and optional online check; previous-version rollback; preserve local corrections; retire removed records without reuse. GEM personal export plus a SafeSave full-player component. No account requirement, live sync, ROM/media sharing or changes to engine SaveBinding. BRAND-01 shared menu rows and GEM Manage destination.

## Catalogue updates
The baseline is the existing 14,216-record independently pinned dataset. New full catalogue releases use the GEMC1 binary envelope:
- ASCII magic GEMC1 (5 bytes), big-endian positive signed int64 sequence, big-endian int32 JSON byte length, exact schema-1 JSON payload.
- Big-endian int32 signature length and RSA SHA-256 signature over magic, sequence, length and payload. Payload limit 32 MiB; RSA 2048–8192-bit signature lengths only.
- The APK obtains the independently trusted DER/SPKI public key from `gem/update-public-key.der` (not from the imported file). Optional `gem/update-origin.txt` must be a controlled HTTPS URL. No redirects or automatic startup network requests.
- Preview validates signature, schema, source declarations, existing stable IDs and increasing sequence. New release IDs are added; missing IDs retire; history of source ID, source record ID, system and exact source title prevents reassignment even after retirement. Changes to those identity fields require a new ID rather than silent correction of identity. Genre/other allowed fields may be revised.
- Activation verifies candidate bytes again, persists and reopens the signed package, then atomically writes the active/previous pointer and identity history. Rollback swaps to previous validated version and retains highest accepted sequence. Bad active content falls back to verified previous/bundled data. Imported catalogues cannot change ratings, overrides, shelf or save paths.
- Same-version bundled shard restoration is retained separately. Signer and endpoint not provisioned; new-version actions deliberately disabled in this candidate. No actual independently approved second production version was issued. One-version rollback across a future APK change to its bundled baseline requires explicit compatibility testing.

### Controlled publishing
Review data licensing, provenance, stable IDs, eligibility and exact package validation first. Use an externally controlled RSA release key, never the APK signing key or a key in source control. Generate public DER via OpenSSL pkey -pubout -outform DER from the approved key; pin it in the APK asset at release preparation. `python3 scripts/gem/sign_catalogue_update.py manifest.json update.gemcatalogue --sequence N --private-key /controlled/key.pem` signs already-reviewed content, not a legal/data-quality approval. Deploy the exact signed package to the approved HTTPS origin. No private key or endpoint invented for this branch.

## Personal GEM recovery
- Backward-compatible player ledger adds nullable Preferences (wildcard target, recent surprises, shelf display). Prior SharedPreferences read only when the preference field is missing; the first verified ledger write persists the migrated values. No deletion of legacy data.
- Versioned .gembackup envelope includes exact player UUID, cartridge UUID ratings, explicit clears/revisions, display metadata and preferences; SHA-256 for corruption detection, not authentication. No ROM, save, credentials or filesystem paths.
- SAF export verifies readback where supported; unverified destinations are reported accurately. Import size bounded and decoded before preview. Keep existing by default including tombstones; optional explicit conflict replacement and preference restore; different source player requires deliberate copy confirmation. UUID only, no filename/title auto-transfer. Stale preview rejected. A local recovery copy is written before atomic ledger replacement. Recovery review is a merge, not deletion of new local records. Global cartridge identification links/custom information stay local and are preserved through catalogue changes; on another installation links must be confirmed again.
- GEM metadata is private, not encrypted in standalone export; user chooses destination. No cloud or automatic upload.

## SafeSave dependency
`GemSafeSaveComponent` supplies a versioned owner/path/length/hash descriptor and verified profile bytes; capture, prepare and apply call the authorized rating repository. Optional component path `components/gem/<playerUUID>/profile.gembackup` for future PLAYER/ALL_PLAYERS archive. Full archive coordinator must explicitly adopt extension, authorize target and copy, validate complete archive paths/limits, journal group recovery, and include this component in full-player export. SAVE-01/02 #290 and PROF-01 #289 are design-only; do not represent this component as a complete .cartsave, integrate it into legacy Drive sync, or touch engine save files. Legacy manager bridge only; authoritative Junior/session validation remains required before multi-player launch.

## Acceptance
B4 exact source import-order finding corrected. Eleven B5 test cases authored (6 backup/component, 4 version store incl fixture-only test signing key, 1 migration); B4 regression cases retained. No final execution pass claimed until exact candidate Kotlin/Room/catalogue/APK gates and physical install-over, restore/corruption/recovery and profile tests complete. Test keys generated at test runtime only. PR #343 draft; master not modified.
