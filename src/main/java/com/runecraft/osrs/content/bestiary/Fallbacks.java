package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.runecraft.osrs.content.bestiary.Humanoid.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Models for every module monster and boss that still has none: hand-picked forms for the ones that are not
 * people (implings, golems, crabs, beasts, TzHaar...), then an outfit chosen from the creature's name and its
 * skin colours (skin, top, bottoms, accent) for everyone else, scaled to the creature's height.
 */
final class Fallbacks {
    private Fallbacks() {
    }

    static List<ModelSpec> all(Set<String> existing) {
        List<ModelSpec> out = new ArrayList<>();
        Set<String> done = new HashSet<>(existing);
        List<CombatProfile<?>> creatures = new ArrayList<>();
        for (MonsterDef m : ContentRegistry.monsters()) {
            creatures.add(m);
        }
        for (BossDef b : ContentRegistry.bosses()) {
            creatures.add(b);
        }
        for (CombatProfile<?> c : creatures) {
            if (c.model != null || done.contains(c.id)) {
                continue;
            }
            ModelSpec spec = forCreature(c);
            if (spec != null) {
                out.add(spec);
                done.add(c.id);
            }
        }
        return out;
    }

    static boolean has(String text, String... keys) {
        for (String k : keys) {
            if (text.contains(k)) {
                return true;
            }
        }
        return false;
    }

    static ModelSpec forCreature(CombatProfile<?> c) {
        String id = c.id;
        String key = (id + " " + c.name).toLowerCase(Locale.ROOT);
        int[] s = c.skin != null && c.skin.length >= 4 ? c.skin : new int[]{SKIN, 0x6A4A2A, 0x4A3A2A, 0x202020};
        float h = c.height / 1.95f;
        // ---- creatures that are not people ----
        if (has(key, "impling")) {
            return imp(id, s[0], s[3]);
        }
        if (has(key, "tzhaar")) {
            return tzhaar(id, s);
        }
        if (has(key, "spiritual")) {
            return spiritual(id, s, h);
        }
        if (id.equals("catacomb_abyssal_demon")) {
            return Slayers.abyssalDemon(id);
        }
        if (has(key, "dagannoth", "wallasalki")) {
            return Slayers.dagannoth(id, s[0], s[3]);
        }
        if (has(key, "spectre")) {
            return Slayers.spectre(id, s[0], s[1]);
        }
        if (has(key, "banshee")) {
            return Slayers.banshee(id, s[0], s[1]);
        }
        if (has(key, "jelly")) {
            return Slayers.jelly(id, s[0]);
        }
        if (has(key, "bloodveld")) {
            return Slayers.bloodveld(id, s[0], s[3]);
        }
        if (has(key, "hellhound")) {
            return Creatures.canine(id, s[0], s[3], true);
        }
        if (has(key, "werewolf", "wild dog")) {
            return beastman(id, s, "wolf", h);
        }
        if (has(key, "minotaur")) {
            return beastman(id, s, "bull", h);
        }
        if (has(key, "dust devil", "dust_devil")) {
            return devil(id, s[0], s[3]);
        }
        if (has(key, "crab", "lobster")) {
            return crab(id, s[0], s[1], s[3], has(key, "lobster"), has(key, "ammonite"));
        }
        if (has(key, "golem", "elemental", "slagilith", "avatar")) {
            return golem(id, s, h, has(key, "fire") ? "lava" : has(key, "ice") ? "ice" : "rock");
        }
        if (has(key, "leech")) {
            return worm(id, s[0], s[3]);
        }
        if (has(key, "crawler")) {
            return Creatures.spider(id, s[0], s[3], 0.8f);
        }
        if (has(key, "catablepon", "suqah", "shaikahan", "kendal")) {
            return beast(id, s, h);
        }
        if (has(key, "roc", "bird")) {
            return bird(id, s, h);
        }
        if (has(key, "sourhog", "hog")) {
            return Creatures.pig(id);
        }
        if (has(key, "mudskipper")) {
            return mudskipper(id, s[0], s[3]);
        }
        if (has(key, "pyrefiend")) {
            return fiend(id, s[0], s[3]);
        }
        if (has(key, "basilisk")) {
            return Slayers.basilisk(id);
        }
        if (id.equals("abyssal_guardian")) {
            return Creatures.demon(id, s[0], s[1], s[3], 1.0f, false);
        }
        if (id.equals("abyssal_walker")) {
            return beast(id, s, h);
        }
        if (has(key, "moss_guardian", "moss guardian")) {
            ModelSpec.Builder b = Humanoid.of(id).scale(h).giantBody().skin(s[0]).skinMaterial("moss").hair(0, "none").top(s[0], "skin")
                    .sleeves("none").bottoms(s[2], "loincloth").boots(s[0]).weapon("club", WOOD).builder();
            return b.build();
        }
        if (has(key, "skeleton", "boneguard")) {
            return Creatures.skeleton(id, s[0], "sword", true);
        }
        // ---- people and humanoid monsters ----
        return person(id, key, s, h);
    }

