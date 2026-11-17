package dev.ryanhcode.sable.mixin.compatibility.scalablelux;

import ca.spottedleaf.starlight.common.light.BlockStarLightEngine;
import ca.spottedleaf.starlight.common.light.StarLightEngine;
import dev.ryanhcode.sable.render.SubLevelDynamicLights;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Desc;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {StarLightEngine.class},
   remap = false
)
public abstract class StarLightEngineMixin {
   @Shadow
   protected int chunkOffsetX;
   @Shadow
   protected int chunkOffsetY;
   @Shadow
   protected int chunkOffsetZ;

   @Inject(require = 0, target = {@Desc(
         value = "getBlockState",
         ret = BlockState.class,
         args = {int.class, int.class, int.class}
      )},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   private void sable$includeSubLevelOcclusion(int worldX, int worldY, int worldZ, CallbackInfoReturnable<BlockState> cir) {
      if (((Object)this) instanceof BlockStarLightEngine) {
         BlockState projected = SubLevelDynamicLights.getOccludingBlockState(BlockPos.asLong(worldX, worldY, worldZ));
         if (projected != null) {
            cir.setReturnValue(projected);
         }
      }
   }

   @Inject(require = 0, target = {@Desc(
         value = "getBlockState",
         ret = BlockState.class,
         args = {int.class, int.class}
      )},
      at = {@At("RETURN")},
      cancellable = true,
      remap = false
   )
   private void sable$includeCachedSubLevelOcclusion(int sectionIndex, int localIndex, CallbackInfoReturnable<BlockState> cir) {
      if (((Object)this) instanceof BlockStarLightEngine) {
         int sectionX = sectionIndex % 5 - this.chunkOffsetX;
         int sectionZ = sectionIndex / 5 % 5 - this.chunkOffsetZ;
         int sectionY = sectionIndex / 25 - this.chunkOffsetY;
         int worldX = sectionX << 4 | localIndex & 15;
         int worldY = sectionY << 4 | localIndex >>> 8 & 15;
         int worldZ = sectionZ << 4 | localIndex >>> 4 & 15;
         BlockState projected = SubLevelDynamicLights.getOccludingBlockState(BlockPos.asLong(worldX, worldY, worldZ));
         if (projected != null) {
            cir.setReturnValue(projected);
         }
      }
   }
}
