package dev.ryanhcode.sable.mixin.plot.lighting;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({BlockAndLightGetter.class})
public interface BlockAndTintGetterMixin {
   @Shadow
   LevelLightEngine getLightEngine();

   @Overwrite
   default int getBrightness(LightLayer var1, BlockPos var2) {
      LevelLightEngine var3 = this.getLightEngine();
      if (this instanceof SubLevelContainerHolder var4) {
         SubLevelContainer var5 = var4.sable$getPlotContainer();
         if (var5.getLevel() instanceof ServerLevel) {
            LevelPlot var7 = var5.getPlot(new ChunkPos(var2.getX() >> 4, var2.getZ() >> 4));
            if (var7 instanceof LevelPlot) {
               var3 = var7.getLightEngine();
            }
         }
      }

      return var3.getLayerListener(var1).getLightValue(var2);
   }

   @Overwrite
   default int getRawBrightness(BlockPos var1, int var2) {
      LevelLightEngine var3 = this.getLightEngine();
      if (this instanceof SubLevelContainerHolder var4) {
         SubLevelContainer var5 = var4.sable$getPlotContainer();
         if (var5.getLevel() instanceof ServerLevel) {
            LevelPlot var7 = var5.getPlot(new ChunkPos(var1.getX() >> 4, var1.getZ() >> 4));
            if (var7 instanceof LevelPlot) {
               var3 = var7.getLightEngine();
            }
         }
      }

      return var3.getRawBrightness(var1, var2);
   }
}
