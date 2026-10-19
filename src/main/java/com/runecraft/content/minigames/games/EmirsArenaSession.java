package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.LmsStash;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.EmirsArena;
import com.runecraft.osrs.content.minigames.EmirsArena.DuelSlot;
import com.runecraft.osrs.content.minigames.EmirsArena.Options;
import com.runecraft.osrs.content.minigames.EmirsArena.Rule;
import com.runecraft.osrs.content.minigames.LastManStanding;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.network.ServerNetworking;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * A fight in Emir's Arena (see {@link EmirsArena}): a legacy duel between a challenger and the player who accepted, under the
 * challenger's duel options, or a ranked 1v1 duel paired at the recruitment board (reward points and rank). Deaths are safe;
 * the loser is sent back to the hospital and both players are healed. Leaving early (exit portal, logging out, getting out of
 * the arena) forfeits.
 * Ranked fighters play the week's account build ({@link EmirsArena#weeklyBuild}): their own items and stats wait in the
 * crash-safe {@link LmsStash} (given back on leaving, or on the next login) while they fight with the build's levels and kit.
 */
public class EmirsArenaSession extends MinigameSession {
    private static final int HALF_X = 10;
    private static final int HALF_Z = 7;

    private final boolean ranked;
    private final Options options;
    private boolean started;
    private boolean resolved;
    private final Map<UUID, String> weapons = new HashMap<>();
    private final Map<UUID, Integer> specEnergy = new HashMap<>();
    private final Map<UUID, BlockPos> spots = new HashMap<>();
    private EmirsArena.Build build;

