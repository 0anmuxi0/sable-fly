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
   private static void sable$extendStreamCodec(CallbackInfo var0) {
      StreamCodec var1 = STREAM_CODEC;
      STREAM_CODEC = StreamCodec.of((var1x, var2) -> {
         var1.encode(var1x, var2);
         var1x.writeBoolean(((PacketActuallyInSubLevelExtension)(Object)var2).sable$isActuallyInSubLevel());
      }, var1x -> {
         ClientboundTeleportEntityPacket var2 = (ClientboundTeleportEntityPacket)var1.decode(var1x);
         ((PacketActuallyInSubLevelExtension)(Object)var2).sable$setActuallyInSubLevel(var1x.readBoolean());
         return var2;
      });
   }

   @Override
   public void sable$setActuallyInSubLevel(boolean var1) {
      this.sable$actuallyInSubLevel = var1;
   }

   @Override
   public boolean sable$isActuallyInSubLevel() {
      return this.sable$actuallyInSubLevel;
   }
}
