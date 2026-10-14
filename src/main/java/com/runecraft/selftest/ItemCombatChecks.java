package com.runecraft.selftest;

import com.runecraft.action.ActionManager;
import com.runecraft.bank.BankService;
import com.runecraft.bank.Notes;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.Equipment;
import com.runecraft.combat.MagicService;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.npcs.ShopService;
import com.runecraft.content.slayer.SlayerService;
import com.runecraft.content.slayer.SlayerState;
import com.runecraft.content.systems.GeService;
import com.runecraft.content.systems.JewelleryService;
import com.runecraft.content.systems.SpellService;
import com.runecraft.content.systems.SysCombat;
import com.runecraft.content.systems.SysState;
import com.runecraft.item.ItemCharges;
import com.runecraft.item.OsrsEquipmentItems;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Rune;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.Spell;
import com.runecraft.osrs.bank.BankModel;
import com.runecraft.osrs.bank.BankQuantity;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.npcs.ShopDef;
import com.runecraft.osrs.content.npcs.ShopStock;
import com.runecraft.osrs.content.npcs.Shops;
import com.runecraft.osrs.content.skilling.CookingData;
import com.runecraft.osrs.content.skilling.HerbloreData;
import com.runecraft.osrs.content.slayer.SlayerCategories;
import com.runecraft.osrs.content.slayer.SlayerMasters;
import com.runecraft.osrs.content.slayer.SlayerTasks;
import com.runecraft.osrs.content.slayer.Superiors;
import com.runecraft.osrs.content.systems.GrandExchange;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.data.EquipmentBonuses;
import com.runecraft.osrs.data.Monster;
import com.runecraft.osrs.data.Potion;
import com.runecraft.player.PlayerData;
import com.runecraft.player.PlayerEvents;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Self-test checks for items, combat and interactions: every food, potion, wearable, prayer and spell, player
 * attacks with each weapon class and combat style, a task from every Slayer master, banking, the Grand Exchange and
 * every shop. Each check starts from {@link TestContext#reset()} plus {@link #fresh()} (the systems module's worn
 * slots, spellbook and autocast are not part of PlayerData, so they are cleared here).
 */
final class ItemCombatChecks {
    /** Attacks tried per combat check until one hits. */
    private static final int ATTACK_TRIES = 25;
    /** NPC attacks rolled per protection-prayer phase. */
    private static final int NPC_ATTACKS = 30;
    /** Game ticks a prayer is left on to measure its drain. */
    private static final int DRAIN_TICKS = 100;
    /** Monster with plenty of Hitpoints and low Defence, used as the combat dummy. */
    private static final String DUMMY = "moss_giant";
    private static final String SHIELD = "rune_kiteshield";
    /** Items handed out before a utility spell, so the common ones (bones to bananas, orbs, superheat...) can take effect. */
    private static final String[][] SPELL_KIT = {{"bones", "3"}, {"big_bones", "2"}, {"unpowered_orb", "2"}, {"vial", "2"},
            {"clay", "2"}, {"iron_ore", "1"}};
    /** Potion keywords and the skills a dose must raise (on a stat-drained player). */
    private static final String[][] POTION_KEYWORDS = {{"attack", "ATTACK"}, {"strength", "STRENGTH"}, {"_str_", "STRENGTH"},
            {"defence", "DEFENCE"}, {"_def_", "DEFENCE"}, {"ranging", "RANGED"}, {"magic", "MAGIC"}, {"combat", "ATTACK,STRENGTH"},
            {"agility", "AGILITY"}, {"fishing", "FISHING"}, {"hunt", "HUNTER"}, {"bastion", "RANGED,DEFENCE"},
            {"battlemage", "MAGIC,DEFENCE"}, {"restore", "ATTACK,STRENGTH,DEFENCE"}};

    /** Damage rolls against {@link #watching} (protection prayer checks). */
    private static final List<Integer> ROLLS = new ArrayList<>();
    private static ServerPlayerEntity watching;
    private static boolean listening;

    private final TestContext c;

