package dev.ryanhcode.sable.physics.impl.rapier;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineBody;
import dev.ryanhcode.sable.api.physics.callback.BlockSubLevelCollisionCallback;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.physics.impl.rapier.collider.RapierVoxelColliderData;
import dev.ryanhcode.sable.platform.SableLoaderPlatform;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import net.jpountz.lz4.LZ4FrameInputStream;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Util;
import net.minecraft.util.Util.OS;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.joml.Matrix3dc;
import org.joml.Vector3dc;

@Internal
public final class Rapier3D {
   private static final Path NATIVE_DIR = resolveNativeDir();
   private static final String LIB_ZIP_NAME = "sable_rapier_binaries.zip.l4z";
   private static final String LIB_NAME = "sable_rapier";
   public static final String NATIVE_NAME = getNativeName();
   private static int countingObjectID = 0;

   private static Path resolveNativeDir() {
      Path gameDir = SableLoaderPlatform.INSTANCE.getGameDirectory();
      if (gameDir != null) {
         Path gameDirRelativeDir = gameDir.resolve(".sable").resolve("natives").normalize();
         Sable.LOGGER.info("Using game-dir-relative Rapier native directory {}", gameDirRelativeDir.toAbsolutePath());
         return gameDirRelativeDir;
      } else {
         Path fallbackDir = Paths.get(System.getProperty("user.home", System.getProperty("user.dir")), ".sable", "natives");
         Sable.LOGGER.info("Using fallback Rapier native directory {}", fallbackDir.toAbsolutePath());
         return fallbackDir;
      }
   }

   private static String getNativeName() {
      String arch;
      if (!System.getProperty("os.arch").equals("arm") && !System.getProperty("os.arch").startsWith("aarch64")) {
         arch = "x86_64";
      } else {
         arch = "aarch64";
      }

      OS os = Util.getPlatform();
      if (os == OS.WINDOWS) {
         return "sable_rapier_" + arch + "_windows.dll";
      } else if (os == OS.OSX) {
         return "sable_rapier_" + arch + "_macos.dylib";
      } else {
         if (os != OS.LINUX) {
            Sable.LOGGER
               .error("Unknown platform '{}' detected, sable will attempt to use linux natives, this may or may not work.", System.getProperty("os.name"));
         }

         return "sable_rapier_" + arch + "_linux.so";
      }
   }

   private static void loadLibrary() {
      try {
         try (InputStream is = Rapier3D.class.getResourceAsStream("/natives/sable_rapier/sable_rapier_binaries.zip.l4z")) {
            if (is == null) {
               throw new FileNotFoundException("sable_rapier_binaries.zip.l4z");
            }

            if (!Files.exists(NATIVE_DIR)) {
               Files.createDirectories(NATIVE_DIR);
            }

            LZ4FrameInputStream is2 = new LZ4FrameInputStream(is);

            try (ZipInputStream ti = new ZipInputStream(is2)) {
               while (true) {
                  ZipEntry entry;
                  if ((entry = ti.getNextEntry()) == null) {
                     throw new FileNotFoundException(NATIVE_NAME);
                  }

                  if (entry.getName().equals(NATIVE_NAME)) {
                     Path tempFile = NATIVE_DIR.resolve(NATIVE_NAME);
                     if (Files.exists(tempFile)) {
                        Files.delete(tempFile);
                     }

                     Files.createFile(tempFile);
                     Files.copy(ti, tempFile, StandardCopyOption.REPLACE_EXISTING);
                     System.load(tempFile.toAbsolutePath().toString());
                     break;
                  }
               }
            } catch (Throwable e) {
               try {
                  is2.close();
               } catch (Throwable closeException) {
                  e.addSuppressed(closeException);
               }

               throw e;
            }

            is2.close();
         }
      } catch (Throwable t) {
         Sable.LOGGER
            .error(
               "Sable has failed to load the natives needed for its Rapier pipeline. Please report with system details and logs to {}",
               "https://github.com/ryanhcode/sable/issues",
               t
            );
         CrashReport crashReport = CrashReport.forThrowable(
            t instanceof UnsatisfiedLinkError && t.getCause() != null ? t.getCause() : t, "Sable linking with Rapier natives"
         );
         CrashReportCategory category = crashReport.addCategory("Natives");
         category.setDetail("Name", NATIVE_NAME);
         category.setDetail("Native Directory", NATIVE_DIR);
         throw new ReportedException(crashReport);
      }
   }

