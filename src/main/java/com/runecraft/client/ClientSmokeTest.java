package com.runecraft.client;

import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.world.Gielinor;
import com.runecraft.world.LandmarkBuilder;
import com.runecraft.platform.client.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.GenericMessageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Dev/CI-only automated client walkthrough, enabled ONLY with {@code -Drunecraft.clientSmoke=true}
 * (see the {@code clientSmoke} Loom run and .github/workflows/build.yml). Normal players never
 * reach this code.
 *
 * <p>It loads the singleplayer world named by {@code -Drunecraft.clientSmoke.world} (default
 * "smoke"; normally started by {@code --quickPlaySingleplayer}, otherwise from the title screen),
 * goes to Gielinor, visits Lumbridge, Varrock and a boss arena, summons creatures with custom models,
 * places station blocks, opens the side panel tabs, the full interface and the inventory, takes a
 * screenshot at every stop ({@code screenshots/runecraft_*.png} in the run directory), then runs the in-game
 * self-test as the real player ({@code /rc selftest real <filter>}, filter {@code -Drunecraft.clientSmoke.selftest},
 * default minigames, raids and bosses: the checks that need an online player; "none" skips it) and quits.
 * The last line is {@code RUNECRAFT CLIENT SMOKE: OK} or {@code RUNECRAFT CLIENT SMOKE: FAILED}.
 */
public final class ClientSmokeTest {
    public static final String PROPERTY = "runecraft.clientSmoke";
    private static final Logger LOG = LoggerFactory.getLogger("RuneCraftSmoke");
    private static final int WORLD_TIMEOUT_TICKS = 20 * 60 * 6;
    /** Client ticks for the whole run, self-test included (the shell step's wall-clock timeout is the real limit). */
    private static final int TOTAL_TIMEOUT_TICKS = 20 * 60 * 26;
    private static final long QUIT_TIMEOUT_MS = 90_000;
    private static final List<String> BOSSES = List.of("tztok_jad", "general_graardor", "king_black_dragon", "kreearra",
            "zulrah", "vorkath", "cerberus", "abyssal_sire");
    private static final List<String> MONSTERS = List.of("man", "goblin", "guard", "hill_giant", "hobgoblin");
    private static final List<String> STATIONS = List.of("bank_booth", "furnace", "anvil", "prayer_altar", "cooking_range");
    /** Buttons that dismiss "experimental settings"/backup prompts while a world loads. */
    private static final List<String> PROCEED_KEYS = List.of("selectWorld.backupJoinSkipButton", "gui.proceed",
            "gui.yes", "gui.continue", "selectWorld.experimental.details.confirm");

    private record Step(String name, int waitAfter, Runnable action) {
    }

    private static final Deque<Step> STEPS = new ArrayDeque<>();
    private static final Queue<String> ERRORS = new ConcurrentLinkedQueue<>();
    private static final List<String> SHOTS = new ArrayList<>();
    private static String worldName;
    private static int ticks;
    private static int worldTicks;
    private static int wait;
    private static boolean started;
    private static boolean finished;
    private static boolean triedTitleLoad;
    private static Screen lastScreen;
    /** While set, the walkthrough waits until it returns true or {@link #untilDeadline} (wall clock) passes. */
    private static java.util.function.BooleanSupplier until;
    private static long untilDeadline;
    private static String untilName;
    private static int screenTicks;
    private static int clicks;

    private ClientSmokeTest() {
    }

    public static boolean enabled() {
        return Boolean.getBoolean(PROPERTY);
    }

    public static void install() {
        worldName = System.getProperty(PROPERTY + ".world", "smoke");
        LOG.info("[ClientSmoke] enabled: will load world '{}'", worldName);
        ClientTickEvents.END_CLIENT_TICK.register(ClientSmokeTest::tick);
    }

