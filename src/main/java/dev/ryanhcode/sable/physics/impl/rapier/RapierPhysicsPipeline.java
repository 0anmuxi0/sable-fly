package dev.ryanhcode.sable.physics.impl.rapier;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.GenericConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.constraint.PhysicsConstraintHandle;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.api.physics.object.box.BoxHandle;
import dev.ryanhcode.sable.api.physics.object.box.BoxPhysicsObject;
import dev.ryanhcode.sable.api.physics.object.rope.RopeHandle;
import dev.ryanhcode.sable.api.physics.object.rope.RopePhysicsObject;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.chunk.VoxelNeighborhoodState;
import dev.ryanhcode.sable.physics.config.PhysicsConfigData;
import dev.ryanhcode.sable.physics.impl.rapier.box.RapierBoxHandle;
import dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderBakery;
import dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderData;
import dev.ryanhcode.sable.physics.impl.rapier.constraint.fixed.RapierFixedConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.constraint.free.RapierFreeConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.constraint.generic.RapierGenericConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.constraint.rotary.RapierRotaryConstraintHandle;
import dev.ryanhcode.sable.physics.impl.rapier.rope.RapierRopeHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.util.LevelAccelerator;
import dev.ryanhcode.sable.util.SableMathUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public class RapierPhysicsPipeline implements PhysicsPipeline {
   private static final double DISTANCE_THRESHOLD = 1.0E-7;
   private static final double ANGULAR_THRESHOLD = 1.0E-7;
   private final ServerLevel level;
   private final LevelAccelerator accelerator;
   private final RapierVoxelColliderBakery colliderBakery;
   private final Int2ObjectMap<ServerSubLevel> activeSubLevels = new Int2ObjectArrayMap();
   private final Object2ObjectMap<KinematicContraption, RapierPhysicsPipeline.TrackedKinematicContraption> activeContraptions = new Object2ObjectOpenHashMap();
   private final Long2LongOpenHashMap recentCollisions = new Long2LongOpenHashMap();
   private final ReferenceList<PhysicsPipelineBody> queuedWakeUps = new ReferenceArrayList();
   private final double[] poseCache;
   private RapierPhysicsScene scene;

   public RapierPhysicsPipeline(ServerLevel level) {
      this.level = level;
      this.accelerator = new LevelAccelerator(level);
      this.colliderBakery = new RapierVoxelColliderBakery(this.accelerator);
      this.recentCollisions.defaultReturnValue(-1L);
      this.poseCache = new double[7];
   }

   private static int packBlockState(VoxelNeighborhoodState state, int colliderID) {
      return state.byteRepresentation() | colliderID << 16;
   }

   protected long getSceneHandle() {
      if (this.scene == null) {
         throw new IllegalStateException("Physics scene is not initialized");
      } else {
         return this.scene.handle();
      }
   }

   @Override
   public void init(@Nullable Vector3dc gravity, double universalDrag) {
      try {
         this.scene = new RapierPhysicsScene(Rapier3D.initialize(gravity.x(), gravity.y(), gravity.z(), universalDrag));
      } catch (UnsatisfiedLinkError e) {
         Sable.LOGGER
            .error(
               "Sable has failed to link with the natives for its Rapier pipeline. Please report with system details to https://github.com/ryanhcode/sable/issues",
               e
            );
         CrashReport crashReport = CrashReport.forThrowable(e.getCause(), "Sable linking with Rapier natives");
         CrashReportCategory category = crashReport.addCategory("Natives");
         category.setDetail("Name", Rapier3D.NATIVE_NAME);
         throw new ReportedException(crashReport);
      }
   }

   @Override
   public void dispose() {
      if (this.scene != null) {
         Rapier3D.dispose(this.scene.handle());
         this.scene = null;
      }
   }

   @Override
   public void prePhysicsTicks() {
      double timeStep = 0.05;
      Rapier3D.tick(this.scene.handle(), 0.05);
   }

   @Override
   public void physicsTick(double timeStep) {
      this.updateContraptionPoses();
      Rapier3D.step(this.scene.handle(), timeStep);
      ObjectListIterator iterator = this.queuedWakeUps.iterator();

      while (iterator.hasNext()) {
         PhysicsPipelineBody queuedWakeUp = (PhysicsPipelineBody)iterator.next();
         if (!queuedWakeUp.isRemoved()) {
            Rapier3D.wakeUpObject(this.scene.handle(), queuedWakeUp.getRuntimeId());
         }
      }

      this.queuedWakeUps.clear();
   }

   @Override
   public void postPhysicsTicks() {
      this.processCollisionEffects();
   }

   @Override
   public void tick() {
      this.accelerator.clearCache();
   }

   @Override
   public void add(ServerSubLevel subLevel, Pose3dc pose) {
      this.assertBodyValid(subLevel);
      Vector3dc pos = pose.position();
      Quaterniondc rot = pose.orientation();
      int id = Rapier3D.getID(subLevel);
      Rapier3D.createSubLevel(this.scene.handle(), id, new double[]{pos.x(), pos.y(), pos.z(), rot.x(), rot.y(), rot.z(), rot.w()});
      subLevel.updateMergedMassData(1.0F);
      Vector3dc centerOfMass = subLevel.getMassTracker().getCenterOfMass();
      if (centerOfMass != null) {
         subLevel.logicalPose().rotationPoint().set(centerOfMass);
         this.onStatsChanged(subLevel);
      }

      this.activeSubLevels.put(Rapier3D.getID(subLevel), subLevel);
   }

   @Override
   public void remove(ServerSubLevel subLevel) {
      Rapier3D.removeSubLevel(this.scene.handle(), Rapier3D.getID(subLevel));
      this.activeSubLevels.remove(Rapier3D.getID(subLevel));
   }

   @Override
   public void add(KinematicContraption contraption) {
      if (this.activeContraptions.containsKey(contraption)) {
         throw new IllegalStateException("Contraption " + contraption + " is already present in pipeline");
      } else {
         int id = this.getNextRuntimeID();
         this.activeContraptions
            .put(contraption, new RapierPhysicsPipeline.TrackedKinematicContraption(new Vector3d(), new Quaterniond(), new Vector3d(), new Vector3d(), id));
         SubLevel mountSubLevel = Sable.HELPER.getContaining(this.level, contraption.sable$getPosition());
         int mountId = mountSubLevel != null ? Rapier3D.getID((ServerSubLevel)mountSubLevel) : -1;
         BoundingBox3i localBounds = new BoundingBox3i();
         contraption.sable$getLocalBounds(localBounds);
         Vector3dc pos = contraption.sable$getPosition();
         Quaterniond rot = contraption.sable$getOrientation();
         double[] pose = new double[]{pos.x(), pos.y(), pos.z(), rot.x(), rot.y(), rot.z(), rot.w()};
         Rapier3D.createKinematicContraption(this.scene.handle(), mountId, id, pose);

         record UploadingContraptionChunk(int[] data) {
         }

         Long2ObjectMap<UploadingContraptionChunk> chunks = new Long2ObjectOpenHashMap();
         MutableBlockPos blockPos = new MutableBlockPos();

         for (int x = localBounds.minX(); x <= localBounds.maxX(); x++) {
            for (int z = localBounds.minZ(); z <= localBounds.maxZ(); z++) {
               for (int y = localBounds.minY(); y <= localBounds.maxY(); y++) {
                  BlockState blockState = contraption.sable$blockGetter().getBlockState(blockPos.set(x, y, z));
                  if (!blockState.isAir()) {
                     SectionPos sectionPos = SectionPos.of(blockPos);
                     UploadingContraptionChunk chunk = (UploadingContraptionChunk)chunks.computeIfAbsent(
                        sectionPos.asLong(), longPos -> new UploadingContraptionChunk(new int[4096])
                     );
                     VoxelNeighborhoodState state = VoxelNeighborhoodState.CORNER;
                     RapierVoxelColliderData colliderData = this.colliderBakery.getPhysicsDataForBlock(blockState);
                     int index = (x & 15) + ((z & 15) << 4) + ((y & 15) << 8);
                     int colliderValue = colliderData == null ? 0 : colliderData.handle() + 1;
                     chunk.data[index] = packBlockState(state, colliderValue);
                  }
               }
            }
         }

         if (contraption.sable$shouldCollide()) {
            ObjectIterator iterator = chunks.long2ObjectEntrySet().iterator();

            while (iterator.hasNext()) {
               Entry<UploadingContraptionChunk> entry = (Entry<UploadingContraptionChunk>)iterator.next();
               SectionPos sectionPos = SectionPos.of(entry.getLongKey());
               UploadingContraptionChunk chunk = (UploadingContraptionChunk)entry.getValue();
               Rapier3D.addKinematicContraptionChunkSection(this.scene.handle(), id, sectionPos.x(), sectionPos.y(), sectionPos.z(), chunk.data());
            }
         }

         this.updateContraptionPose(contraption, 1.0);
         Rapier3D.setLocalBounds(
            this.scene.handle(), id, localBounds.minX, localBounds.minY, localBounds.minZ, localBounds.maxX, localBounds.maxY, localBounds.maxZ
         );
      }
   }

   @Override
   public void remove(KinematicContraption contraption) {
      RapierPhysicsPipeline.TrackedKinematicContraption removed = (RapierPhysicsPipeline.TrackedKinematicContraption)this.activeContraptions
         .remove(contraption);
      if (removed != null) {
         Rapier3D.removeKinematicContraption(this.scene.handle(), removed.id());
      }
   }

   @Override
   public Pose3d readPose(ServerSubLevel subLevel, Pose3d dest) {
      this.assertBodyValid(subLevel);
      Rapier3D.getPose(this.scene.handle(), Rapier3D.getID(subLevel), this.poseCache);
      dest.position().set(this.poseCache[0], this.poseCache[1], this.poseCache[2]);
      dest.orientation().set(this.poseCache[3], this.poseCache[4], this.poseCache[5], this.poseCache[6]);
      return dest;
   }

   @Override
   public RopeHandle addRope(RopePhysicsObject rope) {
      return RapierRopeHandle.create(this.scene.handle(), rope.getCollisionRadius(), rope.getPoints());
   }

   @Override
   public BoxHandle addBox(BoxPhysicsObject box) {
      return RapierBoxHandle.create(this.scene.handle(), box.getPose(), box.getHalfExtents(), box.getMass());
   }

   @Override
   public void handleChunkSectionAddition(LevelChunkSection section, int x, int y, int z, boolean uploadDataIfGlobal) {
      this.accelerator.clearCache();
      int[] array = new int[4096];
      SectionPos sectionPos = SectionPos.of(x, y, z);
      if (!section.hasOnlyAir()) {
         LevelChunk chunk = this.accelerator.getChunk(x, z);

         for (int bx = 0; bx < 16; bx++) {
            for (int bz = 0; bz < 16; bz++) {
               for (int by = 0; by < 16; by++) {
                  BlockPos globalPos = new BlockPos(bx, by, bz).offset(sectionPos.minBlockX(), sectionPos.minBlockY(), sectionPos.minBlockZ());
                  VoxelNeighborhoodState state = VoxelNeighborhoodState.getState(this.accelerator, globalPos, chunk);
                  RapierVoxelColliderData colliderData = this.colliderBakery.getPhysicsDataForBlock(this.accelerator.getBlockState(globalPos));
                  int index = bx + (bz << 4) + (by << 8);
                  int colliderValue = colliderData == null ? 0 : colliderData.handle() + 1;
                  array[index] = packBlockState(state, colliderValue);
               }
            }
         }
      }

      LevelPlot plot = SubLevelContainer.getContainer(this.level).getPlot(x, z);
      boolean global = plot == null;
      int id = -1;
      if (plot != null && uploadDataIfGlobal) {
         id = Rapier3D.getID((ServerSubLevel)plot.getSubLevel());
      }

      Rapier3D.addChunk(this.scene.handle(), x, y, z, array, global, id);
   }

   @Override
   public void handleChunkSectionRemoval(int x, int y, int z) {
      Rapier3D.removeChunk(this.scene.handle(), x, y, z, !SubLevelContainer.getContainer(this.level).inBounds(x, z));
   }

   @Override
   public void handleBlockChange(SectionPos sectionPos, LevelChunkSection chunk, int x, int y, int z, BlockState oldState, BlockState newState) {
      x = (sectionPos.x() << 4) + x;
      y = (sectionPos.y() << 4) + y;
      z = (sectionPos.z() << 4) + z;
      BlockPos globalBlockPos = new BlockPos(x, y, z);

      for (Direction dir : Direction.values()) {
         BlockPos pos = globalBlockPos.relative(dir);
         VoxelNeighborhoodState state = VoxelNeighborhoodState.getState(this.accelerator, pos, null);
         RapierVoxelColliderData colliderData = this.colliderBakery.getPhysicsDataForBlock(this.level.getBlockState(pos));
         int colliderValue = colliderData == null ? 0 : colliderData.handle() + 1;
         Rapier3D.changeBlock(this.scene.handle(), pos.getX(), pos.getY(), pos.getZ(), packBlockState(state, colliderValue));
      }

      VoxelNeighborhoodState state = VoxelNeighborhoodState.getState(this.accelerator, globalBlockPos, null);
      RapierVoxelColliderData colliderData = this.colliderBakery.getPhysicsDataForBlock(newState);
      int colliderValue = colliderData == null ? 0 : colliderData.handle() + 1;
      Rapier3D.changeBlock(this.scene.handle(), x, y, z, packBlockState(state, colliderValue));
   }

   @Override
   public void onStatsChanged(@NotNull ServerSubLevel subLevel) {
      this.assertBodyValid(subLevel);
      BoundingBox3ic plotBounds = subLevel.getPlot().getBoundingBox();
      int id = Rapier3D.getID(subLevel);
      Vector3dc centerOfMass = subLevel.getMassTracker().getCenterOfMass();
      if (centerOfMass != null) {
         Rapier3D.setCenterOfMass(this.scene.handle(), id, centerOfMass.x(), centerOfMass.y(), centerOfMass.z());
         Rapier3D.setMassPropertiesFrom(this.scene.handle(), id, subLevel.getMassTracker());
      }

      Rapier3D.setLocalBounds(
         this.scene.handle(), id, plotBounds.minX(), plotBounds.minY(), plotBounds.minZ(), plotBounds.maxX(), plotBounds.maxY(), plotBounds.maxZ()
      );
   }

   @Override
   public void teleport(PhysicsPipelineBody body, Vector3dc position, Quaterniondc orientation) {
      this.assertBodyValid(body);
      Rapier3D.teleportObject(
         this.scene.handle(),
         Rapier3D.getID(body),
         position.x(),
         position.y(),
         position.z(),
         orientation.x(),
         orientation.y(),
         orientation.z(),
         orientation.w()
      );
      if (body instanceof ServerSubLevel subLevel) {
         subLevel.logicalPose().position().set(position);
         subLevel.logicalPose().orientation().set(orientation);
      }
   }

   @Override
   public void applyImpulse(PhysicsPipelineBody body, Vector3dc position, Vector3dc force) {
      this.assertBodyValid(body);
      Vector3dc centerOfMass = body.getMassTracker().getCenterOfMass();
      Rapier3D.applyForce(
         this.scene.handle(),
         Rapier3D.getID(body),
         position.x() - centerOfMass.x(),
         position.y() - centerOfMass.y(),
         position.z() - centerOfMass.z(),
         force.x(),
         force.y(),
         force.z(),
         true
      );
   }

   @Override
   public void applyLinearAndAngularImpulse(PhysicsPipelineBody body, Vector3dc force, Vector3dc torque, boolean wakeUp) {
      this.assertBodyValid(body);
      Rapier3D.applyForceAndTorque(this.scene.handle(), Rapier3D.getID(body), force.x(), force.y(), force.z(), torque.x(), torque.y(), torque.z(), wakeUp);
   }

   @Override
   public void addLinearAndAngularVelocity(PhysicsPipelineBody body, Vector3dc linearVelocity, Vector3dc angularVelocity) {
      this.assertBodyValid(body);
      Rapier3D.addLinearAngularVelocities(
         this.scene.handle(),
         Rapier3D.getID(body),
         linearVelocity.x(),
         linearVelocity.y(),
         linearVelocity.z(),
         angularVelocity.x(),
         angularVelocity.y(),
         angularVelocity.z(),
         true
      );
   }

   public Vector3d sable$orig$getLinearVelocity(PhysicsPipelineBody body, Vector3d dest) {
      this.assertBodyValid(body);
      Rapier3D.getLinearVelocity(this.scene.handle(), Rapier3D.getID(body), this.poseCache);
      return dest.set(this.poseCache);
   }

   public Vector3d sable$orig$getAngularVelocity(PhysicsPipelineBody body, Vector3d dest) {
      this.assertBodyValid(body);
      Rapier3D.getAngularVelocity(this.scene.handle(), Rapier3D.getID(body), this.poseCache);
      return dest.set(this.poseCache);
   }

   @Override
   public void wakeUp(PhysicsPipelineBody body) {
      this.assertBodyValid(body);
      if (!SubLevelPhysicsSystem.IN_PHYSICS_STEP) {
         Rapier3D.wakeUpObject(this.scene.handle(), Rapier3D.getID(body));
      } else {
         this.queuedWakeUps.add(body);
      }
   }

   @Nullable
   @Override
   public <T extends PhysicsConstraintHandle> T addConstraint(
      @Nullable PhysicsPipelineBody bodyA, @Nullable PhysicsPipelineBody bodyB, @NotNull PhysicsConstraintConfiguration<T> configuration
   ) {
      if (bodyA == null && bodyB == null) {
         throw new IllegalArgumentException("Cannot add a constraint between the static world and static world");
      } else if (bodyA == bodyB) {
         throw new IllegalArgumentException("Cannot add a constraint between a body and itself");
      } else {
         try {
            configuration.validate(ServerSubLevelContainer.getContainer(this.level), bodyA, bodyB);
         } catch (Exception e) {
            throw new IllegalArgumentException("Constraint validation failed", e);
         }
         T constraint = (T)(switch (configuration) {
            case RotaryConstraintConfiguration config -> RapierRotaryConstraintHandle.create(this.level, bodyA, bodyB, config);
            case FixedConstraintConfiguration configx -> RapierFixedConstraintHandle.create(this.level, bodyA, bodyB, configx);
            case FreeConstraintConfiguration configxx -> RapierFreeConstraintHandle.create(this.level, bodyA, bodyB, configxx);
            case GenericConstraintConfiguration configxxx -> RapierGenericConstraintHandle.create(this.level, bodyA, bodyB, configxxx);
            default -> throw new MatchException(null, null);
         });
         return !constraint.isValid() ? null : constraint;
      }
   }

   @Override
   public void updateConfigFrom(PhysicsConfigData data) {
      Rapier3D.configFrequencyAndDamping(data.contactSpringFrequency, data.contactSpringDampingRatio);
      Rapier3D.configSolverIterations(data.solverIterations, data.pgsIterations, data.stabilizationIterations);
      Rapier3D.configMinIslandSize(data.minDynamicBodiesPerIsland);
   }

   @Override
   public int getNextRuntimeID() {
      return Rapier3D.nextBodyID();
   }

   private void assertBodyValid(PhysicsPipelineBody body) {
      if (body.isRemoved()) {
         throw new RuntimeException("Body has been removed");
      }
   }

   private void updateContraptionPoses() {
      SubLevelPhysicsSystem system = SubLevelPhysicsSystem.require(this.level);
      double partialPhysicsTick = system.getPartialPhysicsTick();
      ObjectIterator iterator = this.activeContraptions.keySet().iterator();

      while (iterator.hasNext()) {
         KinematicContraption contraption = (KinematicContraption)iterator.next();
         this.updateContraptionPose(contraption, partialPhysicsTick);
      }
   }

   private void updateContraptionPose(KinematicContraption contraption, double partialPhysicsTick) {
      RapierPhysicsPipeline.TrackedKinematicContraption trackedContraption = (RapierPhysicsPipeline.TrackedKinematicContraption)this.activeContraptions
         .get(contraption);
      SubLevel mountSubLevel = Sable.HELPER.getContaining(this.level, contraption.sable$getPosition());
      Vector3dc parentCenterOfMass = mountSubLevel != null ? ((ServerSubLevel)mountSubLevel).getMassTracker().getCenterOfMass() : JOMLConversion.ZERO;
      Vector3dc lastPosition = new Vector3d(contraption.sable$getPosition(partialPhysicsTick - 1.0));
      Quaterniondc lastOrientation = new Quaterniond(contraption.sable$getOrientation(partialPhysicsTick - 1.0));
      Vector3d pos = new Vector3d(contraption.sable$getPosition(partialPhysicsTick));
      Quaterniondc rot = contraption.sable$getOrientation(partialPhysicsTick);
      Vector3d linVel = pos.sub(lastPosition, new Vector3d());
      Vector3d angVel = SableMathUtils.getAngularVelocity(lastOrientation, rot, new Vector3d());
      linVel.mul(20.0);
      angVel.mul(20.0);
      rot.transformInverse(linVel);
      rot.transformInverse(angVel);
      pos.sub(parentCenterOfMass);
      if (pos.distanceSquared(trackedContraption.lastUploadedPosition()) > 9.999999999999998E-15
         || linVel.distanceSquared(trackedContraption.lastUploadedLinVel()) > 9.999999999999998E-15
         || angVel.distanceSquared(trackedContraption.lastUploadedAngVel()) > 9.999999999999998E-15
         || rot.div(trackedContraption.lastUploadedOrientation(), new Quaterniond()).angle() > 9.999999999999998E-15) {
         MassTracker massTracker = contraption.sable$getMassTracker();
         Vector3dc centerOfMass = massTracker.getCenterOfMass();
         double[] centerOfMassArray = new double[]{centerOfMass.x(), centerOfMass.y(), centerOfMass.z()};
         double[] poseArray = new double[]{pos.x(), pos.y(), pos.z(), rot.x(), rot.y(), rot.z(), rot.w()};
         double[] velocityArray = new double[]{linVel.x(), linVel.y(), linVel.z(), angVel.x(), angVel.y(), angVel.z()};
         Rapier3D.setKinematicContraptionTransform(this.scene.handle(), trackedContraption.id(), centerOfMassArray, poseArray, velocityArray);
         trackedContraption.lastUploadedPosition().set(pos);
         trackedContraption.lastUploadedLinVel().set(linVel);
         trackedContraption.lastUploadedAngVel().set(angVel);
         trackedContraption.lastUploadedOrientation().set(rot);
      }
   }

   private void processCollisionEffects() {
      this.recentCollisions.long2LongEntrySet().removeIf(entry -> this.level.getGameTime() - entry.getLongValue() > 2L);
      Vector3d localPointA = new Vector3d();
      Vector3d localPointB = new Vector3d();
      Vector3d localNormalA = new Vector3d();
      Vector3d localNormalB = new Vector3d();
      Vector3d globalPointA = new Vector3d();
      Vector3d globalPointB = new Vector3d();
      double[] collisions = Rapier3D.clearCollisions(this.scene.handle());
      MutableBlockPos pos = new MutableBlockPos();
      MutableBlockPos cornerPos = new MutableBlockPos();

      for (int i = 0; i < collisions.length / 15; i++) {
         int startIndex = i * 15;
         int idA = (int)collisions[startIndex];
         int idB = (int)collisions[startIndex + 1];
         double forceAmount = collisions[startIndex + 2];
         localNormalA.set(collisions[startIndex + 3], collisions[startIndex + 4], collisions[startIndex + 5]);
         localNormalB.set(collisions[startIndex + 6], collisions[startIndex + 7], collisions[startIndex + 8]);
         localPointA.set(collisions[startIndex + 9], collisions[startIndex + 10], collisions[startIndex + 11]);
         localPointB.set(collisions[startIndex + 12], collisions[startIndex + 13], collisions[startIndex + 14]);
         ServerSubLevel subLevelA = (ServerSubLevel)this.activeSubLevels.get(idA);
         ServerSubLevel subLevelB = (ServerSubLevel)this.activeSubLevels.get(idB);
         double minMass = Math.min(
            subLevelA != null ? subLevelA.getMassTracker().getMass() : Double.MAX_VALUE,
            subLevelB != null ? subLevelB.getMassTracker().getMass() : Double.MAX_VALUE
         );
         if (forceAmount > 25.0 * minMass) {
            BlockState stateA = Blocks.STONE.defaultBlockState();
            BlockState stateB = stateA;
            if (subLevelA != null) {
               Pose3d pose = subLevelA.logicalPose();
               pos.set(localPointA.x + pose.rotationPoint().x, localPointA.y + pose.rotationPoint().y, localPointA.z + pose.rotationPoint().z);
               cornerPos.set(
                  localPointA.x + pose.rotationPoint().x + 0.5, localPointA.y + pose.rotationPoint().y + 0.5, localPointA.z + pose.rotationPoint().z + 0.5
               );
               long exists = this.recentCollisions.put(cornerPos.asLong(), this.level.getGameTime());
               if (exists != -1L) {
                  continue;
               }

               stateA = this.accelerator.getBlockState(pos);
            }

            if (subLevelB != null) {
               Pose3d pose = subLevelB.logicalPose();
               pos.set(localPointB.x + pose.rotationPoint().x, localPointB.y + pose.rotationPoint().y, localPointB.z + pose.rotationPoint().z);
               cornerPos.set(
                  localPointB.x + pose.rotationPoint().x + 0.5, localPointB.y + pose.rotationPoint().y + 0.5, localPointB.z + pose.rotationPoint().z + 0.5
               );
               long exists = this.recentCollisions.put(cornerPos.asLong(), this.level.getGameTime());
               if (exists != -1L) {
                  continue;
               }

               stateB = this.accelerator.getBlockState(pos);
            }

            globalPointA.set(localPointA);
            globalPointB.set(localPointB);
            if (subLevelA != null) {
               Pose3d pose = subLevelA.logicalPose();
               pose.orientation().transform(globalPointA).add(pose.position());
            }

            if (subLevelB != null) {
               Pose3d pose = subLevelB.logicalPose();
               pose.orientation().transform(globalPointB).add(pose.position());
            }

            this.level
               .sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, stateB), globalPointA.x, globalPointA.y, globalPointA.z, 2, 0.0, 0.0, 0.0, 0.1);
            Vec3 position = JOMLConversion.toMojang(globalPointA);
            float volumeScale = 0.4F;
            SoundType soundType = stateB.getSoundType();
            this.level
               .playSound(
                  null,
                  position.x,
                  position.y,
                  position.z,
                  soundType.getStepSound(),
                  SoundSource.BLOCKS,
                  0.080000006F,
                  (float)(0.39999999999999997 + Math.random() * 0.4)
               );
            this.level
               .playSound(null, position.x, position.y, position.z, soundType.getHitSound(), SoundSource.BLOCKS, 0.080000006F, (float)(Math.random() * 0.4));
            this.level
               .playSound(
                  null, position.x, position.y, position.z, soundType.getPlaceSound(), SoundSource.BLOCKS, 0.080000006F, (float)(0.3 + Math.random() * 0.4)
               );
         }
      }
   }

   @Override
   public Vector3d getLinearVelocity(PhysicsPipelineBody body, Vector3d dest) {
      return !body.isRemoved() ? this.sable$orig$getLinearVelocity(body, dest) : dest.zero();
   }

   @Override
   public Vector3d getAngularVelocity(PhysicsPipelineBody body, Vector3d dest) {
      return !body.isRemoved() ? this.sable$orig$getAngularVelocity(body, dest) : dest.zero();
   }

   private record TrackedKinematicContraption(
      Vector3d lastUploadedPosition, Quaterniond lastUploadedOrientation, Vector3d lastUploadedLinVel, Vector3d lastUploadedAngVel, int id
   ) {
   }
}
