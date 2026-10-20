# NeoForge 1.21.1 port

Decision (owner, 2026-10-19): RuneCraft moves from **Fabric 1.20.1** to **NeoForge 1.21.1 only**. The Fabric build is retired once the NeoForge build is green in CI (jar, server self-test, client self-test). Content rounds are paused until then.

## What changes and what doesn't

| Part | Size | Port work |
|---|---|---|
| Pure-Java core `com.runecraft.osrs` | ~505 files / 150k lines | None: no Minecraft or loader types. Unit tests keep running offline. |
| Minecraft layer (everything else under `com.runecraft`) | ~370 files / 90k lines (`content` 215, `client` 46, `combat` 19, `block` 16, `skill` 14, …) | Full port: names, loader API, 1.21.1 API. |
| `tools/gen_assets.py` resources | ~2,000 generated files | Path and format updates for 1.21 data packs. |
| CI and smoke scripts | `build.yml`, `smoke_server.sh`, `smoke_client.sh` | NeoForge server install, Java 21, run configs. |

There are no mixins and no access wideners, which keeps the port to source changes.

## Phases

1. **Freeze.** Merge the Fabric work in flight (round 31 review fixes, round 32), push, CI green. No new content rounds.
2. **Rename to Mojang mappings on 1.20.1.** Loom's `migrateMappings` task rewrites the sources from Yarn to Mojang's official names while still on Fabric 1.20.1, so the result can be checked by the existing Fabric CI before anything else changes:
   `./gradlew migrateMappings --mappings "net.minecraft:mappings:1.20.1"` then switch `build.gradle` to `mappings loom.officialMojangMappings()`. Needs `maven.fabricmc.net` and Mojang's hosts: run locally if the environment allows them, otherwise in a one-off CI job that commits the result back to the branch.
   Must stay green (jar + both self-tests) before phase 3.
3. **Build switch.** ModDevGradle (`net.neoforged.moddev`), NeoForge 21.1.x, Java 21 toolchain, `META-INF/neoforge.mods.toml` replacing `fabric.mod.json`, run configs `server`, `client` and `clientSmoke` (same system properties as today), `exportClasspath` kept so CI can publish the compile classpath.
4. **Loader API port** (table below), package by package with parallel agents, each owning disjoint packages.
5. **1.20.1 → 1.21.1 API port** (list below).
6. **Resources.** `gen_assets.py` writes 1.21 paths and formats; `pack.mcmeta` pack format 34.
7. **CI.** Java 21; `smoke_server.sh` installs a NeoForge server (`neoforge-21.1.x-installer.jar --installServer`) instead of downloading the Fabric server launcher and Fabric API; `smoke_client.sh` runs the NeoForge `clientSmoke` run. The release body says "Requires Minecraft 1.21.1 and NeoForge".
8. **Green.** Fix compile errors, then server self-test failures, then client self-test failures, until both are 0 failed. Then delete the Fabric leftovers and resume content rounds.

## Fabric API → NeoForge

All game events go on `NeoForge.EVENT_BUS`; registration and client setup events go on the mod event bus (`IEventBus` passed to the `@Mod` constructor).

