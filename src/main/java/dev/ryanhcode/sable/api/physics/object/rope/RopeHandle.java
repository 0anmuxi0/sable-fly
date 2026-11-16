package dev.ryanhcode.sable.api.physics.object.rope;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import java.util.List;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public interface RopeHandle {
   @OverrideOnly
   void readPose(List<Vector3d> dest);

   void remove();

   void setFirstSegmentLength(double length);

   void removeFirstPoint();

   void addPoint(Vector3dc position);

   void setAttachment(RopeHandle.AttachmentPoint attachmentPoint, Vector3dc location, ServerSubLevel subLevel);

   void wakeUp();

   public static enum AttachmentPoint {
      START,
      END;
   }
}
