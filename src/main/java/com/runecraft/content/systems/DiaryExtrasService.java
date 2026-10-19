package com.runecraft.content.systems;

import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.item.ItemCharges;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.Charges;
import com.runecraft.osrs.content.systems.DiaryExtras;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * NPC services and objects behind diary tasks: Tindel Marchant's rusty sword repair, the genie of Spirits of the Elid,
 * the muddy and crystal chests, item-on-item joins (crystal key, skull sceptre) and the skull sceptre teleport.
 */
final class DiaryExtrasService {
    private DiaryExtrasService() {
    }

    static void register() {
        GameEvents.onActivity((p, key) -> {
            if (key.equals("talk:" + DiaryExtras.TINDEL)) {
                tindel(p);
            } else if (key.equals("talk:" + DiaryExtras.GENIE)) {
                genie(p);
            }
        });
        BlockHooks.register(DiaryExtras.MUDDY_CHEST, (p, pos, state) -> chest(p, "muddy_key", "muddy_chest",
                DiaryExtras.muddyChest(p.getRandom()::nextDouble)));
        BlockHooks.register(DiaryExtras.CRYSTAL_CHEST, (p, pos, state) -> chest(p, "crystal_key", "crystal_chest",
                DiaryExtras.crystalChest(p.getRandom()::nextDouble)));
        DiaryObjectsService.register();
    }

    private static boolean questDone(ServerPlayer p, String questId) {
        QuestInfo q = ContentRegistry.quest(questId);
        return q == null || Players.data(p).questComplete(q);
    }

    /** Tindel Marchant: 100 coins to "identify" (repair) a rusty sword; 25 Smithing XP when it comes back a weapon (wiki). */
    private static void tindel(ServerPlayer p) {
        if (!Inv.has(p, "rusty_sword", 1)) {
            QuestApi.say(p, "Tindel Marchant", "Bring me a rusty sword and 100 coins and I'll see what I can make of it.");
            return;
        }
        if (!Inv.has(p, "coins", DiaryExtras.TINDEL_FEE)) {
            QuestApi.say(p, "Tindel Marchant", "It'll cost you 100 coins for me to look at that sword.");
            return;
        }
        Inv.remove(p, "rusty_sword", 1);
        Inv.remove(p, "coins", DiaryExtras.TINDEL_FEE);
        DiaryExtras.Loot result = DiaryExtras.tindel(p.getRandom()::nextDouble);
        if (result == null) {
            QuestApi.say(p, "Tindel Marchant", "Oh dear, it crumbled to pieces in my hands. Sorry about that.");
            return;
        }
        if (!DiaryExtras.identified(result)) {
            QuestApi.say(p, "Tindel Marchant", "Why, this is an ancient artefact! I'll add it to my collection. Here's something for your trouble.");
            Inv.give(p, "coins", result.count());
            return;
        }
        Inv.give(p, result.item(), result.count());
        com.runecraft.skill.XpService.add(p, com.runecraft.osrs.Skill.SMITHING, 25);
        String name = ItemCatalog.get(result.item()) == null ? result.item() : ItemCatalog.get(result.item()).name().toLowerCase();
        QuestApi.say(p, "Tindel Marchant", "There you go: it was " + Msg.article(name) + " under all that rust.");
        GameEvents.fireActivity(p, "tindel_identify");
    }

    /** The genie in the crevice near Nardah; visiting him after Spirits of the Elid is the medium Desert task. */
    private static void genie(ServerPlayer p) {
        if (!questDone(p, "spirits_of_the_elid")) {
            QuestApi.say(p, "Genie", "Who dares disturb my slumber? Begone, mortal!");
            return;
        }
        QuestApi.say(p, "Genie", "Ah, my friend who freed the Elid! Nardah is grateful, and so am I.");
        GameEvents.fireActivity(p, "genie_visit");
    }

    private static void chest(ServerPlayer p, String key, String chestId, List<DiaryExtras.Loot> loot) {
        if (!Inv.has(p, key, 1)) {
            Msg.game(p, "This chest is securely locked shut.");
            return;
        }
        Inv.remove(p, key, 1);
        Msg.game(p, "You unlock the chest with your key.");
        for (DiaryExtras.Loot l : loot) {
            if (ItemCatalog.get(l.item()) == null && !l.item().equals("coins")) {
                continue; // not in RuneCraft
            }
            Inv.give(p, l.item(), l.count());
            com.runecraft.collection.CollectionLog.record(p, l.item());
        }
        Msg.action(p, "You find some treasure in the chest!");
        GameEvents.fireActivity(p, "chest_open:" + chestId);
    }

    /** Held items this service uses on right-click (the client predicts the result). */
    static boolean handles(String id, boolean sneaking) {
        return DiaryExtras.join(id) != null || (id.equals(Charges.SKULL_SCEPTRE) && sneaking);
    }

    /** Right-click: join two halves ("use A on B"), or sneak + right-click the skull sceptre to Invoke its teleport. */
    static boolean use(ServerPlayer p, ItemStack stack, InteractionHand hand, String id, boolean sneaking) {
        if (id.equals(Charges.SKULL_SCEPTRE) && sneaking) {
            int left = ItemCharges.get(stack);
            if (left <= 0) {
                Msg.game(p, "Your sceptre has no charges left.");
                return true;
            }
            if (SysTeleport.teleport(p, Teleports.SKULL_SCEPTRE, SysTeleport.SPELL_WILDERNESS)) {
                GameEvents.fireActivity(p, "teleport:skull_sceptre:barbarian_village");
                ItemCharges.drain(stack, 1);
                left = ItemCharges.get(stack);
                if (left <= 0) {
                    stack.shrink(1);
                    Msg.game(p, "Your skull sceptre crumbles to dust.");
                } else {
                    Msg.game(p, "Your sceptre has " + left + " charge" + (left == 1 ? "" : "s") + " left.");
                }
            }
            return true;
        }
        String[] join = DiaryExtras.join(id);
        if (join == null) {
            return false;
        }
        String other = join[0].equals(id) ? join[1] : join[0];
        if (!Inv.has(p, other, 1)) {
            Msg.game(p, "You need " + Msg.article(ItemCatalog.get(other) == null ? other.replace('_', ' ')
                    : ItemCatalog.get(other).name().toLowerCase()) + " to use with this.");
            return true;
        }
        Inv.remove(p, join[0], 1);
        Inv.remove(p, join[1], 1);
        if (join[2].equals(Charges.SKULL_SCEPTRE)) {
            // Made with the most charges the player's Varrock Diary allows (10, then 14/18/22/26 per tier).
            int tiers = 0;
            for (com.runecraft.osrs.content.systems.Diaries.Tier t : com.runecraft.osrs.content.systems.Diaries.Tier.values()) {
                if (com.runecraft.osrs.content.systems.Diaries.tierComplete(SysState.of(p).diaryDone, "varrock", t)) {
                    tiers++;
                }
            }
            ItemStack sceptre = Inv.stack(Charges.SKULL_SCEPTRE, 1);
            com.runecraft.item.ItemCharges.set(sceptre, Charges.skullSceptreCap(tiers));
            Inv.give(p, sceptre);
        } else {
            Inv.give(p, join[2], 1);
        }
        Msg.action(p, join[2].equals("crystal_key") ? "You join the two halves of the key together."
                : "You fit the pieces together to make " + Msg.article(ItemCatalog.get(join[2]).name().toLowerCase()) + ".");
        GameEvents.fireActivity(p, "join:" + join[2]);
        return true;
    }
}
