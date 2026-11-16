package dev.ryanhcode.sable.platform;

import java.nio.file.Path;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface SableLoaderPlatform {
   SableLoaderPlatform INSTANCE = SablePlatformUtil.load(SableLoaderPlatform.class);

   String getModVersion(String modId);

   boolean isModLoaded(String modId);

   default Path getGameDirectory() {
      return null;
   }
}
