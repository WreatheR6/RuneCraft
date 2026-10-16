package com.runecraft.content.systems;

import com.runecraft.combat.CombatHandler;
import com.runecraft.item.ItemCharges;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;

/**
 * Wearing items in the extra slots, jewellery perks (recoil, life, phoenix, regen...), Vengeance and
 * Power of Death damage reactions, teleport jewellery and the Fountain of Heroes.
 */
public final class JewelleryService {
    private JewelleryService() {
    }

    public static Jewellery.Worn wornDef(ItemStack stack) {
        return stack.isEmpty() ? null : Jewellery.worn(Inv.idOf(stack));
    }

    /** A worn teleport item or a hand-held one (the pharaoh's sceptre), for the teleport menu only. */
    static Jewellery.Worn teleDef(ItemStack stack) {
        Jewellery.Worn w = wornDef(stack);
        return w != null || stack.isEmpty() ? w : Jewellery.held(Inv.idOf(stack));
    }

    /**
     * Activates the locked "visit" destinations (statues of Ates, the Necropolis obelisk) the player stands near; returns
     * the names activated.
     */
    private static List<String> activateNearby(ServerPlayerEntity p, Jewellery.Worn w) {
        List<String> out = new java.util.ArrayList<>();
        if (!Gielinor.is(p)) {
            return out;
        }
        int[] at = SysTeleport.osrs(p);
        SysState.SysPlayer sp = SysState.of(p);
        for (Teleports.Destination d : w.teleports()) {
            String quest = Teleports.activationQuest(d);
            if (Teleports.needsVisit(d) && !sp.unlocks.contains(d.name())
                    && Teleports.within(at[0], at[1], d.x(), d.y(), Teleports.ACTIVATE_RADIUS)) {
                if (quest != null && !com.runecraft.quest.QuestApi.isComplete(p, quest)) {
                    Msg.game(p, "The statue doesn't respond. (Complete "
                            + com.runecraft.osrs.content.ContentRegistry.quest(quest).displayName() + " to activate it.)");
                    continue;
                }
                sp.unlocks.add(d.name());
                out.add(d.name());
            }
        }
        return out;
    }

