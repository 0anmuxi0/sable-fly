package dev.ryanhcode.sable.mixin.plot;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.loaded_chunk_debug.DebugChunkProviderAttachments;
import dev.ryanhcode.sable.platform.SableChunkEventPlatform;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ClientChunkCache.class})
public abstract class ClientChunkCacheMixin implements DebugChunkProviderAttachments {
   @Shadow
   @Final
   private static Logger LOGGER;
   @Shadow
   @Final
   private ClientLevel level;
   @Shadow
   @Final
   private LevelChunk emptyChunk;

   @Shadow
   private static boolean isValidChunk(@Nullable LevelChunk var0, int var1, int var2) {
      return false;
   }

   @Unique
   @NotNull
   private SubLevelContainer sable$getPlotContainer() {
      return SubLevelContainer.getContainer(this.level);
   }

   @Inject(
      method = {"getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getChunk(int var1, int var2, ChunkStatus var3, boolean var4, CallbackInfoReturnable<LevelChunk> var5) {
      SubLevelContainer var6 = this.sable$getPlotContainer();
      if (var6.inBounds(var1, var2)) {
         ChunkPos var7 = new ChunkPos(var1, var2);
         LevelChunk var8 = var6.getChunk(var7);
         if (var8 != null) {
            var5.setReturnValue(var8);
         } else {
            var5.setReturnValue(this.emptyChunk);
         }
      }
   }

   @Inject(
      method = {"drop"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void drop(ChunkPos var1, CallbackInfo var2) {
      SubLevelContainer var3 = this.sable$getPlotContainer();
      if (var3.inBounds(var1)) {
         var2.cancel();
      }
   }

   @Inject(
      method = {"replaceBiomes"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void replaceBiomes(int var1, int var2, FriendlyByteBuf var3, CallbackInfo var4) {
      SubLevelContainer var5 = this.sable$getPlotContainer();
      if (var5.inBounds(var1, var2)) {
         ChunkPos var6 = new ChunkPos(var1, var2);
         LevelChunk var7 = var5.getChunk(var6);
         if (var7 != null && isValidChunk(var7, var1, var2)) {
            var7.replaceBiomes(var3);
         } else {
            LOGGER.warn("Ignoring chunk since it's not present: {}, {}", var1, var2);
         }
      }
   }

   @Inject(
      method = {"replaceWithPacketData"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void replaceWithPacketData(int var1, int var2, FriendlyByteBuf var3, Map var4, Consumer var5, CallbackInfoReturnable<LevelChunk> var6) {
      SubLevelContainer var7 = this.sable$getPlotContainer();
      if (var7.inBounds(var1, var2)) {
         ChunkPos var8 = new ChunkPos(var1, var2);
         LevelChunk var9 = var7.getChunk(var8);
         if (!isValidChunk(var9, var1, var2)) {
            if (var9 != null) {
               SableChunkEventPlatform.INSTANCE.onOldChunkInvalid(var9);
               this.level.unload(var9);
            }

            var9 = new LevelChunk(this.level, var8);
            var9.replaceWithPacketData(var3, var4, var5);
            var7.newPopulatedChunk(var8, var9);
         } else {
            var9.replaceWithPacketData(var3, var4, var5);
         }

         this.level.onChunkLoaded(var8);
         this.level.getLightEngine().setLightEnabled(var8, true);
         SableChunkEventPlatform.INSTANCE.onClientChunkPacketReplaced(var9);
         var6.setReturnValue(var9);
      }
   }

   @Override
   public Collection<LevelChunk> sable$loadedChunks() {
      return List.of();
   }
}
