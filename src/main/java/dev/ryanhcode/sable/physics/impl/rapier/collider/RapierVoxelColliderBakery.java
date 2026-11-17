package dev.ryanhcode.sable.physics.impl.rapier.collider;

import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.ryanhcode.sable.api.block.BlockWithSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class RapierVoxelColliderBakery {
   @NotNull
   private final PhysicsColliderBlockGetter level;
   private final Function<BlockState, RapierVoxelColliderData> blockPhysicsDataBuilder = Util.memoize(this::buildPhysicsDataForBlock);

   public RapierVoxelColliderBakery(@NotNull BlockGetter blockGetter) {
      this.level = new PhysicsColliderBlockGetter(blockGetter);
   }

   @NotNull
   public BlockGetter getLevel() {
      return this.level;
   }

   @NotNull
   private RapierVoxelColliderData buildPhysicsDataForBlock(BlockState childState) {
      boolean liquid = VoxelNeighborhoodState.isLiquid(childState);
      double friction = PhysicsBlockPropertyHelper.getFriction(childState);
      double volume = PhysicsBlockPropertyHelper.getVolume(childState);
      double restitution = PhysicsBlockPropertyHelper.getRestitution(childState);
      BlockSubLevelCollisionCallback callback = BlockWithSubLevelCollisionCallback.sable$getCallback(childState);
      RapierVoxelColliderData entry = Rapier3D.createVoxelColliderEntry(friction, volume, restitution, liquid, callback);
      if (liquid) {
         entry.addBox(JOMLConversion.ZERO, new Vector3d(1.0, 1.0, 1.0));
         return entry;
      } else {
         this.level.setup(childState);
         VoxelShape shape;
         if (childState.getBlock() instanceof BlockSubLevelCollisionShape extension) {
            shape = extension.getSubLevelCollisionShape(this.level, childState);
         } else {
            shape = childState.getCollisionShape(this.level, BlockPos.ZERO, SableCollisionContext.get());
         }

         this.level.setup(Blocks.AIR.defaultBlockState());
         if (shape.isEmpty()) {
            return RapierVoxelColliderData.EMPTY;
         } else {
            shape.forAllBoxes(
               (minX, minY, minZ, maxX, maxY, maxZ) -> entry.addBox(
                  new Vector3d(Math.max(minX, 0.0), Math.max(minY, 0.0), Math.max(minZ, 0.0)),
                  new Vector3d(Math.min(maxX, 1.0), Math.min(maxY, 1.0), Math.min(maxZ, 1.0))
               )
            );
            return entry;
         }
      }
   }

   @org.jetbrains.annotations.Nullable
   public RapierVoxelColliderData getPhysicsDataForBlock(BlockState state) {
      RapierVoxelColliderData data = this.blockPhysicsDataBuilder.apply(Objects.requireNonNull(state, "state"));
      return data == RapierVoxelColliderData.EMPTY ? null : data;
   }
}
