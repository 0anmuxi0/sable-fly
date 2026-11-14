package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels.packet_mixin;

import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ClientboundTeleportEntityPacket.class})
public class ClientboundTeleportEntityPacketMixin
implements PacketActuallyInSubLevelExtension {
    @Shadow
    @Final
    @Mutable
    public static StreamCodec<FriendlyByteBuf, ClientboundTeleportEntityPacket> STREAM_CODEC;
    @Unique
    private boolean sable$actuallyInSubLevel;

    @Inject(method={"<clinit>"}, at={@At(value="TAIL")})
    private static void sable$extendStreamCodec(CallbackInfo callbackInfo) {
        StreamCodec<FriendlyByteBuf, ClientboundTeleportEntityPacket> streamCodec = STREAM_CODEC;
        STREAM_CODEC = StreamCodec.of((friendlyByteBuf, clientboundTeleportEntityPacket) -> {
            streamCodec.encode(friendlyByteBuf, clientboundTeleportEntityPacket);
            friendlyByteBuf.writeBoolean(((PacketActuallyInSubLevelExtension)clientboundTeleportEntityPacket).sable$isActuallyInSubLevel());
        }, friendlyByteBuf -> {
            ClientboundTeleportEntityPacket clientboundTeleportEntityPacket = (ClientboundTeleportEntityPacket)streamCodec.decode(friendlyByteBuf);
            ((PacketActuallyInSubLevelExtension)clientboundTeleportEntityPacket).sable$setActuallyInSubLevel(friendlyByteBuf.readBoolean());
            return clientboundTeleportEntityPacket;
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

