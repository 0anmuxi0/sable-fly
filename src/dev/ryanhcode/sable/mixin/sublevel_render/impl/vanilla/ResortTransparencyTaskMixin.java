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
 * Restored verbatim from the 26.3 original: on 26.2 the target class
 * {@code SectionRenderDispatcher$RenderSection$ResortTransparencyTask}, its
 * {@code doTask(SectionBufferBuilderPack)} method and the wrapped
 * {@code TranslucencyPointOfView.of(Vec3, long)} call all exist (verified against the 26.2 merged jar),
 * and the dependency {@link SubLevelSectionSortHelper} is byte-identical to the 26.3 original.
 * Without this mixin the translucent resort point for sub-level sections falls back to the global
 * camera position, which mis-sorts translucent geometry inside sub-levels.
 */
@Mixin(targets = {"net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$ResortTransparencyTask"})
public abstract class ResortTransparencyTaskMixin {
    @WrapOperation(
        method = {"doTask(Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask$SectionTaskResult;"},
        at = {@At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;of(Lnet/minecraft/world/phys/Vec3;J)Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;"
        )}
    )
    private TranslucencyPointOfView sable$plotSpacePointOfView(Vec3 cameraPos, long sectionNode, Operation<TranslucencyPointOfView> original) {
        return original.call(SubLevelSectionSortHelper.plotCamera(SectionPos.of(sectionNode), cameraPos), sectionNode);
    }
}
