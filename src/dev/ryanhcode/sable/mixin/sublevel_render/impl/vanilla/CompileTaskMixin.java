package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.SubLevelSectionSortHelper;
import java.lang.reflect.Field;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets={"net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask"})
public abstract class CompileTaskMixin {
    @WrapOperation(method={"doTask(Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask$SectionTaskResult;"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;of(Lnet/minecraft/world/phys/Vec3;J)Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;")})
    private TranslucencyPointOfView sable$plotSpacePointOfView(Vec3 cameraPos, long sectionNode, Operation<TranslucencyPointOfView> original) {
        return (TranslucencyPointOfView)original.call(new Object[]{SubLevelSectionSortHelper.plotCamera(SectionPos.of((long)sectionNode), cameraPos), sectionNode});
    }

    /**
     * 26.2: LevelRenderer constructs its SectionRenderDispatcher with a null SectionCompiler while
     * model data is still loading, and injects it afterwards via setCompiler. A sub-level bounds
     * packet can trigger synchronous compilation before that, NPEing here on
     * sectionCompiler.compile(...). Cancel this task when the compiler is not ready yet - the
     * section stays dirty and is re-queued on a later frame.
     */
    @Inject(method={"doTask(Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask$SectionTaskResult;"}, at={@At("HEAD")}, cancellable=true)
    private void sable$skipWhenCompilerMissing(net.minecraft.client.renderer.SectionBufferBuilderPack pack, CallbackInfoReturnable<Object> cir) {
        try {
            Field f = SectionRenderDispatcher.class.getDeclaredField("sectionCompiler");
            f.setAccessible(true);
            if (f.get(sable$dispatcherCompiler()) == null) {
                cir.setReturnValue(net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult.CANCELLED);
            }
        } catch (ReflectiveOperationException ignored) {
            // reflective probe failed - assume ready and let vanilla behave
        }
    }

    @Unique
    private Object sable$dispatcherCompiler() throws ReflectiveOperationException {
        // this(CtrlTask) -> this$1 (RenderSection) -> this$0 (SectionRenderDispatcher)
        Object renderSection = getClass().getDeclaredField("this$1").get(this);
        return renderSection.getClass().getSuperclass() == null ? null
            : renderSection.getClass().getDeclaredField("this$0").get(renderSection);
    }
}