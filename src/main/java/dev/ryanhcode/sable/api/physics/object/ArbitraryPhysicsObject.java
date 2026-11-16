package dev.ryanhcode.sable.api.physics.object;

import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.storage.holding.SubLevelHoldingChunkMap;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.level.ChunkPos;

public interface ArbitraryPhysicsObject {
   void getBoundingBox(BoundingBox3d dest);

   void onUnloaded(SubLevelHoldingChunkMap holdingChunkMap, ChunkPos chunkPos);

   void onRemoved();

   void onAddition(SubLevelPhysicsSystem physicsSystem);

   void wakeUp();
}
