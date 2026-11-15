package dev.ryanhcode.sable.mixinhelpers.entity.entity_collision;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TheFasterEntityCollisionContext extends EntityCollisionContext {
   private final Entity entity;

   public TheFasterEntityCollisionContext(Entity entity) {
      super(false, false, 0.0, ItemStack.EMPTY, false, entity);
      this.entity = entity;
   }

   @Override
   public boolean isHoldingItem(Item item) {
      return this.entity instanceof LivingEntity livingEntity && livingEntity.getMainHandItem().is(item);
   }

   @Override
   public boolean canStandOnFluid(FluidState fluidState, FluidState fluidState2) {
      return this.entity instanceof LivingEntity livingEntity
         && livingEntity.canStandOnFluid(fluidState)
         && !fluidState.getType().isSame(fluidState2.getType());
   }

   @Override
   public boolean isDescending() {
      return this.entity.isDescending();
   }

   @Override
   public boolean isAbove(VoxelShape shape, BlockPos pos, boolean bl) {
      return this.entity.getY() > pos.getY() + shape.max(Axis.Y) - 1.0E-5F;
   }
}
