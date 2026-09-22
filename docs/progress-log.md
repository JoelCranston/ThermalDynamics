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

## Phase A — 1.21.1 source port (2026-09-22)

`../ThermalDynamicsForNeoForge` cloned and diffed against this tree. Only **37 of 105
files differ**, with no files on either side that the other lacks — their fork started
from the same NeoForge 1.20.4 code, so that diff is effectively the complete 1.21.1
worklist for this repo. It was applied hunk by hunk, adapted wherever it calls a CoFHCore
method we named differently, and skipped wherever it is their own refactor rather than an
API change.

Every API shape was confirmed against the real jars before writing code:
`build/moddev/artifacts/neoforge-21.1.251-sources.jar` and
`~/.gradle/caches/.../neoforge-21.1.251-sources.jar`.

One commit per root cause:

- `3e37480` **mod metadata & bus.** `@Mod.EventBusSubscriber` →
  `net.neoforged.fml.common.EventBusSubscriber`; `MenuScreens.register` (now `@Deprecated`
  + `@ApiStatus.Internal`) → `RegisterMenuScreensEvent`. `DebugRenderer`'s annotation was
  dead (no `@SubscribeEvent` methods) and is gone.
- `5aa3da3` **ItemStack NBT → data components.** The redprint branch in
  `DuctBlockEntity` goes through `ItemHelper.getCustomData`/`mutateCustomData`;
  `CustomData.update` drops the component itself when the mutator writes nothing, which
  replaces the old `setTag(null)` clean-up.
- `1dbce2a` **vertex API.** `addVertex`/`setColor`, `endVertex()` deleted, and
  `MultiBufferSource.immediateWithBuffers` now takes a
  `SequencedMap<RenderType, ByteBufferBuilder>`.
- `0d2b2a3` **persistence threading.** `HolderLookup.Provider` through `loadAdditional`/
  `saveAdditional`/`getUpdateTag`, `INBTSerializable`, `SavedData#save` and its
  `Factory` deserializer, and — because CoFHCore's `IFilter` and `SimpleItemInv` need it —
  all the way through `IAttachment#read/write`, `IAttachmentFactory` and
  `AttachmentRegistry`. `IConveyableData` keeps its `(Player, CompoundTag)` shape and
  takes the lookup off `player.registryAccess()`.
- `18c1bb8` **FluidStack.** `copyWithAmount`, `isSameFluidSameComponents`,
  `parseOptional`/`saveOptional`. The GUI/state packets hand-encode fluid id + amount:
  they travel on the plain scratch `FriendlyByteBuf` CoFHCore allocates and
  `FluidStack.STREAM_CODEC` requires a `RegistryFriendlyByteBuf`. (SPLIGAN casts one to
  the other, which would `ClassCastException` at runtime.)
- `0ce6199` **events.** `TickEvent.LevelTickEvent` → `LevelTickEvent.Post`; the
  `phase == END` guard disappears with it.
- `aec2ccf` **block interaction.** `Block#use` → `useItemOn` returning
  `ItemInteractionResult`. `useWithoutItem` is deliberately left alone — see the commit
  message for why delegating into it would break placing a held block on a duct.
- `997fc7c` **networking.** `CustomPacketPayload.Type` + `StreamCodec`,
  `RegisterPayloadHandlersEvent`/`PayloadRegistrar`, `IPayloadContext`, and
  `PacketDistributor`'s builder replaced by `sendToServer`/`sendToAllPlayers`/
  CoFHCore's `Utils.sendNear`. The three blob-carrying payloads keep their
  `FriendlyByteBuf` component via CoFHCore's `PayloadCodecs.REMAINING_BYTES`.
- `224cf5c` **access transformers** re-synced byte-for-byte with CoFHCore's;
  `createMinecraftArtifacts` passes here now.
- `a6ab1c7` **geometry loader & datagen.** `IUnbakedGeometry#bake` and
  `BlockModel.bakeFace` lost their `ResourceLocation`; `BlockElementFace` is a record;
  `RegisterGeometryLoaders#register` takes a `ResourceLocation`; the loot and recipe
  providers take the registry lookup; `Tags.Items.GLASS` → `GLASS_BLOCKS`.

Not applicable in this repo: `Holder<MobEffect>`/`PotionContents`, enchantment datapack
objects, recipe serializers, `IPlantable`/`PlantType`, mixins (none), Curios (no
dependency). The resources sweep landed in Phase 0 (`dc73548`).

**Compile status.** `./gradlew compileJava` still fails inside `:ThermalCore:compileJava`
(ThermalCore is being ported in parallel), so this repo never reaches javac through
Gradle. Verified instead with a standalone `javac` of all 105 sources against the
1.21.1 jars plus CoFHCore's `build/classes/java/main`: **122 errors, every one of them an
unresolved `cofh.thermal.core` / `cofh.thermal.lib` symbol.** Nothing in Minecraft or
NeoForge is unresolved.

---

## Phase A complete — 1.21.1 (2026-09-22)

Ported **source-level and in parallel with ThermalCore**, which meant working without a
compiler: this repo's build `includeBuild`s ThermalCore, so `./gradlew compileJava` stopped
there the whole time. Every shape came out of the 21.1.251 sources jar or CoFHCore's
already-ported code, checked by grep rather than by javac. It compiled clean on the first real
attempt once ThermalCore landed, and boots headless with no data errors.

Ten commits, one per root cause: event bus and menu screens → ItemStack NBT to `CUSTOM_DATA` →
vertex API in the grid debug renderer → `HolderLookup.Provider` through persistence → FluidStack
copy/compare/wire format → `LevelTickEvent.Post` → `Block#use` → `useItemOn` → payload types and
stream codecs → access transformers back in step with CoFHCore → geometry loader and datagen.

Only 37 of 105 files differed from `ThermalDynamicsForNeoForge`, so that diff was effectively
the whole worklist — with one deliberate divergence: their `registryBuf(buffer)` helper casts a
plain `FriendlyByteBuf` to `RegistryFriendlyByteBuf` in every fluid packet, which is a runtime
`ClassCastException` against CoFH's scratch buffers. Five sites here go through
`FluidHelper.writeFluidStack`/`readFluidStack` instead.

### Owed

`runData` has not been run, so the generated loot, recipe and tag output is unverified; and the
client pass (a duct network moving items and energy). Shapes:
`../CoFHCore/docs/api-notes-1.21.1.md`.

---

## runData on 1.21.1 (2026-09-22)

`./gradlew runData` had never worked on this branch. `build.gradle` declared `clientData()`,
which ModDevGradle only offers from 1.21.4, so `prepareDataRun` failed. 1.21.1's run type is
`data()`.

Regenerating added the 8 recipe-unlock advancements that had never been committed (energy and
fluid ducts, item buffer, the attachments), so those recipes never unlocked in the recipe book.
The recipes only lose `"show_notification": true`, which is the default. Commit `c179946`.

The same pass found a client crash in CoFHCore (`LevelRendererMixin`, stale `renderLevel`
signature), since the data run is a client-dist launch. See `../CoFHCore/docs/progress-log.md`,
"Phase A follow-up — runData".
