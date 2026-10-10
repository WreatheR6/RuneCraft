package com.runecraft.content.extras;

import com.runecraft.block.ModuleBlock;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.Skill;
import com.runecraft.player.Players;
import com.runecraft.registry.ModBlocks;
import com.runecraft.util.Inv;
import com.runecraft.world.Gielinor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Small helpers shared by the extras module's Minecraft code. */
final class ExtrasUtil {
    private ExtrasUtil() {
    }

    static BlockState block(String id) {
        Block b = ModBlocks.MODULE_BLOCKS.get(id);
        return b == null ? null : b.getDefaultState();
    }

    static String blockId(BlockState state) {
        return state.getBlock() instanceof ModuleBlock mb ? mb.def().id() : null;
    }

    static int level(ServerPlayerEntity player, Skill skill) {
        return Players.data(player).skills().level(skill);
    }

    static String itemName(String id) {
        ItemDef def = ItemCatalog.get(id);
        if (def != null) {
            return def.name();
        }
        String s = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        s = s.replace('_', ' ');
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String lower(String s) {
        return s.toLowerCase(Locale.ROOT);
    }

    static String coins(int n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** "5 x Oak plank, 2 x Bolt of cloth". */
    static String materials(Map<String, Integer> mats) {
        StringBuilder sb = new StringBuilder();
        mats.forEach((item, n) -> {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(item.equals(Inv.COINS) ? coins(n) + " coins" : n + " x " + itemName(item));
        });
        return sb.toString();
    }

    static boolean hasAll(ServerPlayerEntity player, Map<String, Integer> mats) {
        for (Map.Entry<String, Integer> e : mats.entrySet()) {
            if (!Inv.has(player, e.getKey(), e.getValue())) {
                return false;
            }
        }
        return true;
    }

    static void removeAll(ServerPlayerEntity player, Map<String, Integer> mats) {
        mats.forEach((item, n) -> Inv.remove(player, item, n));
    }

    /** A clickable chat option running a command. */
    static MutableText link(String label, String command, String hover, Formatting color) {
        return Text.literal("[" + label + "]").styled(s -> s.withColor(color)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }

    /** Sends a line of clickable options after a grey prompt. */
    static void options(ServerPlayerEntity player, String prompt, List<MutableText> links) {
        MutableText line = Text.literal(prompt + " ").formatted(Formatting.GRAY);
        for (MutableText l : links) {
            line.append(l).append(Text.literal(" "));
        }
        player.sendMessage(line, false);
    }

    /** Teleports a player to OSRS coordinates in Gielinor. */
    static void teleport(ServerPlayerEntity player, int osrsX, int osrsY) {
        ServerWorld world = Gielinor.world(player.getServer());
        if (world == null) {
            return;
        }
        BlockPos pos = Gielinor.safeSpot(world, GielinorMap.toBlockX(osrsX), GielinorMap.toBlockZ(osrsY));
        player.stopRiding();
        player.teleport(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYaw(), player.getPitch());
        world.playSound(null, pos, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.6f, 1.2f);
    }

    /** Ground level (first free block above the surface) at a column. */
    static int surfaceY(ServerWorld world, int x, int z) {
        return Gielinor.safeSpot(world, x, z).getY();
    }
}
