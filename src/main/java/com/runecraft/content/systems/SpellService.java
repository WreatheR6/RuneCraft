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
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.HuskEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
    }

    private SpellService() {
    }

    // ---------------- Runes ----------------

    /** Whether the wielded staff (or off-hand tome) provides unlimited runes of an element. */
    static boolean staffProvides(ServerPlayerEntity p, String element) {
        String id = p.getMainHandStack().isEmpty() ? "" : Inv.idOf(p.getMainHandStack());
        String off = p.getOffHandStack().isEmpty() ? "" : Inv.idOf(p.getOffHandStack());
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
    static Map<String, Integer> runePlan(ServerPlayerEntity p, Map<String, Integer> runes) {
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
                if (!elemental) {
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

    static void consume(ServerPlayerEntity p, Map<String, Integer> plan) {
        plan.forEach((item, n) -> Inv.remove(p, item, n));
    }

    // ---------------- Requirements ----------------

    /** Whether the player may use a spellbook (quest requirement looked up at runtime). */
    public static boolean bookAllowed(ServerPlayerEntity p, Book book, boolean warn) {
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

    static boolean canCast(ServerPlayerEntity p, Spell s, boolean quiet) {
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
        if (s.item() != null && !Inv.idOf(p.getMainHandStack()).startsWith(s.item())) {
            if (!quiet) {
                ItemDef d = ItemCatalog.get(s.item());
                Msg.game(p, "You need to wield " + (d == null ? s.item().replace('_', ' ') : Msg.article(d.name())) + " to cast this spell.");
            }
            return false;
        }
        if (runePlan(p, s.runes()) == null) {
            if (!quiet) {
                Msg.game(p, "You do not have enough runes to cast this spell.");
            }
            return false;
        }
        return true;
    }

    /** Takes the runes and awards the base XP for a successful cast. */
    static void pay(ServerPlayerEntity p, Spell s) {
        Map<String, Integer> plan = runePlan(p, s.runes());
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
    public static void cast(ServerPlayerEntity p, String spellId) {
        Spell s = Spellbooks.get(spellId);
        if (s == null || !Stun.check(p)) {
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

    public static void setAutocast(ServerPlayerEntity p, String spellId) {
        SysState.SysPlayer sp = SysState.of(p);
        sp.autocast = spellId == null ? "" : spellId;
        Msg.action(p, spellId == null ? "Autocast disabled." : "Autocasting " + Spellbooks.get(spellId).name() + ".");
        SysNet.sendState(p);
    }

    /** Staff right-click while one of this module's combat spells is set to autocast. Returns true if handled. */
    public static boolean staffCast(ServerPlayerEntity p) {
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

    static void castCombat(ServerPlayerEntity p, Spell s, LivingEntity target) {
        PlayerData data = Players.data(p);
        if (target == null) {
            Msg.action(p, "No target.");
            return;
        }
        if (!SysCombat.canAttack(p, target)) {
            Msg.game(p, "You can't attack that.");
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
        p.swingHand(Hand.MAIN_HAND, true);
        ServerWorld world = (ServerWorld) p.getWorld();
        SysCombat.trail(world, p.getEyePos(), target.getPos().add(0, target.getHeight() / 2, 0), colour(s));
        world.playSound(null, p.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 1.2f);
        List<LivingEntity> targets = new ArrayList<>();
        targets.add(target);
        if (s.area()) {
            for (LivingEntity e : world.getEntitiesByClass(LivingEntity.class, target.getBoundingBox().expand(1.6),
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
            long att = (long) (SysCombat.magicAttackRoll(p) * acc * CombatHandler.hookAccuracy(p, t, AttackType.MAGIC));
            long def = SysCombat.baseDefenceRoll(t, AttackType.MAGIC);
            int max = (int) Math.floor(CombatFormulas.magicMaxHit(baseMax, SysCombat.coreStats(p).magicDamagePercent()) * dmgMult
                    * CombatHandler.hookMaxHit(p, t, AttackType.MAGIC));
            boolean hit = p.getRandom().nextDouble() < Chance.hitChance(att, def);
            if (!hit) {
                world.spawnParticles(ParticleTypes.CLOUD, t.getX(), t.getY() + 1, t.getZ(), 8, 0.3, 0.3, 0.3, 0.02);
                t.setAttacker(p);
                CombatHandler.applyDamage(p, t, 0);
                continue;
            }
            int damage = max > 0 ? p.getRandom().nextInt(max + 1) : 0;
            damage = CombatHandler.hookAfterRoll(p, t, AttackType.MAGIC, damage);
            int dealt = max > 0 ? SysCombat.hit(p, t, AttackType.MAGIC, damage) : 0;
            if (max == 0) {
                t.setAttacker(p);
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

    private static void applyEffect(ServerPlayerEntity p, Spell s, LivingEntity t, int dealt) {
        switch (s.effect()) {
            case POISON -> {
                if (dealt > 0 && p.getRandom().nextInt(4) == 0) {
                    SysCombat.poison(t, s.param());
                }
            }
            case DRAIN_ATTACK -> SysCombat.drainStat(t, Skill.ATTACK, Math.max(1, SysCombat.statLevel(t, Skill.ATTACK) * s.param() / 100));
            case DRAIN_DEFENCE -> SysCombat.drainDefence(t, Math.max(1, SysCombat.defenceLevel(t) * s.param() / 100));
            case DRAIN_MAGIC -> SysCombat.drainStat(t, Skill.MAGIC, Math.max(1, SysCombat.statLevel(t, Skill.MAGIC) * s.param() / 100));
            case DRAIN_PRAYER -> {
                if (t instanceof ServerPlayerEntity victim) {
                    PlayerData vd = Players.data(victim);
                    vd.setPrayerPoints(vd.prayerPoints() - s.param());
                    ServerNetworking.sync(victim);
                }
            }
            case FREEZE, BIND -> SysCombat.freeze(t, s.param());
            default -> {
            }
        }
        if (s.id().equals("weaken") || s.id().equals("enfeeble")) {
            SysCombat.drainStat(t, Skill.STRENGTH, Math.max(1, SysCombat.statLevel(t, Skill.STRENGTH) * s.param() / 100));
        }
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

    static void teleport(ServerPlayerEntity p, Spell s) {
        boolean home = s.runes().isEmpty();
        if (!SysTeleport.canTeleport(p, SysTeleport.SPELL_WILDERNESS)) {
            return;
        }
        if (home) {
            Msg.action(p, "You begin casting " + s.name() + "...");
            ActionManager.start(p, new com.runecraft.action.Action() {
                int ticks;

                @Override
                public boolean tick(ServerPlayerEntity pl, long gameTick) {
                    ((ServerWorld) pl.getWorld()).spawnParticles(ParticleTypes.ENCHANT, pl.getX(), pl.getY() + 1, pl.getZ(), 15, 0.5, 0.8, 0.5, 0.5);
                    if (++ticks >= 16) {
                        SysTeleport.teleport(pl, s.destination(), SysTeleport.SPELL_WILDERNESS);
                        return false;
                    }
                    return true;
                }
            });
            return;
        }
        pay(p, s);
        SysTeleport.teleport(p, s.destination(), SysTeleport.SPELL_WILDERNESS);
    }

    // ---------------- Utility ----------------

    static void utility(ServerPlayerEntity p, Spell s) {
        String id = s.id();
        if (id.startsWith("enchant_")) {
            enchant(p, s, Integer.parseInt(id.substring("enchant_".length())));
            return;
        }
        if (id.startsWith("resurrect_")) {
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
                SysCombat.trail((ServerWorld) p.getWorld(), p.getEyePos(), item.getPos(), new Vector3f(0.9f, 0.9f, 0.4f));
                item.setPosition(p.getX(), p.getY() + 0.5, p.getZ());
                item.setPickupDelay(0);
            }
            case "charge" -> {
                pay(p, s);
                sp.chargeUntil = now + 700;
                Msg.action(p, "You feel charged with magic power.");
            }
            case "bake_pie" -> bakePies(p, s);
            case "humidify" -> {
                int n = Inv.count(p, "vial");
                if (n == 0) {
                    Msg.game(p, "You have nothing to fill with water.");
                    return;
                }
                pay(p, s);
                Inv.remove(p, "vial", n);
                Inv.give(p, "vial_of_water", n);
                Msg.action(p, "You fill your vials with water.");
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
                for (ServerPlayerEntity o : nearbyPlayers(p, 8)) {
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
                if (!(t instanceof ServerPlayerEntity other)) {
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
                if (!(t instanceof ServerPlayerEntity other)) {
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
                List<ServerPlayerEntity> others = nearbyPlayers(p, 8);
                others.remove(p);
                if (others.isEmpty() || p.getHealth() < p.getMaxHealth() * 0.11f) {
                    Msg.game(p, "There is nobody nearby to heal, or you are too weak.");
                    return;
                }
                pay(p, s);
                float give = p.getHealth() * 0.75f;
                p.setHealth(p.getHealth() - give);
                for (ServerPlayerEntity o : others) {
                    o.heal(give / others.size());
                }
            }
            case "spellbook_swap" -> SysMenus.spellbookSwap(p);
            // ---------------- Arceuus ----------------
            case "shadow_veil" -> {
                pay(p, s);
                sp.shadowVeilUntil = now + data.skills().currentLevel(Skill.MAGIC);
                Msg.action(p, "Your thieving abilities have been enhanced.");
            }
            case "dark_lure" -> {
                LivingEntity t = SysCombat.lookTarget(p, 16);
                if (t == null || t instanceof ServerPlayerEntity) {
                    Msg.game(p, "Look at a monster to lure it.");
                    return;
                }
                pay(p, s);
                if (t instanceof net.minecraft.entity.mob.MobEntity mob) {
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
                pay(p, s);
                int restore = Math.min(10000 - sp.runEnergy, data.prayerPoints() * 100);
                sp.runEnergy += restore;
                data.setPrayerPoints(data.prayerPoints() - restore / 200);
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
            case "lesser_corruption", "greater_corruption" -> Msg.game(p, "Corruption only affects other players' prayer in PvP.");
            default -> Msg.game(p, "Nothing interesting happens.");
        }
    }

    private static List<ServerPlayerEntity> nearbyPlayers(ServerPlayerEntity p, double r) {
        List<ServerPlayerEntity> out = new ArrayList<>();
        for (ServerPlayerEntity o : ((ServerWorld) p.getWorld()).getPlayers()) {
            if (o.squaredDistanceTo(p) <= r * r) {
                out.add(o);
            }
        }
        return out;
    }

    private static ItemEntity lookItem(ServerPlayerEntity p) {
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVector();
        ItemEntity best = null;
        double bestScore = 0.97;
        for (ItemEntity e : p.getWorld().getEntitiesByClass(ItemEntity.class, new Box(eye, eye).expand(RANGE), x -> true)) {
            Vec3d to = e.getPos().subtract(eye);
            double dist = to.length();
            if (dist > RANGE || dist < 0.1) {
                continue;
            }
            double dot = to.normalize().dotProduct(look);
            if (dot > bestScore) {
                bestScore = dot;
                best = e;
            }
        }
        return best;
    }

    private static void enchant(ServerPlayerEntity p, Spell s, int level) {
        String from = null;
        String held = p.getMainHandStack().isEmpty() ? "" : Inv.idOf(p.getMainHandStack());
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
        ((ServerWorld) p.getWorld()).spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.2, p.getZ(), 30, 0.4, 0.4, 0.4, 0.5);
        Msg.action(p, "You enchant the " + ItemCatalog.get(from).name().toLowerCase(Locale.ROOT) + " into "
                + Msg.article(ItemCatalog.get(to).name()) + ".");
    }

    private static void convertOne(ServerPlayerEntity p, Spell s, String from, String to, Skill skill, double xp) {
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

    private static void bakePies(ServerPlayerEntity p, Spell s) {
        int baked = 0;
        for (Recipe r : Recipes.menu(MenuType.COOKING)) {
            String raw = r.inputs().keySet().iterator().next();
            if (!raw.contains("pie") || r.outputs().isEmpty()) {
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

    private static void superglass(ServerPlayerEntity p, Spell s) {
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

    private static void tan(ServerPlayerEntity p, Spell s) {
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

    private static void stringJewellery(ServerPlayerEntity p, Spell s) {
        int strung = 0;
        for (int i = 0; i < p.getInventory().size(); i++) {
            ItemStack st = p.getInventory().getStack(i);
            if (st.isEmpty()) {
                continue;
            }
            String id = Inv.idOf(st);
            if (id.endsWith("_u") && id.contains("amulet") && ItemCatalog.get(id.substring(0, id.length() - 2)) != null) {
                if (strung == 0) {
                    pay(p, s);
                }
                int n = st.getCount();
                p.getInventory().setStack(i, Inv.stack(id.substring(0, id.length() - 2), n));
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

    private static void plankMake(ServerPlayerEntity p, Spell s) {
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

    private static void otherPlayer(ServerPlayerEntity p, Spell s, ServerPlayerEntity other) {
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

    private static void offering(ServerPlayerEntity p, Spell s, boolean demonic) {
        int used = 0;
        double xp = 0;
        for (int i = 0; i < p.getInventory().size() && used < 3; i++) {
            ItemStack st = p.getInventory().getStack(i);
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
            st.decrement(n);
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

    private static void thrall(ServerPlayerEntity p, Spell s) {
        SysState.SysPlayer sp = SysState.of(p);
        long now = SysTicks.now();
        if (sp.thrallUntil > now) {
            Msg.game(p, "You already have a thrall summoned.");
            return;
        }
        pay(p, s);
        sp.thrallTier = Spellbooks.thrallTier(s.id());
        sp.thrallType = s.id().contains("ghost") ? "ghost" : s.id().contains("skeleton") ? "skeleton" : "zombie";
        sp.thrallUntil = now + Players.data(p).skills().currentLevel(Skill.MAGIC);
        sp.nextThrallAttack = now + 4;
        SysCombat.dust(p, new Vector3f(0.4f, 0.8f, 0.6f), 30);
        Msg.action(p, "You resurrect a " + (sp.thrallTier == 3 ? "greater" : sp.thrallTier == 2 ? "superior" : "lesser") + " "
                + sp.thrallType + " thrall.");
        SysNet.sendState(p);
    }

    /** Thralls attack the player's current target every 4 ticks and always hit (0 to max 1/2/3, x per tier). */
    public static void thrallTick(ServerPlayerEntity p, long tick) {
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
        net.minecraft.entity.Entity e = p.getWorld().getEntityById(sp.lastTargetId);
        if (!(e instanceof LivingEntity target) || !target.isAlive() || target.squaredDistanceTo(p) > 15 * 15) {
            return;
        }
        int dmg = p.getRandom().nextInt(sp.thrallTier + 1);
        Vector3f c = switch (sp.thrallType) {
            case "ghost" -> new Vector3f(0.7f, 0.9f, 1.0f);
            case "skeleton" -> new Vector3f(0.9f, 0.9f, 0.8f);
            default -> new Vector3f(0.3f, 0.6f, 0.2f);
        };
        SysCombat.trail((ServerWorld) p.getWorld(), p.getPos().add(0, 1, 0), target.getPos().add(0, target.getHeight() / 2, 0), c);
        AttackType type = sp.thrallType.equals("ghost") ? AttackType.MAGIC : sp.thrallType.equals("skeleton") ? AttackType.RANGED
                : AttackType.CRUSH;
        if (dmg > 0) {
            if (target instanceof com.runecraft.entity.OsrsMonsterEntity m) {
                dmg = m.modifyIncoming(p, dmg, type);
            }
            CombatHandler.applyDamage(p, target, dmg);
        } else {
            CombatHandler.applyDamage(p, target, 0);
        }
    }

    private static void reanimate(ServerPlayerEntity p, Spell s) {
        String held = p.getMainHandStack().isEmpty() ? "" : Inv.idOf(p.getMainHandStack());
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
        p.getMainHandStack().decrement(1);
        ServerWorld world = (ServerWorld) p.getWorld();
        HuskEntity husk = EntityType.HUSK.create(world);
        if (husk == null) {
            return;
        }
        Vec3d at = p.getPos().add(p.getRotationVector().multiply(2.0));
        husk.refreshPositionAndAngles(at.x, p.getY(), at.z, p.getYaw() + 180, 0);
        husk.initialize(world, world.getLocalDifficulty(p.getBlockPos()), SpawnReason.MOB_SUMMONED, null, null);
        String name = head.name().replace("Ensouled ", "Reanimated ").replace(" head", "");
        husk.setCustomName(Text.literal(name));
        husk.setCustomNameVisible(true);
        husk.addCommandTag(REANIMATED_TAG + (int) Math.round(head.xp()));
        husk.addCommandTag("runecraft_owner:" + p.getUuidAsString());
        husk.setTarget(p);
        world.spawnEntity(husk);
        world.spawnParticles(ParticleTypes.SOUL, at.x, p.getY() + 1, at.z, 30, 0.4, 0.8, 0.4, 0.05);
        Msg.action(p, "You reanimate the " + name.replace("Reanimated ", "").toLowerCase(Locale.ROOT) + ". Defeat it for Prayer XP.");
    }

    /** Awards prayer XP for killing a reanimated creature (called from the kill event). */
    public static void onKill(ServerPlayerEntity p, LivingEntity e) {
        for (String tag : e.getCommandTags()) {
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

    /** All spells shown in the spellbook screen for the player's book. */
    public static List<Spell> visible(ServerPlayerEntity p) {
        SysState.SysPlayer sp = SysState.of(p);
        return Spellbooks.book(sp.swapBook != null ? sp.swapBook : sp.book);
    }

    static ItemDef held(ServerPlayerEntity p) {
        return ModItems.def(p.getMainHandStack());
    }
}
