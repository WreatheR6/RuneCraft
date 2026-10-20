package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.bosses.BossMechanics;
import com.runecraft.osrs.content.bosses.BossMechanics.YamaContract;
import com.runecraft.osrs.content.bosses.YamaLoot;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Yama for the self-test ({@code boss:walk:yama}): phase 1's glyph special blocked on the matching glyph and its void
 * flares destroyed before they explode; the exile at 66.6% to the Judge of Yama (which prays against melee at first) and
 * back; phase 2's Meteor Strike outrun or Shadow Stomp blocked; the second exile at 33.3%; phase 3 with the glyphs gone and
 * half-health flares; the kill shared with a duo partner who rolls their own loot on their contribution. Then a second
 * challenge under the Contract of shard acquisition: exiles at 75% and 50%, no standard table, and the 45 shards for the
 * player who showed the contract.
 */
public final class YamaWalk {
    private YamaWalk() {
    }

    private static IllegalStateException fail(String message) {
        return new IllegalStateException(message);
    }

    public static String walk(DataBossEntity boss, BossWalks.Driver d) {
        if (!(boss.script() instanceof ModernScripts.Yama y)) {
            throw fail("Yama has no Yama script");
        }
        ServerPlayer p = d.player();
        ServerLevel world = (ServerLevel) boss.level();
        List<String> log = new ArrayList<>();
        ServerPlayer partner = com.runecraft.platform.FakePlayer.get(world);
        DataBossEntity second = null;
        try {
            Vec3 c = y.homeVec();
            d.moveTo(c.add(0, 0, 8), boss);
            d.ticks(boss, 2);
            log.add(special(boss, y, d, 1));
            log.add(flares(boss, y, d));
            log.add(exile(boss, y, d, 200 / 3.0 - 1, 2));
            log.add(special(boss, y, d, 2));
            log.add(exile(boss, y, d, 100 / 3.0 - 1, 3));
            if (!y.walkGlyphs(true).isEmpty() || !y.walkGlyphs(false).isEmpty()) {
                throw fail("phase 3: the glyphs still burn");
            }
            y.walkSpawnFlares(boss, 1);
            List<Mob> half = y.walkFlareMobs();
            if (half.isEmpty() || half.get(0).getHealth() > half.get(0).getMaxHealth() / 2 + 0.5f) {
                throw fail("phase 3: void flares don't spawn at half health");
            }
            for (Mob f : half) {
                CombatHandler.applyDamage(p, f, 500);
            }
            log.add("phase 3: glyphs gone, half-health flares");
            // the duo partner dealt 40% (1,000 of 2,500): they roll their own loot on that share
            partner.teleportTo(world, c.x + 3, c.y, c.z + 6, 0, 0);
            int partnerKc = Players.data(partner).killCount("yama");
            int partnerItems = count(partner);
            y.walkCredit(partner, 1000);
            if (Math.abs(y.contribution(partner) - 0.4) > 1e-9) {
                throw fail("a 1,000-damage partner has contribution " + y.contribution(partner));
            }
            DataBossService.LootRoll before = DataBossService.lastRoll();
            BossWalks.finish(boss, d, AttackType.SLASH, 600, 60);
            DataBossService.LootRoll roll = DataBossService.lastRoll();
            if (roll == null || roll == before || roll.items() == 0) {
                throw fail("the kill dropped nothing for the killer");
            }
            if (Players.data(partner).killCount("yama") != partnerKc + 1 || count(partner) <= partnerItems) {
                throw fail("the duo partner got no kill count or loot (kc " + Players.data(partner).killCount("yama")
                        + ", items " + count(partner) + ")");
            }
            log.add("killed, " + roll.items() + " drops for the killer, the partner's own roll on 40%");

            // a second challenge under the Contract of shard acquisition
            ModernScripts.Yama.walkOffer(p, YamaContract.SHARD_ACQUISITION);
            BossDef def = boss.def();
            second = DataBossService.spawn(world, boss.blockPosition(), def, p);
            if (second == null || !(second.script() instanceof ModernScripts.Yama y2)) {
                throw fail("the contract challenge didn't spawn Yama");
            }
            d.ticks(second, 2);
            if (y2.walkContract() != YamaContract.SHARD_ACQUISITION) {
                throw fail("Yama ignored the contract (" + y2.walkContract() + ")");
            }
            BossWalks.setPercent(second, 76);
            d.ticks(second, 2);
            if (y2.walkThresholdsPassed() != 0) {
                throw fail("contract: an exile at 76%");
            }
            log.add(exile(second, y2, d, 74, 2));
            log.add(exile(second, y2, d, 49, 3));
            int shards = Inv.count(p, "oathplate_shards");
            DataBossService.LootRoll before2 = DataBossService.lastRoll();
            BossWalks.finish(second, d, AttackType.SLASH, 600, 60);
            DataBossService.LootRoll roll2 = DataBossService.lastRoll();
            if (roll2 == before2) {
                throw fail("the contract kill rolled no loot");
            }
            for (ItemEntity e : roll2.dropped()) {
                String id = Inv.idOf(e.getItem());
                if (YamaLoot.MAIN.stream().anyMatch(l -> l.item().equals(id)) || id.equals("pineapple_pizza") || id.equals("wild_pie")) {
                    throw fail("a contract kill rolled the standard table (" + id + ")");
                }
            }
            if (Inv.count(p, "oathplate_shards") < shards + YamaContract.SHARD_ACQUISITION.rewardQuantity()) {
                throw fail("the contract's 45 oathplate shards didn't come (" + (Inv.count(p, "oathplate_shards") - shards) + ")");
            }
            log.add("shard contract: exiles at 75%/50%, no standard table, 45 shards");
            return String.join("; ", log);
        } finally {
            if (second != null && second.isAlive()) {
                second.discard();
            }
            partner.getInventory().clearContent();
        }
    }

