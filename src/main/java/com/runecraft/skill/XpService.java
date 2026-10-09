package com.runecraft.skill;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.world.Gielinor;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/** Grants experience, announces level-ups and keeps derived stats (max HP) in sync. */
public final class XpService {
    private static final UUID HITPOINTS_MODIFIER = UUID.fromString("5d2a7c3e-6b8e-4f63-9a51-0e1c2b3a4d5f");

    private XpService() {
    }

    public static void add(ServerPlayerEntity player, Skill skill, double xp) {
        addTenths(player, skill, Experience.tenths(xp));
    }

    public static void addTenths(ServerPlayerEntity player, Skill skill, int tenths) {
        if (tenths <= 0) {
            return;
        }
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int oldCombat = skills.combatLevel();
        int gained = skills.addXp(skill, tenths);
        ServerNetworking.xpDrop(player, skill, tenths);
        if (gained > 0) {
            onLevelUp(player, skill, skills.level(skill), gained);
            int newCombat = skills.combatLevel();
            if (newCombat > oldCombat) {
                player.sendMessage(Text.literal("Congratulations, your combat level is now " + newCombat + ".")
                        .formatted(Formatting.DARK_RED), false);
            }
        }
        ServerNetworking.sync(player);
    }

    private static void onLevelUp(ServerPlayerEntity player, Skill skill, int level, int gained) {
        String article = skill == Skill.ATTACK || skill == Skill.AGILITY ? "an" : "a";
        player.sendMessage(Text.literal("Congratulations, you've just advanced " + (gained == 1 ? article : gained)
                + " " + skill.displayName() + " level" + (gained > 1 ? "s" : "") + ". You are now level " + level + ".")
                .formatted(Formatting.DARK_BLUE), false);
        if (player.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.15);
            world.playSound(null, player.getBlockPos(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 0.6f, 1.2f);
        }
        if (level == Experience.MAX_LEVEL) {
            player.getServer().getPlayerManager().broadcast(Text.literal(player.getName().getString()
                    + " has reached 99 " + skill.displayName() + "!").formatted(Formatting.GOLD), false);
        }
        if (skill == Skill.HITPOINTS) {
            updateMaxHealth(player);
            player.heal(gained);
        }
        if (skill == Skill.PRAYER) {
            PlayerData data = Players.data(player);
            data.setPrayerPoints(data.prayerPoints() + gained);
        }
    }

    /** In Gielinor, max health equals your Hitpoints level; elsewhere it's vanilla (20). */
    public static void updateMaxHealth(ServerPlayerEntity player) {
        EntityAttributeInstance health = player.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(HITPOINTS_MODIFIER);
        if (Gielinor.is(player)) {
            int hp = Players.data(player).skills().level(Skill.HITPOINTS);
            double delta = hp - health.getBaseValue();
            if (delta != 0) {
                health.addTemporaryModifier(new EntityAttributeModifier(HITPOINTS_MODIFIER, "RuneCraft Hitpoints",
                        delta, EntityAttributeModifier.Operation.ADDITION));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void setLevel(ServerPlayerEntity player, Skill skill, int level) {
        PlayerData data = Players.data(player);
        data.skills().setXpTenths(skill, Experience.xpForLevel(level) * 10);
        if (skill == Skill.HITPOINTS) {
            updateMaxHealth(player);
        }
        if (skill == Skill.PRAYER) {
            data.setPrayerPoints(data.skills().level(Skill.PRAYER));
        }
        ServerNetworking.sync(player);
    }
}
