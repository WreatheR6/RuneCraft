package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

/**
 * Round 26 re-sculpts of common slayer creatures, part 5. Same rules as {@link Common1}..{@link Common4}: each spec was
 * rendered before/after against the model it replaces and kept only where it reads clearly closer to the OSRS model.
 * Ids, hitboxes and everything outside the model are unchanged. Registered last in {@link Overrides}.
 */
final class Common5 {
    private Common5() {
    }

    static List<ModelSpec> overrides() {
        List<ModelSpec> l = new ArrayList<>();
        l.add(hellhound("hellhound"));
        l.add(hellhound("catacomb_hellhound"));
        l.add(nechryael());
        l.add(mutatedBloodveld());
        return l;
    }

    /**
     * TzHaar NPCs (Mor Ul Rek shopkeepers, the Fight Cave/Fight Pit officials and TzHaar-Ket-Rak) are drawn on the same
     * shared TzHaar body as the TzHaar monsters of their caste (the caste is the first word after "TzHaar-").
     */
    static List<ModelSpec.Variant> variants() {
        return List.of(
                ModelSpec.Variant.of("tzhaar_hur", "tzhaar_hur_citizen"),
                ModelSpec.Variant.of("mg_tzhaar_hur_tel", "tzhaar_hur_citizen"),
                ModelSpec.Variant.of("mg_tzhaar_mej_jal", "tzhaar_mej"),
                ModelSpec.Variant.of("mg_tzhaar_mej_kah", "tzhaar_mej"),
                ModelSpec.Variant.of("mg_tzhaar_ket_keh", "tzhaar_ket"),
                ModelSpec.Variant.of("mg_tzhaar_ket_rak", "tzhaar_ket"));
    }

    private static final int TOOTH = 0xF0EADA;
    private static final int CLAW = 0x2A2220;

