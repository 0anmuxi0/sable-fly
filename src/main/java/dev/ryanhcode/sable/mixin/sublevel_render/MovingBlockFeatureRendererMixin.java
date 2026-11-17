package dev.ryanhcode.sable.mixin.sublevel_render;

import net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin({MovingBlockFeatureRenderer.class})
public class MovingBlockFeatureRendererMixin {
   @ModifyArg(require = 0, method = {"buildGroup"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;<init>(ZZLnet/minecraft/client/color/block/BlockColors;)V"
      )},
      index = 1
   )
   private boolean sable$forceFaceCulling(boolean cull) {
      return true;
   }
}
