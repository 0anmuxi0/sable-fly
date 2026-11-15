package dev.ryanhcode.sable.util;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.Pose3d;
import net.minecraft.nbt.CompoundTag;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class SableNBTUtils {
   public static CompoundTag writePose3d(Pose3d pose) {
      CompoundTag tag = new CompoundTag();
      tag.put("position", writeVector3d(pose.position()));
      tag.put("orientation", writeQuaternion(pose.orientation()));
      tag.put("rotation_point", writeVector3d(pose.rotationPoint()));
      return tag;
   }

   public static Pose3d readPose3d(CompoundTag tag) {
      return new Pose3d(
         readVector3d(tag.getCompoundOrEmpty("position")),
         readQuaternion(tag.getCompoundOrEmpty("orientation")),
         readVector3d(tag.getCompoundOrEmpty("rotation_point")),
         new Vector3d(1.0)
      );
   }

   public static CompoundTag writeQuaternion(Quaterniondc quat) {
      CompoundTag tag = new CompoundTag();
      tag.putDouble("x", quat.x());
      tag.putDouble("y", quat.y());
      tag.putDouble("z", quat.z());
      tag.putDouble("w", quat.w());
      return tag;
   }

   public static Quaterniond readQuaternion(CompoundTag tag) {
      return new Quaterniond(tag.getDoubleOr("x", 0.0), tag.getDoubleOr("y", 0.0), tag.getDoubleOr("z", 0.0), tag.getDoubleOr("w", 0.0));
   }

   public static CompoundTag writeVector3d(Vector3dc vector) {
      CompoundTag tag = new CompoundTag();
      tag.putDouble("x", vector.x());
      tag.putDouble("y", vector.y());
      tag.putDouble("z", vector.z());
      return tag;
   }

   public static Vector3d readVector3d(CompoundTag tag) {
      return new Vector3d(tag.getDoubleOr("x", 0.0), tag.getDoubleOr("y", 0.0), tag.getDoubleOr("z", 0.0));
   }

   public static CompoundTag writeBoundingBox(BoundingBox3dc bounds) {
      CompoundTag tag = new CompoundTag();
      tag.putDouble("minX", bounds.minX());
      tag.putDouble("minY", bounds.minY());
      tag.putDouble("minZ", bounds.minZ());
      tag.putDouble("maxX", bounds.maxX());
      tag.putDouble("maxY", bounds.maxY());
      tag.putDouble("maxZ", bounds.maxZ());
      return tag;
   }

   public static BoundingBox3d readBoundingBox(CompoundTag tag) {
      return new BoundingBox3d(
         tag.getDoubleOr("minX", 0.0),
         tag.getDoubleOr("minY", 0.0),
         tag.getDoubleOr("minZ", 0.0),
         tag.getDoubleOr("maxX", 0.0),
         tag.getDoubleOr("maxY", 0.0),
         tag.getDoubleOr("maxZ", 0.0)
      );
   }
}
