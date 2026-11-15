package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.mojang.blaze3d.textures.GpuSampler;
import dev.ryanhcode.sable.sublevel.render.SubLevelBatchedTerrainRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChunkSectionsToRender.class})
public abstract class ChunkSectionsToRenderMixin {
   @Inject(
      method = {"renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V"},
      at = {@At("TAIL")}
   )
   private void sable$renderSubLevelTerrain(ChunkSectionLayerGroup var1, GpuSampler var2, CallbackInfo var3) {
      SubLevelBatchedTerrainRenderer.renderGroup(var1, var2);
   }
}