    private static int count(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            n += p.getInventory().getItem(i).getCount();
        }
        return n;
    }

    /**
     * A special answered: phase 1's glyph special blocked on the matching glyph; phase 2's Meteor Strike outrun (four tiles
     * from its centre) or Shadow Stomp blocked on a Glyph of Shadow.
     */
    private static String special(DataBossEntity boss, ModernScripts.Yama y, BossWalks.Driver d, int phase) {
        ServerPlayer p = d.player();
        String name = null;
        for (int i = 0; i < 4 && name == null; i++) {
            y.walkStartSpecial(boss, p); // every second shadow special brings void flares instead
            name = y.walkPendingName();
            for (Mob f : y.walkFlareMobs()) {
                CombatHandler.applyDamage(p, f, 500);
            }
        }
        if (name == null) {
            throw fail("phase " + phase + ": no special after four tries");
        }
        boolean fire = y.walkPendingFire();
        d.clearDamage();
        String attack;
        if (name.equals("METEOR")) {
            // run five tiles from the meteor's centre, towards the middle of the arena
            Vec3 at = y.walkPendingAt();
            Vec3 c = y.homeVec();
            Vec3 in = c.subtract(at);
            double len = Math.hypot(in.x, in.z);
            Vec3 dir = len < 0.5 ? new Vec3(1, 0, 0) : new Vec3(in.x / len, 0, in.z / len);
            Vec3 spot = at.add(dir.scale(10));
            d.moveTo(BossFx.ground(BossFx.world(boss), spot.x, spot.z, at.y), boss);
            attack = "meteor_strike";
        } else {
            List<Vec3> glyphs = y.walkGlyphs(fire);
            if (glyphs.isEmpty()) {
                throw fail("phase " + phase + ": no lit glyph of " + (fire ? "fire" : "shadow") + " for the " + name);
            }
            d.moveTo(glyphs.get(0), boss);
            attack = switch (name) {
                case "GLYPH_FIRE" -> "glyph_flames";
                case "GLYPH_SHADOW" -> "shadow_engulf";
                default -> "shadow_stomp";
            };
        }
        if (BossWalks.until(boss, d, 10, () -> y.walkPendingName() == null) < 0) {
            throw fail("phase " + phase + ": the " + name + " never landed");
        }
        if (d.damage(attack) > 0) {
            throw fail("phase " + phase + ": the " + name + " hit the player for " + d.damage(attack));
        }
        d.moveTo(y.homeVec().add(0, 0, 8), boss);
        return "phase " + phase + ": " + name.toLowerCase(java.util.Locale.ROOT) + " answered";
    }

    /** Void flares destroyed before they charge up: no explosion. */
    private static String flares(DataBossEntity boss, ModernScripts.Yama y, BossWalks.Driver d) {
        ServerPlayer p = d.player();
        y.walkSpawnFlares(boss, 2);
        List<Mob> flares = y.walkFlareMobs();
        if (flares.isEmpty()) {
            throw fail("no void flares spawned");
        }
        d.clearDamage();
        // destroy every flare before it charges up (his own specials may bring more meanwhile)
        for (int t = 0; t < BossMechanics.VOID_FLARE_TICKS + 2; t++) {
            for (Mob f : y.walkFlareMobs()) {
                CombatHandler.applyDamage(p, f, 500);
            }
            d.ticks(boss, 1);
        }
        if (d.damage("void_flare") > 0) {
            throw fail("destroyed void flares still exploded for " + d.damage("void_flare"));
        }
        return flares.size() + " void flares destroyed";
    }

    /** A threshold passed: the exile, the Judge (melee blocked at first) slain, back for the next phase. */
    private static String exile(DataBossEntity boss, ModernScripts.Yama y, BossWalks.Driver d, double percent, int nextStage) {
        ServerPlayer p = d.player();
        BossWalks.setPercent(boss, percent);
        if (BossWalks.until(boss, d, 12, y::walkExiled) < 0) {
            throw fail("no exile at " + Math.round(percent) + "%");
        }
        Mob judge = y.walkJudge();
        if (judge == null || !judge.isAlive()) {
            throw fail("no Judge of Yama in the exile");
        }
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
        float hp = judge.getHealth();
        BossWalks.attack(p, judge);
        if (judge.getHealth() < hp) {
            throw fail("the Judge's prayers let a melee hit through at the start");
        }
        CombatHandler.applyDamage(p, judge, BossMechanics.JUDGE_HITPOINTS + 10);
        if (BossWalks.until(boss, d, 6, () -> !y.walkExiled()) < 0) {
            throw fail("the exile didn't end with the Judge slain");
        }
        if (y.walkStage() != nextStage) {
            throw fail("back from the exile in phase " + y.walkStage() + " (expected " + nextStage + ")");
        }
        return "exiled at " + Math.round(percent) + "%, Judge slain, phase " + nextStage;
    }
}
