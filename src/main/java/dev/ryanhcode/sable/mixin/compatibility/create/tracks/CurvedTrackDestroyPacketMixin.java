/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.client.content.trains.track.CurvedTrackDestroyPacket
 *  com.zurrtum.create.client.content.trains.track.TrackBlockEntity
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  org.spongepowered.asm.mixin.Mixin
 */
package dev.ryanhcode.sable.mixin.compatibility.create.tracks;

import com.zurrtum.create.infrastructure.packet.c2s.CurvedTrackDestroyPacket;
import com.zurrtum.create.content.trains.track.TrackBlockEntity;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={CurvedTrackDestroyPacket.class})
public class CurvedTrackDestroyPacketMixin {
    // Sable-Fly 26.3 移植说明：
    // 原 NeoForge 版本在此 @Redirect applySettings(ServerPlayer, TrackBlockEntity)V 中的
    // TrackBlockEntity.getBlockPos() 调用。Create 26.3 (Fabric) 的 CurvedTrackDestroyPacket
    // 已改为 record 并只提供 handle(ServerGamePacketListener)，内部委托
    // AllHandle.onCurvedTrackDestroy(ServerGamePacketListenerImpl, CurvedTrackDestroyPacket)，
    // 类中不再存在 applySettings 方法，也不再有 TrackBlockEntity.getBlockPos() 调用点。
    // 目标方法彻底不存在，故移除 @Redirect 注解但保留方法体以备后续 API 变化。
    protected BlockPos sable$getWorldBlockPos(TrackBlockEntity instance) {
        return BlockPos.containing((Position)Sable.HELPER.projectOutOfSubLevel(instance.getLevel(), Vec3.atCenterOf(instance.getBlockPos())));
    }
}
