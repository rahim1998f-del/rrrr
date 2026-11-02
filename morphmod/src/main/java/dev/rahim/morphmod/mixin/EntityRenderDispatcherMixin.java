package dev.rahim.morphmod.mixin;

import dev.rahim.morphmod.client.MorphModClient;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** When the local player is rendered, render the dummy mob entity instead. */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @ModifyVariable(method = "method_72977", at = @At("HEAD"), argsOnly = true)
    private Entity morphmod$swapEntity(Entity entity) {
        Entity replacement = MorphModClient.getRenderReplacement(entity);
        return replacement != null ? replacement : entity;
    }
}