    /** Pick an outfit from the name, colour it from the skin palette. */
    static ModelSpec person(String id, String key, int[] s, float h) {
        int skin = s[0];
        int top = s[1];
        int bottom = s[2];
        int acc = s[3];
        Humanoid p = Humanoid.of(id).scale(h).skin(skin).hair(acc, "short").top(top, "cloth").bottoms(bottom, "trousers");
        boolean undead = has(key, "zombie", "undead", "deathly", "arrav", "nazastarool", "husk", "zemouregal", "koschei");
        boolean ghostly = has(key, "ghost", "shadow", "treus", "ulfric", "dessourt", "damis", "spirit", "emissary", " me", "fear reaper",
                "mourner") || key.startsWith("me ");
        if (has(key, "ankou")) {
            p.face("skull").glowingEyes().hair(0, "none").top(top, "robe").bottoms(bottom, "robe").helm("hood", top).weapon("scythe", 0x8A8A8A);
        } else if (has(key, "knight", "mordred", "tyras", "titan", "warlord", "captain", "koschei", "guard", "justiciar",
                "khazard", "paladin", "hero", "warrior", "ithoi", "murderer", "gadderanks")) {
            p.top(top, "metal").bottoms(bottom, "plate").helm(has(key, "general", "justiciar") ? "horned" : "full", top)
                    .shield("kite", top, acc).weapon(has(key, "warlord", "titan") ? "greataxe" : "longsword", 0xB0B0B8);
            if (has(key, "tyras")) {
                p.helm("crown", GOLD).cape(0x8A1A1A);
            }
            if (has(key, "titan")) {
                p.giantBody().face("eyes").glowingEyes();
            }
        } else if (has(key, "mage", "wizard", "melzar", "solus", "alomone", "zemouregal", "culinaromancer", "shaman",
                "druid", "priest", "kolodion", "porazdir", "derwen")) {
            p.top(top, "robe").bottoms(bottom, "robe").trim(acc).helm(has(key, "priest", "druid", "derwen") ? "hood" : "wizard", bottom)
                    .beard(HAIR_GREY, "long").weapon("staff", acc);
            if (has(key, "ogre")) {
                p.ogreBody().face("eyes").beard(0, "none");
            }
        } else if (has(key, "queen", "king", "roald")) {
            p.top(top, "cloth").bottoms(bottom, "robe").helm("crown", GOLD).cape(0x8A1A2A).trim(acc);
        } else if (has(key, "moon")) {
            p.top(top, "metal").bottoms(bottom, "plate").helm("full", top).trim(acc).cape(bottom).weapon("longsword", acc).glowingEyes();
        } else if (has(key, "chef", "flambeed", "karamel")) {
            p.top(0xF0F0F0, "cloth").bottoms(bottom, "trousers").helm("chef", 0xF4F4F4).belt(acc);
        } else if (has(key, "troll", "arrg")) {
            p.trollBody().face("eyes").hair(0, "none").top(skin, "skin").sleeves("none").bottoms(bottom, "loincloth").boots(skin)
                    .skinMaterial("rock");
        } else if (has(key, "ogre", "ogress", "jogre", "cyclops", "barrelchest", "gorak")) {
            p.ogreBody().face("eyes").hair(0, "none").top(top, "leather").sleeves("none").bottoms(bottom, "kilt").weapon("club", WOOD);
        } else if (has(key, "giant", "glod", "guardian")) {
            p.giantBody().hair(0, "none").top(skin, "skin").sleeves("none").bottoms(bottom, "loincloth").boots(skin).weapon("club", WOOD);
            if (has(key, "moss")) {
                p.skinMaterial("moss");
            }
        } else if (has(key, "lizard", "nagua", "naga")) {
            ModelSpec.Builder b = p.body(12, 8, 11, 5, 12, 3).hunch(18).skin(skin).skinMaterial("scales").face("eyes").hair(0, "none")
                    .top(top, "leather").sleeves("none").bottoms(bottom, "loincloth").boots(skin).weapon("spear", acc).builder();
            part(b, "snout", "head", Role.STATIC, skin).at(0, -2.5f, -4).mat("scales").box(-2, -1.5f, -3, 4, 3, 3).add();
            tail(b, "hips", skin, 4);
            return b.build();
        } else if (has(key, "orc", "ork", "hobgoblin", "goblin")) {
            p.body(11, 9, 11, 5, 12, 3).ears("pointy").bigNose(true).face("eyes").hair(0, "none").top(top, "metal").tusks(has(key, "ork"))
                    .bottoms(bottom, "kilt").weapon("scimitar", 0x8A8A8A);
        } else if (has(key, "vampire", "count")) {
            p.skin(0xD8D0C8).face("eyes").glowingEyes().hair(HAIR_BLACK, "short").top(top, "cloth").cape(acc);
        } else if (has(key, "horror", "abomination")) {
            ModelSpec.Builder b = p.body(11, 9, 11, 5, 15, 3).head(8, 7, 8).hunch(28).arms(-20).skin(skin).face("eyes").hair(0, "none")
                    .top(skin, "skin").sleeves("none").bottoms(bottom, "loincloth").boots(skin).builder();
            sym(b, "talon", "hand", true, acc).at(0, 2, -1).rot(-30, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 3, 1).add();
            return b.build();
        } else if (has(key, "pirate", "jonny", "beard")) {
            p.beard(acc, "long").helm("bandana", top).weapon("scimitar", 0xB0B0B8);
        } else if (has(key, "mercenary", "bandit", "phoenix", "gang", "highway", "rogue", "thug", "mugger")) {
            p.top(top, "leather").weapon(has(key, "phoenix") ? "dagger" : "scimitar", 0xA0A0A8).helm("bandana", top);
        } else if (has(key, "elf")) {
            p.ears("pointy").hair(acc, "long").weapon("longbow", 0xE0D8B0).cape(bottom);
        } else if (has(key, "animated", "armour")) {
            p.face("none").skin(top).hair(0, "none").top(top, "metal").sleeves("long").bottoms(bottom, "plate").helm("full", top)
                    .gloves(top).boots(top).weapon("longsword", top);
        }
        if (undead) {
            p.face("eyes").glowingEyes();
        }
        if (has(key, "fire", "lesarkus", "fareed", "flambeed")) {
            ModelSpec.Builder b = p.face("eyes").glowingEyes().builder();
            part(b, "flames", "head", Role.STATIC, 0xFFA020).at(0, -8, 1).mat("lava").glow("full").box(-4, -3, -3, 8, 3, 7).add();
            return b.build();
        }
        if (has(key, "ice", "frost", "kamil", "karamel")) {
            p.skinMaterial("ice");
        }
        if (ghostly) {
            p.face("eyes").glowingEyes();
            ModelSpec.Builder b = p.builder();
            return b.build();
        }
        return p.build();
    }

