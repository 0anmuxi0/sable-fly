package dev.ryanhcode.sable.mixin.compatibility.scalablelux;

import ca.spottedleaf.starlight.common.light.BlockStarLightEngine;
import ca.spottedleaf.starlight.common.light.SkyStarLightEngine;
import ca.spottedleaf.starlight.common.light.StarLightInterface;
import ca.spottedleaf.starlight.common.light.StarLightInterface.LightQueue.ChunkTasks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.render.SubLevelDynamicLights;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {StarLightInterface.class},
   remap = false
)
public abstract class StarLightInterfaceMixin {
   @Shadow
   @Final
   protected Level world;
   @Shadow
   @Final
   public LevelLightEngine lightEngine;

   @Inject(require = 0, method = {"getAnyChunkNow"},
      at = {@At("HEAD")},
      cancellable = true,
      remap = false
   )
   private void sable$getPlotChunk(int chunkX, int chunkZ, CallbackInfoReturnable<ChunkAccess> cir) {
      if (this.world != null) {
         SubLevelContainer container = SubLevelContainer.getContainer(this.world);
         if (container != null && container.inBounds(chunkX, chunkZ)) {
            cir.setReturnValue(container.getChunk(new ChunkPos(chunkX, chunkZ)));
         }
      }
   }

   @WrapMethod(require = 0, method = {"handleUpdateInternal"}
   )
   private void sable$withSubLevelLights(ChunkTasks task, SkyStarLightEngine skyEngine, BlockStarLightEngine blockEngine, Operation<Void> original) {
      SubLevelDynamicLights.beginLightUpdates(this.lightEngine);

      try {
         original.call(task, skyEngine, blockEngine);
      } finally {
         SubLevelDynamicLights.endLightUpdates(this.lightEngine);
      }
   }
}
