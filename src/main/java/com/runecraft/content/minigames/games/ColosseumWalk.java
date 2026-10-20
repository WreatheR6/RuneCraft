package com.runecraft.content.minigames.games;

import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.Mechanics;
import com.runecraft.combat.CombatHandler;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.bosses.SolHereditScript;
import com.runecraft.content.minigames.MinigameData;
import com.runecraft.content.minigames.MinigameManager;
import com.runecraft.content.minigames.MinigameSession;
import com.runecraft.entity.OsrsMonsterEntity;
import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.PlaceDef;
import com.runecraft.osrs.content.bosses.ColosseumRewards;
import com.runecraft.osrs.content.minigames.ColosseumLobby;
import com.runecraft.osrs.content.minigames.FortisColosseum;
import com.runecraft.osrs.content.minigames.FortisColosseum.Modifier;
import com.runecraft.osrs.content.minigames.FortisColosseum.Offer;
import com.runecraft.osrs.content.minigames.Minigame;
import com.runecraft.osrs.content.minigames.SolHeredit;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestScripts;
import com.runecraft.util.Inv;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Self-test walk of the Fortis Colosseum as a real player (round 34; {@code com.runecraft.selftest} "minigame:colosseum_walk",
 * real-player mode): down the surface entrance into the lobby, the scoreboard and the Brawlers' bank chest, Minimus's
 * briefing (the arena doorway refuses anyone he hasn't briefed), then a whole run through the doorway: a modifier picked
 * through the chat-link command before every wave, every wave's monsters killed through the player's real hit path, the
 * reinforcements, the warband's weak-style max hits, the Javelin Colossus's every-fifth-attack javelin, manticore patterns
 * and staggered volleys, the minotaur's heal, Totemic, Volatility, Bees! and Solarflare, then Sol Heredit behind the
 * gladiators: his attacks, the grapple (a perfect parry, an undefended one tearing off armour, and one that leaves it on
 * with a full inventory), the phases down to the enrage, Showboating salutes and the kill; the Rewards Chest (the exit
 * refuses an unsearched chest), the quiver traded to Minimus and the stairs back up. Two more runs check that dying loses
 * the chest and that ending a run between waves leaves the loot in the chest, searched with one free slot (one stack
 * carried, the rest banked, nothing on the floor). The session is ticked by hand (accelerated); any broken step throws an
 * {@link AssertionError} naming it.
 */
public final class ColosseumWalk {
    private final ServerPlayer player;
    private final ServerLevel world;
    private final List<String> notes = new ArrayList<>();
    private String step = "surface";
    private ColosseumSession session;
    private int ticks;

    private ColosseumWalk(ServerPlayer player) {
        this.player = player;
        this.world = (ServerLevel) player.level();
    }

    /** Walks the Colosseum as {@code p}; returns a summary or throws {@link AssertionError}. */
    public static String run(ServerPlayer p) {
        ColosseumWalk w = new ColosseumWalk(p);
        try {
            return w.walk();
        } catch (AssertionError e) {
            throw new AssertionError("[" + w.step + "] " + e.getMessage() + " (after " + w.ticks + " ticks; done: "
                    + String.join("; ", w.notes) + ")");
        } finally {
            if (w.session != null && !w.session.isEnded()) {
                w.session.end(false);
            }
            p.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        }
    }

    private String walk() {
        MinigameData data = MinigameData.of(player);
        UUID u = player.getUUID();
        int glory = data.stat(u, FortisColosseum.STAT_GLORY);
        try {
            data.setStat(u, FortisColosseum.STAT_GLORY, 0);
            surfaceToLobby();
            lobby();
            fullRun();
            afterRun();
            deathRun();
            cashOutRun();
        } finally {
            data.setStat(u, FortisColosseum.STAT_GLORY, glory);
        }
        return ticks + " game ticks: " + String.join("; ", notes);
    }

    // ------------------------------------------------------------------ the lobby

    private void surfaceToLobby() {
        step = "surface";
        PlaceDef entrance = ContentRegistry.place(Minigame.FORTIS_COLOSSEUM.place());
        expect(entrance != null, "no Colosseum entrance place");
        com.runecraft.world.LandmarkBuilder.buildNow(world, entrance);
        BlockPos portal = find(entrance.blockX(), entrance.blockZ(), 8, 40, 140, Minigame.FORTIS_COLOSSEUM.entranceBlock());
        expect(portal != null, "no Colosseum entrance block at the surface entrance");
        moveTo(portal.north());
        use(portal);
        expect(ColosseumSession.inLobby(player), "the surface entrance did not lead down into the lobby (player at "
                + player.blockPosition() + ")");
        expect(player.blockPosition().equals(ColosseumSession.lobbyArrival()) || ColosseumSession.inLobby(player), "arrived off the stairs");
        notes.add("down the entrance into the lobby");
    }

    private BlockPos lobbyBlock(int tx, int ty, String id) {
        int[] s = ColosseumLobby.stand(tx, ty);
        BlockPos pos = new BlockPos(s[0], s[1], s[2]);
        expect(id.equals(blockId(pos)), "no " + id + " at " + tx + "," + ty + " (found " + blockId(pos) + ")");
        return pos;
    }

    private void lobby() {
        step = "lobby";
        PlaceDef lobbyPlace = ContentRegistry.place(FortisColosseum.LOBBY_PLACE);
        expect(lobbyPlace != null, "no lobby place");
        com.runecraft.world.LandmarkBuilder.buildNow(world, lobbyPlace);
        BlockPos scoreboard = lobbyBlock(FortisColosseum.SCOREBOARD_X, FortisColosseum.SCOREBOARD_Y, FortisColosseum.SCOREBOARD);
        BlockPos bank = lobbyBlock(FortisColosseum.BANK_CHEST_X, FortisColosseum.BANK_CHEST_Y, FortisColosseum.BANK_CHEST);
        BlockPos doorway = lobbyBlock(FortisColosseum.ARENA_ENTRANCE_X, FortisColosseum.ARENA_ENTRANCE_Y, FortisColosseum.ARENA_ENTRANCE);
        lobbyBlock(1797, FortisColosseum.LOBBY_Y, FortisColosseum.LOBBY_EXIT);
        use(scoreboard);
        // The Brawlers' bank chest needs 2,000 Glory.
        use(bank);
        expect(player.containerMenu == player.inventoryMenu, "the bank chest opened with no Glory");
        MinigameData.of(player).setStat(player.getUUID(), FortisColosseum.STAT_GLORY, FortisColosseum.GLORY_BANK);
        use(bank);
        expect(player.containerMenu != player.inventoryMenu, "the bank chest did not open at 2,000 Glory");
        player.closeContainer();
        MinigameData.of(player).setStat(player.getUUID(), FortisColosseum.STAT_GLORY, 0);
        // The doorway refuses a player Minimus hasn't briefed.
        moveTo(doorway.south());
        use(doorway);
        expect(MinigameManager.session(player) == null, "the arena doorway let in a player Minimus never spoke to");
        // The lobby's people (their spots on carved floor are checked by ColosseumLobbyTest; the entities of a chunk loaded
        // inside this tick may not be visible yet, so they are talked to through their handlers).
        QuestNpcEntity minimus = npc(FortisColosseum.MINIMUS);
        for (String id : new String[]{ColosseumLobby.GLORIA, ColosseumLobby.UEMAN, ColosseumLobby.SEIA, ColosseumLobby.GLADIATOR,
                ColosseumLobby.GUARD}) {
            talk(id, npc(id));
        }
        talk(FortisColosseum.MINIMUS, minimus);
        expect(MinigameData.of(player).flag(player.getUUID(), ColosseumSession.FLAG_BRIEFED), "talking to Minimus did not brief the player");
        notes.add("lobby: scoreboard, bank chest at 2,000 Glory, doorway refused until Minimus briefed"
                + (minimus != null ? ", Minimus in the hall" : ""));
    }

    private QuestNpcEntity npc(String id) {
        int[] s = ColosseumLobby.stand(FortisColosseum.LOBBY_X, FortisColosseum.LOBBY_Y);
        net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(s[0] - 10, s[1] - 4, s[2] - 60, s[0] + 90, s[1] + 8, s[2] + 30);
        for (QuestNpcEntity e : world.getEntitiesOfClass(QuestNpcEntity.class, box, e -> id.equals(e.npcId()))) {
            return e;
        }
        return null;
    }

    private void talk(String id, QuestNpcEntity npc) {
        var h = QuestScripts.handler(id);
        expect(h != null, "no talk handler for " + id);
        h.accept(player, npc);
    }

    private void enter() {
        BlockPos doorway = lobbyBlock(FortisColosseum.ARENA_ENTRANCE_X, FortisColosseum.ARENA_ENTRANCE_Y, FortisColosseum.ARENA_ENTRANCE);
        moveTo(doorway.south());
        use(doorway);
        MinigameSession s = MinigameManager.session(player);
        expect(s instanceof ColosseumSession, "the arena doorway did not start a Colosseum run (session " + s + ")");
        session = (ColosseumSession) s;
        expect(session.choosing() && session.offers().size() == 3, "no modifier offer at the start");
    }

    // ------------------------------------------------------------------ a whole run

    /** The offers this walk prefers (harmless to the invulnerable test player and exercising mechanics), best first. */
    private static final List<Modifier> PREFERENCE = List.of(Modifier.BLASPHEMY, Modifier.RELENTLESS, Modifier.MYOPIA, Modifier.QUARTET,
            Modifier.DYNAMIC_DUO, Modifier.RED_FLAG, Modifier.REENTRY, Modifier.VOLATILITY, Modifier.TOTEMIC, Modifier.MANTIMAYHEM,
            Modifier.SOLARFLARE, Modifier.BEES, Modifier.FRAILTY, Modifier.DOOM);

    /** Picks a modifier through the chat link's command and waits for the wave to start. */
    private void pickAndStart() {
        List<Offer> offers = session.offers();
        expect(offers.size() == 3 || (session.wave() + 1 >= FortisColosseum.WAVES && !offers.isEmpty()), "offers " + offers.size());
        int best = 0;
        for (int i = 1; i < offers.size(); i++) {
            if (PREFERENCE.indexOf(offers.get(i).modifier()) < PREFERENCE.indexOf(offers.get(best).modifier())) {
                best = i;
            }
        }
        int wave = session.wave() + 1;
        command("minigame colosseum pick " + (best + 1));
        expect(!session.choosing(), "the pick command did not choose " + offers.get(best).label());
        until(() -> session.fighting() && session.wave() == wave, FortisColosseum.WAVE_START_DELAY + 3, "wave " + wave + " did not start");
    }

    private int expectedMonsters(int wave) {
        int n = FortisColosseum.wave(wave).monsters().size();
        if (session.modifiers().containsKey(Modifier.QUARTET)) {
            n++;
        }
        if (session.modifiers().containsKey(Modifier.DYNAMIC_DUO)) {
            n += (int) FortisColosseum.wave(wave).monsters().stream().filter(FortisColosseum.SHOCKWAVE::equals).count();
        }
        return n;
    }

    private void fullRun() {
        step = "entering";
        enter();
        List<Offer> first = session.offers();
        expect(first.stream().map(Offer::modifier).toList().equals(FortisColosseum.FIRST_OFFER), "first offers " + first);
        int kcBefore = MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.FORTIS_COLOSSEUM.id());
        for (int wave = 1; wave < FortisColosseum.WAVES; wave++) {
            step = "wave " + wave;
            beforeWave(wave);
            pickAndStart();
            expect(session.monstersAlive() == expectedMonsters(wave), session.monstersAlive() + " monsters (expected "
                    + expectedMonsters(wave) + ")");
            String note = waveMechanics(wave);
            int killed = killWave();
            until(session::choosing, 12, "the wave did not complete after its monsters died");
            expect(!session.chest().isEmpty(), "the Rewards Chest is empty after wave " + wave);
            notes.add("wave " + wave + ": " + killed + " killed" + (note.isEmpty() ? "" : ", " + note));
        }
        int kc = MinigameData.of(player).stat(player.getUUID(), "kc:" + Minigame.FORTIS_COLOSSEUM.id()) - kcBefore;
        expect(kc == 11, kc + " waves counted after 11 cleared");
        solWave();
    }

    /** Forces modifiers ahead of some waves so their mechanics are walked whatever Minimus offered. */
    private void beforeWave(int wave) {
        switch (wave) {
            case 4 -> {
                session.selfTestForce(Modifier.TOTEMIC, 1);
                session.selfTestForce(Modifier.VOLATILITY, 3);
            }
            case 9 -> session.selfTestForce(Modifier.MANTIMAYHEM, 3);
            case 10 -> session.selfTestForce(Modifier.BEES, 1);
            case 11 -> session.selfTestForce(Modifier.SOLARFLARE, 1);
            default -> {
            }
        }
    }

    private LivingEntity first(String id) {
        for (LivingEntity e : session.waveMonstersForTest()) {
            if (MinigameManager.creatureId(e).equals(id)) {
                return e;
            }
        }
        return null;
    }

    private List<LivingEntity> all(String id) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : session.waveMonstersForTest()) {
            if (MinigameManager.creatureId(e).equals(id)) {
                out.add(e);
            }
        }
        return out;
    }

    private String waveMechanics(int wave) {
        List<String> done = new ArrayList<>();
        if (wave == 1) {
            for (String w : FortisColosseum.WARBAND) {
                LivingEntity e = first(w);
                expect(e instanceof OsrsMonsterEntity m && m.meleeRangeOnly, w + " would attack from range");
            }
            // Reinforcements arrive after 40 seconds.
            until(() -> first(FortisColosseum.JAGUAR) != null, FortisColosseum.REINFORCEMENT_TICKS + 3, "no jaguar reinforcement after 40 s");
            done.add("jaguar reinforcement");
            // The berserker always takes a max hit from magic.
            LivingEntity berserker = first(FortisColosseum.BERSERKER);
            int dealt = hit(berserker, 1, 60, AttackType.MAGIC);
            expect(dealt >= 48 || !berserker.isAlive(), "a 1-damage magic roll on the berserker dealt " + dealt + " (always a max hit)");
            done.add("berserker maxed by magic");
        }
        if (wave == 2) {
            LivingEntity c = first(FortisColosseum.JAVELIN);
            expect(c instanceof OsrsMonsterEntity m && m.attackInterceptor != null && m.rangedReach == FortisColosseum.COLOSSUS_RANGE,
                    "the Javelin Colossus has no 15-tile reach or javelin hook");
            OsrsMonsterEntity m = (OsrsMonsterEntity) c;
            for (int i = 1; i <= 4; i++) {
                m.chooseStyle(player, m.distanceToSqr(player));
                m.performAttack(player, AttackType.RANGED);
                expect(session.javelinsPending() == 0, "attack " + i + " was a sky javelin");
            }
            m.chooseStyle(player, m.distanceToSqr(player));
            m.performAttack(player, AttackType.RANGED);
            expect(session.javelinsPending() == 1, "the fifth attack was not a sky javelin");
            tick(FortisColosseum.JAVELIN_FLIGHT_TICKS + 1);
            expect(session.javelinsPending() == 0, "the javelin never landed");
            done.add("fifth attack a sky javelin");
        }
        if (wave == 4) {
            LivingEntity manticore = first(FortisColosseum.MANTICORE);
            expect(manticore instanceof OsrsMonsterEntity, "no manticore");
            ((OsrsMonsterEntity) manticore).performAttack(player, AttackType.MAGIC);
            List<AttackType> pattern = session.manticorePattern(manticore);
            boolean anyOrder = session.modifiers().getOrDefault(Modifier.MANTIMAYHEM, 0) >= 3;
            expect(pattern != null && pattern.size() == 3 && (anyOrder || pattern.get(2) == AttackType.CRUSH)
                    && new java.util.HashSet<>(pattern).size() == 3, "manticore pattern " + pattern);
            expect(session.orbsPending() == 3, session.orbsPending() + " orbs queued (expected 3)");
            tick(3);
            expect(session.orbsPending() == 0, "the orbs never landed");
            done.add("manticore " + pattern);
            // Totemic: a totem rises beside a monster at half health and heals it.
            LivingEntity shaman = first(FortisColosseum.SHAMAN);
            shaman.setHealth(shaman.getMaxHealth() * 0.4f);
            tick(1);
            expect(session.totemCount() >= 1, "no healing totem rose beside the wounded shaman");
            float before = shaman.getHealth();
            tick(FortisColosseum.TOTEM_FIRST_HEAL_TICKS + FortisColosseum.TOTEM_HEAL_TICKS + 1);
            expect(shaman.getHealth() > before, "the totem did not heal the shaman");
            done.add("totem healed the shaman");
            // Volatility III: the shaman explodes and leaves molten sand.
            int sand = session.sandTiles();
            moveTo(shaman.blockPosition().east());
            kill(shaman, AttackType.SLASH, "the shaman");
            expect(session.sandTiles() > sand, "Volatility III left no molten sand");
            done.add("volatility sand");
        }
        if (wave == 7) {
            until(() -> first(FortisColosseum.MINOTAUR) != null, FortisColosseum.REINFORCEMENT_TICKS + 3, "no minotaur reinforcement");
            LivingEntity mino = first(FortisColosseum.MINOTAUR);
            LivingEntity colossus = first(FortisColosseum.JAVELIN);
            colossus.setHealth(colossus.getMaxHealth() * 0.6f); // wounded below 75%, above Totemic's 50%
            BlockPos beside = clearBeside(colossus.blockPosition(), 3);
            expect(beside != null, "no open floor beside the colossus for the minotaur");
            mino.teleportTo(beside.getX() + 0.5, beside.getY(), beside.getZ() + 0.5);
            moveTo(session.arena(0, 1, 15));
            if (Math.sqrt(player.distanceToSqr(mino)) < 6) {
                moveTo(session.arena(0, 1, -15));
            }
            until(() -> colossus.getHealth() >= colossus.getMaxHealth() - 0.01f, 6, "the minotaur did not heal the wounded colossus");
            done.add("minotaur healed the colossus");
        }
        if (wave == 9) {
            List<LivingEntity> manticores = all(FortisColosseum.MANTICORE);
            expect(manticores.size() == 2, manticores.size() + " manticores on wave 9");
            ((OsrsMonsterEntity) manticores.get(0)).performAttack(player, AttackType.MAGIC);
            ((OsrsMonsterEntity) manticores.get(1)).performAttack(player, AttackType.MAGIC);
            List<AttackType> a = session.manticorePattern(manticores.get(0));
            List<AttackType> b = session.manticorePattern(manticores.get(1));
            expect(a != null && a.equals(b), "the second manticore did not copy the first's pattern (" + a + " / " + b + ")");
            long gap = session.manticoreVolleyAt(manticores.get(1)) - session.manticoreVolleyAt(manticores.get(0));
            expect(gap >= FortisColosseum.MANTICORE_STAGGER_TICKS, "volleys only " + gap + " ticks apart");
            tick(FortisColosseum.MANTICORE_STAGGER_TICKS + 3);
            done.add("manticores share " + a + ", volleys " + gap + " ticks apart");
        }
        if (wave == 10) {
            int tier = session.modifiers().getOrDefault(Modifier.BEES, 0);
            expect(tier >= 1 && session.beeCount() == tier, session.beeCount() + " bee swarms with Bees! tier " + tier);
            done.add("bee swarm");
        }
        if (wave == 11) {
            int[] before = session.solarflareTile(0);
            tick(FortisColosseum.solarflareCornerPause(1) + 3);
            int[] after = session.solarflareTile(0);
            expect(before[0] != after[0] || before[1] != after[1], "the solar flare did not move");
            done.add("solar flare moving");
        }
        return String.join(", ", done);
    }

    private int killWave() {
        int n = 0;
        for (int guard = 0; guard < 40 && session.monstersAlive() > 0; guard++) {
            for (LivingEntity e : session.waveMonstersForTest()) {
                kill(e, AttackType.SLASH, MinigameManager.creatureId(e));
                n++;
            }
        }
        expect(session.monstersAlive() == 0, session.monstersAlive() + " monsters would not die");
        return n;
    }

    private void solWave() {
        step = "Sol Heredit";
        int completions = MinigameData.of(player).stat(player.getUUID(), FortisColosseum.STAT_COMPLETIONS);
        int solKc = Players.data(player).killCount(FortisColosseum.SOL_HEREDIT);
        pickAndStart();
        LivingEntity sol = session.sol();
        expect(sol instanceof DataBossEntity, "Sol Heredit is not in the arena");
        DataBossEntity boss = (DataBossEntity) sol;
        expect(session.wallBlocks() > 0 && session.wallSealed(), "the gladiator wall is missing or leaves a gap");
        expect(boss.script() instanceof SolHereditScript, "Sol Heredit runs no SolHereditScript");
        SolHereditScript script = (SolHereditScript) boss.script();
        // Quartet's warbander first, if any.
        for (LivingEntity e : session.waveMonstersForTest()) {
            if (e != sol) {
                kill(e, AttackType.SLASH, MinigameManager.creatureId(e));
            }
        }
        // Next to him he attacks.
        moveTo(sol.blockPosition().east(2));
        until(() -> script.previous() != null, 12, "Sol Heredit never attacked");
        notes.add("Sol opened with " + script.previous());
        // A grapple defended on its last tick is a perfect parry; an undefended one tears the body armour off.
        moveTo(sol.blockPosition().east(2));
        script.selfTestGrapple(boss, player, SolHeredit.Grapple.LEGS);
        tick(SolHeredit.GRAPPLE_WINDOW_TICKS - 1);
        expect(SolHereditScript.parry(player, "legs"), "defending the called slot failed");
        expect(SolHereditScript.perfectReady(player), "a parry on the last tick was not perfect");
        int rolled = CombatHandler.hookAfterRoll(player, sol, AttackType.SLASH, 1, 47);
        expect(rolled >= 47, "the perfect parry's next attack rolled " + rolled + " (expected its max hit, 47)");
        CombatHandler.hookAfterHit(player, sol, AttackType.SLASH, 0);
        expect(!SolHereditScript.perfectReady(player), "the perfect parry outlived the attack it empowered");
        tick(4);
        player.setItemSlot(EquipmentSlot.CHEST, Inv.stack("rune_platebody", 1));
        script.selfTestGrapple(boss, player, SolHeredit.Grapple.BODY);
        tick(SolHeredit.GRAPPLE_WINDOW_TICKS + 1);
        expect(player.getItemBySlot(EquipmentSlot.CHEST).isEmpty() && Inv.count(player, "rune_platebody") >= 1,
                "an undefended body grapple did not tear the platebody into the inventory");
        Inv.remove(player, "rune_platebody", Inv.count(player, "rune_platebody"));
        // With no room in the inventory the armour stays on: nothing is put on the floor, which the arena clears.
        List<ItemStack> kept = fillInventory(0);
        player.setItemSlot(EquipmentSlot.CHEST, Inv.stack("rune_platebody", 1));
        script.selfTestGrapple(boss, player, SolHeredit.Grapple.BODY);
        tick(SolHeredit.GRAPPLE_WINDOW_TICKS + 1);
        expect(Inv.idOf(player.getItemBySlot(EquipmentSlot.CHEST)).equals("rune_platebody"),
                "an undefended body grapple with a full inventory took the platebody off");
        expect(world.getEntitiesOfClass(ItemEntity.class, session.box(), e -> Inv.idOf(e.getItem()).equals("rune_platebody")).isEmpty(),
                "an undefended grapple with a full inventory put the armour on the floor");
        player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
        restoreInventory(kept);
        notes.add("grapple: perfect parry, armour torn off, kept on with a full inventory");
        // The phases down to the enrage.
        for (float f : new float[]{0.85f, 0.70f, 0.45f, 0.20f, 0.08f}) {
            session.selfTestWoundSol(f);
            tick(2);
        }
        expect(script.phase() == 6, "Sol Heredit is in phase " + script.phase() + " below 10% (expected the enrage, 6)");
        notes.add("phases to the enrage");
        // Showboating: the four salutes while he is below 10%.
        float yaw = player.getYRot();
        for (float y : new float[]{180f, 270f, 0f, 90f}) {
            player.setYRot(y);
            session.saluteIn(player);
        }
        player.setYRot(yaw);
        expect(session.salutes().size() == 4, "salutes counted " + session.salutes());
        // The kill.
        kill(sol, AttackType.SLASH, "Sol Heredit");
        until(session::rewardsReady, 6, "the run did not end with Sol Heredit's death");
        expect(Players.data(player).killCount(FortisColosseum.SOL_HEREDIT) == solKc + 1, "no Sol Heredit kill count");
        expect(MinigameData.of(player).stat(player.getUUID(), FortisColosseum.STAT_COMPLETIONS) == completions + 1, "no completion counted");
        expect(session.chest().stream().anyMatch(r -> r.item().equals(ColosseumRewards.QUIVER)), "no Dizana's quiver in the chest");
        expect(world.getEntitiesOfClass(ItemEntity.class, session.box(), e -> true).isEmpty(), "Sol Heredit dropped loot on the floor");
        expect(session.wallBlocks() == 0, "the gladiators stayed after the fight");
        // The exit refuses an unsearched chest; searching it ends the run back in the lobby.
        BlockPos exit = find(session.arena(4, 1, 16).getX(), session.arena(4, 1, 16).getZ(), 2, session.arena(0, 0, 0).getY(),
                session.arena(0, 3, 0).getY(), "minigame_exit");
        expect(exit != null, "no exit portal in the arena");
        use(exit);
        expect(MinigameManager.session(player) == session && session.rewardsReady(), "the exit let the player leave without searching the chest");
        expect(ColosseumSession.REWARDS_CHEST.equals(blockId(session.chestPos())), "no Rewards Chest in the arena's centre");
        player.getInventory().clearContent();
        use(session.chestPos());
        expect(session.isEnded() && MinigameManager.session(player) == null, "searching the chest did not end the run");
        expect(Inv.count(player, ColosseumRewards.QUIVER) == 1, "the chest gave no Dizana's quiver");
        expect(Players.data(player).collectionLog().contains(ColosseumRewards.QUIVER), "the quiver is not in the collection log");
        expect(ColosseumSession.inLobby(player), "the run ended outside the lobby (" + player.blockPosition() + ")");
        notes.add("Sol Heredit slain (KC " + (solKc + 1) + "), chest searched, back in the lobby");
    }

    private void afterRun() {
        step = "after the run";
        int splinters = Inv.count(player, ColosseumRewards.SPLINTERS);
        command("minigame colosseum quiver splinters");
        expect(Inv.count(player, ColosseumRewards.QUIVER) == 0 && Inv.count(player, ColosseumRewards.SPLINTERS) == splinters
                + FortisColosseum.QUIVER_SPLINTERS, "Minimus did not trade the quiver for 4,000 splinters");
        BlockPos stairs = lobbyBlock(1797, FortisColosseum.LOBBY_Y, FortisColosseum.LOBBY_EXIT);
        moveTo(stairs.east());
        use(stairs);
        expect(!ColosseumSession.inLobby(player) && player.getY() > ColosseumLobby.FLOOR + 10, "the stairs did not lead back up");
        notes.add("quiver traded, stairs up");
    }

    /** Wave 1 cleared, then a death in wave 2: the chest is lost and the player is back in the lobby. */
    private void deathRun() {
        step = "death run";
        expect(ColosseumSession.toLobby(player), "could not go back down");
        enter();
        pickAndStart();
        killWave();
        until(session::choosing, 12, "wave 1 did not complete");
        expect(!session.chest().isEmpty(), "nothing in the chest after wave 1");
        int splinters = Inv.count(player, ColosseumRewards.SPLINTERS);
        pickAndStart();
        session.selfTestDie(player);
        expect(session.isEnded() && MinigameManager.session(player) == null, "dying did not end the run");
        expect(Inv.count(player, ColosseumRewards.SPLINTERS) == splinters, "dying still handed out the chest");
        expect(ColosseumSession.inLobby(player), "the dead player did not come back to the lobby");
        notes.add("death in wave 2 lost the chest");
    }

    /** Wave 1 cleared, then the run ended between waves: the chest appears and must be searched. */
    private void cashOutRun() {
        step = "leaving with the loot";
        enter();
        pickAndStart();
        killWave();
        until(session::choosing, 12, "wave 1 did not complete");
        int splinters = Inv.count(player, ColosseumRewards.SPLINTERS);
        command("minigame colosseum leave");
        expect(session.rewardsReady(), "leaving between waves did not end the run");
        expect(Inv.count(player, ColosseumRewards.SPLINTERS) == splinters, "the loot skipped the chest");
        // Searched with one free slot (a run ends with the inventory full of supplies): one stack is carried, the rest is
        // banked, nothing is left on the arena floor.
        List<ItemStack> kept = fillInventory(1);
        com.runecraft.osrs.bank.BankModel bank = Players.data(player).bank();
        long banked = bank.count(ColosseumRewards.SPLINTERS, "");
        int stack = Inv.item(ColosseumRewards.SPLINTERS).getDefaultMaxStackSize();
        use(session.chestPos());
        expect(session.isEnded(), "searching the chest with a full inventory did not end the run");
        expect(world.getEntitiesOfClass(ItemEntity.class, session.box(), e -> true).isEmpty(),
                "searching the chest with a full inventory left loot on the arena floor");
        int carried = Inv.count(player, ColosseumRewards.SPLINTERS);
        long sent = bank.count(ColosseumRewards.SPLINTERS, "") - banked;
        expect(carried == Math.min(80, stack) && carried + sent == 80, "the chest gave " + carried + " splinters to the inventory and "
                + sent + " to the bank (expected wave 1's 80, " + Math.min(80, stack) + " carried)");
        bank.withdraw(ColosseumRewards.SPLINTERS, "", sent);
        Inv.remove(player, ColosseumRewards.SPLINTERS, carried);
        restoreInventory(kept);
        notes.add("left after wave 1: 80 splinters, " + carried + " carried in the free slot and " + sent + " banked");
    }

    // ------------------------------------------------------------------ driver

    /** Fills the main inventory with sharks, leaving {@code free} slots empty; returns what it held, for {@link #restoreInventory}. */
    private List<ItemStack> fillInventory(int free) {
        List<ItemStack> kept = new ArrayList<>();
        for (ItemStack s : player.getInventory().items) {
            kept.add(s.copy());
        }
        ItemStack junk = Inv.stack("shark", 1);
        expect(!junk.isEmpty(), "no shark item to fill the inventory with");
        int size = player.getInventory().items.size();
        for (int i = 0; i < size; i++) {
            player.getInventory().items.set(i, i < size - free ? junk.copy() : ItemStack.EMPTY);
        }
        return kept;
    }

    private void restoreInventory(List<ItemStack> kept) {
        for (int i = 0; i < kept.size(); i++) {
            player.getInventory().items.set(i, kept.get(i));
        }
    }

    /** Advances the run by {@code n} game ticks: the world clock, Sol Heredit (when present), hazards and the session. */
    private void tick(int n) {
        for (int i = 0; i < n; i++) {
            LivingEntity sol = session != null && !session.isEnded() ? session.sol() : null;
            for (int m = 0; m < ActionManager.MC_TICKS_PER_GAME_TICK; m++) {
                ActionManager.advanceForSelfTest();
                if (sol != null && sol.isAlive()) {
                    if (sol instanceof net.minecraft.world.entity.Mob mob && mob.getTarget() != player) {
                        mob.setTarget(player);
                    }
                    sol.tick();
                }
                Mechanics.tickForSelfTest();
            }
            if (session != null && !session.isEnded()) {
                MinigameManager.selfTestTick(session);
            }
            ticks++;
            heal();
        }
    }

    private void heal() {
        player.setHealth(player.getMaxHealth());
        var data = Players.data(player);
        if (data.prayerPoints() < 50) {
            data.setPrayerPoints(99);
        }
    }

    private void until(BooleanSupplier cond, int max, String what) {
        for (int i = 0; i < max && !cond.getAsBoolean(); i++) {
            tick(1);
        }
        expect(cond.getAsBoolean(), what + " (waited " + max + " ticks)");
    }

    /** A hit by the player through the real path after the roll (hooks, max-hit rules, immunities). */
    private int hit(LivingEntity target, int damage, int maxHit, AttackType type) {
        if (target == null || !target.isAlive() || target.isRemoved()) {
            return 0;
        }
        int d = CombatHandler.hookAfterRoll(player, target, type, damage, maxHit);
        if (target instanceof OsrsMonsterEntity m) {
            d = m.modifyIncoming(player, d, type);
        }
        d = CombatHandler.protectionAgainstPlayer(target, type, d);
        int dealt = CombatHandler.applyDamage(player, target, d);
        CombatHandler.hookAfterHit(player, target, type, dealt);
        return dealt;
    }

    private void kill(LivingEntity target, AttackType type, String what) {
        expect(target != null, what + " is missing");
        int guard = 0;
        while (target.isAlive() && !target.isRemoved() && guard++ < 200) {
            int big = target instanceof DataBossEntity b ? Math.max(50, b.trueHitpoints() / 5) : Math.max(20, (int) target.getMaxHealth());
            hit(target, big, big, type);
            tick(1);
        }
        expect(!target.isAlive() || target.isRemoved(), what + " did not die (" + target.getHealth() + " health after " + guard + " hits)");
    }

    private void use(BlockPos pos) {
        BlockHooks.use(player, blockId(pos), pos, world.getBlockState(pos));
    }

    private void command(String cmd) {
        player.getServer().getCommands().performPrefixedCommand(player.createCommandSourceStack(), cmd);
    }

    private String blockId(BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()).getPath();
    }

    private BlockPos find(int cx, int cz, int r, int y1, int y2, String id) {
        for (int y = y1; y <= y2; y++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = new BlockPos(cx + dx, y, cz + dz);
                    if (id.equals(blockId(p))) {
                        return p;
                    }
                }
            }
        }
        return null;
    }

    /** An open spot {@code d} blocks from {@code at} (air for a 3-block-tall body over a solid floor), or null. */
    private BlockPos clearBeside(BlockPos at, int d) {
        for (int[] o : new int[][]{{d, 0}, {-d, 0}, {0, d}, {0, -d}, {d, d}, {-d, -d}, {d, -d}, {-d, d}}) {
            BlockPos p = at.offset(o[0], 0, o[1]);
            if (world.getBlockState(p).isAir() && world.getBlockState(p.above()).isAir() && world.getBlockState(p.above(2)).isAir()
                    && !world.getBlockState(p.below()).isAir()) {
                return p;
            }
        }
        return null;
    }

    private void moveTo(BlockPos stand) {
        player.teleportTo(world, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, player.getYRot(), 0);
        player.fallDistance = 0;
    }

    private static void expect(boolean ok, String message) {
        if (!ok) {
            throw new AssertionError(message);
        }
    }
}
