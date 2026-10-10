package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryWldFig.dk;
import static com.runecraft.osrs.content.bestiary.Humanoid.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/** Combat townsfolk, warriors, wizards, goblins, giants, ogres, trolls and walking dead met all over Gielinor. */
final class BestiaryWldFolk {
    private BestiaryWldFolk() {
    }

    static BestiaryWldFig f(String id) {
        return BestiaryWldFig.of(id);
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        // ---- townsfolk ----
        l.add(f("man").hair(HAIR_BROWN, "short").top(0x8A6A3E, "tunic").trim(0x5A4426).bottoms(0x4E4A3A, "trousers")
                .boots(DARK_LEATHER, "boot").pouch().build());
        l.add(woman());
        l.add(f("farmer").hair(HAIR_BROWN, "short").beard(HAIR_BROWN, "short").top(0x6A8A3A, "shirt").sleeves("short")
                .bottoms(0x6A4A2A, "trousers").helm("straw", 0xD8B860).helmTrim(0x8A2A1A).weapon("pitchfork", IRON).build());
        l.add(f("monk").hair(HAIR_BROWN, "none").beard(HAIR_BROWN, "short").top(0x6A4A2A, "robe").sleeves("wide")
                .bottoms(0x6A4A2A, "robe").belt(0xC8B070).boots(LEATHER, "sandal").build());
        l.add(highwayman());
        l.add(f("mugger").hair(HAIR_BLACK, "short").beard(HAIR_BLACK, "goatee").top(0x4A4A3A, "rags").bottoms(0x3A3A2A, "trousers")
                .helm("bandana", 0x6A2A2A).weapon("dagger", IRON).pose(-10, 8, 30).build());
        l.add(pirate());
        l.add(f("desert_bandit").skin(SKIN_DESERT).hair(HAIR_BLACK, "short").beard(HAIR_BLACK, "short").top(0xE0D8C0, "tunic")
                .trim(0xA03020).bottoms(0x8A6A4A, "trousers").helm("bandana", 0xE0D8C0).belt(0x6A2A2A).pouch()
                .weapon("scimitar", IRON).pose(-6, 8, 26).build());
        l.add(chaosDruid());
        l.add(dwarf());
        l.add(gnome("gnome", false));
        l.add(gnome("gnome_guard", true));
        // ---- guards & warriors ----
        l.add(f("guard").hair(HAIR_BROWN, "short").top(IRON, "chain").sleeves("long", 0x6A6A6E).bottoms(STEEL, "plate")
                .boots(0x3A3A3E, "sabaton").helm("med", STEEL).belt(LEATHER).weapon("longsword", STEEL).pose(-6, 7, 22).build());
        l.add(f("fortis_guard").skin(0xA87850).hair(HAIR_BLACK, "short").top(0xC8A060, "plate").trim(0x8A2A1A)
                .bottoms(0x6A4A2A, "kilt").boots(LEATHER, "sandal").helm("med", 0xD8B040).helmTrim(0xA02A1A).cape(0x8A2A1A)
                .weapon("spear", STEEL).shield("round", 0x8A5A2A, 0xD0B040).build());
        l.add(alKharid());
        l.add(barbarian());
        l.add(knight("white_knight", 0xE4E4EA, 0xC8C8D0, GOLD, -1, 0xE8E8F0));
        l.add(knight("ardougne_knight", STEEL, 0x8A8A90, 0x2A3A8A, 0xC0C0C8, 0x2A3A8A));
        l.add(knight("paladin", 0xE0E0E8, 0xC0C0C8, GOLD, 0xF0F0F0, GOLD));
        l.add(f("hero").hair(HAIR_BLACK, "short").top(0x7A2A2A, "plate").trim(GOLD).bottoms(0x3A3A3E, "plate")
                .boots(0x2A2A2E, "sabaton").helm("med", 0x3A3A3E).helmTrim(GOLD).cape(0x8A2A2A).pauldrons(0x5A1E1E)
                .weapon("longsword", 0xC8C8D0).pose(-6, 8, 24).build());
        l.add(elf());
        // ---- wizards ----
        l.add(darkWizard());
        // ---- goblins ----
        l.add(goblin("goblin", GOBLIN_GREEN, 0x7A4A28, 0x5A3A20, "spear", BRONZE, false));
        l.add(hobgoblin());
        l.add(goblin("cave_goblin", 0xC8D0B8, 0x6A5A8A, 0x4A3A5A, "none", 0, false));
        l.add(goblin("cave_goblin_guard", 0xC0C8B0, 0x5A5A6A, 0x3A3A4A, "spear", IRON, true));
        // ---- giants ----
        l.add(hillGiant("hill_giant", 0xD8B088, false));
        l.add(hillGiant("catacomb_hill_giant", 0xC0A080, true));
        l.add(mossGiant("moss_giant", false));
        l.add(mossGiant("catacomb_moss_giant", true));
        l.add(iceGiant());
        l.add(fireGiant("fire_giant", false));
        l.add(fireGiant("catacomb_fire_giant", true));
        l.add(elemWarrior("ice_warrior", 0x9AC8E8, "ice", 0x7AA8D8, 0xD0F0FF, 0x60E0FF));
        l.add(elemWarrior("earth_warrior", 0x6A5A3A, "rock", 0x5A4A2A, 0x3A2A1A, 0x90E040));
        // ---- ogres, trolls ----
        l.add(ogre("ogre", 0x8AA060, 0x6A5A3A, "club", false));
        l.add(ogre("jogre", 0x8A7A40, 0x5A4A2A, "club", false));
        l.add(ogre("mogre", 0x5A8A7A, 0x3A4A3A, "none", false));
        l.add(ogre("ogress_warrior", 0x8A9A5A, 0x5A3A2A, "club", true));
        l.add(ogre("ogress_shaman", 0x8A9A5A, 0x3A5A3A, "staff", true));
        l.add(troll("mountain_troll", 0x8A8A6A, "rock", 0x4A4A3A));
        l.add(troll("ice_troll", 0xA8C8E0, "ice", 0x5A7A98));
        // ---- walking dead ----
        l.add(zombie());
        l.add(ghoul());
        return l;
    }

