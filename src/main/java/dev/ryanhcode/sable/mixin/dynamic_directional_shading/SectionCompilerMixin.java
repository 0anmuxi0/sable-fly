package dev.ryanhcode.sable.mixin.dynamic_directional_shading;

import com.mojang.blaze3d.vertex.VertexSorting;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import java.lang.reflect.Field;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.SectionCompiler.Results;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({SectionCompiler.class})
public class SectionCompilerMixin {
   @Unique
   private static ThreadLocal<?> sable$cacheField;

   @Unique
   private static Object sable$cacheValue() {
      try {
         if (sable$cacheField == null) {
            Field field = Class.forName("net.minecraft.client.renderer.block.BlockModelLighter").getDeclaredField("CACHE");
            field.setAccessible(true);
            sable$cacheField = (ThreadLocal<?>)field.get(null);
         }

         return sable$cacheField.get();
      } catch (ReflectiveOperationException e) {
         return null;
      }
   }

   @Inject(
      method = {"compile"},
      at = {@At("HEAD")}
   )
   private void sable$preCompile(
      SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack builderPack, CallbackInfoReturnable<Results> cir
   ) {
      ClientLevel level = Minecraft.getInstance().level;
      ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container != null) {
         LevelPlot plot = container.getPlot(new ChunkPos(sectionPos.getX() >> 4, sectionPos.getZ() >> 4));
         if (sable$cacheValue() instanceof ModelBlockRendererCacheExtension cacheExtension) {
            cacheExtension.sable$setOnSubLevel(plot != null);
         }
      }
   }

   @Inject(
      method = {"compile"},
      at = {@At("TAIL")}
   )
   private void sable$postCompile(
      SectionPos sectionPos, RenderSectionRegion region, VertexSorting vertexSorting, SectionBufferBuilderPack builderPack, CallbackInfoReturnable<Results> cir
   ) {
      if (sable$cacheValue() instanceof ModelBlockRendererCacheExtension cacheExtension) {
         cacheExtension.sable$setOnSubLevel(false);
      }
   }
}
