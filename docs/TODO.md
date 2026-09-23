# TODO — ThermalDynamics

Current, outstanding work only. See [progress-log.md](progress-log.md) for what's
already done and why.

## Next up

The plan for all four repos is `../CoFHCore/docs/port-plan.md`; this repo's steps are §4 (Phase 0,
per repo), §5 A.2/A.3 (1.21.1) and §6 B.10 (26.1.2).

Phase B (26.1.2) is done for this repo on branch `26.1.2` (2026-09-22): `./gradlew build` is clean,
`runData` matches the committed output, and the dedicated server boots to `Done` with 1770 recipes
and no data errors. Shapes, decisions and forced behaviour changes are in
`../CoFHCore/docs/api-notes-26.1.2.md` ("B.10 ThermalExpansion and ThermalDynamics") and
`../CoFHCore/docs/TODO.md` (Inbox, "B.10 ThermalExpansion / ThermalDynamics behaviour changes"). This
repo builds against whatever branches `../CoFHCore` and `../ThermalCore` have checked out; all are on
`26.1.2` now.

1. **Joel's `runClient` pass** (port plan §A.4 / §B.10) — everything client-side is unverified on both
   hops. See `../CoFHCore/docs/TODO.md` for the checklist.
2. **B.4 at runtime**: the transfer-API adapters (servos, limiters, filters, grid storages / machine
   handlers) have only been compiled and booted; test with a pipe mod or a GameTest, including an
   aborted simulation.

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