    // ------------------------------------------------------------------------------------------
    // Townsfolk
    // ------------------------------------------------------------------------------------------

    static ModelSpec woman() {
        return f("woman").torso(7, 6, 8, 5, 10).arms(3, 5, 5, 4).legs(12, 3).head(8, 8, 7).skin(SKIN_PALE)
                .hair(0x7A3A1A, "long").top(0xA04A6A, "shirt").trim(0xE8D8C0).bottoms(0x6A3A4A, "robe").belt(0x4A2A3A)
                .boots(0x3A2418, "shoe").foot(5).pose(0, 4, 18).build();
    }

    static ModelSpec highwayman() {
        ModelSpec.Builder b = f("highwayman").hair(HAIR_BLACK, "short").top(0x2A2A2E, "leather").sleeves("long", 0x3A3A40)
                .bottoms(0x2A2A2A, "trousers").cape(0x1A1A1E).helm("tricorn", 0x1E1E22).weapon("dagger", IRON)
                .pose(-8, 8, 30).builder();
        part(b, "mask", "head", Role.STATIC, 0x141416).at(0, -3.2f, -4).mat("cloth").box(-4.3f, -1, -0.6f, 9, 2, 1).add();
        return b.build();
    }

    static ModelSpec pirate() {
        ModelSpec.Builder b = f("pirate").hair(HAIR_BLACK, "long").beard(HAIR_BLACK, "short").top(0xE0D8C0, "shirt")
                .sleeves("long", 0xE8E0CC).trim(0xA02A2A).bottoms(0x3A3A4A, "trousers").helm("bandana", 0xA02A2A)
                .belt(0x1A1A1A).pouch().boots(0x1E1A16, "boot").weapon("scimitar", STEEL).pose(-6, 8, 26).builder();
        // red-striped shirt bands, sash and eyepatch
        part(b, "stripe", "torso", Role.STATIC, 0xA02A2A).at(0, -8, 0).mat("cloth")
                .box(-5, 0, -3, 10, 1, 6, 0.15f).box(-5, 3, -3, 10, 1, 6, 0.15f).box(-4, 6, -2.5f, 8, 1, 5, 0.15f).add();
        part(b, "sash", "torso", Role.STATIC, 0xC03030).at(-3, 0, -2.5f).rot(0, 0, 15).mat("cloth").box(-1, -1, -0.8f, 2, 5, 1).add();
        part(b, "eyepatch", "head", Role.STATIC, 0x101010).at(-2, -4.5f, -4).mat("leather")
                .box(-1, 0, -0.6f, 2, 2, 1).box(-1.5f, -1.5f, -0.4f, 7, 1, 1, -0.2f).add();
        return b.build();
    }

