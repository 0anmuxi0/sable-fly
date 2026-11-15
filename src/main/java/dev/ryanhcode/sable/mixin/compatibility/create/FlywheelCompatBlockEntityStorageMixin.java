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

/**
 * Intercepts {@code BlockEntityVisualizer.createVisual} inside Flywheel's
 * {@code BlockEntityStorage.createRaw}: when the BE belongs to a sublevel, the
 * visualization context is swapped for that sublevel's embedding (independent
 * renderOrigin).
 *
 * <p>26.1.2 create-fly signature (verified by javap):
 * {@code protected BlockEntityVisual<?> createRaw(VisualizationContext, BlockEntity, float)}
 * which calls
 * {@code visualizer.createVisual(VisualizationContext, BlockEntity, float)}.</p>
 */
@Mixin(value = BlockEntityStorage.class, remap = false)
public abstract class FlywheelCompatBlockEntityStorageMixin {

    @WrapOperation(
            method = "createRaw(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Lcom/zurrtum/create/client/flywheel/api/visual/BlockEntityVisual;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;createVisual(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/level/block/entity/BlockEntity;F)Lcom/zurrtum/create/client/flywheel/api/visual/BlockEntityVisual;",
                    remap = false))
    private BlockEntityVisual<?> sable$useEmbeddingForSubLevel(
            BlockEntityVisualizer instance,
            VisualizationContext visualizationContext,
            BlockEntity blockEntity,
            float partialTick,
            Operation<BlockEntityVisual<?>> original) {
        if (!FlywheelCompat.ENABLED) {
            return original.call(instance, visualizationContext, blockEntity, partialTick);
        }
        ClientSubLevel subLevel = FlywheelCompat.getContainingSubLevel(blockEntity);
        if (subLevel == null) {
            return original.call(instance, visualizationContext, blockEntity, partialTick);
        }
        FlywheelCompat.FlywheelEmbeddingState state =
                FlywheelCompat.getOrCreateState(visualizationContext, subLevel);
        BlockEntityVisual<?> visual =
                original.call(instance, state.visualEmbedding(), blockEntity, partialTick);
        if (visual != null) {
            FlywheelCompat.onVisualCreated(blockEntity, state);
        }
        return visual;
    }
}
