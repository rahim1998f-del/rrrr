package dev.rahim.morphmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
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

        // Kill a mob -> unlock its morph.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity instanceof Mob)) return;
            if (source.getEntity() instanceof ServerPlayer player) {
                if (MorphState.unlock(entity.getType())) {
                    player.displayClientMessage(
                            Component.translatable("morphmod.unlocked", entity.getType().getDescription()), true);
                }
            }
        });
    }
}
