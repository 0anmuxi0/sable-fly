package dev.ryanhcode.sable.api.physics.constraint;

import org.joml.Vector3d;

public sealed interface PhysicsConstraintHandle permits FreeConstraintHandle, FixedConstraintHandle, RotaryConstraintHandle, GenericConstraintHandle {
   void getJointImpulses(Vector3d linearImpulseDest, Vector3d angularImpulseDest);

   void setContactsEnabled(boolean enabled);

   void setMotor(ConstraintJointAxis axis, double target, double stiffness, double damping, boolean hasMaxForce, double maxForce);

   void remove();

   boolean isValid();
}
