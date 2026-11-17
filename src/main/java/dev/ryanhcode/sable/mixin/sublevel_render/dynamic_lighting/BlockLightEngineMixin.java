package dev.ryanhcode.sable.mixin.sublevel_render.dynamic_lighting;

import dev.ryanhcode.sable.render.SubLevelDynamicLights;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.BlockLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockLightEngine.class})
public class BlockLightEngineMixin {
   @Inject(require = 0, method = {"getEmission(JLnet/minecraft/world/level/block/state/BlockState;)I"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void sable$includeSubLevelEmission(long packedPos, BlockState state, CallbackInfoReturnable<Integer> cir) {
      int subLevelEmission = SubLevelDynamicLights.getLightEmission(packedPos);
      if (subLevelEmission > cir.getReturnValueI()) {
         cir.setReturnValue(subLevelEmission);
      }
   }
}
