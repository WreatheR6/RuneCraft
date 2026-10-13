package com.runecraft.osrs.content.bestiary;

import com.runecraft.osrs.content.CombatProfile;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.model.ModelSpec;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LookalikesTest {
    private static String shape(ModelSpec m) {
        StringBuilder sb = new StringBuilder();
        for (ModelSpec.Part p : m.parts()) {
            sb.append(p.name).append(p.role).append(p.boxes).append(';');
        }
        return sb.toString();
    }

    @Test
    void variantsCopyTheirBaseModel() {
        for (ModelSpec.Variant v : BestiaryLookalikes.variants()) {
            ModelSpec copy = ContentRegistry.models().get(v.id());
            ModelSpec base = ContentRegistry.models().get(v.base());
            assertNotNull(copy, v.id());
            assertNotNull(base, v.base());
            assertEquals(shape(base), shape(copy), v.id() + " should have " + v.base() + "'s shape");
            assertEquals(base.scale() * v.scale(), copy.scale(), 1e-4, v.id());
        }
        ModelSpec bull = ContentRegistry.models().get("quest_bull");
        assertNotEquals(ContentRegistry.models().get("cow").parts().get(0).color, bull.parts().get(0).color, "the bull is recoloured");
    }

    @Test
    void sculptedCreaturesNoLongerUseTheOutfitFallback() {
        List<CombatProfile<?>> all = new ArrayList<>();
        all.addAll(ContentRegistry.monsters());
        all.addAll(ContentRegistry.bosses());
        List<String> ids = List.of("tormented_demon", "gwd_gorak", "gwd_werewolf", "minotaur", "sulphur_nagua", "frost_nagua",
                "animated_rune_armour", "gwd_spiritual_warrior_armadyl", "gwd_ogre", "nmz_elvarg", "imp_champion", "quest_gryphon");
        for (CombatProfile<?> c : all) {
            if (!ids.contains(c.id)) {
                continue;
            }
            ModelSpec m = ContentRegistry.models().get(c.model != null ? c.model : c.id);
            assertNotNull(m, c.id);
            int[] s = c.skin != null && c.skin.length >= 4 ? c.skin : new int[]{0xC8A080, 0x6A4A2A, 0x4A3A2A, 0x202020};
            ModelSpec person = Fallbacks.person(m.id(), (c.id + " " + c.name).toLowerCase(Locale.ROOT), s, c.height / 1.95f);
            assertNotEquals(shape(person), shape(m), c.id + " still renders as an outfit biped");
        }
        assertTrue(ContentRegistry.models().get("kalphite").parts().size() < 50, "kalphite re-sculpt is in place");
    }
}
