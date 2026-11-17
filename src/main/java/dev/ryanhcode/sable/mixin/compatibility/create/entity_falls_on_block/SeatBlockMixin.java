/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.content.contraptions.actors.seat.SeatBlock
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.ryanhcode.sable.mixin.compatibility.create.entity_falls_on_block;

import com.zurrtum.create.content.contraptions.actors.seat.SeatBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.21.1 通过 @Redirect 拦截 {@code Entity.blockPosition()}，让实体落到椅子上时改用 {@code getOnPos()}。
 *
 * <p>Minecraft 26.x 将 {@code Block.updateEntityAfterFallOn(Entity)} 改为
 * {@code Block.fallOn(Level, BlockState, BlockPos, Entity, double)}，方块位置直接作为参数传入。
 * 经 javap 核对，Create 26.3 的 {@code SeatBlock.fallOn} 方法体内部 <b>不再调用</b>
 * {@code Entity.blockPosition()} / {@code getOnPos()}，原注入点已彻底不存在，
 * 因此移除该 @Redirect 注解，仅保留方法体备查。
 */
@Mixin(value={SeatBlock.class})
public class SeatBlockMixin {
    // 原 @Redirect(require = 0, method={"updateEntityAfterFallOn"},
    //     at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;blockPosition()Lnet/minecraft/core/BlockPos;"))
    // 目标方法 updateEntityAfterFallOn 在 MC 26.x 已不存在，注入点迁移到 Block.fallOn 且不再反查实体位置。
    private BlockPos sable$updateEntityAfterFallOn(Entity instance) {
        return instance.getOnPos();
    }
}

