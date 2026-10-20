package com.runecraft.content.quests;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.quests.BalanceElemental;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;

/** Mechanics for quest bosses that AttackDefs alone can't express. */
final class QuestBossScripts {
    private QuestBossScripts() {
    }

    static void init() {
        BossScripts.register("dagannoth_mother", () -> new ColourShift("The Dagannoth Mother"));
        BossScripts.register("gelatinnoth_mother", () -> new ColourShift("The Gelatinnoth Mother"));
        BossScripts.register("fareed", Fareed::new);
        BossScripts.register("head_menaphite_guard", FalseGods::new);
        BossScripts.register("dark_squall", MagicOnly::new);
        BossScripts.register("balance_elemental", BalanceForms::new);
        BossScripts.register("fragment_of_seren", FragmentSpecials::new);
        BossScripts.register("vanstrom_klause", Vanstrom::new);
    }

    /**
     * While Guthix Sleeps: the Balance Elemental fights four attacks at a time in each form, and below 550 / 200 HP it
     * quickens and protects against one / two styles ({@link BalanceElemental}, wiki).
     */
    static final class BalanceForms implements BossScript {
        private int form = -1;
        private int attacks;
        private long lastMessage;

        private static int hp(DataBossEntity boss) {
            return (int) Math.ceil(boss.getHealth() * boss.trueHitpoints() / boss.getMaxHealth());
        }

        private void changeForm(DataBossEntity boss) {
            int next = boss.getRandom().nextInt(form < 0 ? 3 : 2);
            form = form >= 0 && next >= form ? next + 1 : next;
            attacks = 0;
            announce(boss, "The Balance Elemental turns " + BalanceElemental.FORM_NAMES[form] + "! (" + BalanceElemental.styleName(form)
                    + " attacks)", ChatFormatting.AQUA);
        }

