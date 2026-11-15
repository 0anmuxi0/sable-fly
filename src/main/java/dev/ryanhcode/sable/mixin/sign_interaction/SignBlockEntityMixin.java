package dev.ryanhcode.sable.mixin.sign_interaction;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({SignBlockEntity.class})
public abstract class SignBlockEntityMixin extends BlockEntity {
   public SignBlockEntityMixin(BlockEntityType<?> var1, BlockPos var2, BlockState var3) {
      super(var1, var2, var3);
   }

   @Overwrite
   public boolean isFacingFrontText(Player var1) {
      BlockState var2 = this.getBlockState();
      if (var2.getBlock() instanceof SignBlock var3) {
         Vec3 var22 = var3.getSignHitboxCenterPosition(var2);
         BlockPos var5 = this.getBlockPos();
         double var6 = var5.getX() + var22.x;
         double var8 = var5.getZ() + var22.z;
         ActiveSableCompanion var10 = Sable.HELPER;
         SubLevel var11 = var10.getContaining(this.level, var5);
         double var12 = var1.getX();
         double var14 = var1.getZ();
         if (var11 != null) {
            Vec3 var16 = var11.logicalPose().transformPositionInverse(new Vec3(var12, var1.getY(), var14));
            var12 = var16.x;
            var14 = var16.z;
            Vec3 var17 = var11.logicalPose().transformPositionInverse(new Vec3(var6, var5.getY() + var22.y, var8));
            var6 = var17.x;
            var8 = var17.z;
         }

         double var23 = var12 - var6;
         double var18 = var14 - var8;
         float var20 = var3.getYRotationDegrees(var2);
         float var21 = (float)(Mth.atan2(var18, var23) * 180.0 / (float) Math.PI) - 90.0F;
         return Mth.degreesDifferenceAbs(var20, var21) <= 90.0F;
      } else {
         return false;
      }
   }
}
