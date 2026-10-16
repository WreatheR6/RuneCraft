package com.runecraft.content.systems;

import com.runecraft.action.Action;
import com.runecraft.action.ActionManager;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.menu.MenuService;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.osrs.content.systems.ItemSources;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Tools;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/**
 * Round 28 item sources (see {@link ItemSources}): Eluned sings crystal teleport seeds into teleport crystals, the
 * Lumbridge Guide swaps the tarnished locket for the amulet of the eye, the Weiss Salt Mine rocks give basalt and salts
 * that make the basalt teleports, and only the Rellekka craftsmen (after The Fremennik Trials) make Fremennik armour.
 */
final class ItemSourcesService {
    /** Diary counter key holding how many crystals Eluned has sung for the player (her price falls with each). */
    private static final String ELUNED_COUNT = "eluned_teleport_crystals";
    private static final double MAKER_REACH_SQ = 8.0 * 8.0;

    /** Where a player last asked a Fremennik craftsman for armour, and which line that craftsman makes. */
    private record Maker(Vec3d pos, String line) {
    }

    private static final Map<UUID, Maker> MAKERS = new HashMap<>();

    private ItemSourcesService() {
    }

    static void register() {
        wrap(ItemSources.ELUNED, ItemSourcesService::eluned);
        wrap(ItemSources.LUMBRIDGE_GUIDE, ItemSourcesService::lumbridgeGuide);
        wrap(ItemSources.SIGLI, ItemSourcesService::fremennikMaker);
        wrap(ItemSources.SKULGRIMEN, ItemSourcesService::fremennikMaker);
        wrap(ItemSources.PEER_THE_SEER, ItemSourcesService::fremennikMaker);
        MenuService.REQUIREMENTS.add(ItemSourcesService::fremennikRecipe);
        for (ItemSources.SaltRock r : ItemSources.SALT_ROCKS) {
            BlockHooks.register(r.block(), (p, pos, state) -> mine(p, pos, r));
            LandmarkBuilder.registerCustom(r.block(), (world, centre) -> DiaryObjectsService.place(world, centre, r.block(), false));
        }
    }

    /** Runs {@code mine} first; if it doesn't handle the talk, the NPC's earlier handler (quest dialogue) runs. */
    private static void wrap(String npc, BiPredicate<ServerPlayerEntity, QuestNpcEntity> mine) {
        BiConsumer<ServerPlayerEntity, QuestNpcEntity> earlier = QuestScripts.handler(npc);
        QuestScripts.onTalk(npc, (p, entity) -> {
            if (!mine.test(p, entity) && earlier != null) {
                earlier.accept(p, entity);
            }
        });
    }

    private static boolean questDone(ServerPlayerEntity p, String questId) {
        QuestInfo q = ContentRegistry.quest(questId);
        return q == null || Players.data(p).questComplete(q);
    }

    // ---------------- Eluned: teleport crystals ----------------

    private static boolean eluned(ServerPlayerEntity p, QuestNpcEntity npc) {
        if (!Inv.has(p, "crystal_teleport_seed", 1)) {
            return false;
        }
        if (!questDone(p, ItemSources.TELEPORT_CRYSTAL_QUEST)) {
            QuestApi.say(p, "Eluned", "I can't sing that seed for an outsider. Help the elves in Mourning's End first.");
            return true;
        }
        Map<String, Integer> counters = SysState.of(p).diaryCounters;
        int earlier = counters.getOrDefault(ELUNED_COUNT, 0);
        int cost = ItemSources.elunedCost(earlier);
        if (!Inv.has(p, Inv.COINS, cost)) {
            QuestApi.say(p, "Eluned", "I can sing that seed into a teleport crystal for " + cost + " coins.");
            return true;
        }
        Inv.remove(p, Inv.COINS, cost);
        Inv.remove(p, "crystal_teleport_seed", 1);
        ItemStack crystal = Inv.stack("teleport_crystal", 1);
        boolean westernHard = Diaries.tierComplete(SysState.of(p).diaryDone, "western", Diaries.Tier.HARD);
        TeleportCharges.set(p, crystal, ItemSources.elunedCharges(westernHard));
        Inv.give(p, crystal);
        counters.put(ELUNED_COUNT, earlier + 1);
        GameEvents.fireObtain(p, "teleport_crystal", 1);
        QuestApi.say(p, "Eluned", "There you are: a teleport crystal with " + ItemSources.elunedCharges(westernHard)
                + " charges. It will take you to Lletya, or Prifddinas once the city is restored.");
        return true;
    }

    // ---------------- Lumbridge Guide: amulet of the eye ----------------

