package dev.ryanhcode.sable.platform.registry;

import com.mojang.serialization.Lifecycle;
import java.util.function.Supplier;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

public final class SableRegistrationProvider<T> {
   private final MappedRegistry<T> registry;

   private SableRegistrationProvider(MappedRegistry<T> registry) {
      this.registry = registry;
   }

   public static <T> SableRegistrationProvider<T> get(ResourceKey<? extends Registry<T>> key, String modId) {
      return new SableRegistrationProvider<>(new MappedRegistry<>(key, Lifecycle.stable()));
   }

   public Registry<T> asVanillaRegistry() {
      return this.registry;
   }

   public <I extends T> SableRegistryObject<I> register(Identifier name, Supplier<I> supplier) {
      I value = supplier.get();
      Registry.register(this.registry, name, value);
      return new SableRegistryObject<>(name, value);
   }
}
