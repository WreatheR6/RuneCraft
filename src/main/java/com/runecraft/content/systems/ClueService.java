package com.runecraft.content.systems;

import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.systems.ClueRewards;
import com.runecraft.osrs.content.systems.CluePuzzles;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.ClueScrolls.Step;
import com.runecraft.osrs.content.systems.ClueScrolls.Tier;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Locale;

/** Treasure trails: reading clues, digging, talking to NPCs, reward caskets and clue drops. */
public final class ClueService {
    private static List<ClueScrolls.Npc> npcs;

    private ClueService() {
    }

    static List<ClueScrolls.Npc> npcs() {
        if (npcs == null) {
            npcs = ClueScrolls.npcs();
        }
        return npcs;
    }

    /** The trail of a tier for this player (generating it the first time the clue is read). */
    static List<Step> trail(SysState.SysPlayer sp, Tier tier) {
        long[] prog = sp.clues.get(tier);
        if (prog == null) {
            prog = new long[]{new java.util.Random().nextLong(), 0};
            sp.clues.put(tier, prog);
        }
        return ClueScrolls.generate(tier, prog[0], npcs());
    }

    static Step current(SysState.SysPlayer sp, Tier tier) {
        List<Step> steps = trail(sp, tier);
        int i = (int) sp.clues.get(tier)[1];
        return i < steps.size() ? steps.get(i) : null;
    }

    public static void read(ServerPlayerEntity p, Tier tier) {
        SysState.SysPlayer sp = SysState.of(p);
        List<Step> steps = trail(sp, tier);
        int index = (int) sp.clues.get(tier)[1];
        Step step = index < steps.size() ? steps.get(index) : null;
        SysNet.Screen s = new SysNet.Screen("clue", "Clue scroll (" + tier.id() + ")");
        if (step == null) {
            s.line("This clue has been solved.");
            s.send(p);
            return;
        }
        s.line("Step " + (index + 1) + " of " + steps.size(), 0xC0C0C0);
        s.line(step.text(), 0xF0E0A0);
        if (step.type() == ClueScrolls.StepType.HOT_COLD) {
            s.line("Feel the strange device: the hotter it gets, the closer you are.", 0xA0A0A0);
            s.button("hint:" + tier.name(), "Feel the device");
            s.button("dig:" + tier.name(), "Dig here");
            s.send(p);
            return;
        }
        if (step.isDig()) {
            s.line(spadeExists() ? "Dig at the spot with a spade (or use the button while standing there)." : "Dig at the spot (use the button while standing there).",
                    0xA0A0A0);
            s.button("dig:" + tier.name(), "Dig here");
        } else if (step.type() == ClueScrolls.StepType.EMOTE) {
            s.line("Stand at the spot and perform the emote.", 0xA0A0A0);
            s.button("emote:" + tier.name(), "Perform " + step.emote());
        } else if (step.type() == ClueScrolls.StepType.CHARLIE) {
            s.line("Talk to them while carrying the item.", 0xA0A0A0);
        } else if (step.type() == ClueScrolls.StepType.FALO) {
            s.line("Talk to Falo the Bard, north of Seers' Village, while carrying the item.", 0xA0A0A0);
        } else if (step.type() == ClueScrolls.StepType.PUZZLE || step.type() == ClueScrolls.StepType.LIGHT_BOX) {
            s.line("Talk to them for the " + (step.type() == ClueScrolls.StepType.PUZZLE ? "puzzle box" : "light box")
                    + ", solve it, then talk to them again.", 0xA0A0A0);
        } else {
            s.line("Talk to the person this clue points to.", 0xA0A0A0);
        }
        s.button("hint:" + tier.name(), "Hint", "Roughly where the next step is from here.", true);
        s.send(p);
    }

    static boolean spadeExists() {
        return ItemCatalog.get("spade") != null;
    }

