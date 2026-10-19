package com.runecraft.content.minigames.games;

import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameReward;
import com.runecraft.osrs.content.minigames.ShadeChestRewards;
import com.runecraft.osrs.content.minigames.ShadesOfMortton;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.Shade;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * The Shade Catacombs below Mort'ton: an entrance hall, then five crypts behind key doors - bronze
 * (Phrin shades), steel (Riyl), black (Asyn), silver (Fiyr) and gold (Urium) - each with its metal's
 * chest. A door needs any key of its metal (kept); a chest uses up a key of its metal: the one held,
 * otherwise the lowest colour carried. Shades drop their remains, which are cremated on the funeral
 * pyres in town for keys (see ShadesService).
 */
public class ShadesSession extends MinigameSession {
    /** Crypt width (x) and the first crypt's west wall; the entrance hall is west of it. */
    private static final int CRYPT = 13;
    private static final int FIRST = -26;
    /** The chest blocks, bronze to gold (also placed in the dungeons module's catacombs below Mort'ton). */
    public static final String[] CHESTS = {"shade_chest", "shade_chest_steel", "shade_chest_black", "shade_chest_silver",
            "shade_chest_gold"};

    public ShadesSession(int id, int slot, ServerLevel world, String variant) {
        super(Minigame.SHADES_OF_MORTTON, id, slot, world, variant);
    }

    /** Entry check: the minigame follows the Shades of Mort'ton quest. */
    public static String requirement(ServerPlayer player) {
        if (ContentRegistry.quest(ShadesOfMortton.QUEST) != null && !QuestApi.isComplete(player, ShadesOfMortton.QUEST)) {
            return "You need to complete the Shades of Mort'ton quest to enter the catacombs.";
        }
        return null;
    }

    @Override
    protected int halfX() {
        return 40;
    }

    @Override
    protected int halfZ() {
        return 9;
    }

    @Override
    protected int height() {
        return 6;
    }

    /** West wall x of crypt {@code i} (0 bronze .. 4 gold). */
    private static int wall(int i) {
        return FIRST + i * CRYPT;
    }

    /** The crypt (0..4) a relative x lies in, or -1 for the entrance hall. */
    private static int crypt(int dx) {
        return dx <= FIRST ? -1 : Math.min(4, (dx - FIRST - 1) / CRYPT);
    }

    @Override
    protected void build() {
        room(Blocks.DEEPSLATE_TILES, Blocks.DEEPSLATE_BRICKS, Blocks.SOUL_SOIL);
        for (int i = 0; i < ShadesOfMortton.METALS.length; i++) {
            int x = wall(i);
            for (int dz = -halfZ(); dz <= halfZ(); dz++) {
                for (int y = 1; y <= height(); y++) {
                    set(x, y, dz, Blocks.DEEPSLATE_BRICKS.defaultBlockState());
                }
            }
            place(x, 0, "shade_door_" + ShadesOfMortton.METALS[i]);
            int mid = x + CRYPT / 2 + 1;
            place(mid, -halfZ() + 1, CHESTS[i]);
            // Coffins along the crypt walls, a soul fire and cobwebs.
            for (int cx = x + 2; cx < x + CRYPT - 1; cx += 4) {
                set(cx, 1, halfZ(), Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState());
                set(cx, 1, halfZ() - 1, Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState());
            }
            set(mid, 1, 4, Blocks.SOUL_CAMPFIRE.defaultBlockState());
            set(x + 2, height(), -halfZ() + 1, Blocks.COBWEB.defaultBlockState());
        }
        place(-halfX() + 2, 0, "minigame_exit");
    }

    @Override
    protected BlockPos spawnPoint(ServerPlayer player, int index) {
        return rel(-halfX() + 4, 1, index % 3 - 1);
    }

    @Override
    protected void start() {
        broadcast("You descend into the Shade Catacombs. The doors need a key of their metal; the chests take one.");
    }

    @Override
    protected void gameTick() {
        if (ticks % 10 != 0) {
            return;
        }
        Shade[] shades = Shade.values();
        for (int i = 0; i < ShadesOfMortton.METALS.length; i++) {
            Shade shade = shades[i + 1];
            if (living(shade.creature()).size() < 2 + playerCount()) {
                int x = wall(i) + 2 + random.nextInt(CRYPT - 3);
                int z = random.nextInt(2 * halfZ() - 3) - halfZ() + 2;
                spawn(shade.creature(), rel(x, 1, z));
            }
        }
    }

