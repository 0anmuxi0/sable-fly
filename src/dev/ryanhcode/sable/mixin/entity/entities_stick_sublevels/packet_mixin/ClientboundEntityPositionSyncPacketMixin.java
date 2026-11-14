package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels.packet_mixin;

import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientboundEntityPositionSyncPacket.class})
public class ClientboundEntityPositionSyncPacketMixin
implements PacketActuallyInSubLevelExtension {
    @Shadow
    @Final
    @Mutable
    public static StreamCodec<FriendlyByteBuf, ClientboundEntityPositionSyncPacket> STREAM_CODEC;
    @Unique
    private boolean sable$actuallyInSubLevel;

    @Inject(method={"<clinit>"}, at={@At(value="TAIL")})
    private static void sable$extendStreamCodec(CallbackInfo callbackInfo) {
        StreamCodec<FriendlyByteBuf, ClientboundEntityPositionSyncPacket> streamCodec = STREAM_CODEC;
        STREAM_CODEC = StreamCodec.of((friendlyByteBuf, clientboundEntityPositionSyncPacket) -> {
            streamCodec.encode(friendlyByteBuf, clientboundEntityPositionSyncPacket);
            friendlyByteBuf.writeBoolean(((PacketActuallyInSubLevelExtension)clientboundEntityPositionSyncPacket).sable$isActuallyInSubLevel());
        }, friendlyByteBuf -> {
            ClientboundEntityPositionSyncPacket clientboundEntityPositionSyncPacket = (ClientboundEntityPositionSyncPacket)streamCodec.decode(friendlyByteBuf);
            ((PacketActuallyInSubLevelExtension)clientboundEntityPositionSyncPacket).sable$setActuallyInSubLevel(friendlyByteBuf.readBoolean());
            return clientboundEntityPositionSyncPacket;
        });
    }

    @Override
    public void sable$setActuallyInSubLevel(boolean bl) {
        this.sable$actuallyInSubLevel = bl;
    }

    @Override
    public boolean sable$isActuallyInSubLevel() {
        return this.sable$actuallyInSubLevel;
    }
}

