package dev.ryanhcode.sable.sublevel.render;

import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderPass.Draw;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.DynamicUniforms.ChunkSectionInfo;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayerGroup;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionMesh;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.chunk.SectionMesh.SectionDraw;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSectionBufferSlice;
import net.minecraft.client.renderer.culling.Frustum;
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

   private SubLevelBatchedTerrainRenderer() {
   }

   public static void beginSubmitFrame() {
      FRAME_VISIBLE_SECTIONS.clear();
      FRAME_GROUPS.clear();
   }

   public static boolean isEnabled() {
      return SableClientConfig.SUB_LEVEL_BATCHED_SECTIONS.get();
   }

   public static boolean isSectionBatchable(SectionRenderDispatcher dispatcher, RenderSection section) {
      if (dispatcher != null && section != null) {
         SectionMesh sectionMesh = section.getSectionMesh();
         if (sectionMesh != null && sectionMesh != CompiledSectionMesh.UNCOMPILED) {
            dispatcher.lock();

            try {
               for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                  if (sectionMesh.getSectionDraw(layer) != null && dispatcher.getRenderSectionSlice(sectionMesh, layer) != null) {
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

   public static void build(Matrix4fc modelViewMatrix, Vec3 cameraPos, SectionRenderDispatcher dispatcher, GpuTextureView textureView) {
      FRAME_GROUPS.clear();
      if (isEnabled() && modelViewMatrix != null && cameraPos != null && dispatcher != null && textureView != null) {
         int textureAtlasWidth = textureView.getWidth(0);
         int textureAtlasHeight = textureView.getHeight(0);
         DynamicUniforms dynamicUniforms = ownUniforms();
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
            if (container != null) {
               boolean occlusionCulling = SableClientConfig.SUB_LEVEL_OCCLUSION_CULLING.get();
               double renderDistance = SableClientConfig.SUB_LEVEL_RENDER_DISTANCE.get();
               double renderDistanceSq = renderDistance <= 0.0 ? Double.POSITIVE_INFINITY : renderDistance * renderDistance;
               Frustum frustum = Minecraft.getInstance().gameRenderer.getMainCamera().getCullFrustum();
               long currentMillis = Util.getMillis();
               boolean shouldResort = currentMillis - lastResortMillis >= 500L;
               if (shouldResort) {
                  lastResortMillis = currentMillis;
               }

               dispatcher.lock();

               try {
                  for (ClientSubLevel subLevel : container.getAllSubLevels()) {
                     ChunkSectionsToRender chunkSectionsToRender = sable$buildGroup(subLevel, modelViewMatrix, cameraPos, dispatcher, frustum, currentMillis, occlusionCulling, renderDistanceSq, textureView, textureAtlasWidth, textureAtlasHeight, shouldResort, dynamicUniforms);
                     if (chunkSectionsToRender != null) {
                        FRAME_GROUPS.add(chunkSectionsToRender);
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
      Matrix4fc modelView,
      Vec3 cameraPos,
      SectionRenderDispatcher dispatcher,
      Frustum frustum,
      long now,
      boolean occlusionCulling,
      double renderDistanceSq,
      GpuTextureView textureView,
      int textureAtlasWidth,
      int textureAtlasHeight,
      boolean shouldResort,
      DynamicUniforms dynamicUniforms
   ) {
      ClientLevelPlot plot = subLevel.getPlot();
      BoundingBox3ic boundingBox = plot.getBoundingBox();
      if (boundingBox != null && boundingBox.volume() > 0) {
         if (!(subLevel.getRenderData() instanceof VanillaChunkedSubLevelRenderData renderData)) {
            return null;
         } else {
            Pose3dc renderPose = subLevel.renderPose();
            Vector3dc position = renderPose.position();
            Vector3dc rotationPoint = renderPose.rotationPoint();
            Vector3dc scale = renderPose.scale();
            Vec3 transformedCenter = renderPose.transformPosition(
               new Vec3((boundingBox.minX() + boundingBox.maxX() + 1) * 0.5, (boundingBox.minY() + boundingBox.maxY() + 1) * 0.5, (boundingBox.minZ() + boundingBox.maxZ() + 1) * 0.5)
            );
            if (transformedCenter.distanceToSqr(cameraPos) > renderDistanceSq) {
               return null;
            } else {
               LongSet visibleSections = occlusionCulling ? computeVisibleSections(plot, renderPose, cameraPos) : null;
               BlockPos cameraBlockPos = BlockPos.containing(cameraPos);
               double fractionalX = cameraPos.x - cameraBlockPos.getX();
               double fractionalY = cameraPos.y - cameraBlockPos.getY();
               double fractionalZ = cameraPos.z - cameraBlockPos.getZ();
               Matrix4f subLevelTransform = new Matrix4f()
                  .translate((float)(position.x() - cameraPos.x), (float)(position.y() - cameraPos.y), (float)(position.z() - cameraPos.z))
                  .rotate(new Quaternionf().set(renderPose.orientation()))
                  .scale((float)scale.x(), (float)scale.y(), (float)scale.z())
                  .translate((float)(cameraPos.x - rotationPoint.x()), (float)(cameraPos.y - rotationPoint.y()), (float)(cameraPos.z - rotationPoint.z()));
               Matrix4f modelViewMatrix = new Matrix4f(modelView);
               ArrayList<ChunkSectionInfo> sectionInfos = new ArrayList<>();
               EnumMap drawGroups = new EnumMap<>(ChunkSectionLayer.class);

               for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                  drawGroups.put(layer, new Int2ObjectOpenHashMap());
               }

               int largestIndexCount = 0;
               LongArrayList drawOrder = sable$sectionDrawOrder(plot, visibleSections, renderPose, cameraPos, frustum);

               for (int i = 0; i < drawOrder.size(); i++) {
                  long sectionPos = drawOrder.getLong(i);
                  RenderSection section = renderData.getRenderSection(SectionPos.of(sectionPos));
                  if (isSectionBatchable(dispatcher, section)) {
                     SectionMesh sectionMesh = section.getSectionMesh();
                     BlockPos renderOffset = section.getRenderOrigin();
                     int uboIndex = -1;
                     if (shouldResort && section.hasTranslucentGeometry()) {
                        section.resortTransparency(dispatcher);
                     }

                     for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        SectionDraw draw = sectionMesh.getSectionDraw(layer);
                        RenderSectionBufferSlice slice;
                        if (draw != null
                           && (slice = dispatcher.getRenderSectionSlice(sectionMesh, layer)) != null
                           && (!draw.hasCustomIndexBuffer() || slice.indexBuffer() != null)) {
                           if (uboIndex == -1) {
                              uboIndex = sectionInfos.size();
                              sectionInfos.add(
                                 new ChunkSectionInfo(
                                    modelViewMatrix,
                                    renderOffset.getX() + cameraBlockPos.getX(),
                                    renderOffset.getY() + cameraBlockPos.getY(),
                                    renderOffset.getZ() + cameraBlockPos.getZ(),
                                    section.getVisibility(now),
                                    textureAtlasWidth,
                                    textureAtlasHeight
                                 )
                              );
                           }

                           VertexFormat vertexFormat = layer.pipeline().getVertexFormat();
                           GpuBuffer vertexBuffer = slice.vertexBuffer();
                           int firstIndex;
                           IndexType indexType;
                           GpuBuffer indexBuffer;
                           if (draw.hasCustomIndexBuffer()) {
                              indexBuffer = slice.indexBuffer();
                              indexType = draw.indexType();
                              firstIndex = (int)(slice.indexBufferOffset() / indexType.bytes);
                           } else {
                              indexBuffer = null;
                              indexType = null;
                              firstIndex = 0;
                              largestIndexCount = Math.max(largestIndexCount, draw.indexCount());
                           }

                           int baseVertex = (int)(slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                           int finalUboIndex = uboIndex;
                           ((List)((Int2ObjectOpenHashMap)drawGroups.get(layer)).computeIfAbsent(uboIndex, k -> new ArrayList()))
                              .add(
                                 new Draw<GpuBufferSlice[]>(
                                    0, vertexBuffer, indexBuffer, indexType, firstIndex, draw.indexCount(), baseVertex, (sectionUbos, uploader) -> uploader.upload("ChunkSection", sectionUbos[finalUboIndex])
                                 )
                              );
                        }
                     }
                  }
               }

               if (sectionInfos.isEmpty()) {
                  return null;
               } else {
                  if (largestIndexCount > 0) {
                     RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS).getBuffer(largestIndexCount);
                  }

                  GpuBufferSlice[] chunkSectionInfos = dynamicUniforms.writeChunkSections(sectionInfos.toArray(new ChunkSectionInfo[0]));
                  return new ChunkSectionsToRender(textureView, drawGroups, largestIndexCount, chunkSectionInfos);
               }
            }
         }
      } else {
         return null;
      }
   }

   private static LongArrayList sable$sectionDrawOrder(LevelPlot plot, LongSet visibleSections, Pose3dc renderPose, Vec3 cameraPos, Frustum frustum) {
      Vec3 relativeCameraPos = renderPose.transformPositionInverse(cameraPos);
      LongArrayList drawOrder = new LongArrayList();

      for (PlotChunkHolder chunkHolder : plot.getLoadedChunks()) {
         LevelChunk chunk = chunkHolder.getChunk();
         if (chunk != null) {
            ChunkPos chunkPos = chunk.getPos();
            LevelChunkSection[] sections = chunk.getSections();

            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
               LevelChunkSection section = sections[sectionIndex];
               if (section != null && !section.hasOnlyAir()) {
                  int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
                  long sectionPos = SectionPos.asLong(chunkPos.x(), sectionY, chunkPos.z());
                  if ((visibleSections == null || visibleSections.contains(sectionPos)) && (frustum == null || frustum.isVisible(sectionBounds(renderPose, chunkPos, sectionY << 4)))) {
                     drawOrder.add(sectionPos);
                  }
               }
            }
         }
      }

      drawOrder.sort((leftSection, rightSection) -> sable$compareSectionDistance(leftSection, rightSection, relativeCameraPos));
      return drawOrder;
   }

   private static double sable$sectionDistanceSq(long sectionPos, Vec3 pos) {
      double dx = SectionPos.x(sectionPos) + 8.0 - pos.x;
      double dy = SectionPos.y(sectionPos) + 8.0 - pos.y;
      double dz = SectionPos.z(sectionPos) + 8.0 - pos.z;
      return dx * dx + dy * dy + dz * dz;
   }

   private static int sable$compareSectionDistance(long leftSection, long rightSection, Vec3 pos) {
      return Double.compare(sable$sectionDistanceSq(leftSection, pos), sable$sectionDistanceSq(rightSection, pos));
   }

   private static DynamicUniforms ownUniforms() {
      return RenderSystem.getDynamicUniforms();
   }

   public static void renderGroup(ChunkSectionLayerGroup layerGroup, GpuSampler sampler) {
      if (!insideRender && !FRAME_GROUPS.isEmpty()) {
         insideRender = true;

         try {
            for (int i = 0; i < FRAME_GROUPS.size(); i++) {
               FRAME_GROUPS.get(i).renderGroup(layerGroup, sampler);
            }
         } finally {
            insideRender = false;
         }
      }
   }

   public static LongSet computeVisibleSections(LevelPlot plot, Pose3dc renderPose, Vec3 cameraPos) {
      LongSet cached = FRAME_VISIBLE_SECTIONS.get(plot);
      if (cached != null) {
         return cached;
      } else {
         LongSet visible = sable$scanVisibleSections(plot, renderPose, cameraPos);
         FRAME_VISIBLE_SECTIONS.put(plot, visible);
         return visible;
      }
   }

   private static LongSet sable$scanVisibleSections(LevelPlot plot, Pose3dc renderPose, Vec3 cameraPos) {
      Long2ObjectOpenHashMap sectionsByPos = new Long2ObjectOpenHashMap();

      for (PlotChunkHolder chunkHolder : plot.getLoadedChunks()) {
         LevelChunk chunk = chunkHolder.getChunk();
         if (chunk != null) {
            LevelChunkSection[] sections = chunk.getSections();

            for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
               LevelChunkSection section = sections[sectionIndex];
               if (section != null && !section.hasOnlyAir()) {
                  sectionsByPos.put(SectionPos.asLong(chunk.getPos().x(), chunk.getSectionYFromSectionIndex(sectionIndex), chunk.getPos().z()), section);
               }
            }
         }
      }

      if (sectionsByPos.isEmpty()) {
         return LongSets.EMPTY_SET;
      } else {
         Vec3 relativeCameraPos = renderPose.transformPositionInverse(cameraPos);
         long startSectionPos = SectionPos.asLong(Mth.floor(relativeCameraPos.x) >> 4, Mth.floor(relativeCameraPos.y) >> 4, Mth.floor(relativeCameraPos.z) >> 4);
         if (!sectionsByPos.containsKey(startSectionPos)) {
            return new LongOpenHashSet(sectionsByPos.keySet());
         } else {
            LongOpenHashSet visibleSections = new LongOpenHashSet();
            LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
            visibleSections.add(startSectionPos);
            queue.enqueue(startSectionPos);

            while (!queue.isEmpty()) {
               long currentSection = queue.dequeueLong();
               LevelChunkSection section = (LevelChunkSection)sectionsByPos.get(currentSection);
               int sectionX = SectionPos.x(currentSection);
               int sectionY = SectionPos.y(currentSection);
               int sectionZ = SectionPos.z(currentSection);

               for (Direction direction : Direction.values()) {
                  long neighborPos;
                  if (!faceOpaque(section, direction)
                     && sectionsByPos.containsKey(neighborPos = SectionPos.asLong(sectionX + direction.getStepX(), sectionY + direction.getStepY(), sectionZ + direction.getStepZ()))
                     && visibleSections.add(neighborPos)) {
                     queue.enqueue(neighborPos);
                  }
               }
            }

            return visibleSections;
         }
      }
   }

   private static boolean faceOpaque(LevelChunkSection section, Direction direction) {
      switch (direction) {
         case DOWN:
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  if (!section.getBlockState(x, 0, z).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case UP:
            for (int x = 0; x < 16; x++) {
               for (int z = 0; z < 16; z++) {
                  if (!section.getBlockState(x, 15, z).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case NORTH:
            for (int x = 0; x < 16; x++) {
               for (int y = 0; y < 16; y++) {
                  if (!section.getBlockState(x, y, 0).canOcclude()) {
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
               for (int z = 0; z < 16; z++) {
                  if (!section.getBlockState(0, y, z).canOcclude()) {
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

   public static AABB sectionBounds(Pose3dc renderPose, ChunkPos chunkPos, int sectionBaseY) {
      int baseX = chunkPos.x() << 4;
      int baseZ = chunkPos.z() << 4;
      double minX = Double.POSITIVE_INFINITY;
      double minY = Double.POSITIVE_INFINITY;
      double minZ = Double.POSITIVE_INFINITY;
      double maxX = Double.NEGATIVE_INFINITY;
      double maxY = Double.NEGATIVE_INFINITY;
      double maxZ = Double.NEGATIVE_INFINITY;

      for (byte dx = 0; dx <= 16; dx += 16) {
         for (byte dy = 0; dy <= 16; dy += 16) {
            for (byte dz = 0; dz <= 16; dz += 16) {
               Vec3 cornerPos = renderPose.transformPosition(new Vec3(baseX + dx, sectionBaseY + dy, baseZ + dz));
               minX = Math.min(minX, cornerPos.x);
               minY = Math.min(minY, cornerPos.y);
               minZ = Math.min(minZ, cornerPos.z);
               maxX = Math.max(maxX, cornerPos.x);
               maxY = Math.max(maxY, cornerPos.y);
               maxZ = Math.max(maxZ, cornerPos.z);
            }
         }
      }

      return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
   }
}
