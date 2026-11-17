package dev.ryanhcode.sable.mixin.sublevel_render;

import com.mojang.blaze3d.vertex.VertexSorting;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.sublevel_render.vanilla.RenderSectionExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher.RenderSection;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({RenderSection.class})
public class RenderSectionMixin implements RenderSectionExtension {
   @Unique
   private Set<RenderSectionExtension.DirtyListener> sable$listeners;
   @Unique
   private boolean sable$listening = true;

   @Inject(require = 0, method = {"createVertexSorting(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/phys/Vec3;)Lcom/mojang/blaze3d/vertex/VertexSorting;"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$plotVertexSorting(SectionPos sectionPos, Vec3 cameraPos, CallbackInfoReturnable<VertexSorting> cir) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level != null) {
         SubLevel subLevel = Sable.HELPER.getContaining(level, sectionPos);
         if (subLevel != null) {
            Vec3 plotCamera = subLevel.logicalPose().transformPositionInverse(cameraPos);
            cir.setReturnValue(
               VertexSorting.byDistance(
                  (float)(plotCamera.x - sectionPos.minBlockX()),
                  (float)(plotCamera.y - sectionPos.minBlockY()),
                  (float)(plotCamera.z - sectionPos.minBlockZ())
               )
            );
         }
      }
   }

   @Override
   public void sable$addDirtyListener(RenderSectionExtension.DirtyListener listener) {
      if (this.sable$listeners == null) {
         this.sable$listeners = new ObjectArraySet();
      }

      this.sable$listeners.add(listener);
   }

   @Override
   public void sable$setListening(boolean listening) {
      this.sable$listening = listening;
   }
}
