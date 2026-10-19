package com.runecraft.content.minigames;

import com.runecraft.RuneCraft;
import com.runecraft.content.minigames.games.BarbarianAssaultSession;
import com.runecraft.content.minigames.games.BrimhavenArenaSession;
import com.runecraft.content.minigames.games.CastleWarsSession;
import com.runecraft.content.minigames.games.FightCaveSession;
import com.runecraft.content.minigames.games.FishingTrawlerSession;
import com.runecraft.content.minigames.games.GiantsFoundrySession;
import com.runecraft.content.minigames.games.GnomeRestaurantSession;
import com.runecraft.content.minigames.games.GuardiansSession;
import com.runecraft.content.minigames.games.InfernoSession;
import com.runecraft.content.minigames.games.MageArenaSession;
import com.runecraft.content.minigames.games.MahoganyHomesSession;
import com.runecraft.content.minigames.games.PestControlSession;
import com.runecraft.content.minigames.games.PyramidPlunderSession;
import com.runecraft.content.minigames.games.RoguesDenSession;
import com.runecraft.content.minigames.games.SepulchreSession;
import com.runecraft.content.minigames.games.ShadesSession;
import com.runecraft.content.minigames.games.TemporossSession;
import com.runecraft.content.minigames.games.TitheFarmSession;
import com.runecraft.content.minigames.games.TroubleBrewingSession;
import com.runecraft.content.minigames.games.WarriorsGuildSession;
import com.runecraft.content.minigames.games.WintertodtSession;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Runs every minigame: lobbies (waiting rooms that depart after a countdown), instance slots,
 * active sessions and the routing of kills, block clicks, deaths and logouts to them.
 */
public final class MinigameManager {
    private static final Map<Integer, MinigameSession> SESSIONS = new LinkedHashMap<>();
    private static final Map<UUID, MinigameSession> BY_PLAYER = new HashMap<>();
    private static final Map<Minigame, Set<Integer>> SLOTS = new HashMap<>();
    private static final Map<String, Lobby> LOBBIES = new LinkedHashMap<>();
    private static final Map<UUID, String> CHOSEN = new HashMap<>();
    private static int nextId = 1;
    private static long gameTicks;

    private static final class Lobby {
        final Minigame game;
        final String variant;
        final List<UUID> waiting = new ArrayList<>();
        final Map<UUID, BlockPos> joinedAt = new HashMap<>();
        long departAt;

        Lobby(Minigame game, String variant) {
            this.game = game;
            this.variant = variant;
        }
    }

    private MinigameManager() {
    }

    /** The entrance block a player used last (their chosen role, room or difficulty). */
    public static String chosenVariant(ServerPlayer player) {
        return CHOSEN.getOrDefault(player.getUUID(), "");
    }

    public static MinigameSession session(ServerPlayer player) {
        return BY_PLAYER.get(player.getUUID());
    }

    /**
     * True (and tells the player) while they play a game on a stashed kit (Last Man Standing, a ranked Emir's Arena duel):
     * teleporting out would carry the kit, its supplies and the build's levels away before {@link LmsStash#restore} runs.
     */
    public static boolean teleportBlocked(ServerPlayer player) {
        if (session(player) == null || !LmsStash.has(player)) {
            return false;
        }
        Msg.game(player, "A magical force stops you from teleporting away.");
        return true;
    }

    static void forget(ServerPlayer player) {
        BY_PLAYER.remove(player.getUUID());
    }

    static void release(MinigameSession session) {
        SESSIONS.remove(session.id);
        Set<Integer> used = SLOTS.get(session.game);
        if (used != null) {
            used.remove(session.slot);
        }
        BY_PLAYER.values().removeIf(s -> s == session);
    }

