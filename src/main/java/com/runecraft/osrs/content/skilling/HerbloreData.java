package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.ItemSink;

import java.util.List;
import java.util.function.Consumer;

/**
 * Herblore: super combat, stamina, super energy, super antipoison, antidote+/++, anti-venom(+),
 * combat/agility/fishing/hunter potions, zamorak brew, bastion/battlemage, divine potions, antifire
 * (CommonItems' antifire_potion / extended_antifire) and super antifire, weapon poisons, guthix rest
 * and compost potion, with their secondaries. Levels/XP are OSRS values (XP for 4-dose recipes).
 * Effects are applied by SkillingHooks through {@link Effect}.
 */
public final class HerbloreData extends SkillingPart {

    /** A potion added by this module and how a dose works. */
    public enum Effect {
        SUPER_COMBAT, STAMINA, SUPER_ENERGY, SUPER_ANTIPOISON, ANTIDOTE_PLUS, ANTIDOTE_PLUS_PLUS, ANTI_VENOM, ANTI_VENOM_PLUS,
        COMBAT, AGILITY, FISHING, HUNTER, ZAMORAK_BREW, BASTION, BATTLEMAGE, DIVINE_SUPER_ATTACK, DIVINE_SUPER_STRENGTH,
        DIVINE_SUPER_DEFENCE, DIVINE_RANGING, DIVINE_MAGIC, DIVINE_SUPER_COMBAT, DIVINE_BASTION, DIVINE_BATTLEMAGE,
        SUPER_ANTIFIRE, EXTENDED_SUPER_ANTIFIRE, WEAPON_POISON, WEAPON_POISON_PLUS, WEAPON_POISON_PLUS_PLUS, GUTHIX_REST,
        COMPOST;

        /** Applies the stat part of one dose (boosts/drains). Returns prayer points restored. */
        public int apply(SkillSet s) {
            switch (this) {
                case SUPER_COMBAT, DIVINE_SUPER_COMBAT -> {
                    s.applyBoost(Skill.ATTACK, 5, 0.15);
                    s.applyBoost(Skill.STRENGTH, 5, 0.15);
                    s.applyBoost(Skill.DEFENCE, 5, 0.15);
                }
                case DIVINE_SUPER_ATTACK -> s.applyBoost(Skill.ATTACK, 5, 0.15);
                case DIVINE_SUPER_STRENGTH -> s.applyBoost(Skill.STRENGTH, 5, 0.15);
                case DIVINE_SUPER_DEFENCE -> s.applyBoost(Skill.DEFENCE, 5, 0.15);
                case DIVINE_RANGING -> s.applyBoost(Skill.RANGED, 4, 0.10);
                case DIVINE_MAGIC -> s.applyBoost(Skill.MAGIC, 4, 0.0);
                case COMBAT -> {
                    s.applyBoost(Skill.ATTACK, 3, 0.10);
                    s.applyBoost(Skill.STRENGTH, 3, 0.10);
                }
                case AGILITY -> s.applyBoost(Skill.AGILITY, 3, 0.0);
                case FISHING -> s.applyBoost(Skill.FISHING, 3, 0.0);
                case HUNTER -> s.applyBoost(Skill.HUNTER, 3, 0.0);
                case BASTION, DIVINE_BASTION -> {
                    s.applyBoost(Skill.RANGED, 4, 0.10);
                    s.applyBoost(Skill.DEFENCE, 5, 0.15);
                }
                case BATTLEMAGE, DIVINE_BATTLEMAGE -> {
                    s.applyBoost(Skill.MAGIC, 4, 0.0);
                    s.applyBoost(Skill.DEFENCE, 5, 0.15);
                }
                case ZAMORAK_BREW -> {
                    s.applyBoost(Skill.ATTACK, 2, 0.20);
                    s.applyBoost(Skill.STRENGTH, 2, 0.12);
                    int drain = 2 + (int) Math.floor(s.currentLevel(Skill.DEFENCE) * 0.10);
                    s.setBoost(Skill.DEFENCE, s.boost(Skill.DEFENCE) - drain);
                    return (int) Math.floor(s.level(Skill.PRAYER) * 0.10);
                }
                default -> {
                }
            }
            return 0;
        }

