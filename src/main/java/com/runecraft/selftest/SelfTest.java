package com.runecraft.selftest;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.runecraft.RuneCraft;
import com.runecraft.world.Gielinor;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * In-game play-test: {@code /rc selftest [filter]} (op) queues gameplay checks ({@link GameplayChecks}) that run
 * on the real server against a Fabric fake player, a few per server tick, and logs one line per check:
 * {@code SELFTEST PASS <name>} or {@code SELFTEST FAIL <name>: <exception>} plus the first stack frames, then
 * {@code SELFTEST <n> passed, <m> failed} and {@code SELFTEST END}. Used by tools/smoke_server.sh in CI.
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

    /** Time budget per server tick (checks are never split, so one slow check can exceed it). */
    private static final long TICK_BUDGET_NANOS = 40_000_000L;
    private static final int STACK_FRAMES = 6;

    private static final Deque<Check> QUEUE = new ArrayDeque<>();
    private static final List<String> FAILURES = new ArrayList<>();
    private static TestContext context;
    private static ServerCommandSource reportTo;
    private static int passed;
    private static int failed;
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

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher, String root) {
        dispatcher.register(CommandManager.literal(root)
                .then(CommandManager.literal("selftest").requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> start(ctx.getSource(), ""))
                        .then(CommandManager.argument("filter", StringArgumentType.word())
                                .executes(ctx -> start(ctx.getSource(), StringArgumentType.getString(ctx, "filter"))))));
    }

    public static boolean running() {
        return context != null;
    }

    private static int start(ServerCommandSource source, String filter) {
        if (running()) {
            source.sendError(Text.literal("A self-test is already running (" + QUEUE.size() + " checks left)."));
            return 0;
        }
        MinecraftServer server = source.getServer();
        ServerWorld world = Gielinor.world(server);
        log("SELFTEST BEGIN" + (filter.isEmpty() ? "" : " (filter " + filter + ")"));
        passed = 0;
        failed = 0;
        FAILURES.clear();
        reportTo = source;
        startedAt = System.currentTimeMillis();
        if (world == null) {
            fail("setup", new IllegalStateException("the Gielinor dimension is missing"));
            finish();
            return 0;
        }
        try {
            context = new TestContext(server, world);
            context.setUp();
        } catch (Throwable e) {
            context = null;
            fail("setup", e);
            finish();
            return 0;
        }
        String f = filter.toLowerCase(Locale.ROOT);
        for (Check c : GameplayChecks.all(context)) {
            if (f.isEmpty() || c.name().toLowerCase(Locale.ROOT).contains(f)) {
                QUEUE.add(c);
            }
        }
        log("SELFTEST " + QUEUE.size() + " checks queued");
        source.sendFeedback(() -> Text.literal("RuneCraft self-test started: " + QUEUE.size() + " checks."), false);
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
        String summary = "SELFTEST " + passed + " passed, " + failed + " failed (" + secs + "s)";
        log(summary);
        for (String f : FAILURES) {
            log("SELFTEST-SUMMARY " + f);
        }
        log("SELFTEST END");
        if (reportTo != null) {
            ServerCommandSource src = reportTo;
            src.sendFeedback(() -> Text.literal(summary), false);
            reportTo = null;
        }
    }

    static void log(String line) {
        RuneCraft.LOGGER.info(line);
    }
}
