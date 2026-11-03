package dev.rahim.morphmod.mixin;

import dev.rahim.morphmod.client.MorphModClient;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Show the morph instead of the player skin in the inventory preview. Non-fatal if the target is not found. */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @ModifyVariable(method = {"method_2486", "renderEntityInInventoryFollowsMouse"},
            at = @At("HEAD"), argsOnly = true, require = 0)
    private static LivingEntity morphmod$swapPreview(LivingEntity entity) {
        Entity replacement = MorphModClient.getRenderReplacement(entity);
        return replacement instanceof LivingEntity living ? living : entity;
    }
}
