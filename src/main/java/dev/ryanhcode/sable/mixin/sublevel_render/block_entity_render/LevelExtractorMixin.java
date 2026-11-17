package dev.ryanhcode.sable.mixin.sublevel_render.block_entity_render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.compatibility.entityculling.EntityCullingCompat;
import dev.ryanhcode.sable.mixinterface.BlockEntityRenderDispatcherExtension;
import dev.ryanhcode.sable.mixinterface.sublevel_render.BlockEntityTransformsHolder;
import dev.ryanhcode.sable.mixinterface.sublevel_render.SubLevelBlockEntityRenderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import java.util.SortedSet;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelExtractor.class})
public class LevelExtractorMixin implements SubLevelBlockEntityRenderExtension {
   @Shadow
   @Final
   private LevelRenderer levelRenderer;
   @Shadow
   private ClientLevel level;
   @Shadow
   @Final
   private LevelRenderState levelRenderState;

   @Inject(require = 0, method = {"extractVisibleBlockEntities"},
      at = {@At("RETURN")}
   )
   private void sable$extractBlockEntities(Camera camera, float partialTick, LevelRenderState levelRenderState, CallbackInfo ci) {
      this.sable$extractSubLevelBlockEntities(camera, partialTick, levelRenderState);
   }

   @Override
   public void sable$extractSubLevelBlockEntities(Camera camera, float partialTick, LevelRenderState levelRenderState) {
      ((BlockEntityTransformsHolder)this.levelRenderer).sable$getBlockEntityTransforms().clear();
      levelRenderState.blockEntityRenderStates.removeIf(state -> Sable.HELPER.getContainingClient(state.blockPos) != null);
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         ClientSubLevelContainer container = SubLevelContainer.getContainer(level);
         if (container != null) {
            Vec3 cameraPosition = camera.position();
            BlockEntityRenderDispatcherExtension dispatcherExtension = (BlockEntityRenderDispatcherExtension)this.levelRenderer.blockEntityRenderDispatcher();

            try (EntityCullingCompat.Scope ignored = EntityCullingCompat.suspendBlockEntityCulling()) {
               for (ClientSubLevel subLevel : container.getAllSubLevels()) {
                  SubLevelRenderData renderData = subLevel.getRenderData();
                  if (renderData != null) {
                     Vector3dc rotationPoint = subLevel.renderPose(partialTick).rotationPoint();
                     Matrix4f transformation = renderData.getTransformation(cameraPosition.x, cameraPosition.y, cameraPosition.z);
                     Vector3f localCamera = transformation.invert(new Matrix4f()).transformPosition(new Vector3f());
                     dispatcherExtension.sable$setCameraPosition(
                        new Vec3(localCamera.x + rotationPoint.x(), localCamera.y + rotationPoint.y(), localCamera.z + rotationPoint.z())
                     );

                     try {
                        for (PlotChunkHolder holder : subLevel.getPlot().getLoadedChunks()) {
                           for (BlockEntity blockEntity : holder.getChunk().getBlockEntities().values()) {
                              if (!blockEntity.isRemoved()) {
                                 BlockPos blockPos = blockEntity.getBlockPos();
                                 CrumblingOverlay crumblingOverlay = this.sable$createCrumblingOverlay(blockPos, transformation, rotationPoint);
                                 BlockEntityRenderDispatcher dispatcher = this.levelRenderer.blockEntityRenderDispatcher();
                                 BlockEntityRenderer<?, ?> renderer = dispatcher.getRenderer(blockEntity);
                                 boolean offScreen = renderer != null && renderer.shouldRenderOffScreen();
                                 BlockEntityRenderState renderState = dispatcher.tryExtractRenderState(blockEntity, partialTick, crumblingOverlay, offScreen);
                                 if (renderState != null) {
                                    Vector3d physicalCenter = subLevel.renderPose(partialTick)
                                       .transformPosition(new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5));
                                    renderState.lightCoords = LightCoordsUtil.getLightCoords(
                                       level, BlockPos.containing(physicalCenter.x, physicalCenter.y, physicalCenter.z)
                                    );
                                    levelRenderState.blockEntityRenderStates.add(renderState);
                                    ((BlockEntityTransformsHolder)this.levelRenderer)
                                       .sable$getBlockEntityTransforms()
                                       .put(
                                          renderState,
                                          new BlockEntityTransformsHolder.SableBlockEntityTransform(
                                             transformation, (Vector3d)rotationPoint, new Quaternionf(subLevel.renderPose(partialTick).orientation())
                                          )
                                       );
                                 }
                              }
                           }
                        }
                     } finally {
                        dispatcherExtension.sable$setCameraPosition(null);
                     }
                  }
               }
            }
         }
      }
   }

   @Unique
   private CrumblingOverlay sable$createCrumblingOverlay(BlockPos blockPos, Matrix4f transformation, Vector3dc rotationPoint) {
      SortedSet<BlockDestructionProgress> progress = (SortedSet<BlockDestructionProgress>)this.level.destructionProgress().get(blockPos.asLong());
      if (progress != null && !progress.isEmpty()) {
         PoseStack poseStack = new PoseStack();
         poseStack.mulPose(transformation);
         poseStack.translate(blockPos.getX() - rotationPoint.x(), blockPos.getY() - rotationPoint.y(), blockPos.getZ() - rotationPoint.z());
         return new CrumblingOverlay(progress.last().getProgress(), poseStack.last());
      } else {
         return null;
      }
   }
}
