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
                .attack(ranged(14, 9)).attack(melee(AttackType.CRUSH, 13).special(Special.NONE, 0))
                .size(0.9f, 1.8f, 1.2f).skin(0x903818, 0x602010, 0x401008, 0xFFA040));
        list.add(m("yt_mejkot", "Yt-MejKot", 180).stats(80, 160, 160, 160, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 25)).size(1.0f, 2.2f, 1.3f).skin(0x8A3010, 0x5A2010, 0x3A1008, 0xFFC050));
        list.add(m("ket_zek", "Ket-Zek", 360).stats(160, 320, 320, 320, 240, 1).speed(4).defenceBonus(0, 0, 0, 0, 0)
                .attack(magic(52, 12, "minecraft:flame")).attack(melee(AttackType.STAB, 55))
                .size(1.1f, 2.6f, 1.4f).skin(0x7A2810, 0x4A1808, 0x2A1008, 0xFFD060));
        list.add(m("yt_hurkot", "Yt-HurKot", 108).stats(60, 140, 100, 60, 1, 1).speed(4).passive()
                .attack(melee(AttackType.CRUSH, 14)).size(0.8f, 1.8f, 1.0f).skin(0xA04818, 0x703010, 0x502008, 0xFFE070));
        // The level-141 Yt-HurKot that tend JalTok-Jads (the Inferno, TzHaar-Ket-Rak's challenges): same look, stronger.
        list.add(m("yt_hurkot_inferno", "Yt-HurKot", 141).model("yt_hurkot").stats(90, 165, 125, 100, 150, 150).speed(4).passive()
                .attack(melee(AttackType.CRUSH, 18)).size(0.8f, 1.8f, 1.0f).skin(0xA04818, 0x703010, 0x502008, 0xFFE070));
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
                .attack(ranged(46, 12)).attack(melee(AttackType.CRUSH, 19)).size(1.1f, 2.6f, 1.4f)
                .skin(0x6A2810, 0x4A1808, 0x2A1008, 0xFFB050));
        list.add(m("jal_zek", "Jal-Zek", 490).stats(220, 370, 510, 260, 300, 1).speed(4)
                .attack(magic(70, 12, "minecraft:flame")).attack(melee(AttackType.STAB, 52)).size(1.3f, 3.0f, 1.5f)
                .skin(0x5A2010, 0x3A1008, 0x200808, 0xFFC060));
        list.add(m("jaltok_jad", "JalTok-Jad", 900).stats(350, 750, 1020, 480, 510, 1020).speed(8)
                .attack(magic(113, 14, "minecraft:flame").telegraph(3, "JalTok-Jad rears up!"))
                .attack(ranged(113, 14).telegraph(3, "JalTok-Jad stomps the ground!"))
                .attack(melee(AttackType.STAB, 113))
                .size(1.6f, 3.2f, 1.8f).skin(0xC04010, 0x803010, 0x501808, 0xFFD040));
        // Jal-MejJak heal Zuk until attacked, then lob 3x3 lava balls (5-10) at the player; the Inferno drives them.
        list.add(m("jal_mejjak", "Jal-MejJak", 250).stats(75, 1, 1, 100, 1, 1).speed(3).passive()
                .attack(magic(10, 12, "minecraft:flame").special(Special.AOE, 2)).size(0.8f, 1.8f, 1.1f)
                .skin(0x8A3010, 0x5A2010, 0x3A1008, 0xFF7030));
        list.add(m("ancestral_glyph", "Ancestral Glyph", 0).stats(600, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).size(1.6f, 3.0f, 1.0f).skin(0x3A2A1A, 0x2A1A0A, 0x1A1008, 0xFF8030));
        // ---------------- Pest Control ----------------
        list.add(MonsterDef.of("void_knight", "Void Knight").level(0).stats(200, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).skin(0xC8A080, 0x3A3A3A, 0x2A2A2A, 0xE0E0E0));
        // Portals: level 120 Defence and +100 armour except against their weakness (Mod Ash; PestControlRules). The session sets
        // their hitpoints by lander (200 novice, 250 intermediate and veteran).
        String[][] portals = {{"purple_portal", "Portal (purple)", "8030C0"}, {"blue_portal", "Portal (blue)", "3060E0"},
                {"yellow_portal", "Portal (yellow)", "E0C030"}, {"red_portal", "Portal (red)", "C03030"}};
        for (int i = 0; i < portals.length; i++) {
            String[] p = portals[i];
            int c = Integer.parseInt(p[2], 16);
            int[] d = PestControlRules.portalDefenceBonuses(i);
            list.add(MonsterDef.of(p[0], p[1]).level(0).model("pest_portal")
                    .stats(250, 1, 1, PestControlRules.PORTAL_DEFENCE, 1, 1).defenceBonus(d[0], d[1], d[2], d[3], d[4])
                    .passive().moveSpeed(0.0).attack(NONE).size(1.2f, 2.8f, 1.2f).skin(c, c, c, 0xFFFFFF));
        }
        // Pests: these are the highest levels; the session gives each spawn the stats of a level of its lander
        // (PestControlRules.VARIANTS, osrs-dps-calc). Ravagers and spinners don't seek players out (wiki pest pages).
        list.add(m("brawler", "Brawler", 129).stats(143, 67, 132, 118, 0, 0).speed(4)
                .attack(melee(AttackType.CRUSH, 14)).size(1.0f, 2.0f, 1.3f).skin(0x6A7A4A, 0x4A5A2A, 0x3A4A1A, 0xA0C060));
        list.add(m("defiler", "Defiler", 97).stats(97, 0, 0, 65, 0, 117).speed(4)
                .attack(ranged(13, 9)).size(0.7f, 1.4f, 1.0f).skin(0x7A6A3A, 0x5A4A2A, 0x3A2A1A, 0xC0A060));
        list.add(m("ravager", "Ravager", 106).stats(83, 67, 157, 53, 0, 0).speed(4).passive()
                .attack(melee(AttackType.CRUSH, 17)).size(0.9f, 1.4f, 1.1f).skin(0x5A4A6A, 0x3A2A4A, 0x2A1A3A, 0xA080C0));
        list.add(m("shifter", "Shifter", 104).stats(83, 110, 83, 83, 60, 0).speed(4)
                .attack(melee(AttackType.STAB, 9)).attack(melee(AttackType.STAB, 9).special(Special.BLINK, 0))
                .size(0.7f, 1.2f, 1.0f).skin(0x4A6A6A, 0x2A4A4A, 0x1A3A3A, 0x80C0C0));
        list.add(m("spinner", "Spinner", 74).stats(73, 27, 27, 93, 67, 0).speed(4).passive()
                .attack(melee(AttackType.CRUSH, 4)).size(0.8f, 0.8f, 1.0f)
                .skin(0x8A6A3A, 0x6A4A2A, 0x4A3A1A, 0xE0C080));
        // Splatters bite for 3-5 like any melee pest; their 5-24 detonation is the session's (PestControlRules.SPLATTER_*).
        list.add(m("splatter", "Splatter", 54).stats(43, 32, 32, 93, 32, 0).speed(4)
                .attack(melee(AttackType.CRUSH, 4)).size(0.8f, 0.8f, 1.0f).skin(0x9A4A6A, 0x7A2A4A, 0x5A1A3A, 0xE080A0));
        list.add(m("torcher", "Torcher", 79).stats(57, 0, 0, 73, 97, 0).speed(4)
                .attack(magic(11, 10, "minecraft:flame")).size(0.7f, 1.2f, 1.0f).flying()
                .skin(0xC06030, 0x904020, 0x602010, 0xFFC060));
        // ---------------- Barbarian Assault ----------------
        // Wave 1 stats (OSRS Wiki); BarbarianAssaultSession sets each wave's stats and max hit (BaWaves). Fighters and rangers
        // only take damage from the attacker's called style; runners are lured into traps and healers poisoned with food, so
        // neither has an attack of its own (healers poison players for 2 through the session).
        list.add(m("penance_fighter", "Penance Fighter", 30).stats(28, 26, 26, 25, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 4)).size(0.8f, 1.5f, 1.1f).skin(0x6A5A3A, 0x4A3A2A, 0x2A2A1A, 0xC0A060));
        list.add(m("penance_ranger", "Penance Ranger", 21).stats(20, 22, 1, 21, 1, 23).speed(4)
                .attack(ranged(3, 9)).size(0.8f, 1.5f, 1.1f).skin(0x5A6A3A, 0x3A4A2A, 0x2A3A1A, 0xA0C060));
        list.add(m("penance_runner", "Penance Runner", 1).stats(5, 1, 1, 1, 1, 1).speed(4).passive().moveSpeed(0.32)
                .attack(NONE).size(0.7f, 1.0f, 0.9f).skin(0x5A5A6A, 0x3A3A4A, 0x2A2A3A, 0xA0A0C0));
        list.add(m("penance_healer", "Penance Healer", 1).stats(27, 9, 8, 9, 1, 1).speed(4).passive()
                .attack(NONE).size(0.7f, 1.3f, 1.0f).skin(0x6A5A5A, 0x4A3A3A, 0x2A2A2A, 0xE0A0A0));
        // Wave 10's queen spawns: anyone may fight them with any style (OSRS Wiki, Queen spawn).
        list.add(m("queen_spawn", "Queen spawn", 63).stats(45, 60, 62, 50, 1, 1).speed(4)
                .attack(melee(AttackType.CRUSH, 7)).size(0.8f, 1.2f, 1.0f).skin(0x7A6A3A, 0x5A4A2A, 0x3A2A1A, 0xE0C060));
        // ---------------- Wintertodt / Tempoross / Guardians (scenery creatures) ----------------
        list.add(m("wintertodt", "Wintertodt", 0).stats(255, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).size(2.0f, 4.0f, 2.5f).skin(0xA0C8F0, 0x80A8E0, 0x6088C0, 0xFFFFFF));
        list.add(m("tempoross", "Tempoross", 0).stats(255, 1, 1, 1, 1, 1).passive().moveSpeed(0.0)
                .immune(AttackType.STAB, AttackType.SLASH, AttackType.CRUSH, AttackType.MAGIC, AttackType.RANGED)
                .attack(NONE).size(2.5f, 4.0f, 2.5f).skin(0x3A6A9A, 0x2A5A8A, 0x1A4A7A, 0xE0F0FF));
        // Volcanic Mine lava beast (OSRS Wiki: 60 hitpoints, 70 Defence, 20 Magic, ranged max hit 18 every 6 ticks,
        // defence stab 20 / slash 140 / crush 50 / magic -30).
        list.add(MonsterDef.of(VolcanicMine.LAVA_BEAST, "Lava beast").level(0).stats(VolcanicMine.LAVA_BEAST_HP, 1, 1, 70, 20, 1)
                .speed(6).defenceBonus(20, 140, 50, -30, 0).attack(ranged(18, 8).particle("minecraft:lava"))
                .size(1.4f, 1.4f, 1.3f).skin(0xC04010, 0x802008, 0x601008, 0xFFB030));
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
            // OSRS Wiki infoboxes: every tier has the same bonuses (stab 50, slash 25, crush 19, magic and ranged 400), bronze,
            // iron and adamant crush and the others stab; animated armour must be fought in melee.
            AttackType style = tier.equals("bronze") || tier.equals("iron") || tier.equals("adamant") ? AttackType.CRUSH : AttackType.STAB;
            list.add(MonsterDef.of("animated_" + tier + "_armour", "Animated " + name + " Armour").level(lvl)
                    .stats(hp, lvl, lvl, lvl, 1, 1).speed(4).defenceBonus(50, 25, 19, 400, 400).attackBonus(4, 0, 0)
                    .immune(AttackType.RANGED, AttackType.MAGIC).attack(melee(style, max)).skin(c, c, c, 0x101010));
        }
        // The guild's cyclopes (OSRS Wiki "Cyclops": levels 56 and 76 on the top floor, 106 in the basement) must be fought in
        // melee.
        list.add(m("warriors_guild_cyclops", "Cyclops", 56).model("cyclops").stats(75, 47, 50, 26, 1, 1).speed(4)
                .immune(AttackType.RANGED, AttackType.MAGIC)
                .attack(melee(AttackType.CRUSH, 6)).size(0.9f, 2.4f, 1.3f).skin(0xA08060, 0x6A4A2A, 0x4A3A2A, 0x202020));
        MonsterDef cyclops76 = m(MinigameRules.WarriorsGuild.CYCLOPS_76, "Cyclops", 76).model("cyclops").stats(100, 65, 65, 35, 1, 1)
                .speed(4).immune(AttackType.RANGED, AttackType.MAGIC)
                .attack(melee(AttackType.CRUSH, 7)).size(0.9f, 2.4f, 1.4f).skin(0x987858, 0x624226, 0x463626, 0x202020);
        cyclops76.tiles = 2;
        list.add(cyclops76);
        list.add(m("cyclops_elite", "Cyclops", 106).model("cyclops").stats(150, 95, 75, 55, 1, 1).speed(4)
                .immune(AttackType.RANGED, AttackType.MAGIC)
                .attack(melee(AttackType.CRUSH, 8)).size(0.9f, 2.4f, 1.5f).skin(0x907050, 0x5A3A2A, 0x3A2A1A, 0x202020));
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
        // ---------------- Nightmare Zone: every dream boss form (normal-mode stats; hard dreams raise them in the session) ----------
        for (NmzBosses.Form f : NmzBosses.forms()) {
            list.add(nmzBoss(f));
        }
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
        list.addAll(TempleTrekking.monsters());
        return list;
    }


    /** Hand-picked looks of the first twelve dream bosses: hitbox width, height and scale, then skin colours. */
    private static final java.util.Map<String, float[]> NMZ_SIZE = java.util.Map.ofEntries(
            java.util.Map.entry("nmz_the_kendal", new float[]{1.0f, 2.2f, 1.3f}),
            java.util.Map.entry("nmz_elvarg", new float[]{2.4f, 2.4f, 2.0f}),
            java.util.Map.entry("nmz_dad", new float[]{1.6f, 3.0f, 1.8f}),
            java.util.Map.entry("nmz_arrg", new float[]{1.4f, 2.8f, 1.6f}),
            java.util.Map.entry("nmz_black_knight_titan", new float[]{1.2f, 3.2f, 1.6f}),
            java.util.Map.entry("nmz_bouncer", new float[]{1.2f, 1.2f, 1.4f}),
            java.util.Map.entry("nmz_glod", new float[]{2.0f, 3.6f, 2.2f}));
    private static final java.util.Map<String, int[]> NMZ_SKIN = java.util.Map.ofEntries(
            java.util.Map.entry("nmz_count_draynor", new int[]{0xE0E0E0, 0x2A2A2A, 0x1A1A1A, 0xA01010}),
            java.util.Map.entry("nmz_king_roald", new int[]{0xC8A080, 0x8A2A8A, 0x6A1A6A, 0xE0C040}),
            java.util.Map.entry("nmz_the_kendal", new int[]{0xD0D0D0, 0xB0B0B0, 0x909090, 0x5A3A2A}),
            java.util.Map.entry("nmz_me", new int[]{0x2A2A2A, 0x1A1A1A, 0x101010, 0x8A2A8A}),
            java.util.Map.entry("nmz_elvarg", new int[]{0x2A6A2A, 0x1A4A1A, 0x103010, 0xE04010}),
            java.util.Map.entry("nmz_tree_spirit", new int[]{0x5A7A3A, 0x4A6A2A, 0x3A5A1A, 0x8AB04A}),
            java.util.Map.entry("nmz_dad", new int[]{0x6A7A6A, 0x5A6A5A, 0x4A5A4A, 0x3A3A3A}),
            java.util.Map.entry("nmz_khazard_warlord", new int[]{0xC8A080, 0x6A2A2A, 0x4A1A1A, 0x3A3A3A}),
            java.util.Map.entry("nmz_arrg", new int[]{0x6A6A5A, 0x5A5A4A, 0x4A4A3A, 0x3A3A2A}),
            java.util.Map.entry("nmz_black_knight_titan", new int[]{0x2A2A2A, 0x1A1A1A, 0x101010, 0x6A6A6A}),
            java.util.Map.entry("nmz_bouncer", new int[]{0x6A2A1A, 0x4A1A10, 0x2A1008, 0xE06020}),
            java.util.Map.entry("nmz_glod", new int[]{0x8A7A5A, 0x6A5A3A, 0x4A3A2A, 0x3A2A1A}),
            java.util.Map.entry("nmz_trapped_soul", new int[]{0xC0C8D0, 0x8A9AAA, 0x6A7A8A, 0x60A0FF}),
            java.util.Map.entry("nmz_corsair_traitor", new int[]{0xC8A080, 0x7A2A2A, 0x3A2A1A, 0xE0C040}),
            java.util.Map.entry("nmz_sand_snake", new int[]{0xC8A860, 0xA88A40, 0x8A6A30, 0x202020}),
            java.util.Map.entry("nmz_headless_beast", new int[]{0x8A6A4A, 0x6A4A2A, 0x4A3A2A, 0x3A2A1A}),
            java.util.Map.entry("nmz_elven_traitor", new int[]{0xE8C8A8, 0x3A6A3A, 0x2A4A2A, 0x80C060}));

    /**
     * Bones and remains a dream boss drops (OSRS Wiki boss table "Drops": "all bosses drop their bone-variation"), limited to
     * the built-in items a module may reference; the boss table's ashes, leaves, bananas, choc-ices and chocolate cakes are
     * not given. approx
     */
    private static final java.util.Map<String, String> NMZ_DROP = java.util.Map.ofEntries(
            java.util.Map.entry("nmz_corsair_traitor", "bones"), java.util.Map.entry("nmz_king_roald", "bones"),
            java.util.Map.entry("nmz_witchs_experiment_wolf", "wolf_bones"), java.util.Map.entry("nmz_the_kendal", "bones"),
            java.util.Map.entry("nmz_me", "bones"), java.util.Map.entry("nmz_elvarg", "dragon_bones"),
            java.util.Map.entry("nmz_moss_guardian", "big_bones"), java.util.Map.entry("nmz_skeleton_hellhound", "big_bones"),
            java.util.Map.entry("nmz_dagannoth_mother", "bones"), java.util.Map.entry("nmz_dad", "big_bones"),
            java.util.Map.entry("nmz_khazard_warlord", "bones"), java.util.Map.entry("nmz_arrg", "big_bones"),
            java.util.Map.entry("nmz_black_knight_titan", "big_bones"), java.util.Map.entry("nmz_glod", "big_bones"),
            java.util.Map.entry("nmz_evil_chicken", "bones"), java.util.Map.entry("nmz_flambeed", "cake"),
            java.util.Map.entry("nmz_dessourt", "chocolate_dust"), java.util.Map.entry("nmz_gelatinnoth_mother", "bones"),
            java.util.Map.entry("nmz_culinaromancer", "cake"), java.util.Map.entry("nmz_giant_roc", "big_bones"),
            java.util.Map.entry("nmz_dessous", "vampyre_dust"), java.util.Map.entry("nmz_damis_second", "bones"));

    private static AttackType nmzType(String style) {
        return switch (style) {
            case "Stab" -> AttackType.STAB;
            case "Slash" -> AttackType.SLASH;
            case "Magic", "Magic_melee", "Dragonfire" -> AttackType.MAGIC;
            case "Ranged" -> AttackType.RANGED;
            default -> AttackType.CRUSH;
        };
    }

    /**
     * A dream boss form from {@link NmzBosses}: the infobox's stats, bonuses and attack styles. Every style hits up to the
     * infobox max hit (approx for the styles it isn't given for); Moss Guardian's magic melee ignores protection prayers,
     * Elvarg breathes dragonfire, Dad knocks back.
     */
    private static MonsterDef nmzBoss(NmzBosses.Form f) {
        NmzBosses.Boss b = NmzBosses.ofForm(f.id());
        MonsterDef d = MonsterDef.of(f.id(), b.name()).level(f.level())
                .stats(f.hitpoints(), f.attack(), f.strength(), f.defence(), f.magic(), f.ranged()).speed(f.speed())
                .defenceBonus(f.stabDefence(), f.slashDefence(), f.crushDefence(), f.magicDefence(), f.rangedDefence())
                .attackBonus(f.meleeAttackBonus(), f.magicAttackBonus(), f.rangedAttackBonus());
        double reach = 2.5 + 0.5 * Math.max(0, f.size() - 1);
        // The main attack (the one the infobox max hit is given for) first, then one attack per other style.
        java.util.List<String> styles = new java.util.ArrayList<>();
        styles.add(f.hitStyle());
        styles.addAll(f.styles());
        boolean melee = false;
        boolean magic = false;
        boolean ranged = false;
        for (String style : styles) {
            AttackType type = nmzType(style);
            int weight = style.equals(f.hitStyle()) ? 10 : 5;
            if (style.equals("Dragonfire")) {
                // Elvarg's dragonfire (approx max hit; OSRS provides an anti-dragon shield in the arena).
                d.attack(AttackDef.of("dragonfire", AttackType.MAGIC, 70, 8, 4).special(Special.DRAGONFIRE, 0)
                        .particle("minecraft:flame"));
            } else if (style.equals("Magic_melee")) {
                d.attack(AttackDef.of("magic melee", AttackType.MAGIC, f.maxHit(), reach, weight)
                        .special(Special.PRAYER_PIERCING, 0).particle("minecraft:witch"));
            } else if (type == AttackType.MAGIC) {
                if (!magic) {
                    magic = true;
                    d.attack(AttackDef.of("magic", AttackType.MAGIC, f.maxHit(), 8, weight).particle("minecraft:witch"));
                }
            } else if (type == AttackType.RANGED) {
                if (!ranged) {
                    ranged = true;
                    d.attack(AttackDef.of("ranged", AttackType.RANGED, f.maxHit(), 8, weight).particle("minecraft:crit"));
                }
            } else if (!melee) {
                melee = true;
                AttackDef a = AttackDef.of("melee", type, f.maxHit(), reach, weight);
                d.attack(f.id().equals("nmz_dad") ? a.special(Special.KNOCKBACK, 3) : a);
            }
        }
        float[] size = NMZ_SIZE.get(f.id());
        if (size == null && f.size() > 1) {
            size = new float[]{0.8f + 0.2f * f.size(), 2.0f + 0.2f * f.size(), 1.0f + 0.2f * f.size()};
        }
        if (size != null) {
            d.size(size[0], size[1], size[2]);
        }
        int[] skin = NMZ_SKIN.getOrDefault(f.id(), new int[]{0x8A7A6A, 0x5A4A3A, 0x3A2A1A, 0xC0A040});
        d.skin(skin[0], skin[1], skin[2], skin[3]);
        if (!f.attributes().isEmpty()) {
            d.attributes(f.attributes().toArray(String[]::new));
        }
        MinigameRules.NightmareZone.SlayerCredit slayer = MinigameRules.NightmareZone.SLAYER.get(f.id());
        if (slayer != null) {
            d.slayer(1, slayer.category());
        }
        String drop = NMZ_DROP.get(f.id());
        if (drop != null) {
            d.drops(Drop.always(drop));
        }
        d.tiles = f.size();
        return d;
    }


    public static List<BossDef> bosses() {
        List<BossDef> list = new ArrayList<>();
        list.add(BossDef.of("tzkal_zuk", "TzKal-Zuk").level(1400).stats(1200, 350, 600, 260, 150, 400).speed(10)
                .defenceBonus(0, 0, 0, 350, 100).model("tzkal_zuk")
                .attack(AttackDef.of("typeless", AttackType.MAGIC, 148, 30, 10).special(Special.PRAYER_PIERCING, 0)
                        .particle("minecraft:flame").message("TzKal-Zuk unleashes a blast of fire!"))
                .moveSpeed(0.0).raidOnly().category("Minigames").arena(16, "lava").size(2.0f, 3.5f, 2.6f)
                .skin(0xC04010, 0x803010, 0x501808, 0xFFD040));
        list.add(BossDef.of("penance_queen", "Penance Queen").level(209).stats(250, 260, 92, 132, 1, 116).speed(4)
                .model("penance_queen")
                // OSRS Wiki: a melee hit of up to 5 and ranged attacks of up to 13; her unblockable volley at every player every
                // 15 ticks and her spawns are BarbarianAssaultSession's. Only omega eggs hurt her (PenanceQueenScript).
                .attack(melee(AttackType.CRUSH, 5))
                .attack(ranged(13, 10))
                .raidOnly().category("Minigames").size(1.6f, 2.2f, 2.0f)
                .skin(0x7A6A3A, 0x5A4A2A, 0x3A2A1A, 0xE0C060));
        return list;
    }
}
