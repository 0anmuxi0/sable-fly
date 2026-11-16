package dev.ryanhcode.sable.compatibility.create;

import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
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

   /**
    * 记录"当前正在提取渲染状态的 Contraption 实体"。
    *
    * <p>Create 的 {@code ContraptionEntityRenderer.extractRenderState} 会调用
    * {@code VisualizationManager.supportsVisualization(entity.level())} 决定 Contraption
    * 方块层走 Flywheel（ContraptionVisual，父世界坐标系）还是 vanilla SuperByteBuffer
    * （正确以实体位置渲染）。子关卡内的 Contraption 实体位置在 plot 虚拟坐标（±20.48M），
    * Flywheel 路径会把方块画在 20M 外 → 必须让该判定在"当前 Contraption 位于子关卡内"
    * 时返回 false，改走 vanilla 路径（与 Sable 1.21.1 官方行为一致）。</p>
    */
   private static final ThreadLocal<AbstractContraptionEntity> CURRENT_CONTRAPTION = new ThreadLocal<>();

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

   /** 设置"当前正在提取渲染状态的 Contraption 实体"。 */
   public static void setCurrentContraption(AbstractContraptionEntity contraption) {
      CURRENT_CONTRAPTION.set(contraption);
   }

   /** 清除"当前正在提取渲染状态的 Contraption 实体"。 */
   public static void clearCurrentContraption() {
      CURRENT_CONTRAPTION.remove();
   }

   /** 当前正在提取渲染状态的 Contraption 实体是否位于某个 sub-level 内。 */
   /**
    * 当前正在提取渲染状态的 Contraption 实体是否位于某个 sub-level 内。
    *
    * <p>注意：客户端的 {@code ClientPacketListenerMixin.sable$lerp} 会把子关卡内实体的
    * 位置同步变换到<b>主世界坐标</b>（非 LivingEntity 分支：{@code transformPosition}），
    * 因此 {@code getContaining(entity)} 可能返回 null。这里依次检查：
    * 当前位置所属子关卡 → tracking 子关卡 → 上次 tracking 子关卡。</p>
    */
   public static boolean isCurrentContraptionInSubLevel() {
      AbstractContraptionEntity contraption = CURRENT_CONTRAPTION.get();
      if (contraption == null) {
         return false;
      }
      if (Sable.HELPER.getContaining(contraption) != null) {
         return true;
      }
      if (Sable.HELPER.getTrackingSubLevel(contraption) != null) {
         return true;
      }
      return Sable.HELPER.getLastTrackingSubLevel(contraption) != null;
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
