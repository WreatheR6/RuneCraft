package com.runecraft.content.raids;

import com.runecraft.osrs.content.raids.Invocation;
import com.runecraft.osrs.content.raids.RaidRewards;
import com.runecraft.osrs.content.raids.RaidType;
import com.runecraft.osrs.content.raids.ToaLoot;
import com.runecraft.util.Msg;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * The Tombs of Amascut's invocation interface, in chat (OSRS: the party's invocation tab at the obelisk): every
 * invocation by category, active ones lit, each a link that toggles it ({@code /raids invocation <name>}), the presets,
 * the raid level and mode, and which uniques the raid level can give without the extra 1/50 roll.
 */
final class ToaInvocationMenu {
    private ToaInvocationMenu() {
    }

    static void show(ServerPlayer p, RaidInstance raid) {
        int level = raid.raidLevel();
        p.displayClientMessage(Component.literal("Invocations - raid level " + level + " (" + Invocation.modeName(level) + ")")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        Map<String, MutableComponent> lines = new LinkedHashMap<>();
        for (Invocation i : Invocation.values()) {
            MutableComponent line = lines.computeIfAbsent(i.category(),
                    c -> Component.literal(c + ": ").withStyle(ChatFormatting.YELLOW));
            boolean on = raid.invocations().contains(i);
            String hover = i.displayName() + " (+" + i.level() + ")" + (i.requires() != null ? "\nNeeds " + i.requires().displayName() : "")
                    + (raid.begun() ? "\nThe invocations are locked in." : "\nClick to turn " + (on ? "off" : "on") + ".");
            line.append(Component.literal((on ? "[x] " : "[ ] ") + i.displayName() + " ").withStyle(s -> s
                    .withColor(on ? ChatFormatting.GREEN : ChatFormatting.GRAY)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raids invocation " + i.name().toLowerCase(Locale.ROOT)))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover)))));
        }
        for (MutableComponent line : lines.values()) {
            p.displayClientMessage(line, false);
        }
        MutableComponent presets = Component.literal("Presets: ").withStyle(ChatFormatting.YELLOW);
        List<Invocation.Preset> list = Invocation.presets();
        for (int i = 0; i < list.size(); i++) {
            Invocation.Preset preset = list.get(i);
            boolean on = raid.presetIndex() == i;
            presets.append(Component.literal("[" + preset.name() + "] ").withStyle(s -> s
                    .withColor(on ? ChatFormatting.GREEN : ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/raids preset " + list.indexOf(preset)))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Raid level " + preset.raidLevel())))));
        }
        p.displayClientMessage(presets, false);
        StringBuilder uniques = new StringBuilder();
        for (RaidRewards.Unique u : RaidRewards.TOA_UNIQUES) {
            if (level >= ToaLoot.minLevel(u.item())) {
                var def = com.runecraft.osrs.ItemCatalog.get(u.item());
                uniques.append(uniques.length() == 0 ? "" : ", ").append(def == null ? u.item() : def.name());
            }
        }
        p.displayClientMessage(Component.literal("Uniques at this raid level: " + (uniques.length() == 0
                ? "none (each needs an extra 1/50 roll)" : uniques)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    /** {@code /raids invocation <name>}: the party leader toggles an invocation before the raid begins. */
    static int toggle(ServerPlayer p, String name) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null || raid.type != RaidType.TOA) {
            Msg.game(p, "You must be in the Tombs of Amascut lobby to choose invocations.");
            return 0;
        }
        if (!raid.isLeader(p)) {
            Msg.game(p, "Only the party leader can choose the invocations.");
            return 0;
        }
        Invocation i = Invocation.byName(name);
        if (i == null) {
            Msg.game(p, "There is no invocation called " + name + ".");
            return 0;
        }
        String problem = raid.toggleInvocation(i);
        if (problem != null) {
            Msg.game(p, problem);
            return 0;
        }
        raid.say(p.getName().getString() + (raid.invocations().contains(i) ? " turns on " : " turns off ") + i.displayName()
                + ": raid level " + raid.raidLevel() + ".", ChatFormatting.YELLOW);
        show(p, raid);
        return 1;
    }

    /** {@code /raids preset <index>}: the party leader picks a preset. */
    static int preset(ServerPlayer p, int index) {
        RaidInstance raid = RaidManager.of(p);
        if (raid == null || raid.type != RaidType.TOA) {
            Msg.game(p, "You must be in the Tombs of Amascut lobby to choose invocations.");
            return 0;
        }
        if (raid.begun()) {
            Msg.game(p, "The invocations are locked in: raid level " + raid.raidLevel() + ".");
            return 0;
        }
        if (!raid.isLeader(p)) {
            Msg.game(p, "Only the party leader can choose the invocations.");
            return 0;
        }
        raid.applyPreset(index);
        raid.say(p.getName().getString() + " sets the invocations to " + Invocation.presets().get(raid.presetIndex()).name()
                + " (raid level " + raid.raidLevel() + ").", ChatFormatting.YELLOW);
        show(p, raid);
        return 1;
    }
}
