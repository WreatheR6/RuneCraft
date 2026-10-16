package com.runecraft.skill;

import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.data.RuneAltar;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

import java.util.Locale;

/** Crafting runes at an altar: every essence in the inventory is bound at once. */
public final class Runecrafting {
    /** Whether Magic Imbue is active (combination runes need no talisman); set by the systems module. */
    public static java.util.function.Predicate<net.minecraft.server.network.ServerPlayerEntity> imbued = p -> false;

    private Runecrafting() {
    }

    public static void craft(ServerPlayerEntity player, RuneAltar altar, BlockPos pos) {
        int level = Players.data(player).skills().currentLevel(Skill.RUNECRAFT);
        if (level < altar.level()) {
            Msg.game(player, "You need a Runecraft level of " + altar.level() + " to infuse these runes.");
            return;
        }
        // Daeyalt essence crafts like pure essence for 50% more XP and is used first, in its own craft (OSRS Wiki).
        int daeyalt = Inv.count(player, "daeyalt_essence");
        if (daeyalt > 0) {
            Inv.remove(player, "daeyalt_essence", daeyalt);
            int runes = daeyalt * altar.runesPerEssence(level);
            Inv.give(player, altar.rune().itemId(), runes);
            CollectionLog.record(player, altar.rune().itemId());
            com.runecraft.content.GameEvents.fireObtain(player, altar.rune().itemId(), runes);
            XpService.add(player, Skill.RUNECRAFT,
                    com.runecraft.osrs.content.skilling.RunecraftData.essenceXp(altar.xp(), daeyalt, true));
            Msg.game(player, "You bind the temple's power into " + altar.rune().name().toLowerCase(Locale.ROOT)
                    + " runes, using your daeyalt essence first.");
            ServerWorld world = (ServerWorld) player.getWorld();
            world.spawnParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
            return;
        }
        int pure = Inv.count(player, "pure_essence");
        int normal = altar.pureOnly() ? 0 : Inv.count(player, "rune_essence");
        int essence = pure + normal;
        if (essence == 0) {
            Msg.game(player, altar.pureOnly() && Inv.count(player, "rune_essence") > 0
                    ? "This altar requires pure essence." : "You do not have any essence to bind.");
            return;
        }
        Inv.remove(player, "pure_essence", pure);
        Inv.remove(player, "rune_essence", normal);
        int runes = essence * altar.runesPerEssence(level);
        Inv.give(player, altar.rune().itemId(), runes);
        // The whole trip's runes in one obtain event (diary tasks such as "craft 140 runes at once").
        CollectionLog.record(player, altar.rune().itemId());
        com.runecraft.content.GameEvents.fireObtain(player, altar.rune().itemId(), runes);
        XpService.add(player, Skill.RUNECRAFT, altar.xp() * essence);
        Msg.game(player, "You bind the temple's power into " + altar.rune().name().toLowerCase(Locale.ROOT) + " runes.");
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.WITCH, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.1);
        world.playSound(null, pos, SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }
}
