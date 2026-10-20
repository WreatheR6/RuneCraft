package com.runecraft.content.systems;

import com.runecraft.action.ActionManager;
import com.runecraft.combat.CombatHandler;
import com.runecraft.combat.PoisonService;
import com.runecraft.network.ServerNetworking;
import com.runecraft.osrs.AttackType;
import com.runecraft.osrs.Chance;
import com.runecraft.osrs.CombatFormulas;
import com.runecraft.osrs.ItemCatalog;
import com.runecraft.osrs.ItemDef;
import com.runecraft.osrs.MenuType;
import com.runecraft.osrs.QuestInfo;
import com.runecraft.osrs.Recipe;
import com.runecraft.osrs.Recipes;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.ContentRegistry;
import com.runecraft.osrs.content.systems.BoltEffects;
import com.runecraft.osrs.content.systems.Jewellery;
import com.runecraft.osrs.content.systems.Spellbooks;
import com.runecraft.osrs.content.systems.Spellbooks.Book;
import com.runecraft.osrs.content.systems.Spellbooks.Effect;
import com.runecraft.osrs.content.systems.Spellbooks.Spell;
import com.runecraft.osrs.data.Bone;
import com.runecraft.osrs.data.Herb;
import com.runecraft.player.PlayerData;
import com.runecraft.player.Players;
import com.runecraft.registry.ModItems;
import com.runecraft.skill.Stun;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.Gielinor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Casting spells from the Ancient, Lunar and Arceuus spellbooks and the extra standard spells. */
public final class SpellService {
    public static final String REANIMATED_TAG = "runecraft_reanimated:";
    private static final double RANGE = 20.0;
    private static final String[] ELEMENTS = {"air", "water", "earth", "fire"};
    private static final Map<String, String[]> COMBO = new LinkedHashMap<>();

    static {
        COMBO.put("mist_rune", new String[]{"air", "water"});
        COMBO.put("dust_rune", new String[]{"air", "earth"});
        COMBO.put("smoke_rune", new String[]{"air", "fire"});
        COMBO.put("mud_rune", new String[]{"water", "earth"});
        COMBO.put("steam_rune", new String[]{"water", "fire"});
        COMBO.put("lava_rune", new String[]{"earth", "fire"});
        // An aether rune counts as one cosmic and one soul rune (OSRS Wiki, Aether rune).
        COMBO.put("aether_rune", new String[]{"cosmic", "soul"});
    }

    /** Whether a combination rune can stand in for this base rune (the four elements, cosmic and soul). */
    private static boolean combinable(String base) {
        for (String[] parts : COMBO.values()) {
            if (parts[0].equals(base) || parts[1].equals(base)) {
                return true;
            }
        }
        return false;
    }

    private SpellService() {
    }

    // ---------------- Runes ----------------

    /** Whether the wielded staff (or off-hand tome) provides unlimited runes of an element. */
    static boolean staffProvides(ServerPlayer p, String element) {
        String id = p.getMainHandItem().isEmpty() ? "" : Inv.idOf(p.getMainHandItem());
        String off = p.getOffhandItem().isEmpty() ? "" : Inv.idOf(p.getOffhandItem());
        if (off.equals("tome_of_" + element)) {
            return true;
        }
        if (id.equals("staff_of_" + element) || (id.contains(element) && (id.contains("staff") || id.contains("wand")))) {
            return true;
        }
        if (element.equals("water") && id.equals("kodai_wand")) {
            return true;
        }
        for (Map.Entry<String, String[]> c : COMBO.entrySet()) {
            String name = c.getKey().replace("_rune", "");
            if (id.contains(name) && id.contains("staff") && (c.getValue()[0].equals(element) || c.getValue()[1].equals(element))) {
                return true;
            }
        }
        return false;
    }