    private static void tick(Minecraft client) {
        if (finished) {
            return;
        }
        ticks++;
        try {
            if (ticks > TOTAL_TIMEOUT_TICKS) {
                ERRORS.add("timed out after " + ticks / 20 + "s");
                finish(client);
                return;
            }
            if (!started) {
                waitForWorld(client);
                return;
            }
            if (wait > 0) {
                wait--;
                return;
            }
            if (until != null) {
                if (until.getAsBoolean()) {
                    LOG.info("[ClientSmoke] done waiting: {}", untilName);
                    until = null;
                } else if (System.currentTimeMillis() > untilDeadline) {
                    ERRORS.add("timed out waiting: " + untilName);
                    until = null;
                } else {
                    return;
                }
            }
            Step step = STEPS.poll();
            if (step == null) {
                finish(client);
                return;
            }
            LOG.info("[ClientSmoke] step: {}", step.name());
            try {
                step.action().run();
            } catch (RuntimeException e) {
                LOG.error("[ClientSmoke] step '{}' failed", step.name(), e);
                ERRORS.add(step.name() + ": " + e);
            }
            wait = step.waitAfter();
        } catch (RuntimeException e) {
            LOG.error("[ClientSmoke] unexpected failure", e);
            ERRORS.add("unexpected: " + e);
            finish(client);
        }
    }

    // ------------------------------------------------------------------ world loading

    private static void waitForWorld(Minecraft client) {
        Screen screen = client.screen;
        if (screen != lastScreen) {
            lastScreen = screen;
            screenTicks = 0;
            LOG.info("[ClientSmoke] screen: {}", screen == null ? "none" : screen.getClass().getName());
        }
        screenTicks++;
        if (client.level != null && client.player != null && client.getSingleplayerServer() != null) {
            worldTicks++;
            if (worldTicks == 1) {
                LOG.info("[ClientSmoke] joined world after {}s", ticks / 20);
            }
            // Let the terrain screen go away and the first chunks render.
            if (worldTicks >= 200 && screen == null || worldTicks >= 600) {
                started = true;
                if (screen != null) {
                    client.setScreen(null);
                }
                buildSteps(client);
            }
            return;
        }
        if (ticks > WORLD_TIMEOUT_TICKS) {
            ERRORS.add("world '" + worldName + "' did not load within " + WORLD_TIMEOUT_TICKS / 20 + "s (screen: "
                    + (screen == null ? "none" : screen.getClass().getName()) + ")");
            finish(client);
            return;
        }
        if (screen instanceof TitleScreen && screenTicks == 60 && !triedTitleLoad) {
            triedTitleLoad = true;
            if (client.getLevelSource().levelExists(worldName)) {
                LOG.info("[ClientSmoke] loading world '{}' from the title screen", worldName);
                client.createWorldOpenFlows().openWorld(worldName, () -> client.setScreen(new TitleScreen()));
            } else {
                ERRORS.add("world '" + worldName + "' does not exist in " + client.getLevelSource().getBaseDir());
                finish(client);
            }
            return;
        }
        if (screen != null && !(screen instanceof TitleScreen) && screenTicks == 60 && clicks < 6) {
            proceed(screen);
        }
    }

    /** Presses a "proceed"-style button on a prompt shown while loading (experimental settings, backup...). */
    private static void proceed(Screen screen) {
        Button best = null;
        int bestRank = Integer.MAX_VALUE;
        for (GuiEventListener e : screen.children()) {
            if (!(e instanceof Button b) || !b.active) {
                continue;
            }
            Component msg = b.getMessage();
            int rank = Integer.MAX_VALUE;
            if (msg.getContents() instanceof TranslatableContents t) {
                int i = PROCEED_KEYS.indexOf(t.getKey());
                if (i >= 0) {
                    rank = i;
                }
            }
            String s = msg.getString().toLowerCase(Locale.ROOT);
            if (rank == Integer.MAX_VALUE && (s.contains("i know what") || s.contains("proceed") || s.contains("continue")
                    || s.equals("yes"))) {
                rank = 100;
            }
            if (rank < bestRank) {
                bestRank = rank;
                best = b;
            }
        }
        if (best != null) {
            clicks++;
            LOG.info("[ClientSmoke] pressing '{}' on {}", best.getMessage().getString(), screen.getClass().getName());
            best.onPress();
        }
    }

    // ------------------------------------------------------------------ the walkthrough

