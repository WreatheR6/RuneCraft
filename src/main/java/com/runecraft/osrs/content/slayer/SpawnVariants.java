package com.runecraft.osrs.content.slayer;

import com.runecraft.osrs.Region;
import com.runecraft.osrs.content.MonsterDef;
import com.runecraft.osrs.data.Drop;

import java.util.List;

import static com.runecraft.osrs.content.slayer.Kit.clue;
import static com.runecraft.osrs.content.slayer.Kit.coins;
import static com.runecraft.osrs.content.slayer.Kit.crush;
import static com.runecraft.osrs.content.slayer.Kit.m;
import static com.runecraft.osrs.content.slayer.Kit.slash;
import static com.runecraft.osrs.content.slayer.Kit.stab;

/**
 * Other combat levels of common monsters, for the places where OSRS spawns a different version than the
 * built-in one (checked against the cache spawn list, mejrs/data_osrs NPCList_OSRS.json): Goblin Village's
 * level 5 goblins, the Graveyard of Shadows' level 18
 * and 24 zombies, the Bone Yard's level 25 skeletons and the Combat Training Camp's level 63 ogres (the Dark Wizards' Tower's wizards are in
 * the npcs module's {@code TownMonsters}). Each
 * reuses the base creature's model and counts towards the same Slayer task. Stats come from the wiki's DPS
 * calculator data ({@code MonsterStatsTable}); drops follow the base creature.
 */
final class SpawnVariants {
    private SpawnVariants() {
    }

    static List<MonsterDef> all() {
        return List.of(
                m("goblin_lvl5", "Goblin", 5, 12, 3, 1, 4, 1, 1).speed(6).attack(stab(1)).attackBonus(12, 0, 0)
                        .drops(Drop.of("goblin_champion_scroll", 1, 1, 5000), Drop.always("bones"), coins(1, 9, 4),
                                Drop.of("bronze_scimitar", 1, 1, 64), Drop.of("air_rune", 4, 8, 16), Drop.of("water_rune", 4, 8, 16),
                                Drop.herbs(90))
                        .slayer(1, "goblins").model("goblin").skin(0x6A9A3A, 0x6A4A2A, 0x4A3A2A, 0x202020)
                        .size(0.6f, 1.5f, 0.85f).spawn(Region.ASGARNIA, 3, 2, 4),
                m("zombie_lvl18", "Zombie", 18, 24, 13, 13, 18, 1, 1).speed(5).attack(slash(2)).attackBonus(5, 0, 0)
                        .drops(Drop.of("zombie_champion_scroll", 1, 1, 5000), Drop.always("bones"), coins(1, 25, 4),
                                Drop.herbs(40), Drop.of("eye_of_newt", 1, 1, 16))
                        .slayer(1, "zombies").model("zombie").skin(0x5A7A4A, 0x3A4A6A, 0x3A3A2A, 0x202020)
                        .spawn(Region.WILDERNESS, 4, 2, 4),
                m("zombie_lvl24", "Zombie", 24, 30, 19, 21, 16, 1, 1).speed(5).attack(slash(3)).attackBonus(5, 0, 0)
                        .defenceBonus(9, 8, 12, 10, 11)
                        .drops(Drop.of("zombie_champion_scroll", 1, 1, 5000), Drop.always("bones"), coins(2, 30, 4),
                                Drop.herbs(40), Drop.of("eye_of_newt", 1, 1, 16), clue("beginner", 70))
                        .slayer(1, "zombies").model("zombie").skin(0x5A7A4A, 0x5A3A2A, 0x3A3A2A, 0x202020)
                        .spawn(Region.WILDERNESS, 3, 1, 3),
                m("skeleton_lvl25", "Skeleton", 25, 17, 24, 24, 24, 1, 1).speed(4).attack(slash(4)).attackBonus(15, 0, 0)
                        .defenceBonus(9, 11, -2, 1, 4)
                        .drops(Drop.of("skeleton_champion_scroll", 1, 1, 5000), Drop.always("bones"), coins(2, 30, 4),
                                Drop.of("iron_arrow", 2, 10, 6), Drop.herbs(40), clue("beginner", 70))
                        .slayer(1, "skeletons").model("skeleton").skin(0xE0DCC8, 0xC8C4B0, 0xB0AC98, 0x202020)
                        .spawn(Region.WILDERNESS, 4, 2, 4),
                m("ogre_lvl63", "Ogre", 63, 60, 54, 54, 54, 1, 1).speed(4).attack(crush(7)).attackBonus(8, 0, 0)
                        .defenceBonus(19, 23, 24, 0, 0)
                        .drops(Drop.always("big_bones"), coins(5, 100, 4), Drop.herbs(18), clue("medium", 128))
                        .slayer(1, "ogres").model("ogre").skin(0x8AA060, 0x6A5A3A, 0x4A3A2A, 0x3A3A2A).size(1.0f, 2.6f, 1.0f)
                        .spawn(Region.KANDARIN, 3, 1, 3));
    }
}