    static void tail(ModelSpec.Builder b, String parent, int col, int n) {
        String prev = parent;
        for (int i = 0; i < n; i++) {
            part(b, "tail" + i, prev, Role.TAIL, col).at(0, i == 0 ? 0 : 0, i == 0 ? 2 : 4).rot(i == 0 ? 40 : 10, 0, 0).mat("scales")
                    .box(-1.5f + i * 0.25f, -1.5f + i * 0.25f, 0, 3 - i / 2, 3 - i / 2, 4).add();
            prev = "tail" + i;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Special forms
    // ------------------------------------------------------------------------------------------

    /** Impling: a tiny pot-bellied imp with pointed ears, big eyes and buzzing wings. */
    static ModelSpec imp(String id, int col, int wing) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.25f);
        part(b, "body", null, Role.FLOAT, col).at(0, 16, 0).mat("hide").box(-2.5f, -4, -2, 5, 5, 4).add();
        part(b, "head", "body", Role.HEAD, col).accent(0xFFF080).at(0, -4, 0).mat("hide").pattern("eyes").glow("eyes").box(-3, -5, -3, 6, 5, 5).add();
        sym(b, "ear", "head", false, col).at(3, -3, 0).rot(0, -20, -30).mat("hide").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        part(b, "nose", "head", Role.STATIC, col).at(0, -2, -3).mat("hide").box(-0.5f, 0, -1, 1, 2, 1).add();
        sym(b, "arm", "body", false, col).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(3, -3, 0).rot(-20, 0, -15).mat("hide").box(-0.5f, 0, -0.5f, 1, 4, 1, 0.2f).add();
        sym(b, "leg", "body", false, col).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(1.2f, 1, 0).mat("hide").box(-0.5f, 0, -0.5f, 1, 4, 1, 0.2f).add();
        sym(b, "wing", "body", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).at(1, -3, 2).rot(0, -30, -20).mat("membrane").glow("full")
                .box(0, -2, -0.5f, 5, 5, 1, -0.2f).add();
        return b.build();
    }

