## Scope
- [ ] Change is confined to the stated lane and uses the current master baseline.
- [ ] No unrelated engine, save, profile, schema, signing or packaging changes.

## Evidence (record exact head and base SHAs)
- [ ] Pinned Kotlin formatter and whole-repository lint passed.
- [ ] Existing behavioural tests retained; targeted regression test added where a defect is fixed.
- [ ] UI: portrait, short landscape, section/filters, import/resume/back and controller/focus checked as applicable; note unverified device-only items.
- [ ] UI: complete connected APK/unit suite and UI merge gate passed for the exact prospective merge tree.
- [ ] No cancelled, skipped, failed or stale check counted as a UI pass.

## Integration
- [ ] Current head and base match verified revision; one UI merge at a time.
- [ ] After merge, exact master build and matching APK artifact verified before next UI merge.
- Last known-good master artifact:
- Outstanding risks / device acceptance:
