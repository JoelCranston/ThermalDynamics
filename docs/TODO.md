# TODO — ThermalDynamics

Current, outstanding work only. See [progress-log.md](progress-log.md) for what's
already done and why.

## Next up

The plan for all four repos is `../CoFHCore/docs/port-plan.md`; this repo's steps are §4 (Phase 0,
per repo), §5 A.2/A.3 (1.21.1) and §6 B.10 (26.1.2).

Phase A (1.21.1) is **code-complete** on branch `1.21.1`: `./gradlew build` is clean,
`verify_runserver.sh` reaches `Done`, and `runData` runs and matches the committed output
(2026-09-22). This repo builds against whatever branch `../CoFHCore` has checked out, so
**put CoFHCore on `1.21.1` to build or run it**. CoFHCore's working branch is `26.1.2` now.

1. **Joel's `runClient` pass** (port plan §A.4). It is the one Phase A exit criterion left, and
   everything client-side is unverified. See `../CoFHCore/docs/TODO.md` for the checklist and
   for `MouseHandlerMixin`, a specific suspect.
2. **Phase B (B.10) waits for CoFHCore's 26.1.2 port** (B.0-B.2 done there, ~1537 errors left).
   Nothing to do here until CoFHCore compiles on 26.1.2. When it does, branch `26.1.2` from
   `1.21.1`, switch the data run back to `clientData()`, and **regenerate `src/main/generated`
   rather than hand-migrating it** (see the progress log's runData entry for why).

## Inbox

- **Client-side duct GUI behaviour to eyeball in `runClient`.** `DuctBlock` moved from
  `use` to `useItemOn` and deliberately does *not* override `useWithoutItem` (see that
  commit's message). Worth confirming in game that: empty-hand click opens the duct /
  attachment GUI, a wrench still connects/disconnects, and right-clicking a duct while
  holding an unrelated block still places that block.
- **`GridDebugPayload#id()` used to return `null`** (pre-existing bug, so the debug
  renderer packet could never have been sent). Now fixed by the `Type` constant, but the
  grid debug overlay has therefore never actually been exercised on this branch.
- **`FluidGridStorage`'s NBT layout changed shape slightly.** `FluidStack#save` throws on
  an empty stack, so the write path goes through `saveOptional` and merges the result flat,
  matching what `writeToNBT` used to produce. Existing 1.20.4 grid saves are not readable
  across the MC version anyway, but note it if grid fluid loss is ever reported.
- **`SlotFalseBuffer` / `DuctBlockItem` hunks in SPLIGAN's fork were skipped deliberately.**
  Theirs are cosmetic (`this.` removal) or a behaviour rewrite of
  `computeConnectionPreference`; `UseOnContext#getHitResult` is still public on 1.21.1,
  confirmed in the sources jar, so no API change was required.
