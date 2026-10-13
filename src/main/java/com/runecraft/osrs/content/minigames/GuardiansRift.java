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
 * overcharged). A cell on an inactive tile raises a weak barrier (2 energy of each type); a cell of a
 * higher tier than the barrier strengthens it one tier (7/13/22 energy for a medium/strong/overcharged
 * cell); any other cell recharges it (2/5/9/15 energy, healing 10/25/50/100), capped at 2,500 XP and 450
 * energy per game. Barrier tiers have 50/100/200/400 hitpoints. Cell uses give 30/100/180/250 Runecraft
 * XP; a rift guardian made from a cell with a chisel gives 2x+1 energy of its own type (6/15/27/45) and
 * 80 Crafting XP as well. A broken tile takes 12 guardian fragments to repair, for 25 energy of each
 * type. The first two minutes are preparation; at 60% power the rift damages every barrier by 50.
 *
 * <p>Approx: the creatures themselves are abstracted. Every {@link #ATTACK_INTERVAL} ticks after the
 * preparation the rift strikes for {@link #strike} damage (more with more players, half again after
 * 60% power); rift guardians soak part of it, standing barriers take the rest, and only when every
 * barrier is down does the Great Guardian lose health. The number of cell tiles is a guess.
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

    /** Cell tiers, weakest first: item, barrier hitpoints, strengthen / recharge / guardian energy, heal, Runecraft XP. */
    public enum Cell {
        WEAK("weak_cell", "Weak", 50, 0, 2, 6, 10, 30),
        MEDIUM("medium_cell", "Medium", 100, 7, 5, 15, 25, 100),
        STRONG("strong_cell", "Strong", 200, 13, 9, 27, 50, 180),
        OVERCHARGED("overcharged_cell", "Overcharged", 400, 22, 15, 45, 100, 250);

        public final String item;
        public final String label;
        /** Hitpoints of the barrier of this tier. */
        public final int barrierHp;
        public final int strengthenEnergy;
        public final int rechargeEnergy;
        public final int guardianEnergy;
        public final int heal;
        public final double xp;

        Cell(String item, String label, int barrierHp, int strengthenEnergy, int rechargeEnergy, int guardianEnergy, int heal,
             double xp) {
            this.item = item;
            this.label = label;
            this.barrierHp = barrierHp;
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
            return new Use(true, BUILD_ENERGY, BUILD_ENERGY, cell.xp, "You place the cell and a weak barrier rises.");
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

    /** Damage of one abstracted creature strike (approx). */
    public static int strike(int players, double power) {
        int dmg = 6 + 2 * Math.max(1, players);
        return power >= SURGE_POWER ? dmg * 3 / 2 : dmg;
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
        if (ticks < PREP_TICKS || ticks % ATTACK_INTERVAL != 0) {
            return msg;
        }
        int dmg = strike(players, power);
        // Each rift guardian kills some of the creatures before they reach the barriers.
        dmg = Math.max(0, dmg - 2 * activeRiftGuardians());
        if (dmg == 0) {
            return msg;
        }
        int standing = 0;
        for (int i = 0; i < CELL_TILES; i++) {
            if (tier[i] >= 0) {
                standing++;
            }
        }
        if (standing == 0) {
            guardianHealth = Math.max(0, guardianHealth - Math.max(1, dmg / 4));
            return msg;
        }
        int pick = (int) (random.getAsDouble() * standing);
        for (int i = 0; i < CELL_TILES; i++) {
            if (tier[i] >= 0 && pick-- == 0) {
                String fell = damage(i, dmg);
                return fell != null ? (msg == null ? fell : msg + " " + fell) : msg;
            }
        }
        return msg;
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
