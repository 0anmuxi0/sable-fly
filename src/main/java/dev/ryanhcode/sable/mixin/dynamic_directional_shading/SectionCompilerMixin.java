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
            Field var0 = Class.forName("net.minecraft.client.renderer.block.BlockModelLighter").getDeclaredField("CACHE");
            var0.setAccessible(true);
            sable$cacheField = (ThreadLocal<?>)var0.get(null);
         }

         return sable$cacheField.get();
      } catch (ReflectiveOperationException var1) {
         return null;
      }
   }

   @Inject(
      method = {"compile"},
      at = {@At("HEAD")}
   )
   private void sable$preCompile(
      SectionPos var1, RenderSectionRegion var2, VertexSorting var3, SectionBufferBuilderPack var4, CallbackInfoReturnable<Results> var5
   ) {
      ClientLevel var6 = Minecraft.getInstance().level;
      ClientSubLevelContainer var7 = SubLevelContainer.getContainer(var6);
      if (var7 != null) {
         LevelPlot var8 = var7.getPlot(new ChunkPos(var1.getX() >> 4, var1.getZ() >> 4));
         if (sable$cacheValue() instanceof ModelBlockRendererCacheExtension var10) {
            var10.sable$setOnSubLevel(var8 != null);
         }
      }
   }

   @Inject(
      method = {"compile"},
      at = {@At("TAIL")}
   )
   private void sable$postCompile(
      SectionPos var1, RenderSectionRegion var2, VertexSorting var3, SectionBufferBuilderPack var4, CallbackInfoReturnable<Results> var5
   ) {
      if (sable$cacheValue() instanceof ModelBlockRendererCacheExtension var7) {
         var7.sable$setOnSubLevel(false);
      }
   }
}
