package com.runecraft.content.extras;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.combat.PoisonService;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.systems.RunEnergyService;
import com.runecraft.content.systems.SpecialAttackService;
import com.runecraft.content.systems.SysMenus;
import com.runecraft.menu.MenuService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.extras.HouseLayout;
import com.runecraft.osrs.content.extras.Poh;
import com.runecraft.osrs.content.systems.SpecialAttacks;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.data.Bone;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What built furniture does: altars (bone offerings with incense burner multipliers), restoration
 * pools, portal chamber portals, lecterns (teleport tablets), the jewellery box, the spirit tree,
 * larders, workbenches, the pet house and spellbook altars. In building mode the owner gets a
 * "remove" option instead.
 */
final class HouseFurniture {
    /** Lit incense burners: world|pos -> game tick they go out. */
    private static final Map<String, Long> BURNERS = new HashMap<>();
    private static final Map<UUID, Long> LARDER_COOLDOWN = new HashMap<>();

    private HouseFurniture() {
    }

    static void init() {
        for (Poh.Furniture f : Poh.FURNITURE.values()) {
            if (f.effect() != Poh.Effect.STOVE) {
                BlockHooks.register(f.block(), (player, pos, state) -> use(player, pos, f));
            }
        }
    }

    private static String key(ServerLevel world, BlockPos pos) {
        return world.dimension().location() + "|" + pos.asLong();
    }

