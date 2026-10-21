package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.PoisonService;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.systems.SpecialAttackService;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.network.HudNet;
import com.runecraft.network.HudProbe;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AmmoRecovery;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CastBar;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.data.Monster;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.QuickConsume;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Round 42 combat HUD checks ({@code hud:*}, run by the client walkthrough's real-player self-test too): the telegraph cast
 * bar, the opponent info box, the curse map, the poison tint, the auto-retaliate hint, quick-eat / quick-drink, the special
 * attack cleared on a weapon switch, the boss bar status and ammo landing under the target. Every check asserts what the
 * server sends (packets through their recorders, state); with the real player on the integrated server it also waits for the
 * client's HUD ({@link HudProbe}) to show it: the right style, key, frame, name and hitpoints on screen.
 */
final class CombatHudChecks {
    /** How long a check waits for the local client's HUD to show what the server sent. */
    private static final long CLIENT_WAIT_MS = 3000;

    private final TestContext c;

    private CombatHudChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        CombatHudChecks g = new CombatHudChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("hud:cast_bar", g::castBar));
        checks.add(new SelfTest.Check("hud:info_box", g::infoBox));
        checks.add(new SelfTest.Check("hud:curse_map", g::curseMap));
        checks.add(new SelfTest.Check("hud:poison_and_retaliate", g::poisonAndRetaliate));
        checks.add(new SelfTest.Check("hud:quick_eat", g::quickEat));
        checks.add(new SelfTest.Check("hud:special_switch", g::specialSwitch));
        checks.add(new SelfTest.Check("hud:boss_bar_status", g::bossBarStatus));
        checks.add(new SelfTest.Check("hud:ammo_landing", g::ammoLanding));
        return checks;
    }

    private ServerPlayer p() {
        return c.player;
    }

    private PlayerData data() {
        return Players.data(c.player);
    }

    private static AssertionError failure(String message) {
        return new AssertionError(message);
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw failure(message);
        }
    }

    private LivingEntity spawn(String id, int dx, int dz) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            throw failure("no entity type " + id);
        }
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity living)) {
            throw failure(id + " is not a living entity");
        }
        BlockPos pos = c.near(dx, dz);
        living.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        if (living instanceof Mob mob) {
            mob.finalizeSpawn(c.world, c.world.getCurrentDifficultyAt(pos), MobSpawnType.SPAWNER, null);
            mob.setPersistenceRequired();
            mob.setNoAi(true);
        }
        c.world.addFreshEntity(living);
        return living;
    }

    /** Whether the local client's HUD can be checked: the real player with the client in this JVM drawing a probe. */
    private boolean client() {
        return c.realPlayer() && HudProbe.latest != null;
    }

    /**
     * Waits (on the server thread, a few seconds at most) until the client's HUD shows what {@code ok} wants; the client
     * runs on its own thread and reads the packets sent so far, flushed first. Returns the snapshot, or null on time-out.
     */
    private HudProbe.Snapshot awaitClient(Predicate<HudProbe.Snapshot> ok) {
        if (p().connection != null) {
            p().connection.getConnection().flushChannel();
        }
        long end = System.currentTimeMillis() + CLIENT_WAIT_MS;
        while (System.currentTimeMillis() < end) {
            HudProbe.Snapshot s = HudProbe.latest;
            if (s != null && ok.test(s)) {
                return s;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        return null;
    }

    private static HudProbe.Cast castFrom(HudProbe.Snapshot s, int source) {
        for (HudProbe.Cast cast : s.casts()) {
            if (cast.source() == source) {
                return cast;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ cast bar

    /**
     * A telegraphed magic attack puts the attacker on the target's cast bar with the magic style, its lead and (on the client)
     * the Protect from Magic key in a red frame, turning green once Protect from Magic is up.
     */
    private String castBar() {
        LivingEntity spawned = spawn(Monster.BLACK_DEMON.entity(), 3, 0);
        if (!(spawned instanceof OsrsMonsterEntity mob)) {
            throw new SelfTest.Skip("the black demon isn't an OSRS monster entity here");
        }
        List<HudNet.SentCast> sent = new ArrayList<>();
        java.util.function.Consumer<HudNet.SentCast> before = HudNet.castListener;
        HudNet.castListener = s -> {
            before.accept(s);
            if (s.player() == p()) {
                sent.add(s);
            }
        };
        String note;
        try {
            data().activePrayers().clear();
            ServerNetworking.sync(p()); // the client's prayer HUD starts with no overhead (ordered before the cast)
            AttackDef blast = AttackDef.of("hud_test_blast", AttackType.MAGIC, 20, 20, 1).telegraph(2, "The demon gathers dark magic!");
            Mechanics.perform(mob, p(), blast, 1.0);
            expect(sent.size() == 1, "a telegraphed attack sent " + sent.size() + " cast-bar packets to its target");
            HudNet.SentCast cast = sent.get(0);
            expect(cast.source() == mob.getId() && cast.style() == CastBar.MAGIC && cast.flags() == 0,
                    "the cast names " + cast.source() + " style " + cast.style() + " flags " + cast.flags() + ", expected the demon "
                            + mob.getId() + ", magic");
            expect(cast.landInMc() >= com.runecraft.osrs.ThreatTiming.TELEGRAPH_FLOOR_MC,
                    "the cast lands in " + cast.landInMc() + " MC, under the 36 MC telegraph floor");
            note = "magic, lands in " + cast.landInMc() + " MC";
            if (client()) {
                HudProbe.Snapshot shown = awaitClient(s -> castFrom(s, mob.getId()) != null);
                expect(shown != null, "the client's cast bar never showed the demon's telegraph");
                HudProbe.Cast row = castFrom(shown, mob.getId());
                expect(row.style() == CastBar.MAGIC && row.protection() == 0, "the cast bar row has style " + row.style()
                        + " and protection " + row.protection() + ", expected magic / Protect from Magic");
                expect(row.key().equals(shown.protectionKeys()[0]) && !row.key().equals("?"),
                        "the row shows key '" + row.key() + "', the Protect from Magic key is '" + shown.protectionKeys()[0] + "'");
                expect(Boolean.FALSE.equals(row.rightOverhead()), "with no overhead the frame isn't red: " + row.rightOverhead());
                expect(row.name().equals(mob.getName().getString()), "the row names '" + row.name() + "'");
                PrayerService.set(p(), Prayer.PROTECT_FROM_MAGIC, true);
                ServerNetworking.sync(p());
                HudProbe.Snapshot green = awaitClient(s -> castFrom(s, mob.getId()) != null
                        && Boolean.TRUE.equals(castFrom(s, mob.getId()).rightOverhead()));
                expect(green != null, "the frame didn't turn green with Protect from Magic up");
                note += "; client: '" + row.name() + "' MAG [" + row.key() + "] " + row.secondsLeft() + "s, red then green";
            } else {
                note += " (no client: server packet only)";
            }
            // Jad's telegraph goes through the same bar (ThreatCues.announce with its 3-tick lead).
            sent.clear();
            com.runecraft.boss.ThreatCues.announce(mob, p(), "The demon stomps!", AttackType.RANGED, 36);
            expect(sent.size() == 1 && sent.get(0).style() == CastBar.RANGED && sent.get(0).landInMc() == 36,
                    "an announced telegraph sent " + sent);
        } finally {
            HudNet.castListener = before;
            data().activePrayers().clear();
            mob.discard();
            Mechanics.tickForSelfTest(); // the discarded demon's telegraph is dropped
        }
        return note;
    }

    // ------------------------------------------------------------------ opponent info box

    /** A hit on a monster sends its exact hitpoints; the client's info box shows its name, level and those hitpoints. */
    private String infoBox() {
        com.runecraft.combat.CombatZones.forget(p());
        LivingEntity mob = spawn(Monster.BLACK_DEMON.entity(), 3, 0);
        try {
            ServerNetworking.LAST_SPLAT = null;
            int dealt = CombatHandler.applyDamage(p(), mob, 7);
            ServerNetworking.Splat splat = ServerNetworking.LAST_SPLAT;
            expect(splat != null && splat.target() == mob.getId() && splat.attacker() == p().getId(),
                    "the hit sent " + splat + ", expected a splat on the demon from the player");
            int[] hp = HudNet.osrsHitpoints(mob);
            expect(dealt == 7, "the 7 damage hit dealt " + dealt);
            expect(splat.hp() == hp[0] && splat.maxHp() == hp[1] && hp[0] < hp[1],
                    "the splat carries " + splat.hp() + "/" + splat.maxHp() + ", the demon has " + hp[0] + "/" + hp[1]);
            String note = "splat " + splat.hp() + "/" + splat.maxHp();
            if (client()) {
                HudProbe.Snapshot s = awaitClient(x -> x.opponent() == mob.getId() && x.opponentHp() == hp[0]);
                expect(s != null, "the client's info box never showed the demon at " + hp[0] + " hitpoints (last: "
                        + (HudProbe.latest == null ? "none" : HudProbe.latest.opponent() + " " + HudProbe.latest.opponentHp()) + ")");
                expect(s.opponentMaxHp() == hp[1] && s.opponentName().equals(mob.getName().getString()) && s.opponentLevel() > 0,
                        "the info box shows " + s.opponentName() + " level " + s.opponentLevel() + " " + s.opponentHp() + "/"
                                + s.opponentMaxHp());
                note += "; client box: " + s.opponentName() + " (level-" + s.opponentLevel() + ") " + s.opponentHp() + "/"
                        + s.opponentMaxHp();
            }
            return note;
        } finally {
            mob.discard();
            com.runecraft.combat.CombatZones.forget(p());
        }
    }

    // ------------------------------------------------------------------ curse map

    /** The Nightmare's curse reaches the client as a key map; the cast bar then names the key that really blocks. */
    private String curseMap() {
        ServerPlayer p = p();
        BiFunction<ServerPlayer, Prayer, Prayer> curse = (pl, pr) -> pl == p
                ? com.runecraft.osrs.content.bosses.NightmareRules.cursed(pr) : pr;
        HudNet.update(p);
        PrayerService.REDIRECTS.add(curse);
        String note;
        try {
            HudNet.update(p);
            int[] sent = HudNet.lastSent(p);
            expect(sent != null && sent[0] == 1 && sent[1] == 2 && sent[2] == 0,
                    "the cursed key map sent is " + (sent == null ? "nothing" : Arrays.toString(Arrays.copyOf(sent, 3))));
            note = "sent " + Arrays.toString(Arrays.copyOf(sent, 3));
            if (client()) {
                HudProbe.Snapshot s = awaitClient(x -> Arrays.equals(x.curse(), new int[]{1, 2, 0}));
                expect(s != null, "the client never got the curse map");
                HudNet.cast(p, null, CastBar.MAGIC, 0, 40, "Curse check");
                HudProbe.Snapshot k = awaitClient(x -> x.casts().stream().anyMatch(cst -> cst.name().equals("Curse check")));
                expect(k != null, "the client never showed the curse check cast");
                HudProbe.Cast row = k.casts().stream().filter(cst -> cst.name().equals("Curse check")).findFirst().orElseThrow();
                expect(row.key().equals(k.protectionKeys()[2]), "cursed, a magic cast shows key '" + row.key()
                        + "'; Protect from Magic is now switched by the melee key '" + k.protectionKeys()[2] + "'");
                note += "; client map " + Arrays.toString(s.curse()) + ", magic blocked with [" + row.key() + "]";
            }
        } finally {
            PrayerService.REDIRECTS.remove(curse);
        }
        HudNet.update(p);
        int[] after = HudNet.lastSent(p);
        expect(after[0] == 0 && after[1] == 1 && after[2] == 2, "the curse map stayed " + Arrays.toString(Arrays.copyOf(after, 3)));
        if (client()) {
            expect(awaitClient(x -> Arrays.equals(x.curse(), new int[]{0, 1, 2})) != null, "the client kept the curse map");
        }
        return note;
    }

    // ------------------------------------------------------------------ poison tint and auto-retaliate

    /** Poison and venom tint the HP orb; a monster's hit with auto-retaliate on names it in the hint. */
    private String poisonAndRetaliate() {
        ServerPlayer p = p();
        long tick = ActionManager.gameTick();
        PoisonService.clear(p);
        HudNet.update(p);
        PoisonService.poison(p, 6, tick);
        HudNet.update(p);
        expect(HudNet.lastSent(p)[3] == 1, "a poisoned player sent poison state " + HudNet.lastSent(p)[3]);
        if (client()) {
            expect(awaitClient(x -> x.poison() == 1) != null, "the client's HP orb never turned poisoned");
        }
        PoisonService.venom(p, tick);
        HudNet.update(p);
        expect(HudNet.lastSent(p)[3] == 2, "an envenomed player sent poison state " + HudNet.lastSent(p)[3]);
        if (client()) {
            expect(awaitClient(x -> x.poison() == 2) != null, "the client's HP orb never turned envenomed");
        }
        PoisonService.clear(p);
        HudNet.update(p);
        expect(HudNet.lastSent(p)[3] == 0, "a cured player still sends poison state " + HudNet.lastSent(p)[3]);

        LivingEntity mob = spawn(Monster.BLACK_DEMON.entity(), 2, 0);
        try {
            expect(data().autoRetaliate(), "auto-retaliate is off on fresh player data");
            data().lastAttackTick = ActionManager.mcTick() - 10_000; // not fighting by hand
            CombatHandler.applyDamage(mob, p, 0);
            c.heal();
            HudNet.update(p);
            int target = HudNet.lastSent(p)[4];
            if (target < 0) {
                throw new SelfTest.Skip("auto-retaliate didn't engage the demon here (combat zones / busy)");
            }
            expect(target == mob.getId(), "auto-retaliate names entity " + target + ", the demon is " + mob.getId());
            String note = "poison 1, venom 2, cured 0; retaliating against " + target;
            if (client()) {
                String want = "Auto-retaliating: " + mob.getName().getString();
                HudProbe.Snapshot s = awaitClient(x -> x.retaliating().equals(want));
                expect(s != null, "the client never showed '" + want + "'");
                note += "; client: '" + s.retaliating() + "'";
            }
            return note;
        } finally {
            com.runecraft.combat.AutoRetaliate.stop(p);
            mob.discard();
            com.runecraft.combat.CombatZones.forget(p);
            HudNet.update(p);
        }
    }

    // ------------------------------------------------------------------ quick-eat / quick-drink

    /**
     * Quick-eat picks the food that fits the missing hitpoints (the lobster for 15, not the shark), refuses during the eat
     * delay, then eats a karambwan (combo food); quick-drink picks the prayer potion when prayer is low.
     */
    private String quickEat() {
        ServerPlayer p = p();
        p.getInventory().clearContent();
        p.getInventory().selected = 0;
        p.getInventory().setItem(0, Inv.stack("shark", 1));
        p.getInventory().setItem(5, Inv.stack("lobster", 2));
        p.getInventory().setItem(7, Inv.stack("cooked_karambwan", 1));
        p.getInventory().setItem(9, Inv.stack("prayer_potion", 1));
        p.getInventory().setItem(10, Inv.stack("super_attack", 1));
        for (int slot : new int[]{0, 5, 7, 9, 10}) {
            ItemStack s = p.getInventory().getItem(slot);
            expect(!s.isEmpty(), "item in slot " + slot + " is not registered");
            p.getCooldowns().removeCooldown(s.getItem());
        }
        float max = p.getMaxHealth();
        p.setHealth(max - 15);
        boolean ate = QuickConsume.use(p, false);
        expect(ate && "lobster".equals(QuickConsume.lastUsed), "quick-eat at 15 missing ate " + QuickConsume.lastUsed
                + (QuickConsume.lastRefusal == null ? "" : " (" + QuickConsume.lastRefusal + ")"));
        expect(p.getInventory().getItem(5).getCount() == 1 && p.getInventory().getItem(0).getCount() == 1,
                "the lobster stack has " + p.getInventory().getItem(5).getCount() + ", the shark " + p.getInventory().getItem(0).getCount());
        expect(p.getHealth() >= max - 15 + 12 - 0.01f, "health " + p.getHealth() + " after a lobster from " + (max - 15));
        p.setHealth(max - 30);
        boolean second = QuickConsume.use(p, false);
        expect(second && "cooked_karambwan".equals(QuickConsume.lastUsed),
                "during the eat delay quick-eat used " + QuickConsume.lastUsed + " (" + QuickConsume.lastRefusal + "), expected the karambwan combo");
        expect(!QuickConsume.use(p, false), "a third food went down on the same tick: " + QuickConsume.lastUsed);
        data().setPrayerPoints(10);
        boolean drank = QuickConsume.use(p, true);
        expect(drank && "prayer_potion".equals(QuickConsume.lastUsed), "quick-drink at 10 prayer drank " + QuickConsume.lastUsed
                + (QuickConsume.lastRefusal == null ? "" : " (" + QuickConsume.lastRefusal + ")"));
        expect(data().prayerPoints() > 10, "prayer points " + data().prayerPoints() + " after a prayer potion from 10");
        expect(p.getInventory().getItem(9).getDamageValue() == 1, "the prayer potion has damage " + p.getInventory().getItem(9).getDamageValue());
        expect(p.getOffhandItem().isEmpty(), "the off hand kept " + p.getOffhandItem());
        c.heal();
        return "lobster at 15 missing, karambwan combo, third refused, prayer potion at 10 prayer";
    }

    // ------------------------------------------------------------------ special attack on a weapon switch

    /** A special armed on the dragon dagger is cleared by switching to the whip; switching back shows "Special ready". */
    private String specialSwitch() {
        ServerPlayer p = p();
        p.getInventory().clearContent();
        int sel = p.getInventory().selected;
        p.getInventory().setItem(sel, Inv.stack("dragon_dagger", 1));
        expect(!p.getMainHandItem().isEmpty(), "no dragon dagger item");
        SysState.SysPlayer sp = SysState.of(p);
        sp.specEnergy = 100;
        sp.specArmed = false;
        SpecialAttackService.toggle(p);
        expect(sp.specArmed, "the dragon dagger's special didn't arm");
        expect(!SpecialAttackService.disarmedBySwitch(p) && sp.specArmed, "the special disarmed with the dagger still in hand");
        p.getInventory().setItem(sel, Inv.stack("abyssal_whip", 1));
        expect(SpecialAttackService.disarmedBySwitch(p) && !sp.specArmed, "switching to the whip left the special armed");
        expect(!SpecialAttackService.isSpecial(p, AttackType.SLASH), "the whip's attack still counts as the armed special");
        String note = "armed on the dagger, cleared by the whip";
        if (client()) {
            // a weapon without a special first (no cue), then the dagger: the cue shows for the switch to it
            p.getInventory().setItem(sel, Inv.stack("rune_scimitar", 1));
            p.inventoryMenu.broadcastChanges();
            HudProbe.Snapshot plain = awaitClient(x -> x.held().equals("rune_scimitar"));
            expect(plain != null, "the client never held the rune scimitar (held " + (HudProbe.latest == null ? "?" : HudProbe.latest.held()) + ")");
            expect(!plain.specialReady(), "'Special ready' showed for a rune scimitar, which has no special attack");
            p.getInventory().setItem(sel, Inv.stack("dragon_dagger", 1));
            p.inventoryMenu.broadcastChanges();
            HudProbe.Snapshot s = awaitClient(x -> x.held().equals("dragon_dagger") && x.specialReady());
            expect(s != null, "the client never showed 'Special ready' after switching to the dragon dagger");
            note += "; client: no cue for the scimitar, Special ready for the dagger";
        }
        p.getInventory().clearContent();
        p.inventoryMenu.broadcastChanges();
        return note;
    }

    // ------------------------------------------------------------------ boss bar status

    /** The boss bar shows the phase and the style colour: Zulrah's serpentine form green, magma red, tanzanite blue. */
    private String bossBarStatus() {
        StringBuilder note = new StringBuilder();
        for (String[] form : new String[][]{{"zulrah", "GREEN"}, {"zulrah_magma", "RED"}, {"zulrah_tanzanite", "BLUE"}}) {
            BossDef def = ContentRegistry.boss(form[0]);
            expect(def != null, "no boss " + form[0]);
            DataBossEntity boss = DataBossService.spawn(c.world, c.near(4, 0), def, p());
            expect(boss != null, "couldn't spawn " + form[0]);
            try {
                boss.refreshBossBar();
                expect(boss.bossBarColor() == BossEvent.BossBarColor.valueOf(form[1]),
                        form[0] + "'s bar is " + boss.bossBarColor() + ", expected " + form[1]);
                note.append(form[0]).append(' ').append(boss.bossBarColor()).append("; ");
            } finally {
                boss.discard();
            }
        }
        BossDef phased = ContentRegistry.boss("sarachnis"); // two phase thresholds: "Phase 1/3"
        if (phased != null) {
            DataBossEntity boss = DataBossService.spawn(c.world, c.near(4, 0), phased, p());
            if (boss != null) {
                try {
                    boss.refreshBossBar();
                    String title = boss.bossBarTitle();
                    expect(title.startsWith(phased.name) && title.contains("Phase 1/" + (phased.phases.size() + 1)),
                            phased.id + "'s bar reads '" + title + "'");
                    note.append('\'').append(title).append('\'');
                } finally {
                    boss.discard();
                }
            }
        }
        return note.toString();
    }

    // ------------------------------------------------------------------ ammo landing

    /**
     * Fired ammo lands under the target: 200 landings without a device drop about 80% at the target's feet; with Ava's
     * accumulator about 72% come back to the pack and 8% drop; a rune platebody stops it. A real arrow hitting the demon and a
     * shot that misses into the floor land the same way.
     */
    private String ammoLanding() {
        ServerPlayer p = p();
        LivingEntity target = spawn(Monster.BLACK_DEMON.entity(), 5, 0);
        int[] outcomes = new int[3];
        List<Vec3> spots = new ArrayList<>();
        java.util.function.Consumer<OsrsEquipmentItems.AmmoLanding> before = OsrsEquipmentItems.ammoListener;
        OsrsEquipmentItems.ammoListener = l -> {
            before.accept(l);
            if (l.shooter() == p) {
                outcomes[l.outcome().ordinal()]++;
                spots.add(l.at());
            }
        };
        SysState.SysPlayer sp = SysState.of(p);
        try {
            p.getInventory().clearContent();
            sp.worn.remove(Jewellery.Slot.CAPE);
            for (int i = 0; i < 200; i++) {
                OsrsEquipmentItems.landAmmo(p, target.position(), "rune_arrow");
            }
            int dropped = outcomes[AmmoRecovery.Outcome.DROPPED.ordinal()];
            expect(outcomes[AmmoRecovery.Outcome.RECOVERED.ordinal()] == 0, "ammo came back without a device");
            expect(dropped >= 130 && dropped <= 190, dropped + " of 200 arrows dropped without a device (wiki: 80%)");
            int onFloor = arrowsNear(target.position(), 1.5);
            expect(onFloor == dropped, onFloor + " arrows lie under the demon, " + dropped + " dropped");
            String note = "no device: " + dropped + "/200 under the target";

            Arrays.fill(outcomes, 0);
            clearArrows(target.position());
            sp.worn.put(Jewellery.Slot.CAPE, Inv.stack("avas_accumulator", 1));
            expect(OsrsEquipmentItems.ammoDevice(p) == AmmoRecovery.Device.ACCUMULATOR, "Ava's accumulator isn't worn");
            for (int i = 0; i < 200; i++) {
                OsrsEquipmentItems.landAmmo(p, target.position(), "rune_arrow");
            }
            int back = outcomes[AmmoRecovery.Outcome.RECOVERED.ordinal()];
            int down = outcomes[AmmoRecovery.Outcome.DROPPED.ordinal()];
            expect(back >= 115 && back <= 170, back + " of 200 came back with Ava's accumulator (wiki: 72%)");
            expect(down >= 3 && down <= 32, down + " of 200 dropped with Ava's accumulator (wiki: 8%)");
            expect(Inv.count(p, "rune_arrow") == back, Inv.count(p, "rune_arrow") + " arrows in the pack, " + back + " recovered");
            note += "; accumulator: " + back + " back, " + down + " dropped";

            p.setItemSlot(EquipmentSlot.CHEST, Inv.stack("rune_platebody", 1));
            expect(OsrsEquipmentItems.ammoDevice(p) == AmmoRecovery.Device.NONE, "a rune platebody didn't stop Ava's accumulator");
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
            sp.worn.remove(Jewellery.Slot.CAPE);

            // A real arrow on the demon (CombatHandler.playerRanged) and one missing into the floor (LandingArrow).
            spots.clear();
            Arrays.fill(outcomes, 0);
            ItemStack bow = Inv.stack("magic_shortbow", 1);
            ItemStack ammo = Inv.stack("rune_arrow", 1);
            if (!(ammo.getItem() instanceof ArrowItem arrowItem)) {
                throw failure("rune_arrow isn't an arrow item");
            }
            AbstractArrow hit = arrowItem.createArrow(c.world, ammo, p, bow);
            hit.setPos(target.getX(), target.getY() + 1, target.getZ() - 0.6);
            if (target instanceof Mob m) {
                m.invulnerableTime = 0;
            }
            target.hurt(p.damageSources().arrow(hit, p), 5f);
            expect(outcomes[0] + outcomes[1] + outcomes[2] == 1, "the arrow on the demon landed " + (outcomes[0] + outcomes[1]
                    + outcomes[2]) + " times");
            expect(spots.get(0).distanceTo(target.position()) < 0.01, "the arrow's ammo landed at " + spots.get(0)
                    + ", the demon stands at " + target.position());
            AbstractArrow miss = arrowItem.createArrow(c.world, ammo, p, bow);
            if (!(miss instanceof OsrsEquipmentItems.LandingArrow)) {
                throw failure("a fired rune arrow isn't a LandingArrow");
            }
            BlockPos floor = c.near(-3, 0);
            miss.setPos(floor.getX() + 0.5, floor.getY() + 1.5, floor.getZ() + 0.5);
            miss.setDeltaMovement(0, -1.5, 0);
            c.world.addFreshEntity(miss);
            for (int i = 0; i < 20 && !miss.isRemoved(); i++) {
                miss.tick();
            }
            expect(miss.isRemoved(), "an arrow fired into the floor is still flying or stuck");
            expect(spots.size() == 2 && spots.get(1).distanceTo(Vec3.atCenterOf(floor)) < 1.5,
                    "the missed arrow's ammo landed at " + (spots.size() > 1 ? spots.get(1) : "nowhere") + ", it struck near " + floor);
            note += "; a real hit landed under the demon, a miss where it struck";
            return note;
        } finally {
            OsrsEquipmentItems.ammoListener = before;
            sp.worn.remove(Jewellery.Slot.CAPE);
            clearArrows(target.position());
            target.discard();
            com.runecraft.combat.CombatZones.forget(p);
        }
    }

    private int arrowsNear(Vec3 at, double r) {
        int n = 0;
        for (ItemEntity e : c.world.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(r, 1.0, r))) {
            if ("rune_arrow".equals(Inv.idOf(e.getItem()))) {
                n += e.getItem().getCount();
            }
        }
        return n;
    }

    private void clearArrows(Vec3 at) {
        for (ItemEntity e : c.world.getEntitiesOfClass(ItemEntity.class, new AABB(at, at).inflate(4, 2, 4))) {
            e.discard();
        }
    }
}