    /** Items (and counts) to remove for a spell, or null if the player lacks runes. */
    static Map<String, Integer> runePlan(ServerPlayer p, Map<String, Integer> runes) {
        Map<String, Integer> remove = new LinkedHashMap<>();
        Map<String, Integer> elementNeed = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : runes.entrySet()) {
            String rune = e.getKey();
            String element = rune.replace("_rune", "");
            boolean elemental = java.util.Arrays.asList(ELEMENTS).contains(element);
            if (elemental && staffProvides(p, element)) {
                continue;
            }
            int have = Inv.count(p, rune);
            int use = Math.min(have, e.getValue());
            if (use > 0) {
                remove.merge(rune, use, Integer::sum);
            }
            if (use < e.getValue()) {
                if (!combinable(element)) {
                    return null;
                }
                elementNeed.put(element, e.getValue() - use);
            }
        }
        for (Map.Entry<String, String[]> c : COMBO.entrySet()) {
            int need = 0;
            for (String el : c.getValue()) {
                need = Math.max(need, elementNeed.getOrDefault(el, 0));
            }
            if (need <= 0) {
                continue;
            }
            int use = Math.min(need, Inv.count(p, c.getKey()));
            if (use > 0) {
                remove.merge(c.getKey(), use, Integer::sum);
                for (String el : c.getValue()) {
                    elementNeed.computeIfPresent(el, (k, v) -> Math.max(0, v - use));
                }
            }
        }
        for (int left : elementNeed.values()) {
            if (left > 0) {
                return null;
            }
        }
        return remove;
    }

    /**
     * One blighted spell sack instead of the runes, when the player carries the sack for this spell where sacks work (the
     * Wilderness, Castle Wars, Soul Wars; see {@link com.runecraft.osrs.data.BlightedSacks}), or null.
     */
    public static Map<String, Integer> blightedPlan(ServerPlayer p, Spell s) {
        String sack = com.runecraft.osrs.data.BlightedSacks.sackFor(s.id());
        if (sack == null || !Inv.has(p, sack, 1)) {
            return null;
        }
        com.runecraft.content.minigames.MinigameSession session = com.runecraft.content.minigames.MinigameManager.session(p);
        return com.runecraft.osrs.data.BlightedSacks.usable(sack, com.runecraft.world.Gielinor.wildernessLevel(p),
                session == null ? null : session.game.id()) ? Map.of(sack, 1) : null;
    }

    static void consume(ServerPlayer p, Map<String, Integer> plan) {
        plan.forEach((item, n) -> Inv.remove(p, item, n));
    }

    // ---------------- Requirements ----------------

    /** Whether the player may use a spellbook (quest requirement looked up at runtime). */
    public static boolean bookAllowed(ServerPlayer p, Book book, boolean warn) {
        if (book.quest == null) {
            return true;
        }
        QuestInfo q = ContentRegistry.quest(book.quest);
        if (q == null) {
            SysState.SysPlayer sp = SysState.of(p);
            if (warn && !sp.warnedBook) {
                sp.warnedBook = true;
                Msg.game(p, "(The quest '" + book.quest + "' isn't in this world yet, so the " + book.displayName
                        + " spellbook is available without it.)");
            }
            return true;
        }
        if (!Players.data(p).questComplete(q)) {
            if (warn) {
                Msg.game(p, "You need to complete " + q.displayName() + " to use the " + book.displayName + " spellbook.");
            }
            return false;
        }
        return true;
    }

    /** The spell book a spell may currently be cast from. */
    static boolean inBook(SysState.SysPlayer sp, Spell s) {
        Book current = sp.swapBook != null ? sp.swapBook : sp.book;
        return s.book() == current;
    }

    static boolean canCast(ServerPlayer p, Spell s, boolean quiet) {
        SysState.SysPlayer sp = SysState.of(p);
        if (!inBook(sp, s)) {
            if (!quiet) {
                Msg.game(p, "That spell is in the " + s.book().displayName + " spellbook.");
            }
            return false;
        }
        if (!bookAllowed(p, s.book(), !quiet)) {
            return false;
        }
        if (Players.data(p).skills().currentLevel(Skill.MAGIC) < s.level()) {
            if (!quiet) {
                Msg.game(p, "You need a Magic level of " + s.level() + " to cast " + s.name() + ".");
            }
            return false;
        }
        if (s.item() != null && !Inv.idOf(p.getMainHandItem()).startsWith(s.item())) {
            if (!quiet) {
                ItemDef d = ItemCatalog.get(s.item());
                Msg.game(p, "You need to wield " + (d == null ? s.item().replace('_', ' ') : Msg.article(d.name())) + " to cast this spell.");
            }
            return false;
        }
        if (blightedPlan(p, s) == null && runePlan(p, s.runes()) == null) {
            if (!quiet) {
                Msg.game(p, "You do not have enough runes to cast this spell.");
            }
            return false;
        }
        return true;
    }

    /** Takes the runes and awards the base XP for a successful cast. */
    static void pay(ServerPlayer p, Spell s) {
        Map<String, Integer> plan = blightedPlan(p, s);
        if (plan == null) {
            plan = runePlan(p, s.runes());
        }
        if (plan != null) {
            consume(p, plan);
        }
        XpService.add(p, Skill.MAGIC, s.xp());
        SysState.SysPlayer sp = SysState.of(p);
        sp.spellsCast.add(s.id());
        if (sp.swapBook != null) {
            sp.swapBook = null;
            Msg.game(p, "Your spellbook returns to normal.");
        }
        DiaryService.onCast(p, s.id());
    }

    // ---------------- Entry points ----------------

    /** Clicking a spell in the spellbook screen. */
    public static void cast(ServerPlayer p, String spellId) {
        Spell s = Spellbooks.get(spellId);
        if (s == null || !Stun.check(p)) {
            return;
        }
        if (s.id().equals("enchant_crossbow_bolt")) {
            // Each bolt type has its own level and runes (checked when the bolts are chosen).
            if (!inBook(SysState.of(p), s)) {
                Msg.game(p, "That spell is in the " + s.book().displayName + " spellbook.");
            } else {
                enchantBolts(p);
            }
            return;
        }
        if (!canCast(p, s, false)) {
            return;
        }
        switch (s.kind()) {
            case COMBAT -> {
                SysState.SysPlayer sp = SysState.of(p);
                sp.autocast = s.id();
                Players.data(p).setAutocast(null);
                ServerNetworking.sync(p);
                Msg.action(p, "Autocasting " + s.name() + ". Right-click with a staff to cast it.");
                SysNet.sendState(p);
            }
            case TELEPORT -> teleport(p, s);
            case UTILITY -> utility(p, s);
        }
    }

    public static void setAutocast(ServerPlayer p, String spellId) {
        SysState.SysPlayer sp = SysState.of(p);
        sp.autocast = spellId == null ? "" : spellId;
        Msg.action(p, spellId == null ? "Autocast disabled." : "Autocasting " + Spellbooks.get(spellId).name() + ".");
        SysNet.sendState(p);
    }

    /** Staff right-click while one of this module's combat spells is set to autocast. Returns true if handled. */
    public static boolean staffCast(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.autocast.isEmpty() || Players.data(p).autocast() != null) {
            return false;
        }
        Spell s = Spellbooks.get(sp.autocast);
        if (s == null || !s.isCombat()) {
            sp.autocast = "";
            return false;
        }
        if (!inBook(sp, s)) {
            Msg.game(p, "You can't autocast " + s.name() + " from your current spellbook.");
            sp.autocast = "";
            SysNet.sendState(p);
            return true;
        }
        castCombat(p, s, SysCombat.lookTarget(p, RANGE));
        return true;
    }

    // ---------------- Combat ----------------

    /** Auto-retaliate with this module's autocast spell (staff wielded); returns false if none is set. */
    public static boolean retaliate(ServerPlayer p, LivingEntity target) {
        SysState.SysPlayer sp = SysState.of(p);
        Spell s = sp.autocast.isEmpty() ? null : Spellbooks.get(sp.autocast);
        if (s == null || !s.isCombat() || !inBook(sp, s)) {
            return false;
        }
        castCombat(p, s, target);
        return true;
    }

    /** The combat spell whose hit the attack hooks are judging right now (cast by hand or autocast), or null. */
    private static Spell casting;

    /** The id of the combat spell being cast while the attack hooks run (e.g. ToB's Athanatos checks for Smoke spells), or null. */
    public static String castingSpellId() {
        return casting == null ? null : casting.id();
    }

    static void castCombat(ServerPlayer p, Spell s, LivingEntity target) {
        PlayerData data = Players.data(p);
        if (target == null) {
            Msg.action(p, "No target.");
            return;
        }
        if (target instanceof ServerPlayer other && !CombatHandler.canAttackPlayer(p, other)
                || target instanceof com.runecraft.entity.QuestNpcEntity) {
            Msg.game(p, "You can't attack that.");
            return;
        }
        if (!CombatHandler.singleWayAllows(p, target)) {
            return;
        }
        if (s.effect() == Effect.TELEBLOCK && !(target instanceof ServerPlayer)) {
            Msg.game(p, "You can only cast Tele Block on other players.");
            return;
        }
        long now = ActionManager.mcTick();
        if (now - data.lastAttackTick < 5L * ActionManager.MC_TICKS_PER_GAME_TICK) {
            return;
        }
        if (s.effect() == Effect.UNDEAD && !SysCombat.isUndead(target)) {
            Msg.game(p, "This spell only affects undead.");
            return;
        }
        if (s.effect() == Effect.DEMON && !SysCombat.isDemon(target)) {
            Msg.game(p, "This spell only affects demons.");
            return;
        }
        if (!canCast(p, s, false)) {
            return;
        }
        data.lastAttackTick = now;
        pay(p, s);
        if (target instanceof net.minecraft.world.entity.player.Player) {
            // Combat spells cast on another player (the God spells task of the hard Wilderness diary).
            com.runecraft.content.GameEvents.fireActivity(p, "cast_on_player:" + s.id());
        }
        p.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel world = (ServerLevel) p.level();
        SysCombat.trail(world, p.getEyePosition(), target.position().add(0, target.getBbHeight() / 2, 0), colour(s));
        world.playSound(null, p.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.5f, 1.2f);
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        // Area spells only hit more than one target in multicombat areas.
        if (s.area() && com.runecraft.combat.CombatZones.isMulti(target)) {
            for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(1.6),
                    o -> o != target && o != p && o.isAlive() && SysCombat.canAttack(p, o))) {
                if (targets.size() < 9) {
                    targets.add(e);
                }
            }
        }
        int baseMax = s.maxHit();
        if (s.id().equals("magic_dart")) {
            baseMax = Spellbooks.magicDartMax(data.skills().currentLevel(Skill.MAGIC));
        }
        SysState.SysPlayer sp = SysState.of(p);
        if (s.item() != null && s.item().endsWith("_staff") && s.id().matches("saradomin_strike|claws_of_guthix|flames_of_zamorak")
                && sp.chargeUntil > SysTicks.now()) {
            String god = s.item().replace("_staff", "");
            if (sp.wornId(Jewellery.Slot.CAPE).startsWith(god + "_cape") || sp.wornId(Jewellery.Slot.CAPE).equals("max_cape")) {
                baseMax = 30;
            }
        }
        int totalDealt = 0;
        for (LivingEntity t : targets) {
            double acc = 1.0;
            double dmgMult = 1.0;
            if (s.effect() == Effect.DEMON && sp.markOfDarknessUntil > SysTicks.now()) {
                acc *= 1.25;
                dmgMult *= 1.25;
            }
            // Magic level, prayers, magic damage and gear effects (salve, slayer helmet, void, smoke staff...): PlayerHitCalc.
            com.runecraft.osrs.PlayerHitCalc.Input in = com.runecraft.combat.HitCalcBridge.input(p, t, AttackType.MAGIC, null, 0);
            com.runecraft.combat.HitCalcBridge.spell(in, baseMax, s.book() == Spellbooks.Book.STANDARD);
            long att = (long) (com.runecraft.combat.HitCalcBridge.attackRoll(p, t, in) * acc);
            long def = com.runecraft.combat.HitCalcBridge.defenceRoll(t, in);
            int max = (int) Math.floor(com.runecraft.combat.HitCalcBridge.maxHit(p, t, in) * dmgMult);
            boolean hit = p.getRandom().nextDouble() < Chance.hitChance(att, def);
            if (!hit) {
                world.sendParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + 1, t.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                t.setLastHurtByMob(p);
                CombatHandler.applyDamage(p, t, 0);
                continue;
            }
            int damage = max > 0 ? p.getRandom().nextInt(max + 1) : 0;
            casting = s;
            try {
                damage = CombatHandler.hookAfterRoll(p, t, AttackType.MAGIC, damage, max);
            } finally {
                casting = null;
            }
            int dealt = max > 0 ? SysCombat.hit(p, t, AttackType.MAGIC, damage) : 0;
            if (max == 0) {
                t.setLastHurtByMob(p);
            }
            CombatHandler.hookAfterHit(p, t, AttackType.MAGIC, dealt);
            totalDealt += dealt;
            if (dealt > 0) {
                XpService.addTenths(p, Skill.MAGIC, dealt * 20);
                XpService.addTenths(p, Skill.HITPOINTS, CombatFormulas.hitpointsXpTenths(dealt));
            }
            applyEffect(p, s, t, dealt);
        }
        if (s.effect() == Effect.HEAL && totalDealt > 0) {
            p.heal(totalDealt * s.param() / 100f);
        }
    }

    private static void applyEffect(ServerPlayer p, Spell s, LivingEntity t, int dealt) {
        switch (s.effect()) {
            case POISON -> {
                if (dealt > 0 && p.getRandom().nextInt(4) == 0) {
                    SysCombat.poison(t, s.param());
                }
            }
            case DRAIN_ATTACK -> curse(p, s, t, Skill.ATTACK);
            case DRAIN_DEFENCE -> curse(p, s, t, Skill.DEFENCE);
            case DRAIN_STRENGTH -> curse(p, s, t, Skill.STRENGTH);
            case TELEBLOCK -> {
                if (t instanceof ServerPlayer victim) {
                    com.runecraft.combat.Teleblock.apply(p, victim);
                }
            }
            case DRAIN_MAGIC -> SysCombat.drainStat(t, Skill.MAGIC, Math.max(1, SysCombat.statLevel(t, Skill.MAGIC) * s.param() / 100));
            case DRAIN_PRAYER -> {
                if (t instanceof ServerPlayer victim) {
                    PlayerData vd = Players.data(victim);
                    vd.setPrayerPoints(vd.prayerPoints() - s.param());
                    ServerNetworking.sync(victim);
                }
            }
            // Protect from Magic halves freezes as well as binds (wiki: Freeze).
            case FREEZE -> SysCombat.freeze(t, com.runecraft.osrs.CombatEffects.bindTicks(s.param(),
                    t instanceof ServerPlayer fp && Players.data(fp).isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC)));
            case BIND -> {
                // Ward of Arceuus cuts any bind on the warded player to a single tick.
                if (t instanceof ServerPlayer victim && SysState.of(victim).wardUntil > SysTicks.now()) {
                    SysCombat.freeze(t, Spellbooks.WARDED_BIND_TICKS);
                } else {
                    SysCombat.freeze(t, com.runecraft.osrs.CombatEffects.bindTicks(s.param(),
                            t instanceof ServerPlayer vp && Players.data(vp).isActive(com.runecraft.osrs.Prayer.PROTECT_FROM_MAGIC)));
                }
            }
            default -> {
            }
        }
    }

    /**
     * Confuse / Weaken / Curse / Vulnerability / Enfeeble / Stun (and Shadow spells, god spells): lower a stat by a
     * percentage of the target's base level. Standard-book curses don't stack with an existing drain.
     */
    private static void curse(ServerPlayer p, Spell s, LivingEntity t, Skill skill) {
        int base = SysCombat.baseStatLevel(t, skill);
        int current = SysCombat.drainedStatLevel(t, skill);
        com.runecraft.osrs.CombatEffects.Curse curse = com.runecraft.osrs.CombatEffects.curse(s.id());
        int levels;
        if (curse != null) {
            levels = com.runecraft.osrs.CombatEffects.curseDrain(base, current, curse.percent());
            if (levels <= 0) {
                Msg.game(p, "Your target's " + skill.displayName() + " has already been weakened.");
                return;
            }
        } else {
            levels = Math.max(1, current * s.param() / 100);
        }
        SysCombat.drainStat(t, skill, levels);
    }

    private static Vector3f colour(Spell s) {
        String id = s.id();
        if (id.startsWith("ice") || id.contains("grasp")) {
            return new Vector3f(0.6f, 0.85f, 1.0f);
        }
        if (id.startsWith("blood")) {
            return new Vector3f(0.7f, 0.05f, 0.05f);
        }
        if (id.startsWith("shadow")) {
            return new Vector3f(0.2f, 0.1f, 0.2f);
        }
        if (id.startsWith("smoke")) {
            return new Vector3f(0.5f, 0.5f, 0.5f);
        }
        if (id.contains("saradomin")) {
            return new Vector3f(1.0f, 1.0f, 0.8f);
        }
        if (id.contains("guthix")) {
            return new Vector3f(0.2f, 0.8f, 0.2f);
        }
        if (id.contains("zamorak") || id.contains("demonbane") || id.contains("iban")) {
            return new Vector3f(1.0f, 0.3f, 0.05f);
        }
        return new Vector3f(0.7f, 0.2f, 0.9f);
    }

    // ---------------- Teleports ----------------

    static void teleport(ServerPlayer p, Spell s) {
        boolean home = s.runes().isEmpty();
        if (!SysTeleport.canTeleport(p, SysTeleport.SPELL_WILDERNESS)) {
            return;
        }
        if (home) {
            Msg.action(p, "You begin casting " + s.name() + "...");
            ActionManager.start(p, new com.runecraft.action.Action() {
                int ticks;

                @Override
                public boolean tick(ServerPlayer pl, long gameTick) {
                    ((ServerLevel) pl.level()).sendParticles(ParticleTypes.ENCHANT, pl.getX(), pl.getY() + 1, pl.getZ(), 15, 0.5, 0.8, 0.5, 0.5);
                    if (++ticks >= 16) {
                        SysTeleport.teleport(pl, s.destination(), SysTeleport.SPELL_WILDERNESS);
                        return false;
                    }
                    return true;
                }
            });
            return;
        }
        List<ServerPlayer> group = new ArrayList<>();
        if (Spellbooks.isGroupTeleport(s)) {
            // Tele Group: everyone on the 3x3 square around the caster (one OSRS tile = two blocks) who meets the
            // destination's requirements comes along; the casting cost is the caster's.
            for (ServerPlayer o : nearbyPlayers(p, 3)) {
                if (o != p && o.isAlive() && SysTeleport.canTeleport(o, SysTeleport.SPELL_WILDERNESS)) {
                    group.add(o);
                }
            }
        }
        pay(p, s);
        SysTeleport.teleport(p, s.destination(), SysTeleport.SPELL_WILDERNESS);
        for (ServerPlayer o : group) {
            Msg.game(o, p.getName().getString() + " teleports you along.");
            SysTeleport.teleport(o, s.destination(), SysTeleport.SPELL_WILDERNESS);
        }
    }

    // ---------------- Utility ----------------

    static void utility(ServerPlayer p, Spell s) {
        String id = s.id();
        if (id.startsWith("enchant_")) {
            enchant(p, s, Integer.parseInt(id.substring("enchant_".length())));
            return;
        }
        if (id.startsWith("resurrect_") && !id.equals("resurrect_crops")) {
            thrall(p, s);
            return;
        }
        if (id.endsWith("_reanimation")) {
            reanimate(p, s);
            return;
        }
        if (id.startsWith("charge_") && id.endsWith("_orb")) {
            String element = id.substring("charge_".length(), id.length() - "_orb".length());
            convertOne(p, s, "unpowered_orb", element + "_orb", Skill.MAGIC, 0);
            return;
        }
        SysState.SysPlayer sp = SysState.of(p);
        PlayerData data = Players.data(p);
        long now = SysTicks.now();
        switch (id) {
            case "bones_to_bananas", "bones_to_peaches" -> {
                String fruit = id.endsWith("bananas") ? "banana" : "peach";
                if (ItemCatalog.get(fruit) == null) {
                    Msg.game(p, "There are no " + fruit + "s in this world yet.");
                    return;
                }
                int n = Inv.count(p, "bones") + (id.endsWith("peaches") ? Inv.count(p, "big_bones") : 0);
                if (n == 0) {
                    Msg.game(p, "You aren't holding any bones!");
                    return;
                }
                pay(p, s);
                Inv.remove(p, "bones", 64 * 36);
                if (id.endsWith("peaches")) {
                    Inv.remove(p, "big_bones", 64 * 36);
                }
                Inv.give(p, fruit, n);
                Msg.action(p, "Your bones turn into " + fruit + (fruit.endsWith("h") ? "es" : "s") + ".");
            }
            case "telekinetic_grab" -> {
                ItemEntity item = lookItem(p);
                if (item == null) {
                    Msg.game(p, "Look at an item on the ground to grab it.");
                    return;
                }
                pay(p, s);
                SysCombat.trail((ServerLevel) p.level(), p.getEyePosition(), item.position(), new Vector3f(0.9f, 0.9f, 0.4f));
                item.setPos(p.getX(), p.getY() + 0.5, p.getZ());
                item.setPickUpDelay(0);
            }
            case "charge" -> {
                pay(p, s);
                sp.chargeUntil = now + 700;
                Msg.action(p, "You feel charged with magic power.");
            }
            case "bake_pie" -> bakePies(p, s);
            case "humidify" -> {
                // Vials, jugs and bowls fill with water and clay softens (wiki); buckets are vanilla items here.
                String[][] fills = {{"vial", "vial_of_water"}, {"jug", "jug_of_water"}, {"bowl", "bowl_of_water"}, {"clay", "soft_clay"}};
                int total = 0;
                for (String[] f : fills) {
                    if (ItemCatalog.get(f[1]) != null) {
                        total += Inv.count(p, f[0]);
                    }
                }
                if (total == 0) {
                    Msg.game(p, "You have nothing to fill with water.");
                    return;
                }
                pay(p, s);
                for (String[] f : fills) {
                    int n = ItemCatalog.get(f[1]) == null ? 0 : Inv.count(p, f[0]);
                    if (n > 0) {
                        Inv.remove(p, f[0], n);
                        Inv.give(p, f[1], n);
                    }
                }
                Msg.action(p, "You fill your containers with water.");
            }
            case "house_teleport" -> {
                if (!SysTeleport.canTeleport(p, SysTeleport.SPELL_WILDERNESS)) {
                    return;
                }
                pay(p, s);
                SysTeleport.houseTeleport.accept(p);
            }
            case "geomancy" -> geomancy(p, s);
            case "cure_plant", "fertile_soil", "resurrect_crops" -> farmingSpell(p, s);
            case "monster_examine" -> {
                LivingEntity t = SysCombat.lookTarget(p, 16);
                if (t == null || t instanceof ServerPlayer) {
                    Msg.game(p, "Look at a monster to examine it.");
                    return;
                }
                pay(p, s);
                com.runecraft.combat.MonsterStats st = com.runecraft.combat.MonsterStats.of(t);
                Msg.game(p, st.name() + " (level " + st.combatLevel() + "): " + (int) Math.ceil(t.getHealth()) + "/" + st.hitpoints()
                        + " Hitpoints, max hit " + st.maxHit() + ", " + st.style().name().toLowerCase(Locale.ROOT) + " attacks every "
                        + st.attackSpeed() + " ticks.");
                Msg.game(p, "Attack " + st.attack() + ", Strength " + st.strength() + ", Defence " + st.defence() + ", Magic "
                        + st.magic() + ", Ranged " + st.ranged() + ".");
                com.runecraft.content.GameEvents.fireActivity(p, "monster_examine:"
                        + com.runecraft.osrs.content.systems.Diaries.slug(st.name()));
            }
            case "cure_other" -> {
                LivingEntity t = SysCombat.lookTarget(p, 16);
                if (!(t instanceof ServerPlayer other)) {
                    Msg.game(p, "You can only cast this on another player.");
                    return;
                }
                if (!PoisonService.isPoisoned(other)) {
                    Msg.game(p, other.getName().getString() + " is not poisoned.");
                    return;
                }
                pay(p, s);
                PoisonService.cure(other, false, 0, ActionManager.gameTick());
                Msg.game(other, p.getName().getString() + " has cured you of poison.");
            }
            case "dream" -> dream(p, s);
            case "stat_restore_pot_share", "boost_potion_share" -> potionShare(p, s);
            case "recharge_dragonstone" -> {
                int n = JewelleryService.rechargeAll(p);
                if (n == 0) {
                    Msg.game(p, "You have no dragonstone jewellery that needs recharging.");
                    return;
                }
                pay(p, s);
                SysNet.sendState(p);
                Msg.action(p, "You recharge your dragonstone jewellery.");
            }
            case "cure_me" -> {
                if (!PoisonService.isPoisoned(p)) {
                    Msg.game(p, "You are not poisoned.");
                    return;
                }
                pay(p, s);
                PoisonService.cure(p, false, 0, ActionManager.gameTick());
                Msg.action(p, "You are no longer poisoned.");
            }
            case "cure_group" -> {
                pay(p, s);
                for (ServerPlayer o : nearbyPlayers(p, 8)) {
                    PoisonService.cure(o, false, 0, ActionManager.gameTick());
                    Msg.game(o, "You have been cured of poison.");
                }
            }
            case "hunter_kit" -> {
                pay(p, s);
                Inv.give(p, "bird_snare", 1);
                Inv.give(p, "box_trap", 1);
                Msg.action(p, "A hunter kit appears in your pack.");
            }
            case "stat_spy" -> {
                LivingEntity t = SysCombat.lookTarget(p, 16);
                if (!(t instanceof ServerPlayer other)) {
                    Msg.game(p, "You can only cast this on another player.");
                    return;
                }
                pay(p, s);
                SysMenus.statSpy(p, other);
            }
            case "superglass_make" -> superglass(p, s);
            case "tan_leather" -> tan(p, s);
            case "string_jewellery" -> stringJewellery(p, s);
            case "magic_imbue" -> {
                pay(p, s);
                sp.magicImbueUntil = now + 20;
                Msg.action(p, "You are charged to combine runes!");
            }
            case "plank_make" -> plankMake(p, s);
            case "npc_contact" -> SysMenus.npcContact(p);
            case "energy_transfer", "heal_other", "vengeance_other" -> {
                LivingEntity t = SysCombat.lookTarget(p, 16);
                if (!(t instanceof ServerPlayer other)) {
                    Msg.game(p, "You can only cast this on another player.");
                    return;
                }
                otherPlayer(p, s, other);
            }
            case "vengeance" -> {
                if (data.skills().level(Skill.DEFENCE) < 40) {
                    Msg.game(p, "You need 40 Defence to cast Vengeance.");
                    return;
                }
                if (sp.vengeanceReadyAt > now) {
                    Msg.game(p, "You can only cast vengeance spells every 30 seconds.");
                    return;
                }
                pay(p, s);
                sp.vengeance = true;
                sp.vengeanceReadyAt = now + 50;
                SysCombat.dust(p, new Vector3f(0.8f, 0.2f, 0.2f), 25);
                SysNet.sendState(p);
            }
            case "heal_group" -> {
                List<ServerPlayer> others = nearbyPlayers(p, 8);
                others.remove(p);
                if (others.isEmpty() || p.getHealth() < p.getMaxHealth() * 0.11f) {
                    Msg.game(p, "There is nobody nearby to heal, or you are too weak.");
                    return;
                }
                pay(p, s);
                float give = p.getHealth() * 0.75f;
                p.setHealth(p.getHealth() - give);
                for (ServerPlayer o : others) {
                    o.heal(give / others.size());
                }
            }
            case "spellbook_swap" -> SysMenus.spellbookSwap(p);
            // ---------------- Arceuus ----------------
            case "shadow_veil" -> {
                if (sp.shadowVeilReadyAt > now) {
                    Msg.game(p, "You can only cast Shadow Veil every 30 seconds.");
                    return;
                }
                pay(p, s);
                sp.shadowVeilReadyAt = now + 50;
                sp.shadowVeilUntil = now + data.skills().currentLevel(Skill.MAGIC);
                Msg.action(p, "Your thieving abilities have been enhanced.");
            }
            case "dark_lure" -> {
                LivingEntity t = SysCombat.lookTarget(p, 16);
                if (t == null || t instanceof ServerPlayer) {
                    Msg.game(p, "Look at a monster to lure it.");
                    return;
                }
                pay(p, s);
                if (t instanceof net.minecraft.world.entity.Mob mob) {
                    mob.setTarget(p);
                }
                Msg.action(p, "The monster turns its attention to you.");
            }
            case "mark_of_darkness" -> {
                pay(p, s);
                sp.markOfDarknessUntil = now + data.skills().currentLevel(Skill.MAGIC) * 3L;
                Msg.action(p, "You have placed a Mark of Darkness upon yourself.");
            }
            case "ward_of_arceuus" -> {
                pay(p, s);
                sp.wardUntil = now + data.skills().currentLevel(Skill.MAGIC);
                Msg.action(p, "You place a ward upon yourself.");
            }
            case "death_charge" -> {
                pay(p, s);
                sp.deathCharge = true;
                Msg.action(p, "Upon the death of your next foe, some of your special attack energy will be restored.");
            }
            case "vile_vigour" -> {
                if (sp.runEnergy >= 10000) {
                    Msg.game(p, "Your run energy is already full.");
                    return;
                }
                if (sp.vileVigourReadyAt > now) {
                    Msg.game(p, "You can only cast Vile Vigour every 10 seconds.");
                    return;
                }
                pay(p, s);
                sp.vileVigourReadyAt = now + 17;
                // One prayer point per 1% run energy, only as much as is missing.
                int cost = Spellbooks.vileVigourCost(sp.runEnergy / 100, data.prayerPoints());
                sp.runEnergy = Math.min(10000, sp.runEnergy + cost * 100);
                data.setPrayerPoints(data.prayerPoints() - cost);
                ServerNetworking.sync(p);
                SysNet.sendState(p);
            }
            case "degrime" -> {
                int cleaned = 0;
                double xp = 0;
                for (Herb h : Herb.values()) {
                    if (data.skills().currentLevel(Skill.HERBLORE) < h.cleanLevel()) {
                        continue;
                    }
                    int n = Inv.count(p, h.grimyItem());
                    if (n > 0) {
                        if (cleaned == 0) {
                            pay(p, s);
                        }
                        Inv.remove(p, h.grimyItem(), n);
                        Inv.give(p, h.cleanItem(), n);
                        cleaned += n;
                        xp += h.cleanXp() * n;
                    }
                }
                if (cleaned == 0) {
                    Msg.game(p, "You have no grimy herbs you can clean.");
                    return;
                }
                XpService.add(p, Skill.HERBLORE, xp);
                Msg.action(p, "You clean " + cleaned + " herbs.");
            }
            case "demonic_offering" -> offering(p, s, true);
            case "sinister_offering" -> offering(p, s, false);
            case "lesser_corruption", "greater_corruption" -> {
                pay(p, s);
                sp.corruptionPending = id.startsWith("greater") ? 2 : 1;
                sp.corruptionPendingUntil = now + 100;
                Msg.action(p, "Your next hit on a player may corrupt their prayer.");
            }
            default -> Msg.game(p, "Nothing interesting happens.");
        }
    }

    private static List<ServerPlayer> nearbyPlayers(ServerPlayer p, double r) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer o : ((ServerLevel) p.level()).players()) {
            if (o.distanceToSqr(p) <= r * r) {
                out.add(o);
            }
        }
        return out;
    }

    private static ItemEntity lookItem(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        ItemEntity best = null;
        double bestScore = 0.97;
        for (ItemEntity e : p.level().getEntitiesOfClass(ItemEntity.class, new AABB(eye, eye).inflate(RANGE), x -> true)) {
            Vec3 to = e.position().subtract(eye);
            double dist = to.length();
            if (dist > RANGE || dist < 0.1) {
                continue;
            }
            double dot = to.normalize().dot(look);
            if (dot > bestScore) {
                bestScore = dot;
                best = e;
            }
        }
        return best;
    }

    /**
     * Enchant Crossbow Bolt: enchants up to 10 of the held bolts (or else the best bolts in the inventory the player can
     * enchant), with that bolt type's level, runes and XP.
     */
    private static void enchantBolts(ServerPlayer p) {
        int magic = Players.data(p).skills().currentLevel(Skill.MAGIC);
        String held = p.getMainHandItem().isEmpty() ? "" : Inv.idOf(p.getMainHandItem());
        String from = null;
        BoltEffects.Enchant en = BoltEffects.forPlainBolts(held);
        if (en != null && Inv.has(p, held, 1)) {
            from = held;
        } else {
            en = null;
            for (int i = BoltEffects.ENCHANTS.size() - 1; i >= 0 && from == null; i--) {
                BoltEffects.Enchant e = BoltEffects.ENCHANTS.get(i);
                if (magic < e.level()) {
                    continue;
                }
                for (String b : new String[]{e.dragonBolts(), e.bolts()}) {
                    if (Inv.has(p, b, 1)) {
                        from = b;
                        en = e;
                        break;
                    }
                }
            }
        }
        if (from == null) {
            Msg.game(p, "You have no bolts that you can enchant.");
            return;
        }
        Spell cast = new Spell("enchant_crossbow_bolt", "Enchant Crossbow Bolt", Book.STANDARD, en.level(), en.xp(), en.runes(),
                Spellbooks.Kind.UTILITY, 0, Effect.NONE, 0, false, null, null, "");
        if (!canCast(p, cast, false)) {
            return;
        }
        String to = BoltEffects.enchantedOf(from);
        if (ItemCatalog.get(to) == null) {
            Msg.game(p, "Nothing interesting happens.");
            return;
        }
        int n = Math.min(BoltEffects.BOLTS_PER_CAST, Inv.count(p, from));
        pay(p, cast);
        Inv.remove(p, from, n);
        Inv.give(p, to, n);
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.4, 0.4, 0.4, 0.5);
        Msg.action(p, "The magic of the runes coaxes out the true nature of the gem tips: you enchant " + n + " "
                + ItemCatalog.get(from).name().toLowerCase(Locale.ROOT) + ".");
    }

    private static void enchant(ServerPlayer p, Spell s, int level) {
        String from = null;
        String held = p.getMainHandItem().isEmpty() ? "" : Inv.idOf(p.getMainHandItem());
        if (Jewellery.ENCHANT_LEVEL.getOrDefault(held, 0) == level) {
            from = held;
        } else {
            for (Map.Entry<String, Integer> e : Jewellery.ENCHANT_LEVEL.entrySet()) {
                if (e.getValue() == level && Inv.has(p, e.getKey(), 1)) {
                    from = e.getKey();
                    break;
                }
            }
        }
        if (from == null) {
            Msg.game(p, "You have no jewellery that this spell can enchant.");
            return;
        }
        String to = Jewellery.ENCHANT.get(from);
        if (ItemCatalog.get(to) == null) {
            Msg.game(p, "Nothing interesting happens.");
            return;
        }
        pay(p, s);
        Inv.remove(p, from, 1);
        Inv.give(p, to, 1);
        // Enchanted jewellery counts as obtained (diary tasks, collection log).
        com.runecraft.collection.CollectionLog.obtain(p, to);
        ((ServerLevel) p.level()).sendParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.4, 0.4, 0.4, 0.5);
        Msg.action(p, "You enchant the " + ItemCatalog.get(from).name().toLowerCase(Locale.ROOT) + " into "
                + Msg.article(ItemCatalog.get(to).name()) + ".");
    }

    private static void convertOne(ServerPlayer p, Spell s, String from, String to, Skill skill, double xp) {
        if (ItemCatalog.get(from) == null || ItemCatalog.get(to) == null || !Inv.has(p, from, 1)) {
            ItemDef d = ItemCatalog.get(from);
            Msg.game(p, "You need " + (d == null ? "the right item" : Msg.article(d.name().toLowerCase(Locale.ROOT))) + " to cast this.");
            return;
        }
        pay(p, s);
        Inv.remove(p, from, 1);
        Inv.give(p, to, 1);
        if (xp > 0) {
            XpService.add(p, skill, xp);
        }
        Msg.action(p, "You cast " + s.name() + ".");
    }

    private static void bakePies(ServerPlayer p, Spell s) {
        int baked = 0;
        for (Recipe r : Recipes.menu(MenuType.COOKING)) {
            String raw = r.inputs().keySet().iterator().next();
            // Only baking an uncooked pie (not assembling one from a pie shell and its filling).
            if (r.inputs().size() != 1 || !raw.startsWith("uncooked_") || !raw.endsWith("_pie") || r.outputs().isEmpty()) {
                continue;
            }
            String cooked = r.outputs().keySet().iterator().next();
            if (r.skill() != null && Players.data(p).skills().currentLevel(r.skill()) < r.level()) {
                continue;
            }
            int n = Inv.count(p, raw);
            if (n > 0) {
                if (baked == 0) {
                    pay(p, s);
                }
                Inv.remove(p, raw, n);
                Inv.give(p, cooked, n);
                XpService.addTenths(p, Skill.COOKING, r.xpTenths() * n);
                baked += n;
            }
        }
        if (baked == 0) {
            Msg.game(p, "You have no uncooked pies to bake.");
        } else {
            Msg.action(p, "You bake " + baked + " pie" + (baked == 1 ? "" : "s") + ".");
        }
    }

    private static void superglass(ServerPlayer p, Spell s) {
        String sand = "bucket_of_sand";
        String[] ash = {"giant_seaweed", "seaweed", "soda_ash"};
        if (ItemCatalog.get("molten_glass") == null || ItemCatalog.get(sand) == null) {
            Msg.game(p, "Nothing interesting happens.");
            return;
        }
        int sands = Inv.count(p, sand);
        int ashes = 0;
        for (String a : ash) {
            ashes += ItemCatalog.get(a) == null ? 0 : Inv.count(p, a);
        }
        int n = Math.min(sands, ashes);
        if (n == 0) {
            Msg.game(p, "You need buckets of sand and seaweed or soda ash.");
            return;
        }
        pay(p, s);
        Inv.remove(p, sand, n);
        int left = n;
        for (String a : ash) {
            if (left > 0 && ItemCatalog.get(a) != null) {
                left -= Inv.remove(p, a, left);
            }
        }
        Inv.give(p, "molten_glass", n * 13 / 10);
        XpService.add(p, Skill.CRAFTING, 10.0 * n);
        Msg.action(p, "You make some molten glass.");
    }

    private static void tan(ServerPlayer p, Spell s) {
        String[][] hides = {{"cowhide", "leather"}, {"green_dragonhide", "green_dragon_leather"}, {"blue_dragonhide", "blue_dragon_leather"},
                {"red_dragonhide", "red_dragon_leather"}, {"black_dragonhide", "black_dragon_leather"}};
        int done = 0;
        for (String[] h : hides) {
            if (ItemCatalog.get(h[1]) == null) {
                continue;
            }
            int n = Math.min(5 - done, Inv.count(p, h[0]));
            if (n > 0) {
                if (done == 0) {
                    pay(p, s);
                }
                Inv.remove(p, h[0], n);
                Inv.give(p, h[1], n);
                done += n;
            }
        }
        if (done == 0) {
            Msg.game(p, "You have no hides to tan.");
        } else {
            Msg.action(p, "You tan " + done + " hide" + (done == 1 ? "" : "s") + ".");
        }
    }

    private static void stringJewellery(ServerPlayer p, Spell s) {
        int strung = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.isEmpty()) {
                continue;
            }
            String id = Inv.idOf(st);
            if (id.endsWith("_u") && id.contains("amulet") && ItemCatalog.get(id.substring(0, id.length() - 2)) != null) {
                if (strung == 0) {
                    pay(p, s);
                }
                int n = st.getCount();
                p.getInventory().setItem(i, Inv.stack(id.substring(0, id.length() - 2), n));
                strung += n;
            }
        }
        if (strung == 0) {
            Msg.game(p, "You have no unstrung amulets.");
        } else {
            XpService.add(p, Skill.CRAFTING, 4.0 * strung);
            Msg.action(p, "You string " + strung + " amulet" + (strung == 1 ? "" : "s") + ".");
        }
    }

    private static void plankMake(ServerPlayer p, Spell s) {
        String[][] logs = {{"mahogany_logs", "mahogany_plank", "1050"}, {"teak_logs", "teak_plank", "350"},
                {"oak_logs", "oak_plank", "175"}, {"logs", "plank", "70"}};
        for (String[] l : logs) {
            if (Inv.has(p, l[0], 1)) {
                int cost = Integer.parseInt(l[2]);
                if (!Inv.has(p, "coins", cost)) {
                    Msg.game(p, "You need " + cost + " coins to turn these logs into a plank.");
                    return;
                }
                pay(p, s);
                Inv.remove(p, "coins", cost);
                Inv.remove(p, l[0], 1);
                Inv.give(p, l[1], 1);
                ServerNetworking.sync(p);
                Msg.action(p, "You turn the logs into a plank.");
                return;
            }
        }
        Msg.game(p, "You have no logs to turn into planks.");
    }

    private static void otherPlayer(ServerPlayer p, Spell s, ServerPlayer other) {
        SysState.SysPlayer mine = SysState.of(p);
        SysState.SysPlayer theirs = SysState.of(other);
        switch (s.id()) {
            case "energy_transfer" -> {
                if (p.getHealth() <= 10) {
                    Msg.game(p, "You need more than 10 hitpoints to cast this spell.");
                    return;
                }
                pay(p, s);
                theirs.specEnergy = Math.min(100, theirs.specEnergy + mine.specEnergy);
                mine.specEnergy = 0;
                theirs.runEnergy = Math.min(10000, theirs.runEnergy + mine.runEnergy);
                mine.runEnergy = 0;
                p.setHealth(p.getHealth() - 10);
                SysNet.sendState(other);
            }
            case "heal_other" -> {
                pay(p, s);
                float give = Math.min(p.getHealth() * 0.75f, other.getMaxHealth() - other.getHealth());
                p.setHealth(p.getHealth() - give);
                other.heal(give);
                Msg.game(other, p.getName().getString() + " heals you.");
            }
            default -> {
                if (theirs.vengeance) {
                    Msg.game(p, "That player already has vengeance.");
                    return;
                }
                pay(p, s);
                theirs.vengeance = true;
                Msg.game(other, p.getName().getString() + " has cast Vengeance on you.");
                SysNet.sendState(other);
            }
        }
        SysNet.sendState(p);
    }

    private static void offering(ServerPlayer p, Spell s, boolean demonic) {
        int used = 0;
        double xp = 0;
        for (int i = 0; i < p.getInventory().getContainerSize() && used < 3; i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.isEmpty()) {
                continue;
            }
            String id = Inv.idOf(st);
            double each;
            if (demonic) {
                each = switch (id) {
                    case "fiendish_ashes" -> 10;
                    case "vile_ashes" -> 25;
                    case "malicious_ashes" -> 65;
                    case "abyssal_ashes" -> 85;
                    case "infernal_ashes" -> 110;
                    default -> id.endsWith("ashes") ? 10 : 0;
                };
            } else {
                Bone b = Bone.byItem(id);
                each = b == null ? 0 : b.xp();
            }
            if (each <= 0) {
                continue;
            }
            int n = Math.min(3 - used, st.getCount());
            st.shrink(n);
            used += n;
            xp += each * 3 * n;
        }
        if (used == 0) {
            Msg.game(p, demonic ? "You have no demonic ashes to offer." : "You have no bones to offer.");
            return;
        }
        pay(p, s);
        XpService.add(p, Skill.PRAYER, xp);
        Msg.action(p, "You offer up your " + (demonic ? "ashes" : "bones") + " to the dark altar of Arceuus.");
    }

    // ---------------- Thralls and reanimation ----------------

    private static void thrall(ServerPlayer p, Spell s) {
        SysState.SysPlayer sp = SysState.of(p);
        long now = SysTicks.now();
        if (sp.thrallUntil > now) {
            Msg.game(p, "You already have a thrall summoned.");
            return;
        }
        if (sp.thrallReadyAt > now) {
            Msg.game(p, "You can only resurrect a thrall every 10 seconds.");
            return;
        }
        if (ItemCatalog.get("book_of_the_dead") != null && !Inv.has(p, "book_of_the_dead", 1)
                && !Inv.idOf(p.getOffhandItem()).equals("book_of_the_dead")) {
            Msg.game(p, "You need the Book of the Dead to resurrect a thrall.");
            return;
        }
        pay(p, s);
        sp.thrallReadyAt = now + 17;
        sp.thrallTier = Spellbooks.thrallTier(s.id());
        sp.thrallType = s.id().contains("ghost") ? "ghost" : s.id().contains("skeleton") ? "skeleton" : "zombie";
        // Thralls last 0.6 s per Magic level; twice as long from the master Combat Achievement tier.
        sp.thrallUntil = now + (long) (Players.data(p).skills().currentLevel(Skill.MAGIC)
                * com.runecraft.osrs.content.systems.CombatAchievements.thrallDurationMultiplier(CombatAchievementService.tier(p)));
        sp.nextThrallAttack = now + 4;
        SysCombat.dust(p, new Vector3f(0.4f, 0.8f, 0.6f), 30);
        Msg.action(p, "You resurrect a " + (sp.thrallTier == 3 ? "greater" : sp.thrallTier == 2 ? "superior" : "lesser") + " "
                + sp.thrallType + " thrall.");
        SysNet.sendState(p);
    }

    /** True while a thrall's hit is being resolved (rules thralls ignore, such as the Moons' flat armour, check it). */
    private static boolean thrallHitting;

    public static boolean thrallHitting() {
        return thrallHitting;
    }

    /** Thralls attack the player's current target every 4 ticks and always hit (0 to max 1/2/3, x per tier). */
    public static void thrallTick(ServerPlayer p, long tick) {
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.thrallTier <= 0) {
            return;
        }
        if (sp.thrallUntil <= tick) {
            sp.thrallTier = 0;
            Msg.game(p, "Your thrall returns to the grave.");
            SysNet.sendState(p);
            return;
        }
        if (tick < sp.nextThrallAttack || tick - sp.lastCombatTick > 8) {
            return;
        }
        sp.nextThrallAttack = tick + 4;
        net.minecraft.world.entity.Entity e = p.level().getEntity(sp.lastTargetId);
        if (!(e instanceof LivingEntity target) || !target.isAlive() || target.distanceToSqr(p) > 15 * 15) {
            return;
        }
        int dmg = p.getRandom().nextInt(Spellbooks.thrallMaxHit(sp.thrallTier) + 1);
        Vector3f c = switch (sp.thrallType) {
            case "ghost" -> new Vector3f(0.7f, 0.9f, 1.0f);
            case "skeleton" -> new Vector3f(0.9f, 0.9f, 0.8f);
            default -> new Vector3f(0.3f, 0.6f, 0.2f);
        };
        SysCombat.trail((ServerLevel) p.level(), p.position().add(0, 1, 0), target.position().add(0, target.getBbHeight() / 2, 0), c);
        AttackType type = sp.thrallType.equals("ghost") ? AttackType.MAGIC : sp.thrallType.equals("skeleton") ? AttackType.RANGED
                : AttackType.CRUSH;
        if (dmg > 0) {
            if (target instanceof com.runecraft.entity.OsrsMonsterEntity m) {
                thrallHitting = true;
                try {
                    dmg = m.modifyIncoming(p, dmg, type);
                } finally {
                    thrallHitting = false;
                }
            }
            CombatHandler.applyDamage(p, target, dmg);
        } else {
            CombatHandler.applyDamage(p, target, 0);
        }
    }

    private static void reanimate(ServerPlayer p, Spell s) {
        String held = p.getMainHandItem().isEmpty() ? "" : Inv.idOf(p.getMainHandItem());
        Spellbooks.Head head = Spellbooks.head(held);
        if (head == null) {
            Msg.game(p, "Hold an ensouled head in your main hand to reanimate it.");
            return;
        }
        if (!head.spell().equals(s.id())) {
            Msg.game(p, "You need to cast " + Spellbooks.get(head.spell()).name() + " on this head.");
            return;
        }
        if (!Gielinor.is(p)) {
            Msg.game(p, "The head only responds to the magic of Gielinor.");
            return;
        }
        pay(p, s);
        p.getMainHandItem().shrink(1);
        ServerLevel world = (ServerLevel) p.level();
        Husk husk = EntityType.HUSK.create(world);
        if (husk == null) {
            return;
        }
        Vec3 at = p.position().add(p.getLookAngle().scale(2.0));
        husk.moveTo(at.x, p.getY(), at.z, p.getYRot() + 180, 0);
        husk.finalizeSpawn(world, world.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        String name = head.name().replace("Ensouled ", "Reanimated ").replace(" head", "");
        husk.setCustomName(Component.literal(name));
        husk.setCustomNameVisible(true);
        husk.addTag(REANIMATED_TAG + (int) Math.round(head.xp()));
        husk.addTag("runecraft_owner:" + p.getStringUUID());
        husk.setTarget(p);
        world.addFreshEntity(husk);
        world.sendParticles(ParticleTypes.SOUL, at.x, p.getY() + 1, at.z, 30, 0.4, 0.8, 0.4, 0.05);
        Msg.action(p, "You reanimate the " + name.replace("Reanimated ", "").toLowerCase(Locale.ROOT) + ". Defeat it for Prayer XP.");
    }

    /** Awards prayer XP for killing a reanimated creature (called from the kill event). */
    public static void onKill(ServerPlayer p, LivingEntity e) {
        for (String tag : e.getTags()) {
            if (tag.startsWith(REANIMATED_TAG)) {
                try {
                    XpService.add(p, Skill.PRAYER, Integer.parseInt(tag.substring(REANIMATED_TAG.length())));
                } catch (NumberFormatException ignored) {
                    // malformed tag
                }
            }
        }
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.deathCharge) {
            sp.deathCharge = false;
            SpecialAttackService.restoreEnergy(p, 15);
            Msg.action(p, "Some of your special attack energy has been restored.");
        }
        SysCombat.forget(e);
    }

    // ---------------- Lunar / Arceuus helpers ----------------

    /** The herb or allotment patch the player is looking at (within 6 blocks), or null. */
    private static net.minecraft.core.BlockPos lookPatch(ServerPlayer p) {
        net.minecraft.world.phys.HitResult hit = p.pick(6.0, 0f, false);
        if (hit instanceof net.minecraft.world.phys.BlockHitResult bh && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            net.minecraft.core.BlockPos pos = bh.getBlockPos();
            if (p.level().getBlockState(pos).getBlock() instanceof com.runecraft.block.FarmingPatchBlock) {
                return pos;
            }
            if (p.level().getBlockState(pos.below()).getBlock() instanceof com.runecraft.block.FarmingPatchBlock) {
                return pos.below();
            }
        }
        return null;
    }

    /** Cure Plant, Fertile Soil and Resurrect Crops on the herb/allotment patch the player looks at. */
    private static void farmingSpell(ServerPlayer p, Spell s) {
        net.minecraft.core.BlockPos pos = lookPatch(p);
        if (pos == null) {
            Msg.game(p, "Look at a herb or allotment patch to cast this spell on it.");
            return;
        }
        ServerLevel world = (ServerLevel) p.level();
        net.minecraft.world.level.block.state.BlockState state = world.getBlockState(pos);
        com.runecraft.block.FarmingPatchBlock patch = (com.runecraft.block.FarmingPatchBlock) state.getBlock();
        switch (s.id()) {
            case "cure_plant" -> {
                if (!com.runecraft.skill.FarmingService.curePlant(world, pos, state, patch)) {
                    Msg.game(p, "This patch isn't diseased.");
                    return;
                }
                pay(p, s);
                Msg.action(p, "You cure the plant. It is restored to health.");
            }
            case "fertile_soil" -> {
                if (!com.runecraft.skill.FarmingService.fertilise(world, pos, state, patch)) {
                    Msg.game(p, "This patch can't be treated now.");
                    return;
                }
                pay(p, s);
                Msg.action(p, "The patch is treated with supercompost.");
            }
            default -> {
                if (!com.runecraft.skill.FarmingService.isDead(state, patch)) {
                    Msg.game(p, "This spell can only be cast on dead crops.");
                    return;
                }
                pay(p, s);
                boolean ok = p.getRandom().nextDouble()
                        < Spellbooks.resurrectCropsChance(Players.data(p).skills().currentLevel(Skill.MAGIC));
                com.runecraft.skill.FarmingService.resurrect(world, pos, state, patch, ok);
                Msg.action(p, ok ? "The crop springs back to life!" : "The spell fails and the dead crop crumbles away.");
            }
        }
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 12, 0.4, 0.3, 0.4, 0.0);
    }

    /** Geomancy: the state of the herb and allotment patches within 32 blocks. */
    private static void geomancy(ServerPlayer p, Spell s) {
        ServerLevel world = (ServerLevel) p.level();
        net.minecraft.core.BlockPos c = p.blockPosition();
        List<String> lines = new ArrayList<>();
        net.minecraft.core.BlockPos.MutableBlockPos m = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int dx = -32; dx <= 32 && lines.size() < 8; dx++) {
            for (int dz = -32; dz <= 32 && lines.size() < 8; dz++) {
                for (int dy = -6; dy <= 6; dy++) {
                    m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    net.minecraft.world.level.block.state.BlockState st = world.getBlockState(m);
                    if (st.getBlock() instanceof com.runecraft.block.FarmingPatchBlock patch) {
                        lines.add(com.runecraft.skill.FarmingService.status(st, patch));
                        break;
                    }
                }
            }
        }
        if (lines.isEmpty()) {
            Msg.game(p, "There are no farming patches nearby.");
            return;
        }
        pay(p, s);
        lines.forEach(l -> Msg.game(p, l));
    }

    /** Dream: rest to regenerate an extra Hitpoint every minute (twice the natural rate) until healed or moving. */
    private static void dream(ServerPlayer p, Spell s) {
        if (p.getHealth() >= p.getMaxHealth()) {
            Msg.game(p, "You have no need to cast this spell since your hitpoints are already full.");
            return;
        }
        pay(p, s);
        Msg.action(p, "You lie down to rest...");
        Vec3 start = p.position();
        ActionManager.start(p, new com.runecraft.action.Action() {
            int ticks;

            @Override
            public boolean tick(ServerPlayer pl, long gameTick) {
                if (pl.position().distanceToSqr(start) > 0.25 || pl.getHealth() >= pl.getMaxHealth()) {
                    Msg.game(pl, "You wake up.");
                    return false;
                }
                if (++ticks % com.runecraft.osrs.PrayerEffects.REGEN_TICKS == 0) {
                    pl.heal(1);
                }
                if (ticks % 5 == 0) {
                    ((ServerLevel) pl.level()).sendParticles(ParticleTypes.NOTE, pl.getX(), pl.getY() + 1.6, pl.getZ(), 1, 0.2, 0.1, 0.2, 0.0);
                }
                return true;
            }
        });
    }

    /** Stat Restore Pot Share / Boost Potion Share: one dose of the held potion each for the caster and players next to them. */
    private static void potionShare(ServerPlayer p, Spell s) {
        ItemStack held = p.getMainHandItem();
        String id = held.isEmpty() ? "" : Inv.idOf(held);
        com.runecraft.osrs.data.Potion potion = null;
        for (com.runecraft.osrs.data.Potion pt : com.runecraft.osrs.data.Potion.values()) {
            if (pt.itemId().equals(id)) {
                potion = pt;
            }
        }
        if (potion == null || !Spellbooks.canShare(s.id(), id)) {
            Msg.game(p, s.id().startsWith("stat") ? "Hold a restore, super restore, prayer or energy potion to share it."
                    : "Hold a stat-boosting potion to share it.");
            return;
        }
        int doses = 4 - held.getDamageValue();
        List<ServerPlayer> group = new ArrayList<>();
        group.add(p);
        for (ServerPlayer o : nearbyPlayers(p, 3)) {
            if (o != p && group.size() < doses) {
                group.add(o);
            }
        }
        pay(p, s);
        for (ServerPlayer o : group) {
            PlayerData d = Players.data(o);
            int prayer = potion.apply(d.skills());
            prayer += prayer > 0 ? com.runecraft.skill.ItemUses.wrenchBonus(o, 25) : 0;
            if (prayer > 0) {
                int cap = Math.max(d.skills().level(Skill.PRAYER), d.skills().currentLevel(Skill.PRAYER));
                d.setPrayerPoints(Math.max(d.prayerPoints(), Math.min(cap, d.prayerPoints() + prayer)));
            }
            if (potion == com.runecraft.osrs.data.Potion.ENERGY) {
                SysState.SysPlayer osp = SysState.of(o);
                osp.runEnergy = Math.min(10000, osp.runEnergy + 1000);
                SysNet.sendState(o);
            }
            ServerNetworking.sync(o);
            if (o != p) {
                Msg.game(o, p.getName().getString() + " shares a dose of " + potion.displayName().toLowerCase(Locale.ROOT) + " with you.");
            }
        }
        int left = doses - group.size();
        if (left <= 0) {
            p.setItemInHand(InteractionHand.MAIN_HAND, Inv.stack("vial", 1));
        } else {
            held.setDamageValue(4 - left);
        }
        Msg.action(p, "You share your potion between " + group.size() + " player" + (group.size() == 1 ? "." : "s."));
    }

    /** A corruption spell waiting on the caster's next successful hit against a player (from the attack modifier). */
    static void corruptOnHit(ServerPlayer p, LivingEntity target, int dealt) {
        SysState.SysPlayer sp = SysState.of(p);
        long now = SysTicks.now();
        if (sp.corruptionPending == 0 || dealt <= 0 || !(target instanceof ServerPlayer victim)) {
            return;
        }
        if (sp.corruptionPendingUntil < now) {
            sp.corruptionPending = 0;
            return;
        }
        boolean greater = sp.corruptionPending == 2;
        sp.corruptionPending = 0;
        SysState.SysPlayer vs = SysState.of(victim);
        boolean mark = sp.markOfDarknessUntil > now;
        // 50% chance (always with Mark of Darkness); no effect on the already corrupted or the warded.
        if (vs.corruptionPulses > 0 || vs.wardUntil > now || (!mark && p.getRandom().nextBoolean())) {
            return;
        }
        vs.corruptionPulses = 3;
        vs.corruptionGreater = greater;
        vs.corruptionFast = mark;
        vs.nextCorruptionPulse = now + Spellbooks.corruptionInterval(mark);
        Msg.red(victim, "Your prayer has been corrupted!");
    }

    /** Corruption pulses on a corrupted player (every 10 ticks, 5 with the caster's Mark of Darkness). */
    public static void corruptionTick(ServerPlayer p, long tick) {
        SysState.SysPlayer sp = SysState.of(p);
        if (sp.corruptionPulses <= 0 || tick < sp.nextCorruptionPulse) {
            return;
        }
        int pulse = 3 - sp.corruptionPulses;
        PlayerData d = Players.data(p);
        d.setPrayerPoints(Math.max(0, d.prayerPoints() - Spellbooks.corruptionDrain(sp.corruptionGreater, pulse)));
        sp.corruptionPulses--;
        sp.nextCorruptionPulse = tick + Spellbooks.corruptionInterval(sp.corruptionFast);
        SysCombat.dust(p, new Vector3f(0.3f, 0.6f, 0.2f), 10);
        ServerNetworking.sync(p);
    }

    /** All spells shown in the spellbook screen for the player's book. */
    public static List<Spell> visible(ServerPlayer p) {
        SysState.SysPlayer sp = SysState.of(p);
        return Spellbooks.book(sp.swapBook != null ? sp.swapBook : sp.book);
    }

    static ItemDef held(ServerPlayer p) {
        return ModItems.def(p.getMainHandItem());
    }
}
