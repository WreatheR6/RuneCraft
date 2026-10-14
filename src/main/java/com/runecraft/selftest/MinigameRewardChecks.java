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
        checks.add(new SelfTest.Check("minigame:spoils_of_war", g::spoils));
        checks.add(new SelfTest.Check("minigame:blighted_supplies", g::blighted));
        checks.add(new SelfTest.Check("minigame:gauntlet_run", g::gauntlet));
        checks.add(new SelfTest.Check("minigame:colosseum_cash_out", g::colosseumCashOut));
        checks.add(new SelfTest.Check("minigame:colosseum_waves", g::colosseumWaves));
        return checks;
    }

    private ServerPlayerEntity p() {
        return c.player;
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
            for (int i = 0; i < 5; i++) {
                MinigameManager.selfTestTick(c);
                this.c.heal();
            }
            if (c.chest().isEmpty()) {
                throw failure("the chest is empty after 11 waves");
            }
        } finally {
            c.end(false);
        }
        return "11 waves cleared, Sol Heredit spawned on wave 12; modifiers " + String.join(", ", seen);
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
            Set<String> flags = g.caFlags(p());
            if (flags.contains("noarmour") || !flags.contains("noegniol") || !flags.contains("oneweapon")) {
                throw failure("wrong Combat Achievement flags " + flags);
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
        if (Inv.has(p(), "crystal_helm_basic", 1) || Inv.has(p(), com.runecraft.osrs.content.minigames.Gauntlet.sceptre(mode), 1)) {
            throw failure("Gauntlet items left the Gauntlet");
        }
        Inv.remove(p(), "bronze_dagger", 1);
        return "sang a helm, Hunllef fight started, belongings restored";
    }
}
