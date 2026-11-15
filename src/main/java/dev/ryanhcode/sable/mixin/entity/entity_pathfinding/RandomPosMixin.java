package dev.ryanhcode.sable.mixin.entity.entity_pathfinding;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({RandomPos.class})
public class RandomPosMixin {
   @Overwrite
   public static BlockPos generateRandomPosTowardDirection(PathfinderMob mob, double xzDist, RandomSource random, BlockPos direction) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(mob);
      Vec3 effectiveMobPos = mob.position();
      if (trackingSubLevel != null) {
         effectiveMobPos = trackingSubLevel.logicalPose().transformPositionInverse(effectiveMobPos);
      }

      double xt = direction.getX();
      double zt = direction.getZ();
      if (mob.hasHome() && xzDist > 1.0) {
         BlockPos center = mob.getHomePosition();
         if (effectiveMobPos.x() > center.getX()) {
            xt -= random.nextDouble() * xzDist / 2.0;
         } else {
            xt += random.nextDouble() * xzDist / 2.0;
         }

         if (effectiveMobPos.z() > center.getZ()) {
            zt -= random.nextDouble() * xzDist / 2.0;
         } else {
            zt += random.nextDouble() * xzDist / 2.0;
         }
      }

      return BlockPos.containing(xt + effectiveMobPos.x(), direction.getY() + effectiveMobPos.y(), zt + effectiveMobPos.z());
   }
}
