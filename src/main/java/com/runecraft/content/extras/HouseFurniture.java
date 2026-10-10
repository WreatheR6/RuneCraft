package com.runecraft.content.extras;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.combat.PoisonService;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.systems.SysMenus;
import com.runecraft.menu.MenuService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.extras.HouseLayout;
import com.runecraft.osrs.content.extras.Poh;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.data.Bone;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

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

    private static String key(ServerWorld world, BlockPos pos) {
        return world.getRegistryKey().getValue() + "|" + pos.asLong();
    }

    private static void use(ServerPlayerEntity player, BlockPos pos, Poh.Furniture f) {
        ServerWorld world = (ServerWorld) player.getWorld();
        HouseService.Spot spot = HouseService.locate(world, pos);
        if (spot != null && spot.hotspot() != null && HouseService.building(player) && spot.owner().equals(player.getUuid())) {
            List<MutableText> links = List.of(ExtrasUtil.link("Remove " + f.name(), "/house unfurnish " + spot.cx() + " " + spot.cz() + " "
                    + spot.hotspot().id(), "Remove it (materials are not returned)", Formatting.RED));
            ExtrasUtil.options(player, f.name() + ":", links);
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
            case LARDER -> larder(player, f);
            case BOOKCASE -> bookcase(player);
            case WORKBENCH -> MenuService.open(player, MenuType.CONSTRUCTION, pos, false);
            case PET_HOUSE -> PetService.petHouse(player, f.power());
            case SPELLBOOK_ALTAR -> spellbookAltar(player, f);
            case SEAT -> Msg.game(player, "You sit down for a moment on the " + ExtrasUtil.lower(f.name()) + ".");
            case BED -> Msg.game(player, "The " + ExtrasUtil.lower(f.name()) + " looks comfortable.");
            case CHEST -> Msg.game(player, "The " + ExtrasUtil.lower(f.name()) + " is empty.");
            default -> Msg.game(player, "A fine " + ExtrasUtil.lower(f.name()) + ".");
        }
    }

    // ------------------------------------------------------------------ chapel

    private static int litBurners(ServerWorld world, BlockPos altar) {
        long now = ActionManager.gameTick();
        BURNERS.values().removeIf(until -> until < now);
        int n = 0;
        for (BlockPos p : BlockPos.iterate(altar.add(-8, -1, -8), altar.add(8, 1, 8))) {
            Long until = BURNERS.get(key(world, p));
            if (until != null && until >= now) {
                n++;
            }
        }
        return Math.min(2, n);
    }

    private static void burner(ServerPlayerEntity player, BlockPos pos) {
        ServerWorld world = (ServerWorld) player.getWorld();
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
        world.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 10, 0.2, 0.1, 0.2, 0.01);
        world.playSound(null, pos, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, "You light the incense burner.");
    }

    private static void altar(ServerPlayerEntity player, BlockPos pos, Poh.Furniture f) {
        ServerWorld world = (ServerWorld) player.getWorld();
        Bone first = Bone.byItem(Inv.idOf(player.getMainHandStack()));
        if (first == null) {
            double mult = Poh.altarMultiplier(f, litBurners(world, pos));
            Msg.game(player, "Hold bones and click the altar to offer them to the gods (" + Math.round(mult * 100) + "% Prayer XP).");
            return;
        }
        ActionManager.start(player, new Action() {
            int counter = 2;

            @Override
            public boolean tick(ServerPlayerEntity p, long gameTick) {
                if (++counter < 3) {
                    return true;
                }
                counter = 0;
                Bone bone = Bone.byItem(Inv.idOf(p.getMainHandStack()));
                if (bone == null || p.squaredDistanceTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) > 25) {
                    return false;
                }
                double mult = Poh.altarMultiplier(f, litBurners(world, pos));
                p.getMainHandStack().decrement(1);
                p.swingHand(Hand.MAIN_HAND, true);
                XpService.add(p, Skill.PRAYER, bone.xp() * mult);
                world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.2, 0.3, 0.2, 0.02);
                Msg.action(p, "The gods are pleased with your offering.");
                return true;
            }
        });
    }

    private static void spellbookAltar(ServerPlayerEntity player, Poh.Furniture f) {
        switch (f.power()) {
            case 1 -> SysMenus.altar(player, Spellbooks.Book.ANCIENT);
            case 2 -> SysMenus.altar(player, Spellbooks.Book.LUNAR);
            case 3 -> SysMenus.altar(player, Spellbooks.Book.ARCEUUS);
            default -> {
                List<MutableText> links = new ArrayList<>();
                for (Spellbooks.Book b : Spellbooks.Book.values()) {
                    links.add(ExtrasUtil.link(b.displayName, "/house altar " + b.name(), "Switch to the " + b.displayName + " spellbook",
                            Formatting.LIGHT_PURPLE));
                }
                ExtrasUtil.options(player, "Occult altar:", links);
            }
        }
    }

    static void occult(ServerPlayerEntity player, String book) {
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

    private static void pool(ServerPlayerEntity player, BlockPos pos, Poh.Furniture f) {
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int tier = f.power();
        player.getHungerManager().add(20, 1.0f); // run energy stands in as food/saturation
        if (tier >= 2) {
            for (Skill s : Skill.values()) {
                if (skills.boost(s) < 0 && s != Skill.HITPOINTS && s != Skill.PRAYER) {
                    skills.setBoost(s, 0);
                }
            }
        }
        if (tier >= 3) {
            data.setPrayerPoints(skills.level(Skill.PRAYER));
        }
        if (tier >= 4) {
            PoisonService.cure(player, true, 0, ActionManager.gameTick());
        }
        if (tier >= 5) {
            player.setHealth(player.getMaxHealth());
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 30, 0.4, 0.2, 0.4, 0.1);
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 1f, 1f);
        Msg.action(player, "You drink from the " + ExtrasUtil.lower(f.name()) + " and feel refreshed.");
        ServerNetworking.sync(player);
    }

    private static void larder(ServerPlayerEntity player, Poh.Furniture f) {
        long now = ActionManager.gameTick();
        Long next = LARDER_COOLDOWN.get(player.getUuid());
        if (next != null && now < next) {
            Msg.game(player, "You've just taken something from the larder.");
            return;
        }
        LARDER_COOLDOWN.put(player.getUuid(), now + 5);
        List<String> items = Poh.larderItems(f.power());
        String item = items.get(player.getRandom().nextInt(items.size()));
        Inv.give(player, item, 1);
        Msg.action(player, "You take " + Msg.article(ExtrasUtil.lower(ExtrasUtil.itemName(item))) + " from the larder.");
    }

    private static void bookcase(ServerPlayerEntity player) {
        PlayerData data = Players.data(player);
        Msg.game(player, "You browse your bookcase. You have " + data.questPoints() + " quest points and a total level of "
                + data.skills().totalLevel() + ".");
    }

    // ------------------------------------------------------------------ portals

    private static void portal(ServerPlayerEntity player, HouseService.Spot spot, Poh.Furniture f) {
        if (spot == null || spot.hotspot() == null) {
            Msg.game(player, "The portal frame is empty.");
            return;
        }
        String destId = spot.layout().extraAt(spot.cx(), spot.cz(), spot.hotspot().id());
        boolean own = spot.owner().equals(player.getUuid());
        Poh.Dest dest = destId == null ? null : Poh.portalDest(destId);
        if (dest != null && !(own && player.isSneaking())) {
            Msg.action(player, "You step through the portal to " + dest.name() + ".");
            ExtrasUtil.teleport(player, dest.x(), dest.y());
            return;
        }
        if (!own) {
            Msg.game(player, "The portal hasn't been directed anywhere.");
            return;
        }
        List<MutableText> links = new ArrayList<>();
        int magic = ExtrasUtil.level(player, Skill.MAGIC);
        for (Poh.Dest d : Poh.PORTAL_DESTS) {
            Map<String, Integer> cost = new java.util.LinkedHashMap<>();
            d.runes().forEach((r, n) -> cost.put(r, n * Poh.PORTAL_CASTS));
            boolean ok = magic >= d.magic() && ExtrasUtil.hasAll(player, cost);
            links.add(ExtrasUtil.link(d.name(), "/house direct " + spot.cx() + " " + spot.cz() + " " + spot.hotspot().id() + " " + d.id(),
                    "Direct the portal to " + d.name() + "\nLevel " + d.magic() + " Magic\n" + ExtrasUtil.materials(cost),
                    ok ? Formatting.GREEN : Formatting.DARK_GRAY));
        }
        ExtrasUtil.options(player, (dest == null ? "Direct the portal:" : "Redirect the portal (now " + dest.name() + "):"), links);
    }

    static void direct(ServerPlayerEntity player, int cx, int cz, String hotspotId, String destId) {
        HouseService.Spot here = HouseService.locate((ServerWorld) player.getWorld(), player.getBlockPos());
        Poh.Dest d = Poh.portalDest(destId);
        if (here == null || !here.owner().equals(player.getUuid()) || d == null || here.cx() != cx || here.cz() != cz) {
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
        if (!ExtrasUtil.hasAll(player, cost) && !player.getAbilities().creativeMode) {
            Msg.game(player, "You need " + ExtrasUtil.materials(cost) + " to direct the portal there.");
            return;
        }
        if (!player.getAbilities().creativeMode) {
            ExtrasUtil.removeAll(player, cost);
        }
        layout.setExtra(cx, cz, hotspotId, d.id());
        HouseService.save(player.getServer(), player.getUuid(), layout);
        ExtrasState.get(player.getServer()).entry("houses", player.getUuidAsString()).putString("built", layout.encode());
        Msg.action(player, "You focus the portal on " + d.name() + ".");
    }

    // ------------------------------------------------------------------ lectern

    private static void lectern(ServerPlayerEntity player, Poh.Furniture f) {
        List<MutableText> links = new ArrayList<>();
        int magic = ExtrasUtil.level(player, Skill.MAGIC);
        for (Poh.Tablet t : Poh.tablets(f.power())) {
            Map<String, Integer> cost = new java.util.LinkedHashMap<>(t.runes());
            cost.put("soft_clay", 1);
            String hover = t.name() + "\nLevel " + t.magic() + " Magic, " + t.xp() + " XP\n" + ExtrasUtil.materials(cost);
            Formatting color = magic >= t.magic() && ExtrasUtil.hasAll(player, cost) ? Formatting.GREEN : Formatting.DARK_GRAY;
            links.add(ExtrasUtil.link(t.name(), "/house tablet " + t.item() + " 1", hover, color));
            links.add(ExtrasUtil.link("x10", "/house tablet " + t.item() + " 10", hover, color));
        }
        ExtrasUtil.options(player, "Make a teleport tablet:", links);
    }

    static void makeTablets(ServerPlayerEntity player, String item, int count) {
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
            public boolean tick(ServerPlayerEntity p, long gameTick) {
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
                p.swingHand(Hand.MAIN_HAND, true);
                made++;
                Msg.action(p, "You make " + Msg.article(ExtrasUtil.lower(t.name())) + " tablet.");
                return made < count;
            }
        });
    }

    // ------------------------------------------------------------------ jewellery box and spirit tree

    private static void destinations(ServerPlayerEntity player, String title, List<Poh.Dest> dests, String kind) {
        List<MutableText> links = new ArrayList<>();
        for (Poh.Dest d : dests) {
            links.add(ExtrasUtil.link(d.name(), "/house tele " + kind + " " + d.id(), "Teleport to " + d.name(), Formatting.AQUA));
        }
        ExtrasUtil.options(player, title, links);
    }

    static void teleport(ServerPlayerEntity player, String kind, String destId) {
        List<Poh.Dest> dests;
        if (kind.equals("jewellery")) {
            dests = Poh.jewelleryDests(nearby(player, Poh.Effect.JEWELLERY_BOX));
        } else if (kind.equals("spirit")) {
            dests = nearby(player, Poh.Effect.SPIRIT_TREE) > 0 ? Poh.SPIRIT_TREES : List.of();
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

    /** Highest power of a furniture type within 4 blocks (0 = none). */
    static int nearby(ServerPlayerEntity player, Poh.Effect effect) {
        ServerWorld world = (ServerWorld) player.getWorld();
        int best = 0;
        BlockPos c = player.getBlockPos();
        for (BlockPos p : BlockPos.iterate(c.add(-4, -2, -4), c.add(4, 2, 4))) {
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
