package com.runecraft.client;

import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.registry.ModItems;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** OSRS-style item info: requirements, bonuses, healing and value. */
public final class Tooltips {
    private Tooltips() {
    }

    public static void append(ItemStack stack, TooltipFlag context, List<Component> lines) {
        if (com.runecraft.bank.Notes.isNote(stack)) {
            ItemDef inner = com.runecraft.osrs.ItemCatalog.get(com.runecraft.bank.Notes.notedId(stack));
            lines.add(Component.literal("Bank note").withStyle(ChatFormatting.YELLOW));
            lines.add(Component.literal(com.runecraft.osrs.bank.BankNotes.EXAMINE).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            if (inner != null) {
                lines.add(Component.literal("Value: " + String.format("%,d", inner.value()) + " gp each  (HA " + String.format("%,d", inner.highAlch()) + ")")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            return;
        }
        ItemDef def = ModItems.def(stack);
        if (def == null) {
            return;
        }
        if (def.examine() != null) {
            lines.add(Component.literal(def.examine()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (def.heal() > 0) {
            lines.add(Component.literal("Heals " + def.heal() + " Hitpoints").withStyle(ChatFormatting.GREEN));
        }
        if (def.rangedStrength() > 0) {
            lines.add(Component.literal("Ranged strength: +" + def.rangedStrength()).withStyle(ChatFormatting.GREEN));
        }
        EquipmentDef eq = def.equipment();
        if (eq != null) {
            for (Map.Entry<Skill, Integer> e : eq.requirements().entrySet()) {
                boolean ok = ClientState.SKILLS.level(e.getKey()) >= e.getValue();
                lines.add(Component.literal("Requires " + e.getValue() + " " + e.getKey().displayName())
                        .withStyle(ok ? ChatFormatting.GREEN : ChatFormatting.RED));
            }
            EquipmentStats s = eq.stats();
            if (eq.attackSpeed() > 0) {
                lines.add(Component.literal("Attack speed: " + eq.attackSpeed() + " ticks (" + eq.attackType().name().toLowerCase() + ")")
                        .withStyle(ChatFormatting.YELLOW));
            }
            if (com.runecraft.osrs.data.EquipmentBonuses.twoHanded(def.id())) {
                lines.add(Component.literal("Two-handed (no shield bonuses)").withStyle(ChatFormatting.YELLOW));
            }
            line(lines, "Attack", s.stabAttack(), s.slashAttack(), s.crushAttack(), s.magicAttack(), s.rangedAttack());
            line(lines, "Defence", s.stabDefence(), s.slashDefence(), s.crushDefence(), s.magicDefence(), s.rangedDefence());
            StringBuilder other = new StringBuilder();
            if (s.meleeStrength() != 0) {
                other.append("Str ").append(signed(s.meleeStrength())).append("  ");
            }
            if (s.rangedStrength() != 0) {
                other.append("Rng str ").append(signed(s.rangedStrength())).append("  ");
            }
            if (s.magicDamagePercent() != 0) {
                other.append("Mag dmg ").append(signed(s.magicDamagePercent())).append("%  ");
            }
            if (s.prayer() != 0) {
                other.append("Prayer ").append(signed(s.prayer()));
            }
            if (other.length() > 0) {
                lines.add(Component.literal(other.toString().trim()).withStyle(ChatFormatting.GOLD));
            }
        }
        lines.add(Component.literal("Value: " + String.format("%,d", def.value()) + " gp  (HA " + String.format("%,d", def.highAlch()) + ")")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static void line(List<Component> lines, String label, int stab, int slash, int crush, int magic, int ranged) {
        if (stab == 0 && slash == 0 && crush == 0 && magic == 0 && ranged == 0) {
            return;
        }
        lines.add(Component.literal(label + ": Stab " + signed(stab) + " Slash " + signed(slash) + " Crush " + signed(crush)
                + " Magic " + signed(magic) + " Range " + signed(ranged)).withStyle(ChatFormatting.AQUA));
    }

    private static String signed(int v) {
        return v >= 0 ? "+" + v : String.valueOf(v);
    }
}
