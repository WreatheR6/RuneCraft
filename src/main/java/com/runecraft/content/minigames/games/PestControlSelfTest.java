package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.HitCalcBridge;
import com.runecraft.combat.MonsterStats;
import com.runecraft.content.minigames.Arena;
import com.runecraft.content.minigames.MinigameCombat;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.minigames.RewardShopService;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules.PestControl;
import com.runecraft.osrs.content.minigames.PestControlRules;
import com.runecraft.osrs.content.minigames.PestControlRules.Variant;
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.StackData;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/**
 * Self-test driver for a whole Pest Control game as a real player ({@code com.runecraft.selftest}, the "minigame:" checks in
 * real-player mode): the lander's requirements (combat level, the 4,000 point cap), boarding the novice lander and departing,
 * the arrival on the jetty, the shields dropping on the wiki's schedule in a rotation, the pests' lander levels and max hits,
 * shifters striking the Void Knight, a ravager breaking a barricade and the player repairing it, a splatter detonating on it,
 * a spinner healing its portal and exploding when it falls, the Void seal, the portals' destruction healing the knight, the
 * win with its commendation points, coins and kill count, the reward shop (a Void Knight top, the elite upgrade's diary lock,
 * experience) and the Void set effect; then a lost game, a game won by holding out 20 minutes, one without zeal and a death.
 * Every broken step throws an {@link AssertionError}.
 */
public final class PestControlSelfTest {
    private final ServerPlayer player;
    private final UUID uuid;
    private final List<String> log = new ArrayList<>();

    private PestControlSelfTest(ServerPlayer player) {
        this.player = player;
        this.uuid = player.getUUID();
    }

    public static String run(ServerPlayer player) {
        return new PestControlSelfTest(player).walk();
    }

    private static void expect(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    private void heal() {
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private int points() {
        return MinigameData.of(player).points(uuid, "pest_control");
    }

    private void tick(PestControlSession s, int n) {
        for (int i = 0; i < n && !s.isEnded(); i++) {
            s.selfTestTick();
            heal();
        }
    }

    /** Ticks {@code n} times, killing the pests every few ticks (except {@code keep}) so the knight survives the waits. */
    private void tickClearing(PestControlSession s, int n, List<Mob> keep) {
        for (int i = 0; i < n && !s.isEnded(); i++) {
            s.selfTestTick();
            heal();
            if (i % 4 == 3) {
                clearPests(s, keep);
            }
        }
    }

    /** A player hit through the kill path and the after-hit hooks (activity). */
    private int hit(LivingEntity target, int damage, AttackType type) {
        int dealt = CombatHandler.applyDamage(player, target, damage);
        CombatHandler.hookAfterHit(player, target, type, dealt);
        return dealt;
    }

    /** Kills the pests (splatters are removed without detonating, so they can't wear the portals down between checks). */
    private void clearPests(PestControlSession s, List<Mob> keep) {
        for (PestControlSession.Pest p : new ArrayList<>(s.pests)) {
            if (!p.mob.isAlive() || keep != null && keep.contains(p.mob)) {
                continue;
            }
            if (p.id.equals("splatter")) {
                p.exploded = true;
                p.mob.discard();
            } else {
                hit(p.mob, (int) Math.ceil(p.mob.getHealth()) + 5, AttackType.CRUSH);
            }
        }
    }

    private int combatLevelFor(int level) {
        PlayerData data = Players.data(player);
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC, Skill.PRAYER}) {
            data.skills().setXpTenths(s, Experience.xpForLevel(level) * 10);
        }
        XpService.updateMaxHealth(player);
        return data.skills().combatLevel();
    }

    private PestControlSession board(String entrance) {
        MinigameManager.enter(player, Minigame.PEST_CONTROL, entrance);
        MinigameSession s = MinigameManager.selfTestDepart(player);
        return s instanceof PestControlSession pc ? pc : null;
    }

