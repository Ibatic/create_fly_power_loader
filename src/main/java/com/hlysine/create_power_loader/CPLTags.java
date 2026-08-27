package com.hlysine.create_power_loader;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class CPLTags {

    public enum AllEntityTags {

        CHUNK_LOADER_CAPTURABLE,

        ;

        public final TagKey<EntityType<?>> tag;

        AllEntityTags() {
            tag = TagKey.create(
                    Registries.ENTITY_TYPE,
                    Identifier.fromNamespaceAndPath(CreatePowerLoader.MODID, name().toLowerCase(java.util.Locale.ROOT))
            );
        }

        public boolean matches(EntityType<?> type) {
            return type.builtInRegistryHolder().is(tag);
        }

        public boolean matches(Entity entity) {
            return matches(entity.getType());
        }
    }
}
