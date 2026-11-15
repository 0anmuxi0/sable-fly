package dev.ryanhcode.sable.physics.callback;

import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

public class BellBlockCallback extends FragileBlockCallback {
   public static final BellBlockCallback INSTANCE = new BellBlockCallback();

   @Override
   public boolean shouldTriggerFor(BlockState state) {
      return state.getBlock() instanceof BellBlock;
   }

   @Override
   public BlockSubLevelCollisionCallback.CollisionResult onHit(ServerLevel level, BlockPos pos, BlockState state, Vector3d hitPos) {
      Vec3 hitDir = Vec3.atCenterOf(pos).subtract(hitPos.x, hitPos.y, hitPos.z);
      Direction facing = state.getValue(BellBlock.FACING);
      BellAttachType attachment = state.getValue(BellBlock.ATTACHMENT);
      int xMul = Math.abs(facing.getStepX());
      int zMul = Math.abs(facing.getStepZ());
      if (attachment == BellAttachType.CEILING) {
         xMul = 1;
         zMul = 1;
      }

      Direction direction = Direction.getApproximateNearest(hitDir.x * xMul, 0.0, hitDir.z * zMul);
      ((BellBlock)state.getBlock()).attemptToRing(level, pos, direction.getOpposite());
      return new BlockSubLevelCollisionCallback.CollisionResult(JOMLConversion.ZERO, false);
   }
}
