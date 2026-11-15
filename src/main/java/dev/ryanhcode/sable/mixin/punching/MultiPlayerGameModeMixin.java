package dev.ryanhcode.sable.mixin.punching;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.network.client.ClientSubLevelPunchHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.Success;
import net.minecraft.world.InteractionResult.SwingSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({MultiPlayerGameMode.class})
public class MultiPlayerGameModeMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"startDestroyBlock"},
      at = {@At("HEAD")}
   )
   private void onBlockBreakStart(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
      assert this.minecraft.player != null;

      if (this.minecraft.hitResult instanceof BlockHitResult blockHitResult) {
         ClientSubLevelPunchHelper.clientTryPunch(blockHitResult, this.minecraft.level, true);
      }
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At("RETURN")}
   )
   private void sable$swingOnSubLevelUseOn(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         if (Sable.HELPER.getContaining(level, hitResult.getBlockPos()) != null) {
            InteractionResult result = cir.getReturnValue();
            SwingSource swing = result instanceof Success success ? success.swingSource() : SwingSource.NONE;
            if (swing == SwingSource.NONE) {
               ItemStack stack = player.getItemInHand(hand);
               if (stack.getItem() instanceof BlockItem blockItem) {
                  if (new BlockPlaceContext(player, hand, stack, hitResult).canPlace()) {
                     BlockPos placedPos = hitResult.getBlockPos().relative(hitResult.getDirection());
                     AABB placedBox = new AABB(
                        placedPos.getX(), placedPos.getY(), placedPos.getZ(), placedPos.getX() + 1.0, placedPos.getY() + 1.0, placedPos.getZ() + 1.0
                     );
                     if (level.getEntities((Entity)null, placedBox).isEmpty()) {
                        player.swing(hand, stack.getInteractAnimation(), false);
                     }
                  }
               }
            }
         }
      }
   }
}
