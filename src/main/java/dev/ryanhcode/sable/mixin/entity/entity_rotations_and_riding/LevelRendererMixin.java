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
      EntityRenderDispatcher var1,
      EntityRenderState var2,
      CameraRenderState var3,
      double var4,
      double var6,
      double var8,
      PoseStack var10,
      SubmitNodeCollector var11,
      Operation<Void> var12
   ) {
      EntityTransformationExtension var13 = (EntityTransformationExtension)var2;
      Quaternionf var14 = var13.sable$getSubLevelOrientation();
      Vector3f var15 = var13.sable$getSubLevelScale();
      Vector3f var16 = var13.sable$getSubLevelPivot();
      if (var14 != null && var15 != null && var16 != null) {
         var10.pushPose();
         var10.translate(var4, var6, var8);
         var10.translate(var16.x, var16.y, var16.z);
         var10.scale(var15.x, var15.y, var15.z);
         var10.rotateAround(var14, 0.0F, 0.0F, 0.0F);
         var10.translate(-var16.x, -var16.y, -var16.z);
         var10.translate(-var4, -var6, -var8);
         var12.call(var1, var2, var3, var4, var6, var8, var10, var11);
         var10.popPose();
      } else {
         var12.call(var1, var2, var3, var4, var6, var8, var10, var11);
      }
   }
}
