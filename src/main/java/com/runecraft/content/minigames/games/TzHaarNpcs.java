package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.osrs.content.extras.Pets;
import com.runecraft.osrs.content.minigames.KetRakChallenges;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.TzHaarWaves;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.Random;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * The TzHaar minigame officials: TzHaar-Mej-Jal (the Fight Cave; trades a spare fire cape for 8,000 tokkul or a 1/200 roll at
 * TzRek-Jad), TzHaar-Ket-Keh (the Inferno; takes the fire cape sacrifice and trades a spare infernal cape for a 1/100 roll at
 * Jal-nib-rek) and the pets' metamorphosis (TzRek-Jad into JalRek-Jad after TzHaar-Ket-Rak's six challenges, Jal-nib-rek into
 * TzRek-Zuk). Choices are clickable chat options running {@code /minigame tzhaar <choice>}.
 */
public final class TzHaarNpcs {
    private static final Random RANDOM = new Random();

    private TzHaarNpcs() {
    }

    private static MutableComponent option(String label, String choice, String hover) {
        return Component.literal("[" + label + "]").withStyle(s -> s.withColor(ChatFormatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/minigame tzhaar " + choice))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }

    /** TzHaar-Mej-Jal by the Fight Cave. */
    public static void mejJal(ServerPlayer p) {
        int paused = TzHaarSession.pausedWave(p, Minigame.FIGHT_CAVES);
        QuestApi.say(p, "TzHaar-Mej-Jal", "You want to challenge the Fight Cave, JalYt? Sixty-three waves of TzHaar await. "
                + "Survive, and TzTok-Jad himself will face you. The fire cape is the prize."
                + (paused > 0 ? " You left a fight unfinished: enter and you will face wave " + paused + " again." : ""));
        if (Inv.has(p, "fire_cape", 1)) {
            p.displayClientMessage(Component.literal("Your spare fire cape: ").withStyle(ChatFormatting.GRAY)
                    .append(option("8,000 TokKul", "firecape_tokkul", "Trade one fire cape for 8,000 TokKul"))
                    .append(Component.literal(" "))
                    .append(option("Gamble for TzRek-Jad", "firecape_pet", "Sacrifice one fire cape for a 1/200 chance at the pet")), false);
        }
    }

    /** TzHaar-Ket-Keh by the Inferno. */
    public static void ketKeh(ServerPlayer p) {
        boolean sacrificed = MinigameData.of(p).flag(p.getUUID(), "inferno_cape_sacrificed");
        int paused = TzHaarSession.pausedWave(p, Minigame.INFERNO);
        QuestApi.say(p, "TzHaar-Ket-Keh", (sacrificed ? "You have given the Inferno its fire cape, JalYt; enter whenever you dare."
                : "The Inferno is no place for JalYt. Sacrifice a fire cape and face TzKal-Zuk... if you dare.")
                + " The infernal cape will be yours if you survive sixty-nine waves."
                + (paused > 0 ? " Your fight waits for you at wave " + paused + "." : ""));
        if (Inv.has(p, "infernal_cape", 1)) {
            p.displayClientMessage(Component.literal("Your spare infernal cape: ").withStyle(ChatFormatting.GRAY)
                    .append(option("Gamble for Jal-nib-rek", "infernal_pet", "Sacrifice one infernal cape for a 1/100 chance at the pet")),
                    false);
        }
    }

    /** "/minigame tzhaar &lt;choice&gt;" (near the right official for the cape trades). */
    public static void choose(ServerPlayer p, String choice) {
        switch (choice) {
            case "firecape_tokkul" -> {
                if (Inv.remove(p, "fire_cape", 1) > 0) {
                    Inv.give(p, "tokkul", TzHaarWaves.FIRE_CAPE_TOKKUL);
                    QuestApi.say(p, "TzHaar-Mej-Jal", "A fine cape. Take this TokKul, JalYt.");
                }
            }
            case "firecape_pet" -> {
                if (Inv.remove(p, "fire_cape", 1) > 0) {
                    gamble(p, "tzrek_jad", TzHaarWaves.FIRE_CAPE_PET_CHANCE, "TzHaar-Mej-Jal");
                }
            }
            case "infernal_pet" -> {
                if (Inv.remove(p, "infernal_cape", 1) > 0) {
                    gamble(p, "jal_nib_rek", TzHaarWaves.INFERNAL_CAPE_PET_CHANCE, "TzHaar-Ket-Keh");
                }
            }
            case "metamorphosis" -> metamorphosis(p);
            default -> {
            }
        }
    }

    private static void gamble(ServerPlayer p, String pet, int chance, String npc) {
        if (RANDOM.nextInt(chance) == 0) {
            QuestApi.say(p, npc, "Something stirs in the lava...");
            TzHaarSlayer.pet(p, pet);
        } else {
            QuestApi.say(p, npc, "The lava takes your cape. Nothing comes of it this time, JalYt.");
        }
    }

    /** Whether the player completed all six of TzHaar-Ket-Rak's challenges. */
    public static boolean ketRakComplete(ServerPlayer p) {
        return MinigameData.of(p).flag(p.getUUID(), KetRakChallenges.METAMORPHOSIS);
    }

    /** Switches a carried pet with a metamorphosis to its other form. */
    public static boolean metamorphosis(ServerPlayer p) {
        for (String pet : new String[]{"tzrek_jad", "jalrek_jad", "jal_nib_rek", "tzrek_zuk"}) {
            if (!Inv.has(p, pet, 1)) {
                continue;
            }
            if (!TzHaarWaves.canMetamorphose(pet, ketRakComplete(p))) {
                Msg.game(p, "You must have completed TzHaar-Ket-Rak's Challenges to do that.");
                return false;
            }
            String other = Pets.metamorphosis(pet);
            if (other == null || Inv.remove(p, pet, 1) <= 0) {
                return false;
            }
            Inv.give(p, other, 1);
            Msg.game(p, "Your " + Pets.get(pet).name() + " changes into " + Pets.get(other).name() + ".");
            return true;
        }
        Msg.game(p, "You need to carry your TzRek-Jad or Jal-nib-rek to change its form (pick your follower up first).");
        return false;
    }
}
