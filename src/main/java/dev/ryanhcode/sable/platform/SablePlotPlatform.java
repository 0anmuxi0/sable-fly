package dev.ryanhcode.sable.platform;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.chunk.LevelChunk;

public interface SablePlotPlatform {
   SablePlotPlatform INSTANCE = SablePlatformUtil.load(SablePlotPlatform.class);

   void readLightData(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk);

   void readChunkAttachments(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk);

   void postLoad(CompoundTag tag, LevelChunk chunk);

   void writeLightData(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk);

   void writeChunkAttachments(CompoundTag tag, RegistryAccess registryAccess, LevelChunk chunk);
}
