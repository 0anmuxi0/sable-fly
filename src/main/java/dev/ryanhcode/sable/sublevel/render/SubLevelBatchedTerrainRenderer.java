package dev.ryanhcode.sable.sublevel.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.commands.RenderPass.Draw;
import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.DynamicGpuData.ChunkSectionInfo;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender.DrawSeparate;
import net.minecraft.client.renderer.chunk.SectionMesh.SectionDraw;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSectionBufferSlice;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.oit.OitStage;
import net.minecraft.client.renderer.oit.OitRenderPassProvider.Parameters;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3dc;

public final class SubLevelBatchedTerrainRenderer {
   private static final List<ChunkSectionsToRender> FRAME_GROUPS = new ArrayList<>();
   private static boolean insideRender;
   private static final long RESORT_INTERVAL_MILLIS = 500L;
   private static long lastResortMillis;
   private static final Map<LevelPlot, LongSet> FRAME_VISIBLE_SECTIONS = new IdentityHashMap<>();
   private static DynamicGpuData ownUniforms;

   public static void beginSubmitFrame() {
      FRAME_VISIBLE_SECTIONS.clear();
      FRAME_GROUPS.clear();
   }

   private SubLevelBatchedTerrainRenderer() {
   }

   public static boolean isEnabled() {
      return SableClientConfig.SUB_LEVEL_BATCHED_SECTIONS.get();
   }

