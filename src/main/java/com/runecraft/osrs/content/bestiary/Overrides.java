package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;

import static com.runecraft.osrs.content.bestiary.Wyrms.BRUTAL;
import static com.runecraft.osrs.content.bestiary.Wyrms.CHROMATIC;
import static com.runecraft.osrs.content.bestiary.Wyrms.METAL;
import static com.runecraft.osrs.content.bestiary.Wyrms.dragon;
import static com.runecraft.osrs.content.bestiary.Wyrms.wyvern;

/** Re-sculpted, OSRS-faithful replacements for models first defined by other modules (same ids). */
final class Overrides {
    private Overrides() {
    }

    static List<ModelSpec> all() {
        List<ModelSpec> l = new ArrayList<>();
        // ---- dragons (slayer module) ----
        l.add(dragon("green_dragon", 0x3A8A3A, 0x9AC060, 0x2A6A2A, 0xD8D0B0, CHROMATIC, "scales"));
        l.add(dragon("blue_dragon", 0x3A5AB0, 0x8AA8E0, 0x2A3A8A, 0xD8D0B0, CHROMATIC, "scales"));
        l.add(dragon("red_dragon", 0xB03A3A, 0xE0A060, 0x8A2020, 0xD8D0B0, CHROMATIC, "scales"));
        l.add(dragon("black_dragon", 0x2A2A2A, 0x5A4A4A, 0x1A1A1A, 0xA09080, CHROMATIC, "scales"));
        l.add(dragon("brutal_green_dragon", 0x2A6A2A, 0x7AA050, 0x1A4A1A, 0x3A3A2A, BRUTAL, "scales"));
        l.add(dragon("brutal_blue_dragon", 0x2A4A90, 0x6A88C0, 0x1A2A6A, 0x3A3A2A, BRUTAL, "scales"));
        l.add(dragon("brutal_red_dragon", 0x902A2A, 0xC08050, 0x6A1A1A, 0x3A3A2A, BRUTAL, "scales"));
        l.add(dragon("brutal_black_dragon", 0x1E1E1E, 0x4A3A3A, 0x101010, 0x6A2020, BRUTAL, "scales"));
        l.add(dragon("lava_dragon", 0x3A2A2A, 0xE06020, 0x5A2A1A, 0xF0A030, BRUTAL, "glow"));
        l.add(dragon("bronze_dragon", 0x9A6A34, 0xC89A60, 0x7A5028, 0xE0C080, METAL, "plates"));
        l.add(dragon("iron_dragon", 0x6E6A68, 0x9A9A9A, 0x504C4A, 0xC0C0C0, METAL, "plates"));
        l.add(dragon("steel_dragon", 0x9A9A9E, 0xC0C8D0, 0x707680, 0xE0E8F0, METAL, "plates"));
        l.add(dragon("mithril_dragon", 0x4E5A88, 0x7A8AB0, 0x3A4670, 0xB0C0E0, METAL, "plates"));
        l.add(dragon("adamant_dragon", 0x4A6A48, 0x6A9A7A, 0x34503A, 0x9AE0B0, METAL, "plates"));
        l.add(dragon("rune_dragon", 0x3E8A9E, 0x6AB0C8, 0x2A6A80, 0xA0F0FF, METAL, "glow"));
        l.add(wyvern("skeletal_wyvern", 0xD8D8C8, 0xA8A898, 0xB8B8A8, "plates"));
        l.add(wyvern("spitting_wyvern", 0x6A8A9A, 0xA0C0D0, 0x4A6A7A, "scales"));
        l.add(wyvern("taloned_wyvern", 0x5A6A8A, 0x9AA8C0, 0x3A4A6A, "scales"));
        // ---- slayer creatures ----
        l.addAll(Slayers.all());
        l.addAll(Bosses.overrides());
        l.addAll(BigBosses.overrides());
        l.addAll(BestiaryBst.all());
        return l;
    }
}
