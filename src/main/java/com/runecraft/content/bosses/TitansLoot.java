package com.runecraft.content.bosses;

import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.content.bosses.RoyalTitansLoot;
import com.runecraft.osrs.content.bosses.RoyalTitansLoot.Choice;
import com.runecraft.platform.CommandRegistrationCallback;
import com.runecraft.skill.Drops;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * The fallen Royal Titans (OSRS Wiki "Royal Titans" Rewards): every player who damaged them chooses, through chat links
 * ({@code /titans loot|pages branda|eldric}, {@code /titans sacrifice}), to loot one Titan, sacrifice the loot for a better
 * pet chance, or take desiccated pages. Each player's rewards are rolled on their own contribution
 * ({@link RoyalTitansLoot}); the kill drops nothing itself, but every fighter's kill count comes with it. A choice left open
 * for five minutes loots Branda. Open choices are kept in the world save ({@link State}), so a restart or a crash before
 * the choice loses nothing: the player is asked again when they next log in (or gets Branda's loot if the five minutes
 * are up by then).
 */
public final class TitansLoot {
    /** Five minutes (Minecraft ticks of the overworld's game time, which carries over a restart). */
    private static final long TIMEOUT_MC_TICKS = 5 * 60 * 20;

    /** A player's open choice: their damage to the Titans, when the Titans fell and whether they landed the kill. */
    private record Pending(int damage, long since, boolean killer) {
    }

    /** The fallen Branda behind each open choice, while she is still loaded (for the drop transformers; gone after a restart). */
    private static final Map<UUID, DataBossEntity> BRANDA = new HashMap<>();
    /** The last choice made (self-test: what each player received). */
    private static final Map<UUID, List<RoyalTitansLoot.Loot>> LAST = new HashMap<>();

    private TitansLoot() {
    }

