package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import com.zurrtum.create.client.catnip.render.SuperRenderTypeBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBox"}
)
public class ValueBoxMixin {
   @Shadow
   protected BlockPos pos;

   @WrapMethod(
      method = {"render(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/zurrtum/create/client/catnip/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;F)V"}
   )
   private void sable$submitAtWorldPos(Minecraft minecraft, PoseStack poseStack, SuperRenderTypeBuffer buffer, Vec3 position, float partialTicks, Operation<Void> operation) {
      ClientSubLevel subLevel = this.sable$getSubLevel(minecraft);
      if (subLevel == null) {
         operation.call(minecraft, poseStack, buffer, position, partialTicks);
      } else {
         SubLevelOutlinePose.runProjected(poseStack, subLevel, position, projectedPos -> operation.call(minecraft, poseStack, buffer, projectedPos, partialTicks));
      }
   }

   private ClientSubLevel sable$getSubLevel(Minecraft minecraft) {
      ClientLevel level = minecraft.level;
      if (level == null) {
         return null;
      } else {
         return Sable.HELPER.getContaining(level, this.pos) instanceof ClientSubLevel subLevel ? subLevel : null;
      }
   }
}
