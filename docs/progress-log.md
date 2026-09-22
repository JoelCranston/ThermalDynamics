# Progress log — ThermalDynamics

Append-only. Later entries correct earlier ones rather than editing them away. See
[TODO.md](TODO.md) for what's outstanding right now.

## Phase 0 — pre-existing state

This repo already had a working NeoForge **1.20.4** port before the current porting
effort started (`71991d6` "1.20.4 Initial Port Work" and follow-ups) — but it was
**stalled**, unlike the other three repos: not yet wired into the composite build, no
dev run configs, and a stale SRG-named access transformer left over from the port.

## Phase 1 — finishing the stalled port, then modern tooling

`d67e3d7` "Finish stalled NeoForge 1.20.4 port" brought it up to the same working state
as the other three repos in one pass:
- Wired the composite build to local CoFHCore/ThermalCore (`settings.gradle`).
- Added dev run configs (client/server/data) and Gradle heap settings.
- Fixed the broken `javafml` loaderVersion requirement (`'[20,)' → '[2,)'`) — the same
  bug hit in all four repos.
- Replaced the stale SRG-named `accesstransformer.cfg` (never updated during the
  original port) with CoFHCore's already-modernized "CoFH Master AT List", resolving
  `Entity#level`, `Slot#slot`, and `UseOnContext#getHitResult` visibility for
  compilation.
- Fixed remaining direct field access now that vanilla getters are used instead:
  `player.level` → `player.level()`, `this.slot` → `getSlotIndex()`.

Verified via `runServer`: `thermal_dynamics` scanned and constructed as an `@Mod`
class, server reached `Done`, no `LanguageLoadingProvider` errors.

Then the same modern-tooling pass as the other repos:
- `fa0f3b5` Upgrade to Gradle 9.2.1 + NeoGradle userdev 7.1.38.

## Phase 2 — the primer climb

Full primer chain, decision to target 26.1.2, and CoFHCore-first dependency ordering:
see `../CoFHCore/docs/progress-log.md`'s Phase 2 entry — this repo follows the same
plan, one hop behind CoFHCore.

### 1.20.6 hop

Not yet started. `gradle.properties`/`build.gradle` are bumped locally but uncommitted,
waiting on CoFHCore. See [TODO.md](TODO.md).

## Phase 2, revised (2026-09-21)

The route is now 1.21.1 → 26.1.2 with no other intermediates, ModDevGradle from Phase 0, and
the 1.20.6 hop abandoned before this repo ever started it — see `../CoFHCore/docs/port-plan.md`
and the matching entry in `../CoFHCore/docs/progress-log.md` for what re-verification changed.
Branch `1.21.1` created today. `../ThermalDynamicsForNeoForge` (SPLIGAN's 1.21.1 port of this repo) is the Phase A
worklist; `../Pyronetics` is the 26.1.2 reference. The uncommitted 1.20.6 build bump is left
uncommitted on purpose.
