package com.runecraft.content.minigames;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.minigames.ShadesOfMortton;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.PyreLog;
import com.runecraft.osrs.content.minigames.ShadesOfMortton.Shade;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Shades of Mort'ton in town: sanctity (earned by repairing the Flamtaer temple and killing Loar shades,
 * -1% a minute, lost on logout), the sacred fire (olive oil to sacred oil, 0.9% sanctity a dose, at
 * least 10%), pyre logs (logs + sacred oil, 5 Firemaking XP a dose), the funeral pyre (cremating shade
 * remains for Firemaking and Prayer XP and a key or coins) and Razmire's two shops.
 */
public final class ShadesService {
    private static final Map<UUID, Double> SANCTITY = new HashMap<>();
    private static final Map<UUID, Double> RESOURCES = new HashMap<>();

    private ShadesService() {
    }

    static void register() {
        BlockHooks.register("funeral_pyre", (player, pos, state) -> pyre(player, pos));
        BlockHooks.register("flamtaer_altar", (player, pos, state) -> sanctify(player));
        BlockHooks.register("flamtaer_temple", (player, pos, state) -> repair(player));
        BlockHooks.register("razmire_general_store", (player, pos, state) -> buy(player, ShadesOfMortton.OLIVE_OIL_PRICE,
                "olive oil(4)", new String[]{"olive_oil"}, new int[]{1}));
        BlockHooks.register("razmire_builders_merchant", (player, pos, state) -> buy(player, ShadesOfMortton.REPAIR_SET_PRICE,
                "set of temple repair supplies", new String[]{"timber_beam", "limestone_brick", "swamp_paste"},
                new int[]{1, 1, ShadesOfMortton.PASTE_PER_SET}));
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp) || stack.isEmpty()) {
                return TypedActionResult.pass(stack);
            }
            String id = Inv.idOf(stack);
            if ("sacred_oil".equals(id)) {
                startPyreLogs(sp);
            } else if ("flamtaer_bag".equals(id)) {
                useBag(sp, stack);
            } else if (ShadesOfMortton.Coffin.byId(id) != null) {
                useCoffin(sp, stack, ShadesOfMortton.Coffin.byId(id));
            } else {
                return TypedActionResult.pass(stack);
            }
            return TypedActionResult.success(stack);
        });
        BlockHooks.register(com.runecraft.osrs.content.minigames.MinigameWorld.DAMPE_COFFINS, (player, pos, state) -> dampe(player));
        for (String afflicted : ShadesOfMortton.AFFLICTED) {
            com.runecraft.quest.QuestScripts.onTalk(afflicted, (player, npc) -> cure(player));
        }
        GameEvents.onKill((player, entity, stats) -> {
            if (Shade.LOAR.creature().equals(MinigameManager.creatureId(entity))) {
                addSanctity(player, ShadesOfMortton.SANCTITY_PER_KILL);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 1200 == 0) {
                SANCTITY.replaceAll((u, v) -> Math.max(0, v - ShadesOfMortton.SANCTITY_DRAIN_PER_MINUTE));
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SANCTITY.remove(handler.getPlayer().getUuid());
            RESOURCES.remove(handler.getPlayer().getUuid());
        });
    }

    static boolean questDone(ServerPlayerEntity player) {
        return ContentRegistry.quest(ShadesOfMortton.QUEST) == null || QuestApi.isComplete(player, ShadesOfMortton.QUEST);
    }

    private static boolean gate(ServerPlayerEntity player) {
        if (!questDone(player)) {
            Msg.game(player, "You need to complete the Shades of Mort'ton quest first.");
            return false;
        }
        return true;
    }

    public static double sanctity(ServerPlayerEntity p) {
        return SANCTITY.getOrDefault(p.getUuid(), 0.0);
    }

    public static void addSanctity(ServerPlayerEntity p, double amount) {
        SANCTITY.put(p.getUuid(), Math.max(0, Math.min(100, sanctity(p) + amount)));
    }

    private static String pct(double v) {
        return String.format("%.1f%%", v);
    }

    // ------------------------------------------------------------------ doses (potion-style: stack damage = doses used)

    private static int doses(ServerPlayerEntity p, String id) {
        int total = 0;
        for (ItemStack s : p.getInventory().main) {
            if (!s.isEmpty() && id.equals(Inv.idOf(s))) {
                total += Math.max(0, 4 - s.getDamage());
            }
        }
        return total;
    }

    private static void takeDoses(ServerPlayerEntity p, String id, int n) {
        var main = p.getInventory().main;
        for (int i = 0; i < main.size() && n > 0; i++) {
            ItemStack s = main.get(i);
            if (s.isEmpty() || !id.equals(Inv.idOf(s))) {
                continue;
            }
            int left = Math.max(0, 4 - s.getDamage());
            int use = Math.min(left, n);
            n -= use;
            if (use == left) {
                main.set(i, Inv.stack("vial", 1));
            } else {
                s.setDamage(s.getDamage() + use);
            }
        }
    }

    // ------------------------------------------------------------------ sacred fire

    private static void sanctify(ServerPlayerEntity player) {
        if (!gate(player)) {
            return;
        }
        if (sanctity(player) < ShadesOfMortton.ALTAR_MIN_SANCTITY) {
            Msg.game(player, "You need at least " + pct(ShadesOfMortton.ALTAR_MIN_SANCTITY) + " sanctity to use the sacred fire. "
                    + "Repair the temple or slay the shades around it. (Sanctity: " + pct(sanctity(player)) + ")");
            return;
        }
        var main = player.getInventory().main;
        int serums = sanctifySerums(player);
        int done = 0;
        for (int i = 0; i < main.size(); i++) {
            ItemStack s = main.get(i);
            if (s.isEmpty() || !"olive_oil".equals(Inv.idOf(s))) {
                continue;
            }
            int d = Math.max(0, 4 - s.getDamage());
            double cost = d * ShadesOfMortton.SANCTITY_PER_DOSE;
            if (sanctity(player) < cost) {
                break;
            }
            addSanctity(player, -cost);
            ItemStack oil = Inv.stack("sacred_oil", 1);
            oil.setDamage(s.getDamage());
            main.set(i, oil);
            done++;
        }
        if (done == 0) {
            if (serums == 0) {
                Msg.game(player, "You have no olive oil or serum 207 to sanctify.");
            }
            return;
        }
        CollectionLog.obtain(player, "sacred_oil");
        Msg.game(player, "You sanctify " + done + " vial" + (done == 1 ? "" : "s") + " of olive oil in the sacred fire. (Sanctity: "
                + pct(sanctity(player)) + ")");
    }

    /** Serum 207 into serum 208 at the sacred fire: at least 20% sanctity, 5% a dose. Returns the vials changed. */
    public static int sanctifySerums(ServerPlayerEntity player) {
        var main = player.getInventory().main;
        int done = 0;
        for (int i = 0; i < main.size(); i++) {
            ItemStack s = main.get(i);
            if (s.isEmpty() || !ShadesOfMortton.SERUM_207.equals(Inv.idOf(s))) {
                continue;
            }
            int d = Math.max(0, 4 - s.getDamage());
            double cost = d * ShadesOfMortton.SANCTITY_PER_SERUM_DOSE;
            if (sanctity(player) < ShadesOfMortton.SERUM_MIN_SANCTITY || sanctity(player) < cost) {
                Msg.game(player, "You need at least " + pct(Math.max(ShadesOfMortton.SERUM_MIN_SANCTITY, cost))
                        + " sanctity to turn that serum 207 into serum 208. (Sanctity: " + pct(sanctity(player)) + ")");
                break;
            }
            addSanctity(player, -cost);
            ItemStack serum = Inv.stack(ShadesOfMortton.SERUM_208, 1);
            serum.setDamage(s.getDamage());
            main.set(i, serum);
            done++;
        }
        if (done > 0) {
            CollectionLog.obtain(player, ShadesOfMortton.SERUM_208);
            Msg.game(player, "You sanctify " + done + " vial" + (done == 1 ? "" : "s") + " of serum 207 into serum 208. (Sanctity: "
                    + pct(sanctity(player)) + ")");
        }
        return done;
    }

    // ------------------------------------------------------------------ the afflicted

    /** Talking to an afflicted villager with serum: a dose of 208 (or else 207) cures them for a while, for a reward. */
    public static void cure(ServerPlayerEntity player) {
        String serum = doses(player, ShadesOfMortton.SERUM_208) > 0 ? ShadesOfMortton.SERUM_208
                : doses(player, ShadesOfMortton.SERUM_207) > 0 ? ShadesOfMortton.SERUM_207 : null;
        if (serum == null) {
            String[] nonsense = {"Hmmmm... mmmm... urrgh.", "Bbllrrgh! Mort'ton... mist... mmph!", "Gaahh... the shades... uurgh."};
            QuestApi.say(player, "Afflicted", nonsense[player.getRandom().nextInt(nonsense.length)]);
            return;
        }
        takeDoses(player, serum, 1);
        boolean better = serum.equals(ShadesOfMortton.SERUM_208);
        ShadesOfMortton.CureReward loot = ShadesOfMortton.cure(better ? ShadesOfMortton.CURE_208 : ShadesOfMortton.CURE_207,
                player.getRandom().nextDouble());
        int n = loot.min() + player.getRandom().nextInt(loot.max() - loot.min() + 1);
        giveLoot(player, loot, n);
        QuestApi.say(player, "Mort'ton local", "My head is clear again! Thank you, please take this.");
    }

    /** Gives a cure reward (olive oils with their dose count). */
    static void giveLoot(ServerPlayerEntity player, ShadesOfMortton.CureReward loot, int n) {
        if (loot.doses() > 0) {
            for (int i = 0; i < n; i++) {
                ItemStack oil = Inv.stack(loot.item(), 1);
                oil.setDamage(4 - loot.doses());
                Inv.give(player, oil);
            }
        } else {
            Inv.give(player, loot.item(), n);
            CollectionLog.obtain(player, loot.item());
        }
    }

    // ------------------------------------------------------------------ the flamtaer bag and Dampe's coffins

    /** Right-clicking the flamtaer bag: fills it with carried supplies, or empties it when there's nothing to put in. */
    public static void useBag(ServerPlayerEntity player, ItemStack bag) {
        var nbt = bag.getOrCreateNbt();
        int moved = 0;
        for (int i = 0; i < ShadesOfMortton.BAG_ITEMS.length; i++) {
            String item = ShadesOfMortton.BAG_ITEMS[i];
            int room = ShadesOfMortton.BAG_CAPACITY[i] - nbt.getInt("rc_bag_" + item);
            int n = Math.min(room, Inv.count(player, item));
            if (n > 0) {
                Inv.remove(player, item, n);
                nbt.putInt("rc_bag_" + item, nbt.getInt("rc_bag_" + item) + n);
                moved += n;
            }
        }
        if (moved == 0) {
            for (String item : ShadesOfMortton.BAG_ITEMS) {
                int n = nbt.getInt("rc_bag_" + item);
                if (n > 0) {
                    Inv.give(player, item, n);
                    nbt.putInt("rc_bag_" + item, 0);
                    moved += n;
                }
            }
            Msg.game(player, moved > 0 ? "You empty the flamtaer bag." : "The flamtaer bag holds timber beams, limestone bricks and swamp paste.");
            return;
        }
        Msg.game(player, "The bag holds " + nbt.getInt("rc_bag_timber_beam") + " timber beams, " + nbt.getInt("rc_bag_limestone_brick")
                + " limestone bricks and " + nbt.getInt("rc_bag_swamp_paste") + " swamp paste.");
    }

    /** Takes one repair set's worth of an item from a carried flamtaer bag; true if the bag had it. */
    private static boolean takeFromBag(ServerPlayerEntity p, String item, int n) {
        for (ItemStack s : p.getInventory().main) {
            if (!s.isEmpty() && "flamtaer_bag".equals(Inv.idOf(s)) && s.getNbt() != null && s.getNbt().getInt("rc_bag_" + item) >= n) {
                s.getNbt().putInt("rc_bag_" + item, s.getNbt().getInt("rc_bag_" + item) - n);
                return true;
            }
        }
        return false;
    }

    private static int inBag(ServerPlayerEntity p, String item) {
        for (ItemStack s : p.getInventory().main) {
            if (!s.isEmpty() && "flamtaer_bag".equals(Inv.idOf(s)) && s.getNbt() != null) {
                return s.getNbt().getInt("rc_bag_" + item);
            }
        }
        return 0;
    }

    /** Right-clicking a coffin: stores carried shade remains up to its capacity, or empties it. */
    public static void useCoffin(ServerPlayerEntity player, ItemStack coffin, ShadesOfMortton.Coffin tier) {
        var nbt = coffin.getOrCreateNbt();
        int stored = 0;
        for (Shade s : Shade.values()) {
            stored += nbt.getInt("rc_coffin_" + s.remains());
        }
        int moved = 0;
        for (Shade s : Shade.values()) {
            int n = Math.min(tier.capacity - stored, Inv.count(player, s.remains()));
            if (n > 0) {
                Inv.remove(player, s.remains(), n);
                nbt.putInt("rc_coffin_" + s.remains(), nbt.getInt("rc_coffin_" + s.remains()) + n);
                stored += n;
                moved += n;
            }
        }
        if (moved == 0) {
            for (Shade s : Shade.values()) {
                int n = nbt.getInt("rc_coffin_" + s.remains());
                if (n > 0) {
                    Inv.give(player, s.remains(), n);
                    nbt.putInt("rc_coffin_" + s.remains(), 0);
                    moved += n;
                }
            }
            Msg.game(player, moved > 0 ? "You take the remains out of the coffin." : "The coffin holds up to " + tier.capacity + " shade remains.");
            return;
        }
        Msg.game(player, "You put " + moved + " shade remains in the coffin (" + stored + " / " + tier.capacity + ").");
    }

    /** Dampe: hands out a broken coffin, and fits carried locks to a coffin for a bigger one (remains inside are kept). */
    public static void dampe(ServerPlayerEntity player) {
        var main = player.getInventory().main;
        int slot = -1;
        for (int i = 0; i < main.size(); i++) {
            String id = Inv.idOf(main.get(i));
            if (ShadesOfMortton.BROKEN_COFFIN.equals(id) || ShadesOfMortton.Coffin.byId(id) != null) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            Inv.give(player, ShadesOfMortton.BROKEN_COFFIN, 1);
            QuestApi.say(player, "Dampe", "Here, take this old coffin. Bring me locks from the catacomb chests and I'll fix it up so "
                    + "you can carry shade remains in it.");
            return;
        }
        java.util.Set<String> locks = new java.util.HashSet<>();
        for (ShadesOfMortton.Coffin c : ShadesOfMortton.Coffin.values()) {
            if (Inv.has(player, c.locks(), 1)) {
                locks.add(c.locks());
            }
        }
        ItemStack old = main.get(slot);
        ShadesOfMortton.Coffin to = ShadesOfMortton.upgrade(Inv.idOf(old), locks);
        if (to == null) {
            QuestApi.say(player, "Dampe", "Bring me bronze, steel, black, silver or gold locks better than the ones on that coffin.");
            return;
        }
        Inv.remove(player, to.locks(), 1);
        ItemStack coffin = Inv.stack(to.id(), 1);
        if (old.getNbt() != null) {
            coffin.setNbt(old.getNbt().copy());
        }
        main.set(slot, coffin);
        CollectionLog.obtain(player, to.id());
        QuestApi.say(player, "Dampe", "There you go: a " + to.metal + " coffin. It'll hold " + to.capacity + " shade remains.");
    }

    private static boolean diary(ServerPlayerEntity p, com.runecraft.osrs.content.systems.Diaries.Tier tier) {
        return com.runecraft.osrs.content.systems.Diaries.tierComplete(com.runecraft.content.systems.SysState.of(p).diaryDone, "morytania",
                tier);
    }

    // ------------------------------------------------------------------ temple repairs

    private static void repair(ServerPlayerEntity player) {
        if (!gate(player)) {
            return;
        }
        boolean flamtaer = Inv.has(player, "flamtaer_hammer", 1);
        if (!flamtaer && !Inv.has(player, "hammer", 1)) {
            Msg.game(player, "You need a hammer to repair the temple.");
            return;
        }
        if (!stock(player)) {
            Msg.game(player, "You need a timber beam (or plank), a limestone brick and " + ShadesOfMortton.PASTE_PER_SET
                    + " swamp paste to repair the temple. Razmire sells them.");
            return;
        }
        int every = flamtaer ? ShadesOfMortton.REPAIR_TICKS_FLAMTAER : ShadesOfMortton.REPAIR_TICKS;
        Msg.action(player, "You start repairing the temple walls.");
        ActionManager.start(player, (p, tick) -> {
            if (tick % every != 0) {
                return true;
            }
            if (!stock(p)) {
                Msg.game(p, "You have run out of building materials.");
                return false;
            }
            RESOURCES.put(p.getUuid(), RESOURCES.getOrDefault(p.getUuid(), 0.0) - ShadesOfMortton.RESOURCES_PER_REPAIR);
            // A worn flamtaer bracelet: about 75% more sanctity, one charge per repair.
            boolean bracelet = com.runecraft.content.systems.JewelleryService.useFlamtaerCharge(p);
            addSanctity(p, ShadesOfMortton.repairSanctity(bracelet));
            XpService.add(p, Skill.CRAFTING, ShadesOfMortton.REPAIR_CRAFTING_XP);
            Msg.action(p, "Resources: " + pct(RESOURCES.getOrDefault(p.getUuid(), 0.0)) + "  Sanctity: " + pct(sanctity(p)));
            return true;
        });
    }

    /** Makes sure at least 1% resources are on hand, using a set of materials if needed. */
    private static boolean stock(ServerPlayerEntity p) {
        if (RESOURCES.getOrDefault(p.getUuid(), 0.0) >= ShadesOfMortton.RESOURCES_PER_REPAIR) {
            return true;
        }
        // Supplies in a carried flamtaer bag are used without taking them out.
        String wood = Inv.has(p, "timber_beam", 1) || inBag(p, "timber_beam") >= 1 ? "timber_beam" : Inv.has(p, "plank", 1) ? "plank" : null;
        boolean brick = Inv.has(p, "limestone_brick", 1) || inBag(p, "limestone_brick") >= 1;
        boolean paste = Inv.has(p, "swamp_paste", ShadesOfMortton.PASTE_PER_SET) || inBag(p, "swamp_paste") >= ShadesOfMortton.PASTE_PER_SET;
        if (wood == null || !brick || !paste) {
            return false;
        }
        if (!takeFromBag(p, wood, 1)) {
            Inv.remove(p, wood, 1);
        }
        if (!takeFromBag(p, "limestone_brick", 1)) {
            Inv.remove(p, "limestone_brick", 1);
        }
        if (!takeFromBag(p, "swamp_paste", ShadesOfMortton.PASTE_PER_SET)) {
            Inv.remove(p, "swamp_paste", ShadesOfMortton.PASTE_PER_SET);
        }
        RESOURCES.put(p.getUuid(), RESOURCES.getOrDefault(p.getUuid(), 0.0) + ShadesOfMortton.RESOURCES_PER_SET);
        return true;
    }

    // ------------------------------------------------------------------ pyre logs

    /** The best pyre log the player has the logs and the sacred oil for. */
    private static PyreLog makeable(ServerPlayerEntity p) {
        int oil = doses(p, "sacred_oil");
        PyreLog best = null;
        for (PyreLog log : PyreLog.values()) {
            if (oil >= log.doses() && Inv.has(p, log.log(), 1)) {
                best = log;
            }
        }
        return best;
    }

    private static void startPyreLogs(ServerPlayerEntity player) {
        if (!questDone(player)) {
            Msg.game(player, "This sacred oil doesn't seem fresh. You should make your own.");
            return;
        }
        if (makeable(player) == null) {
            Msg.game(player, "You need logs and enough sacred oil (2 doses for normal and oak logs, 3 for willow to mahogany, "
                    + "4 for yew and better) to make pyre logs.");
            return;
        }
        ActionManager.start(player, (p, tick) -> {
            PyreLog log = makeable(p);
            if (log == null) {
                return false;
            }
            takeDoses(p, "sacred_oil", log.doses());
            Inv.remove(p, log.log(), 1);
            Inv.give(p, log.id(), 1);
            XpService.add(p, Skill.FIREMAKING, log.makeXp());
            Msg.action(p, "You coat the logs with sacred oil.");
            return true;
        });
    }

    // ------------------------------------------------------------------ funeral pyre

    private static void pyre(ServerPlayerEntity player, BlockPos pos) {
        if (!gate(player)) {
            return;
        }
        if (!Inv.has(player, "tinderbox", 1)) {
            Msg.game(player, "You need a tinderbox to light the funeral pyre.");
            return;
        }
        int fm = Players.data(player).skills().currentLevel(Skill.FIREMAKING);
        PyreLog[] logs = PyreLog.values();
        Shade[] shades = Shade.values();
        boolean anyRemains = false;
        boolean anyLogs = false;
        for (int l = logs.length - 1; l >= 0; l--) {
            PyreLog log = logs[l];
            if (!Inv.has(player, log.id(), 1)) {
                continue;
            }
            anyLogs = true;
            if (fm < log.level()) {
                continue;
            }
            for (int s = shades.length - 1; s >= 0; s--) {
                Shade shade = shades[s];
                if (!Inv.has(player, shade.remains(), 1)) {
                    continue;
                }
                anyRemains = true;
                if (log.burns(shade)) {
                    cremate(player, pos, log, shade);
                    return;
                }
            }
        }
        for (Shade shade : shades) {
            anyRemains |= Inv.has(player, shade.remains(), 1);
        }
        if (!anyLogs) {
            Msg.game(player, "You need pyre logs (logs coated with sacred oil) to build a funeral pyre.");
        } else if (!anyRemains) {
            Msg.game(player, "You have no shade remains to cremate.");
        } else {
            Msg.game(player, "Your pyre logs can't burn those remains, or you need a higher Firemaking level to light them.");
        }
    }

    private static void cremate(ServerPlayerEntity player, BlockPos pos, PyreLog log, Shade shade) {
        Inv.remove(player, log.id(), 1);
        Inv.remove(player, shade.remains(), 1);
        // Morytania diary rewards: +50% Prayer XP with the hard diary, +50% Firemaking XP with the elite diary.
        XpService.add(player, Skill.FIREMAKING, ShadesOfMortton.cremationFiremakingXp(log.firemakingXp(),
                diary(player, com.runecraft.osrs.content.systems.Diaries.Tier.ELITE)));
        XpService.add(player, Skill.PRAYER, ShadesOfMortton.cremationPrayerXp(log.prayerXp(shade),
                diary(player, com.runecraft.osrs.content.systems.Diaries.Tier.HARD)));
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 30, 0.3, 0.8, 0.3, 0.03);
        world.spawnParticles(ParticleTypes.SOUL, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 6, 0.2, 0.6, 0.2, 0.05);
        ShadesOfMortton.Cremation c = ShadesOfMortton.cremate(shade, player.getRandom()::nextDouble);
        if (c.key() != null) {
            Inv.give(player, c.key(), 1);
            CollectionLog.obtain(player, c.key());
            Msg.game(player, "The " + shade.displayName() + " shade's spirit rises from the pyre and leaves a key behind.");
        } else {
            Inv.give(player, Inv.COINS, c.coins());
            Msg.game(player, "The " + shade.displayName() + " shade's spirit rises from the pyre and leaves " + c.coins() + " coins.");
        }
    }

    // ------------------------------------------------------------------ Razmire

    private static void buy(ServerPlayerEntity player, int price, String what, String[] items, int[] counts) {
        if (!Inv.has(player, Inv.COINS, price)) {
            Msg.game(player, "Razmire: That'll be " + price + " coins for a " + what + ".");
            return;
        }
        Inv.remove(player, Inv.COINS, price);
        for (int i = 0; i < items.length; i++) {
            Inv.give(player, items[i], counts[i]);
        }
        Msg.game(player, "You buy a " + what + " for " + price + " coins.");
    }
}
