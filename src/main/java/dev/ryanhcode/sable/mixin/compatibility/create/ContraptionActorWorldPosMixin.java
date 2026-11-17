package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让子关卡上的机械（钻头/锯片/犁…）作用到子关卡之外的真实世界方块。
 *
 * <p>诊断版：记录每次 visitNewPosition 的原始坐标、投影后坐标、
 * 以及所属子关卡，用于确认变换是否真的发生。
 */
@Mixin(value = AbstractContraptionEntity.class, remap = false)
public abstract class ContraptionActorWorldPosMixin {

    @WrapOperation(
            method = "tickActors",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/zurrtum/create/api/behaviour/movement/MovementBehaviour;visitNewPosition(Lcom/zurrtum/create/content/contraptions/behaviour/MovementContext;Lnet/minecraft/core/BlockPos;)V"),
            remap = false)
    private void sable$projectActorTargetToWorld(MovementBehaviour behaviour, MovementContext context,
                                                 BlockPos plotPos, Operation<Void> original) {
        AbstractContraptionEntity self = (AbstractContraptionEntity) (Object) this;
        if (context == null || context.world == null || plotPos == null) {
            original.call(behaviour, context, plotPos);
            return;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(self);
        String behaviourName = behaviour.getClass().getSimpleName();
        if (subLevel == null) {
            Sable.LOGGER.info("[drill-diag] {} plotPos={} -> subLevel=NULL (no transform)",
                    behaviourName, plotPos);
            original.call(behaviour, context, plotPos);
            return;
        }
        Vector3d sample = new Vector3d(plotPos.getX() + 0.5, plotPos.getY() + 0.5, plotPos.getZ() + 0.5);
        Vector3d projected = new Vector3d();
        Sable.HELPER.projectOutOfSubLevel(context.world, sample, projected);
        BlockPos worldPos = BlockPos.containing(projected.x, projected.y, projected.z);
        Sable.LOGGER.info("[drill-diag] {} plotPos={} -> worldPos={} (subLevel={})",
                behaviourName, plotPos, worldPos, subLevel.getUniqueId());
        original.call(behaviour, context, worldPos);
    }
}
