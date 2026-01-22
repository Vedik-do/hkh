package com.pavithra.cataclysmomcompat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, CataclysmOMCompat.MODID);

    // These IDs match your resource pack's sounds.json
    public static final RegistryObject<SoundEvent> HARBINGER_IDLE = register("music.harbinger.idle");
    public static final RegistryObject<SoundEvent> HARBINGER_COMBAT = register("music.harbinger.combat");

    public static final RegistryObject<SoundEvent> ENDER_GUARDIAN = register("music.ender_guardian.theme");
    public static final RegistryObject<SoundEvent> ANCIENT_REMNANT = register("music.ancient_remnant.combat");
    public static final RegistryObject<SoundEvent> LEVIATHAN = register("music.leviathan.combat");
    public static final RegistryObject<SoundEvent> IGNIS = register("music.ignis.combat");
    public static final RegistryObject<SoundEvent> MALEDICTUS = register("music.maledictus.combat");
    public static final RegistryObject<SoundEvent> NETHERITE_MONSTROSITY = register("music.netherite_monstrosity.combat");
    public static final RegistryObject<SoundEvent> SCYLLA = register("music.scylla.combat");

    private static RegistryObject<SoundEvent> register(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(CataclysmOMCompat.MODID, id)));
    }

    private ModSounds() {}
}
