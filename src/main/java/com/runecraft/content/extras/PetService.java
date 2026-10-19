package com.runecraft.content.extras;

import com.runecraft.action.ActionManager;
import com.runecraft.collection.CollectionLog;
import com.runecraft.content.BlockHooks;
import com.runecraft.content.GameEvents;
import com.runecraft.osrs.Skill;
import com.runecraft.osrs.content.extras.ExtrasContent;
import com.runecraft.osrs.content.extras.Herbiboar;
import com.runecraft.osrs.content.extras.Pets;
import com.runecraft.osrs.content.extras.SkillingPets;
import com.runecraft.skill.XpService;
import com.runecraft.util.Inv;
import com.runecraft.util.Msg;
import com.runecraft.world.LandmarkBuilder;
import com.runecraft.platform.ServerTickEvents;
import com.runecraft.platform.UseItemCallback;
import com.runecraft.platform.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pets: skilling pet rolls (per gathering action, Thieving action, Agility lap and runecraft),
 * Chompy chick and Herbi, followers (use a pet item to have it follow you, right-click it to pick
 * it up; one follower at a time) and pet house storage.
 */
final class PetService {
    private static final Map<UUID, PetEntity> ACTIVE = new HashMap<>();
    /** Last skilling item obtained this tick, paired with the XP drop that follows it. */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Long> HERBIBOAR_COOLDOWN = new HashMap<>();

    private record Pending(String item, long tick) {
    }

    /** The pickpocket target or stall of a Thieving action this tick ("thieve:" activity), paired with its XP drop. */
    private static final Map<UUID, Pending> THIEVING_SOURCE = new HashMap<>();

    private PetService() {
    }

