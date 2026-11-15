package dev.ryanhcode.sable.sublevel.render;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SubLevelMovingBlockRenderState extends MovingBlockRenderState {
   @Nullable
   private Level level;

   public void sable$setLevel(Level level) {
      this.level = level;
   }

   @Override
   public BlockState getBlockState(BlockPos pos) {
      return this.level == null ? super.getBlockState(pos) : this.level.getBlockState(pos);
   }
}
