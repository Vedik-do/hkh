package com.pavithra.cataclysmomcompat;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * A looping music-channel sound instance with simple fade-in/out.
 * Designed to play boss tracks from a resource pack (streamed OGG).
 */
final class BossThemeSoundInstance extends AbstractTickableSoundInstance {
    private static final float SILENT_EPSILON = 1.0E-4f;

    private int fadeTicksRemaining = 0;
    private int fadeTicksTotal = 0;
    private float fadeFrom = 0f;
    private float fadeTo = 0f;

    private boolean hardStopWhenSilent = false;
    private boolean stopped = false;

    BossThemeSoundInstance(SoundEvent event) {
        super(event, SoundSource.MUSIC, RandomSource.create());

        this.looping = true;
        this.delay = 0;
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = 0;
        this.y = 0;
        this.z = 0;

        // Start very low to ensure "silence" still counts as playing for some engines.
        this.volume = 0.001f;

        // Fade in.
        fadeTo(1.0f, 40);
    }

    void fadeTo(float target, int ticks) {
        this.fadeFrom = this.volume;
        this.fadeTo = target;
        this.fadeTicksTotal = Math.max(1, ticks);
        this.fadeTicksRemaining = this.fadeTicksTotal;
    }

    void requestHardStopAfterFadeOut() {
        this.hardStopWhenSilent = true;
    }

    float currentVolume() {
        return this.volume;
    }

    @Override
    public boolean isStopped() {
        return stopped;
    }

    @Override
    public void tick() {
        if (fadeTicksRemaining > 0) {
            fadeTicksRemaining--;
            float t = 1.0f - ((float) fadeTicksRemaining / (float) fadeTicksTotal);
            this.volume = fadeFrom + (fadeTo - fadeFrom) * t;
        }

        this.volume = Mth.clamp(this.volume, 0.0f, 1.0f);

        if (this.volume <= 0.0f) {
            if (hardStopWhenSilent) {
                this.volume = 0.0f;
                this.stopped = true;
                this.stop();
            } else {
                // Keep barely audible to avoid some engines dropping it unexpectedly.
                this.volume = SILENT_EPSILON;
            }
        }
    }
}
