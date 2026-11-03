package dev.rahim.morphmod.mixin;

import dev.rahim.morphmod.client.MorphModClient;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the first-person player arm (your skin) while morphed. method_3219 = renderPlayerArm. */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "method_3219", at = @At("HEAD"), cancellable = true, require = 0)
    private void morphmod$hideArm(CallbackInfo ci) {
        if (MorphModClient.isMorphed()) {
            ci.cancel();
        }
    }
}
