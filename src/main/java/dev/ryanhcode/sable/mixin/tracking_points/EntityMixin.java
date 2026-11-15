package dev.ryanhcode.sable.mixin.tracking_points;

import dev.ryanhcode.sable.mixinterface.player_freezing.PlayerFreezeExtension;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Shadow
   private Level level;

   @Shadow
   public abstract void setPosRaw(double var1, double var3, double var5);

   @Inject(
      method = {"load"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setPosRaw(DDD)V",
         shift = Shift.AFTER
      )}
   )
   private void sable$load(ValueInput input, CallbackInfo ci) {
      Optional<UUID> loginPoint = input.read("LoginPoint", UUIDUtil.CODEC);
      if (loginPoint.isPresent()) {
         SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad((ServerLevel)this.level);
         SubLevelTrackingPointSavedData.TakenLoginPoint point = data.take(loginPoint.get(), true);
         if (point != null) {
            Vector3dc position = point.position();
            this.setPosRaw(position.x(), position.y(), position.z());
            if (point.subLevelId() != null && this instanceof PlayerFreezeExtension extension) {
               extension.sable$freezeTo(point.subLevelId(), point.localAnchor().add(0.0, 0.2, 0.0));
            }
         }
      }
   }
}
