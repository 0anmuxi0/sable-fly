package dev.ryanhcode.sable.mixin.plot;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.platform.SablePlatform;
import dev.ryanhcode.sable.render.SubLevelDynamicLights;
import dev.ryanhcode.sable.sublevel.storage.SubLevelOccupancySavedData;
import dev.ryanhcode.sable.sublevel.storage.SubLevelTicketsSavedData;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import java.util.function.BooleanSupplier;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({ServerLevel.class})
public abstract class ServerLevelMixin extends Level {
   protected ServerLevelMixin(
      WritableLevelData writableLevelData,
      ResourceKey<Level> resourceKey,
      RegistryAccess registryAccess,
      Holder<DimensionType> holder,
      boolean bl,
      boolean bl2,
      long l,
      int i
   ) {
      super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
   }

   @Shadow
   @Override
   public abstract TickRateManager tickRateManager();

   @Shadow
   @Override
   public abstract ChunkSource getChunkSource();

   @Inject(require = 0, method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void sable$init(CallbackInfo ci) {
      if (!SablePlatform.INSTANCE.isWrappedLevel((ServerLevel)(Object)this)) {
         SubLevelOccupancySavedData.getOrLoad((ServerLevel)(Object)this);
         SubLevelTicketsSavedData.getOrLoad((ServerLevel)(Object)this);
      }

      ServerSubLevelContainer container = (ServerSubLevelContainer)SubLevelContainer.getContainer(this);
      if (container != null) {
         container.initialize();
      }
   }

   @Inject(require = 0, method = {"save"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerLevel;saveLevelData(Z)V",
         shift = Shift.BEFORE
      )}
   )
   public void sable$saveSubLevels(ProgressListener progressListener, boolean bl, boolean bl2, CallbackInfo ci) {
      ServerLevel self = (ServerLevel)(Object)this;
      if (progressListener != null) {
         progressListener.progressStartNoAbort(Component.translatable("menu.savingSubLevels"));
      }

      ServerSubLevelContainer container = SubLevelContainer.getContainer(self);

      assert container != null : "No sub-level container";

      SubLevelHoldingChunkMap holdingChunkMap = container.getHoldingChunkMap();
      holdingChunkMap.saveAll();
   }

   @Inject(require = 0, method = {"tick(Ljava/util/function/BooleanSupplier;)V"},
      at = {@At("HEAD")}
   )
   private void sable$tickPlotContainer(BooleanSupplier booleanSupplier, CallbackInfo ci) {
      ServerLevel self = (ServerLevel)(Object)this;
      TickRateManager tickRateManager = this.tickRateManager();
      boolean runNormally = tickRateManager.runsNormally();
      ServerSubLevelContainer plotContainer = SubLevelContainer.getContainer(self);

      assert plotContainer != null : "SubLevelContainer is null when ticking";

      if (runNormally) {
         plotContainer.tick();
      }

      SubLevelDynamicLights.tick(self);
   }

   @Inject(require = 0, method = {"shouldTickBlocksAt"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$shouldTickBlocksAt(long l, CallbackInfoReturnable<Boolean> cir) {
      SubLevelContainer plotContainer = SubLevelContainer.getContainer((ServerLevel)(Object)this);

      assert plotContainer != null;

      if (plotContainer.getPlot(new ChunkPos(ChunkPos.getX(l), ChunkPos.getZ(l))) != null) {
         cir.setReturnValue(true);
      }
   }

   @Inject(require = 0, method = {"anyPlayerCloseEnoughForSpawning(Lnet/minecraft/world/level/ChunkPos;)Z"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$isNaturalSpawningAllowed(ChunkPos chunkPos, CallbackInfoReturnable<Boolean> cir) {
      SubLevelContainer plotContainer = SubLevelContainer.getContainer((ServerLevel)(Object)this);

      assert plotContainer != null;

      if (plotContainer.getPlot(chunkPos) != null) {
         cir.setReturnValue(true);
      }
   }

   @Inject(require = 0, method = {"close"},
      at = {@At("TAIL")}
   )
   private void sable$close(CallbackInfo ci) {
      ServerLevel self = (ServerLevel)(Object)this;
      ServerSubLevelContainer container = SubLevelContainer.getContainer(self);
      if (container != null) {
         container.close();
      }

      SubLevelDynamicLights.clear(self);
   }
}
