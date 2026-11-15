package dev.ryanhcode.sable.mixinhelpers.block_placement;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;

public final class BlockPlacementEntityHelper {
   private static final Quaterniondc IDENTITY = new Quaterniond();

   private BlockPlacementEntityHelper() {
   }

   public static boolean obstructs(VoxelShape shape, Entity entity) {
      if (intersects(shape, entity)) {
         return true;
      } else {
         AABB entityBounds = entity.getBoundingBox();
         BoundingBox3d queryBounds = new BoundingBox3d(entityBounds);
         queryBounds.expand(1.5, queryBounds);

         for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(entity.level(), queryBounds)) {
            BoundingBox3d localBounds = new BoundingBox3d(entityBounds);
            localBounds.transformInverse(subLevel.logicalPose(), localBounds);
            localBounds.expand(-0.046875, localBounds);
            if (Shapes.joinIsNotEmpty(shape, Shapes.create(localBounds.toMojang()), BooleanOp.AND)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean intersects(VoxelShape shape, Entity entity) {
      AABB box = entity.getBoundingBox();
      Quaterniondc orientation = EntitySubLevelRotationHelper.getEntityOrientation(entity, SubLevel::lastPose, 1.0F, EntitySubLevelRotationHelper.Type.ENTITY);
      if (orientation == null) {
         return Shapes.joinIsNotEmpty(shape, Shapes.create(box), BooleanOp.AND);
      } else {
         LevelReusedVectors sink = ((LevelExtension)entity.level()).sable$getJOMLSink();
         Vec3 eye = entity.getEyePosition();
         Vector3d center = new Vector3d(box.getCenter().x, box.getCenter().y, box.getCenter().z)
            .sub(eye.x, eye.y, eye.z)
            .rotate(orientation)
            .add(eye.x, eye.y, eye.z);
         OrientedBoundingBox3d entityOBB = new OrientedBoundingBox3d(
            center.x, center.y, center.z, box.getXsize(), box.getYsize(), box.getZsize(), orientation, sink
         );
         OrientedBoundingBox3d shapeOBB = new OrientedBoundingBox3d(sink);
         Vector3d shapeCenter = new Vector3d();
         Vector3d shapeDimensions = new Vector3d();

         for (AABB part : shape.toAabbs()) {
            Vec3 partCenter = part.getCenter();
            shapeCenter.set(partCenter.x, partCenter.y, partCenter.z);
            shapeDimensions.set(part.getXsize(), part.getYsize(), part.getZsize());
            shapeOBB.set(shapeCenter, shapeDimensions, IDENTITY);
            if (OrientedBoundingBox3d.sat(entityOBB, shapeOBB, sink.mtv).lengthSquared() > 0.0) {
               return true;
            }
         }

         return false;
      }
   }
}
