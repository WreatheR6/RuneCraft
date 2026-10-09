package com.runecraft.osrs.data;

/** Bows per wood type: fletching levels/XP (cutting and stringing give the same XP) and stats. */
public enum BowWood {
    NORMAL(Tree.TREE, "", "", 5, 5.0, 10, 10.0, 8, 1, 50),
    OAK(Tree.OAK, "oak_", "Oak ", 20, 16.5, 25, 25.0, 14, 5, 100),
    WILLOW(Tree.WILLOW, "willow_", "Willow ", 35, 33.3, 40, 41.5, 20, 20, 200),
    MAPLE(Tree.MAPLE, "maple_", "Maple ", 50, 50.0, 55, 58.3, 29, 30, 400),
    YEW(Tree.YEW, "yew_", "Yew ", 65, 67.5, 70, 75.0, 47, 40, 800),
    MAGIC(Tree.MAGIC, "magic_", "Magic ", 80, 83.3, 85, 91.5, 69, 50, 1600);

    private final Tree tree;
    private final String prefix;
    private final String namePrefix;
    private final int shortLevel;
    private final double shortXp;
    private final int longLevel;
    private final double longXp;
    private final int rangedAttack;
    private final int rangedLevel;
    private final int value;

    BowWood(Tree tree, String prefix, String namePrefix, int shortLevel, double shortXp, int longLevel, double longXp,
            int rangedAttack, int rangedLevel, int value) {
        this.tree = tree;
        this.prefix = prefix;
        this.namePrefix = namePrefix;
        this.shortLevel = shortLevel;
        this.shortXp = shortXp;
        this.longLevel = longLevel;
        this.longXp = longXp;
        this.rangedAttack = rangedAttack;
        this.rangedLevel = rangedLevel;
        this.value = value;
    }

    public Tree tree() {
        return tree;
    }

    public String bowId(boolean longbow, boolean strung) {
        return prefix + (longbow ? "longbow" : "shortbow") + (strung ? "" : "_u");
    }

    public String bowName(boolean longbow, boolean strung) {
        String base = namePrefix + (longbow ? "longbow" : "shortbow");
        base = Character.toUpperCase(base.charAt(0)) + base.substring(1);
        return strung ? base : base + " (u)";
    }

    public int level(boolean longbow) {
        return longbow ? longLevel : shortLevel;
    }

    public double xp(boolean longbow) {
        return longbow ? longXp : shortXp;
    }

    public int rangedAttack() {
        return rangedAttack;
    }

    public int rangedLevel() {
        return rangedLevel;
    }

    public int value(boolean longbow) {
        return longbow ? value * 8 / 5 : value;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
