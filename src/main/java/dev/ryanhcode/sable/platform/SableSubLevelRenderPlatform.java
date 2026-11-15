package dev.ryanhcode.sable.platform;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface SableSubLevelRenderPlatform {
   SableSubLevelRenderPlatform INSTANCE = SablePlatformUtil.load(SableSubLevelRenderPlatform.class);

   void tryAddFlywheelVisual(BlockEntity var1);
}
