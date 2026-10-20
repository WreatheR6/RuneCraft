package com.runecraft.content.raids;

import com.runecraft.action.ActionManager;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.ToaSupplies;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tombs of Amascut supplies (OSRS): the Helpful Spirit's bundles after the second and fourth path, the honey
 * locusts handed out after a wipe, and the effects of each supply item (they only work inside the raid; see
 * {@link ToaSupplies} for the numbers).
 */
public final class ToaSupplyService {
    private ToaSupplyService() {
    }

    private static final Skill[] COMBAT = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC};
    /** Players with an unclaimed Helpful Spirit offer. */
    private static final Set<UUID> PENDING = new HashSet<>();
    /** Game tick each player's liquid adrenaline wears off. */
    private static final Map<UUID, Long> ADRENALINE = new HashMap<>();

    static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !ToaSupplies.ITEMS.contains(Inv.idOf(stack))) {
                return InteractionResultHolder.pass(stack);
            }
            if (!(player instanceof ServerPlayer sp)) {
                return InteractionResultHolder.pass(stack);
            }
            RaidInstance raid = RaidManager.of(sp);
            String id = Inv.idOf(stack);
            if (raid == null || raid.type != RaidType.TOA) {
                if (id.equals(ToaSupplies.HONEY_LOCUST)) {
                    return InteractionResultHolder.pass(stack);
                }
                Msg.game(sp, "This can only be used inside the Tombs of Amascut.");
                return InteractionResultHolder.success(stack);
            }
            if (player.getCooldowns().isOnCooldown(stack.getItem())) {
                return InteractionResultHolder.success(stack);
            }
            use(sp, raid, stack, hand, id);
            return InteractionResultHolder.success(stack);
        });
    }

    /**
     * Icthlarin's shroud's "Change" option ({@code /raids shroud}, OSRS): turns the shroud held, carried or worn in the
     * cape slot into the next tier the player's Normal/Expert completions unlock. Returns 1 when it changed.
     */
    static int changeShroud(ServerPlayer p) {
        RaidsState state = RaidsState.get(p.getServer());
        int completions = state.completions(p.getUUID(), com.runecraft.osrs.content.raids.ToaLoot.modeKey(150))
                + state.completions(p.getUUID(), com.runecraft.osrs.content.raids.ToaLoot.modeKey(300));
        return changeShroud(p, completions);
    }

    /** {@link #changeShroud(ServerPlayer)} for this many Normal/Expert completions (self-test). */
    static int changeShroud(ServerPlayer p, int completions) {
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            String next = s.isEmpty() ? null : com.runecraft.osrs.content.raids.ToaLoot.shroudChange(Inv.idOf(s), completions);
            if (next != null) {
                inv.setItem(i, Inv.stack(next, 1));
                Msg.game(p, "You change your shroud's appearance: " + nameOf(next) + ".");
                return 1;
            }
        }
        var sys = com.runecraft.content.systems.SysState.of(p);
        ItemStack cape = sys.worn.get(com.runecraft.osrs.content.systems.Jewellery.Slot.CAPE);
        String next = cape == null || cape.isEmpty() ? null : com.runecraft.osrs.content.raids.ToaLoot.shroudChange(Inv.idOf(cape), completions);
        if (next != null) {
            sys.worn.put(com.runecraft.osrs.content.systems.Jewellery.Slot.CAPE, Inv.stack(next, 1));
            com.runecraft.content.systems.SysNet.sendState(p);
            Msg.game(p, "You change your shroud's appearance: " + nameOf(next) + ".");
            return 1;
        }
        Msg.game(p, com.runecraft.osrs.content.raids.ToaLoot.shroudTier(completions) <= 1
                ? "You haven't unlocked another tier of Icthlarin's shroud yet."
                : "You need to carry or wear Icthlarin's shroud to change it.");
        return 0;
    }

    private static String nameOf(String item) {
        var def = com.runecraft.osrs.ItemCatalog.get(item);
        return def == null ? item : def.name();
    }

    /** Whether this player's liquid adrenaline is active. */
    static boolean adrenaline(ServerPlayer p) {
        Long until = ADRENALINE.get(p.getUUID());
        return until != null && until > RaidEffects.now();
    }

    // ------------------------------------------------------------------ the Helpful Spirit

    /** The Helpful Spirit appears in the Nexus with three bundles; each player may claim one (OSRS). */
    static void offer(RaidInstance raid) {
        for (ServerPlayer p : raid.players()) {
            PENDING.add(p.getUUID());
            showOffer(p);
        }
    }

    private static void showOffer(ServerPlayer p) {
        p.displayClientMessage(Component.literal("The Helpful Spirit in the Nexus offers you supplies. Choose one bundle:")
                .withStyle(ChatFormatting.AQUA), false);
        MutableComponent line = Component.literal("");
        for (ToaSupplies.Bundle b : ToaSupplies.Bundle.values()) {
            line.append(Component.literal("[" + b.displayName() + "] ").withStyle(s -> s.withColor(ChatFormatting.GREEN)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raids supplies " + b.name().toLowerCase(java.util.Locale.ROOT)))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(b.contents())))));
        }
        p.displayClientMessage(line, false);
    }

    /** Talking to the Helpful Spirit in the Nexus: its offer again, if the player has one waiting. */
    static void talk(ServerPlayer p, RaidInstance raid) {
        if (raid.type != RaidType.TOA) {
            return;
        }
        if (PENDING.contains(p.getUUID())) {
            showOffer(p);
        } else if (raid.pathsCompleted() < 2) {
            Msg.game(p, "The Helpful Spirit drifts quietly. It will have supplies for you once two paths have been walked.");
        } else {
            Msg.game(p, raid.pathsCompleted() < 4 ? "The Helpful Spirit has nothing more for you until the last two paths are walked."
                    : "The Helpful Spirit has nothing more to give. Good luck against the Wardens.");
        }
    }

    /** Whether a player has a Helpful Spirit offer waiting (self-test). */
    static boolean hasOffer(ServerPlayer p) {
        return PENDING.contains(p.getUUID());
    }

    /** Claims a Helpful Spirit bundle ({@code /raids supplies <life|chaos|power>}). */
    static int claim(ServerPlayer p, String name) {
        RaidInstance raid = RaidManager.of(p);
        ToaSupplies.Bundle bundle = ToaSupplies.Bundle.byName(name);
        if (raid == null || raid.type != RaidType.TOA || !PENDING.contains(p.getUUID())) {
            Msg.game(p, "The Helpful Spirit has nothing for you right now.");
            return 0;
        }
        if (bundle == null) {
            Msg.game(p, "Choose Life, Chaos or Power.");
            return 0;
        }
        PENDING.remove(p.getUUID());
        raid.markSuppliesTaken(p.getUUID());
        Map<String, Integer> items = ToaSupplies.bundle(bundle, raid.invocations(), new java.util.Random(p.getRandom().nextLong()));
        StringBuilder got = new StringBuilder();
        items.forEach((id, n) -> {
            give(p, id, n);
            got.append(got.length() == 0 ? "" : ", ").append(n).append(" x ").append(id.replace('_', ' '));
        });
        Msg.game(p, "You take the " + bundle.displayName() + " supplies: " + got + ".");
        return 1;
    }

    /** Gives {@code count} fresh supply items (dosed items one per slot with their starting doses). */
    static void give(ServerPlayer p, String id, int count) {
        int doses = ToaSupplies.doses(id);
        if (doses <= 0) {
            Inv.give(p, id, count);
            return;
        }
        for (int i = 0; i < count; i++) {
            ItemStack stack = Inv.stack(id, 1);
            if (!stack.isEmpty() && doses < 4) {
                stack.setDamageValue(4 - doses);
            }
            Inv.give(p, stack);
        }
    }

    /** After a wipe each player gets honey locusts, unless On a Diet is active (OSRS). */
    static void wiped(RaidInstance raid) {
        int n = ToaSupplies.honeyLocustsOnWipe(raid.invocations());
        if (n <= 0) {
            return;
        }
        for (ServerPlayer p : raid.players()) {
            Inv.give(p, ToaSupplies.HONEY_LOCUST, n);
        }
    }

    static void forget(UUID u) {
        PENDING.remove(u);
        ADRENALINE.remove(u);
    }

    // ------------------------------------------------------------------ the supplies

    private static void use(ServerPlayer p, RaidInstance raid, ItemStack stack, InteractionHand hand, String id) {
        Set<Invocation> inv = raid.invocations();
        boolean heals = id.equals(ToaSupplies.NECTAR) || id.equals(ToaSupplies.AMBROSIA);
        if (heals && !Invocation.healingPotionsAllowed(inv)) {
            Msg.game(p, "The 'Dehydration' invocation stops you from drinking that.");
            return;
        }
        PlayerData data = Players.data(p);
        SkillSet skills = data.skills();
        int hpLevel = skills.level(Skill.HITPOINTS);
        int prayerLevel = skills.level(Skill.PRAYER);
        long now = RaidEffects.now();
        switch (id) {
            case ToaSupplies.NECTAR -> {
                setHitpoints(p, ToaSupplies.afterNectar(hitpoints(p), hpLevel));
                for (Skill s : COMBAT) {
                    skills.setBoost(s, skills.boost(s) - ToaSupplies.nectarDrain(skills.currentLevel(s)));
                }
                Msg.action(p, "You drink some of the nectar. It heals you, but dulls your senses.");
            }
            case ToaSupplies.AMBROSIA -> {
                setHitpoints(p, hpLevel + ToaSupplies.ambrosiaHitpointsBoost(hpLevel));
                int boost = ToaSupplies.ambrosiaPrayerBoost(prayerLevel);
                skills.applyBoost(Skill.PRAYER, boost, 0.0);
                data.setPrayerPoints(prayerLevel + boost);
                com.runecraft.content.systems.RunEnergyService.restore(p, 10000);
                com.runecraft.combat.PoisonService.cure(p, true, com.runecraft.osrs.Toxins.ANTIDOTE_PLUS_PLUS[0], ActionManager.gameTick());
                Msg.action(p, "You drink the ambrosia. You feel invincible!");
            }
            case ToaSupplies.TEARS -> {
                tears(p, skills, true);
                for (ServerPlayer other : raid.players()) {
                    if (other != p && Math.abs(other.getX() - p.getX()) <= 1.5 && Math.abs(other.getZ() - p.getZ()) <= 1.5) {
                        tears(other, Players.data(other).skills(), false);
                        Msg.game(other, p.getName().getString() + "'s tears of Elidinis restore you too.");
                    }
                }
                Msg.action(p, "You drink some of the tears of Elidinis.");
            }
            case ToaSupplies.SILK_DRESSING -> {
                pulse(p, ToaSupplies.SILK_EVERY, ToaSupplies.SILK_TIMES, () -> {
                    int lvl = Players.data(p).skills().level(Skill.HITPOINTS);
                    int cur = hitpoints(p);
                    if (cur < lvl + ToaSupplies.SILK_OVERHEAL) {
                        setHitpoints(p, Math.min(lvl + ToaSupplies.SILK_OVERHEAL, cur + ToaSupplies.SILK_HEAL));
                    }
                });
                Msg.action(p, "You apply the silk dressing.");
            }
            case ToaSupplies.CRYSTAL_SCARAB -> {
                pulse(p, ToaSupplies.SCARAB_EVERY, ToaSupplies.SCARAB_TIMES, () -> {
                    PlayerData d = Players.data(p);
                    d.setPrayerPoints(d.prayerPoints() + ToaSupplies.SCARAB_PRAYER);
                });
                Msg.action(p, "You crack the blessed crystal scarab.");
            }
            case ToaSupplies.SMELLING_SALTS -> {
                pulse(p, ToaSupplies.SALTS_EVERY, ToaSupplies.SALTS_TICKS / ToaSupplies.SALTS_EVERY, () -> salts(p));
                salts(p);
                Msg.action(p, "You crush the smelling salts. You feel energised!");
            }
            case ToaSupplies.ADRENALINE -> {
                ADRENALINE.put(p.getUUID(), now + ToaSupplies.ADRENALINE_TICKS);
                Msg.action(p, "You drink the liquid adrenaline. Your special attacks cost half as much for 150 seconds.");
            }
            case ToaSupplies.HONEY_LOCUST -> {
                raid.markSuppliesTaken(p.getUUID());
                int lvl = hpLevel;
                int cur = hitpoints(p);
                setHitpoints(p, Math.max(cur, Math.min(cur + ToaSupplies.HONEY_LOCUST_HEAL, lvl + ToaSupplies.HONEY_LOCUST_HEAL)));
                data.setPrayerPoints(data.prayerPoints() + ToaSupplies.honeyLocustPrayer(prayerLevel));
                for (Skill s : COMBAT) {
                    skills.restore(s, 8, 0.25);
                }
                Msg.action(p, "You eat the honey locust.");
            }
            default -> {
                return;
            }
        }
        if (id.equals(ToaSupplies.HONEY_LOCUST)) {
            stack.shrink(1);
        } else {
            int used = stack.getDamageValue() + 1;
            if (used >= 4) {
                p.setItemInHand(hand, ItemStack.EMPTY);
            } else {
                stack.setDamageValue(used);
            }
        }
        p.getCooldowns().addCooldown(stack.getItem(), 3 * ActionManager.MC_TICKS_PER_GAME_TICK);
        p.level().playSound(null, p.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        ServerNetworking.sync(p);
    }

    private static void tears(ServerPlayer p, SkillSet skills, boolean drinker) {
        for (Skill s : COMBAT) {
            skills.restore(s, 3, 0.25);
        }
        int level = skills.level(Skill.PRAYER);
        PlayerData d = Players.data(p);
        d.setPrayerPoints(d.prayerPoints() + (drinker ? ToaSupplies.tearsPrayer(level) : ToaSupplies.tearsPrayerShared(level)));
        if (!drinker) {
            ServerNetworking.sync(p);
        }
    }

    private static void salts(ServerPlayer p) {
        SkillSet skills = Players.data(p).skills();
        for (Skill s : COMBAT) {
            skills.applyBoost(s, 11, 0.16);
        }
        com.runecraft.content.systems.RunEnergyService.restore(p, ToaSupplies.SALTS_RUN_ENERGY);
        ServerNetworking.sync(p);
    }

    /** Runs {@code effect} every {@code every} game ticks, {@code times} times, while the player stays in ToA. */
    private static void pulse(ServerPlayer p, int every, int times, Runnable effect) {
        if (times <= 0) {
            return;
        }
        RaidEffects.later(every, () -> {
            RaidInstance raid = RaidManager.of(p);
            if (!p.isAlive() || p.isRemoved() || raid == null || raid.type != RaidType.TOA) {
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

    /** Sets hitpoints, holding anything above the maximum as overheal (absorption, as Saradomin brews do). */
    private static void setHitpoints(ServerPlayer p, int total) {
        float max = p.getMaxHealth();
        p.setHealth(Math.min(max, Math.max(1, total)));
        p.setAbsorptionAmount(Math.max(0, total - max));
    }
}
