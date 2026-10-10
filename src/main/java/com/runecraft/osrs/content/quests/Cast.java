package com.runecraft.osrs.content.quests;

/**
 * Helper for the later quest groups: declares an NPC from a skin tone and a clothing colour,
 * deriving the trouser colour and a unique accent so no two NPCs look identical.
 */
final class Cast {
    static final int PALE = 0xE8C8B0;
    static final int FAIR = 0xE0B89A;
    static final int TAN = 0xC8946A;
    static final int BROWN = 0xA86A45;
    static final int DARK = 0x7A4A30;
    static final int GOBLIN = 0x9AA890;
    static final int GREEN_GOBLIN = 0x6A9A4A;
    static final int TROLL = 0x8A8A78;
    static final int GHOST = 0xD8E8F0;
    static final int VAMPYRE = 0xC8C8D0;
    static final int FROG = 0x5AA04A;

    private static int serial;

    private Cast() {
    }

    /** An NPC wearing {@code top}; trousers are a darker shade and the accent is unique per NPC. */
    static void npc(QuestBook b, String id, String name, String place, int dx, int dz, int skin, int top) {
        serial++;
        int bottom = darken(top);
        int accent = (((serial * 53) & 0x7F) + 0x40) << 16 | (((serial * 29) & 0x7F) + 0x30) << 8 | (serial & 0xFF);
        b.npc(id, name, place, dx, dz, skin, top, bottom, accent);
    }

    private static int darken(int rgb) {
        int r = ((rgb >> 16) & 0xFF) * 2 / 3;
        int g = ((rgb >> 8) & 0xFF) * 2 / 3;
        int bl = (rgb & 0xFF) * 2 / 3;
        return r << 16 | g << 8 | bl;
    }
}
