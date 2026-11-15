package dev.ryanhcode.sable.mixin.sublevel_render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.client.SubLevelBlockEntityRenderRegistry;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockEntityRenderDispatcher.class})
public class BlockEntityRenderDispatcherBerMixin {
   /**
    * 记录"渲染状态 -> 原始方块实体"的对应关系，供自定义 sub-level 渲染器使用。
    *
    * <p>注意：这是静态 Map，若不清理条目会无限堆积（内存泄露）。
    * 这里的做法是：条目在 {@code tryExtractRenderState} 时写入，在 {@code submit} 时
    * 被取出后立即移除（无论是否用到自定义渲染器），保证 Map 大小与"本帧渲染状态数"
    * 相当，不会持续增长。
    */
   @Unique
   private static final Map<BlockEntityRenderState, BlockEntity> sable$berEntities = new IdentityHashMap<>();

   @Inject(
      method = {"tryExtractRenderState"},
      at = {@At("RETURN")}
   )
   private void sable$rememberEntity(BlockEntity var1, float var2, CrumblingOverlay var3, CallbackInfoReturnable<BlockEntityRenderState> var5) {
      BlockEntityRenderState var6 = (BlockEntityRenderState)var5.getReturnValue();
      if (var6 != null) {
         sable$berEntities.put(var6, var1);
      }
   }

   @Inject(
      method = {"submit"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$submitCustomRenderer(BlockEntityRenderState var1, PoseStack var2, SubmitNodeCollector var3, CameraRenderState var4, CallbackInfo var5) {
      BlockEntity var6 = sable$berEntities.remove(var1);
      if (var6 == null) {
         return;
      }

      if (Sable.HELPER.getContaining(var6) instanceof ClientSubLevel) {
         SubLevelBlockEntityRenderRegistry.Renderer var7 = SubLevelBlockEntityRenderRegistry.getRenderer(var1.blockEntityType);
         if (var7 != null) {
            float var8 = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            var2.pushPose();

            try {
               var7.render(var6, var8, var2, var3, var1.lightCoords, 0);
            } finally {
               var2.popPose();
            }

            var5.cancel();
         }
      }
   }
}
