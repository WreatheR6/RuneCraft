package com.runecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.runecraft.entity.BossEntity;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.network.Net;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.data.Boss;
import com.runecraft.osrs.data.Monster;
import com.runecraft.registry.ModBlocks;
import com.runecraft.registry.ModEntities;
import com.runecraft.platform.client.BlockRenderLayerMap;
import com.runecraft.platform.client.ClientTickEvents;
import com.runecraft.platform.client.ItemTooltipCallback;
import com.runecraft.platform.client.KeyBindingHelper;
import com.runecraft.platform.client.ClientPlayNetworking;
import com.runecraft.platform.client.EntityRendererRegistry;
import com.runecraft.platform.client.HudRenderCallback;
import com.runecraft.platform.client.WorldRenderEvents;
import com.runecraft.platform.client.ClientPlatformEvents;
import com.runecraft.RuneCraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod(value = RuneCraft.MOD_ID, dist = Dist.CLIENT)
public class RuneCraftClient {
    /** Opens the full-screen RuneScape interface (all tabs). */
    public static KeyMapping interfaceKey;
    /** Shows/hides the OSRS side panel at the bottom-right of the HUD. */
    public static KeyMapping sidePanelKey;
    /** Hold to free the cursor and use the side panel. */
    public static KeyMapping panelCursorKey;

    public RuneCraftClient(IEventBus modBus) {
        ClientPlatformEvents.init(modBus, this::onInitializeClient);
    }