    private static boolean lumbridgeGuide(ServerPlayerEntity p, QuestNpcEntity npc) {
        if (!Inv.has(p, "tarnished_locket", 1)) {
            return false;
        }
        Inv.remove(p, "tarnished_locket", 1);
        Inv.give(p, "amulet_of_the_eye", 1);
        GameEvents.fireObtain(p, "amulet_of_the_eye", 1);
        QuestApi.say(p, "Lumbridge Guide", "This locket... my family's amulet! I was not the first of us in the order of wizards. "
                + "Take it: it will carry you to the Temple of the Eye.");
        return true;
    }

    // ---------------- Fremennik armour ----------------

    private static boolean fremennikMaker(ServerPlayerEntity p, QuestNpcEntity npc) {
        String line = ItemSources.fremennikLine(npc.npcId());
        if (line == null) {
            return false;
        }
        if (!questDone(p, ItemSources.FREMENNIK_QUEST)) {
            if (npc.npcId().equals(ItemSources.SIGLI)) {
                return false; // Sigli's Fremennik Trials dialogue
            }
            QuestApi.say(p, npc.getName().getString(), "I only make armour for Fremennik. Prove yourself in the Fremennik Trials, outerlander.");
            return true;
        }
        QuestApi.say(p, npc.getName().getString(), "Bring me dagannoth hides, the right piece and coins, and I'll make you "
                + line.replace('_', '-') + " armour.");
        MAKERS.put(p.getUuid(), new Maker(npc.getPos(), line));
        MenuService.open(p, MenuType.TANNING, npc.getBlockPos(), false);
        return true;
    }

    /** Fremennik armour recipes (tanning menu) need The Fremennik Trials and the craftsman of that armour line. */
    private static String fremennikRecipe(ServerPlayerEntity p, com.runecraft.osrs.Recipe r) {
        if (r.menu() != MenuType.TANNING || !r.id().startsWith(ItemSources.FREMENNIK_RECIPE_PREFIX)) {
            return null;
        }
        if (!questDone(p, ItemSources.FREMENNIK_QUEST)) {
            return "Only Fremennik may have Fremennik armour made: complete The Fremennik Trials.";
        }
        Maker m = MAKERS.get(p.getUuid());
        String piece = r.id().substring(ItemSources.FREMENNIK_RECIPE_PREFIX.length());
        if (m == null || !piece.startsWith(m.line()) || p.getPos().squaredDistanceTo(m.pos()) > MAKER_REACH_SQ) {
            return "Only the craftsmen of Rellekka make that armour (Sigli: spined, Skulgrimen: rock-shell, Peer the Seer: skeletal).";
        }
        return null;
    }

    // ---------------- Weiss Salt Mine ----------------

    private static void mine(ServerPlayerEntity p, BlockPos pos, ItemSources.SaltRock rock) {
        if (!questDone(p, ItemSources.SALT_MINE_QUEST)) {
            Msg.game(p, "The Snowflake won't let you mine here until you've finished Making Friends with My Arm.");
            return;
        }
        if (Players.data(p).skills().currentLevel(Skill.MINING) < rock.level()) {
            Msg.game(p, "You need a Mining level of " + rock.level() + " to mine here.");
            return;
        }
        if (Tools.bestPickaxe(p, Players.data(p).skills()) == null) {
            Msg.game(p, "You need a pickaxe to mine this rock.");
            return;
        }
        Msg.game(p, "You swing your pickaxe at the rock.");
        ActionManager.start(p, new Action() {
            int counter;

            @Override
            public boolean tick(ServerPlayerEntity player, long gameTick) {
                if (!DiaryObjectsService.isBlock(player.getWorld().getBlockState(pos), rock.block())) {
                    return false;
                }
                if (++counter < 4) { // approx: one item every 4 ticks
                    return true;
                }
                counter = 0;
                if (!Inv.hasRoomFor(player, rock.item())) {
                    Msg.game(player, "Your inventory is too full to hold any more.");
                    return false;
                }
                Inv.give(player, rock.item(), 1);
                XpService.add(player, Skill.MINING, rock.xp());
                GameEvents.fireObtain(player, rock.item(), 1);
                return true;
            }
        });
    }

    /** Using basalt with 3 efh (or urt) salts and a te salt makes an icy (or stony) basalt (Making Friends with My Arm). */
    static boolean combineBasalt(ServerPlayerEntity p) {
        ItemSources.Basalt b = ItemSources.basaltFor(id -> Inv.count(p, id));
        if (b == null) {
            Msg.game(p, "Basalt takes 3 efh salts (icy) or 3 urt salts (stony) and a te salt.");
            return true;
        }
        if (!questDone(p, ItemSources.SALT_MINE_QUEST)) {
            Msg.game(p, "You need to complete Making Friends with My Arm to make that.");
            return true;
        }
        Inv.remove(p, "basalt", 1);
        Inv.remove(p, "te_salt", 1);
        Inv.remove(p, b.salt(), ItemSources.BASALT_SALTS);
        Inv.give(p, b.item(), 1);
        GameEvents.fireObtain(p, b.item(), 1);
        Msg.game(p, "You rub the salts into the basalt.");
        return true;
    }
}
