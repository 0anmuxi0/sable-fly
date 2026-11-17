package dev.ryanhcode.sable.mixin.punching;

import dev.ryanhcode.sable.client.rope.RopeManager;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.network.client.ClientSubLevelPunchHelper;
import dev.ryanhcode.sable.network.packets.tcp.ServerboundRemoveRopePacket;
import dev.ryanhcode.sable.network.tcp.SablePacketSink;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult.Type;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Minecraft.class})
public abstract class MinecraftMixin {
   @Shadow
   @org.jetbrains.annotations.Nullable
   public LocalPlayer player;
   @Shadow
   @org.jetbrains.annotations.Nullable
   public ClientLevel level;

   @Shadow
   @org.jetbrains.annotations.Nullable
   public abstract ClientPacketListener getConnection();

   @Inject(require = 0, method = {"startAttack"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$attackRope(CallbackInfoReturnable<Boolean> cir) {
      if (this.player != null) {
         UUID rope = RopeManager.pick(this.player.getEyePosition(), this.player.getLookAngle(), this.player.blockInteractionRange());
         if (rope != null) {
            RopeManager.remove(rope);
            SablePacketSink.server().sendPacket(new ServerboundRemoveRopePacket(rope));
            cir.setReturnValue(false);
         }
      }
   }

   @Inject(require = 0, method = {"startAttack"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/player/LocalPlayer;resetAttackStrengthTicker()V"
      )}
   )
   private void tryPaddling(CallbackInfoReturnable<Boolean> cir) {
      if (this.player.getMainHandItem().is(SableTags.PADDLES) && !this.player.getCooldowns().isOnCooldown(this.player.getMainHandItem())) {
         BlockHitResult hitResult = ItemInvoker.sable$getPlayerPOVHitResult(this.level, this.player, Fluid.ANY);
         if (hitResult.getType() == Type.BLOCK) {
            FluidState state = this.level.getFluidState(hitResult.getBlockPos());
            if (!state.isEmpty()) {
               ClientSubLevelPunchHelper.clientTryPunch(hitResult, this.level, false);
            }
         }
      }
   }
}
