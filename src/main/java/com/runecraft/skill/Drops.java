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
    private static final int[] HERB_WEIGHTS = {32, 24, 18, 14, 11, 4, 8, 6, 5, 2, 4, 3, 3, 1};

    private Drops() {
    }

    public record Rolled(String item, int count) {
    }

    public static List<Rolled> roll(List<Drop> table, Random random, boolean ringOfWealth) {
        List<Rolled> out = new ArrayList<>();
        for (Drop d : table) {
            if (d.rarity() <= 1 || random.nextInt(d.rarity()) == 0) {
                if (d.item().equals("#gems") || d.item().equals("#rare")) {
                    // Shared OSRS tables: many slots are empty; items not in this build are skipped.
                    DropTables.Result r = DropTables.roll(d.item(), random::nextInt);
                    if (r != null && ItemCatalog.get(r.item()) != null) {
                        out.add(new Rolled(r.item(), r.count()));
                    }
                    continue;
                }
                String item = switch (d.item()) {
                    case "#herbs" -> rollHerb(random);
                    case "#seeds" -> rollSeed(random);
                    default -> d.item();
                };
                if (item == null) {
                    continue;
                }
                int count = d.min() + (d.max() > d.min() ? random.nextInt(d.max() - d.min() + 1) : 0);
                out.add(new Rolled(item, count));
            }
        }
        return out;
    }

    public static String rollHerb(Random random) {
        int total = 0;
        for (int w : HERB_WEIGHTS) {
            total += w;
        }
        int r = random.nextInt(total);
        Herb[] herbs = Herb.values();
        for (int i = 0; i < herbs.length; i++) {
            r -= HERB_WEIGHTS[i];
            if (r < 0) {
                return herbs[i].grimyItem();
            }
        }
        return Herb.GUAM.grimyItem();
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
