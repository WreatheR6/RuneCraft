package com.runecraft.osrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.runecraft.osrs.content.AttackDef;
import com.runecraft.osrs.content.BossDef;
import com.runecraft.osrs.content.ContentRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BossBarStatusTest {
    private static BossBarStatus.Attack a(AttackType style, int weight) {
        return new BossBarStatus.Attack(style, weight);
    }

    private static List<BossBarStatus.Attack> attacks(String bossId) {
        BossDef def = ContentRegistry.boss(bossId);
        List<BossBarStatus.Attack> out = new ArrayList<>();
        for (AttackDef d : def.attacks) {
            if (!d.isIdle()) {
                out.add(a(d.style(), d.weight()));
            }
        }
        return out;
    }

    @Test
    void zulrahFormsShowTheirColours() {
        assertEquals(BossBarStatus.Colour.GREEN, BossBarStatus.styleColour(attacks("zulrah")), "serpentine: ranged");
        assertEquals(BossBarStatus.Colour.RED, BossBarStatus.styleColour(attacks("zulrah_magma")), "magma: melee");
        assertEquals(BossBarStatus.Colour.BLUE, BossBarStatus.styleColour(attacks("zulrah_tanzanite")), "tanzanite: magic and ranged");
    }

    @Test
    void styleColourRules() {
        assertEquals(BossBarStatus.Colour.RED, BossBarStatus.styleColour(List.of(a(AttackType.SLASH, 3), a(AttackType.RANGED, 1))));
        assertEquals(BossBarStatus.Colour.WHITE, BossBarStatus.styleColour(List.of(a(AttackType.SLASH, 1), a(AttackType.RANGED, 1),
                a(AttackType.MAGIC, 1))));
        assertEquals(BossBarStatus.Colour.YELLOW, BossBarStatus.styleColour(List.of(a(null, 2))));
        assertNull(BossBarStatus.styleColour(List.of()));
    }

    @Test
    void statusText() {
        assertEquals("", BossBarStatus.phaseText(-1, 0));
        assertEquals("Phase 1/3", BossBarStatus.phaseText(-1, 2));
        assertEquals("Phase 3/3", BossBarStatus.phaseText(1, 2));
        assertEquals("switches in 2", BossBarStatus.switchText(2));
        assertEquals("", BossBarStatus.curseText(new int[]{0, 1, 2}));
        // the Nightmare's curse: Magic switches Missiles, Missiles Melee, Melee Magic (NightmareRules.cursed)
        assertEquals("Cursed: MAG>RNG RNG>MEL MEL>MAG", BossBarStatus.curseText(new int[]{1, 2, 0}));
        BossBarStatus.Status s = BossBarStatus.of(0, 1, List.of(a(AttackType.MAGIC, 1)), new int[]{1, 2, 0})
                .with(BossBarStatus.switchText(3));
        assertEquals("The Nightmare · Phase 2/2 · Cursed: MAG>RNG RNG>MEL MEL>MAG · switches in 3",
                BossBarStatus.title("The Nightmare", s));
        assertEquals(BossBarStatus.Colour.BLUE, s.colour());
        assertEquals("Vorkath", BossBarStatus.title("Vorkath", BossBarStatus.of(-1, 0, List.of(), null)));
    }
}