        /** Divine potions keep their boost for 5 minutes and cost 10 hitpoints per dose. */
        public boolean divine() {
            return name().startsWith("DIVINE_");
        }

        /** Weapon poisons are applied to the wielded weapon rather than drunk. */
        public boolean weaponPoison() {
            return name().startsWith("WEAPON_POISON");
        }

        /** Poison damage a poisoned weapon inflicts. */
        public int poisonDamage() {
            return switch (this) {
                case WEAPON_POISON -> 4;
                case WEAPON_POISON_PLUS -> 5;
                case WEAPON_POISON_PLUS_PLUS -> 6;
                default -> 0;
            };
        }
    }

    /** item id, display name, effect, colour. */
    public record ModPotion(String id, String name, Effect effect, int color, int value) {
    }

    public static final List<ModPotion> POTIONS = List.of(
            new ModPotion("super_combat_potion", "Super combat potion", Effect.SUPER_COMBAT, 0x2A8A3A, 900),
            new ModPotion("stamina_potion", "Stamina potion", Effect.STAMINA, 0xC08050, 800),
            new ModPotion("super_energy", "Super energy", Effect.SUPER_ENERGY, 0xC060A0, 300),
            new ModPotion("superantipoison", "Superantipoison", Effect.SUPER_ANTIPOISON, 0xE0D0A0, 250),
            new ModPotion("antidote_plus", "Antidote+", Effect.ANTIDOTE_PLUS, 0xA0A050, 400),
            new ModPotion("antidote_plus_plus", "Antidote++", Effect.ANTIDOTE_PLUS_PLUS, 0x7A8A30, 600),
            new ModPotion("anti_venom", "Anti-venom", Effect.ANTI_VENOM, 0x3A6A4A, 1200),
            new ModPotion("anti_venom_plus", "Anti-venom+", Effect.ANTI_VENOM_PLUS, 0x2A5A3A, 1800),
            new ModPotion("combat_potion", "Combat potion", Effect.COMBAT, 0x50B090, 150),
            new ModPotion("agility_potion", "Agility potion", Effect.AGILITY, 0x60C0C0, 150),
            new ModPotion("fishing_potion", "Fishing potion", Effect.FISHING, 0x4080D0, 200),
            new ModPotion("hunter_potion", "Hunter potion", Effect.HUNTER, 0x6A8A4A, 200),
            new ModPotion("zamorak_brew", "Zamorak brew", Effect.ZAMORAK_BREW, 0xC0A030, 350),
            new ModPotion("bastion_potion", "Bastion potion", Effect.BASTION, 0x6A3A2A, 600),
            new ModPotion("battlemage_potion", "Battlemage potion", Effect.BATTLEMAGE, 0x3A3A6A, 600),
            new ModPotion("divine_super_attack", "Divine super attack potion", Effect.DIVINE_SUPER_ATTACK, 0x5050E0, 700),
            new ModPotion("divine_super_strength", "Divine super strength potion", Effect.DIVINE_SUPER_STRENGTH, 0xF0F0F0, 800),
            new ModPotion("divine_super_defence", "Divine super defence potion", Effect.DIVINE_SUPER_DEFENCE, 0xE0C050, 800),
            new ModPotion("divine_ranging", "Divine ranging potion", Effect.DIVINE_RANGING, 0x50B0E0, 900),
            new ModPotion("divine_magic", "Divine magic potion", Effect.DIVINE_MAGIC, 0xA06040, 900),
            new ModPotion("divine_super_combat", "Divine super combat potion", Effect.DIVINE_SUPER_COMBAT, 0x3AAA4A, 1500),
            new ModPotion("divine_bastion", "Divine bastion potion", Effect.DIVINE_BASTION, 0x8A4A3A, 1200),
            new ModPotion("divine_battlemage", "Divine battlemage potion", Effect.DIVINE_BATTLEMAGE, 0x4A4A8A, 1200),
            new ModPotion("super_antifire", "Super antifire potion", Effect.SUPER_ANTIFIRE, 0xC060E0, 1200),
            new ModPotion("extended_super_antifire", "Extended super antifire", Effect.EXTENDED_SUPER_ANTIFIRE, 0xE080F0, 1500),
            new ModPotion("weapon_poison", "Weapon poison", Effect.WEAPON_POISON, 0x3A5A2A, 150),
            new ModPotion("weapon_poison_plus", "Weapon poison(+)", Effect.WEAPON_POISON_PLUS, 0x2A4A1A, 300),
            new ModPotion("weapon_poison_plus_plus", "Weapon poison(++)", Effect.WEAPON_POISON_PLUS_PLUS, 0x1A3A0A, 500),
            new ModPotion("guthix_rest", "Guthix rest", Effect.GUTHIX_REST, 0xA0C080, 100),
            new ModPotion("compost_potion", "Compost potion", Effect.COMPOST, 0x8A6A3A, 150));