   @Internal
   public static int getID(PhysicsPipelineBody body) {
      return body.getRuntimeId();
   }

   @Internal
   public static synchronized int nextBodyID() {
      return countingObjectID++;
   }

   @Internal
   public static long getSceneHandle(ServerLevel level) {
      if (SubLevelPhysicsSystem.require(level).getPipeline() instanceof RapierPhysicsPipeline rapierPipeline) {
         return rapierPipeline.getSceneHandle();
      } else {
         throw new IllegalStateException("ServerLevel does not use the Rapier physics pipeline");
      }
   }

   @Internal
   static native long initialize(double gravityX, double gravityY, double gravityZ, double universalDrag);

   @Internal
   static native void tick(long sceneHandle, double timeStep);

   @Internal
   static native void step(long sceneHandle, double timeStep);

   @Internal
   static native void createSubLevel(long sceneHandle, int id, double[] pose);

   @Internal
   static native void removeSubLevel(long sceneHandle, int id);

   @Internal
   public static native void createBox(long sceneHandle, int id, double mass, double halfExtentsX, double halfExtentsY, double halfExtentsZ, double[] pose);

   @Internal
   public static native void removeBox(long sceneHandle, int id);

   @Internal
   public static native void getPose(long sceneHandle, int id, double[] store);

   @Internal
   static native void setCenterOfMass(long sceneHandle, int id, double x, double y, double z);

