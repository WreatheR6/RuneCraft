package com.runecraft.content.godwars;

import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossService;
import com.runecraft.content.GameEvents;
import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;
import com.runecraft.osrs.content.godwars.GwdBosses;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;

/**
 * Minecraft-side behaviour for the godwars module: boss scripts (Kree'arra, Zilyana, Nex and her
 * generals, the Wilderness bosses), God Wars kill count (follower kills, chamber entry for all four
 * generals, ecumenical keys), the Ancient Prison's frozen door, follower camps and the built-in
 * generals' hilts, shards and pets.
 */
public final class GodWarsWildernessHooks implements ModuleHooks {
    @Override
    public void init() {
        // God Wars Dungeon.
        BossScripts.register("kreearra", KreeArraScript::new);
        BossScripts.register("commander_zilyana", ZilyanaScript::new);
        BossScripts.register("nex", NexScript::new);
        for (String minion : GwdBosses.NEX_MINIONS) {
            BossScripts.register(minion, NexMinionScript::new);
            DataBossService.companion(minion); // part of Nex's kill: no kill count of their own
        }
        // Wilderness.
        BossScripts.register("callisto", () -> new WildernessScripts.Bear(false));
        BossScripts.register("artio", () -> new WildernessScripts.Bear(true));
        BossScripts.register("venenatis", () -> new WildernessScripts.Spider(false));
        BossScripts.register("spindel", () -> new WildernessScripts.Spider(true));
        BossScripts.register("vetion", () -> new VetionScript(false));
        BossScripts.register("calvarion", () -> new VetionScript(true));
        BossScripts.register("scorpia", WildernessScripts.Scorpia::new);
        BossScripts.register("chaos_elemental", WildernessScripts.ChaosElemental::new);
        BossScripts.register("chaos_fanatic", WildernessScripts.ChaosFanatic::new);
        BossScripts.register("crazy_archaeologist", WildernessScripts.CrazyArchaeologist::new);
        BossScripts.register("king_black_dragon", WildernessScripts.KingBlackDragon::new);

        // The God Wars Dungeon at its real tiles: obstacles, chambers, altars, spawns, essence. The generals stand in their
        // chambers, so summoning stones left by older worlds only point the way.
        for (String general : new String[]{"kreearra", "commander_zilyana", "nex"}) {
            DataBossService.requirement(general, (player, def) -> {
                GwdKillcount.redirect(player, def.name);
                return false;
            });
        }
        GwdKillcount.registerStoneRedirect();
        GameEvents.onKill(GwdKillcount::onKill);
        // Damaging a blood reaver heals the attacker half the damage dealt (OSRS Wiki, Blood Reaver).
        com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
            @Override
            public void afterHit(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.entity.LivingEntity target,
                                 com.runecraft.osrs.AttackType type, int dealt) {
                int heal = com.runecraft.osrs.content.godwars.GodWarsFactions.reaverLeech(
                        GwdKillcount.creatureId(target, com.runecraft.combat.MonsterStats.of(target)), dealt);
                if (heal > 0 && player.isAlive()) {
                    player.heal(heal);
                }
            }
        });
        // A gorak's touch drains a random stat (not Hitpoints) by 1-4 on every hit that lands (OSRS Wiki, Gorak).
        com.runecraft.combat.CombatHooks.addHurtListener((player, attacker, damage) -> {
            if (damage <= 0 || attacker == null || !com.runecraft.osrs.content.godwars.GodWarsFactions.gorak(
                    GwdKillcount.creatureId(attacker, com.runecraft.combat.MonsterStats.of(attacker)))) {
                return;
            }
            var drain = com.runecraft.osrs.content.godwars.GodWarsFactions.goraksTouch(player.getRandom().nextInt(com.runecraft.osrs.Skill.count() - 1),
                    player.getRandom().nextInt(4));
            var skills = com.runecraft.player.Players.data(player).skills();
            skills.setBoost(drain.skill(), Math.max(-skills.level(drain.skill()), skills.boost(drain.skill()) - drain.amount()));
            com.runecraft.network.ServerNetworking.sync(player);
        });
        GameEvents.onKill((player, entity, stats) -> GwdSpawner.onKill(entity));
        GwdDungeonHooks.register();
        GwdSpawner.register();
        // The Revenant Caves: revenant drops (wiki template), their fighting behaviour, the caves themselves, the maledictus.
        GameEvents.onKill(RevenantLoot::onKill);
        GameEvents.onKill(MaledictusService::onKill);
        RevenantLoot.register();
        RevenantBehaviour.register();
        RevenantCavesService.register();
        FeroxService.register();
        BossScripts.register(com.runecraft.osrs.content.godwars.RevenantRewards.MALEDICTUS, MaledictusService.Script::new);
        GwdCamps.register();
    }
}
