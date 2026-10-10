package com.runecraft.content.bosses;

import com.runecraft.boss.BossScripts;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.ModuleHooks;
import com.runecraft.osrs.content.bosses.BossItems;
import com.runecraft.osrs.content.bosses.BossPlaces;
import com.runecraft.osrs.content.bosses.SlayerBosses.HydraPhase;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

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
        BossScripts.register("hespori", ClassicScripts.Hespori::new);
        BossScripts.register("scurrius", ClassicScripts.Scurrius::new);
        BossScripts.register("zalcano", ClassicScripts.Zalcano::new);
        BossScripts.register("sol_heredit", ClassicScripts.SolHeredit::new);

        for (String form : new String[]{"zulrah", "zulrah_magma", "zulrah_tanzanite"}) {
            BossScripts.register(form, () -> new ZulrahScript(form));
        }
        BossScripts.register("vorkath", VorkathScript::new);

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
        BossScripts.register("phosanis_nightmare", EncounterScripts.Nightmare::new);
        BossScripts.register("phantom_muspah", EncounterScripts.Muspah::new);
        BossScripts.register("duke_sucellus", EncounterScripts.Duke::new);
        BossScripts.register("the_leviathan", EncounterScripts.Leviathan::new);
        BossScripts.register("the_whisperer", EncounterScripts.Whisperer::new);
        BossScripts.register("vardorvis", EncounterScripts.Vardorvis::new);
        BossScripts.register("the_hueycoatl", EncounterScripts.Hueycoatl::new);
        BossScripts.register("branda_the_fire_queen", EncounterScripts.RoyalTitans::new);

        BlockHooks.register(BossPlaces.FONT_OF_CONSUMPTION, BossesHooks::offerUnsired);
    }

    /** Offering an unsired to the Font of Consumption rolls the Abyssal Sire's unique table. */
    private static void offerUnsired(ServerPlayerEntity player, BlockPos pos, net.minecraft.block.BlockState state) {
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
        ((ServerWorld) player.getWorld()).spawnParticles(ParticleTypes.SQUID_INK, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                30, 0.4, 0.4, 0.4, 0.05);
        Msg.game(player, "The Font consumes the unsired and returns you " + Msg.article(reward.replace('_', ' ')) + ".");
    }
}
