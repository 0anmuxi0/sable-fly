package dev.ryanhcode.sable.mixin.world_border;

import dev.ryanhcode.sable.mixinterface.world_border.WorldBorderExtension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerLevel.class})
public class ServerLevelMixin {
   @Inject(
      method = {"<init>"},
      at = {@At("TAIL")}
   )
   private void sable$linkWorldBorder(CallbackInfo ci) {
      ServerLevel self = (ServerLevel)(Object)this;
      ((WorldBorderExtension)self.getWorldBorder()).sable$setLevel((Level)(Object)this);
   }
}
