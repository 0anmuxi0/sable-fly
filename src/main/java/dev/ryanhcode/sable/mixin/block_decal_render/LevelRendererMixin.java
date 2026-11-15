package dev.ryanhcode.sable.mixin.block_decal_render;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public abstract class LevelRendererMixin {
   @Inject(
      method = {"submitBlockDestroyAnimation"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
         shift = Shift.AFTER
      )}
   )
   private void sable$transformBlockDamage(
      PoseStack var1, SubmitNodeCollector var2, LevelRenderState var3, CallbackInfo var4, @Local BlockBreakingRenderState var5, @Local BlockPos var6
   ) {
      Vec3 var7 = Vec3.atLowerCornerOf(var6);
      ClientSubLevel var8 = sable$getSubLevel(var6);
      if (var8 != null) {
         Pose3dc var9 = var8.renderPose();
         Vec3 var10 = var3.cameraRenderState.pos;
         Vec3 var11 = var9.transformPosition(var7);
         var1.translate(-(var7.x - var10.x), -(var7.y - var10.y), -(var7.z - var10.z));
         var1.translate(var11.x - var10.x, var11.y - var10.y, var11.z - var10.z);
         var1.mulPose(new Matrix4f().rotate(new Quaternionf(var9.orientation())));
      }
   }

   @Unique
   private static ClientSubLevel sable$getSubLevel(BlockPos var0) {
      ClientLevel var1 = Minecraft.getInstance().level;
      return var1 == null ? null : (ClientSubLevel)Sable.HELPER.getContaining(var1, Vec3.atLowerCornerOf(var0));
   }
}
