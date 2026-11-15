package dev.ryanhcode.sable.mixin.compatibility.scalablelux;

import ca.spottedleaf.starlight.common.light.BlockStarLightEngine;
import dev.ryanhcode.sable.render.SubLevelDynamicLights;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LightChunkGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(
   value = {BlockStarLightEngine.class},
   remap = false
)
public abstract class BlockStarLightEngineMixin {
   @ModifyVariable(
      method = {"checkBlock"},
      at = {@At("STORE")},
      index = 9,
      remap = false
   )
   private int sable$includeSubLevelEmissionOnCheck(int original, LightChunkGetter lightAccess, int worldX, int worldY, int worldZ) {
      return Math.max(original, SubLevelDynamicLights.getLightEmission(BlockPos.asLong(worldX, worldY, worldZ)));
   }

   @ModifyVariable(
      method = {"calculateLightValue"},
      at = {@At("STORE")},
      index = 7,
      remap = false
   )
   private int sable$includeSubLevelEmissionOnRecalculate(int original, LightChunkGetter lightAccess, int worldX, int worldY, int worldZ, int expect) {
      return Math.max(original, SubLevelDynamicLights.getLightEmission(BlockPos.asLong(worldX, worldY, worldZ)));
   }
}
