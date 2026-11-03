package dev.rahim.morphmod.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.rahim.morphmod.MorphMod;
import dev.rahim.morphmod.MorphState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

public class MorphModClient implements ClientModInitializer {
    private static KeyMapping menuKey;
    private static LivingEntity dummy;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MorphMod.ID, "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.morphmod.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) {
                if (mc.player != null && mc.screen == null) {
                    mc.setScreen(new MorphScreen());
                }
            }
            tickDummy(mc);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
            MorphState.clearActive();
            dummy = null;
        });
    }

    /** Switch morph (null = back to human). Singleplayer only. */
    public static void morph(EntityType<?> type) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        MinecraftServer server = mc.getSingleplayerServer();
        if (server == null) {
            player.displayClientMessage(Component.translatable("morphmod.singleplayer_only"), true);
            return;
        }

        UUID id = player.getUUID();
        MorphState.setActive(id, type);
        dummy = null;
        player.refreshDimensions();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(id);
            if (sp != null) sp.refreshDimensions();
        });

        player.displayClientMessage(type == null
                ? Component.translatable("morphmod.demorphed")
                : Component.translatable("morphmod.morphed", type.getDescription()), true);
    }

    /** Called from the render mixin. */
    public static Entity getRenderReplacement(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (dummy != null && entity == mc.player) return dummy;
        return null;
    }

    private static void tickDummy(Minecraft mc) {
        LocalPlayer p = mc.player;
        EntityType<?> type = p == null ? null : MorphState.getActive(p.getUUID());
        if (type == null) {
            dummy = null;
            return;
        }

        try {
            if (dummy == null || dummy.getType() != type || dummy.level() != p.level()) {
                Entity e = type.create(p.level(), EntitySpawnReason.COMMAND);
                if (!(e instanceof LivingEntity living)) {
                    MorphState.clearActive();
                    dummy = null;
                    return;
                }
                living.setNoGravity(true);
                living.noPhysics = true;
                living.setPos(p.getX(), p.getY(), p.getZ());
                dummy = living;
            }
            syncDummy(p, dummy);
        } catch (Throwable t) {
            MorphMod.LOGGER.error("Morph render entity failed, resetting morph", t);
            MorphState.clearActive();
            dummy = null;
        }
    }

    private static void syncDummy(LocalPlayer p, LivingEntity d) {
        // old position = last tick's position, so render interpolation works
        d.xo = d.getX();
        d.yo = d.getY();
        d.zo = d.getZ();
        d.xOld = d.getX();
        d.yOld = d.getY();
        d.zOld = d.getZ();
        d.setPos(p.getX(), p.getY(), p.getZ());
        d.setDeltaMovement(p.getDeltaMovement());

        d.yRotO = p.yRotO;
        d.setYRot(p.getYRot());
        d.xRotO = p.xRotO;
        d.setXRot(p.getXRot());
        d.yBodyRotO = p.yBodyRotO;
        d.setYBodyRot(p.yBodyRot);
        d.yHeadRotO = p.yHeadRotO;
        d.setYHeadRot(p.yHeadRot);

        d.tickCount = p.tickCount;
        d.walkAnimation.update(p.walkAnimation.speed(), 1.0F, 1.0F);
        d.setShiftKeyDown(p.isShiftKeyDown());
        d.setInvisible(p.isInvisible());
    }
}
