package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={AbstractContraptionEntity.class}, remap=false)
public abstract class ContraptionActorWorldPosMixin {
    @WrapOperation(method={"tickActors"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/api/behaviour/movement/MovementBehaviour;visitNewPosition(Lcom/zurrtum/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;)V")}, remap=false)
    private void sable$projectActorTargetToWorld(MovementBehaviour behaviour, MovementContext context, BlockPos plotPos, Operation<Void> original) {
        AbstractContraptionEntity self = (AbstractContraptionEntity)(Object)this;
        if (context == null || context.world == null || plotPos == null) {
            original.call(behaviour, context, plotPos);
            return;
        }
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)self);
        String behaviourName = behaviour.getClass().getSimpleName();
        if (subLevel == null) {
            Sable.LOGGER.info("[drill-diag] {} plotPos={} -> subLevel=NULL (no transform)", behaviourName, plotPos);
            original.call(behaviour, context, plotPos);
            return;
        }
        Vector3d sample = new Vector3d((double)plotPos.getX() + 0.5, (double)plotPos.getY() + 0.5, (double)plotPos.getZ() + 0.5);
        Vector3d projected = new Vector3d();
        Sable.HELPER.projectOutOfSubLevel(context.world, (Vector3dc)sample, projected);
        BlockPos worldPos = BlockPos.containing((double)projected.x, (double)projected.y, (double)projected.z);
        Sable.LOGGER.info("[drill-diag] {} plotPos={} -> worldPos={} (subLevel={})", new Object[]{behaviourName, plotPos, worldPos, subLevel.getUniqueId()});
        original.call(behaviour, context, worldPos);
    }
}