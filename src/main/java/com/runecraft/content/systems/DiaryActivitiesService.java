package com.runecraft.content.systems;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.combat.MonsterStats;
import com.runecraft.combat.PrayerService;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.osrs.content.systems.DiaryActivities;
import com.runecraft.osrs.data.Bone;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseEntityCallback;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import com.runecraft.world.LandmarkBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The activities behind round 40's last diary tasks ({@link DiaryActivities}): chompy bird hunting (Rantz's ogre bellows, bloated
 * toads put down by the Feldip ponds, chompies that fly in and leave after a minute, registered kills and Rantz's hats and daily
 * arrows), Gertrude's kittens (a chosen colour with the ring of charos), feeding Varrock's stray dogs, grabbing the Desert
 * Phoenix's feathers, picking the white tree's fruit, the Altar of Nature and the emotes (the Stronghold of Security's four).
 * Rantz's and Gertrude's extra options are their sneak + right-click (OSRS: "Claim-hats" / "Kitten").
 */
final class DiaryActivitiesService {
    private static final String RANTZ = "rantz";
    private static final String GERTRUDE = "gertrude";
    private static final String CHOMPY_QUEST = "big_chompy_bird_hunting";
    private static final String KITTEN_QUEST = "gertrudes_cat";
    private static final String GARDEN_QUEST = "garden_of_tranquillity";
    private static final String NATURE_QUEST = "nature_spirit";
    private static final String BELLOWS_KEY = "RcBellowsGas";

    /** A toad put down: its chompies arrive at {@code arrive} (game tick). */
    private record Toad(UUID player, ServerLevel world, BlockPos at, long arrive) {
    }

    /** A chompy that flew in, and the game tick it flies off. */
    private record Chompy(DataBossEntity bird, long leaves) {
    }

    /** The white tree's fruit for one player and when that count was last set. */
    private record Tree(int fruit, long since) {
    }

    private static final List<Toad> TOADS = new ArrayList<>();
    private static final List<Chompy> CHOMPIES = new ArrayList<>();
    private static final Map<UUID, Tree> TREES = new HashMap<>();

    private DiaryActivitiesService() {
    }

    static void register() {
        wrap(RANTZ, CHOMPY_QUEST, DiaryActivitiesService::rantz, "Sneak and talk to Rantz to claim chompy bird hats, ogre arrows or bellows.");
        wrap(GERTRUDE, KITTEN_QUEST, DiaryActivitiesService::gertrude, "Sneak and talk to Gertrude to get another kitten.");
        SysNet.onAction("diary_rantz", (p, a, f) -> rantzAction(p, a));
        SysNet.onAction("diary_kitten", (p, a, f) -> {
            if (a.startsWith("colour:")) {
                try {
                    buyKitten(p, Integer.parseInt(a.substring("colour:".length())));
                } catch (NumberFormatException ignored) {
                    // bad button
                }
            } else if (a.equals("random")) {
                buyKitten(p, -1);
            }
        });
        SysNet.onAction("diary_emotes", (p, a, f) -> {
            perform(p, a);
            emotes(p);
        });
        wrap("brimstail", DiaryActivities.GLOUPHRIE_QUEST, (p, npc) -> crystalSaw(p),
                "Sneak and talk to Brimstail to have your crystal saw seed sung into a saw.");
        QuestScripts.onTalk(DiaryActivities.SAFTA_DOC, (p, npc) -> saftaDoc(p));
        SysNet.onAction("diary_machete", (p, a, f) -> {
            try {
                buyMachete(p, Integer.parseInt(a));
            } catch (NumberFormatException ignored) {
                // bad button
            }
        });
        GameEvents.onKill(DiaryActivitiesService::onKill);
        UseEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || world.isClientSide() || !(entity instanceof OsrsMonsterEntity monster)
                    || !(player instanceof ServerPlayer p) || monster.stats() == null || monster.stats().profile() == null) {
                return InteractionResult.PASS;
            }
            String id = monster.stats().profile().id;
            if (id.equals(DiaryActivities.STRAY_DOG)) {
                return feedDog(p, monster) ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
            if (id.equals(DiaryActivities.PHOENIX) && p.getMainHandItem().isEmpty()) {
                grabFeather(p, monster);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
        BlockHooks.register(DiaryActivities.WHITE_TREE, (p, pos, state) -> pickWhiteTree(p));
        BlockHooks.register(DiaryActivities.NATURE_ALTAR, (p, pos, state) -> natureAltar(p, pos));
        LandmarkBuilder.registerCustom(DiaryActivities.WHITE_TREE,
                (world, centre) -> DiaryObjectsService.place(world, centre, DiaryActivities.WHITE_TREE, false));
        LandmarkBuilder.registerCustom(DiaryActivities.NATURE_ALTAR,
                (world, centre) -> DiaryObjectsService.place(world, centre, DiaryActivities.NATURE_ALTAR, false));
        BlockHooks.register(DiaryActivities.SARIM_CHURCH_ALTAR, (p, pos, state) -> PrayerService.useAltar(p, pos));
        LandmarkBuilder.registerCustom(DiaryActivities.SARIM_CHURCH_ALTAR,
                (world, centre) -> DiaryObjectsService.place(world, centre, DiaryActivities.SARIM_CHURCH_ALTAR, true));
        wrap(DiaryActivities.SIR_TIFFY, DiaryActivities.RECRUITMENT_DRIVE, (p, npc) -> templeArmoury(p),
                "Sneak and talk to Sir Tiffy to buy Temple Knight armour.");
        SysNet.onAction("diary_tiffy", (p, a, f) -> {
            buyTempleArmour(p, a);
            templeArmoury(p);
        });
        BlockHooks.register(DiaryActivities.LECTERN, (p, pos, state) -> lectern(p));
        LandmarkBuilder.registerCustom(DiaryActivities.LECTERN,
                (world, centre) -> DiaryObjectsService.place(world, centre, DiaryActivities.LECTERN, true));
        SysNet.onAction("diary_lectern", (p, a, f) -> {
            makeTablet(p, a);
            lectern(p);
        });
        BlockHooks.register(DiaryActivities.DEMONIC_RUINS, (p, pos, state) -> PrayerService.useAltar(p, pos));
        LandmarkBuilder.registerCustom(DiaryActivities.DEMONIC_RUINS,
                (world, centre) -> DiaryObjectsService.place(world, centre, DiaryActivities.DEMONIC_RUINS, false));
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    /** Sneak + talk runs {@code mine} once {@code quest} is done; otherwise the NPC's quest dialogue, with a hint afterwards. */
    private static void wrap(String npc, String quest, BiConsumer<ServerPlayer, QuestNpcEntity> mine, String hint) {
        BiConsumer<ServerPlayer, QuestNpcEntity> earlier = QuestScripts.handler(npc);
        QuestScripts.onTalk(npc, (p, entity) -> {
            if (p.isShiftKeyDown() && questDone(p, quest)) {
                mine.accept(p, entity);
                return;
            }
            if (earlier != null) {
                earlier.accept(p, entity);
            }
            if (questDone(p, quest)) {
                Msg.game(p, hint);
            }
        });
    }

    private static boolean questDone(ServerPlayer p, String questId) {
        QuestInfo q = ContentRegistry.quest(questId);
        return q == null || Players.data(p).questComplete(q);
    }

    /** The highest Western Provinces tier completed (0 none .. 4 elite), for the chompy perks. */
    static int westernTiers(ServerPlayer p) {
        int best = 0;
        for (Diaries.Tier t : Diaries.Tier.values()) {
            if (Diaries.tierComplete(SysState.of(p).diaryDone, "western", t)) {
                best = t.ordinal() + 1;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ chompy bird hunting

    static int chompyKills(ServerPlayer p) {
        return SysState.of(p).diaryCounters.getOrDefault(DiaryActivities.CHOMPY_KILLS, 0);
    }

    private static boolean hasOgreBow(ServerPlayer p) {
        return DiaryActivities.CHOMPY_BOWS.stream().anyMatch(b -> Inv.has(p, b, 1));
    }

    /**
     * A chompy killed after Big Chompy Bird Hunting with an ogre bow counts. The ogre bow isn't a working ranged weapon here,
     * so carrying one (wielded or in the pack) is what counts. // approx
     */
    private static void onKill(ServerPlayer p, LivingEntity entity, MonsterStats stats) {
        if (stats == null || stats.profile() == null || !DiaryActivities.CHOMPY.equals(stats.profile().id)
                || !questDone(p, CHOMPY_QUEST) || !hasOgreBow(p)) {
            return;
        }
        int kills = SysState.of(p).diaryCounters.merge(DiaryActivities.CHOMPY_KILLS, 1, Integer::sum);
        DiaryActivities.Hat hat = DiaryActivities.HATS.stream().filter(h -> h.kills() == kills).findFirst().orElse(null);
        if (hat != null) {
            Msg.action(p, "You've registered " + kills + " chompy kills: Rantz has a " + hat.name().toLowerCase() + " for you.");
        }
        if (kills == 4000) {
            XpService.add(p, Skill.RANGED, DiaryActivities.CHOMPY_4000_XP);
            Msg.action(p, "You've been awarded ranged experience for your relentless pursuit of chompies!");
        }
    }

    private static int gas(ItemStack bellows) {
        return StackData.get(bellows).getInt(BELLOWS_KEY);
    }

    /** The pond the player stands by, or -1. */
    private static int pond(ServerPlayer p) {
        return DiaryActivities.pondAt(GielinorMap.toOsrsX(p.getBlockX()), GielinorMap.toOsrsY(p.getBlockZ()));
    }

    /**
     * Ogre bellows by a Feldip pond: an empty pair is filled with three loads of swamp gas; a full one blows up a swamp toad
     * (one load) into a bloated toad, at most three carried. The swamp's bubbles and toads aren't objects here. // approx
     */
    static void useBellows(ServerPlayer p, ItemStack bellows) {
        if (pond(p) < 0) {
            Msg.game(p, "You need to be by the swamp ponds of the Feldip Hills or the Chompy Marsh to use the bellows.");
            return;
        }
        int loads = gas(bellows);
        if (loads <= 0) {
            StackData.update(bellows, t -> t.putInt(BELLOWS_KEY, DiaryActivities.BELLOWS_LOADS));
            Msg.action(p, "You fill the bellows with swamp gas.");
            return;
        }
        if (Inv.count(p, DiaryActivities.BLOATED_TOAD) >= DiaryActivities.MAX_TOADS) {
            Msg.game(p, "You can't carry any more bloated toads.");
            return;
        }
        StackData.update(bellows, t -> t.putInt(BELLOWS_KEY, loads - 1));
        Inv.give(p, DiaryActivities.BLOATED_TOAD, 1);
        Msg.action(p, "You catch a swamp toad and inflate it with swamp gas.");
    }

    /** A bloated toad put down by a pond: a chompy (two with the diary perk) flies in shortly after. */
    static void putDownToad(ServerPlayer p, ItemStack toad) {
        if (!questDone(p, CHOMPY_QUEST)) {
            Msg.game(p, "Rantz knows what to do with that toad.");
            return;
        }
        if (pond(p) < 0) {
            Msg.game(p, "Chompy birds only come to the swamps of the Feldip Hills and the Chompy Marsh.");
            return;
        }
        toad.shrink(1);
        TOADS.add(new Toad(p.getUUID(), p.serverLevel(), p.blockPosition(), ActionManager.gameTick() + DiaryActivities.CHOMPY_ARRIVAL_TICKS));
        Msg.action(p, "You place the bloated toad on the ground.");
    }

    /** Spawns the chompies for a toad; returns them. */
    static List<DataBossEntity> chompiesArrive(ServerPlayer p, ServerLevel world, BlockPos at) {
        List<DataBossEntity> out = new ArrayList<>();
        BossDef def = ContentRegistry.boss(DiaryActivities.CHOMPY);
        if (def == null) {
            return out;
        }
        int n = p.getRandom().nextInt(100) < DiaryActivities.twoChompiesPercent(westernTiers(p)) ? 2 : 1;
        for (int i = 0; i < n; i++) {
            int x = at.getX() + (p.getRandom().nextBoolean() ? 1 : -1) * (3 + p.getRandom().nextInt(3));
            int z = at.getZ() + (p.getRandom().nextBoolean() ? 1 : -1) * (3 + p.getRandom().nextInt(3));
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            DataBossEntity bird = DataBossService.spawn(world, new BlockPos(x, y, z), def, null);
            if (bird != null) {
                CHOMPIES.add(new Chompy(bird, ActionManager.gameTick() + DiaryActivities.CHOMPY_TICKS));
                out.add(bird);
            }
        }
        if (!out.isEmpty()) {
            Msg.action(p, n == 2 ? "Two chompy birds swoop down on the toad!" : "A chompy bird swoops down on the toad!");
        }
        return out;
    }

    private static void tick() {
        long now = ActionManager.gameTick();
        for (Iterator<Toad> it = TOADS.iterator(); it.hasNext(); ) {
            Toad t = it.next();
            if (now < t.arrive()) {
                continue;
            }
            it.remove();
            ServerPlayer p = t.world().getServer().getPlayerList().getPlayer(t.player());
            if (p != null) {
                chompiesArrive(p, t.world(), t.at());
            }
        }
        for (Iterator<Chompy> it = CHOMPIES.iterator(); it.hasNext(); ) {
            Chompy c = it.next();
            if (c.bird().isRemoved() || !c.bird().isAlive()) {
                it.remove();
            } else if (now >= c.leaves()) {
                c.bird().discard();
                it.remove();
            }
        }
    }

    private static void rantz(ServerPlayer p, QuestNpcEntity npc) {
        QuestApi.say(p, "Rantz", "You want hatsies? You need kill many chompy! Rantz give you big stinky arrows too.");
        rantzMenu(p);
    }

    static void rantzMenu(ServerPlayer p) {
        int kills = chompyKills(p);
        SysNet.Screen s = new SysNet.Screen("diary_rantz", "Rantz");
        s.line("Chompy kills registered: " + kills, 0xC0E080);
        boolean bow = hasOgreBow(p);
        if (!bow) {
            s.line("Bring an ogre bow to claim a hat.", 0xE0A060);
        }
        for (int i = 0; i < DiaryActivities.HATS.size(); i++) {
            DiaryActivities.Hat h = DiaryActivities.HATS.get(i);
            boolean unlocked = kills >= h.kills();
            s.button("hat:" + i, h.name() + " (" + h.kills() + ")", unlocked ? "" : "Needs " + h.kills() + " chompy kills.", unlocked && bow);
            if (!unlocked) {
                break; // the next hat to work towards
            }
        }
        int arrows = DiaryActivities.rantzDailyArrows(westernTiers(p));
        s.button("arrows", "Claim " + arrows + " ogre arrows", arrows == 0 ? "Complete the Western Provinces easy diary first."
                : DiaryService.dailyLeft(p, "rantz_arrows", 1) > 0 ? "Once a day." : "Already claimed today.",
                arrows > 0 && DiaryService.dailyLeft(p, "rantz_arrows", 1) > 0);
        s.button("bellows", "Take some ogre bellows", "", true);
        s.send(p);
    }

    /** Rantz's options: claim a hat (fires the diary's hat keys), the daily ogre arrows, or a pair of bellows. */
    static void rantzAction(ServerPlayer p, String action) {
        if (!questDone(p, CHOMPY_QUEST)) {
            return;
        }
        if (action.startsWith("hat:")) {
            int i;
            try {
                i = Integer.parseInt(action.substring(4));
            } catch (NumberFormatException e) {
                return;
            }
            claimHat(p, i);
        } else if (action.equals("arrows")) {
            int arrows = DiaryActivities.rantzDailyArrows(westernTiers(p));
            if (arrows > 0 && DiaryService.useDaily(p, "rantz_arrows", 1)) {
                Inv.give(p, "ogre_arrow", arrows);
                QuestApi.say(p, "Rantz", "Here, " + arrows + " big arrows for you. Go kill chompy!");
            }
        } else if (action.equals("bellows")) {
            // OSRS: taken from the Locked Ogre chest in Rantz's cave, which isn't built; Rantz hands them over. // approx
            Inv.give(p, DiaryActivities.BELLOWS, 1);
            QuestApi.say(p, "Rantz", "You take bellows. Fill with swamp gas, blow up toady!");
        }
        rantzMenu(p);
    }

    static boolean claimHat(ServerPlayer p, int index) {
        if (index < 0 || index >= DiaryActivities.HATS.size()) {
            return false;
        }
        DiaryActivities.Hat hat = DiaryActivities.HATS.get(index);
        int kills = chompyKills(p);
        if (kills < hat.kills()) {
            QuestApi.say(p, "Rantz", "You not kill enough chompy for that hat! You need " + hat.kills() + ".");
            return false;
        }
        if (!hasOgreBow(p)) {
            QuestApi.say(p, "Rantz", "Where your big bow? Rantz only give hatsies to bow hunters.");
            return false;
        }
        if (!Inv.has(p, hat.item(), 1)) {
            Inv.give(p, hat.item(), 1);
        }
        QuestApi.say(p, "Rantz", "You great hunter! Here, you wear this " + hat.name().toLowerCase() + ".");
        for (String key : DiaryActivities.hatKeys(kills)) {
            GameEvents.fireActivity(p, key);
        }
        return true;
    }

    // ------------------------------------------------------------------ Gertrude's kittens

    private static boolean charmed(ServerPlayer p) {
        // The activated ring of charos; the quests module's ring isn't a ring-slot item here, so carrying it counts. // approx
        return (Inv.has(p, "ring_of_charos", 1) || ClueService.equipped(p, SysState.of(p), "ring_of_charos"))
                && QuestApi.stage(p, GARDEN_QUEST) > 0;
    }

    private static void gertrude(ServerPlayer p, QuestNpcEntity npc) {
        if (Inv.has(p, DiaryActivities.KITTEN, 1)) {
            QuestApi.say(p, "Gertrude", "You already have one of Fluffs' kittens, dear. Look after it!");
            return;
        }
        SysNet.Screen s = new SysNet.Screen("diary_kitten", "Gertrude's kittens");
        boolean coins = Inv.has(p, Inv.COINS, DiaryActivities.KITTEN_PRICE);
        if (charmed(p)) {
            QuestApi.say(p, "Gertrude", "Oh, what a charming adventurer! Of course you may pick whichever kitten you like.");
            s.line("Choose your kitten's colour (" + DiaryActivities.KITTEN_PRICE + " coins).");
            for (int i = 0; i < DiaryActivities.KITTEN_COLOURS.size(); i++) {
                s.button("colour:" + i, DiaryActivities.KITTEN_COLOURS.get(i), coins ? "" : "You need 100 coins.", coins);
            }
        } else {
            QuestApi.say(p, "Gertrude", "Fluffs has had more kittens! You can have one for " + DiaryActivities.KITTEN_PRICE + " coins.");
            s.button("random", "Buy a kitten (" + DiaryActivities.KITTEN_PRICE + " coins)", coins ? "" : "You need 100 coins.", coins);
        }
        s.send(p);
    }

    /** Buys a kitten; {@code colour} is an index of {@link DiaryActivities#KITTEN_COLOURS} (charmed players) or -1 for any. */
    static boolean buyKitten(ServerPlayer p, int colour) {
        if (!questDone(p, KITTEN_QUEST) || Inv.has(p, DiaryActivities.KITTEN, 1)
                || !Inv.has(p, Inv.COINS, DiaryActivities.KITTEN_PRICE)) {
            return false;
        }
        boolean chosen = colour >= 0 && colour < DiaryActivities.KITTEN_COLOURS.size() && charmed(p);
        int c = chosen ? colour : p.getRandom().nextInt(DiaryActivities.KITTEN_COLOURS.size());
        Inv.remove(p, Inv.COINS, DiaryActivities.KITTEN_PRICE);
        String name = DiaryActivities.KITTEN_COLOURS.get(c).toLowerCase();
        ItemStack kitten = Inv.stack(DiaryActivities.KITTEN, 1);
        kitten.set(DataComponents.CUSTOM_NAME, Component.literal("Kitten (" + name + ")"));
        Inv.give(p, kitten);
        QuestApi.say(p, "Gertrude", "Here you are, a lovely " + name + " kitten. Be good to it!");
        if (chosen) {
            GameEvents.fireActivity(p, "kitten_colour");
        }
        return true;
    }

    // ------------------------------------------------------------------ the Arceuus lectern

    /** The lectern's tablets: each needs the Arceuus spellbook, its spell's Magic level and runes, and a dark essence block. */
    static void lectern(ServerPlayer p) {
        SysNet.Screen s = new SysNet.Screen("diary_lectern", "Lectern");
        boolean book = SysState.of(p).book == com.runecraft.osrs.content.systems.Spellbooks.Book.ARCEUUS;
        boolean block = Inv.has(p, DiaryActivities.DARK_ESSENCE_BLOCK, 1);
        s.line(!book ? "You need to be on the Arceuus spellbook to study the lectern."
                : block ? "Choose a teleport tablet to make." : "Each tablet needs a dark essence block.");
        for (Map.Entry<String, String> e : DiaryActivities.LECTERN_TABLETS.entrySet()) {
            com.runecraft.osrs.content.systems.Spellbooks.Spell spell = com.runecraft.osrs.content.systems.Spellbooks.get(e.getValue());
            if (spell == null) {
                continue;
            }
            boolean ok = book && block && SpellService.canCast(p, spell, true);
            s.button(e.getKey(), spell.name().replace(" Teleport", "") + " tablet (level " + spell.level() + ")",
                    ok ? "" : "Needs Magic " + spell.level() + ", its runes and a dark essence block.", ok);
        }
        s.send(p);
    }

    /** Makes one tablet at the lectern: the spell's runes and XP plus a dark essence block (not a cast of the spell). */
    static boolean makeTablet(ServerPlayer p, String tablet) {
        String spellId = DiaryActivities.LECTERN_TABLETS.get(tablet);
        com.runecraft.osrs.content.systems.Spellbooks.Spell spell = spellId == null ? null
                : com.runecraft.osrs.content.systems.Spellbooks.get(spellId);
        if (spell == null || SysState.of(p).book != com.runecraft.osrs.content.systems.Spellbooks.Book.ARCEUUS) {
            return false;
        }
        if (!Inv.has(p, DiaryActivities.DARK_ESSENCE_BLOCK, 1)) {
            Msg.game(p, "You need a dark essence block to make a tablet here.");
            return false;
        }
        if (!SpellService.canCast(p, spell, false)) {
            return false;
        }
        Map<String, Integer> plan = SpellService.runePlan(p, spell.runes());
        if (plan == null) {
            return false;
        }
        SpellService.consume(p, plan);
        Inv.remove(p, DiaryActivities.DARK_ESSENCE_BLOCK, 1);
        Inv.give(p, tablet, 1);
        XpService.add(p, Skill.MAGIC, spell.xp());
        Msg.action(p, "You study the lectern and bind the spell into the dark essence: " + spell.name().toLowerCase() + " tablet.");
        GameEvents.fireActivity(p, "lectern:" + tablet);
        return true;
    }

    // ------------------------------------------------------------------ Sir Tiffy's Temple Knight armour

    /** Sir Tiffy's stock: Initiate armour after Recruitment Drive, Proselyte too after The Slug Menace (OSRS values). */
    static void templeArmoury(ServerPlayer p) {
        QuestApi.say(p, "Sir Tiffy Cashien", "What-ho! Kit yourself out like a proper Temple Knight, don'tchaknow.");
        SysNet.Screen s = new SysNet.Screen("diary_tiffy", "Temple Knight armour");
        boolean sallet = Inv.has(p, Inv.COINS, DiaryActivities.INITIATE_SALLET_PRICE);
        s.button("initiate_sallet", "Initiate sallet (" + DiaryActivities.INITIATE_SALLET_PRICE + " coins)",
                sallet ? "" : "Not enough coins.", sallet);
        for (DiaryActivities.TempleArmour a : DiaryActivities.TEMPLE_ARMOUR) {
            boolean unlocked = questDone(p, a.quest());
            boolean coins = Inv.has(p, Inv.COINS, a.value());
            s.button(a.item(), a.name() + " (" + a.value() + " coins)", !unlocked ? "Complete The Slug Menace first."
                    : coins ? "" : "Not enough coins.", unlocked && coins);
        }
        s.send(p);
    }

    static boolean buyTempleArmour(ServerPlayer p, String item) {
        if (!questDone(p, DiaryActivities.RECRUITMENT_DRIVE)) {
            return false;
        }
        int price;
        if (item.equals("initiate_sallet")) {
            price = DiaryActivities.INITIATE_SALLET_PRICE;
        } else {
            DiaryActivities.TempleArmour a = DiaryActivities.TEMPLE_ARMOUR.stream().filter(t -> t.item().equals(item)).findFirst()
                    .orElse(null);
            if (a == null || !questDone(p, a.quest())) {
                return false;
            }
            price = a.value();
        }
        if (!Inv.has(p, Inv.COINS, price)) {
            Msg.game(p, "You don't have enough coins.");
            return false;
        }
        Inv.remove(p, Inv.COINS, price);
        Inv.give(p, item, 1);
        GameEvents.fireActivity(p, "shop_buy:sir_tiffy:" + item);
        return true;
    }

    // ------------------------------------------------------------------ the crystal saw

    /**
     * After The Eyes of Glouphrie: the crystal saw seed sung into a crystal saw at the singing bowl in Brimstail's cave (the bowl
     * isn't an object of its own, so Brimstail's sneak + talk uses it); with neither seed nor saw, Brimstail gives a new seed.
     */
    static boolean crystalSaw(ServerPlayer p) {
        if (Inv.has(p, DiaryActivities.CRYSTAL_SAW_SEED, 1)) {
            Inv.remove(p, DiaryActivities.CRYSTAL_SAW_SEED, 1);
            Inv.give(p, DiaryActivities.CRYSTAL_SAW, 1);
            QuestApi.say(p, "Brimstail", "Place the seed in my singing bowl... There! The crystal has grown into a fine saw.");
            GameEvents.fireActivity(p, "crystal_saw");
            return true;
        }
        if (!Inv.has(p, DiaryActivities.CRYSTAL_SAW, 1)) {
            Inv.give(p, DiaryActivities.CRYSTAL_SAW_SEED, 1);
            QuestApi.say(p, "Brimstail", "Lost your crystal seed? Here, King Narnode left me another. My singing bowl can turn it into a saw.");
            return false;
        }
        QuestApi.say(p, "Brimstail", "That crystal saw of yours looks in fine shape.");
        return false;
    }

    // ------------------------------------------------------------------ Safta Doc's gem machetes

    private static int gems(ServerPlayer p, String gem) {
        return Inv.count(p, gem) + Inv.count(p, "uncut_" + gem);
    }

    private static boolean canBuy(ServerPlayer p, DiaryActivities.Machete m) {
        return Inv.has(p, DiaryActivities.GOUT_TUBER, 1) && Inv.has(p, DiaryActivities.TRADING_STICKS, m.sticks())
                && gems(p, m.gem()) >= DiaryActivities.MACHETE_GEMS;
    }

    /**
     * Safta Doc (after Jungle Potion): a gout tuber, three opals, jades or red topaz (cut or uncut) and 300 / 600 / 1,200
     * trading sticks for a gem machete. The Tai Bwo Wannai favour OSRS also asks for isn't checked. // approx
     */
    private static void saftaDoc(ServerPlayer p) {
        if (!questDone(p, "jungle_potion")) {
            QuestApi.say(p, "Safta Doc", "Hello there. I'm just an old fisherman, resting my bones.");
            return;
        }
        QuestApi.say(p, "Safta Doc", "I make fine machetes, set with gems. Bring me a gout tuber, three gems and some trading sticks.");
        SysNet.Screen s = new SysNet.Screen("diary_machete", "Safta Doc's machetes");
        for (int i = 0; i < DiaryActivities.MACHETES.size(); i++) {
            DiaryActivities.Machete m = DiaryActivities.MACHETES.get(i);
            com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(m.item());
            String name = def == null ? m.item() : def.name();
            s.button(String.valueOf(i), name + " - 3 " + m.gem().replace('_', ' ') + "s, " + m.sticks() + " trading sticks, a gout tuber",
                    canBuy(p, m) ? "" : "You don't have everything for this one.", canBuy(p, m));
        }
        s.send(p);
    }

    static boolean buyMachete(ServerPlayer p, int index) {
        if (index < 0 || index >= DiaryActivities.MACHETES.size() || !questDone(p, "jungle_potion")) {
            return false;
        }
        DiaryActivities.Machete m = DiaryActivities.MACHETES.get(index);
        if (!canBuy(p, m)) {
            QuestApi.say(p, "Safta Doc", "You haven't got everything I need for that machete.");
            return false;
        }
        Inv.remove(p, DiaryActivities.GOUT_TUBER, 1);
        Inv.remove(p, DiaryActivities.TRADING_STICKS, m.sticks());
        int left = DiaryActivities.MACHETE_GEMS - Inv.remove(p, "uncut_" + m.gem(), DiaryActivities.MACHETE_GEMS);
        if (left > 0) {
            Inv.remove(p, m.gem(), left);
        }
        Inv.give(p, m.item(), 1);
        QuestApi.say(p, "Safta Doc", "There you go, a fine machete. Look after it!");
        GameEvents.fireActivity(p, "machete_trade");
        return true;
    }

    // ------------------------------------------------------------------ stray dogs and the Desert Phoenix

    /** Bones held on a stray dog: it eats them (any bones but the two it refuses). */
    static boolean feedDog(ServerPlayer p, LivingEntity dog) {
        ItemStack held = p.getMainHandItem();
        String id = held.isEmpty() ? "" : Inv.idOf(held);
        if (DiaryActivities.DOG_REFUSES.contains(id)) {
            Msg.game(p, "That's disgusting - you can't give it to the poor dog!");
            return true;
        }
        if (Bone.byItem(id) == null) {
            return false;
        }
        held.shrink(1);
        dog.playSound(net.minecraft.sounds.SoundEvents.WOLF_AMBIENT, 1f, 1.2f);
        Msg.action(p, "You give the dog a nice bone. It happily gnaws on it.");
        GameEvents.fireActivity(p, "dog_bone");
        return true;
    }

    /** Grab-feather: 25 Thieving (boostable); a failed grab stuns and hurts. One feather owned at a time. */
    static void grabFeather(ServerPlayer p, LivingEntity phoenix) {
        if (!Stun.check(p)) {
            return;
        }
        int level = Players.data(p).skills().currentLevel(Skill.THIEVING);
        if (level < DiaryActivities.PHOENIX_THIEVING) {
            Msg.game(p, "You need a Thieving level of " + DiaryActivities.PHOENIX_THIEVING + " to grab a feather from the phoenix.");
            return;
        }
        if (Inv.has(p, DiaryActivities.PHOENIX_FEATHER, 1) || Inv.has(p, "phoenix_quill_pen", 1)) {
            Msg.game(p, "You already have a phoenix feather.");
            return;
        }
        if (p.getRandom().nextDouble() >= DiaryActivities.phoenixGrabChance(level)) {
            Msg.red(p, "The phoenix flaps its wings at you angrily and you are stunned!");
            // typeless: a stun, not an attack the player would auto-retaliate against
            com.runecraft.combat.CombatHandler.applyDamage(null, p, DiaryActivities.PHOENIX_FAIL_DAMAGE);
            Stun.apply(p, DiaryActivities.PHOENIX_STUN_TICKS);
            return;
        }
        Inv.give(p, DiaryActivities.PHOENIX_FEATHER, 1);
        XpService.add(p, Skill.THIEVING, DiaryActivities.PHOENIX_XP);
        Msg.action(p, "You grab a feather from the phoenix's tail.");
        GameEvents.fireObtain(p, DiaryActivities.PHOENIX_FEATHER, 1);
        GameEvents.fireActivity(p, "phoenix_feather");
    }

    // ------------------------------------------------------------------ the white tree and the Altar of Nature

    /** Picks a white tree fruit (Garden of Tranquillity, 25 Farming): 12 Farming XP; up to 4 fruit, one regrowing every ~5 min. */
    static boolean pickWhiteTree(ServerPlayer p) {
        if (!questDone(p, GARDEN_QUEST)) {
            Msg.game(p, "The white tree has no fruit yet. Perhaps Queen Ellamaria's garden needs more work.");
            return false;
        }
        if (Players.data(p).skills().currentLevel(Skill.FARMING) < DiaryActivities.WHITE_TREE_FARMING) {
            Msg.game(p, "You need a Farming level of " + DiaryActivities.WHITE_TREE_FARMING + " to pick the white tree's fruit.");
            return false;
        }
        long now = ActionManager.gameTick();
        Tree t = TREES.get(p.getUUID());
        int fruit = t == null ? DiaryActivities.WHITE_TREE_MAX_FRUIT : DiaryActivities.whiteTreeFruit(t.fruit(), now - t.since());
        if (fruit <= 0) {
            Msg.game(p, "There are no fruit left on the tree. More will grow soon.");
            return false;
        }
        // Keep the regrowth clock running from the last full fruit grown.
        long since = t == null || fruit >= DiaryActivities.WHITE_TREE_MAX_FRUIT ? now
                : t.since() + (now - t.since()) / DiaryActivities.WHITE_TREE_REGROW_TICKS * DiaryActivities.WHITE_TREE_REGROW_TICKS;
        TREES.put(p.getUUID(), new Tree(fruit - 1, since));
        Inv.give(p, DiaryActivities.WHITE_TREE_FRUIT, 1);
        XpService.add(p, Skill.FARMING, DiaryActivities.WHITE_TREE_XP);
        Msg.action(p, "You pick a fruit from the white tree.");
        GameEvents.fireObtain(p, DiaryActivities.WHITE_TREE_FRUIT, 1);
        return true;
    }

    /** The Altar of Nature: open to those who finished Nature Spirit; prays like any world altar. */
    static void natureAltar(ServerPlayer p, BlockPos pos) {
        if (!questDone(p, NATURE_QUEST)) {
            Msg.game(p, "The altar's power is closed to you. Perhaps Filliman Tarlock needs your help first.");
            return;
        }
        PrayerService.useAltar(p, pos);
    }

    // ------------------------------------------------------------------ emotes

    /** Whether a player has unlocked an emote (the Stronghold's four come from its reward chests, in floor order). */
    static boolean unlocked(ServerPlayer p, String emote) {
        int k = DiaryActivities.STRONGHOLD_EMOTES.indexOf(emote);
        if (k >= 0) {
            return com.runecraft.content.dungeons.DungeonsHooks.securityChestOpened(p, k);
        }
        return DiaryActivities.BASIC_EMOTES.contains(emote);
    }

    /** The emote list. */
    static void emotes(ServerPlayer p) {
        SysNet.Screen s = new SysNet.Screen("diary_emotes", "Emotes");
        String[] chests = {"Gift of Peace", "Grain of Plenty", "Box of Health", "Cradle of Life"};
        for (String e : DiaryActivities.BASIC_EMOTES) {
            s.button(e, e, "", true);
        }
        for (int i = 0; i < DiaryActivities.STRONGHOLD_EMOTES.size(); i++) {
            String e = DiaryActivities.STRONGHOLD_EMOTES.get(i);
            boolean open = unlocked(p, e);
            s.button(e, e, open ? "" : "Unlocked by the Stronghold of Security's " + chests[i] + ".", open);
        }
        s.send(p);
    }

    /** Performs an emote the player has unlocked. */
    static boolean perform(ServerPlayer p, String emote) {
        if (!unlocked(p, emote)) {
            return false;
        }
        p.swing(InteractionHand.MAIN_HAND, true);
        Msg.action(p, "You perform the " + emote + " emote.");
        GameEvents.fireActivity(p, DiaryActivities.emoteKey(emote));
        return true;
    }

    // ------------------------------------------------------------------ held items

    /** Held items this service uses on right-click (the client predicts the result). */
    static boolean handles(String id) {
        return id.equals(DiaryActivities.BELLOWS) || id.equals(DiaryActivities.BLOATED_TOAD) || id.equals(DiaryActivities.SWAMP_TOAD);
    }

    static boolean use(ServerPlayer p, ItemStack stack, String id) {
        if (id.equals(DiaryActivities.BELLOWS)) {
            useBellows(p, stack);
        } else if (id.equals(DiaryActivities.SWAMP_TOAD)) {
            stack.shrink(1);
            Inv.give(p, DiaryActivities.TOADS_LEGS, 1);
            Msg.action(p, "You pull the legs off the toad. Poor toad. At least they'll grow back.");
        } else {
            putDownToad(p, stack);
        }
        return true;
    }
}
