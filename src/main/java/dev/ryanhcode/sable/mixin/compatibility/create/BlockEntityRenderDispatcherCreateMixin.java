package dev.ryanhcode.sable.mixin.compatibility.create;

import dev.ryanhcode.sable.compatibility.create.CreateSupportsVisualizationContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={BlockEntityRenderDispatcher.class})
public class BlockEntityRenderDispatcherCreateMixin {
    @Inject(method={"tryExtractRenderState"}, at={@At(value="HEAD")})
    private void sable$setCreateContext(BlockEntity blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, boolean bl, CallbackInfoReturnable<BlockEntityRenderState> cir) {
        CreateSupportsVisualizationContext.setCurrentBlockEntity(blockEntity);
    }

    @Inject(method={"tryExtractRenderState"}, at={@At(value="RETURN")})
    private void sable$clearCreateContext(BlockEntity blockEntity, float partialTick, ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, boolean bl, CallbackInfoReturnable<BlockEntityRenderState> cir) {
        CreateSupportsVisualizationContext.clearCurrentBlockEntity();
    }
}