    /** The client init, run once after registration (see {@link ClientPlatformEvents}). */
    public void onInitializeClient() {
        registerPackets();
        sidePanelKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.runecraft.side_panel", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_K, "key.categories.runecraft"));
        panelCursorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.runecraft.panel_cursor", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT, "key.categories.runecraft"));
        interfaceKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.runecraft.interface", InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J, "key.categories.runecraft"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (interfaceKey.consumeClick()) {
                client.setScreen(new RuneScapeScreen(SidePanel.fullTab(SidePanel.tab)));
            }
            while (sidePanelKey.consumeClick()) {
                SidePanel.visible = !SidePanel.visible;
            }
            while (panelCursorKey.consumeClick()) {
                if (client.screen == null && client.player != null) {
                    SidePanel.visible = true;
                    client.setScreen(new SidePanelScreen());
                }
            }
        });
        HudRenderCallback.EVENT.register(HudOverlay::render);
        WorldRenderEvents.AFTER_ENTITIES.register(HitsplatRenderer::render);
        ItemTooltipCallback.EVENT.register(Tooltips::append);

        com.runecraft.client.model.ModelSpecs.registerAll();
        com.runecraft.client.model.GielinorLooks.registerAll();
        CoinStacks.register();
        com.runecraft.client.bank.BankClient.init();
        for (Map.Entry<Monster, EntityType<OsrsMonsterEntity>> e : ModEntities.MONSTERS.entrySet()) {
            String id = e.getKey().entity();
            register(e.getValue(), id, null, ModEntities.scale(id));
        }
        for (Map.Entry<Boss, EntityType<BossEntity>> e : ModEntities.BOSS_TYPES.entrySet()) {
            String id = e.getKey().entityId();
            register(e.getValue(), id, null, e.getKey().scale());
        }
        for (com.runecraft.osrs.content.MonsterDef m : com.runecraft.osrs.content.ContentRegistry.monsters()) {
            register(ModEntities.MODULE_MONSTERS.get(m.id), m.id, m.model, m.scale);
        }
        for (com.runecraft.osrs.content.BossDef b : com.runecraft.osrs.content.ContentRegistry.bosses()) {
            register(ModEntities.DATA_BOSS_TYPES.get(b.id), b.id, b.model, b.scale);
        }
        EntityRendererRegistry.register(ModEntities.QUEST_NPC, QuestNpcRenderer::new);

        List<Block> cutout = new ArrayList<>(ModBlocks.FISHING_SPOTS.values());
        cutout.add(ModBlocks.FIRE);
        cutout.add(ModBlocks.BIRD_SNARE);
        cutout.add(ModBlocks.BOX_TRAP);
        cutout.add(ModBlocks.HERB_PATCH);
        cutout.add(ModBlocks.ALLOTMENT_PATCH);
        for (Block b : cutout) {
            BlockRenderLayerMap.INSTANCE.putBlock(b, RenderType.cutout());
        }
        for (com.runecraft.client.content.ClientModuleHooks hooks : com.runecraft.client.content.ClientModuleHooksRegistry.ALL) {
            hooks.initClient();
        }
        // Dev/CI only: automated in-game walkthrough, never active without -Drunecraft.clientSmoke=true.
        if (ClientSmokeTest.enabled()) {
            ClientSmokeTest.install();
        }
    }

    /**
     * Registers the renderer for a creature: its custom model if one exists (explicit {@code model}
     * id, or a model spec with the creature's own id), otherwise the textured biped.
     */
    public static <T extends net.minecraft.world.entity.Mob> void register(EntityType<T> type, String id, String model, float scale) {
        if (type == null) {
            return;
        }
        java.util.Map<String, com.runecraft.osrs.model.ModelSpec> specs = com.runecraft.osrs.content.ContentRegistry.models();
        com.runecraft.osrs.model.ModelSpec spec = specs.get(model != null ? model : id);
        if (spec != null) {
            EntityRendererRegistry.register(type, ctx -> new com.runecraft.client.model.GenericEntityRenderer<>(ctx, spec, scale));
        } else {
            EntityRendererRegistry.register(type, ctx -> new OsrsBipedRenderer<>(ctx, id, scale));
        }
    }

    private static void registerPackets() {
        ClientPlayNetworking.registerGlobalReceiver(Net.COMBAT_STATE, (client, handler, buf, sender) -> {
            boolean retaliate = buf.readBoolean();
            boolean multi = buf.readBoolean();
            client.execute(() -> {
                ClientState.autoRetaliate = retaliate;
                ClientState.inMulti = multi;
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.SYNC, (client, handler, buf, sender) -> {
            int n = buf.readVarInt();
            int[] xp = new int[n];
            int[] boosts = new int[n];
            for (int i = 0; i < n; i++) {
                xp[i] = buf.readVarInt();
                boosts[i] = buf.readVarInt();
            }
            int prayer = buf.readVarInt();
            int mask = buf.readInt();
            int style = buf.readVarInt();
            int autocast = buf.readVarInt();
            long coins = buf.readVarLong();
            String task = buf.readUtf();
            int remaining = buf.readVarInt();
            int questCount = buf.readVarInt();
            int[] stages = new int[questCount];
            for (int i = 0; i < questCount; i++) {
                stages[i] = buf.readVarInt();
            }
            int qp = buf.readVarInt();
            int logCount = buf.readVarInt();
            List<String> log = new ArrayList<>();
            for (int i = 0; i < logCount; i++) {
                log.add(buf.readUtf());
            }
            java.util.Map<String, Integer> kills = new java.util.HashMap<>();
            int killCount = buf.isReadable() ? buf.readVarInt() : 0;
            for (int i = 0; i < killCount; i++) {
                String key = buf.readUtf();
                kills.put(key, buf.readVarInt());
            }
            long tokkul = buf.isReadable() ? buf.readVarLong() : 0;
            client.execute(() -> {
                ClientState.tokkul = tokkul;
                ClientState.SKILLS.load(xp, boosts);
                ClientState.prayerPoints = prayer;
                ClientState.prayerMask = mask;
                ClientState.style = CombatStyle.values()[Math.max(0, Math.min(style, CombatStyle.values().length - 1))];
                ClientState.autocast = autocast < 0 || autocast >= Spell.values().length ? null : Spell.values()[autocast];
                ClientState.coins = coins;
                ClientState.slayerTask = task;
                ClientState.slayerRemaining = remaining;
                System.arraycopy(stages, 0, ClientState.QUEST_STAGES, 0, Math.min(stages.length, ClientState.QUEST_STAGES.length));
                ClientState.questPoints = qp;
                ClientState.COLLECTION_LOG.clear();
                ClientState.COLLECTION_LOG.addAll(log);
                ClientState.KILL_COUNTS.clear();
                ClientState.KILL_COUNTS.putAll(kills);
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.XP_DROP, (client, handler, buf, sender) -> {
            int skill = buf.readVarInt();
            int tenths = buf.readVarInt();
            client.execute(() -> ClientState.XP_DROPS.add(new ClientState.XpDrop(Skill.byIndex(skill), tenths, System.currentTimeMillis())));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.HITSPLAT, (client, handler, buf, sender) -> {
            int entity = buf.readVarInt();
            int damage = buf.readVarInt();
            byte type = buf.readByte();
            float ratio = buf.readFloat();
            client.execute(() -> ClientState.HITSPLATS.computeIfAbsent(entity, k -> new ArrayList<>())
                    .add(new ClientState.Hitsplat(damage, type, ratio, System.currentTimeMillis())));
        });
        ClientPlayNetworking.registerGlobalReceiver(Net.OPEN_MENU, (client, handler, buf, sender) -> {
            int type = buf.readVarInt();
            int count = buf.readVarInt();
            List<ClientState.MenuEntry> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                entries.add(new ClientState.MenuEntry(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(),
                        buf.readVarInt(), buf.readBoolean()));
            }
            client.execute(() -> {
                ClientState.menuType = MenuType.values()[type];
                ClientState.MENU.clear();
                ClientState.MENU.addAll(entries);
                client.setScreen(new MakeXScreen());
            });
        });
    }
}
