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

@Mixin({GameRenderer.class})
public class GameRendererMixin {
   @Shadow
   @Final
   private Minecraft minecraft;

   @Inject(
      method = {"extractCamera"},
      at = {@At("HEAD")}
   )
   public void sable$setupCamera(DeltaTracker var1, float var2, float var3, CallbackInfo var4) {
      CameraType var5 = this.minecraft.options.getCameraType();
      if (var5 == SableCameraTypes.SUB_LEVEL_VIEW || var5 == SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED) {
         Entity var6 = this.minecraft.getCameraEntity().getVehicle();
         if (var6 != null) {
            SubLevel var7 = Sable.HELPER.getContaining(this.minecraft.level, var6.position());
            if (var7 != null) {
               return;
            }
         }

         this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);
      }
   }
}
