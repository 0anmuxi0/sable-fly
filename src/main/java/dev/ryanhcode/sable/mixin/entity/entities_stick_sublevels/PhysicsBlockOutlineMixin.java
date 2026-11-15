package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import java.util.ArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.Gizmo;
import net.minecraft.gizmos.GizmoPrimitives;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({EntityHitboxDebugRenderer.class})
public abstract class PhysicsBlockOutlineMixin {
   @Unique
   private static final int SABLE$MAX_BOXES = 4096;
   @Unique
   private static final double SABLE$MAX_DISTANCE_SQR = 16384.0;

   @Inject(
      method = {"showHitboxes"},
      at = {@At("TAIL")}
   )
   private void sable$drawPhysicsBlockOutlines(Entity var1, float var2, boolean var3, CallbackInfo var4) {
      Minecraft var5 = Minecraft.getInstance();
      ClientLevel var6 = var5.level;
      if (var6 != null) {
         SubLevelContainer var7 = ((SubLevelContainerHolder)var6).sable$getPlotContainer();
         if (var7 != null) {
            Vec3 var8 = var5.gameRenderer.mainCamera().position();
            int var9 = 4096;
            ArrayList<SubLevel> var10 = new ArrayList<>();
            var7.getAllSubLevels().forEach(var10::add);

            for (SubLevel var12 : var10) {
               if (var9 <= 0) {
                  break;
               }

               if (var12 instanceof ClientSubLevel var13) {
                  ClientLevelPlot var14 = var13.getPlot();
                  if (var14 != null) {
                     Pose3dc var15 = var13.renderPose(var2);

                     for (PlotChunkHolder var17 : var14.getLoadedChunks()) {
                        if (var9 <= 0) {
                           break;
                        }

                        LevelChunk var18 = var17.getChunk();
                        if (var18 != null) {
                           ChunkPos var19 = var18.getPos();
                           LevelChunkSection[] var20 = var18.getSections();

                           for (int var21 = 0; var21 < var20.length && var9 > 0; var21++) {
                              LevelChunkSection var22 = var20[var21];
                              if (var22 != null && !var22.hasOnlyAir()) {
                                 int var23 = var18.getSectionYFromSectionIndex(var21) << 4;
                                 AABB var24 = sable$transformedSectionBounds(var15, var19, var23);
                                 if (!(var24.distanceToSqr(var8) > 16384.0)) {
                                    for (int var25 = 0; var25 < 16 && var9 > 0; var25++) {
                                       for (int var26 = 0; var26 < 16 && var9 > 0; var26++) {
                                          for (int var27 = 0; var27 < 16 && var9 > 0; var27++) {
                                             if (!var22.getBlockState(var27, var25, var26).isAir()) {
                                                int var28 = (var19.x() << 4) + var27;
                                                int var29 = var23 + var25;
                                                int var30 = (var19.z() << 4) + var26;
                                                Vec3[] var31 = new Vec3[8];
                                                int var32 = 0;

                                                for (int var33 = 0; var33 <= 1; var33++) {
                                                   for (int var34 = 0; var34 <= 1; var34++) {
                                                      for (int var35 = 0; var35 <= 1; var35++) {
                                                         Vector3d var36 = new Vector3d(var28 + var33, var29 + var34, var30 + var35);
                                                         var15.transformPosition(var36);
                                                         var31[var32++] = new Vec3(var36.x, var36.y, var36.z);
                                                      }
                                                   }
                                                }

                                                Gizmos.addGizmo(new PhysicsBlockOutlineMixin.Sable$PhysicsBox(var31, GizmoStyle.stroke(-16711936)));
                                                var9--;
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Unique
   private static AABB sable$transformedSectionBounds(Pose3dc var0, ChunkPos var1, int var2) {
      int var3 = var1.x() << 4;
      int var4 = var1.z() << 4;
      double var5 = Double.POSITIVE_INFINITY;
      double var7 = Double.POSITIVE_INFINITY;
      double var9 = Double.POSITIVE_INFINITY;
      double var11 = Double.NEGATIVE_INFINITY;
      double var13 = Double.NEGATIVE_INFINITY;
      double var15 = Double.NEGATIVE_INFINITY;

      for (byte var17 = 0; var17 <= 16; var17 += 16) {
         for (byte var18 = 0; var18 <= 16; var18 += 16) {
            for (byte var19 = 0; var19 <= 16; var19 += 16) {
               Vector3d var20 = var0.transformPosition(new Vector3d(var3 + var17, var2 + var18, var4 + var19));
               var5 = Math.min(var5, var20.x);
               var7 = Math.min(var7, var20.y);
               var9 = Math.min(var9, var20.z);
               var11 = Math.max(var11, var20.x);
               var13 = Math.max(var13, var20.y);
               var15 = Math.max(var15, var20.z);
            }
         }
      }

      return new AABB(var5, var7, var9, var11, var13, var15);
   }

   @Unique
   private record Sable$PhysicsBox(Vec3[] corners, GizmoStyle style) implements Gizmo {
      @Override
      public void emit(GizmoPrimitives var1, float var2) {
         if (this.style.hasFill()) {
            int var3 = this.style.multipliedFill(var2);
            var1.addQuad(this.corners[4], this.corners[6], this.corners[7], this.corners[5], var3);
            var1.addQuad(this.corners[0], this.corners[1], this.corners[3], this.corners[2], var3);
            var1.addQuad(this.corners[0], this.corners[2], this.corners[6], this.corners[4], var3);
            var1.addQuad(this.corners[1], this.corners[5], this.corners[7], this.corners[3], var3);
            var1.addQuad(this.corners[0], this.corners[4], this.corners[5], this.corners[1], var3);
            var1.addQuad(this.corners[2], this.corners[3], this.corners[7], this.corners[6], var3);
         }

         if (this.style.hasStroke()) {
            int var5 = this.style.multipliedStroke(var2);
            float var4 = this.style.strokeWidth();
            var1.addLine(this.corners[0], this.corners[4], var5, var4);
            var1.addLine(this.corners[0], this.corners[2], var5, var4);
            var1.addLine(this.corners[0], this.corners[1], var5, var4);
            var1.addLine(this.corners[4], this.corners[6], var5, var4);
            var1.addLine(this.corners[6], this.corners[2], var5, var4);
            var1.addLine(this.corners[2], this.corners[3], var5, var4);
            var1.addLine(this.corners[3], this.corners[1], var5, var4);
            var1.addLine(this.corners[1], this.corners[5], var5, var4);
            var1.addLine(this.corners[5], this.corners[4], var5, var4);
            var1.addLine(this.corners[3], this.corners[7], var5, var4);
            var1.addLine(this.corners[5], this.corners[7], var5, var4);
            var1.addLine(this.corners[6], this.corners[7], var5, var4);
         }
      }
   }
}
