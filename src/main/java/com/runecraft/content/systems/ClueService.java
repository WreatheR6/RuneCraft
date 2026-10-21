package com.runecraft.content.systems;

import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.ClueChallenges;
import com.runecraft.osrs.content.systems.ClueNpcs;
import com.runecraft.osrs.content.systems.ClueReqs;
import com.runecraft.osrs.content.systems.ClueRewards;
import com.runecraft.osrs.content.systems.CluePuzzles;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.ClueScrolls.Action;
import com.runecraft.osrs.content.systems.ClueScrolls.Step;
import com.runecraft.osrs.content.systems.ClueScrolls.StepType;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.MimicRewards;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.Players;
import com.runecraft.registry.ModEntities;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import com.runecraft.world.Gielinor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;

/**
 * Treasure trails: reading clues, digging, searching, emotes (STASH units, Uri and double agents), talking to clue
 * NPCs (challenge scrolls, puzzle boxes, light boxes, Falo the Bard, Charlie the Tramp, Sherlock, Cecilia), kill
 * steps, hot/cold with the strange device, guardians, reward caskets (and the Mimic), scroll boxes, Watson and clue
 * drops. The steps are the real OSRS clues ({@link ClueScrolls}).
 */
public final class ClueService {
    /** Step progress flags (third slot of a tier's saved progress). */
    static final long GUARDIAN_KILLED = 1;
    static final long FIRST_EMOTE = 2;
    static final long TALKED = 4;
    static final long CHALLENGE_DONE = 8;
    static final long ANSWERED = 16;

    private static List<ClueScrolls.Npc> npcs;
    /** Live guardians: entity id -> its owner and the step it guards. */
    private static final Map<UUID, Guard> GUARDS = new HashMap<>();
    /** Marks a clue guardian, so one that outlived its entry (saved with its chunk, a server restart) goes when it loads. */
    static final String GUARDIAN_TAG = "runecraft_clue_guardian";
    /** Guardians loaded with no entry, discarded on the next tick (not while the world is adding them). */
    private static final List<Entity> STRAY_GUARDIANS = new ArrayList<>();
    /** The strange device's last reading per player (distance in steps), for "warmer/colder than last time". */
    private static final Map<UUID, Integer> LAST_HOT_COLD = new HashMap<>();
    /** Trails the self-test forces on a player (tier -> steps), used instead of the seeded trail. */
    static final Map<UUID, Map<Tier, List<Step>>> FORCED = new HashMap<>();

    /**
     * A summoned guardian. {@code mob} is the entity itself: an entity search ({@code ServerLevel.getEntity}) doesn't see a
     * mob in a chunk the server hasn't made visible yet, but the guardian can still be discarded through it.
     */
    private record Guard(UUID owner, Tier tier, int index, long spawnedAt, Entity mob) {
    }

    /** The guardian's entity while it is in the world (also in a chunk entity searches don't see yet), else null. */
    private static Entity guardEntity(ServerLevel world, UUID id, Guard g) {
        Entity found = world.getEntity(id);
        if (found != null) {
            return found;
        }
        return g.mob() != null && !g.mob().isRemoved() ? g.mob() : null;
    }

    private ClueService() {
    }

    static List<ClueScrolls.Npc> npcs() {
        if (npcs == null) {
            npcs = ClueScrolls.npcs();
        }
        return npcs;
    }

    // ------------------------------------------------------------------ trail state

    static long[] progress(SysState.SysPlayer sp, Tier tier) {
        long[] prog = sp.clues.get(tier);
        if (prog == null) {
            prog = new long[]{new Random().nextLong(), 0, 0};
            sp.clues.put(tier, prog);
        } else if (prog.length < 3) {
            prog = new long[]{prog[0], prog.length > 1 ? prog[1] : 0, 0};
            sp.clues.put(tier, prog);
        }
        return prog;
    }

    /** The trail of a tier for this player (generating it the first time the clue is read). */
    static List<Step> trail(ServerPlayer p, SysState.SysPlayer sp, Tier tier) {
        long[] prog = progress(sp, tier);
        Map<Tier, List<Step>> forced = FORCED.get(p.getUUID());
        if (forced != null && forced.containsKey(tier)) {
            return forced.get(tier);
        }
        return ClueScrolls.generate(tier, prog[0], npcs());
    }

    static Step current(ServerPlayer p, Tier tier) {
        SysState.SysPlayer sp = SysState.of(p);
        List<Step> steps = trail(p, sp, tier);
        int i = (int) progress(sp, tier)[1];
        return i < steps.size() ? steps.get(i) : null;
    }

    static boolean flag(ServerPlayer p, Tier tier, long f) {
        return (progress(SysState.of(p), tier)[2] & f) != 0;
    }

    static void setFlag(ServerPlayer p, Tier tier, long f) {
        progress(SysState.of(p), tier)[2] |= f;
    }

    /** Whether the player follows a clue of this tier (carries the scroll). */
    static boolean following(ServerPlayer p, Tier tier) {
        return Inv.has(p, tier.scrollItem(), 1);
    }

    // ------------------------------------------------------------------ reading

    public static void read(ServerPlayer p, Tier tier) {
        SysState.SysPlayer sp = SysState.of(p);
        List<Step> steps = trail(p, sp, tier);
        int index = (int) progress(sp, tier)[1];
        Step step = index < steps.size() ? steps.get(index) : null;
        SysNet.Screen s = new SysNet.Screen("clue", "Clue scroll (" + tier.id() + ")");
        if (step == null) {
            s.line("This clue has been solved.");
            s.send(p);
            return;
        }
        s.line("Step " + (index + 1) + " of " + steps.size(), 0xC0C0C0);
        s.line(step.text(), 0xF0E0A0);
        String t = tier.name();
        switch (step.action()) {
            case DIG -> {
                if (step.type() == StepType.HOT_COLD) {
                    boolean device = Inv.has(p, ClueScrolls.strangeDevice(tier), 1);
                    s.line(device ? "Feel the strange device: the hotter it gets, the closer you are."
                            : "Talk to " + (tier == Tier.MASTER ? "Jorral, at the outpost north-west of West Ardougne"
                            : "Reldo, in the Varrock Palace library") + ", for a strange device.", 0xA0A0A0);
                    s.button("feel:" + t, "Feel the device", "", device);
                } else if (step.type() == StepType.COORDINATE) {
                    s.button("sextant:" + t, "Use your sextant", "Needs a sextant (Murphy, Port Khazard), a watch (Brother Kojo,"
                            + " Clock Tower) and a chart (the Observatory professor).", true);
                }
                s.line("Dig at the spot with a spade" + guardianNote(step), 0xA0A0A0);
                s.button("dig:" + t, "Dig here");
            }
            case SEARCH -> {
                String obj = ClueScrolls.objectName(step.clue().object());
                s.line("Search the " + obj + " at the spot" + (step.keyMonster() != null ? " (it is locked: kill " + Msg.article(
                        step.keyMonster().toLowerCase(Locale.ROOT)) + " for the key)" : "") + ".", 0xA0A0A0);
                s.button("search:" + t, "Search the " + obj);
            }
            case EMOTE -> {
                List<String> emotes = step.emotes();
                s.line("Equip: " + reqText(step.req()), 0xC0C0C0);
                for (int i = 0; i < emotes.size(); i++) {
                    s.button("emote:" + t + ":" + i, "Perform " + emotes.get(i));
                }
                ClueScrolls.Stash stash = ClueScrolls.stashUnits().get(step.clue().stash());
                if (stash != null) {
                    String state = sp.stash.get(stash.name());
                    s.line("STASH unit nearby: " + (state == null ? "not built (Construction " + stash.tier().stashLevel() + ")"
                            : state.isEmpty() ? "empty" : "holds this clue's items"), 0xA0A0A0);
                    s.button("stash:" + t, state == null ? "Build the STASH unit" : state.isEmpty() ? "Fill the STASH unit"
                            : "Take the items out");
                }
            }
            case KILL -> s.line("Kill " + Msg.article(step.clue().npc().split("\\|")[0].toLowerCase(Locale.ROOT)) + ".", 0xA0A0A0);
            case TALK -> {
                if (step.type() == StepType.FALO || step.type() == StepType.CHARLIE) {
                    if (flag(p, tier, TALKED) || step.type() == StepType.FALO) {
                        s.line("Bring: " + (step.type() == StepType.FALO && !flag(p, tier, TALKED) ? "whatever his song names"
                                : reqText(step.req())), 0xA0A0A0);
                    }
                } else if (step.type() == StepType.SHERLOCK && flag(p, tier, TALKED)) {
                    s.line("Sherlock's challenge: " + step.clue().text()
                            + (flag(p, tier, CHALLENGE_DONE) ? " (done: go back to Sherlock)" : ""), 0xA0A0A0);
                } else if (step.hasChallenge() && flag(p, tier, TALKED) && !flag(p, tier, ANSWERED)) {
                    challengeScreen(p, tier, step);
                    return;
                } else {
                    s.line("Talk to the person this clue points to.", 0xA0A0A0);
                }
            }
            default -> {
            }
        }
        s.button("hint:" + t, "Hint", "Roughly where the next step is from here.", true);
        s.send(p);
    }

    private static String guardianNote(Step step) {
        return step.guardian() == null ? "." : ". Beware: " + step.guardian().display() + " guards it.";
    }

