package com.runecraft.content.bosses;

import com.runecraft.boss.BossScripts;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.content.bosses.BossItems;
import com.runecraft.osrs.content.bosses.BossPlaces;
import com.runecraft.osrs.content.bosses.Dt2Bosses;
import com.runecraft.osrs.content.bosses.SlayerBosses.HydraPhase;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Minecraft-side behaviour for the bosses content module: boss scripts and the Font of Consumption. */
public final class BossesHooks implements ModuleHooks {
    @Override
    public void init() {
        BossScripts.register("giant_mole", ClassicScripts.GiantMole::new);
        BossScripts.register("kalphite_queen", ClassicScripts.KalphiteQueen::new);
        BossScripts.register("kalphite_queen_airborne", ClassicScripts.KalphiteQueenAirborne::new);
        BossScripts.register("dagannoth_rex", ClassicScripts.DagannothRex::new);
        BossScripts.register("corporeal_beast", ClassicScripts.Corp::new);
        BossScripts.register("skotizo", ClassicScripts.Skotizo::new);
        ClassicScripts.Skotizo.registerAltarBreaker();
        BossScripts.register("hespori", ClassicScripts.Hespori::new);
        BossScripts.register("scurrius", ClassicScripts.Scurrius::new);
        BossScripts.register("sarachnis", ClassicScripts.Sarachnis::new);
        BossScripts.register("zalcano", ClassicScripts.Zalcano::new);
        BossScripts.register("sol_heredit", SolHereditScript::new);
        SolHereditScript.register();

        for (String form : new String[]{"zulrah", "zulrah_magma", "zulrah_tanzanite"}) {
            BossScripts.register(form, () -> new ZulrahScript(form));
        }
        BossScripts.register("vorkath", VorkathScript::new);
        // Dragon Slayer II's Vorkath (quests module boss): the same fight with the quest version's max hits.
        BossScripts.register("ds2_vorkath", () -> new VorkathScript(true));
        VorkathScript.registerCrumbleUndead();
        com.runecraft.boss.DataBossService.loot("vorkath", VorkathScript::rollLoot);
        // OSRS Wiki: Zulrah's shrine needs Regicide (to reach Zul-Andra), Ungael needs Dragon Slayer II.
        com.runecraft.boss.DataBossService.requirement("zulrah", (player, def) -> questDone(player, "regicide",
                "Priestess Zul-Gwenwynig: Outsiders may not reach the shrine. (Complete Regicide to reach Zul-Andra.)"));
        com.runecraft.boss.DataBossService.requirement("vorkath", (player, def) -> questDone(player, "dragon_slayer_ii",
                "Torfinn: Ungael? Nobody sails there. (Complete Dragon Slayer II to fight Vorkath.)"));

        BossScripts.register("kraken", SlayerScripts.Kraken::new);
        BossScripts.register("thermonuclear_smoke_devil", SlayerScripts.Thermy::new);
        BossScripts.register("cerberus", SlayerScripts.Cerberus::new);
        BossScripts.register("abyssal_sire", SlayerScripts.Sire::new);
        for (HydraPhase phase : HydraPhase.values()) {
            BossScripts.register(phase.bossId, () -> new SlayerScripts.Hydra(phase));
        }
        BossScripts.register("dusk", SlayerScripts.Dusk::new);
        BossScripts.register("dawn", SlayerScripts.Dawn::new);
        BossScripts.register("araxxor", SlayerScripts.Araxxor::new);

        BossScripts.register("crystalline_hunllef", () -> new EncounterScripts.Hunllef(false));
        BossScripts.register("corrupted_hunllef", () -> new EncounterScripts.Hunllef(true));
        BossScripts.register("the_nightmare", EncounterScripts.Nightmare::new);
        BossScripts.register("phosanis_nightmare", () -> new EncounterScripts.Nightmare(true));
        BossScripts.register("phantom_muspah", EncounterScripts.Muspah::new);
        BossScripts.register("duke_sucellus", EncounterScripts.Duke::new);
        BossScripts.register("the_leviathan", EncounterScripts.Leviathan::new);
        BossScripts.register("the_whisperer", WhispererScript::new);
        BossScripts.register("vardorvis", EncounterScripts.Vardorvis::new);
        // Desert Treasure II's quest versions (quests module ids): the same fights with lower max hits.
        BossScripts.register("dt2_quest_duke_sucellus", () -> new EncounterScripts.Duke(Dt2Bosses.QUEST_DUKE_SCALE));
        BossScripts.register("dt2_quest_leviathan", () -> new EncounterScripts.Leviathan(Dt2Bosses.QUEST_LEVIATHAN_SCALE));
        BossScripts.register("dt2_quest_whisperer", () -> new WhispererScript(Dt2Bosses.QUEST_WHISPERER_SCALE));
        BossScripts.register("dt2_quest_vardorvis", () -> new EncounterScripts.Vardorvis(Dt2Bosses.QUEST_VARDORVIS_SCALE));
        AwakenedService.init();
        DoomDelveService.init();
        BossScripts.register("the_hueycoatl", EncounterScripts.Hueycoatl::new);
        // Moons of Peril (zeah module bosses): flat armour and the Eclipse Moon's clones.
        BossScripts.register("eclipse_moon", MoonScripts.EclipseMoon::new);
        BossScripts.register("blue_moon", MoonScripts.BlueMoon::new);
        BossScripts.register("branda_the_fire_queen", EncounterScripts.RoyalTitans::new);
        // Fought beside the main boss, whose kill count they share (one "Royal Titans" / "Grotesque Guardians" kill).
        com.runecraft.boss.DataBossService.companion("eldric_the_ice_king");
        com.runecraft.boss.DataBossService.companion("dawn");
        MinionRules.register();
        BossDots.register();
        BossScripts.register("yama", ModernScripts.Yama::new);
        BossScripts.register("amoxliatl", ModernScripts.Amoxliatl::new);
        BossScripts.register("maggot_king", ModernScripts.MaggotKing::new);
        OldBossScripts.register();
        // Yama's sigil contracts: one held in the main hand when challenging him is shown to him (and consumed).
        com.runecraft.boss.DataBossService.requirement("yama", (player, def) -> ModernScripts.Yama.offerContract(player));

        BlockHooks.register(BossPlaces.FONT_OF_CONSUMPTION, BossesHooks::offerUnsired);

        BossScripts.register(com.runecraft.osrs.content.bosses.IslandBosses.DERANGED, DerangedArchaeologistScript::new);
        BossScripts.register("demonic_brutus", DemonicBrutusScript::new);
        // Brutus is fought after The Ides of Milk; Demonic Brutus also needs an abyssal potato (the boss key, plucked in the
        // Scar after Desert Treasure II).
        com.runecraft.boss.DataBossService.requirement("brutus", (player, def) -> questDone(player, "the_ides_of_milk",
                "You need to complete The Ides of Milk to fight Brutus."));
        com.runecraft.boss.DataBossService.requirement("demonic_brutus", (player, def) -> questDone(player, "the_ides_of_milk",
                "You need to complete The Ides of Milk to fight Brutus.") && questDone(player, "desert_treasure_ii",
                "Brutus won't eat anything that hasn't been touched by the Abyss. (Desert Treasure II)"));
        // The Shellbane gryphon is a Slayer boss: only on a gryphon task (or a boss task for it).
        com.runecraft.boss.DataBossService.requirement("shellbane_gryphon", (player, def) -> {
            String task = com.runecraft.content.slayer.SlayerService.profile(player).hasTask()
                    ? com.runecraft.content.slayer.SlayerService.profile(player).task : null;
            if ("gryphons".equals(task) || com.runecraft.osrs.content.slayer.SlayerBossTasks.counts(task, "shellbane_gryphon")) {
                return true;
            }
            Msg.game(player, "Elder Nama: Only those hunting gryphons for a Slayer master may face the shellbane gryphon.");
            return false;
        });
        BlockHooks.register(BossPlaces.SCAR_GROWTH, BossesHooks::pluckGrowth);
        BlockHooks.register(BossPlaces.ANCIENT_VAULT_CHEST, BossesHooks::openVaultChest);
        // Yama's dossiers open into a contract or chasm teleport scrolls (OSRS Wiki "Dossier").
        com.runecraft.platform.UseItemCallback.EVENT.register((player, world, hand) -> {
            net.minecraft.world.item.ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || stack.isEmpty() || !(player instanceof ServerPlayer p) || !"dossier".equals(Inv.idOf(stack))) {
                return net.minecraft.world.InteractionResultHolder.pass(stack);
            }
            openDossier(p);
            return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, false);
        });
    }

    /**
     * The Ancient Vault's chest: a quartz (the one in hand, else the first carried) is consumed for one of its five
     * rewards (Dt2Bosses.VAULT_CHEST); noted rewards come as bank notes.
     */
    private static void openVaultChest(ServerPlayer player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!questDone(player, "desert_treasure_ii", "The chest is sealed by Zarosian magic. (Complete Desert Treasure II.)")) {
            return;
        }
        int q = Dt2Bosses.QUARTZES.indexOf(com.runecraft.util.Inv.idOf(player.getMainHandItem()));
        for (int i = 0; q < 0 && i < Dt2Bosses.QUARTZES.size(); i++) {
            if (Inv.has(player, Dt2Bosses.QUARTZES.get(i), 1)) {
                q = i;
            }
        }
        if (q < 0) {
            Msg.game(player, "The chest has four sockets, shaped for quartzes from the old empire's generals.");
            return;
        }
        Inv.remove(player, Dt2Bosses.QUARTZES.get(q), 1);
        java.util.List<Object[]> table = Dt2Bosses.VAULT_CHEST.get(q);
        Object[] r = table.get(player.getRandom().nextInt(table.size()));
        String item = (String) r[0];
        int n = (Integer) r[1];
        net.minecraft.world.item.ItemStack note = (Integer) r[2] == 1 && com.runecraft.osrs.bank.BankNotes.canNote(item)
                ? com.runecraft.bank.Notes.note(item, "", 1) : net.minecraft.world.item.ItemStack.EMPTY;
        if (note.isEmpty()) {
            Inv.give(player, item, n);
        } else {
            int in = com.runecraft.bank.Notes.insert(player, note, n);
            if (in < n) {
                Inv.drop(player, note.copyWithCount(n - in), false);
            }
        }
        com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(item);
        Msg.game(player, "The quartz shatters as the chest opens. You find " + n + " x " + (def == null ? item : def.name()) + ".");
    }

    /** Opens one dossier: a roll on its 1,740-weight table. Public for the self-test. */
    public static String openDossier(ServerPlayer player) {
        if (!Inv.has(player, "dossier", 1)) {
            return null;
        }
        Inv.remove(player, "dossier", 1);
        Object[] e = com.runecraft.osrs.content.bosses.BossMechanics.dossierRoll(
                player.getRandom().nextInt(com.runecraft.osrs.content.bosses.BossMechanics.DOSSIER_TOTAL));
        String item = (String) e[0];
        int min = (Integer) e[1];
        int max = (Integer) e[2];
        int n = min + player.getRandom().nextInt(max - min + 1);
        Inv.give(player, item, n);
        com.runecraft.osrs.ItemDef def = com.runecraft.osrs.ItemCatalog.get(item);
        Msg.game(player, "You open the dossier and find " + n + " x " + (def == null ? item : def.name()) + ".");
        return item;
    }

    private static boolean questDone(ServerPlayer player, String questId, String message) {
        var quest = com.runecraft.osrs.content.ContentRegistry.quest(questId);
        if (quest != null && com.runecraft.player.Players.data(player).questComplete(quest)) {
            return true;
        }
        Msg.game(player, message);
        return false;
    }

    /** The growth in the Scar gives abyssal potatoes once Desert Treasure II is complete. */
    private static void pluckGrowth(ServerPlayer player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!questDone(player, "desert_treasure_ii", "The growth recoils from your touch. (Complete Desert Treasure II.)")) {
            return;
        }
        Inv.give(player, "abyssal_potato", 1);
        Msg.game(player, "You harvest an odd looking object. It kind of looks like a potato.");
    }

    /** Offering an unsired to the Font of Consumption rolls the Abyssal Sire's unique table. */
    private static void offerUnsired(ServerPlayer player, BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (!Inv.has(player, "unsired", 1)) {
            Msg.game(player, "The Font of Consumption hungers for an unsired.");
            return;
        }
        Inv.remove(player, "unsired", 1);
        int total = 0;
        for (Map.Entry<String, Integer> e : BossItems.UNSIRED_TABLE) {
            total += e.getValue();
        }
        int roll = player.getRandom().nextInt(total);
        String reward = BossItems.UNSIRED_TABLE.get(0).getKey();
        for (Map.Entry<String, Integer> e : BossItems.UNSIRED_TABLE) {
            roll -= e.getValue();
            if (roll < 0) {
                reward = e.getKey();
                break;
            }
        }
        Inv.give(player, reward, 1);
        CollectionLog.obtain(player, reward);
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                30, 0.4, 0.4, 0.4, 0.05);
        Msg.game(player, "The Font consumes the unsired and returns you " + Msg.article(reward.replace('_', ' ')) + ".");
    }
}
