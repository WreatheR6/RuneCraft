package com.runecraft.selftest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.RuneCraft;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * In-game play-test: {@code /rc selftest [filter]} (op) queues gameplay checks ({@link GameplayChecks}) that run
 * on the real server against a Fabric fake player, a few per server tick, and logs one line per check:
 * {@code SELFTEST PASS <name>}, {@code SELFTEST SKIP <name>: <reason>} (the check can't run meaningfully, e.g. a
 * minion that only exists next to its boss) or {@code SELFTEST FAIL <name>: <exception>} plus the first stack frames,
 * then {@code SELFTEST <n> passed, <m> failed, <k> skipped} and {@code SELFTEST END}. Used by tools/smoke_server.sh in CI.
 *
 * <p>{@code /rc selftest real [filter...]} runs the checks against the player who typed the command instead of a fake
 * player: a real, online player is in the server's player list, so minigame sessions, raid rooms, boss targeting and
 * area triggers see it. Its RuneCraft data, inventory, position and game mode are saved first and restored at the end;
 * meanwhile it is in survival mode (bosses ignore creative players), invulnerable and healed before every check. The
 * modules' per-player state (systems, Slayer profile, minigame points and flags, quest flags) is reset before every
 * check and put back at the end too; other module state is not, so it is meant for a throwaway test world
 * (tools/smoke_client.sh). Several filter words (separated by spaces or commas) match a check containing any of them.
 * Everything is logged at INFO so a failing check never trips the smoke test's "RuneCraft logged errors" rule.
 */
public final class SelfTest {
    /** One check; returns an optional note shown after PASS. Throw (e.g. {@link AssertionError}) to fail. */
    @FunctionalInterface
    public interface Body {
        String run() throws Exception;
    }

    public record Check(String name, Body body) {
    }

    /** Thrown by a check that can't run meaningfully here; logged as SKIP, counted apart from passes and failures. */
    public static final class Skip extends RuntimeException {
        public Skip(String reason) {
            super(reason);
        }
    }

    /** Time budget per server tick (checks are never split, so one slow check can exceed it). */
    private static final long TICK_BUDGET_NANOS = 40_000_000L;
    private static final int STACK_FRAMES = 6;

    private static final Deque<Check> QUEUE = new ArrayDeque<>();
    private static final List<String> FAILURES = new ArrayList<>();
    private static final List<String> SKIPS = new ArrayList<>();
    private static volatile TestContext context;
    private static volatile int finishedRuns;
    private static CommandSourceStack reportTo;
    private static int passed;
    private static int failed;
    private static int skipped;
    private static long startedAt;

