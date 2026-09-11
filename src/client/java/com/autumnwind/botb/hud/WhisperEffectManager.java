package com.autumnwind.botb.hud;

import com.autumnwind.botb.BloodOnTheBlocktower;
import com.autumnwind.botb.config.WhisperSettings;
import com.autumnwind.botb.networking.WhisperEffectS2CPayload;
import com.autumnwind.botb.sound.WhisperSoundInstance;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Client-side renderer for whisper visual + audio effects.
 *
 * <p>Visual is a single mode: when on, particles are emitted from the sender's head
 * over a short window, each launched with a random initial velocity within a tight
 * cone pointed at the receiver. Each particle then smoothly accelerates toward the
 * (live) receiver position with light drag, despawning on arrival or after the 10 s
 * timeout. The visual is rendered by spawning a vanilla {@link ParticleTypes#ENCHANT}
 * at each particle's simulated position once per game tick, which is cheap, magical-looking,
 * and gets vanilla's existing particle pipeline for free.
 */
public final class WhisperEffectManager {
    private static final ResourceLocation WHISPER_SOUND_ID = ResourceLocation.fromNamespaceAndPath("blood-on-the-blocktower", "whisper");

    /** Effect-level lifetime cap (10 s). Safety net so effects can't accumulate forever. */
    private static final int MAX_EFFECT_TICKS = 200;

    /** How long the sender keeps emitting new particles after the effect starts. */
    private static final int EMIT_DURATION_TICKS = 30;
    /** Total particles emitted per effect across the emission window. */
    private static final int RUNE_BURST_SIZE = 4;

    /** Half-angle of the emission cone, in radians. Wide enough that individual runes
     *  visibly start in different directions before homing in on the receiver. */
    private static final double CONE_HALF_ANGLE = Math.toRadians(35);

    /** Default whisper baseline. */
    private static final float WHISPER_BASE_VOLUME = 1.0f;

    private static final List<Effect> ACTIVE = new ArrayList<>();
    private static final Random RNG = new Random();

    private WhisperEffectManager() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(WhisperEffectManager::render);
    }

    /**
     * Kick off a new effect. Audio starts immediately if the server flagged this
     * client as audio-eligible, and visuals queue up an Effect for the world render pass.
     */
    public static void onEffect(WhisperEffectS2CPayload payload) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        AbstractClientPlayer sender = lookupPlayer(client, payload.senderUuid());
        AbstractClientPlayer target = lookupPlayer(client, payload.targetUuid());
        WhisperSettings.VisualMode visualMode = payload.visual();

        BloodOnTheBlocktower.LOGGER.debug("Whisper effect received: visual={} audio={} sender={} target={} senderLoaded={} targetLoaded={}",
                visualMode, payload.audio(), payload.senderUuid(), payload.targetUuid(),
                sender != null, target != null);

        // Audio is positional, anchored to the sender. If the sender isn't loaded
        // locally we can't pick a position, so the cue is dropped, since vanilla's
        // attenuation would make it inaudible anyway.
        if (payload.audio() && sender != null) {
            playWhisperSound(client, sender, payload.pitch());
        }

        if (visualMode == WhisperSettings.VisualMode.RUNES && sender != null && target != null) {
            ACTIVE.add(new Effect(
                    payload.senderUuid(),
                    payload.targetUuid(),
                    client.level.getGameTime()
            ));
        }
    }

    private static AbstractClientPlayer lookupPlayer(Minecraft client, UUID uuid) {
        if (client.level == null) return null;
        for (AbstractClientPlayer p : client.level.players()) {
            if (p.getUUID().equals(uuid)) return p;
        }
        return null;
    }

    private static void playWhisperSound(Minecraft client,
                                          AbstractClientPlayer sender,
                                          float pitch) {
        SoundEvent event = SoundEvent.createVariableRangeEvent(WHISPER_SOUND_ID);
        WhisperSoundInstance inst = new WhisperSoundInstance(
                event, WHISPER_BASE_VOLUME, pitch, sender);
        client.getSoundManager().play(inst);
    }

    private static void render(WorldRenderContext ctx) {
        if (ACTIVE.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return;

        float tickDelta = ctx.tickCounter().getGameTimeDeltaPartialTick(true);
        long worldTick = client.level.getGameTime();

        Iterator<Effect> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            long ageTicks = worldTick - e.startTick;

            AbstractClientPlayer sender = lookupPlayer(client, e.senderUuid);
            AbstractClientPlayer target = lookupPlayer(client, e.targetUuid);
            if (sender == null || target == null) {
                it.remove();
                continue;
            }

            Vec3 senderHead = headPos(sender, tickDelta);
            Vec3 targetHead = headPos(target, tickDelta);

            // One physics step per game tick, gated so multiple render frames within
            // the same tick don't double-step. Emission progress and ENCHANT spawns
            // ride the sim step.
            if (e.lastSimTick < worldTick) {
                e.lastSimTick = worldTick;
                stepSimulation(client, e, senderHead, targetHead, ageTicks);
            }

            // Despawn the effect when emission is finished AND no live particles remain,
            // OR when we hit the absolute lifetime cap.
            boolean emissionDone = ageTicks >= EMIT_DURATION_TICKS && e.emittedCount >= RUNE_BURST_SIZE;
            if ((emissionDone && e.particles.isEmpty()) || ageTicks >= MAX_EFFECT_TICKS) {
                it.remove();
            }
        }
    }

    private static Vec3 headPos(AbstractClientPlayer p, float tickDelta) {
        Vec3 lerped = p.getPosition(tickDelta);
        return new Vec3(lerped.x, lerped.y + p.getEyeHeight(), lerped.z);
    }

    /**
     * One game-tick advancement: emit any new particles for this tick, step physics
     * for all live particles, and spawn a vanilla ENCHANT at each particle's current
     * position to render the rune visual.
     */
    private static void stepSimulation(Minecraft client, Effect e,
                                         Vec3 senderHead, Vec3 targetHead,
                                         long ageTicks) {
        if (client.level == null) return;

        // Emission is linear across the emission window. Slight rounding-up so all
        // particles get emitted by the end of the window even if the count doesn't
        // divide evenly.
        if (ageTicks < EMIT_DURATION_TICKS && e.emittedCount < RUNE_BURST_SIZE) {
            int targetByNow = (int) Math.ceil(((double) RUNE_BURST_SIZE * (ageTicks + 1)) / EMIT_DURATION_TICKS);
            while (e.emittedCount < Math.min(RUNE_BURST_SIZE, targetByNow)) {
                e.particles.add(emitParticle(senderHead, targetHead));
                e.emittedCount++;
            }
        }

        Iterator<WhisperParticle> pit = e.particles.iterator();
        while (pit.hasNext()) {
            WhisperParticle p = pit.next();
            if (!p.tick(targetHead)) {
                pit.remove();
                continue;
            }
            // Tiny random destination jitter so the ENCHANT glyph has somewhere to
            // fly to (zero delta makes it immediately mark-dead). The jitter is small
            // enough that it doesn't visibly shift the glyph location. It just keeps
            // vanilla's target-seeking from auto-killing the particle.
            double dx = (RNG.nextDouble() - 0.5) * 0.2;
            double dy = (RNG.nextDouble() - 0.5) * 0.2;
            double dz = (RNG.nextDouble() - 0.5) * 0.2;
            client.level.addParticle(
                    ParticleTypes.ENCHANT,
                    p.x, p.y, p.z,
                    dx, dy, dz
            );
        }
    }

    /**
     * Build one rune particle: spawned at the sender's head with an initial velocity
     * sampled uniformly within a {@link #CONE_HALF_ANGLE} cone around the
     * sender→receiver line. Speed is randomized so particles don't move in lockstep.
     */
    private static WhisperParticle emitParticle(Vec3 senderHead, Vec3 targetHead) {
        Vec3 toTarget = targetHead.subtract(senderHead);
        double dist = toTarget.length();
        Vec3 toTargetN = dist > 1e-6 ? toTarget.scale(1.0 / dist) : new Vec3(0, 1, 0);

        Vec3 dir = randomConeDirection(toTargetN, CONE_HALF_ANGLE);
        double speed = 0.05 + RNG.nextDouble() * 0.10; // 0.05–0.15 blocks/tick
        Vec3 vel = dir.scale(speed);

        // Per-particle target jitter so they don't all converge to one exact point.
        double jitter = 0.4;
        Vec3 targetOffset = new Vec3(
                (RNG.nextDouble() - 0.5) * 2 * jitter,
                (RNG.nextDouble() - 0.5) * 2 * jitter,
                (RNG.nextDouble() - 0.5) * 2 * jitter
        );

        return new WhisperParticle(senderHead.x, senderHead.y, senderHead.z, vel, targetOffset);
    }

    /**
     * Sample a uniformly random direction within a cone of half-angle {@code halfAngle}
     * around {@code axis}. Standard spherical-coordinates trick: pick (azimuth ∈ [0, 2π],
     * zenith ∈ [0, halfAngle]) in the cone's local frame, then convert back to world.
     */
    private static Vec3 randomConeDirection(Vec3 axis, double halfAngle) {
        Vec3 up = Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = axis.cross(up).normalize();
        Vec3 localUp = right.cross(axis).normalize();

        double azimuth = RNG.nextDouble() * 2 * Math.PI;
        double zenith = RNG.nextDouble() * halfAngle;
        double cz = Math.cos(zenith);
        double sz = Math.sin(zenith);

        return axis.scale(cz)
                .add(right.scale(sz * Math.cos(azimuth)))
                .add(localUp.scale(sz * Math.sin(azimuth)));
    }

    /** Per-effect state: sender/receiver, particles, and emission progress. */
    private static final class Effect {
        final UUID senderUuid;
        final UUID targetUuid;
        final long startTick;
        final List<WhisperParticle> particles = new ArrayList<>();
        long lastSimTick = -1;
        int emittedCount = 0;

        Effect(UUID senderUuid, UUID targetUuid, long startTick) {
            this.senderUuid = senderUuid;
            this.targetUuid = targetUuid;
            this.startTick = startTick;
        }
    }

    /**
     * Single rune simulated client-side: position, velocity, per-particle target
     * jitter, age. Each tick the velocity smoothly accelerates toward the (live)
     * target plus its jitter, with light drag so the particle settles into a homing
     * curve rather than overshooting wildly.
     */
    private static final class WhisperParticle {
        /** 10 s. Even slow-moving particles never live longer than this. */
        private static final int MAX_AGE = 200;
        /** Despawn radius², used when within this distance² of the (jittered) target. */
        private static final double ARRIVE_DIST_SQ = 0.6 * 0.6;
        /** Per-tick velocity gain toward target (blocks/tick²). Lower = slower homing. */
        private static final double ACCEL = 0.025;
        /** Per-tick velocity multiplier (1.0 = no drag). Higher drag (lower retention)
         *  combined with low ACCEL caps terminal velocity at ~0.5 blocks/tick, about
         *  half the previous tuning, for a more deliberate magical drift. */
        private static final double DRAG = 0.95;

        double x, y, z;
        double vx, vy, vz;
        final Vec3 targetOffset;
        int age = 0;

        WhisperParticle(double x, double y, double z, Vec3 initialVelocity, Vec3 targetOffset) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = initialVelocity.x;
            this.vy = initialVelocity.y;
            this.vz = initialVelocity.z;
            this.targetOffset = targetOffset;
        }

        /** Advance one tick. Returns true while the particle is alive. */
        boolean tick(Vec3 currentTargetHead) {
            double tx = currentTargetHead.x + targetOffset.x;
            double ty = currentTargetHead.y + targetOffset.y;
            double tz = currentTargetHead.z + targetOffset.z;

            double dx = tx - x;
            double dy = ty - y;
            double dz = tz - z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < ARRIVE_DIST_SQ) return false;

            // Smooth attraction: velocity gains a small step toward the target each
            // tick. Combined with drag, this produces an asymptotic homing curve.
            double dist = Math.sqrt(distSq);
            vx += (dx / dist) * ACCEL;
            vy += (dy / dist) * ACCEL;
            vz += (dz / dist) * ACCEL;

            vx *= DRAG;
            vy *= DRAG;
            vz *= DRAG;

            x += vx;
            y += vy;
            z += vz;

            return ++age < MAX_AGE;
        }
    }
}
