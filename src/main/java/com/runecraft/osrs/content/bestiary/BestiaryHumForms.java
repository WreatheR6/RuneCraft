package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;
import com.runecraft.osrs.model.ModelSpec.Role;

import static com.runecraft.osrs.content.bestiary.BestiaryHumKit.*;
import static com.runecraft.osrs.content.bestiary.Kit.part;
import static com.runecraft.osrs.content.bestiary.Kit.sym;

import com.runecraft.osrs.content.bestiary.BestiaryHumKit.Fig;

/**
 * Species forms built on {@link BestiaryHumKit}: demons, aviansies, goblins, ourgs, trolls/giants and the horse body
 * (unicorns, centaurs). Each returns the {@link Fig} so the caller can arm and dress it.
 */
final class BestiaryHumForms {
    private BestiaryHumForms() {
    }

    /**
     * A demon: hunched, heavily muscled torso, shoulder humps, a horned head with a muzzle, a hinged fanged jaw
     * and burning eyes, clawed hands, digitigrade hoofed legs, a spade tail and spikes down the back.
     * {@code bulk} 0 (lesser) to 6 (huge).
     */
    static Fig demon(String id, int skin, int dark, int horn, int eye, int bulk) {
        Fig f = fig(id).legs(12 + bulk / 3, 4 + bulk / 2).torso(8 + bulk, 7 + bulk, 11 + bulk * 2, 7 + bulk / 2, 5, 6 + bulk / 2)
                .arms(14 + bulk, 3 + bulk / 2, 4 + bulk / 2).head(6, 6, 6).pose(26, 4, 14).armPitch(-16, -16).skin(skin, "hide")
                .shadow(0.8f + bulk * 0.1f);
        f.foreBend = 26;
        int mid = darker(skin, 0.88f);
        f.core(dark, "hide", mid, "hide", skin, "hide").muscles(skin).head("monster", eye).beastFace(mid, dark, IVORY, true)
                .horns(horn, 2.2f, -5, -0.5f, 32, 38, 2)
                .armsBuild(skin, "hide", mid, "hide", dark, "hide").claws(horn, 2)
                .beastLegs(skin, "hide", darker(dark, 0.7f), "horn", false)
                .tail(mid, horn, 5, "spade").loincloth(darker(dark, 0.8f), 5).backSpikes(horn, 3, 1);
        f.s("shoulder", "chest", false, skin).at(f.chestW / 2f - 1, -f.chestH + 1, 0).rot(0, 0, -22).mat("hide")
                .box(-2, -3, -3, 5, 4, 6).add();
        f.s("shoulder_spike", "shoulder", true, horn).at(1.5f, -3, 0).rot(-10, 0, 25).mat("horn").box(-0.5f, -3, -0.5f, 1, 3, 1).add();
        f.s("forearm_ridge", "forearm", true, darker(dark, 0.9f)).at(f.foreW / 2f, 1, 0).mat("horn").box(-0.3f, 0, -1, 1, f.foreLen() - 2, 2).add();
        return f;
    }

