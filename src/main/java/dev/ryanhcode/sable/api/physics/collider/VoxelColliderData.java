package dev.ryanhcode.sable.api.physics.collider;

import org.joml.Vector3dc;

public interface VoxelColliderData {
   void addBox(Vector3dc min, Vector3dc max);

   void clearBoxes();
}
