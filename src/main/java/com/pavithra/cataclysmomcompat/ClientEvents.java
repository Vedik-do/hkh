package com.pavithra.cataclysmomcompat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;


public class ClientEvents {
    // Keep this similar to your working Mowzie compat, but a bit bigger because Cataclysm bosses are huge.
    private static final double RANGE_BLOCKS = 40.0;
    private static final double RANGE_SQ = RANGE_BLOCKS * RANGE_BLOCKS;

    private static final int FADE_IN_TICKS = 40;
    private static final int FADE_OUT_TICKS = 40;

    // After leaving range, keep muting OverhauledMusic for a while to prevent abrupt overlap if you kite the boss.
    private static final int KEEP_MUTE_TICKS = 60 * 20; // 60s

    private boolean cataclysmLoaded = false;

    private BossThemeSoundInstance bossSound = null;
    private BossType bossType = null;
    private Entity bossEntity = null;

    private boolean wasInRange = false;
    private int ticksSinceOutOfRange = 0;

    // Harbinger idle/combat switch (debounced)
    private boolean harbingerCombat = false;
    private Boolean pendingHarbingerCombat = null;
private int pendingHarbingerTicks = 0;

// Harbinger combat should only trigger after the player is hit by the Harbinger.
// We keep combat active for a short grace window after a hit so it doesn't instantly drop.
private int harbingerCombatTicksLeft = 0;
private int lastPlayerHurtTime = 0;

    public ClientEvents() {
        this.cataclysmLoaded = ModList.get().isLoaded("cataclysm");
        if (!cataclysmLoaded) {
            CataclysmOMCompat.LOGGER.warn("[cataclysmomcompat] Cataclysm not detected. This compat mod will do nothing.");
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!cataclysmLoaded) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        Level level = mc.level;
        if (player == null || level == null) return;

        updateHarbingerCombatTimer(player);

        BossHit hit = findBossInRange(player);
        boolean inRange = hit != null;

        if (bossSound != null && bossSound.isStopped()) {
            bossSound = null;
            bossType = null;
            bossEntity = null;
            wasInRange = false;
            ticksSinceOutOfRange = 0;
        }

        if (inRange) {
            ticksSinceOutOfRange = 0;

            // if boss changed, start new sound
            if (bossType != hit.type || bossSound == null) {
                startBossTheme(mc, hit);
            } else if (bossType == BossType.HARBINGER) {
                // only Harbinger switches between idle/combat
                handleHarbingerSwitch(mc, hit);
            } else if (!wasInRange && bossSound != null) {
                bossSound.fadeTo(1.0f, FADE_IN_TICKS);
            }

            wasInRange = true;
            applyOverhauledMusicBossOverride(true);

        } else {
            // Out of range
            if (wasInRange && bossSound != null) {
                bossSound.fadeTo(0.0f, FADE_OUT_TICKS);
                wasInRange = false;
                ticksSinceOutOfRange = 0;
            }

            if (bossSound != null) {
                ticksSinceOutOfRange++;

                // after a while, hard stop so it truly ends and OM can resume cleanly
                if (ticksSinceOutOfRange > KEEP_MUTE_TICKS) {
                    bossSound.requestHardStopAfterFadeOut();
                    bossSound.fadeTo(0.0f, FADE_OUT_TICKS);
                }

                // give it a bit more time to actually stop
                if (ticksSinceOutOfRange > KEEP_MUTE_TICKS + 60) {
                    bossSound = null;
                    bossType = null;
                    bossEntity = null;
                    pendingHarbingerCombat = null;
                    pendingHarbingerTicks = 0;
                }
            }

            applyOverhauledMusicBossOverride(isBossMusicActive());
        }
    }

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent event) {
        if (!cataclysmLoaded) return;

        SoundInstance sound = event.getSound();
        if (sound == null) return;

        // If our boss override is active, suppress any other music (Minecraft/OverhauledMusic/Cataclysm/anything).
        if (isBossMusicActive() && sound.getSource() == SoundSource.MUSIC) {
            ResourceLocation id = sound.getLocation();
            if (id != null && !CataclysmOMCompat.MODID.equals(id.getNamespace())) {
                event.setSound(null);
            }
        }

        // Extra safety: Cataclysm sometimes plays its own boss music; suppress it even if we haven't started yet.
        ResourceLocation id = sound.getLocation();
        if (id != null && "cataclysm".equals(id.getNamespace()) && sound.getSource() == SoundSource.MUSIC) {
            // Only cancel obvious music entries to avoid messing with other cataclysm SFX.
            String path = id.getPath();
            if (path.contains("music") || path.contains("boss")) {
                event.setSound(null);
            }
        }
    }

    private void handleHarbingerSwitch(Minecraft mc, BossHit hit) {
    if (hit.type != BossType.HARBINGER) return;

    // Immediate switch. Combat is driven by the "player was hit" timer, so it won't flicker.
    if (hit.combat != harbingerCombat) {
        harbingerCombat = hit.combat;
        startBossTheme(mc, hit);
    }
}



