package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.platform.SableLoaderPlatform;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class SableLoaderPlatformImpl implements SableLoaderPlatform {
   @Override
   public boolean isModLoaded(String modId) {
      return FabricLoader.getInstance().isModLoaded(modId);
   }

   @Override
   public Path getGameDirectory() {
      return FabricLoader.getInstance().getGameDir();
   }

   @Override
   public String getModVersion(String modId) {
      return ((ModContainer)FabricLoader.getInstance().getModContainer(modId).orElseThrow()).getMetadata().getVersion().getFriendlyString();
   }
}
