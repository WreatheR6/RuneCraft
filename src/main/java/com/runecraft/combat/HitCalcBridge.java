package com.runecraft.combat;

import com.runecraft.content.slayer.SlayerService;
import com.runecraft.content.systems.SysCombat;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.NpcHitCalc;
import com.runecraft.osrs.PlayerHitCalc;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.SkillSet;
import com.runecraft.osrs.Spell;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Builds a {@link PlayerHitCalc.Input} from a player and their target, so every player attack (melee, ranged, autocast
 * spells, spellbook spells and powered staves) gets its attack roll and max hit from the DPS-calculator-tested pure
 * calculation: levels, prayers, worn items (armour, weapon, shield and the extra jewellery / cape / gloves / ammo slots),
 * gear totals and the target's defence, attributes (undead, demon, dragon, kalphite, ...) and slayer task.
 * Other modules' {@link CombatHooks} modifiers (special attacks, minigame and raid rules, stat drains) still multiply
 * on top of the result.
 */
public final class HitCalcBridge {
    private HitCalcBridge() {
    }

    /** The player's input for an attack of {@code type}; {@code ammoStrength} is the fired arrow's ranged strength. */
    public static PlayerHitCalc.Input input(ServerPlayerEntity p, LivingEntity target, AttackType type, String ammoId, int ammoStrength) {
        PlayerData d = Players.data(p);
        SkillSet s = d.skills();
        PlayerHitCalc.Input in = new PlayerHitCalc.Input();
        in.type = type;
        in.style = d.style();
        in.attack = s.currentLevel(Skill.ATTACK);
        in.strength = s.currentLevel(Skill.STRENGTH);
        in.ranged = s.currentLevel(Skill.RANGED);
        in.magic = s.currentLevel(Skill.MAGIC);
        in.hitpointsLevel = Math.max(1, Math.round(p.getMaxHealth()));
        in.hitpoints = Math.max(0, (int) Math.ceil(p.getHealth()));
        in.prayers.addAll(d.activePrayers());
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            add(in, p.getEquippedStack(slot));
        }
        in.weapon = Inv.idOf(p.getMainHandStack());
        add(in, p.getMainHandStack());
        add(in, p.getOffHandStack());
        for (ItemStack stack : SysState.of(p).worn.values()) {
            add(in, stack);
        }
        in.worn.addAll(extraWornIds.apply(p));
        if (ammoId != null) {
            in.worn.add(ammoId);
        }
        EquipmentStats eq = gearStats(p);
        in.attackBonus = eq.attack(type);
        // A thrown weapon (knife, dart, thrownaxe, Toktz-xil-ul) is fired as ammunition: it is the weapon (its class picks
        // the light ranged defence, also once the last one has left the hand) and adds its own ranged attack bonus.
        if (type == AttackType.RANGED && ammoId != null && com.runecraft.osrs.content.skilling.FletchingData.THROWN.contains(ammoId)) {
            in.weapon = ammoId;
            in.attackBonus += com.runecraft.osrs.data.EquipmentBonuses.thrownRangedAttack(ammoId);
        }
        in.strengthBonus = eq.meleeStrength();
        in.rangedStrength = eq.rangedStrength() + ammoStrength;
        in.magicDamageTenths = eq.magicDamagePercent() * 10;
        target(in, p, target);
        return in;
    }

    /**
     * Stats of items worn in slots another module keeps (the quests module's cape / hands / neck gear such as the imbued
     * god capes and chaos gauntlets), added to the gear totals; set by that module.
     */
    public static java.util.function.Function<ServerPlayerEntity, EquipmentStats> extraWornStats = p -> EquipmentStats.NONE;
    /** Ids of those items (for item effects such as the chaos gauntlets' bolt bonus). */
    public static java.util.function.Function<ServerPlayerEntity, java.util.Collection<String>> extraWornIds = p -> java.util.List.of();

    /**
     * Every bonus the player wears, summed: armour, weapon and shield, the extra jewellery / cape / gloves / ammo slots and
     * other modules' worn slots. The totals PlayerHitCalc and the player's defence roll use; no worn item multiplies on
     * its own.
     */
    public static EquipmentStats gearStats(ServerPlayerEntity p) {
        return Equipment.total(p, Players.data(p).skills()).plus(SysCombat.wornStats(p)).plus(extraWornStats.apply(p));
    }

    private static void add(PlayerHitCalc.Input in, ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            in.worn.add(Inv.idOf(stack));
        }
    }

    /** Fills the target side: defence level and bonus for the attack style, attributes, task, twisted bow magic. */
    static void target(PlayerHitCalc.Input in, ServerPlayerEntity p, LivingEntity target) {
        if (target == null) {
            return;
        }
        in.targetSize = Math.max(1, Math.round(target.getWidth()));
        if (target instanceof ServerPlayerEntity other) {
            in.targetTbowMagic = Players.data(other).skills().currentLevel(Skill.MAGIC);
            return;
        }
        MonsterStats stats = MonsterStats.of(target);
        in.targetDefenceLevel = stats.defence();
        in.targetMagicLevel = stats.magic();
        in.targetDefenceBonus = stats.defenceBonus(in.type);
        in.targetRangedDefence = stats.rangedDefences();
        in.weaknessElement = stats.weakness();
        in.weaknessPercent = stats.weaknessPercent();
        in.targetTbowMagic = Math.max(stats.magic(), stats.attackBonus(AttackType.MAGIC));
        in.attributes.addAll(stats.attributes());
        int tiles = com.runecraft.osrs.data.MonsterTraits.size(stats.traitsKey());
        if (tiles > 0) {
            in.targetSize = tiles; // OSRS size in tiles (colossal blade), not the Minecraft hitbox
        }
        if (SysCombat.isUndead(target)) {
            in.attributes.add("undead");
        }
        if (SysCombat.isDemon(target)) {
            in.attributes.add("demon");
        }
        in.onTask = SlayerService.isOnTask(p, target);
    }

    // ---------------------------------------------------------------- monsters (and players) attacking a player

    /**
     * The defending player's side of an attack of {@code style}: boosted Defence and Magic, hitpoints, prayers, stance,
     * the total defence bonus against the style (every worn slot) and Torag's set.
     */
    public static NpcHitCalc.Input defenderInput(ServerPlayerEntity p, AttackType style) {
        PlayerData d = Players.data(p);
        SkillSet s = d.skills();
        NpcHitCalc.Input in = new NpcHitCalc.Input();
        in.style = style;
        in.defence = s.currentLevel(Skill.DEFENCE);
        in.magic = s.currentLevel(Skill.MAGIC);
        in.hitpointsLevel = Math.max(1, Math.round(p.getMaxHealth()));
        in.hitpoints = Math.max(0, (int) Math.ceil(p.getHealth()));
        in.prayers.addAll(d.activePrayers());
        in.stance = d.style();
        in.defenceBonus = gearStats(p).defence(style);
        in.torags = Inv.idOf(p.getEquippedStack(EquipmentSlot.HEAD)).equals("torags_helm")
                && Inv.idOf(p.getEquippedStack(EquipmentSlot.CHEST)).equals("torags_platebody")
                && Inv.idOf(p.getEquippedStack(EquipmentSlot.LEGS)).equals("torags_platelegs")
                && Inv.idOf(p.getMainHandStack()).equals("torags_hammers")
                && "amulet_of_the_damned".equals(SysState.of(p).wornId(com.runecraft.osrs.content.systems.Jewellery.Slot.NECK));
        return in;
    }

    /** The player's defence roll against an attack style (NpcHitCalc, tested against the DPS calculator). */
    public static long playerDefenceRoll(ServerPlayerEntity p, AttackType style) {
        return NpcHitCalc.playerDefenceRoll(defenderInput(p, style));
    }

    /**
     * A monster's attack roll against a player: {@code (level + 9) * (bonus + 64)} with the monster's (drained) level and
     * attack bonus for the style.
     */
    public static long npcAttackRoll(int level, int attackBonus, AttackType style) {
        NpcHitCalc.Input in = new NpcHitCalc.Input();
        in.style = style;
        in.npcLevel = level;
        in.npcAttackBonus = attackBonus;
        return NpcHitCalc.npcAttackRoll(in);
    }

    /** True when the player's overhead prayer blocks a monster's attack of this style. */
    public static boolean protectedFrom(ServerPlayerEntity p, AttackType style) {
        return NpcHitCalc.protectedFrom(Players.data(p).activePrayers(), style);
    }

    /** Sets a standard-spellbook spell: max hit scaled with Magic level, element and bolt flag. */
    public static void spell(PlayerHitCalc.Input in, Spell spell) {
        in.spellMaxHit = spell.maxHitAt(in.magic);
        in.spellElement = spell.element();
        in.standardSpell = true;
        in.boltSpell = spell.displayName().endsWith("Bolt");
    }

    /** Sets another spell by its base max hit (ancient, Arceuus, god spells, Iban Blast, Magic Dart...). */
    public static void spell(PlayerHitCalc.Input in, int baseMaxHit, boolean standardBook) {
        in.spellMaxHit = Math.max(0, baseMaxHit);
        in.standardSpell = standardBook;
    }

    /**
     * The target's defence roll against this attack: a monster's from {@link PlayerHitCalc#defenceRoll} (so a ranged
     * weapon rolls against the light, standard or heavy ranged defence of its class), a player's from
     * {@link CombatHandler#defenceRoll}.
     */
    public static long defenceRoll(LivingEntity target, PlayerHitCalc.Input in) {
        if (target == null || target instanceof ServerPlayerEntity) {
            return CombatHandler.defenceRoll(target, in.type);
        }
        return PlayerHitCalc.defenceRoll(in);
    }

    /** Attack roll including the remaining {@link CombatHooks} accuracy modifiers. */
    public static long attackRoll(ServerPlayerEntity p, LivingEntity target, PlayerHitCalc.Input in) {
        return (long) (PlayerHitCalc.attackRoll(in) * CombatHooks.accuracy(p, target, in.type));
    }

    /** Max hit including the remaining {@link CombatHooks} max-hit modifiers. */
    public static int maxHit(ServerPlayerEntity p, LivingEntity target, PlayerHitCalc.Input in) {
        return Math.max(0, (int) Math.floor(PlayerHitCalc.maxHit(in) * CombatHooks.maxHit(p, target, in.type) + 1e-9));
    }

    /** Osmumten's fang's minimum hit (0 for everything else). */
    public static int minHit(PlayerHitCalc.Input in) {
        return PlayerHitCalc.minMax(in)[0];
    }

    /** Hit chance of these rolls: Osmumten's fang rolls accuracy twice on stab attacks. */
    public static double hitChance(PlayerHitCalc.Input in, long attackRoll, long defenceRoll) {
        if (in.weapon.startsWith("osmumtens_fang") && in.type == AttackType.STAB) {
            return com.runecraft.osrs.Chance.fangHitChance(attackRoll, defenceRoll);
        }
        return com.runecraft.osrs.Chance.hitChance(attackRoll, defenceRoll);
    }

    /** Rolls one hit: accuracy, then uniform damage between the min hit (fang) and the max hit. */
    public static int roll(java.util.Random random, PlayerHitCalc.Input in, long attackRoll, long defenceRoll, int maxHit) {
        if (random.nextDouble() >= hitChance(in, attackRoll, defenceRoll)) {
            return 0;
        }
        int min = Math.min(minHit(in), Math.max(0, maxHit));
        return min + random.nextInt(Math.max(0, maxHit - min) + 1);
    }
}
