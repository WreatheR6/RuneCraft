package com.runecraft.selftest;

import com.runecraft.RuneCraft;
import com.runecraft.action.ActionManager;
import com.runecraft.boss.DataBossEntity;
import com.runecraft.boss.DataBossService;
import com.runecraft.boss.Mechanics;
import com.runecraft.content.bosses.BossWalks;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.skill.XpService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

/**
 * Phase walks of the modern bosses ({@code boss:walk:<id>}, {@link BossWalks}): the boss is spawned on the test platform and
 * fought phase by phase, each mechanic answered the OSRS way, by the real player in the client walkthrough. Like the fight
 * checks, nothing else in the world ticks while a walk runs; the player keeps a health buffer and is healed every tick.
 */
final class BossWalkChecks {
    private static final int MC = ActionManager.MC_TICKS_PER_GAME_TICK;
    private static final ResourceLocation HP_BUFFER = RuneCraft.id("selftest_walk_hp");

    /** A walk key ending in this fights the awakened variant. */
    private static final String AWAKENED = ":awakened";

    /** Boss id (":awakened" for the variant) -> its walk. */
    private static final Map<String, BiFunction<DataBossEntity, BossWalks.Driver, String>> WALKS = new LinkedHashMap<>();

    static {
        WALKS.put("the_whisperer", BossWalks::whisperer);
        WALKS.put("blue_moon", BossWalks::blueMoon);
        WALKS.put("eclipse_moon", BossWalks::eclipseMoon);
        WALKS.put("blood_moon", BossWalks::bloodMoon);
        WALKS.put("the_leviathan", BossWalks::leviathan);
        WALKS.put("amoxliatl", BossWalks::amoxliatl);
        WALKS.put("the_hueycoatl", BossWalks::hueycoatl);
        WALKS.put("branda_the_fire_queen", BossWalks::royalTitans);
        WALKS.put("duke_sucellus", BossWalks::duke);
        WALKS.put("vardorvis", BossWalks::vardorvis);
        // The awakened Desert Treasure II fights walk the same way (their own numbers and rules apply).
        WALKS.put("the_whisperer" + AWAKENED, BossWalks::whisperer);
        WALKS.put("the_leviathan" + AWAKENED, BossWalks::leviathan);
        WALKS.put("duke_sucellus" + AWAKENED, BossWalks::duke);
        WALKS.put("vardorvis" + AWAKENED, BossWalks::vardorvis);
    }

    /** Damage the walking player took, by attack name (attack-less hits under their style, or "typeless"). */
    private static final Map<String, Integer> DAMAGE = new java.util.HashMap<>();
    private static final Map<String, Integer> BIGGEST = new java.util.HashMap<>();
    private static ServerPlayer walking;
    private static boolean hooked;

    private final TestContext c;

    private BossWalkChecks(TestContext c) {
        this.c = c;
    }

    static List<SelfTest.Check> all(TestContext context) {
        BossWalkChecks w = new BossWalkChecks(context);
        List<SelfTest.Check> checks = new ArrayList<>();
        for (Map.Entry<String, BiFunction<DataBossEntity, BossWalks.Driver, String>> e : WALKS.entrySet()) {
            checks.add(new SelfTest.Check("boss:walk:" + e.getKey(), () -> w.walk(e.getKey(), e.getValue())));
        }
        return checks;
    }

    private String walk(String key, BiFunction<DataBossEntity, BossWalks.Driver, String> walk) {
        boolean awakened = key.endsWith(AWAKENED);
        String id = awakened ? key.substring(0, key.length() - AWAKENED.length()) : key;
        BossDef def = ContentRegistry.boss(id);
        if (def == null) {
            throw new AssertionError("no BossDef " + id);
        }
        ServerPlayer p = c.player;
        if (!hooked) {
            hooked = true;
            com.runecraft.combat.CombatHooks.addDamageListener((player, attacker, attack, style, damage) -> {
                if (player == walking && damage > 0) {
                    String name = attack != null ? attack : style != null ? style.name().toLowerCase(java.util.Locale.ROOT) : "typeless";
                    DAMAGE.merge(name, damage, Integer::sum);
                    BIGGEST.merge(name, damage, Math::max);
                }
            });
        }
        DAMAGE.clear();
        BIGGEST.clear();
        walking = p;
        DataBossEntity boss = DataBossService.spawn(c.world, c.near(0, 0), def, p);
        if (boss == null) {
            throw new AssertionError("DataBossService.spawn returned null for " + id);
        }
        if (awakened) {
            boss.applyVariant(com.runecraft.osrs.content.bosses.AwakenedBosses.variant(def));
        }
        AttributeInstance health = p.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) {
            health.removeModifier(HP_BUFFER);
            health.addTransientModifier(new AttributeModifier(HP_BUFFER, 2000, AttributeModifier.Operation.ADD_VALUE));
        }
        BossWalks.Driver driver = new BossWalks.Driver() {
            @Override
            public void ticks(DataBossEntity b, int n) {
                for (int t = 0; t < n; t++) {
                    for (int m = 0; m < MC; m++) {
                        ActionManager.advanceForSelfTest();
                        if (b.isAlive()) {
                            if (b.getTarget() != p) {
                                b.setTarget(p);
                            }
                            b.tickCount++; // as ServerLevel.tickNonPassenger does (see BossFightChecks)
                            b.tick();
                        }
                        Mechanics.tickForSelfTest();
                        p.setHealth(p.getMaxHealth());
                    }
                }
            }

            @Override
            public ServerPlayer player() {
                return p;
            }

            @Override
            public int damage(String attack) {
                return DAMAGE.getOrDefault(attack, 0);
            }

            @Override
            public int biggest(String attack) {
                return BIGGEST.getOrDefault(attack, 0);
            }

            @Override
            public void clearDamage() {
                DAMAGE.clear();
                BIGGEST.clear();
            }

            @Override
            public void moveTo(Vec3 at, DataBossEntity b) {
                double dx = b.getX() - at.x;
                double dz = b.getZ() - at.z;
                float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                p.teleportTo(c.world, at.x, at.y, at.z, yaw, p.getXRot());
            }
        };
        try {
            // one game tick first: the scripts take their home (the arena centre their mechanics are laid out around) on it
            driver.ticks(boss, 1);
            return walk.apply(boss, driver);
        } catch (IllegalStateException e) {
            throw new AssertionError(e.getMessage(), e);
        } finally {
            walking = null;
            if (boss.isAlive()) {
                boss.discard();
            }
            if (health != null) {
                health.removeModifier(HP_BUFFER);
            }
            XpService.updateMaxHealth(p);
            p.setHealth(p.getMaxHealth());
            c.home();
        }
    }
}