    /**
     * An aviansie: a bird-man with a feathered body, a hooked beak (JAW lower mandible), a swept crest, clawed
     * feathered arms, taloned digitigrade legs, a tail fan and great layered wings.
     */
    static Fig aviansie(String id, int feather, int light, int beak, int eye, int span, int bulk) {
        Fig f = fig(id).legs(12, 3 + bulk / 2).torso(7 + bulk, 7 + bulk, 10 + bulk * 2, 6 + bulk / 2, 5, 5 + bulk / 2)
                .arms(13 + bulk, 3 + bulk / 3, 3 + bulk / 2).head(5, 5, 6).pose(18, 3, 10).armPitch(-12, -12).skin(feather, "feathers")
                .shadow(0.8f + bulk * 0.1f);
        int dk = darker(feather, 0.82f);
        f.core(dk, "feathers", feather, "feathers", feather, "feathers").head("monster", eye)
                .armsBuild(feather, "feathers", dk, "feathers", beak, "horn").claws(darker(beak, 0.7f), 2)
                .beastLegs(dk, "feathers", beak, "horn", true);
        f.p("breast", "chest", light).at(0, -f.chestH + 1, -f.depth / 2f - 0.5f).mat("feathers").box(-f.chestW / 2f + 1.5f, 0, -0.8f, f.chestW - 3, f.chestH - 1, 1).add();
        f.p("beak", "head", beak).at(0, -2.5f, -3).rot(14, 0, 0).mat("horn").box(-1, -1, -3, 2, 2, 3).add();
        f.p("beak_hook", "beak", darker(beak, 0.8f)).at(0, 0, -3).rot(55, 0, 0).mat("horn").box(-1, -1, -1.5f, 2, 2, 2, -0.2f).add();
        part(f.b, "beak_low", "head", Role.JAW, darker(beak, 0.85f)).at(0, -1, -3).rot(10, 0, 0).mat("horn").box(-0.5f, 0, -2, 1, 1, 2).add();
        for (int k = 0; k < 3; k++) {
            f.p("crest" + k, "head", k == 1 ? light : feather).at((k - 1) * 1.2f, -5, 1).rot(-55 + k * 0, 0, (k - 1) * 14).mat("feathers")
                    .box(-0.5f, -4 - (k == 1 ? 1 : 0), -1, 1, 4 + (k == 1 ? 1 : 0), 2).add();
        }
        f.s("ruff", "chest", false, light).at(f.chestW / 2f - 1, -f.chestH, 0).rot(0, 0, -30).mat("feathers").box(-1.5f, -1, -2, 3, 2, 4).add();
        for (int k = 0; k < 3; k++) {
            f.p("tail_feather" + k, "hips", k == 1 ? light : dk).at((k - 1) * 1.5f, 0, f.depth / 2f).rot(55, (k - 1) * 18, 0).mat("feathers")
                    .box(-1, 0, -0.5f, 2, 8, 1).add();
        }
        f.featherWings("chest", feather, light, darker(light, 0.85f), 2, -f.chestH + 1.5f, f.depth / 2f + 0.5f, span, 5);
        return f;
    }

    /** A goblin: short, big-headed, long-nosed, pointy-eared, hunched. */
    static Fig goblin(String id, int skin, int eyeCol) {
        Fig f = fig(id).legs(8, 3).torso(7, 6, 8, 5, 4, 4).arms(10, 3, 3).head(8, 7, 7).pose(14, 4, 10).armPitch(-10, -20).skin(skin, "skin")
                .shadow(0.5f);
        f.foreBend = 18;
        return f;
    }

    /** Head extras for a goblin (call after head()). */
    static void goblinHead(Fig f) {
        int sk = f.skin;
        f.s("goblin_ear", "head", false, sk).at(f.headW / 2f, -f.headH * 0.55f, 0).rot(0, -25, -30).mat(f.skinMat).box(0, -1, -0.5f, 4, 2, 1).add();
        f.s("goblin_ear_tip", "goblin_ear", true, darker(sk, 0.9f)).at(4, 0, 0).rot(0, 0, 25).mat(f.skinMat).box(0, -0.5f, -0.5f, 2, 1, 1).add();
        f.p("goblin_nose", "head", darker(sk, 0.92f)).at(0, -f.headH * 0.45f, -f.headD / 2f).rot(-15, 0, 0).mat(f.skinMat).box(-1, 0, -2, 2, 2, 2).add();
        f.p("goblin_nose_tip", "goblin_nose", darker(sk, 0.85f)).at(0, 1.5f, -2).rot(30, 0, 0).mat(f.skinMat).box(-0.5f, -0.5f, -1, 1, 1, 1).add();
        f.s("goblin_tooth", "head", false, IVORY).at(1.2f, -1.2f, -f.headD / 2f).mat("horn").box(-0.5f, 0, -0.6f, 1, 1, 1).add();
    }

