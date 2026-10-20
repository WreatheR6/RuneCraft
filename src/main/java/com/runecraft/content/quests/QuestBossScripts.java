package com.runecraft.content.quests;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Mechanics for quest bosses that AttackDefs alone can't express. */
final class QuestBossScripts {
    private QuestBossScripts() {
    }

    static void init() {
        BossScripts.register("dagannoth_mother", () -> new ColourShift("The Dagannoth Mother"));
        BossScripts.register("gelatinnoth_mother", () -> new ColourShift("The Gelatinnoth Mother"));
        BossScripts.register("fareed", Fareed::new);
        BossScripts.register("head_menaphite_guard", FalseGods::new);
    }

    /**
     * Beneath Cursed Sands: the Head Menaphite Guard shouts "Your false gods will not protect you!" at a player using an overhead
     * protection prayer and hits them for a third of their Hitpoints level (wiki).
     */
    static final class FalseGods implements BossScript {
        private static final com.runecraft.osrs.Prayer[] OVERHEADS = {com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE,
                com.runecraft.osrs.Prayer.PROTECT_FROM_MISSILES, com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC};

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(player);
            for (com.runecraft.osrs.Prayer prayer : OVERHEADS) {
                if (data.isActive(prayer)) {
                    announce(boss, "Head Menaphite Guard: Your false gods will not protect you!", ChatFormatting.RED);
                    return Math.max(damage, data.skills().level(com.runecraft.osrs.Skill.HITPOINTS) / 3);
                }
            }
            return damage;
        }
    }

    static void announce(DataBossEntity boss, String message, ChatFormatting colour) {
        if (boss.level() instanceof ServerLevel world) {
            for (ServerPlayer p : world.getPlayers(x -> x.distanceToSqr(boss) < 40 * 40)) {
                p.displayClientMessage(Component.literal(message).withStyle(colour), false);
            }
        }
    }

    /**
     * Horror from the Deep / RFD: the mother changes colour every few seconds and can only be hurt by
     * the matching style: white = air, blue = water, brown = earth, red = fire spells (any magic here),
     * green = ranged, orange = melee.
     */
    static final class ColourShift implements BossScript {
        private static final String[] COLOURS = {"white", "blue", "brown", "red", "green", "orange"};
        private final String name;
        private int colour;
        private long lastImmuneMessage;

        ColourShift(String name) {
            this.name = name;
        }

        private boolean weakTo(AttackType type) {
            return switch (COLOURS[colour]) {
                case "green" -> type == AttackType.RANGED;
                case "orange" -> type.isMelee();
                default -> type == AttackType.MAGIC;
            };
        }

        private String hint() {
            return switch (COLOURS[colour]) {
                case "white" -> "air spells";
                case "blue" -> "water spells";
                case "brown" -> "earth spells";
                case "red" -> "fire spells";
                case "green" -> "ranged attacks";
                default -> "melee attacks";
            };
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            colour = boss.getRandom().nextInt(COLOURS.length);
            announce(boss, name + " is " + COLOURS[colour] + ": vulnerable to " + hint() + ".", ChatFormatting.AQUA);
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (gameTick % 12 == 0) {
                int next = boss.getRandom().nextInt(COLOURS.length - 1);
                colour = next >= colour ? next + 1 : next;
                announce(boss, name + " turns " + COLOURS[colour] + "! (vulnerable to " + hint() + ")", ChatFormatting.AQUA);
            }
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (weakTo(type)) {
                return damage;
            }
            long now = boss.level().getGameTime();
            if (now - lastImmuneMessage > 40) {
                lastImmuneMessage = now;
                Msg.action(attacker, name + " is " + COLOURS[colour] + " and shrugs off your attack. Use " + hint() + ".");
            }
            return 0;
        }
    }

    /**
     * Desert Treasure: Fareed disarms anyone who hits him in melee without ice gloves, so melee does nothing
     * without them; ranged and magic (water spells in OSRS: any spell here, approx) work from a distance.
     */
    static final class Fareed implements BossScript {
        private long lastMessage;

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (type.isMelee() && !Inv.has(attacker, "ice_gloves", 1)) {
                long now = boss.level().getGameTime();
                if (now - lastMessage > 40) {
                    lastMessage = now;
                    Msg.action(attacker, "Fareed's flames scorch your hands and you can't keep hold of your weapon! Ice gloves would protect you.");
                }
                return 0;
            }
            return damage;
        }
    }
}
