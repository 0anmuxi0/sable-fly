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

   boolean isSectionCompiled(int x, int y, int z);

   void setDirty(int x, int y, int z, boolean playerChanged);

   void compileSections(PrioritizeChunkUpdates chunkUpdates, RenderRegionCache renderRegionCache, Camera camera);

   int getVisibleSectionCount();

   default Matrix4f getTransformation(double camX, double camY, double camZ) {
      return this.getTransformation(camX, camY, camZ, new Matrix4f());
   }

   default Matrix4f getTransformation(double camX, double camY, double camZ, Matrix4f store) {
      store.identity();
      Pose3dc pose = this.getSubLevel().renderPose();
      store.translate((float)(pose.position().x() - camX), (float)(pose.position().y() - camY), (float)(pose.position().z() - camZ));
      store.rotate(new Quaternionf(pose.orientation()));
      store.scale((float)pose.scale().x(), (float)pose.scale().y(), (float)pose.scale().z());
      return store;
   }

   ClientSubLevel getSubLevel();

   default Vector3d getChunkOffset() {
      return this.getChunkOffset(new Vector3d());
   }

   default Vector3d getChunkOffset(Vector3d dest) {
      return this.getSubLevel().renderPose().rotationPoint().negate(dest);
   }
}