| Fabric | NeoForge 1.21.1 |
|---|---|
| `ModInitializer` / `ClientModInitializer` | `@Mod` class constructor; `@Mod(dist = Dist.CLIENT)` class or `FMLClientSetupEvent` |
| `Registry.register(Registries.X, id, v)` at class init | `DeferredRegister.create(Registries.X, MODID)` (`DeferredRegister.Items` / `Blocks` helpers), registered on the mod bus. Registries are frozen outside `RegisterEvent`. |
| `FabricEntityTypeBuilder` | `EntityType.Builder.of(...).sized(..).build(id)`; attributes via `EntityAttributeCreationEvent` |
| `FabricItemGroup.builder()` | `CreativeModeTab.builder()` in a `DeferredRegister<CreativeModeTab>` |
| `ServerTickEvents.END_SERVER_TICK` / `END_WORLD_TICK` | `ServerTickEvent.Post` / `LevelTickEvent.Post` |
| `ServerLifecycleEvents.SERVER_STARTING/STARTED/STOPPING` | `ServerStartingEvent` / `ServerStartedEvent` / `ServerStoppingEvent` |
| `CommandRegistrationCallback` | `RegisterCommandsEvent` |
| `UseItemCallback` | `PlayerInteractEvent.RightClickItem` (cancel + `setCancellationResult`) |
| `UseBlockCallback` | `PlayerInteractEvent.RightClickBlock` |
| `UseEntityCallback` | `PlayerInteractEvent.EntityInteract` (and `EntityInteractSpecific`) |
| `AttackEntityCallback` | `AttackEntityEvent` |
| `PlayerBlockBreakEvents.BEFORE` | `BlockEvent.BreakEvent` |
| `ServerLivingEntityEvents.ALLOW_DAMAGE` | `LivingIncomingDamageEvent` (cancel) |
| `ServerLivingEntityEvents.ALLOW_DEATH` / `AFTER_DEATH` | `LivingDeathEvent` (cancel to prevent) |
| `ServerPlayConnectionEvents.JOIN` / `DISCONNECT` | `PlayerEvent.PlayerLoggedInEvent` / `PlayerLoggedOutEvent` |
| `ServerPlayerEvents.AFTER_RESPAWN` / `COPY_FROM` | `PlayerEvent.PlayerRespawnEvent` / `PlayerEvent.Clone` |
| `ServerEntityWorldChangeEvents` | `PlayerEvent.PlayerChangedDimensionEvent` |
| `ServerEntityEvents.ENTITY_LOAD` | `EntityJoinLevelEvent` |
| `ServerChunkEvents.CHUNK_LOAD` | `ChunkEvent.Load` |
| `FakePlayer` | `FakePlayerFactory.get(level, profile)` |
| `PacketByteBufs` + `ServerPlayNetworking` / `ClientPlayNetworking` | `CustomPacketPayload` records with `StreamCodec`s, registered in `RegisterPayloadHandlersEvent`; send with `PacketDistributor.sendToPlayer` / `sendToServer` |
| `ClientTickEvents.END_CLIENT_TICK` | `ClientTickEvent.Post` |
| `HudRenderCallback` | `RegisterGuiLayersEvent` (a layer above the hotbar) |
| `EntityRendererRegistry` / `EntityModelLayerRegistry` | `EntityRenderersEvent.RegisterRenderers` / `RegisterLayerDefinitions` |
| `KeyBindingHelper` | `RegisterKeyMappingsEvent` |
| `ItemTooltipCallback` | `ItemTooltipEvent` |
| `WorldRenderEvents` | `RenderLevelStageEvent` |
| `ModelLoadingPlugin` | `ModelEvent.RegisterAdditional` (`ModelResourceLocation.standalone`) |
| `BlockRenderLayerMap` | `"render_type": "minecraft:cutout"` in the block model JSON (gen_assets) |
| `ClientPlayConnectionEvents` | `ClientPlayerNetworkEvent.LoggingIn` / `LoggingOut` |

## 1.20.1 → 1.21.1 changes that hit this code

- **Item NBT → data components.** `getNbt`/`getOrCreateNbt`/`setNbt` (~45 sites) become a custom `DataComponentType` (preferred for our charges, notes and per-stack data) or `DataComponents.CUSTOM_DATA` with `CustomData.update`. Stack equality for stacking now compares components.
- **`PersistentState` → `SavedData`** with `SavedData.Factory` and `HolderLookup.Provider` in `save`/`load` (all `*State` classes).
- **`new Identifier(ns, path)` → `ResourceLocation.fromNamespaceAndPath`**, `ResourceLocation.parse`.
- **Attributes are `Holder<Attribute>`**; attribute modifiers use a `ResourceLocation` id instead of a UUID.
- **Armor materials are a registry** (`Holder<ArmorMaterial>` with layers); `OsrsArmorMaterial` becomes registered entries.
- **Food is a component** (`FoodProperties` via `Item.Properties.food`), and `Item.Properties` carries max stack/durability components.
- **Entity data save/load** keeps the same hooks (`addAdditionalSaveData`/`readAdditionalSaveData`), but `ItemStack.save`/`parse` take a `HolderLookup.Provider`.
- **Enchantments are data-driven**; `EnchantmentHelper` lookups need registry holders.
- **Damage**: `DamageSource` usage is unchanged, but `hurt`/`isInvulnerableTo` signatures moved; `LivingEntity.getArmorValue` and attack speed through attributes.
- **Commands**: `CommandSourceStack` API unchanged apart from names.
- **Screens/GUI**: `GuiGraphics` (already in 1.20.1 as `DrawContext` under Yarn) gained `renderTooltip` and blit changes; check every client screen.

## Resources (gen_assets.py)

| 1.20.1 | 1.21.1 |
|---|---|
| `data/<ns>/loot_tables/` | `data/<ns>/loot_table/` |
| `data/<ns>/recipes/` | `data/<ns>/recipe/` |
| `data/<ns>/tags/items/`, `tags/blocks/` | `tags/item/`, `tags/block/` |
| recipe `"result": {"item": …}` | `"result": {"id": …, "count": n}` |
| `pack.mcmeta` pack_format 15 | 34 |
| `fabric.mod.json` | `META-INF/neoforge.mods.toml` |

