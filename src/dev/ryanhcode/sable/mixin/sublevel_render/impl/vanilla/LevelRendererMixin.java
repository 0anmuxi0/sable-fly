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
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.render.SubLevelMovingBlockRenderState;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData.VisibleBlocks;
import it.unimi.dsi.fastutil.longs.LongSet;
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
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import dev.ryanhcode.sable.sublevel.render.SubLevelBatchedTerrainRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.level.CardinalLighting;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
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
   private void submitHitOutline(
      PoseStack poseStack, SubmitNodeCollector collector, RenderType renderType, BlockOutlineRenderState state, int color, float width, boolean afterTerrain
   ) {
   }

   @Inject(
      method = {"submitBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$submitSubLevelBlockOutline(PoseStack poseStack, SubmitNodeCollector collector, LevelRenderState levelRenderState, CallbackInfo ci) {
      BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
      if (state != null) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            BlockPos pos = state.pos();
            if (Sable.HELPER.getContaining(level, pos) instanceof ClientSubLevel subLevel) {
               Vec3 var16 = levelRenderState.cameraRenderState.pos;
               Pose3dc pose = subLevel.renderPose();
               Vector3dc rotationPoint = pose.rotationPoint();
               Vector3dc scale = pose.scale();
               poseStack.pushPose();
               poseStack.translate(pose.position().x() - var16.x, pose.position().y() - var16.y, pose.position().z() - var16.z);
               poseStack.rotateAround(this.sable$subLevelRotation.set(pose.orientation()), 0.0F, 0.0F, 0.0F);
               poseStack.scale((float)scale.x(), (float)scale.y(), (float)scale.z());
               poseStack.translate(pos.getX() - rotationPoint.x(), pos.getY() - rotationPoint.y(), pos.getZ() - rotationPoint.z());
               boolean highContrast = state.highContrast();
               if (highContrast) {
                  this.submitHitOutline(poseStack, collector, RenderTypes.secondaryBlockOutline(), state, -16777216, 7.0F, state.isTranslucent());
               }

               int color = highContrast ? -11010079 : ARGB.black(102);
               RenderType renderType;
               if (highContrast) {
                  // 26.3 used RenderTypes.linesDepthBias() here; 26.2 has no depth-biased line type,
                  // so approximate with the opaque lines() type (solid high-contrast outline).
                  // 26.3 also had a linesTranslucentNoDepthWrite() branch behind the OIT-backed
                  // GameRenderer.useImprovedTransparency() toggle, which does not exist on 26.2;
                  // linesTranslucent() is the shared fallback for that branch and the default path.
                  renderType = RenderTypes.lines();
               } else {
                  renderType = RenderTypes.linesTranslucent();
               }
               this.submitHitOutline(
                  poseStack,
                  collector,
                  renderType,
                  state,
                  color,
                  this.gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth,
                  state.isTranslucent()
               );
               poseStack.popPose();
               ci.cancel();
            }
         }
      }
   }

   @Inject(
      method = {"compileSections"},
      at = {@At("TAIL")}
   )
   private void sable$compileSections(CameraRenderState camera, CallbackInfo ci) {
      ClientLevel level = Minecraft.getInstance().level;
      Iterable<ClientSubLevel> sublevels = ((ClientSubLevelContainer)((SubLevelContainerHolder)level).sable$getPlotContainer()).getAllSubLevels();
      RenderRegionCache renderRegionCache = new RenderRegionCache();
      PrioritizeChunkUpdates chunkUpdates = (PrioritizeChunkUpdates)Minecraft.getInstance().options.prioritizeChunkUpdates().get();
      Camera camera1 = Minecraft.getInstance().gameRenderer.mainCamera();

      for (ClientSubLevel sublevel : sublevels) {
         SubLevelRenderData renderData = sublevel.getRenderData();
         if (renderData != null) {
            renderData.compileSections(chunkUpdates, renderRegionCache, camera1);
         }
      }
   }

   @Inject(
      method = {"prepareChunkRenders"},
      at = {@At("RETURN")}
   )
   private void sable$prepareSubLevelTerrain(Matrix4fc viewRotation, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
      // 26.3 also had a prepareChunkRendersIndirect hook; 26.2 has no indirect path, so one hook covers it.
      this.sable$buildSubLevelTerrain(viewRotation);
   }

   /**
    * 26.2 port of 26.3's batched terrain path. The 26.3 renderer compiled against renderpearl
    * ({@code com.mojang.renderpearl.api.*}), {@code DynamicGpuData} and {@code client.renderer.oit};
    * none of those exist on 26.2, so {@link SubLevelBatchedTerrainRenderer} is rebuilt on 26.2's
    * blaze3d API (private {@code DynamicUniforms} instance + the vanilla {@code ChunkSectionsToRender}
    * record, which carries per-section uniform slices itself). 26.2's
    * {@code ChunkSectionsToRender.renderGroup} reverses translucent draws internally, so 26.3's
    * {@code reverseTranslucent} flag has no counterpart here; 26.2 also has no OIT stage.
    */
   @Unique
   private void sable$buildSubLevelTerrain(Matrix4fc viewRotation) {
      // intentionally empty on 26.2 - the batched path is disabled pending a GPU-visibility root
      // cause: groups build fine (frameGroups=3, upload("ChunkSection") fires with valid slices)
      // yet produce no pixels, so we keep the proven per-block submission path for now.
   }

   @Inject(
      method = {"submitFeatures"},
      at = {@At("TAIL")}
   )
   private void sable$submitSubLevelBlocks(LevelRenderState levelRenderState, SubmitNodeCollector collector, boolean bl, CallbackInfo ci) {
      PoseStack poseStack = new PoseStack();
      ClientLevel clientLevel = Minecraft.getInstance().level;
      if (clientLevel != null) {
         ClientSubLevelContainer container = SubLevelContainer.getContainer(clientLevel);
         if (container != null) {
            Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
            double camX = cameraPos.x;
            double camY = cameraPos.y;
            double camZ = cameraPos.z;
            Frustum frustum = Minecraft.getInstance().gameRenderer.mainCamera().getCullFrustum();
            boolean occlusionCulling = (Boolean)SableClientConfig.SUB_LEVEL_OCCLUSION_CULLING.get();
            boolean cullEnclosedBlocks = (Boolean)SableClientConfig.SUB_LEVEL_CULL_ENCLOSED_BLOCKS.get();
            double renderDistance = (Double)SableClientConfig.SUB_LEVEL_RENDER_DISTANCE.get();
            double renderDistanceSq = renderDistance <= 0.0 ? Double.POSITIVE_INFINITY : renderDistance * renderDistance;
            boolean batched = false; // batched path disabled pending GPU-visibility root cause
            SectionRenderDispatcher dispatcher = null;

            for (ClientSubLevel subLevel : container.getAllSubLevels()) {
               LevelPlot plot = subLevel.getPlot();
               BoundingBox3ic bounds = plot.getBoundingBox();
               if (bounds != null && !(bounds.volume() <= 0.0)) {
                  Pose3dc pose = subLevel.renderPose();
                  Vector3dc position = pose.position();
                  Vector3dc rotationPoint = pose.rotationPoint();
                  Vector3dc scale = pose.scale();
                  this.sable$subLevelRotation.set(pose.orientation());
                  Level plotLevel = subLevel.getLevel();
                  if (subLevel.getRenderData() instanceof VanillaChunkedSubLevelRenderData chunkedRenderData) {
                     CardinalLighting cardinalLighting = ((ClientLevel)plotLevel).cardinalLighting();
                     LevelLightEngine lightEngine = plotLevel.getLightEngine();
                     Vec3 worldCenter = pose.transformPosition(
                        new Vec3(
                           (bounds.minX() + bounds.maxX() + 1) * 0.5, (bounds.minY() + bounds.maxY() + 1) * 0.5, (bounds.minZ() + bounds.maxZ() + 1) * 0.5
                        )
                     );
                     if (!(worldCenter.distanceToSqr(cameraPos) > renderDistanceSq)) {
                        LongSet visibleSections = null; // occlusion culling disabled with the batched path

                        for (PlotChunkHolder holder : plot.getLoadedChunks()) {
                           LevelChunk chunk = holder.getChunk();
                           if (chunk != null) {
                              ChunkPos chunkPos = chunk.getPos();
                              LevelChunkSection[] sections = chunk.getSections();

                              for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
                                 LevelChunkSection section = sections[sectionIndex];
                                 if (section != null && !section.hasOnlyAir()) {
                                    int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
                                    long sectionNode = SectionPos.asLong(chunkPos.x(), sectionY, chunkPos.z());
                                    if (visibleSections == null || visibleSections.contains(sectionNode)) {
                                       int sectionMinY = sectionY << 4;
                                     if (frustum == null || frustum.isVisible(this.sable$sectionBounds(pose, chunkPos, sectionMinY))) {
                                          VisibleBlocks visible = chunkedRenderData.sable$getVisibleBlocks(
                                             sectionNode, section, sectionMinY, chunkPos.x() << 4, chunkPos.z() << 4, cullEnclosedBlocks
                                          );
                                          BlockPos[] visiblePositions = visible.positions;
                                          if (visiblePositions.length != 0) {
                                             BlockState[] visibleStates = visible.states;
                                             Holder<Biome>[] visibleBiomes = visible.biomes;
                                             poseStack.pushPose();
                                             poseStack.translate(position.x() - camX, position.y() - camY, position.z() - camZ);
                                             poseStack.rotateAround(this.sable$subLevelRotation, 0.0F, 0.0F, 0.0F);
                                             poseStack.scale((float)scale.x(), (float)scale.y(), (float)scale.z());

                                             for (int i = 0; i < visiblePositions.length; i++) {
                                                BlockPos blockPos = visiblePositions[i];
                                                double localX = blockPos.getX() - rotationPoint.x();
                                                double localY = blockPos.getY() - rotationPoint.y();
                                                double localZ = blockPos.getZ() - rotationPoint.z();
                                                poseStack.translate(localX, localY, localZ);
                                                SubLevelMovingBlockRenderState state = new SubLevelMovingBlockRenderState();
                                                state.sable$setLevel(plotLevel);
                                                state.blockPos = blockPos;
                                                state.randomSeedPos = blockPos;
                                                state.blockState = visibleStates[i];
                                                state.biome = visibleBiomes[i];
                                                state.cardinalLighting = cardinalLighting;
                                                state.lightEngine = lightEngine;
                                                collector.submitMovingBlock(poseStack, state, 0);
                                                poseStack.translate(-localX, -localY, -localZ);
                                             }

                                             poseStack.popPose();
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

            RopeManager.renderAll(poseStack, collector, cameraPos);
         }
      }
   }

   @Inject(
      method = {"isSectionCompiledAndVisible"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$isSectionCompiled(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
      ClientLevel level = Minecraft.getInstance().level;
      ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
      if (container != null) {
         if (container.inBounds(blockPos)) {
            ClientSubLevel subLevel = (ClientSubLevel)Sable.HELPER.getContaining(level, blockPos);
            if (subLevel == null) {
               cir.setReturnValue(false);
            } else {
               SubLevelRenderData renderData = subLevel.getRenderData();
               if (renderData == null) {
                  cir.setReturnValue(false);
                  return;
               }

               SectionPos sectionPos = SectionPos.of(blockPos);
               cir.setReturnValue(renderData.isSectionCompiled(sectionPos.x(), sectionPos.y(), sectionPos.z()));
            }
         }
      }
   }

   /**
    * 26.2 local replacement for SubLevelBatchedTerrainRenderer.sectionBounds (pure geometry, no GPU deps).
    * Port note: must sample all 8 corners of the section box (same as 26.3's
    * SubLevelBatchedTerrainRenderer.sectionBounds). Under rotation a 2-diagonal-point
    * min/max AABB under-covers the section (e.g. 45° yaw), so the vanilla frustum test
    * culls visible sublevel sections and their blocks flicker in and out.
    */
   @Unique
   private net.minecraft.world.phys.AABB sable$sectionBounds(Pose3dc pose, net.minecraft.world.level.ChunkPos chunkPos, int sectionMinY) {
      int minX = chunkPos.x() << 4;
      int minZ = chunkPos.z() << 4;
      double x0 = Double.POSITIVE_INFINITY;
      double y0 = Double.POSITIVE_INFINITY;
      double z0 = Double.POSITIVE_INFINITY;
      double x1 = Double.NEGATIVE_INFINITY;
      double y1 = Double.NEGATIVE_INFINITY;
      double z1 = Double.NEGATIVE_INFINITY;
      for (int dx = 0; dx <= 16; dx += 16) {
         for (int dy = 0; dy <= 16; dy += 16) {
            for (int dz = 0; dz <= 16; dz += 16) {
               org.joml.Vector3d corner = pose.transformPosition(
                  new org.joml.Vector3d(minX + dx, sectionMinY + dy, minZ + dz)
               );
               x0 = Math.min(x0, corner.x);
               y0 = Math.min(y0, corner.y);
               z0 = Math.min(z0, corner.z);
               x1 = Math.max(x1, corner.x);
               y1 = Math.max(y1, corner.y);
               z1 = Math.max(z1, corner.z);
            }
         }
      }
      return new net.minecraft.world.phys.AABB(x0, y0, z0, x1, y1, z1);
   }
}