    /** TzHaar: obsidian-skinned, crested, glowing-eyed people of Mor Ul Rek (Ket: armour + maul, Xil: spear, Mej: robes + staff). */
    static ModelSpec tzhaar(String id, int[] s) {
        Humanoid p = Humanoid.of(id).body(12, 9, 11, 5, 12, 3).head(8, 9, 8).skin(s[0]).skinMaterial("rock").face("eyes").glowingEyes()
                .hair(0, "none").top(s[1], "metal").bottoms(s[2], "kilt").boots(0x2A1410);
        if (id.contains("ket")) {
            p.top(0x3A1A10, "metal").bottoms(0x3A1A10, "plate").weapon("warhammer", 0x2A1A14).shield("sq", 0x2A1A14, 0xE06010);
        } else if (id.contains("xil")) {
            p.top(0x4A2A1A, "leather").weapon("spear", 0x2A1A14);
        } else if (id.contains("mej")) {
            p.top(0x2A1A3A, "robe").bottoms(0x2A1A3A, "robe").trim(0xA040E0).weapon("staff", 0xFF8020);
        } else {
            p.top(s[0], "skin").sleeves("none").bottoms(s[2], "loincloth").weapon("pickaxe", 0x2A1A14);
        }
        ModelSpec.Builder b = p.builder();
        part(b, "crest", "head", Role.STATIC, s[0]).accent(0xFF8020).at(0, -9, 0).mat("rock").glow("cracks").box(-1.5f, -3, -3, 3, 3, 7).add();
        sym(b, "cheek_ridge", "head", false, s[0]).at(4, -4, -1).mat("rock").box(0, -2, -2, 1, 4, 4).add();
        return b.build();
    }

    /** Spiritual warriors/rangers/mages of the God Wars: pale, glowing-eyed, armoured in their god's colours. */
    static ModelSpec spiritual(String id, int[] s, float h) {
        Humanoid p = Humanoid.of(id).scale(h).skin(s[0]).skinMaterial("ghost").face("eyes").glowingEyes().hair(0, "none");
        if (id.contains("mage")) {
            p.top(s[1], "robe").bottoms(s[2], "robe").trim(s[3]).helm("hood", s[1]).weapon("staff", s[3]);
        } else if (id.contains("ranger")) {
            p.top(s[1], "leather").bottoms(s[2], "trousers").helm("coif", s[1]).weapon("longbow", 0x8A6A3A).cape(s[1]);
        } else {
            p.top(s[1], "metal").bottoms(s[2], "plate").helm("full", s[3]).weapon("longsword", 0xC0C0C8).shield("kite", s[1], s[3]);
        }
        return p.build();
    }

