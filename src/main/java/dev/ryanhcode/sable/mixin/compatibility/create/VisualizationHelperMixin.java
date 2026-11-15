package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让位于 Sable sub-level 内的 Create 动力件（飞轮 / 锯片 / 齿轮 / 大水轮 等）
 * 不进入 Create 的 Flywheel 可视化系统，改走 vanilla 的 {@code BlockEntityRenderer}。
 *
 * <p>根因（javap 逐字节码确认）：
 *
 * <ol>
 *   <li>Create 的动力件视觉由 Flywheel 可视化系统绘制，它工作在<b>父世界坐标系</b>下：
 *       {@code AbstractBlockEntityVisual} 里 {@code pos = BE.getBlockPos()}，
 *       {@code visualPos = pos - context.renderOrigin()}，
 *       最终 instance 落在 BE 的<b>原始 plot 坐标</b>。</li>
 *   <li>Sable 的 sub-level 有自己的平移 / 旋转 / 缩放变换（{@code SubLevel.renderPose()}），
 *       Flywheel 完全不知道这套变换 —— 于是动力件停在 plot 原坐标不动，
 *       视觉上就是"物理结构上的动态结构不渲染 / 飞轮不见了"。</li>
 *   <li>Sable 的 plot 区块是用父世界的 {@code LevelChunk} 构造的
 *       （{@code plot/ClientChunkCacheMixin}：{@code new LevelChunk(this.level, var8)}），
 *       所以 plot 方块实体的 {@code getLevel()} 返回<b>父世界</b>，
 *       于是 {@code VisualizationManager.supportsVisualization(level)} 判定为 true，
 *       Create 跳过 vanilla 渲染，把动力件完全交给 Flywheel。</li>
 * </ol>
 *
 * <p>解决办法：对位于 sub-level 内的动力件，让 {@code canVisualize} 返回 false。
 * 这个改动同时修好三处：
 *
 * <ul>
 *   <li>{@code BlockEntityStorage.willAccept} → {@code canVisualize} 为 false
 *       → 不会进入 Flywheel 队列，不会被 Flywheel 用父世界坐标重复绘制；</li>
 *   <li>Create 的 {@code ClientLevelMixin.flywheel$decideNotToRenderEntity}
 *       （依据 {@code VisualHelper.skipVanillaRender} → {@code canVisualize}）
 *       → 不会跳过 vanilla 渲染；</li>
 *   <li>{@code BlockEntityStorage.createRaw} → 没有 visualizer → 不创建 visual。</li>
 * </ul>
 *
 * <p>动力件于是走 vanilla 的 {@code BlockEntityRenderer} 路径，
 * 再由 Sable 已有的 sub-level 方块实体位姿变换
 * （{@code sublevel_render/block_entity_render/LevelRendererMixin.sable$transformBlockEntity}）
 * 施加正确的 sub-level 变换 —— 两者正好配套。
 *
 * <p>主世界完全不受影响：只有该动力件确实位于某个 sub-level 内时才改变判定结果。
 */
@Pseudo
@Mixin(targets = "com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper")
public class VisualizationHelperMixin {
   @WrapOperation(
      method = {"canVisualize"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;"
      )}
   )
   private Object sable$skipSubLevelVisuals(
      BlockEntity blockEntity, Operation<Object> original
   ) {
      if (Sable.HELPER.getContaining(blockEntity) != null) {
         return null;
      }

      return original.call(blockEntity);
   }
}
