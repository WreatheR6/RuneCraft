package com.runecraft.osrs.content.minigames;

import java.util.function.DoubleSupplier;

/**
 * Guardians of the Rift's defence: charged cells, the barriers on the cell tiles, rift guardians and
 * the Great Guardian's health (OSRS Wiki: Guardians of the Rift "Charging cells" and "Gaining energy",
 * Weak/Medium/Strong/Overcharged Barrier, Cell tile (broken), Uncharged cells).
 *
 * <p>Wiki values: uncharged cells come from the table by the entrance (at most ten carried, one charged
 * cell at a time) and are charged at the active altar, its tier set by the altar (air, mind and body
 * weak; water, cosmic and chaos medium; earth, nature and law strong; fire, death and blood
 * overcharged). A cell on an inactive tile raises a weak barrier (17/20/24/30 energy of each type by the cell); a cell of a
 * higher tier than the barrier strengthens it one tier (7/13/22 energy for a medium/strong/overcharged
 * cell); any other cell recharges it (2/5/9/15 energy, healing 10/25/50/100), capped at 2,500 XP and 450
 * energy per game. Barrier tiers have 50/100/200/400 hitpoints. Cell uses give 30/100/180/250 Runecraft
 * XP; a rift guardian made from a cell with a chisel gives 2x+1 energy of its own type (6/15/27/45) and
 * 80 Crafting XP as well. A broken tile takes 12 guardian fragments to repair, for 25 energy of each
 * type. The first two minutes are preparation; at 60% power the rift damages every barrier by 50.
 *
 * <p>Creatures: after the preparation, abyssal leeches and guardians (and a few walkers; more guardians and
 * walkers after the 60% surge) come out of the rift, walk to a standing barrier and attack it; where a
 * barrier is broken or never raised they walk on to the Great Guardian and drain his health. Rift
 * guardians attack the creatures (the creatures only fight back against them). All creatures die when
 * the rift is sealed. Approx: the creatures' hitpoints, damage, walking time and spawn rate are not
 * published, nor is the number of cell tiles.
 */
public final class GuardiansRift {
    /** Game ticks of preparation before creatures come through (two minutes). */
    public static final int PREP_TICKS = 200;
    public static final int MAX_UNCHARGED_CELLS = 10;
    public static final int CELL_TILES = 4; // approx
    public static final int BUILD_ENERGY = 2;
    public static final int REPAIR_FRAGMENTS = 12;
    public static final int REPAIR_ENERGY = 25;
    public static final double RECHARGE_XP_CAP = 2500;
    public static final int RECHARGE_ENERGY_CAP = 450;
    public static final double GUARDIAN_CRAFTING_XP = 80;
    /** Damage dealt to every barrier when the Great Guardian reaches 60% power. */
    public static final int SURGE_DAMAGE = 50;
    public static final double SURGE_POWER = 60;
    /** Abstracted creature attacks (approx). */
    public static final int ATTACK_INTERVAL = 5;
    public static final int GUARDIAN_HEALTH = 100;
    /** Game ticks a rift guardian fights for (approx). */
    public static final int RIFT_GUARDIAN_TICKS = 150;
    public static final int MAX_RIFT_GUARDIANS = 10;

    public static final String UNCHARGED_CELL = "uncharged_cell";

    // ---- mining and the workbench (OSRS Wiki: Guardian parts / remains / Large guardian remains / Huge guardian remains,
    // Guardian fragments, Guardian essence) ----
    /** Mining XP per successful swing at any remains. */
    public static final double MINING_XP = 5;
    /** Guardian parts and remains give 1-3 fragments a swing, the large remains (56 Agility, east shelf) 2-4. */
    public static final int[] REMAINS_FRAGMENTS = {1, 3};
    public static final int[] LARGE_REMAINS_FRAGMENTS = {2, 4};
    public static final int LARGE_REMAINS_AGILITY = 56;
    /** The huge guardian remains (west shelf, through the portal) give 4-6 guardian essence a swing. */
    public static final int[] HUGE_REMAINS_ESSENCE = {4, 6};
    /** The huge remains' portal: first after 160 seconds, then every 140, open about 25 seconds. */
    public static final int PORTAL_FIRST_TICKS = 267;
    public static final int PORTAL_INTERVAL_TICKS = 233;
    public static final int PORTAL_OPEN_TICKS = 42;

    /** Crafting XP per guardian essence made at the workbench: Crafting level / 10, clamped to 1-5. */
    public static double workbenchXp(int crafting) {
        return Math.max(1.0, Math.min(5.0, crafting / 10.0));
    }

