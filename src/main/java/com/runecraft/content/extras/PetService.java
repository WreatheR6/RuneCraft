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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

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

    private PetService() {
    }

    static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            String id = stack.isEmpty() ? null : Inv.idOf(stack);
            if (id == null || !Pets.isPet(id)) {
                return TypedActionResult.pass(stack);
            }
            if (!world.isClient() && player instanceof ServerPlayerEntity sp) {
                drop(sp, stack, id);
            }
            return TypedActionResult.success(stack);
        });
        GameEvents.onObtain((player, item, count) -> {
            if (SkillingPets.forItem(item) != null) {
                PENDING.put(player.getUuid(), new Pending(item, player.getServer().getTicks()));
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
                world.setBlockState(new BlockPos(centre.getX(), ExtrasUtil.surfaceY(world, centre.getX(), centre.getZ()), centre.getZ()),
                        burrow);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 20 == 0) {
                ensureFollowers(server);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            PetEntity e = ACTIVE.remove(handler.player.getUuid());
            if (e != null) {
                e.discard();
            }
            PENDING.remove(handler.player.getUuid());
        });
    }

    // ------------------------------------------------------------------ rolls

    private static void onXp(ServerPlayerEntity player, Skill skill, int tenths) {
        Random random = player.getRandom();
        switch (skill) {
            case THIEVING -> rollSkilling(player, SkillingPets.ROCKY, skill, SkillingPets.thievingBase(tenths), random);
            case RUNECRAFT -> rollSkilling(player, SkillingPets.RIFT_GUARDIAN, skill, SkillingPets.RUNECRAFT_BASE, random);
            case AGILITY -> {
                int base = SkillingPets.agilityLapBase(tenths);
                if (base > 0) {
                    rollSkilling(player, SkillingPets.GIANT_SQUIRREL, skill, base, random);
                }
            }
            default -> {
                Pending p = PENDING.remove(player.getUuid());
                if (p == null || p.tick() != player.getServer().getTicks()) {
                    return;
                }
                SkillingPets.Roll roll = SkillingPets.forItem(p.item());
                if (roll != null && roll.skill() == skill) {
                    rollSkilling(player, roll.pet(), skill, roll.base(), random);
                }
            }
        }
    }

    private static void rollSkilling(ServerPlayerEntity player, String pet, Skill skill, int base, Random random) {
        if (random.nextDouble() < Pets.skillingChance(base, ExtrasUtil.level(player, skill))) {
            award(player, pet);
        }
    }

    /** Rolls a non-engine pet (Herbi, Chompy chick) at its real rate. */
    private static void rollBoss(ServerPlayerEntity player, String source, String what) {
        Pets.PetDef pet = Pets.bySource(source);
        int rate = pet == null ? 0 : Pets.rate(pet.id(), source);
        if (rate > 0 && player.getRandom().nextInt(rate) == 0) {
            award(player, pet.id());
        }
    }

    /** Gives a pet: it starts following if there's no follower, otherwise it goes into the backpack. */
    static void award(ServerPlayerEntity player, String petId) {
        Pets.PetDef pet = Pets.get(petId);
        if (pet == null) {
            return;
        }
        ExtrasState state = ExtrasState.get(player.getServer());
        String key = player.getUuidAsString();
        if (!state.section("followers").contains(key)) {
            state.section("followers").putString(key, petId);
            spawn(player, petId);
            Msg.red(player, "You have a funny feeling like you're being followed.");
        } else {
            Inv.give(player, petId, 1);
            Msg.red(player, "You feel something weird sneaking into your backpack.");
        }
        CollectionLog.obtain(player, petId);
        player.getServer().getPlayerManager().broadcast(Text.literal(player.getName().getString() + " has received a pet: "
                + pet.name() + "!").formatted(Formatting.GOLD), false);
    }

    // ------------------------------------------------------------------ followers

    private static void drop(ServerPlayerEntity player, ItemStack stack, String petId) {
        ExtrasState state = ExtrasState.get(player.getServer());
        String key = player.getUuidAsString();
        if (state.section("followers").contains(key)) {
            Msg.game(player, "You already have a follower.");
            return;
        }
        stack.decrement(1);
        state.section("followers").putString(key, petId);
        spawn(player, petId);
        Msg.game(player, "Your " + Pets.get(petId).name().toLowerCase() + " starts following you.");
    }

    /** Right-click on a pet: its owner picks it up; menagerie pets go back into the pet house. */
    static void interact(ServerPlayerEntity player, PetEntity pet) {
        if (pet.roaming()) {
            Msg.game(player, "The " + Pets.get(pet.petId()).name().toLowerCase() + " is enjoying the menagerie.");
            return;
        }
        if (!player.getUuid().equals(pet.owner())) {
            Msg.game(player, "This isn't your pet.");
            return;
        }
        if (!Inv.hasRoomFor(player, pet.petId())) {
            Msg.game(player, "You don't have enough inventory space to pick up your pet.");
            return;
        }
        ExtrasState.get(player.getServer()).section("followers").remove(player.getUuidAsString());
        ACTIVE.remove(player.getUuid());
        pet.discard();
        Inv.give(player, pet.petId(), 1);
        Msg.game(player, "You pick up your pet.");
    }

    /** Removes the follower (into the inventory) - used by the pet house. Returns the pet id or null. */
    static String takeFollower(ServerPlayerEntity player) {
        ExtrasState state = ExtrasState.get(player.getServer());
        String key = player.getUuidAsString();
        if (!state.section("followers").contains(key)) {
            return null;
        }
        String id = state.section("followers").getString(key);
        state.section("followers").remove(key);
        PetEntity e = ACTIVE.remove(player.getUuid());
        if (e != null) {
            e.discard();
        }
        return id;
    }

    private static void spawn(ServerPlayerEntity player, String petId) {
        PetEntity old = ACTIVE.remove(player.getUuid());
        if (old != null) {
            old.discard();
        }
        if (ExtrasEntities.PET == null || player.isSpectator()) {
            return;
        }
        ServerWorld world = (ServerWorld) player.getWorld();
        PetEntity pet = ExtrasEntities.PET.create(world);
        if (pet == null) {
            return;
        }
        pet.setPetId(petId);
        pet.setOwner(player.getUuid());
        Pets.PetDef def = Pets.get(petId);
        if (def != null) {
            pet.setCustomName(Text.literal(def.name()));
        }
        pet.refreshPositionAndAngles(player.getX() + 1, player.getY(), player.getZ() + 1, player.getYaw(), 0);
        world.spawnEntity(pet);
        world.spawnParticles(ParticleTypes.POOF, pet.getX(), pet.getY() + 0.3, pet.getZ(), 6, 0.2, 0.2, 0.2, 0.01);
        ACTIVE.put(player.getUuid(), pet);
    }

    private static void ensureFollowers(MinecraftServer server) {
        NbtCompound followers = ExtrasState.get(server).section("followers");
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String key = player.getUuidAsString();
            if (!followers.contains(key) || !player.isAlive()) {
                continue;
            }
            PetEntity e = ACTIVE.get(player.getUuid());
            if (e == null || e.isRemoved() || e.getWorld() != player.getWorld()) {
                spawn(player, followers.getString(key));
            }
        }
    }

    // ------------------------------------------------------------------ pet house

    static List<String> stored(ServerPlayerEntity player) {
        NbtCompound c = ExtrasState.get(player.getServer()).entry("pet_house", player.getUuidAsString());
        return new ArrayList<>(c.getKeys());
    }

    /** Pet house capacity by tier (OSRS: an oak pet house holds fewer pets than a natural one; approx). */
    static int capacity(int tier) {
        return tier >= 3 ? 1000 : 10 * tier;
    }

    static void store(ServerPlayerEntity player, String petId, int tier) {
        NbtCompound c = ExtrasState.get(player.getServer()).entry("pet_house", player.getUuidAsString());
        if (c.getKeys().size() >= capacity(tier) && !c.contains(petId)) {
            Msg.game(player, "Your pet house is full.");
            return;
        }
        c.putInt(petId, c.getInt(petId) + 1);
        Msg.game(player, "You put your " + Pets.get(petId).name().toLowerCase() + " in the pet house.");
    }

    static void take(ServerPlayerEntity player, String petId) {
        NbtCompound c = ExtrasState.get(player.getServer()).entry("pet_house", player.getUuidAsString());
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
    static void petHouse(ServerPlayerEntity player, int tier) {
        ItemStack held = player.getMainHandStack();
        String id = held.isEmpty() ? null : Inv.idOf(held);
        if (id != null && Pets.isPet(id)) {
            held.decrement(1);
            store(player, id, tier);
            return;
        }
        if (player.isSneaking()) {
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
        List<MutableText> links = new ArrayList<>();
        for (String p : pets) {
            Pets.PetDef def = Pets.get(p);
            links.add(ExtrasUtil.link(def == null ? p : def.name(), "/house pet " + p, "Take this pet out", Formatting.GOLD));
        }
        ExtrasUtil.options(player, "Pets in your house (" + pets.size() + "/" + Math.min(capacity(tier), 999) + "):", links);
    }

    // ------------------------------------------------------------------ Herbiboar

    private static void herbiboar(ServerPlayerEntity player, BlockPos pos, BlockState state) {
        if (ExtrasUtil.level(player, Skill.HUNTER) < Herbiboar.HUNTER_LEVEL) {
            Msg.game(player, "You need a Hunter level of 80 to track the herbiboar.");
            return;
        }
        if (ExtrasUtil.level(player, Skill.HERBLORE) < Herbiboar.HERBLORE_LEVEL) {
            Msg.game(player, "You need a Herblore level of 31 to harvest herbs from the herbiboar.");
            return;
        }
        long now = ActionManager.gameTick();
        Long next = HERBIBOAR_COOLDOWN.get(player.getUuid());
        if (next != null && now < next) {
            Msg.game(player, "The herbiboar's tracks are still faint. Wait a while before tracking it again.");
            return;
        }
        HERBIBOAR_COOLDOWN.put(player.getUuid(), now + 50);
        Random random = player.getRandom();
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
        ServerWorld world = (ServerWorld) player.getWorld();
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 10, 0.4, 0.3, 0.4, 0.01);
        world.playSound(null, pos, SoundEvents.ENTITY_HOGLIN_AMBIENT, SoundCategory.NEUTRAL, 0.8f, 1.4f);
        Msg.action(player, "You stun the herbiboar and harvest some herbs from its back.");
        rollBoss(player, "herbiboar", "Herbi");
    }
}