    /**
     * Hellhound ("Hello, nice doggy..."): a big, lean, near-black demonic hound with dark red highlights; a deep chest
     * and narrow waist, a ridge of bony spikes from the nape down the spine, a heavy wolfish head with swept-back
     * pointed ears, glowing orange eyes and a fanged muzzle, long legs with hocked hind legs and a thin whip tail.
     */
    static ModelSpec hellhound(String id) {
        int hide = 0x3C1512;
        int dark = 0x1C0A08;
        int light = 0x6A2418;
        int spike = 0x2E1E1A;
        ModelSpec.Builder b = ModelSpec.builder(id).shadow(1.0f);
        part(b, "body", null, Role.BODY, hide).accent(dark).at(0, 11, 0).mat("fur")
                .box(-5, -5, -9, 10, 10, 9)
                .box(-4, -4, 0, 8, 7, 9).add();
        part(b, "chest", "body", Role.STATIC, light).accent(hide).at(0, 3, -9).mat("fur").box(-3.5f, -5, -0.6f, 7, 6, 1).add();
        part(b, "spikes", "body", Role.STATIC, spike).accent(dark).at(0, -5, 0).mat("horn")
                .box(-1, -4, -8, 2, 4, 2).box(-1, -4, -5, 2, 4, 2).box(-1, -3, -2, 2, 3, 2)
                .box(-0.5f, -2, 1, 1, 3, 2).box(-0.5f, -2, 4, 1, 2, 2).add();
        part(b, "neck", "body", Role.NECK, hide).accent(dark).at(0, -2, -8).rot(-38, 0, 0).mat("fur")
                .box(-3.5f, -3.5f, -6, 7, 7, 6).add();
        part(b, "neck_spikes", "neck", Role.STATIC, spike).at(0, -3.5f, -2).mat("horn")
                .box(-1, -4, -3, 2, 4, 2).box(-1, -3, 0, 2, 3, 2).add();
        part(b, "head", "neck", Role.HEAD, hide).accent(dark).at(0, 0, -5.5f).rot(38, 0, 0).mat("fur").pattern("blank")
                .box(-4, -4, -6, 8, 7, 6).add();
        part(b, "eyes", "head", Role.STATIC, 0xFF9A1A).accent(0xFFC040).at(0, -2.5f, -6).mat("flesh").glow("full")
                .box(-3, -0.5f, -0.4f, 2, 1, 1).box(1, -0.5f, -0.4f, 2, 1, 1).add();
        part(b, "snout", "head", Role.STATIC, hide).accent(dark).at(0, 0, -6).mat("fur").box(-2.5f, -1, -5, 5, 3, 5).add();
        part(b, "nose", "snout", Role.STATIC, dark).at(0, -1, -5).mat("skin").box(-1.5f, -0.2f, -0.4f, 3, 1, 1).add();
        BestiaryBst.teeth(b, "fangs_up", "snout", TOOTH, 0, 2, -4.4f, 5, 3, 1, false);
        part(b, "jaw", "head", Role.JAW, dark).accent(hide).at(0, 3, -5.5f).rot(16, 0, 0).mat("fur").box(-2.5f, 0, -5, 5, 2, 5).add();
        BestiaryBst.teeth(b, "fangs_lo", "jaw", TOOTH, 0, 0, -4.4f, 5, 3, 1, true);
        sym(b, "ear", "head", false, dark).accent(hide).at(2.8f, -4, -1).rot(-50, 0, 15).mat("fur").box(-1, -3, -1, 2, 3, 2).add();
        sym(b, "ear_tip", "ear", true, dark).at(0, -3, 0).rot(-20, 0, 0).mat("fur").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        // long front legs
        sym(b, "leg_f", "body", false, hide).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(3.6f, 3, -6)
                .mat("fur").box(-2, -1, -2, 4, 7, 4).add();
        sym(b, "shin_f", "leg_f", true, dark).at(0, 6, 0).mat("fur").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        sym(b, "paw_f", "shin_f", true, dark).at(0, 4, 0).mat("fur").box(-2, 0, -3, 4, 1, 4).add();
        sym(b, "claw_f", "paw_f", true, CLAW).at(0, 0, -3).mat("horn").box(-2, 0, -1, 1, 1, 1).box(1, 0, -1, 1, 1, 1).add();
        // hocked hind legs
        sym(b, "leg_b", "body", false, hide).roles(Role.LEG_BACK_LEFT, Role.LEG_BACK_RIGHT).accent(dark).at(3.4f, 1, 6.5f).rot(-18, 0, 0)
                .mat("fur").box(-2.5f, -2, -3, 5, 7, 5).add();
        sym(b, "hock", "leg_b", true, dark).at(0, 5, 0.5f).rot(42, 0, 0).mat("fur").box(-1.5f, 0, -1.5f, 3, 4, 3).add();
        sym(b, "meta", "hock", true, dark).at(0, 4, 0).rot(-24, 0, 0).mat("fur").box(-1.5f, 0, -1, 3, 3, 2).add();
        sym(b, "paw_b", "meta", true, dark).at(0, 3, 0).mat("fur").box(-2, 0, -2.5f, 4, 1, 4).add();
        // thin whip tail
        part(b, "tail", "body", Role.TAIL, hide).accent(dark).at(0, -2, 9).rot(-35, 0, 0).mat("fur").box(-1, -1, 0, 2, 2, 7).add();
        part(b, "tail_tip", "tail", Role.STATIC, dark).at(0, 0, 7).rot(22, 0, 0).mat("fur").box(-0.5f, -0.5f, 0, 1, 1, 6)
                .box(-0.5f, -1.5f, 4, 1, 1, 2).add();
        return b.build();
    }

