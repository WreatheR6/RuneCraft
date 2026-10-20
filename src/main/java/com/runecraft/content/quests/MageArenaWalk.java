package com.runecraft.content.quests;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.Aggression;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.systems.SpellService;
import com.runecraft.content.systems.SysState;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Experience;
import com.runecraft.osrs.GielinorMap;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.MageArena;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.world.ArenaLayout;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.quest.QuestScripts;
import com.runecraft.registry.ModEntities;
import com.runecraft.util.Inv;
import com.runecraft.world.Gielinor;
import com.runecraft.world.StructurePlacer;
import java.util.List;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/**
 * Self-test drivers for the Mage Arena miniquests ({@code minigame:mage_arena_i}, {@code minigame:mage_arena_ii}), walked as
 * the real player through the real NPC talk handlers, the arena's summoning stone, the spellbook's cast path and the
 * enchanted symbol: Kolodion's five forms (only magic hurts them), a statue's cape and the chamber guardian's staff, the god
 * spells refused outside the arena until cast 100 times on its battle mages (who leave their own god's cape-wearers alone);
 * then the symbol (not outside the Wilderness, hot and cold, up to 16 damage, the follower appearing at its spot), each
 * follower only hurt by its own god spell, the remains handed to Kolodion and a cape imbued (and another after the quest).
 * Every broken step throws an {@link AssertionError} naming it; quests, flags, skills and items are put back after.
 */
public final class MageArenaWalk {
    private final ServerPlayer player;
    private final ServerLevel world;
    private final StringBuilder log = new StringBuilder();

    private static final net.minecraft.resources.ResourceLocation HP_BUFFER = com.runecraft.RuneCraft.id("selftest_ma_hp");
    private static final String[] QUESTS = {"mage_arena_i", "mage_arena_ii"};
    private static final String[] GEAR = {"saradomin_cape", "guthix_cape", "zamorak_cape", "saradomin_staff", "guthix_staff", "zamorak_staff",
            "imbued_saradomin_cape", "imbued_guthix_cape", "imbued_zamorak_cape", MageArena.SYMBOL, "justiciars_hand", "ents_roots",
            "demons_heart", "blood_rune", "fire_rune", "air_rune", "bones", "looting_bag"};

    private MageArenaWalk(ServerPlayer player) {
        this.player = player;
        this.world = Gielinor.world(player.getServer());
    }

    public static String ma1(ServerPlayer player, BlockPos back) {
        return run(player, back, false);
    }

    public static String ma2(ServerPlayer player, BlockPos back) {
        return run(player, back, true);
    }

