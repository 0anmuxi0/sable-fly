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

/**
 * 26.2 port of Sable's sub-level aware sign interaction.
 *
 * 26.3 overwrote {@code SignBlockEntity.getSlotPlayerIsFacing(Player) : SignTextSlot} to return
 * FRONT / BACK. {@code SignTextSlot} does not exist in 26.2; the exact same front/back decision
 * lives in {@code isFacingFrontText(Player) : boolean} (FRONT == true). We therefore @Overwrite
 * that boolean method and keep vanilla's own geometry
 * ({@code degreesDifferenceAbs(signYRot, atan2(zd, xd)*180/PI - 90) <= 90}) verbatim, with the
 * only difference being that the player position is brought into the sign's local space when the
 * sign sits inside a sub-level - which is precisely the behaviour 26.3 added.
 */
@Mixin({SignBlockEntity.class})
public abstract class SignBlockEntityMixin extends BlockEntity {
   public SignBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
      super(blockEntityType, blockPos, blockState);
   }

   @Overwrite
   public boolean isFacingFrontText(Player player) {
      BlockState state = this.getBlockState();
      if (!(state.getBlock() instanceof SignBlock sign)) {
         return false;
      }

      Vec3 signPositionOffset = sign.getSignHitboxCenterPosition(state);
      BlockPos pos = this.getBlockPos();

      double signCenterX = pos.getX() + signPositionOffset.x;
      double signCenterZ = pos.getZ() + signPositionOffset.z;

      // Sable: if the sign lives inside a sub-level, resolve the player position into that
      // sub-level's local space before doing vanilla's angle comparison.
      ActiveSableCompanion helper = Sable.HELPER;
      SubLevel subLevel = helper.getContaining(this.level, pos);
      double playerX = player.getX();
      double playerZ = player.getZ();
      if (subLevel != null) {
         Vec3 local = subLevel.logicalPose().transformPositionInverse(new Vec3(playerX, player.getY(), playerZ));
         playerX = local.x;
         playerZ = local.z;
         Vec3 localSignCenter = subLevel.logicalPose().transformPositionInverse(new Vec3(signCenterX, pos.getY() + signPositionOffset.y, signCenterZ));
         signCenterX = localSignCenter.x;
         signCenterZ = localSignCenter.z;
      }

      double xd = playerX - signCenterX;
      double zd = playerZ - signCenterZ;
      float signYRot = sign.getYRotationDegrees(state);
      float playerYRot = (float) (Mth.atan2(zd, xd) * 180.0F / (float) Math.PI) - 90.0F;
      return Mth.degreesDifferenceAbs(signYRot, playerYRot) <= 90.0F;
   }
}