    private static void use(ServerPlayer player, BlockPos pos, Poh.Furniture f) {
        ServerLevel world = (ServerLevel) player.level();
        HouseService.Spot spot = HouseService.locate(world, pos);
        if (spot != null && spot.hotspot() != null && HouseService.building(player) && spot.owner().equals(player.getUUID())) {
            List<MutableComponent> links = new ArrayList<>();
            for (Poh.Furniture up : spot.hotspot().options()) {
                if (f.id().equals(Poh.upgradeOf(up.id()))) {
                    boolean ok = ExtrasUtil.level(player, Skill.CONSTRUCTION) >= up.level() && ExtrasUtil.hasAll(player, up.materials());
                    links.add(ExtrasUtil.link("Upgrade to " + up.name() + " (" + up.level() + ")", "/house furnish " + spot.cx() + " "
                            + spot.cz() + " " + spot.hotspot().id() + " " + up.id(), up.name() + "\nLevel " + up.level()
                            + " Construction\n" + ExtrasUtil.materials(up.materials()) + "\n" + up.xp() + " XP",
                            ok ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
                }
            }
            links.add(ExtrasUtil.link("Remove " + f.name(), "/house unfurnish " + spot.cx() + " " + spot.cz() + " "
                    + spot.hotspot().id(), "Remove it (materials are not returned)", ChatFormatting.RED));
            ExtrasUtil.options(player, f.name() + ":", links);
            return;
        }
        if ("armour_stand".equals(f.id())) {
            com.runecraft.content.systems.ChargeService.repair(player, true);
            return;
        }
        switch (f.effect()) {
            case EXIT_PORTAL -> HouseService.leave(player);
            case ALTAR -> altar(player, pos, f);
            case BURNER -> burner(player, pos);
            case POOL -> pool(player, pos, f);
            case PORTAL_FRAME -> portal(player, spot, f);
            case LECTERN -> lectern(player, f);
            case JEWELLERY_BOX -> destinations(player, "Jewellery box:", Poh.jewelleryDests(f.power()), "jewellery");
            case SPIRIT_TREE -> destinations(player, "Spirit tree:", Poh.SPIRIT_TREES, "spirit");
            case FAIRY_RING -> fairyRing(player, pos, f);
            case LARDER -> larder(player, f);
            case BOOKCASE -> bookcase(player);
            case WORKBENCH -> MenuService.open(player, MenuType.CONSTRUCTION, pos, false);
            case PET_HOUSE -> PetService.petHouse(player, f.power());
            case SPELLBOOK_ALTAR -> spellbookAltar(player, f);
            case NEXUS -> nexus(player, spot, f);
            case BELL_PULL -> HouseServants.ring(player);
            case SEAT -> Msg.game(player, "You sit down for a moment on the " + ExtrasUtil.lower(f.name()) + ".");
            case BED -> Msg.game(player, "The " + ExtrasUtil.lower(f.name()) + " looks comfortable.");
            case CHEST -> Msg.game(player, "The " + ExtrasUtil.lower(f.name()) + " is empty.");
            default -> Msg.game(player, "A fine " + ExtrasUtil.lower(f.name()) + ".");
        }
    }

    // ------------------------------------------------------------------ chapel

    private static int litBurners(ServerLevel world, BlockPos altar) {
        long now = ActionManager.gameTick();
        BURNERS.values().removeIf(until -> until < now);
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(altar.offset(-8, -1, -8), altar.offset(8, 1, 8))) {
            Long until = BURNERS.get(key(world, p));
            if (until != null && until >= now) {
                n++;
            }
        }
        return Math.min(2, n);
    }

    private static void burner(ServerPlayer player, BlockPos pos) {
        ServerLevel world = (ServerLevel) player.level();
        Long until = BURNERS.get(key(world, pos));
        if (until != null && until > ActionManager.gameTick()) {
            Msg.game(player, "The incense burner is already lit.");
            return;
        }
        if (ExtrasUtil.level(player, Skill.FIREMAKING) < 30) {
            Msg.game(player, "You need a Firemaking level of 30 to light the incense burner.");
            return;
        }
        if (!Inv.has(player, "tinderbox", 1)) {
            Msg.game(player, "You need a tinderbox to light the burner.");
            return;
        }
        if (!Inv.has(player, "marrentill", 1)) {
            Msg.game(player, "You need a clean marrentill to light the incense burner.");
            return;
        }
        Inv.remove(player, "marrentill", 1);
        // OSRS: burners stay lit for 130 + (Firemaking level) game ticks (approx 2 minutes).
        BURNERS.put(key(world, pos), ActionManager.gameTick() + 130 + ExtrasUtil.level(player, Skill.FIREMAKING));
        world.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.2, 0.1, 0.2, 0.01);
        world.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1f, 1f);
        Msg.action(player, "You light the incense burner.");
    }

    private static void altar(ServerPlayer player, BlockPos pos, Poh.Furniture f) {
        ServerLevel world = (ServerLevel) player.level();
        Bone first = Bone.byItem(Inv.idOf(player.getMainHandItem()));
        if (first == null) {
            // Praying at a house altar recharges Prayer points like any altar.
            double mult = Poh.altarMultiplier(f, litBurners(world, pos));
            PrayerService.useAltar(player);
            Msg.game(player, "Hold bones and click the altar to offer them to the gods (" + Math.round(mult * 100) + "% Prayer XP).");
            return;
        }
        ActionManager.start(player, new Action() {
            int counter = 2;

            @Override
            public boolean tick(ServerPlayer p, long gameTick) {
                if (++counter < 3) {
                    return true;
                }
                counter = 0;
                if (p.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 25) {
                    return false;
                }
                // The bone offering itself is the shared Prayer one, with this altar's multiplier.
                if (!PrayerService.offerHeldBone(p, Poh.altarMultiplier(f, litBurners(world, pos)))) {
                    return false;
                }
                world.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.02);
                return true;
            }
        });
    }

    private static void spellbookAltar(ServerPlayer player, Poh.Furniture f) {
        switch (f.power()) {
            case 1 -> SysMenus.altar(player, Spellbooks.Book.ANCIENT);
            case 2 -> SysMenus.altar(player, Spellbooks.Book.LUNAR);
            case 3 -> SysMenus.altar(player, Spellbooks.Book.ARCEUUS);
            default -> {
                List<MutableComponent> links = new ArrayList<>();
                for (Spellbooks.Book b : Spellbooks.Book.values()) {
                    links.add(ExtrasUtil.link(b.displayName, "/house altar " + b.name(), "Switch to the " + b.displayName + " spellbook",
                            ChatFormatting.LIGHT_PURPLE));
                }
                ExtrasUtil.options(player, "Occult altar:", links);
            }
        }
    }

    static void occult(ServerPlayer player, String book) {
        if (nearby(player, Poh.Effect.SPELLBOOK_ALTAR) < 4) {
            Msg.game(player, "You need to be next to an occult altar.");
            return;
        }
        try {
            SysMenus.altar(player, Spellbooks.Book.valueOf(book));
        } catch (IllegalArgumentException e) {
            Msg.game(player, "There's no such spellbook.");
        }
    }

    // ------------------------------------------------------------------ pools, larder, bookcase

    private static void pool(ServerPlayer player, BlockPos pos, Poh.Furniture f) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        Poh.PoolEffects fx = Poh.poolEffects(f.power());
        if (fx.special()) {
            SpecialAttackService.restoreEnergy(player, SpecialAttacks.MAX_ENERGY);
        }
        if (fx.run()) {
            RunEnergyService.restore(player, 10000);
        }
        if (fx.prayer()) {
            data.setPrayerPoints(skills.level(Skill.PRAYER));
        }
        if (fx.stats()) {
            for (Skill s : Skill.values()) {
                if (skills.boost(s) < 0 && s != Skill.HITPOINTS && s != Skill.PRAYER) {
                    skills.setBoost(s, 0);
                }
            }
        }
        if (fx.hitpoints()) {
            player.setHealth(player.getMaxHealth());
            PoisonService.cure(player, true, 0, ActionManager.gameTick());
        }
        ServerLevel world = (ServerLevel) player.level();
        world.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
        world.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1f, 1f);
        Msg.action(player, "You drink from the " + ExtrasUtil.lower(f.name()) + " and feel refreshed.");
        ServerNetworking.sync(player);
    }

    private static void larder(ServerPlayer player, Poh.Furniture f) {
        long now = ActionManager.gameTick();
        Long next = LARDER_COOLDOWN.get(player.getUUID());
        if (next != null && now < next) {
            Msg.game(player, "You've just taken something from the larder.");
            return;
        }
        LARDER_COOLDOWN.put(player.getUUID(), now + 5);
        List<String> items = Poh.larderItems(f.power());
        String item = items.get(player.getRandom().nextInt(items.size()));
        Inv.give(player, item, 1);
        Msg.action(player, "You take " + Msg.article(ExtrasUtil.lower(ExtrasUtil.itemName(item))) + " from the larder.");
    }

    private static void bookcase(ServerPlayer player) {
        PlayerData data = Players.data(player);
        Msg.game(player, "You browse your bookcase. You have " + data.questPoints() + " quest points and a total level of "
                + data.skills().totalLevel() + ".");
    }

    // ------------------------------------------------------------------ portals

    private static void portal(ServerPlayer player, HouseService.Spot spot, Poh.Furniture f) {
        if (spot == null || spot.hotspot() == null) {
            Msg.game(player, "The portal frame is empty.");
            return;
        }
        String destId = spot.layout().extraAt(spot.cx(), spot.cz(), spot.hotspot().id());
        boolean own = spot.owner().equals(player.getUUID());
        Poh.Dest dest = destId == null ? null : Poh.portalDest(destId);
        if (dest != null && !(own && player.isShiftKeyDown())) {
            Msg.action(player, "You step through the portal to " + dest.name() + ".");
            ExtrasUtil.teleport(player, dest.x(), dest.y());
            com.runecraft.content.GameEvents.fireActivity(player, "poh_portal:" + dest.id());
            return;
        }
        if (!own) {
            Msg.game(player, "The portal hasn't been directed anywhere.");
            return;
        }
        List<MutableComponent> links = new ArrayList<>();
        int magic = ExtrasUtil.level(player, Skill.MAGIC);
        for (Poh.Dest d : Poh.PORTAL_DESTS) {
            Map<String, Integer> cost = new java.util.LinkedHashMap<>();
            d.runes().forEach((r, n) -> cost.put(r, n * Poh.PORTAL_CASTS));
            boolean ok = magic >= d.magic() && ExtrasUtil.hasAll(player, cost);
            links.add(ExtrasUtil.link(d.name(), "/house direct " + spot.cx() + " " + spot.cz() + " " + spot.hotspot().id() + " " + d.id(),
                    "Direct the portal to " + d.name() + "\nLevel " + d.magic() + " Magic\n" + ExtrasUtil.materials(cost),
                    ok ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        ExtrasUtil.options(player, (dest == null ? "Direct the portal:" : "Redirect the portal (now " + dest.name() + "):"), links);
    }

    static void direct(ServerPlayer player, int cx, int cz, String hotspotId, String destId) {
        HouseService.Spot here = HouseService.locate((ServerLevel) player.level(), player.blockPosition());
        Poh.Dest d = Poh.portalDest(destId);
        if (here == null || !here.owner().equals(player.getUUID()) || d == null || here.cx() != cx || here.cz() != cz) {
            Msg.game(player, "You need to be in your portal chamber to direct its portals.");
            return;
        }
        HouseLayout layout = here.layout();
        Poh.Furniture frame = Poh.furniture(layout.furnitureAt(cx, cz, hotspotId) == null ? "" : layout.furnitureAt(cx, cz, hotspotId));
        if (frame == null || frame.effect() != Poh.Effect.PORTAL_FRAME) {
            Msg.game(player, "There's no portal frame there.");
            return;
        }
        if (ExtrasUtil.level(player, Skill.MAGIC) < d.magic()) {
            Msg.game(player, "You need a Magic level of " + d.magic() + " to direct a portal to " + d.name() + ".");
            return;
        }
        Map<String, Integer> cost = new java.util.LinkedHashMap<>();
        d.runes().forEach((r, n) -> cost.put(r, n * Poh.PORTAL_CASTS));
        if (!ExtrasUtil.hasAll(player, cost) && !player.getAbilities().instabuild) {
            Msg.game(player, "You need " + ExtrasUtil.materials(cost) + " to direct the portal there.");
            return;
        }
        if (!player.getAbilities().instabuild) {
            ExtrasUtil.removeAll(player, cost);
        }
        layout.setExtra(cx, cz, hotspotId, d.id());
        HouseService.save(player.getServer(), player.getUUID(), layout);
        ExtrasState.get(player.getServer()).entry("houses", player.getStringUUID()).putString("built", layout.encode());
        Msg.action(player, "You focus the portal on " + d.name() + ".");
    }

    // ------------------------------------------------------------------ lectern

    private static void lectern(ServerPlayer player, Poh.Furniture f) {
        List<MutableComponent> links = new ArrayList<>();
        int magic = ExtrasUtil.level(player, Skill.MAGIC);
        for (Poh.Tablet t : Poh.tablets(f.power())) {
            Map<String, Integer> cost = new java.util.LinkedHashMap<>(t.runes());
            cost.put("soft_clay", 1);
            String hover = t.name() + "\nLevel " + t.magic() + " Magic, " + t.xp() + " XP\n" + ExtrasUtil.materials(cost);
            ChatFormatting color = magic >= t.magic() && ExtrasUtil.hasAll(player, cost) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY;
            links.add(ExtrasUtil.link(t.name(), "/house tablet " + t.item() + " 1", hover, color));
            links.add(ExtrasUtil.link("x10", "/house tablet " + t.item() + " 10", hover, color));
        }
        ExtrasUtil.options(player, "Make a teleport tablet:", links);
    }

    static void makeTablets(ServerPlayer player, String item, int count) {
        int tier = nearby(player, Poh.Effect.LECTERN);
        Poh.Tablet tablet = null;
        for (Poh.Tablet t : Poh.TABLETS) {
            if (t.item().equals(item)) {
                tablet = t;
            }
        }
        if (tablet == null || tier < tablet.lecternTier()) {
            Msg.game(player, tier == 0 ? "You need to be at a lectern." : "Your lectern can't make that tablet.");
            return;
        }
        if (ExtrasUtil.level(player, Skill.MAGIC) < tablet.magic()) {
            Msg.game(player, "You need a Magic level of " + tablet.magic() + " to make that tablet.");
            return;
        }
        Poh.Tablet t = tablet;
        Map<String, Integer> cost = new java.util.LinkedHashMap<>(t.runes());
        cost.put("soft_clay", 1);
        ActionManager.start(player, new Action() {
            int made;
            int counter = 2;

            @Override
            public boolean tick(ServerPlayer p, long gameTick) {
                if (++counter < 3) {
                    return true;
                }
                counter = 0;
                if (made >= Math.max(1, Math.min(count, 28))) {
                    return false;
                }
                if (!ExtrasUtil.hasAll(p, cost)) {
                    Msg.game(p, "You don't have the soft clay and runes to make " + Msg.article(ExtrasUtil.lower(t.name())) + " tablet.");
                    return false;
                }
                ExtrasUtil.removeAll(p, cost);
                Inv.give(p, t.item(), 1);
                XpService.add(p, Skill.MAGIC, t.xp());
                p.swing(InteractionHand.MAIN_HAND, true);
                made++;
                Msg.action(p, "You make " + Msg.article(ExtrasUtil.lower(t.name())) + " tablet.");
                return made < count;
            }
        });
    }

    // ------------------------------------------------------------------ jewellery box and spirit tree

    private static void destinations(ServerPlayer player, String title, List<Poh.Dest> dests, String kind) {
        List<MutableComponent> links = new ArrayList<>();
        for (Poh.Dest d : dests) {
            links.add(ExtrasUtil.link(d.name(), "/house tele " + kind + " " + d.id(), "Teleport to " + d.name(), ChatFormatting.AQUA));
        }
        ExtrasUtil.options(player, title, links);
    }

    /**
     * The house fairy ring (code DIQ): works like any other fairy ring (dramen or lunar staff, sneak-use to turn the dial,
     * use to travel). The spirit tree &amp; fairy ring also lists the spirit tree's destinations.
     */
    private static void fairyRing(ServerPlayer player, BlockPos pos, Poh.Furniture f) {
        if (f.power() >= 2 && !player.isShiftKeyDown()) {
            List<MutableComponent> links = new ArrayList<>();
            links.add(ExtrasUtil.link("Fairy ring", "/house tele fairy ring", "Step into the fairy ring (sneak and use the tree to "
                    + "turn the dial)", ChatFormatting.LIGHT_PURPLE));
            for (Poh.Dest d : Poh.SPIRIT_TREES) {
                links.add(ExtrasUtil.link(d.name(), "/house tele spirit " + d.id(), "Teleport to " + d.name(), ChatFormatting.AQUA));
            }
            ExtrasUtil.options(player, f.name() + ":", links);
            return;
        }
        com.runecraft.content.islands.IslandsTravelService.use(player, pos, com.runecraft.osrs.content.islands.IslandsTravel.FAIRY_RINGS);
    }

    static void teleport(ServerPlayer player, String kind, String destId) {
        if (kind.equals("fairy")) {
            if (nearby(player, Poh.Effect.FAIRY_RING) > 0) {
                com.runecraft.content.islands.IslandsTravelService.use(player, player.blockPosition(),
                        com.runecraft.osrs.content.islands.IslandsTravel.FAIRY_RINGS);
            } else {
                Msg.game(player, "You need to be next to your house's fairy ring to use it.");
            }
            return;
        }
        List<Poh.Dest> dests;
        if (kind.equals("jewellery")) {
            dests = Poh.jewelleryDests(nearby(player, Poh.Effect.JEWELLERY_BOX));
        } else if (kind.equals("spirit")) {
            dests = nearby(player, Poh.Effect.SPIRIT_TREE) > 0 || nearby(player, Poh.Effect.FAIRY_RING) >= 2 ? Poh.SPIRIT_TREES
                    : List.of();
        } else if (kind.equals("nexus")) {
            HouseService.Spot spot = nexusSpot(player);
            dests = new ArrayList<>();
            if (spot != null) {
                for (String id : Poh.nexusList(spot.layout().extraAt(spot.cx(), spot.cz(), spot.hotspot().id()))) {
                    dests.add(Poh.nexusDest(id));
                }
            }
        } else {
            dests = List.of();
        }
        for (Poh.Dest d : dests) {
            if (d.id().equals(destId)) {
                Msg.action(player, kind.equals("spirit") ? "You place your hands on the dry tough bark of the spirit tree, and feel a "
                        + "surge of energy run through your veins." : "You teleport to " + d.name() + ".");
                ExtrasUtil.teleport(player, d.x(), d.y());
                return;
            }
        }
        Msg.game(player, "You need to be next to the right furniture in a house to teleport there.");
    }

    // ------------------------------------------------------------------ portal nexus

    /** The portal nexus hotspot the player is standing at, or null. */
    private static HouseService.Spot nexusSpot(ServerPlayer player) {
        ServerLevel world = (ServerLevel) player.level();
        BlockPos c = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 2, 4))) {
            String id = ExtrasUtil.blockId(world.getBlockState(p));
            Poh.Furniture f = id == null ? null : Poh.byBlock(id);
            if (f != null && f.effect() == Poh.Effect.NEXUS) {
                HouseService.Spot spot = HouseService.locate(world, p.immutable());
                if (spot != null && spot.hotspot() != null) {
                    return spot;
                }
            }
        }
        return null;
    }

    private static Map<String, Integer> nexusCost(Poh.Dest d) {
        Map<String, Integer> cost = new java.util.LinkedHashMap<>();
        d.runes().forEach((r, n) -> cost.put(r, n * Poh.NEXUS_CASTS));
        return cost;
    }

    private static void nexus(ServerPlayer player, HouseService.Spot spot, Poh.Furniture f) {
        if (spot == null || spot.hotspot() == null) {
            return;
        }
        List<String> stored = Poh.nexusList(spot.layout().extraAt(spot.cx(), spot.cz(), spot.hotspot().id()));
        List<Poh.Dest> dests = new ArrayList<>();
        stored.forEach(id -> dests.add(Poh.nexusDest(id)));
        if (!dests.isEmpty()) {
            destinations(player, f.name() + " (" + stored.size() + "/" + Poh.nexusCapacity(f.power()) + "):", dests, "nexus");
        } else {
            Msg.game(player, "The portal nexus has no teleports yet.");
        }
        if (!spot.owner().equals(player.getUUID())) {
            return;
        }
        int magic = ExtrasUtil.level(player, Skill.MAGIC);
        List<MutableComponent> links = new ArrayList<>();
        for (Poh.Dest d : Poh.NEXUS_DESTS) {
            if (stored.contains(d.id())) {
                continue;
            }
            Map<String, Integer> cost = nexusCost(d);
            boolean ok = Poh.canAddToNexus(f.power(), stored, d.id(), magic) == null && ExtrasUtil.hasAll(player, cost);
            links.add(ExtrasUtil.link(d.name(), "/house nexus " + d.id(), "Add a teleport to " + d.name() + "\nLevel " + d.magic()
                    + " Magic\n" + ExtrasUtil.materials(cost), ok ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        }
        if (!links.isEmpty()) {
            ExtrasUtil.options(player, "Add a teleport:", links);
        }
    }

    static void addToNexus(ServerPlayer player, String destId) {
        HouseService.Spot spot = nexusSpot(player);
        if (spot == null || !spot.owner().equals(player.getUUID())) {
            Msg.game(player, "You need to be next to the portal nexus in your own house.");
            return;
        }
        String hotspot = spot.hotspot().id();
        String builtId = spot.layout().furnitureAt(spot.cx(), spot.cz(), hotspot);
        Poh.Furniture f = builtId == null ? null : Poh.furniture(builtId);
        if (f == null || f.effect() != Poh.Effect.NEXUS) {
            return;
        }
        List<String> stored = Poh.nexusList(spot.layout().extraAt(spot.cx(), spot.cz(), hotspot));
        String why = Poh.canAddToNexus(f.power(), stored, destId, ExtrasUtil.level(player, Skill.MAGIC));
        if (why != null) {
            Msg.game(player, why);
            return;
        }
        Poh.Dest d = Poh.nexusDest(destId);
        Map<String, Integer> cost = nexusCost(d);
        if (!player.getAbilities().instabuild) {
            if (!ExtrasUtil.hasAll(player, cost)) {
                Msg.game(player, "You need " + ExtrasUtil.materials(cost) + " to add that teleport.");
                return;
            }
            ExtrasUtil.removeAll(player, cost);
        }
        stored.add(destId);
        HouseLayout layout = spot.layout();
        layout.setExtra(spot.cx(), spot.cz(), hotspot, String.join(",", stored));
        HouseService.save(player.getServer(), player.getUUID(), layout);
        Msg.action(player, "You add a teleport to " + d.name() + " to your portal nexus.");
    }

    /** Highest power of a furniture type within 4 blocks (0 = none). */
    static int nearby(ServerPlayer player, Poh.Effect effect) {
        ServerLevel world = (ServerLevel) player.level();
        int best = 0;
        BlockPos c = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-4, -2, -4), c.offset(4, 2, 4))) {
            BlockState state = world.getBlockState(p);
            String id = ExtrasUtil.blockId(state);
            Poh.Furniture f = id == null ? null : Poh.byBlock(id);
            if (f != null && f.effect() == effect) {
                best = Math.max(best, Math.max(1, f.power()));
            }
        }
        return best;
    }

    /** Lower-case helper for messages. */
    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
