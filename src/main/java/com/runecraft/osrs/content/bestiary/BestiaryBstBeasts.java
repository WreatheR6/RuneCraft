package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.BestiaryBst.chain;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.dark;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.light;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.sideFangs;
import static com.runecraft.osrs.content.bestiary.BestiaryBst.teeth;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Beasts and arthropods: Cerberus, Callisto/Artio, the Giant Mole, Scurrius, the Corporeal Beast, the Kalphite
 * Queen (both forms), Venenatis/Spindel, Araxxor, Sarachnis and Scorpia.
 */
final class BestiaryBstBeasts {
    private BestiaryBstBeasts() {
    }

    static final int VENENATIS = 0;
    static final int ARAXXOR = 1;
    static final int SARACHNIS = 2;

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(cerberus());
        l.add(bear("callisto", 0x4E3220, 0x8A6A4A, 0xFF4020));
        l.add(bear("artio", 0x6A4A2E, 0xA88A64, 0xFF6A30));
        l.add(giantMole());
        l.add(scurrius());
        l.add(corp());
        l.add(kalphiteQueen());
        l.add(kalphiteQueenAirborne());
        l.add(spider("venenatis", 0x2A1A2E, 0x3A2440, 0xB03A8A, 0xFF4ACA, VENENATIS));
        l.add(spider("spindel", 0x3A2238, 0x4A2E48, 0xD05AA0, 0xFF70D0, VENENATIS));
        l.add(spider("araxxor", 0x2E2E26, 0x3A3A2A, 0xB8D040, 0xC8FF40, ARAXXOR));
        l.add(spider("sarachnis", 0x6A2218, 0x7A2A1A, 0xE0A040, 0xFF6020, SARACHNIS));
        l.add(scorpia());
        return l;
    }

    // ------------------------------------------------------------------------------------------------
    // Cerberus
    // ------------------------------------------------------------------------------------------------

    /**
     * Cerberus: a colossal three-headed hellhound of black, fissured hide glowing with hellfire, massive spiked
     * collars, a ridge of spines, clawed paws and a burning tail.
     */
    static ModelSpec cerberus() {
        int hide = 0x2A1C1C;
        int deep = 0x3A2420;
        int dk = 0x140C0C;
        int fire = 0xFF7A1E;
        int horn = 0x4A3A34;
        int metal = 0x4A4448;
        ModelSpec.Builder b = ModelSpec.builder("cerberus").shadow(1.7f);
        part(b, "body", null, Role.BODY, hide).accent(fire).at(0, 9, 2).rot(-8, 0, 0).mat("rock").glow("cracks")
                .box(-7, -7, -12, 14, 13, 12).box(-6, -6, 0, 12, 11, 11).add();
        part(b, "chest", "body", Role.STATIC, deep).accent(fire).at(0, 1, -12).rot(10, 0, 0).mat("rock").glow("cracks")
                .box(-6, -6, -2, 12, 11, 3).add();
        part(b, "belly", "body", Role.STATIC, dk).accent(fire).at(0, 5, 0).mat("rock").box(-5, -1, -10, 10, 2, 18).add();
        sym(b, "shoulder", "body", false, hide).accent(fire).at(7, -4, -8).rot(0, 0, -12).mat("rock").glow("cracks")
                .box(-2.5f, -4, -4.5f, 5, 9, 9).add();
        sym(b, "haunch", "body", false, hide).accent(fire).at(6, -2, 6).rot(0, 0, -6).mat("rock").glow("cracks")
                .box(-2, -4, -4.5f, 4, 9, 9).add();
        sym(b, "rib", "body", false, deep).accent(fire).at(7, 0, -2).rot(0, 0, -4).mat("rock").box(-0.5f, -3, -3, 1, 5, 6).add();
        int[] sp = {4, 6, 5, 5, 4, 3};
        for (int i = 0; i < sp.length; i++) {
            part(b, "spine" + i, "body", Role.STATIC, horn).accent(dk).at(0, -7, -10 + i * 3.8f).rot(-35, 0, 0).mat("horn")
                    .box(-1, -sp[i], -1, 2, sp[i], 2).box(-0.5f, -sp[i] - 1, -0.5f, 1, 1, 1).add();
        }
        // three heads
        for (int h = 0; h < 3; h++) {
            String s = String.valueOf(h);
            float off = h - 1;
            boolean mid = off == 0;
            part(b, "neck" + s, "chest", Role.NECK, hide).accent(fire).at(off * 5f, -3, -2).rot(mid ? -38 : -26, -off * 30, -off * 10)
                    .mat("rock").glow("cracks").box(-3, -3, -6, 6, 6, 7).add();
            part(b, "collar" + s, "neck" + s, Role.STATIC, metal).accent(0x8A8A90).at(0, 0, -3).mat("metal")
                    .box(-3.5f, -3.5f, -1, 7, 7, 2).add();
            part(b, "collar_spikes" + s, "collar" + s, Role.STATIC, 0x9A9AA0).at(0, 0, 0).mat("metal")
                    .box(-0.5f, -6, -0.5f, 1, 3, 1).box(3, -0.5f, -0.5f, 3, 1, 1).box(-6, -0.5f, -0.5f, 3, 1, 1).add();
            String hd = "head" + s;
            part(b, hd, "neck" + s, Role.HEAD, hide).accent(fire).at(0, 0, -6).rot(mid ? 38 : 26, 0, 0).mat("rock")
                    .box(-3.5f, -3.5f, -5, 7, 6, 6).add();
            part(b, "muzzle" + s, hd, Role.STATIC, deep).accent(fire).at(0, 0, -5).rot(4, 0, 0).mat("rock")
                    .box(-2.5f, -1.5f, -5, 5, 3, 5).add();
            part(b, "nose" + s, "muzzle" + s, Role.STATIC, dk).at(0, -1.5f, -5).mat("rock").box(-1.5f, -0.5f, -0.6f, 3, 1, 1).add();
            part(b, "brow" + s, hd, Role.STATIC, dk).accent(fire).at(0, -3, -5).rot(16, 0, 0).mat("rock").box(-3.5f, -1, -1, 7, 2, 2).add();
            b.part("eyes" + s, hd, Role.STATIC, 0xFFC040).pivot(0, -1.4f, -5.2f).material("energy").glow("full")
                    .box(1.2f, -0.5f, -0.4f, 2, 1, 1).box(-3.2f, -0.5f, -0.4f, 2, 1, 1);
            part(b, "jaw" + s, hd, Role.JAW, dk).accent(fire).at(0, 2, -4).rot(16, 0, 0).mat("rock").glow("maw")
                    .box(-2.5f, 0, -6, 5, 2, 6).add();
            sideFangs(b, "fangs" + s, "muzzle" + s, 0xF0E8D0, 2.4f, 1.4f, -4.6f, 2.2f, 2, 1, false);
            sym(b, "ear" + s, hd, false, hide).accent(fire).at(2.6f, -3.5f, -1).rot(-35, -10, -20).mat("rock")
                    .box(-1, -4, -1, 2, 4, 2).box(-0.5f, -5, -0.5f, 1, 1, 1).add();
        }
        // legs
        sym(b, "leg_f", null, false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(fire).at(6.5f, 10, -8).rot(8, 0, -4)
                .mat("rock").glow("cracks").box(-3, -2, -3.5f, 6, 9, 7).add();
        sym(b, "fore_f", "leg_f", true, deep).accent(fire).at(0, 7, 0).rot(-10, 0, 4).mat("rock").glow("cracks").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "elbow_f", "leg_f", true, horn).at(0, 6, 3).rot(55, 0, 0).mat("horn").box(-0.5f, 0, -0.5f, 1, 3, 1, 0.2f).add();
        sym(b, "paw_f", "fore_f", true, dk).accent(fire).at(0, 5.5f, 0).rot(2, 0, 0).mat("rock").box(-3, 0, -4.5f, 6, 2, 6).add();
        sym(b, "claws_f", "paw_f", true, 0xD8D0B8).at(0, 1, -4.5f).mat("horn")
                .box(-3, -0.5f, -1.5f, 1, 1, 2).box(-1.2f, -0.5f, -2, 1, 1, 2).box(0.6f, -0.5f, -2, 1, 1, 2).box(2.2f, -0.5f, -1.5f, 1, 1, 2).add();
        sym(b, "thigh_b", null, false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(fire).at(6, 9, 7).rot(-24, 0, -4)
                .mat("rock").glow("cracks").box(-3, -3, -4, 6, 10, 8).add();
        sym(b, "shin_b", "thigh_b", true, deep).accent(fire).at(0, 6.5f, 1).rot(54, 0, 4).mat("rock").box(-2, 0, -2, 4, 6, 4).add();
        sym(b, "meta_b", "shin_b", true, deep).accent(fire).at(0, 5.5f, 0).rot(-36, 0, 0).mat("rock").glow("cracks").box(-2, 0, -2, 4, 4, 4).add();
        sym(b, "paw_b", "meta_b", true, dk).accent(fire).at(0, 3.5f, 0).rot(6, 0, 0).mat("rock").box(-3, 0, -4.5f, 6, 2, 6).add();
        sym(b, "claws_b", "paw_b", true, 0xD8D0B8).at(0, 1, -4.5f).mat("horn")
                .box(-3, -0.5f, -1.5f, 1, 1, 2).box(-1.2f, -0.5f, -2, 1, 1, 2).box(0.6f, -0.5f, -2, 1, 1, 2).box(2.2f, -0.5f, -1.5f, 1, 1, 2).add();
        // tail ending in hellfire
        String tail = chain(b, "tail", "body", Role.TAIL, hide, fire, "rock", 0, -4, 10.5f, false,
                new int[]{4, 3, 3, 2}, new int[]{5, 5, 5, 4}, new float[]{-30, 10, 14, 16}, null);
        part(b, "tail_fire", tail, Role.STATIC, fire).accent(0xFFE070).at(0, 0, 4).mat("lava").glow("full")
                .box(-1.5f, -2.5f, 0, 3, 3, 3).box(-0.5f, -4.5f, 0.5f, 1, 2, 2).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Callisto / Artio
    // ------------------------------------------------------------------------------------------------

    /** Callisto and Artio: towering, hump-backed bears with shaggy ruffs, pale muzzles and great hooked claws. */
    static ModelSpec bear(String id, int fur, int pale, int eye) {
        int dk = dark(fur, 0.35f);
        int claw = 0xE8E0C8;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.3f);
        part(b, "body", null, Role.BODY, fur).accent(pale).at(0, 10, 2).rot(-6, 0, 0).mat("fur")
                .box(-7, -7, -11, 14, 13, 12).box(-6.5f, -6, 1, 13, 12, 10).add();
        part(b, "hump", "body", Role.STATIC, fur).at(0, -7, -6).rot(-8, 0, 0).mat("fur").box(-5.5f, -3, -4, 11, 4, 9).add();
        for (int i = 0; i < 4; i++) {
            part(b, "tuft" + i, "body", Role.STATIC, i % 2 == 0 ? dk : fur).at(0, -7 + (i == 0 ? -2 : 0), -9 + i * 5.5f).rot(-30 + i * 5, 0, 0)
                    .mat("fur").box(-3.5f + i * 0.5f, -2, -2, 7 - i, 3, 4).add();
        }
        part(b, "fringe", "body", Role.STATIC, dk).at(0, 5, -4).mat("fur").box(-6, 0, -6, 12, 2, 14).add();
        sym(b, "shoulder", "body", false, fur).at(7, -3, -7).rot(0, 0, -10).mat("fur").box(-2.5f, -4, -4.5f, 5, 9, 9).add();
        sym(b, "haunch", "body", false, fur).at(6.5f, -2, 7).rot(0, 0, -6).mat("fur").box(-2, -4, -4.5f, 4, 9, 9).add();
        // head
        part(b, "head", "body", Role.HEAD, fur).accent(eye).at(0, -1, -12).rot(14, 0, 0).mat("fur").box(-4.5f, -4, -6, 9, 8, 7).add();
        sym(b, "ruff", "head", false, dk).at(4.5f, 0, -1).rot(0, -10, -8).mat("fur").box(-1, -3.5f, -2, 2, 7, 5).add();
        part(b, "brow", "head", Role.STATIC, dk).at(0, -3, -6).rot(10, 0, 0).mat("fur").box(-4, -1, -1, 8, 2, 2).add();
        part(b, "muzzle", "head", Role.STATIC, pale).accent(fur).at(0, 1.5f, -6).rot(4, 0, 0).mat("fur").box(-2.5f, -2, -4, 5, 4, 4).add();
        part(b, "nose", "muzzle", Role.STATIC, 0x1A1412).at(0, -2, -4).mat("skin").box(-1.5f, 0, -0.6f, 3, 2, 1).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -1.5f, -6.1f).material("energy").glow("full")
                .box(1.6f, -0.5f, -0.4f, 2, 1, 1).box(-3.6f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, dk).accent(0x7A2A2A).at(0, 3.5f, -5).rot(14, 0, 0).mat("fur").box(-2.5f, 0, -4.5f, 5, 2, 5).add();
        sideFangs(b, "fangs", "muzzle", 0xF0E8D0, 2.3f, 2, -3.6f, 2, 1, 1, false);
        teeth(b, "teeth_lo", "jaw", 0xF0E8D0, 0, 0, -4.3f, 4, 3, 1, true);
        sym(b, "ear", "head", false, fur).accent(pale).at(3, -4, -2).rot(-10, 0, -15).mat("fur").box(-1.5f, -2.5f, -1, 3, 3, 2).add();
        // legs
        sym(b, "leg_f", null, false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(pale).at(5.5f, 11, -8).rot(6, 0, -3)
                .mat("fur").box(-3, -2, -3.5f, 6, 9, 7).add();
        sym(b, "fore_f", "leg_f", true, fur).at(0, 7, 0).rot(-6, 0, 3).mat("fur").box(-2.5f, 0, -2.5f, 5, 5, 5).add();
        sym(b, "paw_f", "fore_f", true, dk).at(0, 4.5f, 0).mat("fur").box(-3, 0, -4.5f, 6, 2, 6).add();
        sym(b, "claws_f", "paw_f", true, claw).at(0, 1.2f, -4.5f).rot(20, 0, 0).mat("horn")
                .box(-3, -0.5f, -2, 1, 1, 2).box(-1.3f, -0.5f, -2.5f, 1, 1, 3).box(0.3f, -0.5f, -2.5f, 1, 1, 3).box(2, -0.5f, -2, 1, 1, 2).add();
        sym(b, "thigh_b", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(pale).at(5.5f, 10, 7).rot(-14, 0, -3)
                .mat("fur").box(-3, -3, -4, 6, 10, 8).add();
        sym(b, "shin_b", "thigh_b", true, fur).at(0, 6.5f, 0.5f).rot(20, 0, 3).mat("fur").box(-2.5f, 0, -2.5f, 5, 6, 5).add();
        sym(b, "paw_b", "shin_b", true, dk).at(0, 5.5f, 0).rot(-6, 0, 0).mat("fur").box(-3, 0, -4.5f, 6, 2, 6).add();
        sym(b, "claws_b", "paw_b", true, claw).at(0, 1.2f, -4.5f).mat("horn")
                .box(-2.5f, -0.5f, -1.5f, 1, 1, 2).box(-0.5f, -0.5f, -2, 1, 1, 2).box(1.5f, -0.5f, -1.5f, 1, 1, 2).add();
        part(b, "tail", "body", Role.TAIL, fur).at(0, -4, 11).rot(-20, 0, 0).mat("fur").box(-1.5f, -1.5f, 0, 3, 3, 3).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Giant Mole / Scurrius
    // ------------------------------------------------------------------------------------------------

    /** Giant Mole: a huge, rounded dark-furred mole with a fleshy star snout and enormous pink digging hands. */
    static ModelSpec giantMole() {
        int fur = 0x3A2A22;
        int furL = 0x4E3A2C;
        int pink = 0xE0A0A0;
        int claw = 0xE8E0C8;
        ModelSpec.Builder b = ModelSpec.builder("giant_mole").shadow(1.3f);
        part(b, "body", null, Role.BODY, fur).accent(furL).at(0, 15, 2).mat("fur")
                .box(-8, -7, -10, 16, 14, 20).box(-6.5f, -9, -8, 13, 2, 16).box(-6, 7, -8, 12, 1, 16).add();
        sym(b, "flank", "body", false, furL).at(8, 0, 0).mat("fur").box(0, -5, -8, 1, 10, 16).add();
        part(b, "rump", "body", Role.STATIC, fur).at(0, 0, 10).mat("fur").box(-6.5f, -6, 0, 13, 11, 2).box(-4.5f, -4, 2, 9, 7, 1).add();
        part(b, "front", "body", Role.STATIC, furL).at(0, 0, -10).mat("fur").box(-6.5f, -6, -1, 13, 12, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "tuft" + i, "body", Role.STATIC, i % 2 == 0 ? furL : fur).at((i % 2 == 0 ? 2 : -2), -9, -6 + i * 4).rot(-20, i * 30, 0)
                    .mat("fur").box(-2, -1, -2, 4, 1, 4).add();
        }
        // head and star snout
        part(b, "head", "body", Role.HEAD, furL).accent(furL).at(0, 1, -11).rot(8, 0, 0).mat("fur").pattern("eyes_animal")
                .box(-5, -4, -4, 10, 8, 4).add();
        part(b, "snout", "head", Role.STATIC, pink).at(0, 1, -4).mat("flesh").box(-3, -2.5f, -3, 6, 5, 3).box(-2, -1.5f, -4, 4, 3, 1).add();
        part(b, "star", "snout", Role.TENTACLE, pink).at(0, 0, -4).mat("flesh")
                .box(-0.5f, -4, -0.5f, 1, 2, 1).box(-0.5f, 2, -0.5f, 1, 2, 1).box(2, -0.5f, -0.5f, 2, 1, 1).box(-4, -0.5f, -0.5f, 2, 1, 1)
                .box(1.5f, -3, -0.5f, 1, 1, 1).box(-2.5f, -3, -0.5f, 1, 1, 1).box(1.5f, 2, -0.5f, 1, 1, 1).box(-2.5f, 2, -0.5f, 1, 1, 1).add();
        part(b, "nostrils", "snout", Role.STATIC, 0x5A2A2A).at(0, 0, -4).mat("flesh").box(-1.5f, -0.5f, -0.6f, 1, 1, 1).box(0.5f, -0.5f, -0.6f, 1, 1, 1).add();
        part(b, "teeth", "head", Role.JAW, 0xF0E8C8).at(0, 3.5f, -6).mat("bone").box(-1.5f, 0, -0.5f, 1, 2, 1).box(0.5f, 0, -0.5f, 1, 2, 1).add();
        // the great digging hands
        sym(b, "arm", "body", false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(7, 3, -7).rot(-6, -30, -30).mat("fur")
                .box(-1, -2.5f, -2.5f, 5, 5, 5).add();
        sym(b, "hand", "arm", true, pink).at(4, 0, 0).rot(0, 0, 30).mat("flesh").box(0, -3.5f, -4, 2, 7, 7).add();
        sym(b, "claws", "hand", true, claw).at(1, 0, -3.5f).rot(-10, 0, 0).mat("horn")
                .box(-0.5f, -3.5f, -3, 1, 1, 3).box(-0.5f, -1.5f, -4, 1, 1, 4).box(-0.5f, 0.5f, -4, 1, 1, 4).box(-0.5f, 2.5f, -3, 1, 1, 3).add();
        sym(b, "thumb", "hand", true, pink).at(1, 3, -2).rot(0, 0, 40).mat("flesh").box(0, -0.5f, -1, 3, 1, 2).add();
        sym(b, "hind", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(5.5f, 19, 8).mat("fur").box(-2, -1, -2, 4, 4, 4).add();
        sym(b, "hind_foot", "hind", true, pink).at(0, 3, 0).mat("flesh").box(-2, 0, -4, 4, 2, 5).box(-2, 1, -5, 1, 1, 1).box(1, 1, -5, 1, 1, 1).add();
        String tail = chain(b, "tail", "rump", Role.TAIL, pink, pink, "flesh", 0, 2, 3, false,
                new int[]{2, 2, 1}, new int[]{3, 3, 3}, new float[]{25, -10, -10}, null);
        return b.build();
    }

    /** Scurrius: the rat king of the Varrock sewers, a giant mangy rat with burning eyes and a long scaly tail. */
    static ModelSpec scurrius() {
        int fur = 0x5A4A3E;
        int furD = 0x3A2E26;
        int pink = 0xD89090;
        int eye = 0xFF3020;
        ModelSpec.Builder b = ModelSpec.builder("scurrius").shadow(1.2f);
        part(b, "body", null, Role.BODY, fur).accent(furD).at(0, 13, 2).rot(-8, 0, 0).mat("fur")
                .box(-6.5f, -6, -11, 13, 11, 11).box(-7, -6.5f, 0, 14, 12, 11).add();
        part(b, "belly", "body", Role.STATIC, 0x8A7A68).at(0, 5, 0).mat("fur").box(-5, -0.5f, -10, 10, 1, 19).add();
        for (int i = 0; i < 6; i++) {
            part(b, "mange" + i, "body", Role.STATIC, i % 2 == 0 ? furD : light(fur, 0.1f)).at(i % 2 == 0 ? 1 : -1, -6.5f + (i > 2 ? -0.5f : 0), -9 + i * 3.5f)
                    .rot(-35, (i % 3 - 1) * 15, 0).mat("hair").box(-2.5f, -2, -1, 5, 2, 3).add();
        }
        sym(b, "scar", "body", false, 0x9A6A60).at(6.6f, -2, -3).rot(0, 0, 0).mat("flesh").box(0, -0.5f, -3, 1, 1, 6).add();
        // head
        part(b, "head", "body", Role.HEAD, fur).accent(eye).at(0, -2, -11).rot(16, 0, 0).mat("fur").box(-4, -3.5f, -6, 8, 7, 6).add();
        part(b, "snout", "head", Role.STATIC, fur).at(0, 1, -6).rot(6, 0, 0).mat("fur").box(-3, -2, -4, 6, 4, 4).box(-2, -1.5f, -6, 4, 3, 2).add();
        part(b, "nose", "snout", Role.STATIC, pink).at(0, -1.5f, -6).mat("skin").box(-1, 0, -0.6f, 2, 1, 1).add();
        part(b, "incisors", "snout", Role.STATIC, 0xE8D070).at(0, 1.5f, -5.5f).mat("bone").box(-1, 0, -0.5f, 2, 2, 1).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -1.4f, -6.1f).material("energy").glow("full")
                .box(1.4f, -0.5f, -0.4f, 2, 1, 1).box(-3.4f, -0.5f, -0.4f, 2, 1, 1);
        part(b, "jaw", "head", Role.JAW, furD).at(0, 3, -5).rot(12, 0, 0).mat("fur").box(-2, 0, -4.5f, 4, 1, 5).add();
        sym(b, "ear", "head", false, fur).accent(pink).at(3, -3, -2).rot(-10, -20, -25).mat("fur").box(-2, -4, -0.5f, 4, 4, 1).add();
        sym(b, "ear_in", "ear", true, pink).at(0, -2, -0.6f).mat("skin").box(-1.5f, -1.5f, -0.2f, 3, 3, 1).add();
        sym(b, "whiskers", "snout", false, 0xE8E0D0).at(2.5f, 0, -4.5f).rot(0, -15, 0).mat("hair")
                .box(0, -1, -0.5f, 5, 1, 1, -0.3f).box(0, 0.5f, -0.5f, 4, 1, 1, -0.3f).add();
        // legs
        sym(b, "leg_f", null, false, fur).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).at(5, 14, -7).rot(10, 0, -4).mat("fur")
                .box(-2, -2, -2.5f, 4, 6, 5).add();
        sym(b, "fore_f", "leg_f", true, furD).at(0, 4, 0).rot(-12, 0, 4).mat("fur").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "hand_f", "fore_f", true, pink).at(0, 4.5f, 0).mat("skin").box(-2, 0, -3, 4, 1, 4).box(-2, 0, -4, 1, 1, 1)
                .box(-0.5f, 0, -4, 1, 1, 1).box(1, 0, -4, 1, 1, 1).add();
        sym(b, "thigh_b", null, false, fur).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).at(6, 12, 7).rot(-25, 0, -4).mat("fur")
                .box(-2.5f, -3, -4, 5, 8, 8).add();
        sym(b, "shin_b", "thigh_b", true, furD).at(0, 5, 1).rot(55, 0, 4).mat("fur").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "foot_b", "shin_b", true, pink).at(0, 4.5f, 0).rot(-30, 0, 0).mat("skin").box(-2, 0, -5, 4, 1, 6).box(-2, 0, -6, 1, 1, 1)
                .box(-0.5f, 0, -6, 1, 1, 1).box(1, 0, -6, 1, 1, 1).add();
        // the long, ringed tail
        String tail = chain(b, "tail", "body", Role.TAIL, pink, dark(pink, 0.3f), "skin", 0, -1, 11, false,
                new int[]{3, 3, 2, 2, 2, 1, 1}, new int[]{5, 5, 5, 5, 5, 4, 4}, new float[]{-20, 6, 6, 8, 8, 10, 10},
                new float[]{0, 10, 10, 10, 10, 10, 10});
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Corporeal Beast
    // ------------------------------------------------------------------------------------------------

    /** The Corporeal Beast: a hulking spectral brute, pale and luminous, with a ridge of spines, tusks and a wide jaw. */
    static ModelSpec corp() {
        int body = 0x9AC8B8;
        int deep = 0x5A8A7A;
        int dk = 0x3A6050;
        int spirit = 0xE0FFF0;
        int horn = 0xE8EEE0;
        ModelSpec.Builder b = ModelSpec.builder("corporeal_beast").shadow(1.8f);
        part(b, "body", null, Role.BODY, body).accent(spirit).at(0, 8, 3).rot(-14, 0, 0).mat("ghost").glow("cracks")
                .box(-8, -8, -10, 16, 14, 11).box(-7, -7, 1, 14, 12, 9).add();
        part(b, "chest", "body", Role.STATIC, deep).accent(spirit).at(0, 0, -10).rot(10, 0, 0).mat("ghost").glow("cracks")
                .box(-7, -6, -3, 14, 11, 3).add();
        part(b, "belly", "body", Role.STATIC, dk).at(0, 6, 0).mat("ghost").box(-6, -0.5f, -9, 12, 1, 17).add();
        sym(b, "shoulder", "body", false, body).accent(spirit).at(8, -4, -6).rot(0, 0, -12).mat("ghost").glow("cracks")
                .box(-2.5f, -5, -5, 5, 10, 10).add();
        for (int i = 0; i < 3; i++) {
            sym(b, "ridge" + i, "body", false, deep).at(8, -1, -6 + i * 5).rot(0, 0, -5).mat("ghost").box(-0.5f, -3, -1.5f, 1, 7, 3).add();
        }
        int[] sp = {5, 7, 7, 6, 5, 4};
        for (int i = 0; i < sp.length; i++) {
            part(b, "spike" + i, "body", Role.STATIC, horn).accent(deep).at(0, -8, -8 + i * 3.4f).rot(-38, 0, 0).mat("horn")
                    .box(-1, -sp[i], -1, 2, sp[i], 2).box(-0.5f, -sp[i] - 2, -0.5f, 1, 2, 1).add();
        }
        sym(b, "side_spike", "body", false, horn).at(6, -6, -2).rot(-30, 0, -40).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.2f).add();
        // broad flat head with tusked jaw
        part(b, "head", "body", Role.HEAD, body).accent(0xFF4030).at(0, -4, -12).rot(18, 0, 0).mat("ghost")
                .box(-6, -4, -7, 12, 7, 7).add();
        part(b, "brow", "head", Role.STATIC, deep).at(0, -4, -7).rot(14, 0, 0).mat("ghost").box(-6.5f, -1, -1, 13, 3, 3).add();
        b.part("eyes", "head", Role.STATIC, 0xFF5030).pivot(0, -1.4f, -7.1f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 3, 1, 1).box(-4.5f, -0.5f, -0.4f, 3, 1, 1);
        part(b, "jaw", "head", Role.JAW, deep).accent(dk).at(0, 3, -5).rot(10, 0, 0).mat("ghost").box(-5.5f, 0, -6, 11, 3, 7).add();
        sym(b, "tusk", "jaw", false, horn).at(4.5f, 0, -5.5f).rot(-15, 0, 10).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.3f)
                .box(-0.5f, -6, 0, 1, 2, 1).add();
        teeth(b, "teeth", "jaw", horn, 0, 0, -5.8f, 7, 5, 1, true);
        sym(b, "horn", "head", false, horn).at(4.5f, -4, -3).rot(-20, 0, -35).mat("horn").box(-1, -5, -1, 2, 5, 2).add();
        sym(b, "horn_tip", "horn", true, light(horn, 0.1f)).at(0, -5, 0).rot(-20, 0, 25).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        // thick, columnar legs
        sym(b, "leg_f", null, false, body).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(spirit).at(7.5f, 10, -7).rot(6, 0, -4)
                .mat("ghost").glow("cracks").box(-3.5f, -2, -3.5f, 7, 8, 7).add();
        sym(b, "fore_f", "leg_f", true, deep).accent(spirit).at(0, 6, 0).rot(-6, 0, 4).mat("ghost").box(-3, 0, -3, 6, 6, 6).add();
        sym(b, "paw_f", "fore_f", true, dk).at(0, 5.5f, 0).mat("ghost").box(-3.5f, 0, -4.5f, 7, 2, 7).add();
        sym(b, "claws_f", "paw_f", true, horn).at(0, 1, -4.5f).mat("horn").box(-3, -0.5f, -1.5f, 1, 1, 2).box(-0.5f, -0.5f, -2, 1, 1, 2)
                .box(2, -0.5f, -1.5f, 1, 1, 2).add();
        sym(b, "thigh_b", null, false, body).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(spirit).at(6.5f, 11, 8).rot(-10, 0, -4)
                .mat("ghost").glow("cracks").box(-3, -3, -3.5f, 6, 8, 7).add();
        sym(b, "shin_b", "thigh_b", true, deep).at(0, 5, 0).rot(10, 0, 4).mat("ghost").box(-2.5f, 0, -2.5f, 5, 5, 5).add();
        sym(b, "paw_b", "shin_b", true, dk).at(0, 4.5f, 0).mat("ghost").box(-3, 0, -4, 6, 2, 6).add();
        sym(b, "claws_b", "paw_b", true, horn).at(0, 1, -4).mat("horn").box(-2.5f, -0.5f, -1.5f, 1, 1, 2).box(1.5f, -0.5f, -1.5f, 1, 1, 2).add();
        String tail = chain(b, "tail", "body", Role.TAIL, body, spirit, "ghost", 0, -3, 9, false,
                new int[]{4, 3, 2}, new int[]{5, 4, 4}, new float[]{-25, 10, 10}, null);
        part(b, "tail_spike", tail, Role.STATIC, horn).at(0, 0, 4).mat("horn").box(-0.5f, -0.5f, 0, 1, 1, 3).box(-1.5f, -2, 0, 3, 1, 1).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Kalphite Queen
    // ------------------------------------------------------------------------------------------------

    /** Kalphite Queen, first form: a vast armoured insect crawling on six legs, scything forelimbs, a swollen abdomen. */
    static ModelSpec kalphiteQueen() {
        int shell = 0x8A8A3A;
        int pale = 0xC8B860;
        int dk = 0x4A4A20;
        int eye = 0x60E040;
        ModelSpec.Builder b = ModelSpec.builder("kalphite_queen").shadow(1.5f);
        part(b, "thorax", null, Role.BODY, shell).accent(pale).at(0, 12, -2).rot(-18, 0, 0).mat("chitin")
                .box(-5.5f, -6, -5, 11, 10, 10).box(-4.5f, -7, -4, 9, 1, 8).add();
        part(b, "collar", "thorax", Role.STATIC, dk).accent(pale).at(0, -2, -5).mat("chitin").box(-6, -4, -1, 12, 7, 2).add();
        // segmented abdomen
        int[] aw = {13, 15, 14, 12, 9};
        String prev = "thorax";
        for (int i = 0; i < aw.length; i++) {
            String n = "abdomen" + i;
            part(b, n, prev, Role.STATIC, i % 2 == 0 ? shell : light(shell, 0.08f)).accent(dk).at(0, i == 0 ? 0 : 0, i == 0 ? 5 : 3.5f)
                    .rot(i == 0 ? 28 : 4, 0, 0).mat("chitin").box(-aw[i] / 2f, -aw[i] / 2f + 1, 0, aw[i], aw[i] - 2, 4).add();
            prev = n;
        }
        part(b, "stinger", prev, Role.TAIL, dk).at(0, 1, 4).rot(-10, 0, 0).mat("horn").box(-1.5f, -1.5f, 0, 3, 3, 3).box(-0.5f, -0.5f, 3, 1, 1, 3).add();
        for (int i = 0; i < 4; i++) {
            part(b, "abd_spike" + i, "abdomen" + i, Role.STATIC, pale).at(0, -aw[i] / 2f + 1, 2).rot(-30, 0, 0).mat("horn")
                    .box(-0.5f, -3, -0.5f, 1, 3, 2).add();
        }
        // head
        part(b, "head", "thorax", Role.HEAD, shell).accent(eye).at(0, -5, -5).rot(30, 0, 0).mat("chitin").box(-4, -4, -5, 8, 6, 6).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -2.5f, -5.1f).material("energy").glow("full")
                .box(1.5f, -0.5f, -0.4f, 2, 2, 1).box(-3.5f, -0.5f, -0.4f, 2, 2, 1);
        part(b, "crest", "head", Role.STATIC, pale).accent(dk).at(0, -4, -1).rot(-35, 0, 0).mat("chitin")
                .box(-3.5f, -4, -1, 7, 4, 3).box(-2.5f, -6, 0, 5, 2, 2).add();
        sym(b, "crest_horn", "crest", false, pale).at(3, -4, 0.5f).rot(0, 0, -20).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1).add();
        sym(b, "mandible", "head", false, dk).roles(Role.JAW, Role.JAW).at(2.2f, 1.5f, -5).rot(10, 25, 0).mat("horn")
                .box(-0.5f, -0.5f, -4, 1, 1, 4, 0.2f).box(-1.5f, -0.5f, -4.5f, 1, 1, 1).add();
        sym(b, "antenna", "head", false, dk).at(1.5f, -4, -4).rot(-45, -18, 0).mat("horn").box(-0.5f, -8, -0.5f, 1, 8, 1, -0.1f)
                .box(-0.5f, -9, -1.5f, 1, 1, 2).add();
        // scything forelimbs
        sym(b, "scythe", "thorax", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dk).at(5, -4, -4).rot(-50, 0, -18).mat("chitin")
                .box(-1.5f, -1, -1.5f, 3, 9, 3).add();
        sym(b, "scythe_fore", "scythe", true, shell).at(0, 8, 0).rot(-80, 0, 0).mat("chitin").box(-1, 0, -1, 2, 7, 2).add();
        sym(b, "scythe_blade", "scythe_fore", true, pale).at(0, 7, 0).rot(-90, 0, 0).mat("horn").box(-0.5f, 0, -1.5f, 1, 9, 3, 0.1f)
                .box(-0.5f, 2, 1.5f, 1, 1, 1).box(-0.5f, 5, 1.5f, 1, 1, 1).add();
        // six legs
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "thorax", false, shell).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(dk).at(5, 2, -2 + i * 3)
                    .rot(25, -(i - 1) * 30f, -40).mat("chitin").box(0, -1, -1, 8, 2, 2).add();
            sym(b, "shin" + i, "leg" + i, true, dk).at(8, 0, 0).rot(0, 0, 85).mat("chitin").box(0, -1, -1, 11, 2, 2, -0.2f)
                    .box(10, -0.5f, -0.5f, 2, 1, 1).add();
            sym(b, "knee" + i, "leg" + i, true, pale).at(8, 0, 0).mat("horn").box(-1, -1.5f, -1.5f, 2, 3, 3).add();
        }
        // folded wings on the back
        sym(b, "wing", "thorax", false, 0xE8E0B0).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(pale).at(3, -6, 2).rot(-28, -12, -12)
                .mat("membrane").box(-1, -0.5f, 0, 5, 1, 18, -0.25f).add();
        return b.build();
    }

    /** Kalphite Queen, airborne form: risen upright, crowned head, four great wings and twin scythes. */
    static ModelSpec kalphiteQueenAirborne() {
        int shell = 0x6A7A3A;
        int pale = 0xC0B058;
        int dk = 0x3A4420;
        int eye = 0x80FF50;
        int wing = 0xD8E0C0;
        ModelSpec.Builder b = ModelSpec.builder("kalphite_queen_airborne").shadow(1.5f);
        part(b, "thorax", null, Role.BODY, shell).accent(pale).at(0, 9, 0).rot(10, 0, 0).mat("chitin")
                .box(-6, -15, -5, 12, 16, 10).box(-5, -12, -6, 10, 10, 1).add();
        for (int i = 0; i < 4; i++) {
            part(b, "plate" + i, "thorax", Role.STATIC, i % 2 == 0 ? pale : light(shell, 0.1f)).accent(dk).at(0, -13 + i * 3.5f, -5.5f)
                    .mat("chitin").box(-4.5f + i * 0.25f, 0, -0.8f, 9 - i / 2 * 1, 3, 1).add();
        }
        sym(b, "pauldron", "thorax", false, dk).accent(pale).at(6, -13, 0).rot(0, 0, -20).mat("chitin").box(-2, -2, -4, 5, 4, 8).add();
        sym(b, "pauldron_spike", "pauldron", true, pale).at(2, -2, 0).rot(0, 0, -30).mat("horn").box(-0.5f, -4, -0.5f, 1, 4, 1, 0.2f).add();
        // hanging abdomen
        int[] aw = {12, 14, 12, 9};
        String prev = "thorax";
        for (int i = 0; i < aw.length; i++) {
            String n = "abdomen" + i;
            part(b, n, prev, i == 3 ? Role.TAIL : Role.STATIC, i % 2 == 0 ? 0x7A8A40 : shell).accent(dk).at(0, i == 0 ? 0 : 0, i == 0 ? 4 : 4)
                    .rot(i == 0 ? 40 : 12, 0, 0).mat("chitin").box(-aw[i] / 2f, -aw[i] / 2f + 1, 0, aw[i], aw[i] - 2, 4).add();
            prev = n;
        }
        // crowned head
        part(b, "head", "thorax", Role.HEAD, shell).accent(eye).at(0, -15, -1).rot(-6, 0, 0).mat("chitin").box(-4.5f, -7, -6, 9, 7, 8).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -4.5f, -6.1f).material("energy").glow("full")
                .box(1.5f, -1, -0.4f, 2, 2, 1).box(-3.5f, -1, -0.4f, 2, 2, 1);
        part(b, "crown", "head", Role.STATIC, pale).accent(dk).at(0, -7, -1).rot(-15, 0, 0).mat("chitin")
                .box(-4, -2, -2, 8, 2, 6).box(-0.5f, -6, -1, 1, 4, 2).add();
        sym(b, "crown_horn", "crown", false, pale).at(3, -2, 0).rot(-10, 0, -25).mat("horn").box(-0.5f, -5, -0.5f, 1, 5, 1, 0.2f).add();
        sym(b, "mandible", "head", false, pale).roles(Role.JAW, Role.JAW).at(2.5f, -0.5f, -6).rot(15, 25, 0).mat("horn")
                .box(-0.5f, -0.5f, -5, 1, 1, 5, 0.2f).box(-1.5f, -0.5f, -5.5f, 1, 1, 1).add();
        sym(b, "antenna", "head", false, dk).at(2, -7, -5).rot(-30, -15, 0).mat("horn").box(-0.5f, -10, -0.5f, 1, 10, 1, -0.1f).add();
        // twin scythes
        sym(b, "arm", "thorax", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dk).at(6.5f, -12, -2).rot(-20, 0, -15).mat("chitin")
                .box(-1.5f, 0, -1.5f, 3, 10, 3).add();
        sym(b, "fore", "arm", true, dk).at(0, 10, 0).rot(-60, 0, 0).mat("chitin").box(-1.2f, 0, -1.2f, 2, 8, 2, 0.2f).add();
        sym(b, "scythe", "fore", true, pale).at(0, 8, 0).rot(-110, 0, 0).mat("horn").box(-0.5f, 0, -1.5f, 1, 13, 3, 0.1f)
                .box(-0.5f, 3, 1.5f, 1, 1, 1).box(-0.5f, 7, 1.5f, 1, 1, 1).box(-0.5f, 11, -2.5f, 1, 2, 1).add();
        // four legs
        for (int i = 0; i < 2; i++) {
            sym(b, "leg" + i, "thorax", false, shell).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(dk).at(5, -1, -2 + i * 4)
                    .rot(0, -(i - 0.5f) * 40f, -30).mat("chitin").box(0, -1, -1, 7, 2, 2).add();
            sym(b, "shin" + i, "leg" + i, true, dk).at(7, 0, 0).rot(0, 0, 90).mat("chitin").box(0, -1, -1, 12, 2, 2, -0.2f)
                    .box(11, -0.5f, -0.5f, 2, 1, 1).add();
        }
        // two pairs of veined wings
        sym(b, "wing", "thorax", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(dk).at(4, -13, 4).rot(-20, -40, -50)
                .mat("membrane").box(0, -0.5f, -1, 22, 1, 9, -0.25f).box(20, -0.5f, 0, 4, 1, 6, -0.25f).add();
        sym(b, "wing_vein", "wing", true, dk).at(0, -0.4f, 0).mat("horn").box(0, -0.5f, -1, 22, 1, 1, -0.2f).add();
        sym(b, "wing_lo", "thorax", false, light(wing, 0.1f)).accent(dk).at(4, -9, 4).rot(-5, -55, -25).mat("membrane")
                .box(0, -0.5f, -1, 17, 1, 7, -0.25f).add();
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Spiders: Venenatis / Spindel, Araxxor, Sarachnis
    // ------------------------------------------------------------------------------------------------

    /** A giant spider boss: armoured cephalothorax, cluster of glowing eyes, fangs, a great marked abdomen, eight legs. */
    static ModelSpec spider(String id, int cara, int abdomen, int mark, int eye, int style) {
        int dk = dark(cara, 0.4f);
        int joint = style == ARAXXOR ? mark : light(cara, 0.15f);
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.6f);
        part(b, "body", null, Role.BODY, cara).accent(mark).at(0, 12, -3).rot(-4, 0, 0).mat("chitin")
                .box(-6, -4, -7, 12, 7, 11).box(-5, -5, -6, 10, 1, 9).add();
        part(b, "groove", "body", Role.STATIC, mark).accent(dk).at(0, -5.2f, -2).mat("chitin").box(-0.5f, -0.5f, -3, 1, 1, 6).add();
        // head and eyes
        part(b, "head", "body", Role.HEAD, cara).accent(eye).at(0, -1, -7).rot(8, 0, 0).mat("chitin").box(-4, -3, -4, 8, 5, 4).add();
        b.part("eyes", "head", Role.STATIC, eye).pivot(0, -2.2f, -4.1f).material("energy").glow("full")
                .box(0.5f, -0.5f, -0.4f, 2, 2, 1).box(-2.5f, -0.5f, -0.4f, 2, 2, 1)
                .box(2.8f, -0.8f, -0.2f, 1, 1, 1).box(-3.8f, -0.8f, -0.2f, 1, 1, 1)
                .box(1.2f, -1.8f, -0.2f, 1, 1, 1).box(-2.2f, -1.8f, -0.2f, 1, 1, 1)
                .box(3, 1, -0.2f, 1, 1, 1).box(-4, 1, -0.2f, 1, 1, 1);
        sym(b, "chelicera", "head", false, dk).roles(Role.JAW, Role.JAW).at(1.6f, 1.5f, -3.5f).rot(18, 0, 0).mat("chitin")
                .box(-1, 0, -1, 2, 3, 2).add();
        sym(b, "fang", "chelicera", true, style == ARAXXOR ? mark : 0xE8E0C8).at(0, 3, 0).rot(35, 0, 0).mat("horn")
                .box(-0.5f, 0, -0.5f, 1, 3, 1, 0.1f).add();
        sym(b, "palp", "head", false, cara).accent(dk).at(3.2f, 1, -3).rot(30, -25, 0).mat("chitin").box(-0.5f, 0, -0.5f, 1, 4, 1, 0.2f)
                .box(-1, 4, -1, 2, 2, 2).add();
        // abdomen
        part(b, "abdomen", "body", Role.STATIC, abdomen).accent(mark).at(0, -1, 4).rot(8, 0, 0)
                .mat(style == VENENATIS ? "fur" : "chitin").glow(style == ARAXXOR ? "cracks" : null)
                .box(-7, -5, 0, 14, 10, 13).box(-5.5f, -6.5f, 1.5f, 11, 2, 10).box(-8, -3.5f, 2, 16, 7, 9).box(-5, -3.5f, 13, 10, 7, 3)
                .box(-4, 5, 2, 8, 1, 9).add();
        part(b, "spinneret", "abdomen", Role.TAIL, dk).at(0, 1, 17).mat("chitin").box(-1.5f, -1.5f, 0, 3, 3, 2).add();
        switch (style) {
            case VENENATIS -> {
                // hourglass marking and bristles
                part(b, "mark", "abdomen", Role.STATIC, mark).accent(dark(mark, 0.4f)).at(0, -6, 0).mat("hide").glow("full")
                        .box(-3, -0.6f, 2, 6, 1, 2).box(-1.5f, -0.6f, 4, 3, 1, 3).box(-0.5f, -0.6f, 7, 1, 1, 1).box(-1.5f, -0.6f, 8, 3, 1, 2)
                        .box(-3, -0.6f, 10, 6, 1, 2).add();
                for (int i = 0; i < 4; i++) {
                    sym(b, "bristle" + i, "abdomen", false, dk).at(6, -5 + (i % 2), 2 + i * 3).rot(-30, 0, -40).mat("hair")
                            .box(-0.5f, -3, -0.5f, 1, 3, 1, -0.1f).add();
                }
            }
            case ARAXXOR -> {
                // armoured ridge plates and spikes over a glowing acid abdomen
                for (int i = 0; i < 4; i++) {
                    part(b, "plate" + i, "abdomen", Role.STATIC, cara).accent(mark).at(0, -6, 1 + i * 3.5f).rot(-6, 0, 0).mat("chitin")
                            .box(-6 + i, -1, 0, 12 - 2 * i, 2, 3).box(-0.5f, -4 + i / 2, 0.5f, 1, 3 - i / 2, 2).add();
                }
                for (int i = 0; i < 3; i++) {
                    sym(b, "spike" + i, "abdomen", false, mark).at(7, -3, 3 + i * 4).rot(-20, 0, -50).mat("horn")
                            .box(-0.5f, -4, -0.5f, 1, 4, 1, 0.2f).add();
                }
            }
            default -> {
                // Sarachnis: banded abdomen with bone spines
                part(b, "bands", "abdomen", Role.STATIC, mark).accent(dark(mark, 0.4f)).at(0, -6, 0).mat("chitin")
                        .box(-7, -0.6f, 3, 14, 1, 2).box(-6.5f, -0.6f, 7, 13, 1, 2).box(-5.5f, -0.6f, 11, 11, 1, 2).add();
                for (int i = 0; i < 3; i++) {
                    part(b, "bone_spine" + i, "abdomen", Role.STATIC, 0xE8DCC0).at((i - 1) * 3.5f, -6, 5 + Math.abs(i - 1) * 2)
                            .rot(-25, 0, (i - 1) * -20).mat("bone").box(-1, -4, -1, 2, 4, 2).box(-0.5f, -6, -0.5f, 1, 2, 1).add();
                }
            }
        }
        // eight legs
        float[] yaw = {42, 14, -14, -42};
        for (int i = 0; i < 4; i++) {
            sym(b, "leg" + i, "body", false, cara).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(mark).at(5.5f, 0.5f, -5 + i * 2.8f)
                    .rot(0, yaw[i], -38).mat("chitin").box(0, -1, -1, 9, 2, 2).add();
            sym(b, "knee" + i, "leg" + i, true, joint).at(9, 0, 0).mat("chitin").box(-1, -1.5f, -1.5f, 2, 3, 3).add();
            sym(b, "tibia" + i, "leg" + i, true, cara).at(9, 0, 0).rot(0, 0, 78).mat("chitin").box(0, -1, -1, 9, 2, 2, -0.2f).add();
            sym(b, "tarsus" + i, "tibia" + i, true, dk).at(9, 0, 0).rot(0, 0, 32).mat("chitin").box(0, -0.5f, -0.5f, 6, 1, 1, 0.25f)
                    .box(6, -0.5f, -0.5f, 2, 1, 1).add();
            if (style == ARAXXOR && i < 2) {
                sym(b, "leg_spike" + i, "tibia" + i, true, mark).at(3, -1, 0).rot(0, 0, -50).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
            }
        }
        return b.build();
    }

    // ------------------------------------------------------------------------------------------------
    // Scorpia
    // ------------------------------------------------------------------------------------------------

    /** Scorpia: a giant black scorpion with plated segments, crushing pincers and a venom-tipped tail arched high. */
    static ModelSpec scorpia() {
        int shell = 0x2A2A2E;
        int edge = 0x4A3A30;
        int dk = 0x16161A;
        int venom = 0xE04020;
        ModelSpec.Builder b = ModelSpec.builder("scorpia").shadow(1.2f);
        part(b, "thorax", null, Role.BODY, shell).accent(edge).at(0, 17, 0).mat("chitin").box(-5, -3, -6, 10, 5, 12).add();
        for (int i = 0; i < 4; i++) {
            part(b, "plate" + i, "thorax", Role.STATIC, i % 2 == 0 ? shell : light(shell, 0.08f)).accent(edge).at(0, -3, -5 + i * 3)
                    .rot(-4, 0, 0).mat("chitin").box(-5.5f + i * 0.25f, -1, 0, 11 - i / 2, 1, 4).add();
        }
        sym(b, "flank", "thorax", false, edge).at(5, -1, 0).mat("chitin").box(-0.5f, -1.5f, -5, 1, 3, 10).add();
        part(b, "head", "thorax", Role.HEAD, shell).accent(venom).at(0, -0.5f, -6).rot(4, 0, 0).mat("chitin").box(-3.5f, -2, -3, 7, 3, 3).add();
        b.part("eyes", "head", Role.STATIC, venom).pivot(0, -2.2f, -1.5f).material("energy").glow("full")
                .box(0.3f, -0.5f, -0.5f, 1, 1, 1).box(-1.3f, -0.5f, -0.5f, 1, 1, 1).box(2.3f, 0, 0.5f, 1, 1, 1).box(-3.3f, 0, 0.5f, 1, 1, 1);
        sym(b, "mandible", "head", false, edge).roles(Role.JAW, Role.JAW).at(1.2f, 0.5f, -3).rot(10, 15, 0).mat("horn")
                .box(-0.5f, -0.5f, -2, 1, 1, 2).add();
        // pincers
        sym(b, "arm", "thorax", false, shell).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(edge).at(4, 0, -5).rot(0, 35, -10).mat("chitin")
                .box(0, -1.5f, -1.5f, 6, 3, 3).add();
        sym(b, "forearm", "arm", true, shell).accent(edge).at(6, 0, 0).rot(0, 70, 8).mat("chitin").box(0, -1.5f, -1.5f, 5, 3, 3).add();
        sym(b, "claw", "forearm", true, shell).accent(edge).at(5, 0, 0).rot(0, -15, 0).mat("chitin").box(0, -2.5f, -2.5f, 6, 5, 5).add();
        sym(b, "finger", "claw", true, dk).at(6, 0, 1.5f).rot(0, -10, 0).mat("horn").box(0, -1.5f, -1, 5, 3, 2).box(4, -1, -1.5f, 2, 2, 1).add();
        sym(b, "thumb", "claw", true, dk).roles(Role.JAW, Role.JAW).at(6, 0, -1.5f).rot(0, 18, 0).mat("horn").box(0, -1, -1, 4, 2, 2).add();
        // tail arched over the back
        int[] tw = {4, 4, 3, 3, 3, 2};
        float[] tp = {38, 30, 30, 28, 26, 22};
        String prev = "thorax";
        for (int i = 0; i < tw.length; i++) {
            String n = "tail" + i;
            part(b, n, prev, Role.TAIL, i % 2 == 0 ? shell : light(shell, 0.06f)).accent(edge).at(0, i == 0 ? -1 : 0, i == 0 ? 6 : 4)
                    .rot(tp[i], 0, 0).mat("chitin").box(-tw[i] / 2f, -tw[i] / 2f, 0, tw[i], tw[i], 4).add();
            part(b, "tail_ridge" + i, n, Role.STATIC, edge).at(0, -tw[i] / 2f, 2).mat("horn").box(-0.5f, -1, -1, 1, 1, 2).add();
            prev = n;
        }
        part(b, "sting_bulb", prev, Role.STATIC, edge).accent(venom).at(0, 0, 4).rot(30, 0, 0).mat("chitin").box(-1.5f, -1.5f, 0, 3, 3, 3).add();
        part(b, "sting", "sting_bulb", Role.STATIC, venom).at(0, 1, 3).rot(60, 0, 0).mat("energy").glow("full")
                .box(-0.5f, -0.5f, 0, 1, 1, 3).box(-0.5f, -0.5f, -0.5f, 1, 2, 1).add();
        // walking legs (three pairs)
        float[] yaw = {24, -2, -28};
        for (int i = 0; i < 3; i++) {
            sym(b, "leg" + i, "thorax", false, shell).roles(Role.LEG_EXTRA, Role.LEG_EXTRA).accent(edge).at(4.5f, 0.5f, -3f + i * 3.2f)
                    .rot(0, yaw[i], -30).mat("chitin").box(0, -0.5f, -0.5f, 5, 1, 1, 0.3f).add();
            sym(b, "foot" + i, "leg" + i, true, dk).at(5, 0, 0).rot(0, 0, 75).mat("chitin").box(0, -0.5f, -0.5f, 5, 1, 1, 0.15f)
                    .box(5, -0.5f, -0.5f, 1, 1, 1).add();
        }
        return b.build();
    }
}
