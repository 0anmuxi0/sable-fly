package dev.ryanhcode.sable.mixin.plot;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableCommonEvents;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityTagOutput;
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
   @Unique
   private Map<BlockPos, BlockEntityType<?>> sable$wantedBlockEntities = null;

   @Inject(
      method = {"setBlockState"},
      at = {@At("HEAD")}
   )
   private void sable$preSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
      this.sable$blockSet = pos;
   }

   @Inject(
      method = {"setBlockState"},
      at = {@At("RETURN")}
   )
   private void sable$postSetBlockState(BlockPos pos, BlockState state, int flags, CallbackInfoReturnable<BlockState> cir) {
      if (this.sable$blockSet != null) {
         SubLevel subLevel = Sable.HELPER.getContaining(this.level, this.sable$blockSet);
         if (subLevel != null) {
            subLevel.getPlot().onBlockChange(this.sable$blockSet, state);
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
   private BlockState sable$setBlockState(LevelChunkSection instance, int x, int y, int z, BlockState newState, Operation<BlockState> original) {
      BlockState oldState = (BlockState)original.call(instance, x, y, z, newState);
      if (this.level instanceof ServerLevel serverLevel && oldState != newState && this.sable$blockSet != null) {
         x = this.sable$blockSet.getX();
         y = this.sable$blockSet.getY();
         z = this.sable$blockSet.getZ();
         SableCommonEvents.handleBlockChange(serverLevel, (LevelChunk)(Object)this, x, y, z, oldState, newState);
      }

      return oldState;
   }

   @Unique
   private boolean sable$isPlotChunk() {
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      return container != null && container.inBounds(((LevelChunk)(Object)this).getPos());
   }

   @WrapOperation(
      method = {"replaceWithPacketData"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/chunk/LevelChunk;clearAllBlockEntities()V"
      )}
   )
   private void sable$keepPlotBlockEntities(LevelChunk instance, Operation<Void> original) {
      if (!this.sable$isPlotChunk()) {
         original.call(instance);
      }
   }

   @WrapOperation(
      method = {"replaceWithPacketData"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
      )}
   )
   private void sable$captureWantedBlockEntities(Consumer instance, final Object tagOutput, Operation<Void> original) {
      if (!this.sable$isPlotChunk()) {
         original.call(instance, tagOutput);
      } else {
         final HashMap capturedBlockEntities = new HashMap();
         this.sable$wantedBlockEntities = capturedBlockEntities;
         BlockEntityTagOutput wrappedTagOutput = new BlockEntityTagOutput() {
            {
               Objects.requireNonNull(LevelChunkMixin.this);
            }

            @Override
            public void accept(BlockPos pos, BlockEntityType<?> type, CompoundTag tag) {
               capturedBlockEntities.put(pos.immutable(), type);
               ((BlockEntityTagOutput)tagOutput).accept(pos, type, tag);
            }
         };
         original.call(instance, wrappedTagOutput);
      }
   }

   @Inject(
      method = {"replaceWithPacketData"},
      at = {@At("RETURN")}
   )
   private void sable$resyncBlockStates(FriendlyByteBuf buf, Map<?, ?> heightmaps, Consumer<?> consumer, CallbackInfo ci) {
      if (this.sable$isPlotChunk()) {
         LevelChunk chunk = (LevelChunk)(Object)this;
         Map wantedBlockEntities = this.sable$wantedBlockEntities;
         this.sable$wantedBlockEntities = null;
         if (wantedBlockEntities != null) {
            for (BlockPos pos : new ArrayList<>(chunk.getBlockEntities().keySet())) {
               BlockEntity blockEntity = chunk.getBlockEntity(pos);
               if (blockEntity != null && wantedBlockEntities.get(pos) != blockEntity.getType()) {
                  chunk.removeBlockEntity(pos);
               }
            }
         }

         for (Entry entry : chunk.getBlockEntities().entrySet()) {
            BlockEntity blockEntity = (BlockEntity)entry.getValue();
            BlockState state = chunk.getBlockState((BlockPos)entry.getKey());
            if (blockEntity.getBlockState() != state && blockEntity.isValidBlockState(state)) {
               blockEntity.setBlockState(state);
            }
         }
      }
   }
}
