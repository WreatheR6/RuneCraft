package com.runecraft.content.extras;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.extras.Seafaring;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

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
        PET = Registry.register(Registries.ENTITY_TYPE, RuneCraft.id("ext_pet"),
                FabricEntityTypeBuilder.<PetEntity>createMob()
                        .spawnGroup(SpawnGroup.MISC)
                        .entityFactory(PetEntity::new)
                        .dimensions(EntityDimensions.fixed(0.5f, 0.7f))
                        .defaultAttributes(PetEntity::createAttributes)
                        .trackRangeBlocks(48)
                        .disableSaving()
                        .build());
        for (Seafaring.BoatTier tier : Seafaring.BOATS) {
            EntityType<SailBoatEntity> type = FabricEntityTypeBuilder.<SailBoatEntity>create(SpawnGroup.MISC,
                            (t, world) -> new SailBoatEntity(t, world, tier))
                    .dimensions(EntityDimensions.changing(1.375f, 0.5625f))
                    .trackRangeChunks(10)
                    .build();
            BOATS.put(tier.id(), Registry.register(Registries.ENTITY_TYPE, RuneCraft.id("ext_boat_" + tier.id()), type));
        }
    }
}
