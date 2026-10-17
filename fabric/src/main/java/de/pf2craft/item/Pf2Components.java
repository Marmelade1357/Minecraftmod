package de.pf2craft.item;

import de.pf2craft.Pf2Craft;
import net.minecraft.component.ComponentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Custom item data components. Registered on both sides because the mod is required everywhere. */
public final class Pf2Components {
    public static final ComponentType<WeaponRef> WEAPON = register("weapon",
            ComponentType.<WeaponRef>builder().codec(WeaponRef.CODEC).packetCodec(WeaponRef.PACKET_CODEC).build());
    public static final ComponentType<ShieldRef> SHIELD = register("shield",
            ComponentType.<ShieldRef>builder().codec(ShieldRef.CODEC).packetCodec(ShieldRef.PACKET_CODEC).build());

    private Pf2Components() {
    }

    /** Forces class loading, which registers the components. Call early in onInitialize. */
    public static void register() {
    }

    private static <T> ComponentType<T> register(String path, ComponentType<T> type) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, Pf2Craft.id(path), type);
    }
}
