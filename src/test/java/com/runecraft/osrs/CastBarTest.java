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
}
