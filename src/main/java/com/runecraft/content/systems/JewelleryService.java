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
        Jewellery.Worn w = wornDef(stack);
        if (w == null) {
            return;
        }
        SysNet.Screen s = new SysNet.Screen("tele", w.name());
        Charges.Spec charged = teleportCharges(stack);
        if (w.chargeBase() != null && w.charges() == 0) {
            s.line("It has no charges left. Recharge it at the Fountain of Heroes (Heroes' Guild, Burthorpe).", 0xFF8080);
        } else if (charged != null && ItemCharges.get(stack) <= 0) {
            s.line("It has no charges left. Sneak and use it with " + materialName(charged) + "s in your inventory to charge it.", 0xFF8080);
        } else {
            if (charged != null) {
                s.line(Charges.describe(charged, ItemCharges.get(stack)) + " (one per teleport; sneak and use it with "
                        + materialName(charged) + "s to add more).");
            }
            s.line("Where would you like to teleport to?");
            List<Teleports.Destination> dests = w.teleports();
            boolean diary = com.runecraft.osrs.content.systems.DiaryRewards.areaOf(w.id()) != null;
            for (int i = 0; i < dests.size(); i++) {
                String unlock = Teleports.unlockItem(dests.get(i));
                boolean locked = unlock != null && !SysState.of(p).unlocks.contains(dests.get(i).name());
                s.button(source + "|" + i, diary ? DiaryService.rewardTeleLabel(p, w.id(), i)
                        : dests.get(i).name() + (locked ? " (locked)" : ""));
                if (locked && Inv.has(p, unlock, 1)) {
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
        if (source.startsWith("hand")) {
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
        Jewellery.Worn w = wornDef(stack);
        if (w == null) {
            Msg.game(p, "You are no longer holding that item.");
            return;
        }
        switch (what) {
            case "wear" -> wear(p, Hand.MAIN_HAND);
            case "energy" -> {
                if (DiaryService.useDaily(p, "explorer_energy", energyUses(tierOf(w.id())))) {
                    RunEnergyService.restore(p, 5000);
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
                Charges.Spec charged = teleportCharges(stack);
                if (charged != null && ItemCharges.get(stack) <= 0) {
                    Msg.game(p, "Your " + charged.name() + " has no charges left.");
                    return;
                }
                if (SysTeleport.teleport(p, dest, Teleports.wildernessLimit(w.id()))) {
                    if (w.chargeBase() != null) {
                        setStack(p, source, useCharge(p, stack));
                    } else if (charged != null) {
                        ItemCharges.drain(stack, 1);
                        int left = ItemCharges.get(stack);
                        Msg.game(p, "Your " + charged.name() + " has " + left + " charge" + (left == 1 ? "" : "s") + " left.");
                    }
                }
            }
        }
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
