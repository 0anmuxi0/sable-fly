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
   private static double sable$fixDistance(Vec3 var0, Vec3 var1, @Local(argsOnly = true) Entity var2) {
      return Sable.HELPER.distanceSquaredWithSubLevels(var2.level(), var0, var1);
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )}
   )
   private static double sable$fixDistance2(Vec3 var0, Vec3 var1, @Local(argsOnly = true) Level var2) {
      return Sable.HELPER.distanceSquaredWithSubLevels(var2, var0, var1);
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )}
   )
   private static Optional<Vec3> sable$getBoundingBox(AABB var0, Vec3 var1, Vec3 var2, @Local(argsOnly = true) Entity var3, @Local(ordinal = 2) Entity var4) {
      Vec3 var5 = sable$toLocal(var4, var1);
      Vec3 var6 = sable$toLocal(var4, var2);
      Optional<Vec3> var7 = sable$getHitPosWithSublevels(
         var3.level(), var0, var5, var6, Sable.HELPER.getContaining(var3.level(), var1), Sable.HELPER.getContaining(var4.level(), var4.position())
      );
      return var7.map(var1x -> sable$toGlobal(var4, var1x));
   }

   @Redirect(
      method = {"getEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;F)Lnet/minecraft/world/phys/EntityHitResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )}
   )
   private static Optional<Vec3> sable$getBoundingBox2(AABB var0, Vec3 var1, Vec3 var2, @Local(argsOnly = true) Level var3, @Local(ordinal = 2) Entity var4) {
      Vec3 var5 = sable$toLocal(var4, var1);
      Vec3 var6 = sable$toLocal(var4, var2);
      Optional<Vec3> var7 = sable$getHitPosWithSublevels(
         var3, var0, var5, var6, Sable.HELPER.getContaining(var3, var1), Sable.HELPER.getContaining(var4.level(), var4.position())
      );
      return var7.map(var1x -> sable$toGlobal(var4, var1x));
   }

   @Redirect(
      method = {"getManyEntityHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;FLnet/minecraft/world/level/ClipContext$Block;Z)Ljava/util/Collection;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"
      )}
   )
   private static Optional<Vec3> sable$getBoundingBox3(AABB var0, Vec3 var1, Vec3 var2, @Local(argsOnly = true) Level var3, @Local(ordinal = 1) Entity var4) {
      Vec3 var5 = sable$toLocal(var4, var1);
      Vec3 var6 = sable$toLocal(var4, var2);
      Optional<Vec3> var7 = sable$getHitPosWithSublevels(
         var3, var0, var5, var6, Sable.HELPER.getContaining(var3, var1), Sable.HELPER.getContaining(var4.level(), var4.position())
      );
      return var7.map(var1x -> sable$toGlobal(var4, var1x));
   }

   @WrapOperation(
      method = {"getHitEntitiesAlong(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Ljava/util/function/Predicate;Lnet/minecraft/world/phys/Vec3;FLnet/minecraft/world/level/ClipContext$Block;)Lcom/mojang/datafixers/util/Either;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;clipIncludingBorder(Lnet/minecraft/world/level/ClipContext;)Lnet/minecraft/world/phys/BlockHitResult;"
      )}
   )
   private static BlockHitResult sable$fixBlockHit(Level var0, ClipContext var1, Operation<BlockHitResult> var2) {
      BlockHitResult var3 = (BlockHitResult)var2.call(var0, var1);
      if (var3.getType() == Type.MISS) {
         return var3;
      } else {
         SubLevel var4 = Sable.HELPER.getContaining(var0, var3.getLocation());
         SubLevel var5 = Sable.HELPER.getContaining(var0, var1.getFrom());
         if (var4 == var5) {
            return var3;
         } else {
            Vec3 var6 = null;
            if (var4 != null) {
               var6 = var4.logicalPose().transformPosition(var3.getLocation());
            }

            if (var5 != null) {
               var6 = var5.logicalPose().transformPositionInverse(var6);
            }

            return new BlockHitResult(var6, var3.getDirection(), var3.getBlockPos(), var3.isInside());
         }
      }
   }

   @Unique
   @NotNull
   private static Optional<Vec3> sable$getHitPosWithSublevels(Level var0, AABB var1, Vec3 var2, Vec3 var3, SubLevel var4, SubLevel var5) {
      if (var4 == var5) {
         return var1.clip(var2, var3);
      } else {
         if (var0 instanceof LevelPoseProviderExtension var6) {
            if (var4 != null) {
               var2 = var6.sable$getPose(var4).transformPosition(var2);
               var3 = var6.sable$getPose(var4).transformPosition(var3);
            }

            if (var5 != null) {
               var2 = var6.sable$getPose(var5).transformPositionInverse(var2);
               var3 = var6.sable$getPose(var5).transformPositionInverse(var3);
            }
         } else {
            if (var4 != null) {
               var2 = var4.logicalPose().transformPosition(var2);
               var3 = var4.logicalPose().transformPosition(var3);
            }

            if (var5 != null) {
               var2 = var5.logicalPose().transformPositionInverse(var2);
               var3 = var5.logicalPose().transformPositionInverse(var3);
            }
         }

         return var1.clip(var2, var3);
      }
   }

   @Unique
   private static Vec3 sable$toLocal(Entity var0, Vec3 var1) {
      Quaterniond var2 = EntitySubLevelRotationHelper.getEntityOrientation(
         var0, var0x -> var0x instanceof ClientSubLevel var1x ? var1x.renderPose() : var0x.lastPose(), 1.0F, EntitySubLevelRotationHelper.Type.ENTITY
      );
      if (var2 != null && EntitySubLevelUtil.shouldKick(var0)) {
         Vec3 var3 = var0.getEyePosition();
         Vector3d var4 = new Vector3d(var1.x - var3.x, var1.y - var3.y, var1.z - var3.z);
         var2.conjugate().transform(var4);
         return new Vec3(var4.x + var3.x, var4.y + var3.y, var4.z + var3.z);
      } else {
         return var1;
      }
   }

   @Unique
   private static Vec3 sable$toGlobal(Entity var0, Vec3 var1) {
      Quaterniond var2 = EntitySubLevelRotationHelper.getEntityOrientation(
         var0, var0x -> var0x instanceof ClientSubLevel var1x ? var1x.renderPose() : var0x.lastPose(), 1.0F, EntitySubLevelRotationHelper.Type.ENTITY
      );
      if (var2 != null && EntitySubLevelUtil.shouldKick(var0)) {
         Vec3 var3 = var0.getEyePosition();
         Vector3d var4 = new Vector3d(var1.x - var3.x, var1.y - var3.y, var1.z - var3.z);
         var2.transform(var4);
         return new Vec3(var4.x + var3.x, var4.y + var3.y, var4.z + var3.z);
      } else {
         return var1;
      }
   }
}
