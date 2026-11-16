package dev.ryanhcode.sable.mixin.compatibility.create;

import com.zurrtum.create.client.content.contraptions.render.ContraptionEntityRenderer;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.compatibility.create.CreateSupportsVisualizationContext;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create Contraption 实体渲染的子关卡适配（26.1.2）。
 *
 * <p><b>问题 1：方块层完全不渲染。</b>
 * {@code ContraptionEntityRenderer.extractRenderState} 里：
 * <pre>
 * boolean supportsVisualization = VisualizationManager.supportsVisualization(entity.level());
 * if (!supportsVisualization) { state.layers = SuperByteBuffer... }   // 只有 !support 时才构建方块层
 * </pre>
 * 子关卡内 Contraption 实体的 level 是父世界 → supportsVisualization 判定为 true →
 * 方块层交给 Flywheel 的 {@code ContraptionVisual}（entity visual）。而
 * {@code ContraptionVisual} 工作在父世界坐标系，实例位置 = Contraption 实体的 plot
 * 虚拟坐标（±20.48M）→ 结构被画在 20M 外，视觉上完全不可见。
 *
 * <p><b>问题 2：vanilla 路径的渲染位置是 plot 坐标。</b>
 * 该方法的开头会把 {@code state.x/y/z} 设为实体自身位置（lerp(xOld, getX())）——对子
 * 关卡内的实体这是 plot 虚拟坐标，覆盖了 Sable 的
 * {@code EntityRenderDispatcherMixin.sable$captureEntity} 在 {@code extractEntity} 里
 * 做的投影（主世界坐标），因此即使走 vanilla 路径也会画到 20M 外。
 *
 * <p><b>修复</b>：
 * <ol>
 *   <li>HEAD：把"当前正在渲染的 Contraption 实体"写入
 *       {@link CreateSupportsVisualizationContext}，供 {@code FlwApiLinkImplMixin}
 *       让 {@code supportsVisualization} 返回 false → 方块层走 vanilla SuperByteBuffer；</li>
 *   <li>RETURN：对位于子关卡内的实体，用子关卡 renderPose 把 {@code state.x/y/z}
 *       投影到主世界坐标（与 {@code EntityRenderDispatcherMixin.sable$captureEntity}
 *       对普通实体的处理一致）。</li>
 * </ol>
 *
 * <p>注入的是 {@code extractRenderState(Entity, EntityRenderState, float)} 桥接方法
 * （泛型 {@code extractRenderState(C, S, float)} 的擦除 bridge；javap 已确认存在）。
 * HEAD/RETURN 覆盖桥接方法内对泛型方法的调用，即覆盖整个提取过程，包括提前 return
 * 的分支。</p>
 */
@Mixin(value = ContraptionEntityRenderer.class, remap = false)
public abstract class ContraptionEntityRendererMixin {

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("HEAD"),
            remap = false)
    private void sable$beginContraptionExtract(Entity entity, EntityRenderState renderState, float partialTick,
                                               CallbackInfo ci) {
        if (entity instanceof AbstractContraptionEntity contraption) {
            CreateSupportsVisualizationContext.setCurrentContraption(contraption);
        }
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("RETURN"),
            remap = false)
    private void sable$endContraptionExtract(Entity entity, EntityRenderState renderState, float partialTick,
                                             CallbackInfo ci) {
        CreateSupportsVisualizationContext.clearCurrentContraption();
        if (!(entity instanceof AbstractContraptionEntity contraption) || renderState == null) {
            return;
        }
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient(contraption);
        if (subLevel == null) {
            return;
        }
        // 与 EntityRenderDispatcherMixin.sable$captureEntity 的普通实体投影保持一致：
        // plot 虚拟坐标 -> 主世界坐标（含子关卡位姿的平移/旋转/缩放）。
        Pose3dc pose = subLevel.renderPose(partialTick);
        Vector3d position = new Vector3d(
                Mth.lerp(partialTick, contraption.xOld, contraption.getX()),
                Mth.lerp(partialTick, contraption.yOld, contraption.getY()),
                Mth.lerp(partialTick, contraption.zOld, contraption.getZ()));
        pose.transformPosition(position);
        renderState.x = position.x;
        renderState.y = position.y;
        renderState.z = position.z;
    }
}
