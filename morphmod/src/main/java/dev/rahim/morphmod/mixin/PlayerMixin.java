package dev.rahim.morphmod.mixin;

import dev.rahim.morphmod.MorphState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In 1.21.11 getDefaultDimensions lives in Avatar (method_55694 = intermediary name). */
@Mixin(Avatar.class)
public abstract class PlayerMixin {
    @Inject(method = "method_55694", at = @At("HEAD"), cancellable = true)
    private void morphmod$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Object self = this;
        if (!(self instanceof Player player)) return;
        EntityType<?> type = MorphState.getActive(player.getUUID());
        if (type != null) {
            cir.setReturnValue(type.getDimensions().scale(player.getScale()));
        }
    }
}