Worldgen, dimension, damage-type JSON and block/item models carry over (1.21.1 still uses the `models/item` layout; the 1.21.4 item-definition format does not apply).

## Compiling locally

`tools/compile_full.sh <dir>` compiles `src/main/java` against the real NeoForge classpath. CI's
`exportClasspath` step publishes those jars as `neoforge-classpath.zip` on the `dev-latest` release
(release assets download even where CI artifact storage is blocked):

    mkdir -p /tmp/nfcp && cd /tmp/nfcp && curl -sSL -o cp.zip \
      https://github.com/WreatheR6/RuneCraft/releases/download/dev-latest/neoforge-classpath.zip && unzip -q cp.zip
    tools/compile_full.sh /tmp/nfcp

## 1.21.1 fix guide (how each compile error is fixed)

Keep behaviour identical; only adapt to the new API. Shared helpers: `com.runecraft.util.StackData`,
`com.runecraft.platform.*` (the Fabric-shaped facade; don't edit it from feature code).

- **Stack NBT** → `StackData` (the `custom_data` component). `stack.getTag()` → `StackData.get(stack)` (a *copy*,
  never null: an old `tag != null` check stays harmless, `tag == null` means `!StackData.has(stack)`);
  `hasTag()` → `StackData.has`; `getOrCreateTag().putX(..)` and any edit of a fetched tag →
  `StackData.update(stack, t -> ...)` (edits to the copy are lost otherwise); `setTag(t)` → `StackData.set`.
  `ItemStack.isSameItemSameTags` → `ItemStack.isSameItemSameComponents`. "Plain, stackable stack" checks →
  `stack.getComponentsPatch().isEmpty()`.
- **Stacks in RuneCraft's own saved data** (stashes, worn slots, bank migration): read them with
  `com.runecraft.util.LegacyStacks.parse(provider, tag)`, which runs stacks saved by the Fabric 1.20.1 build through
  the vanilla data fixer first. Bank and looting-bag keys carry every non-custom-data component under
  `rc_components` (`bank/Notes`), and old keys are upgraded once on load.
- **ItemStack save/load** need a `HolderLookup.Provider` (`level.registryAccess()`, `server.registryAccess()`, or the
  provider passed to `SavedData.save`/load): `ItemStack.of(tag)` → `ItemStack.parseOptional(provider, tag)`;
  `stack.save(new CompoundTag())` → `stack.saveOptional(provider)` (`save` throws on an empty stack).
- **SavedData**: `save(CompoundTag tag, HolderLookup.Provider registries)`; loaders take `(CompoundTag, Provider)`;
  `storage.computeIfAbsent(load, create, name)` →
  `storage.computeIfAbsent(new SavedData.Factory<>(Ctor::new, Cls::load, null), name)`.
- **Mob spawning**: `finalizeSpawn(level, difficulty, reason, data, nbt)` → drop the last argument (4 params).
  `defineSynchedData()` → `defineSynchedData(SynchedEntityData.Builder builder)` + `builder.define(KEY, value)`
  (call `super.defineSynchedData(builder)` first).
- **Sounds**: many `SoundEvents` constants are `Holder.Reference<SoundEvent>` now → `.value()` where a `SoundEvent`
  is needed (or pass the holder to a `Holder<SoundEvent>` overload).
- **Screens**: `renderBackground(g)` → `renderBackground(g, mouseX, mouseY, partialTick)`, but 1.21's
  `Screen.render` already draws the background (with blur) and blurring twice in a frame throws
  `IllegalStateException: Can only blur once per frame`: when the method also calls `super.render(...)`, remove
  the explicit call instead. `mouseScrolled(x, y, amount)` → `mouseScrolled(x, y, scrollX, scrollY)` (use
  `scrollY`). `GenericDirtMessageScreen` → `GenericMessageScreen`.
- **Models**: `render(pose, vc, light, overlay, r, g, b, a)` → `render(pose, vc, light, overlay, argbColor)`
  (`renderToBuffer` likewise; `-1` for opaque white).
- **Attribute modifiers**: `AttributeModifier(ResourceLocation id, double, Operation)` (no UUID, no name);
  `ADDITION` → `ADD_VALUE`, `MULTIPLY_BASE` → `ADD_MULTIPLIED_BASE`, `MULTIPLY_TOTAL` → `ADD_MULTIPLIED_TOTAL`;
  `removeModifier(ResourceLocation)`. Derive the id from the old UUID's purpose, e.g. `RuneCraft.id("prayer_boost")`.
- **Items**: `appendHoverText(stack, level, lines, flag)` → `(stack, Item.TooltipContext ctx, lines, flag)`;
  `getUseDuration(stack)` → `getUseDuration(stack, LivingEntity entity)`; `Item#getMaxStackSize()` →
  `getDefaultMaxStackSize()`. Tools: `Tier#getIncorrectBlocksForDrops()` replaces `getLevel()`;
  `SwordItem(tier, props.attributes(SwordItem.createAttributes(tier, dmg, speed)))`, `AxeItem`/`PickaxeItem` with
  `DiggerItem.createAttributes`. Armour: `ArmorMaterial` is a record registered in `ARMOR_MATERIAL`
  (`Holder<ArmorMaterial>`), durability moves to `Item.Properties.durability(type.getDurability(n))`.
- **Blocks**: `use(state, level, pos, player, hand, hit)` → `useWithoutItem(state, level, pos, player, hit)`
  returning `InteractionResult` (or `useItemOn(...)` returning `ItemInteractionResult` when the held item matters).
  Abstract `codec()` on `BaseEntityBlock`/`HorizontalDirectionalBlock` subclasses: `simpleCodec(Ctor::new)`.
- **Arrows**: `new Arrow(level, shooter, pickupStack, weaponStack)` (pickup = the arrow item stack, weapon may be null).

## Runtime lessons (things the compiler did not catch)

- **Data pack formats.** 1.20.5 flattened int providers (`{"type":"uniform","min_inclusive":..}` instead of
  `"value":{..}`); one old-format field makes the whole world registry fail to load. `minecraft:grass` is
  `minecraft:short_grass` since 1.20.3 (StructureBlockIdsTest now rejects renamed ids).
- **Fake players.** NeoForge's `FakePlayer` connection ignores `teleport()`, so a fake player never moves;
  `platform.FakePlayer` keeps NeoForge's fake player but with a Fabric-style connection that only drops packets.
- **Item stacks over 99** cannot be saved (`ItemStack.CODEC` count range 1..99): split loot and ground drops
  into full stacks (`Inv.splitStacks`, `Inv.spawnAtLocation`, `Inv.drop`).
- **Screens.** 1.21 `Screen.render` draws the blurred background itself and a second blur in one frame throws;
  draw panels in a `renderBackground` override after `super.renderBackground`.
- **Key mappings.** NeoForge fires every mapping bound to a pressed key (vanilla kept one), so two RuneCraft keys
  on one default key both fire.
- **Damage and death.** `LivingIncomingDamageEvent` cancels leave a `DamageContainer` behind (popped by the
  bridge); Fabric's `ALLOW_DEATH` ran before the totem check, so the bridge asks it from `LivingDamageEvent.Post`.
- **Knockback.** 1.21 knocks targets back for damage without an attacker unless the type is in
  `#minecraft:no_knockback`; attacker-less OSRS hits use `runecraft:osrs_typeless`.
- **Chunk unloading livelock.** In 1.21.1 a stale unload callback for a chunk taken back after a teleport under
  lag can re-queue itself forever inside `ChunkMap.processUnloads` during `stopServer`, hanging Save and Quit;
  `mixin/ChunkMapMixin` drops such callbacks. The client smoke test's quit watchdog dumps threads and chunk state
  if quitting hangs again.
- **Log patterns.** NeoForge stack frames read `at TRANSFORMER/runecraft@<ver>/com.runecraft...`; the smoke
  scripts match both forms.
- **Enchanting** in 1.21 is driven by item tags (`#minecraft:swords`, `enchantable/*`); gen_worldgen writes them.

## Status

- [x] Phase 1 freeze (CI green on 389aca80: server 3046/0, client 368/0)
- [x] Phase 2 Mojang mappings on Fabric 1.20.1 (CI green on bcd2a480, same self-test results)
- [x] Phase 3 build switch (ModDevGradle, NeoForge 21.1.220, Java 21)
- [x] Phase 4 loader API port (com.runecraft.platform facade, @Mod entry points, RegisterEvent registries)
- [x] Phase 5 1.21.1 API port (0 compile errors; review fixes merged)
- [x] Phase 6 resources (1.21 data pack layout and formats, item tags)
- [x] Phase 7 CI and smoke scripts (NeoForge installer server, runClientSmoke, classpath asset)
- [ ] Phase 8 green self-tests, Fabric retired
