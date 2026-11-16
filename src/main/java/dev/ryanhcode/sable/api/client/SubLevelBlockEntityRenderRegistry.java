package dev.ryanhcode.sable.api.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class SubLevelBlockEntityRenderRegistry {
   private static final Map<BlockEntityType<?>, SubLevelBlockEntityRenderRegistry.Renderer> RENDERERS = new IdentityHashMap<>();

   private SubLevelBlockEntityRenderRegistry() {
   }

   public static void register(BlockEntityType<?> type, SubLevelBlockEntityRenderRegistry.Renderer renderer) {
      RENDERERS.put(type, renderer);
   }

   public static SubLevelBlockEntityRenderRegistry.Renderer getRenderer(BlockEntityType<?> type) {
      return type == null ? null : RENDERERS.get(type);
   }

   public interface Renderer {
      void render(BlockEntity blockEntity, float partialTicks, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay);
   }
}