    static ModelSpec chaosDruid() {
        ModelSpec.Builder b = f("chaos_druid").skin(0xC89C78).hair(HAIR_GREY, "none").beard(HAIR_GREY, "long")
                .top(0x2E3A22, "robe").trim(0x6A1A1A).sleeves("wide").bottoms(0x26301C, "robe").belt(0x4A3A1E)
                .helm("hood", 0x22301A).boots(DARK_LEATHER, "shoe").weapon("staff", 0x6A2A8A).weaponGlow(0xB060F0)
                .pose(-4, 6, 24).builder();
        part(b, "amulet", "torso", Role.STATIC, 0xC02020).at(0, -8, -3.2f).mat("energy").glow("full").box(-0.5f, 0, -0.6f, 1, 1, 1, 0.1f).add();
        return b.build();
    }

    static ModelSpec dwarf() {
        ModelSpec.Builder b = f("dwarf").legs(7, 4).torso(9, 9, 11, 7, 9).arms(4, 4, 4, 5).head(8, 8, 8).belly(1)
                .skin(0xD09878).hair(HAIR_GINGER, "short").beard(HAIR_GINGER, "forked").nose("big").brow()
                .top(0x7A3A1A, "tunic").trim(0x4A2410).bottoms(0x4A3A2A, "trousers").helm("med", IRON).helmTrim(BRONZE)
                .belt(LEATHER).pouch().boots(0x3A2414, "boot").foot(6).weapon("pickaxe", IRON).pose(-8, 6, 30).builder();
        part(b, "braid", "beard", Role.STATIC, dk(HAIR_GINGER, 0.9f)).at(0, 6, -0.5f).mat("hair")
                .box(-0.5f, 0, -0.5f, 1, 3, 1, 0.2f).add();
        return b.build();
    }

    static ModelSpec gnome(String id, boolean guard) {
        BestiaryWldFig g = f(id).legs(7, 3).torso(6, 6, 7, 4, 7).arms(3, 3, 4, 3).head(8, 8, 7).foot(5)
                .skin(0xC89A72).hair(HAIR_BROWN, "short").nose("long").ears("pointy").pose(-4, 6, 20);
        if (guard) {
            g.top(0x8A8A9A, "chain").bottoms(0x6A4A2A, "trousers").helm("med", 0x7A7A8A).weapon("spear", IRON).boots(LEATHER, "boot");
        } else {
            g.top(0x3A8A3A, "tunic").trim(0x2A5A2A).bottoms(0x6A4A2A, "trousers").helm("cap", 0x9A2A2A).helmTrim(0xE0C040)
                    .boots(0x5A3A1E, "shoe");
        }
        return g.build();
    }

    // ------------------------------------------------------------------------------------------
    // Guards, warriors, knights
    // ------------------------------------------------------------------------------------------

    static ModelSpec alKharid() {
        ModelSpec.Builder b = f("al_kharid_warrior").skin(SKIN_DESERT).hair(HAIR_BLACK, "short").beard(HAIR_BLACK, "moustache")
                .top(0xD8CC9A, "tunic").trim(0xA02020).sleeves("short").bottoms(0x8A3A2A, "kilt").belt(0xA02020)
                .boots(LEATHER, "sandal").helm("bandana", 0xE8E0C8).weapon("scimitar", IRON).pose(-6, 8, 28).builder();
        part(b, "sash_x", "torso", Role.STATIC, 0xA02020).at(0, -9, -3.1f).rot(0, 0, -35).mat("cloth").box(-0.75f, 0, -0.6f, 2, 9, 1).add();
        return b.build();
    }