    public static ModPotion potion(String id) {
        for (ModPotion p : POTIONS) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public void items(ItemSink s) {
        tool(s, "pestle_and_mortar", "Pestle and mortar", "pestle", 0x9A9A9A, 4);
        String[][] secondaries = {
                {"mort_myre_fungus", "Mort myre fungus", "8A7A6A", "15"},
                {"amylase_crystal", "Amylase crystal", "E0E0F0", "10"},
                {"goat_horn", "Desert goat horn", "D0C0A0", "10"},
                {"goat_horn_dust", "Goat horn dust", "E0D8C0", "15"},
                {"toads_legs", "Toad's legs", "6A9A3A", "10"},
                {"kebbit_teeth_dust", "Kebbit teeth dust", "E8E0D0", "20"},
                {"yew_roots", "Yew roots", "6A4A2A", "50"},
                {"magic_roots", "Magic roots", "3A8AA8", "200"},
                {"oak_roots", "Oak roots", "8A6A3A", "5"},
                {"willow_roots", "Willow roots", "6E7A3A", "10"},
                {"maple_roots", "Maple roots", "A0502A", "20"},
                {"coconut_milk", "Coconut milk", "F0F0E8", "20"},
                {"blue_dragon_scale", "Blue dragon scale", "3A5AB0", "50"},
                {"dragon_scale_dust", "Dragon scale dust", "5A7AC0", "60"},
                {"lava_scale_shard", "Lava scale shard", "D0602A", "50"},
                {"crushed_superior_dragon_bones", "Crushed superior dragon bones", "C8C0A0", "1000"},
                {"crystal_dust", "Crystal dust", "C0E0F0", "100"},
                {"cactus_spine", "Cactus spine", "8AA040", "70"},
                {"nightshade", "Cave nightshade", "4A2A6A", "60"},
                {"poison_ivy_berries", "Poison ivy berries", "3A6A2A", "40"},
                {"jangerberries", "Jangerberries", "8AA030", "15"},
                {"volcanic_ash", "Volcanic ash", "5A5A5A", "30"},
                {"vial_of_blood", "Vial of blood", "8A1010", "150"}};
        for (String[] sec : secondaries) {
            simple(s, sec[0], sec[1], "secondary", Integer.parseInt(sec[2], 16), Integer.parseInt(sec[3]));
        }
        // Unfinished bases that aren't "herb + vial of water"
        simple(s, "antidote_plus_unf", "Antidote+ (unf)", "potion_unf", 0xA0A050, 40);
        simple(s, "antidote_plus_plus_unf", "Antidote++ (unf)", "potion_unf", 0x7A8A30, 60);
        simple(s, "weapon_poison_plus_unf", "Weapon poison(+) (unf)", "potion_unf", 0x2A4A1A, 40);
        simple(s, "weapon_poison_plus_plus_unf", "Weapon poison(++) (unf)", "potion_unf", 0x1A3A0A, 60);
        simple(s, "cadantine_blood_potion_unf", "Cadantine blood potion (unf)", "potion_unf", 0x6A1A2A, 80);
        simple(s, "guthix_rest_unf", "Herbal tea mix", "cup", 0xA0C080, 10);
        for (ModPotion p : POTIONS) {
            potion(s, p.id(), p.name(), p.color(), p.value());
        }
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        MenuType H = MenuType.HERBLORE;
        // Grinding (pestle and mortar)
        grind(out, "goat_horn", "goat_horn_dust");
        grind(out, "kebbit_teeth", "kebbit_teeth_dust");
        grind(out, "blue_dragon_scale", "dragon_scale_dust");
        grind(out, "lava_scale", "lava_scale_shard");
        grind(out, "superior_dragon_bones", "crushed_superior_dragon_bones");
        grind(out, "bird_nest", "crushed_nest");
        out.accept(plain("trade_amylase", H, "10 amylase crystals (1 mark of grace)").in("mark_of_grace", 1)
                .out("amylase_crystal", 10).ticks(0).build());
        out.accept(r("make_coconut_milk", H, "Coconut milk", Skill.HERBLORE, 1, 0).in("coconut", 1).in("vial", 1)
                .out("coconut_milk", 1).out("coconut_shell", 1).tool("hammer").ticks(1).build());
        // Unfinished bases
        out.accept(r("unf_antidote_plus", H, "Antidote+ (unf)", Skill.HERBLORE, 68, 0).in("toadflax", 1).in("coconut_milk", 1)
                .out("antidote_plus_unf", 1).ticks(1).build());
        out.accept(r("unf_antidote_plus_plus", H, "Antidote++ (unf)", Skill.HERBLORE, 79, 0).in("irit_leaf", 1).in("coconut_milk", 1)
                .out("antidote_plus_plus_unf", 1).ticks(1).build());
        out.accept(r("unf_weapon_poison_plus", H, "Weapon poison(+) (unf)", Skill.HERBLORE, 73, 0).in("coconut_milk", 1)
                .in("cactus_spine", 1).out("weapon_poison_plus_unf", 1).ticks(1).build());
        out.accept(r("unf_weapon_poison_plus_plus", H, "Weapon poison(++) (unf)", Skill.HERBLORE, 82, 0).in("coconut_milk", 1)
                .in("nightshade", 1).out("weapon_poison_plus_plus_unf", 1).ticks(1).build());
        out.accept(r("unf_cadantine_blood", H, "Cadantine blood potion (unf)", Skill.HERBLORE, 80, 0).in("cadantine", 1)
                .in("vial_of_blood", 1).out("cadantine_blood_potion_unf", 1).ticks(1).build());
        out.accept(r("mix_guthix_rest_tea", H, "Herbal tea mix", Skill.HERBLORE, 18, 0).in("cup_of_hot_water", 1).in("guam_leaf", 2)
                .in("marrentill", 1).out("guthix_rest_unf", 1).ticks(2).build());
        // Finished potions
        mix(out, "guthix_rest", 18, 59.0, "guthix_rest_unf", "harralander");
        mix(out, "compost_potion", 22, 60.0, "harralander_potion_unf", "volcanic_ash");
        mix(out, "agility_potion", 34, 80.0, "toadflax_potion_unf", "toads_legs");
        mix(out, "combat_potion", 36, 84.0, "harralander_potion_unf", "goat_horn_dust");
        mix(out, "superantipoison", 48, 106.3, "irit_potion_unf", "unicorn_horn_dust");
        mix(out, "fishing_potion", 50, 112.5, "avantoe_potion_unf", "snape_grass");
        mix(out, "super_energy", 52, 117.5, "avantoe_potion_unf", "mort_myre_fungus");
        mix(out, "hunter_potion", 53, 120.0, "avantoe_potion_unf", "kebbit_teeth_dust");
        mix(out, "weapon_poison", 60, 137.5, "kwuarm_potion_unf", "dragon_scale_dust");
        mix(out, "antidote_plus", 68, 155.0, "antidote_plus_unf", "yew_roots");
        out.accept(r("mix_antifire_potion", H, "Antifire potion", Skill.HERBLORE, 69, 157.5).in("lantadyme_potion_unf", 1)
                .in("dragon_scale_dust", 1).out("antifire_potion", 1).ticks(2).build());
        mix(out, "weapon_poison_plus", 73, 165.0, "weapon_poison_plus_unf", "red_spiders_eggs");
        out.accept(r("mix_stamina_potion", H, "Stamina potion", Skill.HERBLORE, 77, 102.0).in("super_energy", 1)
                .in("amylase_crystal", 4).out("stamina_potion", 1).ticks(2).build());
        mix(out, "zamorak_brew", 78, 175.0, "torstol_potion_unf", "jangerberries");
        mix(out, "antidote_plus_plus", 79, 177.5, "antidote_plus_plus_unf", "magic_roots");
        mix(out, "bastion_potion", 80, 155.0, "cadantine_blood_potion_unf", "wine_of_zamorak");
        mix(out, "battlemage_potion", 80, 155.0, "cadantine_blood_potion_unf", "potato_cactus");
        mix(out, "weapon_poison_plus_plus", 82, 190.0, "weapon_poison_plus_plus_unf", "poison_ivy_berries");
        out.accept(r("mix_extended_antifire", H, "Extended antifire", Skill.HERBLORE, 84, 110.0).in("antifire_potion", 1)
                .in("lava_scale_shard", 4).out("extended_antifire", 1).ticks(2).build());
        out.accept(r("mix_anti_venom", H, "Anti-venom", Skill.HERBLORE, 87, 120.0).in("antidote_plus_plus", 1)
                .in("zulrahs_scales", 20).out("anti_venom", 1).ticks(2).build());
        out.accept(r("mix_super_combat_potion", H, "Super combat potion", Skill.HERBLORE, 90, 150.0).in("torstol", 1)
                .in("super_attack", 1).in("super_strength", 1).in("super_defence", 1).out("super_combat_potion", 1)
                .out("vial", 2).ticks(2).build());
        out.accept(r("mix_super_antifire", H, "Super antifire potion", Skill.HERBLORE, 92, 130.0).in("antifire_potion", 1)
                .in("crushed_superior_dragon_bones", 1).out("super_antifire", 1).ticks(2).build());
        out.accept(r("mix_anti_venom_plus", H, "Anti-venom+", Skill.HERBLORE, 94, 125.0).in("anti_venom", 1).in("torstol", 1)
                .out("anti_venom_plus", 1).ticks(2).build());
        out.accept(r("mix_extended_super_antifire", H, "Extended super antifire", Skill.HERBLORE, 98, 160.0).in("super_antifire", 1)
                .in("lava_scale_shard", 4).out("extended_super_antifire", 1).ticks(2).build());
        // Divine potions: base potion + 2 crystal dust (1.5 XP per dose in-game, 4 doses).
        divine(out, "divine_super_attack", 70, "super_attack");
        divine(out, "divine_super_strength", 75, "super_strength");
        divine(out, "divine_super_defence", 76, "super_defence");
        divine(out, "divine_ranging", 74, "ranging_potion");
        divine(out, "divine_magic", 78, "magic_potion");
        divine(out, "divine_bastion", 86, "bastion_potion");
        divine(out, "divine_battlemage", 86, "battlemage_potion");
        divine(out, "divine_super_combat", 97, "super_combat_potion");
    }

    private static void grind(Consumer<Recipe> out, String from, String to) {
        out.accept(r("grind_" + from, MenuType.HERBLORE, title(to), Skill.HERBLORE, 1, 0).in(from, 1).out(to, 1)
                .tool("pestle_and_mortar").ticks(1).build());
    }

    private static void mix(Consumer<Recipe> out, String id, int level, double xp, String base, String secondary) {
        ModPotion p = potion(id);
        out.accept(r("mix_" + id, MenuType.HERBLORE, p.name(), Skill.HERBLORE, level, xp).in(base, 1).in(secondary, 1)
                .out(id, 1).ticks(2).build());
    }

    private static void divine(Consumer<Recipe> out, String id, int level, String base) {
        ModPotion p = potion(id);
        out.accept(r("mix_" + id, MenuType.HERBLORE, p.name(), Skill.HERBLORE, level, 6.0).in(base, 1).in("crystal_dust", 2)
                .out(id, 1).ticks(2).build());
    }
}
