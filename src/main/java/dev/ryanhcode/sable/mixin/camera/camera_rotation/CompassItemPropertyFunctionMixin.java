package dev.ryanhcode.sable.mixin.camera.camera_rotation;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.item.properties.numeric.CompassAngleState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin({CompassAngleState.class})
public abstract class CompassItemPropertyFunctionMixin {
   @Overwrite
   private static double getAngleFromEntityToPos(ItemOwner owner, BlockPos pos) {
      Vec3 localPos = Vec3.atCenterOf(pos);
      Vec3 ownerPosition = owner.position();
      double entityX = ownerPosition.x();
      double entityZ = ownerPosition.z();
      ActiveSableCompanion helper = Sable.HELPER;
      LivingEntity entity = owner.asLivingEntity();
      SubLevel subLevel = entity != null ? helper.getContaining(entity) : helper.getContaining(owner.level(), ownerPosition);
      if (subLevel == null && entity != null) {
         Entity vehicle = entity.getVehicle();
         if (vehicle != null) {
            subLevel = helper.getContaining(vehicle);
            if (subLevel != null) {
               Vec3 localEntityPos = subLevel.lastPose().transformPositionInverse(ownerPosition);
               entityX = localEntityPos.x;
               entityZ = localEntityPos.z;
            }
         }
      }

      if (subLevel != null) {
         localPos = subLevel.lastPose().transformPositionInverse(localPos);
      }

      return Math.atan2(localPos.z() - entityZ, localPos.x() - entityX) / (float) (Math.PI * 2);
   }
}
