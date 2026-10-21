package com.runecraft.content.godwars;

import com.runecraft.block.BossSummonBlock;
import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.BossEntity;
import com.runecraft.osrs.content.godwars.GodItems;
import com.runecraft.osrs.content.godwars.GodWarsFactions;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.content.godwars.GwdBosses;
import com.runecraft.osrs.data.Boss;
import com.runecraft.quest.QuestApi;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

/**
 * God Wars kill count ("essence"): killing a god's followers, its general's bodyguards or the general raises that god's
 * count; a general's door costs {@link #required} of it (an ecumenical key works instead). The Ancient Prison's Zarosians
 * give Zarosian essence (more for Zarosian gear worn), spent at the door to the refuge before Nex. Leaving the dungeon
 * clears it all (OSRS). Also rolls the built-in General Graardor's and K'ril Tsutsaroth's hilts, godsword shards and
 * pets, the frozen key pieces of The Frozen Door from the bodyguards, and the ecumenical keys of the Wilderness God Wars
 * Dungeon.
 */
public final class GwdKillcount {
    /** Kill count needed to open a general's chamber (the systems module lowers it with Combat Achievement tiers). */
    public static java.util.function.ToIntFunction<ServerPlayer> required = p -> GodWarsFactions.REQUIRED_KILLCOUNT;
    /**
     * Ecumenical key chance (1 in n) for a Wilderness God Wars Dungeon kill: 1/60 (OSRS Wiki); the systems module lowers it
     * to 1/55..1/40 with the hard..grandmaster Combat Achievement tiers.
     */
    public static java.util.function.ToIntFunction<ServerPlayer> ecumenicalChance = p -> 60;
    /** Most ecumenical keys a player may hold (3; 4 and 5 with the medium and hard Wilderness diaries in OSRS). // approx */
    public static final int ECUMENICAL_KEY_CAP = 3;
    /** The Wilderness God Wars Dungeon (dungeons module, carved under its entrance at 3017, 3738): its OSRS tile box. */
    private static final int[] WILDERNESS_GWD = {3010, 3700, 3056, 3765};
    /** A bodyguard's frozen key piece during The Frozen Door (OSRS Wiki: 1/20). */
    private static final int KEY_PIECE_RATE = 20;

    private GwdKillcount() {
    }

    /** A player's essence for a god. */
    public static int get(ServerPlayer player, God god) {
        return GwdKillcountState.get(player.getServer()).get(player.getUUID(), god);
    }

    /** A player's Zarosian essence. */
    public static int zaros(ServerPlayer player) {
        return GwdKillcountState.get(player.getServer()).zaros(player.getUUID());
    }

    /** Sets a player's essence for a god (the self-test, commands). */
    public static void set(ServerPlayer player, God god, int value) {
        GwdKillcountState.get(player.getServer()).set(player.getUUID(), god, value);
    }

    public static void setZaros(ServerPlayer player, int value) {
        GwdKillcountState.get(player.getServer()).setZaros(player.getUUID(), value);
    }

    /** Checks and consumes the kill count (or an ecumenical key) needed to enter a general's chamber. */
    static boolean enter(ServerPlayer player, God god, String general) {
        GwdKillcountState state = GwdKillcountState.get(player.getServer());
        int kc = state.get(player.getUUID(), god);
        int need = required.applyAsInt(player);
        if (kc >= need) {
            state.set(player.getUUID(), god, kc - need);
            Msg.game(player, "The door to " + general + "'s chamber opens. (" + need + " " + god.displayName() + " kill count used.)");
            return true;
        }
        if (useKey(player)) {
            Msg.game(player, "You use your ecumenical key to open the door to " + general + "'s chamber.");
            return true;
        }
        Msg.red(player, "You need " + need + " " + god.displayName() + " kill count to enter " + general + "'s chamber. You have "
                + kc + ".");
        return false;
    }

    /** Checks and consumes the Zarosian essence (or an ecumenical key) needed to enter the refuge before Nex's chamber. */
    static boolean enterRefuge(ServerPlayer player) {
        GwdKillcountState state = GwdKillcountState.get(player.getServer());
        int kc = state.zaros(player.getUUID());
        int need = Math.min(GodWarsFactions.REQUIRED_ZAROS_ESSENCE, required.applyAsInt(player));
        if (kc >= need) {
            state.setZaros(player.getUUID(), kc - need);
            Msg.game(player, "The door opens. (" + need + " Zarosian essence used.)");
            return true;
        }
        if (useKey(player)) {
            Msg.game(player, "You use your ecumenical key to open the door.");
            return true;
        }
        Msg.red(player, "You need " + need + " Zarosian essence to pass this door. You have " + kc + ".");
        return false;
    }

    private static boolean useKey(ServerPlayer player) {
        if (Inv.has(player, "ecumenical_key", 1)) {
            Inv.remove(player, "ecumenical_key", 1);
            return true;
        }
        return false;
    }

    /** Clears every essence counter of a player (left the dungeon). */
    static void clear(ServerPlayer player) {
        GwdKillcountState state = GwdKillcountState.get(player.getServer());
        if (state.anyEssence(player.getUUID())) {
            state.clearEssence(player.getUUID());
            Msg.game(player, "The essence you gathered in the God Wars Dungeon fades away.");
        }
    }

    static boolean anyEssence(ServerPlayer player) {
        return GwdKillcountState.get(player.getServer()).anyEssence(player.getUUID());
    }

