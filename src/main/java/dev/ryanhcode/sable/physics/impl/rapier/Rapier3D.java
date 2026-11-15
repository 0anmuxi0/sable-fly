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
            } catch (Throwable var9) {
               try {
                  is2.close();
               } catch (Throwable var6) {
                  var9.addSuppressed(var6);
               }

               throw var9;
            }

            is2.close();
         }
      } catch (Throwable var11) {
         Sable.LOGGER
            .error(
               "Sable has failed to load the natives needed for its Rapier pipeline. Please report with system details and logs to {}",
               "https://github.com/ryanhcode/sable/issues",
               var11
            );
         CrashReport crashReport = CrashReport.forThrowable(
            var11 instanceof UnsatisfiedLinkError && var11.getCause() != null ? var11.getCause() : var11, "Sable linking with Rapier natives"
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
   static native long initialize(double var0, double var2, double var4, double var6);

   @Internal
   static native void tick(long var0, double var2);

   @Internal
   static native void step(long var0, double var2);

   @Internal
   static native void createSubLevel(long var0, int var2, double[] var3);

   @Internal
   static native void removeSubLevel(long var0, int var2);

   @Internal
   public static native void createBox(long var0, int var2, double var3, double var5, double var7, double var9, double[] var11);

   @Internal
   public static native void removeBox(long var0, int var2);

   @Internal
   public static native void getPose(long var0, int var2, double[] var3);

   @Internal
   static native void setCenterOfMass(long var0, int var2, double var3, double var5, double var7);

   @Internal
   static native void setLocalBounds(long var0, int var2, int var3, int var4, int var5, int var6, int var7, int var8);

   @Internal
   static native void addChunk(long var0, int var2, int var3, int var4, int[] var5, boolean var6, int var7);

   @Internal
   static native void removeChunk(long var0, int var2, int var3, int var4, boolean var5);

   @Internal
   public static native void changeBlock(long var0, int var2, int var3, int var4, int var5);

   @Internal
   private static native int newVoxelCollider(double var0, double var2, double var4, boolean var6, BlockSubLevelCollisionCallback var7);

   @Internal
   public static native void addVoxelColliderBox(int var0, double[] var1);

   @Internal
   public static native void clearVoxelColliderBoxes(int var0);

   @Internal
   private static native void setMassProperties(long var0, int var2, double var3, double[] var5, double[] var6);

   @Internal
   public static RapierVoxelColliderData createVoxelColliderEntry(
      double frictionMultiplier, double volume, double restitution, boolean isFluid, BlockSubLevelCollisionCallback contactEvents
   ) {
      return new RapierVoxelColliderData(newVoxelCollider(frictionMultiplier, volume, restitution, isFluid, contactEvents));
   }

   @Internal
   static native void teleportObject(long var0, int var2, double var3, double var5, double var7, double var9, double var11, double var13, double var15);

   @Internal
   public static native void wakeUpObject(long var0, int var2);

   @Internal
   public static native long addRotaryConstraint(
      long var0,
      int var2,
      int var3,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      double var14,
      double var16,
      double var18,
      double var20,
      double var22,
      double var24,
      double var26
   );

   @Internal
   public static native long addFixedConstraint(
      long var0,
      int var2,
      int var3,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      double var14,
      double var16,
      double var18,
      double var20,
      double var22
   );

   @Internal
   public static native long addFreeConstraint(
      long var0,
      int var2,
      int var3,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      double var14,
      double var16,
      double var18,
      double var20,
      double var22
   );

   @Internal
   public static native long addGenericConstraint(
      long var0,
      int var2,
      int var3,
      double var4,
      double var6,
      double var8,
      double var10,
      double var12,
      double var14,
      double var16,
      double var18,
      double var20,
      double var22,
      double var24,
      double var26,
      double var28,
      double var30,
      int var32
   );

   @Internal
   public static native void setConstraintFrame(
      long var0, long var2, int var4, double var5, double var7, double var9, double var11, double var13, double var15, double var17
   );

   @Internal
   public static native void setConstraintContactsEnabled(long var0, long var2, boolean var4);

   @Internal
   public static native void getConstraintImpulses(long var0, long var2, double[] var4);

   @Internal
   public static native boolean isConstraintValid(long var0, long var2);

   @Internal
   public static native void removeConstraint(long var0, long var2);

   @Internal
   public static native void setConstraintMotor(long var0, long var2, int var4, double var5, double var7, double var9, boolean var11, double var12);

   @Internal
   public static native void setConstraintLimit(long var0, long var2, int var4, double var5, double var7);

   @Internal
   public static native void lockConstraintAxes(long var0, long var2, byte var4);

   @Internal
   public static native void addLinearAngularVelocities(
      long var0, int var2, double var3, double var5, double var7, double var9, double var11, double var13, boolean var15
   );

   @Internal
   static native double[] clearCollisions(long var0);

   @Internal
   static native void applyForce(long var0, int var2, double var3, double var5, double var7, double var9, double var11, double var13, boolean var15);

   @Internal
   static native void applyForceAndTorque(long var0, int var2, double var3, double var5, double var7, double var9, double var11, double var13, boolean var15);

   @Internal
   static native void getLinearVelocity(long var0, int var2, double[] var3);

   @Internal
   static native void getAngularVelocity(long var0, int var2, double[] var3);

   @Internal
   static native void createKinematicContraption(long var0, int var2, int var3, double[] var4);

   @Internal
   static native void removeKinematicContraption(long var0, int var2);

   @Internal
   static native void setKinematicContraptionTransform(long var0, int var2, double[] var3, double[] var4, double[] var5);

   @Internal
   static native void addKinematicContraptionChunkSection(long var0, int var2, int var3, int var4, int var5, int[] var6);

   @Internal
   public static native long createRope(long var0, double var2, double var4, double[] var6, int var7);

   @Internal
   public static native long removeRope(long var0, long var2);

   @Internal
   public static native void setRopeAttachment(long var0, long var2, int var4, double var5, double var7, double var9, boolean var11);

   @Internal
   public static native void addRopePointAtStart(long var0, long var2, double var4, double var6, double var8);

   @Internal
   public static native void removeRopePointAtStart(long var0, long var2);

   @Internal
   public static native void wakeUpRope(long var0, long var2);

   @Internal
   public static native void setRopeFirstSegmentLength(long var0, long var2, double var4);

   @Internal
   public static native double[] queryRope(long var0, long var2);

   @Internal
   static native void configFrequencyAndDamping(double var0, double var2);

   @Internal
   static native void configSolverIterations(int var0, int var1, int var2);

   @Internal
   static native void configMinIslandSize(int var0);

   @Internal
   static native void dispose(long var0);

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
