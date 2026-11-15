package dev.ryanhcode.sable.client.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.network.packets.tcp.ClientboundRopeSyncPacket;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.Vec3;

public final class RopeManager {
   private static final Map<UUID, BaseRope> ROPES = new Object2ObjectOpenHashMap();

   private RopeManager() {
   }

   public static void sync(List<ClientboundRopeSyncPacket.Entry> entries) {
      List<UUID> present = new ArrayList<>(entries.size());

      for (ClientboundRopeSyncPacket.Entry entry : entries) {
         present.add(entry.id());
         BaseRope rope = ROPES.computeIfAbsent(entry.id(), RopeManager::createRope);
         rope.setPoints(entry.points());
         rope.setColor(entry.color());
         rope.setWidth(entry.width());
      }

      ROPES.keySet().removeIf(id -> !present.contains(id));
   }

   public static BaseRope createRope(UUID id) {
      return new BaseRope(id);
   }

   public static void remove(UUID id) {
      ROPES.remove(id);
   }

   public static void clear() {
      ROPES.clear();
   }

   public static Collection<BaseRope> getRopes() {
      return ROPES.values();
   }

   public static void renderAll(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cameraPos) {
      for (BaseRope rope : ROPES.values()) {
         rope.render(poseStack, collector, cameraPos);
      }
   }

   public static UUID pick(Vec3 origin, Vec3 direction, double range) {
      Vec3 end = origin.add(direction.scale(range));
      UUID best = null;
      double bestDistance = Double.MAX_VALUE;
      double threshold = 0.8;

      for (BaseRope rope : ROPES.values()) {
         List<Vec3> points = rope.getPoints();

         for (int i = 0; i + 1 < points.size(); i++) {
            double distance = segmentDistance(origin, end, points.get(i), points.get(i + 1));
            if (distance < 0.8 && distance < bestDistance) {
               bestDistance = distance;
               best = rope.getId();
            }
         }
      }

      return best;
   }

   private static double segmentDistance(Vec3 p1, Vec3 q1, Vec3 p2, Vec3 q2) {
      double d1x = q1.x - p1.x;
      double d1y = q1.y - p1.y;
      double d1z = q1.z - p1.z;
      double d2x = q2.x - p2.x;
      double d2y = q2.y - p2.y;
      double d2z = q2.z - p2.z;
      double rx = p1.x - p2.x;
      double ry = p1.y - p2.y;
      double rz = p1.z - p2.z;
      double a = d1x * d1x + d1y * d1y + d1z * d1z;
      double e = d2x * d2x + d2y * d2y + d2z * d2z;
      double f = d2x * rx + d2y * ry + d2z * rz;
      if (a <= 1.0E-12 && e <= 1.0E-12) {
         return Math.sqrt(rx * rx + ry * ry + rz * rz);
      } else {
         double s;
         double t;
         if (a <= 1.0E-12) {
            s = 0.0;
            t = clamp(f / e, 0.0, 1.0);
         } else {
            double c = d1x * rx + d1y * ry + d1z * rz;
            if (e <= 1.0E-12) {
               t = 0.0;
               s = clamp(-c / a, 0.0, 1.0);
            } else {
               double b = d1x * d2x + d1y * d2y + d1z * d2z;
               double denom = a * e - b * b;
               s = denom != 0.0 ? clamp((b * f - c * e) / denom, 0.0, 1.0) : 0.0;
               t = (b * s + f) / e;
               if (t < 0.0) {
                  t = 0.0;
                  s = clamp(-c / a, 0.0, 1.0);
               } else if (t > 1.0) {
                  t = 1.0;
                  s = clamp((b - c) / a, 0.0, 1.0);
               }
            }
         }

         double cx = p1.x + d1x * s - (p2.x + d2x * t);
         double cy = p1.y + d1y * s - (p2.y + d2y * t);
         double cz = p1.z + d1z * s - (p2.z + d2z * t);
         return Math.sqrt(cx * cx + cy * cy + cz * cz);
      }
   }

   private static double clamp(double value, double min, double max) {
      return value < min ? min : Math.min(value, max);
   }
}
