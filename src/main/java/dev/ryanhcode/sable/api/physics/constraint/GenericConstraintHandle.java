package dev.ryanhcode.sable.api.physics.constraint;

import org.joml.Quaterniondc;
import org.joml.Vector3dc;

public non-sealed interface GenericConstraintHandle extends PhysicsConstraintHandle {
   void setFrame1(Vector3dc localPosition, Quaterniondc localOrientation);

   void setFrame2(Vector3dc localPosition, Quaterniondc localOrientation);

   void setLimit(ConstraintJointAxis axis, double min, double max);

   void lockAxes(ConstraintJointAxis... axes);
}
