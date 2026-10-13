package com.runecraft.osrs.content.skilling;

import com.runecraft.osrs.Landmark;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ItemSink;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Construction: furniture is built at a workbench as a flatpack (Make-X, CONSTRUCTION menu, real
 * levels/XP/materials) and placed anywhere by using the flatpack on a block. Placed furniture blocks
 * keep their OSRS behaviour: altars (bone offering XP multipliers with lit incense burners),
 * larders, bookcases, restoration pools and teleport portals. Building materials (bolts of cloth,
 * limestone, marble, gold leaf, magic stones) are sold at the workbench like at a sawmill/stonemason.
 */
public final class ConstructionData extends SkillingPart {

    public enum Use { DECOR, ALTAR, BURNER, LARDER, BOOKCASE, POOL, PORTAL }

    /**
     * A furniture piece. {@code power}: altar XP multiplier (percent), pool tier (1-5) or unused.
     * {@code destination} is a Landmark name for portals.
     */
    public record Furniture(String id, String name, int level, double xp, Map<String, Integer> materials, String look, Use use,
                            int power, String destination, int color) {
        public String block() {
            return "poh_" + id;
        }

        public String flatpack() {
            return id + "_flatpack";
        }
    }

    public static final List<Furniture> FURNITURE = new ArrayList<>();