    public EmirsArenaSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.EMIRS_ARENA, id, slot, world, variant);
        this.ranked = !variant.startsWith(EmirsArena.DUEL_PREFIX);
        Options o = Options.none();
        if (!ranked) {
            try {
                o = Options.decode(Long.parseLong(variant.substring(EmirsArena.DUEL_PREFIX.length())));
            } catch (NumberFormatException e) {
                o = Options.none();
            }
        }
        this.options = o;
    }

    @Override
    protected int halfX() {
        return HALF_X;
    }

    @Override
    protected int halfZ() {
        return HALF_Z;
    }

    @Override
    protected int height() {
        return 8;
    }

    @Override
    protected void build() {
        room(Blocks.SAND, Blocks.CUT_SANDSTONE, Blocks.CHISELED_SANDSTONE);
        if (!options.has(Rule.NO_MOVEMENT) && random.nextBoolean()) {
            // Half of the arenas have obstacles, as at the old Duel Arena.
            for (int[] p : new int[][]{{-5, -3}, {5, 3}, {0, -5}, {0, 5}}) {
                set(p[0], 1, p[1], Blocks.SANDSTONE_WALL.defaultBlockState());
            }
        }
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        int d = options.has(Rule.NO_MOVEMENT) ? 1 : 5;
        BlockPos at = rel(index == 0 ? -d : d, 1, 0);
        spots.put(player.getUUID(), at);
        return at;
    }

    // ------------------------------------------------------------------ the fight

    @Override
    protected void start() {
        if (playerCount() < 2) {
            broadcast("Your opponent didn't arrive. The duel is cancelled.");
            end(false);
            return;
        }
        List<ServerPlayer> fighters = online();
        if (ranked) {
            build = EmirsArena.weeklyBuild(java.time.LocalDate.now(java.time.ZoneOffset.UTC).toEpochDay());
            for (ServerPlayer p : fighters) {
                equipBuild(p, build);
                Msg.game(p, "This week's ranked build is " + build.displayName + ". Your own items and levels wait for you in the "
                        + "hospital until the fight is over.");
            }
        }
        for (ServerPlayer p : fighters) {
            var data = Players.data(p);
            if (options.has(Rule.NO_DRINKS)) {
                for (Skill s : Skill.values()) {
                    data.skills().setBoost(s, 0);
                }
            }
            if (options.has(Rule.NO_PRAYER)) {
                data.activePrayers().clear();
            }
            if (options.has(Rule.NO_SPECIAL_ATTACKS)) {
                specEnergy.put(p.getUUID(), SysState.of(p).specEnergy);
            }
            stripDisabled(p);
            weapons.put(p.getUUID(), Inv.idOf(p.getMainHandItem()));
            Msg.game(p, (ranked ? "Ranked duel" : "Duel") + " rules: " + options.describe() + ".");
            if (!ranked) {
                Msg.game(p, "As this is an unofficial duel, you will not receive rank or reward points for participating.");
            }
        }
        if (options.has(Rule.SHOW_INVENTORIES)) {
            for (ServerPlayer p : fighters) {
                ServerPlayer other = opponent(p);
                if (other != null) {
                    Msg.game(p, other.getName().getString() + " carries: " + inventory(other) + ".");
                }
            }
        }
        started = true;
        broadcastRed("3... 2... 1... FIGHT!");
    }

    /**
     * Stashes the player's items and stats ({@link LmsStash}) and kits them out as {@code b}: the build's levels, no boosts or
     * prayers, the starter kit worn and carried, the supplies, and full hitpoints, prayer and special attack.
     */
    public static void equipBuild(ServerPlayer p, EmirsArena.Build b) {
        LmsStash.stash(p);
        var data = Players.data(p);
        for (Skill s : LastManStanding.BUILD_SKILLS) {
            XpService.setLevel(p, s, b.level(s));
        }
        for (Skill s : Skill.values()) {
            data.skills().setBoost(s, 0);
        }
        data.activePrayers().clear();
        LastManStanding.Loadout kit = EmirsArena.loadout(b);
        for (String id : kit.worn()) {
            LmsSession.wear(p, Inv.stack(id, 1));
        }
        for (LastManStanding.Stack st : kit.carried()) {
            Inv.give(p, st.item(), st.count());
        }
        for (LastManStanding.Stack st : LastManStanding.SUPPLIES) {
            Inv.give(p, st.item(), st.count());
        }
        p.setHealth(p.getMaxHealth());
        data.setPrayerPoints(b.level(Skill.PRAYER));
        SysState.of(p).specEnergy = 100;
        ServerNetworking.sync(p);
    }

    private ServerPlayer opponent(ServerPlayer p) {
        for (ServerPlayer o : online()) {
            if (!o.getUUID().equals(p.getUUID())) {
                return o;
            }
        }
        return null;
    }

    private static String inventory(ServerPlayer p) {
        List<String> names = new ArrayList<>();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && names.size() < 28; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                ItemDef def = ItemCatalog.get(Inv.idOf(s));
                String name = def == null ? s.getHoverName().getString() : def.name();
                names.add(s.getCount() > 1 ? s.getCount() + " x " + name : name);
            }
        }
        return names.isEmpty() ? "nothing" : String.join(", ", names);
    }

    /** Moves items out of the slots the duel disables into the inventory (OSRS unequips them when the duel starts). */
    private void stripDisabled(ServerPlayer p) {
        unequip(p, DuelSlot.HEAD, EquipmentSlot.HEAD);
        unequip(p, DuelSlot.BODY, EquipmentSlot.CHEST);
        unequip(p, DuelSlot.LEGS, EquipmentSlot.LEGS);
        unequip(p, DuelSlot.FEET, EquipmentSlot.FEET);
        unequip(p, DuelSlot.SHIELD, EquipmentSlot.OFFHAND);
        unwear(p, DuelSlot.CAPE, Jewellery.Slot.CAPE);
        unwear(p, DuelSlot.NECK, Jewellery.Slot.NECK);
        unwear(p, DuelSlot.RING, Jewellery.Slot.RING);
        unwear(p, DuelSlot.HANDS, Jewellery.Slot.HANDS);
        unwear(p, DuelSlot.AMMO, Jewellery.Slot.AMMO);
    }

    private void unequip(ServerPlayer p, DuelSlot rule, EquipmentSlot slot) {
        ItemStack s = p.getItemBySlot(slot);
        if (options.disabled(rule) && !s.isEmpty()) {
            ItemStack copy = s.copy();
            p.setItemSlot(slot, ItemStack.EMPTY);
            Inv.give(p, copy);
            Msg.game(p, "The " + rule.key() + " slot is disabled for this duel.");
        }
    }

    private void unwear(ServerPlayer p, DuelSlot rule, Jewellery.Slot slot) {
        SysState.SysPlayer sp = SysState.of(p);
        ItemStack s = sp.worn(slot);
        if (options.disabled(rule) && !s.isEmpty()) {
            sp.worn.remove(slot);
            Inv.give(p, s.copy());
            Msg.game(p, "The " + rule.key() + " slot is disabled for this duel.");
        }
    }

    /** No Prayer duels forbid every prayer; otherwise the normal rules apply. */
    public String prayerRule(com.runecraft.osrs.Prayer prayer) {
        return options.has(Rule.NO_PRAYER) ? "Prayer has been disabled for this duel." : null;
    }

    /** True if the duel's No Food / No Drinks options forbid using this item. */
    public boolean blocksConsumable(ItemDef def) {
        return EmirsArena.blockedConsumable(options, def.kind(), def.id());
    }

    @Override
    protected boolean canAttackPlayer(ServerPlayer attacker, ServerPlayer target) {
        return started && !resolved;
    }

    @Override
    protected double damageMultiplier(ServerPlayer player, LivingEntity target, AttackType type) {
        if (!(target instanceof ServerPlayer)) {
            return 1.0;
        }
        if (!EmirsArena.styleAllowed(options, type)) {
            Msg.action(player, (type == AttackType.MAGIC ? "Magic" : type == AttackType.RANGED ? "Ranged" : "Melee")
                    + " has been disabled for this duel.");
            return 0;
        }
        String weapon = Inv.idOf(player.getMainHandItem());
        ItemDef def = weapon.isEmpty() ? null : ItemCatalog.get(weapon);
        boolean holdsWeapon = def != null && def.equipment() != null
                && def.equipment().slot() == com.runecraft.osrs.EquipmentDef.Slot.WEAPON;
        if (!EmirsArena.weaponAllowed(options, holdsWeapon ? weapon : "")) {
            Msg.action(player, options.has(Rule.FUN_WEAPONS) ? "Only fun weapons may be used in this duel."
                    : "Weapons have been disabled for this duel.");
            return 0;
        }
        if (options.has(Rule.NO_WEAPON_SWITCH) && !weapon.equals(weapons.getOrDefault(player.getUUID(), weapon))) {
            Msg.action(player, "Weapon switching has been disabled for this duel.");
            return 0;
        }
        return 1.0;
    }

    @Override
    protected void gameTick() {
        if (!started || resolved) {
            return;
        }
        for (ServerPlayer p : online()) {
            if (p.level() != world || !box().contains(p.position())) {
                // Getting out of the arena any other way forfeits, like the exit portal, and gives a ranked fighter's own
                // items and levels back (the build's kit and supplies stay behind).
                Msg.game(p, "You have left the arena.");
                removePlayer(p, false);
                return;
            }
            if (options.has(Rule.NO_SPECIAL_ATTACKS)) {
                SysState.of(p).specEnergy = 0;
            }
            if (options.has(Rule.NO_PRAYER) && !Players.data(p).activePrayers().isEmpty()) {
                Players.data(p).activePrayers().clear();
            }
            if (ticks % 5 == 0 && !options.slots().isEmpty()) {
                stripDisabled(p);
            }
            BlockPos spot = spots.get(p.getUUID());
            if (options.has(Rule.NO_MOVEMENT) && spot != null
                    && p.distanceToSqr(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5) > 0.5) {
                p.teleportTo(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, p.getYRot(), p.getXRot());
            }
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        if (blockId.equals("minigame_exit") && options.has(Rule.NO_FORFEIT) && started && !resolved) {
            Msg.game(player, "You can't forfeit this duel.");
            return true;
        }
        return false;
    }

    @Override
    protected void onDeath(ServerPlayer player) {
        ServerPlayer winner = opponent(player);
        Msg.red(player, "You have been defeated! You wake up in the arena hospital.");
        resolve(winner, player);
        removePlayer(player, true);
        end(true);
    }

    @Override
    protected void onLeave(ServerPlayer player) {
        if (started && !resolved) {
            // Forfeit: the opponent wins.
            ServerPlayer winner = opponent(player);
            Msg.game(player, "You forfeit the duel.");
            resolve(winner, player);
            if (!isEnded()) {
                end(true);
            }
        }
        if (ranked) {
            // The kit stays in the arena; the player's own items and levels come back.
            LmsStash.restore(player);
        }
        Integer spec = specEnergy.remove(player.getUUID());
        if (spec != null) {
            SysState.of(player).specEnergy = spec;
        }
        // The hospital heals both fighters. // approx
        player.setHealth(player.getMaxHealth());
        Players.data(player).setPrayerPoints(Players.data(player).skills().level(Skill.PRAYER));
    }

    private void resolve(ServerPlayer winner, ServerPlayer loser) {
        if (resolved) {
            return;
        }
        resolved = true;
        MinigameData data = MinigameData.of(loser);
        if (winner == null) {
            return;
        }
        Msg.game(winner, "You are victorious! You defeated " + loser.getName().getString() + ".");
        if (!ranked) {
            int w = data.incrementStat(winner.getUUID(), EmirsArena.LEGACY_WINS);
            int l = data.incrementStat(loser.getUUID(), EmirsArena.LEGACY_LOSSES);
            Msg.game(winner, "Legacy duel wins: " + w + ".");
            Msg.game(loser, "Legacy duel losses: " + l + ".");
            return;
        }
        int winnerRank = rank(data, winner.getUUID());
        int loserRank = rank(data, loser.getUUID());
        int up = EmirsArena.rankChange(winnerRank, loserRank, true);
        int down = EmirsArena.rankChange(loserRank, winnerRank, false);
        data.setStat(winner.getUUID(), EmirsArena.RANK, Math.max(1, winnerRank + up));
        data.setStat(loser.getUUID(), EmirsArena.RANK, Math.max(1, loserRank + down));
        int streak = data.stat(winner.getUUID(), EmirsArena.STREAK);
        award(winner, EmirsArena.rewardPoints(true, streak), winnerRank + up);
        award(loser, EmirsArena.rewardPoints(false, 0), loserRank + down);
        data.setStat(winner.getUUID(), EmirsArena.STREAK, streak + 1);
        data.setStat(loser.getUUID(), EmirsArena.STREAK, 0);
        data.incrementStat(winner.getUUID(), EmirsArena.WINS);
        data.incrementStat(loser.getUUID(), EmirsArena.LOSSES);
        data.incrementStat(winner.getUUID(), "kc:" + game.id());
    }

    /** A player's rank, starting at 2,500 (an unset stat reads 0; stored ranks are at least 1). */
    static int rank(MinigameData data, UUID u) {
        int stored = data.stat(u, EmirsArena.RANK);
        return stored == 0 ? EmirsArena.START_RANK : stored;
    }

    private void award(ServerPlayer p, int points, int rank) {
        MinigameData data = MinigameData.of(p);
        int balance = data.points(p.getUUID(), MinigameCurrencies.key(EmirsArena.POINTS));
        int gain = EmirsArena.cappedGain(balance, points);
        if (gain > 0) {
            addPoints(p, EmirsArena.POINTS, gain);
        }
        Msg.game(p, "You receive " + gain + " reward point" + (gain == 1 ? "" : "s") + ". Your rank is now " + rank + " ("
                + EmirsArena.rankTitle(rank) + ").");
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        List<String> lines = new ArrayList<>();
        lines.add(ranked ? "Emir's Arena: ranked duel" + (build == null ? "" : " (" + build.displayName + ")") : "Emir's Arena: duel");
        ServerPlayer other = opponent(player);
        if (other != null) {
            lines.add("Opponent: " + other.getName().getString() + " (" + Math.round(other.getHealth()) + " hp)");
        }
        if (!options.rules().isEmpty()) {
            lines.add(options.describe());
        }
        lines.add("Time: " + time(ticks));
        return lines;
    }
}