   @Internal
   static native void setLocalBounds(long sceneHandle, int id, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

   @Internal
   static native void addChunk(long sceneHandle, int x, int y, int z, int[] chunk, boolean global, int id);

   @Internal
   static native void removeChunk(long sceneHandle, int x, int y, int z, boolean global);

   @Internal
   public static native void changeBlock(long sceneHandle, int x, int y, int z, int newState);

   @Internal
   private static native int newVoxelCollider(double frictionMultiplier, double volume, double restitution, boolean isFluid, BlockSubLevelCollisionCallback contactEvents);

   @Internal
   public static native void addVoxelColliderBox(int index, double[] bounds);

   @Internal
   public static native void clearVoxelColliderBoxes(int index);

   @Internal
   private static native void setMassProperties(long sceneHandle, int index, double mass, double[] centerOfMass, double[] inertiaTensor);

   @Internal
   public static RapierVoxelColliderData createVoxelColliderEntry(
      double frictionMultiplier, double volume, double restitution, boolean isFluid, BlockSubLevelCollisionCallback contactEvents
   ) {
      return new RapierVoxelColliderData(newVoxelCollider(frictionMultiplier, volume, restitution, isFluid, contactEvents));
   }

   @Internal
   static native void teleportObject(long sceneHandle, int id, double x, double y, double z, double orientationX, double orientationY, double orientationZ, double orientationW);

   @Internal
   public static native void wakeUpObject(long sceneHandle, int id);

   @Internal
   public static native long addRotaryConstraint(
      long sceneHandle,
      int id,
      int otherId,
      double localAnchorXA,
      double localAnchorYA,
      double localAnchorZA,
      double localAnchorXB,
      double localAnchorYB,
      double localAnchorZB,
      double localAxisXA,
      double localAxisYA,
      double localAxisZA,
      double localAxisXB,
      double localAxisYB,
      double localAxisZB
   );

   @Internal
   public static native long addFixedConstraint(
      long sceneHandle,
      int id,
      int otherId,
      double localAnchorXA,
      double localAnchorYA,
      double localAnchorZA,
      double localAnchorXB,
      double localAnchorYB,
      double localAnchorZB,
      double localOrientationXB,
      double localOrientationYB,
      double localOrientationZB,
      double localOrientationWB
   );

   @Internal
   public static native long addFreeConstraint(
      long sceneHandle,
      int id,
      int otherId,
      double localAnchorXA,
      double localAnchorYA,
      double localAnchorZA,
      double localAnchorXB,
      double localAnchorYB,
      double localAnchorZB,
      double localOrientationXB,
      double localOrientationYB,
      double localOrientationZB,
      double localOrientationWB
   );

   @Internal
   public static native long addGenericConstraint(
      long sceneHandle,
      int id,
      int otherId,
      double localAnchorXA,
      double localAnchorYA,
      double localAnchorZA,
      double localOrientationXA,
      double localOrientationYA,
      double localOrientationZA,
      double localOrientationWA,
      double localAnchorXB,
      double localAnchorYB,
      double localAnchorZB,
      double localOrientationXB,
      double localOrientationYB,
      double localOrientationZB,
      double localOrientationWB,
      int lockedAxesMask
   );

   @Internal
   public static native void setConstraintFrame(
      long sceneHandle, long handle, int side, double localPosX, double localPosY, double localPosZ, double localOrientationX, double localOrientationY, double localOrientationZ, double localOrientationW
   );

   @Internal
   public static native void setConstraintContactsEnabled(long sceneHandle, long handle, boolean contactsEnabled);

   @Internal
   public static native void getConstraintImpulses(long sceneHandle, long handle, double[] store);

   @Internal
   public static native boolean isConstraintValid(long sceneHandle, long handle);

   @Internal
   public static native void removeConstraint(long sceneHandle, long handle);

   @Internal
   public static native void setConstraintMotor(long sceneHandle, long handle, int axis, double desiredPosition, double stiffness, double damping, boolean hasForceLimit, double maxForce);

   @Internal
   public static native void setConstraintLimit(long sceneHandle, long handle, int axis, double min, double max);

   @Internal
   public static native void lockConstraintAxes(long sceneHandle, long handle, byte mask);

   @Internal
   public static native void addLinearAngularVelocities(
      long sceneHandle, int bodyId, double linearX, double linearY, double linearZ, double angularX, double angularY, double angularZ, boolean wakeUp
   );

   @Internal
   static native double[] clearCollisions(long sceneHandle);

   @Internal
   static native void applyForce(long sceneHandle, int bodyID, double x, double y, double z, double fx, double fy, double fz, boolean wakeUp);

   @Internal
   static native void applyForceAndTorque(long sceneHandle, int bodyID, double fx, double fy, double fz, double tx, double ty, double tz, boolean wakeUp);

   @Internal
   static native void getLinearVelocity(long sceneHandle, int bodyID, double[] store);

   @Internal
   static native void getAngularVelocity(long sceneHandle, int bodyID, double[] store);

   @Internal
   static native void createKinematicContraption(long sceneHandle, int mountId, int id, double[] pose);

   @Internal
   static native void removeKinematicContraption(long sceneHandle, int id);

   @Internal
   static native void setKinematicContraptionTransform(long sceneHandle, int id, double[] centerOfMass, double[] pose, double[] velocities);

   @Internal
   static native void addKinematicContraptionChunkSection(long sceneHandle, int id, int x, int y, int z, int[] data);

   @Internal
   public static native long createRope(long sceneHandle, double pointRadius, double firstJointLength, double[] points, int pointCount);

   @Internal
   public static native long removeRope(long sceneHandle, long ropeId);

   @Internal
   public static native void setRopeAttachment(long sceneHandle, long ropeId, int subLevelId, double x, double y, double z, boolean end);

   @Internal
   public static native void addRopePointAtStart(long sceneHandle, long ropeId, double x, double y, double z);

   @Internal
   public static native void removeRopePointAtStart(long sceneHandle, long ropeId);

   @Internal
   public static native void wakeUpRope(long sceneHandle, long ropeId);

   @Internal
   public static native void setRopeFirstSegmentLength(long sceneHandle, long ropeId, double firstSegmentLength);

   @Internal
   public static native double[] queryRope(long sceneHandle, long ropeId);

   @Internal
   static native void configFrequencyAndDamping(double contactNaturalFrequency, double contactDampingRatio);

   @Internal
   static native void configSolverIterations(int solverIterations, int pgsIterations, int stabilizationIterations);

   @Internal
   static native void configMinIslandSize(int islandSize);

   @Internal
   static native void dispose(long sceneHandle);

   @Internal
   static void setMassPropertiesFrom(long sceneHandle, int id, MassData massTracker) {
      Matrix3dc inertiaTensor = massTracker.getInertiaTensor();
      Vector3dc centerOfMass = massTracker.getCenterOfMass();
      double mass = massTracker.getMass();
      double[] centerOfMassArray = new double[]{centerOfMass.x(), centerOfMass.y(), centerOfMass.z()};
      double[] inertiaTensorArray = new double[]{
         inertiaTensor.m00(),
         inertiaTensor.m01(),
         inertiaTensor.m02(),
         inertiaTensor.m10(),
         inertiaTensor.m11(),
         inertiaTensor.m12(),
         inertiaTensor.m20(),
         inertiaTensor.m21(),
         inertiaTensor.m22()
      };
      setMassProperties(sceneHandle, id, mass, centerOfMassArray, inertiaTensorArray);
   }

   static {
      loadLibrary();
   }
}
