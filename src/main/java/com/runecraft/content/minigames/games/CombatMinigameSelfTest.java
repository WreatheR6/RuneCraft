package com.runecraft.content.minigames.games;

import com.runecraft.combat.CombatHandler;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.content.minigames.RewardShopService;
import com.runecraft.content.systems.SysState;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.MageTrainingArena;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.MinigameCurrencies;
import com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone;
import com.runecraft.osrs.content.minigames.MinigameRules.WarriorsGuild;
import com.runecraft.osrs.content.minigames.RewardShop;
import com.runecraft.osrs.content.minigames.RewardShops;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.selftest.ChunkSettle;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Real-player walks of the combat minigames (round 40; the "minigame:" checks of {@code /rc selftest real}): a Nightmare Zone
 * rumble played to a reward, the Warriors' Guild from animated armour to a dragon defender, and one task in each of the Mage
 * Training Arena's four rooms followed by a purchase from the Rewards Guardian. Minigame sessions only see players in the
 * server's player list, so the fake player can't play them. Every broken step throws an {@link AssertionError}.
 */
public final class CombatMinigameSelfTest {
    private final ServerPlayer player;
    private final UUID uuid;
    private final List<String> log = new ArrayList<>();

    private CombatMinigameSelfTest(ServerPlayer player) {
        this.player = player;
        this.uuid = player.getUUID();
    }

    private static void expect(boolean ok, String what) {
        if (!ok) {
            throw new AssertionError(what);
        }
    }

