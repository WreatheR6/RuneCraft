package com.runecraft.client;

import com.runecraft.osrs.EquipmentDef;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;

/** OSRS-style item info: requirements, bonuses, healing and value. */
public final class Tooltips {
    private Tooltips() {
    }

    public static void append(ItemStack stack, TooltipContext context, List<Text> lines) {
        if (com.runecraft.bank.Notes.isNote(stack)) {
            ItemDef inner = com.runecraft.osrs.ItemCatalog.get(com.runecraft.bank.Notes.notedId(stack));
            lines.add(Text.literal("Bank note").formatted(Formatting.YELLOW));
            lines.add(Text.literal(com.runecraft.osrs.bank.BankNotes.EXAMINE).formatted(Formatting.GRAY, Formatting.ITALIC));
            if (inner != null) {
                lines.add(Text.literal("Value: " + String.format("%,d", inner.value()) + " gp each  (HA " + String.format("%,d", inner.highAlch()) + ")")
                        .formatted(Formatting.DARK_GRAY));
            }
            return;
        }
        ItemDef def = ModItems.def(stack);
        if (def == null) {
            return;
        }
        if (def.examine() != null) {
            lines.add(Text.literal(def.examine()).formatted(Formatting.GRAY, Formatting.ITALIC));
        }
        if (def.heal() > 0) {
            lines.add(Text.literal("Heals " + def.heal() + " Hitpoints").formatted(Formatting.GREEN));
        }
        if (def.rangedStrength() > 0) {
            lines.add(Text.literal("Ranged strength: +" + def.rangedStrength()).formatted(Formatting.GREEN));
        }
        EquipmentDef eq = def.equipment();
        if (eq != null) {
            for (Map.Entry<Skill, Integer> e : eq.requirements().entrySet()) {
                boolean ok = ClientState.SKILLS.level(e.getKey()) >= e.getValue();
                lines.add(Text.literal("Requires " + e.getValue() + " " + e.getKey().displayName())
                        .formatted(ok ? Formatting.GREEN : Formatting.RED));
            }
            EquipmentStats s = eq.stats();
            if (eq.attackSpeed() > 0) {
                lines.add(Text.literal("Attack speed: " + eq.attackSpeed() + " ticks (" + eq.attackType().name().toLowerCase() + ")")
                        .formatted(Formatting.YELLOW));
            }
            if (com.runecraft.osrs.data.EquipmentBonuses.twoHanded(def.id())) {
                lines.add(Text.literal("Two-handed (no shield bonuses)").formatted(Formatting.YELLOW));
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
                lines.add(Text.literal(other.toString().trim()).formatted(Formatting.GOLD));
            }
        }
        lines.add(Text.literal("Value: " + String.format("%,d", def.value()) + " gp  (HA " + String.format("%,d", def.highAlch()) + ")")
                .formatted(Formatting.DARK_GRAY));
    }

    private static void line(List<Text> lines, String label, int stab, int slash, int crush, int magic, int ranged) {
        if (stab == 0 && slash == 0 && crush == 0 && magic == 0 && ranged == 0) {
            return;
        }
        lines.add(Text.literal(label + ": Stab " + signed(stab) + " Slash " + signed(slash) + " Crush " + signed(crush)
                + " Magic " + signed(magic) + " Range " + signed(ranged)).formatted(Formatting.AQUA));
    }

    private static String signed(int v) {
        return v >= 0 ? "+" + v : String.valueOf(v);
    }
}
