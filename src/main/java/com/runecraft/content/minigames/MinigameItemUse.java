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
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;

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
            NbtCompound nbt = stack.getOrCreateNbt();
            nbt.putInt(ROLLS, rolls);
        }
        return stack;
    }

    private static int rolls(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.contains(ROLLS) ? Math.max(1, nbt.getInt(ROLLS)) : 1;
    }

    static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient() || !(player instanceof ServerPlayerEntity sp) || stack.isEmpty()) {
                return TypedActionResult.pass(stack);
            }
            String id = Inv.idOf(stack);
            boolean used = switch (id) {
                case "supply_crate" -> {
                    openSupplyCrate(sp, rolls(stack));
                    yield true;
                }
                case "intricate_pouch" -> {
                    openPouch(sp);
                    yield true;
                }
                case "aluft_aloft_box" -> {
                    openAluftBox(sp);
                    yield true;
                }
                case "bones_to_peaches_tablet" -> {
                    bonesToPeaches(sp);
                    yield false;
                }
                default -> false;
            };
            if (used) {
                stack.decrement(1);
                return TypedActionResult.success(stack);
            }
            return TypedActionResult.pass(stack);
        });
    }

    static void give(ServerPlayerEntity p, String item, int count) {
        if (item == null || count <= 0) {
            return;
        }
        Inv.give(p, item, count);
        CollectionLog.obtain(p, item);
    }

    private static void pet(ServerPlayerEntity p, String pet) {
        give(p, pet, 1);
        p.getServer().getPlayerManager().broadcast(Text.literal(p.getName().getString()
                + " has a funny feeling like they're being followed.").formatted(Formatting.GOLD), false);
    }

    /** Wintertodt supply crate: N rolls of the wiki reward table (uniques first, then a skill/coin table). */
    public static void openSupplyCrate(ServerPlayerEntity p, int rolls) {
        java.util.Set<String> log = com.runecraft.player.Players.data(p).collectionLog();
        com.runecraft.osrs.SkillSet skills = com.runecraft.player.Players.data(p).skills();
        WintertodtRewards.Player who = new WintertodtRewards.Player(skills.level(Skill.CRAFTING), skills.level(Skill.HERBLORE),
                skills.level(Skill.FARMING), skills.level(Skill.WOODCUTTING), skills.level(Skill.MINING), skills.level(Skill.FISHING),
                log.contains("phoenix") || Inv.has(p, "phoenix", 1), log.contains("warm_gloves"), log.contains("bruma_torch"));
        for (int i = 0; i < rolls; i++) {
            MinigameReward r = WintertodtRewards.roll(who, RANDOM::nextDouble);
            if (r.item().equals("phoenix")) {
                pet(p, "phoenix");
                who = new WintertodtRewards.Player(who.crafting(), who.herblore(), who.farming(), who.woodcutting(), who.mining(),
                        who.fishing(), true, who.hasWarmGloves(), who.hasBrumaTorch());
                continue;
            }
            give(p, r.item(), r.amount());
        }
        Msg.game(p, "You open the supply crate (" + rolls + " roll" + (rolls == 1 ? "" : "s") + ").");
    }

    /** Guardians of the Rift intricate pouch (approx. contents; 1/200 blood essence pre-roll). */
    public static void openPouch(ServerPlayerEntity p) {
        for (MinigameReward r : GuardiansRewards.openIntricatePouch(RANDOM::nextDouble)) {
            give(p, r.item(), r.amount());
        }
    }

    /** One Rewards Guardian search (costs one elemental and one catalytic point, paid by the caller). */
    public static void riftSearch(ServerPlayerEntity p) {
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
    public static void openAluftBox(ServerPlayerEntity p) {
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
    static void bonesToPeaches(ServerPlayerEntity p) {
        if (!MinigameData.of(p).flag(p.getUuid(), "bones_to_peaches")) {
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
