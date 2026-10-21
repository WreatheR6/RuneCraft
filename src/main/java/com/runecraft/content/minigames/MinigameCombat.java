package com.runecraft.content.minigames;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.CombatHooks;
import com.runecraft.combat.Equipment;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.CombatStyle;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.content.minigames.MinigameRules;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.platform.AttackEntityCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.Set;

/**
 * Combat and experience effects of minigame rewards: Void Knight and Elite Void set effects,
 * obsidian armour with TzHaar weapons, the tomes, the Castle Wars bracelet, minigame shields
 * (Pest Control portals), the skilling outfits' experience bonus, and team PvP inside Castle Wars.
 */
public final class MinigameCombat {
    /** Creatures carrying this tag can't be damaged (e.g. shielded Pest Control portals). */
    public static final String INVULNERABLE = "rcmg_invulnerable";

    private static final Set<String> TZHAAR_MELEE = Set.of("tzhaar_ket_om", "tzhaar_ket_em", "toktz_xil_ak", "toktz_xil_ek");
    private static final Map<Skill, String[]> OUTFITS = Map.of(
            Skill.FIREMAKING, new String[]{"pyromancer_hood", "pyromancer_garb", "pyromancer_robe", "pyromancer_boots"},
            Skill.FISHING, new String[]{"angler_hat", "angler_top", "angler_waders", "angler_boots"},
            Skill.FARMING, new String[]{"farmers_strawhat", "farmers_jacket", "farmers_boro_trousers", "farmers_boots"},
            Skill.CONSTRUCTION, new String[]{"carpenters_helmet", "carpenters_shirt", "carpenters_trousers", "carpenters_boots"},
            Skill.WOODCUTTING, new String[]{"lumberjack_hat", "lumberjack_top", "lumberjack_legs", "lumberjack_boots"});
    /** OSRS outfit bonus per piece (hat, top, legs, boots); the full set gives 2.5%. */
    private static final double[] PIECE_BONUS = {0.004, 0.008, 0.006, 0.002};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static boolean addingBonus;

    private MinigameCombat() {
    }

    static String worn(ServerPlayer p, EquipmentSlot slot) {
        return Inv.idOf(p.getItemBySlot(slot));
    }

