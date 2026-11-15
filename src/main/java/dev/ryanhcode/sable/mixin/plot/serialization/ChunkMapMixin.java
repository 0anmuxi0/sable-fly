package dev.ryanhcode.sable.mixin.plot.serialization;

import com.mojang.datafixers.DataFixer;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.entity.Visibility;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ChunkMap.class})
public class ChunkMapMixin {
   @Mutable
   @Shadow
   @Final
   private Queue<Runnable> unloadQueue;
   @Shadow
   @Final
   private ServerLevel level;

   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void sable$init(
      ServerLevel var1,
      LevelStorageAccess var2,
      DataFixer var3,
      StructureTemplateManager var4,
      Executor var5,
      BlockableEventLoop var6,
      LightChunkGetter var7,
      ChunkGenerator var8,
      ChunkStatusUpdateListener var9,
      Supplier<?> var10,
      TicketStorage var11,
      int var12,
      boolean var13,
      CallbackInfo var14
   ) {
      this.unloadQueue = new ConcurrentLinkedDeque<>();
   }

   @Inject(
      method = {"onFullChunkStatusChange"},
      at = {@At("TAIL")}
   )
   private void sable$onStatusChange(ChunkPos var1, FullChunkStatus var2, CallbackInfo var3) {
      ServerSubLevelContainer var4 = SubLevelContainer.getContainer(this.level);

      assert var4 != null : "Sub-level container is null";

      if (!var4.inBounds(var1)) {
         SubLevelHoldingChunkMap var5 = var4.getHoldingChunkMap();
         var5.updateChunkStatus(var1, Visibility.fromFullChunkStatus(var2) != Visibility.HIDDEN);
      }
   }
}
