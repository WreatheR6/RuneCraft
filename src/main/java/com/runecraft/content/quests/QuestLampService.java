package com.runecraft.content.quests;

import com.runecraft.content.systems.SysNet;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.quests.QuestLamps;
import com.runecraft.osrs.content.quests.QuestLamps.Grant;
import com.runecraft.osrs.content.quests.QuestLamps.Kind;
import com.runecraft.osrs.content.quests.QuestLamps.Lamp;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.player.Players;
import com.runecraft.quest.QuestApi;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;

/**
 * Quest lamps, tomes and skill choices ({@link QuestLamps}): completing a quest hands them out, rubbing a lamp (or reading a
 * tome) opens the skill choice, and the quest's NPC re-offers an unmade choice or gives back a lost lamp. Uses left on a
 * combat lamp or a tome are kept on the item; the player's grants and uses taken are kept in {@link QuestsState} so a lamp
 * is never reclaimed twice.
 */
public final class QuestLampService {
    /** The systems screen the choice opens on. */
    static final String SCREEN = "questlamp";
    /** Uses left on a multi-use item (combat lamp wishes, tome chapters); absent means all of them. */
    static final String USES_LEFT = "runecraft_lamp_uses";

    private QuestLampService() {
    }

    static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            Lamp lamp = QuestLamps.get(Inv.idOf(stack));
            if (lamp == null || lamp.kind() == Kind.CHOICE) {
                return InteractionResultHolder.pass(stack);
            }
            if (player instanceof ServerPlayer p && !world.isClientSide()) {
                open(p, Inv.idOf(stack));
            }
            return InteractionResultHolder.sidedSuccess(stack, world.isClientSide());
        });
        SysNet.onAction(SCREEN, (p, action, fields) -> choose(p, action));
    }

    // ------------------------------------------------------------------ bookkeeping

    private static String grantKey(Grant g) {
        return "lamp:" + g.quest() + ":" + g.lamp();
    }

    private static String usedKey(String lamp) {
        return "lampused:" + lamp;
    }

    /** How many items (or choices) a grant handed this player. */
    public static int granted(ServerPlayer p, Grant g) {
        return QuestsState.get(p).kills(p.getUUID(), grantKey(g));
    }

    /** Uses the player has taken of a lamp, tome or choice. */
    public static int used(ServerPlayer p, String lamp) {
        return QuestsState.get(p).kills(p.getUUID(), usedKey(lamp));
    }

    /** Uses of a lamp the player's grants still owe them (choices not yet made; lamps not yet rubbed). */
    public static int owed(ServerPlayer p, Lamp l) {
        return QuestLamps.owedUses(l.id(), g -> granted(p, g), used(p, l.id()));
    }

    /**
     * The choice a player must make before this one ({@link Grant#needsChoice()}: Duke trains only players Daero trained), or
     * null when nothing blocks it. A player who completed the granting quest before the choices existed was trained with fixed
     * XP then, so the need counts as met.
     */
    public static Lamp blockedBy(ServerPlayer p, Lamp l) {
        for (Grant g : QuestLamps.grants()) {
            if (g.lamp().equals(l.id()) && !QuestLamps.needMet(g, need -> used(p, need) > 0, need -> completedWithoutGrant(p, need))) {
                return QuestLamps.get(g.needsChoice());
            }
        }
        return null;
    }

    /** Whether the player completed a grant's quest without receiving the grant (before the lamps existed). */
    private static boolean completedWithoutGrant(ServerPlayer p, Grant g) {
        return QuestApi.isComplete(p, g.quest()) && !QuestsState.get(p).hasFlag(p.getUUID(), grantKey(g));
    }

    /** Items of a lamp the player carries or banks (an old reward item that now works as it counts too). */
    static int held(ServerPlayer p, String lamp) {
        int n = Inv.count(p, lamp) + bankCount(p, lamp);
        if ("lamp_of_knowledge".equals(lamp)) {
            n += Inv.count(p, "book_of_knowledge") + bankCount(p, "book_of_knowledge");
        }
        return n;
    }

    private static int bankCount(ServerPlayer p, String id) {
        long n = 0;
        for (com.runecraft.osrs.bank.BankModel.Entry e : Players.data(p).bank().entries()) {
            if (e.item.equals(id)) {
                n += e.count;
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, n);
    }

    // ------------------------------------------------------------------ granting

    /**
     * Hands out what completing {@code quest} gives: its lamps and tomes go into the inventory, a choice opens straight away
     * (and stays with the quest's NPC until made). Each grant is made once per player.
     */
    public static void grant(ServerPlayer p, String quest) {
        QuestsState st = QuestsState.get(p);
        UUID u = p.getUUID();
        for (Grant g : QuestLamps.grants(quest)) {
            String key = grantKey(g);
            if (st.hasFlag(u, key)) {
                continue;
            }
            st.setFlag(u, key);
            int n = QuestLamps.count(g, other -> {
                for (Grant og : QuestLamps.grants(other)) {
                    if (og.lamp().equals(g.lamp()) && granted(p, og) > 0) {
                        return true;
                    }
                }
                return false;
            });
            if (n <= 0) {
                continue;
            }
            st.setCount(u, key, n);
            Lamp l = QuestLamps.get(g.lamp());
            if (l.kind() == Kind.CHOICE) {
                Msg.action(p, "Quest reward: " + l.name() + ", " + choices(l, n) + l.describe() + ".");
                Lamp need = blockedBy(p, l);
                if (need != null) {
                    Msg.game(p, "You can claim it" + (l.npc() != null ? " from " + npcName(l.npc()) : "") + " once you have accepted "
                            + need.name() + ".");
                } else {
                    if (l.npc() != null) {
                        Msg.game(p, "If you don't choose now, " + npcName(l.npc()) + " will offer it again later.");
                    }
                    open(p, l.id());
                }
            } else {
                Inv.give(p, l.id(), n);
                Msg.action(p, "Quest reward: " + (n > 1 ? n + " x " : "") + l.name() + " (" + l.describe()
                        + (l.uses() > 1 ? ", " + l.uses() + (l.kind() == Kind.TOME ? " chapters" : " wishes") : "")
                        + "). Use it to choose the skill.");
            }
        }
        ServerNetworking.sync(p);
    }

    private static String choices(Lamp l, int n) {
        int total = n * l.uses();
        return total == 1 ? "" : total + " x ";
    }

    private static String npcName(String npc) {
        var d = ContentRegistry.npc(npc);
        return d == null ? npc : d.name();
    }

    // ------------------------------------------------------------------ the choice

    /** The held stack of a lamp (the old reward item counts for the lamp of knowledge), main hand first; null if none. */
    static ItemStack heldStack(ServerPlayer p, String lamp) {
        ItemStack main = p.getMainHandItem();
        if (!main.isEmpty() && lamp.equals(QuestLamps.canonical(Inv.idOf(main)))) {
            return main;
        }
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (!s.isEmpty() && lamp.equals(QuestLamps.canonical(Inv.idOf(s)))) {
                return s;
            }
        }
        return null;
    }

    /**
     * Whether a held lamp or tome may be used: a quest's lamp works only while its quest still owes the player a use, so one
     * picked up from another player, or reclaimed while the first still lay on the floor, gives nothing. An old reward item
     * from before the lamps (no grant to count) still works; the museum lamps aren't a quest's. Tells the player when not.
     */
    static boolean yours(ServerPlayer p, Lamp l, ItemStack stack) {
        boolean legacy = QuestLamps.legacyItem(Inv.idOf(stack)) && !anyGrantMade(p, l);
        if (!QuestLamps.granted(l.id()) || legacy || owed(p, l) > 0) {
            return true;
        }
        Msg.game(p, "Nothing happens: this " + l.name().toLowerCase(Locale.ROOT) + " wasn't given to you.");
        return false;
    }

    /** Uses left on a held item: the wishes or chapters not yet taken. */
    static int usesLeft(ItemStack stack, Lamp l) {
        var tag = StackData.get(stack);
        return tag.contains(USES_LEFT) ? Math.max(1, Math.min(l.uses(), tag.getInt(USES_LEFT))) : l.uses();
    }

    /** Opens the skill choice of a lamp the player holds, or of a choice they are owed. */
    public static void open(ServerPlayer p, String id) {
        Lamp l = QuestLamps.get(id);
        if (l == null) {
            return;
        }
        int left;
        if (l.kind() == Kind.CHOICE) {
            left = owed(p, l);
            if (left <= 0) {
                return;
            }
            Lamp need = blockedBy(p, l);
            if (need != null) {
                Msg.game(p, "You must accept " + need.name() + " first.");
                return;
            }
        } else {
            ItemStack stack = heldStack(p, l.id());
            if (stack == null || !yours(p, l, stack)) {
                return;
            }
            left = usesLeft(stack, l);
        }
        SysNet.Screen s = new SysNet.Screen(SCREEN, l.name());
        if (!l.packages().isEmpty()) {
            s.line("Choose which skills to focus your training on:");
            for (int i = 0; i < l.packages().size(); i++) {
                QuestLamps.Package pk = l.packages().get(i);
                StringBuilder tip = new StringBuilder();
                pk.xp().forEach((sk, xp) -> tip.append(tip.length() == 0 ? "" : ", ").append(String.format(Locale.ROOT, "%,d", xp))
                        .append(' ').append(sk.displayName()));
                s.button(l.id() + ":pkg" + i, pk.label(), tip.toString(), true);
            }
        } else {
            s.line("Choose a skill to gain " + String.format(Locale.ROOT, "%,d", l.xp()) + " XP"
                    + (l.minLevel() > 1 ? " (level " + l.minLevel() + " or above)" : "") + ".");
            if (left > 1) {
                s.line((l.kind() == Kind.TOME ? "Chapters" : l.kind() == Kind.CHOICE ? "Choices" : "Wishes") + " left: " + left, 0xFFD700);
            }
            for (Skill sk : Skill.values()) {
                if (l.skills().contains(sk)) {
                    int level = Players.data(p).skills().level(sk);
                    s.button(l.id() + ":" + sk.name(), sk.displayName(), level < l.minLevel() ? "Needs level " + l.minLevel() : "",
                            level >= l.minLevel());
                }
            }
        }
        s.send(p);
    }

    /**
     * A button of the choice ({@code "<lamp>:<SKILL>"} or {@code "<lamp>:pkg<n>"}): checks the lamp is held (or the choice owed)
     * and the skill's level, takes one use and grants the XP. Returns whether XP was granted.
     */
    public static boolean choose(ServerPlayer p, String action) {
        int colon = action.lastIndexOf(':');
        if (colon <= 0) {
            return false;
        }
        Lamp l = QuestLamps.get(action.substring(0, colon));
        if (l == null) {
            return false;
        }
        String pick = action.substring(colon + 1);
        Skill skill = null;
        int pkg = -1;
        try {
            if (pick.startsWith("pkg")) {
                pkg = Integer.parseInt(pick.substring(3));
            } else {
                skill = Skill.valueOf(pick);
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        Map<Skill, Integer> xp = QuestLamps.xpOf(l, skill, pkg);
        if (xp.isEmpty()) {
            return false;
        }
        if (skill != null && Players.data(p).skills().level(skill) < l.minLevel()) {
            Msg.game(p, "You need at least level " + l.minLevel() + " " + skill.displayName() + " to put this experience there.");
            return false;
        }
        ItemStack stack = null;
        if (l.kind() == Kind.CHOICE) {
            if (owed(p, l) <= 0) {
                return false;
            }
            Lamp need = blockedBy(p, l);
            if (need != null) {
                Msg.game(p, "You must accept " + need.name() + " first.");
                return false;
            }
        } else {
            stack = heldStack(p, l.id());
            if (stack == null) {
                return false;
            }
            if (!yours(p, l, stack)) {
                return false;
            }
        }
        // take the use first, then the XP
        int left = 0;
        if (stack != null) {
            left = usesLeft(stack, l) - 1;
            if (left <= 0) {
                stack.shrink(1);
            } else {
                int keep = left;
                StackData.update(stack, tag -> tag.putInt(USES_LEFT, keep));
            }
        }
        QuestsState.get(p).addKill(p.getUUID(), usedKey(l.id()));
        xp.forEach((sk, n) -> XpService.add(p, sk, n));
        StringBuilder got = new StringBuilder();
        xp.forEach((sk, n) -> got.append(got.length() == 0 ? "" : " and ").append(String.format(Locale.ROOT, "%,d", n)).append(' ')
                .append(sk.displayName()));
        switch (l.kind()) {
            case TOME -> {
                Msg.action(p, "You read a fascinating chapter and earn experience! You have been awarded " + got + " experience!");
                if (left <= 0) {
                    Msg.game(p, "The book crumbles into a dust cloud of knowledge.");
                }
            }
            case LAMP -> Msg.action(p, "Your wish has been granted! You have been awarded " + got + " experience!");
            case CHOICE -> Msg.action(p, "You have been awarded " + got + " experience!");
        }
        ServerNetworking.sync(p);
        if (l.kind() == Kind.CHOICE ? owed(p, l) > 0 : left > 0) {
            open(p, l.id());
        }
        return true;
    }

    // ------------------------------------------------------------------ the NPCs

    /**
     * Talking to a lamp's NPC after its quest: an unmade choice is offered again, a lost lamp (or a tome with chapters left)
     * is given back. Returns whether the NPC had something of this to say.
     */
    public static boolean talk(ServerPlayer p, String npcId) {
        for (Lamp l : QuestLamps.all()) {
            if (!npcId.equals(l.npc()) || !anyGrantMade(p, l)) {
                continue;
            }
            if (l.kind() == Kind.CHOICE) {
                // a choice still blocked by another (Duke before Daero's training) leaves the NPC's own dialogue alone
                if (owed(p, l) > 0 && blockedBy(p, l) == null) {
                    QuestApi.say(p, npcName(npcId), "You haven't claimed all of your reward yet. Choose wisely.");
                    open(p, l.id());
                    return true;
                }
                continue;
            }
            int owed = owed(p, l);
            int give = QuestLamps.reclaimable(l.id(), g -> granted(p, g), used(p, l.id()), held(p, l.id()));
            if (give > 0 && owed > 0) {
                for (int i = 0; i < give; i++) {
                    ItemStack stack = Inv.stack(l.id(), 1);
                    int uses = Math.min(l.uses(), owed - i * l.uses());
                    if (uses < l.uses() && uses > 0) {
                        StackData.update(stack, tag -> tag.putInt(USES_LEFT, uses));
                    }
                    Inv.give(p, stack);
                }
                QuestApi.say(p, npcName(npcId), "You lost " + (give > 1 ? "some of your rewards" : "your " + l.name().toLowerCase(Locale.ROOT))
                        + "? Here, take " + (give > 1 ? "them" : "it") + " back.");
                ServerNetworking.sync(p);
                return true;
            }
        }
        return false;
    }

    private static boolean anyGrantMade(ServerPlayer p, Lamp l) {
        for (Grant g : QuestLamps.grants()) {
            if (g.lamp().equals(l.id()) && granted(p, g) > 0) {
                return true;
            }
        }
        return false;
    }

    /** Self-test: forgets every grant and use of the player's quest lamps (a walk replays a quest from the start). */
    public static void selfTestReset(ServerPlayer p, String quest) {
        QuestsState st = QuestsState.get(p);
        for (Grant g : QuestLamps.grants(quest)) {
            st.clearFlag(p.getUUID(), grantKey(g));
            st.setCount(p.getUUID(), grantKey(g), 0);
            st.setCount(p.getUUID(), usedKey(g.lamp()), 0);
        }
    }
}
