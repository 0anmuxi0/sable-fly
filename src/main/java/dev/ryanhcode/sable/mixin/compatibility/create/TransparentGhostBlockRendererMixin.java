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

/**
 * 让"幽灵方块"（放置预览）在 sub-level 上渲染到正确的世界位置。
 *
 * <p><b>26.3 与 26.1.2/26.2 的关键差异</b>：26.1.2/26.2 的
 * {@code TransparentGhostBlockRenderer.render} 调用
 * {@code PoseStack.translate(DDD)}（double 参数，直接在 poseStack 上平移）；
 * 而 <b>26.3</b> 改为 {@code poseStack.last().translate(FFF)}（float 参数，
 * 作用在 {@code PoseStack.Pose} 上）。字节码对比：
 * <pre>
 * 26.2  48: invokevirtual PoseStack.translate:(DDD)V
 * 26.3  58: invokevirtual PoseStack\$Pose.translate:(FFF)Lorg/joml/Matrix4f;
 * </pre>
 * 因此 26.3 必须拦截 {@code PoseStack$Pose.translate(FFF)}，否则
 * {@code @At(INVOKE, PoseStack.translate)} 匹配不到任何调用点，
 * Mixin 会抛 {@code InvalidInjectionException: Scanned 0 target(s)} 并导致客户端崩溃。
 *
 * <p>修复方式：拦截 {@code Pose.translate(FFF)}，把 sub-level 内方块的
 * 预览位置从 plot 虚拟坐标投影到主世界坐标，并叠加 sub-level 的朝向。
 */
@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.catnip.ghostblock.GhostBlockRenderer$TransparentGhostBlockRenderer"}
)
public class TransparentGhostBlockRendererMixin {
   @WrapOperation(
      method = {"render(Lnet/minecraft/client/renderer/block/BlockStateModelSet;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/phys/Vec3;Lcom/zurrtum/create/client/catnip/ghostblock/GhostBlockParams;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack$Pose;translate(FFF)Lorg/joml/Matrix4f;"
      )}
   )
   private Matrix4f sable$projectGhostOntoSubLevel(
      PoseStack.Pose pose, float x, float y, float z, Operation<Matrix4f> original,
      @Local(argsOnly = true) PoseStack poseStack, @Local(argsOnly = true) Vec3 camera, @Local BlockPos pos
   ) {
      ClientSubLevel subLevel = this.sable$getSubLevel(pos);
      if (subLevel == null) {
         return original.call(pose, x, y, z);
      }
      Pose3dc renderPose = subLevel.renderPose();
      Vec3 projectedPos = renderPose.transformPosition(Vec3.atLowerCornerOf(pos));
      Matrix4f result = original.call(pose,
            (float) (projectedPos.x - camera.x),
            (float) (projectedPos.y - camera.y),
            (float) (projectedPos.z - camera.z));
      poseStack.mulPose(new Matrix4f().rotate(new Quaternionf(renderPose.orientation())));
      return result;
   }

   private ClientSubLevel sable$getSubLevel(BlockPos pos) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      }
      return Sable.HELPER.getContaining(level, pos) instanceof ClientSubLevel clientSubLevel ? clientSubLevel : null;
   }
}
