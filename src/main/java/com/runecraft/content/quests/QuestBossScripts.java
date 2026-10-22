package com.runecraft.content.quests;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.quests.BalanceElemental;
import com.runecraft.osrs.content.quests.QuestBossRules;
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
        BossScripts.register("gelatinnoth_mother", Gelatinnoth::new);
        BossScripts.register("agrith_na_na", AgrithNaNa::new);
        BossScripts.register("flambeed", Flambeed::new);
        BossScripts.register("karamel", Karamel::new);
        BossScripts.register("dessourt", Culinary::new);
        BossScripts.register("culinaromancer", Culinaromancer::new);
        BossScripts.register("fareed", Fareed::new);
        BossScripts.register("kamil", Kamil::new);
        BossScripts.register("damis", Damis::new);
        BossScripts.register("dessous", Dessous::new);
        BossScripts.register("jungle_demon", JungleDemon::new);
        BossScripts.register("elvarg", Elvarg::new);
        BossScripts.register("nezikchened", Nezikchened::new);
        BossScripts.register("treus_dayth", TreusDayth::new);
        BossScripts.register("quest_mother", MotherOfWeiss::new);
        BossScripts.register("the_inadequacy", Inadequacy::new);
        BossScripts.register("the_everlasting", DreamFear::new);
        BossScripts.register("the_untouchable", DreamFear::new);
        BossScripts.register("the_illusive", Illusive::new);
        BossScripts.register("judge_of_yama", JudgeOfYama::new);
        BossScripts.register("xamphur", Xamphur::new);
        BossScripts.register("warped_terrorbird", WarpedTerrorbird::new);
        BossScripts.register("champion_of_scabaras", ChampionOfScabaras::new);
        BossScripts.register("head_menaphite_guard", FalseGods::new);
        BossScripts.register("dark_squall", MagicOnly::new);
        BossScripts.register("balance_elemental", BalanceForms::new);
        BossScripts.register("wgs_tormented_demon", StoneDemon::new);
        com.runecraft.platform.ServerTickEvents.END_SERVER_TICK.register(StoneDemon::releaseStonePower);
        BossScripts.register("fragment_of_seren", FragmentSpecials::new);
        BossScripts.register("vanstrom_klause", Vanstrom::new);
        BossScripts.register("arrav", () -> new EveryNth(QuestBossRules.COA_ARRAV_AXE_EVERY, "axe_swing"));
        BossScripts.register("tfd_lucius", () -> new EveryNth(QuestBossRules.TFD_LUCIUS_SPECIAL_EVERY, "triple_projectile"));
        BossScripts.register("tfd_augur_metzli", () -> new EveryNth(QuestBossRules.TFD_METZLI_SPECIAL_EVERY, "sunfire_waves"));
        Vanstrom.register();
    }

    /**
     * Whether a player is in this boss's fight: close by and its target, or hit by it or hitting it in the last five seconds
     * (the vanilla last-hurt-by memory, which CombatHandler sets both ways). Fight-wide effects touch only these players.
     */
    static boolean engaged(DataBossEntity boss, ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator() || p.distanceToSqr(boss) > 24 * 24) {
            return false;
        }
        return boss.getTarget() == p || boss.getLastHurtByMob() == p || p.getLastHurtByMob() == boss;
    }

    /**
     * While Guthix Sleeps: the Balance Elemental fights four attacks at a time in each form, and below 550 / 200 HP it
     * quickens and protects against one / two styles ({@link BalanceElemental}, wiki).
     */
    static final class BalanceForms implements BossScript {
        private int form = -1;
        private int attacks;
        private long lastMessage;
        /** The next attack is the new form's stat-draining special (wiki: each form opens with it). */
        private boolean drainNext;

        private static int hp(DataBossEntity boss) {
            return (int) Math.ceil(boss.getHealth() * boss.trueHitpoints() / boss.getMaxHealth());
        }

        private void changeForm(DataBossEntity boss) {
            int next = boss.getRandom().nextInt(form < 0 ? 3 : 2);
            form = form >= 0 && next >= form ? next + 1 : next;
            attacks = 0;
            drainNext = true;
            announce(boss, "The Balance Elemental turns " + BalanceElemental.FORM_NAMES[form] + "! (" + BalanceElemental.styleName(form)
                    + " attacks: pray against " + BalanceElemental.styleName(form) + " before it drains you)", ChatFormatting.AQUA);
        }

        private static AttackDef named(DataBossEntity boss, String name) {
            for (AttackDef a : boss.def().attacks) {
                if (a.name().equals(name)) {
                    return a;
                }
            }
            return null;
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
            AttackDef drain = drainNext ? named(boss, BalanceElemental.FORM_DRAINS[form]) : null;
            return drain != null ? drain : named(boss, BalanceElemental.FORM_ATTACKS[form]);
        }

        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (attack != null && BalanceElemental.isDrain(attack.name())) {
                drainNext = false; // the draining special opens the form; the four normal attacks follow
                return attack;
            }
            if (form >= 0 && ++attacks >= BalanceElemental.ATTACKS_PER_FORM) {
                changeForm(boss); // the next attack is the new form's drain
            }
            return attack;
        }

        /**
         * The draining special (wiki): unless the matching protection prayer is up when it lands, it lowers every combat stat and
         * a large chunk of Prayer. The damage itself already went through the prayer (up to 89, blocked when protected).
         */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type, AttackDef attack) {
            if (attack == null || !BalanceElemental.isDrain(attack.name())) {
                return damage;
            }
            com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(player);
            if (com.runecraft.combat.CombatHandler.isProtected(data, type)) {
                Msg.game(player, "Your prayer shields you from the Balance Elemental's drain.");
                return damage;
            }
            drain(player);
            return damage;
        }

        /** Lowers Attack, Strength, Defence, Ranged and Magic and takes Prayer points ({@link BalanceElemental#statDrain}). */
        static void drain(ServerPlayer player) {
            com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(player);
            for (com.runecraft.osrs.Skill s : new com.runecraft.osrs.Skill[]{com.runecraft.osrs.Skill.ATTACK, com.runecraft.osrs.Skill.STRENGTH,
                    com.runecraft.osrs.Skill.DEFENCE, com.runecraft.osrs.Skill.RANGED, com.runecraft.osrs.Skill.MAGIC}) {
                data.skills().setBoost(s, data.skills().boost(s) - BalanceElemental.statDrain(data.skills().currentLevel(s)));
            }
            data.setPrayerPoints(data.prayerPoints() - BalanceElemental.prayerDrain(data.prayerPoints()));
            Msg.red(player, "The Balance Elemental drains your combat stats and prayer!");
            com.runecraft.network.ServerNetworking.sync(player);
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

    /**
     * While Guthix Sleeps, Lucien's two tormented demons at the Stone of Jas (wiki quick guide): touching the Stone raises the
     * player's combat stats to 255 for the fight, and drains don't touch them; the demons' fire shield weakens hits that aren't
     * demonbane, and every eighth attack is a fire bomb: a short bind, then fire on and around the player's tile (up to 45), to
     * be stepped out of. The Stone's power leaves the player once no demon is left near them ({@link #releaseStonePower}).
     */
    static final class StoneDemon implements BossScript {
        /** Demons alive now (the Stone's power lasts while one is near the player). */
        static final java.util.Set<DataBossEntity> LIVE = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
        private static final com.runecraft.osrs.Skill[] COMBAT = {com.runecraft.osrs.Skill.ATTACK, com.runecraft.osrs.Skill.STRENGTH,
                com.runecraft.osrs.Skill.DEFENCE, com.runecraft.osrs.Skill.RANGED, com.runecraft.osrs.Skill.MAGIC};
        private final QuestStrikes strikes = new QuestStrikes();
        private int attacks;
        /** Game tick the fire shield is down until (each fire bomb drops it for a while, as the slayer demons'). */
        private long shieldDownUntil;
        private long gameTick;

        @Override
        public void onSpawn(DataBossEntity boss) {
            LIVE.add(boss);
        }

        @Override
        public void tick(DataBossEntity boss) {
            strikes.tick(boss);
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            this.gameTick = gameTick;
            LIVE.add(boss);
            if (!(boss.level() instanceof ServerLevel world)) {
                return;
            }
            for (ServerPlayer p : QuestStrikes.players(world)) {
                if (p.distanceToSqr(boss) <= 24 * 24) {
                    empower(p);
                }
            }
        }

        /** The Stone of Jas's power on a player fighting the demons in the quest: combat stats held at 255. */
        static void empower(ServerPlayer p) {
            QuestsState st = QuestsState.get(p);
            if (!st.hasFlag(p.getUUID(), "wgs_stone") || st.hasFlag(p.getUUID(), "wgs_demons")) {
                return;
            }
            com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
            boolean first = !st.hasFlag(p.getUUID(), QuestBossRules.STONE_OF_JAS_FLAG);
            st.setFlag(p.getUUID(), QuestBossRules.STONE_OF_JAS_FLAG);
            for (com.runecraft.osrs.Skill s : COMBAT) {
                data.skills().setBoost(s, QuestBossRules.stoneOfJasBoost(data.skills().level(s)));
            }
            if (first) {
                Msg.action(p, "The power of the Stone of Jas surges through you: your combat stats are 255!");
                com.runecraft.network.ServerNetworking.sync(p);
            }
        }

        /**
         * Once a second: a player still holding the Stone's power with no demon left within 32 blocks loses it (the boosts go back
         * to none, drained stats included).
         */
        static void releaseStonePower(net.minecraft.server.MinecraftServer server) {
            if (server.getTickCount() % 20 != 0) {
                return;
            }
            for (ServerLevel world : server.getAllLevels()) {
                for (ServerPlayer p : QuestStrikes.players(world)) { // the self-test's player too
                    release(p, false);
                }
            }
        }

        /** Takes the Stone's power from {@code p} if no demon is near (or at once with {@code now}); true if it was taken. */
        static boolean release(ServerPlayer p, boolean now) {
            QuestsState st = QuestsState.get(p);
            if (!st.hasFlag(p.getUUID(), QuestBossRules.STONE_OF_JAS_FLAG)) {
                return false;
            }
            if (!now) {
                for (DataBossEntity d : LIVE) {
                    if (d.isAlive() && d.level() == p.level() && d.distanceToSqr(p) <= 32 * 32) {
                        return false;
                    }
                }
            }
            st.clearFlag(p.getUUID(), QuestBossRules.STONE_OF_JAS_FLAG);
            com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
            for (com.runecraft.osrs.Skill s : COMBAT) {
                data.skills().setBoost(s, 0);
            }
            Msg.game(p, "The Stone of Jas's power fades from you.");
            com.runecraft.network.ServerNetworking.sync(p);
            return true;
        }

        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (target != null && ++attacks % QuestBossRules.WGS_DEMON_ATTACKS_PER_BOMB == 0) {
                fireBomb(boss, target);
                return AttackDef.idle(AttackType.MAGIC); // the bomb is this turn's attack
            }
            return attack;
        }

        private void fireBomb(DataBossEntity boss, ServerPlayer target) {
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN,
                    QuestBossRules.WGS_DEMON_BIND_TICKS * 12, 10, false, true));
            Msg.red(target, "The tormented demon binds you in flame! Get off the burning tiles!");
            strikes.add(boss, target.position(), QuestBossRules.WGS_DEMON_BOMB_RADIUS, 0, QuestBossRules.WGS_DEMON_BOMB_MAX,
                    QuestBossRules.WGS_DEMON_BOMB_TICKS, net.minecraft.core.particles.ParticleTypes.FLAME, com.runecraft.boss.HazardKind.FIRE,
                    "fire_bomb");
            shieldDownUntil = gameTick + com.runecraft.osrs.content.slayer.TormentedDemons.SHIELD_DOWN_TICKS;
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (damage <= 0 || gameTick < shieldDownUntil) {
                return damage;
            }
            String weapon = attacker.getMainHandItem().isEmpty() ? "" : Inv.idOf(attacker.getMainHandItem());
            if (com.runecraft.osrs.content.slayer.TormentedDemons.pierces(weapon)) {
                return damage;
            }
            return Math.max(1, (int) Math.floor(damage * QuestBossRules.WGS_DEMON_SHIELD));
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            onRemoved(boss);
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            strikes.clear();
            LIVE.remove(boss);
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

        /**
         * Her next attack. The attack goal asks for one every Minecraft tick (to know whether to chase), so nothing is counted
         * here: the attack she actually makes is counted in {@link #beforeAttack}. (Counting here made a special out of every
         * fourth goal tick instead of every fourth attack.)
         */
        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss.def() == null) {
                return null;
            }
            String name = QuestBossRules.fragmentSpecialNext(attacks) ? SPECIALS[special]
                    : distance <= 2.5 ? "swipe" : "crystal_volley";
            for (AttackDef a : boss.def().attacks) {
                if (a.name().equals(name)) {
                    return a;
                }
            }
            return null;
        }

        /** The attack she really makes: counted, and a special moves her on to the next one in order. */
        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (attack == null) {
                return null;
            }
            attacks++;
            if (attack.name().equals(SPECIALS[special])) {
                special = (special + 1) % SPECIALS.length;
            }
            last = attack.name();
            return attack;
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            return judged(boss, player, damage, last);
        }

        /** The hit's own attack: it lands after its flight or wind-up, when {@code last} may already name her next one. */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type, AttackDef attack) {
            return judged(boss, player, damage, attack != null ? attack.name() : last);
        }

        private int judged(DataBossEntity boss, ServerPlayer player, int damage, String attack) {
            if (!"crystal_judgement".equals(attack)) {
                return damage;
            }
            float left = boss.getHealth() / boss.getMaxHealth();
            int spare = left > 2 / 3f ? 5 : left > 1 / 3f ? 3 : 1;
            int max = com.runecraft.player.Players.data(player).skills().level(com.runecraft.osrs.Skill.HITPOINTS);
            return Math.max(1, max - spare);
        }
    }
    /**
     * Sins of the Father, Vanstrom Klause (wiki quest guide): magic blood blasts that heal him for part of what they deal and
     * that Protect from Magic only halves, and an attack that lowers Attack. He may open with one of his three specials at any
     * point, then uses one after every 9-12 regular attacks; the first is random, then they follow a set order and none comes
     * back until all three were used ({@link QuestBossRules#vanstromSpecial}):
     * <ul>
     *   <li>"My pets will feast on your corpse!": an acidic bloodveld appears on the side of the arena away from the player and
     *   walks at them, exploding for 35-50 through any prayer if it reaches them; killed, it leaves an acid pool;</li>
     *   <li>"Stare into darkness!": the player's tile goes dark and is hit for 40+ unless they step off it;</li>
     *   <li>"Blood will be my strength!": a 3x3 blood orb that strengthens his blasts through Protect from Magic; luring him into
     *   it bursts it and hurts him, stepping into it hurts the player.</li>
     * </ul>
     * Only the blisterwood flail hurts him (recoil and Vengeance do nothing). Brought to 0 hitpoints he heals 200 and enrages: no
     * more specials, his blasts ignore protection prayers, and every couple of seconds 10-15 shadows mark tiles around the player
     * for lightning of up to 40, which only stepping off them avoids. Every dodgeable hit is marked on the floor and the cast bar
     * ({@link QuestStrikes}).
     */
    static final class Vanstrom implements BossScript {
        private boolean healed;
        /** The game time a blisterwood hit passed {@link #modifyIncoming}: the one damage the gate lets through. */
        private long approvedAt = Long.MIN_VALUE;
        private final QuestStrikes strikes = new QuestStrikes();
        /** Real attacks until the next special (first: "at any time"), counted in {@link #beforeAttack}. */
        private int untilSpecial = -1;
        /** The order's starting special (random) and how many have been used. */
        private int firstSpecial = -1;
        private int specialsUsed;
        /** His next regular attack, picked when the last one was made (chooseAttack runs every Minecraft tick). */
        private String nextRegular = "blood_blast";
        /** The acidic bloodveld out now, the player it hunts and where it was last seen. */
        private java.util.UUID pet;
        private java.util.UUID petPrey;
        private net.minecraft.world.phys.Vec3 petLast;
        /** The blood orb: its centre and the game tick it fades (0: none). */
        private net.minecraft.world.phys.Vec3 orb;
        private long orbUntil;
        private com.runecraft.boss.FloorMarkers.Layer orbFloor;
        private long gameTick;
        private long nextLightning;

        /** The bloodveld he calls up with "My pets will feast on your corpse!". */
        static final String PET = "sotf_acidic_bloodveld";

        /**
         * Damage that skipped the blisterwood rule (a ring of recoil or suffering, Vengeance, thrown potions) never reaches
         * him (wiki), and whatever else brings him to 0 still meets his heal before the second phase. Melee barely scratches
         * his acidic bloodveld (wiki: "highly resistant and take reduced damage from melee attacks").
         */
        static void register() {
            com.runecraft.combat.CombatHooks.addMonsterDamageGate((player, target) ->
                    target instanceof DataBossEntity boss && boss.script() instanceof Vanstrom v && !v.consumeApproval(boss)
                            ? "Only the blisterwood flail can hurt Vanstrom Klause." : null);
            com.runecraft.platform.ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) ->
                    !(entity instanceof DataBossEntity boss && boss.script() instanceof Vanstrom v)
                            || source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY) || !v.rise(boss));
            com.runecraft.combat.CombatHooks.addModifier(new com.runecraft.combat.CombatHooks.PlayerAttackModifier() {
                @Override
                public int afterRoll(ServerPlayer player, net.minecraft.world.entity.LivingEntity target, AttackType type, int damage) {
                    if (type.isMelee() && damage > 1 && isPet(target)) {
                        return 1; // approx: the wiki gives no number for the melee resistance
                    }
                    return damage;
                }
            });
        }

        static boolean isPet(net.minecraft.world.entity.Entity e) {
            if (!(e instanceof net.minecraft.world.entity.LivingEntity living)) {
                return false;
            }
            com.runecraft.combat.MonsterStats stats = com.runecraft.combat.MonsterStats.of(living);
            return stats.profile() != null && PET.equals(stats.profile().id);
        }

        private boolean consumeApproval(DataBossEntity boss) {
            boolean ok = approvedAt == boss.level().getGameTime();
            approvedAt = Long.MIN_VALUE;
            return ok;
        }

        /** At 0 hitpoints the first time: heals 200 and starts the second phase. False when already risen. */
        private boolean rise(DataBossEntity boss) {
            if (healed) {
                return false;
            }
            healed = true;
            boss.setHealth(boss.getMaxHealth() * 200f / boss.trueHitpoints());
            announce(boss, "Vanstrom Klause drinks deep of the blood around him and rises again! Watch for the shadows of his lightning.",
                    ChatFormatting.DARK_RED);
            endOrb();
            nextLightning = gameTick + 2;
            return true;
        }

        private AttackDef named(DataBossEntity boss, String name) {
            for (AttackDef a : boss.def().attacks) {
                if (a.name().equals(name)) {
                    return a;
                }
            }
            return null;
        }

        /**
         * His next attack: a blood blast (prayer-piercing here so the script can halve it under Protect from Magic, and leave it
         * whole in his second phase) or the Attack-lowering one. No side effects: the engine asks every Minecraft tick.
         */
        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss.def() == null) {
                return null;
            }
            return named(boss, healed ? "blood_blast" : nextRegular);
        }

        /** The attack he really makes: counted towards his next special, which takes this turn's place when due. */
        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (healed || target == null || boss.def() == null) {
                return attack;
            }
            nextRegular = boss.getRandom().nextInt(4) == 0 ? "weaken" : "blood_blast";
            if (untilSpecial < 0) {
                untilSpecial = 1 + boss.getRandom().nextInt(13); // approx: "may use a special attack at any time" at the start
            }
            if (--untilSpecial > 0) {
                return attack;
            }
            untilSpecial = 10 + boss.getRandom().nextInt(4); // the special after 9-12 regular attacks
            if (firstSpecial < 0) {
                firstSpecial = boss.getRandom().nextInt(QuestBossRules.VANSTROM_SPECIALS.size());
            }
            String special = QuestBossRules.vanstromSpecial(firstSpecial, specialsUsed++);
            switch (special) {
                case "pets" -> pets(boss, target);
                case "stare_into_darkness" -> stare(boss, target);
                default -> bloodOrb(boss, target);
            }
            return AttackDef.idle(AttackType.MAGIC); // the special is this turn's attack
        }

        private void pets(DataBossEntity boss, ServerPlayer target) {
            if (!(boss.level() instanceof ServerLevel world)) {
                return;
            }
            announce(boss, "Vanstrom Klause: My pets will feast on your corpse!", ChatFormatting.RED);
            // the side of the arena (north or south of him) farther from the player
            int side = target.getZ() > boss.getZ() ? -1 : 1;
            net.minecraft.world.entity.Entity e = null;
            for (int d = QuestBossRules.VANSTROM_PET_DISTANCE; d >= 2 && e == null; d -= 3) {
                double z = boss.getZ() + side * d;
                if (world.noCollision(new net.minecraft.world.phys.AABB(boss.getX() - 0.5, boss.getY(), z - 0.5, boss.getX() + 0.5,
                        boss.getY() + 2, z + 0.5))) {
                    e = ChampionOfScabaras.spawn(world, PET, boss.getX(), boss.getY(), z);
                }
            }
            if (e == null) {
                return;
            }
            boss.claimMinion(e);
            if (e instanceof net.minecraft.world.entity.Mob mob) {
                mob.setTarget(target);
            }
            pet = e.getUUID();
            petPrey = target.getUUID();
            petLast = e.position();
            Msg.red(target, "An acidic bloodveld lopes towards you! Kill it from afar before it reaches you.");
        }

        private void stare(DataBossEntity boss, ServerPlayer target) {
            announce(boss, "Vanstrom Klause: Stare into darkness!", ChatFormatting.DARK_PURPLE);
            target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS, 50, 0, false, false));
            // playability: OSRS gives about a tick to step off the tile; here the strike floor, with the tile marked
            strikes.add(boss, target.position(), 1.0, QuestBossRules.VANSTROM_STARE_MIN, QuestBossRules.VANSTROM_STARE_MAX,
                    QuestBossRules.VANSTROM_STARE_TICKS, net.minecraft.core.particles.ParticleTypes.SQUID_INK,
                    com.runecraft.boss.HazardKind.SHADOW, "stare_into_darkness");
            Msg.red(target, "Darkness gathers on your tile: step off it!");
        }

        private void bloodOrb(DataBossEntity boss, ServerPlayer target) {
            if (!(boss.level() instanceof ServerLevel world)) {
                return;
            }
            announce(boss, "Vanstrom Klause: Blood will be my strength!", ChatFormatting.DARK_RED);
            endOrb();
            // between him and the player, offset a little: lure him through it (wiki)
            double a = boss.getRandom().nextDouble() * Math.PI * 2;
            net.minecraft.world.phys.Vec3 mid = boss.position().add(target.position()).scale(0.5);
            orb = new net.minecraft.world.phys.Vec3(mid.x + Math.cos(a) * 2, boss.getY(), mid.z + Math.sin(a) * 2);
            orbUntil = gameTick + QuestBossRules.VANSTROM_ORB_TICKS;
            orbFloor = com.runecraft.boss.FloorMarkers.show(new com.runecraft.boss.FloorMarkers.Shape(world)
                    .box(orb, QuestBossRules.VANSTROM_ORB_RADIUS, com.runecraft.boss.HazardKind.BLOOD), boss);
            Msg.red(target, "A blood orb strengthens Vanstrom's magic! Lure him into it, but don't step in yourself.");
        }

        private void endOrb() {
            if (orbFloor != null) {
                orbFloor.clear();
            }
            orbFloor = null;
            orb = null;
            orbUntil = 0;
        }

        private static boolean inOrb(net.minecraft.world.phys.Vec3 orb, net.minecraft.world.entity.Entity e) {
            return Math.abs(e.getX() - orb.x) <= QuestBossRules.VANSTROM_ORB_RADIUS
                    && Math.abs(e.getZ() - orb.z) <= QuestBossRules.VANSTROM_ORB_RADIUS && Math.abs(e.getY() - orb.y) <= 2;
        }

        @Override
        public void tick(DataBossEntity boss) {
            strikes.tick(boss);
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            this.gameTick = gameTick;
            if (!(boss.level() instanceof ServerLevel world)) {
                return;
            }
            tickPet(boss, world);
            if (orb != null) {
                world.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.6f, 0.0f, 0.05f), 1.6f),
                        orb.x, orb.y + 0.8, orb.z, 8, 0.5, 0.4, 0.5, 0);
                if (gameTick >= orbUntil) {
                    endOrb();
                } else if (inOrb(orb, boss)) {
                    // lured into his own orb: it bursts and hurts him (wiki: 20+), never past his rise
                    int dmg = QuestBossRules.VANSTROM_ORB_SELF_MIN
                            + boss.getRandom().nextInt(QuestBossRules.VANSTROM_ORB_SELF_MAX - QuestBossRules.VANSTROM_ORB_SELF_MIN + 1);
                    float left = boss.getHealth() - dmg * boss.getMaxHealth() / boss.trueHitpoints();
                    boss.setHealth(Math.max(boss.getMaxHealth() / boss.trueHitpoints(), left));
                    announce(boss, "Vanstrom Klause blunders into his blood orb and it bursts over him!", ChatFormatting.GOLD);
                    endOrb();
                } else {
                    for (ServerPlayer p : QuestStrikes.players(world)) {
                        if (inOrb(orb, p)) {
                            com.runecraft.combat.CombatHooks.nameAttack("blood_orb", null);
                            com.runecraft.combat.CombatHandler.applyDamage(boss, p, boss.getRandom().nextInt(QuestBossRules.VANSTROM_ORB_PLAYER_MAX + 1));
                            Msg.red(p, "The blood orb bursts as you step into it!");
                            endOrb();
                            break;
                        }
                    }
                }
            }
            if (healed && gameTick >= nextLightning && boss.getTarget() instanceof ServerPlayer target && target.isAlive()) {
                nextLightning = gameTick + QuestBossRules.VANSTROM_LIGHTNING_EVERY_TICKS;
                lightning(boss, target);
            }
        }

        /** A volley of 10-15 shadows: one on the player's tile, the rest around them, each a bolt of up to 40 (wiki). */
        private void lightning(DataBossEntity boss, ServerPlayer target) {
            int bolts = QuestBossRules.vanstromBolts(boss.getRandom().nextInt(6));
            for (int i = 0; i < bolts; i++) {
                net.minecraft.world.phys.Vec3 at = target.position();
                if (i > 0) {
                    int spread = QuestBossRules.VANSTROM_LIGHTNING_SPREAD;
                    at = new net.minecraft.world.phys.Vec3(Math.floor(at.x) + boss.getRandom().nextInt(spread * 2 + 1) - spread + 0.5, at.y,
                            Math.floor(at.z) + boss.getRandom().nextInt(spread * 2 + 1) - spread + 0.5);
                }
                strikes.add(boss, at, 0.8, 0, QuestBossRules.VANSTROM_LIGHTNING_MAX, 3,
                        net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK, com.runecraft.boss.HazardKind.SHADOW, "lightning");
            }
        }

        private void tickPet(DataBossEntity boss, ServerLevel world) {
            if (pet == null) {
                return;
            }
            net.minecraft.world.entity.Entity e = world.getEntity(pet);
            if (e == null || !e.isAlive()) {
                // killed before it reached anyone: it leaves an acid pool (wiki) // approx: the pool's damage and lifetime
                if (petLast != null && e != null) {
                    com.runecraft.boss.Mechanics.addHazard(world, petLast, 4, 15, boss, com.runecraft.boss.HazardKind.ACID);
                }
                pet = null;
                return;
            }
            petLast = e.position();
            ServerPlayer prey = petPrey == null ? null : (ServerPlayer) world.getPlayerByUUID(petPrey);
            if (prey == null) {
                for (ServerPlayer p : QuestStrikes.players(world)) {
                    if (p.getUUID().equals(petPrey)) {
                        prey = p; // the self-test's player
                    }
                }
            }
            if (prey == null || !prey.isAlive()) {
                return;
            }
            if (e instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != prey) {
                mob.setTarget(prey);
            }
            if (e.distanceTo(prey) <= QuestBossRules.VANSTROM_PET_REACH + prey.getBbWidth() / 2) {
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, e.getX(), e.getY() + 0.5, e.getZ(), 2, 0.3, 0.3, 0.3, 0);
                world.sendParticles(net.minecraft.core.particles.ParticleTypes.SNEEZE, e.getX(), e.getY() + 0.5, e.getZ(), 30, 0.8, 0.5, 0.8, 0.05);
                com.runecraft.combat.CombatHooks.nameAttack("pets", null);
                com.runecraft.combat.CombatHandler.applyDamage(boss, prey, QuestBossRules.VANSTROM_PET_MIN
                        + boss.getRandom().nextInt(QuestBossRules.VANSTROM_PET_MAX - QuestBossRules.VANSTROM_PET_MIN + 1));
                Msg.red(prey, "The acidic bloodveld explodes over you!");
                pet = null;
                e.discard();
            }
        }

        /**
         * His blood blast: Protect from Magic halves it, unless his blood orb strengthens it (wiki); in his second phase it
         * ignores prayers. It heals him for part of what it deals.
         */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type, AttackDef attack) {
            if (attack == null || !"blood_blast".equals(attack.name()) || damage <= 0) {
                return damage;
            }
            if (healed) {
                return damage;
            }
            int out = damage;
            if (orb != null) {
                out = (int) Math.round(damage * QuestBossRules.VANSTROM_ORB_MAGIC_MULTIPLIER);
            } else if (com.runecraft.combat.CombatHandler.isProtected(com.runecraft.player.Players.data(player), AttackType.MAGIC)) {
                out = (int) Math.floor(damage * QuestBossRules.VANSTROM_BLAST_PRAYED_SHARE);
            }
            float heal = (float) (out * QuestBossRules.VANSTROM_BLAST_HEAL_SHARE);
            if (heal > 0) {
                boss.heal(heal * boss.getMaxHealth() / boss.trueHitpoints());
            }
            return out;
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            onRemoved(boss);
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            strikes.clear();
            endOrb();
            if (pet != null && boss.level() instanceof ServerLevel world && world.getEntity(pet) != null) {
                world.getEntity(pet).discard();
            }
            pet = null;
        }

        /** Self-test: whether he is in his second phase, the strikes he has marked, and whether his bloodveld or orb is out. */
        boolean selfTestRisen() {
            return healed;
        }

        int selfTestStrikes() {
            return strikes.size();
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (damage > 0 && !type.isMelee()) {
                Msg.action(attacker, "Only a blow from the blisterwood flail will hurt Vanstrom Klause.");
                return 0;
            }
            int hp = hp(boss);
            if (!healed && damage >= hp) {
                rise(boss);
                return 0;
            }
            if (damage > 0) {
                approvedAt = boss.level().getGameTime();
            }
            return damage;
        }

        private static int hp(DataBossEntity boss) {
            return (int) Math.ceil(boss.getHealth() * boss.trueHitpoints() / boss.getMaxHealth());
        }

        /** One of the player's blows, through his rules ({@link #modifyIncoming} and the damage gate); returns what it took. */
        private static int blow(DataBossEntity boss, ServerPlayer p, int damage, AttackType type) {
            int d = boss.modifyIncoming(p, damage, type);
            return d > 0 ? com.runecraft.combat.CombatHandler.applyDamage(p, boss, d) : 0;
        }

        private static void expect(boolean ok, String what) {
            if (!ok) {
                throw new IllegalStateException(what);
            }
        }

        /**
         * The {@code boss:<id>} check's kill, played as the fight goes (wiki, Sins of the Father): he dies only the second time
         * he reaches 0 hitpoints, so the check's outright kill meets his rise instead. A whip does nothing and a hit outside
         * his rules (a ring of recoil's or Vengeance's) is refused; blisterwood flail blows take him to 0, where he heals 200
         * and starts his second phase; blows in that phase kill him. Nothing hurts him while the check lets him act, so he must
         * still be in his first phase when the play starts: a rise before it means something other than a blisterwood blow took
         * him to 0. Returns what was played, or null for another boss.
         */
        static String selfTestToKill(DataBossEntity boss, ServerPlayer p) {
            if (!(boss.script() instanceof Vanstrom v)) {
                return null;
            }
            expect(!v.healed, "he had already risen before any blow (" + hp(boss) + " hitpoints): something other than a blisterwood "
                    + "blow took him to 0 while he was left to act");
            net.minecraft.world.item.ItemStack held = p.getMainHandItem().copy();
            try {
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("abyssal_whip", 1));
                float before = boss.getHealth();
                blow(boss, p, 50, AttackType.SLASH);
                expect(boss.getHealth() >= before, "an abyssal whip hurt him (only the blisterwood flail can)");
                com.runecraft.combat.CombatHandler.applyDamage(p, boss, 10);
                expect(boss.getHealth() >= before, "a hit outside his rules (recoil, Vengeance) hurt him");
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, Inv.stack("blisterwood_flail", 1));
                int first = 0;
                for (; !v.healed && boss.isAlive() && first < 100; first++) {
                    blow(boss, p, 60, AttackType.CRUSH);
                    p.setHealth(p.getMaxHealth());
                }
                expect(v.healed && boss.isAlive(), "blisterwood blows didn't bring him to his second phase (" + hp(boss) + " hitpoints left, "
                        + (boss.isAlive() ? "alive" : "dead") + ")");
                expect(hp(boss) == 200, "he rose with " + hp(boss) + " hitpoints (wiki: heals 200)");
                int second = 0;
                for (; boss.isAlive() && second < 100; second++) {
                    blow(boss, p, 60, AttackType.CRUSH);
                    p.setHealth(p.getMaxHealth());
                }
                expect(!boss.isAlive(), "blisterwood blows in his second phase didn't kill him (" + hp(boss) + " hitpoints left)");
                return "only blisterwood hurt him; risen with 200 hitpoints after " + first + " blows, slain " + second + " blows later";
            } finally {
                p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, held);
                p.setHealth(p.getMaxHealth());
            }
        }
    }

    /**
     * A quest boss whose every {@code n}th real attack is one named special, its other attacks picked as usual (The Curse of
     * Arrav: Arrav swings his axe ahead of him every third attack, QuestBossRules.COA_ARRAV_AXE_EVERY). Counted in beforeAttack,
     * on the attacks it really makes.
     */
    static final class EveryNth implements BossScript {
        private final int n;
        private final String special;
        private int attacks;

        EveryNth(int n, String special) {
            this.n = n;
            this.special = special;
        }

        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (attack == null || boss.def() == null || !QuestBossRules.nthIsSpecial(++attacks, n)) {
                return attack;
            }
            for (AttackDef a : boss.def().attacks) {
                if (a.name().equals(special)) {
                    return a;
                }
            }
            return attack;
        }
    }

    static final class MagicOnly implements BossScript {
        private long lastMessage;
        private int attacks;

        /** His regular attack (no side effects: the engine asks every Minecraft tick). */
        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss.def() == null || boss.def().attacks.isEmpty()) {
                return null;
            }
            return boss.def().attacks.get(0);
        }

        /**
         * Wiki: Surok fights like Vorkath, six regular attacks and then one of his specials as the seventh. Counted here, on the
         * attacks he really makes (the count used to run in chooseAttack, which the engine calls every tick).
         */
        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (boss.def() == null || boss.def().attacks.size() < 2 || ++attacks % QuestBossRules.SUROK_SPECIAL_EVERY != 0) {
                return attack;
            }
            List<AttackDef> specials = boss.def().attacks.subList(1, boss.def().attacks.size());
            return specials.get(boss.getRandom().nextInt(specials.size()));
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
                if (data.isActive(prayer)) { // strict on purpose: he punishes the prayer he sees, not one just switched off
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
    static class ColourShift implements BossScript {
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

    /** Whether the player carries an item or wears it in a quest gear slot (gloves, capes, amulets). */
    static boolean hasOrWears(ServerPlayer p, String item) {
        return Inv.has(p, item, 1) || QuestsState.get(p).wornAll(p.getUUID()).containsValue(item);
    }

    /**
     * The element of the spell a player attacks with ("air", "water", "earth", "fire"), as the game knows it: the elemental
     * autocast spell, or null for a powered staff's own spell or a non-elemental one. Spells cast by hand from the
     * spellbook count as the autocast spell (approx).
     */
    static String spellElement(ServerPlayer p) {
        com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
        if (com.runecraft.osrs.CombatBonuses.poweredStaffMaxHit(Inv.idOf(p.getMainHandItem()), data.skills().currentLevel(
                com.runecraft.osrs.Skill.MAGIC)) >= 0) {
            return null;
        }
        com.runecraft.osrs.Spell spell = data.autocast();
        return spell == null ? null : spell.element();
    }

    /** At most one message every two seconds per boss script. */
    static boolean quiet(DataBossEntity boss, long[] last) {
        long now = boss.level().getGameTime();
        if (now - last[0] > 40) {
            last[0] = now;
            return false;
        }
        return true;
    }

    /**
     * Desert Treasure I (wiki): Fareed disarms anyone who hits him in melee without ice gloves, so melee does nothing
     * without them; magic only hurts him with water spells (not a trident's); any ranged attack works. His melee hits up to
     * 40, 37 against ice gloves.
     */
    static final class Fareed implements BossScript {
        private final long[] lastMessage = {-100};

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (type.isMelee() && !hasOrWears(attacker, "ice_gloves")) {
                if (!quiet(boss, lastMessage)) {
                    Msg.action(attacker, "Fareed's flames scorch your hands and you can't keep hold of your weapon! Ice gloves would protect you.");
                }
                return 0;
            }
            if (type == AttackType.MAGIC && !QuestBossRules.spellHurts("fareed", spellElement(attacker))) {
                if (!quiet(boss, lastMessage)) {
                    Msg.action(attacker, "Your spell fizzles in Fareed's flames. Only water spells will hurt him.");
                }
                return 0;
            }
            return damage;
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            return type.isMelee() && hasOrWears(player, "ice_gloves") ? Math.min(damage, QuestBossRules.FAREED_MAX_ICE_GLOVES) : damage;
        }
    }

    /**
     * Desert Treasure I (wiki): Kamil is immune to every spell but fire spells, and the cold of the Ice Path keeps draining
     * the combat stats of the player fighting him (one level of each every 10 game ticks here, approx).
     */
    static final class Kamil implements BossScript {
        private static final com.runecraft.osrs.Skill[] CHILLED = {com.runecraft.osrs.Skill.ATTACK, com.runecraft.osrs.Skill.STRENGTH,
                com.runecraft.osrs.Skill.DEFENCE, com.runecraft.osrs.Skill.RANGED, com.runecraft.osrs.Skill.MAGIC};
        private final long[] lastMessage = {-100};

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (type == AttackType.MAGIC && !QuestBossRules.spellHurts("kamil", spellElement(attacker))) {
                if (!quiet(boss, lastMessage)) {
                    Msg.action(attacker, "Kamil shrugs off the spell. Only fire spells can melt his armour of ice.");
                }
                return 0;
            }
            return damage;
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (gameTick % 10 != 0 || !(boss.getTarget() instanceof ServerPlayer p) || p.distanceToSqr(boss) > 24 * 24) {
                return;
            }
            com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
            for (com.runecraft.osrs.Skill s : CHILLED) {
                if (data.skills().currentLevel(s) > 1) {
                    data.skills().setBoost(s, data.skills().boost(s) - 1);
                }
            }
            com.runecraft.network.ServerNetworking.sync(p);
        }
    }

    /**
     * Desert Treasure I (wiki, DPS calculator): Damis can only be seen (and so fought) with a ring of visibility. His first
     * form (level 103, 90 Hitpoints) turns into the second (level 174, 200 Hitpoints, Defence 160, faster) when it falls,
     * and the second form drains the Prayer of a player fighting him in melee distance.
     */
    static final class Damis implements BossScript {
        static final int SECOND_FORM_HP = QuestBossRules.DAMIS_SECOND[1];
        private boolean second;
        private final long[] lastMessage = {-100};

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (!Inv.has(attacker, "ring_of_visibility", 1)) {
                if (!quiet(boss, lastMessage)) {
                    Msg.action(attacker, "You can't see what you're attacking. A ring of visibility would show you.");
                }
                return 0;
            }
            if (!second && damage >= Math.ceil(boss.getHealth())) {
                transform(boss);
                return 0;
            }
            return damage;
        }

        private void transform(DataBossEntity boss) {
            second = true;
            int[] f = QuestBossRules.DAMIS_SECOND;
            boss.overrideStats(s -> new com.runecraft.combat.MonsterStats(s.name(), f[0], f[1], f[2], f[3], f[4], s.magic(),
                    s.ranged(), 100, f[5], s.style(), f[6], s.monster(), s.profile(), s.boss())); // +100 melee (80 magic, 120 ranged)
            net.minecraft.world.entity.ai.attributes.AttributeInstance max = boss.getAttribute(
                    net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (max != null) {
                max.setBaseValue(SECOND_FORM_HP);
            }
            boss.setHealth(SECOND_FORM_HP);
            announce(boss, "Damis transforms into his second form! (level 174)", ChatFormatting.RED);
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (second && player.distanceToSqr(boss) <= 4 * 4) {
                com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(player);
                data.setPrayerPoints(Math.max(0, data.prayerPoints() - Math.max(1, damage / 2))); // "drains Prayer rapidly", approx
            }
            return second ? Math.min(damage, QuestBossRules.DAMIS_SECOND[6]) : Math.min(damage, QuestBossRules.DAMIS_FIRST[6]);
        }

        @Override
        public void writeNbt(DataBossEntity boss, net.minecraft.nbt.CompoundTag nbt) {
            if (second) {
                nbt.putBoolean("second", true);
            }
        }

        @Override
        public void readNbt(DataBossEntity boss, net.minecraft.nbt.CompoundTag nbt) {
            if (nbt.getBoolean("second") && !second) {
                float hp = boss.getHealth();
                transform(boss);
                boss.setHealth(Math.min(hp, SECOND_FORM_HP));
            }
        }
    }

    /**
     * Recipe for Disaster's final fights (wiki): Prayer can't be used in the Culinaromancer's dimension, so the protection and
     * boosting prayers of the player fighting there switch off. Only the players in the fight ({@link #engaged}): the lair
     * lies under Lumbridge Castle, and bystanders above keep their prayers.
     */
    static class Culinary implements BossScript {
        private final java.util.Set<java.util.UUID> told = new java.util.HashSet<>();

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (!(boss.level() instanceof ServerLevel world)) {
                return;
            }
            for (ServerPlayer p : world.getPlayers(x -> engaged(boss, x))) {
                com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
                if (!data.activePrayers().isEmpty()) {
                    data.activePrayers().clear();
                    com.runecraft.network.ServerNetworking.sync(p);
                    if (told.add(p.getUUID())) {
                        Msg.red(p, noPrayerMessage());
                    }
                }
            }
        }

        String noPrayerMessage() {
            return "Your prayers have no power in this dimension.";
        }

        /** The boss's attack of this name, or null. */
        static AttackDef attack(DataBossEntity boss, String name) {
            if (boss.def() != null) {
                for (AttackDef a : boss.def().attacks) {
                    if (a.name().equals(name)) {
                        return a;
                    }
                }
            }
            return null;
        }
    }

    /** Agrith-Na-Na: melee up close, Fire Blast from a distance (wiki). */
    static final class AgrithNaNa extends Culinary {
        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return attack(boss, distance <= 2.5 ? "claws" : "fire_blast");
        }
    }

    /** Flambeed (wiki): like Fareed, he disarms anyone hitting him in melee without ice gloves; water spells work best. */
    static final class Flambeed extends Culinary {
        private final long[] lastMessage = {-100};

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (type.isMelee() && !hasOrWears(attacker, "ice_gloves")) {
                if (!quiet(boss, lastMessage)) {
                    Msg.action(attacker, "Flambeed's flames make you drop your weapon! Ice gloves would protect your hands.");
                }
                return 0;
            }
            return damage;
        }
    }

    /** Karamel (wiki): a draining ice spell that hits twice from afar, a ranged attack in melee distance. */
    static final class Karamel extends Culinary {
        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return attack(boss, distance <= 2.5 ? "toffee_shot" : "ice_spell");
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            // the spell's second hit (7 each)
            return type == AttackType.MAGIC ? damage + boss.getRandom().nextInt(QuestBossRules.KARAMEL_SPELL_HIT + 1) : damage;
        }
    }

    /** The Gelatinnoth Mother: the Dagannoth Mother's colours, in the prayerless dimension. */
    static final class Gelatinnoth extends ColourShift {
        private final Culinary dimension = new Culinary();

        Gelatinnoth() {
            super("The Gelatinnoth Mother");
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            super.gameTick(boss, gameTick);
            dimension.gameTick(boss, gameTick);
        }
    }

    /** The Culinaromancer (wiki): his magic has a flat 1-in-3 chance to hit, up to 25. */
    static final class Culinaromancer extends Culinary {
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (type != AttackType.MAGIC) {
                return damage;
            }
            return QuestBossRules.culinaromancerHits(boss.getRandom().nextInt(3))
                    ? boss.getRandom().nextInt(QuestBossRules.CULINAROMANCER_MAGIC_MAX + 1) : 0;
        }
    }

    /**
     * Haunted Mine (wiki): Treus Dayth shifts about the room when attacked, more often the closer he is to dying, and sets the
     * mine carts and cranes on the player (small typeless hits every few ticks here, approx).
     */
    static final class TreusDayth implements BossScript {
        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            float left = boss.getHealth() / boss.getMaxHealth();
            if (damage > 0 && boss.getRandom().nextInt(QuestBossRules.treusShiftOneIn(left)) == 0) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                boss.teleportTo(boss.getX() + Math.cos(a) * 5, boss.getY(), boss.getZ() + Math.sin(a) * 5);
                announce(boss, "Treus Dayth shifts across the mine!", ChatFormatting.GRAY);
            }
            return damage;
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (gameTick % QuestBossRules.TREUS_MACHINERY_TICKS != 0 || !(boss.getTarget() instanceof ServerPlayer p)
                    || p.distanceToSqr(boss) > 20 * 20) {
                return;
            }
            int hit = boss.getRandom().nextInt(QuestBossRules.TREUS_MACHINERY_MAX + 1);
            if (hit > 0) {
                Msg.red(p, boss.getRandom().nextBoolean() ? "A mine cart rolls into you!" : "A crane swings into you!");
                com.runecraft.combat.CombatHandler.applyDamage(null, p, hit);
            }
        }
    }

    /**
     * Making Friends with My Arm (wiki): the Fire of Domination makes Mother untouchable until the player has doused it with a
     * bucket of water (the quest's {@code mfa_fire_out} flag).
     */
    static final class MotherOfWeiss implements BossScript {
        private final long[] lastMessage = {-100};

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (QuestsState.get(attacker).hasFlag(attacker.getUUID(), QuestBossRules.MOTHER_FIRE_OUT_FLAG)) {
                return damage;
            }
            if (!quiet(boss, lastMessage)) {
                Msg.action(attacker, "The Fire of Domination shields Mother. Douse it with a bucket of water from the large barrel.");
            }
            return 0;
        }
    }

    /** Dream Mentor (wiki): Cyrisus's fears are fought in his dream, where prayers don't work. */
    static class DreamFear extends Culinary {
        @Override
        String noPrayerMessage() {
            return "Your prayers have no power in Cyrisus's dream.";
        }
    }

    /**
     * Dream Mentor (wiki): the Inadequacy uses melee up close and ranged from afar, and summons Doubts, which vanish when it
     * falls.
     */
    static final class Inadequacy extends DreamFear {
        private static java.util.List<? extends net.minecraft.world.entity.Entity> doubts(DataBossEntity boss) {
            net.minecraft.world.entity.EntityType<?> type = com.runecraft.registry.ModEntities.typeById("a_doubt");
            return type == null ? java.util.List.of()
                    : ((ServerLevel) boss.level()).getEntities(type, boss.getBoundingBox().inflate(24),
                            net.minecraft.world.entity.Entity::isAlive);
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            if (boss.getRandom().nextInt(QuestBossRules.INADEQUACY_SUMMON_ONE_IN) == 0
                    && doubts(boss).size() < QuestBossRules.INADEQUACY_MAX_DOUBTS) {
                return attack(boss, "summon_doubts");
            }
            return attack(boss, distance <= 2.5 ? "crush" : "doubt");
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            doubts(boss).forEach(net.minecraft.world.entity.Entity::discard);
        }
    }

    /**
     * Dream Mentor (wiki, quest-helper): the Illusive never attacks, but after every two hits that land it burrows into the
     * ground and comes up somewhere else.
     */
    static final class Illusive extends DreamFear {
        private static final AttackDef IDLE = AttackDef.idle(AttackType.CRUSH);
        private int hits;

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return IDLE;
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (damage > 0 && ++hits >= QuestBossRules.ILLUSIVE_HITS_BEFORE_BURROW) {
                hits = 0;
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                boss.teleportTo(boss.getX() + Math.cos(a) * 6, boss.getY(), boss.getZ() + Math.sin(a) * 6);
                Msg.action(attacker, "The Illusive burrows into the ground and comes up somewhere else!");
            }
            return damage;
        }
    }

    /**
     * A Kingdom Divided (wiki): on the pier only melee hurts the Judge of Yama; it claws up close, sends waves of fire from
     * afar and now and then banishes the player down the docks.
     */
    static final class JudgeOfYama implements BossScript {
        private final long[] lastMessage = {-100};
        private int attacks;

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (QuestBossRules.judgeOfYamaHurtBy(type)) {
                return damage;
            }
            if (!quiet(boss, lastMessage)) {
                Msg.action(attacker, "The Judge of Yama is immune to magic and ranged attacks.");
            }
            return 0;
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return Culinary.attack(boss, distance <= 2.5 ? "claws" : "fire_wave");
        }

        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            if (++attacks % QuestBossRules.JUDGE_BANISH_EVERY == 0) {
                double dx = target.getX() - boss.getX();
                double dz = target.getZ() - boss.getZ();
                double len = Math.max(0.1, Math.sqrt(dx * dx + dz * dz));
                net.minecraft.core.BlockPos land = banishSpot(target, dx / len, dz / len);
                if (land != null) {
                    target.teleportTo(land.getX() + 0.5, land.getY(), land.getZ() + 0.5);
                    Msg.red(target, "The Judge of Yama banishes you down the docks!");
                    return Culinary.attack(boss, "fire_wave");
                }
            }
            return attack;
        }

        /**
         * Where a banish lands: the farthest floor up to {@link QuestBossRules#JUDGE_BANISH_BLOCKS} away along the push,
         * walked a block at a time from the player (one step up or down at most, never through a wall or into the sea), or
         * null when there's no room for at least two blocks (the banish is skipped).
         */
        static net.minecraft.core.BlockPos banishSpot(ServerPlayer p, double ux, double uz) {
            ServerLevel world = p.serverLevel();
            net.minecraft.core.BlockPos last = null;
            int y = p.getBlockY();
            int moved = 0;
            for (int d = 1; d <= QuestBossRules.JUDGE_BANISH_BLOCKS; d++) {
                int x = net.minecraft.util.Mth.floor(p.getX() + ux * d);
                int z = net.minecraft.util.Mth.floor(p.getZ() + uz * d);
                net.minecraft.core.BlockPos found = null;
                for (int dy : new int[]{0, 1, -1}) {
                    net.minecraft.core.BlockPos feet = new net.minecraft.core.BlockPos(x, y + dy, z);
                    if (com.runecraft.world.NpcFloor.standable(world, feet)) {
                        found = feet;
                        break;
                    }
                }
                if (found == null) {
                    break;
                }
                last = found;
                y = found.getY();
                moved = d;
            }
            return moved >= 2 ? last : null;
        }
    }

    /**
     * A Kingdom Divided (wiki): Xamphur is immune to magic; every tenth attack hands fall from the ceiling or two Phantom
     * hands appear, in turn.
     */
    static final class Xamphur implements BossScript {
        private final long[] lastMessage = {-100};
        private int attacks;

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (type != AttackType.MAGIC) {
                return damage;
            }
            if (!quiet(boss, lastMessage)) {
                Msg.action(attacker, "Xamphur is immune to magic.");
            }
            return 0;
        }

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return Culinary.attack(boss, "dark_bolt");
        }

        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            String special = QuestBossRules.xamphurSpecial(++attacks);
            AttackDef a = special == null ? null : Culinary.attack(boss, special);
            return a != null ? a : attack;
        }
    }

    /**
     * The Path of Glouphrie (wiki): warped terrorbirds can only be hurt by a player carrying the crystal chime, and earmuffs
     * or a slayer helmet soften their ranged screech.
     */
    static final class WarpedTerrorbird implements BossScript {
        private final long[] lastMessage = {-100};

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (Inv.has(attacker, QuestBossRules.TERRORBIRD_CHIME, 1)) {
                return damage;
            }
            if (!quiet(boss, lastMessage)) {
                Msg.action(attacker, "The warped terrorbird shrugs off your attack. Without the crystal chime you can't hurt it.");
            }
            return 0;
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            java.util.Collection<String> worn = QuestsState.get(player).wornAll(player.getUUID()).values();
            boolean ears = QuestBossRules.TERRORBIRD_EAR_PROTECTION.stream().anyMatch(worn::contains);
            return type == AttackType.RANGED && ears ? Math.min(damage, QuestBossRules.TERRORBIRD_SCREECH_MAX_WITH_EARS) : damage;
        }
    }

    /**
     * Beneath Cursed Sands (wiki): the Champion of Scabaras melees up close and casts shadow magic from afar; after several
     * attacks it bursts shadow around itself, opens a Shadow Rift that explodes on everyone unless destroyed in time, or calls a
     * scarab swarm and dark flames.
     */
    static final class ChampionOfScabaras implements BossScript {
        private int attacks;
        private java.util.UUID rift;
        private long riftAt;

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            return Culinary.attack(boss, distance <= 2.5 ? "crush" : "shadow");
        }

        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            String special = QuestBossRules.scabarasSpecial(++attacks);
            if (special == null || !(boss.level() instanceof ServerLevel world)) {
                return attack;
            }
            switch (special) {
                case "shadow_rift" -> {
                    announce(boss, "Champion of Scabaras: We are born in the shadows!", ChatFormatting.RED);
                    net.minecraft.world.entity.Entity e = spawn(world, "shadow_rift", boss.getX() + 4, boss.getY(), boss.getZ());
                    if (e != null) {
                        boss.claimMinion(e);
                        rift = e.getUUID();
                        riftAt = com.runecraft.action.ActionManager.gameTick();
                    }
                    return attack;
                }
                case "shadow_flames" -> {
                    announce(boss, "Champion of Scabaras: The children of Scabaras are eternal!", ChatFormatting.RED);
                    boss.summonMinions(1); // the scarab swarm (BossDef minion)
                    AttackDef flames = Culinary.attack(boss, "shadow_flames");
                    return flames != null ? flames : attack;
                }
                default -> {
                    AttackDef burst = Culinary.attack(boss, "shadow_burst");
                    return burst != null ? burst : attack;
                }
            }
        }

        static net.minecraft.world.entity.Entity spawn(ServerLevel world, String id, double x, double y, double z) {
            net.minecraft.world.entity.EntityType<?> type = com.runecraft.registry.ModEntities.typeById(id);
            net.minecraft.world.entity.Entity e = type == null ? null : type.create(world);
            if (e == null) {
                return null;
            }
            e.moveTo(x, y, z, 0, 0);
            if (e instanceof net.minecraft.world.entity.Mob mob) {
                mob.finalizeSpawn(world, world.getCurrentDifficultyAt(mob.blockPosition()),
                        net.minecraft.world.entity.MobSpawnType.MOB_SUMMONED, null);
            }
            world.addFreshEntity(e);
            return e;
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (rift == null || !(boss.level() instanceof ServerLevel world)) {
                return;
            }
            net.minecraft.world.entity.Entity e = world.getEntity(rift);
            if (e == null || !e.isAlive()) {
                rift = null; // destroyed in time
            } else if (gameTick - riftAt >= QuestBossRules.SCABARAS_RIFT_FUSE_TICKS) {
                for (ServerPlayer p : world.getPlayers(x -> engaged(boss, x))) {
                    com.runecraft.combat.CombatHandler.applyDamage(null, p,
                            QuestBossRules.scabarasRiftDamage(new java.util.Random(boss.getRandom().nextLong())));
                    Msg.red(p, "The Shadow Rift explodes!");
                }
                e.discard();
                rift = null;
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            onRemoved(boss);
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            if (rift != null && boss.level() instanceof ServerLevel world && world.getEntity(rift) != null) {
                world.getEntity(rift).discard();
            }
            rift = null;
        }
    }

    /** Legends' Quest (wiki): Nezikchened drains the player's Prayer to a tenth of their level when he appears. */
    static final class Nezikchened implements BossScript {
        @Override
        public void onSpawn(DataBossEntity boss) {
            if (boss.getTarget() instanceof ServerPlayer p) {
                com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
                int cap = QuestBossRules.nezikchenedPrayerCap(data.skills().level(com.runecraft.osrs.Skill.PRAYER));
                if (data.prayerPoints() > cap) {
                    data.setPrayerPoints(cap);
                    com.runecraft.network.ServerNetworking.sync(p);
                    Msg.red(p, "Nezikchened drains your prayer!");
                }
            }
        }
    }

    /** Dragon Slayer I (wiki): every one of Elvarg's dragonfire breaths drains a tenth of the player's Prayer points. */
    static final class Elvarg implements BossScript {
        private boolean breathing;

        @Override
        public AttackDef beforeAttack(DataBossEntity boss, ServerPlayer target, AttackDef attack) {
            breathing = attack != null && attack.special() == AttackDef.Special.DRAGONFIRE;
            return attack;
        }

        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type) {
            if (breathing) {
                drain(player);
                breathing = false;
            }
            return damage;
        }

        /**
         * The hit's own attack: hits land in real time (round 41: the breath flies before it lands), so she may already have
         * started her next attack, and {@link #breathing} would describe that one.
         */
        @Override
        public int onHitPlayer(DataBossEntity boss, ServerPlayer player, int damage, AttackType type, AttackDef attack) {
            if (attack == null) {
                return onHitPlayer(boss, player, damage, type);
            }
            if (attack.special() == AttackDef.Special.DRAGONFIRE) {
                drain(player);
            }
            return damage;
        }

        private static void drain(ServerPlayer player) {
            com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(player);
            data.setPrayerPoints(data.prayerPoints() - data.prayerPoints() * QuestBossRules.ELVARG_PRAYER_DRAIN_PERCENT / 100);
        }
    }

    /**
     * Monkey Madness I (wiki): the 10th squad's gnomes fight the Jungle demon beside the player, chipping at it, but the
     * demon regenerates to a fifth of its hitpoints unless the player deals the final blow.
     */
    static final class JungleDemon implements BossScript {
        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (gameTick % QuestBossRules.GNOME_HIT_TICKS != 0 || !(boss.getTarget() instanceof ServerPlayer p)
                    || p.distanceToSqr(boss) > 24 * 24) {
                return;
            }
            int hit = boss.getRandom().nextInt(QuestBossRules.GNOME_MAX_HIT + 1);
            if (hit <= 0) {
                return;
            }
            if (hit >= boss.getHealth()) {
                boss.setHealth(Math.max(1, boss.getMaxHealth() * QuestBossRules.JUNGLE_DEMON_REGEN_PERCENT / 100f));
                announce(boss, "The jungle demon regenerates! Only you can deal the final blow.", ChatFormatting.RED);
                return;
            }
            com.runecraft.combat.CombatHandler.applyDamage(null, boss, hit);
        }
    }

    /**
     * Desert Treasure I (wiki): Dessous answers a player's protection prayer: against Protect from Melee he sends his bats
     * (half Magic, half Ranged, up to 10), otherwise he uses his fast melee attack. Hit from afar he teleports next to the
     * player; the third time he grows bored and returns to his grave (the blood must be poured again).
     */
    static final class Dessous implements BossScript {
        private int teleports;

        @Override
        public AttackDef chooseAttack(DataBossEntity boss, ServerPlayer target, double distance) {
            // strict on purpose: his choice answers the overhead he sees (the switch grace only decides damage)
            boolean meleePrayer = com.runecraft.player.Players.data(target).isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MELEE);
            String want = meleePrayer || distance > 3 ? "bat_swarm" : "vampyric_slash";
            if (boss.def() != null) {
                for (AttackDef a : boss.def().attacks) {
                    if (a.name().equals(want)) {
                        return a;
                    }
                }
            }
            return null;
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (attacker.distanceToSqr(boss) > 8 * 8) {
                if (++teleports >= QuestBossRules.DESSOUS_TELEPORTS) {
                    announce(boss, "Dessous: I grow bored of toying with you. *He sinks back into his grave.*", ChatFormatting.RED);
                    boss.discard();
                    return 0;
                }
                boss.teleportTo(attacker.getX() + 1, attacker.getY(), attacker.getZ());
                announce(boss, "Dessous appears beside you!", ChatFormatting.RED);
            }
            return damage;
        }
    }
}
