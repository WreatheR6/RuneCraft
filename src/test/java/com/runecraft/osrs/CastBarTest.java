package com.runecraft.osrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CastBarTest {
    @Test
    void stylesKeepTheRound41Colours() {
        assertEquals(0xE03030, CastBar.rgb(CastBar.styleOf(AttackType.SLASH)));
        assertEquals(0x40C040, CastBar.rgb(CastBar.styleOf(AttackType.RANGED)));
        assertEquals(0x4080FF, CastBar.rgb(CastBar.styleOf(AttackType.MAGIC)));
        assertEquals(0xFFA020, CastBar.rgb(CastBar.styleOf(null)));
        // the protection index matches PrayerGrace's order: magic, missiles, melee
        assertEquals(PrayerGrace.index(AttackType.MAGIC), CastBar.protectionOf(CastBar.MAGIC));
        assertEquals(PrayerGrace.index(AttackType.RANGED), CastBar.protectionOf(CastBar.RANGED));
        assertEquals(PrayerGrace.index(AttackType.CRUSH), CastBar.protectionOf(CastBar.MELEE));
        assertEquals(-1, CastBar.protectionOf(CastBar.TYPELESS));
    }

    @Test
    void overlappingCastsStackSoonestFirstAndExpire() {
        List<CastBar.Cast> casts = new ArrayList<>();
        CastBar.add(casts, 7, CastBar.MAGIC, 0, 100, 60, null);
        CastBar.add(casts, 9, CastBar.RANGED, 0, 105, 36, null);
        CastBar.add(casts, 7, CastBar.MAGIC, 0, 100, 60, null); // the same packet twice
        assertEquals(2, casts.size());
        List<CastBar.Cast> shown = CastBar.shown(casts, CastBar.MAX_SHOWN);
        assertEquals(9, shown.get(0).source(), "the ranged hit lands at 141, before the magic at 160");
        assertEquals(7, shown.get(1).source());
        CastBar.expire(casts, 141 + CastBar.LINGER_MC);
        assertEquals(2, casts.size(), "a landed cast lingers " + CastBar.LINGER_MC + " MC");
        CastBar.expire(casts, 142 + CastBar.LINGER_MC);
        assertEquals(1, casts.size());
        for (int i = 0; i < 6; i++) {
            CastBar.add(casts, 20 + i, CastBar.MELEE, 0, 200, 40 + i, null);
        }
        assertEquals(CastBar.MAX_SHOWN, CastBar.shown(casts, CastBar.MAX_SHOWN).size());
        CastBar.Cast capped = CastBar.add(casts, 99, CastBar.MAGIC, 0, 0, 100_000, null);
        assertEquals(CastBar.MAX_LEAD_MC, capped.landMc());
    }

    @Test
    void specialsMapToFlags() {
        // Protect from Magic lowers dragonfire (Mechanics caps it at 2/3 without a shield or antifire; OSRS Wiki, Dragonfire)
        // and the prayer-piercing hits' scripts take part off under the right prayer: the row names the key, never "no prayer"
        assertEquals(CastBar.FLAG_REDUCED, CastBar.flagsOf(com.runecraft.osrs.content.AttackDef.Special.DRAGONFIRE));
        assertEquals(CastBar.FLAG_REDUCED, CastBar.flagsOf(com.runecraft.osrs.content.AttackDef.Special.PRAYER_PIERCING));
        assertEquals(CastBar.FLAG_DODGE, CastBar.flagsOf(com.runecraft.osrs.content.AttackDef.Special.SKY_STRIKE));
        assertEquals(0, CastBar.flagsOf(com.runecraft.osrs.content.AttackDef.Special.NONE));
        assertEquals(0, CastBar.flagsOf(null));
        for (com.runecraft.osrs.content.AttackDef.Special s : com.runecraft.osrs.content.AttackDef.Special.values()) {
            assertEquals(0, CastBar.flagsOf(s) & CastBar.FLAG_UNPRAYABLE, s + ": no attack special is shown as unprayable");
        }
        CastBar.Cast dragonfire = new CastBar.Cast(1, CastBar.styleOf(AttackType.MAGIC),
                CastBar.flagsOf(com.runecraft.osrs.content.AttackDef.Special.DRAGONFIRE), 0, 36, "");
        assertEquals(PrayerGrace.index(AttackType.MAGIC), dragonfire.protection());
    }

    @Test
    void countdownAndProgress() {
        assertEquals("1.8s", CastBar.seconds(36));
        assertEquals("0.0s", CastBar.seconds(-5));
        CastBar.Cast c = new CastBar.Cast(1, CastBar.MAGIC, 0, 100, 136, "");
        assertEquals(0f, c.progress(100), 1e-6);
        assertEquals(0.5f, c.progress(118), 1e-6);
        assertEquals(1f, c.progress(200), 1e-6);
        assertEquals(18, c.left(118));
    }

    @Test
    void frameAndKeys() {
        CastBar.Cast magic = new CastBar.Cast(1, CastBar.MAGIC, 0, 0, 36, "");
        assertTrue(CastBar.rightOverhead(magic, 0));
        assertFalse(CastBar.rightOverhead(magic, 2));
        assertFalse(CastBar.rightOverhead(magic, -1));
        assertNull(CastBar.rightOverhead(new CastBar.Cast(1, CastBar.TYPELESS, CastBar.FLAG_DODGE, 0, 36, ""), 0));
        assertNull(CastBar.rightOverhead(new CastBar.Cast(1, CastBar.MAGIC, CastBar.FLAG_UNPRAYABLE, 0, 36, ""), 0));
        // a hit the prayer only lowers still wants it up: right/wrong frame and the protection's key
        CastBar.Cast reduced = new CastBar.Cast(1, CastBar.MAGIC, CastBar.FLAG_REDUCED, 0, 36, "");
        assertTrue(reduced.prayable() && reduced.reduced());
        assertEquals(0, reduced.protection());
        assertTrue(CastBar.rightOverhead(reduced, 0));
        assertFalse(CastBar.rightOverhead(reduced, -1));
        assertFalse(magic.reduced());
        // uncursed: the protection's own key; cursed (magic key -> missiles, missiles -> melee, melee -> magic):
        // magic is reached with the melee key
        assertEquals(0, CastBar.keyFor(0, new int[]{0, 1, 2}));
        assertFalse(CastBar.cursed(new int[]{0, 1, 2}));
        int[] curse = {1, 2, 0};
        assertTrue(CastBar.cursed(curse));
        assertEquals(2, CastBar.keyFor(0, curse));
        assertEquals(0, CastBar.keyFor(1, curse));
        assertEquals(1, CastBar.keyFor(2, curse));
    }

    /**
     * Round 43: ground strikes and hazards feed the bar. The landing a row counts down to is the strike's real landing, the
     * same game tick ArenaTiming.strikeDue gives (the 3-tick floor unchanged), and only players who could walk into it get it.
     */
    @Test
    void groundStrikeRows() {
        // a strike made 5 MC into game tick 100 with a 1-tick OSRS delay lands on the floored tick 104
        long due = ArenaTiming.strikeDue(100, 1, true, 5);
        assertEquals(104, due);
        assertEquals(4 * 12 - 5, CastBar.landInMc(due, 100, 5));
        assertTrue(CastBar.landInMc(due, 100, 5) >= ArenaTiming.MIN_LEAD_MC, "a row never counts down less than the floor");
        // made on its game tick: the full 3-tick floor (36 MC)
        assertEquals(ArenaTiming.MIN_LEAD_MC, CastBar.landInMc(ArenaTiming.strikeDue(200, 0, true, 0), 200, 0));
        // a strike already landing (or a clock past it) never counts down below 0
        assertEquals(0, CastBar.landInMc(10, 12, 0));
        // near: inside the ring, or within the walking margin of its edge, at about its height
        assertTrue(CastBar.nearStrike(0, 0, 0, 1.5));
        assertTrue(CastBar.nearStrike(1.5 + CastBar.NEAR_MARGIN - 0.01, 0.5, 0, 1.5));
        assertFalse(CastBar.nearStrike(1.5 + CastBar.NEAR_MARGIN + 0.01, 0, 0, 1.5));
        assertFalse(CastBar.nearStrike(0, CastBar.NEAR_DY, 0, 1.5), "a strike on another floor isn't shown");
        assertTrue(CastBar.nearStrike(2, -1, 2, 1.5));
        // labels: the strike's attack name, readable; none leaves the boss's name
        assertEquals("Fire wall", CastBar.strikeLabel("fire_wall"));
        assertEquals("Acid", CastBar.strikeLabel("ACID"));
        assertNull(CastBar.strikeLabel(null));
        assertNull(CastBar.strikeLabel(" "));
        // a ground row is a spot to step off: no prayer frame, whatever its style colour
        CastBar.Cast ground = new CastBar.Cast(1, CastBar.styleOf(AttackType.MAGIC), CastBar.FLAG_DODGE, 0, 36, "Lightning");
        assertFalse(ground.prayable());
        assertNull(CastBar.rightOverhead(ground, 0));
        assertEquals(CastBar.MAGIC_RGB, ground.rgb());
        // a volley landing together is one row on the client too (same source, style and landing tick)
        List<CastBar.Cast> casts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            CastBar.add(casts, 7, CastBar.TYPELESS, CastBar.FLAG_DODGE, 50, 36, null);
        }
        assertEquals(1, casts.size());
    }
}
