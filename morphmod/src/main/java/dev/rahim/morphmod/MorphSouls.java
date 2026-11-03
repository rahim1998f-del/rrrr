package dev.rahim.morphmod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "Souls": when a player kills a mob whose morph is not unlocked yet, a soul stays at the
 * place of death (drawn with particles). Walk into it to unlock the morph.
 * Server-thread only.
 */
public final class MorphSouls {
    private static final int LIFETIME_TICKS = 20 * 60 * 5;
    private static final int PICKUP_DELAY_TICKS = 20;
    private static final List<Soul> SOULS = new ArrayList<>();

    private static final class Soul {
        final ServerLevel level;
        final double x, y, z;
        final EntityType<?> type;
        final float w, h;
        int age;

        Soul(ServerLevel level, double x, double y, double z, EntityType<?> type, float w, float h) {
            this.level = level;
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.w = Math.max(0.4F, w);
            this.h = Math.max(0.4F, h);
        }
    }

    private MorphSouls() {}

    public static void spawn(ServerLevel level, LivingEntity dead) {
        EntityType<?> type = dead.getType();
        if (MorphState.isUnlocked(type)) return;
        SOULS.add(new Soul(level, dead.getX(), dead.getY(), dead.getZ(), type, dead.getBbWidth(), dead.getBbHeight()));
    }

    public static void clear() {
        SOULS.clear();
    }

    public static void tick() {
        Iterator<Soul> it = SOULS.iterator();
        while (it.hasNext()) {
            Soul s = it.next();
            s.age++;
            if (s.age > LIFETIME_TICKS || MorphState.isUnlocked(s.type)) {
                it.remove();
                continue;
            }

            if (s.age % 2 == 0) {
                double bob = Math.sin(s.age * 0.1) * 0.15;
                double hw = s.w / 2.0;
                double hh = s.h / 2.0;
                s.level.sendParticles(ParticleTypes.SMOKE, s.x, s.y + hh + 0.2 + bob, s.z,
                        6, hw * 0.6, hh * 0.6, hw * 0.6, 0.0);
                s.level.sendParticles(ParticleTypes.SOUL, s.x, s.y + hh + 0.2 + bob, s.z,
                        1, hw * 0.5, hh * 0.5, hw * 0.5, 0.01);
            }

            if (s.age < PICKUP_DELAY_TICKS) continue;

            AABB box = new AABB(s.x - s.w / 2.0, s.y, s.z - s.w / 2.0,
                    s.x + s.w / 2.0, s.y + s.h, s.z + s.w / 2.0).inflate(0.6);
            for (ServerPlayer p : s.level.players()) {
                if (p.isSpectator()) continue;
                if (p.getBoundingBox().intersects(box)) {
                    if (MorphState.unlock(s.type)) {
                        p.displayClientMessage(
                                Component.translatable("morphmod.unlocked", s.type.getDescription()), true);
                    }
                    it.remove();
                    break;
                }
            }
        }
    }
}
