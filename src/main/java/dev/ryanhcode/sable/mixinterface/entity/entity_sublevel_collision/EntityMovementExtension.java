package dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision;

import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public interface EntityMovementExtension {
   SubLevelEntityCollision.CollisionInfo sable$getCollisionInfo();

   SubLevel sable$getTrackingSubLevel();

   UUID sable$getLastTrackingSubLevelID();

   void sable$setPosField(Vec3 posField);

   void sable$setTrackingSubLevel(SubLevel subLevel);

   void sable$setLastTrackingSubLevelID(UUID subLevelId);

   BlockPos sable$getInBlockStatePos();
}
