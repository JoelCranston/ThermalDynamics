# ThermalDynamics — NeoForge port

## Where things are

| File | Read it when |
|---|---|
| **This file** | Always. Context, decisions, and current state. |
| [docs/TODO.md](docs/TODO.md) | Picking up work. **Anything noticed mid-session goes in its Inbox.** |
| [docs/progress-log.md](docs/progress-log.md) | The story behind a decision, or what's already been done and why. Append-only. |
| `../CoFHCore/docs/api-notes-1.20.6.md` | Writing any code against a 1.20.5/1.20.6 API. CoFHCore is the foundation library and hits every hop's API breakage first — check there before re-deriving something this repo will very likely also hit. |
| `docs/context/` (**local only, gitignored**) | The progress log doesn't have the detail you need. Full session transcript exports — this whole 4-repo porting effort runs in one shared session, exported under `../ThermalExpansion/docs/context/` (that's the session's project directory) rather than duplicated into each repo. `grep` it, don't read it whole. |

## Context

ThermalDynamics depends on **CoFHCore** and **ThermalCore** (`../CoFHCore`,
`../ThermalCore`). **ThermalExpansion** is a sibling at the same level. All four repos
are worked in **one shared Claude Code session**, in dependency order each hop —
CoFHCore first, then this repo and the others.

This is a **primer-climbing port**: starting from this repo's existing NeoForge 1.20.4
codebase and climbing NeoForge's official primers one version at a time toward the
target, **26.1.2** — see `../CoFHCore/CLAUDE.md` for why.

This is unrelated to **Pyronetics** (`../Pyronetics`), a separate from-scratch mod in
its own repo/session with zero dependency on this codebase.

## Decisions already made

Same as CoFHCore's (`../CoFHCore/CLAUDE.md`) — license (CoFH "Don't Be a Jerk"), target
26.1.2 via the full primer chain, branch-per-target-version, verify every API shape
against the real mapped jar via `javap` before writing code against it.

## Current state

Branch `1.20.6`. `gradle.properties`/`build.gradle` are bumped to 1.20.6 locally but
**uncommitted** — waiting on CoFHCore to compile clean on 1.20.6 first. See
[docs/TODO.md](docs/TODO.md).

**Next step**: once `../CoFHCore` compiles clean on 1.20.6, commit the version bump
here and start this repo's own 1.20.6 migration, checking
`../CoFHCore/docs/api-notes-1.20.6.md` first for API shapes already confirmed.
