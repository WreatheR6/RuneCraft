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
import com.runecraft.util.StackData;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Right-click behaviour for this module's items. */
public final class ItemHooks {
    private static final int EAT_DELAY = 3 * ActionManager.MC_TICKS_PER_GAME_TICK;
    /** NBT key on a weapon stack holding its poison damage (weapon poison). */
    public static final String POISON_NBT = "RunecraftPoison";
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
    private static void scatter(ServerPlayer player, ItemStack stack, PrayerData.Ashes ashes) {
        player.getCooldowns().addCooldown(stack.getItem(), 2 * ActionManager.MC_TICKS_PER_GAME_TICK);
        stack.shrink(1);
        com.runecraft.skill.XpService.add(player, Skill.PRAYER, ashes.xp());
        player.level().playSound(null, player.blockPosition(), SoundEvents.SAND_BREAK, SoundSource.PLAYERS, 0.8f, 1.2f);
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
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (world.isClientSide()) {
                return HANDLED.contains(id) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
            }
            if (!(player instanceof ServerPlayer sp) || player.getCooldowns().isOnCooldown(stack.getItem())) {
                return HANDLED.contains(id) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
            }
            return use(sp, stack, hand, id) ? InteractionResultHolder.success(stack) : InteractionResultHolder.pass(stack);
        });

        // Poisoned melee weapons poison on hit (25% chance, like OSRS).
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                ItemStack weapon = player.getMainHandItem();
                CompoundTag nbt = StackData.get(weapon);
                if (dealt > 0 && type != AttackType.MAGIC && nbt != null && nbt.contains(POISON_NBT)
                        && player.getRandom().nextInt(4) == 0) {
                    PoisonService.poison(target, nbt.getInt(POISON_NBT), ActionManager.gameTick());
                }
            }
        });
    }

    /** Returns true if handled. */
    private static boolean use(ServerPlayer player, ItemStack stack, InteractionHand hand, String id) {
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
        if (player.isShiftKeyDown() && player.getVehicle() == null && com.runecraft.osrs.content.skilling.SailingCrew.storable(id)
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
                stack.shrink(1);
                Loot l = SkillingUtil.roll(player, GatheringData.CASKET, player.getRandom());
                Msg.action(player, "You open the casket and find " + SkillingUtil.lower(SkillingUtil.name(l.item())) + ".");
                return true;
            }
            case "oyster" -> {
                stack.shrink(1);
                Loot l = SkillingUtil.roll(player, GatheringData.OYSTER, player.getRandom());
                Msg.action(player, l.item().equals("oyster_pearl") ? "You find a pearl inside the oyster!" : "The oyster is empty.");
                return true;
            }
            case "seed_pack" -> {
                FarmingContractService.open(player, stack);
                return true;
            }
            case "bird_nest_seeds", "bird_nest_ring" -> {
                stack.shrink(1);
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
                stack.shrink(1);
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
                net.minecraft.core.BlockPos at = player.blockPosition();
                boolean hay = false;
                for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 2, 2))) {
                    if (player.level().getBlockState(p).is(net.minecraft.world.level.block.Blocks.HAY_BLOCK)) {
                        hay = true;
                        break;
                    }
                }
                if (!hay) {
                    Msg.game(player, "Stand next to some hay to fill the sack.");
                    return true;
                }
                stack.shrink(1);
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
                stack.shrink(1);
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

    private static void bite(ServerPlayer player, ItemStack stack, CookingData.Bite b) {
        net.minecraft.world.item.Item item = stack.getItem();
        stack.shrink(1);
        player.heal(b.heal());
        player.getFoodData().eat(Math.max(1, b.heal() / 2), 0.3f);
        com.runecraft.skill.ConsumeCooldowns.consume(player, b.full());
        if (b.next() != null) {
            Inv.give(player, b.next(), 1);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5f, 1.0f);
        Msg.action(player, "You eat " + (b.next() == null || b.next().equals("pie_dish") ? "the rest of " : "some of ") + "the "
                + SkillingUtil.lower(SkillingUtil.name(b.full())) + ".");
        ActionManager.cancel(player);
    }

    // ------------------------------------------------------------------ potions

    private static void drink(ServerPlayer player, ItemStack stack, InteractionHand hand, HerbloreData.ModPotion potion) {
        HerbloreData.Effect e = potion.effect();
        PlayerData data = Players.data(player);
        long now = ActionManager.gameTick();
        if (e.weaponPoison()) {
            ItemStack weapon = hand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();
            ItemDef wdef = ModItems.def(weapon);
            if (wdef == null || (wdef.kind() != ItemDef.Kind.MELEE_WEAPON && wdef.kind() != ItemDef.Kind.ARROW)) {
                Msg.game(player, "Hold the weapon you want to poison in your other hand.");
                return;
            }
            StackData.update(weapon, t -> t.putInt(POISON_NBT, e.poisonDamage()));
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

    /**
     * Called when a player drinks a dose that cures disease (sanfew serum, relicym's balm): the Nightmare's parasites are
     * weakened by it.
     */
    public static final java.util.List<java.util.function.Consumer<ServerPlayer>> DISEASE_CURE =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    /** The effect of one dose of a module potion (also used by barbarian mixes). */
    private static void applyEffect(ServerPlayer player, HerbloreData.Effect e, long now) {
        PlayerData data = Players.data(player);
        int prayer = e.apply(data.skills());
        if (prayer > 0 && e == HerbloreData.Effect.SANFEW) {
            prayer += com.runecraft.skill.ItemUses.wrenchBonus(player, 30);
        }
        if (prayer > 0) {
            data.setPrayerPoints(Math.min(data.skills().level(Skill.PRAYER), data.prayerPoints() + prayer));
        }
        switch (e) {
            case SANFEW -> {
                PoisonService.cure(player, false, com.runecraft.osrs.Toxins.ANTIPOISON[0], now);
                DISEASE_CURE.forEach(l -> l.accept(player));
            }
            case RELICYMS_BALM -> {
                DISEASE_CURE.forEach(l -> l.accept(player));
                // Cures disease: RuneCraft has none, so it clears the closest vanilla ailments.
                player.removeEffect(MobEffects.WEAKNESS);
                player.removeEffect(MobEffects.HUNGER);
                player.removeEffect(MobEffects.CONFUSION);
            }
            case PRAYER_REGENERATION -> PRAYER_REGEN.put(player.getUUID(), now + HerbloreData.Effect.REGEN_DURATION);
            case GOADING -> GOADING.put(player.getUUID(), now + HerbloreData.Effect.GOADING_DURATION);
            case STAMINA -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 120, 0));
                player.getFoodData().eat(4, 0.5f);
            }
            case SUPER_ENERGY -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 0));
                player.getFoodData().eat(6, 0.4f);
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
            case SUPER_ANTIFIRE -> player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 180, 1));
            case EXTENDED_SUPER_ANTIFIRE -> player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 20 * 360, 1));
            case GUTHIX_REST -> {
                player.heal(5);
                player.getFoodData().eat(3, 0.3f);
                PoisonService.cure(player, false, 0, now);
            }
            case ZAMORAK_BREW -> player.setHealth(Math.max(1f, player.getHealth() - Math.max(1, (int) (player.getMaxHealth() * 0.12))));
            default -> {
            }
        }
        if (e.divine()) {
            player.setHealth(Math.max(1f, player.getHealth() - 10));
            DIVINE.put(player.getUUID(), new Divine(e, now + 500));
        }
        player.removeEffect(MobEffects.POISON);
    }

    /** A barbarian mix: the base potion's effect plus a heal, two doses. */
    private static void drinkMix(ServerPlayer player, ItemStack stack, InteractionHand hand, HerbloreData.Mix mix) {
        long now = ActionManager.gameTick();
        PlayerData data = Players.data(player);
        HerbloreData.ModPotion mod = HerbloreData.potion(mix.base());
        if (mod != null) {
            applyEffect(player, mod.effect(), now);
        } else if (mix.base().equals("antifire_potion") || mix.base().equals("extended_antifire")) {
            int minutes = mix.base().equals("antifire_potion") ? 6 : 12;
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, minutes * 60 * 20, 0));
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
                    player.removeEffect(MobEffects.POISON);
                    PoisonService.cure(player, false, com.runecraft.osrs.Toxins.ANTIPOISON[0], now);
                } else if (p == com.runecraft.osrs.data.Potion.ENERGY) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 60, 0));
                }
            }
        }
        player.heal(mix.heal());
        useDose(player, stack, hand, mix.name(), HerbloreData.MIX_DOSES, true);
        ServerNetworking.sync(player);
    }

    /** A keg of ale: one pint per sip; the empty calquat keg is kept after the fourth. */
    private static void drinkFromKeg(ServerPlayer player, ItemStack stack, InteractionHand hand, BrewingData.Drink drink) {
        drink.apply(Players.data(player).skills());
        if (drink.heal() > 0) {
            player.heal(drink.heal());
            player.getFoodData().eat(Math.max(1, drink.heal() / 2), 0.2f);
        }
        int used = stack.getDamageValue() + 1;
        com.runecraft.skill.ConsumeCooldowns.consume(player, Inv.idOf(stack));
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        int left = BrewingData.KEG_PINTS - used;
        Msg.action(player, "You drink from the keg of " + SkillingUtil.lower(drink.name()) + "."
                + (left > 0 ? " It has " + left + " pint" + (left > 1 ? "s" : "") + " left." : " The keg is empty."));
        if (left <= 0) {
            player.setItemInHand(hand, Inv.stack(BrewingData.KEG, 1));
        } else {
            stack.setDamageValue(used);
        }
        ServerNetworking.sync(player);
        ActionManager.cancel(player);
    }

    /** Ales, ciders, cocktails and spirits: heal, boosts and drains; the glass is kept. */
    private static void drinkBeverage(ServerPlayer player, ItemStack stack, BrewingData.Drink drink) {
        net.minecraft.world.item.Item item = stack.getItem();
        stack.shrink(1);
        drink.apply(Players.data(player).skills());
        if (drink.heal() > 0) {
            player.heal(drink.heal());
            player.getFoodData().eat(Math.max(1, drink.heal() / 2), 0.2f);
        }
        String glass = BrewingData.emptyGlass(drink.id());
        if (glass != null) {
            Inv.give(player, glass, 1);
        }
        com.runecraft.skill.ConsumeCooldowns.consume(player, drink.id());
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.5f, 1.0f);
        Msg.action(player, "You drink the " + SkillingUtil.lower(drink.name()) + "."
                + (drink.mods().isEmpty() ? "" : " You feel slightly reinvigorated... and slightly dizzy too."));
        ServerNetworking.sync(player);
        ActionManager.cancel(player);
    }

    /**
     * Goading potion: every 6 game ticks, monsters within a 9x9-tile area (4 tiles = 8 blocks each way) that aren't
     * already fighting someone turn on the player. Called every game tick.
     */
    static void goadingTick(ServerPlayer player, long gameTick) {
        Long until = GOADING.get(player.getUUID());
        if (until == null) {
            return;
        }
        if (gameTick >= until || !player.isAlive()) {
            GOADING.remove(player.getUUID());
            Msg.red(player, "Your goading potion has worn off.");
            return;
        }
        if (gameTick % HerbloreData.Effect.GOADING_INTERVAL != 0) {
            return;
        }
        for (com.runecraft.entity.OsrsMonsterEntity mob : player.level().getEntitiesOfClass(com.runecraft.entity.OsrsMonsterEntity.class,
                player.getBoundingBox().inflate(8), m -> m.isAlive() && m.getTarget() == null)) {
            mob.setTarget(player);
        }
    }

    /** Prayer regeneration potions: one Prayer point every 12 game ticks while active. Called every game tick. */
    static void prayerRegenTick(ServerPlayer player, long gameTick) {
        Long until = PRAYER_REGEN.get(player.getUUID());
        if (until == null) {
            return;
        }
        if (gameTick >= until) {
            PRAYER_REGEN.remove(player.getUUID());
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

    private static void useDose(ServerPlayer player, ItemStack stack, InteractionHand hand, String name, int doses, boolean drank) {
        int used = stack.getDamageValue() + 1;
        int left = doses - used;
        com.runecraft.skill.ConsumeCooldowns.consume(player, stack, drank);
        player.level().playSound(null, player.blockPosition(), drank ? SoundEvents.GENERIC_DRINK : SoundEvents.BOTTLE_EMPTY,
                SoundSource.PLAYERS, 0.5f, 1.0f);
        if (drank) {
            Msg.action(player, "You drink some of your " + SkillingUtil.lower(name) + ". "
                    + (left > 0 ? "You have " + left + " dose" + (left > 1 ? "s" : "") + " of potion left." : "You have finished your potion."));
        }
        if (left <= 0) {
            player.setItemInHand(hand, Inv.stack("vial", 1));
        } else {
            stack.setDamageValue(used);
        }
    }

    /** Keeps divine potion boosts topped up for 5 minutes. Called every game tick. */
    static void divineTick(ServerPlayer player, long gameTick) {
        Divine d = DIVINE.get(player.getUUID());
        if (d == null) {
            return;
        }
        if (gameTick >= d.until()) {
            DIVINE.remove(player.getUUID());
            Msg.red(player, "The effects of the divine potion have worn off.");
            return;
        }
        if (gameTick % 10 == 0) {
            d.effect().apply(Players.data(player).skills());
            ServerNetworking.sync(player);
        }
    }

    // ------------------------------------------------------------------ thrown weapons

    private static void throwItem(ServerPlayer player, ItemStack stack, String id) {
        ItemDef def = ItemCatalog.get(id);
        Arrow arrow = new Arrow(player.level(), player, new ItemStack(Items.ARROW), null);
        arrow.addTag(OsrsEquipmentItems.RANGED_STRENGTH_TAG + def.rangedStrength());
        arrow.addTag("runecraft_arrow_" + id);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        boolean javelin = id.endsWith("_javelin");
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, javelin ? 2.2f : 2.6f, 1.0f);
        player.level().addFreshEntity(arrow);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.swing(InteractionHand.MAIN_HAND, true);
        player.getCooldowns().addCooldown(stack.getItem(), (javelin ? 6 : 3) * ActionManager.MC_TICKS_PER_GAME_TICK);
        player.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    // ------------------------------------------------------------------ essence pouches

    private static void pouch(ServerPlayer player, ItemStack stack, RunecraftData.Pouch pouch) {
        CompoundTag nbt = StackData.get(stack);
        int held = nbt.getInt(ESSENCE_NBT);
        if (player.isShiftKeyDown()) {
            if (held == 0) {
                Msg.game(player, "There is no essence in this pouch.");
                return;
            }
            nbt.putInt(ESSENCE_NBT, 0);
            StackData.set(stack, nbt);
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
        StackData.set(stack, nbt);
        int after = RunecraftData.capacity(pouch, used + add);
        Msg.action(player, "You fill the pouch (" + (held + add) + "/" + capacity + ").");
        if (after < capacity) {
            Msg.red(player, "Your pouch has decayed through use.");
        }
    }

    /** Empties every essence pouch in the inventory (before crafting at an altar). */
    static void emptyPouches(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (s.isEmpty() || RunecraftData.pouch(Inv.idOf(s)) == null) {
                continue;
            }
            CompoundTag nbt = StackData.get(s);
            int held = nbt.getInt(ESSENCE_NBT);
            if (held > 0) {
                StackData.update(s, t -> t.putInt(ESSENCE_NBT, 0));
                Inv.give(player, "pure_essence", held);
            }
        }
    }

    static RandomSource random(ServerPlayer player) {
        return player.getRandom();
    }
}