    /** Wears the item in the given hand (swapping with whatever is in that slot). */
    public static void wear(ServerPlayerEntity p, Hand hand) {
        ItemStack stack = p.getStackInHand(hand);
        Jewellery.Worn w = wornDef(stack);
        if (w == null || ItemCatalog.get(Inv.idOf(stack)) == null) {
            Msg.game(p, "You can't wear that in an extra slot.");
            return;
        }
        ItemDef def = ModItems.def(stack);
        if (def != null && def.equipment() != null) {
            String unmet = def.equipment().unmetRequirement(Players.data(p).skills());
            if (unmet != null) {
                Msg.game(p, "You need " + unmet + " to wear this.");
                return;
            }
        }
        SysState.SysPlayer sp = SysState.of(p);
        ItemStack old = sp.worn(w.slot());
        ItemStack one = stack.split(1);
        sp.worn.put(w.slot(), one);
        if (w.id().equals("ring_of_recoil") && !Inv.idOf(old).equals("ring_of_recoil")) {
            sp.recoilCharges = Jewellery.RECOIL_CHARGES;
        }
        if (!old.isEmpty()) {
            if (p.getStackInHand(hand).isEmpty()) {
                p.setStackInHand(hand, old);
            } else {
                Inv.give(p, old);
            }
        }
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 0.6f, 1.2f);
        Msg.action(p, "You put on the " + one.getName().getString() + ".");
        ServerNetworking.sync(p);
    }

    public static void remove(ServerPlayerEntity p, Jewellery.Slot slot) {
        SysState.SysPlayer sp = SysState.of(p);
        ItemStack old = sp.worn.remove(slot);
        if (old != null && !old.isEmpty()) {
            Inv.give(p, old);
            Msg.action(p, "You remove the " + old.getName().getString() + ".");
        }
    }

    // ---------------- Teleports ----------------

    /** Uses one charge of a teleport item (in hand or worn) and returns the new stack. */
    static ItemStack useCharge(ServerPlayerEntity p, ItemStack stack) {
        Jewellery.Worn w = wornDef(stack);
        if (w == null || w.chargeBase() == null) {
            return stack;
        }
        String next = Jewellery.afterCharge(w);
        if (next == null) {
            Msg.game(p, "Your " + w.name().replaceAll("\\(\\d\\)", "").trim().toLowerCase() + " crumbles to dust.");
            return ItemStack.EMPTY;
        }
        Jewellery.Worn after = Jewellery.worn(next);
        if (after != null && after.charges() > 0) {
            Msg.game(p, "Your " + w.name().replaceAll("\\(\\d\\)", "").trim().toLowerCase() + " has " + after.charges()
                    + " charge" + (after.charges() == 1 ? "" : "s") + " left.");
        } else {
            Msg.game(p, "You use your " + w.name().replaceAll("\\(\\d\\)", "").trim().toLowerCase() + "'s last charge.");
        }
        return Inv.stack(next, 1);
    }

    /** Opens the teleport menu of the item in hand or in a worn slot. */
    public static void openTeleports(ServerPlayerEntity p, String source) {
        ItemStack stack = stackFor(p, source);
        Jewellery.Worn w = teleDef(stack);
        if (w == null) {
            return;
        }
        SysNet.Screen s = new SysNet.Screen("tele", w.name());
        for (String name : activateNearby(p, w)) {
            Msg.action(p, "You commune with the " + (w.id().equals("pharaohs_sceptre") ? "obelisk" : "statue")
                    + ". Your " + w.name().toLowerCase(java.util.Locale.ROOT) + " can now teleport you to " + name + ".");
        }
        Charges.Spec charged = teleportCharges(stack);
        if (w.chargeBase() != null && w.charges() == 0) {
            s.line("It has no charges left. Recharge it at the Fountain of Heroes (Heroes' Guild, Burthorpe).", 0xFF8080);
        } else if (charged != null && TeleportCharges.get(p, stack) <= 0) {
            s.line("It has no charges left. Sneak and use it with " + materialName(charged) + "s in your inventory to charge it.", 0xFF8080);
        } else {
            if (charged != null) {
                s.line(Charges.describe(charged, TeleportCharges.get(p, stack)) + " (one per teleport; sneak and use it with "
                        + materialName(charged) + "s to add more).");
            }
            s.line("Where would you like to teleport to?");
            List<Teleports.Destination> dests = w.teleports();
            boolean diary = com.runecraft.osrs.content.systems.DiaryRewards.areaOf(w.id()) != null;
            for (int i = 0; i < dests.size(); i++) {
                String unlock = Teleports.unlockItem(dests.get(i));
                boolean locked = (unlock != null || Teleports.needsVisit(dests.get(i)))
                        && !SysState.of(p).unlocks.contains(dests.get(i).name());
                s.button(source + "|" + i, diary ? DiaryService.rewardTeleLabel(p, w.id(), i)
                        : dests.get(i).name() + (locked ? " (locked)" : ""));
                if (locked && unlock != null && Inv.has(p, unlock, 1)) {
                    s.button(source + "|unlock" + i, "Use your " + itemName(unlock).toLowerCase() + " to unlock " + dests.get(i).name());
                }
            }
        }
        if (w.id().startsWith("slayer_ring")) {
            s.button(source + "|task", "Check your slayer task");
        }
        if (w.id().startsWith("explorers_ring")) {
            int tier = tierOf(w.id());
            s.line("Run energy restores today: " + DiaryService.dailyLeft(p, "explorer_energy", energyUses(tier)) + ", alchemy casts: "
                    + DiaryService.dailyLeft(p, "explorer_alch", com.runecraft.osrs.content.systems.DiaryRewards.EXPLORER_ALCHS_PER_DAY));
            s.button(source + "|energy", "Restore 50% run energy");
            s.button(source + "|alch", (com.runecraft.osrs.content.systems.DiaryRewards.explorerHighAlch(tier) ? "High" : "Low")
                    + " alchemy (off-hand item)");
        }
        if (source.startsWith("hand") && Jewellery.worn(w.id()) != null) {
            s.button(source + "|wear", "Wear it");
        }
        s.send(p);
    }

    /** The per-stack charge spec of a teleport item that keeps its charges in NBT (Xeric's talisman), or null. */
    static Charges.Spec teleportCharges(ItemStack stack) {
        Charges.Spec spec = ItemCharges.spec(stack);
        return spec != null && spec.drain() == Charges.Drain.TELEPORT ? spec : null;
    }

    private static String materialName(Charges.Spec spec) {
        return spec.materials().isEmpty() ? "charge" : itemName(spec.materials().keySet().iterator().next()).toLowerCase();
    }

    private static String itemName(String id) {
        ItemDef def = ItemCatalog.get(id);
        return def == null ? id.replace('_', ' ') : def.name();
    }

    /** "a"/"an" + name. */
    private static String article(String name) {
        return (name.matches("^[aeiouAEIOU].*") ? "an " : "a ") + name;
    }

    private static int energyUses(int tier) {
        return com.runecraft.osrs.content.systems.DiaryRewards.explorerEnergyRestores(tier);
    }

    static int tierOf(String id) {
        char c = id.charAt(id.length() - 1);
        return Character.isDigit(c) ? c - '0' : 1;
    }

    static ItemStack stackFor(ServerPlayerEntity p, String source) {
        if (source.equals("hand")) {
            return p.getMainHandStack();
        }
        if (source.startsWith("worn:")) {
            try {
                return SysState.of(p).worn(Jewellery.Slot.valueOf(source.substring(5)));
            } catch (IllegalArgumentException e) {
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void setStack(ServerPlayerEntity p, String source, ItemStack stack) {
        if (source.equals("hand")) {
            ItemStack held = p.getMainHandStack();
            if (held.getCount() > 1) {
                held.decrement(1);
                Inv.give(p, stack);
            } else {
                p.setStackInHand(Hand.MAIN_HAND, stack);
            }
        } else if (source.startsWith("worn:")) {
            Jewellery.Slot slot = Jewellery.Slot.valueOf(source.substring(5));
            if (stack.isEmpty()) {
                SysState.of(p).worn.remove(slot);
            } else {
                SysState.of(p).worn.put(slot, stack);
            }
        }
    }

    static void teleAction(ServerPlayerEntity p, String action, Map<String, String> fields) {
        int bar = action.lastIndexOf('|');
        if (bar < 0) {
            return;
        }
        String source = action.substring(0, bar);
        String what = action.substring(bar + 1);
        ItemStack stack = stackFor(p, source);
        Jewellery.Worn w = teleDef(stack);
        if (w == null) {
            Msg.game(p, "You are no longer holding that item.");
            return;
        }
        switch (what) {
            case "wear" -> {
                if (Jewellery.worn(w.id()) != null) {
                    wear(p, Hand.MAIN_HAND);
                }
            }
            case "energy" -> {
                if (DiaryService.useDaily(p, "explorer_energy", energyUses(tierOf(w.id())))) {
                    RunEnergyService.restore(p, 100 * com.runecraft.osrs.content.systems.DiaryRewards.explorerEnergyPercent(tierOf(w.id())));
                    Msg.action(p, "You feel refreshed and renewed.");
                } else {
                    Msg.game(p, "You have used up your energy restores for today.");
                }
            }
            case "alch" -> explorerAlch(p, com.runecraft.osrs.content.systems.DiaryRewards.explorerHighAlch(tierOf(w.id())));
            case "task" -> com.runecraft.combat.Slayer.checkTask(p);
            default -> {
                if (what.startsWith("unlock")) {
                    unlock(p, w, what.substring("unlock".length()));
                    return;
                }
                int i;
                try {
                    i = Integer.parseInt(what);
                } catch (NumberFormatException e) {
                    return;
                }
                if (i < 0 || i >= w.teleports().size() || (w.chargeBase() != null && w.charges() == 0)) {
                    return;
                }
                if (com.runecraft.osrs.content.systems.DiaryRewards.areaOf(w.id()) != null) {
                    DiaryService.rewardTeleport(p, w.id(), i);
                    return;
                }
                Teleports.Destination dest = w.teleports().get(i);
                String unlockItem = Teleports.unlockItem(dest);
                if (unlockItem != null && !SysState.of(p).unlocks.contains(dest.name())) {
                    Msg.game(p, "You need to use " + article(itemName(unlockItem).toLowerCase()) + " on your "
                            + w.name() + " to unlock " + dest.name() + ".");
                    return;
                }
                if (Teleports.needsVisit(dest) && !SysState.of(p).unlocks.contains(dest.name())) {
                    Msg.game(p, "You need to commune with the " + (w.id().equals("pharaohs_sceptre") ? "obelisk" : "statue of Ates")
                            + " at " + dest.name() + " with your " + w.name().toLowerCase(java.util.Locale.ROOT)
                            + " before you can teleport there.");
                    return;
                }
                String locked = lockedReason(p, dest);
                if (locked != null) {
                    Msg.game(p, locked);
                    return;
                }
                Charges.Spec charged = teleportCharges(stack);
                if (charged != null && TeleportCharges.get(p, stack) <= 0) {
                    Msg.game(p, "Your " + charged.name() + " has no charges left.");
                    return;
                }
                // Ring of returning: the player's own respawn point (a bed in Gielinor), else Lumbridge.
                net.minecraft.util.math.BlockPos home = "ring_of_returning".equals(w.chargeBase()) ? SysTeleport.respawnPoint(p) : null;
                boolean went = home != null ? SysTeleport.teleportTo(p, home, "your respawn point", Teleports.wildernessLimit(w.id()))
                        : SysTeleport.teleport(p, dest, Teleports.wildernessLimit(w.id()));
                if (went) {
                    if (Jewellery.SINGLE_USE.contains(w.id()) && source.equals("hand")) {
                        stack.decrement(1);
                        Msg.game(p, "Your " + w.name().toLowerCase(java.util.Locale.ROOT) + " is used up.");
                    }
                    DiaryService.onActivity(p, "teleport:" + w.id() + ":" + com.runecraft.osrs.content.systems.Diaries.slug(dest.name()));
                    if (w.chargeBase() != null) {
                        setStack(p, source, useCharge(p, stack));
                    } else if (charged != null) {
                        TeleportCharges.set(p, stack, TeleportCharges.get(p, stack) - 1);
                        int left = TeleportCharges.get(p, stack);
                        Msg.game(p, "Your " + charged.name() + " has " + left + " charge" + (left == 1 ? "" : "s") + " left.");
                    }
                }
            }
        }
    }

    /**
     * Why a destination is closed to this player (a Kourend memoir story whose quest isn't done, the camulet's quarry or the
     * stony basalt's roof without their diary tier or Agility level), or null.
     */
    static String lockedReason(ServerPlayerEntity p, Teleports.Destination dest) {
        String memoir = Teleports.memoirQuest(dest);
        if (memoir != null && com.runecraft.osrs.content.ContentRegistry.quest(memoir) != null
                && !com.runecraft.quest.QuestApi.isComplete(p, memoir)) {
            return "You need to complete " + com.runecraft.osrs.content.ContentRegistry.quest(memoir).displayName()
                    + " to read that story.";
        }
        String[] diary = Teleports.needsDiary(dest);
        if (diary != null && !com.runecraft.osrs.content.systems.Diaries.tierComplete(SysState.of(p).diaryDone, diary[0],
                com.runecraft.osrs.content.systems.Diaries.Tier.valueOf(diary[1]))) {
            return "You need to complete the " + diary[1].toLowerCase(java.util.Locale.ROOT) + " " + Character.toUpperCase(diary[0].charAt(0))
                    + diary[0].substring(1) + " Diary to teleport to " + dest.name() + ".";
        }
        if (dest.equals(Teleports.STONY_BASALT_ROOF)
                && data(p).skills().level(Skill.AGILITY) < Teleports.STONY_BASALT_ROOF_AGILITY) {
            return "You need level " + Teleports.STONY_BASALT_ROOF_AGILITY + " Agility to teleport onto the Stronghold's roof.";
        }
        return null;
    }

    /** Uses an unlock item (the ancient tablet) on a teleport item: permanently unlocks that destination. */
    private static void unlock(ServerPlayerEntity p, Jewellery.Worn w, String index) {
        int i;
        try {
            i = Integer.parseInt(index);
        } catch (NumberFormatException e) {
            return;
        }
        if (i < 0 || i >= w.teleports().size()) {
            return;
        }
        Teleports.Destination dest = w.teleports().get(i);
        String item = Teleports.unlockItem(dest);
        SysState.SysPlayer sp = SysState.of(p);
        if (item == null || sp.unlocks.contains(dest.name())) {
            return;
        }
        if (!Inv.has(p, item, 1)) {
            Msg.game(p, "You need " + article(itemName(item).toLowerCase()) + " to do that.");
            return;
        }
        Inv.remove(p, item, 1);
        sp.unlocks.add(dest.name());
        Msg.action(p, "As the talisman and tablet are held together, a surge of power connects the two. After a brief moment, "
                + "the tablet crumbles, and the talisman feels somehow different.");
    }

    private static void explorerAlch(ServerPlayerEntity p, boolean high) {
        ItemStack off = p.getOffHandStack();
        ItemDef def = ModItems.def(off);
        if (def == null || def.id().equals("coins")) {
            Msg.game(p, "Hold the item you want to alchemise in your off-hand.");
            return;
        }
        if (!DiaryService.useDaily(p, "explorer_alch", com.runecraft.osrs.content.systems.DiaryRewards.EXPLORER_ALCHS_PER_DAY)) {
            Msg.game(p, "Your ring has no alchemy charges left today.");
            return;
        }
        off.decrement(1);
        int coins = high ? def.highAlch() : def.lowAlch();
        Inv.give(p, "coins", coins);
        Msg.action(p, "Your ring alchemises the " + def.name().toLowerCase() + " into " + coins + " coins.");
        ServerNetworking.sync(p);
    }

    // ---------------- Fountain of Heroes ----------------

    public static void fountain(ServerPlayerEntity p, BlockPos pos, net.minecraft.block.BlockState state) {
        int charged = rechargeAll(p);
        if (charged == 0) {
            Msg.game(p, "You need dragonstone jewellery (glory, skills necklace, combat bracelet or ring of wealth) to recharge.");
            return;
        }
        SysCombat.dust(p, new Vector3f(0.3f, 0.6f, 1.0f), 30);
        p.getWorld().playSound(null, pos, SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 0.8f, 1.2f);
        Msg.action(p, "You feel a power emanating from the fountain. It recharges your jewellery.");
    }

    /**
     * Recharges every rechargeable dragonstone jewellery item carried or worn (Fountain of Heroes, Recharge Dragonstone);
     * returns how many were recharged.
     */
    static int rechargeAll(ServerPlayerEntity p) {
        int charged = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            Jewellery.Worn w = wornDef(s);
            if (w == null || w.chargeBase() == null) {
                continue;
            }
            String to = Jewellery.recharged(w.chargeBase());
            Jewellery.Worn target = to == null ? null : Jewellery.worn(to);
            if (target == null || w.charges() >= target.charges()) {
                continue;
            }
            p.getInventory().setStack(i, Inv.stack(to, s.getCount()));
            charged += s.getCount();
        }
        SysState.SysPlayer sp = SysState.of(p);
        for (Jewellery.Slot slot : Jewellery.Slot.values()) {
            Jewellery.Worn w = wornDef(sp.worn(slot));
            if (w != null && w.chargeBase() != null) {
                String to = Jewellery.recharged(w.chargeBase());
                Jewellery.Worn target = to == null ? null : Jewellery.worn(to);
                if (target != null && w.charges() < target.charges()) {
                    sp.worn.put(slot, Inv.stack(to, 1));
                    charged++;
                }
            }
        }
        return charged;
    }

    // ---------------- Damage reactions ----------------

    /** Uses one charge of a per-player pool (Jewellery.chargePool); the worn item crumbles when the pool runs out. */
    static int useCharge(ServerPlayerEntity p, Jewellery.Slot slot, String id) {
        SysState.SysPlayer sp = SysState.of(p);
        int left = sp.jewelleryCharges.getOrDefault(id, Jewellery.chargePool(id)) - 1;
        if (left <= 0) {
            sp.worn.remove(slot);
            sp.jewelleryCharges.remove(id);
            ItemDef def = ItemCatalog.get(id);
            Msg.red(p, "Your " + (def == null ? id : def.name()) + " crumbles to dust.");
            return 0;
        }
        sp.jewelleryCharges.put(id, left);
        return left;
    }

    /**
     * Flamtaer bracelet: while worn, each Mort'ton temple repair uses one of its 80 charges (the minigames module adds the
     * extra sanctity). Returns whether a bracelet was worn.
     */
    public static boolean useFlamtaerCharge(ServerPlayerEntity p) {
        if (!SysState.of(p).wornId(Jewellery.Slot.HANDS).equals("flamtaer_bracelet")) {
            return false;
        }
        useCharge(p, Jewellery.Slot.HANDS, "flamtaer_bracelet");
        return true;
    }

    /** Slayer task credit of a kill with an expeditious bracelet or bracelet of slaughter worn (OSRS: 25%, 30 charges). */
    public static int slayerCredit(ServerPlayerEntity p, int base, boolean jadOrZuk) {
        String hands = SysState.of(p).wornId(Jewellery.Slot.HANDS);
        if (!hands.equals("expeditious_bracelet") && !hands.equals("bracelet_of_slaughter")) {
            return base;
        }
        int credit = Jewellery.slayerCredit(base, hands, p.getRandom().nextDouble(), jadOrZuk);
        if (credit != base) {
            int left = useCharge(p, Jewellery.Slot.HANDS, hands);
            Msg.game(p, (credit > base ? "Your expeditious bracelet helps you progress your slayer task faster."
                    : "Your bracelet of slaughter prevents your slayer count from decreasing.")
                    + (left > 0 ? " It has " + left + " charge" + (left == 1 ? "" : "s") + " left." : ""));
        }
        return credit;
    }

    /** Bracelet of ethereum: a charged one cuts revenant damage by 75% for one revenant ether per attack. */
    public static int ethereum(ServerPlayerEntity p, LivingEntity attacker, int damage) {
        if (attacker == null || damage <= 0
                || !net.minecraft.registry.Registries.ENTITY_TYPE.getId(attacker.getType()).getPath().startsWith("revenant_")) {
            return damage;
        }
        ItemStack hands = SysState.of(p).worn(Jewellery.Slot.HANDS);
        if (hands.isEmpty() || !Inv.idOf(hands).equals("bracelet_of_ethereum") || ItemCharges.get(hands) <= 0) {
            return damage;
        }
        if (ItemCharges.drain(hands, 1)) {
            Msg.red(p, "Your bracelet of ethereum has run out of revenant ether.");
        }
        return Jewellery.ethereumDamage(damage);
    }

    /**
     * Kills in the Wilderness: the looting bag drop (only while the player owns none) and the charged bracelet of
     * ethereum absorbing the revenant ether a revenant just dropped.
     */
    public static void onKill(ServerPlayerEntity p, LivingEntity mob, com.runecraft.combat.MonsterStats stats) {
        if (Gielinor.wildernessLevel(mob) <= 0) {
            return;
        }
        String cid = net.minecraft.registry.Registries.ENTITY_TYPE.getId(mob.getType()).getPath();
        if (!com.runecraft.osrs.content.systems.LootingBag.excluded(cid) && !Inv.has(p, com.runecraft.osrs.content.systems.LootingBag.ID, 1)
                && p.getRandom().nextDouble() < com.runecraft.osrs.content.systems.LootingBag.chance(stats.combatLevel())) {
            mob.dropStack(Inv.stack(com.runecraft.osrs.content.systems.LootingBag.ID, 1));
        }
        ItemStack hands = SysState.of(p).worn(Jewellery.Slot.HANDS);
        if (cid.startsWith("revenant_") && !hands.isEmpty() && Inv.idOf(hands).equals("bracelet_of_ethereum")
                && ItemCharges.get(hands) > 0) {
            for (net.minecraft.entity.ItemEntity ie : mob.getWorld().getEntitiesByClass(net.minecraft.entity.ItemEntity.class,
                    mob.getBoundingBox().expand(2.0), ie -> "revenant_ether".equals(Inv.idOf(ie.getStack())))) {
                int n = ie.getStack().getCount();
                ItemCharges.set(hands, Math.min(Charges.REVENANT_ETHER_MAX, ItemCharges.get(hands) + n));
                ie.discard();
                Msg.game(p, "Your bracelet of ethereum absorbs " + n + " revenant ether.");
            }
        }
    }

    public static void onHurt(ServerPlayerEntity p, LivingEntity attacker, int damage) {
        SysState.SysPlayer sp = SysState.of(p);
        long now = SysTicks.now();
        if (sp.powerOfDeathUntil > now && damage > 1) {
            p.heal(damage / 2f);
        }
        if (attacker != null && attacker.isAlive()) {
            if (sp.vengeance) {
                sp.vengeance = false;
                int back = damage * 3 / 4;
                p.sendMessage(Text.literal(p.getName().getString() + ": Taste vengeance!").formatted(Formatting.YELLOW), false);
                if (back > 0) {
                    // Combat Achievements "finish with Vengeance": the recoil is the player's latest damage until they attack again.
                    CombatAchievementService.flag(p, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_VENGEANCE_LAST);
                    // "Demonic Rebound": which attack the recoil reflected (the Abyssal Sire's explosion).
                    CombatAchievementService.flag(p, com.runecraft.osrs.content.systems.CombatAchievements.FLAG_VENGEANCE_ON
                            + CombatAchievementService.lastHitAttack(p));
                    CombatHandler.applyDamage(p, attacker, back);
                }
                SysNet.sendState(p);
            }
            String ring = sp.wornId(Jewellery.Slot.RING);
            if (ring.equals("ring_of_recoil") || ring.equals("ring_of_suffering")) {
                int back = Jewellery.recoil(damage);
                if (ring.equals("ring_of_recoil")) {
                    back = Math.min(back, sp.recoilCharges);
                    sp.recoilCharges -= back;
                    if (sp.recoilCharges <= 0) {
                        sp.worn.remove(Jewellery.Slot.RING);
                        sp.recoilCharges = Jewellery.RECOIL_CHARGES;
                        Msg.red(p, "Your Ring of Recoil has shattered.");
                    }
                }
                if (back > 0) {
                    CombatHandler.applyDamage(p, attacker, back);
                }
            }
        }
        if (!p.isAlive()) {
            return;
        }
        float hp = p.getHealth();
        float max = p.getMaxHealth();
        String neck = sp.wornId(Jewellery.Slot.NECK);
        if (neck.equals("phoenix_necklace") && hp > 0 && hp < max * 0.2f) {
            p.heal(max * 0.3f);
            sp.worn.remove(Jewellery.Slot.NECK);
            Msg.red(p, "Your phoenix necklace heals you, but is destroyed in the process.");
            hp = p.getHealth();
        }
        // Necklace of faith (OSRS Wiki): below 20% hitpoints it restores 25% of the Prayer level, then crumbles.
        if (neck.equals("necklace_of_faith") && hp > 0 && hp < max * Jewellery.FAITH_HP_FRACTION) {
            PlayerData data = Players.data(p);
            data.setPrayerPoints(data.prayerPoints() + Jewellery.faithRestore(data.skills().level(Skill.PRAYER)));
            sp.worn.remove(Jewellery.Slot.NECK);
            Msg.red(p, "Your necklace of faith restores your prayer, then degrades to dust.");
            ServerNetworking.sync(p);
        }
        String ring = sp.wornId(Jewellery.Slot.RING);
        String cape = sp.wornId(Jewellery.Slot.CAPE);
        boolean capeLife = cape.startsWith("defence_cape") || cape.equals("max_cape");
        if ((ring.equals("ring_of_life") || capeLife) && hp > 0 && hp <= max * 0.1f && Gielinor.wildernessLevel(p) <= 30) {
            if (!capeLife) {
                sp.worn.remove(Jewellery.Slot.RING);
                Msg.red(p, "Your Ring of Life saves you, but is destroyed in the process.");
            } else {
                Msg.red(p, "Your cape saves you from certain death.");
            }
            SysTeleport.teleport(p, Teleports.at("Lumbridge", 3222, 3218), 30);
        }
    }

    /** Per-tick perks: doubled hitpoint regeneration (regen bracelet, Hitpoints cape, max cape). */
    public static void gameTick(ServerPlayerEntity p, long tick) {
        if (tick % 100 != 50 || !Gielinor.is(p) || !p.isAlive() || p.getHealth() >= p.getMaxHealth()) {
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
        String hands = sp.wornId(Jewellery.Slot.HANDS);
        String cape = sp.wornId(Jewellery.Slot.CAPE);
        if (hands.equals("regen_bracelet") || cape.startsWith("hitpoints_cape") || cape.equals("max_cape")) {
            p.heal(1);
        }
    }

    /** Prayer bonus of worn items (shown in the worn-equipment screen). */
    public static int wornPrayer(ServerPlayerEntity p) {
        return SysCombat.wornStats(p).prayer();
    }

    static PlayerData data(ServerPlayerEntity p) {
        return Players.data(p);
    }

    static boolean hasSkill99(ServerPlayerEntity p, Skill s) {
        return data(p).skills().level(s) >= 99;
    }
}
