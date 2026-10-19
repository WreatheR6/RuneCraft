package com.runecraft.content.extras;

import com.runecraft.RuneCraft;
import com.runecraft.osrs.content.extras.Seafaring;
import com.runecraft.registry.MobTypeBuilder;
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

    /** Called from the entity type registration event (see {@code RuneCraft}). */
    public static void register() {
        if (PET != null) {
            return;
        }
        PET = MobTypeBuilder.<PetEntity>create(PetEntity::new, MobCategory.MISC)
                .size(0.5f, 0.7f)
                .defaultAttributes(PetEntity::createAttributes)
                .trackRangeBlocks(48)
                .disableSaving()
                .register(RuneCraft.id("ext_pet"));
        for (Seafaring.BoatTier tier : Seafaring.BOATS) {
            EntityType<SailBoatEntity> type = MobTypeBuilder.<SailBoatEntity>create((t, world) -> new SailBoatEntity(t, world, tier), MobCategory.MISC)
                    .size(1.375f, 0.5625f)
                    .trackRangeChunks(10)
                    .register(RuneCraft.id("ext_boat_" + tier.id()));
            BOATS.put(tier.id(), type);
        }
    }
}
