package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.catnip.outliner.LineOutline"}
)
public class LineOutlineMixin {
   @Shadow
   @Final
   protected Vector3d start;

   @Shadow
   @Final
   protected Vector3d end;

   @WrapMethod(
      method = {"renderInner(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/Vec3;FFLorg/joml/Vector4f;IZ)V"}
   )
   private void sable$projectOutlineOntoSubLevel(
      PoseStack ms,
      VertexConsumer consumer,
      Vec3 camera,
      float pt,
      float width,
      Vector4f color,
      int lightmap,
      boolean disableNormals,
      Operation<Void> original
   ) {
      ClientSubLevel subLevel = SubLevelOutlinePose.find(new Vec3(start.x, start.y, start.z));
      if (subLevel == null) {
         subLevel = SubLevelOutlinePose.find(new Vec3(end.x, end.y, end.z));
      }

      if (subLevel == null) {
         original.call(ms, consumer, camera, pt, width, color, lightmap, disableNormals);
      } else {
         SubLevelOutlinePose.runProjected(
            ms, subLevel, camera, plotCamera -> original.call(ms, consumer, plotCamera, pt, width, color, lightmap, disableNormals)
         );
      }
   }
}
