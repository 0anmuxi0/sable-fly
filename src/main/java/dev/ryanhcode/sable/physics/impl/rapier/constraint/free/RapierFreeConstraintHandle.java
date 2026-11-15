package dev.ryanhcode.sable.physics.impl.rapier.constraint.free;

import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.Rapier3D;
import dev.ryanhcode.sable.physics.impl.rapier.constraint.RapierConstraintHandle;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

public class RapierFreeConstraintHandle extends RapierConstraintHandle implements FreeConstraintHandle {
   public static RapierFreeConstraintHandle create(
      ServerLevel serverLevel, @Nullable PhysicsPipelineBody bodyA, @Nullable PhysicsPipelineBody bodyB, FreeConstraintConfiguration config
   ) {
      long sceneHandle = Rapier3D.getSceneHandle(serverLevel);
      long handle = Rapier3D.addFreeConstraint(
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
      return new RapierFreeConstraintHandle(sceneHandle, handle);
   }

   public RapierFreeConstraintHandle(long sceneHandle, long handle) {
      super(sceneHandle, handle);
   }
}
