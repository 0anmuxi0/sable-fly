package dev.ryanhcode.sable.mixin.plot;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableCommonEvents;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityTagOutput;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.2 port.
 *
 * 26.3 injected into {@code LevelChunk.replaceWithPacketData(int,int,ClientboundLevelChunkPacketData)}.
 * 26.2 splits that method: {@code LevelChunk.replaceWithPacketData(FriendlyByteBuf, Map, Consumer)} takes an
 * already-extracted buffer plus a {@code Consumer<BlockEntityTagOutput>} that the vanilla body invokes exactly
 * once inside a try-with-resources block (verified: single {@code Consumer.accept} call site, ordinal 0).
 *
 * We therefore hook that single {@code Consumer.accept} call with {@code @WrapOperation} and hand vanilla a
 * <em>proxied</em> {@code BlockEntityTagOutput} that records what the packet wants before delegating.
 *
 * On RETURN we then prune stale block entities and resync block states for plot chunks - the behaviour 26.3
 * implemented directly in its {@code forEachBlockEntityTag}/{@code clearAllBlockEntities} hooks.
 */
@Mixin({LevelChunk.class})
public class LevelChunkMixin {
   @Shadow
   @Final
   private Level level;
   @Unique
   private BlockPos sable$blockSet = null;
   @Unique
   private Map<BlockPos, BlockEntityType<?>> sable$wantedBlockEntities = null;

   @Inject(method = {"setBlockState"}, at = {@At("HEAD")})
   private void sable$preSetBlockState(BlockPos pPos, BlockState pState, int pFlags, CallbackInfoReturnable<BlockState> cir) {
      this.sable$blockSet = pPos;
   }

   @Inject(method = {"setBlockState"}, at = {@At("RETURN")})
   private void sable$postSetBlockState(BlockPos pPos, BlockState pState, int pFlags, CallbackInfoReturnable<BlockState> cir) {
      if (this.sable$blockSet != null) {
         SubLevel subLevel = Sable.HELPER.getContaining(this.level, this.sable$blockSet);
         if (subLevel != null) {
            subLevel.getPlot().onBlockChange(this.sable$blockSet, pState);
         }
      }
      this.sable$blockSet = null;
   }

   @WrapOperation(
      method = {"setBlockState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;setBlockState(IIILnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"
      )}
   )
   private BlockState sable$setBlockState(LevelChunkSection instance, int pX, int pY, int pZ, BlockState newState, Operation<BlockState> original) {
      BlockState oldState = (BlockState) original.call(new Object[]{instance, pX, pY, pZ, newState});
      if (this.level instanceof ServerLevel serverLevel && oldState != newState && this.sable$blockSet != null) {
         pX = this.sable$blockSet.getX();
         pY = this.sable$blockSet.getY();
         pZ = this.sable$blockSet.getZ();
         SableCommonEvents.handleBlockChange(serverLevel, (LevelChunk) (Object) this, pX, pY, pZ, oldState, newState);
      }
      return oldState;
   }

   @Unique
   private boolean sable$isPlotChunk() {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      return container != null && container.inBounds(((LevelChunk) (Object) this).getPos());
   }

   @WrapOperation(
      method = {"replaceWithPacketData"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/chunk/LevelChunk;clearAllBlockEntities()V"
      )}
   )
   private void sable$keepPlotBlockEntities(LevelChunk self, Operation<Void> original) {
      if (!this.sable$isPlotChunk()) {
         original.call(new Object[]{self});
      }
   }

   /**
    * Wrap the single {@code Consumer.accept} call inside 26.2's
    * {@code replaceWithPacketData(FriendlyByteBuf, Map, Consumer)}. For plot chunks we hand vanilla a proxy
    * {@link BlockEntityTagOutput} that records the wanted (pos -> type) pairs, so the RETURN hook below can
    * prune stale block entities exactly like 26.3 did.
    */
   @WrapOperation(
      method = {"replaceWithPacketData"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
      )}
   )
   @SuppressWarnings({"rawtypes", "unchecked"})
   private void sable$captureWantedBlockEntities(Consumer instance, Object tagOutput, Operation<Void> original) {
      if (!this.sable$isPlotChunk()) {
         original.call(new Object[]{instance, tagOutput});
         return;
      }

      Map<BlockPos, BlockEntityType<?>> wanted = new HashMap<>();
      this.sable$wantedBlockEntities = wanted;

      BlockEntityTagOutput proxy = new BlockEntityTagOutput() {
         @Override
         public void accept(BlockPos pos, BlockEntityType<?> type, CompoundTag tag) {
            wanted.put(pos.immutable(), type);
            ((BlockEntityTagOutput) tagOutput).accept(pos, type, tag);
         }
      };

      original.call(new Object[]{instance, proxy});
   }

   @Inject(method = {"replaceWithPacketData"}, at = {@At("RETURN")})
   private void sable$resyncBlockStates(FriendlyByteBuf buf, Map<?, ?> heightmaps, Consumer<?> consumer, CallbackInfo ci) {
      if (!this.sable$isPlotChunk()) {
         return;
      }

      LevelChunk self = (LevelChunk) (Object) this;
      Map<BlockPos, BlockEntityType<?>> wanted = this.sable$wantedBlockEntities;
      this.sable$wantedBlockEntities = null;

      // prune block entities the packet no longer wants (26.3: forEachBlockEntityTag + wanted map)
      if (wanted != null) {
         for (BlockPos pos : new java.util.ArrayList<>(self.getBlockEntities().keySet())) {
            BlockEntity old = self.getBlockEntity(pos);
            if (old != null && wanted.get(pos) != old.getType()) {
               self.removeBlockEntity(pos);
            }
         }
      }

      // resync surviving block entity states with the freshly written sections
      for (Entry<BlockPos, BlockEntity> entry : self.getBlockEntities().entrySet()) {
         BlockEntity be = entry.getValue();
         BlockState state = self.getBlockState(entry.getKey());
         if (be.getBlockState() != state && be.isValidBlockState(state)) {
            be.setBlockState(state);
         }
      }
   }
}
