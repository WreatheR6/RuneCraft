package com.runecraft.osrs.content.godwars;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentDef.WeaponClass;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.godwars.GodWarsFactions.God;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Items of the God Wars Dungeon, Nex and the Wilderness bosses with their OSRS equipment bonuses.
 *
 * <p>The equipment system has no ring, hands or neck slots yet, so rings (tyrannical, treasonous,
 * ring of the gods) and the zaryte vambraces are plain items for now (bonuses in the examine text).
 */
public final class GwdItems {
    private GwdItems() {
    }

    private static EquipmentStats stats(int stabA, int slashA, int crushA, int magicA, int rangedA, int stabD, int slashD,
                                        int crushD, int magicD, int rangedD, int str, int rangedStr, int magicDmg, int prayer) {
        return new EquipmentStats(stabA, slashA, crushA, magicA, rangedA, stabD, slashD, crushD, magicD, rangedD, str, rangedStr,
                magicDmg, prayer);
    }

    private static EquipmentDef armour(Slot slot, EquipmentStats stats, Map<Skill, Integer> reqs) {
        return new EquipmentDef(slot, stats, reqs, 0, AttackType.CRUSH, WeaponClass.NONE);
    }

    private static EquipmentDef weapon(EquipmentStats stats, Map<Skill, Integer> reqs, int speed, AttackType type, WeaponClass cls) {
        return new EquipmentDef(Slot.WEAPON, stats, reqs, speed, type, cls);
    }

    public static void add(ItemSink items) {
        godWars(items);
        nex(items);
        wilderness(items);
        revenants(items);
        pets(items);
    }

