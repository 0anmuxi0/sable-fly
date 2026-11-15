package dev.ryanhcode.sable.mixin.dynamic_directional_shading;

import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import net.minecraft.client.renderer.block.BlockModelLighter.Cache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin({Cache.class})
public class ModelBlockRendererCacheMixin implements ModelBlockRendererCacheExtension {
   @Unique
   private boolean sable$onSubLevel;

   @Override
   public void sable$setOnSubLevel(boolean var1) {
      this.sable$onSubLevel = var1;
   }

   @Override
   public boolean sable$getOnSubLevel() {
      return this.sable$onSubLevel;
   }
}
