package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.flywheel.api.visualization.BlockEntityVisualizer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelWhitelist;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 子关卡 Create 方块实体的 Flywheel <b>白名单分流</b>（26.1.2，阶段一方案的一部分）。
 *
 * <p>升级前（降级方案）：三处拦截点对子关卡 BE 一律让 {@code getVisualizer} 返回 null，
 * 把所有 Create 动力件踢回 vanilla {@code BlockEntityRenderer}（用户实测可见，但失去
 * Flywheel 的 GPU 实例化批量渲染与 OIT 透明排序）。</p>
 *
 * <p>升级后（本版本）：与 Flywheel embedding 方案
 * （{@code FlywheelCompatBlockEntityStorageMixin} / {@code FlywheelCompatRenderDispatcherMixin} /
 * {@code FlywheelCompatAbstractBlockEntityVisualMixin}）配合，按 {@link FlywheelWhitelist} 分流：</p>
 *
 * <ul>
 *   <li><b>白名单内</b>（核心动力件：齿轮箱/轴/泵/钻头等 34 类）——放行进入 Flywheel，
 *       由 storage mixin 换成子关卡 embedding context 渲染（正确位姿 + GPU 批量渲染）；</li>
 *   <li><b>白名单外</b>（传送带、普通齿轮、机械手、锯片等）——维持降级行为，走
 *       vanilla BER + Sable 的子关卡方块实体变换，与 FlywheelCompat 之前的行为一致。</li>
 * </ul>
 *
 * <p>为什么三处拦截点仍需分别处理（javap 逐字节码确认）：</p>
 *
 * <ol>
 *   <li>{@code canVisualize} —— 被 {@code BlockEntityStorage.willAccept} 调用，决定
 *       方块实体能否进入 Flywheel 队列。白名单外必须拦成 null，否则经任何注册路径
 *       （例如 {@code VisualizationManager} 对 plot 区块的处理）进来的白名单外 BE 会用
 *       父世界 context 创建视觉，落在 ±20.48M 的 plot 虚拟坐标，不可见。</li>
 *   <li>{@code skipVanillaRender} —— 被 Create 的 {@code ClientLevelMixin.
 *       flywheel$decideNotToRenderEntity} 调用（{@code supportsVisualization(be.level) &&
 *       skipVanillaRender(be)} 时 cancel vanilla 渲染）。Create 动力件的 skipVanillaRender
 *       谓词恒 true：白名单外若不拦，vanilla 渲染被取消而 Flywheel 又不接管 → 方块直接消失；
 *       白名单内必须放行（让 vanilla 取消），否则 Flywheel embedding 与 vanilla 双重渲染。</li>
 *   <li>{@code tryAddBlockEntity} —— 被 Create 的 {@code SectionCompilerMixin} 调用，
 *       返回 true 会把该 BE <b>移出 vanilla 区块编译列表</b>并用父世界 context 注册。
 *       子关卡 BE 一律拦成 false：白名单 BE 由 {@code FlywheelCompat.syncSubLevelBlockEntities}
 *       用 embedding context 注册（正确位姿），不能走这条会产生错误坐标的路径。</li>
 * </ol>
 *
 * <p>回退开关：{@code -Dsable.flywheelCompat=false} 时全部恢复降级行为（一律返回 null）。
 * 主世界完全不受影响：只有该方块实体确实位于某个 sub-level 内时才改变判定结果。</p>
 */
@Pseudo
@Mixin(targets = "com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper")
public class VisualizationHelperMixin {
   /**
    * 拦截点 1：{@code canVisualize}（{@code getVisualizer(x) != null}）。
    * 白名单内放行（可进 Flywheel），白名单外拦 null（不进 Flywheel，vanilla 渲染）。
    */
   @WrapOperation(
      method = {"canVisualize"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;"
      )}
   )
   private static BlockEntityVisualizer<?> sable$gateCanVisualize(
      BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original
   ) {
      if (Sable.HELPER.getContaining(blockEntity) == null) {
         return original.call(blockEntity);
      }

      // 子关卡 BE：适配器关闭或非白名单类型 → 不进 Flywheel，走 vanilla BER。
      if (!FlywheelCompat.ENABLED || !FlywheelWhitelist.contains(blockEntity.getType())) {
         return null;
      }

      return original.call(blockEntity);
   }

   /**
    * 拦截点 2：{@code skipVanillaRender}（决定是否 cancel vanilla 渲染）。
    * 白名单内放行（vanilla 取消渲染，交给 Flywheel embedding，避免双渲染），
    * 白名单外拦 null（vanilla 渲染保留，否则方块消失）。
    */
   @WrapOperation(
      method = {"skipVanillaRender"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;"
      )}
   )
   private static BlockEntityVisualizer<?> sable$gateSkipVanillaRender(
      BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original
   ) {
      if (Sable.HELPER.getContaining(blockEntity) == null) {
         return original.call(blockEntity);
      }

      if (!FlywheelCompat.ENABLED || !FlywheelWhitelist.contains(blockEntity.getType())) {
         return null;
      }

      return original.call(blockEntity);
   }

   /**
    * 拦截点 3：{@code tryAddBlockEntity}（区块编译路径，返回 true 会把 BE 移出
    * vanilla 列表并用父世界 context 注册）。
    * 子关卡 BE 一律拦 null：白名单 BE 由 {@code FlywheelCompat.syncSubLevelBlockEntities}
    * 用 embedding context 注册，不走这条会产生错误坐标（±20.48M）的路径。
    */
   @WrapOperation(
      method = {"tryAddBlockEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;"
      )}
   )
   private static BlockEntityVisualizer<?> sable$gateTryAddBlockEntity(
      BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original
   ) {
      if (Sable.HELPER.getContaining(blockEntity) == null) {
         return original.call(blockEntity);
      }

      // 子关卡 BE：一律不让 Create 的区块编译路径注册（无论白名单与否）。
      return null;
   }
}
