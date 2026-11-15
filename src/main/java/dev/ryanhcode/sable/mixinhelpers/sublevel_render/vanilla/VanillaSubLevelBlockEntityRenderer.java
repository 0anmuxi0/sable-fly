package dev.ryanhcode.sable.mixinhelpers.sublevel_render.vanilla;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.SortedSet;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;

public class VanillaSubLevelBlockEntityRenderer implements SubLevelRenderDispatcher.BlockEntityRenderer {
   private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
   private final RenderBuffers renderBuffers;
   private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

   public VanillaSubLevelBlockEntityRenderer(
      BlockEntityRenderDispatcher blockEntityRenderDispatcher,
      RenderBuffers renderBuffers,
      Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress
   ) {
      this.blockEntityRenderDispatcher = blockEntityRenderDispatcher;
      this.renderBuffers = renderBuffers;
      this.destructionProgress = destructionProgress;
   }

   @Override
   public BlockEntityRenderDispatcher getBlockEntityRenderDispatcher() {
      return this.blockEntityRenderDispatcher;
   }

   @Override
   public void renderSingleBE(BlockEntity blockEntity, PoseStack poseStack, float partialTick, double cameraX, double cameraY, double cameraZ) {
   }
}
