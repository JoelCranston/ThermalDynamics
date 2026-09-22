# TODO — ThermalDynamics

Current, outstanding work only. See [progress-log.md](progress-log.md) for what's
already done and why.

## Next up

The plan for all four repos is `../CoFHCore/docs/port-plan.md`; this repo's steps are §4 (Phase 0,
per repo), §5 A.2/A.3 (1.21.1) and §6 B.10 (26.1.2).

Phase 0 and the A.1 source categories are **done** (2026-09-22) — see progress-log.md. The
`../ThermalDynamicsForNeoForge` fork was cloned and diffed (37 files differ; the rest are
byte-identical, so that diff is effectively the whole 1.21.1 port for this repo).

1. **Blocked on ThermalCore's Phase A.** `./gradlew compileJava` still fails inside
   `:ThermalCore:compileJava`; nothing in this repo reaches javac until that lands.
   Verified meanwhile with a standalone `javac` of `src/main/java` against
   `build/moddev/artifacts/neoforge-21.1.251{,-merged}.jar` + CoFHCore's
   `build/classes/java/main`: **every remaining error is an unresolved
   `cofh.thermal.core` / `cofh.thermal.lib` symbol.** No Minecraft or NeoForge API is
   unresolved.
2. When ThermalCore compiles: run `./gradlew build`, then
   `../Pyronetics/scripts/verify_runserver.sh`, then Joel's `runClient` check.
3. Datagen has not been run yet (`./gradlew runData`) — the loot/recipe/tag providers were
   ported blind. Re-run and diff `src/main/generated` once the build works.

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
