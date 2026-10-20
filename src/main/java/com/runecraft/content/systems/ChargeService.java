package com.runecraft.content.systems;

import com.runecraft.combat.CombatHooks;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.item.ItemCharges;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.AccessGates;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.UseEntityCallback;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft side of {@link Charges}: drains charges in combat (Barrows per tick in combat, crystal
 * armour per hit taken, weapons per attack, serpentine helms on a combat timer), blocks attacks with an
 * empty weapon, recharges items with sneak + use (crystal shards only in Lletya or Prifddinas), and
 * repairs Barrows at Bob in Lumbridge (right-click him holding a Barrows piece) or a POH armour stand.
 */
public final class ChargeService {
    private static final class Fight {
        long lastCombat = -1000;
        long start = -1000;
        long ticksIn;
        long blowpipeShots;
    }

    private record Quote(long expires, int cost, boolean stand) {
    }

    private static final EquipmentSlot[] WORN = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
            EquipmentSlot.FEET, EquipmentSlot.OFFHAND};
    /** Lletya (KandarinElves) in OSRS tiles. */
    private static final AccessGates.Rect LLETYA = new AccessGates.Rect(2318, 3146, 2362, 3190);
    /** Pollnivneach and Ali the Camel Man's camels, where a camulet wearer can get ugthanki dung. approx bounds */
    private static final AccessGates.Rect POLLNIVNEACH = new AccessGates.Rect(3330, 2930, 3385, 3005);
    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();
    private static final Map<UUID, Quote> QUOTES = new HashMap<>();

    private ChargeService() {
    }

    public static void register() {
        CombatHooks.addAttackGate(ChargeService::denyAttack);
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                onAttack(player, type);
            }
        });
        CombatHooks.addHurtListener(ChargeService::onHurt);
        SysTicks.everyPlayer(ChargeService::gameTick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            FIGHTS.remove(handler.player.getUUID());
            QUOTES.remove(handler.player.getUUID());
        });
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            Charges.Spec spec = ItemCharges.spec(stack);
            if (spec == null || spec.materials().isEmpty() || !player.isShiftKeyDown()) {
                return InteractionResultHolder.pass(stack);
            }
            if (world.isClientSide()) {
                return InteractionResultHolder.sidedSuccess(stack, true);
            }
            if (player instanceof ServerPlayer sp) {
                recharge(sp, stack, spec);
            }
            return InteractionResultHolder.sidedSuccess(stack, false);
        });
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown() || !(entity instanceof QuestNpcEntity npc) || !"npc_bob".equals(npc.npcId())) {
                return InteractionResult.PASS;
            }
            Charges.Spec held = ItemCharges.spec(player.getMainHandItem());
            if (held == null || held.kind() != Charges.Kind.BARROWS) {
                return InteractionResult.PASS;
            }
            if (!world.isClientSide() && player instanceof ServerPlayer sp) {
                repair(sp, false);
            }
            return InteractionResult.SUCCESS;
        });
    }

    private static Fight fight(ServerPlayer p) {
        return FIGHTS.computeIfAbsent(p.getUUID(), u -> new Fight());
    }

    private static void markCombat(ServerPlayer p) {
        Fight f = fight(p);
        long now = SysTicks.now();
        if (now - f.lastCombat > Charges.COMBAT_TIMEOUT_TICKS) {
            f.start = now;
            f.ticksIn = 0;
        }
        f.lastCombat = now;
    }

    private static String name(Charges.Spec s) {
        return s.name();
    }

    // ------------------------------------------------------------------ combat

    private static String denyAttack(ServerPlayer p, AttackType type) {
        ItemStack weapon = p.getMainHandItem();
        Charges.Spec s = ItemCharges.spec(weapon);
        if (s != null && !ItemCharges.activated(weapon)) {
            return "Your " + name(s) + " must first be activated with " + Charges.WILDERNESS_WEAPON_ACTIVATION
                    + " revenant ether. (Sneak + use it with revenant ether in your inventory.)";
        }
        if (s == null || s.drain() == Charges.Drain.TELEPORT || !ItemCharges.depleted(weapon)) {
            return null;
        }
        return switch (s.kind()) {
            case BARROWS -> "Your " + name(s) + " is broken. Bob in Lumbridge can repair it.";
            case CRYSTAL -> "Your " + name(s) + " has run out of charges. Recharge it with crystal shards in Lletya or Prifddinas.";
            case BLOWPIPE -> "Your blowpipe needs Zulrah's scales to fire. (Sneak + use it with scales in your inventory.)";
            default -> "Your " + name(s) + " has no charges left. (Sneak + use it with " + materials(s) + " to charge it.)";
        };
    }

    private static void onAttack(ServerPlayer p, AttackType type) {
        markCombat(p);
        ItemStack weapon = p.getMainHandItem();
        Charges.Spec s = ItemCharges.spec(weapon);
        if (s == null || s.drain() != Charges.Drain.ATTACK) {
            return;
        }
        int amount = 1;
        if (s.kind() == Charges.Kind.BLOWPIPE) {
            amount = Charges.blowpipeScales(fight(p).blowpipeShots++);
        }
        if (CombatAchievementService.pennySavesCharge(p)) {
            return;
        }
        if (ItemCharges.drain(weapon, amount)) {
            Msg.red(p, "Your " + name(s) + " has run out of charges.");
        }
    }

    private static void onHurt(ServerPlayer p, LivingEntity attacker, int damage) {
        markCombat(p);
        if (damage <= 0) {
            return;
        }
        for (EquipmentSlot slot : ARMOUR) {
            ItemStack stack = p.getItemBySlot(slot);
            Charges.Spec s = ItemCharges.spec(stack);
            if (s != null && s.drain() == Charges.Drain.HIT_TAKEN && !CombatAchievementService.pennySavesCharge(p)
                    && ItemCharges.drain(stack, 1)) {
                Msg.red(p, "Your " + name(s) + " has run out of charges.");
            }
        }
    }

    private static void gameTick(ServerPlayer p, long tick) {
        Fight f = FIGHTS.get(p.getUUID());
        if (f == null || tick - f.lastCombat > Charges.COMBAT_TIMEOUT_TICKS || !Gielinor.is(p)) {
            return;
        }
        long inFight = tick - f.start;
        f.ticksIn++;
        // Barrows: one charge per tick in combat, written in batches of 10 to keep stack updates rare.
        boolean barrowsBatch = f.ticksIn % 10 == 0;
        for (EquipmentSlot slot : WORN) {
            ItemStack stack = p.getItemBySlot(slot);
            Charges.Spec s = ItemCharges.spec(stack);
            if (s == null || ItemCharges.depleted(stack)) {
                continue;
            }
            if (s.drain() == Charges.Drain.COMBAT_TICK && barrowsBatch) {
                int before = Charges.barrowsStage(ItemCharges.get(stack));
                boolean broke = ItemCharges.drain(stack, 10);
                int after = Charges.barrowsStage(ItemCharges.get(stack));
                if (broke) {
                    Msg.red(p, "Your " + name(s) + " has broken! Bob in Lumbridge can repair it.");
                } else if (after < before) {
                    Msg.game(p, "Your " + name(s) + " has degraded to " + after + ".");
                }
            } else if (s.drain() == Charges.Drain.SERPENTINE && slot == EquipmentSlot.HEAD) {
                if (ItemCharges.drain(stack, Charges.serpentineDrain(inFight))) {
                    Msg.red(p, "Your " + name(s) + " has run out of Zulrah's scales.");
                }
            }
        }
    }

    // ------------------------------------------------------------------ recharging

    private static String materials(Charges.Spec s) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Integer> e : s.materials().entrySet()) {
            parts.add(e.getValue() + " " + e.getKey().replace('_', ' '));
        }
        return String.join(", ", parts) + (s.chargesPerUnit() > 1 ? " per " + s.chargesPerUnit() + " charges" : " per charge");
    }

    private static void recharge(ServerPlayer p, ItemStack stack, Charges.Spec s) {
        if (s.id().equals(Charges.CAMULET)) {
            if (Inv.count(p, Charges.UGTHANKI_DUNG) <= 0 && POLLNIVNEACH.contains(AccessGateService.tileX(p.getX()), AccessGateService.tileY(p.getZ()))
                    && (com.runecraft.quest.QuestApi.stage(p, "my_arms_big_adventure") > 0
                    || com.runecraft.quest.QuestApi.isComplete(p, "my_arms_big_adventure"))) {
                // OSRS: camels talk to camulet wearers (after starting My Arm's Big Adventure) and leave ugthanki dung.
                Inv.give(p, Charges.UGTHANKI_DUNG, 1);
                Msg.action(p, "One of the camels takes a liking to your camulet and obliges. You scoop up some ugthanki dung.");
                return;
            }
            int add = Charges.camuletRefill(TeleportCharges.get(p, stack), Inv.count(p, Charges.UGTHANKI_DUNG));
            if (add <= 0) {
                Msg.game(p, TeleportCharges.get(p, stack) >= s.max() ? "Your camulet is already fully charged."
                        : "You need a bucket of ugthanki dung to recharge your camulet.");
                return;
            }
            Inv.remove(p, Charges.UGTHANKI_DUNG, 1);
            TeleportCharges.set(p, stack, s.max());
            Msg.game(p, "You recharge the camulet with the ugthanki dung. It has " + s.max() + " charges.");
            return;
        }
        if (s.kind() == Charges.Kind.CRYSTAL || s.id().equals(Charges.TELEPORT_CRYSTAL)) {
            double x = AccessGateService.tileX(p.getX());
            double y = AccessGateService.tileY(p.getZ());
            AccessGates.Gate prif = AccessGates.byId("prifddinas");
            if (!LLETYA.contains(x, y) && (prif == null || !prif.contains(x, y))) {
                Msg.game(p, "Crystal equipment can only be recharged by the elves in Lletya or Prifddinas.");
                return;
            }
        }
        if (!ItemCharges.activated(stack)) {
            // A Wilderness weapon first takes 1,000 revenant ether to activate; that ether is never fired.
            if (Inv.count(p, "revenant_ether") < Charges.WILDERNESS_WEAPON_ACTIVATION) {
                Msg.game(p, "You need " + Charges.WILDERNESS_WEAPON_ACTIVATION + " revenant ether to activate your " + name(s) + ".");
                return;
            }
            Inv.remove(p, "revenant_ether", Charges.WILDERNESS_WEAPON_ACTIVATION);
            ItemCharges.setActivated(stack, true);
            Msg.game(p, "You activate your " + name(s) + " with " + Charges.WILDERNESS_WEAPON_ACTIVATION + " revenant ether.");
            if (Inv.count(p, "revenant_ether") <= 0) {
                return;
            }
        }
        int charges = TeleportCharges.get(p, stack);
        Map<String, Integer> have = new LinkedHashMap<>();
        for (String item : s.materials().keySet()) {
            have.put(item, Inv.count(p, item));
        }
        int add = Charges.rechargeable(s, charges, have);
        if (add <= 0) {
            if (charges >= s.max()) {
                Msg.game(p, "Your " + name(s) + " is already fully charged.");
            } else {
                Msg.game(p, "You need " + materials(s) + " to charge your " + name(s) + ".");
            }
            return;
        }
        for (Map.Entry<String, Integer> e : Charges.cost(s, add).entrySet()) {
            Inv.remove(p, e.getKey(), e.getValue());
        }
        TeleportCharges.set(p, stack, charges + add);
        Msg.game(p, "You add " + add + " charge" + (add == 1 ? "" : "s") + " to your " + name(s) + ". "
                + Charges.describe(s, TeleportCharges.get(p, stack)) + ".");
        com.runecraft.network.ServerNetworking.sync(p);
    }

    // ------------------------------------------------------------------ Barrows repairs

    /** Degraded Barrows stacks carried or worn. */
    private static List<ItemStack> degradedBarrows(ServerPlayer p) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            Charges.Spec spec = ItemCharges.spec(s);
            if (spec != null && spec.kind() == Charges.Kind.BARROWS && ItemCharges.get(s) < spec.max()) {
                out.add(s);
            }
        }
        return out;
    }

    /**
     * Repairs every degraded Barrows piece the player carries: the first use quotes the price, a second
     * use within 15 seconds pays. {@code armourStand}: the POH armour stand's Smithing discount.
     */
    public static void repair(ServerPlayer p, boolean armourStand) {
        List<ItemStack> pieces = degradedBarrows(p);
        String who = armourStand ? "Armour stand" : "Bob";
        if (pieces.isEmpty()) {
            if (armourStand) {
                Msg.game(p, "You have no damaged Barrows equipment to repair.");
            } else {
                QuestApi.say(p, who, "That looks in fine shape to me. Come back when it needs fixing.");
            }
            return;
        }
        int smithing = Players.data(p).skills().level(Skill.SMITHING);
        int cost = 0;
        for (ItemStack s : pieces) {
            Charges.Spec spec = ItemCharges.spec(s);
            cost += armourStand ? Charges.armourStandCost(spec, ItemCharges.get(s), smithing) : Charges.repairCost(spec, ItemCharges.get(s));
        }
        long now = SysTicks.now();
        Quote q = QUOTES.get(p.getUUID());
        if (q == null || q.expires() < now || q.cost() != cost || q.stand() != armourStand) {
            QUOTES.put(p.getUUID(), new Quote(now + 25, cost, armourStand));
            String line = "Repairing " + pieces.size() + " piece" + (pieces.size() == 1 ? "" : "s") + " of Barrows equipment will cost "
                    + String.format("%,d", cost) + " coins. Use me again to pay.";
            if (armourStand) {
                Msg.game(p, line + " (Smithing " + smithing + " discount applied.)");
            } else {
                QuestApi.say(p, who, line);
            }
            return;
        }
        QUOTES.remove(p.getUUID());
        if (!Inv.has(p, Inv.COINS, cost)) {
            Msg.game(p, "You don't have enough coins: repairing costs " + String.format("%,d", cost) + ".");
            return;
        }
        Inv.remove(p, Inv.COINS, cost);
        for (ItemStack s : pieces) {
            ItemCharges.set(s, ItemCharges.spec(s).max());
        }
        if (armourStand) {
            Msg.game(p, "You repair your Barrows equipment for " + String.format("%,d", cost) + " coins.");
        } else {
            QuestApi.say(p, who, "There you go, good as new! That's " + String.format("%,d", cost) + " coins.");
        }
        com.runecraft.network.ServerNetworking.sync(p);
    }
}
