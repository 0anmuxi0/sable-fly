package dev.ryanhcode.sable.mixin.entity.entity_collision;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LevelReader.class})
public interface LevelReaderMixin {
   @Inject(
      method = {"containsAnyLiquid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   default void sable$containsAnyLiquidInSubLevels(AABB box, CallbackInfoReturnable<Boolean> cir) {
      if (this instanceof Level level) {
         BoundingBox3d worldBox = new BoundingBox3d(box);
         BoundingBox3d localBox = new BoundingBox3d();

         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, worldBox)) {
            worldBox.transformInverse(subLevel.lastPose(), localBox);
            AABB localAabb = localBox.toMojang();
            if (level.hasChunksAt(BlockPos.containing(localAabb.minX, localAabb.minY, localAabb.minZ), BlockPos.containing(localAabb.maxX, localAabb.maxY, localAabb.maxZ))) {
               BlockPos minPos = BlockPos.containing(localAabb.minX, localAabb.minY, localAabb.minZ);
               BlockPos maxPos = BlockPos.containing(localAabb.maxX, localAabb.maxY, localAabb.maxZ);

               for (BlockPos pos : BlockPos.betweenClosed(minPos, maxPos)) {
                  BlockState state = level.getBlockState(pos);
                  if (!state.getFluidState().isEmpty()) {
                     cir.setReturnValue(true);
                     return;
                  }
               }
            }
         }
      }
   }
}