    static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getItemInHand(hand);
            String id = stack.isEmpty() ? null : Inv.idOf(stack);
            if (id == null || !Pets.isPet(id)) {
                return InteractionResultHolder.pass(stack);
            }
            if (!world.isClientSide() && player instanceof ServerPlayer sp) {
                drop(sp, stack, id);
            }
            return InteractionResultHolder.success(stack);
        });
        GameEvents.onObtain((player, item, count) -> {
            if (SkillingPets.forItem(item) != null) {
                PENDING.put(player.getUUID(), new Pending(item, player.getServer().getTickCount()));
            }
        });
        GameEvents.onActivity((player, key) -> {
            if (key.startsWith("thieve:")) {
                THIEVING_SOURCE.put(player.getUUID(), new Pending(key.substring("thieve:".length()), player.getServer().getTickCount()));
            }
        });
        GameEvents.onXp(PetService::onXp);
        GameEvents.onKill((player, entity, stats) -> {
            if (stats.profile() != null && "chompy_bird".equals(stats.profile().id)) {
                rollBoss(player, "chompy_bird", "Chompy chick");
            }
        });
        BlockHooks.register(ExtrasContent.HERBIBOAR_BURROW, PetService::herbiboar);
        LandmarkBuilder.registerCustom(ExtrasContent.HERBIBOAR_SITE, (world, centre) -> {
            BlockState burrow = ExtrasUtil.block(ExtrasContent.HERBIBOAR_BURROW);
            if (burrow != null) {
                world.setBlockAndUpdate(new BlockPos(centre.getX(), ExtrasUtil.surfaceY(world, centre.getX(), centre.getZ()), centre.getZ()),
                        burrow);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTickCount() % 20 == 0) {
                ensureFollowers(server);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PetEntity e = ACTIVE.remove(handler.player.getUUID());
            if (e != null) {
                e.discard();
            }
            PENDING.remove(handler.player.getUUID());
            THIEVING_SOURCE.remove(handler.player.getUUID());
        });
    }

    // ------------------------------------------------------------------ rolls

    private static void onXp(ServerPlayer player, Skill skill, int tenths) {
        RandomSource random = player.getRandom();
        switch (skill) {
            case THIEVING -> {
                Pending src = THIEVING_SOURCE.remove(player.getUUID());
                String source = src != null && src.tick() == player.getServer().getTickCount() ? src.item() : null;
                int base = SkillingPets.thievingBase(source, tenths);
                if (base > 0) {
                    rollSkilling(player, SkillingPets.ROCKY, skill, base, random);
                }
            }
            case RUNECRAFT -> rollSkilling(player, SkillingPets.RIFT_GUARDIAN, skill, SkillingPets.RUNECRAFT_BASE, random);
            case AGILITY -> {
                int base = SkillingPets.agilityLapBase(tenths);
                if (base > 0) {
                    rollSkilling(player, SkillingPets.GIANT_SQUIRREL, skill, base, random);
                }
            }
            default -> {
                Pending p = PENDING.remove(player.getUUID());
                if (p == null || p.tick() != player.getServer().getTickCount()) {
                    return;
                }
                SkillingPets.Roll roll = SkillingPets.forItem(p.item());
                if (roll != null && roll.skill() == skill) {
                    rollSkilling(player, roll.pet(), skill, roll.base(), random);
                }
            }
        }
    }

    private static void rollSkilling(ServerPlayer player, String pet, Skill skill, int base, RandomSource random) {
        if (random.nextDouble() < Pets.skillingChance(base, ExtrasUtil.level(player, skill))) {
            award(player, pet);
        }
    }

    /** Rolls a non-engine pet (Herbi, Chompy chick) at its real rate. */
    private static void rollBoss(ServerPlayer player, String source, String what) {
        Pets.PetDef pet = Pets.bySource(source);
        int rate = pet == null ? 0 : Pets.rate(pet.id(), source);
        if (rate > 0 && player.getRandom().nextInt(rate) == 0) {
            award(player, pet.id());
        }
    }

    /** Gives a pet: it starts following if there's no follower, otherwise it goes into the backpack. */
    static void award(ServerPlayer player, String petId) {
        Pets.PetDef pet = Pets.get(petId);
        if (pet == null) {
            return;
        }
        ExtrasState state = ExtrasState.get(player.getServer());
        String key = player.getStringUUID();
        if (!state.section("followers").contains(key)) {
            state.section("followers").putString(key, petId);
            spawn(player, petId);
            Msg.red(player, "You have a funny feeling like you're being followed.");
        } else {
            Inv.give(player, petId, 1);
            Msg.red(player, "You feel something weird sneaking into your backpack.");
        }
        CollectionLog.obtain(player, petId);
        player.getServer().getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString() + " has received a pet: "
                + pet.name() + "!").withStyle(ChatFormatting.GOLD), false);
    }

    // ------------------------------------------------------------------ followers

    private static void drop(ServerPlayer player, ItemStack stack, String petId) {
        ExtrasState state = ExtrasState.get(player.getServer());
        String key = player.getStringUUID();
        if (state.section("followers").contains(key)) {
            Msg.game(player, "You already have a follower.");
            return;
        }
        stack.shrink(1);
        state.section("followers").putString(key, petId);
        spawn(player, petId);
        Msg.game(player, "Your " + Pets.get(petId).name().toLowerCase() + " starts following you.");
    }

    /** Right-click on a pet: its owner picks it up; menagerie pets go back into the pet house. */
    static void interact(ServerPlayer player, PetEntity pet) {
        if (pet.roaming()) {
            Msg.game(player, "The " + Pets.get(pet.petId()).name().toLowerCase() + " is enjoying the menagerie.");
            return;
        }
        if (!player.getUUID().equals(pet.owner())) {
            Msg.game(player, "This isn't your pet.");
            return;
        }
        if (!Inv.hasRoomFor(player, pet.petId())) {
            Msg.game(player, "You don't have enough inventory space to pick up your pet.");
            return;
        }
        ExtrasState.get(player.getServer()).section("followers").remove(player.getStringUUID());
        ACTIVE.remove(player.getUUID());
        pet.discard();
        Inv.give(player, pet.petId(), 1);
        Msg.game(player, "You pick up your pet.");
    }

    /** Removes the follower (into the inventory) - used by the pet house. Returns the pet id or null. */
    static String takeFollower(ServerPlayer player) {
        ExtrasState state = ExtrasState.get(player.getServer());
        String key = player.getStringUUID();
        if (!state.section("followers").contains(key)) {
            return null;
        }
        String id = state.section("followers").getString(key);
        state.section("followers").remove(key);
        PetEntity e = ACTIVE.remove(player.getUUID());
        if (e != null) {
            e.discard();
        }
        return id;
    }

    private static void spawn(ServerPlayer player, String petId) {
        PetEntity old = ACTIVE.remove(player.getUUID());
        if (old != null) {
            old.discard();
        }
        if (ExtrasEntities.PET == null || player.isSpectator()) {
            return;
        }
        ServerLevel world = (ServerLevel) player.level();
        PetEntity pet = ExtrasEntities.PET.create(world);
        if (pet == null) {
            return;
        }
        pet.setPetId(petId);
        pet.setOwner(player.getUUID());
        Pets.PetDef def = Pets.get(petId);
        if (def != null) {
            pet.setCustomName(Component.literal(def.name()));
        }
        pet.moveTo(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYRot(), 0);
        world.addFreshEntity(pet);
        world.sendParticles(ParticleTypes.POOF, pet.getX(), pet.getY() + 0.3, pet.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        ACTIVE.put(player.getUUID(), pet);
    }

    private static void ensureFollowers(MinecraftServer server) {
        CompoundTag followers = ExtrasState.get(server).section("followers");
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            String key = player.getStringUUID();
            if (!followers.contains(key) || !player.isAlive()) {
                continue;
            }
            PetEntity e = ACTIVE.get(player.getUUID());
            if (e == null || e.isRemoved() || e.level() != player.level()) {
                spawn(player, followers.getString(key));
            }
        }
    }

    // ------------------------------------------------------------------ pet house

    static List<String> stored(ServerPlayer player) {
        CompoundTag c = ExtrasState.get(player.getServer()).entry("pet_house", player.getStringUUID());
        return new ArrayList<>(c.getAllKeys());
    }

    /** Pet house capacity by tier (OSRS: an oak pet house holds fewer pets than a natural one; approx). */
    static int capacity(int tier) {
        return tier >= 3 ? 1000 : 10 * tier;
    }

    static void store(ServerPlayer player, String petId, int tier) {
        CompoundTag c = ExtrasState.get(player.getServer()).entry("pet_house", player.getStringUUID());
        if (c.getAllKeys().size() >= capacity(tier) && !c.contains(petId)) {
            Msg.game(player, "Your pet house is full.");
            return;
        }
        c.putInt(petId, c.getInt(petId) + 1);
        Msg.game(player, "You put your " + Pets.get(petId).name().toLowerCase() + " in the pet house.");
    }

    static void take(ServerPlayer player, String petId) {
        CompoundTag c = ExtrasState.get(player.getServer()).entry("pet_house", player.getStringUUID());
        int n = c.getInt(petId);
        if (n <= 0) {
            Msg.game(player, "That pet isn't in your pet house.");
            return;
        }
        if (!Inv.hasRoomFor(player, petId)) {
            Msg.game(player, "You don't have enough inventory space.");
            return;
        }
        if (n == 1) {
            c.remove(petId);
        } else {
            c.putInt(petId, n - 1);
        }
        Inv.give(player, petId, 1);
        Msg.game(player, "You take your " + Pets.get(petId).name().toLowerCase() + " out of the pet house.");
    }

    /** Pet house click: store the held pet (or the follower), or list stored pets with "take" links. */
    static void petHouse(ServerPlayer player, int tier) {
        ItemStack held = player.getMainHandItem();
        String id = held.isEmpty() ? null : Inv.idOf(held);
        if (id != null && Pets.isPet(id)) {
            held.shrink(1);
            store(player, id, tier);
            return;
        }
        if (player.isShiftKeyDown()) {
            String follower = takeFollower(player);
            if (follower != null) {
                store(player, follower, tier);
                return;
            }
        }
        List<String> pets = stored(player);
        if (pets.isEmpty()) {
            Msg.game(player, "Your pet house is empty. Use a pet on it (or sneak and click it) to store your follower.");
            return;
        }
        List<MutableComponent> links = new ArrayList<>();
        for (String p : pets) {
            Pets.PetDef def = Pets.get(p);
            links.add(ExtrasUtil.link(def == null ? p : def.name(), "/house pet " + p, "Take this pet out", ChatFormatting.GOLD));
        }
        ExtrasUtil.options(player, "Pets in your house (" + pets.size() + "/" + Math.min(capacity(tier), 999) + "):", links);
    }

    // ------------------------------------------------------------------ Herbiboar

    private static void herbiboar(ServerPlayer player, BlockPos pos, BlockState state) {
        if (ExtrasUtil.level(player, Skill.HUNTER) < Herbiboar.HUNTER_LEVEL) {
            Msg.game(player, "You need a Hunter level of 80 to track the herbiboar.");
            return;
        }
        if (ExtrasUtil.level(player, Skill.HERBLORE) < Herbiboar.HERBLORE_LEVEL) {
            Msg.game(player, "You need a Herblore level of 31 to harvest herbs from the herbiboar.");
            return;
        }
        long now = ActionManager.gameTick();
        Long next = HERBIBOAR_COOLDOWN.get(player.getUUID());
        if (next != null && now < next) {
            Msg.game(player, "The herbiboar's tracks are still faint. Wait a while before tracking it again.");
            return;
        }
        HERBIBOAR_COOLDOWN.put(player.getUUID(), now + 50);
        RandomSource random = player.getRandom();
        int hunter = ExtrasUtil.level(player, Skill.HUNTER);
        // Trail searches (50 XP each, chance of a fossil), then the harvest (OSRS Wiki, Module:Herbiboar).
        int searches = Herbiboar.searches(random.nextDouble());
        for (int i = 0; i < searches; i++) {
            XpService.add(player, Skill.HUNTER, Herbiboar.SEARCH_XP);
            String fossil = Herbiboar.searchFossil(random::nextDouble);
            if (fossil != null) {
                Inv.give(player, fossil, 1);
                Msg.game(player, "You find an unidentified fossil while searching the trail.");
            }
        }
        XpService.add(player, Skill.HUNTER, Herbiboar.harvestXp(hunter));
        boolean secateurs = Inv.has(player, "magic_secateurs", 1);
        for (String herb : Herbiboar.harvest(ExtrasUtil.level(player, Skill.HERBLORE), secateurs, random::nextDouble)) {
            Inv.give(player, herb, 1);
            CollectionLog.obtain(player, herb);
        }
        ServerLevel world = (ServerLevel) player.level();
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 10, 0.4, 0.3, 0.4, 0.01);
        world.playSound(null, pos, SoundEvents.HOGLIN_AMBIENT, SoundSource.NEUTRAL, 0.8f, 1.4f);
        Msg.action(player, "You stun the herbiboar and harvest some herbs from its back.");
        rollBoss(player, "herbiboar", "Herbi");
    }
}
