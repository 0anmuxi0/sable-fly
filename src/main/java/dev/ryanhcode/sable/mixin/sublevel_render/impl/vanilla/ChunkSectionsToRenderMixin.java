package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import dev.ryanhcode.sable.sublevel.render.SubLevelBatchedTerrainRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.oit.OitRenderPassProvider.Parameters;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChunkSectionsToRender.class})
public abstract class ChunkSectionsToRenderMixin {
   @Inject(
      method = {"renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/renderpearl/api/commands/RenderPass;Lcom/mojang/renderpearl/api/textures/GpuSampler;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Z)V"},
      at = {@At("TAIL")}
   )
   private void sable$renderSubLevelTerrain(
      ChunkSectionLayerGroup layerGroup, RenderPass renderPass, GpuSampler sampler, GpuTextureView atlasView, boolean wireframe, CallbackInfo ci
   ) {
      SubLevelBatchedTerrainRenderer.renderGroup(layerGroup, renderPass, sampler, atlasView, wireframe);
   }

   @Inject(
      method = {"renderOit(Lcom/mojang/renderpearl/api/textures/GpuSampler;Lnet/minecraft/client/renderer/oit/OitStage;Lnet/minecraft/client/renderer/oit/OitRenderPassProvider$Parameters;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"},
      at = {@At("TAIL")}
   )
   private void sable$renderSubLevelTerrainOit(
      GpuSampler sampler, OitStage stage, Parameters parameters, GpuTextureView colorView, GpuTextureView depthView, CallbackInfo ci
   ) {
      SubLevelBatchedTerrainRenderer.renderOit(sampler, stage, parameters, colorView, depthView);
   }
}
