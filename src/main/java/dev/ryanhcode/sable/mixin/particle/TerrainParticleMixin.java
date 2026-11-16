package dev.ryanhcode.sable.mixin.particle;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Particle.class})
public abstract class TerrainParticleMixin {
   @Shadow
   protected double x;
   @Shadow
   protected double y;
   @Shadow
   protected double z;

   @Inject(
      method = {"getLightCoords(F)I"},
      at = {@At("RETURN")},
      cancellable = true
   )
   private void sable$scaleLightCoords(float partialTick, CallbackInfoReturnable<Integer> cir) {
      if ((Object)this instanceof TerrainParticle) {
         BlockPos blockPos = BlockPos.containing(this.x, this.y, this.z);
         ClientSubLevel clientSubLevel = Sable.HELPER.getContainingClient(blockPos);
         if (clientSubLevel != null) {
            cir.setReturnValue(clientSubLevel.scaleLightColor((Integer)cir.getReturnValue()));
         }
      }
   }
}
