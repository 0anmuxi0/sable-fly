package dev.ryanhcode.sable.network.udp;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.flow.FlowControlHandler;
import java.net.InetSocketAddress;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.LocalFrameDecoder;
import net.minecraft.network.LocalFrameEncoder;
import net.minecraft.network.MonitoredLocalFrameDecoder;
import net.minecraft.network.Varint21FrameDecoder;
import net.minecraft.network.Varint21LengthFieldPrepender;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface SableUDPPacket {
   static void configureSerialization(ChannelPipeline pipeline, PacketFlow flow, boolean memoryOnly, @org.jetbrains.annotations.Nullable BandwidthDebugMonitor debugMonitor) {
      pipeline.addLast("splitter", createFrameDecoder(debugMonitor, memoryOnly))
         .addLast(new ChannelHandler[]{new FlowControlHandler()})
         .addLast("decoder", new SableUDPPacketDecoder())
         .addLast("prepender", createFrameEncoder(memoryOnly))
         .addLast("encoder", new SableUDPPacketEncoder());
   }

   private static ChannelOutboundHandler createFrameEncoder(boolean memoryOnly) {
      return (ChannelOutboundHandler)(memoryOnly ? new LocalFrameEncoder() : new Varint21LengthFieldPrepender());
   }

   private static ChannelInboundHandler createFrameDecoder(@org.jetbrains.annotations.Nullable BandwidthDebugMonitor debugMonitor, boolean memoryOnly) {
      if (!memoryOnly) {
         return new Varint21FrameDecoder(debugMonitor);
      } else {
         return (ChannelInboundHandler)(debugMonitor != null ? new MonitoredLocalFrameDecoder(debugMonitor) : new LocalFrameDecoder());
      }
   }

   static void configureInMemoryPipeline(ChannelPipeline channelPipeline, PacketFlow arg) {
      configureSerialization(channelPipeline, arg, true, null);
   }

   SableUDPPacketType getType();

   default void handleClient(Level level) {
   }

   default void handleServer(MinecraftServer server, InetSocketAddress sender) {
   }
}
