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
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;

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
            Skill.CONSTRUCTION, new String[]{"carpenters_helmet", "carpenters_shirt", "carpenters_trousers", "carpenters_boots"});
    /** OSRS outfit bonus per piece (hat, top, legs, boots); the full set gives 2.5%. */
    private static final double[] PIECE_BONUS = {0.004, 0.008, 0.006, 0.002};
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static boolean addingBonus;

    private MinigameCombat() {
    }

    static String worn(ServerPlayerEntity p, EquipmentSlot slot) {
        return Inv.idOf(p.getEquippedStack(slot));
    }

    /** Void style worn: 0 melee, 1 ranged, 2 magic, or -1 without a full set. */
    static int voidStyle(ServerPlayerEntity p) {
        String top = worn(p, EquipmentSlot.CHEST);
        String legs = worn(p, EquipmentSlot.LEGS);
        if (!(top.equals("void_knight_top") || top.equals("elite_void_top"))
                || !(legs.equals("void_knight_robe") || legs.equals("elite_void_robe"))
                || !Inv.has(p, "void_knight_gloves", 1)) {
            return -1;
        }
        return switch (worn(p, EquipmentSlot.HEAD)) {
            case "void_melee_helm" -> 0;
            case "void_ranger_helm" -> 1;
            case "void_mage_helm" -> 2;
            default -> -1;
        };
    }

    static boolean eliteVoid(ServerPlayerEntity p) {
        return worn(p, EquipmentSlot.CHEST).equals("elite_void_top") && worn(p, EquipmentSlot.LEGS).equals("elite_void_robe");
    }

    private static int styleIndex(AttackType type) {
        return type == AttackType.RANGED ? 1 : type == AttackType.MAGIC ? 2 : 0;
    }

    private static boolean obsidianEffect(ServerPlayerEntity p, AttackType type) {
        return type.isMelee() && worn(p, EquipmentSlot.HEAD).equals("obsidian_helmet")
                && worn(p, EquipmentSlot.CHEST).equals("obsidian_platebody")
                && worn(p, EquipmentSlot.LEGS).equals("obsidian_platelegs")
                && TZHAAR_MELEE.contains(Inv.idOf(p.getMainHandStack()));
    }

    static void register() {
        CombatHooks.addModifier(new CombatHooks.PlayerAttackModifier() {
            @Override
            public double accuracy(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                double m = 1.0;
                int style = voidStyle(player);
                if (style >= 0 && style == styleIndex(type)) {
                    m *= MinigameRules.PestControl.voidAccuracy(style);
                }
                if (obsidianEffect(player, type)) {
                    m *= 1.10;
                }
                return m;
            }

            @Override
            public double maxHit(ServerPlayerEntity player, LivingEntity target, AttackType type) {
                double m = 1.0;
                int style = voidStyle(player);
                if (style >= 0 && style == styleIndex(type)) {
                    m *= MinigameRules.PestControl.voidDamage(style, eliteVoid(player));
                }
                if (obsidianEffect(player, type)) {
                    m *= 1.10;
                }
                String off = Inv.idOf(player.getOffHandStack());
                if (type == AttackType.MAGIC && (off.equals("tome_of_fire") || off.equals("tome_of_water"))) {
                    m *= 1.10;
                }
                MinigameSession s = MinigameManager.session(player);
                if (s != null) {
                    m *= s.damageMultiplier(player, target, type);
                }
                return m;
            }

            @Override
            public int afterRoll(ServerPlayerEntity player, LivingEntity target, AttackType type, int damage) {
                if (target.getCommandTags().contains(INVULNERABLE)) {
                    Msg.action(player, "It's protected by a shield.");
                    return 0;
                }
                return damage;
            }

            @Override
            public void afterHit(ServerPlayerEntity player, LivingEntity target, AttackType type, int dealt) {
                MinigameSession s = MinigameManager.owner(target);
                if (s != null && !s.isEnded()) {
                    s.onPlayerHit(player, target, dealt);
                }
            }
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
            if (world.isClient() || !(player instanceof ServerPlayerEntity attacker) || !(entity instanceof ServerPlayerEntity target)) {
                return ActionResult.PASS;
            }
            MinigameSession s = MinigameManager.session(attacker);
            if (s == null || s != MinigameManager.session(target)) {
                return ActionResult.PASS;
            }
            if (!s.canAttackPlayer(attacker, target)) {
                return ActionResult.FAIL;
            }
            pvpMelee(attacker, target, s);
            return ActionResult.SUCCESS;
        });
    }

    /** One OSRS melee attack between players inside a PvP minigame (Castle Wars). */
    static void pvpMelee(ServerPlayerEntity attacker, ServerPlayerEntity target, MinigameSession session) {
        PlayerData data = Players.data(attacker);
        SkillSet skills = data.skills();
        int speed = Equipment.weaponSpeed(attacker, skills);
        long now = ActionManager.mcTick();
        if (now - data.lastAttackTick < (long) speed * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        data.lastAttackTick = now;
        CombatStyle style = data.style().forMelee();
        EquipmentStats eq = Equipment.total(attacker, skills);
        AttackType type = Equipment.meleeType(attacker, skills);
        int effAtt = CombatFormulas.effectiveAttack(skills.currentLevel(Skill.ATTACK), PrayerService.attack(data), style);
        int effStr = CombatFormulas.effectiveStrength(skills.currentLevel(Skill.STRENGTH), PrayerService.strength(data), style);
        PlayerData td = Players.data(target);
        SkillSet ts = td.skills();
        EquipmentStats teq = Equipment.total(target, ts);
        int effDef = CombatFormulas.effectiveDefence(ts.currentLevel(Skill.DEFENCE), PrayerService.defence(td), td.style());
        long attackRoll = (long) (CombatFormulas.attackRoll(effAtt, eq.attack(type)) * CombatHandler.hookAccuracy(attacker, target, type));
        long defenceRoll = CombatFormulas.attackRoll(effDef, teq.defence(type));
        int maxHit = (int) Math.floor(CombatFormulas.meleeMaxHit(effStr, eq.meleeStrength()) * CombatHandler.hookMaxHit(attacker, target, type));
        int damage = CombatFormulas.roll(new java.util.Random(attacker.getRandom().nextLong()), attackRoll, defenceRoll, maxHit);
        if (td.isActive(Prayer.PROTECT_FROM_MELEE)) {
            damage = (int) Math.floor(damage * 0.6);
        }
        attacker.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        int dealt = CombatHandler.applyDamage(attacker, target, damage);
        CombatHandler.awardXp(attacker, style, dealt, false);
        session.onPlayerHit(attacker, target, dealt);
    }
}
