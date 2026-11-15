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

   SubLevelRenderData resize(ClientSubLevel var1, SubLevelRenderData var2);

   SubLevelRenderData createRenderData(ClientSubLevel var1);

   default void rebuild(Iterable<ClientSubLevel> sublevels) {
      for (ClientSubLevel sublevel : sublevels) {
         SubLevelRenderData renderData = sublevel.getRenderData();
         if (renderData != null) {
            renderData.rebuild();
         }
      }
   }

   void updateCulling(Iterable<ClientSubLevel> var1, double var2, double var4, double var6, Frustum var8, boolean var9);

   void renderBlockEntities(Iterable<ClientSubLevel> var1, SubLevelRenderDispatcher.BlockEntityRenderer var2, double var3, double var5, double var7, float var9);

   void addDebugInfo(Consumer<String> var1);

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

      void renderSingleBE(BlockEntity var1, PoseStack var2, float var3, double var4, double var6, double var8);

      BlockEntityRenderDispatcher getBlockEntityRenderDispatcher();
   }
}
