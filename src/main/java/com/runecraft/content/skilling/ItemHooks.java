package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.PoisonService;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.skill.XpService;
import com.runecraft.osrs.content.skilling.CookingData;
import com.runecraft.osrs.content.skilling.FletchingData;
import com.runecraft.osrs.content.skilling.GatheringData;
import com.runecraft.osrs.content.skilling.HerbloreData;
import com.runecraft.osrs.content.skilling.HunterData;
import com.runecraft.osrs.content.skilling.Loot;
import com.runecraft.osrs.content.skilling.PrayerData;
import com.runecraft.osrs.content.skilling.RunecraftData;
import com.runecraft.osrs.content.skilling.SailingData;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Right-click behaviour for this module's items. */
final class ItemHooks {
    private static final int EAT_DELAY = 3 * ActionManager.MC_TICKS_PER_GAME_TICK;
    /** NBT key on a weapon stack holding its poison damage (weapon poison). */
    static final String POISON_NBT = "RunecraftPoison";
    private static final String ESSENCE_NBT = "Essence";

    /** Items whose right-click this module always handles (the client swings and forwards them). */
    private static final Set<String> HANDLED = new HashSet<>();
    /** Active divine potions: player -> (effect, game tick it expires). */
    private static final Map<UUID, Divine> DIVINE = new HashMap<>();

    private record Divine(HerbloreData.Effect effect, long until) {
    }

    private ItemHooks() {
    }

