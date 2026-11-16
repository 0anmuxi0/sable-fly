package dev.ryanhcode.sable.sound;

public interface SoundInstanceDelegated {
   MovingSoundInstanceDelegate getDelegate();

   void setDelegate(MovingSoundInstanceDelegate delegate);
}