    private String walk() {
        MinigameData md = MinigameData.of(player);
        // ---- Boarding: the squire turns away players holding 4,000 points, and each lander has its combat level.
        md.addPoints(uuid, "pest_control", PestControl.MAX_POINTS);
        expect(board("pest_control_entrance") == null && MinigameManager.session(player) == null,
                "a player holding 4,000 commendation points boarded a lander");
        md.addPoints(uuid, "pest_control", -PestControl.MAX_POINTS);
        int combat = combatLevelFor(50);
        expect(combat >= 40 && combat < 70, "combat level " + combat + " for the lander checks");
        expect(board("pest_control_intermediate_entrance") == null, "a combat " + combat + " player boarded the intermediate lander (70+)");
        combatLevelFor(99);
        log.add("4,000-point cap and combat " + combat + " turned away from the intermediate lander");

        // ---- The novice lander departs with the player.
        PestControlSession pc = board("pest_control_entrance");
        expect(pc != null && MinigameManager.session(player) == pc, "boarding the novice lander did not start a game");
        try {
            playToWin(pc);
        } finally {
            if (!pc.isEnded()) {
                pc.end(false);
            }
        }
        expect(MinigameManager.session(player) == null, "the player is still bound to the game after it was won");
        expect(!Arena.inInstanceArea(player.blockPosition()), "the player was not sent back to the outpost after the game");
        int combatLevel = Players.data(player).skills().combatLevel();
        expect(points() == PestControl.POINTS[0], "a novice win paid " + points() + " points, not " + PestControl.POINTS[0]);
        expect(Inv.count(player, "coins") == combatLevel * PestControl.COINS_PER_COMBAT_LEVEL,
                "a win paid " + Inv.count(player, "coins") + " coins, not ten per combat level (" + combatLevel * 10 + ")");
        expect(md.stat(uuid, "kc:pest_control") == 1, "the win was not counted");
        log.add("won: " + points() + " points, " + Inv.count(player, "coins") + " coins");

        shop();
        voidSetEffect();
        lostGame();
        timedOutGames();
        death();
        return String.join("; ", log);
    }

