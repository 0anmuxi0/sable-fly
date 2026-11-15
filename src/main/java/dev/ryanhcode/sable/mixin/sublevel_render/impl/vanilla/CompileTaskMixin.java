package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.SubLevelSectionSortHelper;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 26.1.2 兼容：26.2 的 CompileTask.doTask 为具体方法，可直接注入；
 * 26.1.2 的 CompileTask.doTask 是抽象方法，实际编译任务在 RebuildTask.doTask。
 * 功能等价：在编译任务创建 CompiledSectionMesh 前，把排序点投影到 sub-level 的 plot 空间。
 */
@Mixin(
   targets = {"net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$RebuildTask"}
)
public abstract class CompileTaskMixin {
   @WrapOperation(
      method = {"doTask(Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$CompileTask$SectionTaskResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;of(Lnet/minecraft/world/phys/Vec3;J)Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;"
      )}
   )
   private TranslucencyPointOfView sable$plotSpacePointOfView(Vec3 var1, long var2, Operation<TranslucencyPointOfView> var4) {
      return (TranslucencyPointOfView)var4.call(SubLevelSectionSortHelper.plotCamera(SectionPos.of(var2), var1), var2);
   }
}
