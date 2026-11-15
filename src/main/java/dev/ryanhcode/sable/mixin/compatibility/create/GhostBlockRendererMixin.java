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
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.catnip.ghostblock.GhostBlockRenderer$DefaultGhostBlockRenderer"}
)
public class GhostBlockRendererMixin {
   @WrapOperation(
      method = {"render(Lnet/minecraft/client/renderer/block/BlockStateModelSet;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/phys/Vec3;Lcom/zurrtum/create/client/catnip/ghostblock/GhostBlockParams;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"
      )}
   )
   private void sable$projectGhostOntoSubLevel(
      PoseStack poseStack, double x, double y, double z, Operation<Void> original, @Local(argsOnly = true) Vec3 camera, @Local BlockPos pos
   ) {
      ClientSubLevel subLevel = this.sable$getSubLevel(pos);
      if (subLevel == null) {
         original.call(poseStack, x, y, z);
      } else {
         Pose3dc renderPose = subLevel.renderPose();
         Vec3 projectedPos = renderPose.transformPosition(Vec3.atLowerCornerOf(pos));
         original.call(poseStack, projectedPos.x - camera.x, projectedPos.y - camera.y, projectedPos.z - camera.z);
         poseStack.mulPose(new Matrix4f().rotate(new Quaternionf(renderPose.orientation())));
      }
   }

   private ClientSubLevel sable$getSubLevel(BlockPos pos) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         return Sable.HELPER.getContaining(level, pos) instanceof ClientSubLevel clientSubLevel ? clientSubLevel : null;
      }
   }
}