    /** Whether the huge remains' portal is open at a game tick. */
    public static boolean portalOpen(long ticks) {
        if (ticks < PORTAL_FIRST_TICKS) {
            return false;
        }
        return (ticks - PORTAL_FIRST_TICKS) % PORTAL_INTERVAL_TICKS < PORTAL_OPEN_TICKS;
    }

    /**
     * Cell tiers, weakest first: item, barrier hitpoints, place / strengthen / recharge / guardian energy, heal, Runecraft
     * XP. Placing a cell on an inactive tile always raises a weak barrier but gives 17/20/24/30 energy by the cell's tier
     * (OSRS Wiki energy table; 2 before the 2022 change).
     */
    public enum Cell {
        WEAK("weak_cell", "Weak", 50, 17, 0, 2, 6, 10, 30),
        MEDIUM("medium_cell", "Medium", 100, 20, 7, 5, 15, 25, 100),
        STRONG("strong_cell", "Strong", 200, 24, 13, 9, 27, 50, 180),
        OVERCHARGED("overcharged_cell", "Overcharged", 400, 30, 22, 15, 45, 100, 250);

        public final String item;
        public final String label;
        /** Hitpoints of the barrier of this tier. */
        public final int barrierHp;
        public final int placeEnergy;
        public final int strengthenEnergy;
        public final int rechargeEnergy;
        public final int guardianEnergy;
        public final int heal;
        public final double xp;

        Cell(String item, String label, int barrierHp, int placeEnergy, int strengthenEnergy, int rechargeEnergy, int guardianEnergy,
             int heal, double xp) {
            this.item = item;
            this.label = label;
            this.barrierHp = barrierHp;
            this.placeEnergy = placeEnergy;
            this.strengthenEnergy = strengthenEnergy;
            this.rechargeEnergy = rechargeEnergy;
            this.guardianEnergy = guardianEnergy;
            this.heal = heal;
            this.xp = xp;
        }

        public static Cell byItem(String item) {
            for (Cell c : values()) {
                if (c.item.equals(item)) {
                    return c;
                }
            }
            return null;
        }
    }

    /** Tier of cell charged at an altar (index into {@link MinigameRules.Guardians#ALTARS}). */
    public static Cell cellFor(String altar) {
        return switch (altar) {
            case "air", "mind", "body" -> Cell.WEAK;
            case "water", "cosmic", "chaos" -> Cell.MEDIUM;
            case "earth", "nature", "law" -> Cell.STRONG;
            default -> Cell.OVERCHARGED;
        };
    }

    /** What using a cell (or fragments) gave: energy of each type, Runecraft XP and a message. */
    public record Use(boolean ok, int elemental, int catalytic, double xp, String message) {
        static Use fail(String message) {
            return new Use(false, 0, 0, 0, message);
        }
    }

    /** Barrier tier per tile: -1 inactive, -2 broken, otherwise the {@link Cell} ordinal. */
    private final int[] tier = new int[CELL_TILES];
    private final int[] hp = new int[CELL_TILES];
    /** Remaining fighting time of each active rift guardian. */
    private final int[] riftGuardians = new int[MAX_RIFT_GUARDIANS];
    private int guardianHealth = GUARDIAN_HEALTH;
    private boolean surged;

    public GuardiansRift() {
        java.util.Arrays.fill(tier, -1);
    }

    public int tier(int tile) {
        return tier[tile];
    }

    public int hp(int tile) {
        return hp[tile];
    }

    public int guardianHealth() {
        return guardianHealth;
    }

    public int activeRiftGuardians() {
        int n = 0;
        for (int t : riftGuardians) {
            if (t > 0) {
                n++;
            }
        }
        return n;
    }

    /** Short text for a tile: "Weak 32/50", "inactive" or "broken". */
    public String describe(int tile) {
        if (tier[tile] == -1) {
            return "inactive";
        }
        if (tier[tile] == -2) {
            return "broken";
        }
        Cell c = Cell.values()[tier[tile]];
        return c.label + " " + hp[tile] + "/" + c.barrierHp;
    }

