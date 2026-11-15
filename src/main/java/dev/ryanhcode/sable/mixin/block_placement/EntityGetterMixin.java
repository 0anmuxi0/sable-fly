package dev.ryanhcode.sable.mixin.block_placement;

import dev.ryanhcode.sable.mixinhelpers.block_placement.BlockPlacementEntityHelper;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EntityGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({EntityGetter.class})
public interface EntityGetterMixin {
   @Shadow
   List<Entity> getEntities(@Nullable Entity var1, AABB var2);

   @Shadow
   List<? extends Player> players();

   @Overwrite
   default boolean isUnobstructed(@Nullable Entity pEntity, VoxelShape voxelShape) {
      if (!voxelShape.isEmpty()) {
         for (Entity entity : this.getEntities(pEntity, voxelShape.bounds())) {
            if (!entity.isRemoved()
               && entity.blocksBuilding
               && (pEntity == null || !entity.isPassengerOfSameVehicle(pEntity))
               && BlockPlacementEntityHelper.obstructs(voxelShape, entity)) {
               return false;
            }
         }
      }

      return true;
   }
}
