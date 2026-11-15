package dev.ryanhcode.sable.mixin.particle;

import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import dev.ryanhcode.sable.sublevel.water_occlusion.WaterOcclusionContainer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.SuspendedParticle;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({SuspendedParticle.class})
public abstract class SuspendedParticleMixin extends Particle implements ParticleSubLevelKickable {
   protected SuspendedParticleMixin(ClientLevel var1, double var2, double var4, double var6) {
      super(var1, var2, var4, var6);
   }

   @Override
   public void move(double var1, double var3, double var5) {
      super.move(var1, var3, var5);
      WaterOcclusionContainer var7 = WaterOcclusionContainer.getContainer(this.level);
      if (var7 != null && var7.isOccluded(new Vec3(this.x, this.y, this.z))) {
         this.remove();
      }
   }

   @Override
   public boolean sable$shouldKickFromTracking() {
      return false;
   }

   @Override
   public boolean sable$shouldCollideWithTrackingSubLevel() {
      return false;
   }
}