    /**
     * Uses a charged cell on a tile. {@code rechargeXp} and {@code rechargeEnergy} are what the player has
     * already earned from recharging this game (for the per-game caps).
     */
    public Use useCell(int tile, Cell cell, double rechargeXp, int rechargeEnergy) {
        int t = tier[tile];
        if (t == -2) {
            return Use.fail("The cell tile is broken. Repair it with " + REPAIR_FRAGMENTS + " guardian fragments first.");
        }
        if (t == -1) {
            tier[tile] = Cell.WEAK.ordinal();
            hp[tile] = Cell.WEAK.barrierHp;
            return new Use(true, cell.placeEnergy, cell.placeEnergy, cell.xp, "You place the cell and a weak barrier rises.");
        }
        if (cell.ordinal() > t) {
            Cell next = Cell.values()[t + 1];
            tier[tile] = next.ordinal();
            hp[tile] = next.barrierHp;
            return new Use(true, cell.strengthenEnergy, cell.strengthenEnergy, cell.xp,
                    "The barrier grows stronger: " + next.label.toLowerCase(java.util.Locale.ROOT) + " barrier.");
        }
        Cell cur = Cell.values()[t];
        hp[tile] = Math.min(cur.barrierHp, hp[tile] + cell.heal);
        int energy = Math.max(0, Math.min(cell.rechargeEnergy, RECHARGE_ENERGY_CAP - rechargeEnergy));
        double xp = Math.max(0, Math.min(cell.xp, RECHARGE_XP_CAP - rechargeXp));
        return new Use(true, energy, energy, xp, "You recharge the barrier (" + hp[tile] + "/" + cur.barrierHp + ").");
    }

    /** Repairs a broken tile with 12 guardian fragments (the caller takes them). */
    public Use repair(int tile) {
        if (tier[tile] != -2) {
            return Use.fail("The cell tile doesn't need repairing.");
        }
        tier[tile] = -1;
        hp[tile] = 0;
        return new Use(true, REPAIR_ENERGY, REPAIR_ENERGY, 0, "You repair the cell tile.");
    }

    /** Assembles a rift guardian from a cell: 2x+1 energy of one type. False when ten are already fighting. */
    public Use assemble(Cell cell, boolean catalytic) {
        for (int i = 0; i < riftGuardians.length; i++) {
            if (riftGuardians[i] <= 0) {
                riftGuardians[i] = RIFT_GUARDIAN_TICKS;
                int e = cell.guardianEnergy;
                return new Use(true, catalytic ? 0 : e, catalytic ? e : 0, cell.xp,
                        "You assemble " + (catalytic ? "a catalytic" : "an elemental") + " guardian to defend the Great Guardian.");
            }
        }
        return Use.fail("There are already " + MAX_RIFT_GUARDIANS + " rift guardians fighting.");
    }

    /** Abyssal creatures of the rift: the creature id they are shown as, hitpoints and damage per attack (approx). */
    public enum CreatureType {
        LEECH("abyssal_leech", 10, 3, 1),
        GUARDIAN("abyssal_guardian", 18, 4, 1),
        WALKER("abyssal_walker", 30, 6, 2);

        public final String creatureId;
        public final int hp;
        /** Damage to a barrier per attack. */
        public final int barrierDamage;
        /** Great Guardian health (percent) drained per attack. */
        public final int guardianDamage;

        CreatureType(String creatureId, int hp, int barrierDamage, int guardianDamage) {
            this.creatureId = creatureId;
            this.hp = hp;
            this.barrierDamage = barrierDamage;
            this.guardianDamage = guardianDamage;
        }
    }

    /** Game ticks a creature takes to walk from the rift to the barriers, and from there to the Great Guardian (approx). */
    public static final int WALK_TICKS = 10;
    /** Damage a rift guardian deals to a creature per attack (approx). */
    public static final int RIFT_GUARDIAN_DAMAGE = 6;
    /** Most creatures out of the rift at once (approx). */
    public static final int MAX_CREATURES = 12;

    /** Where a creature is: walking to the barriers, attacking a barrier, walking on, attacking the Great Guardian. */
    public enum Stage { TO_BARRIER, AT_BARRIER, TO_GUARDIAN, AT_GUARDIAN }

    /** One creature out of the rift, in the lane of one cell tile. */
    public static final class Creature {
        public final int id;
        public final CreatureType type;
        public final int lane;
        private int hp;
        private int walked;
        private Stage stage = Stage.TO_BARRIER;

        Creature(int id, CreatureType type, int lane) {
            this.id = id;
            this.type = type;
            this.lane = lane;
            this.hp = type.hp;
        }

        public int hp() {
            return hp;
        }

        public Stage stage() {
            return stage;
        }

        /** How far along its current walk the creature is (0-1; 1 while attacking). */
        public double progress() {
            return stage == Stage.TO_BARRIER || stage == Stage.TO_GUARDIAN ? Math.min(1.0, walked / (double) WALK_TICKS) : 1.0;
        }
    }

    private final java.util.List<Creature> creatures = new java.util.ArrayList<>();
    private int nextCreature;

    /** Creatures out of the rift (alive). */
    public java.util.List<Creature> creatures() {
        return java.util.Collections.unmodifiableList(creatures);
    }

