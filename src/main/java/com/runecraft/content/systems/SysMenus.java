package com.runecraft.content.systems;

import com.runecraft.entity.QuestNpcEntity;
import com.runecraft.osrs.EquipmentStats;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.systems.ClueScrolls;
import com.runecraft.osrs.content.systems.Diaries;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.SpecialAttacks;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Builders and click handlers for the systems module's screens. */
public final class SysMenus {
    private static final int SPELLS_PER_PAGE = 14;

    private SysMenus() {
    }

    public static void registerHandlers() {
        SysNet.onAction("hub", (p, a, f) -> {
            switch (a) {
                case "worn" -> worn(p);
                case "spellbook" -> spellbook(p, 0);
                case "diaries" -> diaries(p);
                case "combat" -> CaMenus.overview(p);
                case "clues" -> clues(p);
                case "ge" -> GeService.open(p, null, null);
                case "hiscores" -> hiscores(p, null);
                case "capes" -> CapeService.open(p);
                case "spec" -> SpecialAttackService.toggle(p);
                default -> {
                }
            }
        });
        SysNet.onAction("worn", SysMenus::wornAction);
        SysNet.onAction("tele", JewelleryService::teleAction);
        SysNet.onAction("spellbook", SysMenus::spellbookAction);
        SysNet.onAction("swap", (p, a, f) -> {
            try {
                Spellbooks.Book b = Spellbooks.Book.valueOf(a);
                if (!SpellService.bookAllowed(p, b, true)) {
                    return;
                }
                Spellbooks.Spell swap = Spellbooks.get("spellbook_swap");
                if (!SpellService.canCast(p, swap, false)) {
                    return;
                }
                SpellService.pay(p, swap);
                SysState.of(p).swapBook = b;
                Msg.action(p, "You may cast one spell from the " + b.displayName + " spellbook.");
                spellbook(p, 0);
            } catch (IllegalArgumentException ignored) {
                // bad book
            }
        });
        SysNet.onAction("diaries", (p, a, f) -> {
            if (a.startsWith("area:")) {
                String[] parts = a.split(":");
                diary(p, parts[1], parts.length > 2 ? Diaries.Tier.valueOf(parts[2]) : Diaries.Tier.EASY);
            }
        });
        SysNet.onAction("diary", (p, a, f) -> {
            String[] parts = a.split(":");
            if (parts.length < 3) {
                if (a.equals("back")) {
                    diaries(p);
                }
                return;
            }
            Diaries.Area area = Diaries.area(parts[1]);
            Diaries.Tier tier = Diaries.Tier.valueOf(parts[2]);
            if (area == null) {
                return;
            }
            if (parts[0].equals("claim")) {
                DiaryService.claim(p, area, tier);
            }
            diary(p, area.id(), tier);
        });
        SysNet.onAction("clue", ClueService::action);
        ClueService.registerPuzzles();
        SysNet.onAction("clues", (p, a, f) -> {
            ClueScrolls.Tier t = ClueScrolls.Tier.byScroll(a);
            if (t != null) {
                ClueService.read(p, t);
            }
        });
        SysNet.onAction("ge", GeService::action);
        SysNet.onAction("capes", CapeService::action);
        SysNet.onAction("hiscores", (p, a, f) -> hiscores(p, a.equals("overall") ? null : Skill.byId(a)));
        SysNet.onAction("lamp", (p, a, f) -> {
            String[] parts = a.split(":");
            if (parts.length == 2) {
                try {
                    DiaryService.rubLamp(p, Diaries.Tier.valueOf(parts[0]), Skill.valueOf(parts[1]));
                } catch (IllegalArgumentException ignored) {
                    // bad value
                }
            }
        });
        SysNet.onAction("npc_contact", (p, a, f) -> contact(p, a));
        SysNet.onAction("casket", (p, a, f) -> {
        });
        SysNet.onAction("statspy", (p, a, f) -> {
        });
    }

    // ---------------- Hub ----------------

