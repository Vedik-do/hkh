package com.pavithra.cataclysmomcompat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Entity IDs are the Cataclysm boss entity resource locations.
 * (e.g. /summon cataclysm:ignis)
 */
public enum BossType {
    HARBINGER(new ResourceLocation("cataclysm", "the_harbinger"), ModSounds.HARBINGER_IDLE, ModSounds.HARBINGER_COMBAT),
    ENDER_GUARDIAN(new ResourceLocation("cataclysm", "ender_guardian"), ModSounds.ENDER_GUARDIAN, ModSounds.ENDER_GUARDIAN),
    ANCIENT_REMNANT(new ResourceLocation("cataclysm", "ancient_remnant"), ModSounds.ANCIENT_REMNANT, ModSounds.ANCIENT_REMNANT),
    LEVIATHAN(new ResourceLocation("cataclysm", "the_leviathan"), ModSounds.LEVIATHAN, ModSounds.LEVIATHAN),
    IGNIS(new ResourceLocation("cataclysm", "ignis"), ModSounds.IGNIS, ModSounds.IGNIS),
    MALEDICTUS(new ResourceLocation("cataclysm", "maledictus"), ModSounds.MALEDICTUS, ModSounds.MALEDICTUS),
    NETHERITE_MONSTROSITY(new ResourceLocation("cataclysm", "netherite_monstrosity"), ModSounds.NETHERITE_MONSTROSITY, ModSounds.NETHERITE_MONSTROSITY),
    SCYLLA(new ResourceLocation("cataclysm", "scylla"), ModSounds.SCYLLA, ModSounds.SCYLLA);

    public final ResourceLocation entityId;
    private final RegistryObject<SoundEvent> idleSound;
    private final RegistryObject<SoundEvent> combatSound;

    BossType(ResourceLocation entityId, RegistryObject<SoundEvent> idleSound, RegistryObject<SoundEvent> combatSound) {
        this.entityId = entityId;
        this.idleSound = idleSound;
        this.combatSound = combatSound;
    }

    public RegistryObject<SoundEvent> soundForState(boolean combat) {
        return combat ? combatSound : idleSound;
    }
}
