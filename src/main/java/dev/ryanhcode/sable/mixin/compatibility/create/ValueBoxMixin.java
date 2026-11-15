package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBox"}
)
public class ValueBoxMixin {
   @Shadow
   protected BlockPos pos;

   @WrapOperation(
      method = {"submit(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/phys/Vec3;F)V"},
      at = {@At(
         value = "INVOKE",
         ordinal = 0,
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"
      )}
   )
   private void sable$submitAtWorldPos(
      PoseStack poseStack, double x, double y, double z, Operation<Void> original, @Local(argsOnly = true) Minecraft mc, @Local(argsOnly = true) Vec3 camera
   ) {
      ClientSubLevel subLevel = this.sable$getSubLevel(mc);
      if (subLevel == null) {
         original.call(poseStack, x, y, z);
      } else {
         Pose3dc renderPose = subLevel.renderPose();
         Vec3 projectedPos = renderPose.transformPosition(Vec3.atLowerCornerOf(this.pos));
         original.call(poseStack, projectedPos.x - camera.x, projectedPos.y - camera.y, projectedPos.z - camera.z);
         poseStack.mulPose(new Matrix4f().rotate(new Quaternionf(renderPose.orientation())));
      }
   }

   private ClientSubLevel sable$getSubLevel(Minecraft mc) {
      ClientLevel level = mc.level;
      if (level == null) {
         return null;
      } else {
         return Sable.HELPER.getContaining(level, this.pos) instanceof ClientSubLevel clientSubLevel ? clientSubLevel : null;
      }
   }
}
