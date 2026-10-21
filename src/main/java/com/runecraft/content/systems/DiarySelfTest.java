package com.runecraft.content.systems;

import com.runecraft.combat.PrayerService;
import com.runecraft.content.GameEvents;
import com.runecraft.content.npcs.TravelService;
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
 * met) and the rewards (a claim with its Karamja lamp, taskmaster proximity, teleport charges, the travel fare perks); then the
 * activities behind the last stand-ins: chompy hunting up to Rantz's four hat tiers, a chosen kitten colour, a stray dog's bone, the
 * Desert Phoenix's feather, the white tree, the Altar of Nature and the Stronghold of Security's emotes.
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
        checks.add(new SelfTest.Check("minigame:diary_chompies", () -> chompies(c)));
        checks.add(new SelfTest.Check("minigame:diary_activities", () -> activities(c)));
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
                {"fairy_ring:cip", "Travel to Miscellania by Fairy ring"},
                {"talk:hans", "Learn your age from Hans in Lumbridge"},
                {"talk:npc_emblem_trader", "Talk to the Emblem Trader in Edgeville about emblems"}};
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
        entranaBoat(c);
        return "2 eagle flights, the train, " + trips.length + " trip keys, the Entrana boat refused armed and taken unarmed";
    }

    /**
     * The Entrana boat through the travel service: the Port Sarim monk refuses a player wielding a sword (no trip, no task),
     * then sails them over once nothing banned is carried. Banned stacks already in the inventory are set aside and put back.
     */
    private static void entranaBoat(TestContext c) {
        String monkId = "npc_monk_of_entrana_port_sarim";
        NpcDef monk = ContentRegistry.npc(monkId);
        com.runecraft.entity.QuestNpcEntity npc = com.runecraft.registry.ModEntities.QUEST_NPC.create(c.world);
        if (npc == null || monk == null) {
            throw new AssertionError("could not spawn the Port Sarim Monk of Entrana");
        }
        net.minecraft.world.entity.player.Inventory inv = c.player.getInventory();
        java.util.Map<Integer, net.minecraft.world.item.ItemStack> aside = new java.util.HashMap<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && com.runecraft.osrs.content.systems.AccessGates.entranaBanned(com.runecraft.registry.ModItems.def(s))) {
                aside.put(i, s.copy());
                inv.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
        Diaries.Task boat = task("Take the boat to Entrana");
        try {
            c.home();
            net.minecraft.core.BlockPos spot = c.near(2, 0);
            npc.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0f, 0f);
            npc.setNpc(monk);
            c.world.addFreshEntity(npc);
            wield(c, "bronze_sword");
            TravelService.travel(c.player, monkId, 0);
            int[] pos = SysTeleport.osrs(c.player);
            if (Teleports.within(pos[0], pos[1], 2834, 3331, 40)) {
                throw new AssertionError("the monk sailed a player wielding a bronze sword to Entrana");
            }
            expect(c, boat, false, "the monk refused a player with a sword");
            c.player.setItemInHand(InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            TravelService.travel(c.player, monkId, 0);
            pos = SysTeleport.osrs(c.player);
            if (!Teleports.within(pos[0], pos[1], 2834, 3331, 8)) {
                throw new AssertionError("the unarmed boat trip landed at " + pos[0] + "," + pos[1]);
            }
            expect(c, boat, true, "the boat trip to Entrana");
        } finally {
            npc.discard();
            c.home();
            aside.forEach(inv::setItem);
        }
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
        // The demonic ruins (a scripted block): praying there with points missing.
        Diaries.Task ruins = task("Restore some Prayer points at the demonic ruins");
        at(c, com.runecraft.osrs.content.systems.DiaryActivities.DEMONIC_RUINS_X,
                com.runecraft.osrs.content.systems.DiaryActivities.DEMONIC_RUINS_Y);
        d.setPrayerPoints(d.skills().level(Skill.PRAYER));
        com.runecraft.content.BlockHooks.use(c.player, com.runecraft.osrs.content.systems.DiaryActivities.DEMONIC_RUINS,
                c.player.blockPosition(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        expect(c, ruins, false, "praying at the ruins with full points");
        d.setPrayerPoints(1);
        com.runecraft.content.BlockHooks.use(c.player, com.runecraft.osrs.content.systems.DiaryActivities.DEMONIC_RUINS,
                c.player.blockPosition(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        expect(c, ruins, true, "restoring points at the ruins");
        c.home();
        return "4 altars";
    }

    /** Conditions: the Wolfbane dagger, a Prospector helmet, a complete Barrows set, each first missed and then met. */
    private static String conditions(TestContext c) {
        c.completeQuests(null);
        Diaries.Task werewolf = task("Kill a Werewolf in its human form using the Wolfbane Dagger");
        at(c, 3490, 3480);
        kill(c, "Werewolf");
        expect(c, werewolf, false, "a kill without the dagger");
        wield(c, "wolfbane");
        kill(c, "Werewolf");
        expect(c, werewolf, true, "a kill with Wolfbane");
        Diaries.Task guild = task("Enter the Mining Guild while wearing a Prospector helmet");
        at(c, 3016, 3339);
        DiaryService.check(c.player, Diaries.Type.VISIT);
        expect(c, guild, false, "entering without Prospector");
        c.player.setItemSlot(EquipmentSlot.CHEST, Inv.stack("prospector_jacket", 1));
        DiaryService.check(c.player, Diaries.Type.VISIT);
        expect(c, guild, false, "entering in a Prospector jacket only");
        c.player.setItemSlot(EquipmentSlot.CHEST, net.minecraft.world.item.ItemStack.EMPTY);
        c.player.setItemSlot(EquipmentSlot.HEAD, Inv.stack("prospector_helmet", 1));
        DiaryService.check(c.player, Diaries.Type.VISIT);
        expect(c, guild, true, "entering in a Prospector helmet");
        c.home();
        Diaries.Task barrows = task("Loot the Barrows chest while wearing any complete Barrows set");
        GameEvents.fireActivity(c.player, "chest_open:barrows_chest");
        expect(c, barrows, false, "a chest in a Prospector helmet");
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

    // ------------------------------------------------------------------ round 40 activities

    /** The first stack of an item in the player's inventory. */
    private static net.minecraft.world.item.ItemStack carried(TestContext c, String id) {
        for (int i = 0; i < c.player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack s = c.player.getInventory().getItem(i);
            if (!s.isEmpty() && Inv.idOf(s).equals(id)) {
                return s;
            }
        }
        throw new AssertionError("no " + id + " carried");
    }

    private static void setLevel(TestContext c, Skill skill, int level) {
        Players.data(c.player).skills().setXpTenths(skill, Experience.xpForLevel(level) * 10);
    }

    /** Bellows filled and a toad blown up by a Feldip pond, its chompy flying in and killed with an ogre bow, then the four hats. */
    private static String chompies(TestContext c) {
        c.completeQuests(null);
        SysState.SysPlayer sp = SysState.of(c.player);
        int[] pond = com.runecraft.osrs.content.systems.DiaryActivities.PONDS[0];
        at(c, pond[0], pond[1]);
        Inv.give(c.player, com.runecraft.osrs.content.systems.DiaryActivities.BELLOWS, 1);
        net.minecraft.world.item.ItemStack bellows = carried(c, com.runecraft.osrs.content.systems.DiaryActivities.BELLOWS);
        DiaryActivitiesService.useBellows(c.player, bellows);
        DiaryActivitiesService.useBellows(c.player, bellows);
        if (Inv.count(c.player, com.runecraft.osrs.content.systems.DiaryActivities.BLOATED_TOAD) != 1) {
            throw new AssertionError("filling the bellows and using them again did not make a bloated toad");
        }
        DiaryActivitiesService.putDownToad(c.player, carried(c, com.runecraft.osrs.content.systems.DiaryActivities.BLOATED_TOAD));
        if (Inv.count(c.player, com.runecraft.osrs.content.systems.DiaryActivities.BLOATED_TOAD) != 0) {
            throw new AssertionError("the toad was not put down");
        }
        List<com.runecraft.boss.DataBossEntity> birds = DiaryActivitiesService.chompiesArrive(c.player, c.world, c.player.blockPosition());
        if (birds.isEmpty()) {
            throw new AssertionError("no chompy came to the toad");
        }
        com.runecraft.boss.DataBossEntity bird = birds.get(0);
        com.runecraft.combat.MonsterStats stats = bird.stats();
        Inv.remove(c.player, "ogre_bow", Inv.count(c.player, "ogre_bow"));
        sp.diaryCounters.remove(com.runecraft.osrs.content.systems.DiaryActivities.CHOMPY_KILLS);
        GameEvents.fireKill(c.player, bird, stats);
        if (DiaryActivitiesService.chompyKills(c.player) != 0) {
            throw new AssertionError("a chompy killed without an ogre bow was registered");
        }
        Inv.give(c.player, "ogre_bow", 1);
        GameEvents.fireKill(c.player, bird, stats);
        if (DiaryActivitiesService.chompyKills(c.player) != 1) {
            throw new AssertionError("a chompy killed with an ogre bow was not registered");
        }
        birds.forEach(net.minecraft.world.entity.Entity::discard);
        // Rantz's hats: none below 30 kills; then the four tiers' tasks as the kills pass 30, 125, 300 and 1,000.
        String[] tasks = {"Claim any Chompy bird hat", "Claim a Chompy bird hat from Rantz after registering at least 125",
                "Claim a Chompy bird hat from Rantz after registering at least 300", "Claim a Chompy bird hat from Rantz after registering at least 1,000"};
        int[] kills = {30, 125, 300, 1000};
        int[] hats = {0, 5, 8, 12};
        sp.diaryCounters.put(com.runecraft.osrs.content.systems.DiaryActivities.CHOMPY_KILLS, 28);
        if (DiaryActivitiesService.claimHat(c.player, 0)) {
            throw new AssertionError("Rantz gave a hat for 28 kills");
        }
        for (int i = 0; i < tasks.length; i++) {
            sp.diaryCounters.put(com.runecraft.osrs.content.systems.DiaryActivities.CHOMPY_KILLS, kills[i] - 1);
            GameEvents.fireKill(c.player, bird, stats);
            expect(c, task(tasks[i]), false, (kills[i]) + " kills before a hat is claimed");
            if (!DiaryActivitiesService.claimHat(c.player, hats[i])
                    || !Inv.has(c.player, com.runecraft.osrs.content.systems.DiaryActivities.HATS.get(hats[i]).item(), 1)) {
                throw new AssertionError("Rantz did not hand out the hat for " + kills[i] + " kills");
            }
            expect(c, task(tasks[i]), true, "claiming a hat at " + kills[i] + " kills");
        }
        // The daily arrows follow the Western Provinces tiers (the easy tier: 25).
        Diaries.tasks("western", Diaries.Tier.EASY).forEach(t -> sp.diaryDone.add(t.id()));
        int before = Inv.count(c.player, "ogre_arrow");
        DiaryActivitiesService.rantzAction(c.player, "arrows");
        DiaryActivitiesService.rantzAction(c.player, "arrows");
        if (Inv.count(c.player, "ogre_arrow") - before != 25) {
            throw new AssertionError("Rantz's daily arrows after the easy diary: " + (Inv.count(c.player, "ogre_arrow") - before));
        }
        // The elite diary's perk: two chompies always come to a toad.
        for (Diaries.Tier t : Diaries.Tier.values()) {
            Diaries.tasks("western", t).forEach(task -> sp.diaryDone.add(task.id()));
        }
        List<com.runecraft.boss.DataBossEntity> pair = DiaryActivitiesService.chompiesArrive(c.player, c.world, c.player.blockPosition());
        pair.forEach(net.minecraft.world.entity.Entity::discard);
        if (pair.size() != 2) {
            throw new AssertionError(pair.size() + " chompies came to a toad after the elite diary, not 2");
        }
        c.home();
        return "1 hunted chompy, 4 hat tiers, daily arrows, the elite pair";
    }

    /** A kitten colour, a stray dog's bone, the phoenix's feather, the white tree, the Altar of Nature and the Stronghold emotes. */
    private static String activities(TestContext c) {
        c.completeQuests(null);
        PlayerData d = Players.data(c.player);
        // Gertrude: without the ring of charos the colour is random (no task); with it the player chooses.
        Diaries.Task kitten = task("Select a colour for your kitten");
        String kit = com.runecraft.osrs.content.systems.DiaryActivities.KITTEN;
        Inv.remove(c.player, kit, Inv.count(c.player, kit));
        Inv.remove(c.player, "ring_of_charos", Inv.count(c.player, "ring_of_charos"));
        Inv.give(c.player, Inv.COINS, 200);
        if (!DiaryActivitiesService.buyKitten(c.player, 1) || !Inv.has(c.player, kit, 1)) {
            throw new AssertionError("Gertrude sold no kitten");
        }
        expect(c, kitten, false, "a kitten without the ring of charos");
        Inv.remove(c.player, kit, 1);
        Inv.give(c.player, "ring_of_charos", 1);
        if (!DiaryActivitiesService.buyKitten(c.player, 1)) {
            throw new AssertionError("Gertrude sold no kitten to a charming player");
        }
        expect(c, kitten, true, "choosing a white kitten with the ring of charos");
        // Safta Doc: an opal machete for a gout tuber, three opals (cut and uncut mixed) and 300 trading sticks.
        Diaries.Task machete = task("Exchange gems for a machete");
        Inv.give(c.player, "gout_tuber", 1);
        Inv.give(c.player, "trading_sticks", 299);
        Inv.give(c.player, "uncut_opal", 2);
        Inv.give(c.player, "opal", 1);
        if (DiaryActivitiesService.buyMachete(c.player, 0)) {
            throw new AssertionError("Safta Doc made a machete for 299 trading sticks");
        }
        Inv.give(c.player, "trading_sticks", 1);
        if (!DiaryActivitiesService.buyMachete(c.player, 0) || !Inv.has(c.player, "opal_machete", 1)
                || Inv.count(c.player, "uncut_opal") + Inv.count(c.player, "opal") != 0 || Inv.has(c.player, "gout_tuber", 1)) {
            throw new AssertionError("Safta Doc's opal machete trade went wrong");
        }
        expect(c, machete, true, "trading gems for a machete");
        // Brimstail: a new crystal saw seed for a player with neither, then the seed sung into a saw.
        Diaries.Task saw = task("Turn your small crystal seed into a Crystal saw");
        String seed = com.runecraft.osrs.content.systems.DiaryActivities.CRYSTAL_SAW_SEED;
        String sawItem = com.runecraft.osrs.content.systems.DiaryActivities.CRYSTAL_SAW;
        Inv.remove(c.player, seed, Inv.count(c.player, seed));
        Inv.remove(c.player, sawItem, Inv.count(c.player, sawItem));
        DiaryActivitiesService.crystalSaw(c.player);
        if (!Inv.has(c.player, seed, 1)) {
            throw new AssertionError("Brimstail gave no crystal saw seed");
        }
        expect(c, saw, false, "getting the seed");
        DiaryActivitiesService.crystalSaw(c.player);
        if (!Inv.has(c.player, sawItem, 1) || Inv.has(c.player, seed, 1)) {
            throw new AssertionError("the seed did not become a crystal saw");
        }
        expect(c, saw, true, "singing the seed into a saw");
        // A swamp toad picked up in the Gnome Stronghold's swamp (its ground spawn), then its legs pulled off.
        Diaries.Task toad = task("Collect a swamp toad at the Gnome Stronghold");
        String toadItem = com.runecraft.osrs.content.systems.DiaryActivities.SWAMP_TOAD;
        Inv.remove(c.player, toadItem, Inv.count(c.player, toadItem));
        at(c, 3222, 3218);
        DiarySpawnsService.take(c.player, c.near(3, 0), DiaryExtras.SWAMP_TOAD);
        expect(c, toad, false, "a toad taken in Lumbridge");
        at(c, 2417, 3512);
        DiarySpawnsService.take(c.player, c.near(3, 0), DiaryExtras.SWAMP_TOAD);
        expect(c, toad, true, "a toad taken in the Stronghold's swamp");
        int legs = Inv.count(c.player, "toads_legs");
        DiaryActivitiesService.use(c.player, carried(c, toadItem), toadItem);
        if (Inv.count(c.player, "toads_legs") != legs + 1) {
            throw new AssertionError("pulling the toad's legs off gave no toad's legs");
        }
        // A stray dog: refused bones, then a bone.
        Diaries.Task dog = task("Give a dog a bone!");
        net.minecraft.world.entity.LivingEntity stray = com.runecraft.registry.ModEntities.MODULE_MONSTERS
                .get(com.runecraft.osrs.content.systems.DiaryActivities.STRAY_DOG).create(c.world);
        net.minecraft.world.entity.LivingEntity phoenix = com.runecraft.registry.ModEntities.MODULE_MONSTERS
                .get(com.runecraft.osrs.content.systems.DiaryActivities.PHOENIX).create(c.world);
        if (stray == null || phoenix == null) {
            throw new AssertionError("could not create a stray dog or the phoenix");
        }
        wield(c, "shaikahan_bones");
        DiaryActivitiesService.feedDog(c.player, stray);
        expect(c, dog, false, "offering Shaikahan bones");
        wield(c, "bones");
        if (!DiaryActivitiesService.feedDog(c.player, stray) || !c.player.getMainHandItem().isEmpty()) {
            throw new AssertionError("the dog did not eat the bone");
        }
        expect(c, dog, true, "feeding it a bone");
        // The Desert Phoenix: no feather below 25 Thieving; at 99 one is grabbed within a few tries (stuns cleared).
        Diaries.Task feather = task("Steal a feather from the Desert Phoenix");
        String f = com.runecraft.osrs.content.systems.DiaryActivities.PHOENIX_FEATHER;
        Inv.remove(c.player, f, Inv.count(c.player, f));
        at(c, com.runecraft.osrs.content.systems.DiaryActivities.PHOENIX_X, com.runecraft.osrs.content.systems.DiaryActivities.PHOENIX_Y);
        setLevel(c, Skill.THIEVING, 20);
        DiaryActivitiesService.grabFeather(c.player, phoenix);
        expect(c, feather, false, "a grab at 20 Thieving");
        setLevel(c, Skill.THIEVING, 99);
        for (int i = 0; i < 30 && !Inv.has(c.player, f, 1); i++) {
            d.stunnedUntil = 0;
            c.heal();
            DiaryActivitiesService.grabFeather(c.player, phoenix);
        }
        if (!Inv.has(c.player, f, 1)) {
            throw new AssertionError("no phoenix feather in 30 grabs at 99 Thieving");
        }
        expect(c, feather, true, "grabbing a feather");
        d.stunnedUntil = 0;
        // The white tree: 25 Farming, four fruit then none until one regrows.
        Diaries.Task fruit = task("Pick a White tree fruit");
        at(c, com.runecraft.osrs.content.systems.DiaryActivities.WHITE_TREE_X, com.runecraft.osrs.content.systems.DiaryActivities.WHITE_TREE_Y);
        setLevel(c, Skill.FARMING, 24);
        if (DiaryActivitiesService.pickWhiteTree(c.player)) {
            throw new AssertionError("picked white tree fruit at 24 Farming");
        }
        setLevel(c, Skill.FARMING, 25);
        int picked = 0;
        while (picked < 6 && DiaryActivitiesService.pickWhiteTree(c.player)) {
            picked++;
        }
        if (picked != 4) {
            throw new AssertionError("picked " + picked + " fruit from a full white tree, not 4");
        }
        expect(c, fruit, true, "picking a fruit");
        // The Altar of Nature: restoring points (easy), then praying with Piety at 70 Prayer and Defence (hard).
        Diaries.Task restore = task("Restore your prayer points at the nature altar");
        Diaries.Task piety = task("Pray at the Altar of Nature with Piety activated");
        at(c, com.runecraft.osrs.content.systems.DiaryActivities.NATURE_ALTAR_X, com.runecraft.osrs.content.systems.DiaryActivities.NATURE_ALTAR_Y);
        setLevel(c, Skill.PRAYER, 70);
        setLevel(c, Skill.DEFENCE, 70);
        d.activePrayers().clear();
        d.setPrayerPoints(70);
        DiaryActivitiesService.natureAltar(c.player, null);
        expect(c, restore, false, "praying with full points");
        expect(c, piety, false, "praying without Piety");
        d.setPrayerPoints(10);
        DiaryActivitiesService.natureAltar(c.player, null);
        expect(c, restore, true, "restoring points at the altar");
        d.activePrayers().add(Prayer.PIETY);
        DiaryActivitiesService.natureAltar(c.player, null);
        expect(c, piety, true, "praying there with Piety on");
        d.activePrayers().clear();
        // The Stronghold's emotes: locked until each reward chest is opened, then all four performed.
        Diaries.Task emotes = task("Perform the 4 emotes from the Stronghold of Security");
        List<String> four = com.runecraft.osrs.content.systems.DiaryActivities.STRONGHOLD_EMOTES;
        int chests = com.runecraft.content.dungeons.DungeonsHooks.securityChests(c.player);
        com.runecraft.content.dungeons.DungeonsHooks.setSecurityChests(c.player, 0);
        try {
            for (int i = 0; i < four.size(); i++) {
                if (DiaryActivitiesService.perform(c.player, four.get(i))) {
                    throw new AssertionError(four.get(i) + " was performed before its chest was opened");
                }
                com.runecraft.content.BlockHooks.use(c.player, com.runecraft.osrs.content.dungeons.DungeonBlocks.SOS_CHESTS.get(i),
                        c.player.blockPosition(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            for (int i = 0; i < 4; i++) {
                DiaryActivitiesService.perform(c.player, four.get(0)); // the same emote four times doesn't count (wiki)
            }
            expect(c, emotes, false, "performing one emote four times");
            for (String e : four) {
                if (!DiaryActivitiesService.perform(c.player, e)) {
                    throw new AssertionError(e + " is still locked after its chest");
                }
            }
            expect(c, emotes, true, "performing all four");
        } finally {
            com.runecraft.content.dungeons.DungeonsHooks.setSecurityChests(c.player, chests);
        }
        c.home();
        // Temple Knight armour from Sir Tiffy: full Initiate at the Taverley stone circle, full Proselyte at the Rimmington church altar.
        Inv.give(c.player, Inv.COINS, 60000);
        for (String piece : new String[]{"initiate_sallet", "initiate_hauberk", "initiate_cuisse", "proselyte_sallet",
                "proselyte_hauberk", "proselyte_tasset"}) {
            if (!DiaryActivitiesService.buyTempleArmour(c.player, piece) || !Inv.has(c.player, piece, 1)) {
                throw new AssertionError("Sir Tiffy sold no " + piece);
            }
        }
        Diaries.Task initiate = task("Pray at the Altar of Guthix in Taverley whilst wearing full Initiate");
        Diaries.Task proselyte = task("Recharge your prayer in the Port Sarim church while wearing full Proselyte");
        at(c, 2925, 3483);
        PrayerService.useAltar(c.player, null);
        expect(c, initiate, false, "praying at the stone circle without the armour");
        c.player.setItemSlot(EquipmentSlot.HEAD, carried(c, "initiate_sallet").split(1));
        c.player.setItemSlot(EquipmentSlot.CHEST, carried(c, "initiate_hauberk").split(1));
        c.player.setItemSlot(EquipmentSlot.LEGS, carried(c, "initiate_cuisse").split(1));
        PrayerService.useAltar(c.player, null);
        expect(c, initiate, true, "praying there in full Initiate");
        at(c, com.runecraft.osrs.content.systems.DiaryActivities.SARIM_CHURCH_X,
                com.runecraft.osrs.content.systems.DiaryActivities.SARIM_CHURCH_Y);
        com.runecraft.content.BlockHooks.use(c.player, com.runecraft.osrs.content.systems.DiaryActivities.SARIM_CHURCH_ALTAR,
                c.player.blockPosition(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        expect(c, proselyte, false, "praying in the church in Initiate armour");
        c.player.setItemSlot(EquipmentSlot.HEAD, carried(c, "proselyte_sallet").split(1));
        c.player.setItemSlot(EquipmentSlot.CHEST, carried(c, "proselyte_hauberk").split(1));
        c.player.setItemSlot(EquipmentSlot.LEGS, carried(c, "proselyte_tasset").split(1));
        com.runecraft.content.BlockHooks.use(c.player, com.runecraft.osrs.content.systems.DiaryActivities.SARIM_CHURCH_ALTAR,
                c.player.blockPosition(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        expect(c, proselyte, true, "praying in the church in full Proselyte (tasset)");
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS}) {
            c.player.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
        }
        // The Arceuus lectern: an Ape Atoll teleport tablet on the Arceuus book (90 Magic, 2 blood/law/soul, a dark essence block).
        Diaries.Task apeTablet = task("Create an Ape Atoll teleport tablet");
        SysState.SysPlayer spl = SysState.of(c.player);
        spl.book = com.runecraft.osrs.content.systems.Spellbooks.Book.ARCEUUS;
        setLevel(c, Skill.MAGIC, 90);
        Inv.give(c.player, "blood_rune", 2);
        Inv.give(c.player, "law_rune", 2);
        Inv.give(c.player, "soul_rune", 2);
        Inv.remove(c.player, "dark_essence_block", Inv.count(c.player, "dark_essence_block"));
        if (DiaryActivitiesService.makeTablet(c.player, "ape_atoll_teleport_tablet")) {
            throw new AssertionError("a tablet was made without a dark essence block");
        }
        Inv.give(c.player, "dark_essence_block", 1);
        if (!DiaryActivitiesService.makeTablet(c.player, "ape_atoll_teleport_tablet")
                || !Inv.has(c.player, "ape_atoll_teleport_tablet", 1) || Inv.has(c.player, "dark_essence_block", 1)) {
            throw new AssertionError("the lectern made no Ape Atoll tablet");
        }
        expect(c, apeTablet, true, "making the tablet at the lectern");
        spl.book = com.runecraft.osrs.content.systems.Spellbooks.Book.STANDARD;
        // Shops and services: a stew bought at the Forester's Arms, Thessalia's store opened, a plank from the sawmill operator.
        Inv.give(c.player, Inv.COINS, 500);
        at(c, 2691, 3493);
        com.runecraft.content.npcs.ShopService.open(c.player, "foresters_arms", c.player.position());
        com.runecraft.content.npcs.ShopService.selfTestBuy(c.player, "foresters_arms", "stew", 1);
        expect(c, task("Buy a stew from the Seers' Village pub"), true, "buying a stew at the Forester's Arms");
        at(c, 3206, 3417);
        com.runecraft.content.npcs.ShopService.open(c.player, "thessalias_fine_clothes", c.player.position());
        expect(c, task("Browse Thessalia's store"), true, "opening Thessalia's store");
        Diaries.Task plank = task("Make a normal plank at the Sawmill");
        at(c, DiaryExtras.SAWMILL_X, DiaryExtras.SAWMILL_Y);
        Inv.give(c.player, "logs", 1);
        GameEvents.fireActivity(c.player, "talk:" + DiaryExtras.SAWMILL_OPERATOR);
        if (!Inv.has(c.player, "plank", 1)) {
            throw new AssertionError("the sawmill operator made no plank");
        }
        expect(c, plank, true, "a plank from the sawmill");
        c.home();
        return "kitten, machete, crystal saw, swamp toad, dog, phoenix, white tree, nature altar, 4 emotes, shops, sawmill";
    }
}
