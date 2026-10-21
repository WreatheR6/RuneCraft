package com.runecraft.skill;

import com.runecraft.combat.AttackTimer;
import com.runecraft.combat.PoisonService;
import com.runecraft.osrs.AttackClock;
import com.runecraft.osrs.ConsumeRules;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.QuickConsumeRules;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The quick-eat and quick-drink keys (round 42, {@code // playability}; {@link QuickConsumeRules} picks the item): the chosen
 * food or potion is used exactly as a right-click would ({@code ServerPlayerGameMode.useItem}: the shared eat / potion delays,
 * every module's eat and drink hooks, the item's own effect), held in the off hand for that one use when it isn't in a hand
 * already. OSRS has no such key: players click the item in the always-open inventory.
 */
public final class QuickConsume {
    /** Off-hand slot index used for {@link QuickConsumeRules.Food#slot()} / {@link QuickConsumeRules.Potion#slot()}. */
    public static final int OFFHAND = 40;

    /** What the last quick-eat / quick-drink did (self-tests): the item used, or null with the reason shown. */
    public static volatile String lastUsed;
    public static volatile String lastRefusal;

    private QuickConsume() {
    }

    /** Quick-eat ({@code drink} false) or quick-drink; returns whether something was consumed. */
    public static boolean use(ServerPlayer player, boolean drink) {
        lastUsed = null;
        lastRefusal = null;
        if (player.isSpectator() || !player.isAlive()) {
            return false;
        }
        return drink ? drink(player) : eat(player);
    }

    private static boolean eat(ServerPlayer player) {
        List<QuickConsumeRules.Food> foods = new ArrayList<>();
        for (int slot : slots(player)) {
            ItemStack stack = stack(player, slot);
            ItemDef def = def(stack);
            ConsumeRules.Group group = def == null ? null : ConsumeCooldowns.groupOf(def.id(), def);
            if (def != null && def.kind() == ItemDef.Kind.FOOD && def.heal() > 0
                    && (group == ConsumeRules.Group.FOOD || group == ConsumeRules.Group.COMBO)) {
                foods.add(new QuickConsumeRules.Food(slot, def.id(), def.heal(), group == ConsumeRules.Group.COMBO));
            }
        }
        if (foods.isEmpty()) {
            return refuse(player, "You have no food to eat.");
        }
        int hp = (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount());
        int missing = Math.max(0, Math.round(player.getMaxHealth()) - hp);
        if (missing <= 0) {
            return refuse(player, "You don't need to eat right now.");
        }
        boolean foodReady = !ConsumeCooldowns.blocked(player, ConsumeRules.Group.FOOD);
        boolean comboReady = !ConsumeCooldowns.blocked(player, ConsumeRules.Group.COMBO);
        int pick = QuickConsumeRules.chooseFood(foods, missing, foodReady, comboReady);
        if (pick < 0) {
            return refuse(player, "You are already eating.");
        }
        return useSlot(player, foods.get(pick).slot(), foods.get(pick).id());
    }

    private static boolean drink(ServerPlayer player) {
        List<QuickConsumeRules.Potion> potions = new ArrayList<>();
        for (int slot : slots(player)) {
            ItemStack stack = stack(player, slot);
            ItemDef def = def(stack);
            if (def != null && ConsumeCooldowns.groupOf(def.id(), def) == ConsumeRules.Group.POTION) {
                potions.add(new QuickConsumeRules.Potion(slot, def.id(), Math.max(1, 4 - stack.getDamageValue())));
            }
        }
        if (potions.isEmpty()) {
            return refuse(player, "You have no potions to drink.");
        }
        if (ConsumeCooldowns.blocked(player, ConsumeRules.Group.POTION)) {
            return refuse(player, "You are already drinking.");
        }
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        AttackClock.Kind kind = AttackTimer.kindFor(player);
        int style = kind == AttackClock.Kind.SPELL ? 2 : kind == AttackClock.Kind.RANGED || kind == AttackClock.Kind.THROWN ? 1 : 0;
        int hp = (int) Math.ceil(player.getHealth() + player.getAbsorptionAmount());
        QuickConsumeRules.State state = new QuickConsumeRules.State(hp, Math.round(player.getMaxHealth()), data.prayerPoints(),
                skills.level(Skill.PRAYER), PoisonService.isPoisoned(player), PoisonService.isEnvenomed(player), style,
                skills.boost(Skill.STRENGTH) > 0 || skills.boost(Skill.ATTACK) > 0, skills.boost(Skill.RANGED) > 0,
                skills.boost(Skill.MAGIC) > 0);
        int pick = QuickConsumeRules.choosePotion(potions, state);
        if (pick < 0) {
            return refuse(player, "None of your potions would help right now.");
        }
        return useSlot(player, potions.get(pick).slot(), potions.get(pick).id());
    }

    private static boolean refuse(ServerPlayer player, String why) {
        lastRefusal = why;
        Msg.action(player, why);
        return false;
    }

    /** The slots to look in: the hotbar and pack (0-35), then the off hand. */
    private static int[] slots(ServerPlayer player) {
        int[] out = new int[Inventory.INVENTORY_SIZE + 1];
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            out[i] = i;
        }
        out[Inventory.INVENTORY_SIZE] = OFFHAND;
        return out;
    }

    private static ItemStack stack(ServerPlayer player, int slot) {
        return slot == OFFHAND ? player.getOffhandItem() : player.getInventory().getItem(slot);
    }

    private static ItemDef def(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        ItemDef def = ModItems.def(stack);
        if (def == null) {
            String id = Inv.idOf(stack);
            def = id == null ? null : ItemCatalog.get(id);
        }
        return def;
    }

    /**
     * Uses the item in {@code slot} as a right-click with it in hand: the selected hotbar slot from the main hand, the off
     * hand from there, any other slot moved into the off hand for the use (the off hand's own item is put back after; what
     * the use leaves goes back to the slot, or into the pack when the use filled that slot).
     */
    private static boolean useSlot(ServerPlayer player, int slot, String id) {
        Inventory inv = player.getInventory();
        ItemStack before = stack(player, slot);
        int count = before.getCount();
        int damage = before.getDamageValue();
        ItemStack after;
        if (slot == inv.selected) {
            player.gameMode.useItem(player, player.level(), before, InteractionHand.MAIN_HAND);
            after = player.getMainHandItem();
        } else if (slot == OFFHAND) {
            player.gameMode.useItem(player, player.level(), before, InteractionHand.OFF_HAND);
            after = player.getOffhandItem();
        } else {
            ItemStack offhand = inv.offhand.get(0);
            inv.setItem(slot, ItemStack.EMPTY);
            inv.offhand.set(0, before);
            try {
                player.gameMode.useItem(player, player.level(), before, InteractionHand.OFF_HAND);
            } finally {
                after = inv.offhand.get(0);
                inv.offhand.set(0, offhand);
                if (inv.getItem(slot).isEmpty()) {
                    inv.setItem(slot, after);
                } else if (!after.isEmpty() && !inv.add(after)) {
                    player.drop(after, false);
                }
            }
            inv.setChanged();
        }
        boolean used = after != before || before.getCount() < count || before.getDamageValue() != damage
                || after.isEmpty() || !id.equals(Inv.idOf(after));
        if (used) {
            lastUsed = id;
        } else {
            lastRefusal = "the " + id + " was not used";
        }
        return used;
    }
}
