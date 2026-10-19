package com.runecraft.content.systems;

import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.function.BiConsumer;

/**
 * The enchanted lyre's charges (OSRS Wiki): after The Fremennik Trials the Fossegrimen blesses an empty lyre for an offering
 * of a raw shark (2 charges), manta ray (3) or sea turtle (4) - a raw bass (2) with a ring of charos (a) worn - plus one
 * with the easy Fremennik Diary ({@link Charges#lyreCharges}). A charged lyre can't be topped up. A player without a lyre
 * gets a freshly blessed one (approx: OSRS needs a lyre made from the swaying tree's branch, which isn't modelled).
 */
final class LyreService {
    static final String FOSSEGRIMEN = "fossegrimen";
    static final String TRIALS = "fremennik_trials";
    /** Offerings in the order she takes them when the player carries several. */
    private static final String[] FISH = {"raw_sea_turtle", "raw_manta_ray", "raw_shark", "raw_bass"};

    private LyreService() {
    }

    /** Wraps the Fossegrimen's quest dialogue (call after the quests module registered it). */
    static void register() {
        BiConsumer<ServerPlayerEntity, QuestNpcEntity> earlier = QuestScripts.handler(FOSSEGRIMEN);
        QuestScripts.onTalk(FOSSEGRIMEN, (p, npc) -> {
            if (!offer(p) && earlier != null) {
                earlier.accept(p, npc);
            }
        });
    }

    /** Handles a talk to the Fossegrimen after The Fremennik Trials; false leaves it to the quest dialogue. */
    static boolean offer(ServerPlayerEntity p) {
        if (ContentRegistry.quest(TRIALS) == null || !QuestApi.isComplete(p, TRIALS)) {
            return false;
        }
        ItemStack lyre = null;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack s = p.getInventory().getStack(i);
            if (Charges.ENCHANTED_LYRE.equals(Inv.idOf(s))) {
                lyre = s;
                break;
            }
        }
        if (lyre != null && TeleportCharges.get(p, lyre) > 0) {
            QuestApi.say(p, "Fossegrimen", "Your lyre still sings with my blessing. Come back when its song is spent.");
            return true;
        }
        SysState.SysPlayer sp = SysState.of(p);
        boolean charos = "ring_of_charos_a".equals(Inv.idOf(sp.worn(Jewellery.Slot.RING)));
        boolean easy = Diaries.tierComplete(sp.diaryDone, "fremennik", Diaries.Tier.EASY);
        for (String fish : FISH) {
            int charges = Charges.lyreCharges(fish, charos, easy);
            if (charges <= 0 || !Inv.has(p, fish, 1)) {
                continue;
            }
            Inv.remove(p, fish, 1);
            if (lyre == null) {
                ItemStack fresh = Inv.stack(Charges.ENCHANTED_LYRE, 1);
                TeleportCharges.set(p, fresh, charges);
                Inv.give(p, fresh);
            } else {
                TeleportCharges.set(p, lyre, charges);
            }
            QuestApi.say(p, "Fossegrimen", "I accept your offering. Play your lyre and it shall carry you home to Rellekka.");
            Msg.game(p, "Your enchanted lyre now holds " + charges + " charge" + (charges == 1 ? "" : "s") + ".");
            return true;
        }
        QuestApi.say(p, "Fossegrimen", "Bring me an offering - a raw shark, manta ray or sea turtle - and I shall bless your lyre.");
        return true;
    }
}
