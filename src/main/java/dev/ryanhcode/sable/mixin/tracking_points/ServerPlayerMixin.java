package dev.ryanhcode.sable.mixin.tracking_points;

import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
   @Shadow
   public abstract ServerLevel level();

   @Inject(
      method = {"addAdditionalSaveData"},
      at = {@At("TAIL")}
   )
   private void addAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
      SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad(this.level());
      UUID loginPointUUID = data.generateTrackingPoint((ServerPlayer)(Object)this);
      if (loginPointUUID != null) {
         output.store("LoginPoint", UUIDUtil.CODEC, loginPointUUID);
      }
   }
}
