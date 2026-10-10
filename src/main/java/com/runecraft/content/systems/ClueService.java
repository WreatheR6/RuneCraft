package com.runecraft.content.systems;

import com.runecraft.collection.CollectionLog;
import com.runecraft.combat.MonsterStats;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.systems.ClueRewards;
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
        if (step.isDig()) {
            s.line(spadeExists() ? "Dig at the spot with a spade (or use the button while standing there)." : "Dig at the spot (use the button while standing there).",
                    0xA0A0A0);
            s.button("dig:" + tier.name(), "Dig here");
        } else if (step.type() == ClueScrolls.StepType.CHARLIE) {
            s.line("Talk to them while carrying the item.", 0xA0A0A0);
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
                if (step != null) {
                    int[] at = SysTeleport.osrs(p);
                    Msg.game(p, Gielinor.is(p) ? "Hint: the next step is " + hint(at[0], at[1], step.x(), step.y()) + "."
                            : "Hint: the next step is in Gielinor near OSRS coordinates " + step.x() + ", " + step.y() + ".");
                }
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
            if (step.type() == ClueScrolls.StepType.CHARLIE) {
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
        for (Tier tier : Tier.values()) {
            int oneIn = ClueScrolls.dropOneIn(tier, stats.combatLevel());
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
