package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.flywheel.api.visual.BlockEntityVisual;
import com.zurrtum.create.client.flywheel.api.visualization.BlockEntityVisualizer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.BlockEntityStorage;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={BlockEntityStorage.class}, remap=false)
public abstract class FlywheelCompatBlockEntityStorageMixin {
    @WrapOperation(method={"createRaw(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Lcom/zurrtum/create/client/flywheel/api/visual/BlockEntityVisual;"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;createVisual(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Lcom/zurrtum/create/client/flywheel/api/visual/BlockEntityVisual;", remap=false)})
    private BlockEntityVisual<?> sable$useEmbeddingForSubLevel(BlockEntityVisualizer instance, VisualizationContext visualizationContext, BlockEntity blockEntity, float partialTick, Operation<BlockEntityVisual<?>> original) {
        if (!FlywheelCompat.ENABLED) {
            return (BlockEntityVisual)original.call(instance, visualizationContext, blockEntity, Float.valueOf(partialTick));
        }
        ClientSubLevel subLevel = FlywheelCompat.getContainingSubLevel(blockEntity);
        if (subLevel == null) {
            return (BlockEntityVisual)original.call(instance, visualizationContext, blockEntity, Float.valueOf(partialTick));
        }
        FlywheelCompat.FlywheelEmbeddingState state = FlywheelCompat.getOrCreateState(visualizationContext, subLevel);
        BlockEntityVisual visual = (BlockEntityVisual)original.call(instance, state.visualEmbedding(), blockEntity, Float.valueOf(partialTick));
        if (visual != null) {
            FlywheelCompat.onVisualCreated(blockEntity, state);
        }
        return visual;
    }
}

