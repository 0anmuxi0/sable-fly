package dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({ServerGamePacketListenerImpl.class})
public class ServerGamePacketListenerImplMixin {
   @Shadow
   public ServerPlayer player;

   @Redirect(require = 0, method = {"handlePlayerPositionChange"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerPlayer;isCreative()Z"
      )}
   )
   private boolean sable$ignoreCreativeModeForSubLevelCollision(ServerPlayer instance) {
      return true;
   }
}
