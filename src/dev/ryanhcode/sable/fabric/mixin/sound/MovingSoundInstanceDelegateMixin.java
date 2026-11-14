package dev.ryanhcode.sable.fabric.mixin.sound;

import dev.ryanhcode.sable.sound.MovingSoundInstanceDelegate;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * 26.2 sound bridge.
 *
 * 26.3 renamed the {@code SoundInstance} lookup to {@code getOrResolve(SoundManager)}, and Sable's
 * {@code MovingSoundInstanceDelegate} implements that name. 26.2 still calls it {@code resolve(SoundManager)}
 * (verified by javap: {@code SoundInstance.resolve(SoundManager) : WeighedSoundEvents} is the abstract method;
 * there is no {@code getOrResolve} anywhere in 26.2's interface), so without this bridge the delegate leaves
 * {@code resolve} unimplemented and any Sable sub-level sound fails in {@code SoundEngine.play} with an
 * {@link AbstractMethodError}.
 *
 * This mixin supplies the 26.2-named method, forwarding to the wrapped instance's own {@code resolve}.
 */
@Mixin({MovingSoundInstanceDelegate.class})
public abstract class MovingSoundInstanceDelegateMixin implements SoundInstance {
   @Shadow
   public SoundInstance instance;

   @Override
   @Unique
   public WeighedSoundEvents resolve(SoundManager soundManager) {
      return this.instance.resolve(soundManager);
   }
}
