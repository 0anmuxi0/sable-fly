package dev.ryanhcode.sable.mixin.entity.entity_interaction;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Optional;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ProjectileUtil.class})
public class ProjectileUtilMixin {
   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )}
   )
   private static double sable$fixDistance(Vec3 start, Vec3 hitPos, @Local(argsOnly = true) Entity source) {
      return Sable.HELPER.distanceSquaredWithSubLevels(source.level(), start, hitPos);
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )}
   )
   private static double sable$fixDistance2(Vec3 start, Vec3 hitPos, @Local(argsOnly = true) Level level) {
      return Sable.HELPER.distanceSquaredWithSubLevels(level, start, hitPos);
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )}
   )
   private static Optional<Vec3> sable$getBoundingBox(
      AABB toClip, Vec3 start, Vec3 end, @Local(argsOnly = true) Entity source, @Local(ordinal = 2) Entity clipping
   ) {
      Vec3 localStart = sable$toLocal(clipping, start);
      Vec3 localEnd = sable$toLocal(clipping, end);
      Optional<Vec3> hit = sable$getHitPosWithSublevels(
         source.level(),
         toClip,
         localStart,
         localEnd,
         Sable.HELPER.getContaining(source.level(), start),
         Sable.HELPER.getContaining(clipping.level(), clipping.position())
      );
      return hit.map(vec -> sable$toGlobal(clipping, vec));
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )}
   )
   private static Optional<Vec3> sable$getBoundingBox2(
      AABB toClip, Vec3 start, Vec3 end, @Local(argsOnly = true) Level level, @Local(ordinal = 2) Entity clipping
   ) {
      Vec3 localStart = sable$toLocal(clipping, start);
      Vec3 localEnd = sable$toLocal(clipping, end);
      Optional<Vec3> hit = sable$getHitPosWithSublevels(
         level, toClip, localStart, localEnd, Sable.HELPER.getContaining(level, start), Sable.HELPER.getContaining(clipping.level(), clipping.position())
      );
      return hit.map(vec -> sable$toGlobal(clipping, vec));
   }

   @Redirect(
      method = {"getManyEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;FLnet/minecraft/world/level/ClipContext$Block;ZZ)Ljava/util/Collection;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )}
   )
   private static Optional<Vec3> sable$getBoundingBox3(
      AABB toClip, Vec3 start, Vec3 end, @Local(argsOnly = true) Level level, @Local(ordinal = 1) Entity clipping
   ) {
      Vec3 localStart = sable$toLocal(clipping, start);
      Vec3 localEnd = sable$toLocal(clipping, end);
      Optional<Vec3> hit = sable$getHitPosWithSublevels(
         level, toClip, localStart, localEnd, Sable.HELPER.getContaining(level, start), Sable.HELPER.getContaining(clipping.level(), clipping.position())
      );
      return hit.map(vec -> sable$toGlobal(clipping, vec));
   }

   @WrapOperation(
      method = {"getHitEntitiesAlong(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;Lnet/minecraft/world/phys/Vec3;FLnet/minecraft/world/level/ClipContext$Block;)Lcom/mojang/datafixers/util/Either;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;clipIncludingBorder(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"
      )}
   )
   private static BlockHitResult sable$fixBlockHit(Level instance, ClipContext clipContext, Operation<BlockHitResult> original) {
      BlockHitResult blockHitResult = original.call(instance, clipContext);
      if (blockHitResult.getType() == Type.MISS) {
         return blockHitResult;
      } else {
         SubLevel subLevelHit = Sable.HELPER.getContaining(instance, blockHitResult.getLocation());
         SubLevel subLevelFrom = Sable.HELPER.getContaining(instance, clipContext.getFrom());
         if (subLevelHit == subLevelFrom) {
            return blockHitResult;
         } else {
            Vec3 pos = null;
            if (subLevelHit != null) {
               pos = subLevelHit.logicalPose().transformPosition(blockHitResult.getLocation());
            }

            if (subLevelFrom != null) {
               pos = subLevelFrom.logicalPose().transformPosition(pos);
            }

            return new BlockHitResult(pos, blockHitResult.getDirection(), blockHitResult.getBlockPos(), blockHitResult.isInside());
         }
      }
   }

   @Unique
   @NotNull
   private static Optional<Vec3> sable$getHitPosWithSublevels(
      Level level, AABB toClip, Vec3 start, Vec3 end, SubLevel sourceSubLevel, SubLevel clippingSubLevel
   ) {
      if (sourceSubLevel == clippingSubLevel) {
         return toClip.clip(start, end);
      } else {
         if (level instanceof LevelPoseProviderExtension poseProvider) {
            if (sourceSubLevel != null) {
               start = poseProvider.sable$getPose(sourceSubLevel).transformPosition(start);
               end = poseProvider.sable$getPose(sourceSubLevel).transformPosition(end);
            }

            if (clippingSubLevel != null) {
               start = poseProvider.sable$getPose(clippingSubLevel).transformPositionInverse(start);
               end = poseProvider.sable$getPose(clippingSubLevel).transformPositionInverse(end);
            }
         } else {
            if (sourceSubLevel != null) {
               start = sourceSubLevel.logicalPose().transformPosition(start);
               end = sourceSubLevel.logicalPose().transformPosition(end);
            }

            if (clippingSubLevel != null) {
               start = clippingSubLevel.logicalPose().transformPositionInverse(start);
               end = clippingSubLevel.logicalPose().transformPositionInverse(end);
            }
         }

         return toClip.clip(start, end);
      }
   }

   @Unique
   private static Vec3 sable$toLocal(Entity entity, Vec3 global) {
      Quaterniond orientation = EntitySubLevelRotationHelper.getEntityOrientation(
         entity,
         subLevel -> subLevel instanceof ClientSubLevel clientSubLevel ? clientSubLevel.renderPose() : subLevel.lastPose(),
         1.0F,
         EntitySubLevelRotationHelper.Type.ENTITY
      );
      if (orientation != null && EntitySubLevelUtil.shouldKick(entity)) {
         Vec3 eyePos = entity.getEyePosition();
         Vector3d pos = new Vector3d(global.x - eyePos.x, global.y - eyePos.y, global.z - eyePos.z);
         orientation.conjugate().transform(pos);
         return new Vec3(pos.x + eyePos.x, pos.y + eyePos.y, pos.z + eyePos.z);
      } else {
         return global;
      }
   }

   @Unique
   private static Vec3 sable$toGlobal(Entity entity, Vec3 local) {
      Quaterniond orientation = EntitySubLevelRotationHelper.getEntityOrientation(
         entity,
         subLevel -> subLevel instanceof ClientSubLevel clientSubLevel ? clientSubLevel.renderPose() : subLevel.lastPose(),
         1.0F,
         EntitySubLevelRotationHelper.Type.ENTITY
      );
      if (orientation != null && EntitySubLevelUtil.shouldKick(entity)) {
         Vec3 eyePos = entity.getEyePosition();
         Vector3d pos = new Vector3d(local.x - eyePos.x, local.y - eyePos.y, local.z - eyePos.z);
         orientation.transform(pos);
         return new Vec3(pos.x + eyePos.x, pos.y + eyePos.y, pos.z + eyePos.z);
      } else {
         return local;
      }
   }
}
