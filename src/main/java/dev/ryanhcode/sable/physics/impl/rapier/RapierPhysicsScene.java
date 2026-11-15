package dev.ryanhcode.sable.physics.impl.rapier;

import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class RapierPhysicsScene {
   private final long handle;

   RapierPhysicsScene(long handle) {
      if (handle == 0L) {
         throw new IllegalArgumentException("invalid scene handle");
      } else {
         this.handle = handle;
      }
   }

   long handle() {
      return this.handle;
   }
}
