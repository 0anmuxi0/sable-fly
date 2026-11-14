package dev.ryanhcode.sable.mixin.block_decal_render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public abstract class LevelRendererMixin {
   @Inject(
      method = {"submitBlockDestroyAnimation"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
         shift = Shift.AFTER
      )}
   )
   private void sable$transformBlockDamage(
      PoseStack poseStack,
      SubmitNodeCollector submitNodeCollector,
      LevelRenderState levelRenderState,
      CallbackInfo ci,
      @Local BlockBreakingRenderState renderState,
      @Local BlockPos pos
   ) {
      Vec3 plotPos = Vec3.atLowerCornerOf(pos);
      ClientSubLevel subLevel = sable$getSubLevel(pos);
      if (subLevel != null) {
         Pose3dc renderPose = subLevel.renderPose();
         Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
         Vec3 projectedPos = renderPose.transformPosition(plotPos);
         poseStack.translate(-(plotPos.x - cameraPos.x), -(plotPos.y - cameraPos.y), -(plotPos.z - cameraPos.z));
         poseStack.translate(projectedPos.x - cameraPos.x, projectedPos.y - cameraPos.y, projectedPos.z - cameraPos.z);
         poseStack.mulPose(new Matrix4f().rotate(new Quaternionf(renderPose.orientation())));
      }
   }

   @WrapOperation(
      method = {"submitBlockDestroyAnimation"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitBreakingBlockModel(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;I)V"
      )}
   )
   private void sable$submitBlockDamageOverBlocks(
      SubmitNodeCollector collector,
      PoseStack poseStack,
      List<BlockStateModelPart> parts,
      int progress,
      Operation<Void> original,
      @Local BlockBreakingRenderState renderState
   ) {
      // 26.2 has no boolean "materialFlag" parameter; the sub-level overlay is conveyed purely
      // by the transform applied in sable$transformBlockDamage above.
      original.call(new Object[]{collector, poseStack, parts, progress});
   }

   @Unique
   private static ClientSubLevel sable$getSubLevel(BlockPos pos) {
      ClientLevel level = Minecraft.getInstance().level;
      return level == null ? null : (ClientSubLevel)Sable.HELPER.getContaining(level, Vec3.atLowerCornerOf(pos));
   }
}