    private void heal() {
        if (player.getHealth() < player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private void tick(MinigameSession s, int n) {
        for (int i = 0; i < n && !s.isEnded(); i++) {
            MinigameManager.selfTestTick(s);
            heal();
        }
    }

    private void level(Skill skill, int level) {
        Players.data(player).skills().setXpTenths(skill, Experience.xpForLevel(level) * 10);
        XpService.updateMaxHealth(player);
    }

    private int points(String currency) {
        return MinigameData.of(player).points(uuid, MinigameCurrencies.key(currency));
    }

    private void setPoints(String currency, int amount) {
        MinigameData.of(player).addPoints(uuid, MinigameCurrencies.key(currency), amount - points(currency));
    }

    private static int entry(String shop, String item) {
        List<RewardShop.Entry> entries = RewardShops.get(shop).entries();
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).item().equals(item)) {
                return i;
            }
        }
        throw new AssertionError(shop + " sells no " + item);
    }

    /** Kills a creature through the player's real damage path (kill events, drops, minigame kill hooks). */
    private void kill(LivingEntity mob) {
        CombatHandler.applyDamage(player, mob, (int) Math.ceil(mob.getHealth()) + 5);
    }

    private <T extends MinigameSession> T enter(Minigame game, String variant, Class<T> type) {
        MinigameManager.enter(player, game, variant);
        MinigameSession s = MinigameManager.session(player);
        expect(type.isInstance(s), "entering " + game.displayName() + " (" + variant + ") started " + (s == null ? "nothing" : s.game.id()));
        if (!ChunkSettle.settle(s.world, s.box())) {
            log.add("(" + game.id() + " arena chunks still loading after " + ChunkSettle.MAX_NANOS / 1_000_000_000L + " s)");
        }
        return type.cast(s);
    }

    private void leave(MinigameSession s) {
        if (!s.isEnded()) {
            MinigameManager.leave(player);
        }
        expect(MinigameManager.session(player) == null, "the player is still in " + s.game.displayName() + " after leaving");
    }

    // ================================================================== Nightmare Zone

    /** A hard rumble played to a reward: entry fee, absorption from the barrel, boss kills for points, power-ups, a herb box. */
    public static String nightmareZone(ServerPlayer player) {
        return new CombatMinigameSelfTest(player).nmz();
    }

    private String nmz() {
        PlayerData data = Players.data(player);
        for (QuestInfo q : ContentRegistry.quests()) {
            data.setQuestStage(q, q.finalStage());
        }
        for (Skill s : new Skill[]{Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS, Skill.RANGED, Skill.MAGIC}) {
            level(s, 99);
        }
        player.getInventory().clearContent();
        setPoints(MinigameCurrencies.NIGHTMARE_ZONE, 4000);
        // Dom Onion's benefits go into the barrel by the dose: 4 doses of absorption, withdrawn as one potion.
        MinigameData.of(player).setStat(uuid, "nmz_barrel:" + NightmareZone.ABSORPTION, 0);
        RewardShopService.buy(player, "nightmare_zone", entry("nightmare_zone", NightmareZone.ABSORPTION), 4);
        expect(NightmareZoneSession.barrel(player, NightmareZone.ABSORPTION) == 4 && points(MinigameCurrencies.NIGHTMARE_ZONE) == 0,
                "buying 4 absorption doses left " + NightmareZoneSession.barrel(player, NightmareZone.ABSORPTION) + " in the barrel");
        expect(NightmareZoneSession.withdraw(player, NightmareZone.ABSORPTION, 4) == 4 && Inv.has(player, NightmareZone.ABSORPTION, 1),
                "withdrawing the doses gave no potion");
        // A hard rumble costs 6,000 coins; every boss of the completed quests is in it.
        Inv.give(player, "coins", 6000);
        NightmareZone.Dream dream = new NightmareZone.Dream(NightmareZone.Mode.RUMBLE, true, null);
        NightmareZoneSession nmz = enter(Minigame.NIGHTMARE_ZONE, dream.variant(), NightmareZoneSession.class);
        try {
            expect(Inv.count(player, "coins") == 0, "the hard rumble didn't take its 6,000 coins (" + Inv.count(player, "coins") + " left)");
            expect(nmz.pool().size() == NightmareZone.BOSSES.length, "the dream holds " + nmz.pool().size() + " bosses, not all fifty");
            expect(nmz.drink(player, NightmareZone.ABSORPTION) && nmz.absorption(player) == NightmareZone.ABSORPTION_PER_DOSE,
                    "an absorption dose gave " + nmz.absorption(player) + " points");
            tick(nmz, 22);
            List<LivingEntity> bosses = nmz.bosses();
            expect(!bosses.isEmpty() && bosses.size() <= NightmareZone.MAX_BOSSES, bosses.size() + " bosses in a solo rumble");
            for (LivingEntity b : bosses) {
                String id = MinigameManager.creatureId(b);
                expect(b.getTags().contains(NightmareZoneSession.HARD_TAG)
                        && Math.round(b.getMaxHealth()) == NightmareZone.hardHitpoints(id), id + " isn't a hard boss");
            }
            // Kill bosses (later forms included) until the dream has paid enough for a herb box.
            int earned = 0;
            int kills = 0;
            for (int round = 0; round < 40 && earned < 9500; round++) {
                for (LivingEntity b : nmz.bosses()) {
                    String id = MinigameManager.creatureId(b);
                    int expectPts = NightmareZone.finalForm(id) ? nmz.pointsFor(id) : 0;
                    int before = points(MinigameCurrencies.NIGHTMARE_ZONE);
                    kill(b);
                    int got = points(MinigameCurrencies.NIGHTMARE_ZONE) - before;
                    expect(got == expectPts, id + " paid " + got + " points, not " + expectPts);
                    earned += got;
                    kills++;
                }
                tick(nmz, 5);
            }
            expect(earned >= 9500, "forty rounds of kills paid only " + earned + " points");
            log.add(kills + " hard boss kills for " + earned + " points");
            // Power-ups: the zapper stands where it was picked up; ultimate force clears the floor for no points.
            nmz.activate(player, NightmareZone.PowerUp.ZAPPER);
            expect(nmz.activePowerUp(player) == NightmareZone.PowerUp.ZAPPER && nmz.zapperAt() != null, "the zapper didn't start");
            tick(nmz, 6);
            int before = points(MinigameCurrencies.NIGHTMARE_ZONE);
            for (LivingEntity b : nmz.bosses()) {
                nmz.noteDamage(b);
            }
            nmz.activate(player, NightmareZone.PowerUp.ULTIMATE_FORCE);
            expect(nmz.bosses().isEmpty(), nmz.bosses().size() + " bosses survived ultimate force");
            expect(points(MinigameCurrencies.NIGHTMARE_ZONE) == before, "ultimate force kills paid points");
            log.add("zapper and ultimate force (no points)");
        } finally {
            leave(nmz);
        }
        expect(!Inv.has(player, NightmareZone.ABSORPTION, 1), "the dream potions weren't taken away on waking");
        // The reward: a herb box from Dom Onion's Reward Shop, then an imbue.
        int pts = points(MinigameCurrencies.NIGHTMARE_ZONE);
        RewardShopService.buy(player, "nightmare_zone", entry("nightmare_zone", "herb_box"), 1);
        expect(Inv.has(player, "herb_box", 1) && points(MinigameCurrencies.NIGHTMARE_ZONE) == pts - 9500,
                "the herb box wasn't bought with the dream's points");
        setPoints(MinigameCurrencies.NIGHTMARE_ZONE, 1_250_000);
        Inv.give(player, "black_mask", 1);
        RewardShopService.buy(player, "nightmare_zone", entry("nightmare_zone", "black_mask_i"), 1);
        expect(Inv.has(player, "black_mask_i", 1) && !Inv.has(player, "black_mask", 1), "the black mask wasn't imbued");
        log.add("herb box bought; black mask imbued for " + (1_250_000 - points(MinigameCurrencies.NIGHTMARE_ZONE)) + " points");
        player.getInventory().clearContent();
        return String.join("; ", log);
    }

    // ================================================================== Warriors' Guild

    /** From animated armour to a dragon defender: tokens, the dummy room, the catapult, the cyclopes and the basement. */
    public static String warriorsGuild(ServerPlayer player) {
        return new CombatMinigameSelfTest(player).wg();
    }

    private WarriorsGuildSession wgRoom(String variant) {
        return enter(Minigame.WARRIORS_GUILD, variant, WarriorsGuildSession.class);
    }

    private String wg() {
        level(Skill.ATTACK, 99);
        level(Skill.STRENGTH, 99);
        level(Skill.DEFENCE, 99);
        level(Skill.HITPOINTS, 99);
        player.getInventory().clearContent();
        // ---- Animation room: rune armour comes to life and pays 40 tokens (times the Combat Achievement multiplier).
        WarriorsGuildSession anim = wgRoom(Minigame.WARRIORS_GUILD.entranceBlock());
        int mult = Math.max(1, WarriorsGuildSession.tokenMultiplier.applyAsInt(player));
        try {
            for (String piece : new String[]{"rune_full_helm", "rune_platebody", "rune_platelegs"}) {
                Inv.give(player, piece, 1);
            }
            MinigameManager.onBlock(player, "wg_armour_animator", anim.animatorAt());
            expect(!Inv.has(player, "rune_platebody", 1), "the animator didn't take the rune armour");
            List<LivingEntity> armour = new ArrayList<>();
            for (LivingEntity e : anim.world.getEntitiesOfClass(LivingEntity.class, anim.box(),
                    e -> e.isAlive() && MinigameManager.creatureId(e).equals("animated_rune_armour"))) {
                armour.add(e);
            }
            expect(armour.size() == 1, armour.size() + " animated rune armours");
            kill(armour.get(0));
            expect(Inv.count(player, "warrior_guild_token") == 40 * mult, "rune armour paid " + Inv.count(player, "warrior_guild_token")
                    + " tokens, not " + 40 * mult);
            expect(Inv.has(player, "rune_platebody", 1) && Inv.has(player, "rune_full_helm", 1), "the rune armour wasn't given back");
        } finally {
            leave(anim);
        }
        log.add("animated rune armour: " + 40 * mult + " tokens and the armour back");
        // ---- Dummy room: the right style pays 2 tokens and 15 Attack XP.
        WarriorsGuildSession dummy = wgRoom(WarriorsGuild.DUMMY_ENTRANCE);
        try {
            Players.data(player).setStyle(com.runecraft.osrs.CombatStyle.CONTROLLED);
            int tokens = Inv.count(player, "warrior_guild_token");
            dummy.popDummy(2, "controlled");
            MinigameManager.onBlock(player, "wg_dummy", dummy.dummyUp());
            expect(Inv.count(player, "warrior_guild_token") == tokens + 2 * mult, "a controlled hit on the controlled dummy paid nothing");
            dummy.popDummy(4, "defensive");
            MinigameManager.onBlock(player, "wg_dummy", dummy.dummyUp());
            expect(Inv.count(player, "warrior_guild_token") == tokens + 2 * mult, "the wrong style paid tokens");
            Players.data(player).setStyle(com.runecraft.osrs.CombatStyle.ACCURATE);
        } finally {
            leave(dummy);
        }
        // ---- Catapult: blocking the right ammunition pays 1 token.
        WarriorsGuildSession cat = wgRoom(WarriorsGuild.CATAPULT_ENTRANCE);
        try {
            int tokens = Inv.count(player, "warrior_guild_token");
            cat.fire("slash");
            expect(MinigameManager.action(player, "block", "slash"), "the block option wasn't taken");
            tick(cat, WarriorsGuild.CATAPULT_INTERVAL);
            expect(Inv.count(player, "warrior_guild_token") >= tokens + mult, "blocking the slash shot paid nothing");
        } finally {
            leave(cat);
        }
        log.add("dummy and catapult pay");
        // ---- Top floor: 10 tokens on entry; a bronze defender drops first (2/100 per cyclops).
        Inv.give(player, "warrior_guild_token", 600);
        int before = Inv.count(player, "warrior_guild_token");
        WarriorsGuildSession top = wgRoom(WarriorsGuild.CYCLOPS_ENTRANCE);
        int cyclopes = 0;
        try {
            expect(Inv.count(player, "warrior_guild_token") == before - WarriorsGuild.CYCLOPS_TOKENS, "entering didn't take 10 tokens");
            expect(top.target(player) == 0, "with no defender the cyclopes drop tier " + top.target(player));
            while (!Inv.has(player, "bronze_defender", 1) && cyclopes < 800) {
                Mob c = top.selfTestSpawnCyclops();
                expect(c != null, "no cyclops spawned");
                kill(c);
                cyclopes++;
            }
            expect(Inv.has(player, "bronze_defender", 1), cyclopes + " cyclopes dropped no bronze defender");
            expect(top.target(player) == -1, "a second defender can drop without re-entering");
        } finally {
            leave(top);
        }
        log.add("bronze defender after " + cyclopes + " cyclopes");
        // Showing a better defender moves the drop up; with a rune defender shown the basement opens.
        Inv.give(player, "rune_defender", 1);
        WarriorsGuildSession again = wgRoom(WarriorsGuild.CYCLOPS_ENTRANCE);
        try {
            expect(again.target(player) == WarriorsGuild.RUNE, "showing a rune defender upstairs targets tier " + again.target(player));
        } finally {
            leave(again);
        }
        // ---- Basement: level 106 cyclopes, the dragon defender at 1/100.
        WarriorsGuildSession base = wgRoom(WarriorsGuild.BASEMENT_ENTRANCE);
        cyclopes = 0;
        try {
            expect(MinigameData.of(player).flag(uuid, WarriorsGuild.BASEMENT_FLAG), "Lorelai didn't remember the rune defender");
            expect(base.target(player) == WarriorsGuild.DRAGON, "the basement targets tier " + base.target(player));
            while (!Inv.has(player, "dragon_defender", 1) && cyclopes < 2000) {
                Mob c = base.selfTestSpawnCyclops();
                expect(c != null && MinigameManager.creatureId(c).equals("cyclops_elite"), "the basement spawned no level 106 cyclops");
                kill(c);
                cyclopes++;
            }
            expect(Inv.has(player, "dragon_defender", 1), cyclopes + " basement cyclopes dropped no dragon defender");
        } finally {
            leave(base);
        }
        log.add("dragon defender after " + cyclopes + " basement cyclopes");
        player.getInventory().clearContent();
        return String.join("; ", log);
    }

    // ================================================================== Mage Training Arena

    /** One task in each of the four rooms (a maze, alchemy, enchantment, bones to fruit), then a Rewards Guardian purchase. */
    public static String mageTrainingArena(ServerPlayer player) {
        return new CombatMinigameSelfTest(player).mta();
    }

    private MageArenaSession mtaRoom(String room) {
        return enter(Minigame.MAGE_TRAINING_ARENA, "mta_" + room + "_entrance", MageArenaSession.class);
    }

    private String mta() {
        level(Skill.MAGIC, 99);
        player.getInventory().clearContent();
        String[] currencies = {MinigameCurrencies.MTA_TELEKINETIC, MinigameCurrencies.MTA_ALCHEMIST, MinigameCurrencies.MTA_ENCHANTMENT,
                MinigameCurrencies.MTA_GRAVEYARD};
        for (String c : currencies) {
            setPoints(c, 0);
        }
        // ---- Telekinetic Theatre: pull the statue along the maze's solution from the side of each pull.
        Inv.give(player, "law_rune", 40);
        Inv.give(player, "air_rune", 40);
        MinigameData.of(player).setStat(uuid, "mta_tele_streak", 4);
        MageArenaSession tele = mtaRoom("telekinetic");
        try {
            List<MageTrainingArena.Dir> path = tele.maze().solve();
            expect(path != null && path.size() >= MageTrainingArena.MIN_MOVES && path.size() <= MageTrainingArena.MAX_MOVES,
                    "the maze's solution takes " + (path == null ? "no" : path.size()) + " pulls");
            int laws = Inv.count(player, "law_rune");
            for (MageTrainingArena.Dir d : path) {
                BlockPos stand = tele.standFor(d);
                player.teleportTo(tele.world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(), player.getXRot());
                int[] statue = tele.statue();
                MinigameManager.onBlock(player, "mta_statue", tele.origin.offset(-5 + statue[0], 1, -5 + statue[1]));
            }
            expect(tele.mazes() == 1, "the statue didn't reach the exit (" + tele.mazes() + " mazes solved)");
            // The fifth maze of the streak: 2 + 8 points and 10 law runes back.
            expect(points(MinigameCurrencies.MTA_TELEKINETIC) == 10, "the fifth maze paid " + points(MinigameCurrencies.MTA_TELEKINETIC)
                    + " Telekinetic points, not 10");
            expect(Inv.count(player, "law_rune") == laws - path.size() + 10, "the streak bonus didn't give 10 law runes");
        } finally {
            leave(tele);
        }
        log.add("maze solved: " + points(MinigameCurrencies.MTA_TELEKINETIC) + " Telekinetic points");
        // ---- Alchemists' Playground: take the most valuable item, alch it, deposit the coins.
        Inv.give(player, "nature_rune", 20);
        Inv.give(player, "fire_rune", 100);
        MageArenaSession alch = mtaRoom("alchemist");
        try {
            String best = null;
            for (String item : MageTrainingArena.ALCHEMY_ITEMS) {
                if (best == null || alch.cupboards().value(item) > alch.cupboards().value(best)) {
                    best = item;
                }
            }
            expect(alch.cupboards().value(best) == 30, "the best item is worth " + alch.cupboards().value(best));
            BlockPos cupboard = alch.cupboardAt(alch.cupboards().cupboardOf(best));
            for (int i = 0; i < 4; i++) {
                MinigameManager.onBlock(player, "mta_cupboard", cupboard);
            }
            expect(Inv.count(player, best) == 4, "the cupboard gave " + Inv.count(player, best) + " items");
            for (int i = 0; i < 4; i++) {
                expect(MageArenaSession.useItem(player, best), "using the item didn't cast alchemy");
            }
            expect(alch.coinsHeld(player) == 120 && !Inv.has(player, best, 1), "alchemy made " + alch.coinsHeld(player) + " coins");
            int coins = Inv.count(player, "coins");
            MinigameManager.onBlock(player, "mta_coin_collector", alch.origin.offset(0, 1, -2));
            int pts = points(MinigameCurrencies.MTA_ALCHEMIST);
            expect(pts >= 1 && pts <= 2 && Inv.count(player, "coins") == coins + pts * 10,
                    "120 coins deposited gave " + pts + " points and " + (Inv.count(player, "coins") - coins) + " coins");
        } finally {
            leave(alch);
        }
        log.add("alchemy: " + points(MinigameCurrencies.MTA_ALCHEMIST) + " Alchemist points");
        // ---- Enchanting Chamber: shapes from a pile, Lvl-7 Enchant, orbs down the hole.
        Inv.give(player, "cosmic_rune", 10);
        Inv.give(player, "blood_rune", 200);
        Inv.give(player, "soul_rune", 200);
        MageArenaSession ench = mtaRoom("enchantment");
        try {
            for (int i = 0; i < 3; i++) {
                MinigameManager.onBlock(player, "mta_shape_pile", ench.pileAt(0));
            }
            int shapes = Inv.count(player, "mta_cube") + Inv.count(player, MageTrainingArena.DRAGONSTONE);
            expect(shapes == 3, "the pile gave " + shapes + " shapes");
            // Each use enchants one shape (bounded: a failed cast leaves the shape, and the use is still the arena's).
            for (int i = 0; i < 3; i++) {
                String shape = Inv.has(player, "mta_cube", 1) ? "mta_cube" : MageTrainingArena.DRAGONSTONE;
                expect(MageArenaSession.useItem(player, shape), "using a " + shape + " wasn't the arena's");
            }
            expect(Inv.count(player, MageTrainingArena.ORB) == 3, Inv.count(player, MageTrainingArena.ORB) + " orbs from three shapes");
            expect(points(MinigameCurrencies.MTA_ENCHANTMENT) >= 21, "three Lvl-7 enchants paid only "
                    + points(MinigameCurrencies.MTA_ENCHANTMENT) + " points");
            MinigameManager.onBlock(player, "mta_enchant_hole", ench.origin.offset(0, 1, -2));
            expect(!Inv.has(player, MageTrainingArena.ORB, 1), "the orbs weren't deposited");
        } finally {
            leave(ench);
        }
        log.add("enchanting: " + points(MinigameCurrencies.MTA_ENCHANTMENT) + " Enchantment points");
        // ---- Creature Graveyard: bones from a pile, Bones to Bananas, fruit down the chute.
        Inv.give(player, "earth_rune", 20);
        Inv.give(player, "water_rune", 20);
        MageArenaSession grave = mtaRoom("graveyard");
        try {
            for (int i = 0; i < 12; i++) {
                MinigameManager.onBlock(player, "mta_bone_pile", grave.pileAt(0));
            }
            int bones = 0;
            String first = null;
            for (String b : MageTrainingArena.BONES) {
                bones += Inv.count(player, b);
                if (first == null && Inv.has(player, b, 1)) {
                    first = b;
                }
            }
            expect(bones == 12, "the pile gave " + bones + " bones");
            expect(MageArenaSession.useItem(player, first), "using the bones didn't cast Bones to Bananas");
            int[] fruit = grave.fruit(player);
            expect(fruit[0] >= 12 && fruit[0] <= 48 && fruit[1] == 0, "twelve bones made " + fruit[0] + " bananas");
            heal();
            MinigameManager.onBlock(player, "mta_food_chute", grave.origin.offset(0, 1, -12));
            expect(points(MinigameCurrencies.MTA_GRAVEYARD) >= 1, fruit[0] + " bananas paid no Graveyard point");
        } finally {
            leave(grave);
        }
        log.add("graveyard: " + points(MinigameCurrencies.MTA_GRAVEYARD) + " Graveyard points");
        // ---- The Rewards Guardian: a mist rune with the points just earned (1 / 1 / 15 / 1).
        int mist = Inv.count(player, "mist_rune");
        RewardShopService.buy(player, "mage_training_arena", entry("mage_training_arena", "mist_rune"), 1);
        expect(Inv.count(player, "mist_rune") == mist + 1, "the mist rune wasn't bought with the earned pizazz points");
        log.add("bought a mist rune from the Rewards Guardian");
        player.getInventory().clearContent();
        return String.join("; ", log);
    }
}
