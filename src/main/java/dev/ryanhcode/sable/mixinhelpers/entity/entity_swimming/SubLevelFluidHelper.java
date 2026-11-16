package dev.ryanhcode.sable.mixinhelpers.entity.entity_swimming;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class SubLevelFluidHelper {
   private static final Set<TagKey<Fluid>> FLUIDS_WITH_CURRENT = Set.of(FluidTags.WATER, FluidTags.LAVA);

   private SubLevelFluidHelper() {
   }

   public static void gather(Entity entity, boolean ignoreCurrent, Map<Holder<Fluid>, SubLevelFluidHelper.Found> fluidsByType, Map<TagKey<Fluid>, SubLevelFluidHelper.Found> currentsByTag) {
      fluidsByType.clear();
      currentsByTag.clear();
      AABB interactionBox = entity.getFluidInteractionBox();
      if (interactionBox != null) {
         Level level = entity.level();
         LevelReusedVectors reusedVectors = ((LevelExtension)level).sable$getJOMLSink();
         BoundingBox3d box = new BoundingBox3d(interactionBox);
         BoundingBox3d localBox = new BoundingBox3d();
         MutableBlockPos blockPos = new MutableBlockPos();
         Vector3d tmpCenter = new Vector3d();
         Vector3d tmpSize = new Vector3d();
         Quaterniond tmpQuat = new Quaterniond();
         Vec3 eyePos = entity.getEyePosition();

         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, box)) {
            Pose3dc pose = subLevel.lastPose();
            box.transformInverse(pose, localBox);
            Quaterniond hitBoxOrientation = pose.orientation().conjugate(tmpQuat);
            hitBoxOrientation.rotateY(SubLevelEntityCollision.getHitBoxYaw(pose));
            OrientedBoundingBox3d entityObb = new OrientedBoundingBox3d(pose.transformPositionInverse(box.center(tmpCenter)), box.size(tmpSize), hitBoxOrientation, reusedVectors);
            OrientedBoundingBox3d blockObb = new OrientedBoundingBox3d(new Vector3d(), new Vector3d(1.0, 1.0, 1.0), JOMLConversion.QUAT_IDENTITY, reusedVectors);
            Vec3 localEyePos = pose.transformPositionInverse(eyePos);
            int eyeBlockX = Mth.floor(localEyePos.x);
            int eyeBlockZ = Mth.floor(localEyePos.z);
            int minX = Mth.floor(localBox.minX);
            int maxX = Mth.ceil(localBox.maxX);
            int minY = Mth.floor(localBox.minY);
            int maxY = Mth.ceil(localBox.maxY);
            int minZ = Mth.floor(localBox.minZ);
            int maxZ = Mth.ceil(localBox.maxZ);
            double lowestVertexY = Double.MAX_VALUE;
            boolean lowestVertexYSet = false;

            for (int x = minX; x < maxX; x++) {
               for (int y = minY; y < maxY; y++) {
                  for (int z = minZ; z < maxZ; z++) {
                     blockPos.set(x, y, z);
                     FluidState fluidState = level.getFluidState(blockPos);
                     if (!fluidState.isEmpty()) {
                        double fluidTopY = (double)y + fluidState.getHeight(level, blockPos);
                        if (!lowestVertexYSet) {
                           for (Vector3d vertex : entityObb.vertices(reusedVectors.a)) {
                              lowestVertexY = Math.min(lowestVertexY, vertex.y);
                           }

                           lowestVertexYSet = true;
                        }

                        if (!(fluidTopY < lowestVertexY)) {
                           blockObb.getPosition().set(x + 0.5, y + 0.5, z + 0.5);
                           if (!(OrientedBoundingBox3d.sat(entityObb, blockObb, reusedVectors.mtv).lengthSquared() <= 0.0)) {
                              Holder fluidType = fluidState.typeHolder();
                              SubLevelFluidHelper.Found found = fluidsByType.computeIfAbsent(fluidType, type -> new SubLevelFluidHelper.Found());
                              found.height = Math.max(fluidTopY - lowestVertexY, found.height);
                              if (x == eyeBlockX && z == eyeBlockZ && localEyePos.y >= y && localEyePos.y <= (double)y + fluidState.getHeight(level, blockPos)) {
                                 found.eyesInside = true;
                              }

                              if (!ignoreCurrent) {
                                 for (TagKey tag : FLUIDS_WITH_CURRENT) {
                                    if (fluidType.is(tag)) {
                                       SubLevelFluidHelper.Found tagFound = currentsByTag.computeIfAbsent(tag, tagKey -> new SubLevelFluidHelper.Found());
                                       tagFound.height = Math.max(found.height, tagFound.height);
                                       Vec3 flow = fluidState.getFlow(level, blockPos);
                                       if (tagFound.height < 0.4) {
                                          flow = flow.scale(tagFound.height);
                                       }

                                       tagFound.current = tagFound.current.add(pose.transformNormal(flow));
                                       tagFound.currentBlocks++;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static FluidState getFluidStateAt(Entity entity, BlockPos pos) {
      Level level = entity.level();
      FluidState fluidState = level.getFluidState(pos);
      if (!fluidState.isEmpty()) {
         return fluidState;
      } else {
         Vec3 centerPos = Vec3.atCenterOf(pos);
         BoundingBox3d searchBox = new BoundingBox3d(pos).expand(0.5);

         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, searchBox)) {
            BlockPos blockPos = BlockPos.containing(subLevel.lastPose().transformPositionInverse(centerPos));
            FluidState subLevelFluidState = level.getFluidState(blockPos);
            if (!subLevelFluidState.isEmpty()) {
               return subLevelFluidState;
            }
         }

         return fluidState;
      }
   }

   public static void applyCurrent(Entity entity, SubLevelFluidHelper.Found found, double strength) {
      if (found.currentBlocks != 0 && !(found.current.lengthSqr() < 1.0E-5F)) {
         Vec3 push = entity instanceof Player ? found.current.scale(1.0 / found.currentBlocks) : found.current.normalize();
         Vec3 deltaMovement = entity.getDeltaMovement();
         push = push.scale(strength);
         if (Math.abs(deltaMovement.x) < 0.003 && Math.abs(deltaMovement.z) < 0.003 && push.length() < 0.0045000000000000005) {
            push = push.normalize().scale(0.0045000000000000005);
         }

         entity.addDeltaMovement(push);
      }
   }

   public static final class Found {
      public double height;
      public boolean eyesInside;
      public Vec3 current = Vec3.ZERO;
      public int currentBlocks;
   }
}
