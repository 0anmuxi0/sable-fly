package dev.ryanhcode.sable.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockSubLevelAssemblyListener {
   default void beforeMove(ServerLevel originLevel, ServerLevel resultingLevel, BlockState newState, BlockPos oldPos, BlockPos newPos) {
   }

   void afterMove(ServerLevel sourceLevel, ServerLevel resultingLevel, BlockState state, BlockPos oldPos, BlockPos newPos);
}
