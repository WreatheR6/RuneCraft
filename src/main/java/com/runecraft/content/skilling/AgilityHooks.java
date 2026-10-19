package com.runecraft.content.skilling;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.dungeons.Dungeons;
import com.runecraft.osrs.content.skilling.AgilityData;
import com.runecraft.osrs.content.skilling.AgilityRewards;
import com.runecraft.osrs.content.skilling.AgilityShortcuts;
import com.runecraft.osrs.content.skilling.Loot;
import com.runecraft.osrs.data.AgilityCourse;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.AgilityService;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import com.runecraft.platform.ServerLivingEntityEvents;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Agility outside the courses' obstacle blocks: Grace's Graceful clothing shop in the Rogues' Den
 * (talk to buy the selected piece for Marks of grace; talk while sneaking to choose another piece),
 * the shortcuts (surface, grapple and indoor ones), the course lap rewards (Prifddinas crystal
 * shards, Colossal Wyrm termites, Wilderness tickets and streak loot), Worm Tongue's termite stall
 * and the Wilderness Agility dispenser.
 */
final class AgilityHooks {
    /** Piece each player has selected at Grace's shop (not persisted). */
    private static final Map<UUID, Integer> SELECTED = new HashMap<>();
    /** Server tick ({@link ActionManager#mcTick()}) until which each player is still crossing a shortcut (not persisted). */
    private static final Map<UUID, Long> CROSSING = new HashMap<>();
    /**
     * Wilderness course state per player, kept in the skilling world save (section "wilderness_agility"):
     * tickets waiting at the dispenser, fee paid, lap streak and the loot multipliers waiting.
     */
    private static final String WILD_SECTION = "wilderness_agility";

    private static final class Wild {
        int tickets;
        boolean paid;
        int streak;
        final List<Integer> loot = new ArrayList<>();
    }

    private static Wild wild(ServerPlayer player) {
        Wild w = new Wild();
        CompoundTag nbt = SkillingState.get(player.getServer()).section(WILD_SECTION).getCompound(player.getStringUUID());
        w.tickets = nbt.getInt("tickets");
        w.paid = nbt.getBoolean("paid");
        w.streak = nbt.getInt("streak");
        for (int m : nbt.getIntArray("loot")) {
            w.loot.add(m);
        }
        return w;
    }

    private static void save(ServerPlayer player, Wild w) {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("tickets", w.tickets);
        nbt.putBoolean("paid", w.paid);
        nbt.putInt("streak", w.streak);
        int[] loot = new int[w.loot.size()];
        for (int i = 0; i < loot.length; i++) {
            loot[i] = w.loot.get(i);
        }
        nbt.putIntArray("loot", loot);
        SkillingState.get(player.getServer()).section(WILD_SECTION).put(player.getStringUUID(), nbt);
    }

    private AgilityHooks() {
    }

