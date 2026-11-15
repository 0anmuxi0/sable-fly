package dev.ryanhcode.sable.mixin.plot;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableCommonEvents;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
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

@Mixin({LevelChunk.class})
public class LevelChunkMixin {
   @Shadow
   @Final
   private Level level;
   @Unique
   private BlockPos sable$blockSet = null;

   @Inject(
      method = {"setBlockState"},
      at = {@At("HEAD")}
   )
   private void sable$preSetBlockState(BlockPos pPos, BlockState pState, int pFlags, CallbackInfoReturnable<BlockState> cir) {
      this.sable$blockSet = pPos;
   }

   @Inject(
      method = {"setBlockState"},
      at = {@At("RETURN")}
   )
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
      BlockState oldState = original.call(instance, pX, pY, pZ, newState);
      if (this.level instanceof ServerLevel serverLevel && oldState != newState) {
         pX = this.sable$blockSet.getX();
         pY = this.sable$blockSet.getY();
         pZ = this.sable$blockSet.getZ();
         SableCommonEvents.handleBlockChange(serverLevel, (LevelChunk)(Object)this, pX, pY, pZ, oldState, newState);
      }

      return oldState;
   }

   @Unique
   private boolean sable$isPlotChunk() {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      return container != null && container.inBounds(((LevelChunk)(Object)this).getPos());
   }

   @WrapOperation(
      method = {"replaceWithPacketData(IILnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/chunk/LevelChunk;clearAllBlockEntities()V"
      )}
   )
   private void sable$keepPlotBlockEntities(LevelChunk self, Operation<Void> original) {
      if (!this.sable$isPlotChunk()) {
         original.call(self);
      }
   }

   @Inject(
      method = {"replaceWithPacketData(IILnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;)V"},
      at = {@At("HEAD")}
   )
   private void sable$pruneStaleBlockEntities(int chunkX, int chunkZ, ClientboundLevelChunkPacketData data, CallbackInfo ci) {
      if (this.sable$isPlotChunk()) {
         LevelChunk self = (LevelChunk)(Object)this;
         Map<BlockPos, BlockEntityType<?>> wanted = new HashMap<>();
         data.forEachBlockEntityTag(chunkX, chunkZ, (posx, type, tag) -> wanted.put(posx.immutable(), type));

         for (BlockPos pos : List.copyOf(self.getBlockEntities().keySet())) {
            BlockEntity old = self.getBlockEntity(pos);
            if (old != null && wanted.get(pos) != old.getType()) {
               self.removeBlockEntity(pos);
            }
         }
      }
   }

   @Inject(
      method = {"replaceWithPacketData(IILnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;)V"},
      at = {@At("RETURN")}
   )
   private void sable$resyncBlockStates(int chunkX, int chunkZ, ClientboundLevelChunkPacketData data, CallbackInfo ci) {
      if (this.sable$isPlotChunk()) {
         LevelChunk self = (LevelChunk)(Object)this;

         for (Entry<BlockPos, BlockEntity> entry : self.getBlockEntities().entrySet()) {
            BlockEntity be = entry.getValue();
            BlockState state = self.getBlockState(entry.getKey());
            if (be.getBlockState() != state && be.isValidBlockState(state)) {
               be.setBlockState(state);
            }
         }
      }
   }
}
