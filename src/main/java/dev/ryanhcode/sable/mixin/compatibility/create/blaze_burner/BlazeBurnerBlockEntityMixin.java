/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef
 *  com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity
 *  com.zurrtum.create.foundation.blockEntity.SmartBlockEntity
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  org.joml.Vector3d
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.ryanhcode.sable.mixin.compatibility.create.blaze_burner;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalDoubleRef;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlazeBurnerBlockEntity.class})
public abstract class BlazeBurnerBlockEntityMixin
extends SmartBlockEntity {
    @Unique
    private static Vector3d sable$playerPos = new Vector3d();

    public BlazeBurnerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /**
     * 1.21.1 的 {@code tickAnimation} 定义在 {@code BlazeBurnerBlockEntity} 上。
     *
     * <p>Create 26.3 把它整体搬到了客户端渲染类
     * {@code com.zurrtum.create.client.content.processing.burner.BlazeBurnerRenderer#tickAnimation(BlazeBurnerBlockEntity)}，
     * 且签名只剩 BlockEntity 一个参数，{@code @At("INVOKE_ASSIGN")} 指向的
     * {@code LocalPlayer.getZ()} 调用点仍存在但宿主方法已不可注入（本 mixin 的 target 是 BlockEntity）。
     * 目标方法彻底不存在，故移除 {@code @Inject}，仅保留方法体。
     */
    private void sable$projectPlayerPosition(CallbackInfo ci, @Local(name={"x"}) LocalDoubleRef x, @Local(name={"z"}) LocalDoubleRef z, @Local(name={"player"}) LocalPlayer player) {
        SubLevel subLevel = Sable.HELPER.getContaining((BlockEntity)this);
        if (subLevel != null) {
            sable$playerPos.set(x.get(), player.getEyeY(), z.get());
            subLevel.logicalPose().transformPositionInverse(sable$playerPos);
            x.set(BlazeBurnerBlockEntityMixin.sable$playerPos.x);
            z.set(BlazeBurnerBlockEntityMixin.sable$playerPos.z);
        }
    }
}
