package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.platform.SablePlotPlatform;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;

public class SablePlotPlatformImpl implements SablePlotPlatform {
   @Override
   public void readLightData(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk) {
   }

   @Override
   public void readChunkAttachments(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk) {
   }

   @Override
   public void postLoad(CompoundTag tag, LevelChunk chunk) {
   }

   @Override
   public void writeLightData(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk) {
   }

   @Override
   public void writeChunkAttachments(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk) {
   }
}
