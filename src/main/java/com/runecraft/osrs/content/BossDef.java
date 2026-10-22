package com.runecraft.osrs.content;

import java.util.ArrayList;
import java.util.List;

/**
 * A data-driven boss. The generic boss entity picks attacks from {@link #attacks} (or the active
 * {@link Phase}'s list), applies {@link AttackDef.Special} mechanics, shows a boss bar, tracks kill
 * counts and rolls {@link #drops}. Truly unique behaviour can be added by registering a
 * {@code com.runecraft.boss.BossScript} with the same id.
 *
 * <p>A boss is fought at {@link #place}: an arena is generated there with a summoning block
 * ({@code <id>_summoning_stone}) that consumes {@link #key} if set.
 */
public final class BossDef extends CombatProfile<BossDef> {
    /** A phase that starts when the boss drops below a percentage of its hitpoints. */
    public record Phase(int belowPercent, List<AttackDef> attacks, double damageMultiplier, String message,
                        boolean summonMinions) {
    }

    public final List<Phase> phases = new ArrayList<>();
    public String place;
    public String key;
    public String pet;
    public String minion;
    public int minionCount = 2;
    public int arenaRadius = 12;
    /** Arena floor/wall style: "stone", "lava", "ice", "crypt", "jungle", "void", "water". */
    public String arenaStyle = "stone";
    /** Raid bosses are only spawned by their raid, never by a summoning stone. */
    public boolean raidOnly;
    public boolean wilderness;
    public String category = "Bosses";
    /** The category of the bosses a quest adds ({@code QuestBook.boss}). */
    public static final String QUEST_BOSSES = "Quest bosses";

    public BossDef(String id, String name) {
        super(id, name);
        this.size(0.9f, 2.6f, 1.4f);
    }

    public static BossDef of(String id, String name) {
        return new BossDef(id, name);
    }

    public BossDef phase(int belowPercent, String message, double damageMultiplier, boolean summon, AttackDef... attacks) {
        phases.add(new Phase(belowPercent, List.of(attacks), damageMultiplier, message, summon));
        return this;
    }

    public BossDef at(String place) {
        this.place = place;
        return this;
    }

    public BossDef key(String key) {
        this.key = key;
        return this;
    }

    public BossDef pet(String pet) {
        this.pet = pet;
        return this;
    }

    public BossDef minion(String monsterId, int count) {
        this.minion = monsterId;
        this.minionCount = count;
        return this;
    }

    public BossDef arena(int radius, String style) {
        this.arenaRadius = radius;
        this.arenaStyle = style;
        return this;
    }

    /**
     * An underground lair: the arena is carved at block (x, floorY, z) below ground instead of beside the place on the
     * surface ({@code ArenaLayout}); null for a surface arena. The bosses module carves the cave around it and its way in.
     */
    public int[] lair;

    /** The arena lies underground, centred on OSRS tile (tileX, tileY), its floor at block height {@code floorY}. */
    public BossDef lair(int tileX, int tileY, int floorY) {
        this.lair = new int[]{com.runecraft.osrs.GielinorMap.toBlockX(tileX), floorY, com.runecraft.osrs.GielinorMap.toBlockZ(tileY)};
        return this;
    }

    public BossDef raidOnly() {
        this.raidOnly = true;
        return this;
    }

    public BossDef wilderness() {
        this.wilderness = true;
        return this;
    }

    public BossDef category(String category) {
        this.category = category;
        return this;
    }

    /**
     * A boss fought as part of a quest. OSRS keeps no kill count for these (wiki, Boss: "Excluding minigame and quest
     * bosses, each boss has its own hiscore table"): no kill count message, no boss log line; the kill is the quest's.
     */
    public boolean questBoss() {
        return QUEST_BOSSES.equals(category);
    }

    public String summonBlockId() {
        return id + "_summoning_stone";
    }
}
