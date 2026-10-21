package com.runecraft.content.systems;

import com.runecraft.combat.PrayerService;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.Prayer;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.NpcDef;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.osrs.content.systems.DiaryExtras;
import com.runecraft.osrs.content.systems.DiaryRewards;
import com.runecraft.osrs.content.systems.DiaryTransport;
import com.runecraft.osrs.content.systems.Teleports;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.selftest.SelfTest;
import com.runecraft.selftest.TestContext;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

/**
 * Round 40 self-test of the achievement diaries, run as the player (named {@code minigame:diary_*} so the client walkthrough's
 * real-player run includes it): a sample of tasks in every area completed through the game's own event paths while the
 * player stands in the task's area, every task wired to a new mechanic this round (eagle and train trips through the
 * transport service, NPC trips, fairy rings, altars through the prayer service, the memoirs' five cities, pumps and wells
 * through the objects service, the five shortcuts, Tears of Guthix), the gear/prayer/weapon conditions (first missed, then
 * met) and the rewards (a claim with its Karamja lamp, taskmaster proximity, teleport charges, the travel fare perks).
 */
public final class DiarySelfTest {
    private DiarySelfTest() {
    }

    public static List<SelfTest.Check> checks(TestContext c) {
        List<SelfTest.Check> checks = new ArrayList<>();
        checks.add(new SelfTest.Check("minigame:diary_area_sample", () -> areaSample(c)));
        checks.add(new SelfTest.Check("minigame:diary_travel", () -> travel(c)));
        checks.add(new SelfTest.Check("minigame:diary_altars", () -> altars(c)));
        checks.add(new SelfTest.Check("minigame:diary_conditions", () -> conditions(c)));
        checks.add(new SelfTest.Check("minigame:diary_memoirs", () -> memoirs(c)));
        checks.add(new SelfTest.Check("minigame:diary_water", () -> water(c)));
        checks.add(new SelfTest.Check("minigame:diary_shortcuts_tears", () -> shortcutsAndTears(c)));
        checks.add(new SelfTest.Check("minigame:diary_rewards", () -> rewards(c)));
        checks.add(new SelfTest.Check("minigame:diary_taskmasters", () -> taskmasters(c)));
        return checks;
    }

    // ------------------------------------------------------------------ helpers

    private static Diaries.Task task(String descPrefix) {
        return Diaries.tasks().stream().filter(t -> t.description().startsWith(descPrefix)).findFirst()
                .orElseThrow(() -> new AssertionError("no diary task " + descPrefix));
    }

    private static boolean done(TestContext c, Diaries.Task t) {
        return SysState.of(c.player).diaryDone.contains(t.id());
    }

    private static void expect(TestContext c, Diaries.Task t, boolean want, String after) {
        if (done(c, t) != want) {
            throw new AssertionError(t.id() + " (" + t.description() + ") " + (want ? "not completed" : "completed") + " after " + after);
        }
    }

    /** Stands the player over an OSRS tile (high above it unless {@code blockY} is given), where area checks look. */
    private static void at(TestContext c, int x, int y) {
        at(c, x, y, 250);
    }

    private static void at(TestContext c, int x, int y, int blockY) {
        c.player.teleportTo(c.world, GielinorMap.toBlockX(x) + 0.5, blockY, GielinorMap.toBlockZ(y) + 0.5, c.player.getYRot(),
                c.player.getXRot());
    }

    /** A creature named like the task's monster (the diary matches kill names), not added to the world. */
    private static void kill(TestContext c, String name) {
        LivingEntity e = EntityType.ZOMBIE.create(c.world);
        if (e == null) {
            throw new AssertionError("could not create a creature");
        }
        e.setCustomName(Component.literal(name));
        GameEvents.fireKill(c.player, e, null);
    }