    private static void f(String id, String name, int level, double xp, String look, Use use, int power, String dest, int color,
                          Object... mats) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < mats.length; i += 2) {
            m.put((String) mats[i], (Integer) mats[i + 1]);
        }
        FURNITURE.add(new Furniture(id, name, level, xp, m, look, use, power, dest, color));
    }

    static {
        int wood = 0x8A6A3A;
        int oak = 0x9A7A4A;
        int teak = 0x9C7A4A;
        int mahog = 0x6A2A1A;
        // Chairs
        f("crude_wooden_chair", "Crude wooden chair", 1, 58.0, "chair", Use.DECOR, 0, null, wood, "plank", 2, "iron_nails", 2);
        f("wooden_chair", "Wooden chair", 8, 87.0, "chair", Use.DECOR, 0, null, wood, "plank", 3, "iron_nails", 2);
        f("rocking_chair", "Rocking chair", 14, 87.0, "chair", Use.DECOR, 0, null, wood, "plank", 3, "iron_nails", 3);
        f("oak_chair", "Oak chair", 19, 120.0, "chair", Use.DECOR, 0, null, oak, "oak_plank", 2);
        f("oak_armchair", "Oak armchair", 26, 180.0, "chair", Use.DECOR, 0, null, oak, "oak_plank", 3);
        f("teak_armchair", "Teak armchair", 35, 180.0, "chair", Use.DECOR, 0, null, teak, "teak_plank", 2);
        f("mahogany_armchair", "Mahogany armchair", 50, 280.0, "chair", Use.DECOR, 0, null, mahog, "mahogany_plank", 2);
        // Bookcases
        f("wooden_bookcase", "Wooden bookcase", 4, 115.0, "bookcase", Use.BOOKCASE, 0, null, wood, "plank", 4, "iron_nails", 4);
        f("oak_bookcase", "Oak bookcase", 29, 180.0, "bookcase", Use.BOOKCASE, 0, null, oak, "oak_plank", 3);
        f("mahogany_bookcase", "Mahogany bookcase", 40, 420.0, "bookcase", Use.BOOKCASE, 0, null, mahog, "mahogany_plank", 3);
        // Tables
        f("wood_dining_table", "Wood dining table", 10, 115.0, "table", Use.DECOR, 0, null, wood, "plank", 4, "iron_nails", 4);
        f("oak_dining_table", "Oak dining table", 22, 240.0, "table", Use.DECOR, 0, null, oak, "oak_plank", 4);
        f("carved_oak_table", "Carved oak table", 31, 360.0, "table", Use.DECOR, 0, null, oak, "oak_plank", 6);
        f("teak_table", "Teak table", 38, 360.0, "table", Use.DECOR, 0, null, teak, "teak_plank", 4);
        f("carved_teak_table", "Carved teak table", 45, 600.0, "table", Use.DECOR, 0, null, teak, "teak_plank", 6, "bolt_of_cloth", 4);
        f("mahogany_table", "Mahogany table", 52, 840.0, "table", Use.DECOR, 0, null, mahog, "mahogany_plank", 6);
        f("opulent_table", "Opulent table", 72, 3100.0, "table", Use.DECOR, 0, null, 0xE0C060, "mahogany_plank", 6, "bolt_of_cloth", 4,
                "gold_leaf", 4, "marble_block", 2);
        // Larders
        f("wooden_larder", "Wooden larder", 9, 228.0, "larder", Use.LARDER, 1, null, wood, "plank", 8, "iron_nails", 8);
        f("oak_larder", "Oak larder", 33, 480.0, "larder", Use.LARDER, 2, null, oak, "oak_plank", 8);
        f("teak_larder", "Teak larder", 43, 750.0, "larder", Use.LARDER, 3, null, teak, "teak_plank", 8, "bolt_of_cloth", 2);
        // Beds
        f("wooden_bed", "Wooden bed", 20, 117.0, "bed", Use.DECOR, 0, null, wood, "plank", 3, "iron_nails", 3, "bolt_of_cloth", 2);
        f("oak_bed", "Oak bed", 30, 210.0, "bed", Use.DECOR, 0, null, oak, "oak_plank", 3, "bolt_of_cloth", 2);
        f("large_oak_bed", "Large oak bed", 34, 330.0, "bed", Use.DECOR, 0, null, oak, "oak_plank", 5, "bolt_of_cloth", 2);
        f("teak_bed", "Teak bed", 40, 300.0, "bed", Use.DECOR, 0, null, teak, "teak_plank", 3, "bolt_of_cloth", 2);
        f("large_teak_bed", "Large teak bed", 45, 480.0, "bed", Use.DECOR, 0, null, teak, "teak_plank", 5, "bolt_of_cloth", 2);
        f("four_poster_bed", "4-poster bed", 53, 450.0, "bed", Use.DECOR, 0, null, mahog, "mahogany_plank", 3, "bolt_of_cloth", 2);
        f("gilded_four_poster", "Gilded 4-poster", 60, 1330.0, "bed", Use.DECOR, 0, null, 0xE0C060, "mahogany_plank", 5,
                "bolt_of_cloth", 2, "gold_leaf", 2);
        // Chapel altars (bone offering XP multiplier, percent)
        f("oak_altar", "Oak altar", 45, 240.0, "altar", Use.ALTAR, 110, null, oak, "oak_plank", 4);
        f("teak_altar", "Teak altar", 50, 360.0, "altar", Use.ALTAR, 120, null, teak, "teak_plank", 4);
        f("cloth_covered_altar", "Cloth-covered altar", 56, 390.0, "altar", Use.ALTAR, 130, null, 0xE0E0F0, "teak_plank", 4,
                "bolt_of_cloth", 2);
        f("mahogany_altar", "Mahogany altar", 60, 590.0, "altar", Use.ALTAR, 140, null, mahog, "mahogany_plank", 4, "bolt_of_cloth", 2);
        f("limestone_altar", "Limestone altar", 64, 910.0, "altar", Use.ALTAR, 150, null, 0xD8D0B0, "mahogany_plank", 6,
                "bolt_of_cloth", 2, "limestone_brick", 2);
        f("marble_altar", "Marble altar", 70, 1030.0, "altar", Use.ALTAR, 200, null, 0xF0F0F0, "marble_block", 2, "bolt_of_cloth", 2);
        f("gilded_altar", "Gilded altar", 75, 2230.0, "altar", Use.ALTAR, 250, null, 0xE0C060, "marble_block", 2, "bolt_of_cloth", 2,
                "gold_leaf", 4);
        // Incense burners (each lit burner adds 50% to an altar within 8 blocks, up to 2)
        f("oak_burner", "Oak incense burner", 55, 280.0, "burner", Use.BURNER, 0, null, oak, "oak_plank", 4, "steel_bar", 2);
        f("teak_burner", "Teak incense burner", 64, 300.0, "burner", Use.BURNER, 0, null, teak, "teak_plank", 4, "steel_bar", 2);
        f("mahogany_burner", "Mahogany incense burner", 72, 300.0, "burner", Use.BURNER, 0, null, mahog, "mahogany_plank", 4,
                "steel_bar", 2);
        // Pools (OSRS superior garden): each is an upgrade of the previous pool, so its flatpack is made from the previous
        // pool's flatpack plus the upgrade materials. Tier 1 restores special attack, 2 adds run energy, 3 prayer, 4 lowered
        // stats, 5 hitpoints and cures poison/venom.
        f("restoration_pool", "Restoration pool", 65, 706.0, "pool", Use.POOL, 1, null, 0x6080C0, "limestone_brick", 5,
                "minecraft:water_bucket", 5, "soul_rune", 1000, "body_rune", 1000);
        f("revitalisation_pool", "Revitalisation pool", 70, 850.0, "pool", Use.POOL, 2, null, 0x5090C0,
                "restoration_pool_flatpack", 1, "stamina_potion", 10);
        f("rejuvenation_pool", "Rejuvenation pool", 80, 900.0, "pool", Use.POOL, 3, null, 0x40A0C0,
                "revitalisation_pool_flatpack", 1, "prayer_potion", 10);
        f("fancy_rejuvenation_pool", "Fancy rejuvenation pool", 85, 1950.0, "pool", Use.POOL, 4, null, 0x40B0D0,
                "rejuvenation_pool_flatpack", 1, "marble_block", 2, "super_restore", 10);
        f("ornate_rejuvenation_pool", "Ornate rejuvenation pool", 90, 3107.0, "pool", Use.POOL, 5, null, 0x40C0E0,
                "fancy_rejuvenation_pool_flatpack", 1, "gold_leaf", 2, "anti_venom_plus", 10); // approx materials
        // Portal chamber (teak portal + focus runes)
        f("varrock_portal", "Varrock portal", 50, 270.0, "portal_glow", Use.PORTAL, 0, Landmark.VARROCK.name(), 0x9070E0,
                "teak_plank", 3, "law_rune", 100, "air_rune", 300, "fire_rune", 100);
        f("lumbridge_portal", "Lumbridge portal", 50, 270.0, "portal_glow", Use.PORTAL, 0, Landmark.LUMBRIDGE.name(), 0x9070E0,
                "teak_plank", 3, "law_rune", 100, "air_rune", 300, "earth_rune", 100);
        f("falador_portal", "Falador portal", 50, 270.0, "portal_glow", Use.PORTAL, 0, Landmark.FALADOR.name(), 0x9070E0,
                "teak_plank", 3, "law_rune", 100, "air_rune", 300, "water_rune", 100);
        f("camelot_portal", "Camelot portal", 50, 270.0, "portal_glow", Use.PORTAL, 0, Landmark.CAMELOT.name(), 0x9070E0,
                "teak_plank", 3, "law_rune", 100, "air_rune", 500);
        f("ardougne_portal", "Ardougne portal", 50, 270.0, "portal_glow", Use.PORTAL, 0, Landmark.ARDOUGNE.name(), 0x9070E0,
                "teak_plank", 3, "law_rune", 200, "water_rune", 200);
        f("yanille_portal", "Yanille portal", 50, 270.0, "portal_glow", Use.PORTAL, 0, Landmark.YANILLE.name(), 0x9070E0,
                "teak_plank", 3, "law_rune", 200, "earth_rune", 200);
    }

    public static Furniture byBlock(String block) {
        for (Furniture f : FURNITURE) {
            if (f.block().equals(block)) {
                return f;
            }
        }
        return null;
    }

    public static Furniture byFlatpack(String item) {
        for (Furniture f : FURNITURE) {
            if (f.flatpack().equals(item)) {
                return f;
            }
        }
        return null;
    }

    /** Items a larder of the given tier hands out (one per search). */
    public static List<String> larderItems(int tier) {
        List<String> items = new ArrayList<>(List.of("cup_of_tea", "minecraft:egg", "pot_of_flour", "minecraft:milk_bucket"));
        if (tier >= 2) {
            items.addAll(List.of("potato", "garlic", "onion", "cheese"));
        }
        if (tier >= 3) {
            items.addAll(List.of("pat_of_butter", "chocolate_bar", "cabbage", "minecraft:cooked_beef"));
        }
        return items;
    }

    @Override
    public void items(ItemSink s) {
        // limestone_brick is in CommonItems (also used by the Mort'ton temple repairs in the minigames module).
        simple(s, "marble_block", "Marble block", "brick", 0xF0F0F0, 325000);
        simple(s, "gold_leaf", "Gold leaf", "leaf", 0xE8C030, 130000);
        simple(s, "magic_stone", "Magic stone", "brick", 0x9070E0, 975000);
        for (Furniture f : FURNITURE) {
            simple(s, f.flatpack(), f.name() + " (flatpack)", "package", f.color(), Math.max(10, (int) f.xp())).stack(16)
                    .examine("Use it on the ground to set up the " + f.name().toLowerCase(Locale.ROOT) + ".");
        }
    }

    @Override
    public void recipes(Consumer<Recipe> out) {
        Object[][] materials = {{"bolt_of_cloth", 650}, {"limestone_brick", 21}, {"marble_block", 325000}, {"gold_leaf", 130000},
                {"magic_stone", 975000}};
        for (Object[] m : materials) {
            int price = (Integer) m[1];
            out.accept(plain("buy_" + m[0], MenuType.CONSTRUCTION, title((String) m[0]) + " (" + String.format(Locale.ROOT, "%,d", price)
                    + " coins)").in("coins", price).out((String) m[0], 1).ticks(0).build());
        }
        for (Furniture f : FURNITURE) {
            Recipe.Builder b = r("flatpack_" + f.id(), MenuType.CONSTRUCTION, f.name() + " (flatpack)", Skill.CONSTRUCTION, f.level(), f.xp())
                    .out(f.flatpack(), 1).tool("hammer").ticks(4);
            f.materials().forEach(b::in);
            out.accept(b.build());
        }
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        for (Furniture f : FURNITURE) {
            // Furniture is breakable (it drops itself) so players can rearrange their house.
            list.add(new BlockDef(f.block(), f.name(), f.use() == Use.DECOR ? BlockDef.Kind.DECOR : BlockDef.Kind.SCRIPTED, null,
                    f.color(), f.look(), true));
        }
        return list;
    }
}
