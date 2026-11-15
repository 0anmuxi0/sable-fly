package dev.ryanhcode.sable.fabric.mixin.compatibility.zume;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.ryanhcode.sable.annotation.MixinModVersionConstraint;
import dev.ryanhcode.sable.mixinhelpers.camera.new_camera_types.SableCameraTypes;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@MixinModVersionConstraint("[1.2.1,1.2.2]")
@Pseudo
@Mixin(
   targets = {"zume.K"},
   remap = false
)
public abstract class ModernZumeMixin {
   @ModifyExpressionValue(
      method = {"d()Lzume/i;"},
      at = {@At(
         value = "INVOKE",
         target = "Ljava/lang/Enum;ordinal()I",
         remap = false
      )},
      remap = false
   )
   private int sable$mapSubLevelCameraPerspective(int ordinal) {
      CameraType cameraType = Minecraft.getInstance().options.getCameraType();
      return cameraType != SableCameraTypes.SUB_LEVEL_VIEW && cameraType != SableCameraTypes.SUB_LEVEL_VIEW_UNLOCKED
         ? ordinal
         : CameraType.THIRD_PERSON_BACK.ordinal();
   }
}
