package dev.rahim.morphmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MorphMod implements ModInitializer {
    public static final String ID = "morphmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);

    @Override
    public void onInitialize() {
        MorphState.load();

        // Kill a mob -> its soul appears; touch the soul to unlock the morph.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof Mob)) return;
            if (source.getEntity() instanceof ServerPlayer && entity.level() instanceof ServerLevel level) {
                MorphSouls.spawn(level, entity);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> MorphSouls.tick());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> MorphSouls.clear());
    }
}
