package dev.ryanhcode.sable.mixin.compatibility.create;

import dev.ryanhcode.sable.compatibility.create.CreateSupportsVisualizationContext;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让位于 Sable sub-level 内的 Create 动力件，其 <b>vanilla 回退渲染器</b>
 * 也能真正填出模型。
 *
 * <p><b>为什么 {@link VisualizationHelperMixin} 不够</b>
 *
 * <p>{@code VisualizationHelperMixin} 拦的是 {@code getVisualizer} 的三个调用点，
 * 作用是让动力件<b>不被 Flywheel 接管</b>（不入队 / 不被移出 vanilla 列表 / 不取消 vanilla 渲染）。
 * 但 Create 里有一部分 renderer 在自己的 {@code extractRenderState} 内部
 * <b>另外</b>做了一次 {@code supportsVisualization} 判定，命中就直接提前 return、
 * <b>不填 vanilla 渲染状态</b> —— 于是动力件虽然回到了 vanilla 管线，却依然是个"空模型"。
 *
 * <p>字节码实证（{@code SawRenderer.extractRenderState}，javap -c 原文）：
 * <pre>
 * 101: aload         6                                 // Level
 * 103: invokestatic  #129  // InterfaceMethod com/zurrtum/create/client/flywheel/api/visualization/VisualizationManager.supportsVisualization:(Lnet/minecraft/world/level/LevelAccessor;)Z
 * 106: ifeq          110
 * 109: return                                            // &lt;-- 直接返回，不填模型！
 * 110: ...
 * </pre>
 *
 * <p>{@code KineticBlockEntityRenderer.extractRenderState} 同理：
 * <pre>
 *  6: aload         2                                 // renderState
 *  7: aload         6                                 // Level
 *  9: invokestatic  #18   // InterfaceMethod ...VisualizationManager.supportsVisualization:(...)Z
 * 12: putfield      #24   // KineticRenderState.support
 * 15: aload         2
 * 16: getfield      #24   // KineticRenderState.support
 * 19: ifeq          23
 * 22: return
 * 23: ...
 * </pre>
 *
 * <p>一共 19 个 Create renderer 在 {@code extractRenderState} 里做了这个判定
 * （锯片、齿轮、传送带、链式输送机、漏斗、包装器、投掷器、Schematic 等）。
 *
 * <p><b>注入点为什么是 {@code FlwApiLinkImpl} 而不是 {@code VisualizationManager}</b>
 *
 * <p>{@code VisualizationManager} 是<b>接口</b>。Mixin 对接口目标只支持
 * {@code ACCESSOR} / {@code INTERFACE} 两种 variant，
 * 标准 mixin 会被拒绝并抛出
 * {@code @Mixin target type mismatch: ... is an interface}。
 * 所以改为注入它真正的实现类 {@code FlwApiLinkImpl}（普通类）。
 *
 * <p>{@code FlwApiLinkImpl.supportsVisualization(LevelAccessor)} 的字节码：
 * <pre>
 * public boolean supportsVisualization(LevelAccessor);
 *   0: aload_1
 *   1: invokestatic  #30  // Method VisualizationManagerImpl.supportsVisualization:(Lnet/minecraft/world/level/LevelAccessor;)Z
 *   4: ireturn
 * </pre>
 * 它是 {@code VisualizationManager.supportsVisualization} 的唯一转发点，
 * 因此在这里拦截等价于拦截接口静态方法本身。
 *
 * <p><b>难点与解决办法</b>：{@code supportsVisualization(LevelAccessor)} 的参数只有 level，
 * <b>没有方块实体上下文</b>，而 sub-level 的方块实体的 {@code getLevel()} 就是父世界 level，
 * 因此单看 level 无法区分"主世界"和"sub-level 内"。
 *
 * <p>解决办法：在 vanilla 的 {@code BlockEntityRenderDispatcher.tryExtractRenderState}
 * 里，用 {@link CreateSupportsVisualizationContext} 记录"当前正在抽取渲染状态的方块实体"。
 * 因为该方法在调用 {@code renderer.extractRenderState(...)} <b>之前</b>就持有 BE 上下文，
 * 所以在它执行期间，{@code supportsVisualization} 就能知道自己面对的是哪个 BE。
 *
 * <p><b>主世界完全不受影响</b>：只有在"当前 BE 确实位于某个 sub-level 内"时才返回 false。
 */
@Pseudo
@Mixin(targets = "com.zurrtum.create.client.flywheel.impl.FlwApiLinkImpl")
public class FlwApiLinkImplMixin {
   /**
    * 拦截 {@code supportsVisualization(LevelAccessor)}。
    *
    * <p>字节码（javap -c 原文）：
    * <pre>
    * public boolean supportsVisualization(LevelAccessor);
    *   0: aload_1
    *   1: invokestatic  #30  // Method VisualizationManagerImpl.supportsVisualization:(Lnet/minecraft/world/level/LevelAccessor;)Z
    *   4: ireturn
    * </pre>
    *
    * <p>这里在"当前正在抽取渲染状态的 BE 位于 sub-level 内"时返回 false，
    * 让 Create 的 renderer 继续往下走、把 vanilla 渲染状态填完整。
    */
   @WrapOperation(
      method = {"supportsVisualization"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/zurrtum/create/client/flywheel/impl/visualization/VisualizationManagerImpl;supportsVisualization(Lnet/minecraft/world/level/LevelAccessor;)Z"
      )}
   )
   private boolean sable$disableForSubLevel(LevelAccessor level, Operation<Boolean> original) {
      if (CreateSupportsVisualizationContext.isCurrentBlockEntityInSubLevel()
         || CreateSupportsVisualizationContext.isCurrentContraptionInSubLevel()) {
         return false;
      }

      return original.call(level);
   }
}
