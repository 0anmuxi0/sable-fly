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
   default void sable$containsAnyLiquidInSubLevels(AABB var1, CallbackInfoReturnable<Boolean> var2) {
      if (this instanceof Level var3) {
         BoundingBox3d var4 = new BoundingBox3d(var1);
         BoundingBox3d var5 = new BoundingBox3d();

         for (SubLevel var7 : Sable.HELPER.getAllIntersecting(var3, var4)) {
            var4.transformInverse(var7.lastPose(), var5);
            AABB var8 = var5.toMojang();
            if (var3.hasChunksAt(BlockPos.containing(var8.minX, var8.minY, var8.minZ), BlockPos.containing(var8.maxX, var8.maxY, var8.maxZ))) {
               BlockPos var9 = BlockPos.containing(var8.minX, var8.minY, var8.minZ);
               BlockPos var10 = BlockPos.containing(var8.maxX, var8.maxY, var8.maxZ);

               for (BlockPos var12 : BlockPos.betweenClosed(var9, var10)) {
                  BlockState var13 = var3.getBlockState(var12);
                  if (!var13.getFluidState().isEmpty()) {
                     var2.setReturnValue(true);
                     return;
                  }
               }
            }
         }
      }
   }
}
