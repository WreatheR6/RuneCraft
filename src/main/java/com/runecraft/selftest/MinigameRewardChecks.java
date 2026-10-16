package com.runecraft.selftest;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.minigames.RewardShopService;
import com.runecraft.content.minigames.games.BarbarianAssaultSession;
import com.runecraft.content.minigames.games.ColosseumSession;
import com.runecraft.content.minigames.games.NightmareZoneSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.bosses.ColosseumRewards;
import com.runecraft.osrs.content.minigames.BaGambles;
import com.runecraft.osrs.content.minigames.FortisColosseum;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone;
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
import com.runecraft.osrs.content.minigames.SpoilsOfWar;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Self-test checks for minigame rewards with their own mechanics: Nightmare Zone potions and power-ups,
 * Barbarian Assault gambles, spoils of war and the Wilderness-only blighted supplies. The generic food and
 * potion checks leave these items to this class ({@link #OWN}).
 */
final class MinigameRewardChecks {
    /** Items the generic food/potion checks skip because they only work inside a dream or the Wilderness. */
    static final Set<String> OWN = Set.of(NightmareZone.OVERLOAD, NightmareZone.ABSORPTION, NightmareZone.SUPER_RANGING,
            NightmareZone.SUPER_MAGIC, com.runecraft.osrs.content.minigames.Gauntlet.EGNIOL, "blighted_super_restore", "blighted_karambwan", "blighted_anglerfish", "blighted_manta_ray");

    private final TestContext c;

    private MinigameRewardChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        MinigameRewardChecks g = new MinigameRewardChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("minigame:nmz_potions", g::nmzPotions));
        checks.add(new SelfTest.Check("minigame:nmz_powerups", g::nmzPowerUps));
        checks.add(new SelfTest.Check("minigame:ba_gamble", g::baGamble));
        checks.add(new SelfTest.Check("minigame:nmz_shop", g::nmzShop));
        checks.add(new SelfTest.Check("minigame:spoils_of_war", g::spoils));
        checks.add(new SelfTest.Check("minigame:blighted_supplies", g::blighted));
        checks.add(new SelfTest.Check("minigame:gauntlet_run", g::gauntlet));
        checks.add(new SelfTest.Check("minigame:colosseum_cash_out", g::colosseumCashOut));
        checks.add(new SelfTest.Check("minigame:colosseum_waves", g::colosseumWaves));
        checks.add(new SelfTest.Check("minigame:colosseum_glory", g::colosseumGlory));
        checks.add(new SelfTest.Check("minigame:nmz_modes", g::nmzModes));
        checks.add(new SelfTest.Check("minigame:nmz_barrels", g::nmzBarrels));
        checks.add(new SelfTest.Check("minigame:trek_tomes", g::trekTomes));
        checks.add(new SelfTest.Check("minigame:shades_town", g::shadesTown));
        checks.add(new SelfTest.Check("minigame:blighted_sacks", g::blightedSacks));
        checks.add(new SelfTest.Check("minigame:emirs_ranked_build", g::emirsBuild));
        return checks;
    }

    private ServerPlayerEntity p() {
        return c.player;
    }

    /** Attack and Hitpoints experience (tenths) a 10-damage accurate melee hit gives the player right now. */
    private int combatXpFor(int damage) {
        SkillSet skills = Players.data(p()).skills();
        int before = skills.xpTenths(Skill.ATTACK) + skills.xpTenths(Skill.HITPOINTS);
        com.runecraft.combat.CombatHandler.awardXp(p(), com.runecraft.osrs.CombatStyle.ACCURATE, damage, false);
        return skills.xpTenths(Skill.ATTACK) + skills.xpTenths(Skill.HITPOINTS) - before;
    }

    /** Ticks a session until a boss is alive (at most 12 game ticks). */
    private static List<net.minecraft.entity.LivingEntity> bosses(MinigameSession s, NightmareZoneSession nmz) {
        for (int i = 0; i < 12 && nmz.bosses().isEmpty(); i++) {
            MinigameManager.selfTestTick(s);
        }
        return nmz.bosses();
    }

    /**
     * Nightmare Zone modes: a hard practice dream spawns only the chosen boss with its hard hitpoints and the hard tag and
     * pays no points or experience; endurance uses every boss at the wiki's full points; the zapper spares Count Draynor
     * until he's hit.
     */
    private String nmzModes() {
        List<String> done = new ArrayList<>();
        // Dreams only hold the bosses of completed quests; the real player starts with none (the fake player, who isn't
        // in the session's player list, gets every boss).
        c.completeQuests(null);
        NightmareZone.Dream practice = new NightmareZone.Dream(NightmareZone.Mode.PRACTICE, true, "nmz_glod");
        MinigameSession s = MinigameManager.selfTestStart(Minigame.NIGHTMARE_ZONE, practice.variant(), p());
        try {
            if (!(s instanceof NightmareZoneSession nmz) || nmz.dream().mode() != NightmareZone.Mode.PRACTICE) {
                throw failure("a practice dream did not start in practice mode");
            }
            List<net.minecraft.entity.LivingEntity> bosses = bosses(s, nmz);
            if (bosses.size() != 1 || !MinigameManager.creatureId(bosses.get(0)).equals("nmz_glod")) {
                throw failure("practice spawned " + bosses.size() + " bosses instead of Glod alone");
            }
            net.minecraft.entity.LivingEntity glod = bosses.get(0);
            if (!glod.getCommandTags().contains(NightmareZoneSession.HARD_TAG)
                    || Math.round(glod.getMaxHealth()) != NightmareZone.hardHitpoints("nmz_glod")) {
                throw failure("hard Glod has " + glod.getMaxHealth() + " hitpoints / tag " + glod.getCommandTags());
            }
            if (nmz.pointsFor("nmz_glod") != 0) {
                throw failure("practice kills pay points");
            }
            if (combatXpFor(10) != 0) {
                throw failure("practice hits give combat experience");
            }
            done.add("hard practice Glod (" + Math.round(glod.getMaxHealth()) + " HP, no points, no XP)");
        } finally {
            s.end(true);
        }
        NightmareZone.Dream endurance = new NightmareZone.Dream(NightmareZone.Mode.ENDURANCE, false, null);
        s = MinigameManager.selfTestStart(Minigame.NIGHTMARE_ZONE, endurance.variant(), p());
        try {
            NightmareZoneSession nmz = (NightmareZoneSession) s;
            List<net.minecraft.entity.LivingEntity> bosses = bosses(s, nmz);
            if (nmz.pool().size() != NightmareZone.BOSSES.length || bosses.size() != 1) {
                throw failure("endurance pool " + nmz.pool().size() + ", " + bosses.size() + " bosses at once");
            }
            if (nmz.pointsFor("nmz_glod") != NightmareZone.points("nmz_glod", false)) {
                throw failure("endurance with every boss pays " + nmz.pointsFor("nmz_glod") + " for Glod");
            }
            if (combatXpFor(10) <= 0) {
                throw failure("endurance hits give no combat experience");
            }
            net.minecraft.entity.LivingEntity first = bosses.get(0);
            if (MinigameManager.creatureId(first).equals("nmz_count_draynor")) {
                first.setHealth(2);
                nmz.zap(p(), first, NightmareZone.ZAPPER_MAX_HIT);
                if (!first.isAlive() || first.getHealth() < 1) {
                    throw failure("the zapper killed Count Draynor before the player hit him");
                }
                nmz.noteHit(first);
                nmz.zap(p(), first, NightmareZone.ZAPPER_MAX_HIT);
                if (first.isAlive() && first.getHealth() > 0) {
                    throw failure("the zapper could not finish Count Draynor after he was hit");
                }
                done.add("zapper spares Count Draynor until hit");
            }
            done.add("endurance: " + nmz.pool().size() + " bosses one at a time, full points");
        } finally {
            s.end(true);
        }
        return String.join("; ", done);
    }

    /** Dom Onion's potions go into the barrel by the dose and come out as 4-dose potions plus the rest. */
    private String nmzBarrels() {
        MinigameData data = MinigameData.of(p());
        String key = MinigameCurrencies.key(MinigameCurrencies.NIGHTMARE_ZONE);
        int before = data.points(p().getUuid(), key);
        int stored = NightmareZoneSession.barrel(p(), NightmareZone.ABSORPTION);
        RewardShop shop = RewardShops.get("nightmare_zone");
        int index = -1;
        for (int i = 0; i < shop.entries().size(); i++) {
            if (shop.entries().get(i).item().equals(NightmareZone.ABSORPTION)) {
                index = i;
            }
        }
        try {
            p().getInventory().clear();
            data.setStat(p().getUuid(), "nmz_barrel:" + NightmareZone.ABSORPTION, 0);
            data.addPoints(p().getUuid(), key, 6 * 1000 - data.points(p().getUuid(), key));
            RewardShopService.buy(p(), "nightmare_zone", index, 6);
            if (NightmareZoneSession.barrel(p(), NightmareZone.ABSORPTION) != 6 || Inv.count(p(), NightmareZone.ABSORPTION) != 0
                    || data.points(p().getUuid(), key) != 0) {
                throw failure("buying 6 doses left " + NightmareZoneSession.barrel(p(), NightmareZone.ABSORPTION) + " in the barrel and "
                        + Inv.count(p(), NightmareZone.ABSORPTION) + " potions carried");
            }
            int taken = NightmareZoneSession.withdraw(p(), NightmareZone.ABSORPTION, 6);
            int doses = 0;
            for (ItemStack st : p().getInventory().main) {
                if (NightmareZone.ABSORPTION.equals(Inv.idOf(st))) {
                    doses += 4 - st.getDamage();
                }
            }
            if (taken != 6 || doses != 6 || NightmareZoneSession.barrel(p(), NightmareZone.ABSORPTION) != 0) {
                throw failure("withdrew " + taken + " doses, carrying " + doses);
            }
        } finally {
            p().getInventory().clear();
            data.setStat(p().getUuid(), "nmz_barrel:" + NightmareZone.ABSORPTION, stored);
            data.addPoints(p().getUuid(), key, before - data.points(p().getUuid(), key));
        }
        return "6 absorption doses bought into the barrel and withdrawn as 4 + 2";
    }

    /** A reward token's tome is an item; reading it gives XP in the route's range. */
    private String trekTomes() {
        try {
            p().getInventory().clear();
            ItemStack token = Inv.stack("reward_token_red", 1);
            p().setStackInHand(Hand.MAIN_HAND, token);
            com.runecraft.content.minigames.games.TempleTrekkingSession.claim(p(), "tome");
            String tome = null;
            for (ItemStack st : p().getInventory().main) {
                if (com.runecraft.osrs.content.minigames.TempleTrekking.tome(Inv.idOf(st)) != null) {
                    tome = Inv.idOf(st);
                }
            }
            if (tome == null || !tome.endsWith("_red")) {
                throw failure("the red token's tome reward gave " + tome);
            }
            Skill skill = (Skill) com.runecraft.osrs.content.minigames.TempleTrekking.tome(tome)[0];
            int xp = Players.data(p()).skills().xp(skill);
            if (!com.runecraft.content.minigames.games.TempleTrekkingSession.readTome(p(), tome)) {
                throw failure(tome + " could not be read");
            }
            int gained = Players.data(p()).skills().xp(skill) - xp;
            var route = com.runecraft.osrs.content.minigames.TempleTrekking.Route.ROUTE_3;
            if (gained < route.tomeMin || gained > route.tomeMax + 1) {
                throw failure("reading " + tome + " gave " + gained + " XP");
            }
            return tome + " read for " + gained + " " + skill.displayName() + " XP";
        } finally {
            p().getInventory().clear();
        }
    }

    /**
     * Mort'ton: serum 207 becomes serum 208 at the sacred fire (5% sanctity a dose), an afflicted villager takes a dose for a
     * reward, the flamtaer bag holds supplies, Dampe fits locks to coffins and a coffin holds shade remains.
     */
    private String shadesTown() {
        String riyl = com.runecraft.osrs.content.minigames.ShadesOfMortton.Shade.RIYL.remains();
        try {
            p().getInventory().clear();
            com.runecraft.content.minigames.ShadesService.addSanctity(p(), 30 - com.runecraft.content.minigames.ShadesService.sanctity(p()));
            Inv.give(p(), "serum_207", 1);
            if (com.runecraft.content.minigames.ShadesService.sanctifySerums(p()) != 1 || !Inv.has(p(), "serum_208", 1)
                    || Math.abs(com.runecraft.content.minigames.ShadesService.sanctity(p()) - 10) > 0.01) {
                throw failure("serum 207 did not become serum 208 for 20% sanctity (sanctity now "
                        + com.runecraft.content.minigames.ShadesService.sanctity(p()) + ")");
            }
            int items = p().getInventory().main.stream().filter(st -> !st.isEmpty()).mapToInt(ItemStack::getCount).sum();
            com.runecraft.content.minigames.ShadesService.cure(p());
            ItemStack serum = ItemStack.EMPTY;
            for (ItemStack st : p().getInventory().main) {
                if ("serum_208".equals(Inv.idOf(st))) {
                    serum = st;
                }
            }
            int after = p().getInventory().main.stream().filter(st -> !st.isEmpty()).mapToInt(ItemStack::getCount).sum();
            boolean coins = Players.data(p()).coins() > 0;
            if (serum.isEmpty() || serum.getDamage() != 1 || (after <= items && !coins)) {
                throw failure("curing an afflicted villager used " + (serum.isEmpty() ? "?" : serum.getDamage()) + " doses / gave nothing");
            }
            p().getInventory().clear();
            Inv.give(p(), "flamtaer_bag", 1);
            Inv.give(p(), "timber_beam", 10);
            com.runecraft.content.minigames.ShadesService.useBag(p(), p().getInventory().getStack(0));
            ItemStack bag = p().getInventory().getStack(0);
            if (Inv.count(p(), "timber_beam") != 0 || bag.getNbt() == null || bag.getNbt().getInt("rc_bag_timber_beam") != 10) {
                throw failure("the flamtaer bag did not take the timber beams");
            }
            p().getInventory().clear();
            com.runecraft.content.minigames.ShadesService.dampe(p());
            Inv.give(p(), "steel_locks", 1);
            com.runecraft.content.minigames.ShadesService.dampe(p());
            if (!Inv.has(p(), "steel_coffin", 1) || Inv.has(p(), "steel_locks", 1)) {
                throw failure("Dampe did not make a steel coffin from a broken coffin and steel locks");
            }
            Inv.give(p(), riyl, 10);
            ItemStack coffin = ItemStack.EMPTY;
            for (ItemStack st : p().getInventory().main) {
                if ("steel_coffin".equals(Inv.idOf(st))) {
                    coffin = st;
                }
            }
            com.runecraft.content.minigames.ShadesService.useCoffin(p(), coffin,
                    com.runecraft.osrs.content.minigames.ShadesOfMortton.Coffin.STEEL);
            if (Inv.count(p(), riyl) != 2) {
                throw failure("the steel coffin took " + (10 - Inv.count(p(), riyl)) + " remains (holds 8)");
            }
        } finally {
            p().getInventory().clear();
        }
        return "serum 208 made and given to an afflicted villager; bag holds 10 beams; steel coffin holds 8 remains";
    }

    private static AssertionError failure(String message) {
        return new AssertionError(message);
    }

    private void use() {
        UseItemCallback.EVENT.invoker().interact(p(), c.world, Hand.MAIN_HAND);
    }

    private ItemStack hold(String id) {
        ItemStack s = Inv.stack(id, 1);
        if (s.isEmpty()) {
            throw failure("item " + id + " is not registered");
        }
        p().setStackInHand(Hand.MAIN_HAND, s);
        p().getItemCooldownManager().remove(s.getItem());
        return s;
    }

    private int usedSlots() {
        int n = 0;
        for (int i = 0; i < p().getInventory().size(); i++) {
            n += p().getInventory().getStack(i).isEmpty() ? 0 : 1;
        }
        return n;
    }

    private String nmzPotions() {
        for (String id : NightmareZone.POTIONS) {
            ItemStack s = hold(id);
            use();
            if (p().getMainHandStack() != s || s.getDamage() != 0) {
                throw failure(id + " was drunk outside the Nightmare Zone");
            }
        }
        MinigameSession s = MinigameManager.selfTestStart(Minigame.NIGHTMARE_ZONE, Minigame.NIGHTMARE_ZONE.entranceBlock(), p());
        List<String> done = new ArrayList<>();
        try {
            if (!(s instanceof NightmareZoneSession nmz)) {
                throw failure("the Nightmare Zone did not start a NightmareZoneSession");
            }
            SkillSet skills = Players.data(p()).skills();
            for (Skill k : Skill.values()) {
                skills.setBoost(k, 0);
            }
            for (String id : NightmareZone.POTIONS) {
                if (id.equals(NightmareZone.OVERLOAD) && p().getMaxHealth() <= 50) {
                    done.add("overload skipped (max HP " + (int) p().getMaxHealth() + ")");
                    continue;
                }
                p().setHealth(p().getMaxHealth());
                ItemStack held = hold(id);
                use();
                if (held.getDamage() != 1) {
                    throw failure(id + " did not use a dose in the dream (damage " + held.getDamage() + ")");
                }
                switch (id) {
                    case NightmareZone.ABSORPTION -> {
                        if (nmz.absorption(p()) != NightmareZone.ABSORPTION_PER_DOSE) {
                            throw failure("absorption points " + nmz.absorption(p()) + " after one dose (expected 50)");
                        }
                    }
                    case NightmareZone.OVERLOAD -> {
                        int want = NightmareZone.boost(skills.level(Skill.ATTACK));
                        if (!nmz.overloaded(p()) || skills.boost(Skill.ATTACK) != want || skills.boost(Skill.MAGIC) <= 0) {
                            throw failure("overload boost: Attack +" + skills.boost(Skill.ATTACK) + " (expected +" + want + ")");
                        }
                    }
                    case NightmareZone.SUPER_RANGING, NightmareZone.SUPER_MAGIC -> {
                        Skill k = id.equals(NightmareZone.SUPER_RANGING) ? Skill.RANGED : Skill.MAGIC;
                        int want = NightmareZone.boost(skills.level(k));
                        if (skills.boost(k) < want) {
                            throw failure(id + ": " + k.displayName() + " +" + skills.boost(k) + " (expected +" + want + ")");
                        }
                    }
                    default -> {
                    }
                }
                done.add(id);
            }
        } finally {
            s.end(true);
        }
        if (!c.realPlayer()) {
            // end() only wakes players in the world's player list, which the fake player is not in
            return "outside: refused; in the dream: " + String.join(", ", done) + "; waking checked by the real-player run";
        }
        for (String id : NightmareZone.POTIONS) {
            if (Inv.count(p(), id) > 0) {
                throw failure(id + " left in the inventory after waking up");
            }
        }
        return "outside: refused; in the dream: " + String.join(", ", done) + "; potions removed on waking";
    }

    private String nmzPowerUps() {
        MinigameSession s = MinigameManager.selfTestStart(Minigame.NIGHTMARE_ZONE, Minigame.NIGHTMARE_ZONE.entranceBlock(), p());
        try {
            if (!(s instanceof NightmareZoneSession nmz)) {
                throw failure("the Nightmare Zone did not start a NightmareZoneSession");
            }
            for (int i = 0; i < 6; i++) {
                MinigameManager.selfTestTick(s);
                c.heal();
            }
            nmz.activate(p(), NightmareZone.PowerUp.RECURRENT_DAMAGE);
            if (nmz.activePowerUp(p()) != NightmareZone.PowerUp.RECURRENT_DAMAGE) {
                throw failure("recurrent damage did not become active");
            }
            nmz.activate(p(), NightmareZone.PowerUp.ULTIMATE_FORCE);
            nmz.activate(p(), NightmareZone.PowerUp.ZAPPER);
            for (int i = 0; i < 6; i++) {
                MinigameManager.selfTestTick(s);
                c.heal();
            }
            if (nmz.activePowerUp(p()) != NightmareZone.PowerUp.ZAPPER) {
                throw failure("the zapper did not stay active");
            }
        } finally {
            s.end(true);
        }
        return "recurrent damage, ultimate force and zapper activated";
    }

    /** Dom Onion's shop: 15 herb boxes a day; a scroll of redirection turns a house tablet into a Rimmington tablet. */
    private String nmzShop() {
        MinigameData data = MinigameData.of(p());
        String key = MinigameCurrencies.key(MinigameCurrencies.NIGHTMARE_ZONE);
        int before = data.points(p().getUuid(), key);
        RewardShop shop = RewardShops.get("nightmare_zone");
        int box = -1;
        for (int i = 0; i < shop.entries().size(); i++) {
            if (shop.entries().get(i).item().equals("herb_box")) {
                box = i;
            }
        }
        if (box < 0) {
            throw failure("Dom Onion's shop has no herb box");
        }
        try {
            p().getInventory().clear();
            RewardShopService.resetDaily(p(), "nightmare_zone", "herb_box");
            data.addPoints(p().getUuid(), key, 20 * 9500 - data.points(p().getUuid(), key));
            RewardShopService.buy(p(), "nightmare_zone", box, 20);
            int boxes = Inv.count(p(), "herb_box");
            if (boxes != 15) {
                throw failure("bought " + boxes + " herb boxes in a day (expected the limit of 15)");
            }
            RewardShopService.buy(p(), "nightmare_zone", box, 1);
            if (Inv.count(p(), "herb_box") != 15 || data.points(p().getUuid(), key) != 5 * 9500) {
                throw failure("a 16th herb box was sold today");
            }
            p().getInventory().clear();
            Inv.give(p(), "scroll_of_redirection", 1);
            Inv.give(p(), "teleport_to_house", 1);
            if (!com.runecraft.content.minigames.MinigameItemUse.redirect(p(), "rimmington")
                    || !Inv.has(p(), "rimmington_teleport_tablet", 1) || Inv.has(p(), "scroll_of_redirection", 1)) {
                throw failure("the scroll of redirection did not make a Rimmington teleport tablet");
            }
        } finally {
            p().getInventory().clear();
            RewardShopService.resetDaily(p(), "nightmare_zone", "herb_box");
            data.addPoints(p().getUuid(), key, before - data.points(p().getUuid(), key));
        }
        return "15 herb boxes a day; scroll of redirection made a Rimmington tablet";
    }

    private int entry(String label) {
        RewardShop shop = RewardShops.get("barbarian_assault");
        for (int i = 0; i < shop.entries().size(); i++) {
            if (label.equals(shop.entries().get(i).label())) {
                return i;
            }
        }
        throw failure("Commander Connad's shop has no \"" + label + "\"");
    }

    private String baGamble() {
        MinigameData data = MinigameData.of(p());
        String key = MinigameCurrencies.key(MinigameCurrencies.BA_ATTACKER);
        data.addPoints(p().getUuid(), key, 1000 - data.points(p().getUuid(), key));
        data.setStat(p().getUuid(), BarbarianAssaultSession.WAVE_STAT, 0);
        p().getInventory().clear();
        int low = entry(BaGambles.Tier.LOW.displayName() + " (Attacker)");
        RewardShopService.buy(p(), "barbarian_assault", low, 1);
        if (data.points(p().getUuid(), key) != 1000) {
            throw failure("a low gamble went through before reaching wave 3");
        }
        data.setStat(p().getUuid(), BarbarianAssaultSession.WAVE_STAT, 6);
        long coinsBefore = Players.data(p()).coins();
        RewardShopService.buy(p(), "barbarian_assault", low, 1);
        int left = data.points(p().getUuid(), key);
        if (left != 1000 - BaGambles.Tier.LOW.cost()) {
            throw failure("a low gamble left " + left + " Attacker points (expected " + (1000 - BaGambles.Tier.LOW.cost()) + ")");
        }
        // the coins line (1/31) goes to the coin pouch, not an inventory slot
        if (usedSlots() == 0 && Players.data(p()).coins() <= coinsBefore) {
            throw failure("the low gamble handed out nothing");
        }
        p().getInventory().clear();
        return "locked before wave 3; low gamble paid 200 Attacker points and gave a prize";
    }

    private String spoils() {
        p().getInventory().clear();
        hold(SpoilsOfWar.ITEM);
        use();
        if (Inv.count(p(), SpoilsOfWar.ITEM) != 0) {
            throw failure("the spoils of war did not open");
        }
        int slots = usedSlots();
        p().getInventory().clear();
        if (slots == 0) {
            throw failure("the spoils of war contained nothing");
        }
        return "opened into " + slots + " stack(s)";
    }

    private ColosseumSession colosseum() {
        MinigameSession s = MinigameManager.selfTestStart(Minigame.FORTIS_COLOSSEUM, Minigame.FORTIS_COLOSSEUM.entranceBlock(), p());
        if (!(s instanceof ColosseumSession c)) {
            s.end(false);
            throw failure("the Fortis Colosseum did not start a ColosseumSession");
        }
        return c;
    }

    /** Clears the current wave and ticks until the next modifier choice. */
    private void clearWave(ColosseumSession c, int wave) {
        c.clearWaveForTest();
        for (int i = 0; i < 12 && !c.choosing() && !c.isEnded(); i++) {
            MinigameManager.selfTestTick(c);
            c.clearWaveForTest();
            this.c.heal();
        }
        if (!c.choosing()) {
            throw failure("wave " + wave + " did not complete after its monsters died");
        }
    }

    private String colosseumCashOut() {
        ColosseumSession c = colosseum();
        try {
            List<FortisColosseum.Modifier> first = c.offers().stream().map(FortisColosseum.Offer::modifier).toList();
            if (!first.equals(FortisColosseum.FIRST_OFFER)) {
                throw failure("first offers " + first + " (expected Relentless, Blasphemy, Frailty)");
            }
            if (!c.pick(p(), 2)) {
                throw failure("could not pick Blasphemy");
            }
            for (int i = 0; i < 10 && c.wave() == 0; i++) {
                MinigameManager.selfTestTick(c);
            }
            int expected = FortisColosseum.startingMonsters(1, c.modifiers(), new java.util.Random()).size();
            if (c.wave() != 1 || c.monstersAlive() != expected) {
                throw failure("wave " + c.wave() + " with " + c.monstersAlive() + " monsters (expected wave 1 with " + expected + ")");
            }
            clearWave(c, 1);
            if (!c.chest().equals(List.of(new ColosseumRewards.Reward(ColosseumRewards.SPLINTERS, 80)))) {
                throw failure("chest after wave 1: " + c.chest());
            }
            if (c.offers().size() != 3) {
                throw failure(c.offers().size() + " modifiers offered before wave 2");
            }
            int before = Inv.count(p(), ColosseumRewards.SPLINTERS);
            if (!c.cashOut(p())) {
                throw failure("cashing out between waves was refused");
            }
            int got = Inv.count(p(), ColosseumRewards.SPLINTERS) - before;
            if (got != 80) {
                throw failure("cashing out gave " + got + " sunfire splinters (expected 80)");
            }
        } finally {
            c.end(false);
        }
        return "first offer Relentless/Blasphemy/Frailty, wave 1 cleared, 80 splinters claimed";
    }

    private String colosseumWaves() {
        ColosseumSession c = colosseum();
        List<String> seen = new ArrayList<>();
        try {
            for (int wave = 1; wave < FortisColosseum.WAVES; wave++) {
                if (!c.pick(p(), 1)) {
                    throw failure("no modifier to pick before wave " + wave);
                }
                c.startWave();
                if (c.monstersAlive() < FortisColosseum.wave(wave).monsters().size()) {
                    throw failure("wave " + wave + " spawned " + c.monstersAlive() + " of " + FortisColosseum.wave(wave).monsters());
                }
                clearWave(c, wave);
            }
            c.modifiers().forEach((m, t) -> seen.add(m.label(t)));
            if (!c.pick(p(), 1)) {
                throw failure("no modifier to pick before wave 12");
            }
            c.startWave();
            if (c.monstersAlive() != 1) {
                throw failure("wave 12 has " + c.monstersAlive() + " monsters (expected Sol Heredit alone)");
            }
            if (c.wallBlocks() == 0) {
                throw failure("no gladiator wall closed the arena for Sol Heredit");
            }
            if (!c.wallSealed()) {
                throw failure("the gladiator wall leaves a gap a player fits through (ring to ring or ring to pillar)");
            }
            for (int i = 0; i < 5; i++) {
                MinigameManager.selfTestTick(c);
                this.c.heal();
            }
            if (c.chest().isEmpty()) {
                throw failure("the chest is empty after 11 waves");
            }
            // Showboating: salutes to the four sides while Sol Heredit is below 10% hitpoints.
            if (!c.selfTestWoundSol(0.05f)) {
                throw failure("Sol Heredit is not in the arena on wave 12");
            }
            float yaw = p().getYaw();
            for (float y : new float[]{180f, 270f, 0f, 90f}) {
                p().setYaw(y);
                c.saluteIn(p());
            }
            p().setYaw(yaw);
            if (c.salutes().size() != 4) {
                throw failure("salutes counted " + c.salutes() + " (expected north, east, south and west)");
            }
        } finally {
            c.end(false);
        }
        if (c.wallBlocks() != 0) {
            throw failure("the gladiator wall is still standing after the run ended");
        }
        return "11 waves cleared, Sol Heredit spawned on wave 12 behind the gladiator wall, four salutes counted; modifiers "
                + String.join(", ", seen);
    }

    /** Glory unlocks (bank chest, respawn point, ring of dueling) and the daily Varlamore token from wave 3. */
    private String colosseumGlory() {
        MinigameData data = MinigameData.of(p());
        java.util.UUID u = p().getUuid();
        int glory = data.stat(u, FortisColosseum.STAT_GLORY);
        var dim = p().getSpawnPointDimension();
        net.minecraft.util.math.BlockPos spawn = p().getSpawnPointPosition();
        float angle = p().getSpawnAngle();
        boolean forced = p().isSpawnForced();
        try {
            data.setStat(u, FortisColosseum.STAT_GLORY, FortisColosseum.GLORY_DUELING - 1);
            if (ColosseumSession.duelingGate(p()) == null) {
                throw failure("the ring of dueling teleport was allowed below 12,000 Glory");
            }
            data.setStat(u, FortisColosseum.STAT_GLORY, FortisColosseum.GLORY_DUELING);
            if (ColosseumSession.duelingGate(p()) != null) {
                throw failure("the ring of dueling teleport was refused at 12,000 Glory");
            }
            data.setStat(u, FortisColosseum.STAT_GLORY, 0);
            if (ColosseumSession.setRespawn(p())) {
                throw failure("Civitas illa Fortis became the respawn point with no Glory");
            }
            data.setStat(u, FortisColosseum.STAT_GLORY, FortisColosseum.GLORY_RESPAWN);
            if (!ColosseumSession.setRespawn(p()) || p().getSpawnPointDimension() != com.runecraft.world.Gielinor.WORLD) {
                throw failure("Civitas illa Fortis did not become the respawn point at 5,000 Glory");
            }
            Inv.remove(p(), FortisColosseum.TOKEN, Inv.count(p(), FortisColosseum.TOKEN));
            data.setStat(u, ColosseumSession.STAT_TOKEN_DAY, 0);
            java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
            boolean eligible = FortisColosseum.tokenActivity(p().getName().getString().length(), today.getDayOfWeek().getValue())
                    .equals("colosseum");
            if (ColosseumSession.varlamoreToken(p()) != eligible || Inv.has(p(), FortisColosseum.TOKEN, 1) != eligible) {
                throw failure("the wave 3 token " + (eligible ? "was not given" : "was given on a day this name can't get it"));
            }
            if (ColosseumSession.varlamoreToken(p())) {
                throw failure("a second token was given the same day");
            }
            Inv.remove(p(), FortisColosseum.TOKEN, Inv.count(p(), FortisColosseum.TOKEN));
            return "dueling teleport at 12,000, respawn point at 5,000, token " + (eligible ? "given once" : "not due today");
        } finally {
            data.setStat(u, FortisColosseum.STAT_GLORY, glory);
            p().setSpawnPoint(dim, spawn, angle, forced, false);
        }
    }

    /** A blighted entangle sack pays for Entangle (one sack, no runes) in the Wilderness only, and not for other spells. */
    private String blightedSacks() {
        if (com.runecraft.world.Gielinor.wildernessLevel(p()) > 0) {
            throw new SelfTest.Skip("the test platform is in the Wilderness");
        }
        com.runecraft.osrs.content.systems.Spellbooks.Spell entangle = com.runecraft.osrs.content.systems.Spellbooks.get("entangle");
        com.runecraft.osrs.content.systems.Spellbooks.Spell barrage = com.runecraft.osrs.content.systems.Spellbooks.get("ice_barrage");
        if (entangle == null || barrage == null) {
            throw failure("Entangle or Ice Barrage is missing from the spellbooks");
        }
        String sack = com.runecraft.osrs.data.BlightedSacks.ENTANGLE;
        int had = Inv.count(p(), sack);
        Inv.give(p(), sack, 1);
        double x = p().getX();
        double y = p().getY();
        double z = p().getZ();
        try {
            if (com.runecraft.content.systems.SpellService.blightedPlan(p(), entangle) != null) {
                throw failure("the entangle sack paid for Entangle outside the Wilderness");
            }
            // Stand in level 23 Wilderness (OSRS tile 3100, 3700) for the lookups only.
            p().setPosition(com.runecraft.osrs.GielinorMap.toBlockX(3100) + 0.5, y, com.runecraft.osrs.GielinorMap.toBlockZ(3700) + 0.5);
            if (com.runecraft.world.Gielinor.wildernessLevel(p()) <= 0) {
                throw new SelfTest.Skip("the Wilderness lookup position isn't in the Wilderness");
            }
            java.util.Map<String, Integer> plan = com.runecraft.content.systems.SpellService.blightedPlan(p(), entangle);
            if (plan == null || !plan.equals(java.util.Map.of(sack, 1))) {
                throw failure("the entangle sack didn't pay for Entangle in the Wilderness: " + plan);
            }
            if (com.runecraft.content.systems.SpellService.blightedPlan(p(), barrage) != null) {
                throw failure("the entangle sack paid for Ice Barrage");
            }
        } finally {
            p().setPosition(x, y, z);
            int extra = Inv.count(p(), sack) - had;
            if (extra > 0) {
                Inv.remove(p(), sack, extra);
            }
        }
        return "the entangle sack pays for Entangle in the Wilderness only";
    }

    /**
     * A ranked Emir's Arena fighter plays the week's account build: the 1 Def Pure's levels and kit replace the player's own
     * (which wait in the LMS stash and come back), and with its 52 Prayer the soul cape (70 Prayer) can't be worn.
     */
    private String emirsBuild() {
        if (com.runecraft.content.minigames.LmsStash.has(p())) {
            com.runecraft.content.minigames.LmsStash.restore(p());
        }
        SkillSet skills = Players.data(p()).skills();
        int defenceXp = skills.xpTenths(Skill.DEFENCE);
        int compost = Inv.count(p(), "compost");
        Inv.give(p(), "compost", 1);
        try {
            com.runecraft.content.minigames.games.EmirsArenaSession.equipBuild(p(),
                    com.runecraft.osrs.content.minigames.EmirsArena.Build.PURE);
            if (skills.level(Skill.ATTACK) != 60 || skills.level(Skill.DEFENCE) != 1 || skills.level(Skill.PRAYER) != 52
                    || skills.level(Skill.STRENGTH) != 99) {
                throw failure("the Pure build's levels weren't applied: attack " + skills.level(Skill.ATTACK) + ", defence "
                        + skills.level(Skill.DEFENCE) + ", prayer " + skills.level(Skill.PRAYER));
            }
            if (Inv.count(p(), "compost") != 0) {
                throw failure("the player's own items weren't put away");
            }
            if (!Inv.idOf(p().getMainHandStack()).equals("lms_ancient_staff") && Inv.count(p(), "lms_ancient_staff") == 0) {
                throw failure("the Pure kit's ancient staff is missing");
            }
            hold("soul_cape");
            com.runecraft.content.systems.JewelleryService.wear(p(), Hand.MAIN_HAND);
            if (Inv.idOf(com.runecraft.content.systems.SysState.of(p()).worn(com.runecraft.osrs.content.systems.Jewellery.Slot.CAPE))
                    .equals("soul_cape")) {
                throw failure("a soul cape was worn with 52 Prayer");
            }
        } finally {
            com.runecraft.content.minigames.LmsStash.restore(p());
        }
        if (skills.xpTenths(Skill.DEFENCE) != defenceXp || Inv.count(p(), "compost") != compost + 1) {
            throw failure("the player's levels and items didn't come back");
        }
        Inv.remove(p(), "compost", 1);
        return "Pure build 60/99/1 with 52 Prayer and the ghostly kit; soul cape refused; own levels and items restored";
    }

    private String blighted() {
        if (com.runecraft.world.Gielinor.wildernessLevel(p()) > 0) {
            throw new SelfTest.Skip("the test platform is in the Wilderness");
        }
        for (String id : List.of("blighted_karambwan", "blighted_super_restore")) {
            ItemStack s = hold(id);
            use();
            if (p().getMainHandStack() != s || s.getCount() != 1 || s.getDamage() != 0) {
                throw failure(id + " was used outside the Wilderness");
            }
        }
        return "refused outside the Wilderness";
    }

    /**
     * The Gauntlet: entering stores the player's belongings and hands out the sceptre, the singing bowl sings a basic helm
     * (and the Combat Achievement flags follow), the barrier starts the Hunllef fight, and leaving gives everything back
     * without the Gauntlet's items.
     */
    private String gauntlet() {
        com.runecraft.osrs.content.minigames.Gauntlet.Mode mode = com.runecraft.osrs.content.minigames.Gauntlet.Mode.NORMAL;
        // the generic minigame:gauntlet run can leave the fake player stashed (it is never "online" to be restored)
        com.runecraft.content.minigames.GauntletStash.restore(p());
        Inv.give(p(), "bronze_dagger", 1);
        MinigameSession s = MinigameManager.selfTestStart(Minigame.GAUNTLET, Minigame.GAUNTLET.entranceBlock(), p());
        if (!(s instanceof com.runecraft.content.minigames.games.GauntletSession g)) {
            s.end(false);
            throw failure("the Gauntlet did not start a GauntletSession");
        }
        try {
            if (Inv.has(p(), "bronze_dagger", 1)) {
                throw failure("the player's items were not stored on entry");
            }
            if (!Inv.has(p(), com.runecraft.osrs.content.minigames.Gauntlet.sceptre(mode), 1)) {
                throw failure("no crystal sceptre on entry");
            }
            Inv.give(p(), com.runecraft.osrs.content.minigames.Gauntlet.shards(mode), 50);
            Inv.give(p(), com.runecraft.osrs.content.minigames.Gauntlet.ore(mode), 1);
            Inv.give(p(), com.runecraft.osrs.content.minigames.Gauntlet.bark(mode), 1);
            Inv.give(p(), com.runecraft.osrs.content.minigames.Gauntlet.linum(mode), 1);
            if (!g.singRecipe(p(), "helm_basic") || !Inv.has(p(), "crystal_helm_basic", 1)) {
                throw failure("singing a basic crystal helm failed");
            }
            Inv.give(p(), com.runecraft.osrs.content.minigames.Gauntlet.shards(mode), 200);
            if (!g.singRecipe(p(), "escape") || !Inv.has(p(), com.runecraft.osrs.content.minigames.Gauntlet.ESCAPE_CRYSTAL, 1)) {
                throw failure("singing an escape crystal from 200 shards failed");
            }
            Set<String> flags = g.caFlags(p());
            if (flags.contains("noarmour") || !flags.contains("noegniol") || !flags.contains("oneweapon")) {
                throw failure("wrong Combat Achievement flags " + flags);
            }
            // Entry hands out the crystal tools and the pestle and mortar; egniol: grym leaf + water vial, ground shards.
            for (String item : com.runecraft.osrs.content.minigames.Gauntlet.startingItems(mode)) {
                if (!Inv.has(p(), item, 1)) {
                    throw failure("entering the Gauntlet didn't hand out " + item);
                }
            }
            Inv.give(p(), com.runecraft.osrs.content.minigames.Gauntlet.grym(mode), 1);
            Inv.give(p(), com.runecraft.osrs.content.minigames.Gauntlet.WATER_VIAL, 1);
            Inv.give(p(), com.runecraft.osrs.content.minigames.Gauntlet.shards(mode), 10);
            if (!com.runecraft.content.minigames.games.GauntletSession.mixGrym(p())
                    || com.runecraft.content.minigames.games.GauntletSession.grind(p()) != 10
                    || !com.runecraft.content.minigames.games.GauntletSession.finishEgniol(p())
                    || !Inv.has(p(), com.runecraft.osrs.content.minigames.Gauntlet.EGNIOL, 1)) {
                throw failure("grym leaf + water vial + 10 ground shards didn't make an egniol potion");
            }
            if (g.caFlags(p()).contains("noegniol")) {
                throw failure("making an egniol potion left the no-egniol flag set");
            }
            g.selfTestEnterBoss(p());
            if (!g.inBossFight() || !g.selfTestHunllefAlive()) {
                throw failure("the barrier did not start the Hunllef fight");
            }
            for (int i = 0; i < 5 && !s.isEnded(); i++) {
                MinigameManager.selfTestTick(s);
                c.heal();
            }
        } finally {
            s.end(false);
        }
        if (MinigameManager.session(p()) != null) {
            throw failure("the player is still bound to the Gauntlet after it ended");
        }
        if (com.runecraft.content.minigames.GauntletStash.has(p())) {
            // A player missing from the server's player list (the fake player) is treated like one who logged out mid-run:
            // the session can't reach them, and their stash comes back as it does on their next login.
            if (c.realPlayer()) {
                throw failure("the online player's stash was not restored when the run ended");
            }
            com.runecraft.content.minigames.GauntletStash.restore(p());
        }
        if (!Inv.has(p(), "bronze_dagger", 1)) {
            throw failure("the player's items were not given back");
        }
        if (Inv.has(p(), "crystal_helm_basic", 1) || Inv.has(p(), com.runecraft.osrs.content.minigames.Gauntlet.sceptre(mode), 1)
                || Inv.has(p(), com.runecraft.osrs.content.minigames.Gauntlet.ESCAPE_CRYSTAL, 1)) {
            throw failure("Gauntlet items left the Gauntlet");
        }
        Inv.remove(p(), "bronze_dagger", 1);
        return "sang a helm, Hunllef fight started, belongings restored";
    }
}
