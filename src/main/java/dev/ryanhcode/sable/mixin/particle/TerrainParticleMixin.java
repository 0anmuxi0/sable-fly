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
   private void sable$scaleLightCoords(float var1, CallbackInfoReturnable<Integer> var2) {
      if ((Object)this instanceof TerrainParticle) {
         BlockPos var3 = BlockPos.containing(this.x, this.y, this.z);
         ClientSubLevel var4 = Sable.HELPER.getContainingClient(var3);
         if (var4 != null) {
            var2.setReturnValue(var4.scaleLightColor((Integer)var2.getReturnValue()));
         }
      }
   }
}
