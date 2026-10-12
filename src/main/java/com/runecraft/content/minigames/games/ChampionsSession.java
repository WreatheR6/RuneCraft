package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.content.minigames.ChampionsChallenge;
import com.runecraft.osrs.content.minigames.ChampionsChallenge.Champion;
import com.runecraft.osrs.content.minigames.ChampionsChallenge.Rule;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.List;

/**
 * Champions' Challenge: Larxus takes a champion scroll and the player fights that champion alone in the
 * Champions' Guild arena under the champion's rule. Breaking the rule forfeits the fight (the scroll is
 * spent, as in OSRS). Beating all ten champions earns Leon d'Cour's scroll; beating Leon, the Champion's cape.
 */
public class ChampionsSession extends MinigameSession {
    private Champion champion;
    private long endAt = -1;

    public ChampionsSession(int id, int slot, ServerWorld world, String variant) {
        super(Minigame.CHAMPIONS_CHALLENGE, id, slot, world, variant);
    }

    // ------------------------------------------------------------------ rules

    /** Why the player can't take on their scroll's champion, or null. */
    public static String requirement(ServerPlayerEntity p) {
        if (Players.data(p).questPoints() < ChampionsChallenge.QUEST_POINTS) {
            return "Larxus: You need " + ChampionsChallenge.QUEST_POINTS + " quest points to take part in the Champions' Challenge.";
        }
        Champion c = ChampionsChallenge.chooseScroll(item -> Inv.has(p, item, 1));
        if (c == null) {
            return "Larxus: Bring me a champion scroll and I'll arrange the fight.";
        }
        String broken = broken(p, c.rule(), true);
        return broken == null ? null : "Larxus: The " + c.name() + "'s rule is: " + c.rule().text() + ". " + broken;
    }

    /** What breaks a rule's equipment, inventory or prayer conditions, or null. */
    static String broken(ServerPlayerEntity p, Rule rule, boolean holdingScroll) {
        PlayerInventory inv = p.getInventory();
        if (rule.noArmour()) {
            for (ItemStack s : inv.armor) {
                if (!s.isEmpty()) {
                    return "Take off your armour.";
                }
            }
            if (!inv.offHand.get(0).isEmpty()) {
                return "Take off your shield.";
            }
            for (ItemStack s : com.runecraft.content.systems.SysState.of(p).worn.values()) {
                if (!s.isEmpty()) {
                    return "Remove your cape, jewellery, gloves and ammunition.";
                }
            }
        }
        if (rule.noWeapon() && isWeapon(p.getMainHandStack())) {
            return "Put away your weapon.";
        }
        if (rule.emptyInventory()) {
            int allowed = holdingScroll ? 1 : 0;
            for (int i = 0; i < inv.main.size(); i++) {
                ItemStack s = inv.main.get(i);
                if (s.isEmpty() || i == inv.selectedSlot && isWeapon(s)) {
                    continue;
                }
                if (allowed > 0 && ChampionsChallenge.byScroll(Inv.idOf(s)) != null && s.getCount() == 1) {
                    allowed--;
                    continue;
                }
                return "Your inventory must be empty" + (rule == Rule.WEAPON_ONLY ? " (only your weapon is allowed)." : ".");
            }
            if (!rule.noArmour() && !inv.offHand.get(0).isEmpty()) {
                return "Your off-hand must be empty.";
            }
        }
        if (rule == Rule.NO_PRAYER && !Players.data(p).activePrayers().isEmpty()) {
            return "Turn off your prayers.";
        }
        return null;
    }

    private static boolean isWeapon(ItemStack s) {
        if (s.isEmpty()) {
            return false;
        }
        ItemDef def = com.runecraft.registry.ModItems.def(s);
        return def != null && def.equipment() != null && def.equipment().slot() == EquipmentDef.Slot.WEAPON
                || s.getItem() instanceof net.minecraft.item.SwordItem || s.getItem() instanceof net.minecraft.item.BowItem;
    }

    /** Attack gate: the champion's rule on attack styles and special attacks (null = allowed). */
    public static String deniedAttack(ServerPlayerEntity p, AttackType type) {
        if (!(MinigameManager.session(p) instanceof ChampionsSession s) || s.champion == null) {
            return null;
        }
        Rule rule = s.champion.rule();
        if (!rule.allows(type)) {
            return "The " + s.champion.name() + "'s rule: " + rule.text() + ".";
        }
        if (rule == Rule.NO_SPECIAL_ATTACKS) {
            com.runecraft.content.systems.SysState.SysPlayer sp = com.runecraft.content.systems.SysState.of(p);
            if (sp.specArmed) {
                sp.specArmed = false;
                return "The " + s.champion.name() + " doesn't allow special attacks.";
            }
        }
        if (rule.noWeapon() && isWeapon(p.getMainHandStack())) {
            return "The " + s.champion.name() + "'s rule: " + rule.text() + ".";
        }
        return null;
    }

