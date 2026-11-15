package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 原版桶（{@link BucketItem#emptyContents}）拿到 Sable 射线给出的 sub-level
 * 局部命中坐标后，仍会在<b>父世界</b>上做 {@code mayInteract} / {@code canBeReplaced}
 * / {@code setBlock} —— 父世界那个位置不是可替换方块时直接 FAIL，表现为
 * "对子关卡使用流体桶毫无反应"；即使成功也把流体塞进了父世界的半空里。
 *
 * <p>修复：命中点位于某个 sub-level 内时，把 {@code level} 参数换成该
 * sub-level 的 {@code Level}（局部坐标对局部世界，语义与原版完全一致），
 * 其余逻辑（液体替换判定、gameEvent、副产物）全部原样重放。</p>
 *
 * <p>用 {@code @WrapMethod} 而不是 HEAD+cancel 再调 {@code emptyContents}：
 * 后者会造成无限递归（注入点又调被注入方法）。{@code @WrapMethod} 的
 * {@code operation} 是原始方法体，换参调用安全。</p>
 */
@Mixin(BucketItem.class)
public abstract class BucketItemMixin {

    @WrapMethod(
            method = "emptyContents(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/BlockHitResult;)Z"
    )
    private boolean sable$emptyIntoSubLevel(
            final LivingEntity entity,
            final Level level,
            final BlockPos pos,
            final BlockHitResult hit,
            final Operation<Boolean> original
    ) {
        final SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
        if (subLevel != null) {
            return original.call(entity, subLevel.getLevel(), pos, hit);
        }
        return original.call(entity, level, pos, hit);
    }
}