    static void action(ServerPlayerEntity p, String action, java.util.Map<String, String> fields) {
        if (action.startsWith("hint:")) {
            try {
                Step step = current(SysState.of(p), Tier.valueOf(action.substring(5)));
                if (step != null && step.type() == ClueScrolls.StepType.HOT_COLD) {
                    int[] at = SysTeleport.osrs(p);
                    int dist = Gielinor.is(p) ? (int) Math.sqrt(Math.pow(step.x() - at[0], 2) + Math.pow(step.y() - at[1], 2)) : Integer.MAX_VALUE;
                    Msg.game(p, "The device is " + ClueScrolls.temperature(dist) + ".");
                } else if (step != null) {
                    int[] at = SysTeleport.osrs(p);
                    Msg.game(p, Gielinor.is(p) ? "Hint: the next step is " + hint(at[0], at[1], step.x(), step.y()) + "."
                            : "Hint: the next step is in Gielinor near OSRS coordinates " + step.x() + ", " + step.y() + ".");
                }
            } catch (IllegalArgumentException ignored) {
                // bad tier
            }
            return;
        }
        if (action.startsWith("emote:")) {
            try {
                emote(p, Tier.valueOf(action.substring(6)));
            } catch (IllegalArgumentException ignored) {
                // bad tier
            }
            return;
        }
        if (action.startsWith("dig:")) {
            try {
                dig(p, Tier.valueOf(action.substring(4)), true);
            } catch (IllegalArgumentException ignored) {
                // bad tier
            }
        }
    }

