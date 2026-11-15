package dev.ryanhcode.sable.compatibility.create;

import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 记录"当前正在提取渲染状态的方块实体"，供 Create 兼容层判断该方块实体
 * 是否位于 Sable sub-level 内。
 *
 * <p><b>为什么需要它</b>
 *
 * <p>Create 的 {@code VisualizationManager.supportsVisualization(LevelAccessor)} 只接收
 * {@code level}，<b>没有方块实体上下文</b>。而 Sable 的 sub-level 方块实体的
 * {@code getLevel()} 返回的是<b>父世界</b>（plot 区块是用父世界的 {@code LevelChunk}
 * 构造的），所以只看 level 无法区分"主世界"和"sub-level"。
 *
 * <p>但 Create 有 19 个 renderer（锯片、齿轮、传送带、链式输送机、漏斗、包装器…）
 * 在自己的 {@code extractRenderState} 内部会调用 {@code supportsVisualization}，
 * 命中就<b>提前 return、不填 vanilla 渲染状态</b>。必须让这次判定知道"当前对应的
 * 是哪个方块实体"。
 *
 * <p><b>怎么记录</b>
 *
 * <p>vanilla 的 {@code BlockEntityRenderDispatcher.tryExtractRenderState} 在调用
 * {@code renderer.extractRenderState(...)} 之前就持有该方块实体，因此在它执行期间
 * 设置线程局部量即可。线程局部量只在 {@code tryExtractRenderState} 期间设置，
 * 因此不会跨线程泄漏，也不会影响主世界。
 */
public final class CreateSupportsVisualizationContext {
   private static final ThreadLocal<BlockEntity> CURRENT_BLOCK_ENTITY = new ThreadLocal<>();

   private CreateSupportsVisualizationContext() {
   }

   /** 设置"当前正在提取渲染状态的方块实体"。 */
   public static void setCurrentBlockEntity(BlockEntity blockEntity) {
      CURRENT_BLOCK_ENTITY.set(blockEntity);
   }

   /** 清除"当前正在提取渲染状态的方块实体"。 */
   public static void clearCurrentBlockEntity() {
      CURRENT_BLOCK_ENTITY.remove();
   }

   /**
    * 当前正在提取渲染状态的方块实体是否位于某个 sub-level 内。
    *
    * <p>主世界的方块实体返回 false，主世界必须保持 Create 的默认行为不变。
    */
   public static boolean isCurrentBlockEntityInSubLevel() {
      BlockEntity blockEntity = CURRENT_BLOCK_ENTITY.get();
      return blockEntity != null && Sable.HELPER.getContaining(blockEntity) != null;
   }
}
