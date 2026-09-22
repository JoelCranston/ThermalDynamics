# TODO — ThermalDynamics

Current, outstanding work only. See [progress-log.md](progress-log.md) for what's
already done and why.

## Next up

1. **Blocked on CoFHCore's 1.20.6 hop.** `gradle.properties`/`build.gradle` here are
   already bumped to `mc_version=1.20.6`/`neo_version=20.6.141`/`java_version=21`
   locally (uncommitted) — don't commit or start migrating this repo's own code until
   `../CoFHCore` compiles clean on 1.20.6 (check its `docs/TODO.md`), since this repo
   depends on it (and on ThermalCore) directly and any attempt here would just inherit
   their breakage on top of its own.
2. Once unblocked: run `./gradlew compileJava`, triage by root-cause category (see
   CoFHCore's `docs/progress-log.md` "working method" section), verifying each API
   shape via `javap` against the real mapped jar. Check
   `../CoFHCore/docs/api-notes-1.20.6.md` first — several categories will likely recur
   here, and this repo's duct-network/grid code (`EnergyGrid`/duct tiles) is exactly
   the kind of code the `HolderLookup.Provider`-threading and `FluidStack`
   `copyWithAmount` categories tend to touch.

## Inbox

_(nothing yet — add anything noticed mid-session here rather than letting it get lost)_
