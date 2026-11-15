package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_riding_sub_level_vehicle.EntityRidingSubLevelVehicleHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityType.class})
public class EntityTypeMixin {
   @Inject(
      method = {"loadPassengersRecursive"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z"
      )}
   )
   private static void sable$startRidingEntity(
      Entity var0,
      ValueInput var1,
      Level var2,
      EntitySpawnRequest var3,
      EntityProcessor var4,
      CallbackInfoReturnable<Entity> var5,
      @Local(ordinal = 1) Entity var6
   ) {
      SubLevel var7 = Sable.HELPER.getContaining(var0);
      if (var7 != null && EntitySubLevelUtil.shouldKick(var6)) {
         Vec3 var8 = EntityRidingSubLevelVehicleHelper.kickRidingEntity(var6, var7);
         var6.setPos(var8);
      }
   }
}
