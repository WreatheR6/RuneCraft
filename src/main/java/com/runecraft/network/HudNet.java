package com.runecraft.network;

import com.runecraft.RuneCraft;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.combat.AutoRetaliate;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.PoisonService;
import com.runecraft.combat.PrayerService;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CastBar;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.PrayerGrace;
import com.runecraft.osrs.content.AttackDef;
import com.runecraft.platform.PacketByteBufs;
import com.runecraft.platform.ServerPlayConnectionEvents;
import com.runecraft.platform.ServerPlayNetworking;
import com.runecraft.platform.ServerTickEvents;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * The combat HUD's packets (round 42, {@code // playability}): the telegraph cast bar, the curse map of the protection keys,
 * the HP orb's poison tint and the auto-retaliate target, and the quick-eat / quick-drink keys.
 * <ul>
 *   <li>{@link #CAST} (S2C): one telegraph on its way to the player: source entity, style ({@link CastBar}), flags, MC ticks
 *       until it lands and an optional label. Sent by {@code ThreatCues.castBar} (every telegraphed AttackDef, Jad) and by
 *       boss scripts through {@code BossFx.cast}.</li>
 *   <li>{@link #CURSE} (S2C): what each protection key switches now (the Nightmare's curse through
 *       {@link PrayerService#REDIRECTS}), so the client's optimistic prayer HUD shows the cursed result at once instead of
 *       flashing the prayer clicked, and the cast bar names the key that really blocks.</li>
 *   <li>{@link #STATUS} (S2C): poison state (0 none, 1 poisoned, 2 envenomed) and the entity auto-retaliate is fighting
 *       (-1 none); sent when either changes.</li>
 *   <li>{@link #QUICK_CONSUME} (C2S): quick-eat (0) or quick-drink (1), see {@code skill.QuickConsume}.</li>
 * </ul>
 */
public final class HudNet {
    public static final ResourceLocation CAST = RuneCraft.id("hud/cast");
    public static final ResourceLocation CURSE = RuneCraft.id("hud/curse");
    public static final ResourceLocation STATUS = RuneCraft.id("hud/status");
    public static final ResourceLocation QUICK_CONSUME = RuneCraft.id("hud/quick_consume");

    /** Longest label sent with a cast. */
    public static final int LABEL_MAX = 48;

    /** One cast sent (self-test recorder). */
    public record SentCast(ServerPlayer player, int source, int style, int flags, int landInMc, String label) {
    }

    /** Self-test hook: sees every cast sent (or that would have been sent to a player without a client). */
    public static java.util.function.Consumer<SentCast> castListener = c -> {
    };

    /** What the client last got: the curse map and the status, per player. */
    private record Sent(int[] curse, int poison, int retaliate) {
    }

    private static final Map<UUID, Sent> SENT = new ConcurrentHashMap<>();
    /** The last few monsters that hit each player (ids, newest last): who auto-retaliate may be fighting. */
    private static final Map<UUID, int[]> ATTACKERS = new ConcurrentHashMap<>();
    private static final int ATTACKERS_KEPT = 4;

    private HudNet() {
    }

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(QUICK_CONSUME, (server, player, handler, buf, sender) -> {
            int kind = buf.readByte();
            server.execute(() -> com.runecraft.skill.QuickConsume.use(player, kind == 1));
        });
        ServerTickEvents.END_SERVER_TICK.register(HudNet::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> forget(handler.player.getUUID()));
        CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
            if (attacker != null && attacker != player) {
                noteAttacker(player, attacker);
            }
        });
    }

    /** Forgets a player (logout): the sent state, the attackers and the merged warning line. */
    public static void forget(UUID player) {
        SENT.remove(player);
        ATTACKERS.remove(player);
        com.runecraft.util.Msg.forget(player);
    }

    // ------------------------------------------------------------------ cast bar

    /**
     * The cast-bar flags of an attack ({@link CastBar#flagsOf}): a sky strike is dodged; dragonfire and prayer-piercing hits are
     * lowered by the protection prayer, so the row still names its key (Elvarg's telegraphed dragonfire under Protect from
     * Magic, the bears' knockback orb that Protect from Magic blocks).
     */
    public static int flagsOf(AttackType style, AttackDef.Special special) {
        return CastBar.flagsOf(special);
    }

    /** The cast-bar style of an attack (a sky strike is typeless: it hits a spot, whatever its style). */
    public static int styleOf(AttackType style, AttackDef.Special special) {
        return special == AttackDef.Special.SKY_STRIKE ? CastBar.TYPELESS : CastBar.styleOf(style);
    }

    /**
     * Shows a telegraph on a player's cast bar: {@code source}'s attack of {@code style} lands in {@code landInMc} Minecraft
     * ticks. {@code label} (nullable) replaces the source's name.
     */
    public static void cast(ServerPlayer player, Entity source, int style, int flags, int landInMc, String label) {
        if (player == null) {
            return;
        }
        String text = label == null ? "" : label.length() > LABEL_MAX ? label.substring(0, LABEL_MAX) : label;
        int id = source == null ? -1 : source.getId();
        castListener.accept(new SentCast(player, id, style, flags, landInMc, text));
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(id);
        buf.writeByte(style);
        buf.writeByte(flags);
        buf.writeVarInt(Math.max(0, landInMc));
        buf.writeUtf(text, LABEL_MAX * 4);
        ServerPlayNetworking.send(player, CAST, buf);
    }

    // ------------------------------------------------------------------ hitpoints

    /**
     * An entity's OSRS hitpoints now and at most: a data boss's true hitpoints (Minecraft health is capped at
     * {@link DataBossEntity#MAX_MC_HEALTH}, so the cap's share is scaled back up), otherwise its health (monsters and players
     * hold OSRS hitpoints as health).
     */
    public static int[] osrsHitpoints(LivingEntity e) {
        float max = Math.max(1f, e.getMaxHealth());
        float health = Math.max(0f, e.getHealth());
        if (e instanceof DataBossEntity boss) {
            int trueMax = Math.max(1, boss.trueHitpoints());
            int now = health <= 0 ? 0 : Math.max(1, (int) Math.ceil(health / max * trueMax - 1e-4));
            return new int[]{Math.min(now, trueMax), trueMax};
        }
        int now = health <= 0 ? 0 : Math.max(1, (int) Math.ceil(health - 1e-4));
        return new int[]{now, Math.max(1, Math.round(max))};
    }

    // ------------------------------------------------------------------ curse map and status

    private static void noteAttacker(ServerPlayer player, LivingEntity attacker) {
        int[] ids = ATTACKERS.computeIfAbsent(player.getUUID(), u -> {
            int[] a = new int[ATTACKERS_KEPT];
            Arrays.fill(a, -1);
            return a;
        });
        int id = attacker.getId();
        if (ids[ids.length - 1] == id) {
            return;
        }
        System.arraycopy(ids, 1, ids, 0, ids.length - 1);
        ids[ids.length - 1] = id;
    }

    /** What each protection key switches now: {@code curse[i]} is the protection (index) a press of key {@code i} toggles. */
    public static int[] curseMap(ServerPlayer player) {
        int[] map = new int[PrayerGrace.PROTECTIONS.length];
        for (int i = 0; i < map.length; i++) {
            Prayer to = PrayerGrace.PROTECTIONS[i];
            for (java.util.function.BiFunction<ServerPlayer, Prayer, Prayer> r : PrayerService.REDIRECTS) {
                Prayer next = r.apply(player, to);
                to = next == null ? to : next;
            }
            int index = PrayerGrace.index(to);
            map[i] = index < 0 ? i : index;
        }
        return map;
    }

    /** Poison state for the HP orb: 2 envenomed, 1 poisoned, 0 neither. */
    public static int poisonState(ServerPlayer player) {
        return PoisonService.isEnvenomed(player) ? 2 : PoisonService.isPoisoned(player) ? 1 : 0;
    }

    /** The entity auto-retaliate is fighting for this player (one of the monsters that hit them lately), or -1. */
    public static int retaliateTarget(ServerPlayer player) {
        LivingEntity last = player.getLastHurtByMob();
        if (last != null && AutoRetaliate.retaliating(player, last)) {
            return last.getId();
        }
        int[] ids = ATTACKERS.get(player.getUUID());
        if (ids != null) {
            for (int i = ids.length - 1; i >= 0; i--) {
                if (ids[i] >= 0 && player.level().getEntity(ids[i]) instanceof LivingEntity e && AutoRetaliate.retaliating(player, e)) {
                    return e.getId();
                }
            }
        }
        return -1;
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            update(p);
        }
    }

    /** Sends the curse map and the status to a player when they changed (every MC tick for listed players; self-tests call it for the fake player). */
    public static void update(ServerPlayer p) {
        int[] curse = curseMap(p);
        int poison = poisonState(p);
        int retaliate = retaliateTarget(p);
        Sent last = SENT.get(p.getUUID());
        if (last == null || !Arrays.equals(last.curse(), curse)) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            for (int c : curse) {
                buf.writeByte(c);
            }
            ServerPlayNetworking.send(p, CURSE, buf);
        }
        if (last == null || last.poison() != poison || last.retaliate() != retaliate) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeByte(poison);
            buf.writeVarInt(retaliate);
            ServerPlayNetworking.send(p, STATUS, buf);
        }
        SENT.put(p.getUUID(), new Sent(curse, poison, retaliate));
    }

    /** Self-test hook: the state last sent to a player (curse map, poison, retaliate target), or null. */
    public static int[] lastSent(ServerPlayer p) {
        Sent s = SENT.get(p.getUUID());
        if (s == null) {
            return null;
        }
        int[] out = Arrays.copyOf(s.curse(), s.curse().length + 2);
        out[s.curse().length] = s.poison();
        out[s.curse().length + 1] = s.retaliate();
        return out;
    }
}
