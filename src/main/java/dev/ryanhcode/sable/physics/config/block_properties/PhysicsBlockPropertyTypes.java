package dev.ryanhcode.sable.physics.config.block_properties;

import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.platform.registry.SableRegistrationProvider;
import dev.ryanhcode.sable.platform.registry.SableRegistryObject;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.phys.Vec3;

public class PhysicsBlockPropertyTypes {
   public static final ResourceKey<Registry<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(
      Sable.sablePath("physics_block_properties")
   );
   private static final SableRegistrationProvider<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?>> VANILLA_PROVIDER = SableRegistrationProvider.get(
      REGISTRY_KEY, "sable"
   );
   private static final Registry<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?>> REGISTRY = VANILLA_PROVIDER.asVanillaRegistry();
   public static final SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> MASS = register(
      Sable.sablePath("mass"), Codec.DOUBLE, 1.0
   );
   public static final SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Vec3>> INERTIA = register(
      Sable.sablePath("inertia"), Vec3.CODEC, null
   );
   public static final SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> VOLUME = register(
      Sable.sablePath("volume"), Codec.DOUBLE, 1.0
   );
   public static final SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> RESTITUTION = register(
      Sable.sablePath("restitution"), Codec.DOUBLE, 0.0
   );
   public static final SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> FRICTION = register(
      Sable.sablePath("friction"), Codec.DOUBLE, 1.0
   );
   public static final SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Boolean>> FRAGILE = register(
      Sable.sablePath("fragile"), Codec.BOOL, false
   );
   public static final SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Identifier>> FLOATING_MATERIAL = register(
      Sable.sablePath("floating_material"), Identifier.CODEC, null
   );
   public static final SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<Double>> FLOATING_SCALE = register(
      Sable.sablePath("floating_scale"), Codec.DOUBLE, 1.0
   );

   public static void register() {
   }

   private static <T> SableRegistryObject<PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<T>> register(Identifier id, Codec<T> codec, T defaultValue) {
      if (REGISTRY.containsKey(id)) {
         throw new IllegalArgumentException("Duplicate physics block property: %s".formatted(id));
      } else {
         return VANILLA_PROVIDER.register(id, () -> new PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<>(REGISTRY.size(), codec, defaultValue));
      }
   }

   public static int count() {
      return REGISTRY.size();
   }

   public static Codec<Object> getPropertyCodec(Identifier id) {
      PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?> property = REGISTRY.getValue(id);
      if (property != null) {
         return (Codec<Object>)property.codec;
      } else {
         throw new IllegalArgumentException("Unknown physics block property: %s".formatted(id));
      }
   }

   public static PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?> getPropertyType(Identifier id) {
      PhysicsBlockPropertyTypes.PhysicsBlockPropertyType<?> property = REGISTRY.getValue(id);
      if (property != null) {
         return property;
      } else {
         throw new IllegalArgumentException("Unknown physics block property: %s".formatted(id));
      }
   }

   public record PhysicsBlockPropertyType<T>(int id, Codec<T> codec, T defaultValue) {
   }
}
