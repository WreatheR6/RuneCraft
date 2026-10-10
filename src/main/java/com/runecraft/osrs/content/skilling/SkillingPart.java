package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * One skill's worth of data in the skilling module. {@link SkillingContent} merges every part.
 * Also holds the small builder helpers the parts share.
 */
public abstract class SkillingPart {
    public void items(ItemSink items) {
    }

    public void recipes(Consumer<Recipe> recipes) {
    }

    public List<BlockDef> blocks() {
        return List.of();
    }

    public List<PlaceDef> places() {
        return List.of();
    }

    public List<NpcDef> npcs() {
        return List.of();
    }

    public List<MonsterDef> monsters() {
        return List.of();
    }

    // ------------------------------------------------------------------ helpers

    protected static ItemDef simple(ItemSink s, String id, String name, String shape, int color, int value) {
        return s.add(id, name, Kind.SIMPLE, shape, color).value(value);
    }

    protected static ItemDef food(ItemSink s, String id, String name, String shape, int color, int value, int heal) {
        return s.add(id, name, Kind.FOOD, shape, color).value(value).heal(heal);
    }

    protected static ItemDef tool(ItemSink s, String id, String name, String shape, int color, int value) {
        return s.add(id, name, Kind.TOOL, shape, color).value(value).stack(1);
    }

    protected static ItemDef potion(ItemSink s, String id, String name, int color, int value) {
        return s.add(id, name, Kind.POTION, "potion", color).value(value).stack(1);
    }

    protected static Recipe.Builder r(String id, MenuType menu, String name, Skill skill, int level, double xp) {
        return Recipe.builder(id, menu, name).skill(skill, level, xp);
    }

    /** A recipe with no skill (shops, simple combining). */
    protected static Recipe.Builder plain(String id, MenuType menu, String name) {
        return Recipe.builder(id, menu, name);
    }

    /** "green_dhide_body" -> "Green dhide body". */
    protected static String title(String id) {
        String s = id.replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    protected static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    protected static EquipmentStats stats(int sa, int sl, int cr, int ma, int ra, int sd, int sld, int cd, int md, int rd,
                                          int str, int rstr, int mdmg, int prayer) {
        return new EquipmentStats(sa, sl, cr, ma, ra, sd, sld, cd, md, rd, str, rstr, mdmg, prayer);
    }

    protected static Map<Skill, Integer> reqs(Object... reqs) {
        Map<Skill, Integer> map = new EnumMap<>(Skill.class);
        for (int i = 0; i < reqs.length; i += 2) {
            int lvl = (Integer) reqs[i + 1];
            if (lvl > 1) {
                map.put((Skill) reqs[i], lvl);
            }
        }
        return map;
    }

    protected static EquipmentDef wear(EquipmentDef.Slot slot, EquipmentStats stats, Object... reqs) {
        return new EquipmentDef(slot, stats, reqs(reqs), 0, AttackType.CRUSH, EquipmentDef.WeaponClass.NONE);
    }

    protected static EquipmentDef weapon(EquipmentStats stats, int speed, AttackType type, EquipmentDef.WeaponClass cls,
                                         Object... reqs) {
        return new EquipmentDef(EquipmentDef.Slot.WEAPON, stats, reqs(reqs), speed, type, cls);
    }

    /** Scales positive bonuses by a tier factor; penalties stay as-is (like the built-in tier tables). */
    protected static EquipmentStats scale(EquipmentStats s, double f) {
        return new EquipmentStats(sc(s.stabAttack(), f), sc(s.slashAttack(), f), sc(s.crushAttack(), f), s.magicAttack(),
                sc(s.rangedAttack(), f), sc(s.stabDefence(), f), sc(s.slashDefence(), f), sc(s.crushDefence(), f),
                s.magicDefence(), sc(s.rangedDefence(), f), sc(s.meleeStrength(), f), sc(s.rangedStrength(), f),
                s.magicDamagePercent(), s.prayer() > 0 ? Math.max(1, (int) Math.round(s.prayer() * f)) : s.prayer());
    }

    private static int sc(int v, double f) {
        return v > 0 ? Math.max(1, (int) Math.round(v * f)) : v;
    }

    /** Store value of a metal tier's base item (matches ItemCatalog's tier values). */
    protected static int tierValue(com.runecraft.osrs.data.MetalTier tier) {
        return switch (tier) {
            case BRONZE -> 32;
            case IRON -> 112;
            case STEEL -> 400;
            case BLACK -> 768;
            case MITHRIL -> 1040;
            case ADAMANT -> 2560;
            case RUNE -> 25600;
            case DRAGON -> 100000;
        };
    }
}
