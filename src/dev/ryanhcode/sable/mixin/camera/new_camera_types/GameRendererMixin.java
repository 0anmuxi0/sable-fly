package dev.ryanhcode.sable.mixin.camera.new_camera_types;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.2 port.
 *
 * 26.2's {@code GameRenderer.extractCamera} is {@code (DeltaTracker, float, float) -> void} - it takes TWO floats
 * (verified by javap). The handler must mirror both, otherwise Mixin rejects it with "Invalid descriptor".
 */
@Mixin({GameRenderer.class})
public class GameRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(method = {"extractCamera"}, at = {@At("HEAD")})
   public void sable$setupCamera(DeltaTracker deltaTracker, float partialTick, float unusedSecondFloat, CallbackInfo ci) {
      CameraType cameraType = this.minecraft.options.getCameraType();
      if (cameraType == SableCameraTypes.SUB_LEVEL_VIEW || cameraType == SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED) {
         Entity vehicle = this.minecraft.getCameraEntity().getVehicle();
         if (vehicle != null) {
            SubLevel subLevel = Sable.HELPER.getContaining(this.minecraft.level, vehicle.position());
            if (subLevel != null) {
               return;
            }
         }

         this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);
      }
   }
}
