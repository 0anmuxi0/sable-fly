package dev.ryanhcode.sable.sublevel.render.dispatcher;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderer;
import java.util.Collection;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.lwjgl.system.NativeResource;

@Internal
public interface SubLevelRenderDispatcher extends NativeResource, ResourceManagerReloadListener {
   static SubLevelRenderDispatcher get() {
      return SubLevelRenderer.getDispatcher();
   }

   SubLevelRenderData resize(ClientSubLevel subLevel, SubLevelRenderData renderData);

   SubLevelRenderData createRenderData(ClientSubLevel subLevel);

   default void rebuild(Iterable<ClientSubLevel> sublevels) {
      for (ClientSubLevel sublevel : sublevels) {
         SubLevelRenderData renderData = sublevel.getRenderData();
         if (renderData != null) {
            renderData.rebuild();
         }
      }
   }

   void updateCulling(Iterable<ClientSubLevel> sublevels, double cameraX, double cameraY, double cameraZ, Frustum frustum, boolean isSpectator);

   void renderBlockEntities(Iterable<ClientSubLevel> sublevels, SubLevelRenderDispatcher.BlockEntityRenderer blockEntityRenderer, double cameraX, double cameraY, double cameraZ, float partialTick);

   void addDebugInfo(Consumer<String> consumer);

   default void preRenderChunks(Camera camera) {
   }

   public interface BlockEntityRenderer {
      default void renderBlockEntities(
         Collection<BlockEntity> blockEntities, PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ
      ) {
         for (BlockEntity blockEntity : blockEntities) {
            this.renderSingleBE(blockEntity, poseStack, partialTick, cameraX, cameraY, cameraZ);
         }
      }

      void renderSingleBE(BlockEntity blockEntity, PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ);

      BlockEntityRenderDispatcher getBlockEntityRenderDispatcher();
   }
}
