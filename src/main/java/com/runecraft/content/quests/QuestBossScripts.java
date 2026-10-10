package com.runecraft.content.quests;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Mechanics for quest bosses that AttackDefs alone can't express. */
final class QuestBossScripts {
    private QuestBossScripts() {
    }

    static void init() {
        BossScripts.register("dagannoth_mother", () -> new ColourShift("The Dagannoth Mother"));
        BossScripts.register("gelatinnoth_mother", () -> new ColourShift("The Gelatinnoth Mother"));
        BossScripts.register("fareed", Fareed::new);
    }

    static void announce(DataBossEntity boss, String message, Formatting colour) {
        if (boss.getWorld() instanceof ServerWorld world) {
            for (ServerPlayerEntity p : world.getPlayers(x -> x.squaredDistanceTo(boss) < 40 * 40)) {
                p.sendMessage(Text.literal(message).formatted(colour), false);
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
            announce(boss, name + " is " + COLOURS[colour] + ": vulnerable to " + hint() + ".", Formatting.AQUA);
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (gameTick % 12 == 0) {
                int next = boss.getRandom().nextInt(COLOURS.length - 1);
                colour = next >= colour ? next + 1 : next;
                announce(boss, name + " turns " + COLOURS[colour] + "! (vulnerable to " + hint() + ")", Formatting.AQUA);
            }
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (weakTo(type)) {
                return damage;
            }
            long now = boss.getWorld().getTime();
            if (now - lastImmuneMessage > 40) {
                lastImmuneMessage = now;
                Msg.action(attacker, name + " is " + COLOURS[colour] + " and shrugs off your attack. Use " + hint() + ".");
            }
            return 0;
        }
    }

    /** Desert Treasure: Fareed's flames halve melee damage unless the attacker carries ice gloves. */
    static final class Fareed implements BossScript {
        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayerEntity attacker, int damage, AttackType type) {
            if (type.isMelee() && !Inv.has(attacker, "ice_gloves", 1)) {
                if (boss.getRandom().nextInt(4) == 0) {
                    Msg.action(attacker, "Fareed's flames scorch your hands! Ice gloves would protect you.");
                }
                return damage / 2;
            }
            return damage;
        }
    }
}
