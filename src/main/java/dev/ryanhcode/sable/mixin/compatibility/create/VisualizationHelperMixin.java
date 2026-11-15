package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.flywheel.api.visualization.BlockEntityVisualizer;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让位于 Sable sub-level 内的 Create 动力件（飞轮 / 锯片 / 齿轮 / 大水轮 等）
 * 不进入 Create 的 Flywheel 可视化系统，改走 vanilla 的 {@code BlockEntityRenderer}。
 *
 * <p><b>根因</b>（javap 逐字节码确认，见下方各处注释）：
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
 * <p><b>为什么必须拦截 {@code getVisualizer} 的三个调用点</b>：
 *
 * <p>Create 的 {@code VisualizationHelper} 里 {@code getVisualizer(BlockEntity)}
 * 被<b>三个互相独立</b>的方法各自调用（javap 字节码确认）：
 *
 * <pre>
 * canVisualize(T)          : getVisualizer(x) != null  ->  返回 visualizer != null
 * skipVanillaRender(T)     : getVisualizer(x)          ->  null 则 return false，
 *                                                      否则 visualizer.skipVanillaRender(x)
 * tryAddBlockEntity(T)     : getVisualizer(x)          ->  null 则 return false，
 *                                                      否则入队并返回 visualizer.skipVanillaRender(x)
 * </pre>
 *
 * <p>三个入口各管一件事，<b>只拦一个不够</b>：
 *
 * <ul>
 *   <li>{@code canVisualize} —— 被 {@code BlockEntityStorage.willAccept} 调用，
 *       决定方块实体能否进入 Flywheel 队列；</li>
 *   <li>{@code skipVanillaRender} —— 被 Create 的
 *       {@code ClientLevelMixin.flywheel$decideNotToRenderEntity} 调用，
 *       决定是否 <b>cancel 掉 vanilla 渲染</b>；</li>
 *   <li>{@code tryAddBlockEntity} —— 被 Create 的
 *       {@code SectionCompilerMixin.flywheel$tryAddBlockEntity} 调用，
 *       返回 true 就把该方块实体<b>从 vanilla 的区块方块实体列表里移走</b>。</li>
 * </ul>
 *
 * <p>三个都拦下之后，sub-level 内的动力件：
 * 不进 Flywheel 队列、不被移出 vanilla 列表、不被取消 vanilla 渲染 ——
 * 于是回到 vanilla 的 {@code BlockEntityRenderer} 路径，
 * 再由 Sable 已有的 sub-level 方块实体位姿变换
 * （{@code sublevel_render/block_entity_render/LevelRendererMixin.sable$transformBlockEntity}）
 * 施加正确的 sub-level 变换 —— 两者正好配套。
 *
 * <p><b>主世界完全不受影响</b>：只有该方块实体确实位于某个 sub-level 内时才改变判定结果。
 */
@Pseudo
@Mixin(targets = "com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper")
public class VisualizationHelperMixin {
   /**
    * 拦截点 1：{@code canVisualize}。
    *
    * <p>字节码（javap -c 原文）：
    * <pre>
    * 0: aload_0
    * 1: invokestatic  #67  // Method getVisualizer:(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;
    * 4: ifnull        11
    * 7: iconst_1
    * 8: goto          12
    * 11: iconst_0
    * 12: ireturn
    * </pre>
    * 即 {@code canVisualize} 就是 {@code getVisualizer(x) != null}。
    */
   @WrapOperation(
      method = {"canVisualize"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;"
      )}
   )
   private static BlockEntityVisualizer<?> sable$skipSubLevelVisuals(
      BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original
   ) {
      if (Sable.HELPER.getContaining(blockEntity) != null) {
         return null;
      }

      return original.call(blockEntity);
   }

   /**
    * 拦截点 2：{@code skipVanillaRender}。
    *
    * <p>字节码（javap -c 原文）：
    * <pre>
    * 0: aload_0
    * 1: invokestatic  #67  // Method getVisualizer:(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;
    * 4: astore_1
    * 5: aload_1
    * 6: ifnonnull      11
    * 9: iconst_0
    * 10: ireturn
    * 11: aload_1
    * 12: aload_0
    * 13: invokeinterface #75, 2 // InterfaceMethod BlockEntityVisualizer.skipVanillaRender:(Lnet/minecraft/world/level/block/entity/BlockEntity;)Z
    * 18: ireturn
    * </pre>
    * <p>关键点：{@code visualizer != null} 时才会调用谓词 {@code visualizer.skipVanillaRender(be)}。
    * Create 为飞轮等动力件注册的谓词是<b>恒 true</b>的
    * （{@code AllBlockEntityRenders#visual} 的 {@code lambda$visual$0} 返回 {@code iconst_1}），
    * 所以只要 {@code getVisualizer} 还返回非 null，vanilla 渲染就会被 cancel。
    * 这里返回 null → {@code skipVanillaRender} 返回 false → vanilla 渲染<b>不被取消</b>。
    */
   @WrapOperation(
      method = {"skipVanillaRender"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;"
      )}
   )
   private static BlockEntityVisualizer<?> sable$skipSubLevelVanillaRender(
      BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original
   ) {
      if (Sable.HELPER.getContaining(blockEntity) != null) {
         return null;
      }

      return original.call(blockEntity);
   }

   /**
    * 拦截点 3：{@code tryAddBlockEntity}。
    *
    * <p>字节码（javap -c 原文）：
    * <pre>
    * 0: aload_0
    * 1: invokevirtual #32  // Method BlockEntity.getLevel:()Lnet/minecraft/world/level/Level;
    * 4: astore_1
    * 5: aload_1
    * 6: invokestatic  #13  // Method VisualizationManager.get:(...)L...VisualizationManager;
    * 9: astore_2
    * 10: aload_2
    * 11: ifnonnull      16
    * 14: iconst_0
    * 15: ireturn
    * 16: aload_0
    * 17: invokestatic  #67  // Method getVisualizer:(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;
    * 20: astore_3
    * 21: aload_3
    * 22: ifnonnull      27
    * 25: iconst_0
    * 26: ireturn
    * 27: aload_2
    * 28: invokeinterface #38, 1 // InterfaceMethod VisualizationManager.blockEntities:()L...VisualManager;
    * 33: aload_0
    * 34: invokeinterface #23, 2 // InterfaceMethod VisualManager.queueAdd:(Ljava/lang/Object;)V
    * 39: aload_3
    * 40: aload_0
    * 41: invokeinterface #75, 2 // InterfaceMethod BlockEntityVisualizer.skipVanillaRender:(...)Z
    * 46: ireturn
    * </pre>
    * <p>关键点：返回 true 会让 Create 的 {@code SectionCompilerMixin}
    * （注入 {@code SectionCompiler.handleBlockEntity}，HEAD，cancellable）cancel，
    * 把该方块实体<b>从 vanilla 的区块方块实体列表里移走</b>。
    * 这里让 {@code getVisualizer} 返回 null → {@code tryAddBlockEntity} 返回 false
    * → <b>不移出</b> vanilla 列表，动力件才会被 vanilla 的
    * {@code LevelRenderer.extractVisibleBlockEntities} 收集到。
    */
   @WrapOperation(
      method = {"tryAddBlockEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;"
      )}
   )
   private static BlockEntityVisualizer<?> sable$skipSubLevelQueue(
      BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original
   ) {
      if (Sable.HELPER.getContaining(blockEntity) != null) {
         return null;
      }

      return original.call(blockEntity);
   }
}