    private void playToWin(PestControlSession pc) {
        expect(pc.tier == 0, "the novice lander started tier " + pc.tier);
        BlockPos rel = player.blockPosition().subtract(pc.origin);
        expect(rel.getZ() < PestControlRules.FORT_NORTH * -1 - 5 && Math.abs(rel.getX()) <= 3, "the player arrived at " + rel + ", not on the jetty");
        Mob knight = pc.knight();
        expect(knight != null && knight.isAlive() && Math.round(knight.getMaxHealth()) == PestControl.VOID_KNIGHT_HP,
                "the Void Knight is missing or has " + (knight == null ? 0 : knight.getMaxHealth()) + " hitpoints");
        for (int i = 0; i < 4; i++) {
            Mob portal = pc.portal(i);
            expect(portal != null && Math.round(portal.getMaxHealth()) == PestControl.PORTAL_HP[0], "novice portal " + i + " hitpoints");
            expect(pc.shielded(i) && portal.getTags().contains(MinigameCombat.INVULNERABLE), "portal " + i + " starts unshielded");
        }
        expect(CombatHandler.hookAfterRoll(player, pc.portal(0), AttackType.RANGED, 30) == 0, "a shielded portal took damage");

        // ---- Shields: the first drops 15 seconds in, the rest every 30 seconds, in the game's rotation.
        tickClearing(pc, 24, null);
        for (int i = 0; i < 4; i++) {
            expect(pc.shielded(i), "a shield dropped before 15 seconds (tick " + pc.elapsed() + ")");
        }
        tick(pc, 1);
        int first = pc.rotation()[0];
        for (int i = 0; i < 4; i++) {
            expect(pc.shielded(i) == (i != first), "after 15 seconds portal " + i + " shielded " + pc.shielded(i) + " (rotation first " + first + ")");
        }
        expect(first != 3, "the red shield dropped first");
        tickClearing(pc, PestControlRules.shieldDropTick(3) - 25, null);
        for (int i = 0; i < 4; i++) {
            expect(!pc.shielded(i) && !pc.portal(i).getTags().contains(MinigameCombat.INVULNERABLE),
                    "portal " + i + " still shielded at 1:45");
        }
        log.add("shields down by tick " + pc.elapsed() + " in rotation " + java.util.Arrays.toString(pc.rotation()));

        // ---- The pests are of the novice lander's levels and hit as hard as those levels.
        tick(pc, 8);
        expect(!pc.pests.isEmpty(), "the portals spawned no pests");
        for (PestControlSession.Pest p : pc.pests) {
            Variant v = p.variant;
            expect(v.on(0), p.id + " level " + v.level() + " doesn't appear on the novice lander");
            MonsterStats st = MonsterStats.of(p.mob);
            expect(st.combatLevel() == v.level() && Math.round(p.mob.getMaxHealth()) == v.hitpoints(),
                    p.id + " spawned with level " + st.combatLevel() + " / " + p.mob.getMaxHealth() + " HP, not level " + v.level());
            AttackDef a = st.profile().attacks.get(0);
            int max = CombatHooks.versusMaxHit(p.mob, player, a.style(), a.maxHit());
            expect(max == v.maxHit(), p.id + " level " + v.level() + " hits up to " + max + ", not " + v.maxHit());
        }
        clearPests(pc, null);
        expect(pc.activity(player) > 0, "killing pests gave no activity");

        // ---- Shifters teleport to the Void Knight and strike him.
        Variant shifter = PestControlRules.variant("shifter", 57);
        List<PestControlSession.Pest> shifters = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            PestControlSession.Pest p = pc.spawnPest(i, "shifter", shifter);
            expect(p != null, "no shifter spawned");
            shifters.add(p);
        }
        long spawnedAt = pc.elapsed();
        knight.setHealth(knight.getMaxHealth());
        float knightBefore = knight.getHealth();
        List<Mob> keep = new ArrayList<>();
        shifters.forEach(p -> keep.add(p.mob));
        tickClearing(pc, 60, keep);
        expect(knight.getHealth() < knightBefore, "three shifters left the Void Knight at " + knight.getHealth() + " hitpoints");
        int near = 0;
        for (PestControlSession.Pest p : shifters) {
            if (p.mob.isAlive() && p.mob.distanceToSqr(knight) <= 9 && p.nextAttack > spawnedAt + PestControlRules.PEST_ATTACK_TICKS) {
                near++;
            }
        }
        expect(near > 0, "no shifter reached and struck the Void Knight");
        log.add("shifters took the knight to " + (int) knight.getHealth());
        clearPests(pc, null);
        knight.setHealth(knight.getMaxHealth());

