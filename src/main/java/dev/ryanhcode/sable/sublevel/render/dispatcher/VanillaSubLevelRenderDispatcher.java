package dev.ryanhcode.sable.sublevel.render.dispatcher;

import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaSingleSubLevelRenderData;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;

public class VanillaSubLevelRenderDispatcher implements SubLevelRenderDispatcher {
   public static boolean isSingleBlock(ClientSubLevel subLevel) {
      return false;
   }

   @Override
   public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
   }

   @Override
   public SubLevelRenderData resize(ClientSubLevel subLevel, SubLevelRenderData renderData) {
      if (renderData instanceof VanillaSingleSubLevelRenderData ^ isSingleBlock(subLevel)) {
         renderData.close();
         SubLevelRenderData data = this.createRenderData(subLevel);
         if (data instanceof VanillaChunkedSubLevelRenderData) {
            data.compileSections(PrioritizeChunkUpdates.NEARBY, new RenderRegionCache(), Minecraft.getInstance().gameRenderer.getMainCamera());
         }

         return data;
      } else {
         if (renderData instanceof VanillaChunkedSubLevelRenderData chunkedRenderData) {
            chunkedRenderData.resize();
            chunkedRenderData.compileSections(PrioritizeChunkUpdates.NEARBY, new RenderRegionCache(), Minecraft.getInstance().gameRenderer.getMainCamera());
         }

         return renderData;
      }
   }

   @Override
   public SubLevelRenderData createRenderData(ClientSubLevel subLevel) {
      if (isSingleBlock(subLevel)) {
         return new VanillaSingleSubLevelRenderData(subLevel);
      } else {
         SectionRenderDispatcher sectionRenderDispatcher = Minecraft.getInstance().levelRenderer.getSectionRenderDispatcher();
         return sectionRenderDispatcher == null ? null : new VanillaChunkedSubLevelRenderData(subLevel, sectionRenderDispatcher);
      }
   }

   @Override
   public void updateCulling(Iterable<ClientSubLevel> sublevels, double cameraX, double cameraY, double cameraZ, Frustum cullFrustum, boolean isSpectator) {
   }

   @Override
   public void renderBlockEntities(
      Iterable<ClientSubLevel> sublevels,
      SubLevelRenderDispatcher.BlockEntityRenderer blockEntityRenderer,
      double cameraX,
      double cameraY,
      double cameraZ,
      float partialTick
   ) {
   }

   @Override
   public void addDebugInfo(Consumer<String> consumer) {
   }

   public void free() {
   }
}