    private ItemCombatChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        ItemCombatChecks g = new ItemCombatChecks(context);
        if (!listening) {
            listening = true;
            CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
                if (player == watching) {
                    ROLLS.add(damage);
                }
            });
        }
        List<SelfTest.Check> checks = new ArrayList<>();
        // Food: catalog food plus the module's pies, pizzas and cakes eaten in bites.
        Map<String, Integer> foods = new LinkedHashMap<>();
        for (ItemDef d : ItemCatalog.all()) {
            if (d.kind() == ItemDef.Kind.FOOD && !MinigameRewardChecks.OWN.contains(d.id())) {
                foods.put(d.id(), d.heal());
            }
        }
        for (CookingData.Bite b : CookingData.BITES) {
            foods.putIfAbsent(b.full(), b.heal());
        }
        foods.forEach((id, heal) -> checks.add(g.check("food:" + id, () -> g.food(id, heal))));
        // Potions: catalog potions (brewing kegs excluded: they pour pints) plus the module's potions and mixes.
        Set<String> potions = new LinkedHashSet<>();
        for (ItemDef d : ItemCatalog.all()) {
            if (d.kind() == ItemDef.Kind.POTION && !d.id().endsWith("_keg") && !MinigameRewardChecks.OWN.contains(d.id())) {
                potions.add(d.id());
            }
        }
        HerbloreData.POTIONS.forEach(p -> potions.add(p.id()));
        HerbloreData.MIXES.forEach(m -> potions.add(m.id()));
        for (String id : potions) {
            checks.add(g.check("potion:" + id, () -> g.potion(id)));
        }
        // Wearables: everything with equipment bonuses plus everything worn in the extra (Jewellery) slots.
        Set<String> wearables = new LinkedHashSet<>();
        for (ItemDef d : ItemCatalog.all()) {
            if (d.equipment() != null) {
                wearables.add(d.id());
            }
        }
        for (Jewellery.Worn w : Jewellery.all()) {
            wearables.add(w.id());
        }
        for (String id : wearables) {
            checks.add(g.check("equip:" + id, () -> g.equip(id)));
        }
        for (Prayer p : Prayer.values()) {
            checks.add(g.check("prayer:" + p.name().toLowerCase(Locale.ROOT), () -> g.prayer(p)));
        }
        checks.add(g.check("protect:melee", () -> g.protect(Prayer.PROTECT_FROM_MELEE, AttackType.SLASH)));
        checks.add(g.check("protect:ranged", () -> g.protect(Prayer.PROTECT_FROM_MISSILES, AttackType.RANGED)));
        checks.add(g.check("protect:magic", () -> g.protect(Prayer.PROTECT_FROM_MAGIC, AttackType.MAGIC)));
        for (Spell s : Spell.values()) {
            checks.add(g.check("spell:standard:" + s.name().toLowerCase(Locale.ROOT), () -> g.standardSpell(s)));
        }
        for (Spellbooks.Spell s : Spellbooks.all()) {
            checks.add(g.check("spell:" + s.book().name().toLowerCase(Locale.ROOT) + ":" + s.id(), () -> g.bookSpell(s)));
        }
        checks.add(g.check("attack:unarmed", () -> g.attack(CombatStyle.ACCURATE, null, null)));
        for (CombatStyle style : CombatStyle.values()) {
            String weapon = style.isRanged() ? "magic_shortbow" : "abyssal_whip";
            String ammo = style.isRanged() ? "rune_arrow" : null;
            checks.add(g.check("attack:" + style.name().toLowerCase(Locale.ROOT), () -> g.attack(style, weapon, ammo)));
        }
        checks.add(g.check("attack:magic_staff", g::magicAttack));
        for (SlayerMasters.Master m : SlayerMasters.all()) {
            checks.add(g.check("slayer:" + m.id(), () -> g.slayer(m)));
        }
        checks.add(g.check("bank:deposit_withdraw_notes", g::bank));
        checks.add(g.check("ge:instant_buy_sell", g::geInstant));
        checks.add(g.check("ge:offer_abort_collect", g::geOffer));
        for (ShopDef s : Shops.all()) {
            checks.add(g.check("shop:" + s.id(), () -> g.shop(s)));
        }
        return checks;
    }

    /** Wraps a body so the systems-module state it touches starts and ends clean. */
    private SelfTest.Check check(String name, SelfTest.Body body) {
        return new SelfTest.Check(name, () -> {
            fresh();
            try {
                return body.run();
            } finally {
                fresh();
            }
        });
    }

    private void fresh() {
        watching = null;
        ROLLS.clear();
        SysState.SysPlayer sp = SysState.of(p());
        sp.worn.clear();
        sp.book = Spellbooks.Book.STANDARD;
        sp.swapBook = null;
        sp.autocast = "";
        data().setAutocast(null);
        data().activePrayers().clear();
        SlayerState.Profile prof = SlayerService.profile(p());
        prof.task = null;
        prof.remaining = 0;
        prof.region = null;
    }

    private static AssertionError failure(String message) {
        return new AssertionError(message);
    }

    private ServerPlayerEntity p() {
        return c.player;
    }

    private PlayerData data() {
        return Players.data(c.player);
    }

    private ItemStack stack(String id, int count) {
        ItemStack s = Inv.stack(id, count);
        if (s.isEmpty()) {
            throw failure("item " + id + " is not registered");
        }
        return s;
    }

    /** A right-click with the item in {@code hand}: the use-item callbacks first, then the item's own use (as vanilla does). */
    private void use(Hand hand) {
        TypedActionResult<ItemStack> result = UseItemCallback.EVENT.invoker().interact(p(), c.world, hand);
        if (result.getResult() != ActionResult.PASS) {
            return;
        }
        ItemStack held = p().getStackInHand(hand);
        if (!held.isEmpty()) {
            held.getItem().use(c.world, p(), hand);
        }
    }

    private void setLevel(Skill skill, int level) {
        data().skills().setXpTenths(skill, Experience.xpForLevel(level) * 10);
    }

    private LivingEntity spawn(String id, int dx, int dz) {
        EntityType<?> type = ModEntities.typeById(id);
        if (type == null) {
            throw failure("no entity type " + id);
        }
        return spawn(type, dx, dz);
    }

    private LivingEntity spawn(EntityType<?> type, int dx, int dz) {
        Entity e = type.create(c.world);
        if (!(e instanceof LivingEntity living)) {
            throw failure(net.minecraft.registry.Registries.ENTITY_TYPE.getId(type) + " is not a living entity");
        }
        BlockPos pos = c.near(dx, dz);
        living.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 0f);
        if (living instanceof MobEntity mob) {
            mob.initialize(c.world, c.world.getLocalDifficulty(pos), SpawnReason.SPAWNER, null, null);
            mob.setPersistent();
        }
        c.world.spawnEntity(living);
        return living;
    }

    private static String fmt(double v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    // ------------------------------------------------------------------ food

    private String food(String id, int heal) {
        ItemStack stack = stack(id, 5);
        Item item = stack.getItem();
        if (p().getItemCooldownManager().isCoolingDown(item)) {
            throw new SelfTest.Skip("still on its eat delay from an earlier check");
        }
        p().setStackInHand(Hand.MAIN_HAND, stack);
        p().setHealth(10f);
        use(Hand.MAIN_HAND);
        float hp = p().getHealth();
        int left = Inv.count(p(), id);
        if (left >= 5) {
            throw failure("eating did not use up the food (still " + left + ")");
        }
        if (heal > 0 && hp <= 10f) {
            throw failure("eating did not heal (HP " + fmt(hp) + "/" + fmt(p().getMaxHealth()) + ", heals " + heal + ")");
        }
        if (!p().getItemCooldownManager().isCoolingDown(item)) {
            throw failure("no eat delay after eating");
        }
        // Eating again at once must do nothing (OSRS: 3 game ticks between foods).
        use(Hand.MAIN_HAND);
        if (Inv.count(p(), id) != left || p().getHealth() != hp) {
            throw failure("a second bite went through during the eat delay");
        }
        return "+" + fmt(hp - 10f) + " hp (heals " + heal + "), delay held";
    }

    // ------------------------------------------------------------------ potions

    /** Applies one dose of a potion to a model skill set; returns the Prayer points it restores, or null if unknown. */
    private static Integer model(String id, SkillSet skills) {
        for (Potion p : Potion.values()) {
            if (p.itemId().equals(id)) {
                return p.apply(skills);
            }
        }
        HerbloreData.ModPotion mod = HerbloreData.potion(id);
        if (mod != null) {
            return mod.effect().apply(skills);
        }
        HerbloreData.Mix mix = HerbloreData.mix(id);
        if (mix != null && !mix.base().equals(id)) {
            return model(mix.base(), skills);
        }
        return null;
    }

    private static SkillSet copy(SkillSet s) {
        SkillSet out = new SkillSet();
        out.load(s.rawXp(), s.rawBoosts());
        return out;
    }

    private String potion(String id) {
        ItemStack stack = stack(id, 1);
        Item item = stack.getItem();
        if (p().getItemCooldownManager().isCoolingDown(item)) {
            throw new SelfTest.Skip("still on its drink delay from an earlier check");
        }
        HerbloreData.ModPotion mod = HerbloreData.potion(id);
        HerbloreData.Effect effect = mod == null ? null : mod.effect();
        if (effect == HerbloreData.Effect.SERUM_207) {
            throw new SelfTest.Skip("not drinkable: used on the Afflicted");
        }
        ItemDef catalogDef = ItemCatalog.get(id);
        if (catalogDef != null && !catalogDef.drinkable()) {
            // OSRS only lets you empty these (Guthix balance, olive and sacred oil): using one must not use a dose.
            p().setStackInHand(Hand.MAIN_HAND, stack);
            use(Hand.MAIN_HAND);
            ItemStack left = p().getMainHandStack();
            if (left.isEmpty() || left.getItem() != item || left.getDamage() > 0) {
                throw failure("a potion OSRS doesn't let you drink used a dose");
            }
            return "not drinkable (as in OSRS); no dose used";
        }
        boolean known = mod != null || HerbloreData.mix(id) != null || id.equals("antifire_potion") || id.equals("extended_antifire");
        for (Potion p : Potion.values()) {
            known |= p.itemId().equals(id);
        }
        // The potion goes in the main hand (inventory slot 0) before anything else is handed out.
        p().setStackInHand(Hand.MAIN_HAND, stack);
        if (effect != null && effect.weaponPoison()) {
            p().setStackInHand(Hand.OFF_HAND, stack("bronze_dagger", 1));
        } else if (effect == HerbloreData.Effect.COMPOST) {
            Inv.give(p(), "compost", 1);
        }
        PlayerData data = data();
        SkillSet skills = data.skills();
        for (Skill s : Skill.values()) {
            if (s != Skill.HITPOINTS && s != Skill.PRAYER) {
                skills.setBoost(s, -10);
            }
        }
        data.setPrayerPoints(40);
        p().setHealth(50f);
        SkillSet expected = copy(skills);
        Integer prayer = model(id, expected);
        use(Hand.MAIN_HAND);
        ItemStack after = p().getMainHandStack();
        boolean used = after.isEmpty() || after.getItem() != item || after.getDamage() > 0;
        if (!used) {
            if (!known) {
                throw new SelfTest.Skip("no drink effect registered for this potion");
            }
            throw failure("drinking did not use a dose");
        }
        List<String> wrong = new ArrayList<>();
        List<String> changed = new ArrayList<>();
        if (prayer != null) {
            for (Skill s : Skill.values()) {
                if (s == Skill.HITPOINTS) {
                    continue;
                }
                int got = skills.currentLevel(s);
                int want = expected.currentLevel(s);
                if (got != want) {
                    wrong.add(s.displayName() + " " + got + " (expected " + want + ")");
                }
                if (want != skills.level(s) - 10 && s != Skill.PRAYER) {
                    changed.add(s.displayName() + " " + (want - skills.level(s) >= 0 ? "+" : "") + (want - skills.level(s)));
                }
            }
            if (prayer > 0 && data.prayerPoints() <= 40) {
                throw failure("restores " + prayer + " Prayer points in the model, but Prayer stayed at " + data.prayerPoints());
            }
        }
        if (!wrong.isEmpty()) {
            throw failure("stats after one dose differ from the potion's effect: " + String.join(", ", wrong));
        }
        // The name says which skill the potion is for: that skill must have moved up from its drained level.
        if (!id.startsWith("anti") && !id.startsWith("prayer_regeneration")) {
            for (String[] k : POTION_KEYWORDS) {
                if (!id.contains(k[0])) {
                    continue;
                }
                for (String skill : k[1].split(",")) {
                    Skill s = Skill.valueOf(skill);
                    if (skills.currentLevel(s) <= skills.level(s) - 10 && !(id.contains("brew") && s != Skill.DEFENCE && s != Skill.MAGIC)) {
                        throw failure("a " + id.replace('_', ' ') + " should raise " + s.displayName() + " but it stayed at "
                                + skills.currentLevel(s) + "/" + skills.level(s));
                    }
                }
            }
        }
        if ((id.startsWith("prayer_potion") || id.startsWith("prayer_mix") || id.startsWith("super_restore")) && data.prayerPoints() <= 40) {
            throw failure("Prayer points did not rise (" + data.prayerPoints() + ")");
        }
        String doses = after.isEmpty() || after.getItem() != item ? "finished -> " + (after.isEmpty() ? "nothing" : Inv.idOf(after))
                : "dose used (" + after.getDamage() + " drunk)";
        return doses + (prayer == null ? ", no stat model" : changed.isEmpty() ? ", no stat change" : ", " + String.join(" ", changed))
                + (data.prayerPoints() != 40 ? ", prayer " + data.prayerPoints() : "");
    }

    // ------------------------------------------------------------------ equipment

    private static EquipmentSlot armourSlot(EquipmentDef.Slot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlot.HEAD;
            case CHEST -> EquipmentSlot.CHEST;
            case LEGS -> EquipmentSlot.LEGS;
            case FEET -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    private void put(EquipmentDef.Slot slot, ItemStack stack) {
        EquipmentSlot armour = armourSlot(slot);
        if (armour != null) {
            p().equipStack(armour, stack);
        } else {
            p().setStackInHand(slot == EquipmentDef.Slot.SHIELD ? Hand.OFF_HAND : Hand.MAIN_HAND, stack);
        }
    }

    /** Runs the game tick that enforces worn-armour requirements (every 5th game tick). */
    private void enforceTick() {
        PlayerEvents.gameTick(p(), (ActionManager.gameTick() / 5 + 1) * 5);
    }

    private String equip(String id) {
        ItemDef def = ItemCatalog.get(id);
        Jewellery.Worn worn = Jewellery.worn(id);
        if (def == null) {
            throw new SelfTest.Skip("worn-slot entry with no catalog item (dead id in Jewellery)");
        }
        c.completeQuests(null); // quest-gated gear
        Map<Skill, Integer> reqs = new java.util.EnumMap<>(Skill.class);
        if (def.equipment() != null) {
            // Level-1 requirements are always met, so they can't be tested by dropping the skill to 1.
            def.equipment().requirements().forEach((skill, level) -> {
                if (level > 1) {
                    reqs.put(skill, level);
                }
            });
        }
        if (worn != null) {
            return wearExtra(id, def, worn, reqs);
        }
        EquipmentDef eq = def.equipment();
        EquipmentSlot armour = armourSlot(eq.slot());
        String reqNote = "no requirements";
        if (!reqs.isEmpty()) {
            reqs.keySet().forEach(s -> setLevel(s, 1));
            put(eq.slot(), stack(id, 1));
            if (armour != null) {
                enforceTick();
                if (!p().getEquippedStack(armour).isEmpty()) {
                    throw failure("a level-1 player keeps wearing it although it needs " + reqs);
                }
                if (Inv.count(p(), id) != 1) {
                    throw failure("taking it off for unmet requirements lost the item");
                }
            } else if (!Equipment.total(p(), data().skills()).equals(EquipmentStats.NONE)) {
                throw failure("its bonuses count for a level-1 player although it needs " + reqs);
            } else if (eq.slot() == EquipmentDef.Slot.WEAPON && Equipment.weapon(p(), data().skills()) != null) {
                throw failure("a level-1 player wields it although it needs " + reqs);
            }
            p().getInventory().clear();
            reqs.keySet().forEach(s -> setLevel(s, 99));
            reqNote = "refused at level 1 (" + reqs + ")";
        }
        ItemStack stack = stack(id, 1);
        put(eq.slot(), stack);
        if (armour != null) {
            enforceTick();
            if (p().getEquippedStack(armour).isEmpty()) {
                throw failure("a level-99 player can't keep it on");
            }
        }
        boolean depleted = ItemCharges.depleted(stack);
        EquipmentStats expected = depleted ? EquipmentStats.NONE : eq.stats();
        EquipmentStats total = Equipment.total(p(), data().skills());
        if (!total.equals(expected)) {
            throw failure("equipment bonus totals " + total + " differ from the item's " + expected);
        }
        String extra = "";
        if (eq.slot() == EquipmentDef.Slot.WEAPON) {
            ItemDef shieldDef = ItemCatalog.get(SHIELD);
            p().setStackInHand(Hand.OFF_HAND, stack(SHIELD, 1));
            EquipmentStats with = Equipment.total(p(), data().skills());
            if (EquipmentBonuses.twoHanded(id)) {
                if (!Equipment.twoHanded(p())) {
                    throw failure("two-handed in EquipmentBonuses but Equipment.twoHanded is false");
                }
                if (!with.equals(expected)) {
                    throw failure("an off-hand shield still adds bonuses next to this two-handed weapon: " + with);
                }
                extra = ", two-handed: shield ignored";
            } else if (!with.equals(expected.plus(shieldDef.equipment().stats()))) {
                throw failure("an off-hand shield's bonuses are missing next to this one-handed weapon: " + with);
            } else {
                extra = ", one-handed: shield counts";
            }
        }
        return eq.slot().name().toLowerCase(Locale.ROOT) + ", " + reqNote + (depleted ? ", uncharged: no bonuses" : "") + extra;
    }

    private String wearExtra(String id, ItemDef def, Jewellery.Worn worn, Map<Skill, Integer> reqs) {
        SysState.SysPlayer sp = SysState.of(p());
        String reqNote = "no requirements";
        if (!reqs.isEmpty()) {
            reqs.keySet().forEach(s -> setLevel(s, 1));
            p().setStackInHand(Hand.MAIN_HAND, stack(id, 1));
            JewelleryService.wear(p(), Hand.MAIN_HAND);
            if (!sp.worn(worn.slot()).isEmpty()) {
                throw failure("a level-1 player put it on although it needs " + reqs);
            }
            p().getInventory().clear();
            reqs.keySet().forEach(s -> setLevel(s, 99));
            reqNote = "refused at level 1 (" + reqs + ")";
        }
        p().setStackInHand(Hand.MAIN_HAND, stack(id, 1));
        JewelleryService.wear(p(), Hand.MAIN_HAND);
        if (!sp.wornId(worn.slot()).equals(id)) {
            throw failure("JewelleryService.wear did not put it in the " + worn.slot() + " slot");
        }
        if (!p().getMainHandStack().isEmpty()) {
            throw failure("wearing it left " + p().getMainHandStack() + " in the hand");
        }
        EquipmentStats total = SysCombat.wornStats(p());
        if (!total.equals(worn.stats())) {
            throw failure("worn-slot bonus totals " + total + " differ from the item's " + worn.stats());
        }
        if (def.equipment() != null && !def.equipment().stats().equals(worn.stats())) {
            throw failure("the catalog's bonuses " + def.equipment().stats() + " differ from the worn-slot bonuses " + worn.stats());
        }
        JewelleryService.remove(p(), worn.slot());
        if (!sp.worn(worn.slot()).isEmpty() || Inv.count(p(), id) != 1) {
            throw failure("removing it did not give it back");
        }
        return worn.slot().name().toLowerCase(Locale.ROOT) + " slot, " + reqNote;
    }

    // ------------------------------------------------------------------ prayers

    private String prayer(Prayer prayer) {
        c.completeQuests(null);
        SysState.of(p()).prayerScrolls.add(prayer.name());
        PlayerData data = data();
        PrayerService.toggle(p(), prayer);
        if (!data.isActive(prayer)) {
            throw failure("did not activate at 99 Prayer with every quest done and its scroll read");
        }
        int before = data.prayerPoints();
        data.setPrayerDrainCounter(0);
        long t0 = ActionManager.gameTick();
        for (int i = 0; i < DRAIN_TICKS; i++) {
            PrayerService.gameTick(p(), t0 + i);
        }
        int drained = before - data.prayerPoints();
        // OSRS: the drain counter rises by the prayer's drain effect every tick and a point is lost each time it
        // exceeds the drain resistance (60 + 2 x prayer bonus; no gear here).
        int expected = prayer.drainEffect() <= 0 ? 0 : (DRAIN_TICKS * prayer.drainEffect() - 1) / 60;
        if (drained != expected) {
            throw failure("drained " + drained + " points over " + DRAIN_TICKS + " ticks, expected " + expected
                    + " (drain effect " + prayer.drainEffect() + ", resistance 60)");
        }
        if (!data.isActive(prayer)) {
            throw failure("switched itself off while draining (" + data.prayerPoints() + " points left)");
        }
        PrayerService.toggle(p(), prayer);
        if (data.isActive(prayer)) {
            throw failure("did not deactivate");
        }
        return "drained " + drained + " in " + DRAIN_TICKS + " ticks";
    }

    private String protect(Prayer prayer, AttackType type) {
        c.completeQuests(null);
        // A weak defender so the unprotected baseline gets hit.
        setLevel(Skill.DEFENCE, 1);
        setLevel(Skill.MAGIC, 1);
        LivingEntity mob = spawn(Monster.BLACK_DEMON.entity(), 2, 0);
        watching = p();
        int baseline = 0;
        for (int i = 0; i < NPC_ATTACKS; i++) {
            CombatHandler.clearCooldown(mob);
            CombatHandler.npcAttack(mob, p(), type);
            c.heal();
        }
        int rolled = ROLLS.size();
        for (int d : ROLLS) {
            if (d > 0) {
                baseline++;
            }
        }
        ROLLS.clear();
        PrayerService.toggle(p(), prayer);
        if (!data().isActive(prayer)) {
            throw failure("could not activate " + prayer.displayName());
        }
        int through = 0;
        for (int i = 0; i < NPC_ATTACKS; i++) {
            CombatHandler.clearCooldown(mob);
            CombatHandler.npcAttack(mob, p(), type);
            c.heal();
        }
        if (ROLLS.isEmpty() && rolled == 0) {
            throw new SelfTest.Skip("no NPC attack reached the damage listener (CombatZones refused?)");
        }
        for (int d : ROLLS) {
            if (d > 0) {
                through++;
            }
        }
        watching = null;
        if (through > 0) {
            throw failure(through + "/" + ROLLS.size() + " " + type.name().toLowerCase(Locale.ROOT) + " hits went through "
                    + prayer.displayName() + " (" + baseline + "/" + rolled + " hit without it)");
        }
        return baseline + "/" + rolled + " hit unprotected, 0/" + ROLLS.size() + " protected";
    }

    // ------------------------------------------------------------------ spells

    private void giveRunes(Map<String, Integer> runes) {
        runes.forEach((rune, n) -> Inv.give(p(), rune, n * 2));
    }

    private Map<String, Integer> counts(Iterable<String> items) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String id : items) {
            m.put(id, Inv.count(p(), id));
        }
        return m;
    }

    private boolean anyFewer(Map<String, Integer> before) {
        for (Map.Entry<String, Integer> e : before.entrySet()) {
            if (Inv.count(p(), e.getKey()) < e.getValue()) {
                return true;
            }
        }
        return false;
    }

    private int magicXp() {
        return data().skills().xpTenths(Skill.MAGIC);
    }

    /** Ticks a delayed cast (home teleports) until its action ends. */
    private void finishAction() {
        for (int i = 0; i < 24 && ActionManager.tickNow(p()); i++) {
            // one game tick per call
        }
    }

    private String landed(int blockX, int blockZ) {
        BlockPos at = p().getBlockPos();
        double d = Math.hypot(at.getX() - blockX, at.getZ() - blockZ);
        if (d > 48) {
            throw failure("ended at " + at.getX() + " " + at.getY() + " " + at.getZ() + ", " + (int) d + " blocks from the destination "
                    + blockX + " " + blockZ);
        }
        return "landed " + at.getX() + " " + at.getY() + " " + at.getZ();
    }

    private String standardSpell(Spell spell) {
        Map<String, Integer> runes = new LinkedHashMap<>();
        for (Map.Entry<Rune, Integer> e : spell.runes().entrySet()) {
            runes.put(e.getKey().itemId(), e.getValue());
        }
        String alchItem = "rune_scimitar";
        if (spell.kind() == Spell.Kind.ALCHEMY) {
            p().setStackInHand(Hand.MAIN_HAND, stack(alchItem, 1)); // before the runes take slot 0
        }
        giveRunes(runes);
        Map<String, Integer> before = counts(runes.keySet());
        int xp0 = magicXp();
        switch (spell.kind()) {
            case COMBAT -> {
                LivingEntity dummy = spawn(DUMMY, 3, 0);
                MagicService.setAutocast(p(), spell);
                if (data().autocast() != spell) {
                    throw failure("could not be set to autocast at 99 Magic");
                }
                data().lastAttackTick = ActionManager.mcTick() - 10_000;
                MagicService.autocastAttack(p(), dummy);
                if (!anyFewer(before)) {
                    throw failure("casting at a " + DUMMY + " used no runes");
                }
                if (magicXp() - xp0 < spell.xpTenths()) {
                    throw failure("gave " + (magicXp() - xp0) / 10.0 + " Magic XP, less than the base " + spell.xpTenths() / 10.0);
                }
                return "cast, +" + (magicXp() - xp0) / 10.0 + " xp, dummy at " + fmt(dummy.getHealth()) + "/" + fmt(dummy.getMaxHealth());
            }
            case TELEPORT -> {
                MagicService.castFromSpellbook(p(), spell);
                finishAction();
                if (!spell.runes().isEmpty() && !anyFewer(before)) {
                    throw failure("teleporting used no runes");
                }
                int[] tile = spell.tile();
                if (tile != null) {
                    return landed(GielinorMap.toBlockX(tile[0]), GielinorMap.toBlockZ(tile[1]));
                }
                if (spell.destination() != null) {
                    return landed(spell.destination().blockX(), spell.destination().blockZ());
                }
                return "no destination";
            }
            case ALCHEMY -> {
                String item = alchItem;
                ItemDef def = ItemCatalog.get(item);
                int coins0 = Inv.count(p(), Inv.COINS);
                MagicService.castFromSpellbook(p(), spell);
                int gained = Inv.count(p(), Inv.COINS) - coins0;
                int want = spell == Spell.HIGH_LEVEL_ALCHEMY ? def.highAlch() : def.lowAlch();
                if (Inv.count(p(), item) != 0) {
                    throw failure("the " + item + " was not used up");
                }
                if (gained != want) {
                    throw failure("gave " + gained + " coins for a " + item + ", expected " + want);
                }
                return item + " -> " + gained + " coins";
            }
            case UTILITY -> {
                Inv.give(p(), "iron_ore", 1);
                MagicService.castFromSpellbook(p(), spell);
                if (spell == Spell.SUPERHEAT_ITEM && Inv.count(p(), "iron_bar") != 1) {
                    throw failure("superheating an iron ore made " + Inv.count(p(), "iron_bar") + " iron bars");
                }
                return (anyFewer(before) ? "cast" : "no runes used") + ", +" + (magicXp() - xp0) / 10.0 + " xp";
            }
            default -> {
                return "unknown kind";
            }
        }
    }

    private String bookSpell(Spellbooks.Spell s) {
        c.completeQuests(null);
        SysState.SysPlayer sp = SysState.of(p());
        sp.book = s.book();
        if (s.item() != null) {
            ItemStack weapon = Inv.stack(s.item(), 1);
            if (weapon.isEmpty()) {
                for (ItemDef d : ItemCatalog.all()) {
                    if (d.id().startsWith(s.item())) {
                        weapon = Inv.stack(d.id(), 1);
                        break;
                    }
                }
            }
            if (weapon.isEmpty()) {
                throw failure("needs " + s.item() + " wielded, but no such item exists");
            }
            p().setStackInHand(Hand.MAIN_HAND, weapon);
        }
        giveRunes(s.runes());
        Map<String, Integer> before = counts(s.runes().keySet());
        int xp0 = magicXp();
        switch (s.kind()) {
            case COMBAT -> {
                if (s.effect() == Spellbooks.Effect.TELEBLOCK) {
                    throw new SelfTest.Skip("Tele Block only targets players");
                }
                String target = s.effect() == Spellbooks.Effect.UNDEAD ? "minecraft:zombie"
                        : s.effect() == Spellbooks.Effect.DEMON ? "lesser_demon" : DUMMY;
                LivingEntity dummy = spawn(target, 3, 0);
                sp.autocast = s.id();
                data().lastAttackTick = ActionManager.mcTick() - 10_000;
                if (!SpellService.retaliate(p(), dummy)) {
                    throw failure("SpellService.retaliate refused the autocast spell");
                }
                int xp = magicXp() - xp0;
                if (xp < Experience.tenths(s.xp())) {
                    throw failure("casting at a " + target + " gave " + xp / 10.0 + " Magic XP, less than the base " + s.xp()
                            + (anyFewer(before) ? "" : " (no runes used: not cast)"));
                }
                return "cast at " + target + ", +" + xp / 10.0 + " xp, target at " + fmt(dummy.getHealth()) + "/" + fmt(dummy.getMaxHealth());
            }
            case TELEPORT -> {
                SpellService.cast(p(), s.id());
                finishAction();
                if (!s.runes().isEmpty() && !anyFewer(before)) {
                    throw failure("teleporting used no runes");
                }
                if (s.destination() != null) {
                    return landed(GielinorMap.toBlockX(s.destination().x()), GielinorMap.toBlockZ(s.destination().y()));
                }
                return "no destination";
            }
            default -> {
                for (String[] k : SPELL_KIT) {
                    if (!Inv.stack(k[0], 1).isEmpty()) {
                        Inv.give(p(), k[0], Integer.parseInt(k[1]));
                    }
                }
                int bananas = Inv.count(p(), "banana");
                SpellService.cast(p(), s.id());
                finishAction();
                if (s.id().equals("bones_to_bananas") && ItemCatalog.get("banana") != null && Inv.count(p(), "banana") <= bananas) {
                    throw failure("bones did not turn into bananas");
                }
                boolean cast = anyFewer(before) || s.runes().isEmpty() && magicXp() > xp0;
                return (cast ? "cast" : "not cast here (needs a target or items)") + ", +" + (magicXp() - xp0) / 10.0 + " xp";
            }
        }
    }

    // ------------------------------------------------------------------ player attacks

    /** Expected XP (tenths) for one hit of {@code d} damage, per OSRS: 4 per damage (2+2 longrange, 1.33 each controlled) + 1.33 HP. */
    private static void expectXp(Map<Skill, Integer> want, CombatStyle style, int d) {
        int third = (int) Math.round(d * 13.3);
        switch (style) {
            case ACCURATE -> want.merge(Skill.ATTACK, d * 40, Integer::sum);
            case AGGRESSIVE -> want.merge(Skill.STRENGTH, d * 40, Integer::sum);
            case DEFENSIVE -> want.merge(Skill.DEFENCE, d * 40, Integer::sum);
            case CONTROLLED -> {
                want.merge(Skill.ATTACK, third, Integer::sum);
                want.merge(Skill.STRENGTH, third, Integer::sum);
                want.merge(Skill.DEFENCE, third, Integer::sum);
            }
            case RANGED_ACCURATE, RAPID -> want.merge(Skill.RANGED, d * 40, Integer::sum);
            case LONGRANGE -> {
                want.merge(Skill.RANGED, d * 20, Integer::sum);
                want.merge(Skill.DEFENCE, d * 20, Integer::sum);
            }
        }
        want.merge(Skill.HITPOINTS, third, Integer::sum);
    }

    private static final Skill[] COMBAT_SKILLS = {Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.HITPOINTS};

    private Map<Skill, Integer> combatXp() {
        Map<Skill, Integer> m = new HashMap<>();
        for (Skill s : COMBAT_SKILLS) {
            m.put(s, data().skills().xpTenths(s));
        }
        return m;
    }

    private String compareXp(Map<Skill, Integer> start, Map<Skill, Integer> want, int dealt, int hits, int tries) {
        List<String> wrong = new ArrayList<>();
        List<String> got = new ArrayList<>();
        for (Skill s : COMBAT_SKILLS) {
            int gained = data().skills().xpTenths(s) - start.get(s);
            int expected = want.getOrDefault(s, 0);
            if (gained != expected) {
                wrong.add(s.displayName() + " +" + gained / 10.0 + " (expected +" + expected / 10.0 + ")");
            }
            if (gained != 0) {
                got.add(s.displayName() + " +" + gained / 10.0);
            }
        }
        if (!wrong.isEmpty()) {
            throw failure("after " + dealt + " damage in " + hits + " hits: " + String.join(", ", wrong));
        }
        return dealt + " damage in " + hits + "/" + tries + " attacks, " + String.join(", ", got);
    }

    private String attack(CombatStyle style, String weapon, String ammo) {
        if (weapon != null) {
            p().setStackInHand(Hand.MAIN_HAND, stack(weapon, 1));
        }
        data().setStyle(style);
        LivingEntity dummy = spawn(DUMMY, 2, 0);
        RegistryEntry<DamageType> osrsHit = c.world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(CombatHandler.OSRS_HIT);
        Map<Skill, Integer> start = combatXp();
        Map<Skill, Integer> want = new HashMap<>();
        int dealt = 0;
        int hits = 0;
        int tries = 0;
        while (tries < ATTACK_TRIES && hits == 0) {
            tries++;
            dummy.setHealth(dummy.getMaxHealth());
            data().lastAttackTick = ActionManager.mcTick() - 10_000;
            float hp = dummy.getHealth();
            if (ammo == null) {
                dummy.damage(CombatHandler.source(p()), 1f);
            } else {
                if (!(Inv.item(ammo) instanceof OsrsEquipmentItems.Arrow arrowItem)) {
                    throw failure(ammo + " is not an arrow item");
                }
                PersistentProjectileEntity arrow = arrowItem.createArrow(c.world, stack(ammo, 1), p());
                dummy.damage(new DamageSource(osrsHit, arrow, p()), 1f);
            }
            int d = Math.round(hp - dummy.getHealth());
            if (d > 0) {
                hits++;
                dealt += d;
                expectXp(want, style, d);
            }
        }
        if (hits == 0) {
            throw failure("no hit in " + ATTACK_TRIES + " attacks on a " + DUMMY + " at 99 combat");
        }
        return (weapon == null ? "unarmed" : weapon) + ": " + compareXp(start, want, dealt, hits, tries);
    }

    private String magicAttack() {
        Spell spell = Spell.FIRE_STRIKE;
        p().setStackInHand(Hand.MAIN_HAND, stack("staff_of_air", 1));
        for (Map.Entry<Rune, Integer> e : spell.runes().entrySet()) {
            Inv.give(p(), e.getKey().itemId(), e.getValue() * (ATTACK_TRIES + 1));
        }
        MagicService.setAutocast(p(), spell);
        LivingEntity dummy = spawn(DUMMY, 2, 0);
        Map<Skill, Integer> start = combatXp();
        Map<Skill, Integer> want = new HashMap<>();
        int dealt = 0;
        int hits = 0;
        int tries = 0;
        while (tries < ATTACK_TRIES && hits == 0) {
            tries++;
            dummy.setHealth(dummy.getMaxHealth());
            data().lastAttackTick = ActionManager.mcTick() - 10_000;
            float hp = dummy.getHealth();
            MagicService.autocastAttack(p(), dummy);
            want.merge(Skill.MAGIC, spell.xpTenths(), Integer::sum);
            int d = Math.round(hp - dummy.getHealth());
            if (d > 0) {
                hits++;
                dealt += d;
                // OSRS: base spell XP + 2 Magic XP per damage + 1.33 Hitpoints.
                want.merge(Skill.MAGIC, d * 20, Integer::sum);
                want.merge(Skill.HITPOINTS, (int) Math.round(d * 13.3), Integer::sum);
            }
        }
        if (Inv.count(p(), "air_rune") > 0 && Inv.count(p(), "air_rune") < spell.runes().getOrDefault(Rune.AIR, 0) * (ATTACK_TRIES + 1)) {
            throw failure("the staff of air did not provide the air runes");
        }
        if (hits == 0) {
            throw failure("no hit in " + ATTACK_TRIES + " casts on a " + DUMMY + " at 99 Magic");
        }
        return "staff_of_air + " + spell.displayName() + ": " + compareXp(start, want, dealt, hits, tries);
    }

    // ------------------------------------------------------------------ slayer

    /** An entity type whose kill counts towards {@code task}. */
    private static EntityType<?> slayerTarget(String task) {
        for (MonsterDef m : SlayerTasks.creatures(task)) {
            if (Superiors.byId(m.id) != null) {
                continue;
            }
            EntityType<?> t = ModEntities.typeById(m.id);
            if (t != null) {
                return t;
            }
        }
        SlayerCategories.Category cat = SlayerCategories.get(task);
        if (cat != null) {
            for (String b : cat.builtIns()) {
                try {
                    EntityType<?> t = ModEntities.typeById(Monster.valueOf(b).entity());
                    if (t != null) {
                        return t;
                    }
                } catch (IllegalArgumentException ignored) {
                    // not a Monster constant
                }
            }
        }
        return ModEntities.typeById(task);
    }

    private void killAsPlayer(LivingEntity victim) {
        victim.setHealth(0f);
        victim.onDeath(CombatHandler.source(p()));
    }

    private String slayer(SlayerMasters.Master master) {
        c.completeQuests(null);
        SlayerState.Profile prof = SlayerService.profile(p());
        SlayerService.talk(p(), master);
        if (!prof.hasTask()) {
            throw failure("gave no task to a maxed player");
        }
        String task = prof.task;
        String note = "task " + prof.amount + " x " + task;
        EntityType<?> type = slayerTarget(task);
        if (type == null) {
            throw failure("no creature counts towards the task " + task);
        }
        int credit = SlayerTasks.killCredit(task, prof.unlocks);
        if (prof.region != null) {
            // Konar: a kill outside her region must not count (the platform is above Lumbridge).
            int before = prof.remaining;
            killAsPlayer(spawn(type, 3, 0));
            if (prof.remaining != before && !prof.region.equals(com.runecraft.world.Gielinor.region(p()).name())) {
                throw failure("a kill outside Konar's region " + prof.region + " counted");
            }
            note += ", outside " + prof.region + " ignored";
            prof.region = null;
        }
        int before = prof.remaining;
        killAsPlayer(spawn(type, 3, 0));
        int drop = before - prof.remaining;
        if (master.wilderness()) {
            if (drop != 0) {
                throw failure("a kill outside the Wilderness counted towards Krystilia's task");
            }
            return note + ", kill outside the Wilderness ignored";
        }
        if (drop != credit) {
            throw failure("killing a " + net.minecraft.registry.Registries.ENTITY_TYPE.getId(type) + " lowered the count by " + drop
                    + ", expected " + credit + " (" + note + ")");
        }
        return note + ", killed " + net.minecraft.registry.Registries.ENTITY_TYPE.getId(type).getPath() + ": " + before + " -> " + prof.remaining;
    }

    // ------------------------------------------------------------------ bank, Grand Exchange, shops

    private int slotOf(String id, boolean note) {
        for (int i = 0; i < 36; i++) {
            ItemStack s = p().getInventory().getStack(i);
            if (!s.isEmpty() && (note ? Notes.isNote(s) && Notes.notedId(s).equals(id) : Inv.idOf(s).equals(id))) {
                return i;
            }
        }
        return -1;
    }

    private String bank() {
        String item = "lobster";
        BankModel bank = data().bank();
        Inv.give(p(), item, 10);
        BankService.open(p());
        BankService.selfTestAction(p(), BankService.OP_DEPOSIT, "", slotOf(item, false), BankQuantity.Mode.ALL, 0);
        if (Inv.count(p(), item) != 0 || bank.count(item, "") != 10) {
            throw failure("deposit-all of 10 left " + Inv.count(p(), item) + " in the inventory and " + bank.count(item, "") + " in the bank");
        }
        BankService.selfTestAction(p(), BankService.OP_WITHDRAW, item, 0, BankQuantity.Mode.ONE, 0);
        BankService.selfTestAction(p(), BankService.OP_WITHDRAW, item, 0, BankQuantity.Mode.FIVE, 0);
        if (Inv.count(p(), item) != 6 || bank.count(item, "") != 4) {
            throw failure("withdrawing 1 then 5 gave " + Inv.count(p(), item) + " (bank " + bank.count(item, "") + ")");
        }
        BankService.selfTestAction(p(), BankService.OP_SET_NOTED, "", 1, BankQuantity.Mode.ONE, 0);
        BankService.selfTestAction(p(), BankService.OP_WITHDRAW, item, 0, BankQuantity.Mode.ALL, 0);
        int noteSlot = slotOf(item, true);
        if (noteSlot < 0 || p().getInventory().getStack(noteSlot).getCount() != 4 || bank.count(item, "") != 0) {
            throw failure("withdrawing 4 as notes gave " + (noteSlot < 0 ? "no note" : p().getInventory().getStack(noteSlot).getCount() + " notes")
                    + " (bank " + bank.count(item, "") + ")");
        }
        if (Notes.countWithNotes(p(), item) != 10) {
            throw failure("countWithNotes says " + Notes.countWithNotes(p(), item) + ", expected 10");
        }
        // Deposit-inventory: the notes go back as the item they stand for.
        BankService.selfTestAction(p(), BankService.OP_DEPOSIT_INVENTORY, "", 0, BankQuantity.Mode.ONE, 0);
        if (bank.count(item, "") != 10 || Notes.countWithNotes(p(), item) != 0) {
            throw failure("deposit-inventory left " + Notes.countWithNotes(p(), item) + " carried and " + bank.count(item, "") + " banked");
        }
        BankService.selfTestAction(p(), BankService.OP_WITHDRAW, item, 0, BankQuantity.Mode.ALL, 0);
        BankService.selfTestAction(p(), BankService.OP_SET_NOTED, "", 0, BankQuantity.Mode.ONE, 0);
        BankService.selfTestAction(p(), BankService.OP_CLOSE, "", 0, BankQuantity.Mode.ONE, 0);
        // A banker exchanges notes held in hand for items.
        noteSlot = slotOf(item, true);
        if (noteSlot < 0) {
            throw failure("no notes after withdrawing as notes again");
        }
        ItemStack notes = p().getInventory().getStack(noteSlot).copy();
        p().getInventory().setStack(noteSlot, ItemStack.EMPTY);
        p().setStackInHand(Hand.MAIN_HAND, notes);
        BankService.open(p());
        if (Inv.count(p(), item) != 10 || slotOf(item, true) >= 0 || Notes.isNote(p().getMainHandStack())) {
            throw failure("un-noting 10 notes left " + Inv.count(p(), item) + " lobsters and " + (slotOf(item, true) >= 0 ? "notes" : "no notes"));
        }
        BankService.selfTestAction(p(), BankService.OP_CLOSE, "", 0, BankQuantity.Mode.ONE, 0);
        return "deposit-all, withdraw 1/5, noted withdraw, deposit-inventory, un-note at a banker OK";
    }

    private String geInstant() {
        String item = "lobster";
        GeService.openAtBooth(p());
        Inv.give(p(), Inv.COINS, 1_000_000);
        int guide = GeService.selfTestGuide(item);
        int each = GrandExchange.npcBuyPrice(guide);
        long coins0 = Inv.count(p(), Inv.COINS);
        Map<String, String> f = new HashMap<>();
        f.put("item", item);
        f.put("qty", "10");
        GeService.selfTestAction(p(), "ibuy", f);
        int got = Notes.countWithNotes(p(), item);
        long paid = coins0 - Inv.count(p(), Inv.COINS);
        if (got <= 0) {
            throw new SelfTest.Skip("instant buy gave nothing (buy limit used up in this world?)");
        }
        if (paid != (long) each * got) {
            throw failure("bought " + got + " for " + paid + " coins, expected " + each + " each (guide " + guide + ")");
        }
        int sellEach = GrandExchange.npcSellPrice(guide);
        long net = (sellEach - GrandExchange.taxPerItem(sellEach)) * (long) got;
        long coins1 = Inv.count(p(), Inv.COINS);
        f.put("qty", Integer.toString(got));
        GeService.selfTestAction(p(), "isell", f);
        long gained = Inv.count(p(), Inv.COINS) - coins1;
        if (Notes.countWithNotes(p(), item) != 0 || gained != net) {
            throw failure("selling " + got + " gave " + gained + " coins (expected " + net + "), " + Notes.countWithNotes(p(), item) + " left");
        }
        return "bought " + got + " @" + each + ", sold @" + sellEach + " (-" + GrandExchange.taxPerItem(sellEach) + " tax)";
    }

    private String geOffer() {
        String item = "nature_rune";
        GeService.openAtBooth(p());
        Inv.give(p(), Inv.COINS, 1000);
        long coins0 = Inv.count(p(), Inv.COINS);
        String uuid = p().getUuidAsString();
        GrandExchange ge = SysState.get(c.server).ge();
        Set<Long> old = new java.util.HashSet<>();
        for (GrandExchange.Offer o : ge.offersOf(uuid)) {
            old.add(o.id);
        }
        Map<String, String> f = new HashMap<>();
        f.put("item", item);
        f.put("qty", "5");
        f.put("price", "1");
        GeService.selfTestAction(p(), "buy", f);
        GrandExchange.Offer mine = null;
        for (GrandExchange.Offer o : ge.offersOf(uuid)) {
            if (!old.contains(o.id)) {
                mine = o;
            }
        }
        if (mine == null) {
            throw failure("placing a buy offer of 5 at 1 gp created no offer");
        }
        if (coins0 - Inv.count(p(), Inv.COINS) != 5) {
            throw failure("the offer took " + (coins0 - Inv.count(p(), Inv.COINS)) + " coins in escrow, expected 5");
        }
        GeService.selfTestAction(p(), "abort:" + mine.id, new HashMap<>());
        GeService.selfTestAction(p(), "collect", new HashMap<>());
        long back = Inv.count(p(), Inv.COINS) + Inv.count(p(), item);
        if (back != coins0) {
            throw failure("after abort + collect: " + Inv.count(p(), Inv.COINS) + " coins and " + Inv.count(p(), item)
                    + " runes, expected the " + coins0 + " coins back");
        }
        return "offer escrowed 5 gp, aborted and collected";
    }

    private String shop(ShopDef def) {
        ShopStock stock = ShopService.selfTestStock(def.id());
        if (stock == null) {
            throw failure("no live stock for this shop");
        }
        ShopStock.Line line = null;
        for (ShopStock.Line l : stock.lines()) {
            if (stock.count(l.item()) > 0 && stock.value(l.item()) > 0 && !Inv.stack(l.item(), 1).isEmpty()
                    && (line == null || stock.priceToBuy(l.item()) < stock.priceToBuy(line.item()))) {
                line = l;
            }
        }
        if (line == null) {
            throw new SelfTest.Skip("nothing in stock with a value");
        }
        String item = line.item();
        int value = stock.value(item);
        int base = stock.base(item);
        int count = stock.count(item);
        // OSRS: price = value x max(10%, sell% - change% x (stock - normal stock)), at least 1.
        int rate = Math.max(100, def.sellPm() - def.deltaPm() * (count - base));
        int expected = (int) Math.max(1L, (long) value * rate / 1000L);
        boolean coins = def.currency().equals(Inv.COINS);
        int give = coins ? expected + 1_000_000 : expected + 10;
        if (!coins && give > 30 * stack(def.currency(), 1).getMaxCount()) {
            throw new SelfTest.Skip("the cheapest item costs " + expected + " " + def.currency() + ", more than an inventory holds");
        }
        Inv.give(p(), def.currency(), give);
        ShopService.open(p(), def.id(), p().getPos());
        int cur0 = Inv.count(p(), def.currency());
        int have0 = Inv.count(p(), item);
        ShopService.selfTestBuy(p(), def.id(), item, 1);
        int paid = cur0 - Inv.count(p(), def.currency());
        int got = Inv.count(p(), item) - have0;
        if (got != 1 || paid != expected) {
            throw failure("buying 1 " + item + " (value " + value + ", stock " + count + "/" + base + ") gave " + got + " for " + paid
                    + ", expected 1 for " + expected);
        }
        String note = "bought " + item + " for " + paid;
        if (stock.accepts(item, value)) {
            int now = stock.count(item);
            // The shop pays value x max(10%, buy% - change% x (stock - normal)), never more than it would sell the next one for.
            int sellRate = Math.max(100, def.sellPm() - def.deltaPm() * (now + 1 - base));
            int buyRate = Math.min(Math.max(100, def.buyPm() - def.deltaPm() * (now - base)), sellRate);
            int pay = (int) ((long) value * buyRate / 1000L);
            int cur1 = Inv.count(p(), def.currency());
            ShopService.selfTestSell(p(), def.id(), item, 1);
            int gained = Inv.count(p(), def.currency()) - cur1;
            if (Inv.count(p(), item) != have0 || gained != pay) {
                throw failure("selling it back paid " + gained + ", expected " + pay + " (stock " + now + "/" + base + ")");
            }
            note += ", sold back for " + gained;
        } else {
            note += ", shop doesn't buy it back";
        }
        return note + " " + def.currency();
    }
}