    /** Beast-headed humanoids: werewolves and minotaurs. */
    static ModelSpec beastman(String id, int[] s, String kind, float h) {
        ModelSpec.Builder b = Humanoid.of(id).scale(h).body(11, 9, 11, 5, 13, 3).hunch(15).skin(s[0]).skinMaterial("fur").face("eyes")
                .hair(0, "none").top(s[0], "skin").sleeves("none").bottoms(s[2], "loincloth").boots(s[0])
                .weapon(kind.equals("bull") ? "battleaxe" : "none", 0x8A8A8A).builder();
        part(b, "snout", "head", Role.STATIC, s[0]).at(0, -3, -4).mat("fur").box(-2, -1.5f, -4, 4, 3, 4).add();
        part(b, "nose", "snout", Role.STATIC, 0x2A2020).at(0, -1.5f, -4).mat("skin").box(-1, 0, -0.6f, 2, 1, 1).add();
        if (kind.equals("bull")) {
            sym(b, "horn", "head", false, s[3]).at(4, -7, -1).rot(0, 0, -20).mat("horn").box(0, -1, -1, 3, 2, 2).add();
            sym(b, "horn_tip", "horn", true, s[3]).at(3, 0, 0).rot(0, 0, -60).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        } else {
            sym(b, "ear", "head", false, s[0]).at(2.5f, -8, 0).rot(-10, 0, -10).mat("fur").box(-1, -3, -0.5f, 2, 3, 1).add();
            sym(b, "claw", "hand", true, s[3]).at(0, 2, -1).rot(-30, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 2, 1).add();
        }
        return b.build();
    }

