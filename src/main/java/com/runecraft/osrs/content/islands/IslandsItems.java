package com.runecraft.osrs.content.islands;

import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.content.ItemSink;

/**
 * Island drops: Fossil Island's numulites and unidentified fossils (from ammonite crabs), yak hides,
 * yak-hair and meat from Neitiznot's yaks, and suqah hides and teeth from Lunar Isle's suqahs.
 */
public final class IslandsItems {
    private IslandsItems() {
    }

    public static void add(ItemSink items) {
        items.add("numulite", "Numulite", Kind.SIMPLE, "shard", 0xD8C8A0).value(1).stack(Integer.MAX_VALUE)
                .examine("A fossilised shell. Fossil Island's currency.");
        items.add("unidentified_small_fossil", "Unidentified small fossil", Kind.SIMPLE, "ore", 0xC0B090).value(5).stack(64)
                .examine("A small fossil encased in rock. Clean it at the Museum Camp.");
        items.add("unidentified_medium_fossil", "Unidentified medium fossil", Kind.SIMPLE, "ore", 0xB0A080).value(5).stack(64)
                .examine("A fossil encased in rock. Clean it at the Museum Camp.");
        items.add("unidentified_large_fossil", "Unidentified large fossil", Kind.SIMPLE, "ore", 0xA09070).value(5).stack(64)
                .examine("A large fossil encased in rock. Clean it at the Museum Camp.");
        items.add("unidentified_rare_fossil", "Unidentified rare fossil", Kind.SIMPLE, "ore", 0x908060).value(5).stack(64)
                .examine("A rare fossil encased in rock. The museum will want this.");
        items.add("yak_hide", "Yak-hide", Kind.SIMPLE, "hide", 0x6A5A4A).value(50).stack(64)
                .examine("The hide of a yak. Thakkrad Sigmundson cures them on Neitiznot.");
        items.add("yak_hair", "Hair", Kind.SIMPLE, "wool", 0x8A7A6A).value(10).stack(64)
                .examine("Some yak hair. It could be spun into rope.");
        items.add("raw_yak_meat", "Raw yak meat", Kind.SIMPLE, "meat_raw", 0xB04A4A).value(4).stack(64)
                .examine("Raw meat from a yak.");
        items.add("suqah_hide", "Suqah hide", Kind.SIMPLE, "hide", 0x9A8AB0).value(80).stack(64)
                .examine("The hide of a suqah. The Moonclan make Lunar armour from it.");
        items.add("suqah_tooth", "Suqah tooth", Kind.SIMPLE, "skull", 0xE8E0D0).value(30).stack(64)
                .examine("A sharp suqah tooth, used for Lunar Diplomacy's mystical armour.");
    }
}