        // ---- A ravager tears at a barricade; the player repairs it with a hammer and a log; a splatter detonates on it.
        PestControlSession.Barrier barricade = null;
        for (PestControlSession.Barrier b : pc.barriers) {
            if (!b.def.gate()) {
                barricade = b;
                break;
            }
        }
        expect(barricade != null, "no barricade");
        barricade.repair();
        BlockPos beside = barricade.blocks.get(1).north();
        PestControlSession.Pest ravager = pc.spawnPest(0, "ravager", PestControlRules.variant("ravager", 71));
        expect(ravager != null, "no ravager spawned");
        ravager.mob.teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
        tick(pc, 24);
        expect(barricade.hp < barricade.max, "a ravager beside a barricade left it at " + barricade.hp + "/" + barricade.max);
        hit(ravager.mob, (int) Math.ceil(ravager.mob.getHealth()) + 5, AttackType.CRUSH);
        int damaged = barricade.hp;
        player.teleportTo(pc.world, beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5, player.getYRot(), player.getXRot());
        Inv.give(player, "hammer", 1);
        Inv.give(player, "logs", 2);
        int crafting = Players.data(player).skills().xpTenths(Skill.CRAFTING);
        expect(pc.repair(player), "the hammer found nothing to repair beside a damaged barricade");
        expect(barricade.hp == barricade.max && Inv.count(player, "logs") == 1, "the repair left it at " + barricade.hp + " with "
                + Inv.count(player, "logs") + " logs");
        int gained = Players.data(player).skills().xpTenths(Skill.CRAFTING) - crafting;
        expect(gained == 50 || Players.data(player).skills().xpTenths(Skill.CRAFTING) >= Experience.MAX_XP_TENTHS,
                "a repair gave " + gained / 10.0 + " Crafting XP, not 5");
        log.add("ravager broke the barricade to " + damaged + "/" + barricade.max + ", repaired");
        BlockPos jetty = pc.origin.offset(0, 1, -18);
        player.teleportTo(pc.world, jetty.getX() + 0.5, jetty.getY(), jetty.getZ() + 0.5, player.getYRot(), player.getXRot());
        PestControlSession.Pest splatter = pc.spawnPest(0, "splatter", PestControlRules.variant("splatter", 22));
        expect(splatter != null, "no splatter spawned");
        splatter.mob.teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
        tick(pc, 2);
        expect(!splatter.mob.isAlive() && splatter.exploded, "a splatter beside a barricade did not detonate");
        expect(barricade.hp < barricade.max && barricade.max - barricade.hp >= PestControlRules.SPLATTER_MIN,
                "the splatter's detonation left the barricade at " + barricade.hp);
        log.add("splatter detonated on it (" + barricade.hp + "/" + barricade.max + ")");

        // ---- A spinner heals its portal, and explodes when the portal falls.
        int target = pc.rotation()[0];
        Mob portal = pc.portal(target);
        hit(portal, 60, AttackType.RANGED);
        float wounded = portal.getHealth();
        PestControlSession.Pest spinner = pc.spawnPest(target, "spinner", PestControlRules.variant("spinner", 36));
        expect(spinner != null, "no spinner spawned");
        spinner.mob.teleportTo(portal.getX() + 1, portal.getY(), portal.getZ());
        List<Mob> keepSpinner = List.of(spinner.mob);
        tickClearing(pc, 12, keepSpinner);
        expect(portal.getHealth() > wounded, "a spinner beside its portal didn't heal it (" + wounded + " -> " + portal.getHealth() + ")");
        knight.setHealth(100);
        hit(portal, (int) Math.ceil(portal.getHealth()) + 10, AttackType.RANGED);
        tick(pc, 1);
        expect(pc.destroyed(target), "the portal wasn't destroyed");
        expect(!spinner.mob.isAlive() && spinner.exploded, "the spinner didn't explode when its portal fell");
        expect(knight.getHealth() >= 140, "a destroyed portal healed the Void Knight to " + knight.getHealth() + ", not 150");
        log.add("spinner healed portal " + target + " (" + (int) wounded + " -> healed), exploded with it; knight healed to "
                + (int) knight.getHealth());

