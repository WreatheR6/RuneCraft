package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiarySlmKit.*;
import static com.runecraft.osrs.content.bestiary.Humanoid.*;

/**
 * Slayer and dungeon creatures that have no model of their own: spiritual warriors/rangers/mages, infernal
 * and malevolent mages, ankous, mogres, cave goblins, chaos druids, the Catacombs giants and dagannoth, the
 * Abyss's leeches, guardians and walkers, flesh crawlers and catablepons.
 */
final class BestiarySlmFolk {
    private BestiarySlmFolk() {
    }

    /** Ids the bestiary used to dress as generic folk (Folk), now sculpted here. */
    static List<ModelSpec> fresh() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(mage("infernal_mage", 0x3A1414, 0xC83A1E, 0xFF8A20, 1.0f));
        l.add(mage("malevolent_mage", 0x241A30, 0x7A3AC0, 0xC070FF, 1.0f));
        l.add(mogre("mogre"));
        l.add(caveGoblin("cave_goblin", false));
        l.add(caveGoblin("cave_goblin_guard", true));
        l.add(chaosDruid("chaos_druid"));
        l.add(giant("catacomb_hill_giant", 0xC8A47A, "hill"));
        l.add(giant("catacomb_moss_giant", 0x8A9A6A, "moss"));
        l.add(giant("catacomb_fire_giant", 0xD8743A, "fire"));
        return l;
    }

    static List<ModelSpec> fallbacks() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(spiritual("spiritual_warrior", 0));
        l.add(spiritual("spiritual_ranger", 1));
        l.add(spiritual("spiritual_mage", 2));
        l.add(ankou("ankou", 0x4A2A5A));
        l.add(ankou("catacomb_ankou", 0x2A3A4A));
        l.add(dagannoth("catacomb_dagannoth"));
        l.add(leech("abyssal_leech"));
        l.add(abyssalGuardian("abyssal_guardian"));
        l.add(abyssalWalker("abyssal_walker"));
        l.add(fleshCrawler("flesh_crawler"));
        l.add(catablepon("catablepon"));
        return l;
    }

    /** Spiritual warrior (0) / ranger (1) / mage (2): a pale, glowing-eyed revenant spirit in its fighting gear, trailing wisps. */
    static ModelSpec spiritual(String id, int kind) {
        int ghost = 0xB8D0D0;
        int wisp = 0x8AE0D8;
        Humanoid h = Humanoid.of(id).skin(ghost).skinMaterial("ghost").face("skull").glowingEyes().hair(0, "none");
        switch (kind) {
            case 0 -> h.top(0x7A8288, "metal").bottoms(0x6A7278, "plate").helm("full", 0x7A8288).pauldrons(0x8A9298)
                    .shield("kite", 0x5A6A7A, 0xC8B060).weapon("longsword", 0xC0C8D0).boots(0x5A6268);
            case 1 -> h.top(0x3E5A3A, "leather").bottoms(0x4A4A3A, "trousers").helm("hood", 0x3E5A3A).cape(0x2E4A2A)
                    .weapon("longbow", 0x7A5A30).boots(LEATHER).gloves(DARK_LEATHER);
            default -> h.top(0x3A4A7A, "robe").bottoms(0x3A4A7A, "robe").helm("hood", 0x2E3A6A).trim(0xC8B060)
                    .weapon("staff", 0x6A4A2A);
        }
        ModelSpec.Builder b = h.builder();
        // spectral wisps trailing from the body
        for (int k = 0; k < 3; k++) {
            Kit.part(b, "wisp" + k, "hips", Role.TAIL, wisp).accent(wisp).at(-2 + k * 2, 2, 2).rot(30 + k * 10, 0, (k - 1) * 15).mat("ghost")
                    .glow("full").box(-0.5f, 0, -0.5f, 1, 5 + k % 2 * 2, 1, -0.1f).add();
        }
        Kit.part(b, "aura", "torso", Role.SPIN, wisp).accent(wisp).at(0, -5, 0).mat("ghost").glow("full")
                .box(-7, -0.5f, -0.5f, 1, 1, 1, -0.2f).box(6, -0.5f, -0.5f, 1, 1, 1, -0.2f).box(-0.5f, -0.5f, 5, 1, 1, 1, -0.2f)
                .box(-0.5f, 2, -6, 1, 1, 1, -0.2f).add();
        switch (kind) {
            case 0 -> {
                st(b, "tabard", "torso", 0x5A6E8A).at(0, -6, -2.7f).mat("cloth").box(-2, 0, -0.5f, 4, 9, 1).add();
                st(b, "plume", "head", 0xA02A2A).at(0, -8.5f, 0).rot(-15, 0, 0).mat("hair").box(-0.5f, -3, -3, 1, 3, 7).add();
                sy(b, "tasset", "hips", 0x7A8288).at(2, 1, -2.6f).rot(-6, 0, 0).mat("metal").box(-1.5f, 0, -0.5f, 3, 4, 1).add();
                st(b, "belt_pouch", "hips", LEATHER).at(3, 0, 1).mat("leather").box(-1, -1, -1, 2, 2, 2).add();
                sy2(b, "elbow_cop", "forearm", 0x8A9298).at(0, 0.5f, 1.4f).mat("metal").box(-1, -1, -0.5f, 2, 2, 1, 0.2f).add();
            }
            case 1 -> {
                st(b, "quiver", "torso", LEATHER).at(2, -9, 2.6f).rot(0, 0, 25).mat("leather").box(-1.5f, 0, -1, 3, 8, 2).add();
                Kit.P ar = st(b, "arrows", "quiver", 0xE0D8C0).at(0, 0, 0).mat("feathers");
                for (int k = 0; k < 3; k++) {
                    ar.box(-1 + k * 0.8f, -2, -0.5f, 1, 2, 1, -0.15f);
                }
                ar.add();
                st(b, "bracer", "forearm_l", DARK_LEATHER).at(0, 1, 0).mat("leather").box(-1.5f, 0, -1.5f, 3, 3, 3, 0.15f).add();
                st(b, "scarf", "torso", 0x2E4A2A).at(0, -10, -1).mat("cloth").box(-3, -1, -2.6f, 6, 2, 5, 0.2f).add();
                sy2(b, "shoulder_pad", "arm", DARK_LEATHER).at(0, -1, 0).mat("leather").box(-2, -1, -2, 4, 2, 4).add();
            }
            default -> {
                Kit.part(b, "orb", "w_staff", Role.SPIN, wisp).accent(wisp).at(0, -12, 0).mat("energy").glow("full")
                        .box(-1.5f, -1.5f, -1.5f, 3, 3, 3).add();
                st(b, "sash", "torso", 0xC8B060).at(0, -10, 0).rot(0, 0, 30).mat("cloth").box(-0.5f, 0, -2.7f, 1, 12, 1).add();
                st(b, "book", "hips", 0x5A2A1A).at(4, 0, 0).mat("leather").box(-0.5f, -1.5f, -1.5f, 1, 3, 3).add();
                sy(b, "collar", "torso", 0x2E3A6A).at(2.5f, -10, 0).rot(0, 0, -20).mat("cloth").box(-1.5f, -1.5f, -2.5f, 3, 2, 5).add();
            }
        }
        return b.build();
    }

    /** Infernal / malevolent mage: a gaunt sorcerer in a deep hood and layered robes, a glowing staff and floating sigils. */
    static ModelSpec mage(String id, int robe, int trim, int magic, float s) {
        ModelSpec.Builder b = Humanoid.of(id).scale(s).skin(0xC8B8A8).face("eyes").glowingEyes().hair(0, "none").beard(0x8A8A88, "long")
                .top(robe, "robe").bottoms(robe, "robe").trim(trim).helm("hood", robe).cape(darker(robe, 0.8f)).weapon("staff", 0x2A1A14)
                .builder();
        Kit.part(b, "staff_gem", "w_staff", Role.SPIN, magic).accent(magic).at(0, -12, 0).mat("energy").glow("full").box(-1, -2, -1, 2, 3, 2).add();
        sy(b, "staff_prong", "w_staff", 0x2A1A14).at(1, -11, 0).rot(0, 0, -25).mat("wood").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        st(b, "mantle", "torso", trim).at(0, -10, 0).mat("cloth").box(-4.5f, -1, -3, 9, 3, 6, 0.1f).add();
        sy(b, "shoulder_spike", "mantle", 0x2A2A2A).at(4, -1, 0).rot(0, 0, -40).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        st(b, "sash", "torso", trim).at(0, -4, 0).mat("cloth").box(-4.2f, 0, -2.7f, 8, 1, 5, 0.1f).add();
        st(b, "sash_tail", "sash", trim).at(1.5f, 1, -2.7f).mat("cloth").box(-1, 0, -0.5f, 2, 7, 1).add();
        st(b, "robe_panel", "skirt", darker(robe, 0.7f)).at(0, 0, -3).mat("cloth").box(-1.5f, 0, -0.4f, 3, 10, 1).add();
        st(b, "amulet", "torso", 0xD8A830).accent(magic).at(0, -8, -2.8f).mat("metal").glow("full").box(-1, 0, -0.5f, 2, 2, 1).add();
        for (int k = 0; k < 3; k++) {
            float a = (float) Math.toRadians(k * 120);
            Kit.part(b, "sigil" + k, "torso", Role.SPIN, magic).accent(magic).at((float) Math.cos(a) * 9, -6 + k, (float) Math.sin(a) * 9).mat("energy")
                    .glow("full").box(-1, -1, -0.25f, 2, 2, 1, -0.2f).add();
        }
        Kit.part(b, "hand_glow", "hand_l", Role.FLOAT, magic).accent(magic).at(0, 3, 0).mat("energy").glow("full").box(-1, -1, -1, 2, 2, 2, -0.2f).add();
        return b.build();
    }

    /** Ankou: a robed skeleton reaper with glowing sockets, a hooded cloak, a bony ribcage and a great scythe. */
    static ModelSpec ankou(String id, int robe) {
        ModelSpec.Builder b = Humanoid.of(id).skin(BONE).skinMaterial("bone").face("skull").glowingEyes().hair(0, "none")
                .top(robe, "robe").bottoms(robe, "robe").trim(darker(robe, 0.6f)).sleeves("none").helm("hood", robe).cape(darker(robe, 0.75f))
                .weapon("scythe", 0x8A8A90).builder();
        st(b, "ribcage", "torso", BONE).at(0, -9, -2.6f).mat("bone").box(-2.5f, 0, -0.6f, 5, 1, 1).box(-2.5f, 1.5f, -0.6f, 5, 1, 1)
                .box(-2, 3, -0.6f, 4, 1, 1).box(-0.5f, 0, -0.8f, 1, 5, 1).add();
        st(b, "robe_open", "torso", 0x141018).at(0, -9, -2.55f).mat("cloth").box(-3, 0, -0.3f, 6, 6, 1).add();
        st(b, "jaw_bone", "head", BONE).at(0, -1, -4).mat("bone").box(-2, 0, -0.6f, 4, 1, 1).add();
        teeth(b, "teeth", "jaw_bone", 0xF0ECDC, -0.5f, -0.7f, 1.8f, 0, true, true);
        st(b, "rope_belt", "hips", 0x8A7A5A).at(0, -1.5f, 0).mat("leather").box(-4.2f, -0.5f, -2.7f, 8, 1, 5, 0.1f).add();
        st(b, "rope_end", "rope_belt", 0x8A7A5A).at(-2, 0.5f, -2.8f).rot(0, 0, 10).mat("leather").box(-0.5f, 0, -0.5f, 1, 6, 1).add();
        for (int k = 0; k < 4; k++) {
            st(b, "tatter" + k, "hem", k % 2 == 0 ? robe : darker(robe, 0.7f)).at(-3 + k * 2, 0.5f, -3).rot(-8, 0, (k - 1.5f) * 6).mat("cloth")
                    .box(-1, 0, -0.5f, 2, 2 + k % 2, 1).add();
        }
        Kit.part(b, "soul_lantern", "hand_l", Role.FLOAT, 0x9AE0FF).accent(0x9AE0FF).at(0, 3, 0).mat("energy").glow("full")
                .box(-1, 0, -1, 2, 3, 2).add();
        st(b, "lantern_cap", "soul_lantern", 0x3A3A3A).at(0, 0, 0).mat("metal").box(-1.5f, -1, -1.5f, 3, 1, 3).add();
        sy2(b, "knuckles", "hand", BONE).at(0, 1.5f, -1.2f).mat("bone").box(-1, 0, -0.5f, 2, 1, 1, 0.1f).add();
        return b.build();
    }

    /** Mogre: a squat, fish-faced ogre of the shallows - finned crest, gill slits, webbed hands and a big gaping lip. */
    static ModelSpec mogre(String id) {
        int skin = 0x6E8A6A;
        int fin = 0x4A6A6A;
        ModelSpec.Builder b = Humanoid.of(id).ogreBody().skin(skin).skinMaterial("scales").face("eyes").hair(0, "none").tusks(false)
                .top(skin, "skin").sleeves("none").bottoms(0x4A3A2A, "loincloth").boots(skin).weapon("spear", 0x7A5A30).builder();
        st(b, "lip", "head", mix(skin, 0xD8A890, 0.4f)).at(0, -1.5f, -3.8f).mat("skin").box(-3, 0, -1.5f, 6, 2, 2).add();
        teeth(b, "teeth", "lip", TOOTH, 2, -1.3f, 2.8f, 1, true, true);
        st(b, "crest", "head", fin).at(0, -7, 0).rot(-10, 0, 0).mat("membrane").box(-0.5f, -3, -3, 1, 3, 7).add();
        for (int k = 0; k < 3; k++) {
            st(b, "crest_ray" + k, "crest", 0x2E4A4A).at(0, -3, -2 + k * 2.5f).rot(-20, 0, 0).mat("horn").box(-0.5f, -1, -0.5f, 1, 2, 1).add();
        }
        sy(b, "gill", "head", fin).at(3.6f, -3, 0).rot(0, -15, 0).mat("membrane").box(0, -2, -2, 1, 4, 4).add();
        sy(b, "gill_slit", "torso", darker(skin, 0.5f)).at(4.4f, -9, 0).mat("skin").box(-0.6f, 0, -1.5f, 1, 1, 3).box(-0.6f, 2, -1.5f, 1, 1, 3).add();
        st(b, "back_fin", "torso", fin).at(0, -11, 3.5f).rot(30, 0, 0).mat("membrane").box(-0.5f, 0, 0, 1, 8, 4).add();
        sy2(b, "elbow_fin", "forearm", fin).at(0, 1, 1.5f).rot(30, 0, 0).mat("membrane").box(-0.5f, 0, 0, 1, 4, 3).add();
        sy2(b, "webbing", "hand", fin).at(0, 2, 0).mat("membrane").box(-2.5f, 0, -2, 5, 2, 1, -0.1f).add();
        st(b, "belly_scales", "belly", mix(skin, 0xE0E0C0, 0.4f)).at(0, -2, -2.4f).mat("plates").box(-3.5f, 0, -0.3f, 7, 6, 1).add();
        st(b, "seaweed", "hips", 0x3A6A2A).at(-3, 0, -3).rot(-10, 0, 10).mat("moss").box(-0.5f, 0, -0.5f, 1, 5, 1).add();
        return b.build();
    }

    /** Cave goblin (Dorgesh-Kaan): a pale-skinned, huge-eyed goblin in a miner's tunic; the guard adds a helmet, mail and a mace. */
    static ModelSpec caveGoblin(String id, boolean guard) {
        int skin = 0xC8D8C0;
        Humanoid h = Humanoid.of(id).goblinBody().skin(skin).face("eyes").hair(0, "none").ears("pointy")
                .top(guard ? 0x5A5A6A : 0x6A5A8A, guard ? "chain" : "cloth").bottoms(guard ? 0x3A3A4A : 0x4A3A5A, "trousers").boots(DARK_LEATHER);
        if (guard) {
            h.helm("med", 0x6A6A72).weapon("mace", 0x7A7A82).shield("round", 0x5A4A3A, 0x8A8A90);
        } else {
            h.weapon("pickaxe", 0x7A7A82);
        }
        ModelSpec.Builder b = h.builder();
        Kit.P eyes = st(b, "big_eyes", "head", 0x1A1A12).accent(0xF0F0D0).at(0, -4.2f, -3.6f).mat("energy").glow("full");
        eyes.box(-3, -1, -0.5f, 2, 2, 1, -0.05f).box(1, -1, -0.5f, 2, 2, 1, -0.05f).add();
        st(b, "nose_tip", "nose", mix(skin, 0xD0A090, 0.4f)).at(0, 2, -1.5f).mat("skin").box(-1, -0.5f, -0.5f, 2, 1, 1).add();
        st(b, "satchel", "hips", LEATHER).at(-3.5f, 0, 0).mat("leather").box(-1, -1, -1.5f, 2, 3, 3).add();
        st(b, "satchel_strap", "torso", DARK_LEATHER).at(0, -7, 0).rot(0, 0, -35).mat("leather").box(-0.5f, 0, -2.3f, 1, 9, 5, 0.05f).add();
        sy2(b, "ear_ring", "ear", 0xD8A830).at(2.5f, 0.5f, 0).mat("metal").box(-0.5f, 0, -0.5f, 1, 1, 1, -0.2f).add();
        if (guard) {
            st(b, "tabard", "torso", 0x8A6A2A).at(0, -6, -2.3f).mat("cloth").box(-1.5f, 0, -0.5f, 3, 8, 1).add();
            sy2(b, "spaulder", "arm", 0x6A6A72).at(0, -1, 0).rot(0, 0, -10).mat("metal").box(-1.5f, -1, -1.5f, 3, 2, 3, 0.2f).add();
        } else {
            Kit.part(b, "lamp", "satchel", Role.FLOAT, 0xFFD060).accent(0xFFD060).at(-1, 3, 0).mat("energy").glow("full").box(-1, 0, -1, 2, 2, 2).add();
            st(b, "lamp_cap", "lamp", 0x4A4A4A).at(0, 0, 0).mat("metal").box(-1.2f, -0.8f, -1.2f, 2, 1, 2, 0.2f).add();
        }
        return b.build();
    }

    /** Chaos druid: a hunched old druid in black robes with a deep hood, a grey beard, a sickle and herb pouches. */
    static ModelSpec chaosDruid(String id) {
        ModelSpec.Builder b = Humanoid.of(id).skin(SKIN_PALE).hair(HAIR_GREY, "long").beard(HAIR_GREY, "long").top(0x2A2A2E, "robe")
                .bottoms(0x2A2A2E, "robe").trim(0x8A1A1A).helm("hood", 0x222226).weapon("staff", WOOD).hunch(10).builder();
        st(b, "mantle", "torso", 0x3A3A40).at(0, -10, 0).mat("cloth").box(-4.5f, -1, -3, 9, 3, 6, 0.1f).add();
        st(b, "rope", "hips", 0x8A7A5A).at(0, -1.5f, 0).mat("leather").box(-4.2f, -0.5f, -2.7f, 8, 1, 5, 0.1f).add();
        sy(b, "pouch", "rope", 0x5A4A2A).at(3, 1, -1).mat("leather").box(-1, 0, -1, 2, 2, 2).add();
        st(b, "sickle", "rope", 0xC0C0C8).at(-3, 1, -1.5f).rot(0, 0, 20).mat("metal").box(-0.5f, 0, -0.5f, 1, 3, 1).box(-0.5f, 3, -0.5f, 3, 1, 1).add();
        st(b, "mistletoe", "w_staff", 0x5A8A3A).at(0, -10, 0).mat("moss").box(-1.5f, -1, -1.5f, 3, 2, 3).add();
        Kit.part(b, "berry", "mistletoe", Role.STATIC, 0xE8E8D8).at(0, 1, -1.6f).mat("energy").box(-0.5f, 0, -0.5f, 1, 1, 1).add();
        st(b, "skull_charm", "torso", BONE).at(0, -7, -2.8f).mat("bone").box(-1, 0, -0.5f, 2, 2, 1).add();
        st(b, "robe_back", "skirt", 0x222226).at(0, 2, 3).rot(8, 0, 0).mat("cloth").box(-3.5f, 0, -0.5f, 7, 9, 1).add();
        sy2(b, "sleeve_hang", "forearm", 0x2A2A2E).at(0, 1, 1.5f).rot(20, 0, 0).mat("cloth").box(-1.5f, 0, -0.5f, 3, 5, 1).add();
        return b.build();
    }

    /** A Catacombs giant: hill (bald brute with a club), moss (moss-grown, mossy club) or fire (blazing hair and a great sword). */
    static ModelSpec giant(String id, int skin, String kind) {
        boolean fire = kind.equals("fire");
        boolean moss = kind.equals("moss");
        Humanoid h = Humanoid.of(id).giantBody().skin(skin).hair(0, "none").top(skin, "skin").sleeves("none")
                .bottoms(fire ? 0x5A2A1A : moss ? 0x4A5A2A : 0x6A4A2A, "loincloth").boots(skin)
                .weapon(fire ? "longsword" : "club", fire ? 0x9A9AA0 : WOOD).face("eyes");
        if (moss) {
            h.skinMaterial("moss");
        }
        ModelSpec.Builder b = h.builder();
        st(b, "brow", "head", darker(skin, 0.7f)).at(0, -5.5f, -4.1f).rot(14, 0, 0).mat(moss ? "moss" : "skin").box(-4, -1, -0.6f, 8, 2, 2).add();
        st(b, "jaw_jut", "head", skin).at(0, -1.5f, -4).mat(moss ? "moss" : "skin").box(-3, 0, -1, 6, 2, 1).add();
        sy(b, "underbite", "jaw_jut", TOOTH).at(2, 0, -1).mat("horn").box(-0.5f, -1.5f, -0.5f, 1, 2, 1, -0.1f).add();
        st(b, "belt_rope", "hips", 0x6A5A3A).at(0, -1.5f, 0).mat("leather").box(-6, -0.5f, -3.5f, 12, 1, 7, 0.1f).add();
        sy2(b, "bracer", "forearm", LEATHER).at(0, 3, 0).mat("leather").box(-2.5f, 0, -2.5f, 5, 2, 5, 0.1f).add();
        st(b, "bone_charm", "torso", BONE).at(0, -9, -3.4f).mat("bone").box(-1.5f, 0, -0.5f, 3, 2, 1).add();
        st(b, "necklace", "torso", 0x6A5A3A).at(0, -10.5f, 0).mat("leather").box(-3, 0, -3.3f, 6, 1, 1).add();
        if (fire) {
            for (int k = 0; k < 5; k++) {
                st(b, "flame_hair" + k, "head", k % 2 == 0 ? 0xFF8A20 : 0xFFD040).accent(0xFFD040).at(-3 + k * 1.5f, -8, -1 + (k % 2) * 2)
                        .rot(-15, 0, (k - 2) * -12).mat("lava").glow("full").box(-1, -3 - (k == 2 ? 2 : 0), -1, 2, 3 + (k == 2 ? 2 : 0), 2).add();
            }
            Kit.part(b, "ember", "w_blade", Role.FLOAT, 0xFF8A20).accent(0xFF8A20).at(0, -6, 1).mat("lava").glow("full").box(-0.5f, -0.5f, -0.5f, 1, 1, 1).add();
            sy2(b, "flame_shoulder", "arm", 0xFF8A20).accent(0xFFD040).at(0, -2, 0).mat("lava").glow("full").box(-1.5f, -2, -1.5f, 3, 2, 3, -0.1f).add();
        } else if (moss) {
            for (int k = 0; k < 5; k++) {
                st(b, "moss_clump" + k, k % 2 == 0 ? "torso" : "head", 0x4A7A2A).at(-3 + k * 1.5f, k % 2 == 0 ? -11 : -8, k % 2 == 0 ? 2 : 1)
                        .mat("moss").box(-1.5f, -1, -1.5f, 3, 2, 3).add();
            }
            sy(b, "moss_beard", "jaw_jut", 0x4A7A2A).at(1.5f, 2, -0.5f).mat("moss").box(-1, 0, -0.5f, 2, 3, 1).add();
        } else {
            st(b, "scalp", "head", darker(skin, 0.9f)).at(0, -8, 0).mat("skin").box(-3.5f, -0.5f, -3.5f, 7, 1, 7).add();
            st(b, "pelt", "torso", 0x8A6A44).at(0, -11, 0).rot(0, 0, 20).mat("fur").box(-5, 0, -3.5f, 5, 4, 7, 0.1f).add();
            sy(b, "ear_big", "head", skin).at(4, -4.5f, 0).mat("skin").box(0, -1, -1, 1, 3, 2).add();
        }
        return b.build();
    }

    /** Dagannoth: an upright sea beast - finned crest, a long-jawed head full of teeth, stubby clawed arms and thick legs. */
    static ModelSpec dagannoth(String id) {
        int skin = 0x4A6A58;
        int dark = 0x2E4636;
        int belly = 0xA8B08A;
        int fin = 0x6A8A6A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "hips", null, Role.BODY, skin).accent(belly).at(0, 13, 2).mat("scales").box(-5, -3, -3.5f, 10, 5, 7).add();
        st(b, "torso", "hips", skin).accent(belly).at(0, -2.5f, 0).rot(18, 0, 0).mat("scales").box(-5.5f, -11, -4, 11, 11, 8).add();
        Kit.P bp = st(b, "belly", "torso", belly).accent(darker(belly, 0.7f)).at(0, -10, -4).mat("plates");
        for (int r = 0; r < 4; r++) {
            bp.box(-3.5f, r * 2.5f, -0.7f, 7, 2, 1, 0.05f);
        }
        bp.add();
        for (int i = 0; i < 4; i++) {
            st(b, "fin" + i, "torso", fin).at(0, -11 + i * 3, 4).rot(30, 0, 0).mat("membrane").box(-0.5f, -1, 0, 1, 3, 3 - (i % 2)).add();
        }
        Kit.part(b, "head", "torso", Role.HEAD, skin).accent(0xF0E040).at(0, -11.5f, -2).rot(-30, 0, 0).mat("scales").pattern("eyes").glow("eyes")
                .box(-3.5f, -6, -4, 7, 6, 6).add();
        st(b, "snout", "head", skin).at(0, -2.5f, -4).mat("scales").box(-2.5f, -1.5f, -4, 5, 3, 4).add();
        teeth(b, "teeth_up", "snout", TOOTH, 1.5f, -3.7f, 2.2f, 2, true, true);
        Kit.part(b, "jaw", "head", Role.JAW, belly).at(0, 0, -3).rot(22, 0, 0).mat("plates").box(-3, 0, -5, 6, 2, 6).add();
        teeth(b, "teeth_lo", "jaw", TOOTH, 0, -4.7f, 2.6f, 2, false, true);
        st(b, "crest", "head", fin).at(0, -6, -1).rot(-30, 0, 0).mat("membrane").box(-0.5f, -4, -2, 1, 4, 6).add();
        sy(b, "head_fin", "head", fin).at(3.5f, -3, 0).rot(0, -30, -20).mat("membrane").box(0, -2, 0, 1, 4, 4).add();
        Kit.sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(belly).at(6, -9, -1).rot(-45, 0, -15)
                .mat("scales").box(-1.5f, -1, -1.5f, 3, 6, 3).add();
        sy2(b, "forearm", "arm", dark).at(0, 5.5f, 0).rot(-40, 0, 0).mat("scales").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        Kit.sym(b, "claws", "forearm", true, CLAW).at(0, 5, -0.5f).rot(20, 0, 0).mat("horn")
                .box(-1.5f, 0, -1, 1, 3, 1, -0.05f).box(0.5f, 0, -1, 1, 3, 1, -0.05f).add();
        Kit.sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(belly).at(3, 13, 2)
                .rot(-15, 0, -4).mat("scales").box(-2.5f, -1, -2.5f, 5, 6, 5).add();
        sy2(b, "shin", "thigh", dark).at(0, 5, 0).rot(25, 0, 3).mat("scales").box(-2, 0, -2, 4, 5, 4).add();
        sy2(b, "foot", "shin", dark).at(0, 4.8f, 0).rot(-10, 0, 0).mat("scales").box(-2.5f, 0, -4, 5, 1, 6).add();
        claws(b, "toe_claws", "foot", true, CLAW, 3, 3, 0.5f, -3.8f, 2);
        String t = chain(b, "tail", "hips", Role.TAIL, skin, belly, "scales", null, 0, 0, 3.5f, 1, new int[]{5, 4, 3, 2},
                new int[]{4, 3, 3, 2}, new int[]{4, 4, 3, 3}, new float[]{50, 10, 6, 0}, new float[]{0, 10, 10, -10});
        st(b, "tail_fin", t, fin).at(0, 0, 3).mat("membrane").box(-2.5f, -0.5f, 0, 5, 1, 3).add();
        return b.build();
    }

    /** Abyssal leech: a fat, segmented crimson-purple leech reared up on its tail, its round sucker mouth ringed with teeth. */
    static ModelSpec leech(String id) {
        int skin = 0x5A2048;
        int band = 0x8A3A5A;
        int mouth = 0xC03030;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.5f);
        Kit.part(b, "body", null, Role.BODY, skin).accent(band).at(0, 21, 4).mat("flesh").box(-3.5f, -3, -3, 7, 6, 7).add();
        String n = chain(b, "seg", "body", Role.STATIC, skin, band, "flesh", null, 0, 0, -3, -1, new int[]{7, 7, 6, 6, 5},
                new int[]{6, 6, 6, 5, 5}, new int[]{3, 3, 3, 3, 3}, new float[]{-25, -18, -15, -10, 0}, null);
        for (int i = 0; i < 5; i += 1) {
            st(b, "ring" + i, "seg" + i, band).at(0, 0, -1.5f).mat("flesh").box(-3.5f + i * 0.3f, -3, -0.5f, 7 - (int) (i * 0.6f), 6 - i / 3, 1, 0.15f).add();
        }
        Kit.part(b, "head", n, Role.HEAD, skin).accent(0xFF4040).at(0, 0, -3).mat("flesh").box(-3, -3, -2, 6, 6, 2).add();
        Kit.part(b, "mouth", "head", Role.JAW, mouth).accent(mouth).at(0, 0, -2).mat("flesh").glow("maw").box(-2, -2, -0.6f, 4, 4, 1).add();
        Kit.P t = st(b, "teeth", "head", TOOTH).at(0, 0, -2.3f).mat("horn");
        for (int k = 0; k < 8; k++) {
            float a = (float) Math.toRadians(k * 45);
            t.box((float) Math.cos(a) * 2.4f - 0.5f, (float) Math.sin(a) * 2.4f - 0.5f, -0.5f, 1, 1, 1, -0.15f);
        }
        t.add();
        String tail = chain(b, "tail", "body", Role.TAIL, skin, band, "flesh", null, 0, 1, 4, 1, new int[]{6, 5, 4, 3, 2},
                new int[]{5, 4, 4, 3, 2}, new int[]{3, 3, 3, 3, 3}, new float[]{-5, 5, 5, 5, 0}, new float[]{0, 15, 15, -15, -15});
        st(b, "tail_sucker", tail, band).at(0, 0, 3).mat("flesh").box(-1.5f, -1.5f, 0, 3, 3, 1).add();
        for (int k = 0; k < 4; k++) {
            st(b, "spot" + k, "seg" + k, 0xA04A70).at(k % 2 == 0 ? 2 : -2, -3, -1.5f).mat("flesh").box(-0.5f, -0.6f, -0.5f, 1, 1, 1, 0.1f).add();
        }
        return b.build();
    }

    /** Abyssal guardian: an eyeless, hunched crimson brute with a cavernous tusked mouth, exposed ribs and long-clawed arms. */
    static ModelSpec abyssalGuardian(String id) {
        int skin = 0x6A2232;
        int dark = 0x3A101A;
        int bone = 0xC8B898;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.8f);
        Kit.part(b, "hips", null, Role.BODY, skin).accent(dark).at(0, 13, 2).mat("flesh").box(-4.5f, -2, -3, 9, 4, 6).add();
        st(b, "torso", "hips", skin).accent(dark).at(0, -1.5f, 0).rot(40, 0, 0).mat("flesh").box(-6, -11, -4, 12, 11, 8).add();
        Kit.P r = st(b, "ribs", "torso", bone).at(0, -9, -4).mat("bone");
        for (int k = 0; k < 4; k++) {
            r.box(-4.5f + k * 0.3f, k * 2, -0.7f, 9 - (int) (k * 0.6f), 1, 1);
        }
        r.add();
        for (int i = 0; i < 4; i++) {
            st(b, "spine" + i, "torso", bone).at(0, -10 + i * 2.6f, 4).rot(45, 0, 0).mat("horn").box(-0.5f, -0.5f, -0.5f, 1, 1, 3).add();
        }
        Kit.part(b, "head", "torso", Role.HEAD, skin).accent(0xF0A020).at(0, -11, -2.5f).rot(-40, 0, 0).mat("flesh")
                .box(-4, -5, -4, 8, 5, 6).add();
        st(b, "upper_lip", "head", dark).at(0, -1, -4).mat("flesh").box(-3.5f, 0, -1.5f, 7, 2, 2).add();
        teeth(b, "teeth_up", "upper_lip", TOOTH, 2, -1.4f, 3.2f, 1, true, true);
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 0, -2.5f).rot(30, 0, 0).mat("flesh").glow("maw").box(-3.5f, 0, -3.5f, 7, 3, 4).add();
        teeth(b, "teeth_lo", "jaw", TOOTH, 0, -3.2f, 3.2f, 1, false, true);
        sy(b, "tusk", "jaw", bone).at(3, 0, -3).rot(-25, 0, 20).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1, 0.1f).add();
        horn(b, "horn", "head", true, false, bone, 3, -4.5f, 0, -30, 0, -40, new int[]{2, 1}, new int[]{3, 3}, -30, 10);
        Kit.sym(b, "arm", "torso", false, skin).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(7, -9.5f, 0).rot(-40, 0, -10)
                .mat("flesh").box(-2, -1.5f, -2, 4, 9, 4).add();
        sy2(b, "shoulder", "arm", dark).at(0, -1, 0).mat("flesh").box(-2.5f, -1.5f, -2.5f, 5, 3, 5).add();
        sy2(b, "forearm", "arm", skin).at(0, 8.5f, 0).rot(-30, 0, 4).mat("flesh").box(-2, 0, -2, 4, 7, 4, 0.2f).add();
        sy2(b, "hand", "forearm", dark).at(0, 7, 0).mat("flesh").box(-2.5f, 0, -2.5f, 5, 2, 5).add();
        Kit.sym(b, "talons", "hand", true, bone).at(0, 2, -1.5f).rot(25, 0, 0).mat("horn")
                .box(-2.5f, 0, -1, 1, 4, 1, -0.05f).box(-0.5f, 0, -1.2f, 1, 4, 1, -0.05f).box(1.5f, 0, -1, 1, 4, 1, -0.05f).add();
        Kit.sym(b, "thigh", null, false, skin).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(3, 13, 2)
                .rot(-22, 0, -6).mat("flesh").box(-2.5f, -1, -2.5f, 5, 6, 5).add();
        sy2(b, "shin", "thigh", dark).at(0, 5, 0).rot(42, 0, 5).mat("flesh").box(-2, 0, -2, 4, 5, 4).add();
        sy2(b, "foot", "shin", dark).at(0, 4.8f, 0).rot(-20, 0, 0).mat("flesh").box(-2.5f, 0, -3.5f, 5, 1, 5).add();
        claws(b, "toe_claws", "foot", true, bone, 3, 3, 0.5f, -3.3f, 2);
        return b.build();
    }

    /** Abyssal walker: a bloated purple body slung between four long, many-jointed stilt legs, with a dangling tendrilled maw. */
    static ModelSpec abyssalWalker(String id) {
        int skin = 0x3E2450;
        int dark = 0x221430;
        int glow = 0xC040E0;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.9f);
        Kit.part(b, "body", null, Role.BODY, skin).accent(glow).at(0, 9, 0).mat("flesh").pattern("spots").box(-5, -5, -6, 10, 8, 12).add();
        st(b, "sac", "body", mix(skin, glow, 0.35f)).accent(glow).at(0, 3, 2).mat("flesh").glow("cracks").box(-3.5f, 0, -3.5f, 7, 4, 7).add();
        for (int i = 0; i < 4; i++) {
            st(b, "knob" + i, "body", dark).at(i % 2 == 0 ? 2.5f : -2.5f, -5, -4 + i * 2.6f).mat("flesh").box(-1, -1.5f, -1, 2, 2, 2, 0.1f).add();
        }
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(glow).at(0, 0, -6).rot(20, 0, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-3.5f, -3, -4, 7, 5, 4).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 2, -3).rot(15, 0, 0).mat("flesh").glow("maw").box(-2.5f, 0, -2, 5, 2, 3).add();
        for (int k = 0; k < 4; k++) {
            Kit.part(b, "feeler" + k, "jaw", Role.TENTACLE, dark).at(-1.5f + k, 2, -1).rot(10, 0, (k - 1.5f) * 10).mat("flesh")
                    .box(-0.5f, 0, -0.5f, 1, 4 + k % 2 * 2, 1, -0.1f).add();
        }
        float[][] lp = {{4.5f, 0, -4}, {4.5f, 0, 4}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, "body", false, skin).roles(roles[i][0], roles[i][1]).accent(glow).at(lp[i][0], lp[i][1], lp[i][2])
                    .rot(0, i == 0 ? 25 : -25, -50).mat("flesh").box(0, -1, -1, 7, 2, 2).add();
            sy2(b, ln + "_knee", ln, dark).at(7, 0, 0).mat("flesh").box(-1, -1.5f, -1.5f, 3, 3, 3).add();
            sy2(b, ln + "_shin", ln, skin).at(7, 0, 0).rot(0, 0, 115).mat("flesh").box(0, -1, -1, 12, 2, 2, -0.1f).add();
            sy2(b, ln + "_foot", ln + "_shin", dark).at(12, 0, 0).rot(0, 0, -25).mat("horn").box(0, -0.5f, -0.5f, 3, 1, 1).add();
        }
        String t = chain(b, "tail", "body", Role.TAIL, skin, glow, "flesh", null, 0, 0, 6, 1, new int[]{4, 3, 2}, new int[]{4, 3, 2},
                new int[]{3, 3, 3}, new float[]{30, 15, 15}, null);
        st(b, "tail_tip", t, glow).accent(glow).at(0, 0, 3).mat("energy").glow("full").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        return b.build();
    }

    /** Flesh crawler: a raw-pink, hunched crawling horror on four clawed limbs with a split, toothed head and back spines. */
    static ModelSpec fleshCrawler(String id) {
        int skin = 0xA06A5A;
        int dark = 0x6A3A2E;
        int raw = 0xC0605A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.6f);
        Kit.part(b, "body", null, Role.BODY, skin).accent(raw).at(0, 15, 1).rot(-8, 0, 0).mat("flesh").box(-4.5f, -4, -6, 9, 7, 12).add();
        st(b, "hump", "body", raw).at(0, -4, -2).mat("flesh").box(-3.5f, -2, -3, 7, 2, 7).add();
        for (int i = 0; i < 4; i++) {
            st(b, "spine" + i, "body", 0xE0D0B0).at(i % 2 == 0 ? 1.5f : -1.5f, -5.5f, -3 + i * 2.5f).rot(-30, 0, i % 2 == 0 ? -15 : 15).mat("horn")
                    .box(-0.5f, -3, -0.5f, 1, 3, 1, 0.1f).add();
        }
        st(b, "belly", "body", mix(skin, 0xE0B0A0, 0.4f)).at(0, 3, 0).mat("flesh").box(-3.5f, -0.6f, -5, 7, 1, 10).add();
        Kit.part(b, "head", "body", Role.HEAD, skin).accent(0xF0E040).at(0, -1, -6).rot(12, 0, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-3.5f, -3, -4, 7, 5, 4).add();
        st(b, "brow", "head", dark).at(0, -2.5f, -4).rot(12, 0, 0).mat("flesh").box(-3.6f, -0.5f, -0.6f, 7, 1, 1).add();
        Kit.part(b, "jaw", "head", Role.JAW, raw).at(0, 2, -3).rot(25, 0, 0).mat("flesh").box(-3, 0, -2.5f, 6, 2, 3).add();
        teeth(b, "teeth", "jaw", TOOTH, 0, -2.3f, 2.8f, 1, false, true);
        sy(b, "mandible", "head", dark).at(3, 1, -3.5f).rot(0, 25, 0).mat("horn").box(-0.5f, -0.5f, -3, 1, 1, 3).add();
        float[][] lp = {{4.5f, 15, -4}, {4.5f, 15, 4.5f}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, null, false, skin).roles(roles[i][0], roles[i][1]).accent(raw).at(lp[i][0], lp[i][1], lp[i][2])
                    .rot(0, 0, -40).mat("flesh").box(-1.5f, -1, -1.5f, 6, 3, 3).add();
            sy2(b, ln + "_shin", ln, dark).at(4.5f, 0.5f, 0).rot(0, 0, 75).mat("flesh").box(-1, 0, -1, 2, 7, 2).add();
            sy2(b, ln + "_hand", ln + "_shin", dark).at(0, 6.5f, 0).rot(0, 0, -30).mat("flesh").box(-1.5f, 0, -2, 3, 1, 3).add();
            claws(b, ln + "_claws", ln + "_hand", true, 0x2A1A14, 3, 2, 0.5f, -1.8f, 2);
        }
        String t = chain(b, "tail", "body", Role.TAIL, skin, raw, "flesh", null, 0, -1, 6, 1, new int[]{4, 3, 2, 1}, new int[]{3, 3, 2, 1},
                new int[]{3, 3, 3, 3}, new float[]{15, 10, 10, 5}, new float[]{0, 10, 10, -10});
        st(b, "tail_tip", t, 0xE0D0B0).at(0, 0, 3).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 2).add();
        return b.build();
    }

    /** Catablepon: a shaggy, bull-like beast whose massive horned head hangs low on a thick neck, with a tufted tail and hooves. */
    static ModelSpec catablepon(String id) {
        int fur = 0x5A4A3A;
        int dark = 0x3A2A1A;
        int mane = 0x2A1E14;
        int horn = 0xD8CCA8;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(0.9f);
        Kit.part(b, "body", null, Role.BODY, fur).accent(mane).at(0, 11, 2).mat("fur").box(-5.5f, -5, -9, 11, 10, 18).add();
        st(b, "hump", "body", mane).at(0, -5, -5).rot(-10, 0, 0).mat("fur").box(-5, -3, -4, 10, 4, 8).add();
        st(b, "belly", "body", mix(fur, 0x8A7A5A, 0.4f)).at(0, 5, 0).mat("fur").box(-4.5f, -0.6f, -7, 9, 1, 14).add();
        sy(b, "flank", "body", dark).at(5.5f, 0, 4).mat("fur").box(-0.5f, -3.5f, -4, 1, 7, 8).add();
        st(b, "neck", "body", mane).accent(fur).at(0, -1, -9).rot(30, 0, 0).mat("fur").box(-4, -4, -5, 8, 8, 5).add();
        Kit.part(b, "head", "neck", Role.HEAD, fur).accent(0xE0302A).at(0, 1, -5).rot(20, 0, 0).mat("fur").pattern("eyes").glow("eyes")
                .box(-4, -3.5f, -6, 8, 7, 6).add();
        st(b, "muzzle", "head", dark).at(0, 1.5f, -6).mat("skin").box(-3, -1.5f, -2, 6, 4, 2).add();
        sy(b, "nostril", "muzzle", 0x141010).at(1.2f, -0.5f, -2.1f).mat("skin").box(-0.5f, -0.5f, -0.3f, 1, 1, 1, -0.1f).add();
        Kit.part(b, "jaw", "head", Role.JAW, dark).at(0, 3.5f, -4).rot(8, 0, 0).mat("fur").box(-2.5f, 0, -3.5f, 5, 1, 4).add();
        horn(b, "horn", "head", true, false, horn, 3.5f, -3, -2, 10, 0, -80, new int[]{2, 2, 1}, new int[]{3, 3, 3}, -35, 0);
        st(b, "forelock", "head", mane).at(0, -3.5f, -3).mat("hair").box(-2, -1, -3, 4, 2, 4).add();
        float[][] lp = {{3.5f, 15, -6}, {3.5f, 15, 6}};
        Role[][] roles = {{Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT}, {Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT}};
        for (int i = 0; i < 2; i++) {
            String ln = i == 0 ? "leg_f" : "leg_b";
            Kit.sym(b, ln, null, false, fur).roles(roles[i][0], roles[i][1]).accent(mane).at(lp[i][0], lp[i][1], lp[i][2])
                    .rot(i == 0 ? 4 : -10, 0, -2).mat("fur").box(-2, -2, -2, 4, 6, 4).add();
            sy2(b, ln + "_shin", ln, dark).at(0, 4, 0).rot(i == 0 ? -4 : 12, 0, 2).mat("fur").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
            sy2(b, ln + "_hoof", ln + "_shin", 0x241A12).at(0, 4, 0).mat("horn").box(-1.75f, 0, -2, 3, 1, 3, 0.25f).add();
            sy2(b, ln + "_feather", ln + "_shin", mane).at(0, 2, 0).mat("hair").box(-1.75f, 0, -1.75f, 3, 2, 3, 0.4f).add();
        }
        String t = chain(b, "tail", "body", Role.TAIL, fur, mane, "fur", null, 0, -3, 9, 1, new int[]{2, 1, 1}, new int[]{2, 1, 1},
                new int[]{3, 3, 3}, new float[]{40, 20, 10}, null);
        st(b, "tail_tuft", t, mane).at(0, 0, 3).mat("hair").box(-1, -1, 0, 2, 2, 3).add();
        return b.build();
    }
}