    private static void godWars(ItemSink items) {
        // --- Armadyl (Kree'arra) ---
        Map<Skill, Integer> arma = Map.of(Skill.RANGED, 70, Skill.DEFENCE, 70);
        items.add("armadyl_helmet", "Armadyl helmet", Kind.ARMOUR, "full_helm", 0xD8D0B0).value(150000).stack(1).tier("armadyl")
                .equipment(armour(Slot.HEAD, stats(-5, -5, -5, -5, 10, 6, 9, 12, 10, 8, 0, 0, 0, 1), arma))
                .examine("A helmet of great craftsmanship.");
        items.add("armadyl_chestplate", "Armadyl chestplate", Kind.ARMOUR, "platebody", 0xD8D0B0).value(1000000).stack(1).tier("armadyl")
                .equipment(armour(Slot.CHEST, stats(-7, -7, -7, -15, 33, 56, 48, 61, 70, 57, 0, 0, 0, 1), arma))
                .examine("A chestplate of great craftsmanship.");
        items.add("armadyl_chainskirt", "Armadyl chainskirt", Kind.ARMOUR, "platelegs", 0xD8D0B0).value(1000000).stack(1).tier("armadyl")
                .equipment(armour(Slot.LEGS, stats(-6, -6, -6, -10, 20, 32, 26, 34, 40, 33, 0, 0, 0, 1), arma))
                .examine("A chainskirt of great craftsmanship.");
        items.add("armadyl_crossbow", "Armadyl crossbow", Kind.BOW, "crossbow", 0xB0B8C8).value(500000).stack(1).tier("armadyl")
                .equipment(weapon(stats(0, 0, 0, 0, 100, 0, 0, 0, 0, 0, 0, 0, 0, 1), Map.of(Skill.RANGED, 70), 6,
                        AttackType.RANGED, WeaponClass.BOW))
                .examine("A crossbow crafted by Armadyl's followers.");

        // --- Saradomin (Commander Zilyana) ---
        items.add("saradomin_sword", "Saradomin sword", Kind.MELEE_WEAPON, "2h_sword", 0xE8E8F0).value(150000).stack(1).tier("saradomin")
                .equipment(weapon(stats(0, 82, 60, 0, 0, 0, 0, 0, 0, 0, 82, 0, 0, 0), Map.of(Skill.ATTACK, 70), 4,
                        AttackType.SLASH, WeaponClass.MELEE))
                .examine("The incredible blade of an Icyene.");
        items.add("saradomins_light", "Saradomin's light", Kind.SIMPLE, "orb", 0xF8F0B0).value(150000).stack(1)
                .examine("A small gift from Saradomin; it could permanently protect you from the darkness.");

        // --- Hilts, shards, blade and godswords ---
        int[] hiltColours = {0xC0C8D8, 0x6A7A3A, 0x3A5AD0, 0xB02020};
        for (God god : God.values()) {
            items.add(god.hilt(), god.displayName() + " hilt", Kind.SIMPLE, "hilt", hiltColours[god.ordinal()]).value(100000).stack(1)
                    .examine("Brimming with potential.");
        }
        items.add("ancient_hilt", "Ancient hilt", Kind.SIMPLE, "hilt", 0x6A3A8A).value(100000).stack(1)
                .examine("Brimming with ancient Zarosian power.");
        for (int i = 1; i <= 3; i++) {
            items.add("godsword_shard_" + i, "Godsword shard " + i, Kind.SIMPLE, "shard", 0xA8B0B8).value(150000).stack(1)
                    .examine("A piece of a huge blade.");
        }
        items.add("godsword_shards_1_2", "Godsword shards 1 & 2", Kind.SIMPLE, "shard", 0xA8B0B8).value(300000).stack(1)
                .examine("Two pieces of a huge blade, joined together.");
        items.add("godsword_shards_1_3", "Godsword shards 1 & 3", Kind.SIMPLE, "shard", 0xA8B0B8).value(300000).stack(1)
                .examine("Two pieces of a huge blade, joined together.");
        items.add("godsword_shards_2_3", "Godsword shards 2 & 3", Kind.SIMPLE, "shard", 0xA8B0B8).value(300000).stack(1)
                .examine("Two pieces of a huge blade, joined together.");
        items.add("godsword_blade", "Godsword blade", Kind.SIMPLE, "godsword_blade", 0xB8C0C8).value(450000).stack(1)
                .examine("A huge blade, missing its hilt.");
        EquipmentStats gs = stats(0, 132, 80, 0, 0, 0, 0, 0, 0, 0, 132, 0, 0, 8);
        String[] gsExamine = {
                "A beautiful, heavy sword (special: The Judgement, +37.5% damage).",
                "A brutally heavy sword (special: Warstrike, drains stats).",
                "A gracious, heavy sword (special: Healing Blade, heals and restores prayer).",
                "A terrifying, heavy sword (special: Ice Cleave, freezes the target)."};
        for (God god : God.values()) {
            items.add(god.godsword(), god.displayName() + " godsword", Kind.MELEE_WEAPON, "godsword", hiltColours[god.ordinal()])
                    .value(1250000).stack(1).tier("godsword")
                    .equipment(weapon(gs, Map.of(Skill.ATTACK, 75), 6, AttackType.SLASH, WeaponClass.MELEE))
                    .examine(gsExamine[god.ordinal()]);
        }
        items.add("ancient_godsword", "Ancient godsword", Kind.MELEE_WEAPON, "godsword", 0x6A3A8A).value(1250000).stack(1)
                .tier("godsword")
                .equipment(weapon(gs, Map.of(Skill.ATTACK, 75), 6, AttackType.SLASH, WeaponClass.MELEE))
                .examine("A sinister, heavy sword (special: Blood Sacrifice).");

        // --- Keys ---
        for (God god : God.values()) {
            items.add(god.keyPiece(), "Frozen key piece (" + god.id() + ")", Kind.SIMPLE, "key", hiltColours[god.ordinal()])
                    .value(1).stack(1).examine("A piece of a frozen key. Its icy surface bears the mark of " + god.displayName() + ".");
        }
        items.add("frozen_key", "Frozen key", Kind.SIMPLE, "key", 0xA8D8F0).value(1).stack(1)
                .examine("Opens the frozen door to the Ancient Prison.");
        items.add("ecumenical_key", "Ecumenical key", Kind.SIMPLE, "key", 0xD8C060).value(1).stack(3)
                .examine("Allows entry to a God Wars general's chamber without kill count.");
    }