    private static String run(ServerPlayer player, BlockPos back, boolean second) {
        PlayerData data = Players.data(player);
        QuestsState state = QuestsState.get(player);
        Object flags = state.selfTestSave(player.getUUID());
        int[] xp = data.skills().rawXp();
        int[] boosts = data.skills().rawBoosts();
        int[] stages = new int[QUESTS.length];
        for (int i = 0; i < QUESTS.length; i++) {
            QuestInfo q = QuestApi.quest(QUESTS[i]);
            stages[i] = q == null ? 0 : data.questStage(q);
        }
        int[] gear = new int[GEAR.length];
        for (int i = 0; i < GEAR.length; i++) {
            gear[i] = Inv.count(player, GEAR[i]);
        }
        SysState.SysPlayer sp = SysState.of(player);
        ItemStack cape = sp.worn(Jewellery.Slot.CAPE).copy();
        ItemStack mainHand = player.getMainHandItem().copy();
        String autocast = sp.autocast;
        Spellbooks.Book book = sp.book;
        try {
            for (Skill s : new Skill[]{Skill.MAGIC, Skill.HITPOINTS, Skill.DEFENCE, Skill.PRAYER}) {
                data.skills().setXpTenths(s, Math.max(data.skills().xpTenths(s), Experience.xpForLevel(90) * 10));
            }
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.removeModifier(HP_BUFFER);
                health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
            }
            player.setHealth(player.getMaxHealth());
            sp.book = Spellbooks.Book.STANDARD;
            sp.swapBook = null;
            MageArenaWalk w = new MageArenaWalk(player);
            try {
                if (second) {
                    w.secondArena(back);
                } else {
                    w.firstArena();
                }
                return w.log.toString();
            } catch (AssertionError e) {
                throw new AssertionError(e.getMessage() + " (done: " + w.log + ")");
            }
        } finally {
            AttributeInstance health = player.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) {
                health.removeModifier(HP_BUFFER);
                player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
            }
            for (DataBossEntity left : nearBosses(player)) {
                left.discard();
            }
            state.selfTestRestore(player.getUUID(), flags);
            data.skills().load(xp, boosts);
            for (int i = 0; i < QUESTS.length; i++) {
                QuestInfo q = QuestApi.quest(QUESTS[i]);
                if (q != null) {
                    data.setQuestStage(q, stages[i]);
                }
            }
            sp.worn.put(Jewellery.Slot.CAPE, cape);
            sp.autocast = autocast;
            sp.book = book;
            player.setItemInHand(InteractionHand.MAIN_HAND, mainHand);
            for (int i = 0; i < GEAR.length; i++) {
                int extra = Inv.count(player, GEAR[i]) - gear[i];
                if (extra > 0) {
                    Inv.remove(player, GEAR[i], extra);
                }
            }
            com.runecraft.network.ServerNetworking.sync(player);
            if (back != null && player.isAlive()) {
                player.teleportTo(back.getX() + 0.5, back.getY(), back.getZ() + 0.5);
            }
        }
    }

    private static List<DataBossEntity> nearBosses(ServerPlayer p) {
        return p.serverLevel().getEntitiesOfClass(DataBossEntity.class, p.getBoundingBox().inflate(96), e -> e.isAlive()
                && e.def() != null && (MageArena.formIndex(e.def().id) >= 0 || MageArena.follower(e.def().id) != null));
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }

    private void note(String s) {
        log.append(log.length() == 0 ? "" : "; ").append(s);
    }

    private void heal() {
        player.setHealth(player.getMaxHealth());
        Players.data(player).setPrayerPoints(99);
    }

    private int stage(String quest) {
        return QuestApi.stage(player, quest);
    }

    private void setStage(String quest, int stage) {
        QuestInfo q = QuestApi.quest(quest);
        expect(q != null, "no quest " + quest);
        Players.data(player).setQuestStage(q, stage);
    }

    private boolean done(String quest) {
        return QuestApi.isComplete(player, quest);
    }

    /** Talks to an NPC through its registered talk handler (quest dialogue and the Mage Arena's own lines). */
    private void talk(String npc) {
        BiConsumer<ServerPlayer, QuestNpcEntity> h = QuestScripts.handler(npc);
        expect(h != null, "nobody answers at " + npc);
        h.accept(player, null);
    }

    private void tick(LivingEntity boss) {
        for (int m = 0; m < ActionManager.MC_TICKS_PER_GAME_TICK; m++) {
            ActionManager.advanceForSelfTest();
            if (boss.isAlive()) {
                if (boss instanceof Mob mob && mob.getTarget() != player) {
                    mob.setTarget(player);
                }
                boss.tick();
            }
            Mechanics.tickForSelfTest();
            heal();
        }
    }

    private DataBossEntity find(String bossId) {
        EntityType<DataBossEntity> type = ModEntities.DATA_BOSS_TYPES.get(bossId);
        expect(type != null, "no entity type for " + bossId);
        List<DataBossEntity> found = world.getEntities(type, player.getBoundingBox().inflate(80), Entity::isAlive);
        return found.isEmpty() ? null : found.get(0);
    }

    /** A hit through the creature's own damage rules for the style (immunities, scripts), without the accuracy roll. */
    private int hit(LivingEntity e, int damage, AttackType type) {
        int d = e instanceof OsrsMonsterEntity m ? m.modifyIncoming(player, damage, type) : damage;
        return d > 0 ? CombatHandler.applyDamage(player, e, d) : 0;
    }

    /** One real cast of the autocast spell at a target through the spellbook (the attack timer skipped). */
    private void cast(LivingEntity target) {
        Players.data(player).lastAttackTick = -1_000_000;
        Inv.give(player, "blood_rune", 4);
        Inv.give(player, "fire_rune", 8);
        Inv.give(player, "air_rune", 8);
        expect(SpellService.retaliate(player, target), "no autocast spell to cast");
    }

    private void wield(String item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack(item, 1));
    }

    // ------------------------------------------------------------------ Mage Arena I

    private void firstArena() {
        expect(world != null, "no Gielinor");
        setStage("mage_arena_i", 0);
        setStage("mage_arena_ii", 0);
        QuestsState state = QuestsState.get(player);
        state.selfTestRestore(player.getUUID(), null);
        talk(MageArena.KOLODION_NPC);
        expect(stage("mage_arena_i") == 1, "talking to Kolodion didn't start Mage Arena I (stage " + stage("mage_arena_i") + ")");

        // The arena's stone raises Kolodion: the form the player left off at (the first, his human form).
        ArenaLayout.Arena arena = ArenaLayout.of(MageArena.KOLODION);
        expect(arena != null, "Kolodion has no arena");
        BlockPos stone = Gielinor.safeSpot(world, arena.x(), arena.z());
        player.teleportTo(world, stone.getX() + 2.5, stone.getY(), stone.getZ() + 0.5, 0, 0);
        BossDef demon = ContentRegistry.boss(MageArena.KOLODION);
        DataBossService.summon(player, world, stone, demon);
        DataBossEntity raised = find(MageArena.KOLODION);
        expect(raised != null, "the summoning stone raised no Kolodion");
        for (int i = 0; i < 4 && find(MageArena.KOLODION_FORMS.get(0).id()) == null; i++) {
            tick(raised);
        }
        StringBuilder forms = new StringBuilder();
        for (MageArena.Form f : MageArena.KOLODION_FORMS) {
            DataBossEntity form = find(f.id());
            expect(form != null, "Kolodion didn't take his " + f.model() + " form (" + f.id() + ")");
            expect(hit(form, 5, AttackType.SLASH) == 0 && hit(form, 5, AttackType.RANGED) == 0,
                    f.id() + " was hurt by melee or ranged: only magic harms Kolodion");
            for (int i = 0; i < 60 && form.isAlive(); i++) {
                hit(form, Math.max(10, form.trueHitpoints() / 3), AttackType.MAGIC);
                tick(form);
            }
            expect(!form.isAlive(), f.id() + " didn't die to magic");
            forms.append(forms.length() == 0 ? "" : " > ").append(f.model());
        }
        expect(stage("mage_arena_i") == 2, "killing Kolodion's demon form didn't move the miniquest on (stage " + stage("mage_arena_i") + ")");
        note("Kolodion: " + forms + " (only magic hurt them)");

        // The statue's cape completes it; the chamber guardian hands over the staff of the god prayed to.
        int capes = Inv.count(player, "saradomin_cape");
        talk("quest_statue_saradomin");
        expect(done("mage_arena_i"), "praying at the statue of Saradomin didn't complete Mage Arena I");
        expect(Inv.count(player, "saradomin_cape") == capes + 1, "the statue gave no Saradomin cape");
        int staves = Inv.count(player, "saradomin_staff");
        talk("npc_chamber_guardian");
        expect(Inv.count(player, "saradomin_staff") == staves + 1, "the chamber guardian didn't hand over the Saradomin staff");
        talk("quest_statue_saradomin");
        expect(Inv.count(player, "saradomin_cape") == capes + 2, "praying again gave no second cape");
        note("statue cape, guardian's staff, a second cape");

        // God spells: refused outside the arena until cast 100 times on its battle mages.
        String spell = "saradomin_strike";
        wield("saradomin_staff");
        SpellService.setAutocast(player, spell);
        BlockPos outside = Gielinor.safeSpot(world, GielinorMap.toBlockX(3105), GielinorMap.toBlockZ(3900));
        player.teleportTo(world, outside.getX() + 0.5, outside.getY(), outside.getZ() + 0.5, 0, 0);
        expect(MageArenaService.castRule(player, spell) != null, "Saradomin Strike was castable outside the arena before 100 casts");
        MageArena.Spawn mageSpot = MageArena.BATTLE_MAGES.get(0);
        int mx = GielinorMap.toBlockX(mageSpot.x());
        int mz = GielinorMap.toBlockZ(mageSpot.y());
        StructurePlacer.applyAround(world, mx, mz, 24);
        BlockPos in = Gielinor.safeSpot(world, mx + 4, mz);
        player.teleportTo(world, in.getX() + 0.5, in.getY(), in.getZ() + 0.5, 0, 0);
        expect(MageArenaService.inArena(player), "the battle mages' tile isn't inside the arena");
        expect(MageArenaService.castRule(player, spell) == null, "Saradomin Strike was refused inside the arena");
        Mob mage = spawn(mageSpot.creature(), in);
        Mob other = spawn(MageArena.battleMage("zamorak"), in);
        SysState.of(player).worn.put(Jewellery.Slot.CAPE, Inv.stack("saradomin_cape", 1));
        expect(Aggression.peaceful(mage, player), "a Saradomin battle mage attacked a player in a Saradomin cape");
        expect(!Aggression.peaceful(other, player), "a Zamorak battle mage left a Saradomin cape-wearer alone");
        other.discard();
        int casts = 0;
        while (!MageArenaService.flag(player, MageArena.chargedFlag("saradomin")) && casts < 150) {
            mage.setHealth(mage.getMaxHealth());
            cast(mage);
            casts++;
        }
        expect(MageArenaService.flag(player, MageArena.chargedFlag("saradomin")), "100 casts of Saradomin Strike on a battle mage didn't charge it");
        expect(casts == MageArena.CASTS_TO_CHARGE, "it took " + casts + " casts to charge Saradomin Strike, not 100");
        mage.discard();
        player.teleportTo(world, outside.getX() + 0.5, outside.getY(), outside.getZ() + 0.5, 0, 0);
        expect(MageArenaService.castRule(player, spell) == null, "a charged Saradomin Strike was still refused outside the arena");
        note("Saradomin Strike charged by " + casts + " casts on a battle mage");
    }

    private Mob spawn(String creature, BlockPos at) {
        EntityType<?> type = ModEntities.typeById(creature);
        expect(type != null, "no entity for " + creature);
        Entity e = type.create(world);
        expect(e instanceof Mob, creature + " is not a mob");
        Mob mob = (Mob) e;
        mob.moveTo(at.getX() + 2.5, at.getY(), at.getZ() + 0.5, 0, 0);
        world.addFreshEntity(mob);
        return mob;
    }

    // ------------------------------------------------------------------ Mage Arena II

    private void secondArena(BlockPos back) {
        expect(world != null, "no Gielinor");
        QuestsState state = QuestsState.get(player);
        state.selfTestRestore(player.getUUID(), null);
        QuestInfo ma1 = QuestApi.quest("mage_arena_i");
        expect(ma1 != null, "no Mage Arena I");
        Players.data(player).setQuestStage(ma1, ma1.finalStage());
        setStage("mage_arena_ii", 0);
        // Not before the three god spells are mastered.
        talk(MageArena.KOLODION_NPC);
        expect(stage("mage_arena_ii") == 0, "Mage Arena II started without the god spells cast 100 times");
        for (String god : MageArena.GODS) {
            state.setFlag(player.getUUID(), MageArena.chargedFlag(god));
        }
        talk(MageArena.KOLODION_NPC);
        expect(stage("mage_arena_ii") == 1, "Kolodion didn't start Mage Arena II (stage " + stage("mage_arena_ii") + ")");
        expect(Inv.has(player, MageArena.SYMBOL, 1), "Kolodion gave no enchanted symbol");
        note("started with the spells mastered; symbol");

        // The symbol does nothing outside the (surface) Wilderness.
        if (back != null) {
            player.teleportTo(world, back.getX() + 0.5, back.getY(), back.getZ() + 0.5, 0, 0);
        }
        if (Gielinor.wildernessLevel(player) <= 0) {
            float hp = player.getHealth();
            useSymbol(false);
            expect(player.getHealth() == hp, "the symbol hurt the player outside the Wilderness");
        }
        String[] staves = {"saradomin_staff", "guthix_staff", "zamorak_staff"};
        for (int f = 0; f < MageArena.FOLLOWERS.size(); f++) {
            MageArena.Follower follower = MageArena.FOLLOWERS.get(f);
            expect(MageArenaService.attuned(player) == f, "the symbol is attuned to follower " + MageArenaService.attuned(player)
                    + ", expected " + follower.name());
            int[] spot = MageArenaService.spot(player, f);
            // Far from the spot: a reading, no follower; the use hurts up to 16.
            BlockPos far = Gielinor.safeSpot(world, GielinorMap.toBlockX(spot[0] + 40), GielinorMap.toBlockZ(spot[1]));
            player.teleportTo(world, far.getX() + 0.5, far.getY(), far.getZ() + 0.5, 0, 0);
            if (Gielinor.wildernessLevel(player) > 0) {
                heal();
                float hp = player.getHealth();
                useSymbol(false);
                expect(hp - player.getHealth() <= MageArena.SYMBOL_MAX_DAMAGE + 0.01f, "the symbol hit for " + (hp - player.getHealth()));
                expect(find(follower.id()) == null, follower.name() + " appeared 40 tiles from the spot");
            }
            // At the spot: it shakes and the follower appears.
            StructurePlacer.applyAround(world, GielinorMap.toBlockX(spot[0]), GielinorMap.toBlockZ(spot[1]), 24);
            BlockPos at = Gielinor.safeSpot(world, GielinorMap.toBlockX(spot[0]), GielinorMap.toBlockZ(spot[1]));
            player.teleportTo(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            heal();
            useSymbol(false);
            DataBossEntity boss = find(follower.id());
            expect(boss != null, follower.name() + " didn't appear at the symbol's spot " + spot[0] + "," + spot[1]);
            // Only its own god spell harms it.
            expect(hit(boss, 50, AttackType.MAGIC) == 0, follower.name() + " was hurt by magic that wasn't " + follower.spell());
            expect(hit(boss, 50, AttackType.SLASH) == 0, follower.name() + " was hurt by melee");
            String wrong = MageArena.GOD_SPELLS[(f + 1) % 3];
            wield(staves[(f + 1) % 3]);
            SpellService.setAutocast(player, wrong);
            float before = boss.getHealth();
            for (int i = 0; i < 5; i++) {
                cast(boss);
            }
            expect(boss.getHealth() == before, follower.name() + " was hurt by " + wrong);
            wield(staves[f]);
            SpellService.setAutocast(player, follower.spell());
            boss.setHealth(Math.max(1f, boss.getMaxHealth() * 0.08f)); // most of the fight skipped: the casts finish it
            int casts = 0;
            while (boss.isAlive() && casts < 120) {
                cast(boss);
                if (casts % 4 == 0) {
                    tick(boss);
                }
                heal();
                casts++;
            }
            expect(!boss.isAlive(), follower.name() + " survived " + casts + " casts of " + follower.spell());
            expect(pickUp(boss, follower.remains()), follower.name() + " dropped no " + follower.remains());
            // Kolodion keeps the remains.
            talk(MageArena.KOLODION_NPC);
            expect(MageArenaService.flag(player, MageArena.handedFlag(follower)), "Kolodion didn't take the " + follower.remains());
            note(follower.name() + " found and killed with " + follower.spell() + " (" + casts + " casts)");
        }
        talk(MageArena.KOLODION_NPC);
        expect(stage("mage_arena_ii") == 2, "handing in all three remains didn't move the miniquest on (stage " + stage("mage_arena_ii") + ")");
        Inv.give(player, "saradomin_cape", 1);
        talk(MageArena.KOLODION_NPC);
        expect(done("mage_arena_ii"), "Kolodion imbuing a cape didn't complete Mage Arena II");
        expect(Inv.has(player, "imbued_saradomin_cape", 1), "no imbued Saradomin cape");
        // Afterwards: one more justiciar's hand and a cape make another.
        Inv.give(player, "justiciars_hand", 1);
        Inv.give(player, "saradomin_cape", 1);
        int imbued = Inv.count(player, "imbued_saradomin_cape");
        talk(MageArena.KOLODION_NPC);
        expect(Inv.count(player, "imbued_saradomin_cape") == imbued + 1, "Kolodion didn't imbue a second cape from a justiciar's hand");
        note("remains handed in, cape imbued, another after the miniquest");
    }

    /** Uses the held enchanted symbol through the real right-click event ({@code sneak}: attune). */
    private void useSymbol(boolean sneak) {
        ItemStack symbol = Inv.stack(MageArena.SYMBOL, 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, symbol);
        player.getCooldowns().removeCooldown(symbol.getItem());
        player.setShiftKeyDown(sneak);
        try {
            UseItemCallback.EVENT.invoker().interact(player, world, InteractionHand.MAIN_HAND);
        } finally {
            player.setShiftKeyDown(false);
        }
    }

    /** Picks up a dropped item next to a dead creature (as walking over it would). */
    private boolean pickUp(LivingEntity near, String item) {
        for (ItemEntity e : world.getEntitiesOfClass(ItemEntity.class, new AABB(near.blockPosition()).inflate(8), i -> true)) {
            if (item.equals(Inv.idOf(e.getItem()))) {
                Inv.give(player, e.getItem().copy());
                e.discard();
                return true;
            }
        }
        return Inv.has(player, item, 1);
    }
}
