package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.platform.SableChunkEventPlatform;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.Load;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents.Unload;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SableChunkEventPlatformImpl implements SableChunkEventPlatform {
   @Override
   public void onClientChunkPacketReplaced(LevelChunk chunk) {
      ((Load)ClientChunkEvents.CHUNK_LOAD.invoker()).onChunkLoad((ClientLevel)chunk.getLevel(), chunk);
   }

   @Override
   public void onOldChunkInvalid(LevelChunk chunk) {
      ((Unload)ClientChunkEvents.CHUNK_UNLOAD.invoker()).onChunkUnload((ClientLevel)chunk.getLevel(), chunk);
   }

   @Override
   public void onPlotChunkLoaded(LevelChunk chunk) {
      ((net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.Load)ServerChunkEvents.CHUNK_LOAD.invoker())
         .onChunkLoad((ServerLevel)chunk.getLevel(), chunk, false);
   }
}
