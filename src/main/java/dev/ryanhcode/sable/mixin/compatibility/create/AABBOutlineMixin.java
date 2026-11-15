package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import com.zurrtum.create.client.catnip.render.SuperRenderTypeBuffer;
import org.joml.Vector4f;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.catnip.outliner.AABBOutline"}
)
public class AABBOutlineMixin {
   @WrapMethod(
      method = {"renderBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/zurrtum/create/client/catnip/render/SuperRenderTypeBuffer;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lorg/joml/Vector4f;IZ)V"}
   )
   private void sable$projectOutlineOntoSubLevel(
      PoseStack ms, SuperRenderTypeBuffer buffer, Vec3 camera, AABB box, Vector4f color, int lightmap, boolean disableLineNormals, Operation<Void> original
   ) {
      ClientSubLevel subLevel = SubLevelOutlinePose.find(box);
      if (subLevel == null) {
         original.call(ms, buffer, camera, box, color, lightmap, disableLineNormals);
      } else {
         SubLevelOutlinePose.runProjected(ms, subLevel, camera, plotCamera -> original.call(ms, buffer, plotCamera, box, color, lightmap, disableLineNormals));
      }
   }
}
