package dev.ryanhcode.sable.network.packets.tcp;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.client.rope.RopeManager;
import dev.ryanhcode.sable.network.tcp.SablePacketContext;
import dev.ryanhcode.sable.network.tcp.SableTCPPacket;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;
import net.minecraft.world.phys.Vec3;

public record ClientboundRopeSyncPacket(List<ClientboundRopeSyncPacket.Entry> ropes) implements SableTCPPacket {
   public static final Type<ClientboundRopeSyncPacket> TYPE = new Type<>(Sable.sablePath("rope_sync"));
   public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundRopeSyncPacket> CODEC = StreamCodec.of(
      (buf, value) -> value.write(buf), ClientboundRopeSyncPacket::read
   );

   private static ClientboundRopeSyncPacket read(FriendlyByteBuf buf) {
      int count = buf.readVarInt();
      List<ClientboundRopeSyncPacket.Entry> entries = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
         UUID id = buf.readUUID();
         int color = buf.readInt();
         float width = buf.readFloat();
         int pointCount = buf.readVarInt();
         List<Vec3> points = new ArrayList<>(pointCount);

         for (int point = 0; point < pointCount; point++) {
            points.add(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
         }

         entries.add(new ClientboundRopeSyncPacket.Entry(id, points, color, width));
      }

      return new ClientboundRopeSyncPacket(entries);
   }

   private void write(FriendlyByteBuf buf) {
      buf.writeVarInt(this.ropes.size());

      for (ClientboundRopeSyncPacket.Entry entry : this.ropes) {
         buf.writeUUID(entry.id());
         buf.writeInt(entry.color());
         buf.writeFloat(entry.width());
         buf.writeVarInt(entry.points().size());

         for (Vec3 point : entry.points()) {
            buf.writeDouble(point.x);
            buf.writeDouble(point.y);
            buf.writeDouble(point.z);
         }
      }
   }

   @Override
   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }

   @Override
   public void handle(SablePacketContext context) {
      RopeManager.sync(this.ropes);
   }

   public record Entry(UUID id, List<Vec3> points, int color, float width) {
   }
}