    static ModelSpec barbarian() {
        ModelSpec.Builder b = f("barbarian").torso(8, 8, 11, 6, 10).arms(4, 5, 5, 5).skin(0xD8A07A).hair(HAIR_BLOND, "long")
                .beard(HAIR_BLOND, "long").brow().top(0x8A6A3A, "fur").bottoms(0x7A5A3A, "kilt").trim(0x4A3A22).belt(LEATHER)
                .boots(0x6A4A2A, "boot").helm("horned", IRON).weapon("battleaxe", IRON).pose(-6, 9, 26).builder();
        sym(b, "armband", "arm", true, 0x8A6A3A).at(0, 3, 0).mat("fur").box(-2, 0, -2, 4, 1, 4, 0.4f).add();
        return b.build();
    }

    static ModelSpec knight(String id, int plate, int legs, int trim, int plume, int shieldTrim) {
        BestiaryWldFig k = f(id).torso(8, 8, 10, 6, 10).hair(HAIR_BROWN, "short").top(plate, "plate").trim(trim)
                .bottoms(legs, "plate").boots(dk(legs, 0.85f), "sabaton").helm("full", plate).shield("kite", plate, shieldTrim)
                .weapon("longsword", 0xE0E0E8).pose(-4, 8, 22);
        if (plume >= 0) {
            k.helmTrim(plume);
        }
        if (id.equals("ardougne_knight")) {
            k.cape(0x2A3A8A);
        }
        return k.build();
    }

    static ModelSpec elf() {
        ModelSpec.Builder b = f("elf_warrior").torso(7, 7, 9, 5, 10).arms(3, 5, 5, 4).legs(13, 4).skin(0xE8D0B0)
                .hair(0x2A2A2A, "long").ears("pointy").top(0x3A6A3A, "tunic").trim(0xC0B060).bottoms(0x2A4A2A, "trousers")
                .cape(0x2A4A2A).boots(0x3A2A1A, "boot").weapon("bow", 0xE8E0B0).pose(-14, 6, 20).builder();
        part(b, "quiver", "torso", Role.STATIC, LEATHER).at(2, -9, 3).rot(0, 0, -25).mat("leather").box(-1.5f, 0, 0, 3, 8, 2).add();
        part(b, "arrows", "quiver", Role.STATIC, 0xE0E0E0).at(0, -2, 1).mat("feathers").box(-1, 0, -0.5f, 2, 2, 1).add();
        return b.build();
    }

