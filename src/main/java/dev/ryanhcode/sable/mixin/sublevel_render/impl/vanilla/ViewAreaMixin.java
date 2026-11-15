package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.1.2 兼容：26.2 的 ViewAreaMixin 注入 LevelExtractor.setSectionDirty(IIIZ)；
 * 26.1.2 没有 LevelExtractor，等价注入 ViewArea.setDirty(IIIZ)（语义相同：
 * 主世界区块脏标记转发到 sub-level 的 renderData）。
 */
@Mixin({ViewArea.class})
public class ViewAreaMixin {
   @Inject(
      method = {"setDirty(IIIZ)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$setDirty(int x, int y, int z, boolean playerChanged, CallbackInfo ci) {
      SubLevelContainer plotContainer = ((SubLevelContainerHolder)Minecraft.getInstance().level).sable$getPlotContainer();
      LevelPlot plot = plotContainer.getPlot(x, z);
      if (plot != null) {
         SubLevelRenderData renderData = ((ClientSubLevel)plot.getSubLevel()).getRenderData();
         if (renderData != null) {
            renderData.setDirty(x, y, z, playerChanged);
         }

         ci.cancel();
      }
   }
}