    /** Scattering demonic ashes for Prayer XP (2 game ticks, like burying bones). */
    private static void scatter(ServerPlayerEntity player, ItemStack stack, PrayerData.Ashes ashes) {
        player.getItemCooldownManager().set(stack.getItem(), 2 * ActionManager.MC_TICKS_PER_GAME_TICK);
        stack.decrement(1);
        com.runecraft.skill.XpService.add(player, Skill.PRAYER, ashes.xp());
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_SAND_BREAK, SoundCategory.PLAYERS, 0.8f, 1.2f);
        Msg.action(player, "You scatter the ashes.");
    }

    static void init() {
        CookingData.BITES.forEach(b -> HANDLED.add(b.full()));
        HerbloreData.POTIONS.forEach(p -> HANDLED.add(p.id()));
        HunterData.IMPLINGS.forEach(i -> HANDLED.add(i.jar()));
        RunecraftData.POUCHES.forEach(p -> HANDLED.add(p.id()));
        SailingData.BOATS.forEach(b -> HANDLED.add(b.kit()));
        SailingData.WRECKS.forEach(w -> HANDLED.add(w.salvage()));
        HANDLED.addAll(FletchingData.THROWN);
        PrayerData.ASHES.forEach(a -> HANDLED.add(a.id()));
        HANDLED.addAll(java.util.List.of("casket", "oyster", "bird_nest_seeds", "bird_nest_ring", "infernal_eel", "trawling_net",
                "unfermented_wine"));

        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.isEmpty()) {
                return TypedActionResult.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (world.isClient()) {
                return HANDLED.contains(id) ? TypedActionResult.success(stack) : TypedActionResult.pass(stack);
            }
            if (!(player instanceof ServerPlayerEntity sp) || player.getItemCooldownManager().isCoolingDown(stack.getItem())) {
                return HANDLED.contains(id) ? TypedActionResult.success(stack) : TypedActionResult.pass(stack);
            }
            return use(sp, stack, hand, id) ? TypedActionResult.success(stack) : TypedActionResult.pass(stack);
        });

        // Poisoned melee weapons poison on hit (25% chance, like OSRS).
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public void afterHit(ServerPlayerEntity player, LivingEntity target, AttackType type, int dealt) {
                ItemStack weapon = player.getMainHandStack();
                NbtCompound nbt = weapon.getNbt();
                if (dealt > 0 && type != AttackType.MAGIC && nbt != null && nbt.contains(POISON_NBT)
                        && player.getRandom().nextInt(4) == 0) {
                    PoisonService.poison(target, nbt.getInt(POISON_NBT), ActionManager.gameTick());
                }
            }
        });
    }

    /** Returns true if handled. */
    private static boolean use(ServerPlayerEntity player, ItemStack stack, Hand hand, String id) {
        PrayerData.Ashes ashes = PrayerData.ashes(id);
        if (ashes != null) {
            scatter(player, stack, ashes);
            return true;
        }
        for (CookingData.Bite b : CookingData.BITES) {
            if (b.full().equals(id)) {
                bite(player, stack, b);
                return true;
            }
        }
        HerbloreData.ModPotion potion = HerbloreData.potion(id);
        if (potion != null) {
            drink(player, stack, hand, potion);
            return true;
        }
        HunterData.Impling imp = HunterData.implingByJar(id);
        if (imp != null) {
            GatheringHooks.openJar(player, stack, imp);
            return true;
        }
        RunecraftData.Pouch pouch = RunecraftData.pouch(id);
        if (pouch != null) {
            pouch(player, stack, pouch);
            return true;
        }
        SailingData.Wreck wreck = SailingData.wreckBySalvage(id);
        if (wreck != null) {
            stack.decrement(1);
            Loot l = SkillingUtil.roll(player, wreck.loot(), player.getRandom());
            XpService.add(player, Skill.SAILING, wreck.sortXp()); // OSRS: sorting salvage gives Sailing XP
            Msg.action(player, "You sort through the salvage" + (l == null ? "." : " and find " + SkillingUtil.lower(SkillingUtil.name(l.item())) + "."));
            return true;
        }
        if (FletchingData.THROWN.contains(id)) {
            throwItem(player, stack, id);
            return true;
        }
        if (SailingData.boat(id) != null) {
            SailingHooks.launch(player, stack, SailingData.boat(id));
            return true;
        }
        switch (id) {
            case "casket" -> {
                stack.decrement(1);
                Loot l = SkillingUtil.roll(player, GatheringData.CASKET, player.getRandom());
                Msg.action(player, "You open the casket and find " + SkillingUtil.lower(SkillingUtil.name(l.item())) + ".");
                return true;
            }
            case "oyster" -> {
                stack.decrement(1);
                Loot l = SkillingUtil.roll(player, GatheringData.OYSTER, player.getRandom());
                Msg.action(player, l.item().equals("oyster_pearl") ? "You find a pearl inside the oyster!" : "The oyster is empty.");
                return true;
            }
            case "seed_pack" -> {
                FarmingContractService.open(player, stack);
                return true;
            }
            case "bird_nest_seeds", "bird_nest_ring" -> {
                stack.decrement(1);
                Loot l = SkillingUtil.roll(player, id.equals("bird_nest_seeds") ? FarmingHooks.NEST_SEEDS : FarmingHooks.NEST_RINGS,
                        player.getRandom());
                SkillingUtil.give(player, "bird_nest", 1);
                Msg.action(player, "You take " + Msg.article(SkillingUtil.lower(SkillingUtil.name(l.item()))) + " out of the bird's nest.");
                return true;
            }
            case "infernal_eel" -> {
                if (!SkillingUtil.needItem(player, "hammer", "You need a hammer to crush the infernal eel.")) {
                    return true;
                }
                stack.decrement(1);
                Loot l = SkillingUtil.roll(player, GatheringData.INFERNAL_EEL, player.getRandom());
                Msg.action(player, "You crush the infernal eel and find " + SkillingUtil.lower(SkillingUtil.name(l.item())) + ".");
                return true;
            }
            case "trawling_net" -> {
                SailingHooks.trawl(player);
                return true;
            }
            case "unfermented_wine" -> {
                Msg.game(player, "The wine needs to ferment for a little while.");
                return true;
            }
            case "tinderbox" -> {
                return GatheringHooks.lightModLogs(player);
            }
            default -> {
            }
        }
        ItemDef def = ItemCatalog.get(id);
        if (def != null && def.kind() == ItemDef.Kind.SEED) {
            return FarmingHooks.potSeed(player, stack, id);
        }
        return false;
    }

    // ------------------------------------------------------------------ food

    private static void bite(ServerPlayerEntity player, ItemStack stack, CookingData.Bite b) {
        net.minecraft.item.Item item = stack.getItem();
        stack.decrement(1);
        player.heal(b.heal());
        player.getHungerManager().add(Math.max(1, b.heal() / 2), 0.3f);
        player.getItemCooldownManager().set(item, EAT_DELAY);
        if (b.next() != null) {
            Inv.give(player, b.next(), 1);
        }
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5f, 1.0f);
        Msg.action(player, "You eat " + (b.next() == null || b.next().equals("pie_dish") ? "the rest of " : "some of ") + "the "
                + SkillingUtil.lower(SkillingUtil.name(b.full())) + ".");
        ActionManager.cancel(player);
    }

    // ------------------------------------------------------------------ potions

    private static void drink(ServerPlayerEntity player, ItemStack stack, Hand hand, HerbloreData.ModPotion potion) {
        HerbloreData.Effect e = potion.effect();
        PlayerData data = Players.data(player);
        long now = ActionManager.gameTick();
        if (e.weaponPoison()) {
            ItemStack weapon = hand == Hand.MAIN_HAND ? player.getOffHandStack() : player.getMainHandStack();
            ItemDef wdef = ModItems.def(weapon);
            if (wdef == null || (wdef.kind() != ItemDef.Kind.MELEE_WEAPON && wdef.kind() != ItemDef.Kind.ARROW)) {
                Msg.game(player, "Hold the weapon you want to poison in your other hand.");
                return;
            }
            weapon.getOrCreateNbt().putInt(POISON_NBT, e.poisonDamage());
            Msg.action(player, "You coat the " + SkillingUtil.lower(wdef.name()) + " with poison.");
            useDose(player, stack, hand, potion, false);
            return;
        }
        if (e == HerbloreData.Effect.COMPOST) {
            if (!Inv.has(player, "compost", 1)) {
                Msg.game(player, "Use the compost potion on a bucket of compost to make supercompost.");
                return;
            }
            Inv.remove(player, "compost", 1);
            Inv.give(player, "supercompost", 1);
            Msg.action(player, "You pour some of the potion into the compost, turning it into supercompost.");
            useDose(player, stack, hand, potion, false);
            return;
        }
        int prayer = e.apply(data.skills());
        if (prayer > 0) {
            data.setPrayerPoints(Math.min(data.skills().level(Skill.PRAYER), data.prayerPoints() + prayer));
        }
        switch (e) {
            case STAMINA -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 120, 0));
                player.getHungerManager().add(4, 0.5f);
            }
            case SUPER_ENERGY -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 0));
                player.getHungerManager().add(6, 0.4f);
            }
            case SUPER_ANTIPOISON -> PoisonService.cure(player, false, 600, now);
            case ANTIDOTE_PLUS -> PoisonService.cure(player, false, 900, now);
            case ANTIDOTE_PLUS_PLUS -> PoisonService.cure(player, false, 1200, now);
            case ANTI_VENOM -> PoisonService.antiVenom(player, com.runecraft.osrs.Toxins.ANTI_VENOM[0],
                    com.runecraft.osrs.Toxins.ANTI_VENOM[1], now);
            case ANTI_VENOM_PLUS -> PoisonService.antiVenom(player, com.runecraft.osrs.Toxins.ANTI_VENOM_PLUS[0],
                    com.runecraft.osrs.Toxins.ANTI_VENOM_PLUS[1], now);
            case SUPER_ANTIFIRE -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 180, 1));
            case EXTENDED_SUPER_ANTIFIRE -> player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 20 * 360, 1));
            case GUTHIX_REST -> {
                player.heal(5);
                player.getHungerManager().add(3, 0.3f);
                PoisonService.cure(player, false, 0, now);
            }
            case ZAMORAK_BREW -> player.setHealth(Math.max(1f, player.getHealth() - Math.max(1, (int) (player.getMaxHealth() * 0.12))));
            default -> {
            }
        }
        if (e.divine()) {
            player.setHealth(Math.max(1f, player.getHealth() - 10));
            DIVINE.put(player.getUuid(), new Divine(e, now + 500));
        }
        player.removeStatusEffect(StatusEffects.POISON);
        useDose(player, stack, hand, potion, true);
        ServerNetworking.sync(player);
    }

    private static void useDose(ServerPlayerEntity player, ItemStack stack, Hand hand, HerbloreData.ModPotion potion, boolean drank) {
        int used = stack.getDamage() + 1;
        int left = 4 - used;
        player.getItemCooldownManager().set(stack.getItem(), EAT_DELAY);
        player.getWorld().playSound(null, player.getBlockPos(), drank ? SoundEvents.ENTITY_GENERIC_DRINK : SoundEvents.ITEM_BOTTLE_EMPTY,
                SoundCategory.PLAYERS, 0.5f, 1.0f);
        if (drank) {
            Msg.action(player, "You drink some of your " + SkillingUtil.lower(potion.name()) + ". "
                    + (left > 0 ? "You have " + left + " dose" + (left > 1 ? "s" : "") + " of potion left." : "You have finished your potion."));
        }
        if (left <= 0) {
            player.setStackInHand(hand, Inv.stack("vial", 1));
        } else {
            stack.setDamage(used);
        }
    }

    /** Keeps divine potion boosts topped up for 5 minutes. Called every game tick. */
    static void divineTick(ServerPlayerEntity player, long gameTick) {
        Divine d = DIVINE.get(player.getUuid());
        if (d == null) {
            return;
        }
        if (gameTick >= d.until()) {
            DIVINE.remove(player.getUuid());
            Msg.red(player, "The effects of the divine potion have worn off.");
            return;
        }
        if (gameTick % 10 == 0) {
            d.effect().apply(Players.data(player).skills());
            ServerNetworking.sync(player);
        }
    }

    // ------------------------------------------------------------------ thrown weapons

    private static void throwItem(ServerPlayerEntity player, ItemStack stack, String id) {
        ItemDef def = ItemCatalog.get(id);
        ArrowEntity arrow = new ArrowEntity(player.getWorld(), player);
        arrow.addCommandTag(OsrsEquipmentItems.RANGED_STRENGTH_TAG + def.rangedStrength());
        arrow.addCommandTag("runecraft_arrow_" + id);
        arrow.pickupType = PersistentProjectileEntity.PickupPermission.DISALLOWED;
        boolean javelin = id.endsWith("_javelin");
        arrow.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, javelin ? 2.2f : 2.6f, 1.0f);
        player.getWorld().spawnEntity(arrow);
        if (!player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        player.swingHand(Hand.MAIN_HAND, true);
        player.getItemCooldownManager().set(stack.getItem(), (javelin ? 6 : 3) * ActionManager.MC_TICKS_PER_GAME_TICK);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.6f, 1.4f);
    }

    // ------------------------------------------------------------------ essence pouches

    private static void pouch(ServerPlayerEntity player, ItemStack stack, RunecraftData.Pouch pouch) {
        NbtCompound nbt = stack.getOrCreateNbt();
        int held = nbt.getInt(ESSENCE_NBT);
        if (player.isSneaking()) {
            if (held == 0) {
                Msg.game(player, "There is no essence in this pouch.");
                return;
            }
            nbt.putInt(ESSENCE_NBT, 0);
            Inv.give(player, "pure_essence", held);
            Msg.action(player, "You empty " + held + " essence from the pouch.");
            return;
        }
        if (!SkillingUtil.need(player, Skill.RUNECRAFT, pouch.level(), "use this pouch")) {
            return;
        }
        int room = pouch.capacity() - held;
        int add = Math.min(room, Inv.count(player, "pure_essence"));
        if (add <= 0) {
            Msg.game(player, room <= 0 ? "Your pouch is full." : "You don't have any pure essence to put in the pouch.");
            return;
        }
        Inv.remove(player, "pure_essence", add);
        nbt.putInt(ESSENCE_NBT, held + add);
        Msg.action(player, "You fill the pouch (" + (held + add) + "/" + pouch.capacity() + ").");
    }

    /** Empties every essence pouch in the inventory (before crafting at an altar). */
    static void emptyPouches(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isEmpty() || RunecraftData.pouch(Inv.idOf(s)) == null) {
                continue;
            }
            NbtCompound nbt = s.getNbt();
            int held = nbt == null ? 0 : nbt.getInt(ESSENCE_NBT);
            if (held > 0) {
                nbt.putInt(ESSENCE_NBT, 0);
                Inv.give(player, "pure_essence", held);
            }
        }
    }

    static Random random(ServerPlayerEntity player) {
        return player.getRandom();
    }
}
