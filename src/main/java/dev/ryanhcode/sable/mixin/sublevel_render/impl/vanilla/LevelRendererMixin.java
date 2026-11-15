package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.SableClientConfig;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.client.rope.RopeManager;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.render.SubLevelMovingBlockRenderState;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.OptionsRenderState;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
   value = {LevelRenderer.class},
   priority = 1002
)
public abstract class LevelRendererMixin {
   @Unique
   private final Quaternionf sable$subLevelRotation = new Quaternionf();
   @Shadow
   @Final
   private GameRenderer gameRenderer;
   @Shadow
   @Final
   private LevelRenderState levelRenderState;
   @Shadow
   @Final
   private OptionsRenderState optionsRenderState;
   @Shadow
   @Final
   private TextureManager textureManager;

   @Shadow
   public SectionRenderDispatcher sectionRenderDispatcher() {
      return null;
   }

   @Shadow
   private void submitHitOutline(PoseStack var1, SubmitNodeCollector var2, RenderType var3, BlockOutlineRenderState var4, int var5, float var6, boolean var7) {
   }

   @Inject(
      method = {"submitBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$submitSubLevelBlockOutline(PoseStack var1, SubmitNodeCollector var2, LevelRenderState var3, CallbackInfo var4) {
      BlockOutlineRenderState var5 = var3.blockOutlineRenderState;
      if (var5 != null) {
         ClientLevel var6 = Minecraft.getInstance().level;
         if (var6 != null) {
            BlockPos var7 = var5.pos();
            if (Sable.HELPER.getContaining(var6, var7) instanceof ClientSubLevel var8) {
               Vec3 var16 = var3.cameraRenderState.pos;
               Pose3dc var10 = var8.renderPose();
               Vector3dc var11 = var10.rotationPoint();
               Vector3dc var12 = var10.scale();
               var1.pushPose();
               var1.translate(var10.position().x() - var16.x, var10.position().y() - var16.y, var10.position().z() - var16.z);
               var1.rotateAround(this.sable$subLevelRotation.set(var10.orientation()), 0.0F, 0.0F, 0.0F);
               var1.scale((float)var12.x(), (float)var12.y(), (float)var12.z());
               var1.translate(var7.getX() - var11.x(), var7.getY() - var11.y(), var7.getZ() - var11.z());
               boolean var13 = var5.highContrast();
               if (var13) {
                  this.submitHitOutline(var1, var2, RenderTypes.secondaryBlockOutline(), var5, -16777216, 7.0F, var5.isTranslucent());
               }

               int var14 = var13 ? -11010079 : ARGB.black(102);
               RenderType var15;
               if (var13) {
                  var15 = RenderTypes.lines();
               } else {
                  var15 = RenderTypes.linesTranslucent();
               }

               this.submitHitOutline(
                  var1, var2, var15, var5, var14, Minecraft.getInstance().getWindow().getAppropriateLineWidth(), var5.isTranslucent()
               );
               var1.popPose();
               var4.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"compileSections"},
      at = {@At("TAIL")}
   )
   private void sable$compileSections(CameraRenderState var1, CallbackInfo var2) {
      ClientLevel var3 = Minecraft.getInstance().level;
      List<ClientSubLevel> var4 = ((ClientSubLevelContainer)((SubLevelContainerHolder)var3).sable$getPlotContainer()).getAllSubLevels();
      RenderRegionCache var5 = new RenderRegionCache();
      PrioritizeChunkUpdates var6 = Minecraft.getInstance().options.prioritizeChunkUpdates().get();
      Camera var7 = Minecraft.getInstance().gameRenderer.getMainCamera();

      for (ClientSubLevel var9 : var4) {
         SubLevelRenderData var10 = var9.getRenderData();
         if (var10 != null) {
            var10.compileSections(var6, var5, var7);
         }
      }
   }

   @Inject(
      method = {"prepareChunkRenders"},
      at = {@At("RETURN")}
   )
   private void sable$prepareSubLevelTerrain(Matrix4fc var1, CallbackInfoReturnable<ChunkSectionsToRender> var2) {
      this.sable$buildSubLevelTerrain(var1);
   }

   @Unique
   private void sable$buildSubLevelTerrain(Matrix4fc var1) {
   }

   @Inject(
      method = {"submitFeatures"},
      at = {@At("TAIL")}
   )
   private void sable$submitSubLevelBlocks(LevelRenderState var1, SubmitNodeCollector var2, boolean var3, CallbackInfo var4) {
      PoseStack var5 = new PoseStack();
      ClientLevel var6 = Minecraft.getInstance().level;
      if (var6 != null) {
         ClientSubLevelContainer var7 = SubLevelContainer.getContainer(var6);
         if (var7 != null) {
            Vec3 var8 = var1.cameraRenderState.pos;
            double var9 = var8.x;
            double var11 = var8.y;
            double var13 = var8.z;
            Frustum var15 = Minecraft.getInstance().gameRenderer.getMainCamera().getCullFrustum();
            boolean var16 = SableClientConfig.SUB_LEVEL_OCCLUSION_CULLING.get();
            boolean var17 = SableClientConfig.SUB_LEVEL_CULL_ENCLOSED_BLOCKS.get();
            double var18 = SableClientConfig.SUB_LEVEL_RENDER_DISTANCE.get();
            double var20 = var18 <= 0.0 ? Double.POSITIVE_INFINITY : var18 * var18;
            boolean var22 = false;
            Object var23 = null;

            for (ClientSubLevel var25 : var7.getAllSubLevels()) {
               ClientLevelPlot var26 = var25.getPlot();
               BoundingBox3ic var27 = var26.getBoundingBox();
               if (var27 != null && !(var27.volume() <= 0.0)) {
                  Pose3dc var28 = var25.renderPose();
                  Vector3dc var29 = var28.position();
                  Vector3dc var30 = var28.rotationPoint();
                  Vector3dc var31 = var28.scale();
                  this.sable$subLevelRotation.set(var28.orientation());
                  ClientLevel var32 = var25.getLevel();
                  if (var25.getRenderData() instanceof VanillaChunkedSubLevelRenderData var33) {
                     CardinalLighting var62 = var32.cardinalLighting();
                     LevelLightEngine var35 = var32.getLightEngine();
                     Vec3 var36 = var28.transformPosition(
                        new Vec3((var27.minX() + var27.maxX() + 1) * 0.5, (var27.minY() + var27.maxY() + 1) * 0.5, (var27.minZ() + var27.maxZ() + 1) * 0.5)
                     );
                     if (!(var36.distanceToSqr(var8) > var20)) {
                        LongSet var37 = null;

                        for (PlotChunkHolder var39 : var26.getLoadedChunks()) {
                           LevelChunk var40 = var39.getChunk();
                           if (var40 != null) {
                              ChunkPos var41 = var40.getPos();
                              LevelChunkSection[] var42 = var40.getSections();

                              for (int var43 = 0; var43 < var42.length; var43++) {
                                 LevelChunkSection var44 = var42[var43];
                                 if (var44 != null && !var44.hasOnlyAir()) {
                                    int var45 = var40.getSectionYFromSectionIndex(var43);
                                    long var46 = SectionPos.asLong(var41.x(), var45, var41.z());
                                    if (var37 == null || var37.contains(var46)) {
                                       int var48 = var45 << 4;
                                       if (var15 == null || var15.isVisible(this.sable$sectionBounds(var28, var41, var48))) {
                                          VanillaChunkedSubLevelRenderData.VisibleBlocks var49 = var33.sable$getVisibleBlocks(
                                             var46, var44, var48, var41.x() << 4, var41.z() << 4, var17
                                          );
                                          BlockPos[] var50 = var49.positions;
                                          if (var50.length != 0) {
                                             BlockState[] var51 = var49.states;
                                             Holder[] var52 = var49.biomes;
                                             var5.pushPose();
                                             var5.translate(var29.x() - var9, var29.y() - var11, var29.z() - var13);
                                             var5.rotateAround(this.sable$subLevelRotation, 0.0F, 0.0F, 0.0F);
                                             var5.scale((float)var31.x(), (float)var31.y(), (float)var31.z());

                                             for (int var53 = 0; var53 < var50.length; var53++) {
                                                BlockPos var54 = var50[var53];
                                                double var55 = var54.getX() - var30.x();
                                                double var57 = var54.getY() - var30.y();
                                                double var59 = var54.getZ() - var30.z();
                                                var5.translate(var55, var57, var59);
                                                SubLevelMovingBlockRenderState var61 = new SubLevelMovingBlockRenderState();
                                                var61.sable$setLevel(var32);
                                                var61.blockPos = var54;
                                                var61.randomSeedPos = var54;
                                                var61.blockState = var51[var53];
                                                var61.biome = var52[var53];
                                                var61.cardinalLighting = var62;
                                                var61.lightEngine = var35;
                                                var2.submitMovingBlock(var5, var61);
                                                var5.translate(-var55, -var57, -var59);
                                             }

                                             var5.popPose();
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

            RopeManager.renderAll(var5, var2, var8);
         }
      }
   }

   @Inject(
      method = {"isSectionCompiledAndVisible"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$isSectionCompiled(BlockPos var1, CallbackInfoReturnable<Boolean> var2) {
      ClientLevel var3 = Minecraft.getInstance().level;
      ClientSubLevelContainer var4 = SubLevelContainer.getContainer(var3);
      if (var4 != null && var4.inBounds(var1)) {
         ClientSubLevel var5 = (ClientSubLevel)Sable.HELPER.getContaining(var3, var1);
         if (var5 == null) {
            var2.setReturnValue(false);
         } else {
            SubLevelRenderData var6 = var5.getRenderData();
            if (var6 == null) {
               var2.setReturnValue(false);
               return;
            }

            SectionPos var7 = SectionPos.of(var1);
            var2.setReturnValue(var6.isSectionCompiled(var7.x(), var7.y(), var7.z()));
         }
      }
   }

   @Unique
   private AABB sable$sectionBounds(Pose3dc var1, ChunkPos var2, int var3) {
      int var4 = var2.x() << 4;
      int var5 = var2.z() << 4;
      double var6 = Double.POSITIVE_INFINITY;
      double var8 = Double.POSITIVE_INFINITY;
      double var10 = Double.POSITIVE_INFINITY;
      double var12 = Double.NEGATIVE_INFINITY;
      double var14 = Double.NEGATIVE_INFINITY;
      double var16 = Double.NEGATIVE_INFINITY;

      for (byte var18 = 0; var18 <= 16; var18 += 16) {
         for (byte var19 = 0; var19 <= 16; var19 += 16) {
            for (byte var20 = 0; var20 <= 16; var20 += 16) {
               Vector3d var21 = var1.transformPosition(new Vector3d(var4 + var18, var3 + var19, var5 + var20));
               var6 = Math.min(var6, var21.x);
               var8 = Math.min(var8, var21.y);
               var10 = Math.min(var10, var21.z);
               var12 = Math.max(var12, var21.x);
               var14 = Math.max(var14, var21.y);
               var16 = Math.max(var16, var21.z);
            }
         }
      }

      return new AABB(var6, var8, var10, var12, var14, var16);
   }
}
