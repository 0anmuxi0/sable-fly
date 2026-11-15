package dev.ryanhcode.sable.sublevel.render;

import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.io.Closeable;
import net.minecraft.client.Camera;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public interface SubLevelRenderData extends Closeable {
   @Override
   void close();

   void rebuild();

   boolean isSectionCompiled(int var1, int var2, int var3);

   void setDirty(int var1, int var2, int var3, boolean var4);

   void compileSections(PrioritizeChunkUpdates var1, RenderRegionCache var2, Camera var3);

   int getVisibleSectionCount();

   default Matrix4f getTransformation(double camX, double camY, double camZ) {
      return this.getTransformation(camX, camY, camZ, new Matrix4f());
   }

   default Matrix4f getTransformation(double var1, double var3, double var5, Matrix4f var7) {
      var7.identity();
      Pose3dc var8 = this.getSubLevel().renderPose();
      var7.translate((float)(var8.position().x() - var1), (float)(var8.position().y() - var3), (float)(var8.position().z() - var5));
      var7.rotate(new Quaternionf(var8.orientation()));
      var7.scale((float)var8.scale().x(), (float)var8.scale().y(), (float)var8.scale().z());
      return var7;
   }

   ClientSubLevel getSubLevel();

   default Vector3d getChunkOffset() {
      return this.getChunkOffset(new Vector3d());
   }

   default Vector3d getChunkOffset(Vector3d dest) {
      return this.getSubLevel().renderPose().rotationPoint().negate(dest);
   }
}