    /**
     * Nechryael ("An evil death demon."): a hovering, legless demon of dark reddish flesh. A barrel torso with heavy
     * shoulders and a sagging paler belly tapers below into a curled, fleshy tail; long arms hang to clawed hands, a
     * pair of small bat wings sprout from its back and its blunt, fanged head has two short horns and glowing eyes.
     */
    static ModelSpec nechryael() {
        int flesh = 0x94402E;
        int dark = 0x561C14;
        int belly = 0xB8745A;
        int wing = 0x5E241C;
        int horn = 0xD8C8A0;
        ModelSpec.Builder b = ModelSpec.builder("nechryael").shadow(0.8f);
        part(b, "torso", null, Role.FLOAT, flesh).accent(dark).at(0, 9, 0).mat("flesh")
                .box(-6, -6, -4, 12, 11, 8)
                .box(-7.5f, -6.5f, -3, 15, 4, 6).add();
        part(b, "belly", "torso", Role.STATIC, belly).accent(flesh).at(0, 0, -4).mat("flesh").box(-4.5f, -2, -1.5f, 9, 7, 2).add();
        part(b, "abdomen", "torso", Role.STATIC, flesh).accent(dark).at(0, 5, 0).mat("flesh").box(-4.5f, 0, -3, 9, 3, 6).add();
        part(b, "tail", "abdomen", Role.TAIL, flesh).accent(dark).at(0, 3, 0).rot(22, 0, 0).mat("flesh").box(-3, 0, -2.5f, 6, 4, 5).add();
        part(b, "tail_mid", "tail", Role.STATIC, dark).accent(flesh).at(0, 4, 0).rot(28, 0, 0).mat("flesh").box(-2, 0, -1.5f, 4, 4, 3).add();
        part(b, "tail_tip", "tail_mid", Role.STATIC, dark).at(0, 4, 0).rot(35, 0, 0).mat("flesh").box(-1, 0, -1, 2, 3, 2).add();
        part(b, "head", "torso", Role.HEAD, flesh).accent(0xFFD040).at(0, -6, -2.5f).mat("flesh").pattern("eyes").glow("eyes")
                .box(-3.5f, -6, -4.5f, 7, 6, 6).add();
        part(b, "brow", "head", Role.STATIC, dark).at(0, -4.5f, -4.5f).mat("flesh").box(-3.5f, -1, -0.8f, 7, 1, 1).add();
        part(b, "jaw", "head", Role.JAW, dark).accent(flesh).at(0, -0.5f, -3).rot(12, 0, 0).mat("flesh").box(-3, 0, -2, 6, 2, 4).add();
        BestiaryBst.teeth(b, "teeth_lo", "jaw", TOOTH, 0, 0, -1.6f, 5, 3, 1, true);
        sym(b, "horn", "head", false, horn).at(2.5f, -5.5f, -1.5f).rot(-35, 0, 28).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        // long arms with clawed hands
        sym(b, "arm", "torso", false, flesh).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(7.5f, -4.5f, 0).rot(-12, 0, -14)
                .mat("flesh").box(-2, -1, -2, 4, 8, 4).add();
        sym(b, "forearm", "arm", true, flesh).accent(dark).at(0, 7, 0).rot(-28, 0, 8).mat("flesh").box(-1.5f, 0, -1.5f, 3, 7, 3).add();
        sym(b, "hand", "forearm", true, dark).at(0, 7, 0).mat("flesh").box(-2, 0, -2, 4, 2, 4).add();
        sym(b, "claws", "hand", true, CLAW).at(0, 2, 0).mat("horn").box(-2, 0, -2, 1, 2, 1).box(-0.5f, 0, -2, 1, 2, 1)
                .box(1, 0, -2, 1, 2, 1).add();
        // small bat wings on the back
        sym(b, "wing", "torso", false, wing).roles(Role.WING_LEFT, Role.WING_RIGHT).accent(dark).at(3, -5, 4).rot(-15, 0, -50)
                .mat("membrane").box(0, -1, 0, 1, 7, 6).add();
        sym(b, "wing_tip", "wing", true, wing).accent(dark).at(0.5f, -1, 6).rot(30, 0, 0).mat("membrane").box(-0.5f, 0, 0, 1, 5, 4).add();
        sym(b, "wing_bone", "wing", true, dark).at(0.5f, -1, 0).mat("bone").box(-0.5f, -1, 0, 1, 1, 9).add();
        return b.build();
    }

