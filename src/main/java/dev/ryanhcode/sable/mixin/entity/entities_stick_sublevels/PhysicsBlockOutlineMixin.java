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
   private void sable$drawPhysicsBlockOutlines(Entity entity, float partialTicks, boolean showAll, CallbackInfo ci) {
      Minecraft minecraft = Minecraft.getInstance();
      ClientLevel level = minecraft.level;
      if (level != null) {
         SubLevelContainer container = ((SubLevelContainerHolder)level).sable$getPlotContainer();
         if (container != null) {
            Vec3 cameraPos = minecraft.gameRenderer.getMainCamera().position();
            int boxesLeft = 4096;
            ArrayList<SubLevel> subLevels = new ArrayList<>();
            container.getAllSubLevels().forEach(subLevels::add);

            for (SubLevel subLevel : subLevels) {
               if (boxesLeft <= 0) {
                  break;
               }

               if (subLevel instanceof ClientSubLevel clientSubLevel) {
                  ClientLevelPlot plot = clientSubLevel.getPlot();
                  if (plot != null) {
                     Pose3dc renderPose = clientSubLevel.renderPose(partialTicks);

                     for (PlotChunkHolder chunkHolder : plot.getLoadedChunks()) {
                        if (boxesLeft <= 0) {
                           break;
                        }

                        LevelChunk chunk = chunkHolder.getChunk();
                        if (chunk != null) {
                           ChunkPos chunkPos = chunk.getPos();
                           LevelChunkSection[] sections = chunk.getSections();

                           for (int sectionIndex = 0; sectionIndex < sections.length && boxesLeft > 0; sectionIndex++) {
                              LevelChunkSection section = sections[sectionIndex];
                              if (section != null && !section.hasOnlyAir()) {
                                 int sectionBaseY = chunk.getSectionYFromSectionIndex(sectionIndex) << 4;
                                 AABB sectionBounds = sable$transformedSectionBounds(renderPose, chunkPos, sectionBaseY);
                                 if (!(sectionBounds.distanceToSqr(cameraPos) > 16384.0)) {
                                    for (int y = 0; y < 16 && boxesLeft > 0; y++) {
                                       for (int z = 0; z < 16 && boxesLeft > 0; z++) {
                                          for (int x = 0; x < 16 && boxesLeft > 0; x++) {
                                             if (!section.getBlockState(x, y, z).isAir()) {
                                                int worldX = (chunkPos.x() << 4) + x;
                                                int worldY = sectionBaseY + y;
                                                int worldZ = (chunkPos.z() << 4) + z;
                                                Vec3[] corners = new Vec3[8];
                                                int cornerIndex = 0;

                                                for (int cornerOffsetX = 0; cornerOffsetX <= 1; cornerOffsetX++) {
                                                   for (int cornerOffsetY = 0; cornerOffsetY <= 1; cornerOffsetY++) {
                                                      for (int cornerOffsetZ = 0; cornerOffsetZ <= 1; cornerOffsetZ++) {
                                                         Vector3d transformedPos = new Vector3d(worldX + cornerOffsetX, worldY + cornerOffsetY, worldZ + cornerOffsetZ);
                                                         renderPose.transformPosition(transformedPos);
                                                         corners[cornerIndex++] = new Vec3(transformedPos.x, transformedPos.y, transformedPos.z);
                                                      }
                                                   }
                                                }

                                                Gizmos.addGizmo(new PhysicsBlockOutlineMixin.Sable$PhysicsBox(corners, GizmoStyle.stroke(-16711936)));
                                                boxesLeft--;
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
   private static AABB sable$transformedSectionBounds(Pose3dc pose, ChunkPos chunkPos, int sectionBaseY) {
      int minBlockX = chunkPos.x() << 4;
      int minBlockZ = chunkPos.z() << 4;
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double minZ = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      double maxZ = Double.NEGATIVE_INFINITY;

      for (byte offsetX = 0; offsetX <= 16; offsetX += 16) {
         for (byte offsetY = 0; offsetY <= 16; offsetY += 16) {
            for (byte offsetZ = 0; offsetZ <= 16; offsetZ += 16) {
               Vector3d transformedPos = pose.transformPosition(new Vector3d(minBlockX + offsetX, sectionBaseY + offsetY, minBlockZ + offsetZ));
               minX = Math.min(minX, transformedPos.x);
               minY = Math.min(minY, transformedPos.y);
               minZ = Math.min(minZ, transformedPos.z);
               maxX = Math.max(maxX, transformedPos.x);
               maxY = Math.max(maxY, transformedPos.y);
               maxZ = Math.max(maxZ, transformedPos.z);
            }
         }
      }

      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }

   @Unique
   private record Sable$PhysicsBox(Vec3[] corners, GizmoStyle style) implements Gizmo {
      @Override
      public void emit(GizmoPrimitives primitives, float alpha) {
         if (this.style.hasFill()) {
            int fillColor = this.style.multipliedFill(alpha);
            primitives.addQuad(this.corners[4], this.corners[6], this.corners[7], this.corners[5], fillColor);
            primitives.addQuad(this.corners[0], this.corners[1], this.corners[3], this.corners[2], fillColor);
            primitives.addQuad(this.corners[0], this.corners[2], this.corners[6], this.corners[4], fillColor);
            primitives.addQuad(this.corners[1], this.corners[5], this.corners[7], this.corners[3], fillColor);
            primitives.addQuad(this.corners[0], this.corners[4], this.corners[5], this.corners[1], fillColor);
            primitives.addQuad(this.corners[2], this.corners[3], this.corners[7], this.corners[6], fillColor);
         }

         if (this.style.hasStroke()) {
            int strokeColor = this.style.multipliedStroke(alpha);
            float strokeWidth = this.style.strokeWidth();
            primitives.addLine(this.corners[0], this.corners[4], strokeColor, strokeWidth);
            primitives.addLine(this.corners[0], this.corners[2], strokeColor, strokeWidth);
            primitives.addLine(this.corners[0], this.corners[1], strokeColor, strokeWidth);
            primitives.addLine(this.corners[4], this.corners[6], strokeColor, strokeWidth);
            primitives.addLine(this.corners[6], this.corners[2], strokeColor, strokeWidth);
            primitives.addLine(this.corners[2], this.corners[3], strokeColor, strokeWidth);
            primitives.addLine(this.corners[3], this.corners[1], strokeColor, strokeWidth);
            primitives.addLine(this.corners[1], this.corners[5], strokeColor, strokeWidth);
            primitives.addLine(this.corners[5], this.corners[4], strokeColor, strokeWidth);
            primitives.addLine(this.corners[3], this.corners[7], strokeColor, strokeWidth);
            primitives.addLine(this.corners[5], this.corners[7], strokeColor, strokeWidth);
            primitives.addLine(this.corners[6], this.corners[7], strokeColor, strokeWidth);
         }
      }
   }
}
