package dev.ryanhcode.sable.physics.impl.rapier.collider;

import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class PhysicsColliderBlockGetter implements BlockGetter {
   private final BlockGetter level;
   private BlockState state;

   public PhysicsColliderBlockGetter(BlockGetter level) {
      this.level = level;
      this.state = Blocks.AIR.defaultBlockState();
   }

   public void setup(BlockState state) {
      this.state = Objects.requireNonNull(state, "state");
   }

   @Nullable
   @Override
   public BlockEntity getBlockEntity(@NotNull BlockPos pos) {
      return null;
   }

   @NotNull
   @Override
   public BlockState getBlockState(@NotNull BlockPos pos) {
      return BlockPos.ZERO.equals(pos) ? this.state : Blocks.AIR.defaultBlockState();
   }

   @NotNull
   @Override
   public FluidState getFluidState(@NotNull BlockPos pos) {
      return BlockPos.ZERO.equals(pos) ? this.state.getFluidState() : Fluids.EMPTY.defaultFluidState();
   }

   @Override
   public int getHeight() {
      return this.level.getHeight();
   }

   @Override
   public int getMinY() {
      return this.level.getMinY();
   }
}