    private SelfTest() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            register(dispatcher, "rc");
            register(dispatcher, "runecraft");
        });
        ServerTickEvents.END_SERVER_TICK.register(SelfTest::tick);
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        dispatcher.register(Commands.literal(root)
                .then(Commands.literal("selftest").requires(s -> s.hasPermission(2))
                        .executes(ctx -> start(ctx.getSource(), "", false))
                        .then(Commands.literal("real")
                                .executes(ctx -> start(ctx.getSource(), "", true))
                                .then(Commands.argument("filter", StringArgumentType.greedyString())
                                        .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "filter"), true))))
                        .then(Commands.argument("filter", StringArgumentType.greedyString())
                                .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "filter"), false)))));
    }

    public static boolean running() {
        return context != null;
    }

    /** Number of self-test runs that have finished (logged SELFTEST END) since the server started. */
    public static int finishedRuns() {
        return finishedRuns;
    }

    private static int start(CommandSourceStack source, String filter, boolean real) {
        if (running()) {
            source.sendFailure(Component.literal("A self-test is already running (" + QUEUE.size() + " checks left)."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        ServerLevel world = Gielinor.world(server);
        ServerPlayer realPlayer = real ? source.getPlayer() : null;
        if (real && realPlayer == null) {
            source.sendFailure(Component.literal("'/rc selftest real' must be run by a player."));
            return 0;
        }
        log("SELFTEST BEGIN" + (real ? " (real player " + realPlayer.getName().getString() + ")" : "")
                + (filter.isEmpty() ? "" : " (filter " + filter + ")"));
        passed = 0;
        failed = 0;
        skipped = 0;
        FAILURES.clear();
        SKIPS.clear();
        reportTo = source;
        startedAt = System.currentTimeMillis();
        if (world == null) {
            fail("setup", new IllegalStateException("the Gielinor dimension is missing"));
            finish();
            return 0;
        }
        try {
            context = new TestContext(server, world, realPlayer);
            context.setUp();
        } catch (Throwable e) {
            context = null;
            fail("setup", e);
            finish();
            return 0;
        }
        List<String> words = new ArrayList<>();
        for (String w : filter.toLowerCase(Locale.ROOT).trim().split("[\\s,]+")) {
            if (!w.isEmpty()) {
                words.add(w);
            }
        }
        for (Check c : GameplayChecks.all(context)) {
            String name = c.name().toLowerCase(Locale.ROOT);
            if (words.isEmpty() || words.stream().anyMatch(name::contains)) {
                QUEUE.add(c);
            }
        }
        log("SELFTEST " + QUEUE.size() + " checks queued");
        source.sendSuccess(() -> Component.literal("RuneCraft self-test started: " + QUEUE.size() + " checks."), false);
        return 1;
    }

    private static void tick(MinecraftServer server) {
        if (context == null) {
            return;
        }
        long t0 = System.nanoTime();
        do {
            Check c = QUEUE.poll();
            if (c == null) {
                finish();
                return;
            }
            run(c);
        } while (System.nanoTime() - t0 < TICK_BUDGET_NANOS);
    }

    private static void run(Check c) {
        String note;
        long start = System.nanoTime();
        try {
            context.reset();
            note = c.body().run();
        } catch (Skip e) {
            skipped++;
            String line = "SELFTEST SKIP " + c.name() + ": " + e.getMessage();
            SKIPS.add(line);
            log(line);
            cleanUp(c.name());
            return;
        } catch (Throwable e) {
            fail(c.name(), e);
            cleanUp(c.name());
            return;
        }
        passed++;
        log("SELFTEST PASS " + c.name() + (note == null || note.isEmpty() ? "" : " (" + note + ")"));
        long ms = (System.nanoTime() - start) / 1_000_000L;
        if (ms >= 2000) {
            log("SELFTEST SLOW " + c.name() + " took " + ms + " ms");
        }
        cleanUp(c.name());
    }

    private static void cleanUp(String name) {
        try {
            context.cleanUp();
        } catch (Throwable e) {
            fail(name + "#cleanup", e);
        }
    }

    private static void fail(String name, Throwable e) {
        failed++;
        String line = "SELFTEST FAIL " + name + ": " + describe(e);
        FAILURES.add(line);
        log(line);
        Throwable t = e;
        for (int depth = 0; t != null && depth < 3; depth++, t = t.getCause()) {
            if (depth > 0) {
                log("SELFTEST   caused by " + describe(t));
            }
            if (t instanceof AssertionError && t.getCause() == null && depth == 0) {
                break; // a failed expectation: its message says it all
            }
            StackTraceElement[] frames = t.getStackTrace();
            for (int i = 0; i < Math.min(STACK_FRAMES, frames.length); i++) {
                log("SELFTEST   | " + frames[i]);
            }
        }
    }

    private static String describe(Throwable e) {
        if (e instanceof AssertionError && e.getMessage() != null) {
            return e.getMessage();
        }
        return e.toString();
    }

    private static void finish() {
        if (context != null) {
            try {
                context.tearDown();
            } catch (Throwable e) {
                fail("teardown", e);
            }
        }
        context = null;
        QUEUE.clear();
        long secs = (System.currentTimeMillis() - startedAt) / 1000;
        String summary = "SELFTEST " + passed + " passed, " + failed + " failed, " + skipped + " skipped (" + secs + "s)";
        log(summary);
        for (String f : FAILURES) {
            log("SELFTEST-SUMMARY " + f);
        }
        for (String s : SKIPS) {
            log("SELFTEST-SUMMARY " + s);
        }
        log("SELFTEST END");
        finishedRuns++;
        if (reportTo != null) {
            CommandSourceStack src = reportTo;
            src.sendSuccess(() -> Component.literal(summary), false);
            reportTo = null;
        }
    }

    static void log(String line) {
        RuneCraft.LOGGER.info(line);
    }
}
