package dev.ryanhcode.sable.physics.impl.rapier.constraint.fixed;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.physics.impl.rapier.constraint.RapierConstraintHandle;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public class RapierFixedConstraintHandle extends RapierConstraintHandle implements FixedConstraintHandle {
   public static RapierFixedConstraintHandle create(
      ServerLevel serverLevel, @org.jetbrains.annotations.Nullable PhysicsPipelineBody bodyA, @org.jetbrains.annotations.Nullable PhysicsPipelineBody bodyB, FixedConstraintConfiguration config
   ) {
      long sceneHandle = Rapier3D.getSceneHandle(serverLevel);
      long handle = Rapier3D.addFixedConstraint(
         sceneHandle,
         bodyA == null ? -1 : Rapier3D.getID(bodyA),
         bodyB == null ? -1 : Rapier3D.getID(bodyB),
         config.pos1().x(),
         config.pos1().y(),
         config.pos1().z(),
         config.pos2().x(),
         config.pos2().y(),
         config.pos2().z(),
         config.orientation().x(),
         config.orientation().y(),
         config.orientation().z(),
         config.orientation().w()
      );
      return new RapierFixedConstraintHandle(sceneHandle, handle);
   }

   public RapierFixedConstraintHandle(long sceneHandle, long handle) {
      super(sceneHandle, handle);
   }
}
