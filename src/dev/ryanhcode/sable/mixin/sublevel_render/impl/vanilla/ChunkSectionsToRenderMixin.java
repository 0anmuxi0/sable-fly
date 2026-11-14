package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.mojang.blaze3d.textures.GpuSampler;
import dev.ryanhcode.sable.sublevel.render.SubLevelBatchedTerrainRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 adaptation of 26.3's ChunkSectionsToRenderMixin.
 *
 * 26.3 injected {@code renderGroup(ChunkSectionLayerGroup, RenderPass, GpuSampler, GpuTextureView, boolean)}
 * (renderpearl) plus a separate {@code renderOit} hook. On 26.2 the vanilla method is
 * {@code renderGroup(ChunkSectionLayerGroup, GpuSampler)} and there is no OIT stage, so a single TAIL
 * injection re-draws the batched sub-level groups through the regular terrain pipeline. Re-entrancy is
 * guarded inside {@link SubLevelBatchedTerrainRenderer#renderGroup} because our groups are
 * {@code ChunkSectionsToRender} instances themselves.
 */
@Mixin(value = {ChunkSectionsToRender.class})
public abstract class ChunkSectionsToRenderMixin {
    @Inject(
        method = {"renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V"},
        at = {@At("TAIL")}
    )
    private void sable$renderSubLevelTerrain(ChunkSectionLayerGroup layerGroup, GpuSampler sampler, CallbackInfo ci) {
        SubLevelBatchedTerrainRenderer.renderGroup(layerGroup, sampler);
    }
}