package com.runecraft.skill;

import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.world.Gielinor;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Grants experience, announces level-ups and keeps derived stats (max HP) in sync. */
public final class XpService {
    private static final UUID HITPOINTS_MODIFIER = UUID.fromString("5d2a7c3e-6b8e-4f63-9a51-0e1c2b3a4d5f");
    /** Gates that can withhold experience (registered by modules, e.g. Nightmare Zone practice dreams give none). */
    private static final List<BiPredicate<ServerPlayer, Skill>> GATES = new CopyOnWriteArrayList<>();

    private XpService() {
    }

    /** Registers a gate: experience in a skill is only given while every gate returns true. */
    public static void addGate(BiPredicate<ServerPlayer, Skill> gate) {
        GATES.add(gate);
    }

    /** Whether the player may gain experience in the skill right now. */
    public static boolean allowed(ServerPlayer player, Skill skill) {
        for (BiPredicate<ServerPlayer, Skill> gate : GATES) {
            if (!gate.test(player, skill)) {
                return false;
            }
        }
        return true;
    }

    public static void add(ServerPlayer player, Skill skill, double xp) {
        addTenths(player, skill, Experience.tenths(xp));
    }

    public static void addTenths(ServerPlayer player, Skill skill, int tenths) {
        if (tenths <= 0 || !allowed(player, skill)) {
            return;
        }
        PlayerData data = Players.data(player);
        SkillSet skills = data.skills();
        int oldCombat = skills.combatLevel();
        int gained = skills.addXp(skill, tenths);
        ServerNetworking.xpDrop(player, skill, tenths);
        com.runecraft.content.GameEvents.fireXp(player, skill, tenths);
        if (gained > 0) {
            onLevelUp(player, skill, skills.level(skill), gained);
            int newCombat = skills.combatLevel();
            if (newCombat > oldCombat) {
                player.displayClientMessage(Component.literal("Congratulations, your combat level is now " + newCombat + ".")
                        .withStyle(ChatFormatting.DARK_RED), false);
            }
        }
        ServerNetworking.sync(player);
    }

    private static void onLevelUp(ServerPlayer player, Skill skill, int level, int gained) {
        String article = skill == Skill.ATTACK || skill == Skill.AGILITY ? "an" : "a";
        player.displayClientMessage(Component.literal("Congratulations, you've just advanced " + (gained == 1 ? article : gained)
                + " " + skill.displayName() + " level" + (gained > 1 ? "s" : "") + ". You are now level " + level + ".")
                .withStyle(ChatFormatting.DARK_BLUE), false);
        if (player.level() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.15);
            world.playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        if (level == Experience.MAX_LEVEL) {
            player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()
                    + " has reached 99 " + skill.displayName() + "!").withStyle(ChatFormatting.GOLD), false);
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
    public static void updateMaxHealth(ServerPlayer player) {
        AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
        if (health == null) {
            return;
        }
        health.removeModifier(HITPOINTS_MODIFIER);
        if (Gielinor.is(player)) {
            int hp = Players.data(player).skills().level(Skill.HITPOINTS);
            double delta = hp - health.getBaseValue();
            if (delta != 0) {
                health.addTransientModifier(new AttributeModifier(HITPOINTS_MODIFIER, "RuneCraft Hitpoints",
                        delta, AttributeModifier.Operation.ADDITION));
            }
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    public static void setLevel(ServerPlayer player, Skill skill, int level) {
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
