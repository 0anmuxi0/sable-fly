package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让子关卡上的机械（钻头 / 锯片 / 收割机 / 犁 / 方块破碎机…）能够作用到
 * <b>子关卡之外</b>的真实世界方块。
 *
 * <p><b>根因</b>：{@code AbstractContraptionEntity.tickActors()} 里，每个 actor 的作用位置
 * 按 contraption 自身位姿算出（{@code toGlobalVector(...)} → {@code BlockPos.containing(...)}）。
 * contraption 位于子关卡时，这条链得到的是<b>子关卡的 plot 虚拟坐标</b>（约 ±20.48M），
 * 而 {@code MovementContext.world} 是父世界、真实方块在主世界位置。
 * 于是 {@code visitNewPosition(context, plotPos)} 里的
 * {@code world.getBlockState(plotPos)} 永远拿到空气 —— 钻头转得再快也破坏不了东西。
 *
 * <p><b>修复</b>：在调用 {@code visitNewPosition} 前，把 plot 虚拟坐标经
 * {@code Sable.HELPER.projectOutOfSubLevel(...)} 投影到真实世界坐标。
 * 与官方 Offroad {@code RockCuttingWheelActor} 使用的坐标链一致。
 *
 * <p><b>注意</b>：{@code tickActors()} <b>没有参数</b>，因此不能用
 * {@code @Local(argsOnly = true)} 取 contraption（那样拿不到任何值，mixin 虽然注入成功
 * 但逻辑永远走"不变换"分支）。这里改用 {@code this} 转型取得目标实例。
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
        if (subLevel == null) {
            // 主世界 contraption：坐标本来就是世界坐标，无需变换
            original.call(behaviour, context, plotPos);
            return;
        }
        Vector3d sample = new Vector3d(plotPos.getX() + 0.5, plotPos.getY() + 0.5, plotPos.getZ() + 0.5);
        Vector3d projected = new Vector3d();
        Sable.HELPER.projectOutOfSubLevel(context.world, sample, projected);
        BlockPos worldPos = BlockPos.containing(projected.x, projected.y, projected.z);
        original.call(behaviour, context, worldPos);
    }
}
