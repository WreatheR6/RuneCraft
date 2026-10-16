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
 * and compost potion, Zogre Flesh Eaters' relicym's balm and sanfew serum, serum 207, the huasca prayer
 * regeneration potion and the barbarian mixes ({@link #MIXES}), with their secondaries. Levels/XP are OSRS
 * values (XP for 4-dose recipes). Effects are applied by SkillingHooks through {@link Effect}.
 */
public final class HerbloreData extends SkillingPart {

    /** A potion added by this module and how a dose works. */
    public enum Effect {
        SUPER_COMBAT, STAMINA, SUPER_ENERGY, SUPER_ANTIPOISON, ANTIDOTE_PLUS, ANTIDOTE_PLUS_PLUS, ANTI_VENOM, ANTI_VENOM_PLUS,
        COMBAT, AGILITY, FISHING, HUNTER, ZAMORAK_BREW, BASTION, BATTLEMAGE, DIVINE_SUPER_ATTACK, DIVINE_SUPER_STRENGTH,
        DIVINE_SUPER_DEFENCE, DIVINE_RANGING, DIVINE_MAGIC, DIVINE_SUPER_COMBAT, DIVINE_BASTION, DIVINE_BATTLEMAGE,
        SUPER_ANTIFIRE, EXTENDED_SUPER_ANTIFIRE, WEAPON_POISON, WEAPON_POISON_PLUS, WEAPON_POISON_PLUS_PLUS, GUTHIX_REST,
        COMPOST,
        /** Super restore that also cures poison (90 s immunity) and disease. */
        SANFEW,
        /** Cures disease (RuneCraft has no disease: clears weakness, hunger and nausea instead). */
        RELICYMS_BALM,
        /** Shades of Mort'ton cure for the Afflicted; not drinkable. */
        SERUM_207,
        /** One Prayer point every 12 ticks for 8 minutes (66 points per dose). */
        PRAYER_REGENERATION,
        /** Goading potion: nearby monsters turn aggressive for 6 minutes (checked every 6 ticks). */
        GOADING,
        /** Magic essence: +3 Magic. */
        MAGIC_ESSENCE,
        /**
         * Ancient brew: Magic +2 + 5%, Prayer 2 + 10% of base restored, Attack, Strength and Defence drained by
         * 2 + 10% of their current level. Approx: the restore stops at the base Prayer level (OSRS allows 5% over).
         */
        ANCIENT_BREW,
        /** Extended anti-venom+ (round 28, appended): anti-venom+ with araxyte venom sacs (Toxins.EXTENDED_ANTI_VENOM_PLUS). */
        EXTENDED_ANTI_VENOM_PLUS;

        /** Goading potion: duration per dose and how often nearby monsters are goaded, in game ticks (OSRS Wiki). */
        public static final int GOADING_DURATION = 600;
        public static final int GOADING_INTERVAL = 6;

        /** Prayer regeneration: game ticks per Prayer point and duration per dose (OSRS Wiki). */
        public static final int REGEN_INTERVAL = 12;
        public static final int REGEN_DURATION = 800;

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
                case SANFEW -> {
                    // Same restore as a super restore: 8 + 25% of every stat except Hitpoints and Prayer.
                    for (Skill sk : Skill.values()) {
                        if (sk != Skill.HITPOINTS && sk != Skill.PRAYER) {
                            s.restore(sk, 8, 0.25);
                        }
                    }
                    // Prayer: 4 + 30% of the Prayer level (OSRS Wiki "Holy wrench" restore table).
                    return 4 + s.level(Skill.PRAYER) * 30 / 100;
                }
                case MAGIC_ESSENCE -> s.applyBoost(Skill.MAGIC, 3, 0.0);
                case ANCIENT_BREW -> {
                    s.applyBoost(Skill.MAGIC, 2, 0.05);
                    for (Skill sk : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE}) {
                        int drain = 2 + (int) Math.floor(s.currentLevel(sk) * 0.10);
                        s.setBoost(sk, s.boost(sk) - drain);
                    }
                    return 2 + s.level(Skill.PRAYER) / 10;
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
            new ModPotion("extended_anti_venom_plus", "Extended anti-venom+", Effect.EXTENDED_ANTI_VENOM_PLUS, 0x3A6A2A, 1900), // value approx
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
            new ModPotion("compost_potion", "Compost potion", Effect.COMPOST, 0x8A6A3A, 150),
            new ModPotion("sanfew_serum", "Sanfew serum", Effect.SANFEW, 0xD0607A, 300),
            new ModPotion("relicyms_balm", "Relicym's balm", Effect.RELICYMS_BALM, 0xC0A0D0, 225),
            new ModPotion("serum_207", "Serum 207", Effect.SERUM_207, 0x6A8AC0, 14),
            new ModPotion("prayer_regeneration_potion", "Prayer regeneration potion", Effect.PRAYER_REGENERATION, 0x40C0B0, 300),
            new ModPotion("goading_potion", "Goading potion", Effect.GOADING, 0xA0B040, 300),
            new ModPotion("magic_essence", "Magic essence", Effect.MAGIC_ESSENCE, 0xE0D060, 280),
            new ModPotion("ancient_brew", "Ancient brew", Effect.ANCIENT_BREW, 0x7A3A9A, 200));

    /** The miniquest whose Herblore part (Otto Godblessed) teaches barbarian mixes. */
    public static final String BARBARIAN_TRAINING = "barbarian_training";

    /** True for the recipes that make a barbarian mix (they need Barbarian Training). */
    public static boolean isBarbarianMix(String recipeId) {
        for (Mix m : MIXES) {
            if (recipeId.equals("mix_" + m.id()) || recipeId.equals("mix_" + m.id() + "_caviar")) {
                return true;
            }
        }
        return false;
    }

    /**
     * A barbarian mix (Barbarian Training's Herblore part): a two-dose potion with roe or caviar added. Each of
     * its two doses has the base potion's effect and also heals {@code heal} Hitpoints. Roe works up to the
     * restore mix; caviar works for every mix.
     */
    public record Mix(String id, String name, String base, int level, double xp, boolean roe, int heal) {
    }

    /** OSRS Wiki levels and XP. Heals: 3 for roe mixes and the energy/combat mixes, 6 for the other caviar ones. */
    public static final List<Mix> MIXES = List.of(
            new Mix("attack_mix", "Attack mix", "attack_potion", 4, 8.0, true, 3),
            new Mix("antipoison_mix", "Antipoison mix", "antipoison", 6, 12.0, true, 3),
            new Mix("relicyms_mix", "Relicym's mix", "relicyms_balm", 9, 14.0, true, 3),
            new Mix("strength_mix", "Strength mix", "strength_potion", 14, 17.0, true, 3),
            new Mix("restore_mix", "Restore mix", "restore_potion", 24, 21.0, true, 3),
            new Mix("energy_mix", "Energy mix", "energy_potion", 29, 23.0, false, 3),
            new Mix("defence_mix", "Defence mix", "defence_potion", 33, 25.0, false, 6),
            new Mix("agility_mix", "Agility mix", "agility_potion", 37, 27.0, false, 6),
            new Mix("combat_mix", "Combat mix", "combat_potion", 40, 28.0, false, 3),
            new Mix("prayer_mix", "Prayer mix", "prayer_potion", 42, 29.0, false, 6),
            new Mix("superattack_mix", "Superattack mix", "super_attack", 47, 33.0, false, 6),
            new Mix("anti_poison_supermix", "Anti-poison supermix", "superantipoison", 51, 35.0, false, 6),
            new Mix("fishing_mix", "Fishing mix", "fishing_potion", 53, 38.0, false, 6),
            new Mix("super_energy_mix", "Super energy mix", "super_energy", 56, 39.0, false, 6),
            new Mix("hunting_mix", "Hunting mix", "hunter_potion", 58, 40.0, false, 6),
            new Mix("super_str_mix", "Super str. mix", "super_strength", 59, 42.0, false, 6),
            new Mix("magic_essence_mix", "Magic essence mix", "magic_essence", 61, 43.0, false, 6),
            new Mix("super_restore_mix", "Super restore mix", "super_restore", 67, 48.0, false, 6),
            new Mix("super_def_mix", "Super def. mix", "super_defence", 71, 50.0, false, 6),
            new Mix("antidote_plus_mix", "Antidote+ mix", "antidote_plus", 74, 52.0, false, 6),
            new Mix("antifire_mix", "Antifire mix", "antifire_potion", 75, 53.0, false, 6),
            new Mix("ranging_mix", "Ranging mix", "ranging_potion", 80, 54.0, false, 6),
            new Mix("magic_mix", "Magic mix", "magic_potion", 83, 57.0, false, 6),
            new Mix("zamorak_mix", "Zamorak mix", "zamorak_brew", 85, 58.0, false, 6),
            new Mix("stamina_mix", "Stamina mix", "stamina_potion", 86, 60.0, false, 6),
            new Mix("extended_antifire_mix", "Extended antifire mix", "extended_antifire", 91, 61.0, false, 6),
            new Mix("ancient_mix", "Ancient mix", "ancient_brew", 92, 63.0, false, 6),
            new Mix("super_antifire_mix", "Super antifire mix", "super_antifire", 98, 70.0, false, 6),
            new Mix("extended_super_antifire_mix", "Extended super antifire mix", "extended_super_antifire", 99, 78.0, false, 6));

    /** Doses in a barbarian mix. */
    public static final int MIX_DOSES = 2;

    public static Mix mix(String id) {
        for (Mix m : MIXES) {
            if (m.id().equals(id)) {
                return m;
            }
        }
        return null;
    }

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
                {"vial_of_blood", "Vial of blood", "8A1010", "150"},
                // Sanfew serum: dropped by nail beasts (Temple Trekking); bought on the Grand Exchange here.
                {"nail_beast_nails", "Nail beast nails", "C8B8A0", "300"},
                // Huasca potions: a Mastering Mixology reward; bought on the Grand Exchange here.
                {"aldarium", "Aldarium", "E0B040", "100"},
                // Ancient brew: a nihil shard (Nex) crushed with a pestle and mortar.
                {"nihil_dust", "Nihil dust", "8A4AAA", "500"},
                // Magic essence: gorak claws crushed with a pestle and mortar.
                {"gorak_claw_powder", "Gorak claw powder", "8A5A8A", "1"}}; // approx value
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
        // Guthix balance (In Aid of the Myreque): a restore potion with garlic, finished with silver dust. It is not drinkable
        // in OSRS (Empty/Drop only): it is used on frozen vampyre juveniles, which isn't modelled.
        simple(s, "guthix_balance_unf", "Guthix balance (unf)", "potion_unf", 0xD08090, 100)
                .examine("I need another ingredient to finish this Guthix balance potion.");
        potion(s, "guthix_balance", "Guthix balance", 0xE0A0B0, 200).notDrinkable()
                .examine("A potion of harralander, red spiders eggs, garlic and silver dust.");
        simple(s, "silver_dust", "Silver dust", "secondary", 0xD0D0D8, 1).examine("Finely ground silver.");
        simple(s, "magic_essence_unf", "Magic essence (unf)", "potion_unf", 0xE0D060, 75)
                .examine("I need another ingredient to finish this magic essence potion.");
        simple(s, "rogues_purse_potion_unf", "Rogue's purse potion (unf)", "potion_unf", 0xC0A0D0, 5);
        simple(s, "mixture_step_1", "Mixture - step 1", "potion", 0xC8607A, 200).stack(1);
        simple(s, "mixture_step_2", "Mixture - step 2", "potion", 0xCC607A, 250).stack(1);
        // Ashes left by fires (and dropped by imps); Serum 207's secondary.
        simple(s, "ashes", "Ashes", "ashes", 0x8A8A8A, 2).examine("A heap of ashes.");
        for (ModPotion p : POTIONS) {
            potion(s, p.id(), p.name(), p.color(), p.value());
        }
        for (Mix m : MIXES) {
            potion(s, m.id(), m.name(), mixColor(m), 50 + m.level() * 4).examine("Two doses of " + lower(m.name()) + ".");
        }
    }

    /** A mix looks like its base potion, a shade darker. */
    private static int mixColor(Mix m) {
        int c = 0x8A8A8A;
        ModPotion mp = potion(m.base());
        if (mp != null) {
            c = mp.color();
        } else {
            for (com.runecraft.osrs.data.Potion p : com.runecraft.osrs.data.Potion.values()) {
                if (p.itemId().equals(m.base())) {
                    c = p.color();
                }
            }
        }
        return ((c >> 16 & 0xFF) * 7 / 8) << 16 | ((c >> 8 & 0xFF) * 7 / 8) << 8 | (c & 0xFF) * 7 / 8;
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
        grind(out, "unicorn_horn", "unicorn_horn_dust"); // unicorn_horn is a CommonItems drop
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
        // Guthix balance: garlic into a restore potion, then silver dust (22 Herblore, 25 XP).
        out.accept(r("unf_guthix_balance", H, "Guthix balance (unf)", Skill.HERBLORE, 22, 0).in("restore_potion", 1).in("garlic", 1)
                .out("guthix_balance_unf", 1).ticks(2).build());
        out.accept(r("mix_guthix_balance", H, "Guthix balance", Skill.HERBLORE, 22, 25.0).in("guthix_balance_unf", 1)
                .in("silver_dust", 1).out("guthix_balance", 1).ticks(2).build());
        // approx: OSRS grinds silver bars in the Ectofuntus bone grinder.
        grind(out, "silver_bar", "silver_dust");
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
        mix(out, "weapon_poison_plus", 73, 190.0, "weapon_poison_plus_unf", "red_spiders_eggs");
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
        // Extended anti-venom+ (OSRS Wiki): one araxyte venom sac per dose of anti-venom+, 94 Herblore, 20 XP a dose, 3 ticks.
        out.accept(r("mix_extended_anti_venom_plus", H, "Extended anti-venom+", Skill.HERBLORE, 94, 80.0).in("anti_venom_plus", 1)
                .in("araxyte_venom_sac", 4).out("extended_anti_venom_plus", 1).ticks(3).build());
        out.accept(r("mix_extended_super_antifire", H, "Extended super antifire", Skill.HERBLORE, 98, 160.0).in("super_antifire", 1)
                .in("lava_scale_shard", 4).out("extended_super_antifire", 1).ticks(2).build());
        // Zogre Flesh Eaters: relicym's balm (rogue's purse + snake weed) and the three-step sanfew serum
        // (OSRS Wiki XP for 4 doses: 57 + 63 + 72).
        out.accept(r("unf_rogues_purse", H, "Rogue's purse potion (unf)", Skill.HERBLORE, 8, 0).in("rogues_purse", 1)
                .in("vial_of_water", 1).out("rogues_purse_potion_unf", 1).ticks(1).build());
        mix(out, "relicyms_balm", 8, 40.0, "rogues_purse_potion_unf", "snake_weed");
        mix(out, "serum_207", 15, 50.0, "tarromin_potion_unf", "ashes");
        out.accept(r("mix_sanfew_step_1", H, "Mixture - step 1", Skill.HERBLORE, 65, 57.0).in("super_restore", 1)
                .in("unicorn_horn_dust", 1).out("mixture_step_1", 1).ticks(2).build());
        out.accept(r("mix_sanfew_step_2", H, "Mixture - step 2", Skill.HERBLORE, 65, 63.0).in("mixture_step_1", 1)
                .in("snake_weed", 1).out("mixture_step_2", 1).ticks(2).build());
        mix(out, "sanfew_serum", 65, 72.0, "mixture_step_2", "nail_beast_nails");
        // Varlamore: huasca potion (unf) + aldarium.
        mix(out, "prayer_regeneration_potion", 58, 132.0, "huasca_potion_unf", "aldarium");
        // Goading potion: harralander potion (unf) + aldarium, 54 Herblore, 132 XP (OSRS: makes a 3-dose potion; 4 here).
        mix(out, "goading_potion", 54, 132.0, "harralander_potion_unf", "aldarium");
        // Ancient brew: nihil dust (crushed nihil shard) in a dwarf weed potion (unf), 85 Herblore, 190 XP. Magic
        // essence (Fairytale II): star flower in a vial of water (57, no XP) then gorak claw powder, 130 XP.
        // Approx: OSRS makes 3-dose potions; 4 here.
        grind(out, "nihil_shard", "nihil_dust");
        grind(out, "gorak_claws", "gorak_claw_powder");
        mix(out, "ancient_brew", 85, 190.0, "dwarf_weed_potion_unf", "nihil_dust");
        out.accept(r("mix_magic_essence_unf", H, "Magic essence (unf)", Skill.HERBLORE, 57, 0).in("vial_of_water", 1)
                .in("star_flower", 1).out("magic_essence_unf", 1).ticks(1).build());
        mix(out, "magic_essence", 57, 130.0, "magic_essence_unf", "gorak_claw_powder");
        // Barbarian mixes (roe mixes also take caviar). Approx: the base potion is used whole rather than as a (2).
        for (Mix m : MIXES) {
            out.accept(r("mix_" + m.id(), H, m.name(), Skill.HERBLORE, m.level(), m.xp()).in(m.base(), 1)
                    .in(m.roe() ? "roe" : "caviar", 1).out(m.id(), 1).ticks(1).build());
            if (m.roe()) {
                out.accept(r("mix_" + m.id() + "_caviar", H, m.name() + " (caviar)", Skill.HERBLORE, m.level(), m.xp())
                        .in(m.base(), 1).in("caviar", 1).out(m.id(), 1).ticks(1).build());
            }
        }
        // Divine potions: 4-dose base potion + 4 crystal dust (1 per dose); 0.5 XP per dose, 2 XP per potion.
        divine(out, "divine_super_attack", 70, "super_attack");
        divine(out, "divine_super_strength", 70, "super_strength");
        divine(out, "divine_super_defence", 70, "super_defence");
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
        out.accept(r("mix_" + id, MenuType.HERBLORE, p.name(), Skill.HERBLORE, level, 2.0).in(base, 1).in("crystal_dust", 4)
                .out(id, 1).ticks(2).build());
    }
}