    private static void nex(ItemSink items) {
        Map<Skill, Integer> torva = Map.of(Skill.DEFENCE, 80);
        items.add("torva_full_helm", "Torva full helm", Kind.ARMOUR, "full_helm", 0x6A2A2A).value(2000000).stack(1).tier("torva")
                .equipment(armour(Slot.HEAD, stats(0, 0, 0, -5, -5, 59, 60, 62, -2, 57, 8, 0, 0, 1), torva))
                .examine("A full helm of ancient Zarosian design.");
        items.add("torva_platebody", "Torva platebody", Kind.ARMOUR, "platebody", 0x6A2A2A).value(4000000).stack(1).tier("torva")
                .equipment(armour(Slot.CHEST, stats(0, 0, 0, -18, -14, 117, 111, 117, -11, 142, 6, 0, 0, 1), torva))
                .examine("A platebody of ancient Zarosian design.");
        items.add("torva_platelegs", "Torva platelegs", Kind.ARMOUR, "platelegs", 0x6A2A2A).value(3000000).stack(1).tier("torva")
                .equipment(armour(Slot.LEGS, stats(0, 0, 0, -24, -11, 87, 78, 79, -4, 102, 4, 0, 0, 1), torva))
                .examine("A pair of platelegs of ancient Zarosian design.");
        items.add("zaryte_vambraces", "Zaryte vambraces", Kind.SIMPLE, "vambraces", 0x8A6AA0).value(3000000).stack(1)
                .examine("Vambraces of Zarosian make. (+18 ranged attack, +2 ranged strength, +1 prayer; needs 80 Ranged and 45 Defence.)");
        items.add("nihil_horn", "Nihil horn", Kind.SIMPLE, "horn", 0x5A2A6A).value(3000000).stack(1)
                .examine("The horn of a nihil. It could be fused with an armadyl crossbow.");
        // nihil_shard lives in CommonItems (ground into nihil dust for ancient brews by the skilling module).
        items.add("zaryte_crossbow", "Zaryte crossbow", Kind.BOW, "crossbow", 0x7A4A9A).value(4000000).stack(1).tier("zaryte")
                .equipment(weapon(stats(0, 0, 0, 0, 110, 14, 14, 14, 15, 16, 0, 0, 0, 1), Map.of(Skill.RANGED, 80), 6,
                        AttackType.RANGED, WeaponClass.BOW))
                .examine("A crossbow imbued with the power of the nihil.");
    }

    private static void wilderness(ItemSink items) {
        items.add("tyrannical_ring", "Tyrannical ring", Kind.SIMPLE, "ring", 0xB04A20).value(200000).stack(1)
                .examine("A ring of brute strength. (+4 crush attack, +4 strength; ring slot not yet supported.)");
        items.add("treasonous_ring", "Treasonous ring", Kind.SIMPLE, "ring", 0x3A8A3A).value(200000).stack(1)
                .examine("A ring of deceit. (+4 stab attack, +4 strength; ring slot not yet supported.)");
        items.add("ring_of_the_gods", "Ring of the gods", Kind.SIMPLE, "ring", 0xE0C040).value(200000).stack(1)
                .examine("A ring blessed by the gods. (+4 prayer; ring slot not yet supported.)");
        for (int i = 1; i <= 3; i++) {
            items.add("odium_shard_" + i, "Odium shard " + i, Kind.SIMPLE, "shard", 0x8A3A2A).value(50000).stack(1)
                    .examine("Part of the odium ward.");
            items.add("malediction_shard_" + i, "Malediction shard " + i, Kind.SIMPLE, "shard", 0x3A2A6A).value(50000).stack(1)
                    .examine("Part of the malediction ward.");
        }
        items.add("odium_ward", "Odium ward", Kind.SHIELD, "ward", 0x8A3A2A).value(300000).stack(1).tier("dragon")
                .equipment(armour(Slot.SHIELD, stats(0, 0, 0, 0, 0, 63, 66, 63, -2, 65, 6, 0, 0, 0), Map.of(Skill.DEFENCE, 60)))
                .examine("A shield infused with the power of the odium.");
        // Approximation: the malediction ward's only offensive bonus is +5% magic damage.
        items.add("malediction_ward", "Malediction ward", Kind.SHIELD, "ward", 0x3A2A6A).value(300000).stack(1).tier("dragon")
                .equipment(armour(Slot.SHIELD, stats(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 5, 0), Map.of(Skill.DEFENCE, 60)))
                .examine("A shield infused with malediction.");

        items.add("voidwaker_hilt", "Voidwaker hilt", Kind.SIMPLE, "hilt", 0x3A3A6A).value(1000000).stack(1)
                .examine("The hilt of the Voidwaker.");
        items.add("voidwaker_blade", "Voidwaker blade", Kind.SIMPLE, "godsword_blade", 0x5A5A8A).value(1000000).stack(1)
                .examine("The blade of the Voidwaker.");
        items.add("voidwaker_gem", "Voidwaker gem", Kind.SIMPLE, "gem", 0x8A3AC0).value(1000000).stack(1)
                .examine("The gem of the Voidwaker.");
        // Approximation of the Voidwaker's bonuses (+70 stab, +80 slash, +80 strength).
        items.add("voidwaker", "Voidwaker", Kind.MELEE_WEAPON, "longsword", 0x6A4AB0).value(3000000).stack(1).tier("voidwaker")
                .equipment(weapon(stats(70, 80, 0, 0, 0, 0, 1, 0, 0, 0, 80, 0, 0, 0), Map.of(Skill.ATTACK, 75), 4,
                        AttackType.SLASH, WeaponClass.MELEE))
                .examine("A sword of the void (special: Disrupt, guaranteed magic hit).");

        items.add("claws_of_callisto", "Claws of callisto", Kind.SIMPLE, "claws", 0x5A3A22).value(800000).stack(1)
                .examine("The claws of the mighty Callisto. They could upgrade Viggora's chainmace.");
        items.add("fangs_of_venenatis", "Fangs of venenatis", Kind.SIMPLE, "fangs", 0x4A2A4A).value(800000).stack(1)
                .examine("The fangs of the fearsome Venenatis. They could upgrade Craw's bow.");
        items.add("skull_of_vetion", "Skull of vet'ion", Kind.SIMPLE, "skull", 0xC8C0A0).value(800000).stack(1)
                .examine("The skull of the fearsome Vet'ion. It could upgrade Thammaron's sceptre.");
        items.add("kbd_heads", "Kbd heads", Kind.SIMPLE, "kbd_heads", 0x2A2A2A).value(60000).stack(1)
                .examine("The heads of the King Black Dragon.");
    }