    /** Dust devil: a spinning column of sand around a squat, big-mouthed body. */
    static ModelSpec devil(String id, int sand, int light) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        part(b, "body", null, Role.FLOAT, sand).accent(light).at(0, 14, 0).mat("hide").pattern("eyes").box(-4, -6, -4, 8, 7, 8).add();
        part(b, "jaw", "body", Role.JAW, Humanoid.darker(sand)).at(0, 0, -3).mat("hide").box(-3, 0, -2, 6, 2, 3).add();
        int[] r = {12, 9, 6, 4};
        String prev = null;
        for (int i = 0; i < r.length; i++) {
            part(b, "whirl" + i, i == 0 ? null : prev, Role.SPIN, i % 2 == 0 ? light : sand).at(0, i == 0 ? 10 : 3, 0)
                    .rot(0, i * 20, 0).mat("ghost").box(-r[i] / 2f, 0, -r[i] / 2f, r[i], 2, r[i]).add();
            prev = "whirl" + i;
        }
        return b.build();
    }

    /** Crabs and rock lobsters: a wide shell, eye stalks, six legs and two big pincers. */
    static ModelSpec crab(String id, int shell, int legs, int claw, boolean lobster, boolean ammonite) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        part(b, "shell", null, Role.BODY, shell).accent(claw).at(0, 18, 0).mat("chitin").box(-6, -3, -5, 12, 5, 10).add();
        part(b, "shell_top", "shell", Role.STATIC, shell).accent(claw).at(0, -3, 0).mat(ammonite ? "plates" : "chitin").box(-5, -2, -4, 10, 2, 8).add();
        if (ammonite) {
            part(b, "spiral", "shell", Role.STATIC, legs).at(0, -5, 1).mat("plates").box(-1, -6, -4, 2, 8, 8).box(-1.5f, -4, -2, 3, 4, 4).add();
        }
        if (lobster) {
            String prev = "shell";
            for (int i = 0; i < 3; i++) {
                part(b, "tail" + i, prev, Role.TAIL, shell).at(0, 0, i == 0 ? 5 : 3).rot(10, 0, 0).mat("chitin").box(-3 + i * 0.5f, -2, 0, 6 - i, 3, 3).add();
                prev = "tail" + i;
            }
        }
        sym(b, "stalk", "shell", false, legs).at(1.5f, -3, -4.5f).rot(-15, 0, 0).mat("chitin").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        sym(b, "eye", "stalk", true, 0x101010).accent(0xFFFFFF).at(0, -3, 0).mat("energy").box(-0.5f, -1, -0.5f, 1, 1, 1, 0.2f).add();
        sym(b, "arm", "shell", false, claw).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(5, 0, -5).rot(0, 30, -20).mat("chitin")
                .box(-1, -1, -5, 2, 2, 5).add();
        sym(b, "pincer", "arm", true, claw).at(0, 0, -5).rot(0, -40, 0).mat("chitin").box(-1.5f, -1.5f, -4, 3, 3, 4).add();
        sym(b, "pincer_tip", "pincer", true, Humanoid.darker(claw)).at(0, 0, -4).mat("horn").box(-1.5f, -1, -2, 1, 2, 2).box(0.5f, -1, -2, 1, 2, 2).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "shell", false, legs).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).at(5.5f, 1, -2.5f + i * 2.5f).rot(0, -(i - 1) * 20, -30)
                    .mat("chitin").box(0, -0.5f, -0.5f, 4, 1, 1, 0.2f).add();
            sym(b, "foot" + i, "leg" + i, true, legs).at(4, 0, 0).rot(0, 0, 75).mat("chitin").box(0, -0.5f, -0.5f, 5, 1, 1).add();
        }
        return b.build();
    }

    /** Golems and elementals: blocky stone/lava/ice bodies with huge fists and a small glowing head. */
    static ModelSpec golem(String id, int[] s, float h, String material) {
        int col = s[0];
        int dark = s[2];
        String glow = material.equals("lava") ? "cracks" : null;
        ModelSpec.Builder b = ModelSpec.builder(id).scale(h).shadow(0.9f);
        part(b, "hips", null, Role.BODY, dark).accent(s[3]).at(0, 12, 0).mat(material).glow(glow).box(-5, -2, -3, 10, 4, 6).add();
        part(b, "torso", "hips", Role.STATIC, col).accent(s[3]).at(0, -2, 0).rot(12, 0, 0).mat(material).glow(glow).box(-7, -11, -4, 14, 11, 8).add();
        part(b, "core", "torso", Role.STATIC, s[3]).at(0, -7, -4).mat("energy").glow("full").box(-2, -2, -0.6f, 4, 4, 1).add();
        sym(b, "boulder", "torso", false, dark).at(6, -11, 0).mat(material).box(-2, -3, -3, 5, 4, 6).add();
        part(b, "head", "torso", Role.HEAD, col).accent(s[3]).at(0, -11, -2).rot(-12, 0, 0).mat(material).pattern("eyes").glow("eyes")
                .box(-3, -5, -3, 6, 5, 5).add();
        sym(b, "arm", "torso", false, col).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(s[3]).at(8.5f, -9, 0).rot(-10, 0, -8).mat(material)
                .glow(glow).box(-2, -1, -2, 4, 8, 4).add();
        sym(b, "fist", "arm", true, dark).accent(s[3]).at(0, 8, 0).mat(material).box(-3, 0, -3, 6, 6, 6).add();
        sym(b, "leg", null, false, dark).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(s[3]).at(3, 12, 0).mat(material).glow(glow)
                .box(-2.5f, 0, -2.5f, 5, 12, 5).add();
        return b.build();
    }

    static ModelSpec worm(String id, int col, int mouth) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        part(b, "head", null, Role.HEAD, col).accent(mouth).at(0, 18, -6).mat("flesh").pattern("eyes").box(-3, -3, -4, 6, 6, 4).add();
        part(b, "jaw", "head", Role.JAW, mouth).at(0, 1, -4).mat("flesh").box(-2, 0, -1, 4, 2, 1).add();
        String prev = "head";
        for (int i = 0; i < 5; i++) {
            part(b, "seg" + i, prev, Role.TAIL, i % 2 == 0 ? col : Humanoid.darker(col)).at(0, i == 0 ? 0 : 0, i == 0 ? 0 : 3)
                    .rot(0, 0, 0).mat("flesh").box(-2.5f + i * 0.25f, -2.5f + i * 0.25f + (i * 0.5f), 0, 5 - i / 2, 5 - i / 2, 3).add();
            prev = "seg" + i;
        }
        return b.build();
    }

    /** Big horned quadruped beasts (catablepon, suqah, yeti-like beasts). */
    static ModelSpec beast(String id, int[] s, float h) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(Math.max(0.8f, h)).shadow(0.9f);
        part(b, "body", null, Role.BODY, s[0]).accent(s[3]).at(0, 11, 1).mat("fur").box(-5, -5, -8, 10, 10, 16, 0.2f).add();
        part(b, "hump", "body", Role.STATIC, s[1]).at(0, -5, -5).mat("fur").box(-4, -2, -2, 8, 3, 6).add();
        part(b, "head", "body", Role.HEAD, s[0]).accent(s[3]).at(0, -1, -8).mat("fur").pattern("eyes").box(-3.5f, -3, -6, 7, 7, 6).add();
        part(b, "jaw", "head", Role.JAW, s[2]).at(0, 3, -5).mat("fur").box(-3, 0, -2, 6, 2, 3).add();
        sym(b, "horn", "head", false, s[3]).at(3.5f, -2, -2).rot(-20, 0, -10).mat("horn").box(0, -1, -1, 3, 2, 2).add();
        sym(b, "horn_tip", "horn", true, s[3]).at(3, 0, 0).rot(0, 0, -70).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        Creatures.legs4(b, s[0], s[3], s[2], 3.5f, -5, 5, 8, 4);
        return b.build();
    }

    static ModelSpec bird(String id, int[] s, float h) {
        ModelSpec.Builder b = ModelSpec.builder(id).scale(Math.max(0.8f, h)).shadow(1.0f);
        part(b, "body", null, Role.BODY, s[0]).accent(s[1]).at(0, 12, 0).rot(-20, 0, 0).mat("feathers").box(-5, -5, -7, 10, 10, 14).add();
        part(b, "neck", "body", Role.NECK, s[0]).at(0, -4, -6).rot(-30, 0, 0).mat("feathers").box(-2, -6, -2, 4, 7, 4).add();
        part(b, "head", "neck", Role.HEAD, s[0]).accent(s[3]).at(0, -6, 0).rot(50, 0, 0).mat("feathers").pattern("eyes").box(-3, -5, -3, 6, 5, 6).add();
        part(b, "beak", "head", Role.JAW, s[3]).at(0, -2, -3).mat("horn").box(-1, -1, -4, 2, 2, 4).add();
        Bosses.featherWings(b, "body", s[1], s[2], 5, -4, -3, 14);
        part(b, "tail", "body", Role.TAIL, s[1]).at(0, -3, 7).rot(-20, 0, 0).mat("feathers").box(-4, -1, 0, 8, 2, 7).add();
        sym(b, "leg", null, false, s[3]).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(2.5f, 16, 1).mat("horn").box(-1, 0, -1, 2, 7, 2).add();
        sym(b, "talon", "leg", true, s[3]).at(0, 7, 0).mat("horn").box(-2, 0, -3, 4, 1, 4).add();
        return b.build();
    }

    static ModelSpec mudskipper(String id, int col, int fin) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        part(b, "body", null, Role.BODY, col).accent(fin).at(0, 20, 0).mat("scales").box(-2.5f, -2.5f, -6, 5, 5, 10).add();
        part(b, "head", "body", Role.HEAD, col).accent(0xF0F040).at(0, -1, -6).mat("scales").pattern("eyes").box(-3, -2.5f, -3, 6, 5, 3).add();
        part(b, "fin", "body", Role.STATIC, fin).at(0, -2.5f, -2).mat("membrane").box(-0.5f, -3, 0, 1, 3, 6).add();
        part(b, "tail", "body", Role.TAIL, col).at(0, 0, 4).mat("scales").box(-1, -1.5f, 0, 2, 3, 4).add();
        part(b, "tail_fin", "tail", Role.FIN, fin).at(0, 0, 4).mat("membrane").box(-0.5f, -3, 0, 1, 6, 2).add();
        sym(b, "leg", "body", false, fin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(2.5f, 1.5f, -3).rot(0, 0, -30).mat("membrane").box(0, 0, -1, 1, 3, 2).add();
        return b.build();
    }

    static ModelSpec fiend(String id, int col, int glow) {
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.4f);
        part(b, "body", null, Role.FLOAT, col).accent(glow).at(0, 12, 0).mat("lava").box(-3, -7, -2.5f, 6, 8, 5).add();
        part(b, "head", "body", Role.HEAD, col).accent(glow).at(0, -7, 0).mat("lava").pattern("eyes").glow("eyes").box(-3, -5, -3, 6, 5, 6).add();
        part(b, "flame", "head", Role.STATIC, glow).at(0, -5, 1).mat("lava").glow("full").box(-2, -4, -2, 4, 4, 4).add();
        sym(b, "arm", "body", false, col).roles(Role.ARM_LEFT, Role.ARM_RIGHT).at(4, -6, 0).rot(-40, 0, -20).mat("lava").box(-1, -1, -1, 2, 6, 2).add();
        part(b, "wisp", "body", Role.TAIL, glow).at(0, 1, 0).rot(15, 0, 0).mat("lava").glow("full").box(-2, 0, -2, 4, 5, 4).add();
        return b.build();
    }
}
