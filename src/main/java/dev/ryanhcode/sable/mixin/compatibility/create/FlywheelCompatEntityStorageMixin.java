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

@Mixin(value={EntityStorage.class}, remap=false)
public abstract class FlywheelCompatEntityStorageMixin {
    @WrapOperation(require = 0, method={"createRaw(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/entity/Entity;F)Lcom/zurrtum/create/client/flywheel/api/visual/EntityVisual;"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/client/flywheel/api/visualization/EntityVisualizer;createVisual(Lcom/zurrtum/create/client/flywheel/api/visualization/VisualizationContext;Lnet/minecraft/world/entity/Entity;F)Lcom/zurrtum/create/client/flywheel/api/visual/EntityVisual;", remap=false)})
    private EntityVisual<?> sable$useEmbeddingForSubLevelEntity(EntityVisualizer instance, VisualizationContext visualizationContext, Entity entity, float partialTick, Operation<EntityVisual<?>> original) {
        if (!FlywheelCompat.ENABLED) {
            return (EntityVisual)original.call(instance, visualizationContext, entity, Float.valueOf(partialTick));
        }
        ClientSubLevel subLevel = FlywheelCompat.getContainingSubLevel(entity);
        if (subLevel == null) {
            return (EntityVisual)original.call(instance, visualizationContext, entity, Float.valueOf(partialTick));
        }
        FlywheelCompat.FlywheelEmbeddingState state = FlywheelCompat.getOrCreateEntityEmbedding(visualizationContext, subLevel);
        return (EntityVisual)original.call(instance, state.visualEmbedding(), entity, Float.valueOf(partialTick));
    }
}