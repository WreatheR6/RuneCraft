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

## Status

- [ ] Phase 1 freeze
- [ ] Phase 2 Mojang mappings on Fabric 1.20.1 (CI green)
- [ ] Phase 3 build switch
- [ ] Phase 4 loader API port
- [ ] Phase 5 1.21.1 API port
- [ ] Phase 6 resources
- [ ] Phase 7 CI and smoke scripts
- [ ] Phase 8 green self-tests, Fabric retired
