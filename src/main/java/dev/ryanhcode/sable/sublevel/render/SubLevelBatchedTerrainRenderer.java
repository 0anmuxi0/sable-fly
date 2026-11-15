package dev.ryanhcode.sable.sublevel.render;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.PrimitiveTopology;
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

   public static boolean isSectionBatchable(SectionRenderDispatcher var0, RenderSection var1) {
      if (var0 != null && var1 != null) {
         SectionMesh var2 = var1.getSectionMesh();
         if (var2 != null && var2 != CompiledSectionMesh.UNCOMPILED) {
            var0.lock();

            try {
               for (ChunkSectionLayer var6 : ChunkSectionLayer.values()) {
                  if (var2.getSectionDraw(var6) != null && var0.getRenderSectionSlice(var2, var6) != null) {
                     return true;
                  }
               }

               return false;
            } finally {
               var0.unlock();
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void build(Matrix4fc var0, Vec3 var1, SectionRenderDispatcher var2, GpuTextureView var3) {
      FRAME_GROUPS.clear();
      if (isEnabled() && var0 != null && var1 != null && var2 != null && var3 != null) {
         int var4 = var3.getWidth(0);
         int var5 = var3.getHeight(0);
         DynamicUniforms var6 = ownUniforms();
         ClientLevel var7 = Minecraft.getInstance().level;
         if (var7 != null) {
            ClientSubLevelContainer var8 = SubLevelContainer.getContainer(var7);
            if (var8 != null) {
               boolean var9 = SableClientConfig.SUB_LEVEL_OCCLUSION_CULLING.get();
               double var10 = SableClientConfig.SUB_LEVEL_RENDER_DISTANCE.get();
               double var12 = var10 <= 0.0 ? Double.POSITIVE_INFINITY : var10 * var10;
               Frustum var14 = Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum();
               long var15 = Util.getMillis();
               boolean var17 = var15 - lastResortMillis >= 500L;
               if (var17) {
                  lastResortMillis = var15;
               }

               var2.lock();

               try {
                  for (ClientSubLevel var19 : var8.getAllSubLevels()) {
                     ChunkSectionsToRender var20 = sable$buildGroup(var19, var0, var1, var2, var14, var15, var9, var12, var3, var4, var5, var17, var6);
                     if (var20 != null) {
                        FRAME_GROUPS.add(var20);
                     }
                  }
               } finally {
                  var2.unlock();
               }
            }
         }
      }
   }

   private static ChunkSectionsToRender sable$buildGroup(
      ClientSubLevel var0,
      Matrix4fc var1,
      Vec3 var2,
      SectionRenderDispatcher var3,
      Frustum var4,
      long var5,
      boolean var7,
      double var8,
      GpuTextureView var10,
      int var11,
      int var12,
      boolean var13,
      DynamicUniforms var14
   ) {
      ClientLevelPlot var15 = var0.getPlot();
      BoundingBox3ic var16 = var15.getBoundingBox();
      if (var16 != null && var16.volume() > 0) {
         if (!(var0.getRenderData() instanceof VanillaChunkedSubLevelRenderData var17)) {
            return null;
         } else {
            Pose3dc var57 = var0.renderPose();
            Vector3dc var19 = var57.position();
            Vector3dc var20 = var57.rotationPoint();
            Vector3dc var21 = var57.scale();
            Vec3 var22 = var57.transformPosition(
               new Vec3((var16.minX() + var16.maxX() + 1) * 0.5, (var16.minY() + var16.maxY() + 1) * 0.5, (var16.minZ() + var16.maxZ() + 1) * 0.5)
            );
            if (var22.distanceToSqr(var2) > var8) {
               return null;
            } else {
               LongSet var23 = var7 ? computeVisibleSections(var15, var57, var2) : null;
               BlockPos var24 = BlockPos.containing(var2);
               double var25 = var2.x - var24.getX();
               double var27 = var2.y - var24.getY();
               double var29 = var2.z - var24.getZ();
               Matrix4f var31 = new Matrix4f()
                  .translate((float)(var19.x() - var2.x), (float)(var19.y() - var2.y), (float)(var19.z() - var2.z))
                  .rotate(new Quaternionf().set(var57.orientation()))
                  .scale((float)var21.x(), (float)var21.y(), (float)var21.z())
                  .translate((float)(var2.x - var20.x()), (float)(var2.y - var20.y()), (float)(var2.z - var20.z()));
               Matrix4f var32 = new Matrix4f(var1);
               ArrayList<ChunkSectionInfo> var33 = new ArrayList<>();
               EnumMap var34 = new EnumMap<>(ChunkSectionLayer.class);

               for (ChunkSectionLayer var38 : ChunkSectionLayer.values()) {
                  var34.put(var38, new Int2ObjectOpenHashMap());
               }

               int var58 = 0;
               LongArrayList var59 = sable$sectionDrawOrder(var15, var23, var57, var2, var4);

               for (int var60 = 0; var60 < var59.size(); var60++) {
                  long var62 = var59.getLong(var60);
                  RenderSection var40 = var17.getRenderSection(SectionPos.of(var62));
                  if (isSectionBatchable(var3, var40)) {
                     SectionMesh var41 = var40.getSectionMesh();
                     BlockPos var42 = var40.getRenderOrigin();
                     int var43 = -1;
                     if (var13 && var40.hasTranslucentGeometry()) {
                        var40.resortTransparency();
                     }

                     for (ChunkSectionLayer var47 : ChunkSectionLayer.values()) {
                        SectionDraw var52 = var41.getSectionDraw(var47);
                        RenderSectionBufferSlice var51;
                        if (var52 != null
                           && (var51 = var3.getRenderSectionSlice(var41, var47)) != null
                           && (!var52.hasCustomIndexBuffer() || var51.indexBuffer() != null)) {
                           if (var43 == -1) {
                              var43 = var33.size();
                              var33.add(
                                 new ChunkSectionInfo(
                                    var32,
                                    var42.getX() + var24.getX(),
                                    var42.getY() + var24.getY(),
                                    var42.getZ() + var24.getZ(),
                                    var40.getVisibility(var5),
                                    var11,
                                    var12
                                 )
                              );
                           }

                           VertexFormat var53 = var47.pipeline().getVertexFormatBinding(0);
                           GpuBuffer var54 = var51.vertexBuffer();
                           int var48;
                           IndexType var49;
                           GpuBuffer var50;
                           if (var52.hasCustomIndexBuffer()) {
                              var50 = var51.indexBuffer();
                              var49 = var52.indexType();
                              var48 = (int)(var51.indexBufferOffset() / var49.bytes);
                           } else {
                              var50 = null;
                              var49 = null;
                              var48 = 0;
                              var58 = Math.max(var58, var52.indexCount());
                           }

                           int var55 = (int)(var51.vertexBufferOffset() / var53.getVertexSize());
                           int var56 = var43;
                           ((List)((Int2ObjectOpenHashMap)var34.get(var47)).computeIfAbsent(var43, var0x -> new ArrayList()))
                              .add(
                                 new Draw<GpuBufferSlice[]>(
                                    0, var54, var50, var49, var48, var52.indexCount(), var55, (var1x, var2x) -> var2x.upload("ChunkSection", var1x[var56])
                                 )
                              );
                        }
                     }
                  }
               }

               if (var33.isEmpty()) {
                  return null;
               } else {
                  if (var58 > 0) {
                     RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS).getBuffer(var58);
                  }

                  GpuBufferSlice[] var61 = var14.writeChunkSections(var33.toArray(new ChunkSectionInfo[0]));
                  return new ChunkSectionsToRender(var10, var34, var58, var61);
               }
            }
         }
      } else {
         return null;
      }
   }

   private static LongArrayList sable$sectionDrawOrder(LevelPlot var0, LongSet var1, Pose3dc var2, Vec3 var3, Frustum var4) {
      Vec3 var5 = var2.transformPositionInverse(var3);
      LongArrayList var6 = new LongArrayList();

      for (PlotChunkHolder var8 : var0.getLoadedChunks()) {
         LevelChunk var9 = var8.getChunk();
         if (var9 != null) {
            ChunkPos var10 = var9.getPos();
            LevelChunkSection[] var11 = var9.getSections();

            for (int var12 = 0; var12 < var11.length; var12++) {
               LevelChunkSection var13 = var11[var12];
               if (var13 != null && !var13.hasOnlyAir()) {
                  int var14 = var9.getSectionYFromSectionIndex(var12);
                  long var15 = SectionPos.asLong(var10.x(), var14, var10.z());
                  if ((var1 == null || var1.contains(var15)) && (var4 == null || var4.isVisible(sectionBounds(var2, var10, var14 << 4)))) {
                     var6.add(var15);
                  }
               }
            }
         }
      }

      var6.sort((var1x, var3x) -> sable$compareSectionDistance(var1x, var3x, var5));
      return var6;
   }

   private static double sable$sectionDistanceSq(long var0, Vec3 var2) {
      double var3 = SectionPos.x(var0) + 8.0 - var2.x;
      double var5 = SectionPos.y(var0) + 8.0 - var2.y;
      double var7 = SectionPos.z(var0) + 8.0 - var2.z;
      return var3 * var3 + var5 * var5 + var7 * var7;
   }

   private static int sable$compareSectionDistance(long var0, long var2, Vec3 var4) {
      return Double.compare(sable$sectionDistanceSq(var0, var4), sable$sectionDistanceSq(var2, var4));
   }

   private static DynamicUniforms ownUniforms() {
      return RenderSystem.getDynamicUniforms();
   }

   public static void renderGroup(ChunkSectionLayerGroup var0, GpuSampler var1) {
      if (!insideRender && !FRAME_GROUPS.isEmpty()) {
         insideRender = true;

         try {
            for (int var2 = 0; var2 < FRAME_GROUPS.size(); var2++) {
               FRAME_GROUPS.get(var2).renderGroup(var0, var1);
            }
         } finally {
            insideRender = false;
         }
      }
   }

   public static LongSet computeVisibleSections(LevelPlot var0, Pose3dc var1, Vec3 var2) {
      LongSet var3 = FRAME_VISIBLE_SECTIONS.get(var0);
      if (var3 != null) {
         return var3;
      } else {
         LongSet var4 = sable$scanVisibleSections(var0, var1, var2);
         FRAME_VISIBLE_SECTIONS.put(var0, var4);
         return var4;
      }
   }

   private static LongSet sable$scanVisibleSections(LevelPlot var0, Pose3dc var1, Vec3 var2) {
      Long2ObjectOpenHashMap var3 = new Long2ObjectOpenHashMap();

      for (PlotChunkHolder var5 : var0.getLoadedChunks()) {
         LevelChunk var6 = var5.getChunk();
         if (var6 != null) {
            LevelChunkSection[] var7 = var6.getSections();

            for (int var8 = 0; var8 < var7.length; var8++) {
               LevelChunkSection var9 = var7[var8];
               if (var9 != null && !var9.hasOnlyAir()) {
                  var3.put(SectionPos.asLong(var6.getPos().x(), var6.getSectionYFromSectionIndex(var8), var6.getPos().z()), var9);
               }
            }
         }
      }

      if (var3.isEmpty()) {
         return LongSets.EMPTY_SET;
      } else {
         Vec3 var21 = var1.transformPositionInverse(var2);
         long var22 = SectionPos.asLong(Mth.floor(var21.x) >> 4, Mth.floor(var21.y) >> 4, Mth.floor(var21.z) >> 4);
         if (!var3.containsKey(var22)) {
            return new LongOpenHashSet(var3.keySet());
         } else {
            LongOpenHashSet var23 = new LongOpenHashSet();
            LongArrayFIFOQueue var24 = new LongArrayFIFOQueue();
            var23.add(var22);
            var24.enqueue(var22);

            while (!var24.isEmpty()) {
               long var25 = var24.dequeueLong();
               LevelChunkSection var11 = (LevelChunkSection)var3.get(var25);
               int var12 = SectionPos.x(var25);
               int var13 = SectionPos.y(var25);
               int var14 = SectionPos.z(var25);

               for (Direction var18 : Direction.values()) {
                  long var19;
                  if (!faceOpaque(var11, var18)
                     && var3.containsKey(var19 = SectionPos.asLong(var12 + var18.getStepX(), var13 + var18.getStepY(), var14 + var18.getStepZ()))
                     && var23.add(var19)) {
                     var24.enqueue(var19);
                  }
               }
            }

            return var23;
         }
      }
   }

   private static boolean faceOpaque(LevelChunkSection var0, Direction var1) {
      switch (var1) {
         case DOWN:
            for (int var8 = 0; var8 < 16; var8++) {
               for (int var13 = 0; var13 < 16; var13++) {
                  if (!var0.getBlockState(var8, 0, var13).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case UP:
            for (int var7 = 0; var7 < 16; var7++) {
               for (int var12 = 0; var12 < 16; var12++) {
                  if (!var0.getBlockState(var7, 15, var12).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case NORTH:
            for (int var6 = 0; var6 < 16; var6++) {
               for (int var11 = 0; var11 < 16; var11++) {
                  if (!var0.getBlockState(var6, var11, 0).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case SOUTH:
            for (int var5 = 0; var5 < 16; var5++) {
               for (int var10 = 0; var10 < 16; var10++) {
                  if (!var0.getBlockState(var5, var10, 15).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case WEST:
            for (int var4 = 0; var4 < 16; var4++) {
               for (int var9 = 0; var9 < 16; var9++) {
                  if (!var0.getBlockState(0, var4, var9).canOcclude()) {
                     return false;
                  }
               }
            }
            break;
         case EAST:
            for (int var2 = 0; var2 < 16; var2++) {
               for (int var3 = 0; var3 < 16; var3++) {
                  if (!var0.getBlockState(15, var2, var3).canOcclude()) {
                     return false;
                  }
               }
            }
      }

      return true;
   }

   public static AABB sectionBounds(Pose3dc var0, ChunkPos var1, int var2) {
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
               Vec3 var20 = var0.transformPosition(new Vec3(var3 + var17, var2 + var18, var4 + var19));
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
}
