package com.runecraft.skill;

import com.runecraft.osrs.data.Allotment;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.data.Drop;
import com.runecraft.osrs.data.DropTables;
import com.runecraft.osrs.data.Gem;
import com.runecraft.osrs.data.Herb;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.List;

/** Rolls OSRS-style drop tables, including the shared herb, seed and gem tables. */
public final class Drops {
    private Drops() {
    }

    /** A rolled item ({@code doses} > 0 for a potion dropped with 1-3 doses; see {@link Drop#doses()}). */
    public record Rolled(String item, int count, int doses) {
        public Rolled(String item, int count) {
            this(item, count, 0);
        }
    }

    /** {@code stack} with {@code doses} doses left when it is a partly drunk potion (doses are item damage). */
    public static net.minecraft.item.ItemStack withDoses(net.minecraft.item.ItemStack stack, int doses) {
        if (doses > 0 && doses < 4 && !stack.isEmpty()) {
            com.runecraft.osrs.ItemDef def = ItemCatalog.get(com.runecraft.util.Inv.idOf(stack));
            if (def != null && def.kind() == com.runecraft.osrs.ItemDef.Kind.POTION) {
                stack.setDamage(4 - doses);
            }
        }
        return stack;
    }

    public static List<Rolled> roll(List<Drop> table, Random random, boolean ringOfWealth) {
        List<Rolled> out = new ArrayList<>();
        for (Drop d : table) {
            if (d.rarity() <= 1 || random.nextInt(d.rarity()) == 0) {
                if (d.item().startsWith("#")) {
                    // Shared OSRS tables (gems, rare, herbs, seeds): some slots are empty; items not in this
                    // build are skipped.
                    DropTables.Result r = DropTables.roll(d.item(), random::nextInt);
                    if (r != null && ItemCatalog.get(r.item()) != null) {
                        out.add(new Rolled(r.item(), r.count()));
                    }
                    continue;
                }
                int count = d.min() + (d.max() > d.min() ? random.nextInt(d.max() - d.min() + 1) : 0);
                out.add(new Rolled(d.baseItem(), count, d.doses()));
            }
        }
        return out;
    }

    /** One grimy herb from the OSRS herb drop table. */
    public static String rollHerb(Random random) {
        DropTables.Result r = DropTables.roll("#herbs", random::nextInt);
        return r != null ? r.item() : Herb.GUAM.grimyItem();
    }

    public static String rollGem(Random random) {
        int r = random.nextInt(64);
        if (r < 32) {
            return Gem.SAPPHIRE.uncutItem();
        } else if (r < 48) {
            return Gem.EMERALD.uncutItem();
        } else if (r < 58) {
            return Gem.RUBY.uncutItem();
        } else if (r < 63) {
            return Gem.DIAMOND.uncutItem();
        }
        return Gem.DRAGONSTONE.uncutItem();
    }

    public static String rollSeed(Random random) {
        if (random.nextBoolean()) {
            Allotment[] a = Allotment.values();
            return a[Math.min(a.length - 1, (int) Math.floor(Math.pow(random.nextDouble(), 2) * a.length))].seedItem();
        }
        // Huasca seeds only come from Varlamore (not the general seed drop table).
        Herb[] herbs = java.util.Arrays.stream(Herb.values()).filter(h -> h != Herb.HUASCA).toArray(Herb[]::new);
        return herbs[Math.min(herbs.length - 1, (int) Math.floor(Math.pow(random.nextDouble(), 2.5) * herbs.length))].seedItem();
    }
}
