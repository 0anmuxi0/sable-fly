package dev.ryanhcode.sable.mixin.sublevel_render;

import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.render.dispatcher.SubLevelRenderDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LevelRenderer.class})
public class LevelRendererMixin {
   @Inject(
      method = {"allChanged"},
      at = {@At("TAIL")}
   )
   private void sable$allChanged(CallbackInfo ci) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         SubLevelRenderDispatcher.get().rebuild(((ClientSubLevelContainer)((SubLevelContainerHolder)level).sable$getPlotContainer()).getAllSubLevels());
      }
   }
}