    private static void revenants(ItemSink items) {
        items.add("revenant_ether", "Revenant ether", Kind.SIMPLE, "essence", 0x40C0A0).value(180).stack(64)
                .examine("Spectral essence used to charge revenant weapons.");
        items.add("ancient_emblem", "Ancient emblem", Kind.SIMPLE, "emblem", 0x8A8A9A).value(500000).stack(1)
                .examine("An ancient artefact. Emir's Arena traders and the Emblem Trader value it.");
        items.add("ancient_totem", "Ancient totem", Kind.SIMPLE, "totem", 0x8A7A5A).value(1000000).stack(1)
                .examine("An ancient artefact.");
        items.add("ancient_statuette", "Ancient statuette", Kind.SIMPLE, "statuette", 0xA89060).value(2000000).stack(1)
                .examine("An ancient artefact.");
        items.add("ancient_medallion", "Ancient medallion", Kind.SIMPLE, "amulet", 0xC0A040).value(4000000).stack(1)
                .examine("An ancient artefact.");
        items.add("ancient_effigy", "Ancient effigy", Kind.SIMPLE, "statuette", 0x9AA0B0).value(8000000).stack(1)
                .examine("An ancient artefact.");
        items.add("ancient_relic", "Ancient relic", Kind.SIMPLE, "statuette", 0xD8C060).value(16000000).stack(1)
                .examine("An ancient artefact.");
        items.add("bracelet_of_ethereum", "Bracelet of ethereum (uncharged)", Kind.SIMPLE, "ring", 0x40C0A0).value(50000).stack(1)
                .examine("Protects against revenants when charged with revenant ether.");
        // Revenant weapons (uncharged bonuses approximated from their charged values).
        items.add("craws_bow", "Craw's bow", Kind.BOW, "longbow", 0x3A6A4A).value(800000).stack(1).tier("revenant")
                .equipment(weapon(stats(0, 0, 0, 0, 60, 0, 0, 0, 0, 0, 0, 0, 0, 0), Map.of(Skill.RANGED, 60), 4,
                        AttackType.RANGED, WeaponClass.BOW))
                .examine("An ancient bow, imbued with the power of the revenants.");
        items.add("viggoras_chainmace", "Viggora's chainmace", Kind.MELEE_WEAPON, "flail", 0x6A4A3A).value(800000).stack(1)
                .tier("revenant")
                .equipment(weapon(stats(0, 0, 70, 0, 0, 0, 0, 0, 0, 0, 66, 0, 0, 0), Map.of(Skill.ATTACK, 60), 4,
                        AttackType.CRUSH, WeaponClass.MELEE))
                .examine("An ancient chainmace, imbued with the power of the revenants.");
        items.add("thammarons_sceptre", "Thammaron's sceptre", Kind.STAFF, "staff_orb", 0x8A3A3A).value(800000).stack(1)
                .tier("revenant")
                .equipment(weapon(stats(0, 0, 0, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), Map.of(Skill.MAGIC, 60), 4,
                        AttackType.MAGIC, WeaponClass.STAFF))
                .examine("An ancient sceptre, imbued with the power of the revenants.");
        items.add("webweaver_bow", "Webweaver bow", Kind.BOW, "longbow", 0x4A2A4A).value(3000000).stack(1).tier("revenant")
                .equipment(weapon(stats(0, 0, 0, 0, 70, 0, 0, 0, 0, 0, 0, 0, 0, 0), Map.of(Skill.RANGED, 70), 3,
                        AttackType.RANGED, WeaponClass.BOW))
                .examine("Craw's bow, upgraded with the fangs of Venenatis.");
        items.add("ursine_chainmace", "Ursine chainmace", Kind.MELEE_WEAPON, "flail", 0x5A3A22).value(3000000).stack(1)
                .tier("revenant")
                .equipment(weapon(stats(0, 0, 80, 0, 0, 0, 0, 0, 0, 0, 74, 0, 0, 0), Map.of(Skill.ATTACK, 70), 4,
                        AttackType.CRUSH, WeaponClass.MELEE))
                .examine("Viggora's chainmace, upgraded with the claws of Callisto.");
        items.add("accursed_sceptre", "Accursed sceptre", Kind.STAFF, "staff_orb", 0xC8C0A0).value(3000000).stack(1)
                .tier("revenant")
                .equipment(weapon(stats(0, 0, 0, 52, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), Map.of(Skill.MAGIC, 70), 4,
                        AttackType.MAGIC, WeaponClass.STAFF))
                .examine("Thammaron's sceptre, upgraded with the skull of Vet'ion.");
    }

