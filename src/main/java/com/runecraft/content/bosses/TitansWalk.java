package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.CombatHandler;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.bosses.RoyalTitansLoot;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;

/**
 * The Royal Titans as a duo for the self-test ({@code boss:walk:branda_the_fire_queen:duo}): the player's hits on the Titans
 * count as their contribution (through the real combat path), a partner deals half the Titans' hitpoints, both Titans
 * fall, and each fighter makes their own choice: the player loots Eldric (two rolls on his table, or a unique, quantities
 * scaled to their small share), the partner takes pages (10-19 at 50%: 5-9) and gets their own kill count.
 */
public final class TitansWalk {
    private TitansWalk() {
    }

    private static IllegalStateException fail(String message) {
        return new IllegalStateException(message);
    }

    public static String duo(DataBossEntity branda, BossWalks.Driver d) {
        if (!(branda.script() instanceof EncounterScripts.RoyalTitans t)) {
            throw fail("Branda has no RoyalTitans script");
        }
        ServerPlayer p = d.player();
        ServerLevel world = (ServerLevel) branda.level();
        ServerPlayer partner = com.runecraft.platform.FakePlayer.get(world);
        try {
            d.moveTo(t.homeVec().add(0, 0, 3), branda);
            d.ticks(branda, 1);
            DataBossEntity eldric = t.walkEldric();
            if (eldric == null || !eldric.isAlive()) {
                throw fail("Eldric didn't rise beside Branda");
            }
            // the player's own hits count, through the combat path
            p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("dragon_scimitar", 1));
            for (int i = 0; i < 40 && t.damageBy(p) <= 0; i++) {
                BossWalks.attack(p, i % 2 == 0 ? branda : eldric);
                d.ticks(branda, 1);
            }
            int mine = t.damageBy(p);
            if (mine <= 0) {
                throw fail("40 melee hits on the Titans credited no contribution");
            }
            // the partner dealt half the Titans' hitpoints
            Vec3 c = t.homeVec();
            partner.teleportTo(world, c.x - 3, c.y, c.z + 3, 0, 0);
            t.credit(partner, RoyalTitansLoot.TOTAL_HITPOINTS / 2);
            int partnerKc = Players.data(partner).killCount(RoyalTitansLoot.BRANDA);
            // Branda subdued while Eldric stands, then both fall
            BossWalks.hit(branda, p, 5000, AttackType.SLASH);
            CombatHandler.applyDamage(p, eldric, 5000);
            if (BossWalks.until(branda, d, 6, () -> !branda.isAlive()) < 0) {
                throw fail("Branda didn't fall with Eldric");
            }
            if (TitansLoot.pendingDamage(p) != mine) {
                throw fail("the player's choice holds " + TitansLoot.pendingDamage(p) + " damage, they dealt " + mine);
            }
            if (TitansLoot.pendingDamage(partner) != RoyalTitansLoot.TOTAL_HITPOINTS / 2) {
                throw fail("the partner has no choice of their own (" + TitansLoot.pendingDamage(partner) + ")");
            }
            // the player loots Eldric on their own share
            if (!TitansLoot.choose(p, RoyalTitansLoot.Choice.LOOT, "eldric")) {
                throw fail("the player couldn't loot Eldric");
            }
            List<RoyalTitansLoot.Loot> loot = TitansLoot.lastLoot(p);
            int pct = RoyalTitansLoot.percent(mine);
            int rolls = 0;
            for (RoyalTitansLoot.Loot l : loot) {
                if (l.item().startsWith("clue_scroll")) {
                    continue;
                }
                boolean pre = RoyalTitansLoot.ELDRIC_PRE.stream().anyMatch(u -> u.item().equals(l.item()));
                RoyalTitansLoot.Line line = RoyalTitansLoot.ELDRIC_MAIN.stream().filter(m -> m.item().equals(l.item())).findFirst().orElse(null);
                if (!pre && line == null) {
                    throw fail("looting Eldric gave " + l.item() + ", not on his table");
                }
                if (line != null && l.count() > RoyalTitansLoot.scaled(line.max(), pct)) {
                    throw fail(l.count() + " x " + l.item() + " at " + pct + "% contribution (at most "
                            + RoyalTitansLoot.scaled(line.max(), pct) + ")");
                }
                rolls++;
            }
            if (rolls != 1 && rolls != 2) {
                throw fail("looting Eldric gave " + rolls + " items (a unique or two rolls)");
            }
            // the partner takes pages on 50%
            if (!TitansLoot.choose(partner, RoyalTitansLoot.Choice.PAGES, "branda")) {
                throw fail("the partner couldn't take pages");
            }
            int pages = TitansLoot.lastLoot(partner).stream().filter(l -> l.item().equals(RoyalTitansLoot.PAGE))
                    .mapToInt(RoyalTitansLoot.Loot::count).sum();
            if (pages < 5 || pages > 9) {
                throw fail("the partner took " + pages + " pages at 50% (5-9)");
            }
            if (Players.data(partner).killCount(RoyalTitansLoot.BRANDA) != partnerKc + 1) {
                throw fail("the partner's kill count didn't rise");
            }
            if (TitansLoot.choose(p, RoyalTitansLoot.Choice.LOOT, "branda")) {
                throw fail("the player looted the Titans twice");
            }
            return "player " + pct + "% looted Eldric (" + loot.size() + " stacks), partner 50% took " + pages + " pages";
        } finally {
            partner.getInventory().clearContent();
            p.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        }
    }
}
