package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla.SubLevelSectionSortHelper;
import java.lang.reflect.Field;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection.SectionTask.SectionTaskResult;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   targets = {"net.minecraft.client.renderer.chunk.SectionRenderDispatcher$RenderSection$CompileTask"}
)
public abstract class CompileTaskMixin {
   @WrapOperation(
      method = {"doTask(Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask$SectionTaskResult;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;of(Lnet/minecraft/world/phys/Vec3;J)Lnet/minecraft/client/renderer/chunk/TranslucencyPointOfView;"
      )}
   )
   private TranslucencyPointOfView sable$plotSpacePointOfView(Vec3 var1, long var2, Operation<TranslucencyPointOfView> var4) {
      return (TranslucencyPointOfView)var4.call(SubLevelSectionSortHelper.plotCamera(SectionPos.of(var2), var1), var2);
   }

   @Inject(
      method = {"doTask(Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionRenderDispatcher$RenderSection$SectionTask$SectionTaskResult;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$skipWhenCompilerMissing(SectionBufferBuilderPack var1, CallbackInfoReturnable<Object> var2) {
      try {
         Field var3 = SectionRenderDispatcher.class.getDeclaredField("sectionCompiler");
         var3.setAccessible(true);
         if (var3.get(this.sable$dispatcherCompiler()) == null) {
            var2.setReturnValue(SectionTaskResult.CANCELLED);
         }
      } catch (ReflectiveOperationException var4) {
      }
   }

   @Unique
   private Object sable$dispatcherCompiler() throws ReflectiveOperationException {
      Object var1 = this.getClass().getDeclaredField("this$1").get(this);
      return var1.getClass().getSuperclass() == null ? null : var1.getClass().getDeclaredField("this$0").get(var1);
   }
}
