# TODO — ThermalDynamics

Current, outstanding work only. See [progress-log.md](progress-log.md) for what's
already done and why.

## Next up

The plan for all four repos is `../CoFHCore/docs/port-plan.md`; this repo's steps are §4 (Phase 0,
per repo), §5 A.2/A.3 (1.21.1) and §6 B.10 (26.1.2).

1. **Blocked on CoFHCore's Phase A** (`../CoFHCore/docs/TODO.md`). Nothing here compiles
   independently of it.
2. When unblocked, in this order: Phase 0.3 (ModDevGradle 2.0.147, template in port-plan.md
   §4.3), 0.4 (`git mv META-INF/mods.toml META-INF/neoforge.mods.toml`), A.0 (`gradle.properties`
   values from §5 A.0), then `diff -ru src ../ThermalDynamicsForNeoForge/src` and apply the 1.21.1 hunks;
   compile; fix what remains by category, reusing CoFHCore's confirmed shapes
   (`../CoFHCore/docs/api-notes-1.21.1.md`) rather than re-deriving them.
3. Resources sweep (§5 A.1.15): singular tag/data folders, `"forge:` → `"c:`, in
   `src/main/resources` and `src/main/generated`.
4. `../Pyronetics/scripts/verify_runserver.sh` passes; Joel's `runClient` check; commit.

## Inbox

_(nothing yet — add anything noticed mid-session here rather than letting it get lost)_
