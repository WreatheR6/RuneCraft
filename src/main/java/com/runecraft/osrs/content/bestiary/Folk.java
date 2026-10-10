package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Humanoid.*;

/** Humanoid monsters met all over Gielinor, dressed as in OSRS. */
final class Folk {
    private Folk() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        // ---- townsfolk ----
        l.add(of("man").skin(SKIN).hair(HAIR_BROWN, "short").top(0x8A6A3E, "cloth").bottoms(0x4E4A3A, "trousers")
                .boots(DARK_LEATHER).belt(LEATHER).build());
        l.add(of("woman").femaleBody().skin(SKIN_PALE).hair(0x7A3A1A, "long").top(0xA04A6A, "cloth")
                .bottoms(0x6A3A4A, "robe").sleeves("long").belt(0x4A2A3A).build());
        l.add(of("farmer").skin(SKIN).hair(HAIR_BROWN, "short").top(0x6A8A3A, "cloth").sleeves("short")
                .bottoms(0x6A4A2A, "trousers").helm("cap", 0xC0A060).weapon("pitchfork", IRON).build());
        l.add(of("monk").skin(SKIN).hair(HAIR_BROWN, "none").top(0x6A4A2A, "robe").bottoms(0x6A4A2A, "robe")
                .belt(0xC0A060).beard(HAIR_BROWN, "short").build());
        l.add(of("highwayman").skin(SKIN).hair(HAIR_BLACK, "short").top(0x2A2A2A, "leather").bottoms(0x2A2A2A, "trousers")
                .cape(0x1A1A1A).helm("mask", 0x1A1A1A).weapon("dagger", IRON).build());
        l.add(of("mugger").skin(SKIN).hair(HAIR_BLACK, "short").top(0x4A4A3A, "cloth").bottoms(0x3A3A2A, "trousers")
                .helm("bandana", 0x6A2A2A).weapon("dagger", IRON).build());
        l.add(of("pirate").skin(SKIN).hair(HAIR_BLACK, "long").beard(HAIR_BLACK, "short").top(0xE0D8C0, "cloth")
                .bottoms(0x3A3A4A, "trousers").helm("bandana", 0xA02A2A).belt(0x1A1A1A).weapon("scimitar", STEEL).build());
        l.add(of("desert_bandit").skin(SKIN_DESERT).hair(HAIR_BLACK, "short").top(0xE0D8C0, "cloth")
                .bottoms(0x8A6A4A, "trousers").helm("bandana", 0xE0D8C0).belt(0x6A2A2A).weapon("scimitar", IRON).build());
        l.add(of("chaos_druid").skin(SKIN).hair(HAIR_GREY, "long").beard(HAIR_GREY, "long").top(0x3A2A4A, "robe")
                .bottoms(0x2A1A3A, "robe").helm("hood", 0x2A1A3A).weapon("staff", 0x8A3AC0).build());
        l.add(of("dwarf").dwarfBody().skin(0xC08A68).hair(HAIR_GINGER, "short").beard(HAIR_GINGER, "long")
                .top(0x6A3A1A, "cloth").bottoms(0x4A3A2A, "trousers").helm("med", IRON).belt(LEATHER)
                .weapon("pickaxe", IRON).build());
        l.add(of("gnome").gnomeBody().skin(0xC09070).hair(HAIR_BROWN, "short").top(0x3A8A3A, "cloth")
                .bottoms(0x6A4A2A, "trousers").helm("cap", 0x9A2A2A).build());
        l.add(of("gnome_guard").gnomeBody().skin(0xC09070).hair(HAIR_BROWN, "short").top(0x8A8A9A, "chain")
                .bottoms(0x6A4A2A, "trousers").helm("med", 0x6A6A7A).weapon("spear", IRON).build());
        // ---- guards and warriors ----
        l.add(of("guard").skin(SKIN).hair(HAIR_BROWN, "short").top(IRON, "chain").sleeves("long", 0x5A5A5E)
                .bottoms(0x5A4A3A, "trousers").helm("med", STEEL).belt(LEATHER).weapon("longsword", STEEL).build());
        l.add(of("fortis_guard").skin(0xA87850).hair(HAIR_BLACK, "short").top(0xC0A060, "metal")
                .bottoms(0x6A4A2A, "trousers").helm("med", 0xD0B040).weapon("spear", STEEL).shield("round", 0x8A5A2A, 0xD0B040).build());
        l.add(of("al_kharid_warrior").skin(SKIN_DESERT).hair(HAIR_BLACK, "short").top(0xD8CC9A, "cloth").sleeves("short")
                .bottoms(0x8A3A2A, "trousers").belt(0xA02020).helm("bandana", 0xE8E0C8).weapon("scimitar", IRON).build());
        l.add(of("barbarian").skin(SKIN).hair(HAIR_BLOND, "long").beard(HAIR_BLOND, "long").top(0x8A6A3A, "fur")
                .sleeves("none").bottoms(0x7A5A3A, "kilt").belt(LEATHER).helm("horned", IRON).weapon("battleaxe", IRON).build());
        l.add(of("white_knight").skin(SKIN).hair(HAIR_BROWN, "short").top(0xE0E0E8, "metal").bottoms(0xC8C8D0, "plate")
                .helm("full", 0xE8E8F0).shield("kite", 0xE8E8F0, GOLD).weapon("longsword", 0xE8E8F0).build());
        l.add(of("ardougne_knight").skin(SKIN).hair(HAIR_BROWN, "short").top(STEEL, "metal").bottoms(STEEL, "plate")
                .helm("full", STEEL).cape(0xC0C0C8).shield("kite", 0xC0C0C8, 0x2A3A8A).weapon("longsword", STEEL).build());
        l.add(of("paladin").skin(SKIN).hair(HAIR_BLOND, "short").top(0xE0E0E8, "metal").bottoms(0xC0C0C8, "plate")
                .helm("full", 0xE0E0E8).trim(GOLD).shield("kite", 0xE0E0E8, GOLD).weapon("longsword", 0xE0E0E8).build());
        l.add(of("hero").skin(SKIN).hair(HAIR_BLACK, "short").top(0x7A2A2A, "metal").bottoms(0x3A3A3A, "plate")
                .helm("med", 0x3A3A3A).cape(0x8A2A2A).trim(GOLD).weapon("longsword", 0xC0C0C8).build());
        l.add(of("void_knight").skin(SKIN).hair(HAIR_BROWN, "short").top(0x3A3A3A, "cloth").trim(0xE0E0E0)
                .bottoms(0x3A3A3A, "robe").helm("full", 0xE0E0E0).weapon("mace", 0xE0E0E0).build());
        l.add(of("elf_warrior").skin(0xE0C8A8).hair(0x2A2A2A, "long").ears("pointy").top(0x3A6A3A, "cloth")
                .bottoms(0x2A4A2A, "trousers").cape(0x2A4A2A).weapon("longbow", 0xE8E0B0).build());
        // ---- wizards ----
        l.add(of("dark_wizard").skin(SKIN_PALE).hair(HAIR_GREY, "short").beard(HAIR_GREY, "long")
                .top(0x26263E, "robe").bottoms(0x1E1E32, "robe").trim(0x8A1A1A).helm("wizard", 0x1E1E30)
                .weapon("staff", 0xE04020).build());
        l.add(of("infernal_mage").skin(0x8A6A5A).hair(HAIR_BLACK, "none").top(0x6A1A1A, "robe").bottoms(0x3A0A0A, "robe")
                .helm("hood", 0x2A0A0A).glowingEyes().face("eyes").weapon("staff", 0xFF6020).build());
        l.add(of("malevolent_mage").skin(0x8A6A5A).hair(HAIR_BLACK, "none").top(0x6A1A1A, "robe").bottoms(0x3A0A0A, "robe")
                .trim(0xE0A020).helm("hood", 0x2A0A0A).glowingEyes().face("eyes").weapon("staff", 0xFF3020).build());
        // ---- goblins ----
        l.add(of("goblin").goblinBody().skin(GOBLIN_GREEN).face("eyes").hair(0, "none").top(0x7A4A28, "leather")
                .sleeves("none").bottoms(0x5A3A20, "loincloth").boots(GOBLIN_GREEN).weapon("spear", BRONZE).build());
        l.add(of("hobgoblin").body(11, 9, 11, 5, 12, 3).head(8, 8, 8).ears("pointy").bigNose(true).hunch(10)
                .skin(0x7A9A4A).face("eyes").hair(0, "none").top(0x6A3A2A, "leather").sleeves("none")
                .bottoms(0x4A3A2A, "kilt").boots(0x7A9A4A).weapon("spear", IRON).build());
        l.add(of("cave_goblin").goblinBody().skin(0xC8D8C0).face("eyes").hair(0, "none").top(0x6A5A8A, "cloth")
                .sleeves("short").bottoms(0x4A3A5A, "loincloth").boots(0xC8D8C0).helm("cap", 0x4A3A5A).build());
        l.add(of("cave_goblin_guard").goblinBody().skin(0xC8D8C0).face("eyes").hair(0, "none").top(0x5A5A6A, "chain")
                .bottoms(0x3A3A4A, "kilt").helm("med", 0x5A5A6A).weapon("spear", IRON).build());
        // ---- giants ----
        l.add(of("hill_giant").giantBody().skin(0xC8A070).hair(HAIR_BROWN, "none").top(0xC8A070, "skin").sleeves("none")
                .bottoms(0x6A4A2A, "loincloth").boots(0xC8A070).weapon("club", WOOD).build());
        l.add(of("catacomb_hill_giant").giantBody().skin(0xB89068).hair(HAIR_BROWN, "none").top(0xB89068, "skin").sleeves("none")
                .bottoms(0x4A3A2A, "loincloth").boots(0xB89068).weapon("club", WOOD).glowingEyes().build());
        l.add(moss(of("moss_giant"), 0x6A7A3A, 0x3A6A1E));
        l.add(moss(of("catacomb_moss_giant"), 0x5A7A3A, 0x2A5A16));
        l.add(of("ice_giant").giantBody().skin(0x9ABCD8).skinMaterial("ice").hair(HAIR_WHITE, "long").beard(HAIR_WHITE, "long")
                .top(0x9ABCD8, "skin").sleeves("none").bottoms(0x5A6A8A, "loincloth").boots(0x9ABCD8)
                .helm("horned", 0x6A7A8A).weapon("club", 0x8AA8C8).build());
        l.add(fire(of("fire_giant")));
        l.add(fire(of("catacomb_fire_giant")));
        l.add(of("ice_warrior").skin(0x9AC8E8).skinMaterial("ice").face("eyes").glowingEyes().hair(0, "none").top(0x7AA8D8, "metal")
                .bottoms(0x5A88B8, "plate").helm("full", 0xB0D8F0).weapon("mace", 0xD0F0FF).build());
        l.add(of("earth_warrior").skin(0x6A5A3A).face("eyes").hair(0, "none").top(0x5A4A2A, "metal").bottoms(0x4A3A2A, "plate")
                .helm("full", 0x5A4A2A).weapon("mace", 0x3A2A1A).build());
        // ---- ogres, trolls ----
        l.add(of("ogre").ogreBody().skin(0x8AA060).face("eyes").hair(0, "none").top(0x8AA060, "skin").sleeves("none")
                .bottoms(0x6A5A3A, "loincloth").boots(0x8AA060).weapon("club", WOOD).build());
        l.add(of("mogre").ogreBody().skin(0x5A8A7A).skinMaterial("scales").face("eyes").hair(0, "none").top(0x5A8A7A, "skin")
                .sleeves("none").bottoms(0x3A4A3A, "loincloth").boots(0x5A8A7A).build());
        l.add(of("jogre").ogreBody().skin(0x7A6A3A).face("eyes").hair(0, "none").top(0x7A6A3A, "skin").sleeves("none")
                .bottoms(0x5A4A2A, "loincloth").boots(0x7A6A3A).weapon("club", WOOD).build());
        l.add(of("ogress_warrior").ogreBody().skin(0x8A9A5A).face("eyes").hair(HAIR_BLACK, "ponytail").top(0x5A3A2A, "leather")
                .bottoms(0x5A3A2A, "kilt").weapon("club", WOOD).build());
        l.add(of("ogress_shaman").ogreBody().skin(0x8A9A5A).face("eyes").hair(HAIR_BLACK, "long").top(0x3A5A3A, "robe")
                .bottoms(0x3A5A3A, "robe").weapon("staff", 0xA0E060).build());
        l.add(of("mountain_troll").trollBody().skin(0x8A8A6A).skinMaterial("rock").face("eyes").hair(0, "none")
                .top(0x8A8A6A, "skin").sleeves("none").bottoms(0x4A4A3A, "loincloth").boots(0x8A8A6A).build());
        l.add(of("ice_troll").trollBody().skin(0xA0C0D8).skinMaterial("ice").face("eyes").hair(0, "none")
                .top(0xA0C0D8, "skin").sleeves("none").bottoms(0x5A7A98, "loincloth").boots(0xA0C0D8).build());
        // ---- undead humanoids ----
        l.add(of("zombie").skin(0x7A9A62).face("eyes").hair(0x3A3A2A, "short").top(0x5A4A3A, "cloth").sleeves("short")
                .bottoms(0x4A3A2A, "shorts").boots(0x7A9A62).arms(-78).hunch(8).build());
        l.add(of("ghoul").body(12, 8, 10, 4, 13, 3).skin(0x8A9A8A).face("eyes").hair(0, "none").top(0x8A9A8A, "skin")
                .sleeves("none").bottoms(0x4A4A3A, "loincloth").boots(0x8A9A8A).hunch(28).arms(-30).build());
        l.add(of("nightmare_husk").skin(0x6A6A5A).face("eyes").glowingEyes().hair(0, "none").top(0x4A4A3A, "cloth")
                .bottoms(0x3A3A2A, "shorts").boots(0x6A6A5A).arms(-60).hunch(15).build());
        return l;
    }

    private static ModelSpec moss(Humanoid h, int skin, int moss) {
        ModelSpec.Builder b = h.giantBody().skin(skin).skinMaterial("moss").hair(moss, "none").top(skin, "skin").sleeves("none")
                .bottoms(0x4A3A1A, "loincloth").boots(skin).weapon("club", WOOD).builder();
        Kit.part(b, "moss_cap", "head", ModelSpec.Role.STATIC, moss).at(0, -8, 0).mat("moss").box(-4.5f, -1, -4.5f, 9, 2, 9).add();
        Kit.sym(b, "moss_shoulder", "torso", false, moss).at(4, -10, 0).mat("moss").box(-1, -1, -3, 4, 2, 6).add();
        return b.build();
    }

    private static ModelSpec fire(Humanoid h) {
        ModelSpec.Builder b = h.giantBody().skin(0xD07040).hair(0, "none").top(0xD07040, "skin").sleeves("none").face("eyes")
                .glowingEyes().bottoms(0x5A2A1A, "loincloth").boots(0xD07040).weapon("longsword", 0x5A5A5A).builder();
        Kit.part(b, "flames", "head", ModelSpec.Role.STATIC, 0xFFA020).at(0, -8, 1).mat("lava").glow("full")
                .box(-4, -3, -3, 8, 3, 7).box(-2, -5, -1, 4, 2, 4).add();
        Kit.part(b, "flame_beard", "head", ModelSpec.Role.STATIC, 0xFF7A10).at(0, -2, -4).mat("lava").glow("full")
                .box(-3, 0, -1, 6, 4, 2).add();
        return b.build();
    }
}
