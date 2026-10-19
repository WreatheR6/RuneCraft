package com.runecraft.content.extras;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.extras.Seafaring;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import java.util.LinkedHashMap;
import java.util.Map;

/** Entity types of the extras module: the pet follower and one boat type per Sailing boat tier. */
public final class ExtrasEntities {
    public static EntityType<PetEntity> PET;
    /** Boat tier id -> entity type. */
    public static final Map<String, EntityType<SailBoatEntity>> BOATS = new LinkedHashMap<>();

    private ExtrasEntities() {
    }

    static void register() {
        if (PET != null) {
            return;
        }
        PET = Registry.register(BuiltInRegistries.ENTITY_TYPE, RuneCraft.id("ext_pet"),
                FabricEntityTypeBuilder.<PetEntity>createMob()
                        .spawnGroup(MobCategory.MISC)
                        .entityFactory(PetEntity::new)
                        .dimensions(EntityDimensions.fixed(0.5f, 0.7f))
                        .defaultAttributes(PetEntity::createAttributes)
                        .trackRangeBlocks(48)
                        .disableSaving()
                        .build());
        for (Seafaring.BoatTier tier : Seafaring.BOATS) {
            EntityType<SailBoatEntity> type = FabricEntityTypeBuilder.<SailBoatEntity>create(MobCategory.MISC,
                            (t, world) -> new SailBoatEntity(t, world, tier))
                    .dimensions(EntityDimensions.scalable(1.375f, 0.5625f))
                    .trackRangeChunks(10)
                    .build();
            BOATS.put(tier.id(), Registry.register(BuiltInRegistries.ENTITY_TYPE, RuneCraft.id("ext_boat_" + tier.id()), type));
        }
    }
}