    private static void wield(TestContext c, String item) {
        c.player.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack(item, 1));
    }

    // ------------------------------------------------------------------ checks

    /** Two tasks per area through kills, obtains, XP drops and casts, standing where each task must happen. */
    private static String areaSample(TestContext c) {
        c.completeQuests(null);
        int n = 0;
        // Ardougne: a cake from the market, and the Ardougne Teleport spell (after Plague City).
        Diaries.Task cake = task("Steal a cake from the East Ardougne market stalls");
        at(c, 3222, 3218);
        GameEvents.fireObtain(c.player, "cake", 1);
        expect(c, cake, false, "a cake in Lumbridge");
        at(c, 2662, 3305);
        GameEvents.fireObtain(c.player, "cake", 1);
        expect(c, cake, true, "a cake in East Ardougne");
        GameEvents.fireCast(c.player, "ardougne_teleport");
        expect(c, task("Cast the Ardougne Teleport spell"), true, "casting it");
        n += 2;
        // Desert: a vulture and 5 clay.
        at(c, 3330, 2950);
        kill(c, "Vulture");
        expect(c, task("Kill a Vulture"), true, "killing one in the desert");
        Diaries.Task clay = task("Mine 5 clay in the north-eastern desert");
        GameEvents.fireObtain(c.player, "clay", 4);
        expect(c, clay, false, "4 clay");
        GameEvents.fireObtain(c.player, "clay", 1);
        expect(c, clay, true, "5 clay");
        n += 2;
        // Falador: a mind tiara (anywhere), a duck in Falador Park.
        GameEvents.fireObtain(c.player, "mind_tiara", 1);
        expect(c, task("Make a Mind Tiara"), true, "binding one");
        at(c, 2986, 3370);
        kill(c, "Duck");
        expect(c, task("Kill a duck in Falador Park"), true, "killing one in the park");
        n += 2;
        // Fremennik: five rock crabs.
        Diaries.Task crabs = task("Kill 5 Rock crabs");
        at(c, 2673, 3712);
        for (int i = 0; i < 4; i++) {
            kill(c, "Rock crab");
        }
        expect(c, crabs, false, "4 rock crabs");
        kill(c, "Rock crab");
        expect(c, crabs, true, "5 rock crabs");
        n++;
        // Kandarin: a mackerel at Catherby.
        at(c, 2850, 3430);
        GameEvents.fireObtain(c.player, "raw_mackerel", 1);
        expect(c, task("Catch a mackerel at Catherby"), true, "catching one in Kandarin");
        n++;
        // Karamja: five bananas.
        at(c, 2915, 3160);
        GameEvents.fireObtain(c.player, "banana", 5);
        expect(c, task("Pick 5 bananas"), true, "picking 5 on Karamja");
        n++;
        // Kourend & Kebos: a sand crab (anywhere).
        kill(c, "Sand crab");
        expect(c, task("Kill a sand crab"), true, "killing one");
        n++;
        // Lumbridge & Draynor: water runes.
        GameEvents.fireObtain(c.player, "water_rune", 5);
        expect(c, task("Craft some water runes"), true, "crafting them");
        n++;
        // Morytania: a ghoul.
        at(c, 3490, 3480);
        kill(c, "Ghoul");
        expect(c, task("Kill a Ghoul"), true, "killing one in Morytania");
        n++;
        // Varrock: Thieving XP from the tea stall, in Varrock.
        at(c, 3270, 3410);
        GameEvents.fireXp(c.player, Skill.THIEVING, 160);
        expect(c, task("Steal from the tea stall in Varrock"), true, "Thieving XP in Varrock");
        n++;
        // Western Provinces: a terrorbird in its enclosure.
        at(c, 2378, 3434);
        kill(c, "Terrorbird");
        expect(c, task("Kill a Terrorbird in the Terrorbird enclosure"), true, "killing one there");
        n++;
        // Wilderness: a mammoth.
        at(c, 3170, 3600);
        kill(c, "Mammoth");
        expect(c, task("Kill a Mammoth in the Wilderness"), true, "killing one in the Wilderness");
        n++;
        c.home();
        return n + " tasks in 12 areas";
    }

    /** The eagles and the train through the transport service; NPC trips, fairy rings by their keys. */
    private static String travel(TestContext c) {
        c.completeQuests(null);
        Inv.give(c.player, "rope", 2);
        DiaryTransport.Link toFeldip = DiaryTransport.from("eagles_peak").stream().filter(l -> l.to().equals("feldip_hills")).findFirst()
                .orElseThrow();
        if (!DiaryTransportService.travel(c.player, toFeldip)) {
            throw new AssertionError("the eagle did not fly to the Feldip Hills");
        }
        if (Inv.count(c.player, "rope") != 1) {
            throw new AssertionError("the eagle flight used " + (2 - Inv.count(c.player, "rope")) + " ropes");
        }
        int[] pos = SysTeleport.osrs(c.player);
        if (!Teleports.within(pos[0], pos[1], 2515, 2922, 8)) {
            throw new AssertionError("the eagle landed at " + pos[0] + "," + pos[1]);
        }
        expect(c, task("Travel from Eagles' Peak to the Feldip Hills by Eagle"), true, "the flight");
        DiaryTransport.Link toDesert = DiaryTransport.from("eagles_peak").stream().filter(l -> l.to().equals("desert")).findFirst()
                .orElseThrow();
        DiaryTransportService.travel(c.player, toDesert);
        expect(c, task("Travel to the Desert via the Eagle transport system"), true, "the flight to the desert");
        if (DiaryTransportService.travel(c.player, toDesert)) {
            throw new AssertionError("an eagle flew without a rope");
        }
        DiaryTransport.Link train = DiaryTransport.from("dorgesh_kaan").get(0);
        if (!DiaryTransportService.travel(c.player, train)) {
            throw new AssertionError("the train did not leave Dorgesh-Kaan");
        }
        expect(c, task("Take the train from Dorgesh-Kaan to Keldagrim"), true, "the train ride");
        c.home();
        // NPC trips report their route and destination (TravelService), the fairy rings their code.
        String[][] trips = {{"travel:carpet_shantay:pollnivneach", "Travel from the Shantay Pass to Pollnivneach"},
                {"travel:carpet_pollnivneach_north:shantay_pass", null},
                {"travel:spirit_tree_grown:gnome_stronghold", "Travel to the Gnome Stronghold by Spirit Tree"},
                {"travel:spirit_tree_grand_exchange:feldip_hills", "Use the spirit tree in the north eastern corner of Grand Exchange"},
                {"travel:cart_shilo_brimhaven:brimhaven", "Use Vigroy and Hajedy's cart service"},
                {"travel:charter_karamja_ship_yard:port_sarim", "Charter a ship from the shipyard"},
                {"travel:boat_swamp_hollows:mortton", "Board the Swamp boat at the Hollows"},
                {"travel:glider_0:gandius_karamja", "Use the gnome glider to travel to Karamja"},
                {"travel:canoe_lumbridge:edgeville", "Travel from Lumbridge to Edgeville on a Waka Canoe"},
                {"fairy_ring:bis", "Enter the unicorn pen in Ardougne Zoo using Fairy rings"},
                {"fairy_ring:cip", "Travel to Miscellania by Fairy ring"}};
        Diaries.Task uzer = task("Travel to Uzer via magic carpet");
        for (String[] trip : trips) {
            GameEvents.fireActivity(c.player, trip[0]);
            if (trip[1] != null) {
                expect(c, task(trip[1]), true, trip[0]);
            }
        }
        expect(c, uzer, false, "carpet rides that did not go to Uzer");
        GameEvents.fireActivity(c.player, "travel:carpet_shantay:uzer");
        expect(c, uzer, true, "a carpet ride to Uzer");
        return "2 eagle flights, the train, " + trips.length + " trip keys";
    }

    /** Altars through the prayer service: the Chaos Altar, Emir's Arena with Smite (missed without it), Sophanem's 85 points. */
    private static String altars(TestContext c) {
        PlayerData d = Players.data(c.player);
        Diaries.Task chaos = task("Pray at the Chaos Altar in Western Wilderness");
        at(c, 3222, 3218);
        d.setPrayerPoints(1);
        PrayerService.useAltar(c.player, null);
        expect(c, chaos, false, "praying at an altar in Lumbridge");
        at(c, 2952, 3825);
        d.setPrayerPoints(1);
        PrayerService.useAltar(c.player, null);
        expect(c, chaos, true, "praying at the Chaos Altar");
        Diaries.Task emir = task("Recharge your prayer at the Emir's Arena with Smite activated");
        c.completeQuests(null);
        at(c, 3343, 3273);
        d.activePrayers().clear();
        d.setPrayerPoints(1);
        PrayerService.useAltar(c.player, null);
        expect(c, emir, false, "praying there without Smite");
        d.activePrayers().add(Prayer.SMITE);
        d.setPrayerPoints(1);
        PrayerService.useAltar(c.player, null);
        expect(c, emir, true, "praying there with Smite on");
        d.activePrayers().clear();
        Diaries.Task sophanem = task("Restore at least 85 Prayer points when praying at the altar in Sophanem");
        at(c, 3290, 2759);
        d.setPrayerPoints(50);
        PrayerService.useAltar(c.player, null);
        expect(c, sophanem, false, "restoring 49 points");
        d.setPrayerPoints(1);
        PrayerService.useAltar(c.player, null);
        expect(c, sophanem, true, "restoring 98 points");
        c.home();
        return "3 altars";
    }

    /** Conditions: the Wolfbane dagger, full Prospector, a complete Barrows set, each first missed and then met. */
    private static String conditions(TestContext c) {
        c.completeQuests(null);
        Diaries.Task werewolf = task("Kill a Werewolf in its human form using the Wolfbane Dagger");
        at(c, 3490, 3480);
        kill(c, "Werewolf");
        expect(c, werewolf, false, "a kill without the dagger");
        wield(c, "wolfbane");
        kill(c, "Werewolf");
        expect(c, werewolf, true, "a kill with Wolfbane");
        Diaries.Task guild = task("Enter the Mining Guild wearing full Prospector");
        at(c, 3016, 3339);
        DiaryService.check(c.player, Diaries.Type.VISIT);
        expect(c, guild, false, "entering without Prospector");
        c.player.setItemSlot(EquipmentSlot.HEAD, Inv.stack("prospector_helmet", 1));
        c.player.setItemSlot(EquipmentSlot.CHEST, Inv.stack("prospector_jacket", 1));
        c.player.setItemSlot(EquipmentSlot.LEGS, Inv.stack("prospector_legs", 1));
        c.player.setItemSlot(EquipmentSlot.FEET, Inv.stack("prospector_boots", 1));
        DiaryService.check(c.player, Diaries.Type.VISIT);
        expect(c, guild, true, "entering in full Prospector");
        c.home();
        Diaries.Task barrows = task("Loot the Barrows chest while wearing any complete Barrows set");
        GameEvents.fireActivity(c.player, "chest_open:barrows_chest");
        expect(c, barrows, false, "a chest in Prospector");
        c.player.setItemSlot(EquipmentSlot.HEAD, Inv.stack("dharoks_helm", 1));
        c.player.setItemSlot(EquipmentSlot.CHEST, Inv.stack("dharoks_platebody", 1));
        c.player.setItemSlot(EquipmentSlot.LEGS, Inv.stack("dharoks_platelegs", 1));
        wield(c, "dharoks_greataxe");
        GameEvents.fireActivity(c.player, "chest_open:barrows_chest");
        expect(c, barrows, true, "a chest in Dharok's set");
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            c.player.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }
        return "3 conditions";
    }

    /** Kharedst's memoirs: four cities leave the task open (the progress is kept), the fifth completes it. */
    private static String memoirs(TestContext c) {
        c.completeQuests(null);
        Diaries.Task memoirs = task("Use Kharedst's memoirs to teleport to all five cities");
        List<Teleports.Destination> cities = Teleports.MEMOIRS;
        for (int i = 0; i < cities.size(); i++) {
            String book = i % 2 == 0 ? "kharedsts_memoirs" : "book_of_the_dead";
            GameEvents.fireActivity(c.player, Diaries.teleportKey(book, null, cities.get(i).name()));
            // A repeat of the same city never completes it.
            GameEvents.fireActivity(c.player, Diaries.teleportKey(book, null, cities.get(0).name()));
            expect(c, memoirs, i == cities.size() - 1, (i + 1) + " cities");
        }
        return cities.size() + " cities";
    }

    /** The Falador pump and the Rellekka well through the objects service: a bucket fills, a jug doesn't count. */
    private static String water(TestContext c) {
        Diaries.Task pump = task("Fill a bucket from the pump north of Falador West Bank");
        at(c, DiaryExtras.FALADOR_PUMP_X, DiaryExtras.FALADOR_PUMP_Y);
        wield(c, "minecraft:bucket");
        DiaryObjectsService.fillWater(c.player, "pump");
        if (Inv.count(c.player, "minecraft:water_bucket") != 1) {
            throw new AssertionError("the pump did not fill the bucket");
        }
        expect(c, pump, true, "filling a bucket at the pump");
        Diaries.Task well = task("Fill a bucket with water at the Rellekka well");
        at(c, DiaryExtras.RELLEKKA_WELL_X, DiaryExtras.RELLEKKA_WELL_Y);
        wield(c, "jug");
        DiaryObjectsService.fillWater(c.player, "well");
        expect(c, well, false, "filling a jug");
        wield(c, "minecraft:bucket");
        DiaryObjectsService.fillWater(c.player, "well");
        expect(c, well, true, "filling a bucket at the well");
        c.home();
        return "pump and well";
    }

    /** The five shortcuts built this round and Tears of Guthix's 100 tears. */
    private static String shortcutsAndTears(TestContext c) {
        c.completeQuests(null);
        String[][] uses = {{"dwarven_mine_narrow_crevice", "Squeeze through the crevice in the Dwarven mines"},
                {"taverley_dungeon_strange_floor", "Jump over the strange floor in Taverley dungeon"},
                {"edgeville_dungeon_pipe", "Squeeze through the obstacle pipe in Edgeville dungeon"},
                {"zanaris_jutting_wall", "Squeeze past the jutting wall on your way to the cosmic altar"},
                {"brimhaven_ropeswing", "Use the rope swing to travel to the small island north-west of Karamja"}};
        for (String[] u : uses) {
            if (com.runecraft.osrs.content.skilling.AgilityShortcuts.indoor(u[0]) == null
                    && com.runecraft.osrs.content.skilling.AgilityShortcuts.SHORTCUTS.stream().noneMatch(s -> s.id().equals(u[0]))) {
                throw new AssertionError("no shortcut " + u[0]);
            }
            GameEvents.fireActivity(c.player, "shortcut:" + u[0]);
            expect(c, task(u[1]), true, "using " + u[0]);
        }
        Diaries.Task tears = task("Collect at least 100 Tears of Guthix in one visit");
        GameEvents.fireActivity(c.player, "minigame_win:tears_of_guthix");
        expect(c, tears, false, "a visit without 100 tears");
        GameEvents.fireActivity(c.player, "tears_of_guthix:100");
        expect(c, tears, true, "100 tears");
        return uses.length + " shortcuts and the tears";
    }

    /** A claim with its Karamja lamp, reclaiming, the teleport charges and the travel fare perks. */
    private static String rewards(TestContext c) {
        SysState.SysPlayer sp = SysState.of(c.player);
        Diaries.Area karamja = Diaries.area("karamja");
        Diaries.tasks("karamja", Diaries.Tier.EASY).forEach(t -> sp.diaryDone.add(t.id()));
        DiaryService.claim(c.player, karamja, Diaries.Tier.EASY);
        if (!Inv.has(c.player, "karamja_gloves_1", 1) || !Inv.has(c.player, "antique_lamp_karamja_easy", 1)) {
            throw new AssertionError("the easy Karamja claim gave no gloves or Karamja lamp");
        }
        PlayerData d = Players.data(c.player);
        d.skills().setXpTenths(Skill.COOKING, 0);
        DiaryService.rubLamp(c.player, "karamja_easy", Skill.COOKING);
        if (d.skills().xpTenths(Skill.COOKING) != 10_000) {
            throw new AssertionError("the Karamja easy lamp gave " + d.skills().xpTenths(Skill.COOKING) / 10 + " XP, not 1,000 at level 1");
        }
        d.skills().setXpTenths(Skill.COOKING, Experience.xpForLevel(99) * 10);
        // Lost: the taskmaster hands out a replacement, without a second lamp.
        Inv.remove(c.player, "karamja_gloves_1", 1);
        DiaryService.claim(c.player, karamja, Diaries.Tier.EASY);
        if (!Inv.has(c.player, "karamja_gloves_1", 1) || Inv.has(c.player, "antique_lamp_karamja_easy", 1)) {
            throw new AssertionError("reclaiming the gloves did not give exactly the gloves back");
        }
        // Too far from any taskmaster: no claim.
        Diaries.Area varrock = Diaries.area("varrock");
        Diaries.tasks("varrock", Diaries.Tier.EASY).forEach(t -> sp.diaryDone.add(t.id()));
        DiaryService.claimFromTaskmaster(c.player, varrock, Diaries.Tier.EASY);
        if (sp.diaryClaimed.contains("varrock:EASY")) {
            throw new AssertionError("claimed the Varrock reward with no taskmaster near");
        }
        // Teleport charges: Morytania legs 1 (2 a day to the Ectofuntus), Desert amulet 3 (still one Nardah teleport a day).
        DiaryRewards.Tele ecto = DiaryService.rewardTele("morytania_legs_1", 0);
        DiaryRewards.Tele nardah = DiaryService.rewardTele("desert_amulet_3", 0);
        if (ecto == null || ecto.perDay(1) != 2 || nardah == null || nardah.perDay(3) != 1) {
            throw new AssertionError("reward teleport charges: legs 1 " + (ecto == null ? "none" : ecto.perDay(1)) + ", amulet 3 "
                    + (nardah == null ? "none" : nardah.perDay(3)));
        }
        if (DiaryService.dailyLeft(c.player, ecto.key(), 2) != 2) {
            throw new AssertionError("fresh Ectofuntus teleports: " + DiaryService.dailyLeft(c.player, ecto.key(), 2));
        }
        // Fares: Karamja gloves halve the Musa Point boat; the hard Desert diary makes carpets free.
        DiaryRewards.TierDone tiers = (area, tier) -> Diaries.tierComplete(sp.diaryDone, area, tier);
        if (DiaryRewards.travelFare("boat_karamja_port_sarim", "Port Sarim", 30, true, tiers) != 15
                || DiaryRewards.travelFare("carpet_shantay", "Pollnivneach", 200, false, tiers) != 200) {
            throw new AssertionError("travel fares with gloves / without the hard Desert diary are wrong");
        }
        for (Diaries.Tier t : Diaries.Tier.values()) {
            Diaries.tasks("desert", t).forEach(task -> sp.diaryDone.add(task.id()));
        }
        if (DiaryRewards.travelFare("carpet_shantay", "Pollnivneach", 200, false, tiers) != 0) {
            throw new AssertionError("carpet rides still cost coins after the hard Desert diary");
        }
        return "claim, lamp, reclaim, proximity, charges, fares";
    }

    /** A taskmaster standing beside the player lets the claim through (and the NPCs exist for every area). */
    private static String taskmasters(TestContext c) {
        Set<String> areas = new java.util.HashSet<>();
        for (DiaryExtras.Taskmaster t : DiaryExtras.TASKMASTERS) {
            areas.add(t.area());
            if (ContentRegistry.npc(t.npcId()) == null) {
                throw new AssertionError("no NPC " + t.npcId() + " for the " + t.area() + " diary");
            }
        }
        if (areas.size() != Diaries.AREAS.size()) {
            throw new AssertionError("taskmasters for " + areas.size() + " areas");
        }
        NpcDef toby = ContentRegistry.npc(DiaryExtras.taskmaster("varrock"));
        com.runecraft.entity.QuestNpcEntity npc = com.runecraft.registry.ModEntities.QUEST_NPC.create(c.world);
        if (npc == null || toby == null) {
            throw new AssertionError("could not spawn Toby");
        }
        net.minecraft.core.BlockPos spot = c.near(2, 0);
        npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0f, 0f);
        npc.setNpc(toby);
        c.world.addFreshEntity(npc);
        SysState.SysPlayer sp = SysState.of(c.player);
        Diaries.tasks("varrock", Diaries.Tier.EASY).forEach(t -> sp.diaryDone.add(t.id()));
        DiaryObjectsService.taskmaster(c.player, DiaryExtras.TASKMASTERS.stream().filter(t -> t.area().equals("varrock")).findFirst()
                .orElseThrow());
        DiaryService.claimFromTaskmaster(c.player, Diaries.area("varrock"), Diaries.Tier.EASY);
        if (!sp.diaryClaimed.contains("varrock:EASY") || !Inv.has(c.player, "varrock_armour_1", 1)
                || !Inv.has(c.player, "antique_lamp_easy", 1)) {
            throw new AssertionError("Toby did not hand out the easy Varrock reward");
        }
        npc.discard();
        return "12 taskmasters, Toby's claim";
    }
}
