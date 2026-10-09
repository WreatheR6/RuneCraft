package com.runecraft.osrs;

/**
 * Description of one RuneCraft item. The Minecraft layer turns each into a registered item and
 * the asset generator turns {@link #shape()} + {@link #color()} into a texture.
 */
public final class ItemDef {
    public enum Kind {
        SIMPLE, FOOD, BONES, POTION, GRIMY_HERB, AXE, PICKAXE, MELEE_WEAPON, ARMOUR, SHIELD, BOW, ARROW, STAFF,
        TOOL, SEED, RUNE, COINS, SLAYER_GEM, VIAL, TRAP
    }

    private final String id;
    private final String name;
    private final Kind kind;
    private final String shape;
    private final int color;
    private int value = 1;
    private int maxStack = 64;
    private int heal;
    private EquipmentDef equipment;
    private String tier;
    private int rangedStrength;
    private String examine;

    public ItemDef(String id, String name, Kind kind, String shape, int color) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.shape = shape;
        this.color = color;
    }

    public ItemDef value(int value) {
        this.value = value;
        return this;
    }

    public ItemDef stack(int maxStack) {
        this.maxStack = maxStack;
        return this;
    }

    public ItemDef heal(int heal) {
        this.heal = heal;
        return this;
    }

    public ItemDef equipment(EquipmentDef equipment) {
        this.equipment = equipment;
        return this;
    }

    public ItemDef tier(String tier) {
        this.tier = tier;
        return this;
    }

    public ItemDef rangedStrength(int rangedStrength) {
        this.rangedStrength = rangedStrength;
        return this;
    }

    public ItemDef examine(String examine) {
        this.examine = examine;
        return this;
    }

    /** Registry path in the runecraft namespace. */
    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Kind kind() {
        return kind;
    }

    /** Texture template name for the asset generator. */
    public String shape() {
        return shape;
    }

    public int color() {
        return color;
    }

    /** Store value in coins; low alchemy gives 40% of this and high alchemy 60%. */
    public int value() {
        return value;
    }

    public int lowAlch() {
        return (int) Math.floor(value * 0.4);
    }

    public int highAlch() {
        return (int) Math.floor(value * 0.6);
    }

    public int maxStack() {
        return maxStack;
    }

    /** Hitpoints healed when eaten (food only). */
    public int heal() {
        return heal;
    }

    public EquipmentDef equipment() {
        return equipment;
    }

    /** Metal or wood tier name (e.g. "rune", "yew") for tools/weapons/armour, else null. */
    public String tier() {
        return tier;
    }

    public int rangedStrength() {
        return rangedStrength;
    }

    public String examine() {
        return examine;
    }
}
