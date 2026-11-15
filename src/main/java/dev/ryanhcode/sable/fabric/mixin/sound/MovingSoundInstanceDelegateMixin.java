package dev.ryanhcode.sable.fabric.mixin.sound;

import dev.ryanhcode.sable.sound.MovingSoundInstanceDelegate;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({MovingSoundInstanceDelegate.class})
public abstract class MovingSoundInstanceDelegateMixin implements SoundInstance {
   @Shadow
   public SoundInstance instance;

   public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary loader, Identifier id, boolean repeatInstantly) {
      return this.instance.getAudioStream(loader, id, repeatInstantly);
   }
}
