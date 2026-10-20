package com.runecraft.osrs.content.quests;

import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Start NPCs audited in round 33 against the wiki's quest details ({@code |start =}) and quest-helper: who starts the quest,
 * and where (cache spawn tiles from mejrs/data_osrs, else quest-helper's tile).
 */
class QuestStartNpcsTest {
    /** quest id, start NPC name, OSRS x, OSRS y. */
    private static final Object[][] STARTS = {
            {"rum_deal", "Pirate Pete", 3680, 3537},
            {"the_corsair_curse", "Captain Tock", 3030, 3273},
            {"kings_ransom", "Gossip", 2742, 3555},
            {"tower_of_life", "Effigy", -1, -1},
            {"a_night_at_the_theatre", "Mysterious Stranger", -1, -1},
            {"a_taste_of_hope", "Garth", 3669, 3215},
            {"getting_ahead", "Gordon", 1248, 3686},
            {"the_queen_of_thieves", "Tomas Lawry", 1796, 3782},
            {"tale_of_the_righteous", "Phileas Rimor", 1543, 3571},
            {"the_ascent_of_arceuus", "Mori", 1698, 3742},
            {"a_kingdom_divided", "Martin Holt", 1664, 3670},
            {"twilights_promise", "Ennius Tullus", 1687, 3141},
            {"the_ribbiting_tale_of_a_lily_pad_labour_dispute", "Marcellus", 1683, 2973},
            {"meat_and_greet", "Emelio", 1753, 3074},
            {"scrambled", "Alan", 1247, 3167},
            {"defender_of_varrock", "Elias White", 3283, 3501},
            {"the_curse_of_arrav", "Elias White", 3505, 3037},
            {"family_crest", "Dimintheis", 3279, 3404},
            {"plague_city", "Edmond", 2568, 3334},
            {"song_of_the_elves", "Edmond", 2568, 3334},
            {"biohazard", "Elena", 2592, 3336},
            {"regicide", "King Lathas", 2572, 3296},
            {"underground_pass", "King Lathas", 2572, 3296},
            {"lunar_diplomacy", "Lokar Searunner", 2620, 3693},
            {"the_fremennik_isles", "Mord Gunnars", 2644, 3709},
            {"fishing_contest", "Vestri", 2820, 3487},
            {"the_feud", "Ali Morrisane", 3304, 3211},
            {"icthlarins_little_helper", "Wanderer", 3315, 2849},
            {"temple_of_the_eye", "Wizard Persten", 3285, 3232},
            {"shadow_of_the_storm", "Father Reen", 3271, 3159},
            {"enakhras_lament", "Lazim", 3191, 2926},
            {"while_guthix_sleeps", "Ivy Sophista", 2907, 3450},
            {"beneath_cursed_sands", "Jamila", 3311, 2780},
    };

    @Test
    void questsStartWithTheirOsrsNpcOnItsTile() {
        List<String> problems = new ArrayList<>();
        for (Object[] row : STARTS) {
            QuestSpec q = QuestBook.get().spec((String) row[0]);
            if (q == null) {
                problems.add(row[0] + ": no such quest");
                continue;
            }
            QuestSpec.Trigger start = q.stages.get(0).triggers.stream().filter(t -> t.kind == QuestSpec.Trigger.Kind.TALK)
                    .findFirst().orElse(null);
            NpcDef npc = start == null ? null : ContentRegistry.npc(start.target);
            if (npc == null || !npc.name().equals(row[1])) {
                problems.add(row[0] + ": starts with " + (npc == null ? "?" : npc.name()) + ", OSRS " + row[1]);
                continue;
            }
            int x = (Integer) row[2];
            int y = (Integer) row[3];
            if (x < 0) {
                continue;
            }
            PlaceDef p = ContentRegistry.place(npc.place());
            int nx = p.osrsX() + npc.dx() / com.runecraft.osrs.GielinorMap.SCALE;
            int ny = p.osrsY() - npc.dz() / com.runecraft.osrs.GielinorMap.SCALE;
            if (Math.max(Math.abs(nx - x), Math.abs(ny - y)) > 6) {
                problems.add(row[0] + ": " + npc.name() + " at " + nx + "," + ny + ", OSRS " + x + "," + y);
            }
        }
        assertTrue(problems.isEmpty(), String.join("\n", problems));
    }
}