    static void init() {
        QuestScripts.onTalk(AgilityData.GRACE, AgilityHooks::grace);
        QuestScripts.onTalk(AgilityRewards.WORM_TONGUE, AgilityHooks::wormTongue);
        com.runecraft.content.BlockHooks.register(AgilityShortcuts.BLOCK, (player, pos, state) -> shortcut(player, pos));
        // The Wilderness course's dispenser is the minigames module's ticket dispenser block; outside the
        // Brimhaven arena it runs the Wilderness course's tickets, fee and loot.
        com.runecraft.content.BlockHooks.registerFallback("agility_ticket_dispenser", (player, pos, state) -> dispenser(player));
        AgilityService.LAP_LISTENERS.add(AgilityHooks::lap);
        // Logging out inside the Wilderness course costs ten laps of streak; dying loses the fee (and the streak).
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.player;
            CROSSING.remove(player.getUUID());
            Wild w = wild(player);
            if (w.paid && Gielinor.wildernessLevel(player) > 0) {
                w.streak = AgilityRewards.streakAfterLogout(w.streak);
                save(player, w);
            }
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                Wild w = wild(player);
                if (w.paid) {
                    w.paid = false;
                    w.streak = 0;
                    w.loot.clear();
                    save(player, w);
                }
            }
        });
        AgilityPlacer.init();
    }

    /**
     * Every few game ticks: a player who paid the Wilderness course's fee and is no longer inside its walls (walked out,
     * teleported away, left Gielinor) forfeits the fee and the lap streak. Uncollected tickets stay at the dispenser.
     */
    static void wildernessTick(ServerPlayer player, long gameTick) {
        if (gameTick % 5 != 0 || player.isDeadOrDying()) {
            return;
        }
        boolean inside = Gielinor.is(player) && AgilityRewards.inWildernessCourse(GielinorMap.toOsrsX(player.getBlockX()),
                GielinorMap.toOsrsY(player.getBlockZ()));
        if (inside) {
            return;
        }
        Wild w = wild(player);
        if (!w.paid) {
            return;
        }
        w.paid = false;
        w.streak = 0;
        w.loot.clear();
        save(player, w);
        Msg.game(player, "You have left the Wilderness Agility Course: your fee is lost, and your lap streak is reset.");
    }

    // ------------------------------------------------------------------ shortcuts

    /**
     * Uses the agility shortcut whose end is at this block: requirement checks, then off to the other end. A use takes the
     * shortcut's ticks; until they pass, using a shortcut again does nothing (no instant back-and-forth).
     */
    private static void shortcut(ServerPlayer player, BlockPos pos) {
        ServerLevel world = (ServerLevel) player.level();
        Long crossing = CROSSING.get(player.getUUID());
        if (crossing != null && ActionManager.mcTick() < crossing) {
            return;
        }
        // Indoor shortcuts (dungeons, the Slayer Tower) by exact block position.
        for (List<AgilityShortcuts.BlockEnds> list : List.of(Dungeons.shortcutEnds(), AgilityShortcuts.TOWER_ENDS)) {
            for (AgilityShortcuts.BlockEnds e : list) {
                int end = e.endAt(pos.getX(), pos.getY(), pos.getZ());
                if (end < 0) {
                    continue;
                }
                AgilityShortcuts.Shortcut s = AgilityShortcuts.indoor(e.id());
                if (s == null || !allowed(player, s)) {
                    return;
                }
                BlockPos to = end == 0 ? new BlockPos(e.x2(), e.y2(), e.z2()) : new BlockPos(e.x1(), e.y1(), e.z1());
                BlockPos land = beside(world, to);
                player.teleportTo(world, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, player.getYRot(), player.getXRot());
                done(player, s, e.ticks());
                return;
            }
        }
        int x = GielinorMap.toOsrsX(pos.getX());
        int y = GielinorMap.toOsrsY(pos.getZ());
        AgilityShortcuts.Shortcut s = AgilityShortcuts.at(x, y);
        if (s == null) {
            Msg.game(player, "Nothing interesting happens.");
            return;
        }
        boolean fromA = s.endAt(x, y) == 0;
        if (!fromA && s.oneWay()) {
            Msg.game(player, "You can't " + s.verb() + " from this side.");
            return;
        }
        if (!allowed(player, s)) {
            return;
        }
        int tx = fromA ? s.bx() : s.ax();
        int ty = fromA ? s.by() : s.ay();
        BlockPos land = Gielinor.safeSpot(world, GielinorMap.toBlockX(tx) + 1, GielinorMap.toBlockZ(ty) + 1);
        player.teleportTo(world, land.getX() + 0.5, land.getY(), land.getZ() + 0.5, player.getYRot(), player.getXRot());
        done(player, s, s.ticks());
    }

    /** Level, skill and equipment requirements of a shortcut (messages the player when one is missing). */
    private static boolean allowed(ServerPlayer player, AgilityShortcuts.Shortcut s) {
        if (s.level() > 0 && !SkillingUtil.need(player, Skill.AGILITY, s.level(), s.verb())) {
            return false;
        }
        for (Map.Entry<Skill, Integer> req : s.other().entrySet()) {
            if (!SkillingUtil.need(player, req.getKey(), req.getValue(), s.verb())) {
                return false;
            }
        }
        if (AgilityShortcuts.GRAPPLE.equals(s.needs())) {
            if (!AgilityShortcuts.isCrossbow(Inv.idOf(player.getItemBySlot(EquipmentSlot.MAINHAND)))) {
                Msg.game(player, "You need to be wielding a crossbow to fire a grapple.");
                return false;
            }
            if (!Inv.has(player, AgilityShortcuts.MITH_GRAPPLE, 1)) {
                Msg.game(player, "You need a mith grapple to " + s.verb() + ".");
                return false;
            }
        }
        if (AgilityShortcuts.ROPE.equals(s.needs()) && !Inv.has(player, "rope", 1)) {
            Msg.game(player, "You need a rope to tie to the rocks before you can climb down.");
            return false;
        }
        return true;
    }

    private static void done(ServerPlayer player, AgilityShortcuts.Shortcut s, int ticks) {
        CROSSING.put(player.getUUID(), ActionManager.mcTick() + (long) ticks * ActionManager.MC_TICKS_PER_GAME_TICK);
        if (s.xp() > 0) {
            XpService.add(player, Skill.AGILITY, s.xp());
        }
        Msg.action(player, "You " + s.verb() + ".");
        // Achievement diary tasks that need the shortcut used ("shortcut:" + id).
        com.runecraft.content.GameEvents.fireActivity(player, "shortcut:" + s.id());
    }

    /** A free standing spot (air at feet and head, solid floor) next to the shortcut block at {@code end}. */
    private static BlockPos beside(ServerLevel world, BlockPos end) {
        for (int r = 1; r <= 2; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = end.offset(dx, 0, dz);
                    if (world.getBlockState(p).isAir() && world.getBlockState(p.above()).isAir()
                            && world.getBlockState(p.below()).isRedstoneConductor(world, p.below())) {
                        return p;
                    }
                }
            }
        }
        return end.above();
    }

    // ------------------------------------------------------------------ lap rewards

    private static void lap(ServerPlayer player, AgilityCourse course, int laps) {
        int shards = AgilityRewards.crystalShards(course);
        if (shards > 0) {
            give(player, "crystal_shard", shards);
            Msg.game(player, "You find " + (shards == 1 ? "a crystal shard" : shards + " crystal shards") + " along the course.");
        }
        int[] termites = AgilityRewards.termites(course);
        if (termites[1] > 0) {
            int n = termites[0] + player.getRandom().nextInt(termites[1] - termites[0] + 1);
            give(player, AgilityRewards.TERMITES, n);
            Msg.game(player, "You scoop up " + n + " termites as you go.");
        }
        if (course == AgilityCourse.WILDERNESS) {
            Wild w = wild(player);
            w.tickets++;
            if (w.paid) {
                w.streak++;
                w.loot.add(AgilityRewards.streakMultiplier(w.streak));
            }
            save(player, w);
            Msg.game(player, "Tag the Agility dispenser for your reward.");
        }
    }

    private static void give(ServerPlayer player, String item, int n) {
        Inv.give(player, item, n);
        CollectionLog.obtain(player, item);
    }

    /**
     * The Wilderness Agility dispenser: hands out the tickets (and streak loot) of the laps finished
     * since the last tag; otherwise sneak-use pays the 150,000 coin fee, and use exchanges carried
     * tickets for Agility XP.
     */
    private static void dispenser(ServerPlayer player) {
        Wild w = wild(player);
        if (w.tickets > 0) {
            give(player, AgilityRewards.WILDERNESS_TICKET, w.tickets);
            Msg.game(player, "You tag the dispenser and receive " + w.tickets + " Wilderness agility ticket" + (w.tickets == 1 ? "." : "s."));
            w.tickets = 0;
            for (int mult : w.loot) {
                Loot l = Loot.pick(AgilityRewards.WILDERNESS_LOOT, player.getRandom().nextInt(Loot.totalWeight(AgilityRewards.WILDERNESS_LOOT)));
                give(player, l.item(), l.amount(player.getRandom().nextDouble()) * mult);
            }
            if (!w.loot.isEmpty()) {
                Msg.game(player, "The dispenser also gives you some loot. Your lap streak is " + w.streak + ".");
            }
            w.loot.clear();
            save(player, w);
            return;
        }
        if (player.isShiftKeyDown()) {
            if (w.paid) {
                Msg.game(player, "You've already paid. Your lap streak is " + w.streak + ".");
            } else if (!Inv.has(player, Inv.COINS, AgilityRewards.WILDERNESS_FEE)) {
                Msg.game(player, "The dispenser wants " + String.format("%,d", AgilityRewards.WILDERNESS_FEE) + " coins before it gives out loot.");
            } else {
                Inv.remove(player, Inv.COINS, AgilityRewards.WILDERNESS_FEE);
                w.paid = true;
                w.streak = 0;
                save(player, w);
                Msg.game(player, "You pay the dispenser. Each lap now earns loot as well as a ticket.");
                com.runecraft.network.ServerNetworking.sync(player);
            }
            return;
        }
        int tickets = Inv.count(player, AgilityRewards.WILDERNESS_TICKET);
        if (tickets > 0) {
            Inv.remove(player, AgilityRewards.WILDERNESS_TICKET, tickets);
            double xp = AgilityRewards.ticketXp(tickets) * tickets;
            XpService.add(player, Skill.AGILITY, xp);
            Msg.game(player, "You exchange " + tickets + " ticket" + (tickets == 1 ? "" : "s") + " for " + String.format("%,.0f", xp)
                    + " Agility experience.");
            return;
        }
        Msg.game(player, "Finish a lap and tag the dispenser for a ticket. Sneak and use it to pay "
                + String.format("%,d", AgilityRewards.WILDERNESS_FEE) + " coins for lap loot.");
    }

    // ------------------------------------------------------------------ shops

    /** Worm Tongue: an amylase pack for 100 termites, or (sneaking) the Varlamore Graceful recolour for 650. */
    private static void wormTongue(ServerPlayer player, QuestNpcEntity npc) {
        int termites = Inv.count(player, AgilityRewards.TERMITES);
        if (player.isShiftKeyDown()) {
            if (termites < AgilityRewards.VARLAMORE_RECOLOUR_TERMITES) {
                Msg.game(player, "Worm Tongue: Recolourin' a Graceful set costs " + AgilityRewards.VARLAMORE_RECOLOUR_TERMITES
                        + " termites. You've got " + termites + ".");
                return;
            }
            if (!recolour(player, "", AgilityData.VARLAMORE)) {
                Msg.game(player, "Worm Tongue: Bring me a whole Graceful set - all six pieces - and I'll give 'em proper colours.");
                return;
            }
            Inv.remove(player, AgilityRewards.TERMITES, AgilityRewards.VARLAMORE_RECOLOUR_TERMITES);
            Msg.game(player, "Worm Tongue: There. Fit for a Varlamorian.");
            return;
        }
        if (termites < AgilityRewards.AMYLASE_PACK_TERMITES) {
            Msg.game(player, "Worm Tongue: Termites! " + AgilityRewards.AMYLASE_PACK_TERMITES + " gets you an amylase pack. Sneak and talk "
                    + "if you want your Graceful recoloured (" + AgilityRewards.VARLAMORE_RECOLOUR_TERMITES + "). You've got " + termites + ".");
            return;
        }
        Inv.remove(player, AgilityRewards.TERMITES, AgilityRewards.AMYLASE_PACK_TERMITES);
        give(player, "amylase_crystal", AgilityRewards.AMYLASE_PACK_CRYSTALS);
        Msg.game(player, "Worm Tongue: One amylase pack. Lovely doing business.");
    }

    /** Swaps a full set of Graceful pieces carried in the inventory from one colour suffix to another; false if a piece is missing. */
    static boolean recolour(ServerPlayer player, String from, String to) {
        for (AgilityData.Piece p : AgilityData.GRACEFUL) {
            if (!Inv.has(player, p.id() + from, 1)) {
                return false;
            }
        }
        for (AgilityData.Piece p : AgilityData.GRACEFUL) {
            Inv.remove(player, p.id() + from, 1);
            give(player, p.id() + to, 1);
        }
        return true;
    }

    private static void grace(ServerPlayer player, QuestNpcEntity npc) {
        int index = SELECTED.getOrDefault(player.getUUID(), 0) % AgilityData.GRACEFUL.size();
        if (player.isShiftKeyDown() || !SELECTED.containsKey(player.getUUID())) {
            if (SELECTED.containsKey(player.getUUID())) {
                index = (index + 1) % AgilityData.GRACEFUL.size();
            }
            SELECTED.put(player.getUUID(), index);
            AgilityData.Piece p = AgilityData.GRACEFUL.get(index);
            Msg.game(player, "Grace: The " + SkillingUtil.lower(p.name()) + " costs " + p.marks()
                    + " Marks of grace. Talk to me again to buy it, or sneak and talk to see something else.");
            return;
        }
        AgilityData.Piece p = AgilityData.GRACEFUL.get(index);
        if (!Inv.has(player, "mark_of_grace", p.marks())) {
            Msg.game(player, "Grace: You'll need " + p.marks() + " Marks of grace for the " + SkillingUtil.lower(p.name())
                    + ". You have " + Inv.count(player, "mark_of_grace") + ".");
            return;
        }
        Inv.remove(player, "mark_of_grace", p.marks());
        SkillingUtil.give(player, p.id(), 1);
        Msg.game(player, "Grace: Here you go. Light as a feather, isn't it?");
    }
}