    /** A horse body (BODY root at the barrel) with four jointed legs, a neck and head, mane and tail. */
    static void horse(ModelSpec.Builder b, int coat, int mane, int hoof, int eye, boolean neckAndHead) {
        int dk = darker(coat, 0.85f);
        part(b, "barrel", null, Role.BODY, coat).at(0, 11, 1).mat("fur").box(-4.5f, -4, -9, 9, 8, 18).add();
        part(b, "chest_h", "barrel", Role.STATIC, coat).at(0, -1, -9).rot(-8, 0, 0).mat("fur").box(-4, -3.5f, -2, 8, 8, 3).add();
        part(b, "rump", "barrel", Role.STATIC, coat).at(0, -2.5f, 8).rot(10, 0, 0).mat("fur").box(-4.75f, -2, -3, 9, 6, 5, 0.2f).add();
        String[][] legs = {{"lf", "LEG_FRONT_LEFT"}, {"rf", "LEG_FRONT_RIGHT"}, {"lb", "LEG_BACK_LEFT"}, {"rb", "LEG_BACK_RIGHT"}};
        for (String[] lg : legs) {
            boolean left = lg[0].startsWith("l");
            boolean front = lg[0].endsWith("f");
            float x = left ? 2.8f : -2.8f;
            float z = front ? -6.5f : 7;
            String up = "uleg_" + lg[0];
            part(b, up, "barrel", Role.valueOf(lg[1]), coat).at(x, 2, z).rot(front ? 4 : -6, 0, 0).mat("fur").box(-1.5f, -1, -1.5f, 3, 6, 3, 0.2f).add();
            part(b, "lleg_" + lg[0], up, Role.STATIC, dk).at(0, 5, front ? 0 : 0.5f).rot(front ? -4 : 10, 0, 0).mat("fur").box(-1, 0, -1, 2, 4, 2).add();
            part(b, "knee_" + lg[0], "lleg_" + lg[0], Role.STATIC, coat).at(0, 0, front ? -1 : 1).mat("fur").box(-1, -0.5f, -0.5f, 2, 2, 1, 0.1f).add();
            part(b, "fetlock_" + lg[0], "lleg_" + lg[0], Role.STATIC, mane).at(0, 3.5f, 0).mat("hair").box(-1.25f, 0, -1.25f, 2, 1, 2, 0.25f).add();
            part(b, "hoof_" + lg[0], "lleg_" + lg[0], Role.STATIC, hoof).at(0, 4, 0).rot(front ? 0 : -10, 0, 0).mat("horn").box(-1.25f, 0, -1.5f, 3, 2, 3, -0.25f).add();
        }
        sym(b, "shoulder_m", "barrel", false, coat).at(4.2f, -1, -6).rot(0, 0, -8).mat("fur").box(-0.5f, -2, -2.5f, 1, 6, 5).add();
        sym(b, "flank_m", "barrel", false, dk).at(4.3f, -1, 6).rot(0, 0, -6).mat("fur").box(-0.5f, -2, -2.5f, 1, 6, 5).add();
        part(b, "belly_h", "barrel", Role.STATIC, dk).at(0, 4, 0).mat("fur").box(-3.5f, -0.5f, -6, 7, 1, 12).add();
        String prev = "rump";
        for (int i = 0; i < 3; i++) {
            part(b, "htail" + i, prev, Role.TAIL, i == 2 ? darker(mane, 0.9f) : mane).at(0, i == 0 ? -1 : 3.5f, i == 0 ? 2 : 0.5f)
                    .rot(i == 0 ? 35 : 12, 0, 0).mat("hair").box(-1, 0, -1, 2, 4, 2, i == 0 ? 0 : 0.2f).add();
            prev = "htail" + i;
        }
        if (neckAndHead) {
            part(b, "hneck", "barrel", Role.NECK, coat).at(0, -3, -8).rot(35, 0, 0).mat("fur").box(-2, -8, -2.5f, 4, 9, 5).add();
            for (int i = 0; i < 4; i++) {
                part(b, "mane" + i, "hneck", Role.STATIC, i % 2 == 0 ? mane : darker(mane, 0.9f)).at(0, -7.5f + i * 2.2f, 2.5f).rot(-10, 0, 0)
                        .mat("hair").box(-0.75f, -1, -1, 2, 3, 2).add();
            }
            part(b, "hhead", "hneck", Role.HEAD, coat).accent(eye).at(0, -8, -0.5f).rot(10, 0, 0).mat("fur").pattern("eyes_animal")
                    .box(-2, -3, -3, 4, 4, 4).add();
            part(b, "hmuzzle", "hhead", Role.STATIC, dk).at(0, 1, -3).rot(-10, 0, 0).mat("fur").pattern("none").box(-1.5f, -2.5f, -4, 3, 3, 4).add();
            part(b, "hnostril", "hmuzzle", Role.STATIC, darker(dk, 0.6f)).at(0, -0.5f, -4).mat("fur").box(-1, -0.5f, -0.3f, 2, 1, 1, -0.2f).add();
            part(b, "hjaw", "hhead", Role.JAW, dk).at(0, 1, -3).mat("fur").box(-1, 0, -3.5f, 2, 1, 4).add();
            part(b, "hthroat", "hneck", Role.STATIC, dk).at(0, -2, -2.5f).mat("fur").box(-1.5f, -5, -0.5f, 3, 6, 1).add();
            sym(b, "hear", "hhead", false, coat).at(1.5f, -3, 0).rot(-10, 0, -15).mat("fur").box(-0.5f, -2, -0.5f, 1, 2, 1).add();
        }
    }
}
