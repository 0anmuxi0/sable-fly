package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.catnip.outliner.LineOutline"}
)
public class LineOutlineMixin {
   @WrapMethod(
      method = {"submitInner(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/phys/Vec3;FLorg/joml/Vector3d;Lorg/joml/Vector3d;FIIZ)V"}
   )
   private void sable$projectOutlineOntoSubLevel(
      PoseStack ms,
      SubmitNodeCollector queue,
      Vec3 camera,
      float pt,
      Vector3d start,
      Vector3d end,
      float width,
      int color,
      int lightmap,
      boolean disableNormals,
      Operation<Void> original
   ) {
      ClientSubLevel subLevel = SubLevelOutlinePose.find(new Vec3(start.x, start.y, start.z));
      if (subLevel == null) {
         subLevel = SubLevelOutlinePose.find(new Vec3(end.x, end.y, end.z));
      }

      if (subLevel == null) {
         original.call(ms, queue, camera, pt, start, end, width, color, lightmap, disableNormals);
      } else {
         SubLevelOutlinePose.runProjected(
            ms, subLevel, camera, plotCamera -> original.call(ms, queue, plotCamera, pt, start, end, width, color, lightmap, disableNormals)
         );
      }
   }
}
