package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.SubLevelSectionSortHelper;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(
   targets = {"net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask"}
)
public abstract class CompileTaskMixin {
   @WrapOperation(require = 0, method = {"doTask(Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask$SectionTaskResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;of(Lnet/minecraft/world/phys/Vec3;J)Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;"
      )}
   )
   private TranslucencyPointOfView sable$plotSpacePointOfView(Vec3 cameraPos, long sectionNode, Operation<TranslucencyPointOfView> original) {
      return original.call(SubLevelSectionSortHelper.plotCamera(SectionPos.of(sectionNode), cameraPos), sectionNode);
   }
}
