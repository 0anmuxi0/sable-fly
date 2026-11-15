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
   private void sable$preSetBlockState(BlockPos var1, BlockState var2, int var3, CallbackInfoReturnable<BlockState> var4) {
      this.sable$blockSet = var1;
   }

   @Inject(
      method = {"setBlockState"},
      at = {@At("RETURN")}
   )
   private void sable$postSetBlockState(BlockPos var1, BlockState var2, int var3, CallbackInfoReturnable<BlockState> var4) {
      if (this.sable$blockSet != null) {
         SubLevel var5 = Sable.HELPER.getContaining(this.level, this.sable$blockSet);
         if (var5 != null) {
            var5.getPlot().onBlockChange(this.sable$blockSet, var2);
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
   private BlockState sable$setBlockState(LevelChunkSection var1, int var2, int var3, int var4, BlockState var5, Operation<BlockState> var6) {
      BlockState var7 = (BlockState)var6.call(var1, var2, var3, var4, var5);
      if (this.level instanceof ServerLevel var8 && var7 != var5 && this.sable$blockSet != null) {
         var2 = this.sable$blockSet.getX();
         var3 = this.sable$blockSet.getY();
         var4 = this.sable$blockSet.getZ();
         SableCommonEvents.handleBlockChange(var8, (LevelChunk)(Object)this, var2, var3, var4, var7, var5);
      }

      return var7;
   }

   @Unique
   private boolean sable$isPlotChunk() {
      SubLevelContainer var1 = SubLevelContainer.getContainer(this.level);
      return var1 != null && var1.inBounds(((LevelChunk)(Object)this).getPos());
   }

   @WrapOperation(
      method = {"replaceWithPacketData"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/chunk/LevelChunk;clearAllBlockEntities()V"
      )}
   )
   private void sable$keepPlotBlockEntities(LevelChunk var1, Operation<Void> var2) {
      if (!this.sable$isPlotChunk()) {
         var2.call(var1);
      }
   }

   @WrapOperation(
      method = {"replaceWithPacketData"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V"
      )}
   )
   private void sable$captureWantedBlockEntities(Consumer var1, final Object var2, Operation<Void> var3) {
      if (!this.sable$isPlotChunk()) {
         var3.call(var1, var2);
      } else {
         final HashMap var4 = new HashMap();
         this.sable$wantedBlockEntities = var4;
         BlockEntityTagOutput var5 = new BlockEntityTagOutput() {
            {
               Objects.requireNonNull(LevelChunkMixin.this);
            }

            @Override
            public void accept(BlockPos var1, BlockEntityType<?> var2x, CompoundTag var3x) {
               var4.put(var1.immutable(), var2x);
               ((BlockEntityTagOutput)var2).accept(var1, var2x, var3x);
            }
         };
         var3.call(var1, var5);
      }
   }

   @Inject(
      method = {"replaceWithPacketData"},
      at = {@At("RETURN")}
   )
   private void sable$resyncBlockStates(FriendlyByteBuf var1, Map<?, ?> var2, Consumer<?> var3, CallbackInfo var4) {
      if (this.sable$isPlotChunk()) {
         LevelChunk var5 = (LevelChunk)(Object)this;
         Map var6 = this.sable$wantedBlockEntities;
         this.sable$wantedBlockEntities = null;
         if (var6 != null) {
            for (BlockPos var8 : new ArrayList<>(var5.getBlockEntities().keySet())) {
               BlockEntity var9 = var5.getBlockEntity(var8);
               if (var9 != null && var6.get(var8) != var9.getType()) {
                  var5.removeBlockEntity(var8);
               }
            }
         }

         for (Entry var12 : var5.getBlockEntities().entrySet()) {
            BlockEntity var13 = (BlockEntity)var12.getValue();
            BlockState var10 = var5.getBlockState((BlockPos)var12.getKey());
            if (var13.getBlockState() != var10 && var13.isValidBlockState(var10)) {
               var13.setBlockState(var10);
            }
         }
      }
   }
}