        // ---- The Void seal hits the pests around the player, eight charges.
        Inv.give(player, "void_seal", 1);
        ItemStack seal = null;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (Inv.idOf(player.getInventory().getItem(i)).equals("void_seal")) {
                seal = player.getInventory().getItem(i);
            }
        }
        expect(seal != null, "no Void seal");
        int other = (target + 1) % 4;
        while (pc.destroyed(other)) {
            other = (other + 1) % 4;
        }
        PestControlSession.Pest brawler = pc.spawnPest(other, "brawler", PestControlRules.variant("brawler", 101));
        expect(brawler != null, "no brawler spawned");
        brawler.mob.teleportTo(player.getX() + 1, player.getY(), player.getZ());
        pc.rubSeal(player, seal);
        int lost = PestControlRules.variant("brawler", 101).hitpoints() - (int) Math.ceil(brawler.mob.getHealth());
        expect(lost >= PestControlRules.SEAL_MIN && lost <= PestControlRules.SEAL_MAX, "the Void seal hit the brawler for " + lost);
        expect(StackData.get(seal).getInt("SealCharges") == PestControlRules.SEAL_CHARGES - 1, "the seal has "
                + StackData.get(seal).getInt("SealCharges") + " charges after one rub");
        log.add("Void seal hit a brawler for " + lost);
        clearPests(pc, null);

        // ---- Destroy the rest: each fallen portal heals the knight; the last one wins the game.
        for (int i = 0; i < 4 && !pc.isEnded(); i++) {
            if (pc.destroyed(i)) {
                continue;
            }
            hit(pc.portal(i), (int) Math.ceil(pc.portal(i).getHealth()) + 10, i == 1 ? AttackType.MAGIC : AttackType.RANGED);
            tick(pc, 1);
        }
        expect(pc.isEnded(), "destroying all four portals didn't end the game");
    }

    private int entryIndex(String item, int amount) {
        List<RewardShop.Entry> entries = RewardShops.get("pest_control").entries();
        for (int i = 0; i < entries.size(); i++) {
            RewardShop.Entry e = entries.get(i);
            if (e.item().equals(item) && (amount <= 0 || e.costs().get(0).amount() == amount)) {
                return i;
            }
        }
        throw new AssertionError("the Void Knights' shop has no " + item);
    }

    private void shop() {
        MinigameData md = MinigameData.of(player);
        int before = points();
        RewardShopService.buy(player, "pest_control", entryIndex("void_knight_top", 0), 1);
        expect(!Inv.has(player, "void_knight_top", 1) && points() == before, "a top was sold for " + before + " points");
        md.addPoints(uuid, "pest_control", 1000);
        int bal = points();
        for (String id : List.of("void_knight_top", "void_knight_robe", "void_knight_gloves", "void_melee_helm")) {
            RewardShopService.buy(player, "pest_control", entryIndex(id, 0), 1);
            expect(Inv.has(player, id, 1), "buying " + id + " gave nothing");
        }
        expect(bal - points() == 850, "top, robe, gloves and a helm cost " + (bal - points()) + " points, not 850");
        // The elite top needs the hard Western Provinces diary.
        bal = points();
        RewardShopService.buy(player, "pest_control", entryIndex("elite_void_top", 0), 1);
        expect(!Inv.has(player, "elite_void_top", 1) && Inv.has(player, "void_knight_top", 1) && points() == bal,
                "the elite void top was sold without the hard Western Provinces diary");
        // Experience: 10 points at once give floor(level^2/600) x 35 x 10 + 1%.
        int xp = Players.data(player).skills().xpTenths(Skill.ATTACK);
        int level = Players.data(player).skills().level(Skill.ATTACK);
        RewardShopService.buy(player, "pest_control", entryIndex("ATTACK", 10), 1);
        int want = PestControlRules.xpTenths(level, 35, 10);
        int got = Players.data(player).skills().xpTenths(Skill.ATTACK) - xp;
        expect(got == want || Players.data(player).skills().xpTenths(Skill.ATTACK) >= Experience.MAX_XP_TENTHS,
                "10 points gave " + got / 10.0 + " Attack XP, not " + want / 10.0);
        expect(bal - points() == 10, "10 points of experience cost " + (bal - points()));
        log.add("bought top, robe, gloves and melee helm (850), elite top locked, 10 points = " + got / 10.0 + " Attack XP");
    }

    private void voidSetEffect() {
        // Melee void: top, robe and gloves (hands slot) with the melee helm add 10% to the melee max hit.
        PestControlSession pc = (PestControlSession) MinigameManager.selfTestStart(Minigame.PEST_CONTROL, "pest_control_entrance", player);
        try {
            Mob target = pc.portal(2);
            take("void_knight_top");
            take("void_knight_robe");
            take("void_melee_helm");
            take("void_knight_gloves");
            player.setItemSlot(EquipmentSlot.MAINHAND, Inv.stack("abyssal_whip", 1));
            int plain = HitCalcBridge.maxHit(player, target, HitCalcBridge.input(player, target, AttackType.SLASH, null, 0));
            player.setItemSlot(EquipmentSlot.CHEST, Inv.stack("void_knight_top", 1));
            player.setItemSlot(EquipmentSlot.LEGS, Inv.stack("void_knight_robe", 1));
            player.setItemSlot(EquipmentSlot.HEAD, Inv.stack("void_melee_helm", 1));
            int noGloves = HitCalcBridge.maxHit(player, target, HitCalcBridge.input(player, target, AttackType.SLASH, null, 0));
            SysState.of(player).worn.put(Jewellery.Slot.HANDS, Inv.stack("void_knight_gloves", 1));
            int set = HitCalcBridge.maxHit(player, target, HitCalcBridge.input(player, target, AttackType.SLASH, null, 0));
            expect(noGloves == plain || noGloves <= plain + 1, "void without gloves raised the max hit " + plain + " -> " + noGloves);
            expect(set > noGloves, "the full melee void set didn't raise the max hit (" + noGloves + " -> " + set + ")");
            log.add("melee void max hit " + plain + " -> " + set);
        } finally {
            SysState.of(player).worn.remove(Jewellery.Slot.HANDS);
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.HEAD}) {
                player.setItemSlot(slot, ItemStack.EMPTY);
            }
            pc.end(false);
        }
    }

    private void take(String id) {
        expect(Inv.remove(player, id, 1) == 1, "no " + id + " in the inventory");
    }

    private void lostGame() {
        int before = points();
        PestControlSession pc = (PestControlSession) MinigameManager.selfTestStart(Minigame.PEST_CONTROL, "pest_control_veteran_entrance", player);
        expect(pc.tier == 2 && Math.round(pc.portal(0).getMaxHealth()) == PestControl.PORTAL_HP[2], "veteran portals");
        pc.setActivity(player, PestControlRules.ACTIVITY_MAX);
        pc.knight().kill();
        tick(pc, 1);
        expect(pc.isEnded() && points() == before, "the Void Knight's death didn't lose the game (" + pc.isEnded() + ", " + points() + ")");
        log.add("knight killed: game lost, no points");
    }

    private void timedOutGames() {
        int before = points();
        PestControlSession pc = (PestControlSession) MinigameManager.selfTestStart(Minigame.PEST_CONTROL, "pest_control_intermediate_entrance", player);
        pc.setActivity(player, PestControlRules.ACTIVITY_MAX);
        pc.selfTestTimeOut();
        tick(pc, 1);
        expect(pc.isEnded() && points() - before == PestControl.POINTS[1], "holding out 20 minutes on the intermediate lander paid "
                + (points() - before) + " points");
        before = points();
        pc = (PestControlSession) MinigameManager.selfTestStart(Minigame.PEST_CONTROL, "pest_control_veteran_entrance", player);
        pc.setActivity(player, 0);
        pc.selfTestTimeOut();
        tick(pc, 1);
        expect(pc.isEnded() && points() == before, "a player with an empty activity bar was paid " + (points() - before) + " points");
        log.add("20 minutes held: +" + PestControl.POINTS[1] + "; empty activity bar: nothing");
    }

    private void death() {
        PestControlSession pc = (PestControlSession) MinigameManager.selfTestStart(Minigame.PEST_CONTROL, "pest_control_entrance", player);
        try {
            player.teleportTo(pc.world, pc.origin.getX() + 20.5, pc.origin.getY() + 1, pc.origin.getZ() + 0.5, player.getYRot(), player.getXRot());
            expect(!MinigameManager.selfTestDeath(player), "a death in Pest Control was a real death");
            BlockPos rel = player.blockPosition().subtract(pc.origin);
            expect(MinigameManager.session(player) == pc && rel.getZ() < -10, "a dead player respawned at " + rel + ", not on the lander jetty");
            log.add("death: back on the jetty");
        } finally {
            pc.end(false);
        }
    }
}