    private static void pets(ItemSink items) {
        pet(items, "pet_kreearra", "Pet kree'arra", 0x4A4A5A);
        pet(items, "pet_zilyana", "Pet zilyana", 0xE8E8F0);
        pet(items, "pet_general_graardor", "Pet general graardor", 0x6A7A4A);
        pet(items, "pet_kril_tsutsaroth", "Pet k'ril tsutsaroth", 0x8A1A1A);
        pet(items, "nexling", "Nexling", 0x3A2A4A);
        pet(items, "callisto_cub", "Callisto cub", 0x5A3A22);
        pet(items, "pet_venenatis_spiderling", "Venenatis spiderling", 0x3A2A3A);
        pet(items, "vetion_jr", "Vet'ion jr.", 0x8A5AC0);
        pet(items, "pet_scorpias_offspring", "Scorpia's offspring", 0x2E2A26);
        pet(items, "pet_chaos_elemental", "Pet chaos elemental", 0xC03030);
        pet(items, "prince_black_dragon", "Prince black dragon", 0x2A2A2A);
    }

    private static void pet(ItemSink items, String id, String name, int color) {
        items.add(id, name, Kind.SIMPLE, "pet", color).value(1).stack(1).examine("A loyal companion.");
    }

    /** Godsword assembly (80 Smithing at an anvil, 100 XP per join), hilts, wards, keys and upgrades. */
    public static void recipes(Consumer<Recipe> recipes) {
        recipes.accept(Recipe.builder("godsword_shards_1_2", MenuType.SMITHING, "Godsword shards 1 & 2")
                .skill(Skill.SMITHING, 80, 100).in("godsword_shard_1", 1).in("godsword_shard_2", 1).out("godsword_shards_1_2", 1)
                .tool("hammer").ticks(3).build());
        recipes.accept(Recipe.builder("godsword_shards_1_3", MenuType.SMITHING, "Godsword shards 1 & 3")
                .skill(Skill.SMITHING, 80, 100).in("godsword_shard_1", 1).in("godsword_shard_3", 1).out("godsword_shards_1_3", 1)
                .tool("hammer").ticks(3).build());
        recipes.accept(Recipe.builder("godsword_shards_2_3", MenuType.SMITHING, "Godsword shards 2 & 3")
                .skill(Skill.SMITHING, 80, 100).in("godsword_shard_2", 1).in("godsword_shard_3", 1).out("godsword_shards_2_3", 1)
                .tool("hammer").ticks(3).build());
        recipes.accept(Recipe.builder("godsword_blade_12", MenuType.SMITHING, "Godsword blade (shards 1 & 2 + 3)")
                .skill(Skill.SMITHING, 80, 100).in("godsword_shards_1_2", 1).in("godsword_shard_3", 1).out("godsword_blade", 1)
                .tool("hammer").ticks(3).build());
        recipes.accept(Recipe.builder("godsword_blade_13", MenuType.SMITHING, "Godsword blade (shards 1 & 3 + 2)")
                .skill(Skill.SMITHING, 80, 100).in("godsword_shards_1_3", 1).in("godsword_shard_2", 1).out("godsword_blade", 1)
                .tool("hammer").ticks(3).build());
        recipes.accept(Recipe.builder("godsword_blade_23", MenuType.SMITHING, "Godsword blade (shards 2 & 3 + 1)")
                .skill(Skill.SMITHING, 80, 100).in("godsword_shards_2_3", 1).in("godsword_shard_1", 1).out("godsword_blade", 1)
                .tool("hammer").ticks(3).build());
        for (God god : God.values()) {
            recipes.accept(Recipe.builder(god.godsword(), MenuType.SMITHING, god.displayName() + " godsword")
                    .in("godsword_blade", 1).in(god.hilt(), 1).out(god.godsword(), 1).ticks(1).build());
        }
        recipes.accept(Recipe.builder("ancient_godsword", MenuType.SMITHING, "Ancient godsword")
                .in("godsword_blade", 1).in("ancient_hilt", 1).out("ancient_godsword", 1).ticks(1).build());
        // Approximation: OSRS fuses the horn onto the crossbow with 250 nihil shards and no skill requirement.
        recipes.accept(Recipe.builder("zaryte_crossbow", MenuType.SMITHING, "Zaryte crossbow")
                .in("armadyl_crossbow", 1).in("nihil_horn", 1).in("nihil_shard", 250).out("zaryte_crossbow", 1).ticks(2).build());
        Recipe.Builder key = Recipe.builder("frozen_key", MenuType.SMITHING, "Frozen key").out("frozen_key", 1).ticks(1);
        for (God god : God.values()) {
            key.in(god.keyPiece(), 1);
        }
        recipes.accept(key.build());
        recipes.accept(Recipe.builder("odium_ward", MenuType.SMITHING, "Odium ward")
                .in("odium_shard_1", 1).in("odium_shard_2", 1).in("odium_shard_3", 1).out("odium_ward", 1).ticks(3).build());
        recipes.accept(Recipe.builder("malediction_ward", MenuType.SMITHING, "Malediction ward")
                .in("malediction_shard_1", 1).in("malediction_shard_2", 1).in("malediction_shard_3", 1).out("malediction_ward", 1)
                .ticks(3).build());
        recipes.accept(Recipe.builder("voidwaker", MenuType.SMITHING, "Voidwaker")
                .in("voidwaker_hilt", 1).in("voidwaker_blade", 1).in("voidwaker_gem", 1).out("voidwaker", 1).ticks(2).build());
        recipes.accept(Recipe.builder("webweaver_bow", MenuType.SMITHING, "Webweaver bow")
                .in("craws_bow", 1).in("fangs_of_venenatis", 1).out("webweaver_bow", 1).ticks(2).build());
        recipes.accept(Recipe.builder("ursine_chainmace", MenuType.SMITHING, "Ursine chainmace")
                .in("viggoras_chainmace", 1).in("claws_of_callisto", 1).out("ursine_chainmace", 1).ticks(2).build());
        recipes.accept(Recipe.builder("accursed_sceptre", MenuType.SMITHING, "Accursed sceptre")
                .in("thammarons_sceptre", 1).in("skull_of_vetion", 1).out("accursed_sceptre", 1).ticks(2).build());
    }
}
