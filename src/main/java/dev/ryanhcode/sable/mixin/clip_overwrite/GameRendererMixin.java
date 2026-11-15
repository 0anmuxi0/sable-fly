package dev.ryanhcode.sable.mixin.clip_overwrite;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Minecraft.class})
public class GameRendererMixin {
   @WrapMethod(
      method = {"pick(F)V"}
   )
   private void sable$pickWithRenderPoses(float partialTick, Operation<Void> original) {
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft.level == null) {
         original.call(partialTick);
      } else {
         LevelPoseProviderExtension extension = (LevelPoseProviderExtension)minecraft.level;
         extension.sable$pushPoseSupplier(subLevel -> ((ClientSubLevel)subLevel).renderPose(partialTick));
         original.call(partialTick);
         extension.sable$popPoseSupplier();
      }
   }
}
