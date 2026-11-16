package dev.ryanhcode.sable.api.physics;

import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.PhysicsConfigData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface PhysicsPipeline {
   void init(Vector3dc gravity, double universalDrag);

   void dispose();

   void prePhysicsTicks();

   void physicsTick(double timeStep);

   void postPhysicsTicks();

   void tick();

   void add(ServerSubLevel subLevel, Pose3dc pose);

   void remove(ServerSubLevel subLevel);

   void add(KinematicContraption contraption);

   void remove(KinematicContraption contraption);

   @OverrideOnly
   Pose3d readPose(ServerSubLevel subLevel, Pose3d dest);

   @OverrideOnly
   RopeHandle addRope(RopePhysicsObject rope);

   BoxHandle addBox(BoxPhysicsObject boxPhysicsObject);

   void handleChunkSectionAddition(LevelChunkSection chunk, int x, int y, int z, boolean uploadDataIfGlobal);

   void handleChunkSectionRemoval(int x, int y, int z);

   void handleBlockChange(SectionPos sectionPos, LevelChunkSection chunk, int x, int y, int z, BlockState oldState, BlockState newState);

   default void onStatsChanged(@NotNull ServerSubLevel serverSubLevel) {
   }

   void teleport(PhysicsPipelineBody body, Vector3dc position, Quaterniondc orientation);

   void applyImpulse(PhysicsPipelineBody body, Vector3dc position, Vector3dc force);

   void applyLinearAndAngularImpulse(PhysicsPipelineBody body, Vector3dc force, Vector3dc torque, boolean wakeUp);

   default void addLinearAndAngularVelocity(PhysicsPipelineBody body, Vector3dc linearVelocity, Vector3dc angularVelocity) {
   }

   default void resetVelocity(PhysicsPipelineBody body) {
      this.addLinearAndAngularVelocity(body, this.getLinearVelocity(body, new Vector3d()).negate(), this.getAngularVelocity(body, new Vector3d()).negate());
   }

   default Vector3d getLinearVelocity(PhysicsPipelineBody body, Vector3d dest) {
      return dest.zero();
   }

   default Vector3d getAngularVelocity(PhysicsPipelineBody body, Vector3d dest) {
      return dest.zero();
   }

   void wakeUp(PhysicsPipelineBody body);

   @Nullable
   @Contract("null, null, _ -> fail")
   default <T extends PhysicsConstraintHandle> T addConstraint(
      @Nullable PhysicsPipelineBody bodyA, @Nullable PhysicsPipelineBody bodyB, @NotNull PhysicsConstraintConfiguration<T> configuration
   ) {
      throw new UnsupportedOperationException("Not implemented");
   }

   @OverrideOnly
   default void updateConfigFrom(PhysicsConfigData data) {
      throw new UnsupportedOperationException("Not implemented");
   }

   int getNextRuntimeID();
}