private void updateHarbingerCombatTimer(LocalPlayer player) {
    // Tick down the grace window
    if (harbingerCombatTicksLeft > 0) harbingerCombatTicksLeft--;

    // Detect a new damage event on the client (hurtTime jumps up when you take damage)
    int ht = player.hurtTime;
    if (ht > lastPlayerHurtTime) {
        Entity attacker = null;

        // Best-effort: lastHurtByMob is usually set for melee hits.
// Use reflection so we don't hard-depend on method names across mappings.
try {
    Object o = player.getClass().getMethod("getLastHurtByMob").invoke(player);
    if (o instanceof Entity ent) attacker = ent;
} catch (Throwable ignored) {}
if (attacker != null) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType());
            if (key != null && key.equals(BossType.HARBINGER.entityId)) {
                // 12 seconds of "combat" after being hit
                harbingerCombatTicksLeft = 12 * 20;
            }
        }
    }
    lastPlayerHurtTime = ht;
}

    private void startBossTheme(Minecraft mc, BossHit hit) {
        // Fade out old
        if (bossSound != null) {
            bossSound.requestHardStopAfterFadeOut();
            bossSound.fadeTo(0.0f, FADE_OUT_TICKS);
        }

        this.bossType = hit.type;
        this.bossEntity = hit.entity;
        this.harbingerCombat = hit.combat;
        this.pendingHarbingerCombat = null;
        this.pendingHarbingerTicks = 0;

        SoundEvent evt = hit.type.soundForState(hit.combat).get();
        this.bossSound = new BossThemeSoundInstance(evt);

        // Stop vanilla music immediately (OverhauledMusic is handled separately).
        try {
            mc.getMusicManager().stopPlaying();
        } catch (Throwable ignored) {}

        mc.getSoundManager().play(this.bossSound);

        CataclysmOMCompat.LOGGER.info("[cataclysmomcompat] Boss music started: {} (combat={})", hit.type.name(), hit.combat);
    }

    private void applyOverhauledMusicBossOverride(boolean active) {
        if (active) {
            OverhauledMusicBridge.muteTick();
        } else {
            OverhauledMusicBridge.unmuteNow();
        }
    }

    private boolean isBossMusicActive() {
        if (bossSound == null) return false;
        // Consider it active while it is still audible-ish or while we are in range.
        return wasInRange || bossSound.currentVolume() > 0.01f;
    }

    private static class BossHit {
        final BossType type;
        final Entity entity;
        final boolean combat;

        BossHit(BossType type, Entity entity, boolean combat) {
            this.type = type;
            this.entity = entity;
            this.combat = combat;
        }
    }

        private BossHit findBossInRange(LocalPlayer player) {
        Level level = player.level();
        AABB box = player.getBoundingBox().inflate(RANGE_BLOCKS);
        BossHit best = null;
        double bestDist = Double.MAX_VALUE;

        for (Entity e : level.getEntities(player, box, ent -> ent != null && ent.isAlive())) {
            ResourceLocation key = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
            if (key == null) continue;

            BossType type = null;
            for (BossType t : BossType.values()) {
                if (t.entityId.equals(key)) {
                    type = t;
                    break;
                }
            }
            if (type == null) continue;

            double d2 = e.distanceToSqr(player);
            if (d2 > RANGE_SQ) continue;
            if (d2 < bestDist) {
                boolean combat = isCombat(e, player, type);
                bestDist = d2;
                best = new BossHit(type, e, combat);
            }
        }
        return best;
    }

    private boolean isCombat(Entity e, LocalPlayer player, BossType type) {
    if (type != BossType.HARBINGER) return true;

    // Harbinger combat should NOT trigger on proximity.
    // Only switch to combat after the player is hit by the Harbinger recently.
    return harbingerCombatTicksLeft > 0;
}

}