    // ------------------------------------------------------------------ session

    @Override
    protected int halfX() {
        return 9;
    }

    @Override
    protected int halfZ() {
        return 9;
    }

    @Override
    protected int height() {
        return 7;
    }

    @Override
    protected void build() {
        room(Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS, Blocks.CHISELED_STONE_BRICKS);
        place(0, 8, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayerEntity player, int index) {
        return rel(0, 1, 5);
    }

    @Override
    protected void start() {
        List<ServerPlayerEntity> online = online();
        if (online.isEmpty()) {
            end(false);
            return;
        }
        ServerPlayerEntity p = online.get(0);
        champion = ChampionsChallenge.chooseScroll(item -> Inv.has(p, item, 1));
        if (champion == null) {
            end(false);
            return;
        }
        Inv.remove(p, champion.scroll(), 1);
        MobEntity mob = spawn(champion.id(), rel(0, 1, -5));
        if (mob == null) {
            end(false);
            return;
        }
        mob.setTarget(p);
        Msg.red(p, "The " + champion.name() + " accepts your challenge! Rule: " + champion.rule().text() + ".");
    }

    @Override
    protected void gameTick() {
        if (endAt >= 0) {
            if (ticks >= endAt) {
                end(true);
            }
            return;
        }
        if (champion == null || ticks % 2 != 0) {
            return;
        }
        for (ServerPlayerEntity p : online()) {
            String broken = broken(p, champion.rule(), false);
            if (broken != null) {
                if (champion.rule() == Rule.NO_PRAYER) {
                    PlayerData d = Players.data(p);
                    d.activePrayers().clear();
                    com.runecraft.network.ServerNetworking.sync(p);
                }
                Msg.red(p, "You broke the " + champion.name() + "'s rule (" + champion.rule().text() + ") and forfeit the fight.");
                end(false);
                return;
            }
        }
    }

    @Override
    protected void onKill(ServerPlayerEntity player, LivingEntity entity, String creatureId) {
        if (champion == null || !creatureId.equals(champion.id()) || endAt >= 0) {
            return;
        }
        MinigameData data = MinigameData.of(player);
        data.setFlag(player.getUuid(), ChampionsChallenge.defeatedFlag(champion.id()));
        data.incrementStat(player.getUuid(), "kc:" + game.id());
        Msg.game(player, "Congratulations! You have defeated the " + champion.name() + ".");
        if (champion.id().equals(ChampionsChallenge.LEON)) {
            // The cape is Leon's drop (picked up for the player when the arena closes).
            Msg.game(player, "Leon d'Cour yields his cape: you are the new Champion of Champions!");
        }
        endAt = ticks + 5;
    }

    @Override
    protected List<String> hud(ServerPlayerEntity player) {
        return champion == null ? List.of(game.displayName())
                : List.of(game.displayName(), champion.name(), "Rule: " + champion.rule().text());
    }

    /** Larxus: explains the challenge, reports progress and hands out Leon's scroll after all ten. */
    public static void larxus(ServerPlayerEntity p) {
        MinigameData data = MinigameData.of(p);
        int beaten = 0;
        for (Champion c : ChampionsChallenge.CHAMPIONS) {
            if (data.flag(p.getUuid(), ChampionsChallenge.defeatedFlag(c.id()))) {
                beaten++;
            }
        }
        boolean allTen = ChampionsChallenge.allTenDefeated(id -> data.flag(p.getUuid(), ChampionsChallenge.defeatedFlag(id)));
        boolean leon = data.flag(p.getUuid(), ChampionsChallenge.defeatedFlag(ChampionsChallenge.LEON));
        if (allTen && !leon && !Inv.has(p, ChampionsChallenge.LEON_SCROLL, 1)) {
            Inv.give(p, ChampionsChallenge.LARXUS_REWARD.item(), 1);
            com.runecraft.collection.CollectionLog.obtain(p, ChampionsChallenge.LARXUS_REWARD.item());
            QuestApi.say(p, "Larxus", "You've beaten every champion! Leon d'Cour, the Champion of Champions, has sent you his own "
                    + "challenge. Use the arena entrance when you're ready.");
            return;
        }
        QuestApi.say(p, "Larxus", "Champions of every race send out scrolls to challenge adventurers. Bring me one and use the "
                + "arena entrance; each champion sets a rule for the fight. You have beaten " + beaten + " of the ten champions"
                + (leon ? ", and Leon d'Cour himself." : "."));
    }
}
