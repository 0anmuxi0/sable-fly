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

/**
 * 26.2 port.
 *
 * 26.3 relied on {@code LevelReader.findBlocksIn(AABB) : BlockMatcher} (the {@code blockscan}
 * package, new in 26.3) to test sub-level volumes for liquid. That API does not exist in 26.2,
 * so we scan the sub-level's local AABB with plain {@code BlockPos.betweenClosed} instead - 
 * functionally equivalent for a liquid-presence query, just without the streaming short-circuit.
 * A {@code hasChunksAt} guard keeps the cost bounded to loaded regions.
 */
@Mixin({LevelReader.class})
public interface LevelReaderMixin {
   @Inject(
      method = {"containsAnyLiquid"},
      at = {@At("HEAD")},
      cancellable = true
   )
   default void sable$containsAnyLiquidInSubLevels(AABB box, CallbackInfoReturnable<Boolean> cir) {
      if (!((Object) this instanceof Level level)) {
         return;
      }

      BoundingBox3d globalBounds = new BoundingBox3d(box);
      BoundingBox3d localBounds = new BoundingBox3d();

      for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, globalBounds)) {
         globalBounds.transformInverse(subLevel.lastPose(), localBounds);
         AABB localAabb = localBounds.toMojang();
         if (!level.hasChunksAt(BlockPos.containing(localAabb.minX, localAabb.minY, localAabb.minZ),
                                BlockPos.containing(localAabb.maxX, localAabb.maxY, localAabb.maxZ))) {
            continue;
         }

         BlockPos min = BlockPos.containing(localAabb.minX, localAabb.minY, localAabb.minZ);
         BlockPos max = BlockPos.containing(localAabb.maxX, localAabb.maxY, localAabb.maxZ);
         for (BlockPos pos : BlockPos.betweenClosed(min, max)) {
            BlockState state = level.getBlockState(pos);
            if (!state.getFluidState().isEmpty()) {
               cir.setReturnValue(true);
               return;
            }
         }
      }
   }
}
