package com.runecraft.osrs.content.extras;

import com.runecraft.osrs.CollectionLogData;
import com.runecraft.osrs.ItemDef.Kind;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.content.BlockDef;
import com.runecraft.osrs.content.ContentModule;
import com.runecraft.osrs.content.ItemSink;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.model.ModelSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extras: pets (skilling pets, Herbi, Chompy chick, exact boss pet rates, followers and the "All
 * Pets" collection log page), the player-owned house (house portals, rooms, hotspots and functional
 * furniture) and playable Sailing (boats, ports, charter docks and voyages).
 */
public final class ExtrasContent implements ContentModule {
    public static final String HOUSE_PORTAL = "ext_house_portal";
    public static final String HOTSPOT = "ext_poh_hotspot";
    public static final String CHARTER_DOCK = "ext_charter_dock";
    public static final String HERBIBOAR_BURROW = "ext_herbiboar_burrow";
    public static final String HERBIBOAR_SITE = "ext_herbiboar_site";

    /**
     * A town with a house portal: place id, OSRS coordinates of the portal, and the estate agent's Construction level and fee
     * to move a house there (a quest the move needs, or null).
     */
    public record PortalTown(String id, String name, int x, int y, int level, int moveCost, String quest) {
        public String place() {
            return "ext_house_portal_" + id;
        }
    }

    /** New houses start in Rimmington, like OSRS. */
    public static final String START_TOWN = "rimmington";

    public static final List<PortalTown> PORTAL_TOWNS = List.of(
            new PortalTown("rimmington", "Rimmington", 2953, 3224, 1, 5_000, null),
            new PortalTown("taverley", "Taverley", 2894, 3465, 10, 5_000, null),
            new PortalTown("pollnivneach", "Pollnivneach", 3340, 3003, 20, 7_500, null),
            new PortalTown("rellekka", "Rellekka", 2670, 3631, 30, 10_000, null),
            new PortalTown("brimhaven", "Brimhaven", 2758, 3178, 40, 15_000, null),
            new PortalTown("yanille", "Yanille", 2544, 3095, 50, 25_000, null),
            new PortalTown("hosidius", "Hosidius", 1743, 3517, 25, 8_750, null),
            new PortalTown("prifddinas", "Prifddinas", 3239, 6075, 70, 50_000, "song_of_the_elves"));

    /** Why a house can't be moved to {@code to}, or null (boosts don't count in OSRS, so pass the real level). */
    public static String canMove(PortalTown to, String currentTown, int constructionLevel, boolean questDone, long coins) {
        if (to == null) {
            return "There's no house portal there.";
        }
        if (to.id().equals(currentTown)) {
            return "Your house is already in " + to.name() + ".";
        }
        if (constructionLevel < to.level()) {
            return "You need a Construction level of " + to.level() + " to move your house to " + to.name() + ".";
        }
        if (to.quest() != null && !questDone) {
            return "You need to complete a quest before you can move your house to " + to.name() + ".";
        }
        if (coins < to.moveCost()) {
            return "It costs " + String.format(Locale.ROOT, "%,d", to.moveCost()) + " coins to move your house to " + to.name() + ".";
        }
        return null;
    }

    public static PortalTown portalTown(String id) {
        for (PortalTown t : PORTAL_TOWNS) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    @Override
    public String id() {
        return "extras";
    }

    @Override
    public void items(ItemSink items) {
        for (Pets.PetDef p : Pets.defined()) {
            items.add(p.id(), p.name(), Kind.SIMPLE, "pet", p.color()).value(1).stack(1)
                    .examine("A loyal companion. Use it to have it follow you.");
        }
        for (Seafaring.BoatTier b : Seafaring.BOATS) {
            items.add(b.deed(), b.name() + " deed", Kind.SIMPLE, "note", b.color()).value(b.price()).stack(1)
                    .examine("Proof that you own a " + b.name().toLowerCase(Locale.ROOT) + ". Use it while looking at water to launch it.");
        }
        items.add(Poh.HOUSE_TABLET, "Teleport to house", Kind.SIMPLE, "tablet", 0x8A6AC0).value(450).stack(64)
                .examine("A tablet containing a magic spell. It teleports you to your house.");
    }

    @Override
    public List<BlockDef> blocks() {
        List<BlockDef> list = new ArrayList<>();
        list.add(new BlockDef(HOUSE_PORTAL, "House portal", BlockDef.Kind.SCRIPTED, null, 0x8A5AE0, "portal_glow", false));
        list.add(new BlockDef(HOTSPOT, "Furniture hotspot", BlockDef.Kind.SCRIPTED, null, 0x7AA0E0, "hotspot", false));
        list.add(new BlockDef(CHARTER_DOCK, "Charter dock", BlockDef.Kind.SCRIPTED, null, 0x8A6A3A, "notice_board", false));
        list.add(new BlockDef(HERBIBOAR_BURROW, "Herbiboar burrow", BlockDef.Kind.SCRIPTED, null, 0x5A4A2A, "burrow", false));
        for (Poh.Furniture f : Poh.FURNITURE.values()) {
            BlockDef def = switch (f.effect()) {
                case STOVE -> new BlockDef(f.block(), f.name(), BlockDef.Kind.MENU, MenuType.COOKING, f.color(), "range", false);
                default -> new BlockDef(f.block(), f.name(), BlockDef.Kind.SCRIPTED, null, f.color(), f.look(), false);
            };
            list.add(def);
        }
        return list;
    }

    @Override
    public List<PlaceDef> places() {
        List<PlaceDef> list = new ArrayList<>();
        for (PortalTown t : PORTAL_TOWNS) {
            list.add(PlaceDef.of(t.place(), t.name() + " house portal", t.x(), t.y(), PlaceDef.Style.NONE, List.of()));
        }
        for (Seafaring.Port p : Seafaring.PORTS) {
            list.add(PlaceDef.of(p.place(), p.name() + " charter dock", p.x(), p.y(), PlaceDef.Style.NONE, List.of()));
        }
        // Herbiboar: Fossil Island's mushroom forest.
        list.add(PlaceDef.of(HERBIBOAR_SITE, "Mushroom forest (Herbiboar)", 3695, 3872, PlaceDef.Style.NONE, List.of()));
        return list;
    }

    @Override
    public List<NpcDef> npcs() {
        List<NpcDef> list = new ArrayList<>();
        for (Seafaring.Port p : Seafaring.PORTS) {
            if (p.shipwright()) {
                list.add(NpcDef.of("ext_shipwright_" + p.id(), "Shipwright", p.place(), 3, 2, 0xC8A080, 0x3A5A8A, 0x4A3A2A, 0x2A2A2A));
            }
        }
        return list;
    }

    @Override
    public List<ModelSpec> models() {
        return PetModels.all();
    }

    @Override
    public List<CollectionLogData.Page> collectionLog() {
        return List.of(new CollectionLogData.Page("Other", "All Pets", Pets.logOrder()));
    }
}
