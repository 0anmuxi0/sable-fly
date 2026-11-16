package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.flywheel.api.visual.EntityVisual;
import com.zurrtum.create.client.flywheel.api.visualization.EntityVisualizer;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.EntityStorage;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Intercepts {@code EntityVisualizer.createVisual} inside Flywheel's
 * {@code EntityStorage.createRaw}: when the entity belongs to a sublevel
 * (e.g. a Create Contraption entity whose anchor is at the plot-virtual
 * coordinate, ~+/-20.48M), the visualization context is swapped for that
 * sublevel's embedding (independent renderOrigin), exactly like the
 * block-entity adapter.
 *
 * <p>Without this, Create's {@code ContraptionVisual} (an entity visual)
 * computes instance positions as plotPos - parentRenderOrigin and the
 * sublevel render pose is never applied, so assembled contraptions render
 * ~20M off-screen.</p>
 *
 * <p>26.1.2 create-fly signature (verified by javap):
 * {@code protected EntityVisual<?> createRaw(VisualizationContext, Entity, float)}
 * which calls {@code visualizer.createVisual(VisualizationContext, Entity, float)}.</p>
 */
@Mixin(value = EntityStorage.class, remap = false)
public abstract class FlywheelCompatEntityStorageMixin {

    @WrapOperation(
            method = "createRaw(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/entity/Entity;F)Lcom/zurrtum/create/client/flywheel/api/visual/EntityVisual;",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/zurrtum/create/client/flywheel/api/visualization/EntityVisualizer;createVisual(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/entity/Entity;F)Lcom/zurrtum/create/client/flywheel/api/visual/EntityVisual;",
                    remap = false))
    private EntityVisual<?> sable$useEmbeddingForSubLevelEntity(
            EntityVisualizer instance,
            VisualizationContext visualizationContext,
            Entity entity,
            float partialTick,
            Operation<EntityVisual<?>> original) {
        if (!FlywheelCompat.ENABLED) {
            return original.call(instance, visualizationContext, entity, partialTick);
        }
        ClientSubLevel subLevel = FlywheelCompat.getContainingSubLevel(entity);
        if (subLevel == null) {
            return original.call(instance, visualizationContext, entity, partialTick);
        }
        FlywheelCompat.FlywheelEmbeddingState state =
                FlywheelCompat.getOrCreateEntityEmbedding(visualizationContext, subLevel);
        return original.call(instance, state.visualEmbedding(), entity, partialTick);
    }
}