        @Override
        public void onSpawn(DataBossEntity boss) {
            changeForm(boss);
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (form < 0) {
                changeForm(boss); // reloaded: scripts are not saved
            }
            int speed = BalanceElemental.attackSpeed(hp(boss));
            if (boss.stats() != null && boss.stats().attackSpeed() != speed) {
                boss.overrideStats(s -> new com.runecraft.combat.MonsterStats(s.name(), s.combatLevel(), s.hitpoints(), s.attack(),
                        s.strength(), s.defence(), s.magic(), s.ranged(), s.defenceBonus(), speed, s.style(), s.maxHit(), s.monster(),
                        s.profile(), s.boss()));
            }
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (form < 0 || boss.def() == null) {
                return null;
            }
            for (AttackDef a : boss.def().attacks) {
                if (a.name().equals(BalanceElemental.FORM_ATTACKS[form])) {
                    return a;
                }
            }
            return null;
        }

        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (form >= 0 && ++attacks >= BalanceElemental.ATTACKS_PER_FORM) {
                changeForm(boss); // the next attack comes in the new form
            }
            return attack;
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            int hp = hp(boss);
            if (form < 0 || !BalanceElemental.protects(form, BalanceElemental.family(type), hp)) {
                return damage;
            }
            long now = boss.level().getGameTime();
            if (now - lastMessage > 40) {
                lastMessage = now;
                Msg.action(attacker, hp < BalanceElemental.TWO_PRAYERS_BELOW
                        ? "The " + BalanceElemental.FORM_NAMES[form] + " Balance Elemental is protected from that. Only "
                                + BalanceElemental.styleName(BalanceElemental.vulnerableStyle(form)) + " will hurt it now."
                        : "The " + BalanceElemental.FORM_NAMES[form] + " Balance Elemental is praying against "
                                + BalanceElemental.styleName(BalanceElemental.prayedStyle(form)) + ".");
            }
            return 0;
        }
    }

    /** While Guthix Sleeps: Surok Magis, the Dark Squall, can only be hurt with magic (wiki). */
    /**
     * Song of the Elves, the Fragment of Seren (wiki): about three normal attacks (crystal volleys at range, a swipe up close), then
     * one of her four specials, always in this order: she pulls the player to her, splits into shadows, summons healing beams, and
     * pins the player for an unblockable hit of their maximum hitpoints minus 5 (above two thirds of her health), minus 3, then
     * minus 1 (below a third).
     */
    static final class FragmentSpecials implements BossScript {
        private static final String[] SPECIALS = {"pull", "shadows", "healing_beams", "crystal_judgement"};
        private int attacks;
        private int special;
        private String last;

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss.def() == null) {
                return null;
            }
            attacks++;
            String name;
            if (attacks % 4 == 0) {
                name = SPECIALS[special];
                special = (special + 1) % SPECIALS.length;
            } else {
                name = distance <= 2.5 ? "swipe" : "crystal_volley";
            }
            last = name;
            for (AttackDef a : boss.def().attacks) {
                if (a.name().equals(name)) {
                    return a;
                }
            }
            return null;
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (!"crystal_judgement".equals(last)) {
                return damage;
            }
            float left = boss.getHealth() / boss.getMaxHealth();
            int spare = left > 2 / 3f ? 5 : left > 1 / 3f ? 3 : 1;
            int max = com.runecraft.player.Players.data(player).skills().level(com.runecraft.osrs.Skill.HITPOINTS);
            return Math.max(1, max - spare);
        }
    }
    /**
     * Sins of the Father, Vanstrom Klause (wiki): magic attacks, some lowering Attack; every 8-10 attacks one of his three
     * specials, never the same twice in a row. Brought to 0 hitpoints he heals 200, stops his specials and calls lightning.
     */
    static final class Vanstrom implements BossScript {
        private static final String[] SPECIALS = {"pets", "blood_orb", "stare_into_darkness"};
        private int sinceSpecial;
        private int nextSpecial = 8;
        private String lastSpecial;
        private boolean healed;

        private AttackDef named(DataBossEntity boss, String name) {
            for (AttackDef a : boss.def().attacks) {
                if (a.name().equals(name)) {
                    return a;
                }
            }
            return null;
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss.def() == null) {
                return null;
            }
            if (healed) {
                return named(boss, boss.getRandom().nextInt(3) == 0 ? "lightning" : "blood_blast");
            }
            if (++sinceSpecial >= nextSpecial) {
                sinceSpecial = 0;
                nextSpecial = 8 + boss.getRandom().nextInt(3);
                String pick;
                do {
                    pick = SPECIALS[boss.getRandom().nextInt(SPECIALS.length)];
                } while (pick.equals(lastSpecial));
                lastSpecial = pick;
                return named(boss, pick);
            }
            return named(boss, boss.getRandom().nextInt(4) == 0 ? "weaken" : "blood_blast");
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            int hp = (int) Math.ceil(boss.getHealth() * boss.trueHitpoints() / boss.getMaxHealth());
            if (!healed && damage >= hp) {
                healed = true;
                boss.setHealth(boss.getMaxHealth() * 200f / boss.trueHitpoints());
                announce(boss, "Vanstrom Klause drinks deep of the blood around him and rises again!", ChatFormatting.DARK_RED);
                return 0;
            }
            return damage;
        }
    }

    static final class MagicOnly implements BossScript {
        private long lastMessage;
        private int attacks;

        /** Wiki: Surok fights like Vorkath, six regular attacks and then one of his specials as the seventh. */
        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss.def() == null || boss.def().attacks.isEmpty()) {
                return null;
            }
            List<AttackDef> all = boss.def().attacks;
            attacks++;
            if (attacks % 7 != 0) {
                return all.get(0);
            }
            List<AttackDef> specials = all.subList(1, all.size());
            return specials.isEmpty() ? all.get(0) : specials.get(boss.getRandom().nextInt(specials.size()));
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (type == AttackType.MAGIC) {
                return damage;
            }
            long now = boss.level().getGameTime();
            if (now - lastMessage > 40) {
                lastMessage = now;
                Msg.action(attacker, "Surok's wards turn your blow aside. Only magic will hurt him.");
            }
            return 0;
        }
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
