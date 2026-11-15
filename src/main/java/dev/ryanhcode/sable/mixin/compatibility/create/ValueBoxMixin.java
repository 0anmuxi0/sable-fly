package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
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
      method = {"submit(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/phys/Vec3;F)V"}
   )
   private void sable$submitAtWorldPos(Minecraft var1, PoseStack var2, SubmitNodeCollector var3, Vec3 var4, float var5, Operation<Void> var6) {
      ClientSubLevel var7 = this.sable$getSubLevel(var1);
      if (var7 == null) {
         var6.call(var1, var2, var3, var4, var5);
      } else {
         SubLevelOutlinePose.runProjected(var2, var7, var4, var5x -> var6.call(var1, var2, var3, var5x, var5));
      }
   }

   private ClientSubLevel sable$getSubLevel(Minecraft var1) {
      ClientLevel var2 = var1.level;
      if (var2 == null) {
         return null;
      } else {
         return Sable.HELPER.getContaining(var2, this.pos) instanceof ClientSubLevel var4 ? var4 : null;
      }
   }
}
