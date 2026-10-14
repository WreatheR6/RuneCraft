package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentDef.Slot;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.data.MetalTier;

import java.util.List;
import java.util.function.Consumer;

/**
 * Smithing: the remaining anvil items for every metal tier (med helms, square shields, chainbodies,
 * plateskirts, swords, maces, claws), ranged ammunition parts (dart tips, unfinished bolts, javelin
 * heads, throwing knives, crossbow limbs), nails, steel studs, cannonballs and blurite. Levels follow
 * the OSRS smithing table (tier base level + item offset); XP is the tier's XP per bar.
 */
public final class SmithingData extends SkillingPart {

    /** Smithable tiers (black and dragon have no bar). */
    public static final MetalTier[] SMITHABLE = {MetalTier.BRONZE, MetalTier.IRON, MetalTier.STEEL, MetalTier.MITHRIL,
            MetalTier.ADAMANT, MetalTier.RUNE};

    /** Gear pieces: id suffix, name suffix, level offset, bars, value factor, rune stats, slot (null = weapon). */
    private record Piece(String suffix, String name, int offset, int bars, double valueFactor, EquipmentStats rune, Slot slot,
                         String shape, AttackType type, int speed) {
    }

    private static final Piece[] PIECES = {
            new Piece("med_helm", "med helm", 3, 1, 0.6, EquipmentStats.defence(22, 23, 21, -1, 22, -3, -1), Slot.HEAD, "med_helm", null, 0),
            new Piece("sword", "sword", 4, 1, 0.65, stats(38, 26, -2, 0, 0, 0, 0, 0, 0, 0, 39, 0, 0, 0), null, "sword", AttackType.STAB, 4),
            new Piece("mace", "mace", 2, 1, 0.72, stats(20, -2, 39, 0, 0, 0, 0, 0, 0, 0, 36, 0, 0, 4), null, "mace", AttackType.CRUSH, 5),
            new Piece("sq_shield", "sq shield", 8, 2, 1.2, EquipmentStats.defence(38, 40, 36, -1, 38, -6, -2), Slot.SHIELD, "sq_shield", null, 0),
            new Piece("chainbody", "chainbody", 11, 3, 1.5, EquipmentStats.defence(63, 72, 78, -3, 65, -15, 0), Slot.CHEST, "chainbody", null, 0),
            new Piece("claws", "claws", 13, 2, 1.25, stats(26, 38, -4, 0, 0, 10, 19, 5, 0, 0, 39, 0, 0, 0), null, "claws", AttackType.SLASH, 4),
            new Piece("plateskirt", "plateskirt", 16, 3, 1.6, EquipmentStats.defence(51, 49, 47, -4, 49, -21, -11), Slot.LEGS, "plateskirt", null, 0)};

    /** Ranged ammo parts: suffix, name, level offset, amount per bar. */
    private record Ammo(String suffix, String name, int offset, int amount, String shape) {
    }

    private static final Ammo[] AMMO = {
            new Ammo("bolts_unf", "bolts (unf)", 3, 10, "bolts"),
            new Ammo("dart_tip", "dart tip", 4, 10, "dart_tip"),
            new Ammo("nails", "nails", 4, 15, "nails"),
            new Ammo("javelin_heads", "javelin heads", 6, 5, "javelin_heads"),
            new Ammo("knife", "knife", 7, 5, "throwing_knife")};

    /** Crossbow limbs smithing levels (OSRS table, not the generic offset). */
    private static int limbsLevel(MetalTier t) {
        return switch (t) {
            case BRONZE -> 6;
            case IRON -> 23;
            case STEEL -> 36;
            case MITHRIL -> 56;
            case ADAMANT -> 76;
            default -> 91;
        };
    }

    /** Thrown knife ranged strength (OSRS). */
    public static int knifeStrength(MetalTier t) {
        return switch (t) {
            case BRONZE -> 3;
            case IRON -> 4;
            case STEEL -> 7;
            case MITHRIL -> 10;
            case ADAMANT -> 14;
            default -> 24;
        };
    }

    public static String name(MetalTier t) {
        return t == MetalTier.RUNE ? "Rune" : t.displayName();
    }

