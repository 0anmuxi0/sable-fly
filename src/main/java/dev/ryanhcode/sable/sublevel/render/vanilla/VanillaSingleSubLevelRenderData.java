package dev.ryanhcode.sable.sublevel.render.vanilla;

import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import java.util.Objects;
import net.minecraft.client.Camera;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class VanillaSingleSubLevelRenderData implements SubLevelRenderData {
   private final ClientSubLevel subLevel;
   private BlockState singleBlockState = null;
   private BlockPos singleBlockPos = null;
   private long singleBlockSeed = 42L;
   private BlockEntity singleBlockEntity = null;

   public VanillaSingleSubLevelRenderData(ClientSubLevel subLevel) {
      this.subLevel = subLevel;
      this.rebuild();
   }

   private void handleBlockEntity(@Nullable BlockEntity blockEntity) {
      if (!Objects.equals(this.singleBlockEntity, blockEntity)) {
         this.singleBlockEntity = blockEntity;
      }
   }

   @Nullable
   public BlockEntity getRenderBlockEntity() {
      if (this.singleBlockState.isAir()) {
         this.rebuild();
      }

      return this.singleBlockEntity;
   }

   @Override
   public void rebuild() {
      BoundingBox3ic bounds = this.subLevel.getPlot().getBoundingBox();
      BlockPos pos = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
      BlockState blockState = this.subLevel.getLevel().getBlockState(pos);
      this.singleBlockState = blockState;
      this.singleBlockPos = pos;
      this.singleBlockSeed = blockState.getSeed(pos);
      this.handleBlockEntity(blockState.hasBlockEntity() ? this.subLevel.getLevel().getBlockEntity(pos) : null);
   }

   @Override
   public void compileSections(PrioritizeChunkUpdates chunkUpdates, RenderRegionCache renderRegionCache, Camera camera) {
   }

   @Override
   public int getVisibleSectionCount() {
      return 1;
   }

   @Override
   public ClientSubLevel getSubLevel() {
      return this.subLevel;
   }

   @Override
   public void setDirty(int x, int y, int z, boolean playerChanged) {
      this.rebuild();
   }

   @Override
   public boolean isSectionCompiled(int x, int y, int z) {
      return true;
   }

   @Override
   public void close() {
      this.singleBlockEntity = null;
   }
}