    /** The module creature or boss id of a killed entity, or null. */
    static String creatureId(LivingEntity entity, MonsterStats stats) {
        if (entity instanceof BossEntity be && be.boss() != null) {
            return be.boss().entityId();
        }
        if (entity instanceof com.runecraft.boss.DataBossEntity d && d.def() != null) {
            return d.def().id;
        }
        if (stats == null) {
            return null;
        }
        if (stats.profile() != null) {
            return stats.profile().id;
        }
        if (stats.boss() != null) {
            return stats.boss().entityId();
        }
        return stats.monster() != null ? stats.monster().entity() : null;
    }

    /** Kill listener: essence, the built-in generals' extra loot, frozen key pieces and ecumenical keys. */
    static void onKill(ServerPlayer player, LivingEntity entity, MonsterStats stats) {
        String id = creatureId(entity, stats);
        GwdKillcountState state = GwdKillcountState.get(player.getServer());
        // essence only counts in the dungeon itself (not the Wilderness God Wars Dungeon or a stray follower elsewhere)
        boolean inside = com.runecraft.osrs.content.godwars.GwdDungeon.contains(entity.getBlockX(), entity.getBlockY(), entity.getBlockZ())
                || com.runecraft.osrs.content.godwars.GwdDungeon.contains(player.getBlockX(), player.getBlockY(), player.getBlockZ());
        God god = inside ? GodWarsFactions.essenceGod(id) : null;
        if (god != null) {
            int kc = GodWarsFactions.addKill(state.get(player.getUUID(), god), 1);
            state.set(player.getUUID(), god, kc);
            int need = required.applyAsInt(player);
            Msg.action(player, god.displayName() + " kill count: " + (kc >= GodWarsFactions.MAX_KILLCOUNT ? "MAX" : kc)
                    + (kc < need ? " / " + need : ""));
        }
        int zaros = inside ? GodWarsFactions.zarosEssence(id) : 0;
        if (zaros > 0) {
            int items = GodItems.count("zaros", GwdDungeonHooks.wornIds(player));
            int gained = zaros + GodWarsFactions.zarosBonus(items, entity.getRandom().nextDouble());
            int kc = GodWarsFactions.addKill(state.zaros(player.getUUID()), gained);
            state.setZaros(player.getUUID(), kc);
            Msg.action(player, "Zarosian essence: " + kc + " (+" + gained + ")");
        }
        God guard = GodWarsFactions.bodyguardGod(id);
        if (guard != null && QuestApi.stage(player, "the_frozen_door") > 0 && !QuestApi.isComplete(player, "the_frozen_door")
                && !Inv.has(player, guard.keyPiece(), 1) && entity.getRandom().nextInt(KEY_PIECE_RATE) == 0) {
            com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(guard.keyPiece(), 1), player);
            Msg.game(player, "The bodyguard drops a piece of a frozen key!");
        }
        if (inWildernessGwd(entity) && entity.getRandom().nextInt(Math.max(1, ecumenicalChance.applyAsInt(player))) == 0) {
            if (Inv.count(player, "ecumenical_key") >= ECUMENICAL_KEY_CAP) {
                Msg.game(player, "You already have enough of these.");
            } else {
                com.runecraft.combat.LootDrops.spawn(entity, Inv.stack("ecumenical_key", 1), player);
            }
        }
        if (entity instanceof BossEntity be && be.boss() != null) {
            String bossId = be.boss().entityId();
            for (Drops.Rolled r : Drops.roll(GwdBosses.builtinExtraDrops(bossId), entity.getRandom(), false)) {
                com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(r.item(), r.count()), player);
                CollectionLog.obtain(player, r.item());
            }
            String pet = GwdBosses.builtinPet(bossId);
            if (pet != null && entity.getRandom().nextInt(GwdBosses.BUILTIN_PET_RATE) == 0) {
                GwdScript.givePet(player, pet);
            }
        }
    }

    /** True for a kill inside the Wilderness God Wars Dungeon (every monster there can drop an ecumenical key). */
    static boolean inWildernessGwd(LivingEntity entity) {
        if (com.runecraft.world.Gielinor.wildernessLevel(entity) <= 0 || entity.getY() > com.runecraft.osrs.MultiCombat.UNDERGROUND_TOP) {
            return false;
        }
        int x = com.runecraft.osrs.GielinorMap.toOsrsX(entity.getBlockX());
        int y = com.runecraft.osrs.GielinorMap.toOsrsY(entity.getBlockZ());
        return x >= WILDERNESS_GWD[0] && x <= WILDERNESS_GWD[2] && y >= WILDERNESS_GWD[1] && y <= WILDERNESS_GWD[3];
    }

    /**
     * Summoning stones of the God Wars generals left in older worlds (the built-in Graardor's and K'ril's by the dungeon
     * entrance, the old surface arenas): the generals wait in their chambers inside the dungeon now.
     */
    static void registerStoneRedirect() {
        UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer sp)) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            if (!(world.getBlockState(pos).getBlock() instanceof BossSummonBlock stone)) {
                return InteractionResult.PASS;
            }
            Boss boss = stone.boss();
            if (GodWarsFactions.generalGod(boss.entityId()) == null) {
                return InteractionResult.PASS;
            }
            redirect(sp, boss.displayName());
            return InteractionResult.FAIL;
        });
    }

    static void redirect(ServerPlayer player, String general) {
        Msg.game(player, general + " waits in " + (general.startsWith("Commander") ? "her" : "his")
                + " chamber deep in the God Wars Dungeon. Climb down the hole north of Trollheim.");
    }
}
