package dev.ryanhcode.sable.mixinhelpers.compatibility.create;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Position;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public final class SubLevelOutlinePose {
   private SubLevelOutlinePose() {
   }

   @Nullable
   public static ClientSubLevel find(Position pos) {
      ClientLevel level = Minecraft.getInstance().level;
      if (level == null) {
         return null;
      } else {
         return Sable.HELPER.getContaining(level, pos) instanceof ClientSubLevel clientSubLevel ? clientSubLevel : null;
      }
   }

   @Nullable
   public static ClientSubLevel find(AABB box) {
      ClientSubLevel center = find(box.getCenter());
      if (center != null) {
         return center;
      } else {
         ClientSubLevel min = find(new Vec3(box.minX, box.minY, box.minZ));
         return min != null ? min : find(new Vec3(box.maxX, box.maxY, box.maxZ));
      }
   }

   public static Vec3 plotCamera(Pose3dc pose, Vec3 camera) {
      return pose.transformPositionInverse(camera);
   }

   public static void apply(Pose3dc pose, PoseStack ms) {
      ms.mulPose(new Matrix4f().rotate(new Quaternionf(pose.orientation())).scale((float)pose.scale().x(), (float)pose.scale().y(), (float)pose.scale().z()));
   }

   public static AABB projectToWorld(ClientSubLevel subLevel, AABB box) {
      return new BoundingBox3d(box).transform(subLevel.renderPose()).toMojang();
   }

   public static void runProjected(PoseStack ms, ClientSubLevel subLevel, Vec3 camera, Consumer<Vec3> body) {
      Pose3dc pose = subLevel.renderPose();
      ms.pushPose();

      try {
         apply(pose, ms);
         body.accept(pose.transformPositionInverse(camera));
      } finally {
         ms.popPose();
      }
   }
}