    /**
     * Mutated Bloodveld (Catacombs of Kourend): the bloodveld body, but bigger, leaner and a darker, purplish
     * crimson, with a swollen exposed brain, a ridge of bony spurs down its hunched back, longer clawed arms and an
     * even longer tongue lolling from its fanged maw.
     */
    static ModelSpec mutatedBloodveld() {
        int flesh = 0x8A2A3A;
        int dark = 0x4E1220;
        int brain = 0xD8869A;
        int gum = 0x5A1020;
        int spur = 0xD8C4A8;
        ModelSpec.Builder b = ModelSpec.builder("mutated_bloodveld").scale(1.15f).shadow(0.9f);
        part(b, "hips", null, Role.BODY, flesh).accent(dark).at(0, 12, 1).mat("flesh").box(-4, -3, -3, 8, 5, 6).add();
        part(b, "torso", "hips", Role.STATIC, flesh).accent(dark).at(0, -2, 0).rot(40, 0, 0).mat("flesh").glow("veins")
                .box(-6, -13, -5, 12, 13, 10).add();
        part(b, "brain", "torso", Role.STATIC, brain).accent(dark).at(0, -13, 0.5f).mat("flesh")
                .box(-5.5f, -4, -5, 11, 4, 9).box(-0.5f, -4.6f, -4.5f, 1, 1, 8).add();
        part(b, "brain_folds", "brain", Role.STATIC, 0xB86A80).at(0, -4, 0).mat("flesh")
                .box(-4, -0.6f, -3.5f, 2, 1, 7).box(2, -0.6f, -3.5f, 2, 1, 7).add();
        part(b, "spurs", "torso", Role.STATIC, spur).at(0, -7, 5).mat("horn")
                .box(-1, -4, -0.5f, 2, 2, 3).box(-1, 0, -0.5f, 2, 2, 3).box(-1, 4, -0.5f, 2, 2, 3).add();
        part(b, "head", "torso", Role.HEAD, flesh).accent(0xFFD040).at(0, -8, -5).rot(-28, 0, 0).mat("flesh").pattern("eyes").glow("eyes")
                .box(-5, -5, -5, 10, 5, 6).add();
        part(b, "gums_up", "head", Role.STATIC, gum).at(0, 0, -2).mat("flesh").box(-4, -0.8f, -2.8f, 8, 1, 5).add();
        BestiaryBst.teeth(b, "teeth_up", "head", TOOTH, 0, 0, -4.4f, 8, 5, 2, false);
        part(b, "jaw", "head", Role.JAW, flesh).accent(dark).at(0, 0.5f, 0).rot(32, 0, 0).mat("flesh").box(-4.5f, 0, -5, 9, 3, 6).add();
        part(b, "gums_lo", "jaw", Role.STATIC, gum).at(0, 0, -2).mat("flesh").box(-3.5f, -0.3f, -2.5f, 7, 1, 4).add();
        BestiaryBst.teeth(b, "teeth_lo", "jaw", TOOTH, 0, 0, -4.4f, 7, 4, 1, true);
        part(b, "tongue", "jaw", Role.TENTACLE, 0xB04456).at(0, 1, -5).rot(20, 0, 0).mat("flesh").box(-1.5f, 0, -1, 3, 7, 2).add();
        part(b, "tongue_mid", "tongue", Role.TENTACLE, 0xA03A4C).at(0, 7, 0).rot(-15, 0, 0).mat("flesh").box(-1, 0, -1, 2, 6, 2).add();
        part(b, "tongue_tip", "tongue_mid", Role.STATIC, 0x902E40).at(0, 6, 0).rot(-40, 0, 0).mat("flesh").box(-1.5f, 0, -1, 3, 4, 1).add();
        sym(b, "arm", "torso", false, flesh).roles(Role.ARM_LEFT, Role.ARM_RIGHT).accent(dark).at(6, -10, -2).rot(-45, 0, -20)
                .mat("flesh").box(-1.5f, -1, -1.5f, 3, 8, 3).add();
        sym(b, "forearm", "arm", true, dark).at(0, 7.5f, 0).rot(-35, 0, 0).mat("flesh").box(-1.5f, 0, -1.5f, 3, 7, 3).add();
        sym(b, "claw", "forearm", true, spur).at(0, 7, -0.5f).rot(20, 0, 0).mat("horn")
                .box(-1.5f, 0, -1, 1, 4, 1).box(0.5f, 0, -1, 1, 4, 1).add();
        sym(b, "leg", "hips", false, flesh).roles(Role.LEG_FRONT_LEFT, Role.LEG_FRONT_RIGHT).accent(dark).at(3, 1, 0).rot(-25, 0, -6)
                .mat("flesh").box(-2, -1, -2, 4, 6, 4).add();
        sym(b, "shin", "leg", true, dark).at(0, 5, 0).rot(45, 0, 6).mat("flesh").box(-1.5f, 0, -1.5f, 3, 5, 3).add();
        sym(b, "foot", "shin", true, dark).at(0, 4.5f, 0).rot(-20, 0, 0).mat("flesh").box(-2, 0, -3, 4, 1, 4)
                .box(-2, 0, -4, 1, 1, 1).box(1, 0, -4, 1, 1, 1).add();
        return b.build();
    }
}
