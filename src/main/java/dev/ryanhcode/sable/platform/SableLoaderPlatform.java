package dev.ryanhcode.sable.platform;

import java.nio.file.Path;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface SableLoaderPlatform {
   SableLoaderPlatform INSTANCE = SablePlatformUtil.load(SableLoaderPlatform.class);

   String getModVersion(String var1);

   boolean isModLoaded(String var1);

   default Path getGameDirectory() {
      return null;
   }
}
