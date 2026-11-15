package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.LivingEntityStickExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin extends Entity implements LivingEntityStickExtension {
   @Unique
   private double sable$lerpYRot;
   @Unique
   private double sable$lerpXRot;
   @Unique
   private Vec3 sable$lerpTarget = Vec3.ZERO;
   @Unique
   private int sable$sableLerpSteps;
   @Unique
   private int sable$sableRotLerpSteps;

   public LivingEntityMixin(EntityType<?> var1, Level var2) {
      super(var1, var2);
   }

   @Shadow
   protected abstract void updateWalkAnimation(float var1);

   @Unique
   private InterpolationHandler sable$interpolationHandler() {
      return this.getInterpolation();
   }

   @Override
   public void sable$setupLerp() {
      InterpolationHandler var1 = this.sable$interpolationHandler();
      if (var1 != null) {
         if (this.sable$getPlotPosition() != null && var1.hasActiveInterpolation()) {
            this.sable$lerpYRot = var1.yRot();
            this.sable$lerpXRot = var1.xRot();
            this.sable$sableRotLerpSteps = 3;
            var1.cancel();
         }
      }
   }

   @Override
   public void sable$applyLerp() {
      Vec3 var1 = this.sable$getPlotPosition();
      if (var1 == null) {
         this.sable$sableLerpSteps = 0;
         this.sable$sableRotLerpSteps = 0;
      } else {
         if (this.sable$sableLerpSteps > 0) {
            this.sable$setPlotPosition(var1.lerp(this.sable$lerpTarget, 1.0 / this.sable$sableLerpSteps));
            this.sable$sableLerpSteps--;
         }

         if (this.sable$sableRotLerpSteps > 0) {
            double var2 = Mth.wrapDegrees(this.sable$lerpYRot - this.getYRot());
            this.setYRot(this.getYRot() + (float)var2 / this.sable$sableRotLerpSteps);
            this.setXRot(this.getXRot() + (float)(this.sable$lerpXRot - this.getXRot()) / this.sable$sableRotLerpSteps);
            this.sable$sableRotLerpSteps--;
            this.setRot(this.getYRot(), this.getXRot());
         }
      }
   }

   @Override
   public Vec3 sable$getLerpTarget() {
      return this.sable$lerpTarget;
   }

   @Inject(
      method = {"aiStep"},
      at = {@At("HEAD")}
   )
   private void sable$updateRotLerp(CallbackInfo var1) {
      this.sable$setupLerp();
   }

   @Inject(
      method = {"aiStep"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V",
         shift = Shift.BEFORE
      )}
   )
   private void sable$updatePlotPosition(CallbackInfo var1) {
      this.sable$applyLerp();
   }

   @Override
   public void sable$plotLerpTo(Vec3 var1, int var2) {
      this.sable$lerpTarget = var1;
      this.sable$sableLerpSteps = var2;
   }

   @ModifyVariable(
      method = {"tick"},
      at = {@At("STORE")},
      ordinal = 0
   )
   private double sable$modifyXDifference(double var1) {
      return this.sable$getDifference(true).x;
   }

   @ModifyVariable(
      method = {"tick"},
      at = {@At("STORE")},
      ordinal = 1
   )
   private double sable$modifyZDifference(double var1) {
      return this.sable$getDifference(true).z;
   }

   @Redirect(
      method = {"calculateEntityAnimation"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;updateWalkAnimation(F)V"
      )}
   )
   private void sable$walkAnimation(LivingEntity var1, float var2, boolean var3) {
      Vec3 var4 = this.sable$getDifference(false);
      float var5 = (float)Mth.length(var4.x, var3 ? var4.y : 0.0, var4.z);
      this.updateWalkAnimation(var5);
   }

   @Unique
   private Vec3 sable$getDifference(boolean var1) {
      Vec3 var2 = this.position();
      Vec3 var3 = new Vec3(this.xo, this.yo, this.zo);
      Vec3 var4 = var2.subtract(var3);
      SubLevel var5 = Sable.HELPER.getTrackingSubLevel(this);
      Entity var6 = this;
      if (var5 != null && (!(!var1 && var6 instanceof Player var7) || !var7.isLocalPlayer())) {
         Pose3d var8 = var5.logicalPose();
         Pose3dc var9 = var5.lastPose();
         var2 = var8.transformPositionInverse(var2);
         var3 = var9.transformPositionInverse(var3);
         var4 = var2.subtract(var3);
         var4 = var8.transformNormal(var4);
      }

      Quaterniondc var13 = EntitySubLevelUtil.getCustomEntityOrientation(this, 1.0F);
      if (var13 != null) {
         var4 = JOMLConversion.toMojang(var13.transformInverse(JOMLConversion.toJOML(var4)));
      }

      return var4;
   }

   @Inject(
      method = {"recreateFromPacket"},
      at = {@At("TAIL")}
   )
   public void sable$recreateFromPacket(ClientboundAddEntityPacket var1, CallbackInfo var2) {
      if (EntitySubLevelUtil.shouldKick(this)) {
         double var3 = var1.getX();
         double var5 = var1.getY();
         double var7 = var1.getZ();
         SubLevel var9 = Sable.HELPER.getContaining(this.level(), var3, var7);
         if (var9 != null) {
            Vector3d var10 = var9.logicalPose().transformPosition(new Vector3d(var3, var5, var7));
            this.setPos(var10.x, var10.y, var10.z);
         }
      }
   }
}
