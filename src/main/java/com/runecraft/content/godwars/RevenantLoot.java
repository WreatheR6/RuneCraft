package com.runecraft.content.godwars;

import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.SkullService;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.osrs.content.godwars.RevenantRewards;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * A revenant's drop ({@link RevenantRewards}, the wiki template): coins or a main table and a blighted supplies roll, the
 * rare chain (the unique table, seeds, artefacts, the ancient crystal: better skulled, the unique table five times better
 * on a revenant Slayer task) and the dragon med helm. The ether every kill drops is the creature's own table, so a charged
 * bracelet of ethereum can absorb it. An amulet of avarice notes the drops ({@link #register}).
 */
final class RevenantLoot {
    private RevenantLoot() {
    }

    /** The amulet of avarice notes revenant drops (wiki: "all of the revenants' drops to be noted if applicable"). */
    static void register() {
        CombatHooks.addDropTransformer((killer, mob, item, count) -> {
            if (killer == null || !RevenantRewards.isRevenant(creatureId(mob)) || !wearingAvarice(killer)
                    || "revenant_ether".equals(item) || !com.runecraft.bank.Notes.canNote(item, "")) {
                return null;
            }
            return com.runecraft.bank.Notes.note(item, "", count);
        });
    }

    static String creatureId(LivingEntity mob) {
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
    }

    /** Whether the player wears an amulet of avarice (a jewellery slot, or the vanilla chest slot where amulets go). */
    static boolean wearingAvarice(ServerPlayer player) {
        return "amulet_of_avarice".equals(com.runecraft.content.systems.SysState.of(player)
                .wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.NECK));
    }

    static void onKill(ServerPlayer player, LivingEntity entity, MonsterStats stats) {
        if (stats == null || stats.profile() == null) {
            return;
        }
        RevenantRewards.Revenant rev = RevenantRewards.revenant(stats.profile().id);
        if (rev == null) {
            return;
        }
        boolean skulled = SkullService.isSkulled(player);
        boolean onTask = SlayerService.isOnTask(player, entity);
        java.util.function.IntUnaryOperator rnd = entity.getRandom()::nextInt;
        for (RevenantRewards.Rolled r : RevenantRewards.rollCommon(rev, rnd)) {
            if (r.item().equals("revenant_ether")) {
                continue; // the creature's own table dropped it already
            }
            drop(player, entity, r.item(), r.count());
        }
        for (RevenantRewards.Rolled r : RevenantRewards.rollRares(rev.level(), skulled, onTask, rnd)) {
            drop(player, entity, r.item(), r.count());
            Msg.game(player, "The revenant drops something valuable!");
        }
    }

    static void drop(ServerPlayer player, LivingEntity entity, String item, int count) {
        if (count <= 0 || com.runecraft.osrs.ItemCatalog.get(item) == null) {
            return;
        }
        Inv.spawnAtLocation(entity, CombatHooks.dropStack(player, entity, item, count));
        CollectionLog.obtain(player, item);
    }
}
