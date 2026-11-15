package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.render.SubLevelRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelExtractor.class})
public class ViewAreaMixin {
   @Inject(
      method = {"setSectionDirty(IIIZ)V"},
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
