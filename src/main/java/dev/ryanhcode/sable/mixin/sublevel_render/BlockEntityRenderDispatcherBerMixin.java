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
   private void sable$rememberEntity(BlockEntity blockEntity, float partialTicks, CrumblingOverlay crumblingOverlay, CallbackInfoReturnable<BlockEntityRenderState> cir) {
      BlockEntityRenderState renderState = (BlockEntityRenderState)cir.getReturnValue();
      if (renderState != null) {
         sable$berEntities.put(renderState, blockEntity);
      }
   }

   @Inject(
      method = {"submit"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$submitCustomRenderer(BlockEntityRenderState renderState, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraRenderState, CallbackInfo ci) {
      BlockEntity blockEntity = sable$berEntities.remove(renderState);
      if (blockEntity == null) {
         return;
      }

      if (Sable.HELPER.getContaining(blockEntity) instanceof ClientSubLevel) {
         SubLevelBlockEntityRenderRegistry.Renderer renderer = SubLevelBlockEntityRenderRegistry.getRenderer(renderState.blockEntityType);
         if (renderer != null) {
            float partialTicks = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
            poseStack.pushPose();

            try {
               renderer.render(blockEntity, partialTicks, poseStack, collector, renderState.lightCoords, 0);
            } finally {
               poseStack.popPose();
            }

            ci.cancel();
         }
      }
   }
}
