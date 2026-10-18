package de.pf2craft.npc;

import de.pf2craft.Pf2Craft;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Entity types of the mod. Registered on both sides so dedicated servers know them. */
public final class Pf2Entities {
    public static final EntityType<NpcEntity> NPC = Registry.register(Registries.ENTITY_TYPE, Pf2Craft.id("npc"),
            EntityType.Builder.create(NpcEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.95f)
                    .maxTrackingRange(10)
                    .build("npc"));

    private Pf2Entities() {
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(NPC, VillagerEntity.createVillagerAttributes());
    }
}
