package dev.ryanhcode.sable.mixinterface.sublevel_render;

import java.util.Map;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface BlockEntityTransformsHolder {
   Map<BlockEntityRenderState, BlockEntityTransformsHolder.SableBlockEntityTransform> sable$getBlockEntityTransforms();

   public record SableBlockEntityTransform(Matrix4f transformation, Vector3d rotationPoint, Quaternionf orientation) {
      public SableBlockEntityTransform(Matrix4f transformation, Vector3dc rotationPoint, Quaternionf orientation) {
         this(new Matrix4f(transformation), new Vector3d(rotationPoint), new Quaternionf(orientation));
      }
   }
}