    static String reqText(String req) {
        List<String> parts = ClueReqs.describeAll(req, ClueService::itemName);
        return parts.isEmpty() ? "nothing" : String.join(", ", parts);
    }

    static String itemName(String id) {
        ItemDef d = ItemCatalog.get(id);
        return d == null ? id.replace('_', ' ') : Msg.article(d.name().toLowerCase(Locale.ROOT));
    }

    static boolean spadeExists() {
        return ItemCatalog.get("spade") != null;
    }

    // ------------------------------------------------------------------ screen actions

    static void action(ServerPlayer p, String action, Map<String, String> fields) {
        String[] parts = action.split(":");
        if (parts.length < 2) {
            return;
        }
        Tier tier;
        try {
            tier = Tier.valueOf(parts[1]);
        } catch (IllegalArgumentException e) {
            return;
        }
        if (!following(p, tier)) {
            return;
        }
        switch (parts[0]) {
            case "hint" -> hint(p, tier);
            case "feel" -> feel(p, tier);
            case "sextant" -> sextant(p);
            case "dig" -> dig(p, tier, true);
            case "search" -> search(p, tier, true);
            case "emote" -> {
                Step step = current(p, tier);
                int i = parts.length > 2 ? parseInt(parts[2]) : 0;
                if (step != null && step.type() == StepType.EMOTE && i >= 0 && i < step.emotes().size()) {
                    emote(p, tier, step.emotes().get(i));
                }
            }
            case "stash" -> stash(p, tier);
            case "answer" -> answer(p, tier, fields.getOrDefault("answer", ""));
            case "song" -> playSong(p, tier);
            default -> {
            }
        }
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static void hint(ServerPlayer p, Tier tier) {
        Step step = current(p, tier);
        if (step == null) {
            return;
        }
        if (step.type() == StepType.HOT_COLD) {
            Msg.game(p, "Hint: feel the strange device.");
            return;
        }
        int[] at = SysTeleport.osrs(p);
        int[] to = target(p, step);
        String where = Gielinor.is(p) ? hint(at[0], at[1], to[0], to[1]) : "near OSRS coordinates " + to[0] + ", " + to[1];
        String solution = step.clue().solution().isEmpty() || step.type() == StepType.COORDINATE ? "" : " " + step.clue().solution();
        Msg.game(p, "Hint: the next step is " + where + "." + (step.action() == Action.KILL ? "" : solution));
    }

    /** Where a step happens: the NPC's tile for talk steps, else the clue's tile. */
    static int[] target(ServerPlayer p, Step step) {
        if (step.action() == Action.TALK && step.npcId() != null) {
            for (ClueScrolls.Npc n : npcs()) {
                if (n.id().equals(step.npcId())) {
                    return new int[]{n.x(), n.y()};
                }
            }
        }
        int y = step.y() >= 6400 ? step.y() - 6400 : step.y();
        return new int[]{step.x(), y};
    }

    /** A rough compass hint (OSRS players use a sextant or the map; this keeps trails solvable in a block world). */
    static String hint(int x, int y, int tx, int ty) {
        int dx = tx - x;
        int dy = ty - y;
        String ns = dy > 2 ? "north" : dy < -2 ? "south" : "";
        String ew = dx > 2 ? "east" : dx < -2 ? "west" : "";
        int dist = (int) Math.sqrt((double) dx * dx + (double) dy * dy);
        return "about " + dist + " tiles " + (ns + (ns.isEmpty() || ew.isEmpty() ? "" : "-") + ew) + " of you";
    }

    /** Looking through a sextant (with a watch and a chart) tells the player their own coordinates. */
    static void sextant(ServerPlayer p) {
        if (!Inv.has(p, "sextant", 1) || !Inv.has(p, "watch", 1) || !Inv.has(p, "chart", 1)) {
            Msg.game(p, "You need a sextant, a watch and a chart to work out where you are: the Observatory professor, Murphy in"
                    + " Port Khazard and Brother Kojo at the Clock Tower hand them to treasure hunters.");
            return;
        }
        int[] at = SysTeleport.osrs(p);
        Msg.game(p, "The sextant shows: " + ClueScrolls.coordinateText(at[0], at[1]) + ".");
    }

    // ------------------------------------------------------------------ dig and search

    /** Tries to dig for a clue of the tier at the player's position. Returns true if the dig was used by a clue. */
    public static boolean dig(ServerPlayer p, Tier tier, boolean loud) {
        if (!following(p, tier)) {
            return false;
        }
        Step step = current(p, tier);
        if (step == null || step.action() != Action.DIG) {
            return false;
        }
        if (spadeExists() && !Inv.has(p, "spade", 1)) {
            if (loud) {
                Msg.game(p, "You need a spade to dig.");
            }
            return false;
        }
        int[] at = SysTeleport.osrs(p);
        int[] to = target(p, step);
        if (!Gielinor.is(p) || !Teleports.within(at[0], at[1], to[0], to[1], ClueScrolls.DIG_RADIUS)) {
            if (loud) {
                Msg.game(p, "You dig a hole... Nothing interesting happens.");
            }
            return false;
        }
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY(), p.getZ(), 8, 0.3, 0.1, 0.3, 0.01);
        p.level().playSound(null, p.blockPosition(), SoundEvents.SHOVEL_FLATTEN, SoundSource.PLAYERS, 1f, 1f);
        if (needsGuardian(p, tier, step)) {
            return true;
        }
        Msg.action(p, "You dig a hole...");
        advance(p, tier);
        return true;
    }

    /** Searching a crate, drawers, chest or bookcase of a search step (the key steps need the monster's key). */
    public static boolean search(ServerPlayer p, Tier tier, boolean loud) {
        Step step = current(p, tier);
        if (step == null || step.action() != Action.SEARCH) {
            return false;
        }
        int[] at = SysTeleport.osrs(p);
        int[] to = target(p, step);
        String obj = ClueScrolls.objectName(step.clue().object());
        if (!Gielinor.is(p) || !Teleports.within(at[0], at[1], to[0], to[1], ClueScrolls.DIG_RADIUS)) {
            if (loud) {
                Msg.game(p, "You search around... but find nothing.");
            }
            return false;
        }
        if (step.keyMonster() != null) {
            String key = keyItem(tier);
            if (!Inv.has(p, key, 1)) {
                Msg.game(p, "The " + obj + " is locked. Perhaps " + Msg.article(step.keyMonster().toLowerCase(Locale.ROOT))
                        + " has the key.");
                return true;
            }
            Inv.remove(p, key, 1);
            Msg.action(p, "You unlock the " + obj + " with the key.");
        }
        Msg.action(p, "You search the " + obj + "...");
        advance(p, tier);
        return true;
    }

    static String keyItem(Tier tier) {
        return tier == Tier.ELITE || tier == Tier.MASTER ? "key_elite" : "key_medium";
    }

    // ------------------------------------------------------------------ guardians

    /**
     * Hard and harder coordinate digs, master hot/cold digs and "Beware of double agents!" emotes summon their guardian
     * the first time; the step only completes once it is dead. Returns true when the player has to fight first.
     */
    static boolean needsGuardian(ServerPlayer p, Tier tier, Step step) {
        if (step.guardian() == null || flag(p, tier, GUARDIAN_KILLED)) {
            return false;
        }
        int index = (int) progress(SysState.of(p), tier)[1];
        for (Map.Entry<UUID, Guard> e : GUARDS.entrySet()) {
            Guard g = e.getValue();
            if (g.owner().equals(p.getUUID()) && g.tier() == tier && g.index() == index) {
                Msg.red(p, "Defeat the " + step.guardian().display() + " first!");
                return true;
            }
        }
        List<String> ids = step.guardian().spawn(new Random(p.getRandom().nextLong()));
        ServerLevel world = (ServerLevel) p.level();
        int spawned = 0;
        for (int i = 0; i < ids.size(); i++) {
            EntityType<?> type = ModEntities.typeById(ids.get(i));
            if (type == null) {
                continue;
            }
            Entity e = type.create(world);
            if (!(e instanceof Mob mob)) {
                continue;
            }
            double angle = i * 2.1 + p.getRandom().nextDouble();
            mob.moveTo(p.getX() + Math.cos(angle) * 2.5, p.getY(), p.getZ() + Math.sin(angle) * 2.5, p.getRandom().nextFloat() * 360f, 0);
            mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            mob.setTarget(p);
            mob.addTag(GUARDIAN_TAG);
            // Tracked before it joins the world, so the load hook doesn't take it for a stray.
            GUARDS.put(mob.getUUID(), new Guard(p.getUUID(), tier, index, SysTicks.now(), mob));
            if (!world.addFreshEntity(mob)) {
                GUARDS.remove(mob.getUUID());
                continue;
            }
            spawned++;
        }
        if (spawned == 0) {
            setFlag(p, tier, GUARDIAN_KILLED); // no such monster in this world: nothing to fight
            return false;
        }
        Msg.red(p, step.guardian().doubleAgent() ? "A double agent appears!" : "You disturb " + Msg.article(step.guardian().display())
                + "! Defeat " + (spawned > 1 ? "them" : "it") + ", then try again.");
        return true;
    }

    /** Whether the player still has a guardian alive for this tier's current step. */
    static boolean guardianAlive(ServerPlayer p, Tier tier) {
        for (Guard g : GUARDS.values()) {
            if (g.owner().equals(p.getUUID()) && g.tier() == tier) {
                return true;
            }
        }
        return false;
    }

