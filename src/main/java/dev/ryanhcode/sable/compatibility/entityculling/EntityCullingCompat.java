package dev.ryanhcode.sable.compatibility.entityculling;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.platform.SableLoaderPlatform;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public final class EntityCullingCompat {
   private static final EntityCullingCompat.Scope NO_OP = new EntityCullingCompat.Scope(null, null);
   private static final AtomicBoolean ACCESS_WARNING_LOGGED = new AtomicBoolean();
   private static final EntityCullingCompat.Access ACCESS = createAccess();

   private EntityCullingCompat() {
   }

   public static EntityCullingCompat.Scope suspendBlockEntityCulling() {
      if (ACCESS == null) {
         return NO_OP;
      } else {
         try {
            Object instance = ACCESS.instanceField().get(null);
            if (instance == null) {
               return NO_OP;
            } else {
               Object config = ACCESS.configField().get(instance);
               if (config != null && !ACCESS.skipBlockEntityCullingField().getBoolean(config)) {
                  ACCESS.skipBlockEntityCullingField().setBoolean(config, true);
                  return new EntityCullingCompat.Scope(config, ACCESS.skipBlockEntityCullingField());
               } else {
                  return NO_OP;
               }
            }
         } catch (RuntimeException | ReflectiveOperationException e) {
            logAccessWarning(e);
            return NO_OP;
         }
      }
   }

   private static EntityCullingCompat.Access createAccess() {
      if (!SableLoaderPlatform.INSTANCE.isModLoaded("entityculling")) {
         return null;
      } else {
         try {
            ClassLoader classLoader = EntityCullingCompat.class.getClassLoader();
            Class<?> modClass = Class.forName("dev.tr7zw.entityculling.EntityCullingModBase", false, classLoader);
            Class<?> configClass = Class.forName("dev.tr7zw.entityculling.versionless.Config", false, classLoader);
            return new EntityCullingCompat.Access(modClass.getField("instance"), modClass.getField("config"), configClass.getField("skipBlockEntityCulling"));
         } catch (RuntimeException | ReflectiveOperationException e) {
            logAccessWarning(e);
            return null;
         }
      }
   }

   private static void logAccessWarning(Exception exception) {
      if (ACCESS_WARNING_LOGGED.compareAndSet(false, true)) {
         Sable.LOGGER.warn("Unable to suspend EntityCulling while rendering sub-level block entities", exception);
      }
   }

   private record Access(Field instanceField, Field configField, Field skipBlockEntityCullingField) {
   }

   public static final class Scope implements AutoCloseable {
      private final Object config;
      private final Field skipBlockEntityCullingField;

      private Scope(Object config, Field skipBlockEntityCullingField) {
         this.config = config;
         this.skipBlockEntityCullingField = skipBlockEntityCullingField;
      }

      @Override
      public void close() {
         if (this.config != null) {
            try {
               this.skipBlockEntityCullingField.setBoolean(this.config, false);
            } catch (RuntimeException | IllegalAccessException e) {
               EntityCullingCompat.logAccessWarning(e);
            }
         }
      }
   }
}