    private static void buildSteps(Minecraft client) {
        UUID uuid = client.player.getUUID();
        step("setup", 40, () -> {
            cmd(client, uuid, "gamemode creative");
            cmd(client, uuid, "gamerule doDaylightCycle false");
            cmd(client, uuid, "gamerule doWeatherCycle false");
            cmd(client, uuid, "time set noon");
            cmd(client, uuid, "weather clear");
            cmd(client, uuid, "execute in runecraft:gielinor run forceload remove all");
        });
        step("overworld screenshot", 20, () -> shot(client, "00_overworld"));

        step("/rc gielinor (Lumbridge)", 400, () -> cmd(client, uuid, "rc gielinor"));
        step("face south", 20, () -> cmd(client, uuid, "tp @s ~ ~ ~ 0 10"));
        step("Lumbridge screenshot", 20, () -> shot(client, "01_lumbridge"));
        step("summon creatures at Lumbridge", 300, () -> {
            summonRow(client, uuid, MONSTERS, 7, 3);
            summonRow(client, uuid, BOSSES.subList(0, 4), 16, 8);
            for (int i = 0; i < STATIONS.size(); i++) {
                cmd(client, uuid, "execute at @s run setblock ~" + (i * 2 - 4) + " ~ ~3 runecraft:" + STATIONS.get(i));
            }
        });
        step("Lumbridge creatures screenshot", 20, () -> shot(client, "02_lumbridge_creatures"));

        // HUD side panel (K) on every tab.
        for (SidePanel.PanelTab tab : SidePanel.PanelTab.values()) {
            step("side panel " + tab, 15, () -> {
                SidePanel.visible = true;
                SidePanel.tab = tab;
            });
            step("side panel " + tab + " screenshot", 5, () -> shot(client, "03_panel_" + tab.name().toLowerCase(Locale.ROOT)));
        }
        // Round 41 combat playability: the Protect from Magic hotkey through the real key mapping and the HUD's prayer row,
        // then a freeze (Slowness 10 in Gielinor): no walking and no FOV zoom, with the freeze icon.
        step("prayer level", 20, () -> onServer(client, uuid, p -> {
            com.runecraft.player.PlayerData d = com.runecraft.player.Players.data(p);
            d.skills().setXpTenths(com.runecraft.osrs.Skill.PRAYER, com.runecraft.osrs.Experience.xpForLevel(99) * 10);
            d.setPrayerPoints(99);
            com.runecraft.network.ServerNetworking.sync(p);
        }));
        step("Protect from Magic key", 20, () -> {
            SidePanel.visible = true;
            SidePanel.tab = SidePanel.PanelTab.PRAYER;
            KeyMapping.click(CombatKeys.protectMagic.getKey());
        });
        step("Protect from Magic on", 5, () -> {
            if (!ClientState.prayerActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC)) {
                ERRORS.add("the Protect from Magic key did not turn the prayer on (server mask " + ClientState.prayerMask + ")");
            }
            shot(client, "prayer_hud");
        });
        step("Protect from Magic off", 20, () -> KeyMapping.click(CombatKeys.protectMagic.getKey()));
        step("Protect from Magic off check", 5, () -> {
            if (ClientState.prayerActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC)) {
                ERRORS.add("pressing the Protect from Magic key again did not turn it off");
            }
        });
        float[] fovBefore = new float[1];
        step("freeze", 15, () -> {
            fovBefore[0] = client.player.getFieldOfViewModifier();
            cmd(client, uuid, "effect give @s minecraft:slowness 5 10 true");
        });
        step("walk while frozen", 4, () -> KeyMapping.set(client.options.keyUp.getKey(), true));
        step("freeze check", 5, () -> {
            float forward = client.player.input.forwardImpulse;
            float fov = client.player.getFieldOfViewModifier();
            boolean frozen = FreezeClient.frozen(client.player);
            KeyMapping.set(client.options.keyUp.getKey(), false);
            if (!frozen) {
                ERRORS.add("Slowness 10 in Gielinor is not a freeze on the client");
            }
            if (forward != 0f) {
                ERRORS.add("walked while frozen (forward impulse " + forward + ")");
            }
            if (Math.abs(fov - fovBefore[0]) > 0.01f) {
                ERRORS.add("the freeze changed the FOV modifier from " + fovBefore[0] + " to " + fov);
            }
            shot(client, "freeze_hud");
        });
        step("unfreeze", 10, () -> cmd(client, uuid, "effect clear @s minecraft:slowness"));
        step("side panel cursor screen", 15, () -> client.setScreen(new SidePanelScreen()));
        step("side panel cursor screenshot", 5, () -> shot(client, "03_panel_cursor"));
        // Full RuneScape interface (J) on every tab.
        for (RuneScapeScreen.Tab tab : RuneScapeScreen.Tab.values()) {
            step("interface " + tab, 15, () -> client.setScreen(new RuneScapeScreen(tab)));
            step("interface " + tab + " screenshot", 5, () -> shot(client, "04_interface_" + tab.name().toLowerCase(Locale.ROOT)));
        }
        step("close interface", 20, () -> client.setScreen(null));

        // Item models: RuneCraft items in the inventory, a few pages.
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals("runecraft")) {
                items.add(item);
            }
        }
        LOG.info("[ClientSmoke] {} RuneCraft items registered", items.size());
        int pages = Math.min(4, (items.size() + 35) / 36);
        for (int page = 0; page < pages; page++) {
            List<Item> slice = items.subList(page * 36, Math.min(items.size(), page * 36 + 36));
            int n = page;
            step("inventory page " + n, 20, () -> {
                onServer(client, uuid, p -> {
                    p.getInventory().clearContent();
                    for (int i = 0; i < slice.size(); i++) {
                        p.getInventory().setItem(i, new ItemStack(slice.get(i)));
                    }
                });
            });
            step("open inventory " + n, 20, () -> client.setScreen(new InventoryScreen(client.player)));
            step("inventory " + n + " screenshot", 5, () -> shot(client, "05_inventory_" + n));
            step("close inventory " + n, 5, () -> client.setScreen(null));
        }

        step("/rc tele varrock", 400, () -> cmd(client, uuid, "rc tele varrock"));
        step("Varrock creatures", 200, () -> {
            cmd(client, uuid, "tp @s ~ ~ ~ 0 10");
            summonRow(client, uuid, MONSTERS, 7, 3);
        });
        step("Varrock screenshot", 20, () -> shot(client, "06_varrock"));

        step("boss arena", 400, () -> teleportToArena(client, uuid, "king_black_dragon"));
        step("arena bosses", 300, () -> {
            cmd(client, uuid, "tp @s ~ ~ ~ 0 10");
            summonRow(client, uuid, BOSSES.subList(4, BOSSES.size()), 9, 7);
            summonRow(client, uuid, BOSSES.subList(0, 4), 20, 9);
        });
        step("arena screenshot", 20, () -> shot(client, "07_boss_arena"));
        step("arena turn around", 100, () -> cmd(client, uuid, "tp @s ~ ~ ~ 180 10"));
        step("arena back screenshot", 60, () -> shot(client, "08_boss_arena_back"));

        // The in-game self-test with this real (online) player, so minigame sessions, raid rooms and boss targeting see it.
        String filter = System.getProperty(PROPERTY + ".selftest", "minigame:,raid:,boss:,slayer:,quest:steps:,quest:walk:,skill:,enchant_,telekinetic,bake_pie,superglass,string_jewellery,plank_make");
        if (!filter.equalsIgnoreCase("none")) {
            long minutes = Long.getLong(PROPERTY + ".selftestMinutes", 14L);
            int[] before = new int[1];
            step("/rc selftest real " + filter, 20, () -> {
                before[0] = com.runecraft.selftest.SelfTest.finishedRuns();
                cmd(client, uuid, "rc selftest real " + filter);
                untilName = "self-test (SELFTEST END)";
                untilDeadline = System.currentTimeMillis() + minutes * 60_000L;
                until = () -> com.runecraft.selftest.SelfTest.finishedRuns() > before[0];
            });
            step("after self-test screenshot", 20, () -> shot(client, "09_after_selftest"));
        }
    }

    private static void step(String name, int waitAfter, Runnable action) {
        STEPS.add(new Step(name, waitAfter, action));
    }

    /** Summons creatures in a row {@code ahead} blocks south of the player, facing it, frozen in place. */
    private static void summonRow(Minecraft client, UUID uuid, List<String> ids, int ahead, int spacing) {
        int start = -(ids.size() - 1) * spacing / 2;
        for (int i = 0; i < ids.size(); i++) {
            cmd(client, uuid, "execute at @s run summon runecraft:" + ids.get(i) + " ~" + (start + i * spacing) + " ~ ~" + ahead
                    + " {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}");
        }
    }

    /** Builds a data boss's place now and moves the player into its arena (same layout as LandmarkBuilder). */
    private static void teleportToArena(Minecraft client, UUID uuid, String bossId) {
        BossDef boss = ContentRegistry.bosses().stream().filter(b -> b.id.equals(bossId)).findFirst().orElse(null);
        if (boss == null || boss.place == null) {
            ERRORS.add("no boss/place for " + bossId);
            return;
        }
        PlaceDef place = ContentRegistry.places().stream().filter(p -> p.id().equals(boss.place)).findFirst().orElse(null);
        if (place == null) {
            ERRORS.add("no place " + boss.place + " for " + bossId);
            return;
        }
        int arena = 0;
        for (BossDef b : ContentRegistry.bosses()) {
            if (b == boss) {
                break;
            }
            if (!b.raidOnly && boss.place.equals(b.place)) {
                arena++;
            }
        }
        int r = boss.arenaRadius;
        com.runecraft.osrs.world.ArenaLayout.Arena spot = com.runecraft.osrs.world.ArenaLayout.of(bossId);
        int[] fallback = com.runecraft.osrs.world.ArenaLayout.defaultPosition(place.blockX(), place.blockZ(), arena, r);
        int ax = spot != null ? spot.x() : fallback[0];
        int az = spot != null ? spot.z() : fallback[1];
        LOG.info("[ClientSmoke] {} arena at {} {} (place {})", bossId, ax, az, place.id());
        onServer(client, uuid, p -> {
            ServerLevel world = Gielinor.world(p.getServer());
            if (world == null) {
                ERRORS.add("Gielinor dimension missing");
                return;
            }
            LandmarkBuilder.buildNow(world, place);
            BlockPos pos = Gielinor.safeSpot(world, ax, az - r + 3);
            p.teleportTo(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0f, 10f);
        });
    }

    /** Runs a command as the player with operator rights on the integrated server. */
    private static void cmd(Minecraft client, UUID uuid, String command) {
        LOG.info("[ClientSmoke] /{}", command);
        onServer(client, uuid, p -> p.getServer().getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4), command));
    }

    private static void onServer(Minecraft client, UUID uuid, Consumer<ServerPlayer> action) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null) {
            ERRORS.add("integrated server is gone");
            return;
        }
        server.execute(() -> {
            try {
                ServerPlayer p = server.getPlayerList().getPlayer(uuid);
                if (p == null) {
                    ERRORS.add("player not found on the integrated server");
                    return;
                }
                action.accept(p);
            } catch (RuntimeException e) {
                LOG.error("[ClientSmoke] server-side action failed", e);
                ERRORS.add("server: " + e);
            }
        });
    }

    private static void shot(Minecraft client, String name) {
        String file = "runecraft_" + name + ".png";
        SHOTS.add(file);
        Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(),
                msg -> LOG.info("[ClientSmoke] {}", msg.getString()));
    }

    // ------------------------------------------------------------------ end

    private static void finish(Minecraft client) {
        finished = true;
        LOG.info("[ClientSmoke] {} screenshots: {}", SHOTS.size(), SHOTS);
        if (ERRORS.isEmpty()) {
            LOG.info("RUNECRAFT CLIENT SMOKE: OK");
        } else {
            for (String e : ERRORS) {
                LOG.error("[ClientSmoke] error: {}", e);
            }
            LOG.error("RUNECRAFT CLIENT SMOKE: FAILED ({} errors)", ERRORS.size());
        }
        startQuitWatchdog(client.getSingleplayerServer());
        try {
            if (client.level != null) {
                boolean singleplayer = client.isLocalServer();
                LOG.info("[ClientSmoke] leaving the world");
                client.level.disconnect();
                if (singleplayer) {
                    client.disconnect(new GenericMessageScreen(Component.translatable("menu.savingLevel")));
                } else {
                    client.disconnect();
                }
                LOG.info("[ClientSmoke] left the world");
            }
        } catch (RuntimeException e) {
            LOG.error("[ClientSmoke] disconnect failed", e);
        }
        LOG.info("[ClientSmoke] stopping the client");
        client.stop();
    }

    /**
     * If quitting hangs (e.g. the integrated server never shuts down), logs every thread's stack after
     * QUIT_TIMEOUT_MS and exits with code 3 so CI shows where it stuck instead of waiting for its timeout.
     */
    private static void startQuitWatchdog(MinecraftServer server) {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(QUIT_TIMEOUT_MS);
            } catch (InterruptedException e) {
                return;
            }
            StringBuilder dump = new StringBuilder("[ClientSmoke] quit stuck after " + QUIT_TIMEOUT_MS / 1000 + "s");
            try {
                chunkWork(server, dump);
            } catch (RuntimeException e) {
                dump.append("\n[chunks] unreadable: ").append(e);
            }
            dump.append("\nthread dump:");
            for (Map.Entry<Thread, StackTraceElement[]> e : Thread.getAllStackTraces().entrySet()) {
                dump.append("\n\"").append(e.getKey().getName()).append("\" ").append(e.getKey().getState());
                for (StackTraceElement frame : e.getValue()) {
                    dump.append("\n    at ").append(frame);
                }
            }
            LOG.error(dump.toString());
            System.err.println(dump);
            try {
                java.nio.file.Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("quit_stuck.txt"), dump);
                Thread.sleep(1000);
            } catch (java.io.IOException | InterruptedException ignored) {
                // exiting anyway
            }
            Runtime.getRuntime().halt(3);
        }, "RuneCraft smoke quit watchdog");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Chunk holders that still have a generation reference (ChunkMap.processUnloads skips them forever): their
     * status futures (S done, E failed/unloaded, P pending, - none) and the generation tasks holding them.
     */
    private static void stuckHolders(net.minecraft.server.level.ChunkMap map, StringBuilder out) {
        try {
            java.lang.reflect.Field updating = net.minecraft.server.level.ChunkMap.class.getDeclaredField("updatingChunkMap");
            updating.setAccessible(true);
            java.lang.reflect.Field futures = net.minecraft.server.level.GenerationChunkHolder.class.getDeclaredField("futures");
            futures.setAccessible(true);
            java.lang.reflect.Field task = net.minecraft.server.level.GenerationChunkHolder.class.getDeclaredField("task");
            task.setAccessible(true);
            java.lang.reflect.Field started = net.minecraft.server.level.GenerationChunkHolder.class.getDeclaredField("startedWork");
            started.setAccessible(true);
            java.lang.reflect.Field allowed = net.minecraft.server.level.GenerationChunkHolder.class.getDeclaredField("highestAllowedStatus");
            allowed.setAccessible(true);
            Map<?, ?> holders = (Map<?, ?>) updating.get(map);
            int withRef = 0;
            int withTask = 0;
            int shown = 0;
            java.util.Set<Object> tasks = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
            for (Object o : holders.values()) {
                net.minecraft.server.level.ChunkHolder h = (net.minecraft.server.level.ChunkHolder) o;
                Object t = ((java.util.concurrent.atomic.AtomicReference<?>) task.get(h)).get();
                if (t != null) {
                    withTask++;
                    tasks.add(t);
                }
                if (h.getGenerationRefCount() == 0) {
                    continue;
                }
                withRef++;
                if (shown++ >= 12) {
                    continue;
                }
                java.util.concurrent.atomic.AtomicReferenceArray<?> fs = (java.util.concurrent.atomic.AtomicReferenceArray<?>) futures.get(h);
                StringBuilder states = new StringBuilder();
                for (int i = 0; i < fs.length(); i++) {
                    Object f = fs.get(i);
                    states.append(f == null ? '-' : !((java.util.concurrent.CompletableFuture<?>) f).isDone() ? 'P'
                            : ((net.minecraft.server.level.ChunkResult<?>) ((java.util.concurrent.CompletableFuture<?>) f).getNow(null)).isSuccess() ? 'S' : 'E');
                }
                out.append("\n  holder ").append(h.getPos()).append(" ref=").append(h.getGenerationRefCount())
                        .append(" level=").append(h.getTicketLevel()).append(" persisted=").append(h.getPersistedStatus())
                        .append(" latest=").append(h.getLatestStatus()).append(" started=")
                        .append(((java.util.concurrent.atomic.AtomicReference<?>) started.get(h)).get())
                        .append(" allowed=").append(allowed.get(h)).append(" futures=").append(states)
                        .append(" task=").append(t != null);
            }
            out.append("\n  holders with a generation ref: ").append(withRef).append(", with a task: ").append(withTask);
            int n = 0;
            for (Object t : tasks) {
                if (n++ >= 10) {
                    break;
                }
                StringBuilder fields = new StringBuilder();
                for (String name : List.of("pos", "targetStatus", "scheduledStatus", "markedForCancellation", "needsGeneration")) {
                    java.lang.reflect.Field f = t.getClass().getDeclaredField(name);
                    f.setAccessible(true);
                    fields.append(' ').append(name).append('=').append(f.get(t));
                }
                java.lang.reflect.Field layer = t.getClass().getDeclaredField("scheduledLayer");
                layer.setAccessible(true);
                List<?> pending = (List<?>) layer.get(t);
                long waiting = pending.stream().filter(f -> !((java.util.concurrent.CompletableFuture<?>) f).isDone()).count();
                out.append("\n  task").append(fields).append(" layer=").append(pending.size()).append(" pending=").append(waiting);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            out.append("\n  holders: ").append(e);
        }
    }

    /**
     * What keeps MinecraftServer.stopServer draining chunks: each term of ChunkMap.hasWork() per level (read from
     * another thread without locks, so only indicative), plus the remaining tickets.
     */
    private static void chunkWork(MinecraftServer server, StringBuilder out) {
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            net.minecraft.server.level.ChunkMap map = level.getChunkSource().chunkMap;
            out.append("\n[chunks] ").append(level.dimension().location())
                    .append(" hasWork=").append(map.hasWork())
                    .append(" holders=").append(map.size())
                    .append(" light=").append(level.getChunkSource().getLightEngine().hasLightWork())
                    .append(" poi=").append(level.getPoiManager().hasWork())
                    .append(" tickets=").append(map.getDistanceManager().hasTickets())
                    .append(" (").append(map.getDistanceManager().getDebugStatus()).append(')');
            for (String name : List.of("pendingUnloads", "updatingChunkMap", "toDrop", "unloadQueue", "pendingGenerationTasks", "queueSorter")) {
                try {
                    java.lang.reflect.Field f = net.minecraft.server.level.ChunkMap.class.getDeclaredField(name);
                    f.setAccessible(true);
                    Object v = f.get(map);
                    out.append(' ').append(name).append('=').append(v instanceof Map<?, ?> m ? m.size()
                            : v instanceof java.util.Collection<?> c ? c.size() : String.valueOf(v));
                } catch (ReflectiveOperationException | RuntimeException e) {
                    out.append(' ').append(name).append("=?");
                }
            }
            if (map.hasWork()) {
                stuckHolders(map, out);
            }
            try {
                java.lang.reflect.Field f = net.minecraft.server.level.DistanceManager.class.getDeclaredField("tickets");
                f.setAccessible(true);
                Map<?, ?> tickets = (Map<?, ?>) f.get(map.getDistanceManager());
                tickets.entrySet().stream().limit(20).forEach(e -> out.append("\n  ticket ")
                        .append(new net.minecraft.world.level.ChunkPos((Long) e.getKey())).append(' ').append(e.getValue()));
            } catch (ReflectiveOperationException | RuntimeException e) {
                out.append("\n  tickets: ").append(e);
            }
        }
    }
}
