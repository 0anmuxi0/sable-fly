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

   public static void register(BlockEntityType<?> var0, SubLevelBlockEntityRenderRegistry.Renderer var1) {
      RENDERERS.put(var0, var1);
   }

   public static SubLevelBlockEntityRenderRegistry.Renderer getRenderer(BlockEntityType<?> var0) {
      return var0 == null ? null : RENDERERS.get(var0);
   }

   public interface Renderer {
      void render(BlockEntity var1, float var2, PoseStack var3, SubmitNodeCollector var4, int var5, int var6);
   }
}
