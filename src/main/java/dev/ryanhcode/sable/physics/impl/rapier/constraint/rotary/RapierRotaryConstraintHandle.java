package dev.ryanhcode.sable.physics.impl.rapier.constraint.rotary;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.physics.impl.rapier.constraint.RapierConstraintHandle;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public class RapierRotaryConstraintHandle extends RapierConstraintHandle implements RotaryConstraintHandle {
   public static RapierRotaryConstraintHandle create(
      ServerLevel serverLevel, @Nullable PhysicsPipelineBody bodyA, @Nullable PhysicsPipelineBody bodyB, RotaryConstraintConfiguration config
   ) {
      long sceneHandle = Rapier3D.getSceneHandle(serverLevel);
      long handle = Rapier3D.addRotaryConstraint(
         sceneHandle,
         bodyA == null ? -1 : Rapier3D.getID(bodyA),
         bodyB == null ? -1 : Rapier3D.getID(bodyB),
         config.pos1().x(),
         config.pos1().y(),
         config.pos1().z(),
         config.pos2().x(),
         config.pos2().y(),
         config.pos2().z(),
         config.normal1().x(),
         config.normal1().y(),
         config.normal1().z(),
         config.normal2().x(),
         config.normal2().y(),
         config.normal2().z()
      );
      return new RapierRotaryConstraintHandle(sceneHandle, handle);
   }

   public RapierRotaryConstraintHandle(long sceneHandle, long handle) {
      super(sceneHandle, handle);
   }
}
