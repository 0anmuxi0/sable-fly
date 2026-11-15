package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.catnip.outliner.BlockClusterOutline"}
)
public class BlockClusterOutlineMixin {
   @WrapOperation(
      method = {"submitFaces"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"
      )}
   )
   private void sable$projectFacesOntoSubLevel(
      PoseStack ms, double x, double y, double z, Operation<Void> original, @Local(argsOnly = true) Vec3 camera, @Local BlockPos anchor
   ) {
      ClientSubLevel subLevel = SubLevelOutlinePose.find(Vec3.atLowerCornerOf(anchor));
      if (subLevel == null) {
         original.call(ms, x, y, z);
      } else {
         Pose3dc pose = subLevel.renderPose();
         SubLevelOutlinePose.apply(pose, ms);
         Vec3 plotCamera = SubLevelOutlinePose.plotCamera(pose, camera);
         original.call(ms, anchor.getX() - plotCamera.x, anchor.getY() - plotCamera.y, anchor.getZ() - plotCamera.z);
      }
   }

   @WrapOperation(
      method = {"submitEdges"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"
      )}
   )
   private void sable$projectEdgesOntoSubLevel(
      PoseStack ms, double x, double y, double z, Operation<Void> original, @Local(argsOnly = true) Vec3 camera, @Local BlockPos anchor
   ) {
      ClientSubLevel subLevel = SubLevelOutlinePose.find(Vec3.atLowerCornerOf(anchor));
      if (subLevel == null) {
         original.call(ms, x, y, z);
      } else {
         Pose3dc pose = subLevel.renderPose();
         SubLevelOutlinePose.apply(pose, ms);
         Vec3 plotCamera = SubLevelOutlinePose.plotCamera(pose, camera);
         original.call(ms, anchor.getX() - plotCamera.x, anchor.getY() - plotCamera.y, anchor.getZ() - plotCamera.z);
      }
   }
}