    /** Void style worn: 0 melee, 1 ranged, 2 magic, or -1 without a full set. */
    static int voidStyle(ServerPlayer p) {
        String top = worn(p, EquipmentSlot.CHEST);
        String legs = worn(p, EquipmentSlot.LEGS);
        if (!(top.equals("void_knight_top") || top.equals("elite_void_top"))
                || !(legs.equals("void_knight_robe") || legs.equals("elite_void_robe"))
                || !(com.runecraft.content.systems.SysState.of(p).wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.HANDS)
                        .equals("void_knight_gloves") || Inv.has(p, "void_knight_gloves", 1))) {
            return -1;
        }
        return switch (worn(p, EquipmentSlot.HEAD)) {
            case "void_melee_helm" -> 0;
            case "void_ranger_helm" -> 1;
            case "void_mage_helm" -> 2;
            default -> -1;
        };
    }

    static boolean eliteVoid(ServerPlayer p) {
        return worn(p, EquipmentSlot.CHEST).equals("elite_void_top") && worn(p, EquipmentSlot.LEGS).equals("elite_void_robe");
    }

    private static int styleIndex(AttackType type) {
        return type == AttackType.RANGED ? 1 : type == AttackType.MAGIC ? 2 : 0;
    }

    private static boolean obsidianEffect(ServerPlayer p, AttackType type) {
        return type.isMelee() && worn(p, EquipmentSlot.HEAD).equals("obsidian_helmet")
                && worn(p, EquipmentSlot.CHEST).equals("obsidian_platebody")
                && worn(p, EquipmentSlot.LEGS).equals("obsidian_platelegs")
                && TZHAAR_MELEE.contains(Inv.idOf(p.getMainHandItem()));
    }

    static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            // Void knight equipment, the obsidian armour set and the tomes are part of com.runecraft.osrs.PlayerHitCalc
            // (void: effective level x1.1, mage x1.45 accuracy; obsidian: +10% of the base roll / max hit; tome of
            // fire / water: x1.1 for matching spells), applied through combat.HitCalcBridge.
            @Override
            public double maxHit(ServerPlayer player, LivingEntity target, AttackType type) {
                double m = 1.0;
                MinigameSession s = MinigameManager.session(player);
                if (s != null) {
                    m *= s.damageMultiplier(player, target, type);
                }
                return m;
            }

            @Override
            public int afterRoll(ServerPlayer player, LivingEntity target, AttackType type, int damage) {
                if (target.getTags().contains(INVULNERABLE)) {
                    Msg.action(player, "It's protected by a shield.");
                    return 0;
                }
                return damage;
            }

            @Override
            public void afterHit(ServerPlayer player, LivingEntity target, AttackType type, int dealt) {
                MinigameSession s = MinigameManager.owner(target);
                if (s != null && !s.isEnded()) {
                    s.onPlayerHit(player, target, type, dealt);
                }
            }
        });
        // Sessions that give no experience (Nightmare Zone practice dreams); Slayer task credit follows the Slayer XP.
        XpService.addGate((player, skill) -> {
            MinigameSession s = MinigameManager.session(player);
            return s == null || s.isEnded() || s.awardsXp();
        });
        GameEvents.onXp((player, skill, tenths) -> {
            String[] outfit = OUTFITS.get(skill);
            if (outfit == null || addingBonus || tenths <= 0) {
                return;
            }
            double bonus = 0;
            int pieces = 0;
            for (int i = 0; i < 4; i++) {
                if (worn(player, SLOTS[i]).equals(outfit[i])) {
                    bonus += PIECE_BONUS[i];
                    pieces++;
                }
            }
            if (pieces == 4) {
                bonus = 0.025;
            }
            int extra = (int) Math.round(tenths * bonus);
            if (extra > 0) {
                addingBonus = true;
                try {
                    XpService.addTenths(player, skill, extra);
                } finally {
                    addingBonus = false;
                }
            }
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer attacker) || !(entity instanceof ServerPlayer target)) {
                return InteractionResult.PASS;
            }
            MinigameSession s = MinigameManager.session(attacker);
            if (s == null || s != MinigameManager.session(target)) {
                return InteractionResult.PASS;
            }
            if (!s.canAttackPlayer(attacker, target)) {
                return InteractionResult.FAIL;
            }
            pvpMelee(attacker, target, s);
            return InteractionResult.SUCCESS;
        });
    }

    /** One OSRS melee attack between players inside a PvP minigame (Castle Wars). */
    static void pvpMelee(ServerPlayer attacker, ServerPlayer target, MinigameSession session) {
        PlayerData data = Players.data(attacker);
        SkillSet skills = data.skills();
        int speed = Equipment.weaponSpeed(attacker, skills);
        // playability: an early click is buffered on the shared attack clock and lands on the exact ready tick (was dropped)
        if (!com.runecraft.combat.AttackTimer.gate(attacker, target, com.runecraft.osrs.AttackClock.Kind.MELEE, (p, t) -> {
            if (t instanceof ServerPlayer victim && MinigameManager.session(p) == session && session.canAttackPlayer(p, victim)) {
                pvpMelee(p, victim, session);
            }
        })) {
            return;
        }
        com.runecraft.combat.AttackTimer.accept(attacker, com.runecraft.osrs.AttackClock.Kind.MELEE, speed * ActionManager.MC_TICKS_PER_GAME_TICK);
        CombatStyle style = data.style().forMelee();
        AttackType type = Equipment.meleeType(attacker, skills);
        // Same calculation as every other player attack (PlayerHitCalc / NpcHitCalc through HitCalcBridge).
        com.runecraft.osrs.PlayerHitCalc.Input in = com.runecraft.combat.HitCalcBridge.input(attacker, target, type, null, 0);
        long attackRoll = com.runecraft.combat.HitCalcBridge.attackRoll(attacker, target, in);
        long defenceRoll = com.runecraft.combat.HitCalcBridge.defenceRoll(target, in);
        int maxHit = com.runecraft.combat.HitCalcBridge.maxHit(attacker, target, in);
        int damage = com.runecraft.combat.HitCalcBridge.roll(new java.util.Random(attacker.getRandom().nextLong()), in, attackRoll, defenceRoll, maxHit);
        PlayerData td = Players.data(target);
        if (td.isActive(Prayer.PROTECT_FROM_MELEE)) {
            damage = (int) Math.floor(damage * 0.6);
        }
        attacker.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        int dealt = CombatHandler.applyDamage(attacker, target, damage);
        CombatHandler.awardXp(attacker, style, dealt, false);
        session.onPlayerHit(attacker, target, dealt);
    }
}