    public static void hub(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        PlayerData d = Players.data(p);
        SysNet.Screen s = new SysNet.Screen("hub", "RuneCraft - Adventurer's log");
        SpecialAttacks.Spec spec = SpecialAttackService.weaponSpec(p);
        s.line("Special attack: " + sp.specEnergy + "%" + (spec == null ? " (your weapon has no special)" : " - " + spec.name() + " costs "
                + spec.cost() + "%"), 0xFFD700);
        s.line("Run energy: " + sp.runEnergy / 100 + "%" + (sp.staminaUntil > SysTicks.now() ? " (stamina)" : ""), 0xC0C0FF);
        s.line("Spellbook: " + sp.book.displayName + (sp.autocast.isEmpty() ? "" : " - autocasting " + Spellbooks.get(sp.autocast).name()));
        int diaryDone = sp.diaryDone.size();
        s.line("Achievement diary tasks: " + diaryDone + "/" + Diaries.tasks().size() + "   Quest points: " + d.questPoints());
        s.line("Combat Achievement points: " + CombatAchievementService.points(p) + "/"
                + com.runecraft.osrs.content.systems.CombatAchievements.totalPoints(), 0xE0A060);
        StringBuilder clues = new StringBuilder("Treasure trails completed:");
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            clues.append(' ').append(t.id()).append(' ').append(sp.cluesCompleted.getOrDefault(t, 0));
        }
        s.line(clues.toString(), 0xC0C0C0);
        s.button("worn", "Worn equipment", "Rings, amulets, capes, gloves and blessings.", true);
        s.button("spellbook", "Spellbook", "Ancient, Lunar and Arceuus spells (B).", true);
        s.button("diaries", "Achievement diaries", "", true);
        s.button("combat", "Combat achievements", "Boss tasks, points and Ghommal's rewards.", true);
        s.button("clues", "Clue scrolls", "", true);
        s.button("ge", "Grand Exchange", "View and collect your offers.", true);
        s.button("hiscores", "Hiscores", "", true);
        s.button("capes", "Capes of accomplishment", "", true);
        s.button("spec", "Use special attack", "Toggle your weapon's special attack (V).", spec != null);
        s.send(p);
    }

    // ---------------- Worn equipment ----------------

    public static void worn(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        SysNet.Screen s = new SysNet.Screen("worn", "Worn equipment");
        for (Jewellery.Slot slot : Jewellery.Slot.values()) {
            ItemStack st = sp.worn(slot);
            String label = slot.name().charAt(0) + slot.name().substring(1).toLowerCase(Locale.ROOT);
            s.line(label + ": " + (st.isEmpty() ? "-" : st.getName().getString()), st.isEmpty() ? 0x808080 : 0xFFFFFF);
        }
        EquipmentStats w = SysCombat.wornStats(p);
        s.line("Bonuses: att " + w.stabAttack() + "/" + w.slashAttack() + "/" + w.crushAttack() + "/" + w.magicAttack() + "/" + w.rangedAttack()
                + "  def " + w.stabDefence() + "/" + w.slashDefence() + "/" + w.crushDefence() + "/" + w.magicDefence() + "/" + w.rangedDefence(), 0xA0E0FF);
        s.line("Str " + w.meleeStrength() + "  Rng str " + w.rangedStrength() + "  Mag dmg " + w.magicDamagePercent() + "%  Prayer " + w.prayer()
                + (sp.wornId(Jewellery.Slot.RING).equals("ring_of_recoil") ? "  Recoil: " + sp.recoilCharges : ""), 0xA0E0FF);
        Jewellery.Worn held = JewelleryService.wornDef(p.getMainHandStack());
        s.button("wear", "Wear held item", held == null ? "Hold a ring, amulet, cape, gloves or blessing." : held.name(), held != null);
        for (Jewellery.Slot slot : Jewellery.Slot.values()) {
            ItemStack st = sp.worn(slot);
            if (st.isEmpty()) {
                continue;
            }
            s.button("remove:" + slot.name(), "Remove " + st.getName().getString(), "", true);
            Jewellery.Worn def = JewelleryService.wornDef(st);
            if (def != null && !def.teleports().isEmpty()) {
                s.button("tele:" + slot.name(), "Teleport: " + st.getName().getString(), "", true);
            }
        }
        s.send(p);
    }

    private static void wornAction(ServerPlayerEntity p, String action, Map<String, String> fields) {
        if (action.equals("wear")) {
            JewelleryService.wear(p, Hand.MAIN_HAND);
            worn(p);
        } else if (action.startsWith("remove:")) {
            try {
                JewelleryService.remove(p, Jewellery.Slot.valueOf(action.substring(7)));
            } catch (IllegalArgumentException ignored) {
                // bad slot
            }
            worn(p);
        } else if (action.startsWith("tele:")) {
            JewelleryService.openTeleports(p, "worn:" + action.substring(5));
        }
    }

    // ---------------- Spellbook ----------------

    public static void spellbook(ServerPlayerEntity p, int page) {
        SysState.SysPlayer sp = SysState.of(p);
        PlayerData d = Players.data(p);
        Spellbooks.Book book = sp.swapBook != null ? sp.swapBook : sp.book;
        List<Spellbooks.Spell> spells = SpellService.visible(p);
        int pages = Math.max(1, (spells.size() + SPELLS_PER_PAGE - 1) / SPELLS_PER_PAGE);
        page = Math.max(0, Math.min(page, pages - 1));
        String title = book == Spellbooks.Book.STANDARD ? "Standard spellbook (extra spells)" : book.displayName + " spellbook";
        SysNet.Screen s = new SysNet.Screen("spellbook", title + " - page " + (page + 1) + "/" + pages);
        if (book == Spellbooks.Book.STANDARD) {
            s.line("Combat, teleport and alchemy spells of the standard book are in the K menu.", 0xA0A0A0);
            s.line("Switch spellbooks at the Ancient altar (Jaldraocht Pyramid), Lunar altar (Lunar Isle) or Dark altar (Arceuus).", 0xA0A0A0);
        } else if (!SpellService.bookAllowed(p, book, false)) {
            s.line("You haven't completed the quest for this spellbook.", 0xFF8080);
        }
        s.line("Magic level " + d.skills().currentLevel(Skill.MAGIC) + (sp.autocast.isEmpty() ? "" : "   Autocast: "
                + Spellbooks.get(sp.autocast).name() + " (right-click with a staff)"), 0xFFD700);
        for (int i = page * SPELLS_PER_PAGE; i < Math.min(spells.size(), (page + 1) * SPELLS_PER_PAGE); i++) {
            Spellbooks.Spell sp1 = spells.get(i);
            boolean level = d.skills().currentLevel(Skill.MAGIC) >= sp1.level();
            StringBuilder runes = new StringBuilder();
            sp1.runes().forEach((r, n) -> runes.append(n).append(' ').append(r.replace("_rune", "")).append(", "));
            String tip = sp1.description() + (runes.length() > 0 ? " Runes: " + runes.substring(0, runes.length() - 2) + "." : "")
                    + " XP: " + sp1.xp() + (sp1.maxHit() > 0 ? " Max hit: " + sp1.maxHit() : "");
            String label = (sp.autocast.equals(sp1.id()) ? "> " : "") + sp1.name() + " (" + sp1.level() + ")";
            s.button("cast:" + sp1.id(), label, tip, level);
        }
        if (!sp.autocast.isEmpty()) {
            s.button("noauto", "Disable autocast", "", true);
        }
        if (page > 0) {
            s.button("page:" + (page - 1), "< Previous page", "", true);
        }
        if (page < pages - 1) {
            s.button("page:" + (page + 1), "Next page >", "", true);
        }
        s.send(p);
    }

    private static void spellbookAction(ServerPlayerEntity p, String action, Map<String, String> fields) {
        if (action.startsWith("cast:")) {
            String id = action.substring(5);
            Spellbooks.Spell spell = Spellbooks.get(id);
            SpellService.cast(p, id);
            if (spell != null && spell.isCombat()) {
                spellbook(p, 0);
            }
        } else if (action.equals("noauto")) {
            SpellService.setAutocast(p, null);
            spellbook(p, 0);
        } else if (action.startsWith("page:")) {
            try {
                spellbook(p, Integer.parseInt(action.substring(5)));
            } catch (NumberFormatException ignored) {
                // bad page
            }
        }
    }

    public static void spellbookSwap(ServerPlayerEntity p) {
        SysNet.Screen s = new SysNet.Screen("swap", "Spellbook Swap");
        s.line("Choose a spellbook to cast one spell from.");
        for (Spellbooks.Book b : Spellbooks.Book.values()) {
            if (b != Spellbooks.Book.LUNAR) {
                s.button(b.name(), b.displayName, "", true);
            }
        }
        s.send(p);
    }

    /** Switches spellbook at an altar. */
    public static void altar(ServerPlayerEntity p, Spellbooks.Book book) {
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.book == book) {
            sp.book = Spellbooks.Book.STANDARD;
            sp.autocast = "";
            Msg.action(p, "You feel a strange drain upon your memory... Your mind returns to the standard spellbook.");
        } else {
            if (!SpellService.bookAllowed(p, book, true)) {
                return;
            }
            sp.book = book;
            sp.autocast = "";
            DiaryService.onActivity(p, "spellbook:" + book.name().toLowerCase(java.util.Locale.ROOT));
            Msg.action(p, switch (book) {
                case ANCIENT -> "You feel a strange wisdom fill your mind... The Ancient Magicks are now yours (press B).";
                case LUNAR -> "Lunar spells activated (press B).";
                case ARCEUUS -> "You feel the power of the Arceuus spellbook (press B).";
                default -> "Your spellbook has changed.";
            });
        }
        SysCombat.dust(p, new org.joml.Vector3f(0.6f, 0.3f, 0.9f), 40);
        SysNet.sendState(p);
    }

    public static void npcContact(ServerPlayerEntity p) {
        SysNet.Screen s = new SysNet.Screen("npc_contact", "NPC Contact");
        s.line("Who would you like to contact?");
        // OSRS: the Dark Mage in the Abyss repairs essence pouches over NPC Contact.
        s.button(com.runecraft.osrs.content.skilling.RunecraftData.DARK_MAGE, "Dark Mage");
        int n = 0;
        for (ClueScrolls.Npc npc : ClueService.npcs()) {
            if (n++ >= 24) {
                break;
            }
            s.button(npc.id(), npc.name());
        }
        s.send(p);
    }

    private static void contact(ServerPlayerEntity p, String npcId) {
        ServerWorld world = Gielinor.world(p.getServer());
        if (world == null) {
            return;
        }
        Spellbooks.Spell spell = Spellbooks.get("npc_contact");
        if (!SpellService.canCast(p, spell, false)) {
            return;
        }
        if (npcId.equals(com.runecraft.osrs.content.skilling.RunecraftData.DARK_MAGE)) {
            SpellService.pay(p, spell);
            Msg.action(p, "You contact the Dark Mage through the astral plane.");
            com.runecraft.content.skilling.EssencePouches.darkMage(p);
            return;
        }
        for (Entity e : world.iterateEntities()) {
            if (e instanceof QuestNpcEntity npc && npc.npcId().equals(npcId)) {
                SpellService.pay(p, spell);
                Msg.action(p, "You contact " + npc.getName().getString() + " through the astral plane.");
                com.runecraft.quest.QuestManager.talk(p, npc);
                return;
            }
        }
        Msg.game(p, "They don't answer. (They need to be somewhere loaded in the world.)");
    }

    public static void statSpy(ServerPlayerEntity p, ServerPlayerEntity other) {
        PlayerData d = Players.data(other);
        SysNet.Screen s = new SysNet.Screen("statspy", "Stat Spy - " + other.getName().getString());
        StringBuilder line = new StringBuilder();
        int i = 0;
        for (Skill sk : Skill.values()) {
            line.append(sk.displayName()).append(' ').append(d.skills().currentLevel(sk)).append('/').append(d.skills().level(sk)).append("   ");
            if (++i % 4 == 0) {
                s.line(line.toString());
                line.setLength(0);
            }
        }
        if (line.length() > 0) {
            s.line(line.toString());
        }
        s.send(p);
    }

    // ---------------- Diaries ----------------

    public static void diaries(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        SysNet.Screen s = new SysNet.Screen("diaries", "Achievement Diaries");
        s.line("Complete tasks around Gielinor to earn rewards. Click an area to see its tasks.", 0xC0C0C0);
        for (Diaries.Area a : Diaries.AREAS) {
            StringBuilder prog = new StringBuilder();
            boolean claim = false;
            for (Diaries.Tier t : Diaries.Tier.values()) {
                prog.append(t.displayName().charAt(0)).append(' ').append(DiaryService.done(sp, a, t)).append('/')
                        .append(Diaries.tasks(a.id(), t).size()).append("  ");
                claim |= DiaryService.claimable(sp, a, t);
            }
            s.button("area:" + a.id(), a.name() + (claim ? " (reward!)" : ""), prog.toString().trim(), true);
        }
        s.send(p);
    }

    public static void diary(ServerPlayerEntity p, String areaId, Diaries.Tier tier) {
        SysState.SysPlayer sp = SysState.of(p);
        Diaries.Area a = Diaries.area(areaId);
        if (a == null) {
            return;
        }
        SysNet.Screen s = new SysNet.Screen("diary", a.name() + " Diary - " + tier.displayName());
        for (Diaries.Task t : Diaries.tasks(a.id(), tier)) {
            boolean done = sp.diaryDone.contains(t.id());
            String progress = t.count() > 1 && !done ? " (" + sp.diaryCounters.getOrDefault(t.id(), 0) + "/" + t.count() + ")" : "";
            s.line((done ? "[x] " : "[ ] ") + t.description() + progress, done ? 0x60E060 : 0xE0E0E0);
        }
        StringBuilder req = new StringBuilder();
        PlayerData pd = Players.data(p);
        Diaries.requirements(a.id(), tier).forEach((sk, lvl) -> req.append(req.length() == 0 ? "" : ", ").append(lvl).append(' ')
                .append(sk.displayName()).append(pd.skills().level(sk) >= lvl ? "" : "*"));
        if (req.length() > 0) {
            s.line("Requires: " + req + " (* = not met; boosts count)", 0xA0A0A0);
        }
        java.util.Set<String> quests = Diaries.questRequirements(a.id(), tier);
        if (!quests.isEmpty()) {
            StringBuilder q = new StringBuilder();
            for (String id : quests) {
                com.runecraft.osrs.QuestInfo info = com.runecraft.osrs.content.ContentRegistry.quest(id);
                q.append(q.length() == 0 ? "" : ", ").append(info == null ? id : info.displayName());
            }
            s.line("Quests: " + q, 0xA0A0A0);
        }
        s.line("Reward: " + a.rewardName() + " " + (tier.ordinal() + 1) + " and an antique lamp (" + tier.lampXp + " XP).", 0xFFD700);
        for (String perk : com.runecraft.osrs.content.systems.DiaryRewards.perks(a.id(), tier.ordinal() + 1)) {
            s.line(" - " + perk, 0xE0C060);
        }
        for (Diaries.Tier t : Diaries.Tier.values()) {
            s.button("tier:" + a.id() + ":" + t.name(), (t == tier ? "> " : "") + t.displayName() + " " + DiaryService.done(sp, a, t) + "/"
                    + Diaries.tasks(a.id(), t).size(), "", true);
        }
        boolean complete = Diaries.tierComplete(sp.diaryDone, a.id(), tier);
        s.button("claim:" + a.id() + ":" + tier.name(), sp.diaryClaimed.contains(a.id() + ":" + tier.name()) ? "Reclaim reward" : "Claim reward",
                complete ? "" : "Complete every task first.", complete);
        s.button("back", "Back", "", true);
        s.send(p);
    }

    public static void lamp(ServerPlayerEntity p, Diaries.Tier tier) {
        SysNet.Screen s = new SysNet.Screen("lamp", "Antique lamp");
        s.line("Choose a skill to gain " + String.format(Locale.ROOT, "%,d", tier.lampXp) + " XP (level " + tier.lampMinLevel + "+).");
        PlayerData d = Players.data(p);
        for (Skill sk : Skill.values()) {
            s.button(tier.name() + ":" + sk.name(), sk.displayName(), "", d.skills().level(sk) >= tier.lampMinLevel);
        }
        s.send(p);
    }

    // ---------------- Clues ----------------

    public static void clues(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        SysNet.Screen s = new SysNet.Screen("clues", "Treasure Trails");
        s.line("Monsters drop clue scrolls; right-click a clue to read it, then dig or talk to people to progress.", 0xC0C0C0);
        for (ClueScrolls.Tier t : ClueScrolls.Tier.values()) {
            boolean carrying = Inv.has(p, t.scrollItem(), 1);
            s.line(t.id() + ": completed " + sp.cluesCompleted.getOrDefault(t, 0) + (carrying ? " - carrying a clue" : ""),
                    carrying ? 0xFFD700 : 0xFFFFFF);
            if (carrying) {
                s.button(t.scrollItem(), "Read " + t.id() + " clue", "", true);
            }
        }
        s.send(p);
    }

    // ---------------- Hiscores ----------------

    public static void hiscores(ServerPlayerEntity p, Skill skill) {
        CapeService.snapshot(p);
        SysNet.Screen s = new SysNet.Screen("hiscores", "Hiscores - " + (skill == null ? "Overall" : skill.displayName()));
        for (String l : CapeService.ranking(p.getServer(), skill, 12)) {
            s.line(l);
        }
        s.button("overall", "Overall", "", true);
        for (Skill sk : Skill.values()) {
            s.button(sk.id(), sk.displayName(), "", true);
        }
        s.send(p);
    }
}
