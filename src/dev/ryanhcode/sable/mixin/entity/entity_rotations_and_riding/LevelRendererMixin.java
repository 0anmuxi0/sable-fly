package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.mixinterface.entity.entity_rotations_and_riding.EntityTransformationExtension;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.2 port of the sub-level entity rotation wrap.
 *
 * 26.3 called {@code PoseStack.rotate(Quaternionfc)}, which does not exist in 26.2
 * (only {@code rotateAround(Quaternionfc, float, float, float)}). The pivot is always
 * applied explicitly here, so {@code rotateAround(q, 0, 0, 0)} is exactly equivalent.
 * The extension fields are also null-guarded on 26.2 because a render state may be
 * extracted by a path that did not run the 26.3 capture hook.
 */
@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @WrapOperation(
      method = {"submitEntities"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;submit(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/client/renderer/state/level/CameraRenderState;DDDLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V"
      )}
   )
   private void sable$rotateEntity(
      EntityRenderDispatcher instance,
      EntityRenderState renderState,
      CameraRenderState camera,
      double x, double y, double z,
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      Operation<Void> original
   ) {
      EntityTransformationExtension transformation = (EntityTransformationExtension) (Object) renderState;
      Quaternionf orientation = transformation.sable$getSubLevelOrientation();
      Vector3f scale = transformation.sable$getSubLevelScale();
      Vector3f pivot = transformation.sable$getSubLevelPivot();

      if (orientation == null || scale == null || pivot == null) {
         original.call(new Object[]{instance, renderState, camera, x, y, z, poseStack, submitNodeCollector});
         return;
      }

      poseStack.pushPose();
      poseStack.translate(x, y, z);
      poseStack.translate(pivot.x, pivot.y, pivot.z);
      poseStack.scale(scale.x, scale.y, scale.z);
      poseStack.rotateAround(orientation, 0.0F, 0.0F, 0.0F);   // 26.2 replacement for rotate(q)
      poseStack.translate(-pivot.x, -pivot.y, -pivot.z);
      poseStack.translate(-x, -y, -z);
      original.call(new Object[]{instance, renderState, camera, x, y, z, poseStack, submitNodeCollector});
      poseStack.popPose();
   }
}
