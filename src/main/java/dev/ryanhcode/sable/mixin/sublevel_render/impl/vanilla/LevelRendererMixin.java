package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.renderer.MultiBufferSource;
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
   private LevelRenderState levelRenderState;
   @Shadow
   @Final
   private OptionsRenderState optionsRenderState;

   @Shadow
   public SectionRenderDispatcher getSectionRenderDispatcher() {
      return null;
   }

   @Shadow
   private void renderHitOutline(PoseStack poseStack, VertexConsumer builder, double camX, double camY, double camZ, BlockOutlineRenderState state, int color, float width) {
   }

   @Inject(
      method = {"renderBlockOutline"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$submitSubLevelBlockOutline(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean onlyTranslucentBlocks, LevelRenderState levelRenderState, CallbackInfo ci) {
      BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
      if (state != null) {
         ClientLevel level = Minecraft.getInstance().level;
         if (level != null) {
            BlockPos pos = state.pos();
            if (Sable.HELPER.getContaining(level, pos) instanceof ClientSubLevel subLevel) {
               Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
               Pose3dc renderPose = subLevel.renderPose();
               Vector3dc rotationPoint = renderPose.rotationPoint();
               Vector3dc scale = renderPose.scale();
               poseStack.pushPose();
               poseStack.translate(renderPose.position().x() - cameraPos.x, renderPose.position().y() - cameraPos.y, renderPose.position().z() - cameraPos.z);
               poseStack.rotateAround(this.sable$subLevelRotation.set(renderPose.orientation()), 0.0F, 0.0F, 0.0F);
               poseStack.scale((float)scale.x(), (float)scale.y(), (float)scale.z());
               poseStack.translate(pos.getX() - rotationPoint.x(), pos.getY() - rotationPoint.y(), pos.getZ() - rotationPoint.z());
               boolean highContrast = state.highContrast();
               if (highContrast) {
                  this.renderHitOutline(poseStack, bufferSource.getBuffer(RenderTypes.secondaryBlockOutline()), pos.getX(), pos.getY(), pos.getZ(), state, -16777216, 7.0F);
               }

               int outlineColor = highContrast ? -11010079 : ARGB.black(102);
               RenderType renderType;
               if (highContrast) {
                  renderType = RenderTypes.lines();
               } else {
                  renderType = RenderTypes.linesTranslucent();
               }

               this.renderHitOutline(poseStack, bufferSource.getBuffer(renderType), pos.getX(), pos.getY(), pos.getZ(), state, outlineColor, Minecraft.getInstance().getWindow().getAppropriateLineWidth());
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
   private void sable$compileSections(Camera camera, CallbackInfo ci) {
      ClientLevel level = Minecraft.getInstance().level;
      List<ClientSubLevel> subLevels = ((ClientSubLevelContainer)((SubLevelContainerHolder)level).sable$getPlotContainer()).getAllSubLevels();
      RenderRegionCache cache = new RenderRegionCache();
      PrioritizeChunkUpdates chunkUpdates = Minecraft.getInstance().options.prioritizeChunkUpdates().get();
      Camera mainCamera = Minecraft.getInstance().gameRenderer.getMainCamera();

      for (ClientSubLevel subLevel : subLevels) {
         SubLevelRenderData renderData = subLevel.getRenderData();
         if (renderData != null) {
            renderData.compileSections(chunkUpdates, cache, mainCamera);
         }
      }
   }

   @Inject(
      method = {"prepareChunkRenders"},
      at = {@At("RETURN")}
   )
   private void sable$prepareSubLevelTerrain(Matrix4fc modelViewMatrix, CallbackInfoReturnable<ChunkSectionsToRender> cir) {
      this.sable$buildSubLevelTerrain(modelViewMatrix);
   }

   @Unique
   private void sable$buildSubLevelTerrain(Matrix4fc modelViewMatrix) {
   }

   @Inject(
      method = {"submitEntities"},
      at = {@At("TAIL")}
   )
   private void sable$submitSubLevelBlocks(PoseStack poseStack, LevelRenderState levelRenderState, SubmitNodeCollector output, CallbackInfo ci) {
      PoseStack subLevelPoseStack = new PoseStack();
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
         if (container != null) {
            Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
            double camX = cameraPos.x;
            double camY = cameraPos.y;
            double camZ = cameraPos.z;
            Frustum frustum = Minecraft.getInstance().gameRenderer.getMainCamera().getCullFrustum();
            boolean occlusionCulling = SableClientConfig.SUB_LEVEL_OCCLUSION_CULLING.get();
            boolean cullEnclosedBlocks = SableClientConfig.SUB_LEVEL_CULL_ENCLOSED_BLOCKS.get();
            double renderDistance = SableClientConfig.SUB_LEVEL_RENDER_DISTANCE.get();
            double renderDistanceSq = renderDistance <= 0.0 ? Double.POSITIVE_INFINITY : renderDistance * renderDistance;
            boolean rendered = false;
            Object builtGroup = null;

            for (ClientSubLevel subLevel : container.getAllSubLevels()) {
               ClientLevelPlot plot = subLevel.getPlot();
               BoundingBox3ic boundingBox = plot.getBoundingBox();
               if (boundingBox != null && !(boundingBox.volume() <= 0.0)) {
                  Pose3dc renderPose = subLevel.renderPose();
                  Vector3dc position = renderPose.position();
                  Vector3dc rotationPoint = renderPose.rotationPoint();
                  Vector3dc scale = renderPose.scale();
                  this.sable$subLevelRotation.set(renderPose.orientation());
                  ClientLevel subLevelLevel = subLevel.getLevel();
                  if (subLevel.getRenderData() instanceof VanillaChunkedSubLevelRenderData renderData) {
                     CardinalLighting cardinalLighting = subLevelLevel.cardinalLighting();
                     LevelLightEngine lightEngine = subLevelLevel.getLightEngine();
                     Vec3 transformedCenter = renderPose.transformPosition(
                        new Vec3((boundingBox.minX() + boundingBox.maxX() + 1) * 0.5, (boundingBox.minY() + boundingBox.maxY() + 1) * 0.5, (boundingBox.minZ() + boundingBox.maxZ() + 1) * 0.5)
                     );
                     if (!(transformedCenter.distanceToSqr(cameraPos) > renderDistanceSq)) {
                        LongSet visibleSections = null;

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
                                    if (visibleSections == null || visibleSections.contains(sectionPos)) {
                                       int sectionBaseY = sectionY << 4;
                                       if (frustum == null || frustum.isVisible(this.sable$sectionBounds(renderPose, chunkPos, sectionBaseY))) {
                                          VanillaChunkedSubLevelRenderData.VisibleBlocks visibleBlocks = renderData.sable$getVisibleBlocks(
                                             sectionPos, section, sectionBaseY, chunkPos.x() << 4, chunkPos.z() << 4, cullEnclosedBlocks
                                          );
                                          BlockPos[] positions = visibleBlocks.positions;
                                          if (positions.length != 0) {
                                             BlockState[] states = visibleBlocks.states;
                                             Holder[] biomes = visibleBlocks.biomes;
                                             subLevelPoseStack.pushPose();
                                             subLevelPoseStack.translate(position.x() - camX, position.y() - camY, position.z() - camZ);
                                             subLevelPoseStack.rotateAround(this.sable$subLevelRotation, 0.0F, 0.0F, 0.0F);
                                             subLevelPoseStack.scale((float)scale.x(), (float)scale.y(), (float)scale.z());

                                             for (int i = 0; i < positions.length; i++) {
                                                BlockPos blockPos = positions[i];
                                                double offsetX = blockPos.getX() - rotationPoint.x();
                                                double offsetY = blockPos.getY() - rotationPoint.y();
                                                double offsetZ = blockPos.getZ() - rotationPoint.z();
                                                subLevelPoseStack.translate(offsetX, offsetY, offsetZ);
                                                SubLevelMovingBlockRenderState movingBlockState = new SubLevelMovingBlockRenderState();
                                                movingBlockState.sable$setLevel(subLevelLevel);
                                                movingBlockState.blockPos = blockPos;
                                                movingBlockState.randomSeedPos = blockPos;
                                                movingBlockState.blockState = states[i];
                                                movingBlockState.biome = biomes[i];
                                                movingBlockState.cardinalLighting = cardinalLighting;
                                                movingBlockState.lightEngine = lightEngine;
                                                output.submitMovingBlock(subLevelPoseStack, movingBlockState);
                                                subLevelPoseStack.translate(-offsetX, -offsetY, -offsetZ);
                                             }

                                             subLevelPoseStack.popPose();
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

            RopeManager.renderAll(subLevelPoseStack, output, cameraPos);
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
      if (container != null && container.inBounds(blockPos)) {
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

   @Unique
   private AABB sable$sectionBounds(Pose3dc renderPose, ChunkPos chunkPos, int sectionBaseY) {
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
               Vector3d cornerPos = renderPose.transformPosition(new Vector3d(baseX + dx, sectionBaseY + dy, baseZ + dz));
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