    /** Discards the guardians the load hook found with no entry. */
    private static void discardStrayGuardians() {
        List<Entity> strays;
        synchronized (STRAY_GUARDIANS) {
            if (STRAY_GUARDIANS.isEmpty()) {
                return;
            }
            strays = new ArrayList<>(STRAY_GUARDIANS);
            STRAY_GUARDIANS.clear();
        }
        for (Entity e : strays) {
            if (!e.isRemoved() && !GUARDS.containsKey(e.getUUID())) {
                e.discard();
            }
        }
    }

    /** Despawns guardians whose owner left, logged out or moved on (called every second). */
    static void guardianTick(net.minecraft.server.MinecraftServer server) {
        ServerLevel world = Gielinor.world(server);
        if (world == null || GUARDS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Guard>> it = GUARDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Guard> e = it.next();
            Entity mob = guardEntity(world, e.getKey(), e.getValue());
            ServerPlayer owner = server.getPlayerList().getPlayer(e.getValue().owner());
            boolean gone = mob == null || !mob.isAlive();
            boolean left = owner == null || owner.level() != world || mob == null || owner.distanceToSqr(mob) > 48 * 48
                    || SysTicks.now() - e.getValue().spawnedAt() > 1000;
            if (gone || left) {
                if (!gone && mob != null) {
                    mob.discard();
                }
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ emotes and STASH units

    /** Performs an emote for the current emote step: the right place, the emotes in order, the items worn. */
    static void emote(ServerPlayer p, Tier tier, String emote) {
        Step step = current(p, tier);
        if (step == null || step.type() != StepType.EMOTE) {
            return;
        }
        Msg.action(p, "You perform the " + emote + " emote.");
        int[] at = SysTeleport.osrs(p);
        if (!Gielinor.is(p) || !Teleports.within(at[0], at[1], step.x(), step.y(), ClueScrolls.EMOTE_RADIUS)) {
            Msg.game(p, "Nothing happens.");
            return;
        }
        List<String> emotes = step.emotes();
        if (emotes.size() > 1) {
            if (emote.equals(emotes.get(0))) {
                setFlag(p, tier, FIRST_EMOTE);
                if (!emote.equals(emotes.get(1))) {
                    return;
                }
            } else if (!emote.equals(emotes.get(1)) || !flag(p, tier, FIRST_EMOTE)) {
                Msg.game(p, "Nothing happens.");
                return;
            }
        } else if (!emotes.isEmpty() && !emote.equals(emotes.get(0))) {
            Msg.game(p, "Nothing happens.");
            return;
        }
        List<String> missing = ClueReqs.missing(step.req(), worn(p), ClueService::itemName);
        if (!missing.isEmpty()) {
            say(p, "Uri", "Hmm... I am not sure you are dressed for the occasion. You need " + String.join(", ", missing) + ".");
            return;
        }
        if (needsGuardian(p, tier, step)) {
            return;
        }
        say(p, "Uri", uriLine(p));
        advance(p, tier);
    }

    private static final String[] URI_LINES = {
            "Once, I was a poor man, but then I found a party hat.",
            "There were three goblins in a bar, which one left first?",
            "Would you like to buy a pewter spoon?",
            "In the end, only the three-legged survive.",
            "I heard that the tall man fears only strong winds.",
            "In Canifis the men are known for eating much spam.",
            "I am the egg man, are you one of the egg men?",
            "The sudden appearance of a deceased greyhound portends a wealth of hot pies."};

    private static String uriLine(ServerPlayer p) {
        return URI_LINES[p.getRandom().nextInt(URI_LINES.length)] + " Here is your next clue.";
    }

    /** What the player wears, for emote steps (the OSRS rule: equipped, not carried). */
    static ClueReqs.Has worn(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        return new ClueReqs.Has() {
            @Override
            public boolean has(String item, int qty) {
                return equipped(p, sp, item);
            }

            @Override
            public boolean slotEmpty(String slot) {
                return ClueService.slotEmpty(p, sp, slot);
            }
        };
    }

    /** What the player carries or wears (Falo the Bard, Charlie the Tramp). */
    static ClueReqs.Has carried(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        return new ClueReqs.Has() {
            @Override
            public boolean has(String item, int qty) {
                return Inv.has(p, item, qty) || equipped(p, sp, item);
            }

            @Override
            public boolean slotEmpty(String slot) {
                return ClueService.slotEmpty(p, sp, slot);
            }
        };
    }

    /**
     * Whether the player wears {@code id}: armour, weapon or shield, this module's jewellery / cape / gloves / ammo slots, or
     * another module's worn slots (the quests module's capes and gloves, such as the Cape of Legends an elite emote clue asks
     * for).
     */
    static boolean equipped(ServerPlayer p, SysState.SysPlayer sp, String id) {
        if (sp.wearing(id)) {
            return true;
        }
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            if (Inv.idOf(p.getItemBySlot(slot)).equals(id)) {
                return true;
            }
        }
        return com.runecraft.combat.HitCalcBridge.extraWornIds.apply(p).contains(id);
    }

    static boolean slotEmpty(ServerPlayer p, SysState.SysPlayer sp, String slot) {
        return switch (slot) {
            case "head" -> p.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
            case "body" -> p.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
            case "legs" -> p.getItemBySlot(EquipmentSlot.LEGS).isEmpty();
            case "boots" -> p.getItemBySlot(EquipmentSlot.FEET).isEmpty();
            case "weapon" -> p.getMainHandItem().isEmpty() || ItemCatalog.get(Inv.idOf(p.getMainHandItem())) == null
                    || ItemCatalog.get(Inv.idOf(p.getMainHandItem())).equipment() == null;
            case "shield" -> p.getOffhandItem().isEmpty();
            case "cape" -> sp.worn(Jewellery.Slot.CAPE).isEmpty();
            case "amulet" -> sp.worn(Jewellery.Slot.NECK).isEmpty();
            case "gloves" -> sp.worn(Jewellery.Slot.HANDS).isEmpty();
            case "ring" -> sp.worn(Jewellery.Slot.RING).isEmpty();
            case "ammo" -> sp.worn(Jewellery.Slot.AMMO).isEmpty();
            default -> true;
        };
    }

    /**
     * STASH units: build one (Construction level, two planks of the tier, ten nails of one metal other than dragon, a hammer
     * and a saw; master units also a gold leaf), fill it with the clue's items (the first listed option of each), or take
     * them back out.
     */
    static void stash(ServerPlayer p, Tier tier) {
        Step step = current(p, tier);
        if (step == null || step.type() != StepType.EMOTE) {
            return;
        }
        ClueScrolls.Stash stash = ClueScrolls.stashUnits().get(step.clue().stash());
        if (stash == null) {
            return;
        }
        int[] at = SysTeleport.osrs(p);
        if (!Gielinor.is(p) || !Teleports.within(at[0], at[1], stash.x(), stash.y(), ClueScrolls.EMOTE_RADIUS)) {
            Msg.game(p, "The STASH unit is near the emote spot (" + hint(at[0], at[1], stash.x(), stash.y()) + ").");
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
        String state = sp.stash.get(stash.name());
        if (state == null) {
            buildStash(p, sp, stash);
        } else if (state.isEmpty()) {
            List<String> taken = new ArrayList<>();
            for (List<List<ClueReqs.Token>> group : ClueReqs.groups(step.req())) {
                boolean done = false;
                for (List<ClueReqs.Token> alt : group) {
                    boolean all = true;
                    for (ClueReqs.Token t : alt) {
                        if (!t.isSlotRule() && !Inv.has(p, t.item(), 1)) {
                            all = false;
                        }
                    }
                    if (all) {
                        for (ClueReqs.Token t : alt) {
                            if (!t.isSlotRule()) {
                                Inv.remove(p, t.item(), 1);
                                taken.add(t.item());
                            }
                        }
                        done = true;
                        break;
                    }
                }
                if (!done) {
                    for (String id : taken) {
                        Inv.give(p, id, 1);
                    }
                    Msg.game(p, "You need all of this clue's items in your inventory to fill the STASH unit.");
                    return;
                }
            }
            sp.stash.put(stash.name(), String.join(",", taken));
            Msg.action(p, "You place the items in the STASH unit.");
        } else {
            for (String id : state.split(",")) {
                if (!id.isEmpty()) {
                    Inv.give(p, id, 1);
                }
            }
            sp.stash.put(stash.name(), "");
            Msg.action(p, "You take the items out of the STASH unit.");
        }
        read(p, tier);
    }

    private static void buildStash(ServerPlayer p, SysState.SysPlayer sp, ClueScrolls.Stash stash) {
        Tier t = stash.tier();
        if (Players.data(p).skills().level(Skill.CONSTRUCTION) < t.stashLevel()) {
            Msg.game(p, "You need a Construction level of " + t.stashLevel() + " to build this STASH unit.");
            return;
        }
        String nails = null;
        // OSRS Wiki "STASH": any ten nails of one metal, but dragon nails do not work.
        for (String metal : new String[]{"bronze", "iron", "steel", "black", "mithril", "adamant", "rune"}) {
            if (Inv.has(p, metal + "_nails", 10)) {
                nails = metal + "_nails";
                break;
            }
        }
        boolean leaf = t != Tier.MASTER || Inv.has(p, "gold_leaf", 1);
        boolean sawExists = ItemCatalog.get("saw") != null;
        boolean saw = !sawExists || Inv.has(p, "saw", 1) || Inv.has(p, "amys_saw", 1);
        if (!Inv.has(p, t.stashPlank(), 2) || nails == null || !leaf || !Inv.has(p, "hammer", 1) || !saw) {
            ItemDef plank = ItemCatalog.get(t.stashPlank());
            Msg.game(p, "You need 2 " + (plank == null ? t.stashPlank() : plank.name().toLowerCase(Locale.ROOT))
                    + "s, 10 nails of one metal (not dragon)" + (t == Tier.MASTER ? ", a gold leaf" : "") + ", a hammer"
                    + (sawExists ? " and a saw" : "") + " to build this STASH unit.");
            return;
        }
        Inv.remove(p, t.stashPlank(), 2);
        Inv.remove(p, nails, 10);
        if (t == Tier.MASTER) {
            Inv.remove(p, "gold_leaf", 1);
        }
        XpService.add(p, Skill.CONSTRUCTION, t.stashXp());
        sp.stash.put(stash.name(), "");
        Msg.action(p, "You build a STASH unit.");
    }

    // ------------------------------------------------------------------ talking

    /** Talking to an NPC: completes talk steps. Returns true if the interaction was used by a clue. */
    public static boolean talk(ServerPlayer p, QuestNpcEntity npc) {
        if (coordinateTool(p, npc)) {
            return true;
        }
        for (Tier tier : Tier.values()) {
            if (!following(p, tier)) {
                continue;
            }
            Step step = current(p, tier);
            if (step == null) {
                continue;
            }
            if (step.type() == StepType.HOT_COLD && hotColdGiver(tier, npc)) {
                String device = ClueScrolls.strangeDevice(tier);
                if (!Inv.has(p, device, 1)) {
                    Inv.give(p, device, 1);
                    say(p, npc.getName().getString(), "Ah, a treasure hunter! Take this strange device: it gets warmer the closer you get"
                            + " to the treasure.");
                } else {
                    say(p, npc.getName().getString(), "You already have the strange device. Feel it to find the treasure.");
                }
                return true;
            }
            if (step.action() != Action.TALK || !npc.npcId().equals(step.npcId())) {
                continue;
            }
            String speaker = step.npcName() == null ? npc.getName().getString() : step.npcName();
            if (talkStep(p, tier, step, speaker)) {
                advance(p, tier);
            }
            return true;
        }
        return false;
    }

    /** Unlocks (in {@code SysPlayer.unlocks}) recording who has handed the coordinate tools over. */
    static final String CHART_FROM_PROFESSOR = "clue_chart";
    static final String SEXTANT_FROM_MURPHY = "clue_sextant";

    /**
     * The coordinate clue tools (OSRS Wiki "Sextant", "Watch", "Chart"): the Observatory professor gives a chart, then Murphy
     * in Port Khazard a sextant, then Brother Kojo at the Clock Tower a watch, each to a player who lacks one. Only treasure
     * hunters (carrying a clue scroll) are offered them, so everyone else gets the NPC's own dialogue; a hunter who already
     * has the tool does too. Returns true when the conversation was this.
     */
    static boolean coordinateTool(ServerPlayer p, QuestNpcEntity npc) {
        boolean hunter = false;
        boolean coordinate = false;
        for (Tier t : Tier.values()) {
            if (following(p, t)) {
                hunter = true;
                Step step = current(p, t);
                coordinate |= step != null && step.type() == StepType.COORDINATE;
                if (step != null && step.action() == Action.TALK && npc.npcId().equals(step.npcId())) {
                    return false; // the NPC is this clue's step: the step goes first
                }
            }
        }
        if (!hunter) {
            return false;
        }
        SysState.SysPlayer sp = SysState.of(p);
        String name = npc.getName().getString();
        String tool;
        String needs;
        String line;
        String hint;
        switch (name.toLowerCase(Locale.ROOT)) {
            case "observatory professor" -> {
                tool = "chart";
                needs = null;
                line = "A treasure hunter? Take this star chart. With a sextant and a watch it tells you exactly where you are."
                        + " Murphy, down at the Port Khazard docks, has a sextant.";
                hint = null;
            }
            case "murphy" -> {
                tool = "sextant";
                needs = CHART_FROM_PROFESSOR;
                line = "The professor sent you, did he? Here's a sextant: line the sun up with the horizon and it gives your"
                        + " position. You'll want a watch to go with it: try Brother Kojo at the Clock Tower.";
                hint = "A sextant? Speak to the professor at the Observatory first; he'll explain how it all works.";
            }
            case "brother kojo" -> {
                tool = "watch";
                needs = SEXTANT_FROM_MURPHY;
                line = "Here, take this watch. With your sextant and a chart you'll be able to find any spot in Gielinor.";
                hint = "A watch? Murphy in Port Khazard will give you a sextant first; come back when you have one.";
            }
            default -> {
                return false;
            }
        }
        if (Inv.has(p, tool, 1) || ItemCatalog.get(tool) == null) {
            return false;
        }
        if (needs != null && !sp.unlocks.contains(needs) && !Inv.has(p, tool.equals("watch") ? "sextant" : "chart", 1)) {
            if (!coordinate) {
                return false;
            }
            say(p, name, hint);
            return true;
        }
        Inv.give(p, tool, 1);
        sp.unlocks.add(tool.equals("chart") ? CHART_FROM_PROFESSOR : tool.equals("sextant") ? SEXTANT_FROM_MURPHY : "clue_watch");
        say(p, name, line);
        return true;
    }

    private static boolean hotColdGiver(Tier tier, QuestNpcEntity npc) {
        String name = npc.getName().getString();
        return tier == Tier.MASTER ? name.equalsIgnoreCase("Jorral") : name.equalsIgnoreCase("Reldo");
    }

    /** One conversation with a talk step's NPC. Returns true when the step is done. */
    static boolean talkStep(ServerPlayer p, Tier tier, Step step, String speaker) {
        switch (step.type()) {
            case FALO -> {
                if (!ClueReqs.missing(step.req(), carried(p), ClueService::itemName).isEmpty()) {
                    setFlag(p, tier, TALKED);
                    say(p, speaker, "Here goes... " + step.clue().text());
                    say(p, speaker, "Come back when you have what my song names.");
                    return false;
                }
                say(p, speaker, "Ah, that's it exactly! The song is complete. Here is your next clue.");
                return true;
            }
            case CHARLIE -> {
                if (!flag(p, tier, TALKED)) {
                    setFlag(p, tier, TALKED);
                    say(p, speaker, step.clue().question());
                    return false;
                }
                ClueReqs.Token item = ClueReqs.firstChoice(step.req()).isEmpty() ? null : ClueReqs.firstChoice(step.req()).get(0);
                if (item == null || !Inv.has(p, item.item(), 1)) {
                    say(p, speaker, step.clue().question());
                    return false;
                }
                Inv.remove(p, item.item(), 1);
                say(p, speaker, "Thank you! Here's what you've been looking for.");
                return true;
            }
            case SHERLOCK -> {
                ClueChallenges.Challenge c = ClueChallenges.of(step.clue().text());
                if (!flag(p, tier, TALKED)) {
                    setFlag(p, tier, TALKED);
                    say(p, speaker, "Elementary! Here is your challenge: " + step.clue().text().toLowerCase(Locale.ROOT)
                            + " Come back to me when you have done it.");
                    return false;
                }
                if (c != null && c.kind() == ClueChallenges.Kind.EQUIP) {
                    for (String id : c.targets()) {
                        if (equipped(p, SysState.of(p), id)) {
                            setFlag(p, tier, CHALLENGE_DONE);
                        }
                    }
                }
                if (c != null && c.kind() == ClueChallenges.Kind.PRAYER && Players.data(p).isActive(Prayer.valueOf(c.targets().get(0)))) {
                    setFlag(p, tier, CHALLENGE_DONE);
                }
                if (!flag(p, tier, CHALLENGE_DONE)) {
                    say(p, speaker, "You haven't done my challenge yet: " + step.clue().text().toLowerCase(Locale.ROOT));
                    return false;
                }
                say(p, speaker, "Splendid work! Here is your next clue.");
                return true;
            }
            case MUSIC -> {
                SysNet.Screen s = new SysNet.Screen("clue", "Cecilia");
                s.line("Cecilia: Would you play \"" + step.clue().text() + "\" for me?", 0xF0E0A0);
                s.button("song:" + tier.name(), "Play \"" + step.clue().text() + "\"");
                s.send(p);
                return false;
            }
            default -> {
                if (step.box() != null) {
                    return puzzleTalk(p, tier, step, speaker);
                }
                if (step.hasChallenge() && !flag(p, tier, ANSWERED)) {
                    setFlag(p, tier, TALKED);
                    say(p, speaker, "Ah, you've solved my riddle. Answer my challenge and I'll give you your next clue.");
                    challengeScreen(p, tier, step);
                    return false;
                }
                say(p, speaker, "Ah, you've solved my riddle. Well done, here's your reward.");
                return true;
            }
        }
    }

    /** A challenge scroll: the NPC's question with an answer field. */
    static void challengeScreen(ServerPlayer p, Tier tier, Step step) {
        SysNet.Screen s = new SysNet.Screen("clue", "Challenge scroll");
        s.line(step.npcName() == null ? "Challenge:" : step.npcName() + " asks:", 0xC0C0C0);
        s.line(step.clue().question(), 0xF0E0A0);
        s.field("answer", "Answer", "");
        s.button("answer:" + tier.name(), "Answer");
        s.send(p);
    }

    /** How close (in tiles) the player has to stand to the NPC to give a challenge scroll's answer. */
    static final int CHALLENGE_RADIUS = 10;

    /**
     * Answering a challenge scroll. OSRS takes the number; separators and spaces are ignored. The challenge only exists once
     * the NPC has set it, and the answer is given to them in person.
     */
    static void answer(ServerPlayer p, Tier tier, String given) {
        Step step = current(p, tier);
        if (step == null || !step.hasChallenge() || !flag(p, tier, TALKED)) {
            return;
        }
        if (!nearStepNpc(p, step, CHALLENGE_RADIUS)) {
            Msg.game(p, "You need to give your answer to " + (step.npcName() == null ? "the person who set the challenge"
                    : step.npcName()) + " in person.");
            return;
        }
        if (normaliseAnswer(given).equals(normaliseAnswer(step.clue().answer()))) {
            setFlag(p, tier, ANSWERED);
            say(p, step.npcName() == null ? "Stranger" : step.npcName(), "Correct! Here is your next clue.");
            advance(p, tier);
        } else {
            Msg.game(p, "That isn't the right answer.");
        }
    }

    /** Whether the player stands by a talk step's NPC: its spawn tile, or wherever the NPC has wandered to. */
    static boolean nearStepNpc(ServerPlayer p, Step step, int tiles) {
        if (!Gielinor.is(p)) {
            return false;
        }
        int[] at = SysTeleport.osrs(p);
        int[] to = target(p, step);
        if (Teleports.within(at[0], at[1], to[0], to[1], tiles)) {
            return true;
        }
        String id = step.npcId();
        return id != null && !p.level().getEntitiesOfClass(QuestNpcEntity.class,
                p.getBoundingBox().inflate(tiles * com.runecraft.osrs.GielinorMap.SCALE), e -> e.isAlive() && id.equals(e.npcId())).isEmpty();
    }

    static String normaliseAnswer(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[\\s,]", "");
    }

    static void playSong(ServerPlayer p, Tier tier) {
        Step step = current(p, tier);
        if (step == null || step.type() != StepType.MUSIC) {
            return;
        }
        int[] at = SysTeleport.osrs(p);
        int[] to = target(p, step);
        if (!Gielinor.is(p) || !Teleports.within(at[0], at[1], to[0], to[1], 8)) {
            Msg.game(p, "Cecilia can't hear you from here.");
            return;
        }
        Msg.action(p, "You play \"" + step.clue().text() + "\" for Cecilia.");
        say(p, "Cecilia", "Lovely! That's just the song I wanted to hear. Here, take this.");
        advance(p, tier);
    }

    // ------------------------------------------------------------------ puzzle boxes and light boxes

    /** A puzzle handed over by a clue NPC, saved with the player's data until the step is done. */
    private static final class Puzzle {
        final Tier tier;
        final CluePuzzles.Slider slider;
        final CluePuzzles.LightBox lights;

        Puzzle(Tier tier, CluePuzzles.Slider slider, CluePuzzles.LightBox lights) {
            this.tier = tier;
            this.slider = slider;
            this.lights = lights;
        }

        boolean solved() {
            return slider != null ? slider.solved() : lights.solved();
        }

        String encode() {
            return tier.name() + "|" + (slider != null ? "S|" + slider.encode() : "L|" + lights.encode());
        }

        static Puzzle decode(String s) {
            String[] parts = s == null ? new String[0] : s.split("\\|", 3);
            if (parts.length != 3) {
                return null;
            }
            try {
                Tier tier = Tier.valueOf(parts[0]);
                if (parts[1].equals("S")) {
                    CluePuzzles.Slider slider = CluePuzzles.Slider.decode(parts[2]);
                    return slider == null ? null : new Puzzle(tier, slider, null);
                }
                CluePuzzles.LightBox lights = parts[1].equals("L") ? CluePuzzles.LightBox.decode(parts[2]) : null;
                return lights == null ? null : new Puzzle(tier, null, lights);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    private static Puzzle loadPuzzle(ServerPlayer p) {
        return Puzzle.decode(SysState.of(p).cluePuzzle);
    }

    private static void savePuzzle(ServerPlayer p, Puzzle puzzle) {
        SysState.of(p).cluePuzzle = puzzle == null ? "" : puzzle.encode();
    }

    static void registerPuzzles() {
        for (String id : new String[]{"grid3_puzzle", "grid5_puzzle", "grid5_lights"}) {
            SysNet.onAction(id, ClueService::puzzleAction);
        }
    }

    /** Talking to the NPC of a puzzle step: hands over the box, or takes it back solved. Returns true to advance. */
    private static boolean puzzleTalk(ServerPlayer p, Tier tier, Step step, String speaker) {
        String box = step.box();
        Puzzle puzzle = loadPuzzle(p);
        if (puzzle != null && puzzle.tier == tier && puzzle.solved() && Inv.has(p, box, 1)) {
            Inv.remove(p, box, 1);
            savePuzzle(p, null);
            say(p, speaker, "You've solved it! Well done, here's your next clue.");
            return true;
        }
        if (!Inv.has(p, box, 1)) {
            Inv.give(p, box, 1);
            say(p, speaker, "Solve this " + (box.equals(CluePuzzles.LIGHT_BOX) ? "light box" : "puzzle box")
                    + " and bring it back to me.");
        } else {
            say(p, speaker, "You haven't solved that yet.");
        }
        openPuzzle(p, puzzleFor(p, tier, box));
        return false;
    }

    private static Puzzle puzzleFor(ServerPlayer p, Tier tier, String box) {
        Puzzle puzzle = loadPuzzle(p);
        boolean light = box.equals(CluePuzzles.LIGHT_BOX);
        if (puzzle == null || puzzle.tier != tier || (puzzle.lights != null) != light
                || (puzzle.slider != null && puzzle.slider.size() != CluePuzzles.puzzleSize(tier))) {
            Random r = new Random(p.getRandom().nextLong());
            puzzle = light ? new Puzzle(tier, null, CluePuzzles.LightBox.scrambled(r))
                    : new Puzzle(tier, CluePuzzles.Slider.scrambled(CluePuzzles.puzzleSize(tier), r), null);
            savePuzzle(p, puzzle);
        }
        return puzzle;
    }

    /** Using a puzzle box or light box item: opens the puzzle of the clue step that handed it over. */
    public static boolean useBox(ServerPlayer p, String box) {
        for (Tier tier : Tier.values()) {
            if (!following(p, tier)) {
                continue;
            }
            Step step = current(p, tier);
            if (step != null && box.equals(step.box())) {
                openPuzzle(p, puzzleFor(p, tier, box));
                return true;
            }
        }
        Msg.game(p, "This box isn't part of any clue you're following.");
        return true;
    }

    private static void openPuzzle(ServerPlayer p, Puzzle puzzle) {
        if (puzzle.slider != null) {
            int n = puzzle.slider.size();
            SysNet.Screen s = new SysNet.Screen("grid" + n + "_puzzle", "Puzzle box");
            s.line(puzzle.solved() ? "Solved! Take the box back to the person who gave it to you."
                    : "Slide the tiles into order (1 to " + (n * n - 1) + ", gap last): click a tile next to the gap.",
                    puzzle.solved() ? 0x80FF80 : 0xF0E0A0);
            for (int i = 0; i < n * n; i++) {
                int t = puzzle.slider.tile(i);
                s.button("t:" + i, t == 0 ? " " : Integer.toString(t), "", t != 0 && !puzzle.solved());
            }
            s.send(p);
            return;
        }
        SysNet.Screen s = new SysNet.Screen("grid5_lights", "Light box");
        s.line(puzzle.solved() ? "Every light is on! Take the box back to the person who gave it to you."
                : "Each button toggles its own set of lights. Turn all " + CluePuzzles.LIGHTS + " lights on ("
                + puzzle.lights.litCount() + " lit).", puzzle.solved() ? 0x80FF80 : 0xF0E0A0);
        for (int i = 0; i < CluePuzzles.LIGHTS; i++) {
            s.button("l:" + i, puzzle.lights.lit(i) ? "■" : "□", "", false);
        }
        for (int b = 0; b < CluePuzzles.BUTTONS; b++) {
            s.button("b:" + b, String.valueOf((char) ('A' + b)), "", !puzzle.solved());
        }
        s.send(p);
    }

    static void puzzleAction(ServerPlayer p, String action, Map<String, String> fields) {
        Puzzle puzzle = loadPuzzle(p);
        if (puzzle == null || action.length() < 3) {
            return;
        }
        int index = parseInt(action.substring(2));
        if (index < 0) {
            return;
        }
        if (action.startsWith("t:") && puzzle.slider != null) {
            puzzle.slider.move(index);
        } else if (action.startsWith("b:") && puzzle.lights != null) {
            puzzle.lights.press(index);
        }
        savePuzzle(p, puzzle);
        if (puzzle.solved()) {
            Msg.action(p, "You solve the " + (puzzle.slider != null ? "puzzle box" : "light box") + "!");
        }
        openPuzzle(p, puzzle);
    }

    /** Solves the player's current puzzle box or light box (the self-test's shortcut through the sliding tiles). */
    static boolean solvePuzzleForTest(ServerPlayer p) {
        Puzzle puzzle = loadPuzzle(p);
        if (puzzle == null) {
            return false;
        }
        if (puzzle.slider != null) {
            savePuzzle(p, new Puzzle(puzzle.tier, CluePuzzles.Slider.solvedBoard(puzzle.slider.size()), null));
            return true;
        }
        for (int mask = 0; mask < 1 << CluePuzzles.BUTTONS; mask++) {
            CluePuzzles.LightBox copy = CluePuzzles.LightBox.decode(puzzle.lights.encode());
            for (int b = 0; b < CluePuzzles.BUTTONS; b++) {
                if ((mask >> b & 1) == 1) {
                    copy.press(b);
                }
            }
            if (copy.solved()) {
                for (int b = 0; b < CluePuzzles.BUTTONS; b++) {
                    if ((mask >> b & 1) == 1) {
                        puzzleAction(p, "b:" + b, Map.of());
                    }
                }
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ hot/cold

    /** Feeling the strange device: the wiki's temperature for the distance in steps, and the change since last time. */
    static void feel(ServerPlayer p, Tier tier) {
        Step step = current(p, tier);
        if (step == null || step.type() != StepType.HOT_COLD) {
            return;
        }
        if (!Inv.has(p, ClueScrolls.strangeDevice(tier), 1)) {
            Msg.game(p, "You need the strange device.");
            return;
        }
        int[] at = SysTeleport.osrs(p);
        int dist = Gielinor.is(p) ? ClueScrolls.steps(at[0], at[1], step.x(), step.y()) : 9999;
        Integer last = LAST_HOT_COLD.put(p.getUUID(), dist);
        String change = last == null || last == dist ? "" : dist < last ? ", and warmer than last time" : ", but colder than last time";
        Msg.game(p, "The device is " + ClueScrolls.temperature(tier, dist) + change + ".");
        if (tier == Tier.MASTER) {
            // OSRS: Jorral's device deals 3-8 damage each time it is felt.
            com.runecraft.combat.CombatHandler.applyDamage(null, p, 3 + p.getRandom().nextInt(6));
        }
    }

    // ------------------------------------------------------------------ progress

    private static void say(ServerPlayer p, String speaker, String line) {
        p.displayClientMessage(Component.literal(speaker + ": ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(line).withStyle(ChatFormatting.WHITE)), false);
    }

    static void advance(ServerPlayer p, Tier tier) {
        SysState.SysPlayer sp = SysState.of(p);
        long[] prog = progress(sp, tier);
        List<Step> steps = trail(p, sp, tier);
        prog[1]++;
        prog[2] = 0;
        LAST_HOT_COLD.remove(p.getUUID());
        if (prog[1] >= steps.size()) {
            Inv.remove(p, tier.scrollItem(), 1);
            sp.clues.remove(tier);
            Map<Tier, List<Step>> forced = FORCED.get(p.getUUID());
            if (forced != null) {
                forced.remove(tier);
            }
            Inv.give(p, tier.casketItem(), 1);
            Msg.action(p, "You've found a reward casket!");
        } else {
            Msg.action(p, "You've found another clue!");
            read(p, tier);
        }
    }

    // ------------------------------------------------------------------ events

    /** Kill steps, key steps and Sherlock's kill challenges; guardians killed by their owner. */
    static void onKillEvent(ServerPlayer p, LivingEntity entity, MonsterStats stats) {
        Guard g = GUARDS.remove(entity.getUUID());
        // Only while the player still follows that clue (a guardian outliving its scroll guards nothing).
        if (g != null && g.owner().equals(p.getUUID()) && following(p, g.tier()) && !guardianAlive(p, g.tier())) {
            Step step = current(p, g.tier());
            if (step != null && progress(SysState.of(p), g.tier())[1] == g.index()) {
                setFlag(p, g.tier(), GUARDIAN_KILLED);
                Msg.action(p, step.type() == StepType.EMOTE ? "The double agent is defeated. Perform the emote again."
                        : "The guardian is defeated. Dig again.");
            }
        }
        if (stats == null) {
            return;
        }
        String name = stats.name();
        for (Tier tier : Tier.values()) {
            if (!following(p, tier)) {
                continue;
            }
            Step step = current(p, tier);
            if (step == null) {
                continue;
            }
            if (step.action() == Action.KILL && ClueScrolls.monsterMatches(step.clue().npc(), name)) {
                Msg.action(p, "You find a clue on the " + name + "'s corpse.");
                advance(p, tier);
                return;
            }
            if (step.keyMonster() != null && ClueScrolls.monsterMatches(step.keyMonster(), name) && !Inv.has(p, keyItem(tier), 1)) {
                com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(keyItem(tier), 1), p);
                Msg.red(p, "The " + name + " drops a key!");
            }
            challengeEvent(p, tier, step, ClueChallenges.Kind.KILL, name, 1);
        }
    }

    /** A Sherlock challenge event (obtain, make, kill, Firemaking XP). */
    static void challengeEvent(ServerPlayer p, Tier tier, Step step, ClueChallenges.Kind kind, String target, int n) {
        if (step.type() != StepType.SHERLOCK || !flag(p, tier, TALKED) || flag(p, tier, CHALLENGE_DONE)) {
            return;
        }
        ClueChallenges.Challenge c = ClueChallenges.of(step.clue().text());
        if (c != null && c.matches(kind, target, n)) {
            setFlag(p, tier, CHALLENGE_DONE);
            Msg.action(p, "You have completed Sherlock's challenge. Return to Sherlock.");
        }
    }

    static void challengeEvent(ServerPlayer p, ClueChallenges.Kind kind, String target, int n) {
        for (Tier tier : new Tier[]{Tier.ELITE, Tier.MASTER}) {
            if (following(p, tier)) {
                Step step = current(p, tier);
                if (step != null) {
                    challengeEvent(p, tier, step, kind, target, n);
                }
            }
        }
    }

    static void register() {
        com.runecraft.content.GameEvents.onObtain((p, item, count) -> challengeEvent(p, ClueChallenges.Kind.OBTAIN, item, count));
        com.runecraft.content.GameEvents.onMake((p, r) -> {
            r.outputs().forEach((item, count) -> challengeEvent(p, ClueChallenges.Kind.MAKE, item, count));
        });
        com.runecraft.content.GameEvents.onXp((p, skill, tenths) -> {
            if (skill == Skill.FIREMAKING) {
                challengeEvent(p, ClueChallenges.Kind.FIREMAKING, Integer.toString(tenths), 1);
            }
        });
        SysTicks.everyTick((server, tick) -> {
            if (tick % 2 == 0) {
                guardianTick(server);
            }
            discardStrayGuardians();
        });
        // A guardian saved with its chunk (its owner teleported away, the server stopped) guards nothing once it loads again.
        com.runecraft.platform.ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (entity.getTags().contains(GUARDIAN_TAG) && !GUARDS.containsKey(entity.getUUID())) {
                synchronized (STRAY_GUARDIANS) {
                    STRAY_GUARDIANS.add(entity);
                }
            }
        });
        // The Mimic: summoned with a mimic casket (kept between attempts), its loot by tier and attempt.
        com.runecraft.boss.DataBossService.requirement(MimicRewards.MIMIC, (p, boss) -> mimicChallenge(p));
        com.runecraft.boss.DataBossService.loot(MimicRewards.MIMIC, ClueService::mimicLoot);
        // The clue characters with no other dialogue.
        for (ClueNpcs.ClueNpc n : ClueNpcs.ALL) {
            com.runecraft.quest.QuestScripts.onTalk(n.id(), (p, npc) -> com.runecraft.quest.QuestApi.say(p, n.name(), switch (n.id()) {
                case ClueNpcs.CHARLIE -> "Spare some change, guv? I hear treasure hunters pass through here.";
                case ClueNpcs.CECILIA -> "I do love a good song. Treasure hunters sometimes play for me.";
                case ClueNpcs.SHERLOCK -> "I solve mysteries. Bring me a clue and I'll set you a challenge.";
                default -> "Hello there, adventurer.";
            }));
        }
    }

    // ------------------------------------------------------------------ caskets

    /**
     * Opens a reward casket: counts the completed trail, may turn out to be a Mimic (elite 1/35, master 1/15 when enabled, and
     * the one-time guaranteed one on the 10th master or 25th elite casket; one Mimic carried at a time), else gives the rewards.
     */
    public static void openCasket(ServerPlayer p, ItemStack stack, Tier tier) {
        stack.shrink(1);
        SysState.SysPlayer sp = SysState.of(p);
        int n = sp.cluesCompleted.merge(tier, 1, Integer::sum);
        Players.data(p).incrementKillCount("clue_" + tier.id());
        p.displayClientMessage(Component.literal("You have completed " + n + " " + tier.id() + " Treasure Trail" + (n == 1 ? "" : "s") + ".")
                .withStyle(ChatFormatting.GOLD), false);
        DiaryService.onClue(p, tier);
        Random random = new Random(p.getRandom().nextLong());
        int mimic = ClueRewards.mimicOneIn(tier);
        if (mimic > 0 && sp.mimicEnabled && (MimicRewards.guaranteed(tier, n, sp.mimicSeen) || random.nextInt(mimic) == 0)) {
            if (Inv.has(p, ClueRewards.MIMIC_ITEM, 1)) {
                Msg.game(p, "You already have a Mimic, and cannot be given another one.");
            } else {
                sp.mimicSeen = true;
                giveMimic(p, new MimicRewards.Casket(tier, ClueRewards.rollCount(tier, random), 0));
                Msg.red(p, "The chest turned out to be the Mimic! Visit the strange casket, upstairs in Watson's house in Hosidius,"
                        + " to attempt the Mimic's challenge.");
                return;
            }
        }
        giveRewards(p, tier, ClueRewards.roll(tier, random));
    }

    /** Gives a casket's rewards and shows the reward screen. */
    static void giveRewards(ServerPlayer p, Tier tier, List<ClueRewards.Reward> rewards) {
        long value = 0;
        SysNet.Screen s = new SysNet.Screen("casket", "Treasure trail reward (" + tier.id() + ")");
        for (ClueRewards.Reward r : rewards) {
            ItemDef def = ItemCatalog.get(r.item());
            if (def == null) {
                continue;
            }
            Tier scroll = Tier.byScroll(r.item());
            if (scroll != null) {
                if (!giveClue(p, scroll)) {
                    sneakingSuspicion(p, scroll);
                    continue;
                }
            } else if (r.noted() && com.runecraft.bank.Notes.canNote(r.item(), "")) {
                Inv.give(p, com.runecraft.bank.Notes.note(r.item(), "", r.count()));
            } else {
                Inv.give(p, r.item(), r.count());
            }
            CollectionLog.obtain(p, r.item());
            value += (long) def.value() * r.count();
            boolean rare = ClueRewards.uniques(tier).contains(r.item()) || ClueRewards.megaRares(tier).contains(r.item());
            s.line((r.count() > 1 ? r.count() + " x " : "") + def.name(), rare ? 0xFFD700 : 0xFFFFFF);
        }
        s.line("Your treasure is worth around " + String.format(Locale.ROOT, "%,d", value) + " coins!", 0x80FF80);
        Msg.game(p, "Your treasure is worth around " + String.format(Locale.ROOT, "%,d", value) + " coins!");
        com.runecraft.network.ServerNetworking.sync(p);
        s.send(p);
    }

    // ------------------------------------------------------------------ the Mimic

    /** The custom-data key of a mimic item's casket ({@link MimicRewards.Casket} encoding). */
    static final String MIMIC_DATA = "rc_mimic_casket";

    static void giveMimic(ServerPlayer p, MimicRewards.Casket casket) {
        ItemStack mimic = Inv.stack(ClueRewards.MIMIC_ITEM, 1);
        if (mimic.isEmpty()) {
            return;
        }
        StackData.update(mimic, t -> t.putString(MIMIC_DATA, casket.encode()));
        Inv.give(p, mimic);
    }

    /** The casket a mimic item holds (one with no saved casket counts as a master casket with its fewest rolls). */
    static MimicRewards.Casket casketOf(ItemStack stack) {
        MimicRewards.Casket c = StackData.contains(stack, MIMIC_DATA)
                ? MimicRewards.Casket.decode(StackData.get(stack).getString(MIMIC_DATA)) : null;
        return c != null ? c : new MimicRewards.Casket(Tier.MASTER, Tier.MASTER.minRolls(), 0); // approx
    }

    /** A mimic item in the inventory: the one holding {@code casket} (an encoding) if there is one, else the first; or null. */
    static ItemStack mimicStack(ServerPlayer p, String casket) {
        ItemStack first = null;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.isEmpty() || !Inv.idOf(s).equals(ClueRewards.MIMIC_ITEM)) {
                continue;
            }
            if (casket == null || casketOf(s).encode().equals(casket)) {
                return s;
            }
            if (first == null) {
                first = s;
            }
        }
        return first;
    }

    /**
     * The strange casket in Watson's house (the Mimic's summon requirement): presenting a mimic casket starts a fight, which is
     * one of the casket's attempts (six from a master casket, five from an elite one). The casket is kept until the Mimic dies;
     * every failed attempt (leaving or dying) costs one of its item rolls, and once every attempt is spent its loot is lost.
     */
    static boolean mimicChallenge(ServerPlayer p) {
        ItemStack stack = mimicStack(p, null);
        if (stack == null) {
            Msg.game(p, "The strange casket is waiting for a Mimic: an elite or master reward casket can turn out to be one.");
            return false;
        }
        MimicRewards.Casket casket = casketOf(stack);
        if (casket.exhausted()) {
            stack.shrink(1);
            Msg.red(p, "You've used every attempt at this Mimic: it crumbles away, and the casket's loot with it.");
            return false;
        }
        MimicRewards.Casket next = casket.nextAttempt();
        StackData.update(stack, t -> t.putString(MIMIC_DATA, next.encode()));
        SysState.of(p).mimicFight = next.encode();
        int left = MimicRewards.attempts(next.tier()) - next.attempt();
        if (next.attempt() > 1) {
            Msg.game(p, "Attempt " + next.attempt() + " at the Mimic: its casket now holds " + next.rollsNow() + " item roll"
                    + (next.rollsNow() == 1 ? "" : "s") + (left == 0 ? ". This is your last chance." : "."));
        }
        return true;
    }

    /**
     * The Mimic's own loot (OSRS Wiki "The Mimic"): the mahogany plank, then the pre-roll for its tier and attempt (a ring of
     * 3rd age or a 3rd age piece) or else one main drop whose quantity shrinks by 1/5 (elite) or 1/6 (master) of the maximum
     * for each failed attempt.
     */
    static List<com.runecraft.skill.Drops.Rolled> mimicLoot(ServerPlayer killer, com.runecraft.boss.DataBossEntity boss,
            List<com.runecraft.osrs.data.Drop> table) {
        MimicRewards.Casket c = MimicRewards.Casket.decode(SysState.of(killer).mimicFight);
        Tier tier = c == null ? Tier.MASTER : c.tier();
        int attempt = c == null ? 1 : Math.max(1, c.attempt());
        net.minecraft.util.RandomSource random = boss.getRandom();
        String unique = MimicRewards.roll(tier, attempt, new Random(random.nextLong()));
        List<com.runecraft.osrs.data.Drop> always = new ArrayList<>();
        List<com.runecraft.osrs.data.Drop> main = new ArrayList<>();
        for (com.runecraft.osrs.data.Drop d : com.runecraft.osrs.content.bosses.BossLoot.pick(MimicRewards.MIMIC, table, random::nextInt)) {
            (d.rarity() <= 1 ? always : main).add(d);
        }
        List<com.runecraft.skill.Drops.Rolled> out = new ArrayList<>(com.runecraft.skill.Drops.resolve(always, random));
        if (unique != null) {
            out.add(new com.runecraft.skill.Drops.Rolled(unique, 1));
        } else {
            for (com.runecraft.skill.Drops.Rolled r : com.runecraft.skill.Drops.resolve(main, random)) {
                out.add(new com.runecraft.skill.Drops.Rolled(r.item(), MimicRewards.quantity(tier, r.count(), attempt), r.doses()));
            }
        }
        return out;
    }

    /**
     * The Mimic died: the Mimic scroll case (+1 scroll box of every tier) the first time, and the mimic casket's own rewards
     * with one item roll fewer for each failed attempt.
     */
    static void onMimicKilled(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        sp.mimicSeen = true;
        if (!sp.mimicCase) {
            sp.mimicCase = true;
            Msg.action(p, "You find a Mimic scroll case: you can now stack one more scroll box of every tier.");
        }
        MimicRewards.Casket casket = MimicRewards.Casket.decode(sp.mimicFight);
        sp.mimicFight = "";
        if (casket == null) {
            return;
        }
        ItemStack stack = mimicStack(p, casket.encode());
        if (stack == null) {
            Msg.game(p, "You'd need the mimic casket with you to claim its treasure.");
            return;
        }
        stack.shrink(1);
        giveRewards(p, casket.tier(), ClueRewards.roll(casket.tier(), casket.rollsNow(), new Random(p.getRandom().nextLong())));
    }

    // ------------------------------------------------------------------ clue scrolls and scroll boxes

    /**
     * Whether the player has a clue scroll of the tier in the inventory or the bank (the OSRS rule: one on the ground doesn't
     * count). When it is gone (dropped, lost on death) the old trail is forgotten, so the next scroll of the tier starts afresh
     * (OSRS Wiki "Scroll box": a new clue resets a juggled clue's progress).
     */
    static boolean heldClue(ServerPlayer p, Tier tier) {
        if (Inv.has(p, tier.scrollItem(), 1) || Players.data(p).bank().count(tier.scrollItem(), "") > 0) {
            return true;
        }
        SysState.of(p).clues.remove(tier);
        return false;
    }

    /** The clues of a tier counted against the stack limit: the scroll and the scroll boxes, carried or banked. */
    static int clueStack(ServerPlayer p, Tier tier) {
        String box = ClueRewards.scrollBox(tier);
        return (heldClue(p, tier) ? 1 : 0) + Inv.count(p, box) + (int) Math.min(Integer.MAX_VALUE, Players.data(p).bank().count(box, ""));
    }

    /** The tier's stack limit: 2, +1 at each of its minor and major scroll case milestones, +1 with the Mimic scroll case. */
    static int stackLimit(ServerPlayer p, Tier tier) {
        SysState.SysPlayer sp = SysState.of(p);
        return ClueRewards.stackLimit(tier, sp.cluesCompleted.getOrDefault(tier, 0), sp.mimicCase);
    }

    /** Whether one more clue of the tier can be given (a scroll, or a scroll box after X Marks the Spot). */
    static boolean canTakeClue(ServerPlayer p, Tier tier) {
        return !heldClue(p, tier) || scrollBoxes(p) && clueStack(p, tier) < stackLimit(p, tier);
    }

    static void sneakingSuspicion(ServerPlayer p, Tier tier) {
        Msg.game(p, "You have a sneaking suspicion that you would have received " + Msg.article(tier.id())
                + (scrollBoxes(p) ? " scroll box." : " clue scroll."));
    }

    /**
     * Gives a clue scroll of a tier: the scroll itself, or after X Marks the Spot a scroll box when the player already
     * has one, up to the tier's stack limit. Returns false when the player is at the limit.
     */
    public static boolean giveClue(ServerPlayer p, Tier tier) {
        if (!heldClue(p, tier)) {
            Inv.give(p, tier.scrollItem(), 1);
            return true;
        }
        if (!canTakeClue(p, tier)) {
            return false;
        }
        Inv.give(p, ClueRewards.scrollBox(tier), 1);
        return true;
    }

    /** Whether the player gets scroll boxes (OSRS: after completing X Marks the Spot). */
    static boolean scrollBoxes(ServerPlayer p) {
        com.runecraft.osrs.QuestInfo q = com.runecraft.osrs.content.ContentRegistry.quest("x_marks_the_spot");
        return q == null || Players.data(p).questComplete(q);
    }

    /** Opening a scroll box: a clue scroll of its tier, unless the player already has one. */
    public static boolean openScrollBox(ServerPlayer p, ItemStack stack, Tier tier) {
        if (heldClue(p, tier)) {
            Msg.game(p, "The clue box refuses to open - you already have a clue.");
            return true;
        }
        stack.shrink(1);
        Inv.give(p, tier.scrollItem(), 1);
        Msg.action(p, "You open the scroll box and find a clue scroll.");
        return true;
    }

    // ------------------------------------------------------------------ Watson

    /**
     * Watson takes an easy, medium, hard and elite clue scroll (handed in one at a time if need be) and gives a master
     * clue scroll for the set. He also runs the strange casket that turns Mimic encounters on or off.
     */
    static void watson(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        Tier[] needed = {Tier.EASY, Tier.MEDIUM, Tier.HARD, Tier.ELITE};
        int took = 0;
        for (int i = 0; i < needed.length; i++) {
            Tier t = needed[i];
            if ((sp.watsonHanded & (1 << i)) == 0 && Inv.has(p, t.scrollItem(), 1)) {
                Inv.remove(p, t.scrollItem(), 1);
                sp.clues.remove(t);
                sp.watsonHanded |= 1 << i;
                took++;
            }
        }
        if (sp.watsonHanded == 0b1111) {
            if (!giveClue(p, Tier.MASTER)) {
                say(p, "Watson", "You already have a master clue. Come back when you've finished it.");
            } else {
                sp.watsonHanded = 0;
                say(p, "Watson", "Splendid! That's one of each. Here is a master clue scroll. Good luck!");
            }
        } else {
            StringBuilder want = new StringBuilder();
            for (int i = 0; i < needed.length; i++) {
                if ((sp.watsonHanded & (1 << i)) == 0) {
                    want.append(want.length() == 0 ? "" : ", ").append(needed[i].id());
                }
            }
            say(p, "Watson", (took > 0 ? "Thank you. " : "") + "Bring me " + want + " clue scrolls and I'll give you a master clue.");
        }
        SysNet.Screen s = new SysNet.Screen("watson", "Watson's house");
        s.line("The strange casket upstairs: Mimic encounters are " + (sp.mimicEnabled ? "enabled" : "disabled") + ".", 0xC0C0C0);
        s.button("mimic", sp.mimicEnabled ? "Disable Mimic encounters" : "Enable Mimic encounters");
        s.send(p);
    }

    static void watsonAction(ServerPlayer p, String action, Map<String, String> fields) {
        if (action.equals("mimic")) {
            SysState.SysPlayer sp = SysState.of(p);
            sp.mimicEnabled = !sp.mimicEnabled;
            Msg.game(p, "Elite and master caskets " + (sp.mimicEnabled ? "can now" : "will no longer") + " turn out to be Mimics.");
        }
    }

    // ------------------------------------------------------------------ drops

    public static void onKill(ServerPlayer p, LivingEntity entity, MonsterStats stats) {
        boolean guardian = GUARDS.containsKey(entity.getUUID());
        onKillEvent(p, entity, stats);
        if (!Gielinor.is(entity) || stats == null) {
            return;
        }
        if (!guardian && !dropsNoClues(entity)) {
            rollClueDrop(p, entity, stats);
        }
        Spellbooks.Head head = Spellbooks.headFor(stats.name());
        if (head != null && ItemCatalog.get(head.id()) != null && p.getRandom().nextInt(head.dropRate()) == 0) {
            com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(head.id(), 1), p);
        }
    }

    private static Set<String> noClueDrops;

    /** Clue guardians, double agents and the Mimic drop no clue scrolls (OSRS: they drop nothing / their own table only). */
    static boolean dropsNoClues(LivingEntity entity) {
        if (noClueDrops == null) {
            Set<String> ids = new java.util.HashSet<>(ClueScrolls.guardianMonsters());
            ids.add(MimicRewards.MIMIC);
            noClueDrops = ids;
        }
        String id = entity instanceof com.runecraft.boss.DataBossEntity b && b.def() != null ? b.def().id
                : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return noClueDrops.contains(id);
    }

    private static void rollClueDrop(ServerPlayer p, LivingEntity entity, MonsterStats stats) {
        java.util.Set<Tier> own = java.util.EnumSet.noneOf(Tier.class);
        if (stats.monster() != null) {
            own.addAll(ClueScrolls.ownClueTiers(stats.monster().drops()));
        }
        if (stats.profile() != null) {
            own.addAll(ClueScrolls.ownClueTiers(stats.profile().drops));
        }
        if (stats.boss() != null) {
            own.addAll(ClueScrolls.ownClueTiers(stats.boss().drops()));
        }
        for (Tier tier : Tier.values()) {
            int oneIn = own.contains(tier) ? 0 : ClueScrolls.dropOneIn(tier, stats.name(), stats.combatLevel());
            // Combat Achievement tiers give a 5% higher easy..elite clue chance.
            double caBonus = com.runecraft.osrs.content.systems.CombatAchievements.clueChanceMultiplier(CombatAchievementService.tier(p), tier);
            if (oneIn > 0 && caBonus > 1.0) {
                oneIn = Math.max(1, (int) Math.round(oneIn / caBonus));
            }
            if (oneIn <= 0) {
                continue;
            }
            if (p.getRandom().nextInt(oneIn) == 0) {
                if (!heldClue(p, tier)) {
                    com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(tier.scrollItem(), 1), p);
                    Msg.red(p, "A clue scroll (" + tier.id() + ") has dropped!");
                } else if (canTakeClue(p, tier)) {
                    com.runecraft.combat.LootDrops.spawn(entity, Inv.stack(ClueRewards.scrollBox(tier), 1), p);
                    Msg.red(p, "A scroll box (" + tier.id() + ") has dropped!");
                } else {
                    sneakingSuspicion(p, tier);
                }
                break;
            }
        }
    }

    // ------------------------------------------------------------------ self-test hooks

    /** Forces a trail of the given steps on a player (self-test), with a fresh clue scroll of the tier. */
    static void force(ServerPlayer p, Tier tier, List<Step> steps) {
        FORCED.computeIfAbsent(p.getUUID(), k -> new HashMap<>()).put(tier, steps);
        SysState.of(p).clues.put(tier, new long[]{0, 0, 0});
        if (!Inv.has(p, tier.scrollItem(), 1)) {
            Inv.give(p, tier.scrollItem(), 1);
        }
    }

    static void unforce(ServerPlayer p) {
        FORCED.remove(p.getUUID());
    }

    /** The live guardians summoned by the player's current step of a tier (self-test). */
    static List<LivingEntity> guardians(ServerPlayer p, Tier tier) {
        List<LivingEntity> out = new ArrayList<>();
        ServerLevel world = (ServerLevel) p.level();
        int index = stepIndex(p, tier);
        for (Map.Entry<UUID, Guard> e : GUARDS.entrySet()) {
            Guard g = e.getValue();
            if (g.owner().equals(p.getUUID()) && g.tier() == tier && g.index() == index
                    && world.getEntity(e.getKey()) instanceof LivingEntity le && le.isAlive()) {
                out.add(le);
            }
        }
        return out;
    }

    /**
     * Removes every guardian the player has summoned, of any tier (self-test clean-up), also one in a chunk the server hasn't
     * made visible yet.
     */
    static void clearGuardians(ServerPlayer p) {
        ServerLevel world = (ServerLevel) p.level();
        Iterator<Map.Entry<UUID, Guard>> it = GUARDS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Guard> e = it.next();
            if (e.getValue().owner().equals(p.getUUID())) {
                Entity mob = guardEntity(world, e.getKey(), e.getValue());
                if (mob != null) {
                    mob.discard();
                }
                it.remove();
            }
        }
    }

    /** The index of the player's current step of a tier (self-test). */
    static int stepIndex(ServerPlayer p, Tier tier) {
        long[] prog = SysState.of(p).clues.get(tier);
        return prog == null ? -1 : (int) prog[1];
    }
}
