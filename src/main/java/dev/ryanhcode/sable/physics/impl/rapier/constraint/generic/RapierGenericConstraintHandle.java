package dev.ryanhcode.sable.physics.impl.rapier.constraint.generic;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.ConstraintJointAxis;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.physics.impl.rapier.constraint.RapierConstraintHandle;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;

@Internal
public class RapierGenericConstraintHandle extends RapierConstraintHandle implements GenericConstraintHandle {
   private static final int FRAME_SIDE_FIRST = 0;
   private static final int FRAME_SIDE_SECOND = 1;

   @Contract("_, _, _, _ -> new")
   @NotNull
   public static RapierGenericConstraintHandle create(
      ServerLevel serverLevel, @Nullable PhysicsPipelineBody bodyA, @Nullable PhysicsPipelineBody bodyB, GenericConstraintConfiguration config
   ) {
      long sceneHandle = Rapier3D.getSceneHandle(serverLevel);
      int lockedAxesMask = 0;

      for (ConstraintJointAxis axis : config.lockedAxes()) {
         lockedAxesMask |= 1 << axis.ordinal();
      }

      long handle = Rapier3D.addGenericConstraint(
         sceneHandle,
         bodyA == null ? -1 : Rapier3D.getID(bodyA),
         bodyB == null ? -1 : Rapier3D.getID(bodyB),
         config.pos1().x(),
         config.pos1().y(),
         config.pos1().z(),
         config.orientation1().x(),
         config.orientation1().y(),
         config.orientation1().z(),
         config.orientation1().w(),
         config.pos2().x(),
         config.pos2().y(),
         config.pos2().z(),
         config.orientation2().x(),
         config.orientation2().y(),
         config.orientation2().z(),
         config.orientation2().w(),
         lockedAxesMask
      );
      return new RapierGenericConstraintHandle(sceneHandle, handle);
   }

   public RapierGenericConstraintHandle(long sceneHandle, long handle) {
      super(sceneHandle, handle);
   }

   @Override
   public void setFrame1(Vector3dc localPosition, Quaterniondc localOrientation) {
      this.assertValid();
      Rapier3D.setConstraintFrame(
         this.sceneHandle,
         this.handle,
         0,
         localPosition.x(),
         localPosition.y(),
         localPosition.z(),
         localOrientation.x(),
         localOrientation.y(),
         localOrientation.z(),
         localOrientation.w()
      );
   }

   @Override
   public void setFrame2(Vector3dc localPosition, Quaterniondc localOrientation) {
      this.assertValid();
      Rapier3D.setConstraintFrame(
         this.sceneHandle,
         this.handle,
         1,
         localPosition.x(),
         localPosition.y(),
         localPosition.z(),
         localOrientation.x(),
         localOrientation.y(),
         localOrientation.z(),
         localOrientation.w()
      );
   }

   @Override
   public void setLimit(ConstraintJointAxis axis, double min, double max) {
      this.assertValid();
      Rapier3D.setConstraintLimit(this.sceneHandle, this.handle, axis.ordinal(), min, max);
   }

   @Override
   public void lockAxes(@NotNull ConstraintJointAxis... axes) {
      byte mask = 0;

      for (ConstraintJointAxis axis : axes) {
         byte bit = (byte)(1 << axis.ordinal());
         if ((mask & bit) != 0) {
            throw new RuntimeException("Duplicate axis: " + axis);
         }

         mask |= bit;
      }

      this.assertValid();
      Rapier3D.lockConstraintAxes(this.sceneHandle, this.handle, mask);
   }
}
