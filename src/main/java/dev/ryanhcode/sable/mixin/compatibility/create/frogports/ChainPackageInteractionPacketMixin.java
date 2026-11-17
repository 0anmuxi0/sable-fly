/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity
 *  com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorPackage
 *  com.zurrtum.create.content.kinetics.chainConveyor.ChainPackageInteractionPacket
 *  net.minecraft.core.BlockPos
 *  net.minecraft.server.level.ServerPlayer
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.ryanhcode.sable.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorBlockEntity;
import com.zurrtum.create.content.kinetics.chainConveyor.ChainConveyorPackage;
import com.zurrtum.create.infrastructure.packet.c2s.ChainPackageInteractionPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ChainPackageInteractionPacket.class})
public class ChainPackageInteractionPacketMixin {
    @Shadow
    @Final
    private BlockPos selectedConnection;
    @Shadow
    @Final
    private float chainPosition;

    /**
     * 1.21.1 的 {@code ChainPackageInteractionPacket#applySettings(ServerPlayer, ChainConveyorBlockEntity)}
     * 在 Create 26.3 已被移除：包处理逻辑整体搬入
     * {@code com.zurrtum.create.AllHandle#onChainPackageInteraction}，
     * 包裹位置在 {@code lambda$onChainPackageInteraction$0} 内计算，不再有可注入的
     * {@code addLoopingPackage}/{@code addTravellingPackage} 调用点。
     * 目标方法彻底不存在，故移除 {@code @Inject}，仅保留方法体。
     */
    private void sable$initialiseLoopingWorldPosition(ServerPlayer player, ChainConveyorBlockEntity be, CallbackInfo ci, @Local(name={"chainConveyorPackage"}) ChainConveyorPackage chainConveyorPackage) {
        chainConveyorPackage.worldPosition = be.getPackagePosition(this.chainPosition, null);
    }

    /** 同上：{@code addTravellingPackage} 调用点已随 {@code applySettings} 一并移除。 */
    private void sable$initialiseTravellingWorldPosition(ServerPlayer player, ChainConveyorBlockEntity be, CallbackInfo ci, @Local(name={"chainConveyorPackage"}) ChainConveyorPackage chainConveyorPackage) {
        chainConveyorPackage.worldPosition = be.getPackagePosition(this.chainPosition, this.selectedConnection);
    }
}
