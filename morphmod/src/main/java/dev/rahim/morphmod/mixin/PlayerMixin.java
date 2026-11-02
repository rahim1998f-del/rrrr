package dev.rahim.morphmod.mixin;

import dev.rahim.morphmod.MorphState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces hitbox size and eye height with those of the morph (both client and integrated server). */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "getDefaultDimensions", at = @At("HEAD"), cancellable = true)
    private void morphmod$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player self = (Player) (Object) this;
        EntityType<?> type = MorphState.getActive(self.getUUID());
        if (type != null) {
            cir.setReturnValue(type.getDimensions().scale(self.getScale()));
        }
    }
}
