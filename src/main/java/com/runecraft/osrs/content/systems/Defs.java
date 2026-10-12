package com.runecraft.osrs.content.systems;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Small helpers for defining the systems module's items. */
final class Defs {
    private Defs() {
    }

    static Map<Skill, Integer> req(Object... pairs) {
        Map<Skill, Integer> m = new EnumMap<>(Skill.class);
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((Skill) pairs[i], (Integer) pairs[i + 1]);
        }
        return m;
    }

    static EquipmentStats st(int stab, int slash, int crush, int mag, int rng, int dStab, int dSlash, int dCrush, int dMag, int dRng,
                             int str, int rStr, int mDmg, int pray) {
        return new EquipmentStats(stab, slash, crush, mag, rng, dStab, dSlash, dCrush, dMag, dRng, str, rStr, mDmg, pray);
    }

    /** Armour piece in a vanilla armour slot (head/chest/legs/feet) or an off-hand shield. */
    static ItemDef armour(ItemSink items, String id, String name, String shape, int color, String tier, int value,
                          EquipmentDef.Slot slot, EquipmentStats stats, Map<Skill, Integer> reqs) {
        Kind kind = slot == EquipmentDef.Slot.SHIELD ? Kind.SHIELD : Kind.ARMOUR;
        return items.add(id, name, kind, shape, color).value(value).stack(1).tier(tier)
                .equipment(new EquipmentDef(slot, stats, reqs, 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE));
    }

    static ItemDef melee(ItemSink items, String id, String name, String shape, int color, String tier, int value, EquipmentStats stats,
                         Map<Skill, Integer> reqs, int speed, AttackType type) {
        return items.add(id, name, Kind.MELEE_WEAPON, shape, color).value(value).stack(1).tier(tier)
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, stats, reqs, speed, type, EquipmentDef.WeaponClass.MELEE));
    }

    static ItemDef bow(ItemSink items, String id, String name, String shape, int color, int value, EquipmentStats stats,
                       Map<Skill, Integer> reqs, int speed) {
        return items.add(id, name, Kind.BOW, shape, color).value(value).stack(1).tier("magic")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, stats, reqs, speed, AttackType.RANGED, EquipmentDef.WeaponClass.BOW));
    }

    static ItemDef staff(ItemSink items, String id, String name, String shape, int color, int value, EquipmentStats stats,
                         Map<Skill, Integer> reqs) {
        return items.add(id, name, Kind.STAFF, shape, color).value(value).stack(1).tier("rune")
                .equipment(new EquipmentDef(EquipmentDef.Slot.WEAPON, stats, reqs, 4, AttackType.MAGIC, EquipmentDef.WeaponClass.STAFF));
    }

    /**
     * An item worn in one of this module's extra slots (neck, ring, cape, hands). The catalog entry is a
     * SIMPLE item whose equipment uses the HEAD slot purely so tooltips show its bonuses; core code never
     * equips SIMPLE items, the systems module applies the stats itself.
     */
    static ItemDef worn(ItemSink items, String id, String name, String shape, int color, int value, Jewellery.Slot slot,
                        EquipmentStats stats, Map<Skill, Integer> reqs, Jewellery.Perk perk, List<Teleports.Destination> teleports) {
        Jewellery.register(new Jewellery.Worn(id, name, slot, stats, perk, null, -1, teleports, false));
        ItemDef def = items.add(id, name, Kind.SIMPLE, shape, color).value(value).stack(1);
        if (!stats.equals(EquipmentStats.NONE) || !reqs.isEmpty()) {
            def.equipment(new EquipmentDef(EquipmentDef.Slot.HEAD, stats, reqs, 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE));
        }
        return def;
    }

    static ItemDef simple(ItemSink items, String id, String name, String shape, int color, int value) {
        return items.add(id, name, Kind.SIMPLE, shape, color).value(value);
    }

    // ---------------- Real OSRS armour stats for trimmed/gold-trimmed/god variants ----------------

    enum Piece { HELM, BODY, LEGS, SHIELD }

    static EquipmentDef.Slot slot(Piece p) {
        return switch (p) {
            case HELM -> EquipmentDef.Slot.HEAD;
            case BODY -> EquipmentDef.Slot.CHEST;
            case LEGS -> EquipmentDef.Slot.LEGS;
            case SHIELD -> EquipmentDef.Slot.SHIELD;
        };
    }

    static String shape(Piece p) {
        return switch (p) {
            case HELM -> "full_helm";
            case BODY -> "platebody";
            case LEGS -> "platelegs";
            case SHIELD -> "kiteshield";
        };
    }

    static String suffix(Piece p) {
        return switch (p) {
            case HELM -> "full_helm";
            case BODY -> "platebody";
            case LEGS -> "platelegs";
            case SHIELD -> "kiteshield";
        };
    }

    static String label(Piece p) {
        return switch (p) {
            case HELM -> "full helm";
            case BODY -> "platebody";
            case LEGS -> "platelegs";
            case SHIELD -> "kiteshield";
        };
    }

    /** Plate armour stats per metal: black (10), adamant (30), rune (40). */
    static EquipmentStats plate(String metal, Piece p) {
        return switch (metal) {
            case "bronze" -> switch (p) {
                case HELM -> st(0, 0, 0, -6, -2, 4, 5, 3, -1, 4, 0, 0, 0, 0);
                case BODY -> st(0, 0, 0, -30, -10, 15, 14, 9, -6, 14, 0, 0, 0, 0);
                case LEGS -> st(0, 0, 0, -21, -7, 8, 7, 6, -4, 7, 0, 0, 0, 0);
                case SHIELD -> st(0, 0, 0, -8, -2, 5, 7, 6, -1, 6, 0, 0, 0, 0);
            };
            case "iron" -> switch (p) {
                case HELM -> st(0, 0, 0, -6, -2, 6, 7, 5, -1, 6, 0, 0, 0, 0);
                case BODY -> st(0, 0, 0, -30, -10, 21, 20, 12, -6, 20, 0, 0, 0, 0);
                case LEGS -> st(0, 0, 0, -21, -7, 11, 10, 10, -4, 10, 0, 0, 0, 0);
                case SHIELD -> st(0, 0, 0, -8, -2, 8, 9, 8, -1, 8, 0, 0, 0, 0);
            };
            case "steel" -> switch (p) {
                case HELM -> st(0, 0, 0, -6, -2, 9, 10, 7, -1, 9, 0, 0, 0, 0);
                case BODY -> st(0, 0, 0, -30, -10, 32, 31, 24, -6, 31, 0, 0, 0, 0);
                case LEGS -> st(0, 0, 0, -21, -7, 17, 16, 15, -4, 16, 0, 0, 0, 0);
                case SHIELD -> st(0, 0, 0, -8, -2, 12, 14, 13, -1, 13, 0, 0, 0, 0);
            };
            case "black" -> switch (p) {
                case HELM -> st(0, 0, 0, -6, -2, 12, 13, 10, -1, 12, 0, 0, 0, 0);
                case BODY -> st(0, 0, 0, -30, -10, 41, 40, 30, -6, 40, 0, 0, 0, 0);
                case LEGS -> st(0, 0, 0, -21, -7, 21, 20, 19, -4, 20, 0, 0, 0, 0);
                case SHIELD -> st(0, 0, 0, -8, -2, 19, 21, 20, 0, 20, 0, 0, 0, 0);
            };
            case "adamant" -> switch (p) {
                case HELM -> st(0, 0, 0, -6, -2, 19, 21, 16, -1, 19, 0, 0, 0, 0);
                case BODY -> st(0, 0, 0, -30, -10, 65, 63, 55, -6, 63, 0, 0, 0, 0);
                case LEGS -> st(0, 0, 0, -21, -7, 33, 31, 29, -4, 31, 0, 0, 0, 0);
                case SHIELD -> st(0, 0, 0, -8, -2, 31, 33, 31, -1, 32, 0, 0, 0, 0);
            };
            case "mithril" -> switch (p) {
                case HELM -> st(0, 0, 0, -6, -2, 13, 14, 11, -1, 13, 0, 0, 0, 0);
                case BODY -> st(0, 0, 0, -30, -10, 46, 44, 38, -6, 44, 0, 0, 0, 0);
                case LEGS -> st(0, 0, 0, -21, -7, 24, 22, 20, -4, 22, 0, 0, 0, 0);
                case SHIELD -> st(0, 0, 0, -8, -2, 22, 24, 22, -1, 23, 0, 0, 0, 0);
            };
            default -> switch (p) { // rune (also gilded and god-blessed rune armour)
                case HELM -> st(0, 0, 0, -6, -2, 30, 32, 27, -1, 30, 0, 0, 0, 0);
                case BODY -> st(0, 0, 0, -30, -10, 82, 80, 72, -6, 80, 0, 0, 0, 0);
                case LEGS -> st(0, 0, 0, -21, -7, 51, 49, 47, -4, 49, 0, 0, 0, 0);
                case SHIELD -> st(0, 0, 0, -8, -2, 44, 48, 46, -1, 46, 0, 0, 0, 0);
            };
        };
    }

    static int plateLevel(String metal) {
        return switch (metal) {
            case "bronze", "iron" -> 1;
            case "steel" -> 5;
            case "black" -> 10;
            case "mithril" -> 20;
            case "adamant" -> 30;
            default -> 40;
        };
    }

    /** Dragonhide body and chaps: green (40), blue (50), red (60), black (70) Ranged. */
    static EquipmentStats dhide(String colour, boolean body) {
        return switch (colour) {
            case "green" -> body ? st(0, 0, 0, -15, 15, 18, 27, 24, 20, 35, 0, 0, 0, 0) : st(0, 0, 0, -10, 8, 12, 15, 18, 8, 15, 0, 0, 0, 0);
            case "blue" -> body ? st(0, 0, 0, -15, 20, 26, 37, 32, 25, 47, 0, 0, 0, 0) : st(0, 0, 0, -10, 11, 16, 20, 23, 12, 20, 0, 0, 0, 0);
            case "red" -> body ? st(0, 0, 0, -15, 25, 32, 44, 40, 30, 56, 0, 0, 0, 0) : st(0, 0, 0, -10, 14, 20, 25, 28, 15, 25, 0, 0, 0, 0);
            default -> body ? st(0, 0, 0, -15, 30, 55, 47, 62, 50, 57, 0, 0, 0, 0) : st(0, 0, 0, -10, 17, 31, 25, 33, 28, 31, 0, 0, 0, 0);
        };
    }

    static int dhideLevel(String colour) {
        return switch (colour) {
            case "green" -> 40;
            case "blue" -> 50;
            case "red" -> 60;
            default -> 70;
        };
    }

    static int dhideColor(String colour) {
        return switch (colour) {
            case "green" -> 0x3A8A3A;
            case "blue" -> 0x3A5AB0;
            case "red" -> 0xB03A3A;
            default -> 0x2A2A2A;
        };
    }

    static String cap(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
