package dev.rahim.morphmod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Acquisition effect (inspired by the classic Morph mod): when a player kills a mob, dark
 * "ink" flows from the dead body into the player; when it arrives the morph is unlocked.
 * Server-thread only.
 */
public final class MorphAcquisition {
    private static final int DURATION_TICKS = 30;
    private static final int PARTICLES_PER_TICK = 6;
    private static final List<Acq> ACTIVE = new ArrayList<>();

    private static final class Acq {
        final ServerLevel level;
        final UUID player;
        final EntityType<?> type;
        final double x, y, z;
        final float w, h;
        int age;

        Acq(ServerLevel level, UUID player, EntityType<?> type, double x, double y, double z, float w, float h) {
            this.level = level;
            this.player = player;
            this.type = type;
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = Math.max(0.4F, w);
            this.h = Math.max(0.4F, h);
        }
    }

    private MorphAcquisition() {}

    public static void start(ServerLevel level, ServerPlayer killer, LivingEntity dead) {
        EntityType<?> type = dead.getType();
        if (MorphState.isUnlocked(type)) return;
        ACTIVE.add(new Acq(level, killer.getUUID(), type, dead.getX(), dead.getY(), dead.getZ(),
                dead.getBbWidth(), dead.getBbHeight()));
    }

    public static void clear() {
        ACTIVE.clear();
    }

    public static void tick() {
        Iterator<Acq> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Acq a = it.next();
            Player p = a.level.getPlayerByUUID(a.player);
            if (p == null || MorphState.isUnlocked(a.type)) {
                it.remove();
                continue;
            }
            a.age++;

            double tx = p.getX();
            double ty = p.getY() + p.getBbHeight() * 0.6;
            double tz = p.getZ();
            for (int i = 0; i < PARTICLES_PER_TICK; i++) {
                double px = a.x + (a.level.getRandom().nextDouble() - 0.5) * a.w;
                double py = a.y + a.level.getRandom().nextDouble() * a.h;
                double pz = a.z + (a.level.getRandom().nextDouble() - 0.5) * a.w;
                double dx = tx - px, dy = ty - py, dz = tz - pz;
                double len = Math.max(0.001, Math.sqrt(dx * dx + dy * dy + dz * dz));
                double speed = Math.max(0.1, Math.min(0.5, len * 0.08));
                a.level.sendParticles(ParticleTypes.SQUID_INK, px, py, pz, 0,
                        dx / len, dy / len, dz / len, speed);
            }

            if (a.age >= DURATION_TICKS) {
                if (MorphState.unlock(a.type) && p instanceof ServerPlayer sp) {
                    sp.displayClientMessage(Component.translatable("morphmod.unlocked", a.type.getDescription()), true);
                }
                it.remove();
            }
        }
    }
}
