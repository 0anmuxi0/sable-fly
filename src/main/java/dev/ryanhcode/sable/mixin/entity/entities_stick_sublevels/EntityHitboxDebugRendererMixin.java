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
      AABB box,
      GizmoStyle style,
      Operation<GizmoProperties> operation,
      @Local(argsOnly = true,name = {"entity"}) Entity entity,
      @Local(argsOnly = true,name = {"partialTicks"}) float partialTicks
   ) {
      return this.sable$drawRotatedBox(box, style, operation, entity, partialTicks);
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
      AABB box,
      GizmoStyle style,
      Operation<GizmoProperties> operation,
      @Local(argsOnly = true,name = {"entity"}) Entity entity,
      @Local(argsOnly = true,name = {"partialTicks"}) float partialTicks
   ) {
      Entity vehicle = entity.getVehicle();
      if (vehicle == null) {
         return (GizmoProperties)operation.call(box, style);
      } else {
         ClientSubLevel subLevel = Sable.HELPER.getContainingClient(vehicle);
         if (subLevel == null) {
            return (GizmoProperties)operation.call(box, style);
         } else {
            Vec3[] corners = new Vec3[8];

            for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
               Vector3d cornerPos = new Vector3d(this.sable$cornerX(box, cornerIndex), this.sable$cornerY(box, cornerIndex), this.sable$cornerZ(box, cornerIndex));
               subLevel.renderPose(partialTicks).transformPosition(cornerPos);
               corners[cornerIndex] = new Vec3(cornerPos.x, cornerPos.y, cornerPos.z);
            }

            return Gizmos.addGizmo(new EntityHitboxDebugRendererMixin.BoxGizmo(corners, style));
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
      AABB box,
      GizmoStyle style,
      Operation<GizmoProperties> operation,
      @Local(argsOnly = true,name = {"entity"}) Entity entity,
      @Local(argsOnly = true,name = {"partialTicks"}) float partialTicks
   ) {
      return this.sable$drawRotatedBox(box, style, operation, entity, partialTicks);
   }

   @Unique
   private GizmoProperties sable$drawRotatedBox(AABB box, GizmoStyle style, Operation<GizmoProperties> operation, Entity entity, float partialTicks) {
      Vec3[] corners = new Vec3[8];
      ClientSubLevel subLevel = Sable.HELPER.getContainingClient(entity);
      Quaterniond rotation = EntitySubLevelRotationHelper.getEntityOrientation(
         entity, candidateSubLevel -> ((ClientSubLevel)candidateSubLevel).renderPose(partialTicks), partialTicks, EntitySubLevelRotationHelper.Type.ENTITY
      );
      if (subLevel != null) {
         for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
            Vector3d cornerPos = new Vector3d(this.sable$cornerX(box, cornerIndex), this.sable$cornerY(box, cornerIndex), this.sable$cornerZ(box, cornerIndex));
            subLevel.renderPose(partialTicks).transformPosition(cornerPos);
            corners[cornerIndex] = new Vec3(cornerPos.x, cornerPos.y, cornerPos.z);
         }
      } else {
         if (rotation == null) {
            return (GizmoProperties)operation.call(box, style);
         }

         for (int cornerIndex = 0; cornerIndex < 8; cornerIndex++) {
            Vector3d cornerPos = new Vector3d(
               this.sable$cornerX(box, cornerIndex) - entity.getEyePosition().x,
               this.sable$cornerY(box, cornerIndex) - entity.getEyePosition().y,
               this.sable$cornerZ(box, cornerIndex) - entity.getEyePosition().z
            );
            rotation.transform(cornerPos);
            corners[cornerIndex] = new Vec3(cornerPos.x + entity.getEyePosition().x, cornerPos.y + entity.getEyePosition().y, cornerPos.z + entity.getEyePosition().z);
         }
      }

      return Gizmos.addGizmo(new EntityHitboxDebugRendererMixin.BoxGizmo(corners, style));
   }

   @Unique
   private double sable$cornerX(AABB box, int cornerIndex) {
      return (cornerIndex & 4) == 0 ? box.minX : box.maxX;
   }

   @Unique
   private double sable$cornerY(AABB box, int cornerIndex) {
      return (cornerIndex & 2) == 0 ? box.minY : box.maxY;
   }

   @Unique
   private double sable$cornerZ(AABB box, int cornerIndex) {
      return (cornerIndex & 1) == 0 ? box.minZ : box.maxZ;
   }

   record BoxGizmo(Vec3[] corners, GizmoStyle style) implements Gizmo {
      @Override
      public void emit(@NonNull GizmoPrimitives primitives, float alpha) {
         if (this.style.hasFill()) {
            int fillColor = this.style.multipliedFill(alpha);
            primitives.addQuad(this.corners[4], this.corners[6], this.corners[7], this.corners[5], fillColor);
            primitives.addQuad(this.corners[0], this.corners[1], this.corners[3], this.corners[2], fillColor);
            primitives.addQuad(this.corners[0], this.corners[2], this.corners[6], this.corners[4], fillColor);
            primitives.addQuad(this.corners[1], this.corners[5], this.corners[7], this.corners[3], fillColor);
            primitives.addQuad(this.corners[0], this.corners[4], this.corners[5], this.corners[1], fillColor);
            primitives.addQuad(this.corners[2], this.corners[3], this.corners[7], this.corners[6], fillColor);
         }

         if (this.style.hasStroke()) {
            int strokeColor = this.style.multipliedStroke(alpha);
            primitives.addLine(this.corners[0], this.corners[4], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[0], this.corners[2], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[0], this.corners[1], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[4], this.corners[6], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[6], this.corners[2], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[2], this.corners[3], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[3], this.corners[1], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[1], this.corners[5], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[5], this.corners[4], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[3], this.corners[7], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[5], this.corners[7], strokeColor, this.style.strokeWidth());
            primitives.addLine(this.corners[6], this.corners[7], strokeColor, this.style.strokeWidth());
         }
      }
   }
}
