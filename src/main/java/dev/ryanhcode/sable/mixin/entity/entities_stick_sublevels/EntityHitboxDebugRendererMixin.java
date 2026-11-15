package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoProperties;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({EntityHitboxDebugRenderer.class})
public class EntityHitboxDebugRendererMixin {
   @WrapOperation(
      method = {"showHitboxes"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/gizmos/Gizmos;cuboid(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/gizmos/GizmoStyle;)Lnet/minecraft/gizmos/GizmoProperties;",
         ordinal = 0
      )}
   )
   private GizmoProperties sable$rotateSubLevelHitBox(
      AABB var1,
      GizmoStyle var2,
      Operation<GizmoProperties> var3,
      @Local(argsOnly = true,name = {"entity"}) Entity var4,
      @Local(argsOnly = true,name = {"partialTicks"}) float var5
   ) {
      return this.sable$drawRotatedBox(var1, var2, var3, var4, var5);
   }

   @WrapOperation(
      method = {"showHitboxes"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/gizmos/Gizmos;cuboid(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/gizmos/GizmoStyle;)Lnet/minecraft/gizmos/GizmoProperties;",
         ordinal = 1
      )}
   )
   private GizmoProperties sable$rotateSubLevelRidingStrap(
      AABB var1,
      GizmoStyle var2,
      Operation<GizmoProperties> var3,
      @Local(argsOnly = true,name = {"entity"}) Entity var4,
      @Local(argsOnly = true,name = {"partialTicks"}) float var5
   ) {
      Entity var6 = var4.getVehicle();
      if (var6 == null) {
         return (GizmoProperties)var3.call(var1, var2);
      } else {
         ClientSubLevel var7 = Sable.HELPER.getContainingClient(var6);
         if (var7 == null) {
            return (GizmoProperties)var3.call(var1, var2);
         } else {
            Vec3[] var8 = new Vec3[8];

            for (int var9 = 0; var9 < 8; var9++) {
               Vector3d var10 = new Vector3d(this.sable$cornerX(var1, var9), this.sable$cornerY(var1, var9), this.sable$cornerZ(var1, var9));
               var7.renderPose(var5).transformPosition(var10);
               var8[var9] = new Vec3(var10.x, var10.y, var10.z);
            }

            return Gizmos.addGizmo(new EntityHitboxDebugRendererMixin.BoxGizmo(var8, var2));
         }
      }
   }

   @WrapOperation(
      method = {"showHitboxes"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/gizmos/Gizmos;cuboid(Lnet/minecraft/world/phys/AABB;Lnet/minecraft/gizmos/GizmoStyle;)Lnet/minecraft/gizmos/GizmoProperties;",
         ordinal = 2
      )}
   )
   private GizmoProperties sable$rotateSubLevelEyeBox(
      AABB var1,
      GizmoStyle var2,
      Operation<GizmoProperties> var3,
      @Local(argsOnly = true,name = {"entity"}) Entity var4,
      @Local(argsOnly = true,name = {"partialTicks"}) float var5
   ) {
      return this.sable$drawRotatedBox(var1, var2, var3, var4, var5);
   }

   @Unique
   private GizmoProperties sable$drawRotatedBox(AABB var1, GizmoStyle var2, Operation<GizmoProperties> var3, Entity var4, float var5) {
      Vec3[] var6 = new Vec3[8];
      ClientSubLevel var7 = Sable.HELPER.getContainingClient(var4);
      Quaterniond var8 = EntitySubLevelRotationHelper.getEntityOrientation(
         var4, var1x -> ((ClientSubLevel)var1x).renderPose(var5), var5, EntitySubLevelRotationHelper.Type.ENTITY
      );
      if (var7 != null) {
         for (int var9 = 0; var9 < 8; var9++) {
            Vector3d var10 = new Vector3d(this.sable$cornerX(var1, var9), this.sable$cornerY(var1, var9), this.sable$cornerZ(var1, var9));
            var7.renderPose(var5).transformPosition(var10);
            var6[var9] = new Vec3(var10.x, var10.y, var10.z);
         }
      } else {
         if (var8 == null) {
            return (GizmoProperties)var3.call(var1, var2);
         }

         for (int var11 = 0; var11 < 8; var11++) {
            Vector3d var12 = new Vector3d(
               this.sable$cornerX(var1, var11) - var4.getEyePosition().x,
               this.sable$cornerY(var1, var11) - var4.getEyePosition().y,
               this.sable$cornerZ(var1, var11) - var4.getEyePosition().z
            );
            var8.transform(var12);
            var6[var11] = new Vec3(var12.x + var4.getEyePosition().x, var12.y + var4.getEyePosition().y, var12.z + var4.getEyePosition().z);
         }
      }

      return Gizmos.addGizmo(new EntityHitboxDebugRendererMixin.BoxGizmo(var6, var2));
   }

   @Unique
   private double sable$cornerX(AABB var1, int var2) {
      return (var2 & 4) == 0 ? var1.minX : var1.maxX;
   }

   @Unique
   private double sable$cornerY(AABB var1, int var2) {
      return (var2 & 2) == 0 ? var1.minY : var1.maxY;
   }

   @Unique
   private double sable$cornerZ(AABB var1, int var2) {
      return (var2 & 1) == 0 ? var1.minZ : var1.maxZ;
   }

   record BoxGizmo(Vec3[] corners, GizmoStyle style) implements Gizmo {
      @Override
      public void emit(@NonNull GizmoPrimitives var1, float var2) {
         if (this.style.hasFill()) {
            int var3 = this.style.multipliedFill(var2);
            var1.addQuad(this.corners[4], this.corners[6], this.corners[7], this.corners[5], var3);
            var1.addQuad(this.corners[0], this.corners[1], this.corners[3], this.corners[2], var3);
            var1.addQuad(this.corners[0], this.corners[2], this.corners[6], this.corners[4], var3);
            var1.addQuad(this.corners[1], this.corners[5], this.corners[7], this.corners[3], var3);
            var1.addQuad(this.corners[0], this.corners[4], this.corners[5], this.corners[1], var3);
            var1.addQuad(this.corners[2], this.corners[3], this.corners[7], this.corners[6], var3);
         }

         if (this.style.hasStroke()) {
            int var4 = this.style.multipliedStroke(var2);
            var1.addLine(this.corners[0], this.corners[4], var4, this.style.strokeWidth());
            var1.addLine(this.corners[0], this.corners[2], var4, this.style.strokeWidth());
            var1.addLine(this.corners[0], this.corners[1], var4, this.style.strokeWidth());
            var1.addLine(this.corners[4], this.corners[6], var4, this.style.strokeWidth());
            var1.addLine(this.corners[6], this.corners[2], var4, this.style.strokeWidth());
            var1.addLine(this.corners[2], this.corners[3], var4, this.style.strokeWidth());
            var1.addLine(this.corners[3], this.corners[1], var4, this.style.strokeWidth());
            var1.addLine(this.corners[1], this.corners[5], var4, this.style.strokeWidth());
            var1.addLine(this.corners[5], this.corners[4], var4, this.style.strokeWidth());
            var1.addLine(this.corners[3], this.corners[7], var4, this.style.strokeWidth());
            var1.addLine(this.corners[5], this.corners[7], var4, this.style.strokeWidth());
            var1.addLine(this.corners[6], this.corners[7], var4, this.style.strokeWidth());
         }
      }
   }
}
