package dev.ryanhcode.sable.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface SablePlatform {
   SablePlatform INSTANCE = SablePlatformUtil.load(SablePlatform.class);

   boolean isWrappedLevel(@Nullable Level level);

   boolean isBlockstateLadder(BlockState state, Level level, BlockPos pos, LivingEntity entity);
}