    static void register() {
        DataBossService.loot(RoyalTitansLoot.BRANDA, (killer, boss, table) -> {
            open(killer, boss);
            return List.of(); // each fighter chooses (and rolls) their own loot
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) -> dispatcher.register(Commands.literal("titans")
                .then(Commands.literal("loot").then(Commands.argument("titan", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p != null) {
                        choose(p, Choice.LOOT, StringArgumentType.getString(ctx, "titan"));
                    }
                    return 1;
                })))
                .then(Commands.literal("pages").then(Commands.argument("titan", StringArgumentType.word()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p != null) {
                        choose(p, Choice.PAGES, StringArgumentType.getString(ctx, "titan"));
                    }
                    return 1;
                })))
                .then(Commands.literal("sacrifice").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p != null) {
                        choose(p, Choice.SACRIFICE, RoyalTitansLoot.BRANDA);
                    }
                    return 1;
                }))));
        com.runecraft.platform.ServerTickEvents.END_SERVER_TICK.register(TitansLoot::timeouts);
        // A choice still open from before a restart (or a logout) is asked again, or resolved if its time is up.
        com.runecraft.platform.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.getPlayer();
            Pending pending = State.get(server).pending.get(p.getUUID());
            if (pending == null) {
                return;
            }
            if (expired(server, pending)) {
                State.get(server).remove(p.getUUID());
                resolve(p, pending, Choice.LOOT, RoyalTitansLoot.BRANDA);
            } else {
                prompt(p, pending);
            }
        });
    }

    private static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private static boolean expired(MinecraftServer server, Pending pending) {
        long elapsed = now(server) - pending.since();
        return elapsed >= TIMEOUT_MC_TICKS || elapsed < 0;
    }

    /** The Titans fell: every fighter still in the lair (and the killer) gets their choice. */
    static void open(ServerPlayer killer, DataBossEntity branda) {
        EncounterScripts.RoyalTitans titans = branda.script() instanceof EncounterScripts.RoyalTitans t ? t : null;
        long now = now(killer.getServer());
        boolean killerFought = false;
        if (titans != null) {
            for (ServerPlayer p : titans.fighters()) {
                boolean inLair = p.isAlive() && p.level() == branda.level() && p.distanceToSqr(branda) < 48 * 48;
                if (p == killer || inLair) {
                    killerFought |= p == killer;
                    offer(p, new Pending(titans.damageBy(p), now, p == killer), branda);
                }
            }
        }
        if (!killerFought) {
            // a kill by a player the script didn't see hit (a damage path without the hit hook): full credit for the killer
            offer(killer, new Pending(RoyalTitansLoot.TOTAL_HITPOINTS, now, true), branda);
        }
    }

    private static void offer(ServerPlayer p, Pending pending, DataBossEntity branda) {
        State state = State.get(p.getServer());
        Pending before = state.pending.get(p.getUUID());
        if (before != null) {
            resolve(p, before, Choice.LOOT, RoyalTitansLoot.BRANDA); // a choice left from an earlier kill loots Branda
        }
        state.put(p.getUUID(), pending);
        BRANDA.put(p.getUUID(), branda);
        if (!pending.killer()) {
            // The killer's kill count and first pet roll came with the kill; the other fighters' kill count comes now too
            // (whatever they choose, and even if the choice is never made).
            int kc = com.runecraft.player.Players.data(p).incrementKillCount(RoyalTitansLoot.BRANDA);
            Msg.game(p, "Your Royal Titans kill count is: " + kc + ".");
            if (branda.def() != null) {
                com.runecraft.content.GameEvents.fireKill(p, branda, com.runecraft.combat.MonsterStats.from(branda.def()));
            }
        }
        prompt(p, pending);
    }

    private static MutableComponent button(String label, String command, String hover, ChatFormatting colour) {
        return Component.literal(label).withStyle(s -> s.withColor(colour)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    private static void prompt(ServerPlayer p, Pending pending) {
        p.sendSystemMessage(Component.literal("The Royal Titans lie defeated (your contribution: "
                        + RoyalTitansLoot.percent(pending.damage()) + "%). ").withStyle(ChatFormatting.RED)
                .append(button("[Loot Branda]", "/titans loot branda", "Two rolls on Branda's table (or a unique).", ChatFormatting.GOLD))
                .append(Component.literal(" "))
                .append(button("[Loot Eldric]", "/titans loot eldric", "Two rolls on Eldric's table (or a unique).", ChatFormatting.AQUA))
                .append(Component.literal(" "))
                .append(button("[Sacrifice]", "/titans sacrifice", "Forfeit the loot to double your chance at the pet.",
                        ChatFormatting.DARK_RED))
                .append(Component.literal(" "))
                .append(button("[Take pages]", "/titans pages branda", "10-19 desiccated pages.", ChatFormatting.GRAY)));
    }

    /** Makes a player's choice; false when they have none open. Public for the self-test. */
    public static boolean choose(ServerPlayer p, Choice choice, String titanName) {
        String titan = RoyalTitansLoot.titan(titanName);
        if (titan == null) {
            Msg.game(p, "Loot Branda or Eldric.");
            return false;
        }
        Pending pending = State.get(p.getServer()).remove(p.getUUID());
        if (pending == null) {
            Msg.game(p, "There are no fallen Titans of yours to loot.");
            return false;
        }
        resolve(p, pending, choice, titan);
        return true;
    }

    private static void resolve(ServerPlayer p, Pending pending, Choice choice, String titan) {
        DataBossEntity branda = BRANDA.remove(p.getUUID());
        List<RoyalTitansLoot.Loot> loot = RoyalTitansLoot.roll(titan, choice, pending.damage(),
                new java.util.Random(p.getRandom().nextLong()));
        LAST.put(p.getUUID(), loot);
        for (RoyalTitansLoot.Loot l : loot) {
            int doses = com.runecraft.osrs.data.Drop.dosesOf(l.item());
            String item = com.runecraft.osrs.data.Drop.baseOf(l.item());
            Inv.give(p, Drops.withDoses(branda != null ? com.runecraft.combat.CombatHooks.dropStack(p, branda, item, l.count())
                    : Inv.stack(item, l.count()), doses));
            CollectionLog.obtain(p, item);
        }
        // The killer's first pet roll came with the kill; the others' come here. Sacrificing adds a pet roll (1/3,000 twice:
        // about the wiki's 1/1,500).
        int petRolls = (pending.killer() ? 0 : 1) + (choice == Choice.SACRIFICE ? 1 : 0);
        for (int i = 0; i < petRolls; i++) {
            if (p.getRandom().nextInt(RoyalTitansLoot.PET) == 0) {
                Inv.give(p, "pet_bran", 1);
                CollectionLog.obtain(p, "pet_bran");
                p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                        + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
            } else if (choice == Choice.SACRIFICE && i == petRolls - 1) {
                Msg.game(p, "You don't feel lucky during your sacrifice.");
            }
        }
        Msg.game(p, switch (choice) {
            case LOOT -> "You loot " + (RoyalTitansLoot.ELDRIC.equals(titan) ? "Eldric the Ice King." : "Branda the Fire Queen.");
            case SACRIFICE -> "You sacrifice the Titans' loot.";
            case PAGES -> "You take the Titans' pages.";
        });
    }

    /** Every five seconds: an online player's choice open for five minutes loots Branda (an offline one waits for login). */
    private static void timeouts(MinecraftServer server) {
        if (server.getTickCount() % 100 != 0) {
            return;
        }
        State state = State.get(server);
        if (state.pending.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Pending> e : List.copyOf(state.pending.entrySet())) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null && expired(server, e.getValue())) {
                state.remove(e.getKey());
                resolve(p, e.getValue(), Choice.LOOT, RoyalTitansLoot.BRANDA);
            }
        }
    }

    /** Self-test: the damage behind this player's open choice, or -1 when none is open. */
    public static int pendingDamage(ServerPlayer p) {
        Pending pending = State.get(p.getServer()).pending.get(p.getUUID());
        return pending == null ? -1 : pending.damage();
    }

    /** Self-test: what this player's last choice gave. */
    public static List<RoyalTitansLoot.Loot> lastLoot(ServerPlayer p) {
        return LAST.getOrDefault(p.getUUID(), List.of());
    }

    /** Self-test: open choices held for the player (the fight check counts them as the kill's loot). */
    public static int held(ServerPlayer p) {
        return State.get(p.getServer()).pending.containsKey(p.getUUID()) ? 1 : 0;
    }

    /** Self-test: whether this player's open choice is in the world save (it survives a restart) with this damage. */
    public static boolean savedChoice(ServerPlayer p, int damage) {
        net.minecraft.core.HolderLookup.Provider registries = p.getServer().registryAccess();
        CompoundTag saved = State.get(p.getServer()).save(new CompoundTag(), registries);
        Pending pending = State.fromNbt(saved, registries).pending.get(p.getUUID());
        return pending != null && pending.damage() == damage;
    }

    /**
     * Self-test: drops a player's open choice from the world save, with its fallen Branda and last loot. A fake player never
     * logs in, so {@link #timeouts} never resolves its choice: one a failed check leaves would stay in the save for good, and
     * the next kill's {@link #offer} would loot Branda with it first.
     */
    static void forget(MinecraftServer server, UUID player) {
        State.get(server).remove(player);
        BRANDA.remove(player);
        LAST.remove(player);
    }

    /** A player's open choice with its fallen Branda, saved around the self-test. */
    private record Saved(Pending pending, DataBossEntity branda) {
    }

    /** Self-test: a (real) player's own open choice before the self-test, or null when none is open. */
    static Object selfTestSave(MinecraftServer server, UUID player) {
        Pending pending = State.get(server).pending.get(player);
        return pending == null ? null : new Saved(pending, BRANDA.get(player));
    }

    /** Self-test: puts a saved choice back (its five minutes run from when the Titans fell), or drops the open one (null). */
    static void selfTestRestore(MinecraftServer server, UUID player, Object saved) {
        forget(server, player);
        if (saved instanceof Saved s) {
            State.get(server).put(player, s.pending());
            if (s.branda() != null) {
                BRANDA.put(player, s.branda());
            }
        }
    }

    /** World-save storage of the open choices (player -> damage, when the Titans fell, killer). */
    static final class State extends SavedData {
        static final String KEY = "runecraft_bosses_titans";

        private final Map<UUID, Pending> pending = new HashMap<>();

        static State get(MinecraftServer server) {
            return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(State::new, State::fromNbt, null), KEY);
        }

        void put(UUID uuid, Pending p) {
            pending.put(uuid, p);
            setDirty();
        }

        Pending remove(UUID uuid) {
            Pending p = pending.remove(uuid);
            if (p != null) {
                setDirty();
            }
            return p;
        }

        @Override
        public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
            pending.forEach((uuid, p) -> {
                CompoundTag c = new CompoundTag();
                c.putInt("Damage", p.damage());
                c.putLong("Since", p.since());
                c.putBoolean("Killer", p.killer());
                nbt.put(uuid.toString(), c);
            });
            return nbt;
        }

        static State fromNbt(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
            State state = new State();
            for (String key : nbt.getAllKeys()) {
                try {
                    CompoundTag c = nbt.getCompound(key);
                    state.pending.put(UUID.fromString(key), new Pending(c.getInt("Damage"), c.getLong("Since"), c.getBoolean("Killer")));
                } catch (IllegalArgumentException ignored) {
                    // Not a player entry.
                }
            }
            return state;
        }
    }
}
