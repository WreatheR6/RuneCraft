package com.runecraft.client;

import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Quest;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.Spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Client-side mirror of the player's RuneCraft state, filled by sync packets. */
public final class ClientState {
    public static final SkillSet SKILLS = new SkillSet();
    public static int prayerPoints;
    public static int prayerMask;
    public static CombatStyle style = CombatStyle.ACCURATE;
    public static Spell autocast;
    /** Combat Options: Auto Retaliate (on by default). */
    public static boolean autoRetaliate = true;
    /** Standing in a multicombat area (shows the crossed-swords icon). */
    public static boolean inMulti;
    public static long coins;
    /** The tokkul pouch (OSRS's single tokkul stack). */
    public static long tokkul;
    public static String slayerTask = "";
    public static int slayerRemaining;
    /** Quest stages indexed like {@code ContentRegistry.quests()}. */
    public static final int[] QUEST_STAGES = new int[com.runecraft.osrs.content.ContentRegistry.quests().size()];
    public static int questPoints;
    public static final Set<String> COLLECTION_LOG = new HashSet<>();
    /** Boss/raid/clue kill counts (PlayerData kill-count keys), for the collection log. */
    public static final java.util.Map<String, Integer> KILL_COUNTS = new java.util.HashMap<>();

    public static final List<XpDrop> XP_DROPS = new ArrayList<>();
    public static final Map<Integer, List<Hitsplat>> HITSPLATS = new HashMap<>();

    public record XpDrop(Skill skill, int tenths, long time) {
    }

    public record Hitsplat(int damage, byte type, float hpRatio, long time) {
    }

    public record MenuEntry(String recipeId, String name, String icon, int level, int skill, boolean available) {
    }

    public static MenuType menuType;
    public static final List<MenuEntry> MENU = new ArrayList<>();

    private ClientState() {
    }

    public static boolean prayerActive(Prayer p) {
        return (prayerMask & (1 << p.ordinal())) != 0;
    }
}
