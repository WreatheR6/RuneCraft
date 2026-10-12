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

    public record Rolled(String item, int count) {
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
                out.add(new Rolled(d.item(), count));
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
        Herb[] herbs = Herb.values();
        return herbs[Math.min(herbs.length - 1, (int) Math.floor(Math.pow(random.nextDouble(), 2.5) * herbs.length))].seedItem();
    }
}
