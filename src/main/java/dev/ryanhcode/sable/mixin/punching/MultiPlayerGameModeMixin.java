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
   private void onBlockBreakStart(BlockPos var1, Direction var2, CallbackInfoReturnable<Boolean> var3) {
      assert this.minecraft.player != null;

      if (this.minecraft.hitResult instanceof BlockHitResult var5) {
         ClientSubLevelPunchHelper.clientTryPunch(var5, this.minecraft.level, true);
      }
   }

   @Inject(
      method = {"useItemOn"},
      at = {@At("RETURN")}
   )
   private void sable$swingOnSubLevelUseOn(LocalPlayer var1, InteractionHand var2, BlockHitResult var3, CallbackInfoReturnable<InteractionResult> var4) {
      ClientLevel var6 = Minecraft.getInstance().level;
      if (var6 != null) {
         if (Sable.HELPER.getContaining(var6, var3.getBlockPos()) != null) {
            InteractionResult var7 = (InteractionResult)var4.getReturnValue();
            SwingSource var8;
            if (var7 instanceof Success var9) {
               var8 = var9.swingSource();
            } else {
               SwingSource var5 = SwingSource.NONE;
               var8 = SwingSource.NONE;
            }

            if (var8 == SwingSource.NONE) {
               ItemStack var14 = var1.getItemInHand(var2);
               if (var14.getItem() instanceof BlockItem var11) {
                  if (new BlockPlaceContext(var1, var2, var14, var3).canPlace()) {
                     BlockPos var12 = var3.getBlockPos().relative(var3.getDirection());
                     AABB var13 = new AABB(var12.getX(), var12.getY(), var12.getZ(), var12.getX() + 1.0, var12.getY() + 1.0, var12.getZ() + 1.0);
                     if (var6.getEntities((Entity)null, var13).isEmpty()) {
                        var1.swing(var2, false);
                     }
                  }
               }
            }
         }
      }
   }
}
