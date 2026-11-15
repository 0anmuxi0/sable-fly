package dev.ryanhcode.sable.mixinterface.entity.entity_rotations_and_riding;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public interface EntityTransformationExtension {
   void sable$setSubLevelOrientation(Quaternionf var1);

   void sable$setSubLevelScale(Vector3f var1);

   void sable$setSubLevelPivot(Vector3f var1);

   Quaternionf sable$getSubLevelOrientation();

   Vector3f sable$getSubLevelScale();

   Vector3f sable$getSubLevelPivot();
}
