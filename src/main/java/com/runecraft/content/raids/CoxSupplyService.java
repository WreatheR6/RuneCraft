package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.raids.CoxSupplies;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chambers of Xeric raid herblore (OSRS): cleaning the raid herbs, brewing potions (right-click a secondary, or a
 * noxifer for an overload) and drinking them. The potions only work inside the Chambers (see {@link CoxSupplies}).
 */
public final class CoxSupplyService {
    private CoxSupplyService() {
    }

    private static final Skill[] COMBAT = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};
    /** Game tick each player's overload wears off, and its strength. */
    private static final Map<UUID, Long> OVERLOAD_UNTIL = new HashMap<>();
    private static final Map<UUID, CoxSupplies.Strength> OVERLOAD_STRENGTH = new HashMap<>();

    static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !(player instanceof ServerPlayer sp)) {
                return InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (id == null || !CoxSupplies.ITEMS.contains(id)) {
                return InteractionResultHolder.pass(stack);
            }
            CoxSupplies.Herb grimy = CoxSupplies.Herb.byGrimy(id);
            CoxSupplies.Brew brew = CoxSupplies.brew(id);
            boolean secondary = id.equals(CoxSupplies.STINKHORN) || id.equals(CoxSupplies.ENDARKENED) || id.equals(CoxSupplies.CICELY)
                    || id.equals(CoxSupplies.Herb.NOXIFER.clean);
            if (grimy == null && brew == null && !secondary) {
                return InteractionResultHolder.pass(stack);
            }
            RaidInstance raid = RaidManager.of(sp);
            if (raid == null || raid.type != RaidType.COX) {
                Msg.game(sp, "This can only be used inside the Chambers of Xeric.");
                return InteractionResultHolder.success(stack);
            }
            if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                return InteractionResultHolder.success(stack);
            }
            if (grimy != null) {
                clean(sp, grimy);
            } else if (brew != null) {
                drink(sp, stack, hand, brew);
            } else {
                mix(sp, id);
            }
            return InteractionResultHolder.success(stack);
        });
    }

    static void forget(UUID u) {
        OVERLOAD_UNTIL.remove(u);
        OVERLOAD_STRENGTH.remove(u);
    }

    /**
     * In-game self-test (the player must be in a CoX raid): cleans golpar, brews elder/twisted/kodai (+), mixes an
     * overload (+) and drinks an elder (+), checking each result; restores the player's Herblore XP, boosts and hand.
     * Returns a summary, or throws an {@link AssertionError}.
     */
    public static String selfTest(ServerPlayer p) {
        if (!inCox(p)) {
            throw new AssertionError("the player is not in a Chambers of Xeric raid");
        }
        SkillSet skills = Players.data(p).skills();
        int herbloreTenths = skills.xpTenths(Skill.HERBLORE);
        ItemStack hand = p.getMainHandItem().copy();
        try {
            skills.setXpTenths(Skill.HERBLORE, com.runecraft.osrs.Experience.xpForLevel(99) * 10);
            Inv.give(p, CoxSupplies.Herb.GOLPAR.grimy(), 3);
            clean(p, CoxSupplies.Herb.GOLPAR);
            expect(p, CoxSupplies.Herb.GOLPAR.clean, 3, "cleaning grimy golpar");
            Inv.give(p, CoxSupplies.WATER_GOURD, 3);
            for (String secondary : new String[]{CoxSupplies.STINKHORN, CoxSupplies.CICELY, CoxSupplies.ENDARKENED}) {
                Inv.give(p, secondary, 1);
                mix(p, secondary);
            }
            CoxSupplies.Strength strong = CoxSupplies.Strength.STRONG;
            for (CoxSupplies.Potion potion : new CoxSupplies.Potion[]{CoxSupplies.Potion.ELDER, CoxSupplies.Potion.TWISTED, CoxSupplies.Potion.KODAI}) {
                expect(p, potion.id(strong), 1, "brewing " + potion.displayName(strong));
            }
            Inv.give(p, CoxSupplies.Herb.NOXIFER.clean, 1);
            mix(p, CoxSupplies.Herb.NOXIFER.clean);
            expect(p, CoxSupplies.Potion.OVERLOAD.id(strong), 1, "mixing an overload (+)");
            expect(p, CoxSupplies.Potion.ELDER.id(strong), 0, "the overload used the elder (+)");
            String elder = CoxSupplies.Potion.ELDER.id(strong);
            ItemStack dose = Inv.stack(elder, 1);
            p.setItemInHand(InteractionHand.MAIN_HAND, dose);
            p.getCooldowns().removeCooldown(dose.getItem());
            skills.setBoost(Skill.ATTACK, 0);
            drink(p, dose, InteractionHand.MAIN_HAND, CoxSupplies.brew(elder));
            int want = CoxSupplies.combatBoost(strong, skills.level(Skill.ATTACK));
            if (skills.boost(Skill.ATTACK) != want) {
                throw new AssertionError("elder (+) boosted Attack by " + skills.boost(Skill.ATTACK) + ", expected " + want);
            }
            if (p.getMainHandItem().getDamageValue() != 1) {
                throw new AssertionError("drinking did not use a dose (damage " + p.getMainHandItem().getDamageValue() + ")");
            }
            return "cleaned, brewed elder/twisted/kodai (+), mixed an overload (+), elder (+) Attack +" + want;
        } finally {
            for (String id : CoxSupplies.ITEMS) {
                Inv.remove(p, id, Inv.count(p, id));
            }
            skills.setXpTenths(Skill.HERBLORE, herbloreTenths);
            for (Skill s : COMBAT) {
                skills.setBoost(s, 0);
            }
            p.setItemInHand(InteractionHand.MAIN_HAND, hand);
            ServerNetworking.sync(p);
        }
    }

    private static void expect(ServerPlayer p, String id, int n, String what) {
        int have = Inv.count(p, id);
        if (have != n) {
            throw new AssertionError(what + ": " + have + " x " + id + " (expected " + n + ")");
        }
    }

    private static int level(ServerPlayer p, Skill s) {
        return Players.data(p).skills().level(s);
    }

    // ------------------------------------------------------------------ herblore

    /** Cleans every grimy herb of this kind the player carries (Herblore 47/52/60, 1/2/3 XP each). */
    static void clean(ServerPlayer p, CoxSupplies.Herb herb) {
        if (level(p, Skill.HERBLORE) < herb.cleanLevel) {
            Msg.game(p, "You need a Herblore level of " + herb.cleanLevel + " to clean this herb.");
            return;
        }
        int n = Inv.count(p, herb.grimy());
        if (n <= 0) {
            return;
        }
        Inv.remove(p, herb.grimy(), n);
        Inv.give(p, herb.clean, n);
        XpService.add(p, Skill.HERBLORE, herb.cleanXp * n);
        Msg.action(p, "You clean " + n + " " + herb.name.toLowerCase(java.util.Locale.ROOT) + (n > 1 ? "s." : "."));
        ServerNetworking.sync(p);
    }

    /**
     * Brews with the secondary just used: golpar or buchu leaf (the one in the off-hand first) into a water-filled gourd
     * vial; a noxifer mixes an elder, a twisted and a kodai potion (any strengths) into an overload. The strength made
     * follows the Herblore level (and, for an overload, the weakest of the three potions).
     */
    static void mix(ServerPlayer p, String secondary) {
        int herblore = level(p, Skill.HERBLORE);
        if (secondary.equals(CoxSupplies.Herb.NOXIFER.clean)) {
            CoxSupplies.Strength s = CoxSupplies.Potion.OVERLOAD.strengthFor(herblore);
            if (s == null) {
                Msg.game(p, "You need a Herblore level of " + CoxSupplies.Potion.OVERLOAD.level(CoxSupplies.Strength.WEAK) + " to make overloads.");
                return;
            }
            // OSRS: any variant of each potion works; the overload is capped by the Herblore level and the weakest potion.
            CoxSupplies.Potion[] parts = {CoxSupplies.Potion.ELDER, CoxSupplies.Potion.TWISTED, CoxSupplies.Potion.KODAI};
            CoxSupplies.Strength t = s;
            for (CoxSupplies.Potion part : parts) {
                CoxSupplies.Strength best = null;
                for (CoxSupplies.Strength x : CoxSupplies.Strength.values()) {
                    if (Inv.has(p, part.id(x), 1)) {
                        best = x;
                    }
                }
                if (best == null) {
                    Msg.game(p, "You need an elder, a twisted and a kodai potion to make an overload.");
                    return;
                }
                if (best.ordinal() < t.ordinal()) {
                    t = best;
                }
            }
            // Use the weakest of each potion that still makes this strength, keeping any stronger ones.
            for (CoxSupplies.Potion part : parts) {
                for (int i = t.ordinal(); i < CoxSupplies.Strength.values().length; i++) {
                    String id = part.id(CoxSupplies.Strength.values()[i]);
                    if (Inv.has(p, id, 1)) {
                        Inv.remove(p, id, 1);
                        break;
                    }
                }
            }
            Inv.remove(p, CoxSupplies.Herb.NOXIFER.clean, 1);
            Inv.give(p, CoxSupplies.Potion.OVERLOAD.id(t), 1);
            XpService.add(p, Skill.HERBLORE, CoxSupplies.Potion.OVERLOAD.xp(t));
            Msg.action(p, "You mix the potions with the noxifer into an " + CoxSupplies.Potion.OVERLOAD.displayName(t).toLowerCase(java.util.Locale.ROOT) + ".");
            finish(p);
            return;
        }
        String offhand = Inv.idOf(p.getOffhandItem());
        CoxSupplies.Herb herb = CoxSupplies.Herb.byClean(offhand);
        if (herb == null || herb == CoxSupplies.Herb.NOXIFER) {
            herb = Inv.has(p, CoxSupplies.Herb.GOLPAR.clean, 1) ? CoxSupplies.Herb.GOLPAR
                    : Inv.has(p, CoxSupplies.Herb.BUCHU.clean, 1) ? CoxSupplies.Herb.BUCHU : null;
        }
        if (herb == null) {
            Msg.game(p, "You need a golpar or a buchu leaf to brew with this.");
            return;
        }
        CoxSupplies.Potion potion = CoxSupplies.potionOf(herb, secondary);
        CoxSupplies.Strength s = potion == null ? null : potion.strengthFor(herblore);
        if (s == null) {
            Msg.game(p, "You need a Herblore level of " + (potion == null ? 47 : potion.level(CoxSupplies.Strength.WEAK)) + " to brew that.");
            return;
        }
        if (!Inv.has(p, CoxSupplies.WATER_GOURD, 1)) {
            Msg.game(p, "You need a water-filled gourd vial. Fill an empty one at the geyser.");
            return;
        }
        Inv.remove(p, CoxSupplies.WATER_GOURD, 1);
        Inv.remove(p, herb.clean, 1);
        Inv.remove(p, secondary, 1);
        Inv.give(p, potion.id(s), 1);
        XpService.add(p, Skill.HERBLORE, potion.xp(s));
        Msg.action(p, "You brew " + aOrAn(potion.displayName(s)) + ".");
        finish(p);
    }

    private static String aOrAn(String name) {
        String lower = name.toLowerCase(java.util.Locale.ROOT);
        return ("aeiou".indexOf(lower.charAt(0)) >= 0 ? "an " : "a ") + lower;
    }

    private static void finish(ServerPlayer p) {
        p.level().playSound(null, p.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 0.5f, 1.0f);
        ServerNetworking.sync(p);
    }

    // ------------------------------------------------------------------ drinking

    static void drink(ServerPlayer p, ItemStack stack, InteractionHand hand, CoxSupplies.Brew brew) {
        PlayerData data = Players.data(p);
        SkillSet skills = data.skills();
        CoxSupplies.Strength s = brew.strength();
        switch (brew.potion()) {
            case ELDER, TWISTED, KODAI -> {
                for (Skill skill : CoxSupplies.boosted(brew.potion())) {
                    boost(skills, skill, CoxSupplies.combatBoost(s, skills.level(skill)));
                }
                Msg.action(p, "You drink some of your " + brew.potion().displayName(s).toLowerCase(java.util.Locale.ROOT) + ".");
            }
            case OVERLOAD -> {
                if (OVERLOAD_UNTIL.getOrDefault(p.getUUID(), 0L) > ActionManager.gameTick()) {
                    Msg.game(p, "You are already under the effects of an overload.");
                    return;
                }
                if (hitpoints(p) <= CoxSupplies.OVERLOAD_DAMAGE) {
                    Msg.game(p, "You need more than " + CoxSupplies.OVERLOAD_DAMAGE + " hitpoints to survive the overload.");
                    return;
                }
                overload(p, s);
                Msg.action(p, "You drink some of your overload. You feel ready to take on anything.");
            }
            case XERICS_AID -> {
                int hp = skills.level(Skill.HITPOINTS);
                int cap = hp + CoxSupplies.xericsAidHeal(s, hp);
                setHitpoints(p, Math.min(cap, hitpoints(p) + CoxSupplies.xericsAidHeal(s, hp)));
                boost(skills, Skill.DEFENCE, CoxSupplies.xericsAidDefence(s, skills.level(Skill.DEFENCE)));
                for (Skill skill : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.RANGED, Skill.MAGIC}) {
                    skills.setBoost(skill, skills.boost(skill) - CoxSupplies.xericsAidDrain(s, skills.currentLevel(skill)));
                }
                Msg.action(p, "You drink some of your Xeric's aid.");
            }
            case REVITALISATION -> {
                for (Skill skill : Skill.values()) {
                    if (skill != Skill.HITPOINTS && skill != Skill.PRAYER && skills.boost(skill) < 0) {
                        skills.setBoost(skill, Math.min(0, skills.boost(skill) + CoxSupplies.revitalisationRestore(s, skills.level(skill))));
                    }
                }
                int prayerLevel = skills.level(Skill.PRAYER);
                data.setPrayerPoints(Math.min(prayerLevel, data.prayerPoints() + CoxSupplies.revitalisationRestore(s, prayerLevel)));
                Msg.action(p, "You drink some of your revitalisation potion.");
            }
            case PRAYER_ENHANCE -> {
                int prayerLevel = skills.level(Skill.PRAYER);
                pulse(p, CoxSupplies.prayerEnhanceInterval(s, prayerLevel), CoxSupplies.prayerEnhanceTimes(s, prayerLevel), () -> {
                    PlayerData d = Players.data(p);
                    d.setPrayerPoints(Math.min(d.skills().level(Skill.PRAYER), d.prayerPoints() + 1));
                });
                Msg.action(p, "You drink some of your prayer enhance. You feel your prayer slowly restoring.");
            }
        }
        int used = stack.getDamageValue() + 1;
        if (used >= 4) {
            p.setItemInHand(hand, ItemStack.EMPTY);
        } else {
            stack.setDamageValue(used);
        }
        p.getCooldowns().addCooldown(stack.getItem(), 3 * ActionManager.MC_TICKS_PER_GAME_TICK);
        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        ServerNetworking.sync(p);
    }

    /** Raises a boost to at least {@code amount} (potions never stack, OSRS). */
    private static void boost(SkillSet skills, Skill skill, int amount) {
        if (skills.boost(skill) < amount) {
            skills.setBoost(skill, amount);
        }
    }

    /**
     * Overload (OSRS): 50 hitpoints of damage in five hits of 10, the combat boosts re-applied every 15 seconds for five
     * minutes, then 50 hitpoints healed. It ends when the player leaves the Chambers.
     */
    static void overload(ServerPlayer p, CoxSupplies.Strength s) {
        UUID u = p.getUUID();
        long until = ActionManager.gameTick() + CoxSupplies.OVERLOAD_TICKS;
        OVERLOAD_UNTIL.put(u, until);
        OVERLOAD_STRENGTH.put(u, s);
        for (int i = 0; i < CoxSupplies.OVERLOAD_DAMAGE / CoxSupplies.OVERLOAD_HIT; i++) {
            RaidEffects.later(1 + 2 * i, () -> {
                if (p.isAlive() && inCox(p)) {
                    setHitpoints(p, Math.max(1, hitpoints(p) - CoxSupplies.OVERLOAD_HIT));
                    ServerNetworking.sync(p);
                }
            });
        }
        overloadBoost(p);
        pulse(p, CoxSupplies.OVERLOAD_REAPPLY_TICKS, CoxSupplies.OVERLOAD_TICKS / CoxSupplies.OVERLOAD_REAPPLY_TICKS - 1, () -> overloadBoost(p));
        RaidEffects.later(CoxSupplies.OVERLOAD_TICKS, () -> {
            if (OVERLOAD_UNTIL.getOrDefault(u, 0L) != until) {
                return;
            }
            forget(u);
            if (p.isAlive() && inCox(p)) {
                int max = Players.data(p).skills().level(Skill.HITPOINTS);
                setHitpoints(p, Math.min(max, hitpoints(p) + CoxSupplies.OVERLOAD_DAMAGE));
                Msg.game(p, "The effects of overload have worn off, and you feel normal again.");
                ServerNetworking.sync(p);
            }
        });
    }

    private static void overloadBoost(ServerPlayer p) {
        CoxSupplies.Strength s = OVERLOAD_STRENGTH.get(p.getUUID());
        if (s == null) {
            return;
        }
        SkillSet skills = Players.data(p).skills();
        for (Skill skill : COMBAT) {
            boost(skills, skill, CoxSupplies.combatBoost(s, skills.level(skill)));
        }
        ServerNetworking.sync(p);
    }

    private static boolean inCox(ServerPlayer p) {
        RaidInstance raid = RaidManager.of(p);
        return raid != null && raid.type == RaidType.COX;
    }

    /** Runs {@code effect} every {@code every} game ticks, {@code times} times, while the player stays in the Chambers. */
    private static void pulse(ServerPlayer p, int every, int times, Runnable effect) {
        if (times <= 0) {
            return;
        }
        RaidEffects.later(every, () -> {
            if (!p.isAlive() || p.isRemoved() || !inCox(p)) {
                return;
            }
            effect.run();
            ServerNetworking.sync(p);
            pulse(p, every, times - 1, effect);
        });
    }

    private static int hitpoints(ServerPlayer p) {
        return com.runecraft.combat.Overheal.hitpoints(p);
    }

    /** Sets hitpoints, holding anything above the maximum as overheal (Xeric's aid heals above the maximum). */
    private static void setHitpoints(ServerPlayer p, int total) {
        float max = p.getMaxHealth();
        p.setHealth(Math.min(max, Math.max(1, total)));
        p.setAbsorptionAmount(Math.max(0, total - max));
    }
}
