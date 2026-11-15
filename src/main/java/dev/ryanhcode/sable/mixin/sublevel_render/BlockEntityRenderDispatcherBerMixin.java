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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockEntityRenderDispatcher.class})
public class BlockEntityRenderDispatcherBerMixin {
   @Unique
   private static final Logger SABLE$LOGGER = LoggerFactory.getLogger("SableBer");
   @Unique
   private static final Map<BlockEntityRenderState, BlockEntity> sable$berEntities = new IdentityHashMap<>();
   @Unique
   private static int sable$extractLogs;
   @Unique
   private static int sable$submitLogs;
   @Unique
   private static int sable$transformLogs;

   @Inject(
      method = {"tryExtractRenderState"},
      at = {@At("RETURN")}
   )
   private void sable$rememberEntity(BlockEntity var1, float var2, CrumblingOverlay var3, CallbackInfoReturnable<BlockEntityRenderState> var5) {
      BlockEntityRenderState var6 = (BlockEntityRenderState)var5.getReturnValue();
      if (var6 != null) {
         sable$berEntities.put(var6, var1);
         if (sable$extractLogs < 8) {
            sable$extractLogs++;
            SABLE$LOGGER.info(
               "[sable-ber] extracted {} type={} state={}", new Object[]{var1.getClass().getSimpleName(), var6.blockEntityType, System.identityHashCode(var6)}
            );
         }
      }
   }

   @Inject(
      method = {"submit"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$submitCustomRenderer(BlockEntityRenderState var1, PoseStack var2, SubmitNodeCollector var3, CameraRenderState var4, CallbackInfo var5) {
      BlockEntity var6 = sable$berEntities.get(var1);
      if (sable$submitLogs < 8) {
         sable$submitLogs++;
         SABLE$LOGGER.info("[sable-ber] submit state={} type={} knownBe={}", new Object[]{System.identityHashCode(var1), var1.blockEntityType, var6 != null});
      }

      if (var6 != null) {
         if (Sable.HELPER.getContaining(var6) instanceof ClientSubLevel) {
            SubLevelBlockEntityRenderRegistry.Renderer var7 = SubLevelBlockEntityRenderRegistry.getRenderer(var1.blockEntityType);
            if (var7 == null) {
               if (sable$transformLogs < 12) {
                  sable$transformLogs++;
                  SABLE$LOGGER.info("[sable-ber] no registered renderer for {} type={}", var6.getClass().getSimpleName(), var1.blockEntityType);
               }
            } else {
               if (sable$submitLogs < 12) {
                  sable$submitLogs++;
                  SABLE$LOGGER.info("[sable-ber] invoking custom renderer for {} (type={})", var6.getClass().getSimpleName(), var1.blockEntityType);
               }

               sable$berEntities.remove(var1);
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
}