    @Override
    protected boolean onBlock(ServerPlayer player, String blockId, BlockPos pos) {
        for (int i = 0; i < ShadesOfMortton.METALS.length; i++) {
            String metal = ShadesOfMortton.METALS[i];
            if (blockId.equals("shade_door_" + metal)) {
                door(player, i);
                return true;
            }
            if (blockId.equals(CHESTS[i])) {
                chest(player, metal);
                return true;
            }
        }
        return false;
    }

    private static boolean hasKey(ServerPlayer player, String metal) {
        for (String c : ShadesOfMortton.COLOURS) {
            if (Inv.has(player, ShadesOfMortton.key(metal, c), 1)) {
                return true;
            }
        }
        return false;
    }

    private void door(ServerPlayer player, int i) {
        String metal = ShadesOfMortton.METALS[i];
        int x = wall(i);
        boolean west = relPos(player).getX() < x;
        if (west && !hasKey(player, metal)) {
            Msg.game(player, "The doors are locked. You need " + Msg.article(metal) + " key to open them.");
            return;
        }
        BlockPos to = rel(west ? x + 2 : x - 2, 1, 0);
        player.teleportTo(world, to.getX() + 0.5, to.getY(), to.getZ() + 0.5, player.getYRot(), player.getXRot());
        if (west) {
            Msg.game(player, "You unlock the doors with your " + metal + " key.");
        }
    }

    private void chest(ServerPlayer player, String metal) {
        List<String> uniques = openChest(player, metal, random);
        if (uniques != null) {
            for (String u : uniques) {
                broadcast(player.getName().getString() + " finds " + ItemCatalog.get(u).name() + " in a " + metal + " chest!");
            }
        }
    }

    /**
     * Opens a Shade Catacombs chest of {@code metal} with a key of that metal (the one held, else the first colour carried),
     * which dissolves in the lock, and gives the chest's rolls. Used by this instance and by the chests standing in the
     * dungeons module's catacombs below Mort'ton. Returns the uniques found, or null when the player has no key.
     */
    public static List<String> openChest(ServerPlayer player, String metal, java.util.Random random) {
        String key = null;
        ItemStack held = player.getMainHandItem();
        String[] heldParts = held.isEmpty() ? null : ShadesOfMortton.keyParts(Inv.idOf(held));
        if (heldParts != null && heldParts[0].equals(metal)) {
            key = Inv.idOf(held);
        } else {
            for (String c : ShadesOfMortton.COLOURS) {
                if (Inv.has(player, ShadesOfMortton.key(metal, c), 1)) {
                    key = ShadesOfMortton.key(metal, c);
                    break;
                }
            }
        }
        if (key == null) {
            Msg.game(player, "The " + metal + " chest is locked. You need " + Msg.article(metal) + " key; hold the colour you want to use.");
            return null;
        }
        String colour = ShadesOfMortton.keyParts(key)[1];
        Inv.remove(player, key, 1);
        Set<String> owned = new HashSet<>(Players.data(player).collectionLog());
        for (String piece : List.of("flamtaer_bag", "zealots_boots", "zealots_helm", "zealots_robe_bottom", "zealots_robe_top")) {
            if (Inv.has(player, piece, 1)) {
                owned.add(piece);
            }
        }
        ShadesOfMortton.Opening o = ShadesOfMortton.open(ShadesOfMortton.chest(metal, colour), owned,
                id -> ItemCatalog.exists(id) ? ItemCatalog.get(id).value() : 0, random::nextDouble);
        List<String> uniques = new java.util.ArrayList<>();
        for (MinigameReward r : o.items()) {
            reward(player, r.item(), r.amount());
            if (ShadeChestRewards.UNIQUES.contains(r.item())) {
                uniques.add(r.item());
            }
        }
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.SOUL, player.getX(), player.getY() + 1, player.getZ(), 8,
                0.4, 0.4, 0.4, 0.02);
        Msg.game(player, "The " + colour + " " + metal + " key dissolves in the lock as the chest opens.");
        return uniques;
    }

    @Override
    protected List<String> hud(ServerPlayer player) {
        int c = crypt(relPos(player).getX());
        String where = c < 0 ? "Entrance hall" : Character.toUpperCase(ShadesOfMortton.METALS[c].charAt(0))
                + ShadesOfMortton.METALS[c].substring(1) + " crypt (" + Shade.values()[c + 1].displayName() + " shades)";
        int remains = 0;
        for (Shade s : Shade.values()) {
            remains += Inv.count(player, s.remains());
        }
        return List.of("Shade Catacombs", where, "Remains carried: " + remains);
    }
}
