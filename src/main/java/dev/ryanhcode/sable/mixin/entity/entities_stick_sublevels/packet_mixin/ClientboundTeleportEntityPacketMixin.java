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

@Mixin({ClientboundTeleportEntityPacket.class})
public class ClientboundTeleportEntityPacketMixin implements PacketActuallyInSubLevelExtension {
   @Shadow
   @Final
   @Mutable
   public static StreamCodec<FriendlyByteBuf, ClientboundTeleportEntityPacket> STREAM_CODEC;
   @Unique
   private boolean sable$actuallyInSubLevel;

   @Inject(
      method = {"<clinit>"},
      at = {@At("TAIL")}
   )
   private static void sable$extendStreamCodec(CallbackInfo ci) {
      StreamCodec originalCodec = STREAM_CODEC;
      STREAM_CODEC = StreamCodec.of((buf, packet) -> {
         originalCodec.encode(buf, packet);
         buf.writeBoolean(((PacketActuallyInSubLevelExtension)(Object)packet).sable$isActuallyInSubLevel());
      }, buf -> {
         ClientboundTeleportEntityPacket packet = (ClientboundTeleportEntityPacket)originalCodec.decode(buf);
         ((PacketActuallyInSubLevelExtension)(Object)packet).sable$setActuallyInSubLevel(buf.readBoolean());
         return packet;
      });
   }

   @Override
   public void sable$setActuallyInSubLevel(boolean actuallyInSubLevel) {
      this.sable$actuallyInSubLevel = actuallyInSubLevel;
   }

   @Override
   public boolean sable$isActuallyInSubLevel() {
      return this.sable$actuallyInSubLevel;
   }
}
