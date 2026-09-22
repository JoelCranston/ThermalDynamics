# ThermalDynamics — NeoForge port

## Where things are

| File | Read it when |
|---|---|
| **This file** | Always. Context, decisions, and current state. |
| `../CoFHCore/docs/port-plan.md` | **Starting any porting work.** The approved plan (2026-09-21) for all four repos: 1.21.1 then 26.1.2 directly, with every version, coordinate, replacement API and reference stated. This repo's part: §5 A.2/A.3 and §6 B.10. |
| [docs/TODO.md](docs/TODO.md) | Picking up work. **Anything noticed mid-session goes in its Inbox.** |
| `../ThermalDynamicsForNeoForge/` | Porting this repo to 1.21.1. SPLIGAN's minimal-diff 1.21.1 port of this exact codebase — `diff -ru src ../ThermalDynamicsForNeoForge/src` is the Phase A worklist (review each hunk, apply the API changes, skip their build/IDE noise). |
| `../Pyronetics/` + its `docs/api-notes-26.1.2.md` | Any 26.1.2 shape. Joel's from-scratch mod, built and running on NeoForge 26.1.2.109 — read it before deriving a shape from the primer. |
| `../CoFHCore/docs/reference/` (local only) | Confirming an API shape: vendored primers, NeoForge release notes, 26.1/1.21.1 docs, source file lists. `../CoFHCore/scripts/fetch_reference.sh` recreates it. |
| [docs/progress-log.md](docs/progress-log.md) | The story behind a decision, or what's already been done and why. Append-only. |
| `../CoFHCore/docs/api-notes-1.20.6.md` (then `-1.21.1.md`, `-26.1.2.md`) | Writing any code against a 1.20.5+ API. CoFHCore is the foundation library and hits every hop's API breakage first — check there before re-deriving something this repo will very likely also hit. |
| `docs/context/` (**local only, gitignored**) | The progress log doesn't have the detail you need. Full session transcript exports — this whole 4-repo porting effort runs in one shared session, exported under `../ThermalExpansion/docs/context/` (that's the session's project directory) rather than duplicated into each repo. `grep` it, don't read it whole. |

## Context

ThermalDynamics depends on **CoFHCore** and **ThermalCore** (`../CoFHCore`,
`../ThermalCore`). **ThermalExpansion** is a sibling at the same level. All four repos
are worked in **one shared Claude Code session**, in dependency order each hop —
CoFHCore first, then this repo and the others.

This is a port of the existing NeoForge 1.20.4 codebase to **26.1.2** in exactly two hops —
**1.21.1**, then **26.1.2** directly (Joel, 2026-09-21; no other intermediates). Not a
one-version-at-a-time primer climb any more — see `../CoFHCore/docs/port-plan.md` §1/§3
and `../CoFHCore/docs/progress-log.md`'s revised Phase 2 entry.

This is unrelated to **Pyronetics** (`../Pyronetics`), a separate from-scratch mod in
its own repo/session with zero dependency on this codebase.

## Decisions already made

Same as CoFHCore's (`../CoFHCore/CLAUDE.md`) — license (CoFH "Don't Be a Jerk"), target
26.1.2.109 via 1.21.1 (21.1.251) only, ModDevGradle 2.0.147 as the build plugin from Phase 0,
branch-per-target-version, verify every API shape against the real (patched-sources) jar
before writing code against it.

## Current state

Branch **`1.21.1`** (created 2026-09-21). `gradle.properties`/`build.gradle` still carry the
old uncommitted 1.20.6 bump — **do not commit those values**; they are replaced wholesale by
Phase 0.3/0.4 (ModDevGradle switch, `neoforge.mods.toml`) and Phase A.0 (1.21.1 values) from
`../CoFHCore/docs/port-plan.md` §4.3–§4.4 and §5 A.0.

**Blocked on CoFHCore**: this repo's own Phase A starts only once `../CoFHCore` builds clean
on 1.21.1 and boots headless (its `docs/TODO.md` tracks that). Then: apply Phase 0.3/0.4
here, bump to 1.21.1, and work through the SPLIGAN diff (port-plan.md §5 A.2/A.3).
