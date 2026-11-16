package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_riding_sub_level_vehicle.EntityRidingSubLevelVehicleHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Shadow
   private Level level;
   @Shadow
   @Nullable
   private Entity vehicle;
   @Shadow
   private Vec3 position;

   @Shadow
   public abstract void setPos(Vec3 pos);

   @Shadow
   public abstract boolean hasPassenger(Entity entity);

   @Shadow
   public abstract Vec3 position();

   @Shadow
   public abstract double getX();

   @Shadow
   public abstract double getY();

   @Shadow
   public abstract double getZ();

   @Shadow
   public abstract Level level();

   @Shadow
   @Nullable
   public abstract Entity getVehicle();

   @Shadow
   public abstract Vec3 getLookAngle();

   @Shadow
   public abstract void lookAt(Anchor anchor, Vec3 target);

   @Shadow
   public abstract float getXRot();

   @Shadow
   public abstract float getYRot();

   @Shadow
   protected static Vec3 getInputVector(Vec3 vec3, float f, float g) {
      return null;
   }

   @Shadow
   public abstract void setDeltaMovement(Vec3 deltaMovement);

   @Shadow
   public abstract Vec3 getDeltaMovement();

   @Inject(
      method = {"moveRelative"},
      at = {@At("HEAD")},
      cancellable = true
   )
   public void moveRelative(float f, Vec3 vec3, CallbackInfo ci) {
      Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation((Entity)(Object)this, 1.0F);
      if (orientation != null) {
         Vec3 inputVector = getInputVector(vec3, f, this.getYRot());
         Vec3 impulse = JOMLConversion.toMojang(orientation.transform(JOMLConversion.toJOML(inputVector)));
         this.setDeltaMovement(this.getDeltaMovement().add(impulse));
         ci.cancel();
      }
   }

   @Inject(
      method = {"rideTick"},
      at = {@At("TAIL")}
   )
   public void sable$onRidingTick(CallbackInfo ci) {
      if (this.vehicle != null) {
         ActiveSableCompanion helper = Sable.HELPER;
         SubLevel vehicleSubLevel = helper.getContaining(this.vehicle);
         if (vehicleSubLevel != null) {
            if (helper.getContaining(this.level, this.position) == vehicleSubLevel) {
               Vec3 pos = EntityRidingSubLevelVehicleHelper.kickRidingEntity((Entity)(Object)this, vehicleSubLevel);
               this.setPos(pos);
            }
         }
      }
   }

   @Inject(
      method = {"positionRider(Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At("TAIL")}
   )
   public void sable$onPositionRider(Entity entity, CallbackInfo ci) {
      if (this.hasPassenger(entity)) {
         SubLevel subLevel = Sable.HELPER.getContaining(this.level, entity.position());
         if (subLevel != null) {
            Vec3 pos = EntityRidingSubLevelVehicleHelper.kickRidingEntity(entity, subLevel);
            entity.setPos(pos);
         }
      }
   }

   @WrapOperation(
      method = {"saveWithoutId"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/storage/ValueOutput;store(Ljava/lang/String;Lcom/mojang/serialization/Codec;Ljava/lang/Object;)V",
         ordinal = 0
      )}
   )
   private void sable$fixPassengerSaving(ValueOutput instance, String string, Codec<?> codec, Object value, Operation<Void> original) {
      if (!EntitySubLevelUtil.shouldKick((Entity)(Object)this)) {
         original.call(instance, string, codec, value);
      } else {
         SubLevel subLevel = Sable.HELPER.getContaining(this.vehicle);
         if (subLevel != null) {
            if ((Object)this instanceof ServerPlayer serverPlayer) {
               SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad((ServerLevel)this.level());
               UUID loginPointUUID = data.generateTrackingPoint(serverPlayer, (ServerSubLevel)subLevel);
               if (loginPointUUID != null) {
                  instance.store("LoginPoint", UUIDUtil.CODEC, loginPointUUID);
               }
            }

            original.call(instance, string, Vec3.CODEC, this.position());
         } else {
            original.call(instance, string, codec, value);
         }
      }
   }
}
