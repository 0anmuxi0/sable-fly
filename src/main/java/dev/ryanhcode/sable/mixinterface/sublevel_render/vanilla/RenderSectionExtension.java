package dev.ryanhcode.sable.mixinterface.sublevel_render.vanilla;

import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;

public interface RenderSectionExtension {
   void sable$addDirtyListener(RenderSectionExtension.DirtyListener listener);

   void sable$setListening(boolean listening);

   @FunctionalInterface
   public interface DirtyListener {
      void markDirty(RenderSection section);
   }
}
