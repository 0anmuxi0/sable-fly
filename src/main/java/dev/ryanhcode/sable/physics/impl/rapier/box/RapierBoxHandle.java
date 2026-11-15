package dev.ryanhcode.sable.physics.impl.rapier.box;

import dev.ryanhcode.sable.api.physics.object.box.BoxHandle;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

@Internal
public record RapierBoxHandle(long sceneHandle, int id, double[] poseCache) implements BoxHandle {
   public static RapierBoxHandle create(long sceneHandle, Pose3dc pose, Vector3dc halfExtents, double mass) {
      Vector3dc pos = pose.position();
      Quaterniondc rot = pose.orientation();
      int id = Rapier3D.nextBodyID();
      Rapier3D.createBox(
         sceneHandle, id, mass, halfExtents.x(), halfExtents.y(), halfExtents.z(), new double[]{pos.x(), pos.y(), pos.z(), rot.x(), rot.y(), rot.z(), rot.w()}
      );
      return new RapierBoxHandle(sceneHandle, id, new double[7]);
   }

   @Override
   public void readPose(Pose3d dest) {
      Rapier3D.getPose(this.sceneHandle, this.id, this.poseCache);
      dest.position().set(this.poseCache[0], this.poseCache[1], this.poseCache[2]);
      dest.orientation().set(this.poseCache[3], this.poseCache[4], this.poseCache[5], this.poseCache[6]);
   }

   @Override
   public void remove() {
      Rapier3D.removeBox(this.sceneHandle, this.id);
   }

   @Override
   public void wakeUp() {
      Rapier3D.wakeUpObject(this.sceneHandle, this.id);
   }

   @Override
   public int getRuntimeId() {
      return this.id;
   }
}
