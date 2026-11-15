package dev.ryanhcode.sable.platform.registry;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;

public final class SableRegistryObject<T> implements Supplier<T> {
   private final Identifier id;
   private final T value;

   SableRegistryObject(Identifier id, T value) {
      this.id = id;
      this.value = value;
   }

   @Override
   public T get() {
      return this.value;
   }

   public Identifier getId() {
      return this.id;
   }
}
