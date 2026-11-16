package dev.ryanhcode.sable.mixin.camera.camera_zoom;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import dev.ryanhcode.sable.mixinterface.camera.camera_zoom.CameraZoomExtension;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Collection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public abstract class CameraMixin implements CameraZoomExtension {
   @Shadow
   private Level level;
   @Shadow
   private Vec3 position;
   @Shadow
   @Final
   private Vector3f forwards;
   @Shadow
   private Entity entity;
   @Unique
   private boolean sable$pushed = false;
   @Unique
   private float sable$zoomAmount;
   @Unique
   private float sable$interpolatedZoom;
   @Unique
   private float sable$lastInterpolatedZoom;

   @Shadow
   protected abstract void setPosition(double x, double y, double z);

   @Inject(
      method = {"tick"},
      at = {@At("HEAD")}
   )
   private void sable$preTick(CallbackInfo ci) {
      this.sable$lastInterpolatedZoom = this.sable$interpolatedZoom;
      this.sable$interpolatedZoom = Mth.lerp(0.725F, this.sable$interpolatedZoom, this.sable$zoomAmount);
   }

   @Inject(
      method = {"alignWithEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/Camera;setPosition(DDD)V",
         shift = Shift.AFTER
      )}
   )
   private void sable$setup(float partialTicks, CallbackInfo ci) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.options.getCameraType() == SableCameraTypes.SUB_LEVEL_VIEW || minecraft.options.getCameraType() == SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED
         )
       {
         Entity cameraEntity = minecraft.getCameraEntity();
         Entity vehicle = cameraEntity.getVehicle();
         if (vehicle != null && Sable.HELPER.getContaining(minecraft.level, vehicle.position()) instanceof ClientSubLevel clientSubLevel) {
            Vector3dc pos = clientSubLevel.renderPose().position();
            this.setPosition(pos.x(), pos.y(), pos.z());
         }
      }
   }

   @Unique
   private float sable$clampZoom(float maxZoom, SubLevel ignoredSubLevel) {
      float zoom = maxZoom;
      float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
      Level level = this.entity.level();
      LevelPoseProviderExtension extension = (LevelPoseProviderExtension)this.level;

      assert extension != null;

      Collection<SubLevel> ignoredChain = SubLevelHelper.getConnectedChain(ignoredSubLevel);
      extension.sable$pushPoseSupplier(subLevel -> ((ClientSubLevel)subLevel).renderPose(partialTick));

      for (int i = 0; i < 8; i++) {
         float offsetX = (i & 1) * 2 - 1;
         float offsetY = (i >> 1 & 1) * 2 - 1;
         float offsetZ = (i >> 2 & 1) * 2 - 1;
         Vec3 vec3 = this.position.add(offsetX * 0.1F, offsetY * 0.1F, offsetZ * 0.1F);
         Vec3 vec32 = vec3.add(new Vec3(this.forwards).scale(-zoom));
         ClipContext clipContext = new ClipContext(vec3, vec32, Block.VISUAL, Fluid.NONE, this.entity);
         ((ClipContextExtension)clipContext).sable$setSubLevelIgnoring(ignoredChain::contains);
         HitResult hitResult = this.level.clip(clipContext);
         if (hitResult.getType() != Type.MISS) {
            float l = (float)Sable.HELPER.distanceSquaredWithSubLevels(level, hitResult.getLocation(), this.position);
            if (l < Mth.square(zoom)) {
               zoom = Mth.sqrt(l);
            }
         }
      }

      extension.sable$popPoseSupplier();
      return zoom;
   }

   @Inject(
      method = {"getMaxZoom"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$getMaxZoomHead(float f, CallbackInfoReturnable<Float> cir) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.options.getCameraType() == SableCameraTypes.SUB_LEVEL_VIEW || minecraft.options.getCameraType() == SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED
         )
       {
         Entity cameraEntity = minecraft.getCameraEntity();
         Entity vehicle = cameraEntity.getVehicle();
         boolean isTypeValid = vehicle != null;
         if (isTypeValid) {
            SubLevel subLevel = Sable.HELPER.getContaining(minecraft.level, vehicle.position());
            if (subLevel != null) {
               float partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
               float zoomAmount = Mth.lerp(partialTick, this.sable$lastInterpolatedZoom, this.sable$interpolatedZoom);
               BoundingBox3ic boundingBox = subLevel.getPlot().getBoundingBox();
               Vec3 extents = new Vec3(
                  boundingBox.maxX() - boundingBox.minX(), boundingBox.maxY() - boundingBox.minY(), boundingBox.maxZ() - boundingBox.minZ()
               );
               double maxDist = extents.scale(0.5).length();
               float desiredDistance = (float)Math.max((double)f, maxDist) * (1.75F + zoomAmount);
               cir.setReturnValue(this.sable$clampZoom(desiredDistance, subLevel));
               this.sable$pushed = false;
               return;
            }
         }
      }

      LevelPoseProviderExtension extension = (LevelPoseProviderExtension)minecraft.level;

      assert extension != null;

      extension.sable$pushPoseSupplier(subLevelx -> ((ClientSubLevel)subLevelx).renderPose(minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
      this.sable$pushed = true;
   }

   @Redirect(
      method = {"getMaxZoom"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )}
   )
   private double sable$getMaxZoom(Vec3 instance, Vec3 vec3) {
      return Sable.HELPER.distanceSquaredWithSubLevels(this.entity.level(), instance, vec3);
   }

   @Inject(
      method = {"getMaxZoom"},
      at = {@At("RETURN")}
   )
   private void sable$getMaxZoomTail(float f, CallbackInfoReturnable<Float> cir) {
      if (this.sable$pushed) {
         LevelPoseProviderExtension extension = (LevelPoseProviderExtension)Minecraft.getInstance().level;

         assert extension != null;

         extension.sable$popPoseSupplier();
         this.sable$pushed = false;
      }
   }

   @Override
   public float sable$getZoomAmount() {
      return this.sable$zoomAmount;
   }

   @Override
   public void sable$setZoomAmount(float sable$zoomAmount) {
      this.sable$zoomAmount = Mth.clamp(sable$zoomAmount, 0.0F, 4.0F);
   }
}
