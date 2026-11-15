package dev.ryanhcode.sable.network.tcp;

import dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotDualPacket;
import dev.ryanhcode.sable.network.packets.ClientboundSableSnapshotInfoDualPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundChangeBoundsSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundChangeSubLevelNamePacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundEnterGizmoPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFinalizeSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFloatingBlockMaterialPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundFreezePlayerPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundPhysicsPropertyPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundRecentlySplitSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundRopeSyncPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundSableUDPActivationPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStartTrackingSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStopMovingSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundStopTrackingSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundGizmoMoveSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundPunchSubLevelPacket;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundRemoveRopePacket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public class SableTCPPackets {
   private static final List<SableTCPPackets.Entry<?>> ENTRIES = new ArrayList<>();

   public static void init() {
      clientbound(ClientboundSableSnapshotDualPacket.TYPE, ClientboundSableSnapshotDualPacket.CODEC);
      clientbound(ClientboundSableSnapshotInfoDualPacket.TYPE, ClientboundSableSnapshotInfoDualPacket.CODEC);
      clientbound(ClientboundStopMovingSubLevelPacket.TYPE, ClientboundStopMovingSubLevelPacket.CODEC);
      clientbound(ClientboundChangeSubLevelNamePacket.TYPE, ClientboundChangeSubLevelNamePacket.CODEC);
      clientbound(ClientboundStartTrackingSubLevelPacket.TYPE, ClientboundStartTrackingSubLevelPacket.CODEC);
      clientbound(ClientboundFinalizeSubLevelPacket.TYPE, ClientboundFinalizeSubLevelPacket.CODEC);
      clientbound(ClientboundStopTrackingSubLevelPacket.TYPE, ClientboundStopTrackingSubLevelPacket.CODEC);
      clientbound(ClientboundChangeBoundsSubLevelPacket.TYPE, ClientboundChangeBoundsSubLevelPacket.CODEC);
      clientbound(ClientboundFreezePlayerPacket.TYPE, ClientboundFreezePlayerPacket.CODEC);
      clientbound(ClientboundPhysicsPropertyPacket.TYPE, ClientboundPhysicsPropertyPacket.CODEC);
      clientbound(ClientboundRopeSyncPacket.TYPE, ClientboundRopeSyncPacket.CODEC);
      clientbound(ClientboundFloatingBlockMaterialPacket.TYPE, ClientboundFloatingBlockMaterialPacket.CODEC);
      clientbound(ClientboundRecentlySplitSubLevelPacket.TYPE, ClientboundRecentlySplitSubLevelPacket.CODEC);
      clientbound(ClientboundSableUDPActivationPacket.TYPE, ClientboundSableUDPActivationPacket.CODEC);
      clientbound(ClientboundEnterGizmoPacket.TYPE, ClientboundEnterGizmoPacket.CODEC);
      serverbound(ServerboundPunchSubLevelPacket.TYPE, ServerboundPunchSubLevelPacket.CODEC);
      serverbound(ServerboundRemoveRopePacket.TYPE, ServerboundRemoveRopePacket.CODEC);
      serverbound(ServerboundGizmoMoveSubLevelPacket.TYPE, ServerboundGizmoMoveSubLevelPacket.CODEC);
   }

   public static List<SableTCPPackets.Entry<?>> entries() {
      return Collections.unmodifiableList(ENTRIES);
   }

   private static <T extends SableTCPPacket> void clientbound(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
      ENTRIES.add(new SableTCPPackets.Entry<>(type, codec, true));
   }

   private static <T extends SableTCPPacket> void serverbound(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
      ENTRIES.add(new SableTCPPackets.Entry<>(type, codec, false));
   }

   public record Entry<T extends SableTCPPacket>(Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec, boolean clientbound) {
   }
}