    /** Tries to dig for a clue of the tier at the player's position. Returns true on success. */
    public static boolean dig(ServerPlayerEntity p, Tier tier, boolean loud) {
        if (!Inv.has(p, tier.scrollItem(), 1)) {
            return false;
        }
        SysState.SysPlayer sp = SysState.of(p);
        Step step = current(sp, tier);
        if (step == null || !step.isDig()) {
            return false;
        }
        if (spadeExists() && !Inv.has(p, "spade", 1)) {
            if (loud) {
                Msg.game(p, "You need a spade to dig.");
            }
            return false;
        }
        int[] at = SysTeleport.osrs(p);
        if (!Gielinor.is(p) || !Teleports.within(at[0], at[1], step.x(), step.y(), ClueScrolls.DIG_RADIUS)) {
            if (loud) {
                Msg.game(p, "You dig a hole... Nothing interesting happens. (You are at " + at[0] + ", " + at[1] + ".)");
                Msg.game(p, "Hint: the spot is " + hint(at[0], at[1], step.x(), step.y()) + ".");
            }
            return false;
        }
        ((ServerWorld) p.getWorld()).spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.getX(), p.getY(), p.getZ(), 8, 0.3, 0.1, 0.3, 0.01);
        p.getWorld().playSound(null, p.getBlockPos(), SoundEvents.ITEM_SHOVEL_FLATTEN, SoundCategory.PLAYERS, 1f, 1f);
        Msg.action(p, "You dig a hole...");
        advance(p, tier);
        return true;
    }

    /** Performs the current emote step: right place, and the required item carried or worn. */
    static void emote(ServerPlayerEntity p, Tier tier) {
        if (!Inv.has(p, tier.scrollItem(), 1)) {
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
        Step step = current(sp, tier);
        if (step == null || step.type() != ClueScrolls.StepType.EMOTE) {
            return;
        }
        Msg.action(p, "You perform the " + step.emote() + " emote.");
        int[] at = SysTeleport.osrs(p);
        if (!Gielinor.is(p) || !Teleports.within(at[0], at[1], step.x(), step.y(), ClueScrolls.EMOTE_RADIUS)) {
            Msg.game(p, "Nothing happens. Hint: the spot is " + hint(at[0], at[1], step.x(), step.y()) + ".");
            return;
        }
        if (step.item() != null && !Inv.has(p, step.item(), 1) && !sp.wearing(step.item())
                && !Inv.idOf(p.getEquippedStack(net.minecraft.entity.EquipmentSlot.HEAD)).equals(step.item())
                && !Inv.idOf(p.getEquippedStack(net.minecraft.entity.EquipmentSlot.CHEST)).equals(step.item())
                && !Inv.idOf(p.getEquippedStack(net.minecraft.entity.EquipmentSlot.LEGS)).equals(step.item())
                && !Inv.idOf(p.getEquippedStack(net.minecraft.entity.EquipmentSlot.FEET)).equals(step.item())) {
            ItemDef d = ItemCatalog.get(step.item());
            Msg.game(p, "Uri appears, then shakes his head: you need " + (d == null ? step.item() : Msg.article(d.name().toLowerCase(Locale.ROOT)))
                    + " with you.");
            return;
        }
        say(p, "Uri", "Once, I was a poor man, but then I found a party hat. Here is your next clue.");
        advance(p, tier);
    }

    /** A rough compass hint (OSRS players use a sextant; this keeps trails solvable in a block world). */
    static String hint(int x, int y, int tx, int ty) {
        int dx = tx - x;
        int dy = ty - y;
        String ns = dy > 2 ? "north" : dy < -2 ? "south" : "";
        String ew = dx > 2 ? "east" : dx < -2 ? "west" : "";
        int dist = (int) Math.sqrt((double) dx * dx + (double) dy * dy);
        return "about " + dist + " tiles " + (ns + (ns.isEmpty() || ew.isEmpty() ? "" : "-") + ew) + " of you";
    }

    /** Talking to an NPC: completes talk steps. Returns true if the interaction was used by a clue. */
    public static boolean talk(ServerPlayerEntity p, QuestNpcEntity npc) {
        SysState.SysPlayer sp = SysState.of(p);
        for (Tier tier : Tier.values()) {
            if (!Inv.has(p, tier.scrollItem(), 1) || !sp.clues.containsKey(tier)) {
                continue;
            }
            Step step = current(sp, tier);
            if (step == null || step.isDig() || !npc.npcId().equals(step.npcId())) {
                continue;
            }
            String speaker = step.npcName() == null ? "Stranger" : step.npcName();
            if (step.type() == ClueScrolls.StepType.PUZZLE || step.type() == ClueScrolls.StepType.LIGHT_BOX) {
                if (!puzzleTalk(p, tier, step, speaker)) {
                    return true;
                }
            } else if (step.type() == ClueScrolls.StepType.FALO) {
                if (!Inv.has(p, step.item(), 1) && !sp.wearing(step.item())) {
                    ItemDef d = ItemCatalog.get(step.item());
                    say(p, speaker, "My song is not complete without " + (d == null ? step.item()
                            : Msg.article(d.name().toLowerCase(Locale.ROOT))) + ". Come back when you have it with you.");
                    return true;
                }
                say(p, speaker, "Ah, a fine " + step.item().replace('_', ' ') + "! The song is complete. Here is your reward.");
            } else if (step.type() == ClueScrolls.StepType.CHARLIE) {
                if (!Inv.has(p, step.item(), 1)) {
                    ItemDef d = ItemCatalog.get(step.item());
                    say(p, speaker, "Ah, a treasure hunter! Bring me " + (d == null ? step.item() : Msg.article(d.name().toLowerCase(Locale.ROOT)))
                            + " and I'll give you your next clue.");
                    return true;
                }
                Inv.remove(p, step.item(), 1);
                say(p, speaker, "Thank you! Here's what you've been looking for.");
            } else {
                say(p, speaker, "Ah, you've solved my riddle. Well done, here's your reward.");
            }
            advance(p, tier);
            return true;
        }
        return false;
    }

    // ---------------- Puzzle boxes and light boxes ----------------

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

    /** The player's saved puzzle, or null. */
    private static Puzzle loadPuzzle(ServerPlayerEntity p) {
        return Puzzle.decode(SysState.of(p).cluePuzzle);
    }

    private static void savePuzzle(ServerPlayerEntity p, Puzzle puzzle) {
        SysState.of(p).cluePuzzle = puzzle == null ? "" : puzzle.encode();
    }

    static void registerPuzzles() {
        for (String id : new String[]{"grid3_puzzle", "grid5_puzzle", "grid5_lights"}) {
            SysNet.onAction(id, ClueService::puzzleAction);
        }
    }

    /** Talking to the NPC of a puzzle step: hands over the box, or takes it back solved. Returns true to advance. */
    private static boolean puzzleTalk(ServerPlayerEntity p, Tier tier, Step step, String speaker) {
        String box = step.item();
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

    private static Puzzle puzzleFor(ServerPlayerEntity p, Tier tier, String box) {
        Puzzle puzzle = loadPuzzle(p);
        boolean light = box.equals(CluePuzzles.LIGHT_BOX);
        if (puzzle == null || puzzle.tier != tier || (puzzle.lights != null) != light
                || (puzzle.slider != null && puzzle.slider.size() != CluePuzzles.puzzleSize(tier))) {
            java.util.Random r = new java.util.Random(p.getRandom().nextLong());
            puzzle = light ? new Puzzle(tier, null, CluePuzzles.LightBox.scrambled(r))
                    : new Puzzle(tier, CluePuzzles.Slider.scrambled(CluePuzzles.puzzleSize(tier), r), null);
            savePuzzle(p, puzzle);
        }
        return puzzle;
    }

    /** Using a puzzle box or light box item: opens the puzzle of the clue step that handed it over. */
    public static boolean useBox(ServerPlayerEntity p, String box) {
        SysState.SysPlayer sp = SysState.of(p);
        for (Tier tier : Tier.values()) {
            if (!Inv.has(p, tier.scrollItem(), 1) || !sp.clues.containsKey(tier)) {
                continue;
            }
            Step step = current(sp, tier);
            if (step != null && box.equals(step.item())
                    && (step.type() == ClueScrolls.StepType.PUZZLE || step.type() == ClueScrolls.StepType.LIGHT_BOX)) {
                openPuzzle(p, puzzleFor(p, tier, box));
                return true;
            }
        }
        Msg.game(p, "This box isn't part of any clue you're following.");
        return true;
    }

    private static void openPuzzle(ServerPlayerEntity p, Puzzle puzzle) {
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
            s.button("l:" + i, puzzle.lights.lit(i) ? "\u25A0" : "\u25A1", "", false);
        }
        for (int b = 0; b < CluePuzzles.BUTTONS; b++) {
            s.button("b:" + b, String.valueOf((char) ('A' + b)), "", !puzzle.solved());
        }
        s.send(p);
    }

    static void puzzleAction(ServerPlayerEntity p, String action, java.util.Map<String, String> fields) {
        Puzzle puzzle = loadPuzzle(p);
        if (puzzle == null || action.length() < 3) {
            return;
        }
        int index;
        try {
            index = Integer.parseInt(action.substring(2));
        } catch (NumberFormatException e) {
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

    private static void say(ServerPlayerEntity p, String speaker, String line) {
        p.sendMessage(Text.literal(speaker + ": ").formatted(Formatting.YELLOW).append(Text.literal(line).formatted(Formatting.WHITE)), false);
    }

    static void advance(ServerPlayerEntity p, Tier tier) {
        SysState.SysPlayer sp = SysState.of(p);
        long[] prog = sp.clues.get(tier);
        List<Step> steps = trail(sp, tier);
        prog[1]++;
        if (prog[1] >= steps.size()) {
            Inv.remove(p, tier.scrollItem(), 1);
            sp.clues.remove(tier);
            Inv.give(p, tier.casketItem(), 1);
            int n = sp.cluesCompleted.merge(tier, 1, Integer::sum);
            com.runecraft.player.Players.data(p).incrementKillCount("clue_" + tier.id());
            Msg.action(p, "You've found a reward casket!");
            p.sendMessage(Text.literal("You have completed " + n + " " + tier.id() + " Treasure Trail" + (n == 1 ? "" : "s") + ".")
                    .formatted(Formatting.GOLD), false);
            DiaryService.onClue(p, tier);
        } else {
            Msg.action(p, "You've found another clue!");
            read(p, tier);
        }
    }

    /** Opens a reward casket. */
    public static void openCasket(ServerPlayerEntity p, ItemStack stack, Tier tier) {
        stack.decrement(1);
        List<ClueRewards.Reward> rewards = ClueRewards.roll(tier, new java.util.Random(p.getRandom().nextLong()));
        long value = 0;
        SysNet.Screen s = new SysNet.Screen("casket", "Treasure trail reward (" + tier.id() + ")");
        for (ClueRewards.Reward r : rewards) {
            ItemDef def = ItemCatalog.get(r.item());
            if (def == null) {
                continue;
            }
            Inv.give(p, r.item(), r.count());
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

    // ---------------- Drops ----------------

    public static void onKill(ServerPlayerEntity p, LivingEntity entity, MonsterStats stats) {
        if (!Gielinor.is(entity) || stats == null) {
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
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
            if (oneIn <= 0 || Inv.has(p, tier.scrollItem(), 1) || sp.clues.containsKey(tier)) {
                continue;
            }
            if (p.getRandom().nextInt(oneIn) == 0) {
                entity.dropStack(Inv.stack(tier.scrollItem(), 1));
                Msg.red(p, "A clue scroll (" + tier.id() + ") has dropped!");
                break;
            }
        }
        Spellbooks.Head head = Spellbooks.headFor(stats.name());
        if (head != null && ItemCatalog.get(head.id()) != null && p.getRandom().nextInt(head.dropRate()) == 0) {
            entity.dropStack(Inv.stack(head.id(), 1));
        }
    }
}
