package com.runecraft.content.minigames;

import com.runecraft.collection.CollectionLog;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.minigames.GuardiansRewards;
import com.runecraft.osrs.content.minigames.MinigameItems;
import com.runecraft.osrs.content.minigames.MinigameReward;
import com.runecraft.osrs.content.minigames.WintertodtRewards;
import com.runecraft.skill.Drops;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.util.StackData;
import com.runecraft.platform.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import java.util.Random;

/** Right-click behaviour of reward containers (supply crates, intricate pouches...) and Bones to Peaches. */
public final class MinigameItemUse {
    private static final Random RANDOM = new Random();
    public static final String ROLLS = "runecraft_rolls";

    private MinigameItemUse() {
    }

    /** A reward container that opens for {@code rolls} loot rolls. */
    public static ItemStack container(String item, int rolls) {
        ItemStack stack = Inv.stack(item, 1);
        if (!stack.isEmpty()) {
            StackData.update(stack, nbt -> nbt.putInt(ROLLS, rolls));
        }
        return stack;
    }

    private static int rolls(ItemStack stack) {
        CompoundTag nbt = StackData.get(stack);
        return nbt.contains(ROLLS) ? Math.max(1, nbt.getInt(ROLLS)) : 1;
    }

    static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || stack.isEmpty()) {
                return InteractionResultHolder.pass(stack);
            }
            String id = Inv.idOf(stack);
            if (sp.getCooldowns().isOnCooldown(stack.getItem())) {
                return InteractionResultHolder.pass(stack);
            }
            if (com.runecraft.content.minigames.games.NightmareZoneSession.drink(sp, stack, hand, id) || blighted(sp, stack, hand, id)
                    || com.runecraft.content.minigames.games.VolcanicMineSession.useVessel(sp, id)
                    || com.runecraft.content.minigames.games.WintertodtSession.useKnife(sp, id)
                    || com.runecraft.content.minigames.games.MageArenaSession.useItem(sp, id)) {
                return InteractionResultHolder.success(stack);
            }
            boolean used = switch (id) {
                case "spoils_of_war" -> {
                    openSpoils(sp);
                    yield true;
                }
                case "herb_box" -> {
                    openHerbBox(sp);
                    yield true;
                }
                case "supply_crate" -> {
                    openSupplyCrate(sp, rolls(stack));
                    yield true;
                }
                case "intricate_pouch" -> {
                    openPouch(sp);
                    yield true;
                }
                case com.runecraft.osrs.content.minigames.TemporossRewards.CASKET -> {
                    openPoolCasket(sp);
                    yield true;
                }
                case "aluft_aloft_box" -> {
                    openAluftBox(sp);
                    yield true;
                }
                case "reward_token_blue", "reward_token_yellow", "reward_token_red" -> {
                    com.runecraft.content.minigames.games.TempleTrekkingSession.openToken(sp, stack);
                    yield false;
                }
                case "ring_of_wealth_scroll", "magic_shortbow_scroll" -> LmsService.useImbueScroll(sp, id);
                case "scroll_of_imbuing" -> EmirsArenaService.useScrollOfImbuing(sp);
                case "bones_to_peaches_tablet" -> {
                    bonesToPeaches(sp);
                    yield false;
                }
                case "scroll_of_redirection" -> {
                    showRedirects(sp);
                    yield false;
                }
                case com.runecraft.osrs.content.minigames.Gauntlet.PESTLE -> {
                    com.runecraft.content.minigames.games.GauntletSession.grind(sp);
                    yield false;
                }
                case "crystal_grym_leaf", "corrupted_grym_leaf" -> {
                    com.runecraft.content.minigames.games.GauntletSession.mixGrym(sp);
                    yield false;
                }
                case com.runecraft.osrs.content.minigames.Gauntlet.GRYM_UNF -> {
                    com.runecraft.content.minigames.games.GauntletSession.finishEgniol(sp);
                    yield false;
                }
                default -> com.runecraft.content.minigames.games.TempleTrekkingSession.readTome(sp, id);
            };
            if (used) {
                stack.shrink(1);
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        });
    }

    static void give(ServerPlayer p, String item, int count) {
        if (item == null || count <= 0) {
            return;
        }
        Inv.give(p, item, count);
        CollectionLog.obtain(p, item);
    }

    private static void pet(ServerPlayer p, String pet) {
        give(p, pet, 1);
        p.getServer().getPlayerList().broadcastSystemMessage(Component.literal(p.getName().getString()
                + " has a funny feeling like they're being followed.").withStyle(ChatFormatting.GOLD), false);
    }

    /** Gives {@code n} of an item as bank notes when it can be noted (overflow drops at the player's feet). */
    public static void giveNoted(ServerPlayer p, String item, int n) {
        if (n <= 0) {
            return;
        }
        net.minecraft.world.item.ItemStack note = com.runecraft.osrs.bank.BankNotes.canNote(item)
                ? com.runecraft.bank.Notes.note(item, "", 1) : net.minecraft.world.item.ItemStack.EMPTY;
        if (note.isEmpty()) {
            give(p, item, n);
            return;
        }
        int in = com.runecraft.bank.Notes.insert(p, note, n);
        if (in < n) {
            Inv.drop(p, note.copyWithCount(n - in), false);
        }
        CollectionLog.obtain(p, item);
    }

    /** Soul Wars spoils of war: three rolls plus the Lil' creator roll. */
    public static void openSpoils(ServerPlayer p) {
        for (com.runecraft.osrs.content.minigames.SpoilsOfWar.Prize prize
                : com.runecraft.osrs.content.minigames.SpoilsOfWar.open(RANDOM::nextDouble)) {
            if (prize.item().equals(com.runecraft.osrs.content.minigames.SpoilsOfWar.PET)) {
                pet(p, prize.item());
            } else if (prize.noted()) {
                giveNoted(p, prize.item(), prize.amount());
            } else {
                give(p, prize.item(), prize.amount());
            }
        }
        Msg.game(p, "You open the spoils of war.");
    }

    /** Herb box (Nightmare Zone): seven rolls on the herb drop table (OSRS Wiki weights out of 128). */
    /** The scroll of redirection's towns, as chat links ({@code /minigame redirect <town>}). */
    static void showRedirects(ServerPlayer p) {
        Msg.game(p, "Use the scroll on a Teleport to house tablet to redirect it to:");
        net.minecraft.network.chat.MutableComponent line = net.minecraft.network.chat.Component.literal("");
        for (com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.Redirect r
                : com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.REDIRECTS) {
            line.append(net.minecraft.network.chat.Component.literal("[" + r.town() + "] ").withStyle(s -> s.withColor(net.minecraft.ChatFormatting.AQUA)
                    .withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND,
                            "/minigame redirect " + r.town().toLowerCase(java.util.Locale.ROOT)))));
        }
        p.displayClientMessage(line, false);
    }

    /**
     * Redirects one Teleport to house tablet with a scroll of redirection (both used up), if the player has the Construction
     * level to move a house to that town and any quest it needs. Returns whether a tablet was made.
     */
    public static boolean redirect(ServerPlayer p, String town) {
        com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.Redirect r =
                com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.redirect(town);
        if (r == null) {
            return false;
        }
        if (!Inv.has(p, com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.SCROLL_OF_REDIRECTION, 1)
                || !Inv.has(p, com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.HOUSE_TABLET, 1)) {
            Msg.game(p, "You need a scroll of redirection and a Teleport to house tablet.");
            return false;
        }
        if (com.runecraft.player.Players.data(p).skills().level(com.runecraft.osrs.Skill.CONSTRUCTION) < r.construction()) {
            Msg.game(p, "You need a Construction level of " + r.construction() + " to redirect a tablet to " + r.town() + ".");
            return false;
        }
        if (r.quest() != null && !com.runecraft.quest.QuestApi.isComplete(p, r.quest())) {
            Msg.game(p, "You can't redirect a tablet to " + r.town() + " yet.");
            return false;
        }
        Inv.remove(p, com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.SCROLL_OF_REDIRECTION, 1);
        Inv.remove(p, com.runecraft.osrs.content.minigames.MinigameRules.NightmareZone.HOUSE_TABLET, 1);
        Inv.give(p, r.tablet(), 1);
        Msg.game(p, "You redirect the tablet to " + r.town() + ".");
        return true;
    }

    public static void openHerbBox(ServerPlayer p) {
        String[] herbs = {"guam_leaf", "marrentill", "tarromin", "harralander", "ranarr_weed", "irit_leaf", "avantoe", "kwuarm",
                "cadantine", "lantadyme", "dwarf_weed"};
        int[] weights = {32, 24, 18, 14, 11, 8, 6, 5, 4, 3, 3};
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        for (int i = 0; i < 7; i++) {
            int r = RANDOM.nextInt(total);
            for (int h = 0; h < herbs.length; h++) {
                r -= weights[h];
                if (r < 0) {
                    give(p, "grimy_" + herbs[h], 1);
                    break;
                }
            }
        }
        Msg.game(p, "You open the herb box.");
    }

    /**
     * Blighted supplies only work in the Wilderness. Returns true when the click was handled: blocked outside
     * the Wilderness, or a blighted super restore drunk inside it (blighted food is eaten as normal food).
     */
    static boolean blighted(ServerPlayer p, ItemStack stack, net.minecraft.world.InteractionHand hand, String id) {
        boolean food = id.equals("blighted_karambwan") || id.equals("blighted_anglerfish") || id.equals("blighted_manta_ray");
        boolean restore = id.equals("blighted_super_restore");
        if (!food && !restore) {
            return false;
        }
        if (com.runecraft.world.Gielinor.wildernessLevel(p) <= 0) {
            Msg.game(p, "You can only use this in the Wilderness.");
            return true;
        }
        if (food) {
            return false;
        }
        com.runecraft.player.PlayerData data = com.runecraft.player.Players.data(p);
        int prayer = com.runecraft.osrs.data.Potion.SUPER_RESTORE.apply(data.skills());
        prayer += com.runecraft.skill.ItemUses.wrenchBonus(p, 25);
        if (prayer > 0) {
            int cap = Math.max(data.skills().level(Skill.PRAYER), data.skills().currentLevel(Skill.PRAYER));
            data.setPrayerPoints(Math.max(data.prayerPoints(), Math.min(cap, data.prayerPoints() + prayer)));
        }
        int used = stack.getDamageValue() + 1;
        p.getCooldowns().addCooldown(stack.getItem(), 3 * com.runecraft.action.ActionManager.MC_TICKS_PER_GAME_TICK);
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_DRINK,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.0f);
        Msg.action(p, "You drink some of your blighted super restore.");
        if (used >= 4) {
            p.setItemInHand(hand, Inv.stack("vial", 1));
        } else {
            stack.setDamageValue(used);
        }
        com.runecraft.network.ServerNetworking.sync(p);
        return true;
    }

    /**
     * A Wintertodt supply crate from before the reward cart (October 2024): N rolls of the wiki reward table (uniques first,
     * then a skill/coin table), with the player's levels and uniques owned now.
     */
    public static void openSupplyCrate(ServerPlayer p, int rolls) {
        WintertodtRewards.Player who = com.runecraft.content.minigames.games.WintertodtSession.cartPlayer(p);
        for (int i = 0; i < rolls; i++) {
            MinigameReward r = WintertodtRewards.roll(who, RANDOM::nextDouble);
            who = who.after(r.item());
            if (r.item().equals("phoenix")) {
                pet(p, "phoenix");
                continue;
            }
            give(p, r.item(), r.amount());
        }
        Msg.game(p, "You open the supply crate (" + rolls + " roll" + (rolls == 1 ? "" : "s") + ").");
    }

    /** Guardians of the Rift intricate pouch (OSRS Wiki table; the locket and lost bag only while never owned). */
    public static void openPouch(ServerPlayer p) {
        java.util.Set<String> log = com.runecraft.player.Players.data(p).collectionLog();
        java.util.List<String> owned = new java.util.ArrayList<>();
        if (Inv.has(p, "tarnished_locket", 1) || Inv.has(p, "amulet_of_the_eye", 1) || log.contains("tarnished_locket")
                || log.contains("amulet_of_the_eye")) {
            owned.add("tarnished_locket");
        }
        if (Inv.has(p, GuardiansRewards.LOST_BAG, 1) || log.contains(GuardiansRewards.LOST_BAG)) {
            owned.add(GuardiansRewards.LOST_BAG);
        }
        for (MinigameReward r : GuardiansRewards.openIntricatePouch(RANDOM::nextDouble, owned)) {
            if (r.item().equals("pure_essence")) {
                giveNoted(p, r.item(), r.amount());
            } else {
                give(p, r.item(), r.amount());
            }
        }
    }

    /** The Tempoross reward pool's casket (OSRS Wiki "Casket (Reward pool)"): one table line (noted) and maybe a clue. */
    public static void openPoolCasket(ServerPlayer p) {
        for (MinigameReward r : com.runecraft.osrs.content.minigames.TemporossRewards.openCasket(RANDOM::nextDouble)) {
            if (r.amount() > 1 && !r.item().equals("coins")) {
                giveNoted(p, r.item(), r.amount());
            } else {
                give(p, r.item(), r.amount());
            }
        }
        Msg.game(p, "You open the casket.");
    }

    /** One Rewards Guardian search (costs one elemental and one catalytic point, paid by the caller). */
    public static void riftSearch(ServerPlayer p) {
        java.util.List<String> owned = new java.util.ArrayList<>();
        java.util.List<String> check = new java.util.ArrayList<>(GuardiansRewards.POUCHES);
        check.addAll(GuardiansRewards.ONCE_ONLY);
        check.add("colossal_pouch");
        for (String item : check) {
            if (Inv.has(p, item, 1) || com.runecraft.player.Players.data(p).collectionLog().contains(item)) {
                owned.add(item);
            }
        }
        int rc = com.runecraft.player.Players.data(p).skills().level(Skill.RUNECRAFT);
        for (MinigameReward r : GuardiansRewards.search(rc, owned, RANDOM::nextDouble)) {
            if (r.item().equals("abyssal_protector")) {
                pet(p, "abyssal_protector");
            } else {
                give(p, r.item(), r.amount());
            }
        }
    }

    /** Gnome Restaurant reward box. */
    public static void openAluftBox(ServerPlayer p) {
        Object[] dish = MinigameItems.GNOME_DISHES[RANDOM.nextInt(MinigameItems.GNOME_DISHES.length)];
        give(p, (String) dish[0], 1);
        give(p, "coins", 200 + RANDOM.nextInt(800));
        if (RANDOM.nextInt(10) == 0) {
            give(p, "mint_cake", 1);
        }
        if (RANDOM.nextInt(40) == 0) {
            give(p, RANDOM.nextBoolean() ? "gnome_scarf" : "gnome_goggles", 1);
        }
        if (RANDOM.nextInt(20) == 0) {
            give(p, Drops.rollGem(p.getRandom()), 1);
        }
    }

    /** Bones to Peaches (unlocked at the Mage Training Arena): 2 nature, 4 water, 4 earth runes. */
    static void bonesToPeaches(ServerPlayer p) {
        if (!MinigameData.of(p).flag(p.getUUID(), "bones_to_peaches")) {
            Msg.game(p, "You haven't unlocked Bones to Peaches. Visit the Mage Training Arena.");
            return;
        }
        if (com.runecraft.player.Players.data(p).skills().level(Skill.MAGIC) < 60) {
            Msg.game(p, "You need a Magic level of 60 to cast Bones to Peaches.");
            return;
        }
        int bones = Inv.count(p, "bones") + Inv.count(p, "big_bones");
        if (bones == 0) {
            Msg.game(p, "You don't have any bones to convert.");
            return;
        }
        if (!Inv.has(p, "nature_rune", 2) || !Inv.has(p, "water_rune", 4) || !Inv.has(p, "earth_rune", 4)) {
            Msg.game(p, "You need 2 nature, 4 water and 4 earth runes to cast this spell.");
            return;
        }
        Inv.remove(p, "nature_rune", 2);
        Inv.remove(p, "water_rune", 4);
        Inv.remove(p, "earth_rune", 4);
        Inv.remove(p, "bones", 1000);
        Inv.remove(p, "big_bones", 1000);
        Inv.give(p, "peach", bones);
        XpService.add(p, Skill.MAGIC, 35.5);
        Msg.game(p, "You turn your bones into peaches.");
    }
}
