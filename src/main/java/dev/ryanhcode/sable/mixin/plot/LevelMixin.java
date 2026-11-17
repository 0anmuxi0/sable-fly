package dev.ryanhcode.sable.mixin.plot;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Level.class})
public class LevelMixin {
   @Inject(require = 0, method = {"setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z"},
      at = {@At("HEAD")}
   )
   private void sable$ensurePlotChunk(BlockPos blockPos, BlockState blockState, int flags, int recursionLeft, CallbackInfoReturnable<Boolean> cir) {
      Level level = (Level)(Object)this;
      if (!level.isClientSide()) {
         SubLevelContainer container = SubLevelContainer.getContainer(level);
         if (container != null) {
            ChunkPos chunkPos = new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4);
            if (container.inBounds(chunkPos)) {
               LevelPlot plot = container.getPlot(chunkPos);
               if (plot != null) {
                  ChunkPos local = plot.toLocal(chunkPos);
                  if (plot.getChunkHolder(local) == null) {
                     plot.newEmptyChunk(chunkPos);
                  }
               }
            }
         }
      }
   }
}
