package dev.ryanhcode.sable.physics.impl.rapier.constraint;

import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Vector3d;

@Internal
public abstract class RapierConstraintHandle {
   protected final long sceneHandle;
   protected final long handle;
   private final double[] impulseCache;

   protected RapierConstraintHandle(long sceneHandle, long handle) {
      this.sceneHandle = sceneHandle;
      this.handle = handle;
      this.impulseCache = new double[6];
   }

   public void setContactsEnabled(boolean enabled) {
      this.assertValid();
      Rapier3D.setConstraintContactsEnabled(this.sceneHandle, this.handle, enabled);
   }

   public void getJointImpulses(Vector3d linearImpulseDest, Vector3d angularImpulseDest) {
      this.assertValid();
      Rapier3D.getConstraintImpulses(this.sceneHandle, this.handle, this.impulseCache);
      linearImpulseDest.set(this.impulseCache[0], this.impulseCache[1], this.impulseCache[2]);
      angularImpulseDest.set(this.impulseCache[3], this.impulseCache[4], this.impulseCache[5]);
   }

   public void setMotor(ConstraintJointAxis axis, double target, double stiffness, double damping, boolean hasForceLimit, double maxForce) {
      this.assertValid();
      Rapier3D.setConstraintMotor(this.sceneHandle, this.handle, axis.ordinal(), target, stiffness, damping, hasForceLimit, maxForce);
   }

   public void remove() {
      Rapier3D.removeConstraint(this.sceneHandle, this.handle);
   }

   public boolean isValid() {
      return Rapier3D.isConstraintValid(this.sceneHandle, this.handle);
   }

   protected void assertValid() {
      if (!this.isValid()) {
         throw new RuntimeException("Attempted to mutate an invalid constraint");
      }
   }
}