   public static boolean isSectionBatchable(SectionRenderDispatcher dispatcher, RenderSection renderSection) {
      if (dispatcher != null && renderSection != null) {
         SectionMesh mesh = renderSection.getSectionMesh();
         if (mesh != null && mesh != CompiledSectionMesh.UNCOMPILED) {
            dispatcher.lock();

            try {
               for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                  if (mesh.getSectionDraw(layer) != null && dispatcher.getRenderSectionSlice(mesh, layer) != null) {
                     return true;
                  }
               }

               return false;
            } finally {
               dispatcher.unlock();
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void build(
      Matrix4fc viewRotation, boolean reverseTranslucent, Vec3 cameraPos, long fadeMillis, SectionRenderDispatcher dispatcher, GpuTextureView atlasView
   ) {
      FRAME_GROUPS.clear();
      if (isEnabled() && viewRotation != null && cameraPos != null && dispatcher != null && atlasView != null) {
         int atlasWidth = atlasView.getWidth(0);
         int atlasHeight = atlasView.getHeight(0);
         DynamicGpuData uniforms = ownUniforms();
         uniforms.reset();
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container != null) {
               boolean occlusionCulling = SableClientConfig.SUB_LEVEL_OCCLUSION_CULLING.get();
               double renderDistance = SableClientConfig.SUB_LEVEL_RENDER_DISTANCE.get();
               double renderDistanceSq = renderDistance <= 0.0 ? Double.POSITIVE_INFINITY : renderDistance * renderDistance;
               Frustum frustum = Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum();
               long now = Util.getMillis();
               boolean resortTranslucency = now - lastResortMillis >= 500L;
               if (resortTranslucency) {
                  lastResortMillis = now;
               }

               dispatcher.lock();

               try {
                  for (ClientSubLevel subLevel : container.getAllSubLevels()) {
                     ChunkSectionsToRender group = sable$buildGroup(
                        subLevel,
                        viewRotation,
                        reverseTranslucent,
                        cameraPos,
                        fadeMillis,
                        dispatcher,
                        frustum,
                        now,
                        occlusionCulling,
                        renderDistanceSq,
                        atlasWidth,
                        atlasHeight,
                        resortTranslucency,
                        uniforms
                     );
                     if (group != null) {
                        FRAME_GROUPS.add(group);
                     }
                  }
               } finally {
                  dispatcher.unlock();
               }
            }
         }
      }
   }

   private static ChunkSectionsToRender sable$buildGroup(
      ClientSubLevel subLevel,
      Matrix4fc viewRotation,
      boolean reverseTranslucent,
      Vec3 cameraPos,
      long fadeMillis,
      SectionRenderDispatcher dispatcher,
      Frustum frustum,
      long now,
      boolean occlusionCulling,
      double renderDistanceSq,
      int atlasWidth,
      int atlasHeight,
      boolean resortTranslucency,
      DynamicGpuData uniforms
   ) {
      LevelPlot plot = subLevel.getPlot();
      BoundingBox3ic bounds = plot.getBoundingBox();
      if (bounds != null && bounds.volume() > 0) {
         if (!(subLevel.getRenderData() instanceof VanillaChunkedSubLevelRenderData chunkedRenderData)) {
            return null;
         } else {
            Pose3dc pose = subLevel.renderPose();
            Vector3dc position = pose.position();
            Vector3dc rotationPoint = pose.rotationPoint();
            Vector3dc scale = pose.scale();
            Vec3 worldCenter = pose.transformPosition(
               new Vec3((bounds.minX() + bounds.maxX() + 1) * 0.5, (bounds.minY() + bounds.maxY() + 1) * 0.5, (bounds.minZ() + bounds.maxZ() + 1) * 0.5)
            );
            if (worldCenter.distanceToSqr(cameraPos) > renderDistanceSq) {
               return null;
            } else {
               LongSet visibleSections = occlusionCulling ? computeVisibleSections(plot, pose, cameraPos) : null;
               int offX = (int)Math.round(position.x() - rotationPoint.x());
               int offY = (int)Math.round(position.y() - rotationPoint.y());
               int offZ = (int)Math.round(position.z() - rotationPoint.z());
               Matrix4f plotTransform = new Matrix4f()
                  .translate((float)(position.x() - cameraPos.x), (float)(position.y() - cameraPos.y), (float)(position.z() - cameraPos.z))
                  .rotate(new Quaternionf().set(pose.orientation()))
                  .scale((float)scale.x(), (float)scale.y(), (float)scale.z())
                  .translate(
                     (float)(cameraPos.x - rotationPoint.x() - offX),
                     (float)(cameraPos.y - rotationPoint.y() - offY),
                     (float)(cameraPos.z - rotationPoint.z() - offZ)
                  );
               Matrix4f modelView = new Matrix4f(viewRotation).mul(plotTransform);
               List<ChunkSectionInfo> infos = new ArrayList<>();
               Map<ChunkSectionLayer, List<Draw<GpuBufferSlice[]>>> drawsPerLayer = Util.makeEnumMap(ChunkSectionLayer.class, layerx -> new ArrayList<>());
               int maxIndices = 0;
               LongArrayList drawOrder = sable$sectionDrawOrder(plot, visibleSections, pose, cameraPos, frustum);

               for (int orderIndex = 0; orderIndex < drawOrder.size(); orderIndex++) {
                  long sectionNode = drawOrder.getLong(orderIndex);
                  RenderSection renderSection = chunkedRenderData.getRenderSection(SectionPos.of(sectionNode));
                  if (isSectionBatchable(dispatcher, renderSection)) {
                     SectionMesh mesh = renderSection.getSectionMesh();
                     BlockPos origin = renderSection.getRenderOrigin();
                     int infoIndex = -1;
                     if (resortTranslucency && renderSection.hasTranslucentGeometry()) {
                        renderSection.resortTransparency();
                     }

                     for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        SectionDraw sectionDraw = mesh.getSectionDraw(layer);
                        if (sectionDraw != null) {
                           RenderSectionBufferSlice slice = dispatcher.getRenderSectionSlice(mesh, layer);
                           if (slice != null && (!sectionDraw.hasCustomIndexBuffer() || slice.indexBuffer() != null)) {
                              if (infoIndex == -1) {
                                 infoIndex = infos.size();
                                 infos.add(
                                    new ChunkSectionInfo(
                                       origin.getX() + offX, origin.getY() + offY, origin.getZ() + offZ, renderSection.getVisibility(now, fadeMillis)
                                    )
                                 );
                              }

                              VertexFormat vertexFormat = layer.pipeline(false).getVertexFormatBinding(0);
                              GpuBuffer vertexBuffer = slice.vertexBuffer();
                              GpuBuffer indexBuffer;
                              IndexType indexType;
                              int firstIndex;
                              if (sectionDraw.hasCustomIndexBuffer()) {
                                 indexBuffer = slice.indexBuffer();
                                 indexType = sectionDraw.indexType();
                                 firstIndex = (int)(slice.indexBufferOffset() / indexType.bytes);
                              } else {
                                 indexBuffer = null;
                                 indexType = null;
                                 firstIndex = 0;
                                 maxIndices = Math.max(maxIndices, sectionDraw.indexCount());
                              }

                              int baseVertex = (int)(slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                              int uniformIndex = infoIndex;
                              drawsPerLayer.get(layer)
                                 .add(
                                    new Draw<>(
                                       0,
                                       vertexBuffer,
                                       indexBuffer,
                                       indexType,
                                       firstIndex,
                                       sectionDraw.indexCount(),
                                       baseVertex,
                                       (sectionInfos, uploader) -> uploader.setUniform("ChunkSection", sectionInfos[uniformIndex])
                                    )
                                 );
                           }
                        }
                     }
                  }
               }

               if (infos.isEmpty()) {
                  return null;
               } else {
                  for (ChunkSectionLayer layerx : ChunkSectionLayer.values()) {
                     if (reverseTranslucent && layerx.translucent()) {
                        drawsPerLayer.put(layerx, new ArrayList<>(drawsPerLayer.get(layerx).reversed()));
                     }
                  }

                  if (maxIndices > 0) {
                     RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).requestIndexCount(maxIndices);
                  }

                  GpuBufferSlice terrainTransform = uniforms.writeTerrainTransform(modelView, atlasWidth, atlasHeight);
                  GpuBufferSlice[] chunkSectionInfos = uniforms.writeChunkSections(infos.toArray(new ChunkSectionInfo[0]));
                  return new DrawSeparate(terrainTransform, drawsPerLayer, maxIndices, chunkSectionInfos);
               }
            }
         }
      } else {
         return null;
      }
   }

