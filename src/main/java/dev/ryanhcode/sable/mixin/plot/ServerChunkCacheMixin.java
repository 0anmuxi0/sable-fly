package dev.ryanhcode.sable.mixin.plot;

import com.mojang.datafixers.DataFixer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerChunkCache.class})
public class ServerChunkCacheMixin {
   @Shadow
   @Final
   ServerLevel level;
   @Shadow
   @Final
   private Set<ChunkHolder> chunkHoldersToBroadcast;
   @Unique
   private EmptyLevelChunk sable$emptyChunk;

   @Inject(
      method = {"<init>"},
      at = {@At("RETURN")}
   )
   public void init(
      ServerLevel var1,
      LevelStorageAccess var2,
      DataFixer var3,
      StructureTemplateManager var4,
      Executor var5,
      ChunkGenerator var6,
      int var7,
      int var8,
      boolean var9,
      ChunkStatusUpdateListener var10,
      Supplier<?> var11,
      CallbackInfo var12
   ) {
      this.sable$emptyChunk = new EmptyLevelChunk(var1, new ChunkPos(0, 0), var1.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
   }

   @Unique
   @NotNull
   private SubLevelContainer sable$getPlotContainer() {
      return SubLevelContainer.getContainer(this.level);
   }

   @Inject(
      method = {"getChunkNow"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getChunkNow(int var1, int var2, CallbackInfoReturnable<LevelChunk> var3) {
      SubLevelContainer var4 = this.sable$getPlotContainer();
      if (var4.inBounds(var1, var2)) {
         LevelChunk var5 = var4.getChunk(new ChunkPos(var1, var2));
         var3.setReturnValue(var5);
      }
   }

   @Inject(
      method = {"getChunkFutureMainThread"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getChunkFutureMainThread(
      int var1, int var2, ChunkStatus var3, boolean var4, CallbackInfoReturnable<CompletableFuture<ChunkResult<ChunkAccess>>> var5
   ) {
      SubLevelContainer var6 = this.sable$getPlotContainer();
      if (var6.inBounds(var1, var2)) {
         ChunkPos var7 = new ChunkPos(var1, var2);
         LevelChunk var8 = var6.getChunk(var7);
         if (var8 != null) {
            var5.setReturnValue(CompletableFuture.completedFuture(ChunkResult.of(var8)));
         } else {
            var5.setReturnValue(CompletableFuture.completedFuture(ChunkResult.of(this.sable$emptyChunk)));
         }
      }
   }

   @Inject(
      method = {"hasChunk"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void hasChunk(int var1, int var2, CallbackInfoReturnable<Boolean> var3) {
      SubLevelContainer var4 = this.sable$getPlotContainer();
      if (var4.inBounds(var1, var2)) {
         LevelChunk var5 = var4.getChunk(new ChunkPos(var1, var2));
         var3.setReturnValue(var5 != null);
      }
   }

   @Inject(
      method = {"getChunkForLighting"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getChunkForLighting(int var1, int var2, CallbackInfoReturnable<LightChunk> var3) {
      SubLevelContainer var4 = this.sable$getPlotContainer();
      if (var4.inBounds(var1, var2)) {
         LevelChunk var5 = var4.getChunk(new ChunkPos(var1, var2));
         var3.setReturnValue(var5);
      }
   }

   @Inject(
      method = {"isPositionTicking"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void isPositionTicking(long var1, CallbackInfoReturnable<Boolean> var3) {
      SubLevelContainer var4 = this.sable$getPlotContainer();
      int var5 = ChunkPos.getX(var1);
      int var6 = ChunkPos.getZ(var1);
      if (var4.inBounds(var5, var6)) {
         ChunkPos var7 = new ChunkPos(var5, var6);
         LevelChunk var8 = var4.getChunk(var7);
         var3.setReturnValue(var8 != null);
      }
   }

   @Inject(
      method = {"getFullChunk"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getFullChunk(long var1, Consumer<LevelChunk> var3, CallbackInfo var4) {
      SubLevelContainer var5 = this.sable$getPlotContainer();
      int var6 = ChunkPos.getX(var1);
      int var7 = ChunkPos.getZ(var1);
      if (var5.inBounds(var6, var7)) {
         ChunkPos var8 = new ChunkPos(var6, var7);
         LevelChunk var9 = var5.getChunk(var8);
         if (var9 != null) {
            var3.accept(var9);
         }

         var4.cancel();
      }
   }

   @Inject(
      method = {"blockChanged"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void blockChanged(BlockPos var1, CallbackInfo var2) {
      SubLevelContainer var3 = this.sable$getPlotContainer();
      ChunkPos var4 = new ChunkPos(SectionPos.blockToSectionCoord(var1.getX()), SectionPos.blockToSectionCoord(var1.getZ()));
      if (var3.inBounds(var4)) {
         PlotChunkHolder var5 = var3.getChunkHolder(var4);
         if (var5 == null) {
            throw new UnsupportedOperationException("Cannot change blocks in nonexistent plot holder");
         }

         if (var5.blockChanged(var1)) {
            this.chunkHoldersToBroadcast.add(var5);
         }

         var2.cancel();
      }
   }

   @Inject(
      method = {"getVisibleChunkIfPresent"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void getVisibleChunkIfPresent(long var1, CallbackInfoReturnable<ChunkHolder> var3) {
      int var4 = ChunkPos.getX(var1);
      int var5 = ChunkPos.getZ(var1);
      SubLevelContainer var6 = this.sable$getPlotContainer();
      if (var6.inBounds(var4, var5)) {
         ChunkPos var7 = new ChunkPos(var4, var5);
         PlotChunkHolder var8 = var6.getChunkHolder(var7);
         var3.setReturnValue(var8);
      }
   }

   @Inject(
      method = {"addTicketWithRadius"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void addTicketWithRadius(TicketType var1, ChunkPos var2, int var3, CallbackInfo var4) {
      SubLevelContainer var5 = this.sable$getPlotContainer();
      if (var5.inBounds(var2)) {
         var4.cancel();
      }
   }
}
