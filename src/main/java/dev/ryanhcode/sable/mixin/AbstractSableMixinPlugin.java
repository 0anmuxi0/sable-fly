package dev.ryanhcode.sable.mixin;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.annotation.MixinModVersionConstraint;
import dev.ryanhcode.sable.platform.SableLoaderPlatform;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

public abstract class AbstractSableMixinPlugin implements IMixinConfigPlugin {
   public static final Logger LOGGER = LogUtils.getLogger();
   private final Object2BooleanMap<String> modLoadedCache = new Object2BooleanOpenHashMap();

   @Override
   public void onLoad(String mixinPackage) {
      boolean sodiumLoaded = SableLoaderPlatform.INSTANCE.isModLoaded("sodium");
      LOGGER.info("Using {} renderer mixins", sodiumLoaded ? "Vanilla with Sodium compatibility" : "Vanilla");
   }

   @Override
   public String getRefMapperConfig() {
      return null;
   }

   @Override
   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
      String skip = System.getProperty("sable.port.skip");
      if (skip != null) {
         for (String part : skip.split(",")) {
            if (!part.isBlank() && mixinClassName.contains(part.trim())) {
               LOGGER.warn("PORT-DEBUG skipping mixin {}", mixinClassName);
               return false;
            }
         }
      }

      if (mixinClassName.startsWith("dev.ryanhcode.sable.mixin.sublevel_render.impl")) {
         return mixinClassName.startsWith("dev.ryanhcode.sable.mixin.sublevel_render.impl.sodium")
            ? this.modLoadedCache.computeIfAbsent("sodium", x -> SableLoaderPlatform.INSTANCE.isModLoaded("sodium"))
            : mixinClassName.startsWith("dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla");
      } else if (mixinClassName.equals("dev.ryanhcode.sable.mixin.plot.lighting.ClientPacketListenerMixin")
         && this.modLoadedCache.computeIfAbsent("scalablelux", x -> SableLoaderPlatform.INSTANCE.isModLoaded("scalablelux"))) {
         return false;
      } else if (!mixinClassName.startsWith("dev.ryanhcode.sable.mixin.compatibility.")
         && !mixinClassName.startsWith("dev.ryanhcode.sable.fabric.mixin.compatibility.")) {
         return true;
      } else {
         String[] parts = mixinClassName.split("\\.");
         if (parts.length < 5) {
            return true;
         } else {
            String modId = parts[3].equals("mixin") ? parts[5] : parts[6];
            boolean isModLoaded = this.modLoadedCache.computeIfAbsent(modId, x -> SableLoaderPlatform.INSTANCE.isModLoaded(modId));
            return isModLoaded && AbstractSableMixinPlugin.MixinConstraints.handleClassAnnotation(mixinClassName, modId);
         }
      }
   }

   @Override
   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
   }

   @Override
   public List<String> getMixins() {
      return null;
   }

   @Override
   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   @Override
   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
   }

   static class MixinConstraints {
      private static final Object2ObjectMap<String, String> MOD_VERSION_CACHE = new Object2ObjectOpenHashMap();

      static boolean handleClassAnnotation(String mixinClassName, String modId) {
         try {
            List<AnnotationNode> nodes = MixinService.getService().getBytecodeProvider().getClassNode(mixinClassName).visibleAnnotations;
            return nodes == null ? true : shouldApply(nodes, modId);
         } catch (Throwable t) {
            throw new RuntimeException(t);
         }
      }

      static boolean shouldApply(List<AnnotationNode> nodes, String modId) throws InvalidVersionSpecificationException {
         for (AnnotationNode node : nodes) {
            if (node.desc.equals(Type.getDescriptor(MixinModVersionConstraint.class))) {
               String range = Annotations.getValue(node, "value");
               VersionRange versionRange = VersionRange.createFromVersionSpec(range);
               String modVersion = (String)MOD_VERSION_CACHE.computeIfAbsent(modId, x -> SableLoaderPlatform.INSTANCE.getModVersion(modId));
               ArtifactVersion artifactVersion = new DefaultArtifactVersion(modVersion);
               return versionRange.containsVersion(artifactVersion);
            }
         }

         return true;
      }
   }
}