   private static LongArrayList sable$sectionDrawOrder(LevelPlot plot, LongSet visibleSections, Pose3dc pose, Vec3 cameraPos, Frustum frustum) {
      Vec3 cameraPlot = pose.transformPositionInverse(cameraPos);
      LongArrayList nodes = new LongArrayList();

      for (PlotChunkHolder holder : plot.getLoadedChunks()) {
         LevelChunk chunk = holder.getChunk();
         if (chunk != null) {
            ChunkPos chunkPos = chunk.getPos();
            LevelChunkSection[] sections = chunk.getSections();

            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
               LevelChunkSection section = sections[sectionIndex];
               if (section != null && !section.hasOnlyAir()) {
                  int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
                  long sectionNode = SectionPos.asLong(chunkPos.x, sectionY, chunkPos.z);
                  if ((visibleSections == null || visibleSections.contains(sectionNode))
                     && (frustum == null || frustum.isVisible(sectionBounds(pose, chunkPos, sectionY << 4)))) {
                     nodes.add(sectionNode);
                  }
               }
            }
         }
      }

      nodes.sort((a, b) -> sable$compareSectionDistance(a, b, cameraPlot));
      return nodes;
   }

   private static double sable$sectionDistanceSq(long sectionNode, Vec3 cameraPlot) {
      double dx = SectionPos.x(sectionNode) + 8.0 - cameraPlot.x;
      double dy = SectionPos.y(sectionNode) + 8.0 - cameraPlot.y;
      double dz = SectionPos.z(sectionNode) + 8.0 - cameraPlot.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private static int sable$compareSectionDistance(long a, long b, Vec3 cameraPlot) {
      return Double.compare(sable$sectionDistanceSq(a, cameraPlot), sable$sectionDistanceSq(b, cameraPlot));
   }

   private static DynamicGpuData ownUniforms() {
      if (ownUniforms == null) {
         ownUniforms = new DynamicGpuData();
      }

      return ownUniforms;
   }

   public static void renderGroup(ChunkSectionLayerGroup layerGroup, RenderPass renderPass, GpuSampler sampler, GpuTextureView atlasView, boolean wireframe) {
      if (!insideRender && !FRAME_GROUPS.isEmpty()) {
         insideRender = true;

         try {
            for (int i = 0; i < FRAME_GROUPS.size(); i++) {
               FRAME_GROUPS.get(i).renderGroup(layerGroup, renderPass, sampler, atlasView, wireframe);
            }
         } finally {
            insideRender = false;
         }
      }
   }

   public static void renderOit(GpuSampler sampler, OitStage stage, Parameters parameters, GpuTextureView colorView, GpuTextureView depthView) {
      if (!insideRender && !FRAME_GROUPS.isEmpty()) {
         insideRender = true;

         try {
            for (int i = 0; i < FRAME_GROUPS.size(); i++) {
               FRAME_GROUPS.get(i).renderOit(sampler, stage, parameters, colorView, depthView);
            }
         } finally {
            insideRender = false;
         }
      }
   }

   public static LongSet computeVisibleSections(LevelPlot plot, Pose3dc pose, Vec3 cameraPos) {
      LongSet cached = FRAME_VISIBLE_SECTIONS.get(plot);
      if (cached != null) {
         return cached;
      } else {
         LongSet visible = sable$scanVisibleSections(plot, pose, cameraPos);
         FRAME_VISIBLE_SECTIONS.put(plot, visible);
         return visible;
      }
   }

   private static LongSet sable$scanVisibleSections(LevelPlot plot, Pose3dc pose, Vec3 cameraPos) {
      Long2ObjectMap<LevelChunkSection> sections = new Long2ObjectOpenHashMap();

      for (PlotChunkHolder holder : plot.getLoadedChunks()) {
         LevelChunk chunk = holder.getChunk();
         if (chunk != null) {
            LevelChunkSection[] chunkSections = chunk.getSections();

            for (int i = 0; i < chunkSections.length; i++) {
               LevelChunkSection section = chunkSections[i];
               if (section != null && !section.hasOnlyAir()) {
                  sections.put(SectionPos.asLong(chunk.getPos().x, chunk.getSectionYFromSectionIndex(i), chunk.getPos().z), section);
               }
            }
         }
      }

      if (sections.isEmpty()) {
         return LongSets.emptySet();
      } else {
         Vec3 cameraPlot = pose.transformPositionInverse(cameraPos);
         long start = SectionPos.asLong(Mth.floor(cameraPlot.x) >> 4, Mth.floor(cameraPlot.y) >> 4, Mth.floor(cameraPlot.z) >> 4);
         if (!sections.containsKey(start)) {
            return new LongOpenHashSet(sections.keySet());
         } else {
            LongSet visible = new LongOpenHashSet();
            LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
            visible.add(start);
            queue.enqueue(start);

            while (!queue.isEmpty()) {
               long node = queue.dequeueLong();
               LevelChunkSection section = (LevelChunkSection)sections.get(node);
               int x = SectionPos.x(node);
               int y = SectionPos.y(node);
               int z = SectionPos.z(node);

               for (Direction direction : Direction.values()) {
                  if (!faceOpaque(section, direction)) {
                     long next = SectionPos.asLong(x + direction.getStepX(), y + direction.getStepY(), z + direction.getStepZ());
                     if (sections.containsKey(next) && visible.add(next)) {
                        queue.enqueue(next);
                     }
                  }
               }
            }

            return visible;
         }
      }
   }

   private static boolean faceOpaque(LevelChunkSection section, Direction direction) {
      switch (direction) {
         case DOWN:
            for (int x = 0; x < 16; x++) {
               for (int zxxx = 0; zxxx < 16; zxxx++) {
                  if (!section.getBlockState(x, 0, zxxx).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case UP:
            for (int x = 0; x < 16; x++) {
               for (int zxx = 0; zxx < 16; zxx++) {
                  if (!section.getBlockState(x, 15, zxx).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case NORTH:
            for (int x = 0; x < 16; x++) {
               for (int yx = 0; yx < 16; yx++) {
                  if (!section.getBlockState(x, yx, 0).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case SOUTH:
            for (int x = 0; x < 16; x++) {
               for (int y = 0; y < 16; y++) {
                  if (!section.getBlockState(x, y, 15).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case WEST:
            for (int y = 0; y < 16; y++) {
               for (int zx = 0; zx < 16; zx++) {
                  if (!section.getBlockState(0, y, zx).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case EAST:
            for (int y = 0; y < 16; y++) {
               for (int z = 0; z < 16; z++) {
                  if (!section.getBlockState(15, y, z).canOcclude()) {
                     return false;
                  }
               }
            }
      }

      return true;
   }

   public static AABB sectionBounds(Pose3dc pose, ChunkPos chunkPos, int sectionMinY) {
      int minX = chunkPos.x << 4;
      int minZ = chunkPos.z << 4;
      double x0 = Double.POSITIVE_INFINITY;
      double y0 = Double.POSITIVE_INFINITY;
      double z0 = Double.POSITIVE_INFINITY;
      double x1 = Double.NEGATIVE_INFINITY;
      double y1 = Double.NEGATIVE_INFINITY;
      double z1 = Double.NEGATIVE_INFINITY;

      for (int dx = 0; dx <= 16; dx += 16) {
         for (int dy = 0; dy <= 16; dy += 16) {
            for (int dz = 0; dz <= 16; dz += 16) {
               Vec3 corner = pose.transformPosition(new Vec3(minX + dx, sectionMinY + dy, minZ + dz));
               x0 = Math.min(x0, corner.x);
               y0 = Math.min(y0, corner.y);
               z0 = Math.min(z0, corner.z);
               x1 = Math.max(x1, corner.x);
               y1 = Math.max(y1, corner.y);
               z1 = Math.max(z1, corner.z);
            }
         }
      }

      return new AABB(x0, y0, z0, x1, y1, z1);
   }
}
