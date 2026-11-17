package dev.ryanhcode.sable.mixin.explosion;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level.ExplosionInteraction;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({ServerLevel.class})
public abstract class ServerLevelMixin {
   @Shadow
   public abstract ServerLevel getLevel();

   @WrapMethod(require = 0, method = {"explode"}
   )
   public void sable$preExplode(
      Entity entity,
      DamageSource damageSource,
      ExplosionDamageCalculator explosionDamageCalculator,
      double d,
      double e,
      double f,
      float g,
      boolean bl,
      ExplosionInteraction explosionInteraction,
      ParticleOptions particleOptions,
      ParticleOptions particleOptions2,
      WeightedList<ExplosionParticleInfo> blockParticles,
      Holder<SoundEvent> holder,
      Operation<Void> original
   ) {
      Vector3d projectedPos = Sable.HELPER.projectOutOfSubLevel(this.getLevel(), new Vector3d(d, e, f));
      original.call(
         entity,
         damageSource,
         explosionDamageCalculator,
         projectedPos.x,
         projectedPos.y,
         projectedPos.z,
         g,
         bl,
         explosionInteraction,
         particleOptions,
         particleOptions2,
         blockParticles,
         holder
      );
   }
}