    /** The creature id (registry path) of a RuneCraft entity, or the full id for others. */
    public static String creatureId(Entity e) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType());
        return id.getNamespace().equals(RuneCraft.MOD_ID) ? id.getPath() : id.toString();
    }

    /** The session a tagged creature belongs to. */
    public static MinigameSession owner(Entity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(MinigameSession.TAG + "_")) {
                try {
                    return SESSIONS.get(Integer.parseInt(tag.substring(MinigameSession.TAG.length() + 1)));
                } catch (NumberFormatException ignored) {
                    // not ours
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ joining

    /** Entrance block clicked: check requirements, then start or queue in the lobby. */
    /** Coins Cap'n Izzy No-Beard charges to enter the Brimhaven Agility Arena. */
    static final int BRIMHAVEN_FEE = 200;

    public static void enter(ServerPlayer player, Minigame game, String variant) {
        if (!Gielinor.is(player)) {
            return;
        }
        if (session(player) != null) {
            Msg.game(player, "You're already in a minigame.");
            return;
        }
        String problem = requirement(player, game, variant);
        if (problem != null) {
            Msg.game(player, problem);
            return;
        }
        if (game == Minigame.BRIMHAVEN_AGILITY_ARENA) {
            // Cap'n Izzy No-Beard's entry fee.
            if (!Inv.has(player, Inv.COINS, BRIMHAVEN_FEE)) {
                Msg.game(player, "Cap'n Izzy No-Beard: It costs " + BRIMHAVEN_FEE + " coins to enter the Agility Arena.");
                return;
            }
            Inv.remove(player, Inv.COINS, BRIMHAVEN_FEE);
            Msg.game(player, "You pay Cap'n Izzy No-Beard " + BRIMHAVEN_FEE + " coins.");
        }
        CHOSEN.put(player.getUUID(), variant);
        if (game.lobbySeconds() <= 0) {
            start(game, variant, List.of(player));
            return;
        }
        for (Lobby l : LOBBIES.values()) {
            l.waiting.remove(player.getUUID());
        }
        // Barbarian Assault players pick their role at different entrances but share one lobby.
        String key = game == Minigame.BARBARIAN_ASSAULT ? game.entranceBlock() : variant;
        Lobby lobby = LOBBIES.computeIfAbsent(key, v -> new Lobby(game, key));
        if (lobby.waiting.isEmpty()) {
            lobby.departAt = gameTicks + game.lobbySeconds() * 10L / 6;
        }
        lobby.waiting.add(player.getUUID());
        lobby.joinedAt.put(player.getUUID(), player.blockPosition());
        Msg.game(player, "You join the " + game.displayName() + " lobby. The game starts in "
                + Math.max(1, (lobby.departAt - gameTicks) * 6 / 10) + " seconds.");
        if (lobby.waiting.size() >= game.maxPlayers()) {
            lobby.departAt = gameTicks;
        }
    }

    /** First unmet requirement as a message, or null. */
    static String requirement(ServerPlayer player, Minigame game, String variant) {
        var skills = Players.data(player).skills();
        if (game == Minigame.PEST_CONTROL) {
            int tier = variant.contains("veteran") ? 2 : variant.contains("intermediate") ? 1 : 0;
            int need = MinigameRules.PestControl.COMBAT[tier];
            if (skills.combatLevel() < need) {
                return "You need a combat level of " + need + " to board the " + MinigameRules.PestControl.TIERS[tier] + " lander.";
            }
            if (MinigameData.of(player).points(player.getUUID(), "pest_control") >= MinigameRules.PestControl.MAX_POINTS) {
                return "Squire: You have " + MinigameRules.PestControl.MAX_POINTS + " commendation points already. Spend some first!";
            }
            return null;
        }
        if (game == Minigame.WARRIORS_GUILD) {
            if (!MinigameRules.WarriorsGuild.canEnter(skills.level(Skill.ATTACK), skills.level(Skill.STRENGTH))) {
                return "You need a combined Attack and Strength level of 130, or 99 in either, to enter the Warriors' Guild.";
            }
            if (variant.contains("cyclops") && !Inv.has(player, "warrior_guild_token", MinigameRules.WarriorsGuild.CYCLOPS_ENTRY_TOKENS)) {
                return "Kamfreena: You need at least " + MinigameRules.WarriorsGuild.CYCLOPS_ENTRY_TOKENS
                        + " warrior guild tokens to enter the cyclops room.";
            }
            return null;
        }
        if (game == Minigame.INFERNO) {
            boolean sacrificed = MinigameData.of(player).flag(player.getUUID(), "inferno_cape_sacrificed");
            if (!sacrificed && !Inv.has(player, "fire_cape", 1)) {
                return "TzHaar-Ket-Keh: You must sacrifice a fire cape to enter the Inferno, JalYt.";
            }
            return null;
        }
        if (game == Minigame.TZHAAR_KET_RAK) {
            return ketRakRequirement(player, variant);
        }
        if (game == Minigame.TEMPLE_TREKKING) {
            return com.runecraft.content.minigames.games.TempleTrekkingSession.requirement(player, variant);
        }
        if (game == Minigame.CHAMPIONS_CHALLENGE) {
            return com.runecraft.content.minigames.games.ChampionsSession.requirement(player);
        }
        if (game == Minigame.ROGUES_DEN && skills.level(Skill.AGILITY) < MinigameRules.RoguesDen.LEVEL) {
            return "You need an Agility level of " + MinigameRules.RoguesDen.LEVEL + " to enter the Rogues' Den maze.";
        }
        if (game == Minigame.NIGHTMARE_ZONE) {
            return com.runecraft.content.minigames.games.NightmareZoneSession.requirement(player, variant);
        }
        if (game == Minigame.SOUL_WARS) {
            if (skills.combatLevel() < MinigameRules.SoulWars.COMBAT_LEVEL || skills.totalLevel() < MinigameRules.SoulWars.TOTAL_LEVEL) {
                return "You need a combat level of " + MinigameRules.SoulWars.COMBAT_LEVEL + " and a total level of "
                        + MinigameRules.SoulWars.TOTAL_LEVEL + " to play Soul Wars.";
            }
            return null;
        }
        if (game == Minigame.TEARS_OF_GUTHIX) {
            return com.runecraft.content.minigames.games.TearsOfGuthixSession.requirement(player);
        }
        if (game == Minigame.SHADES_OF_MORTTON) {
            return ShadesSession.requirement(player);
        }
        if (game == Minigame.LAST_MAN_STANDING) {
            return com.runecraft.content.minigames.games.LmsSession.requirement(player, variant);
        }
        if (game == Minigame.GAUNTLET) {
            return com.runecraft.content.minigames.games.GauntletSession.requirement(player, variant);
        }
        if (game == Minigame.FORTIS_COLOSSEUM) {
            return com.runecraft.content.minigames.games.ColosseumSession.requirement(player);
        }
        if (game == Minigame.SORCERESS_GARDEN) {
            return com.runecraft.content.minigames.games.SorceressGardenSession.requirement(player);
        }
        if (game.skill() != null && skills.level(game.skill()) < game.level()) {
            return "You need " + Msg.article(game.skill().displayName())
                    + " level of " + game.level() + " to take part in " + game.displayName() + ".";
        }
        return null;
    }

    /**
     * TzHaar-Ket-Rak's challenges: the first two need access to the inner city of Mor Ul Rek (a fire cape earned), the rest
     * an Inferno completion.
     */
    static String ketRakRequirement(ServerPlayer player, String variant) {
        MinigameData data = MinigameData.of(player);
        int challenge = com.runecraft.osrs.content.minigames.KetRakChallenges.challengeOf(variant);
        if (com.runecraft.osrs.content.minigames.KetRakChallenges.requiresInferno(challenge)
                && data.stat(player.getUUID(), "kc:" + Minigame.INFERNO.id()) < 1) {
            return "TzHaar-Ket-Rak: Only those who have defeated TzKal-Zuk in the Inferno may face this challenge, JalYt.";
        }
        boolean fireCape = data.stat(player.getUUID(), "kc:" + Minigame.FIGHT_CAVES.id()) > 0
                || data.flag(player.getUUID(), "inferno_cape_sacrificed") || Inv.has(player, "fire_cape", 1)
                || Players.data(player).collectionLog().contains("fire_cape");
        if (!fireCape) {
            return "TzHaar-Ket-Rak: Only JalYt who have earned a fire cape may enter the inner city and take my challenges.";
        }
        return null;
    }

    private static int allocateSlot(Minigame game) {
        Set<Integer> used = SLOTS.computeIfAbsent(game, g -> new HashSet<>());
        int slot = 0;
        while (used.contains(slot)) {
            slot++;
        }
        used.add(slot);
        return slot;
    }

    static MinigameSession create(Minigame game, int id, int slot, ServerLevel world, String variant) {
        return switch (game) {
            case FIGHT_CAVES -> new FightCaveSession(id, slot, world, variant);
            case INFERNO -> new InfernoSession(id, slot, world, variant);
            case PEST_CONTROL -> new PestControlSession(id, slot, world, variant);
            case WINTERTODT -> new WintertodtSession(id, slot, world, variant);
            case TEMPOROSS -> new TemporossSession(id, slot, world, variant);
            case GUARDIANS_OF_THE_RIFT -> new GuardiansSession(id, slot, world, variant);
            case BARBARIAN_ASSAULT -> new BarbarianAssaultSession(id, slot, world, variant);
            case CASTLE_WARS -> new CastleWarsSession(id, slot, world, variant);
            case WARRIORS_GUILD -> new WarriorsGuildSession(id, slot, world, variant);
            case MAGE_TRAINING_ARENA -> new MageArenaSession(id, slot, world, variant);
            case PYRAMID_PLUNDER -> new PyramidPlunderSession(id, slot, world, variant);
            case HALLOWED_SEPULCHRE -> new SepulchreSession(id, slot, world, variant);
            case BRIMHAVEN_AGILITY_ARENA -> new BrimhavenArenaSession(id, slot, world, variant);
            case GNOME_RESTAURANT -> new GnomeRestaurantSession(id, slot, world, variant);
            case TROUBLE_BREWING -> new TroubleBrewingSession(id, slot, world, variant);
            case SHADES_OF_MORTTON -> new ShadesSession(id, slot, world, variant);
            case TITHE_FARM -> new TitheFarmSession(id, slot, world, variant);
            case GIANTS_FOUNDRY -> new GiantsFoundrySession(id, slot, world, variant);
            case MAHOGANY_HOMES -> new MahoganyHomesSession(id, slot, world, variant);
            case FISHING_TRAWLER -> new FishingTrawlerSession(id, slot, world, variant);
            case ROGUES_DEN -> new RoguesDenSession(id, slot, world, variant);
            case CHAMPIONS_CHALLENGE -> new com.runecraft.content.minigames.games.ChampionsSession(id, slot, world, variant);
            case VOLCANIC_MINE -> new com.runecraft.content.minigames.games.VolcanicMineSession(id, slot, world, variant);
            case NIGHTMARE_ZONE -> new com.runecraft.content.minigames.games.NightmareZoneSession(id, slot, world, variant);
            case SOUL_WARS -> new com.runecraft.content.minigames.games.SoulWarsSession(id, slot, world, variant);
            case TEARS_OF_GUTHIX -> new com.runecraft.content.minigames.games.TearsOfGuthixSession(id, slot, world, variant);
            case SORCERESS_GARDEN -> new com.runecraft.content.minigames.games.SorceressGardenSession(id, slot, world, variant);
            case FIGHT_PITS -> new com.runecraft.content.minigames.games.FightPitsSession(id, slot, world, variant);
            case TZHAAR_KET_RAK -> new com.runecraft.content.minigames.games.KetRakSession(id, slot, world, variant);
            case TEMPLE_TREKKING -> new com.runecraft.content.minigames.games.TempleTrekkingSession(id, slot, world, variant);
            case LAST_MAN_STANDING -> new com.runecraft.content.minigames.games.LmsSession(id, slot, world, variant);
            case EMIRS_ARENA -> new com.runecraft.content.minigames.games.EmirsArenaSession(id, slot, world, variant);
            case GAUNTLET -> new com.runecraft.content.minigames.games.GauntletSession(id, slot, world, variant);
            case FORTIS_COLOSSEUM -> new com.runecraft.content.minigames.games.ColosseumSession(id, slot, world, variant);
        };
    }

    static void start(Minigame game, String variant, List<ServerPlayer> joining) {
        if (joining.isEmpty()) {
            return;
        }
        ServerLevel world = Gielinor.world(joining.get(0).getServer());
        if (world == null) {
            return;
        }
        int slot = allocateSlot(game);
        MinigameSession s = create(game, nextId++, slot, world, variant);
        SESSIONS.put(s.id, s);
        for (ServerPlayer p : joining) {
            BY_PLAYER.put(p.getUUID(), s);
        }
        try {
            s.begin(joining);
        } catch (RuntimeException e) {
            RuneCraft.LOGGER.error("Failed to start minigame {}", game.id(), e);
            s.end(false);
        }
    }

    /**
     * Self-test hook ({@code com.runecraft.selftest}): starts a session for one player straight away (no lobby or
     * requirements) and lets exceptions through instead of logging them.
     */
    public static MinigameSession selfTestStart(Minigame game, String variant, ServerPlayer player) {
        ServerLevel world = Gielinor.world(player.getServer());
        MinigameSession s = create(game, nextId++, allocateSlot(game), world, variant);
        SESSIONS.put(s.id, s);
        BY_PLAYER.put(player.getUUID(), s);
        CHOSEN.put(player.getUUID(), variant);
        try {
            s.begin(List.of(player));
        } catch (RuntimeException e) {
            s.end(false);
            throw e;
        }
        return s;
    }

    /** Self-test hook: runs one OSRS game tick of a session, letting exceptions through. */
    public static void selfTestTick(MinigameSession s) {
        s.tick();
    }

    /** Leaves a session or lobby (exit portal, command). */
    public static void leave(ServerPlayer player) {
        for (Lobby l : LOBBIES.values()) {
            if (l.waiting.remove(player.getUUID())) {
                Msg.game(player, "You leave the lobby.");
                MinigameNet.sendHud(player, List.of());
            }
        }
        MinigameSession s = session(player);
        if (s != null) {
            Msg.game(player, "You leave " + s.game.displayName() + ".");
            s.removePlayer(player, true);
        }
    }

    // ------------------------------------------------------------------ ticking

    static void serverTick(MinecraftServer server) {
        if (server.getTickCount() % 12 != 0) {
            return;
        }
        gameTicks++;
        for (MinigameSession s : new ArrayList<>(SESSIONS.values())) {
            try {
                s.tick();
                if (!s.isEnded()) {
                    for (ServerPlayer p : s.online()) {
                        MinigameNet.sendHud(p, s.hud(p));
                    }
                }
            } catch (RuntimeException e) {
                RuneCraft.LOGGER.error("Minigame {} crashed; ending it", s.game.id(), e);
                s.end(false);
            }
        }
        Iterator<Lobby> it = LOBBIES.values().iterator();
        List<Lobby> departing = new ArrayList<>();
        while (it.hasNext()) {
            Lobby l = it.next();
            l.waiting.removeIf(u -> {
                ServerPlayer p = server.getPlayerList().getPlayer(u);
                BlockPos at = l.joinedAt.get(u);
                boolean gone = p == null || at == null || p.blockPosition().distSqr(at) > 24 * 24 || session(p) != null;
                if (gone && p != null) {
                    MinigameNet.sendHud(p, List.of());
                }
                return gone;
            });
            if (l.waiting.isEmpty()) {
                it.remove();
                continue;
            }
            if (gameTicks >= l.departAt) {
                departing.add(l);
                it.remove();
            } else {
                for (UUID u : l.waiting) {
                    ServerPlayer p = server.getPlayerList().getPlayer(u);
                    if (p != null) {
                        MinigameNet.sendHud(p, List.of(l.game.displayName() + " lobby",
                                "Players ready: " + l.waiting.size() + "/" + l.game.maxPlayers(),
                                "Departs in: " + Math.max(0, (l.departAt - gameTicks) * 6 / 10) + "s"));
                    }
                }
            }
        }
        for (Lobby l : departing) {
            List<ServerPlayer> players = new ArrayList<>();
            for (UUID u : l.waiting) {
                ServerPlayer p = server.getPlayerList().getPlayer(u);
                if (p != null && requirement(p, l.game, l.variant) == null) {
                    players.add(p);
                }
            }
            start(l.game, l.variant, players);
        }
    }

    // ------------------------------------------------------------------ routing

    static void onKill(ServerPlayer player, net.minecraft.world.entity.LivingEntity entity) {
        MinigameSession s = owner(entity);
        if (s != null && !s.isEnded()) {
            s.onKill(player, entity, creatureId(entity));
        }
    }

    static void onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        MinigameSession s = session(player);
        if (s == null && com.runecraft.content.BlockHooks.useFallback(player, blockId, pos, player.level().getBlockState(pos))) {
            return;
        }
        if (s == null || !s.onBlock(player, blockId, pos)) {
            if (blockId.equals("minigame_exit") && s != null) {
                leave(player);
                return;
            }
            Msg.game(player, "Nothing interesting happens.");
        }
    }

    /** Returns false to cancel a player's death (they're handled by their minigame). */
    static boolean allowDeath(ServerPlayer player) {
        MinigameSession s = session(player);
        if (s == null) {
            return true;
        }
        if (s.isDangerous()) {
            // A real death (items lost as anywhere else), at the minigame's entry point rather than in the instance.
            s.dieDangerously(player);
            return true;
        }
        player.setHealth(player.getMaxHealth());
        player.clearFire();
        s.onDeath(player);
        return false;
    }

    static void onDisconnect(ServerPlayer player) {
        for (Lobby l : LOBBIES.values()) {
            l.waiting.remove(player.getUUID());
        }
        MinigameSession s = session(player);
        if (s != null) {
            s.removePlayer(player, true);
        }
    }

    /** Players who log in inside an instance with no running game are sent to Lumbridge. */
    static void onJoin(ServerPlayer player) {
        if (Gielinor.is(player) && Arena.inInstanceArea(player.blockPosition()) && session(player) == null) {
            Msg.game(player, "The minigame you were in has ended.");
            Gielinor.teleport(player, com.runecraft.osrs.Landmark.LUMBRIDGE);
        }
    }

    public static List<MinigameSession> sessions() {
        return new ArrayList<>(SESSIONS.values());
    }
}