    /** Game ticks between new creatures: faster with more players and after the surge (approx). */
    public static int spawnInterval(int players, double power) {
        int interval = Math.max(4, 12 - Math.max(1, players));
        return power >= SURGE_POWER ? Math.max(3, interval * 2 / 3) : interval;
    }

    /** Creature kind for a roll in [0, 1): mostly leeches and guardians, a few walkers; more of the latter after the surge. */
    public static CreatureType creatureFor(double roll, double power) {
        if (power >= SURGE_POWER) {
            return roll < 0.2 ? CreatureType.LEECH : roll < 0.7 ? CreatureType.GUARDIAN : CreatureType.WALKER;
        }
        return roll < 0.45 ? CreatureType.LEECH : roll < 0.9 ? CreatureType.GUARDIAN : CreatureType.WALKER;
    }

    /** Seals the rift: every creature dies. */
    public void seal() {
        creatures.clear();
    }

    /**
     * Advances the defence one game tick. Returns a message for the players when something happened
     * (a barrier fell, the surge), otherwise null. The game is lost once {@link #guardianHealth()} is 0.
     */
    public String tick(long ticks, int players, double power, DoubleSupplier random) {
        for (int i = 0; i < riftGuardians.length; i++) {
            if (riftGuardians[i] > 0) {
                riftGuardians[i]--;
            }
        }
        String msg = null;
        if (!surged && power >= SURGE_POWER) {
            surged = true;
            msg = "The temple rumbles as the rift glows more intensely!";
            for (int i = 0; i < CELL_TILES; i++) {
                if (tier[i] >= 0) {
                    String fell = damage(i, SURGE_DAMAGE);
                    if (fell != null) {
                        msg = msg + " " + fell;
                    }
                }
            }
        }
        if (ticks < PREP_TICKS) {
            return msg;
        }
        // New creatures come out of the rift and head for a standing barrier (any lane when none stands).
        if (ticks % spawnInterval(players, power) == 0 && creatures.size() < MAX_CREATURES) {
            CreatureType type = creatureFor(random.getAsDouble(), power);
            creatures.add(new Creature(nextCreature++, type, pickLane(random.getAsDouble())));
        }
        for (Creature c : creatures) {
            if (c.stage == Stage.TO_BARRIER || c.stage == Stage.TO_GUARDIAN) {
                if (++c.walked >= WALK_TICKS) {
                    c.walked = 0;
                    c.stage = c.stage == Stage.TO_BARRIER ? Stage.AT_BARRIER : Stage.AT_GUARDIAN;
                }
            } else if (c.stage == Stage.AT_BARRIER && tier[c.lane] < 0) {
                c.stage = Stage.TO_GUARDIAN;   // its barrier is gone: on to the Great Guardian
            }
        }
        if (ticks % ATTACK_INTERVAL != 0) {
            return msg;
        }
        // Rift guardians attack the creatures nearest the Great Guardian first.
        for (int g : riftGuardians) {
            if (g <= 0 || creatures.isEmpty()) {
                continue;
            }
            Creature target = creatures.get(0);
            for (Creature c : creatures) {
                if (c.stage.ordinal() > target.stage.ordinal()) {
                    target = c;
                }
            }
            target.hp -= RIFT_GUARDIAN_DAMAGE;
            if (target.hp <= 0) {
                creatures.remove(target);
            }
        }
        for (Creature c : creatures) {
            if (c.stage == Stage.AT_BARRIER && tier[c.lane] >= 0) {
                String fell = damage(c.lane, c.type.barrierDamage);
                if (fell != null) {
                    msg = msg == null ? fell : msg + " " + fell;
                }
            } else if (c.stage == Stage.AT_GUARDIAN) {
                guardianHealth = Math.max(0, guardianHealth - c.type.guardianDamage);
            }
        }
        return msg;
    }

    /** A lane for a new creature: one of the standing barriers, or any lane when none stands. */
    private int pickLane(double roll) {
        int standing = 0;
        for (int i = 0; i < CELL_TILES; i++) {
            if (tier[i] >= 0) {
                standing++;
            }
        }
        int pick = (int) (roll * (standing == 0 ? CELL_TILES : standing));
        for (int i = 0; i < CELL_TILES; i++) {
            if ((standing == 0 || tier[i] >= 0) && pick-- == 0) {
                return i;
            }
        }
        return 0;
    }

    private String damage(int tile, int dmg) {
        hp[tile] -= dmg;
        if (hp[tile] > 0) {
            return null;
        }
        hp[tile] = 0;
        tier[tile] = -2;
        return "A barrier has been broken!";
    }
}
