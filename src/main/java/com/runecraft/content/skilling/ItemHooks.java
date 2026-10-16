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
import com.runecraft.osrs.content.skilling.BrewingData;
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

    /** Active prayer regeneration potions: player -> game tick the effect ends. */
    private static final Map<UUID, Long> PRAYER_REGEN = new HashMap<>();
    /** Goading potion: game tick each player's goading wears off. */
    private static final Map<UUID, Long> GOADING = new HashMap<>();

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
        HerbloreData.MIXES.forEach(m -> HANDLED.add(m.id()));
        BrewingData.DRINKS.forEach(d -> HANDLED.add(d.id()));
        BrewingData.BREWS.forEach(b -> {
            HANDLED.add(BrewingData.kegOf(b.id()));
            HANDLED.add(BrewingData.kegOf(b.mature()));
        });
        HunterData.IMPLINGS.forEach(i -> HANDLED.add(i.jar()));
        HANDLED.addAll(java.util.List.of("jar_generator", "imp_in_a_box_2", "imp_in_a_box_1"));
        RunecraftData.POUCHES.forEach(p -> HANDLED.add(p.id()));
        SailingData.BOATS.forEach(b -> HANDLED.add(b.kit()));
        SailingData.WRECKS.forEach(w -> HANDLED.add(w.salvage()));
        com.runecraft.osrs.content.skilling.SailingSea.BALLS.stream().filter(b -> !b.item().equals("cannonball")
                && !b.item().equals("granite_cannonball")).forEach(b -> HANDLED.add(b.item()));
        com.runecraft.osrs.content.skilling.SailingCrew.KITS.forEach(k -> HANDLED.add(k.id()));
        com.runecraft.osrs.content.skilling.SailingCrew.SPECIALS.forEach(b -> HANDLED.add(b.item()));
        HANDLED.addAll(FletchingData.THROWN);
        PrayerData.ASHES.forEach(a -> HANDLED.add(a.id()));
        com.runecraft.osrs.data.Thieving.COIN_POUCHES.forEach(p -> HANDLED.add(p.itemId()));
        HANDLED.addAll(java.util.List.of("casket", "oyster", "bird_nest_seeds", "bird_nest_ring", "infernal_eel", "trawling_net",
                "unfermented_wine", "empty_sack", "hay_sack"));

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
        if (com.runecraft.skill.ThievingService.openPouches(player, id)) {
            return true;
        }
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
        HerbloreData.Mix mix = HerbloreData.mix(id);
        if (mix != null) {
            drinkMix(player, stack, hand, mix);
            return true;
        }
        String kegPint = BrewingData.pintOfKeg(id);
        if (kegPint != null) {
            drinkFromKeg(player, stack, hand, BrewingData.drink(kegPint));
            return true;
        }
        BrewingData.Drink beverage = BrewingData.drink(id);
        if (beverage != null) {
            drinkBeverage(player, stack, beverage);
            return true;
        }
        HunterData.Impling imp = HunterData.implingByJar(id);
        if (imp != null) {
            GatheringHooks.openJar(player, stack, imp);
            return true;
        }
        if (id.equals("jar_generator")) {
            GatheringHooks.useJarGenerator(player, stack);
            return true;
        }
        if (HunterData.impBoxAfterUse(id) != null) {
            GatheringHooks.useImpBox(player, stack, hand);
            return true;
        }
        RunecraftData.Pouch pouch = RunecraftData.pouch(id);
        if (pouch != null) {
            pouch(player, stack, pouch);
            return true;
        }
        // Sneak + use cargo (salvage, cannonballs, repair kits) near your boat: store it in the cargo hold.
        if (player.isSneaking() && player.getVehicle() == null && com.runecraft.osrs.content.skilling.SailingCrew.storable(id)
                && SeaHooks.deposit(player, id, stack.getCount())) {
            return true;
        }
        com.runecraft.osrs.content.skilling.SailingCrew.Kit kit = com.runecraft.osrs.content.skilling.SailingCrew.kit(id);
        if (kit != null) {
            return SeaHooks.applyKit(player, stack, kit);
        }
        if (com.runecraft.osrs.content.skilling.SailingCrew.special(id) != null && SeaHooks.useCannonball(player, id)) {
            return true;
        }
        SailingData.Wreck wreck = SailingData.wreckBySalvage(id);
        if (wreck != null) {
            SeaHooks.sort(player, stack, wreck); // OSRS: sorted at a salvaging station, wiki loot and Sailing XP
            return true;
        }
        if (com.runecraft.osrs.content.skilling.SailingSea.ball(id) != null && SeaHooks.useCannonball(player, id)) {
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
            case "empty_sack" -> {
                // Fill a sack with hay from a hay bale next to you (OSRS: haystacks and hay bales).
                net.minecraft.util.math.BlockPos at = player.getBlockPos();
                boolean hay = false;
                for (net.minecraft.util.math.BlockPos p : net.minecraft.util.math.BlockPos.iterate(at.add(-2, -1, -2), at.add(2, 2, 2))) {
                    if (player.getWorld().getBlockState(p).isOf(net.minecraft.block.Blocks.HAY_BLOCK)) {
                        hay = true;
                        break;
                    }
                }
                if (!hay) {
                    Msg.game(player, "Stand next to some hay to fill the sack.");
                    return true;
                }
                stack.decrement(1);
                SkillingUtil.give(player, "hay_sack", 1);
                Msg.action(player, "You fill the sack with hay.");
                return true;
            }
            case "hay_sack" -> {
                // Scarecrow (OSRS, 23 Farming, 25 XP): a bronze spear through the hay sack, a watermelon for its head.
                if (!SkillingUtil.need(player, Skill.FARMING, 23, "make a scarecrow")) {
                    return true;
                }
                if (!Inv.has(player, "bronze_spear", 1) || !Inv.has(player, "watermelon", 1)) {
                    Msg.game(player, "You need a bronze spear and a watermelon to make a scarecrow.");
                    return true;
                }
                stack.decrement(1);
                Inv.remove(player, "bronze_spear", 1);
                Inv.remove(player, "watermelon", 1);
                SkillingUtil.give(player, "scarecrow", 1);
                XpService.add(player, Skill.FARMING, 25.0);
                Msg.action(player, "You push the spear through the hay sack and put the watermelon on top. A scarecrow!");
                return true;
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
            useDose(player, stack, hand, potion.name(), 4, false);
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
            useDose(player, stack, hand, potion.name(), 4, false);
            return;
        }
        if (e == HerbloreData.Effect.SERUM_207) {
            Msg.game(player, "This serum is meant for the Afflicted of Mort'ton, not for drinking.");
            return;
        }
        applyEffect(player, e, now);
        useDose(player, stack, hand, potion.name(), 4, true);
        ServerNetworking.sync(player);
    }

    /** The effect of one dose of a module potion (also used by barbarian mixes). */
    private static void applyEffect(ServerPlayerEntity player, HerbloreData.Effect e, long now) {
        PlayerData data = Players.data(player);
        int prayer = e.apply(data.skills());
        if (prayer > 0 && e == HerbloreData.Effect.SANFEW) {
            prayer += com.runecraft.skill.ItemUses.wrenchBonus(player, 30);
        }
        if (prayer > 0) {
            data.setPrayerPoints(Math.min(data.skills().level(Skill.PRAYER), data.prayerPoints() + prayer));
        }
        switch (e) {
            case SANFEW -> PoisonService.cure(player, false, com.runecraft.osrs.Toxins.ANTIPOISON[0], now);
            case RELICYMS_BALM -> {
                // Cures disease: RuneCraft has none, so it clears the closest vanilla ailments.
                player.removeStatusEffect(StatusEffects.WEAKNESS);
                player.removeStatusEffect(StatusEffects.HUNGER);
                player.removeStatusEffect(StatusEffects.NAUSEA);
            }
            case PRAYER_REGENERATION -> PRAYER_REGEN.put(player.getUuid(), now + HerbloreData.Effect.REGEN_DURATION);
            case GOADING -> GOADING.put(player.getUuid(), now + HerbloreData.Effect.GOADING_DURATION);
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
            case EXTENDED_ANTI_VENOM_PLUS -> PoisonService.antiVenom(player, com.runecraft.osrs.Toxins.EXTENDED_ANTI_VENOM_PLUS[0],
                    com.runecraft.osrs.Toxins.EXTENDED_ANTI_VENOM_PLUS[1], now);
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
    }

    /** A barbarian mix: the base potion's effect plus a heal, two doses. */
    private static void drinkMix(ServerPlayerEntity player, ItemStack stack, Hand hand, HerbloreData.Mix mix) {
        long now = ActionManager.gameTick();
        PlayerData data = Players.data(player);
        HerbloreData.ModPotion mod = HerbloreData.potion(mix.base());
        if (mod != null) {
            applyEffect(player, mod.effect(), now);
        } else if (mix.base().equals("antifire_potion") || mix.base().equals("extended_antifire")) {
            int minutes = mix.base().equals("antifire_potion") ? 6 : 12;
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, minutes * 60 * 20, 0));
        } else {
            for (com.runecraft.osrs.data.Potion p : com.runecraft.osrs.data.Potion.values()) {
                if (!p.itemId().equals(mix.base())) {
                    continue;
                }
                int prayer = p.apply(data.skills());
                prayer += prayer > 0 ? com.runecraft.skill.ItemUses.wrenchBonus(player, 25) : 0;
                if (prayer > 0) {
                    int cap = Math.max(data.skills().level(Skill.PRAYER), data.skills().currentLevel(Skill.PRAYER));
                    data.setPrayerPoints(Math.max(data.prayerPoints(), Math.min(cap, data.prayerPoints() + prayer)));
                }
                if (p == com.runecraft.osrs.data.Potion.ANTIPOISON) {
                    player.removeStatusEffect(StatusEffects.POISON);
                    PoisonService.cure(player, false, com.runecraft.osrs.Toxins.ANTIPOISON[0], now);
                } else if (p == com.runecraft.osrs.data.Potion.ENERGY) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 20 * 60, 0));
                }
            }
        }
        player.heal(mix.heal());
        useDose(player, stack, hand, mix.name(), HerbloreData.MIX_DOSES, true);
        ServerNetworking.sync(player);
    }

    /** A keg of ale: one pint per sip; the empty calquat keg is kept after the fourth. */
    private static void drinkFromKeg(ServerPlayerEntity player, ItemStack stack, Hand hand, BrewingData.Drink drink) {
        drink.apply(Players.data(player).skills());
        if (drink.heal() > 0) {
            player.heal(drink.heal());
            player.getHungerManager().add(Math.max(1, drink.heal() / 2), 0.2f);
        }
        int used = stack.getDamage() + 1;
        player.getItemCooldownManager().set(stack.getItem(), EAT_DELAY);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.5f, 1.0f);
        int left = BrewingData.KEG_PINTS - used;
        Msg.action(player, "You drink from the keg of " + SkillingUtil.lower(drink.name()) + "."
                + (left > 0 ? " It has " + left + " pint" + (left > 1 ? "s" : "") + " left." : " The keg is empty."));
        if (left <= 0) {
            player.setStackInHand(hand, Inv.stack(BrewingData.KEG, 1));
        } else {
            stack.setDamage(used);
        }
        ServerNetworking.sync(player);
        ActionManager.cancel(player);
    }

    /** Ales, ciders, cocktails and spirits: heal, boosts and drains; the glass is kept. */
    private static void drinkBeverage(ServerPlayerEntity player, ItemStack stack, BrewingData.Drink drink) {
        net.minecraft.item.Item item = stack.getItem();
        stack.decrement(1);
        drink.apply(Players.data(player).skills());
        if (drink.heal() > 0) {
            player.heal(drink.heal());
            player.getHungerManager().add(Math.max(1, drink.heal() / 2), 0.2f);
        }
        String glass = BrewingData.emptyGlass(drink.id());
        if (glass != null) {
            Inv.give(player, glass, 1);
        }
        player.getItemCooldownManager().set(item, EAT_DELAY);
        player.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.5f, 1.0f);
        Msg.action(player, "You drink the " + SkillingUtil.lower(drink.name()) + "."
                + (drink.mods().isEmpty() ? "" : " You feel slightly reinvigorated... and slightly dizzy too."));
        ServerNetworking.sync(player);
        ActionManager.cancel(player);
    }

    /**
     * Goading potion: every 6 game ticks, monsters within a 9x9-tile area (4 tiles = 8 blocks each way) that aren't
     * already fighting someone turn on the player. Called every game tick.
     */
    static void goadingTick(ServerPlayerEntity player, long gameTick) {
        Long until = GOADING.get(player.getUuid());
        if (until == null) {
            return;
        }
        if (gameTick >= until || !player.isAlive()) {
            GOADING.remove(player.getUuid());
            Msg.red(player, "Your goading potion has worn off.");
            return;
        }
        if (gameTick % HerbloreData.Effect.GOADING_INTERVAL != 0) {
            return;
        }
        for (com.runecraft.entity.OsrsMonsterEntity mob : player.getWorld().getEntitiesByClass(com.runecraft.entity.OsrsMonsterEntity.class,
                player.getBoundingBox().expand(8), m -> m.isAlive() && m.getTarget() == null)) {
            mob.setTarget(player);
        }
    }

    /** Prayer regeneration potions: one Prayer point every 12 game ticks while active. Called every game tick. */
    static void prayerRegenTick(ServerPlayerEntity player, long gameTick) {
        Long until = PRAYER_REGEN.get(player.getUuid());
        if (until == null) {
            return;
        }
        if (gameTick >= until) {
            PRAYER_REGEN.remove(player.getUuid());
            Msg.red(player, "Your prayer regeneration potion has worn off.");
            return;
        }
        if (gameTick % HerbloreData.Effect.REGEN_INTERVAL == 0) {
            PlayerData data = Players.data(player);
            int cap = data.skills().level(Skill.PRAYER);
            if (data.prayerPoints() < cap) {
                data.setPrayerPoints(data.prayerPoints() + 1);
                ServerNetworking.sync(player);
            }
        }
    }

    private static void useDose(ServerPlayerEntity player, ItemStack stack, Hand hand, String name, int doses, boolean drank) {
        int used = stack.getDamage() + 1;
        int left = doses - used;
        player.getItemCooldownManager().set(stack.getItem(), EAT_DELAY);
        player.getWorld().playSound(null, player.getBlockPos(), drank ? SoundEvents.ENTITY_GENERIC_DRINK : SoundEvents.ITEM_BOTTLE_EMPTY,
                SoundCategory.PLAYERS, 0.5f, 1.0f);
        if (drank) {
            Msg.action(player, "You drink some of your " + SkillingUtil.lower(name) + ". "
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
        int used = nbt.getInt(EssencePouches.USED_NBT);
        int capacity = RunecraftData.capacity(pouch, used);
        int room = capacity - held;
        int add = Math.min(room, Inv.count(player, "pure_essence"));
        if (add <= 0) {
            Msg.game(player, room <= 0 ? "Your pouch is full." : "You don't have any pure essence to put in the pouch.");
            return;
        }
        Inv.remove(player, "pure_essence", add);
        nbt.putInt(ESSENCE_NBT, held + add);
        // Pouches decay by the essence stored in them (OSRS); the Dark Mage repairs them.
        nbt.putInt(EssencePouches.USED_NBT, used + add);
        int after = RunecraftData.capacity(pouch, used + add);
        Msg.action(player, "You fill the pouch (" + (held + add) + "/" + capacity + ").");
        if (after < capacity) {
            Msg.red(player, "Your pouch has decayed through use.");
        }
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
