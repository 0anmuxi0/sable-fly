package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import com.zurrtum.create.client.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.catnip.outliner.ItemOutline"}
)
public class ItemOutlineMixin {
   @Shadow
   protected Vec3 pos;

   @WrapMethod(
      method = {"render(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/zurrtum/create/client/catnip/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;F)V"}
   )
   private void sable$projectOutlineOntoSubLevel(Minecraft mc, PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, float pt, Operation<Void> original) {
      ClientSubLevel subLevel = SubLevelOutlinePose.find(this.pos);
      if (subLevel == null) {
         original.call(mc, ms, buffer, camera, pt);
      } else {
         SubLevelOutlinePose.runProjected(ms, subLevel, camera, plotCamera -> original.call(mc, ms, buffer, plotCamera, pt));
      }
   }
}