    @Override
    public void items(ItemSink s) {
        MetalTier[] gearTiers = {MetalTier.BRONZE, MetalTier.IRON, MetalTier.STEEL, MetalTier.BLACK, MetalTier.MITHRIL,
                MetalTier.ADAMANT, MetalTier.RUNE};
        for (MetalTier t : gearTiers) {
            for (Piece p : PIECES) {
                EquipmentStats st = t == MetalTier.RUNE ? p.rune : scale(p.rune, t.statFactor());
                int value = (int) (tierValue(t) * p.valueFactor);
                String id = t.id() + "_" + p.suffix;
                String name = t.displayName() + " " + p.name;
                if (p.slot == null) {
                    s.add(id, name, Kind.MELEE_WEAPON, p.shape, t.color()).value(value).stack(1).tier(t.id())
                            .equipment(EquipmentDef.melee(st, t.requirement(), p.speed, p.type));
                } else {
                    s.add(id, name, p.slot == Slot.SHIELD ? Kind.SHIELD : Kind.ARMOUR, p.shape, t.color()).value(value).stack(1)
                            .tier(t.id()).equipment(EquipmentDef.armour(p.slot, st, t.requirement()));
                }
            }
        }
        for (MetalTier t : SMITHABLE) {
            int base = Math.max(1, tierValue(t) / 32);
            for (Ammo a : AMMO) {
                String id = t.id() + "_" + a.suffix;
                if (a.suffix.equals("knife")) {
                    s.add(id, name(t) + " knife", Kind.ARROW, a.shape, t.color()).value(base * 2).rangedStrength(knifeStrength(t))
                            .tier(t.id()).examine("A finely balanced throwing knife.");
                } else {
                    String metal = t == MetalTier.RUNE && a.suffix.equals("bolts_unf") ? "Runite" : t.displayName();
                    simple(s, id, metal + " " + a.name, a.shape, t.color(), base);
                }
            }
            simple(s, t.id() + "_limbs", (t == MetalTier.RUNE ? "Runite" : t.displayName()) + " limbs", "limbs", t.color(), base * 4);
        }
        simple(s, "steel_studs", "Steel studs", "nails", 0xB8B8C0, 20);
        // The cannonball and the ammo moulds are in CommonItems (the Dwarf Cannon quest and the multicannon use them too).
        // Blurite
        simple(s, "blurite_ore", "Blurite ore", "ore", 0x4A6AC0, 25);
        simple(s, "blurite_bar", "Blurite bar", "bar", 0x5A7AD0, 50);
        simple(s, "blurite_bolts_unf", "Blurite bolts (unf)", "bolts", 0x5A7AD0, 4);
        simple(s, "blurite_limbs", "Blurite limbs", "limbs", 0x5A7AD0, 20);
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        for (MetalTier t : SMITHABLE) {
            for (Piece p : PIECES) {
                String id = t.id() + "_" + p.suffix;
                smith(out, id, t.displayName() + " " + p.name, 1, t, p.bars, t.smithingLevel(p.offset));
            }
            for (Ammo a : AMMO) {
                String id = t.id() + "_" + a.suffix;
                String name = a.amount + " " + (a.suffix.equals("knife") ? lower(name(t)) + " knives" : lower(t.displayName()) + " " + a.name
                        + (a.suffix.equals("dart_tip") ? "s" : ""));
                smith(out, id, name, a.amount, t, 1, t.smithingLevel(a.offset));
            }
            smith(out, t.id() + "_limbs", (t == MetalTier.RUNE ? "Runite" : t.displayName()) + " limbs", 1, t, 1, limbsLevel(t));
        }
        // Barbarian smithing (Barbarian Training): spears and hastae take one bar plus a log of the tier's wood,
        // and give twice the tier's XP per bar (bronze 25 ... rune 150).
        String[] spearLogs = {"logs", "oak_logs", "willow_logs", "maple_logs", "yew_logs", "magic_logs"};
        for (int i = 0; i < SMITHABLE.length; i++) {
            MetalTier t = SMITHABLE[i];
            int level = t.smithingLevel(5);
            // The bronze hasta is the quests module's Barbarian Training reward, so it isn't smithed here (approx).
            List<String> heads = t == MetalTier.BRONZE ? List.of("spear") : List.of("spear", "hasta");
            for (String head : heads) {
                String id = t.id() + "_" + head;
                out.accept(r("smith_" + id, MenuType.SMITHING, t.displayName() + " " + head, Skill.SMITHING, level, t.xpPerBar() * 2)
                        .in(t.bar(), 1).in(spearLogs[i], 1).out(id, 1).tool("hammer").ticks(4).build());
            }
        }
        // Dragon sq shield: both halves joined on an anvil (Legends' Quest gates wearing it).
        out.accept(r("smith_dragon_sq_shield", MenuType.SMITHING, "Dragon sq shield", Skill.SMITHING, 60, 75.0)
                .in("shield_left_half", 1).in("shield_right_half", 1).out("dragon_sq_shield", 1).tool("hammer").ticks(4).build());
        smith(out, "steel_studs", "Steel studs", 1, MetalTier.STEEL, 1, 36);
        // Steel cannonballs (OSRS Wiki): 35 Smithing, 25.6 XP per bar, 8 ticks at a furnace; the double ammo mould (Giants'
        // Foundry) makes 8 from 2 bars in the same time.
        out.accept(r("smith_cannonball", MenuType.SMELTING, "4 cannonballs", Skill.SMITHING, 35, 25.6).in("steel_bar", 1)
                .out("cannonball", 4).tool("ammo_mould").ticks(8).build());
        out.accept(r("smith_cannonball_double", MenuType.SMELTING, "8 cannonballs (double mould)", Skill.SMITHING, 35, 51.2)
                .in("steel_bar", 2).out("cannonball", 8).tool("double_ammo_mould").ticks(8).build());
        // Blurite: smelted at level 13 (8 XP), smithed into bolts and limbs.
        out.accept(r("smelt_blurite_bar", MenuType.SMELTING, "Blurite bar", Skill.SMITHING, 13, 8.0).in("blurite_ore", 1)
                .out("blurite_bar", 1).ticks(4).build());
        out.accept(r("smith_blurite_bolts_unf", MenuType.SMITHING, "10 blurite bolts (unf)", Skill.SMITHING, 13, 17.0)
                .in("blurite_bar", 1).out("blurite_bolts_unf", 10).tool("hammer").ticks(4).build());
        out.accept(r("smith_blurite_limbs", MenuType.SMITHING, "Blurite limbs", Skill.SMITHING, 13, 15.0)
                .in("blurite_bar", 1).out("blurite_limbs", 1).tool("hammer").ticks(4).build());
    }

    private static void smith(Consumer<Recipe> out, String id, String name, int amount, MetalTier tier, int bars, int level) {
        out.accept(r("smith_" + id, MenuType.SMITHING, name, Skill.SMITHING, Math.max(1, Math.min(99, level)), tier.xpPerBar() * bars)
                .in(tier.bar(), bars).out(id, amount).tool("hammer").ticks(4).build());
    }

    /** All new tiered gear ids (for tests). */
    public static List<String> gearSuffixes() {
        return List.of("med_helm", "sword", "mace", "sq_shield", "chainbody", "claws", "plateskirt");
    }
}
