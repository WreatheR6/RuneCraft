package com.runecraft.content.skilling;

import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.skilling.FarmingContracts;
import com.runecraft.osrs.content.skilling.FarmingContracts.Contract;
import com.runecraft.osrs.content.skilling.FarmingContracts.Tier;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

/**
 * Farming contracts (Guildmaster Jane, Farming Guild) and seed packs. A player's contract is kept in
 * {@link SkillingState} ("contracts", by UUID). Talking to Jane asks for the hardest contract the
 * player can take (sneak-talk for an easier tier); harvesting the crop anywhere completes it, and
 * talking to Jane again pays a seed pack whose tier is saved in the pack's data.
 */
public final class FarmingContractService {
    static final String TIER_KEY = "SeedPackTier";

    private FarmingContractService() {
    }

    public static void talk(ServerPlayer player, String name) {
        SkillingState state = SkillingState.get(player.getServer());
        CompoundTag nbt = state.entry("contracts", player.getStringUUID());
        int farming = SkillingUtil.level(player, Skill.FARMING);
        Tier best = FarmingContracts.bestTier(farming);
        if (best == null) {
            QuestApi.say(player, name, "Come back when you have level " + Tier.EASY.level + " Farming and I'll give you a contract.");
            return;
        }
        Tier saved = nbt.contains("tier") ? Tier.values()[Math.max(0, Math.min(Tier.values().length - 1, nbt.getInt("tier")))] : null;
        Contract current = nbt.contains("crop") ? FarmingContracts.byCrop(nbt.getString("crop"), saved) : null;
        if (current != null && nbt.getBoolean("done")) {
            ItemStack pack = Inv.stack("seed_pack", 1);
            if (!pack.isEmpty()) {
                pack.getOrCreateTag().putInt(TIER_KEY, FarmingContracts.packTier(current));
                Inv.give(player, pack);
            }
            nbt.remove("crop");
            nbt.putBoolean("done", false);
            nbt.putInt("completed", nbt.getInt("completed") + 1);
            QuestApi.say(player, name, "Splendid, you've grown the " + SkillingUtil.lower(current.name())
                    + ". Here's a seed pack for your trouble. Talk to me again for a new contract.");
            return;
        }
        if (current != null && !player.isShiftKeyDown()) {
            QuestApi.say(player, name, "Your contract is to grow " + SkillingUtil.lower(current.name())
                    + ". Harvest it from any patch and come back to me. (Sneak-talk to swap it for an easier contract.)");
            return;
        }
        Tier tier = best;
        if (player.isShiftKeyDown() && current != null) {
            int t = current.tier().ordinal() - 1;
            tier = t < 0 ? best : Tier.values()[Math.min(t, best.ordinal())];
        }
        RandomSource random = player.getRandom();
        Contract next = FarmingContracts.assign(tier, farming, current == null ? null : current.cropId(), random::nextInt);
        if (next == null) {
            QuestApi.say(player, name, "I don't have a contract for you at the moment.");
            return;
        }
        nbt.putString("crop", next.cropId());
        nbt.putInt("tier", next.tier().ordinal());
        nbt.putBoolean("done", false);
        QuestApi.say(player, name, "I'd like you to grow " + SkillingUtil.lower(next.name()) + " for me ("
                + SkillingUtil.lower(next.tier().name()) + " contract). Harvest it from any patch and come back.");
    }

    /** Called when a crop is harvested (or a tree is checked); completes a matching contract. */
    public static void harvested(ServerPlayer player, String cropId) {
        SkillingState state = SkillingState.get(player.getServer());
        if (!state.has("contracts", player.getStringUUID())) {
            return;
        }
        CompoundTag nbt = state.entry("contracts", player.getStringUUID());
        if (nbt.getBoolean("done") || !cropId.equals(nbt.getString("crop"))) {
            return;
        }
        nbt.putBoolean("done", true);
        Msg.game(player, "You've completed a farming contract. Speak to Guildmaster Jane for your reward.");
    }

    /** Opens a seed pack (tier from its data; 3, like the Tithe Farm's pack, if it has none). */
    static void open(ServerPlayer player, ItemStack stack) {
        int tier = stack.hasTag() && stack.getTag().contains(TIER_KEY) ? stack.getTag().getInt(TIER_KEY) : 3;
        stack.shrink(1);
        RandomSource random = player.getRandom();
        for (int t : FarmingContracts.rolls(tier, random::nextInt)) {
            SkillingUtil.roll(player, FarmingContracts.table(t), random);
        }
        Msg.action(player, "You open the seed pack.");
    }
}
