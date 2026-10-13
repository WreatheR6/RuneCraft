package com.runecraft.osrs.content.minigames;

import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.AttackDef.Special;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.ArrayList;
import java.util.List;

/**
 * Creatures and bosses of the minigames. Minigame creatures are spawned by their minigame (they
 * have no natural spawns) except the Mort'ton shades. Every creature has a custom model with the
 * same id (see {@link MinigameModels}).
 */
public final class MinigameMonsters {
    private MinigameMonsters() {
    }

    private static AttackDef melee(AttackType type, int max) {
        return AttackDef.of("melee", type, max, 2.5, 10);
    }

    private static AttackDef ranged(int max, double range) {
        return AttackDef.of("ranged", AttackType.RANGED, max, range, 10).particle("minecraft:crit");
    }

    private static AttackDef magic(int max, double range, String particle) {
        return AttackDef.of("magic", AttackType.MAGIC, max, range, 10).particle(particle);
    }

    private static final AttackDef NONE = AttackDef.of("none", AttackType.CRUSH, 0, 0.5, 1);

    private static MonsterDef m(String id, String name, int level) {
        return MonsterDef.of(id, name).level(level).model(id);
    }

    public static List<MonsterDef> monsters() {
        List<MonsterDef> list = new ArrayList<>();
        // ---------------- TzHaar Fight Cave ----------------
        list.add(m("tz_kih", "Tz-Kih", 22).stats(10, 22, 22, 15, 1, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(melee(AttackType.STAB, 4).special(Special.DRAIN_PRAYER, 1)).size(0.8f, 0.9f, 1.0f).flying()
                .skin(0xC04010, 0x802010, 0x601008, 0xFFB030));
        list.add(m("tz_kek", "Tz-Kek", 45).stats(20, 40, 40, 40, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 7)).size(1.0f, 1.0f, 1.2f).skin(0xA03010, 0x702010, 0x501008, 0xFF8030));
        list.add(m("tz_kek_small", "Tz-Kek", 22).model("tz_kek").stats(10, 20, 20, 20, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 4)).size(0.7f, 0.7f, 0.8f).skin(0xA03010, 0x702010, 0x501008, 0xFF8030));
        list.add(m("tok_xil", "Tok-Xil", 90).stats(40, 80, 80, 80, 1, 80).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(ranged(14, 9)).attack(melee(AttackType.STAB, 13).special(Special.NONE, 0))
                .size(0.9f, 1.8f, 1.2f).skin(0x903818, 0x602010, 0x401008, 0xFFA040));
        list.add(m("yt_mejkot", "Yt-MejKot", 180).stats(80, 160, 160, 160, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 25)).size(1.0f, 2.2f, 1.3f).skin(0x8A3010, 0x5A2010, 0x3A1008, 0xFFC050));
        list.add(m("ket_zek", "Ket-Zek", 360).stats(160, 320, 320, 320, 240, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(magic(49, 12, "minecraft:flame")).attack(melee(AttackType.CRUSH, 54))
                .size(1.1f, 2.6f, 1.4f).skin(0x7A2810, 0x4A1808, 0x2A1008, 0xFFD060));
        list.add(m("yt_hurkot", "Yt-HurKot", 108).stats(60, 140, 100, 60, 1, 1).speed(4).passive()
                .attack(melee(AttackType.CRUSH, 14)).size(0.8f, 1.8f, 1.0f).skin(0xA04818, 0x703010, 0x502008, 0xFFE070));
        // ---------------- The Inferno ----------------
        list.add(m("jal_nib", "Jal-Nib", 32).stats(10, 1, 1, 15, 15, 1).speed(4).passive()
                .attack(melee(AttackType.CRUSH, 4)).size(0.6f, 0.6f, 0.9f).skin(0x902810, 0x601808, 0x401008, 0xFF6020));
        list.add(m("jal_mejrah", "Jal-MejRah", 85).stats(25, 0, 0, 55, 120, 120).speed(3).flying()
                .attack(ranged(19, 10)).size(1.0f, 0.9f, 1.1f).skin(0x6A2010, 0x4A1008, 0x2A0808, 0xFF4020));
        list.add(m("jal_ak", "Jal-Ak", 165).stats(40, 160, 160, 95, 160, 160).speed(6)
                .attack(melee(AttackType.CRUSH, 29)).attack(ranged(29, 9)).attack(magic(29, 9, "minecraft:flame"))
                .size(1.2f, 1.2f, 1.4f).skin(0x8A3818, 0x5A2010, 0x3A1008, 0xFF8040));
        list.add(m("jal_akrek_ket", "Jal-AkRek-Ket", 70).model("jal_akrek").stats(15, 95, 95, 95, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 18)).size(0.6f, 0.6f, 0.8f).skin(0xA03020, 0x702010, 0x501008, 0xFF6040));
        list.add(m("jal_akrek_xil", "Jal-AkRek-Xil", 70).model("jal_akrek").stats(15, 1, 1, 95, 1, 95).speed(4)
                .attack(ranged(18, 8)).size(0.6f, 0.6f, 0.8f).skin(0x30A040, 0x207020, 0x105010, 0x60FF60));
        list.add(m("jal_akrek_mej", "Jal-AkRek-Mej", 70).model("jal_akrek").stats(15, 1, 1, 95, 95, 1).speed(4)
                .attack(magic(18, 8, "minecraft:witch")).size(0.6f, 0.6f, 0.8f).skin(0x3040C0, 0x202080, 0x101050, 0x6080FF));
        list.add(m("jal_imkot", "Jal-ImKot", 240).stats(75, 210, 290, 120, 120, 220).speed(4)
                .attack(melee(AttackType.SLASH, 49)).size(1.3f, 1.6f, 1.4f).skin(0x7A3010, 0x4A2008, 0x2A1008, 0xFF9030));
        list.add(m("jal_xil", "Jal-Xil", 370).stats(125, 140, 180, 60, 90, 250).speed(4)
                .attack(ranged(46, 12)).attack(melee(AttackType.STAB, 46)).size(1.1f, 2.6f, 1.4f)
                .skin(0x6A2810, 0x4A1808, 0x2A1008, 0xFFB050));
        list.add(m("jal_zek", "Jal-Zek", 490).stats(220, 370, 510, 260, 300, 1).speed(4)
                .attack(magic(70, 12, "minecraft:flame")).attack(melee(AttackType.CRUSH, 70)).size(1.3f, 3.0f, 1.5f)
                .skin(0x5A2010, 0x3A1008, 0x200808, 0xFFC060));
        list.add(m("jaltok_jad", "JalTok-Jad", 900).stats(350, 750, 1020, 480, 510, 1020).speed(8)
                .attack(magic(113, 14, "minecraft:flame").telegraph(3, "JalTok-Jad rears up!"))
                .attack(ranged(113, 14).telegraph(3, "JalTok-Jad stomps the ground!"))
                .attack(melee(AttackType.STAB, 113))
                .size(1.6f, 3.2f, 1.8f).skin(0xC04010, 0x803010, 0x501808, 0xFFD040));
        list.add(m("jal_mejjak", "Jal-MejJak", 250).stats(80, 1, 1, 100, 200, 1).speed(4)
                .attack(magic(25, 12, "minecraft:flame").special(Special.AOE, 2)).size(0.8f, 1.8f, 1.1f)
                .skin(0x8A3010, 0x5A2010, 0x3A1008, 0xFF7030));
        list.add(m("ancestral_glyph", "Ancestral Glyph", 0).stats(600, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).size(1.6f, 3.0f, 1.0f).skin(0x3A2A1A, 0x2A1A0A, 0x1A1008, 0xFF8030));
        // ---------------- Pest Control ----------------
        list.add(MonsterDef.of("void_knight", "Void Knight").level(0).stats(200, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).skin(0xC8A080, 0x3A3A3A, 0x2A2A2A, 0xE0E0E0));
        String[][] portals = {{"purple_portal", "Portal (purple)", "8030C0"}, {"blue_portal", "Portal (blue)", "3060E0"},
                {"yellow_portal", "Portal (yellow)", "E0C030"}, {"red_portal", "Portal (red)", "C03030"}};
        for (String[] p : portals) {
            int c = Integer.parseInt(p[2], 16);
            list.add(MonsterDef.of(p[0], p[1]).level(0).model("pest_portal").stats(250, 1, 1, 50, 1, 1).passive().moveSpeed(0.0)
                    .attack(NONE).size(1.2f, 2.8f, 1.2f).skin(c, c, c, 0xFFFFFF));
        }
        list.add(m("brawler", "Brawler", 129).stats(97, 85, 110, 80, 1, 1).speed(5)
                .attack(melee(AttackType.CRUSH, 15)).size(1.0f, 2.0f, 1.3f).skin(0x6A7A4A, 0x4A5A2A, 0x3A4A1A, 0xA0C060));
        list.add(m("defiler", "Defiler", 97).stats(52, 1, 1, 50, 1, 80).speed(4)
                .attack(ranged(10, 9)).size(0.7f, 1.4f, 1.0f).skin(0x7A6A3A, 0x5A4A2A, 0x3A2A1A, 0xC0A060));
        list.add(m("ravager", "Ravager", 106).stats(61, 70, 70, 50, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 12)).size(0.9f, 1.4f, 1.1f).skin(0x5A4A6A, 0x3A2A4A, 0x2A1A3A, 0xA080C0));
        list.add(m("shifter", "Shifter", 104).stats(57, 60, 70, 50, 1, 1).speed(4)
                .attack(melee(AttackType.STAB, 13)).attack(melee(AttackType.STAB, 13).special(Special.BLINK, 0))
                .size(0.7f, 1.2f, 1.0f).skin(0x4A6A6A, 0x2A4A4A, 0x1A3A3A, 0x80C0C0));
        list.add(m("spinner", "Spinner", 74).stats(71, 40, 40, 60, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 6).special(Special.POISON, 5)).size(0.8f, 0.8f, 1.0f)
                .skin(0x8A6A3A, 0x6A4A2A, 0x4A3A1A, 0xE0C080));
        list.add(m("splatter", "Splatter", 54).stats(23, 20, 20, 10, 1, 1).speed(4)
                .attack(AttackDef.of("explode", AttackType.CRUSH, 12, 2.0, 10).special(Special.AOE, 2).particle("minecraft:explosion"))
                .size(0.8f, 0.8f, 1.0f).skin(0x9A4A6A, 0x7A2A4A, 0x5A1A3A, 0xE080A0));
        list.add(m("torcher", "Torcher", 79).stats(51, 1, 1, 40, 70, 1).speed(4)
                .attack(magic(10, 10, "minecraft:flame")).size(0.7f, 1.2f, 1.0f).flying()
                .skin(0xC06030, 0x904020, 0x602010, 0xFFC060));
        // ---------------- Barbarian Assault ----------------
        list.add(m("penance_fighter", "Penance Fighter", 58).stats(40, 60, 60, 40, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 10)).size(0.8f, 1.5f, 1.1f).skin(0x6A5A3A, 0x4A3A2A, 0x2A2A1A, 0xC0A060));
        list.add(m("penance_ranger", "Penance Ranger", 57).stats(35, 1, 1, 30, 1, 60).speed(4)
                .attack(ranged(9, 9)).size(0.8f, 1.5f, 1.1f).skin(0x5A6A3A, 0x3A4A2A, 0x2A3A1A, 0xA0C060));
        list.add(m("penance_runner", "Penance Runner", 47).stats(30, 20, 20, 20, 1, 1).speed(4).passive().moveSpeed(0.32)
                .attack(melee(AttackType.CRUSH, 3)).size(0.7f, 1.0f, 0.9f).skin(0x5A5A6A, 0x3A3A4A, 0x2A2A3A, 0xA0A0C0));
        list.add(m("penance_healer", "Penance Healer", 60).stats(35, 30, 30, 30, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 6).special(Special.POISON, 4)).size(0.7f, 1.3f, 1.0f)
                .skin(0x6A5A5A, 0x4A3A3A, 0x2A2A2A, 0xE0A0A0));
        // ---------------- Wintertodt / Tempoross / Guardians (scenery creatures) ----------------
        list.add(m("wintertodt", "Wintertodt", 0).stats(255, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).size(2.0f, 4.0f, 2.5f).skin(0xA0C8F0, 0x80A8E0, 0x6088C0, 0xFFFFFF));
        list.add(m("tempoross", "Tempoross", 0).stats(255, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).size(2.5f, 4.0f, 2.5f).skin(0x3A6A9A, 0x2A5A8A, 0x1A4A7A, 0xE0F0FF));
        list.add(m("great_guardian", "The Great Guardian", 0).stats(255, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).size(1.5f, 3.5f, 1.8f).skin(0x5A4AA0, 0x3A2A80, 0x2A1A60, 0xC0A0FF));
        // ---------------- Warriors' Guild ----------------
        int[] armourColors = {0x8A5A2A, 0x6A6A6A, 0x9A9AA0, 0x2A2A2A, 0x4A5A9C, 0x4C7A4C, 0x4FB8C8};
        for (int i = 0; i < MinigameRules.WarriorsGuild.ARMOUR.length; i++) {
            Object[] a = MinigameRules.WarriorsGuild.ARMOUR[i];
            String tier = (String) a[0];
            int lvl = (Integer) a[1];
            int hp = (Integer) a[2];
            int max = (Integer) a[3];
            String name = Character.toUpperCase(tier.charAt(0)) + tier.substring(1);
            int c = armourColors[i];
            list.add(MonsterDef.of("animated_" + tier + "_armour", "Animated " + name + " Armour").level(lvl)
                    .stats(hp, lvl, lvl, lvl, 1, 1).speed(4).defenceBonus(20 + i * 15, 20 + i * 15, 20 + i * 15, -5, 20 + i * 15)
                    .attack(melee(AttackType.SLASH, max)).skin(c, c, c, 0x101010));
        }
        list.add(m("warriors_guild_cyclops", "Cyclops", 56).model("cyclops").stats(55, 50, 50, 50, 1, 1).speed(5)
                .attack(melee(AttackType.CRUSH, 6)).size(0.9f, 2.4f, 1.3f).skin(0xA08060, 0x6A4A2A, 0x4A3A2A, 0x202020));
        list.add(m("cyclops_elite", "Cyclops", 106).model("cyclops").stats(75, 90, 105, 90, 1, 1).speed(5)
                .attack(melee(AttackType.CRUSH, 11)).size(0.9f, 2.4f, 1.5f).skin(0x907050, 0x5A3A2A, 0x3A2A1A, 0x202020));
        // ---------------- Shades of Mort'ton ----------------
        // OSRS Wiki / dps-calc: crush, 4-tick; shades drop only their remains (keys come from cremating them).
        for (ShadesOfMortton.Shade s : ShadesOfMortton.Shade.values()) {
            int lvl = s.level();
            list.add(MonsterDef.of(s.creature(), s.displayName() + " shade").level(lvl).model("shade")
                    .stats(s.hitpoints(), s.attack(), s.strength(), s.defence(), 1, 1).speed(4)
                    .attack(melee(AttackType.CRUSH, s.maxHit())).flying()
                    .drops(Drop.always(s.remains()))
                    .size(0.7f, 2.0f, 1.0f).skin(0x2A2A3A, 0x1A1A2A, 0x10101A, 0x8080FF)
                    .spawn(Region.MORYTANIA, 2, 1, 2).underground());
        }
        // ---------------- Nightmare Zone (normal-mode quest bosses; stats per the OSRS Wiki infoboxes) ----------------
        list.add(MonsterDef.of("nmz_count_draynor", "Count Draynor").level(34).stats(35, 30, 25, 30, 1, 1).speed(4)
                .defenceBonus(2, 1, 3, 0, 0).attack(melee(AttackType.CRUSH, 3)).skin(0xE0E0E0, 0x2A2A2A, 0x1A1A1A, 0xA01010));
        list.add(MonsterDef.of("nmz_king_roald", "King Roald").level(47).stats(75, 35, 30, 30, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 3)).skin(0xC8A080, 0x8A2A8A, 0x6A1A6A, 0xE0C040));
        list.add(MonsterDef.of("nmz_the_kendal", "The Kendal").level(70).stats(50, 65, 65, 60, 1, 1).speed(4)
                .attackBonus(5, 0, 0).defenceBonus(10, 10, 10, -10, 0).attack(melee(AttackType.CRUSH, 9))
                .size(1.0f, 2.2f, 1.3f).skin(0xD0D0D0, 0xB0B0B0, 0x909090, 0x5A3A2A));
        list.add(MonsterDef.of("nmz_me", "Me").level(79).stats(45, 75, 78, 74, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 9)).skin(0x2A2A2A, 0x1A1A1A, 0x101010, 0x8A2A8A));
        list.add(MonsterDef.of("nmz_elvarg", "Elvarg").level(83).stats(80, 70, 70, 70, 70, 1).speed(4)
                .defenceBonus(20, 40, 40, 30, 20).attack(melee(AttackType.SLASH, 8))
                .attack(AttackDef.of("dragonfire", AttackType.MAGIC, 70, 8, 4).special(Special.DRAGONFIRE, 0).particle("minecraft:flame"))
                .size(2.4f, 2.4f, 2.0f).skin(0x2A6A2A, 0x1A4A1A, 0x103010, 0xE04010)); // ranged defence approx
        list.add(MonsterDef.of("nmz_tree_spirit", "Tree spirit").level(101).stats(85, 90, 95, 80, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 10)).skin(0x5A7A3A, 0x4A6A2A, 0x3A5A1A, 0x8AB04A));
        list.add(MonsterDef.of("nmz_dad", "Dad").level(101).stats(120, 60, 120, 50, 1, 1).speed(8)
                .attackBonus(40, 0, 0).defenceBonus(25, 25, 40, 200, 200).attack(melee(AttackType.CRUSH, 27).special(Special.KNOCKBACK, 3))
                .size(1.6f, 3.0f, 1.8f).skin(0x6A7A6A, 0x5A6A5A, 0x4A5A4A, 0x3A3A3A)); // ranged defence approx
        list.add(MonsterDef.of("nmz_khazard_warlord", "Khazard warlord").level(112).stats(170, 75, 78, 80, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 10)).skin(0xC8A080, 0x6A2A2A, 0x4A1A1A, 0x3A3A3A));
        list.add(MonsterDef.of("nmz_arrg", "Arrg").level(113).stats(140, 70, 140, 40, 1, 70).speed(5)
                .attackBonus(60, 0, 0).defenceBonus(35, 60, 35, 200, 200).attack(melee(AttackType.CRUSH, 38))
                .attack(AttackDef.of("rock", AttackType.RANGED, 30, 8, 10).particle("minecraft:crit"))
                .size(1.4f, 2.8f, 1.6f).skin(0x6A6A5A, 0x5A5A4A, 0x4A4A3A, 0x3A3A2A)); // ranged defence approx
        list.add(MonsterDef.of("nmz_black_knight_titan", "Black Knight Titan").level(120).stats(142, 91, 100, 91, 1, 1).speed(7)
                .attackBonus(27, 0, 0).defenceBonus(18, 27, 18, 1000, 1000).attack(melee(AttackType.CRUSH, 15))
                .size(1.2f, 3.2f, 1.6f).skin(0x2A2A2A, 0x1A1A1A, 0x101010, 0x6A6A6A));
        list.add(MonsterDef.of("nmz_bouncer", "Bouncer").level(137).stats(116, 120, 120, 120, 1, 1).speed(4)
                .attack(melee(AttackType.STAB, 13)).size(1.2f, 1.2f, 1.4f).skin(0x6A2A1A, 0x4A1A10, 0x2A1008, 0xE06020));
        list.add(MonsterDef.of("nmz_glod", "Glod").level(138).stats(160, 115, 120, 110, 1, 1).speed(4)
                .defenceBonus(105, 110, 130, 125, 115).attack(melee(AttackType.CRUSH, 13))
                .size(2.0f, 3.6f, 2.2f).skin(0x8A7A5A, 0x6A5A3A, 0x4A3A2A, 0x3A2A1A)); // ranged defence approx
        // ---------------- Soul Wars ----------------
        list.add(MonsterDef.of("forgotten_soul", "Forgotten Soul").level(58).stats(60, 40, 40, 40, 1, 1).speed(4) // approx stats
                .attack(melee(AttackType.SLASH, 6)).drops(Drop.always("soul_fragment"))
                .flying().skin(0x9A8AC0, 0x6A5A9A, 0x4A3A7A, 0xC0B0FF));
        list.add(MonsterDef.of("avatar_of_destruction", "Avatar of Destruction").level(525)
                .stats(MinigameRules.SoulWars.AVATAR_HP, 160, 320, 10, 10, 1).speed(6)
                .defenceBonus(20, 20, 20, 20, 20).attack(melee(AttackType.CRUSH, 54))
                .size(1.8f, 4.0f, 2.2f).skin(0x6A1A1A, 0x4A1010, 0x2A0808, 0xFF4020));
        list.add(MonsterDef.of("avatar_of_creation", "Avatar of Creation").level(525)
                .stats(MinigameRules.SoulWars.AVATAR_HP, 160, 320, 10, 10, 1).speed(6)
                .defenceBonus(20, 20, 20, 20, 20).attack(melee(AttackType.CRUSH, 54))
                .size(1.8f, 4.0f, 2.2f).skin(0x1A3A6A, 0x10204A, 0x08102A, 0x40A0FF));
        // ---------------- Sorceress's Garden ----------------
        // The seasonal elementals can't be attacked; they patrol their garden (moved by the session) and teleport intruders out.
        for (SorceressGarden.Season s : SorceressGarden.Season.values()) {
            list.add(MonsterDef.of(s.elemental(), s.displayName() + " Elemental").level(0).stats(255, 1, 1, 1, 1, 1).passive()
                    .moveSpeed(0.0).immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                    .attack(NONE).flying().size(0.7f, 1.9f, 1.0f)
                    .skin(s.juiceColor(), s.fruitColor(), s.fruitColor(), 0xFFFFFF));
        }
        return list;
    }


    public static List<BossDef> bosses() {
        List<BossDef> list = new ArrayList<>();
        list.add(BossDef.of("tzkal_zuk", "TzKal-Zuk").level(1400).stats(1200, 350, 600, 260, 150, 400).speed(10)
                .defenceBonus(0, 0, 0, 350, 100).model("tzkal_zuk")
                .attack(AttackDef.of("typeless", AttackType.MAGIC, 148, 30, 10).special(Special.PRAYER_PIERCING, 0)
                        .particle("minecraft:flame").message("TzKal-Zuk unleashes a blast of fire!"))
                .moveSpeed(0.0).raidOnly().category("Minigames").arena(16, "lava").size(2.0f, 3.5f, 2.6f)
                .skin(0xC04010, 0x803010, 0x501808, 0xFFD040));
        list.add(BossDef.of("penance_queen", "Penance Queen").level(209).stats(250, 300, 200, 260, 1, 1).speed(5)
                .model("penance_queen")
                // OSRS Wiki: a ranged volley hitting every player for up to 13, and a melee attack up to 5.
                .attack(melee(AttackType.CRUSH, 5))
                .attack(AttackDef.of("volley", AttackType.RANGED, 13, 10, 10).special(Special.AOE, 10).particle("minecraft:item_slime"))
                .minion("penance_fighter", 2)
                .phase(50, "The Penance Queen summons her spawn!", 1.1, true)
                .raidOnly().category("Minigames").size(1.6f, 2.2f, 2.0f)
                .skin(0x7A6A3A, 0x5A4A2A, 0x3A2A1A, 0xE0C060));
        return list;
    }
}
