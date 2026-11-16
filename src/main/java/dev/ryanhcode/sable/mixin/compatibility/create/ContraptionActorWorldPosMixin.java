package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.Sable;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让子关卡上的机械（钻头 / 锯片 / 收割机 / 犁 / 方块破碎机…）能够作用到
 * <b>子关卡之外</b>的真实世界方块。
 *
 * <p><b>根因</b>：{@code AbstractContraptionEntity.tickActors} 里，
 * 每个 actor 的作用位置是按 contraption 自身位姿算出来的
 * （{@code toGlobalVector(...)} → {@code BlockPos.containing(...)}）。
 * 当 contraption 位于子关卡时，这条链算出来的是 <b>子关卡的 plot 虚拟坐标</b>
 * （约 ±20.48M），而 {@code MovementContext.world} 是父世界、真实方块在主世界位置。
 * 于是 {@code visitNewPosition(context, plotPos)} 里的
 * {@code world.getBlockState(plotPos)} 永远拿到空气 ——
 * 钻头转得再快也破坏不了任何东西（锯片、犁等同理）。
 *
 * <p><b>修复</b>：在调用 {@code visitNewPosition} 前，把 plot 虚拟坐标经
 * {@code Sable.HELPER.projectOutOfSubLevel(...)} 投影到真实世界坐标。
 * 这与官方 Offroad {@code RockCuttingWheelActor} 使用的坐标链一致：
 * <pre>
 * Vec3 worldPos = entity.toGlobalVector(localPos, 1);              // 子关卡局部 -> 父世界
 * Vec3 realPos  = Sable.HELPER.projectOutOfSubLevel(world, worldPos); // 父世界 -> 真实世界
 * </pre>
 *
 * <p>主世界 contraption 完全不受影响：{@code projectOutOfSubLevel} 对不在子关卡内的
 * 位置原样返回。
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
        if (context == null || context.world == null || plotPos == null) {
            original.call(behaviour, context, plotPos);
            return;
        }
        // plot 虚拟坐标 -> 真实世界坐标
        Vector3d projected = Sable.HELPER.projectOutOfSubLevel(
                context.world,
                new Vector3d(plotPos.getX() + 0.5, plotPos.getY() + 0.5, plotPos.getZ() + 0.5),
                new Vector3d());
        BlockPos worldPos = BlockPos.containing(projected.x, projected.y, projected.z);
        if (Objects.equals(worldPos, plotPos)) {
            original.call(behaviour, context, plotPos);
            return;
        }
        original.call(behaviour, context, worldPos);
    }
}
