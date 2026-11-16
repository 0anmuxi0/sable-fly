package dev.ryanhcode.sable.network.tcp;

import java.util.Collection;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface SablePacketSink {
   void sendPacket(CustomPacketPayload... packets);

   static SablePacketSink player(ServerPlayer player) {
      return payloads -> {
         for (CustomPacketPayload payload : payloads) {
            player.connection.send(new ClientboundCustomPayloadPacket(payload));
         }
      };
   }

   static SablePacketSink players(Collection<ServerPlayer> players) {
      return payloads -> {
         for (ServerPlayer player : players) {
            for (CustomPacketPayload payload : payloads) {
               player.connection.send(new ClientboundCustomPayloadPacket(payload));
            }
         }
      };
   }

   static SablePacketSink server() {
      return payloads -> {
         ClientPacketListener connection = Minecraft.getInstance().getConnection();
         if (connection != null) {
            for (CustomPacketPayload payload : payloads) {
               connection.send(new ServerboundCustomPayloadPacket(payload));
            }
         }
      };
   }
}
