package com.runecraft.content.quests;

import com.runecraft.boss.BossScript;
import com.runecraft.boss.BossScripts;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.combat.Aggression;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.content.systems.SpellService;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.MageArena;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.platform.ServerLifecycleEvents;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Mage Arena I and II in the world ({@link MageArena}): Kolodion's summoning stone raises the form the player left off at
 * and each form's death brings the next; god spells need Mage Arena I and, outside the arena, 100 casts on its battle
 * mages; the battle mages leave their own god's cape-wearers alone; the statues hand out more capes and the chamber
 * guardian the staff of the god first prayed to. Mage Arena II: the enchanted symbol (hot and cold, up to 16 damage, the
 * followers' spots rotating every 45 minutes in the Wilderness), the followers that only their own god spell harms with
 * their specials (Porazdir's energy ball, Zachariah's shock wave, Derwen's healing orbs), and imbuing more capes after it.
 */
final class MageArenaService {
    private MageArenaService() {
    }

    /** Seconds the player has spent in the Wilderness (the followers' rotation timer) and the last rotation they saw. */
    static final String WILD_SECONDS = "ma2_wild_seconds";
    static final String ROTATION_SEEN = "ma2_rotation_seen";
    /** The follower the symbol is attuned to (index into {@link MageArena#FOLLOWERS}). */
    static final String ATTUNED = "ma2_attuned";
    private static long ticks;
    /** Players who just raised Kolodion at the stone (his demon then turns into the form they left off at). */
    private static final java.util.Map<UUID, Long> SUMMONED = new java.util.HashMap<>();

    static void init() {
        // Kolodion: the stone raises the form the player left off at (QuestInterpreter decides whether they may fight).
        DataBossService.requirement(MageArena.KOLODION, (player, def) -> {
            if (!QuestInterpreter.canFight(player, def.id)) {
                return false;
            }
            if (kolodionNear(player) != null) {
                Msg.game(player, "Kolodion is already here!");
                return false;
            }
            SUMMONED.put(player.getUUID(), player.level().getGameTime());
            return true;
        });
        for (MageArena.Form f : MageArena.KOLODION_FORMS) {
            BossScripts.register(f.id(), KolodionForm::new);
        }
        for (MageArena.Follower f : MageArena.FOLLOWERS) {
            BossScripts.register(f.id(), () -> new FollowerScript(f));
        }
        SpellService.castRule = MageArenaService::castRule;
        SpellService.onCombatCast(MageArenaService::counted);
        Aggression.addPeaceful((mob, player) -> MageArena.battleMagePeaceful(creatureId(mob), cape(player)));
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !MageArena.SYMBOL.equals(Inv.idOf(stack))) {
                return InteractionResultHolder.pass(stack);
            }
            if (!world.isClientSide() && player instanceof ServerPlayer sp && !sp.getCooldowns().isOnCooldown(stack.getItem())) {
                sp.getCooldowns().addCooldown(stack.getItem(), 12);
                if (sp.isShiftKeyDown()) {
                    attune(sp);
                } else {
                    activate(sp);
                }
            }
            return InteractionResultHolder.success(stack);
        });
        wrap(MageArena.KOLODION_NPC, MageArenaService::kolodion);
        for (String god : MageArena.GODS) {
            wrap("quest_statue_" + god, (p, npc) -> statue(p, god));
        }
        // The chamber guardian belongs to the npcs module, whose talk handler may be registered after this one.
        ServerLifecycleEvents.SERVER_STARTED.register(server -> wrap("npc_chamber_guardian", MageArenaService::chamberGuardian));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks % 20 != 0) {
                return;
            }
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                if (Gielinor.wildernessLevel(p) > 0 && symbolStarted(p)) {
                    QuestsState.get(p).addKill(p.getUUID(), WILD_SECONDS);
                }
            }
        });
    }

    /** Runs {@code mine} first; if it doesn't handle the talk, the NPC's earlier handler (quest dialogue, shop) runs. */
    private static void wrap(String npc, BiPredicate<ServerPlayer, QuestNpcEntity> mine) {
        BiConsumer<ServerPlayer, QuestNpcEntity> earlier = QuestScripts.handler(npc);
        QuestScripts.onTalk(npc, (p, entity) -> {
            if (!mine.test(p, entity) && earlier != null) {
                earlier.accept(p, entity);
            }
        });
    }

    static String creatureId(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
    }

    static String cape(ServerPlayer p) {
        return SysState.of(p).wornId(Jewellery.Slot.CAPE);
    }

    static int tileX(Entity e) {
        return GielinorMap.toOsrsX(e.getBlockX());
    }

    static int tileY(Entity e) {
        return GielinorMap.toOsrsY(e.getBlockZ());
    }

    /** On the surface (not in the dungeons carved below the Wilderness). */
    static boolean aboveGround(Entity e) {
        return e.getY() >= 45;
    }

    /** Inside the Mage Arena's ring. */
    static boolean inArena(Entity e) {
        return Gielinor.is(e) && aboveGround(e) && MageArena.inArena(tileX(e), tileY(e));
    }

    static boolean flag(ServerPlayer p, String flag) {
        return QuestsState.get(p).hasFlag(p.getUUID(), flag);
    }

    // ------------------------------------------------------------------ god spells

    /** Why a god spell (or Charge) can't be cast here, or null. */
    static String castRule(ServerPlayer p, String spellId) {
        boolean ma1 = QuestApi.isComplete(p, "mage_arena_i");
        if (spellId.equals("charge")) {
            return ma1 ? null : "You need to complete Mage Arena I to cast Charge.";
        }
        String god = MageArena.spellGod(spellId);
        if (god == null) {
            return null;
        }
        return MageArena.castDenied(spellId, ma1, inArena(p), flag(p, MageArena.chargedFlag(god)));
    }

    /** A god spell cast on a battle mage in the arena counts towards the 100 that charge it. */
    static void counted(ServerPlayer p, String spellId, LivingEntity target) {
        String god = MageArena.spellGod(spellId);
        if (god == null || target == null || MageArena.battleMageGod(creatureId(target)) == null || !inArena(p)
                || flag(p, MageArena.chargedFlag(god))) {
            return;
        }
        QuestsState state = QuestsState.get(p);
        int casts = state.addKill(p.getUUID(), MageArena.castCounter(god));
        if (casts >= MageArena.CASTS_TO_CHARGE) {
            state.setFlag(p.getUUID(), MageArena.chargedFlag(god));
            p.displayClientMessage(Component.literal("You have now mastered " + spellName(spellId)
                    + "! You can now cast it outside the Mage Arena.").withStyle(ChatFormatting.GOLD), false);
        }
    }

    static String spellName(String spellId) {
        return switch (spellId) {
            case "saradomin_strike" -> "Saradomin Strike";
            case "claws_of_guthix" -> "Claws of Guthix";
            default -> "Flames of Zamorak";
        };
    }

    // ------------------------------------------------------------------ NPCs

    /** Kolodion after the miniquests: spell progress, imbuing more capes from remains. */
    private static boolean kolodion(ServerPlayer p, QuestNpcEntity npc) {
        if (!QuestApi.isComplete(p, "mage_arena_ii")) {
            if (QuestApi.isComplete(p, "mage_arena_i") && QuestApi.stage(p, "mage_arena_ii") == 0 && !allCharged(p)) {
                QuestsState state = QuestsState.get(p);
                StringBuilder b = new StringBuilder("Your god spells: ");
                for (int i = 0; i < MageArena.GODS.length; i++) {
                    String god = MageArena.GODS[i];
                    int casts = flag(p, MageArena.chargedFlag(god)) ? MageArena.CASTS_TO_CHARGE
                            : Math.min(MageArena.CASTS_TO_CHARGE, state.kills(p.getUUID(), MageArena.castCounter(god)));
                    b.append(i > 0 ? ", " : "").append(spellName(MageArena.GOD_SPELLS[i])).append(' ').append(casts).append('/')
                            .append(MageArena.CASTS_TO_CHARGE);
                }
                QuestApi.say(p, "Kolodion", b + ".");
                // Mage Arena II needs all three cast 100 times in the arena before it starts.
                QuestApi.say(p, "Kolodion", "Master all three god spells in my arena and I may have another challenge for you.");
                return true;
            }
            return false;
        }
        // "bring the respective component of the god cape that they wish to imbue to Kolodion"
        for (MageArena.Follower f : MageArena.FOLLOWERS) {
            String cape = f.god() + "_cape";
            if (Inv.has(p, f.remains(), 1) && Inv.has(p, cape, 1)) {
                Inv.remove(p, f.remains(), 1);
                Inv.remove(p, cape, 1);
                Inv.give(p, f.imbuedCape(), 1);
                QuestApi.say(p, "Kolodion", "*Kolodion chants.* Your cape is imbued with " + capitalise(f.god()) + "'s power.");
                return true;
            }
        }
        for (MageArena.Follower f : MageArena.FOLLOWERS) {
            if (Inv.has(p, f.remains(), 1)) {
                QuestApi.say(p, "Kolodion", "Bring me a " + f.god() + " cape too, and I'll imbue it.");
                return true;
            }
        }
        if (!Inv.has(p, MageArena.SYMBOL, 1)) {
            Inv.give(p, MageArena.SYMBOL, 1);
            QuestApi.say(p, "Kolodion", "Hunting the followers again? Take this symbol. Bring me their remains and a god cape.");
            return true;
        }
        return false;
    }

    private static boolean allCharged(ServerPlayer p) {
        for (String god : MageArena.GODS) {
            if (!flag(p, MageArena.chargedFlag(god))) {
                return false;
            }
        }
        return true;
    }

    /** "Players can receive multiple god capes ... by praying again at the relevant statue." */
    private static boolean statue(ServerPlayer p, String god) {
        if (!QuestApi.isComplete(p, "mage_arena_i")) {
            return false;
        }
        Inv.give(p, god + "_cape", 1);
        Msg.game(p, "You pray at the statue and receive a " + capitalise(god) + " cape.");
        return true;
    }

    /** The chamber guardian hands over the staff of the god first prayed to (later staves are bought at his shop). */
    private static boolean chamberGuardian(ServerPlayer p, QuestNpcEntity npc) {
        QuestsState state = QuestsState.get(p);
        for (String god : MageArena.GODS) {
            if (state.hasFlag(p.getUUID(), MageArena.staffOwedFlag(god))) {
                state.clearFlag(p.getUUID(), MageArena.staffOwedFlag(god));
                Inv.give(p, god + "_staff", 1);
                QuestApi.say(p, "Chamber Guardian", "Ah, you prayed to " + capitalise(god) + ". Here is the staff of your god.");
                return true;
            }
        }
        return false;
    }

    static String capitalise(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ------------------------------------------------------------------ Kolodion

    /** Command tag (saved with the entity) naming the player who raised a Kolodion form at the stone. */
    static final String SUMMONER_TAG = "ma1_summoner:";

    /** The player who raised this Kolodion form, or null (a form not raised at the stone). */
    static UUID summoner(Entity form) {
        for (String tag : form.getTags()) {
            if (tag.startsWith(SUMMONER_TAG)) {
                try {
                    return UUID.fromString(tag.substring(SUMMONER_TAG.length()));
                } catch (IllegalArgumentException e) {
                    return null;
                }
            }
        }
        return null;
    }

    static void setSummoner(DataBossEntity form, UUID player) {
        form.instanceTag = "ma1";
        for (String tag : List.copyOf(form.getTags())) {
            if (tag.startsWith(SUMMONER_TAG)) {
                form.removeTag(tag);
            }
        }
        form.addTag(SUMMONER_TAG + player);
    }

    /** False for a kill of a Kolodion form someone else raised: it counts for nobody's miniquest but its summoner's. */
    static boolean killCounts(ServerPlayer p, LivingEntity entity) {
        if (!(entity instanceof DataBossEntity boss) || boss.def() == null || MageArena.formIndex(boss.def().id) < 0) {
            return true;
        }
        UUID owner = summoner(boss);
        return owner == null || owner.equals(p.getUUID());
    }

    /** A living Kolodion form the player raised near them, or null. */
    static DataBossEntity kolodionNear(ServerPlayer p) {
        for (MageArena.Form f : MageArena.KOLODION_FORMS) {
            EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(f.id());
            if (type == null) {
                continue;
            }
            List<DataBossEntity> found = p.serverLevel().getEntities(type, p.getBoundingBox().inflate(64),
                    e -> e.isAlive() && p.getUUID().equals(summoner(e)));
            if (!found.isEmpty()) {
                return found.get(0);
            }
        }
        return null;
    }

    /** The form index a player continues the fight at. */
    static int formReached(ServerPlayer p) {
        return Math.max(0, Math.min(MageArena.KOLODION_FORMS.size() - 1, QuestsState.get(p).kills(p.getUUID(), MageArena.FORM_COUNTER)));
    }

    static void setFormReached(ServerPlayer p, int index) {
        QuestsState state = QuestsState.get(p);
        state.clearKills(p.getUUID(), MageArena.FORM_COUNTER);
        for (int i = 0; i < index; i++) {
            state.addKill(p.getUUID(), MageArena.FORM_COUNTER);
        }
    }

    /** Swaps a Kolodion form for another at the same spot (same lair), aimed at the player. */
    static DataBossEntity swap(DataBossEntity from, String formId, ServerPlayer target) {
        BossDef def = ContentRegistry.boss(formId);
        if (def == null || !(from.level() instanceof ServerLevel world)) {
            return null;
        }
        DataBossEntity next = DataBossService.spawn(world, from.blockPosition(), def, target);
        if (next != null) {
            next.copyHomeFrom(from);
            setSummoner(next, target.getUUID());
            world.sendParticles(ParticleTypes.WITCH, from.getX(), from.getY() + 1, from.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
        }
        from.discard();
        return next;
    }

    /**
     * Each form: a fresh summon of the demon (the stone's boss) turns into the form the player left off at; a form's death
     * brings the next one; the demon's death ends the fight (the miniquest's kill). The forms are the summoner's alone: nobody
     * else can harm them, and only the summoner's kill counts.
     */
    static final class KolodionForm implements BossScript {
        private boolean checked;
        private long lastMessage;

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            UUID owner = summoner(boss);
            if (attacker == null || owner == null || owner.equals(attacker.getUUID())) {
                return damage;
            }
            long now = boss.level().getGameTime();
            if (now - lastMessage > 40) {
                lastMessage = now;
                Msg.action(attacker, "Kolodion is fighting someone else.");
            }
            return 0;
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            if (checked) {
                return;
            }
            checked = true;
            if (boss.instanceTag != null || boss.def() == null || !boss.def().id.equals(MageArena.KOLODION)
                    || !(boss.getTarget() instanceof ServerPlayer p)) {
                return;
            }
            Long at = SUMMONED.remove(p.getUUID());
            if (at == null || boss.level().getGameTime() - at > 100) {
                return; // not raised at the stone (a test spawn): stays as spawned
            }
            setSummoner(boss, p.getUUID());
            int reached = formReached(p);
            if (reached < MageArena.KOLODION_FORMS.size() - 1) {
                swap(boss, MageArena.KOLODION_FORMS.get(reached).id(), p);
                if (reached > 0) {
                    Msg.game(p, "Kolodion takes up the fight where you left it.");
                }
            }
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            // only the stone's fight chains its forms (a form spawned on its own, e.g. by a test, just dies), and only for the
            // player who raised it
            UUID owner = summoner(boss);
            if (boss.def() == null || killer == null || owner == null || !owner.equals(killer.getUUID())) {
                return;
            }
            String next = MageArena.nextForm(boss.def().id);
            if (next == null) {
                setFormReached(killer, 0);
                return;
            }
            setFormReached(killer, MageArena.formIndex(next));
            swapAfterDeath(boss, next, killer); // the miniquest's kill lines say which form he takes
        }

        private static DataBossEntity swapAfterDeath(DataBossEntity dead, String formId, ServerPlayer target) {
            BossDef def = ContentRegistry.boss(formId);
            if (def == null || !(dead.level() instanceof ServerLevel world)) {
                return null;
            }
            DataBossEntity next = DataBossService.spawn(world, dead.blockPosition(), def, target);
            if (next != null) {
                next.copyHomeFrom(dead);
                setSummoner(next, target.getUUID());
                world.sendParticles(ParticleTypes.WITCH, dead.getX(), dead.getY() + 1, dead.getZ(), 40, 0.6, 1.0, 0.6, 0.05);
            }
            return next;
        }
    }

    // ------------------------------------------------------------------ Mage Arena II: the symbol

    /** The symbol tracks followers once Kolodion has handed it over (and again after the miniquest, for more remains). */
    static boolean symbolStarted(ServerPlayer p) {
        return QuestApi.stage(p, "mage_arena_ii") >= 1 || QuestApi.isComplete(p, "mage_arena_ii");
    }

    /** Followers the symbol can still track: during the miniquest, not those whose remains Kolodion holds. */
    static List<Integer> trackable(ServerPlayer p) {
        List<Integer> out = new ArrayList<>();
        boolean done = QuestApi.isComplete(p, "mage_arena_ii");
        for (int i = 0; i < MageArena.FOLLOWERS.size(); i++) {
            if (done || !flag(p, MageArena.handedFlag(MageArena.FOLLOWERS.get(i)))) {
                out.add(i);
            }
        }
        return out;
    }

    static int attuned(ServerPlayer p) {
        List<Integer> can = trackable(p);
        if (can.isEmpty()) {
            return -1;
        }
        int a = QuestsState.get(p).kills(p.getUUID(), ATTUNED) % MageArena.FOLLOWERS.size();
        for (int k = 0; k < MageArena.FOLLOWERS.size(); k++) {
            int i = (a + k) % MageArena.FOLLOWERS.size();
            if (can.contains(i)) {
                return i;
            }
        }
        return can.get(0);
    }

    /** Sneak + use: attune the symbol to the next follower. */
    static void attune(ServerPlayer p) {
        if (!symbolStarted(p)) {
            Msg.game(p, "The symbol is dull. Kolodion would know what it's for.");
            return;
        }
        int now = attuned(p);
        QuestsState state = QuestsState.get(p);
        state.clearKills(p.getUUID(), ATTUNED);
        List<Integer> can = trackable(p);
        int next = now;
        for (int k = 1; k <= MageArena.FOLLOWERS.size(); k++) {
            int i = (now + k) % MageArena.FOLLOWERS.size();
            if (can.contains(i)) {
                next = i;
                break;
            }
        }
        for (int i = 0; i < next; i++) {
            state.addKill(p.getUUID(), ATTUNED);
        }
        if (next >= 0) {
            Msg.game(p, "You attune the symbol to " + MageArena.FOLLOWERS.get(next).name() + ".");
        }
    }

    static int rotation(ServerPlayer p) {
        return QuestsState.get(p).kills(p.getUUID(), WILD_SECONDS) / MageArena.ROTATION_SECONDS;
    }

    static long seed(UUID id) {
        return id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 17);
    }

    /** Where the player's follower currently is (OSRS tile). */
    static int[] spot(ServerPlayer p, int follower) {
        return MageArena.followerSpot(seed(p.getUUID()), follower, rotation(p));
    }

    /** Use: up to 16 damage, then the temperature and direction; at the spot the follower appears. */
    static void activate(ServerPlayer p) {
        if (!symbolStarted(p)) {
            Msg.game(p, "The symbol is dull. Kolodion would know what it's for.");
            return;
        }
        if (Gielinor.wildernessLevel(p) <= 0 || !aboveGround(p)) {
            Msg.game(p, "The symbol doesn't seem to work here, I should try using it in the Wilderness.");
            return;
        }
        int f = attuned(p);
        if (f < 0) {
            Msg.game(p, "Kolodion already holds all three remains; the symbol has nothing left to find.");
            return;
        }
        QuestsState state = QuestsState.get(p);
        int rot = rotation(p);
        if (state.kills(p.getUUID(), ROTATION_SEEN) != rot) {
            state.clearKills(p.getUUID(), ROTATION_SEEN);
            for (int i = 0; i < rot; i++) {
                state.addKill(p.getUUID(), ROTATION_SEEN);
            }
            if (rot > 0) {
                Msg.game(p, "The symbol flickers: the followers have moved on.");
            }
        }
        int damage = p.getRandom().nextInt(MageArena.SYMBOL_MAX_DAMAGE + 1);
        if (damage > 0) {
            CombatHandler.applyDamage(null, p, damage);
        }
        MageArena.Follower follower = MageArena.FOLLOWERS.get(f);
        int[] at = spot(p, f);
        int steps = MageArena.steps(tileX(p), tileY(p), at[0], at[1]);
        String temp = MageArena.temperature(steps);
        if (steps <= MageArena.SHAKING) {
            Msg.red(p, "The symbol is visibly shaking! " + follower.name() + " appears!");
            summonFollower(p, follower);
            return;
        }
        Msg.game(p, "The symbol attuned to " + follower.name() + " feels " + temp + ". It pulls towards the "
                + MageArena.direction(tileX(p), tileY(p), at[0], at[1]) + ".");
    }

    /** The follower appears beside the player (unless one is already there). */
    static DataBossEntity summonFollower(ServerPlayer p, MageArena.Follower f) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(f.id());
        BossDef def = ContentRegistry.boss(f.id());
        if (type == null || def == null) {
            return null;
        }
        List<DataBossEntity> here = p.serverLevel().getEntities(type, p.getBoundingBox().inflate(48), Entity::isAlive);
        if (!here.isEmpty()) {
            return here.get(0);
        }
        Vec3 dir = p.getLookAngle().multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-4) {
            dir = new Vec3(0, 0, 1);
        }
        dir = dir.normalize().scale(4);
        BlockPos at = Gielinor.safeSpot(p.serverLevel(), (int) Math.floor(p.getX() + dir.x), (int) Math.floor(p.getZ() + dir.z));
        return DataBossService.spawn(p.serverLevel(), at, def, p);
    }

    // ------------------------------------------------------------------ Mage Arena II: the followers

    /**
     * A god follower: only its own god spell harms it; every so often its special (Porazdir's energy ball through prayer,
     * weaker with distance; Zachariah's shock wave that pulls and binds a player who stays on its tile; Derwen's healing
     * orbs); it leaves when nobody fights it.
     */
    static final class FollowerScript implements BossScript {
        final MageArena.Follower f;
        private long lastMessage;
        private int ticks;
        private int idle;
        private int nextSpecial = 20;
        private int ballTicks = -1;
        private Vec3 wave;
        private int waveTicks = -1;
        private int bound;
        private final List<Mob> orbs = new ArrayList<>();

        FollowerScript(MageArena.Follower f) {
            this.f = f;
        }

        @Override
        public int modifyIncoming(DataBossEntity boss, ServerPlayer attacker, int damage, AttackType type) {
            if (MageArena.harms(f, SpellService.castingSpellId())) {
                return damage;
            }
            long now = boss.level().getGameTime();
            if (attacker != null && now - lastMessage > 40) {
                lastMessage = now;
                Msg.action(attacker, "Only " + spellName(f.spell()) + " can harm " + f.name() + ".");
            }
            return 0;
        }

        @Override
        public void gameTick(DataBossEntity boss, long gameTick) {
            ticks++;
            ServerPlayer target = boss.getTarget() instanceof ServerPlayer p && p.isAlive() && p.distanceTo(boss) < 32 ? p : null;
            idle = target == null ? idle + 1 : 0;
            if (idle > 100 || ticks > MageArena.DESPAWN_TICKS && target == null) {
                say(boss, f.name() + " loses interest and fades away.");
                clearOrbs();
                boss.discard();
                return;
            }
            orbs.removeIf(o -> !o.isAlive());
            if (!orbs.isEmpty() && ticks % MageArena.ORB_HEAL_TICKS == 0) {
                boss.heal(MageArena.ORB_HEAL * orbs.size() * boss.getMaxHealth() / Math.max(1, boss.trueHitpoints()));
            }
            if (target == null) {
                return;
            }
            if (ballTicks >= 0 && --ballTicks < 0) {
                int tiles = (int) Math.floor(target.distanceTo(boss) / 2.0);
                int max = MageArena.energyBallMax(tiles);
                CombatHooks.nameAttack("energy_ball", null);
                CombatHandler.applyDamage(boss, target, boss.getRandom().nextInt(max + 1));
            }
            if (waveTicks >= 0 && --waveTicks < 0 && wave != null) {
                if (target.position().distanceTo(wave) < 1.5) {
                    target.teleportTo(boss.getX() + 1.5, boss.getY(), boss.getZ());
                    com.runecraft.skill.Stun.apply(target, MageArena.BIND_TICKS);
                    bound = MageArena.BIND_TICKS;
                    Msg.red(target, "The shock wave drags you to Zachariah and binds you in place!");
                }
                wave = null;
            }
            if (bound > 0) {
                if (--bound % MageArena.BOUND_SWING_TICKS == 0) {
                    for (AttackDef a : boss.def().attacks) {
                        if (a.style().isMelee()) {
                            com.runecraft.boss.Mechanics.perform(boss, target, a, 1.0);
                            break;
                        }
                    }
                }
            }
            if (--nextSpecial > 0) {
                return;
            }
            nextSpecial = 25 + boss.getRandom().nextInt(10); // approx: the wiki gives no rate
            switch (f.god()) {
                case "zamorak" -> {
                    say(boss, "Porazdir fires a ball of energy directly linked to his power!");
                    ballTicks = 3;
                }
                case "saradomin" -> {
                    wave = target.position();
                    waveTicks = 2;
                    if (boss.level() instanceof ServerLevel world) {
                        world.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, wave.x, wave.y + 0.2, wave.z, 30, 0.4, 0.1, 0.4, 0.01);
                    }
                    say(boss, "Justiciar Zachariah swings his sword, sending out a shock wave!");
                }
                default -> spawnOrbs(boss, target);
            }
        }

        private void spawnOrbs(DataBossEntity boss, ServerPlayer target) {
            if (!(boss.level() instanceof ServerLevel world) || orbs.size() >= 3) {
                return;
            }
            EntityType<?> type = ModEntities.typeById(MageArena.ORB);
            if (type == null) {
                return;
            }
            say(boss, "Derwen releases balls of healing energy!");
            for (int i = orbs.size(); i < 3; i++) {
                double a = boss.getRandom().nextDouble() * Math.PI * 2;
                Entity e = type.create(world);
                if (!(e instanceof Mob orb)) {
                    return;
                }
                orb.moveTo(boss.getX() + Math.cos(a) * 3, boss.getY(), boss.getZ() + Math.sin(a) * 3, 0, 0);
                boss.claimMinion(orb);
                world.addFreshEntity(orb);
                orbs.add(orb);
            }
        }

        private void clearOrbs() {
            orbs.forEach(Entity::discard);
            orbs.clear();
        }

        @Override
        public void onDeath(DataBossEntity boss, ServerPlayer killer) {
            clearOrbs();
        }

        @Override
        public void onRemoved(DataBossEntity boss) {
            clearOrbs();
        }
    }

    static void say(Entity boss, String message) {
        if (boss.level() instanceof ServerLevel world) {
            for (ServerPlayer p : world.getEntitiesOfClass(ServerPlayer.class, new AABB(boss.blockPosition()).inflate(32))) {
                p.displayClientMessage(Component.literal(message).withStyle(ChatFormatting.RED), false);
            }
        }
    }
}