    static ModelSpec darkWizard() {
        ModelSpec.Builder b = f("dark_wizard").skin(SKIN_PALE).hair(HAIR_GREY, "long").beard(HAIR_GREY, "long")
                .top(0x26263E, "robe").trim(0x8A1A1A).sleeves("wide").bottoms(0x1E1E32, "robe").belt(0x8A1A1A)
                .helm("wizard", 0x1E1E30).helmTrim(0x8A1A1A).boots(0x1A1A24, "shoe").weapon("staff", 0x8A2A1A).weaponGlow(0xFF5020)
                .pose(-6, 7, 26).builder();
        part(b, "rune_sigil", "torso", Role.STATIC, 0xC02020).at(0, -7, -3.2f).mat("energy").glow("full")
                .box(-1, 0, -0.6f, 2, 2, 1, -0.2f).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Goblins
    // ------------------------------------------------------------------------------------------

    static ModelSpec goblin(String id, int skin, int top, int bottom, String weapon, int wc, boolean guard) {
        BestiaryWldFig g = f(id).legs(8, 3).torso(7, 6, 8, 5, 7).arms(3, 4, 4, 4).head(8, 8, 7).foot(5).hunch(10)
                .skin(skin).face("eyes").eyeColour(guard ? 0xE0E040 : 0xF0D020).hair(0, "none").nose("hooked").ears("goblin")
                .jaw("grin").brow().top(top, guard ? "chain" : "vest").sleeves("none").bottoms(bottom, "loincloth")
                .boots(skin, "bare").belt(0x3A2A1A).pose(-6, 10, 22);
        if (!weapon.equals("none")) {
            g.weapon(weapon, wc);
        }
        if (guard) {
            g.helm("med", 0x5A5A6A);
        } else if (id.equals("cave_goblin")) {
            g.helm("skullcap", 0x4A3A5A).weapon("pickaxe", IRON);
        }
        ModelSpec.Builder b = g.builder();
        part(b, "cranium", "head", Role.STATIC, skin).at(0, -4, 3).rot(-12, 0, 0).mat("skin").box(-3.5f, -4, -1, 7, 5, 4).add();
        part(b, "necklace", "torso", Role.STATIC, 0xE8E0C8).at(0, -7, -2.8f).mat("bone")
                .box(-2.5f, 0, -0.5f, 1, 1, 1).box(-0.5f, 0.5f, -0.6f, 1, 2, 1).box(1.5f, 0, -0.5f, 1, 1, 1).add();
        if (id.equals("goblin")) {
            part(b, "shoulder_pad", "torso", Role.STATIC, 0x5A3A1E).at(-4, -7, 0).rot(0, 0, 20).mat("leather").box(-2, -1, -2.5f, 4, 2, 5).add();
        }
        return b.build();
    }

    static ModelSpec hobgoblin() {
        ModelSpec.Builder b = f("hobgoblin").legs(11, 4).torso(9, 8, 11, 6, 10).arms(4, 5, 5, 5).head(8, 8, 8).hunch(12)
                .skin(0x7A9A4A).face("eyes").eyeColour(0xF0A020).hair(0x2A2A1A, "mohawk").nose("hooked").ears("goblin")
                .jaw("tusks").brow().top(0x6A3A2A, "leather").sleeves("none").bottoms(0x4A3A2A, "kilt").trim(0x2A1A10)
                .boots(0x7A9A4A, "bare").belt(0x3A2214).pauldrons(0x5A5A5E).weapon("spear", IRON).pose(-6, 9, 22).builder();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Giants
    // ------------------------------------------------------------------------------------------

    static BestiaryWldFig giant(String id, int skin) {
        return f(id).legs(12, 6).torso(11, 11, 15, 9, 13).arms(6, 6, 7, 7).head(9, 9, 9).foot(8).belly(2).hunch(10)
                .stance(4).skin(skin).face("eyes").brow().nose("broad").jaw("heavy").ears("round").top(skin, "bare")
                .boots(skin, "bare").belt(-1).pose(-4, 8, 22).weaponLow().weaponPose(28, -14).shadow(1.0f);
    }

    static ModelSpec hillGiant(String id, int skin, boolean catacomb) {
        BestiaryWldFig g = giant(id, skin).face(catacomb ? "eyes" : "face").eyeColour(0xFF4030).hair(HAIR_BROWN, "short")
                .bottoms(0x6A4A2A, "loincloth").weapon("greatclub", WOOD);
        if (catacomb) {
            g.glowEyes(0xFF4030);
        }
        ModelSpec.Builder b = g.builder();
        part(b, "strap", "torso", Role.STATIC, 0x5A3A1E).at(0, -12, -4.6f).rot(0, 0, 32).mat("leather").box(-1, -1, -0.6f, 2, 15, 1).add();
        return b.build();
    }

    static ModelSpec mossGiant(String id, boolean catacomb) {
        int skin = catacomb ? 0x5A7A3A : 0x6A7A3A;
        int moss = catacomb ? 0x2A5A16 : 0x3A6A1E;
        BestiaryWldFig g = giant(id, skin).skin(skin, "skin", -1).eyeColour(catacomb ? 0xFF4030 : 0xE0E040).hair(0, "none")
                .bottoms(0x4A3A1A, "loincloth").weapon("greatclub", WOOD);
        if (catacomb) {
            g.glowEyes(0xFF4030);
        }
        ModelSpec.Builder b = g.builder();
        part(b, "moss_cap", "head", Role.STATIC, moss).at(0, -9, 0).mat("moss").accent(moss)
                .box(-5, -1.5f, -5, 10, 2, 10).box(-3, -3, -2, 6, 2, 6).add();
        part(b, "moss_beard", "head", Role.STATIC, dk(moss, 1.1f)).at(0, -2, -4.5f).rot(10, 0, 0).mat("moss").accent(moss)
                .box(-3.5f, 0, -1, 7, 5, 2).box(-2, 5, -0.5f, 4, 2, 2).add();
        sym(b, "moss_shoulder", "arm", true, moss).at(0, -3, 0).mat("moss").accent(moss).box(-3.5f, -1, -3.5f, 7, 3, 7).add();
        part(b, "moss_back", "torso", Role.STATIC, moss).at(0, -12, 4).rot(8, 0, 0).mat("moss").accent(moss)
                .box(-6, 0, 0, 12, 7, 2).box(-4, 7, -0.5f, 8, 4, 2).add();
        for (int i = 0; i < 3; i++) {
            part(b, "fern" + i, "moss_back", Role.STATIC, 0x5A8A2A).at(-4 + i * 4, 0, 1).rot(-30, 0, -20 + i * 20).mat("moss")
                    .accent(0x4A8A1E).box(-1, -4, -0.5f, 2, 4, 1).add();
        }
        return b.build();
    }

    static ModelSpec iceGiant() {
        ModelSpec.Builder b = giant("ice_giant", 0x9ABCD8).skin(0x9ABCD8, "ice", -1).eyeColour(0x60E0FF).glowEyes(0x60E0FF)
                .hair(HAIR_WHITE, "long").beard(HAIR_WHITE, "forked").bottoms(0x5A6A8A, "loincloth").helm("horned", 0x6A7A8A)
                .weapon("greatclub", 0x8AA8C8).builder();
        for (int i = 0; i < 3; i++) {
            part(b, "icicle" + i, "torso", Role.STATIC, 0xD8F0FF).at(-4 + i * 4, -13, 3).rot(-20 - i * 5, 0, -15 + i * 15).mat("ice")
                    .box(-1, -4, -1, 2, 4, 2).add();
        }
        sym(b, "frost_pad", "arm", true, 0xD0E8F8).at(0, -3, 0).mat("ice").box(-3.5f, -1, -3.5f, 7, 2, 7).add();
        return b.build();
    }

    static ModelSpec fireGiant(String id, boolean catacomb) {
        ModelSpec.Builder b = giant(id, 0xD8743C).skin(0xD8743C, "skin", -1).eyeColour(0xFFE040).glowEyes(0xFFE040).hair(0, "none")
                .bottoms(0x5A2A1A, "loincloth").weapon("longsword", 0x5A5A5A).builder();
        part(b, "flames", "head", Role.STATIC, 0xFFA020).at(0, -9, 1).mat("lava").glow("full")
                .box(-4.5f, -3, -3.5f, 9, 3, 8).box(-3, -6, -2, 6, 3, 6).box(-1.5f, -8, -0.5f, 3, 2, 3).add();
        part(b, "flame_beard", "head", Role.STATIC, 0xFF7A10).at(0, -2, -4.5f).rot(8, 0, 0).mat("lava").glow("full")
                .box(-3.5f, 0, -1, 7, 4, 2).box(-2, 4, -0.5f, 4, 2, 2).add();
        sym(b, "flame_shoulder", "arm", true, 0xFF9020).at(0, -3, 0).mat("lava").glow("full").box(-3, -2, -3, 6, 2, 6).add();
        return b.build();
    }

    static ModelSpec elemWarrior(String id, int skin, String mat, int armour, int weapon, int eye) {
        ModelSpec.Builder b = f(id).torso(9, 8, 11, 6, 10).arms(4, 5, 5, 5).skin(skin, mat, -1).face("eyes").glowEyes(eye)
                .hair(0, "none").top(armour, "plate").trim(dk(armour, 1.3f)).bottoms(dk(armour, 0.85f), "plate")
                .boots(dk(armour, 0.7f), "sabaton").helm("full", dk(armour, 1.1f)).weapon("mace", weapon).pose(-6, 8, 24).builder();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Ogres & trolls
    // ------------------------------------------------------------------------------------------

    static ModelSpec ogre(String id, int skin, int cloth, String weapon, boolean female) {
        BestiaryWldFig o = f(id).legs(11, 6).torso(12, 12, 14, 9, 13).arms(5, 6, 7, 6).head(7, 7, 7).foot(7).belly(4).hunch(16)
                .stance(5).skin(skin).face("eyes").eyeColour(0xF0D040).hair(0, "none").nose("broad").ears("small").jaw("underbite")
                .brow().top(skin, "bare").bottoms(cloth, female ? "kilt" : "loincloth").boots(skin, "bare").belt(-1)
                .pose(-4, 10, 20).shadow(0.9f);
        if (female) {
            o.top(cloth, id.endsWith("shaman") ? "robe" : "leather").hair(HAIR_BLACK, id.endsWith("shaman") ? "long" : "ponytail");
        }
        if (!weapon.equals("none")) {
            o.weapon(weapon, weapon.equals("staff") ? 0x6A8A2A : WOOD);
            if (weapon.equals("staff")) {
                o.weaponGlow(0xA0E060);
            }
        }
        ModelSpec.Builder b = o.builder();
        part(b, "bone_necklace", "torso", Role.STATIC, 0xE8E0C8).at(0, -13, -4.6f).mat("bone")
                .box(-3.5f, 0, -0.5f, 1, 1, 1).box(-1.5f, 1, -0.5f, 1, 2, 1).box(0.5f, 1, -0.5f, 1, 2, 1).box(2.5f, 0, -0.5f, 1, 1, 1).add();
        if (id.equals("mogre")) {
            sym(b, "fin", "head", false, dk(skin, 0.8f)).at(3.5f, -4, 0).rot(0, -10, -30).mat("membrane").box(0, -2, -1.5f, 3, 4, 3).add();
        }
        return b.build();
    }

    static ModelSpec troll(String id, int skin, String mat, int cloth) {
        ModelSpec.Builder b = f(id).legs(10, 6).torso(11, 11, 15, 9, 13).arms(5, 8, 8, 7).head(8, 7, 8).foot(7).hunch(26)
                .stance(6).skin(skin, mat.equals("rock") ? "hide" : mat, -1).face("eyes").eyeColour(0xF06020).hair(0, "none").nose("long").ears("small")
                .jaw("underbite").brow().top(skin, "bare").bottoms(cloth, "loincloth").boots(skin, "bare").belt(-1)
                .pose(-10, 12, 18).shadow(1.0f).builder();
        // spiky back ridge and knuckle-dragging stone fists
        for (int i = 0; i < 3; i++) {
            part(b, "ridge" + i, "torso", Role.STATIC, dk(skin, 0.8f)).at(0, -13 + i * 3.5f, 4).rot(-35, 0, 0).mat(mat)
                    .box(-1, -3, -1, 2, 3, 2).add();
        }
        sym(b, "knuckle", "fingers", true, dk(skin, 0.75f)).at(0, 1.5f, -1).mat(mat).box(-3, -0.5f, -0.5f, 6, 1, 1, 0.1f).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------
    // Undead
    // ------------------------------------------------------------------------------------------

    static ModelSpec zombie() {
        ModelSpec.Builder b = f("zombie").skin(0x7A9A62).face("eyes").eyeColour(0xE0E0A0).hair(0x3A3A2A, "short")
                .top(0x5A4A3A, "rags").sleeves("short").bottoms(0x4A3A2A, "shorts").boots(0x7A9A62, "bare").belt(-1)
                .pose(-78, 4, 8).hunch(8).builder();
        part(b, "wound", "torso", Role.STATIC, 0x7A2A2A).at(1.5f, -7, -3.2f).mat("flesh").box(-1, 0, -0.6f, 2, 3, 1, -0.1f).add();
        part(b, "rib_show", "torso", Role.STATIC, 0xD8D0B0).at(-2, -6, -3.3f).mat("bone").box(-1, 0, -0.5f, 2, 1, 1).box(-1, 1.5f, -0.5f, 2, 1, 1).add();
        return b.build();
    }

    static ModelSpec ghoul() {
        ModelSpec.Builder b = f("ghoul").torso(7, 6, 9, 5, 10).arms(3, 6, 6, 4).legs(12, 3).skin(0x8A9A8A).face("eyes")
                .eyeColour(0xF0F060).hair(0x5A5A50, "wild").nose("hooked").ears("pointy").jaw("fangs").top(0x8A9A8A, "bare")
                .bottoms(0x4A4A3A, "rags").boots(0x8A9A8A, "bare").belt(-1).hunch(30).pose(-40, 10, 24).builder();
        sym(b, "claw", "fingers", true, 0x2A2A20).at(0, 1.5f, -1).rot(30, 0, 0).mat("horn")
                .box(-2, 0, -0.5f, 1, 2, 1).box(-0.5f, 0, -0.5f, 1, 2, 1).box(1, 0, -0.5f, 1, 2, 1).add();
        return b.build();
    }
}
