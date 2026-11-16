package dev.ryanhcode.sable.fabric.mixin.sound;

import dev.ryanhcode.sable.sound.MovingSoundInstanceDelegate;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin({MovingSoundInstanceDelegate.class})
public abstract class MovingSoundInstanceDelegateMixin implements SoundInstance {
   @Shadow
   public SoundInstance instance;

   @Unique
   @Override
   public WeighedSoundEvents resolve(SoundManager soundManager) {
      return this.instance.resolve(soundManager);
   }
}
