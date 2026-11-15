package dev.ryanhcode.sable.mixin.compatibility.create;

import dev.ryanhcode.sable.compatibility.create.CreateSupportsVisualizationContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 vanilla 抽取方块实体渲染状态期间，记录"当前正在处理的方块实体"，
 * 供 {@link CreateSupportsVisualizationContext} 使用。
 *
 * <p><b>为什么需要</b>
 *
 * <p>Create 的 {@code VisualizationManager.supportsVisualization(LevelAccessor)} 只有 level 参数，
 * 而 Sable 的 sub-level 方块实体的 {@code getLevel()} 就是父世界 level，
 * 因此需要靠"当前 BE"来区分主世界和 sub-level（详见
 * {@link CreateSupportsVisualizationContext} 的说明）。
 *
 * <p><b>注入点</b>
 *
 * <p>vanilla {@code BlockEntityRenderDispatcher.tryExtractRenderState} 的字节码里，
 * 调用 {@code renderer.extractRenderState(...)} 的位置在 offset 69，
 * 而方法在 offset 83 直接 {@code areturn} 返回渲染状态。
 * 所以在 HEAD 设置上下文、在 RETURN 清除上下文，就能精确覆盖
 * {@code extractRenderState} 的整个执行过程。
 *
 * <p>注意这里用的是<b>裸方法名</b>（不带描述符）：该方法是泛型方法
 * {@code <E extends BlockEntity, S extends BlockEntityRenderState> S tryExtractRenderState(E, float, CrumblingOverlay)}，
 * 带描述符时泛型参数无法解析（实测会报 {@code Scanned 0 target(s)}）。
 * Sable 已有的 {@code sublevel_render/BlockEntityRenderDispatcherMixin} 也用同样的裸方法名注入。
 */
@Mixin({BlockEntityRenderDispatcher.class})
public class BlockEntityRenderDispatcherCreateMixin {
   @Inject(
      method = {"tryExtractRenderState"},
      at = {@At("HEAD")}
   )
   private void sable$setCreateContext(
      BlockEntity blockEntity, float partialTick, CrumblingOverlay crumblingOverlay,
      CallbackInfoReturnable<BlockEntityRenderState> cir
   ) {
      CreateSupportsVisualizationContext.setCurrentBlockEntity(blockEntity);
   }

   @Inject(
      method = {"tryExtractRenderState"},
      at = {@At("RETURN")}
   )
   private void sable$clearCreateContext(
      BlockEntity blockEntity, float partialTick, CrumblingOverlay crumblingOverlay,
      CallbackInfoReturnable<BlockEntityRenderState> cir
   ) {
      CreateSupportsVisualizationContext.clearCurrentBlockEntity();
   }
}
