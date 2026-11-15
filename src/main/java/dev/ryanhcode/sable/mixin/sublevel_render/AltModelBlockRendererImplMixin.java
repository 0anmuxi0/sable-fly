package dev.ryanhcode.sable.mixin.sublevel_render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(
   targets = {"net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl"}
)
public class AltModelBlockRendererImplMixin {
   @ModifyVariable(
      method = {"<init>"},
      at = {@At("HEAD")},
      argsOnly = true,
      index = 2
   )
   private static boolean sable$forceFaceCulling(boolean cull) {
      return true;
   }
}
