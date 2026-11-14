package dev.ryanhcode.sable.mixin.conduit;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ConduitRenderer;
import net.minecraft.client.renderer.blockentity.state.ConduitRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.21.1 语义：当潜影之瞳（激活的导管）所在方块位于子关卡内时，在其自身旋转（mulPose
 * ordinal=5）之后追加子关卡渲染姿态的反转，保证瞳孔朝向与子关卡旋转同步。
 *
 * 26.2 适配：方块实体渲染重构为 extractRenderState + submit 两阶段。26.2
 * ConduitRenderer.submit(ConduitRenderState,...) 内 mulPose(Quaternionfc) 共 5 次，
 * 取 ordinal=4（最后一次，对应活动之眼段）近似对齐 1.21.1 的 ordinal=5——运行时需视觉验证。
 * 子关卡定位改用 state.blockPos + Sable.HELPER.getContainingClient(Vec3i)；partialTick
 * 在 26.2 state 化渲染中不再可用，改用无参 renderPose()（最新姿态，损失单帧插值精度）。
 */
@Mixin(ConduitRenderer.class)
public class ConduitRendererMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/ConduitRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", ordinal = 4))
    private void sable$reorientEye(ConduitRenderState renderState, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                                   CameraRenderState cameraRenderState, CallbackInfo ci) {
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(renderState.blockPos);
        if (subLevel != null) {
            poseStack.mulPose(subLevel.renderPose().orientation().get(new Quaternionf()).invert());
        }
    }
}
