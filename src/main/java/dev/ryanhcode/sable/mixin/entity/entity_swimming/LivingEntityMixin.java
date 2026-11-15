package dev.ryanhcode.sable.mixin.entity.entity_swimming;

import dev.ryanhcode.sable.mixinhelpers.entity.entity_swimming.SubLevelFluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LivingEntity.class})
public class LivingEntityMixin {
   @Redirect(
      method = {"travel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;"
      )}
   )
   private FluidState sable$subLevelFluidAtBlockPos(Level level, BlockPos pos) {
      return SubLevelFluidHelper.getFluidStateAt((LivingEntity)(Object)this, pos);
   }
}
