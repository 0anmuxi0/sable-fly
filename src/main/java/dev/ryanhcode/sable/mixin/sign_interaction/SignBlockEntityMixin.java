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
   public SignBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
      super(blockEntityType, blockPos, blockState);
   }

   @Overwrite
   public boolean isFacingFrontText(Player player) {
      BlockState state = this.getBlockState();
      if (state.getBlock() instanceof SignBlock signBlock) {
         Vec3 signCenterPos = signBlock.getSignHitboxCenterPosition(state);
         BlockPos pos = this.getBlockPos();
         double signX = pos.getX() + signCenterPos.x;
         double signZ = pos.getZ() + signCenterPos.z;
         ActiveSableCompanion helper = Sable.HELPER;
         SubLevel subLevel = helper.getContaining(this.level, pos);
         double playerX = player.getX();
         double playerZ = player.getZ();
         if (subLevel != null) {
            Vec3 localPlayerPos = subLevel.logicalPose().transformPositionInverse(new Vec3(playerX, player.getY(), playerZ));
            playerX = localPlayerPos.x;
            playerZ = localPlayerPos.z;
            Vec3 localSignPos = subLevel.logicalPose().transformPositionInverse(new Vec3(signX, pos.getY() + signCenterPos.y, signZ));
            signX = localSignPos.x;
            signZ = localSignPos.z;
         }

         double deltaX = playerX - signX;
         double deltaZ = playerZ - signZ;
         float signYRot = signBlock.getYRotationDegrees(state);
         float playerAngle = (float)(Mth.atan2(deltaZ, deltaX) * 180.0 / (float) Math.PI) - 90.0F;
         return Mth.degreesDifferenceAbs(signYRot, playerAngle) <= 90.0F;
      } else {
         return false;
      }
   }
}
