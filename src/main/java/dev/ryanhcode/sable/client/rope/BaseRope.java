package dev.ryanhcode.sable.client.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack.Pose;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class BaseRope {
   protected final UUID id;
   protected final List<Vec3> points = new ArrayList<>();
   protected int color = -1;
   protected float width = 0.15F;

   public BaseRope(UUID id) {
      this.id = id;
   }

   public UUID getId() {
      return this.id;
   }

   public List<Vec3> getPoints() {
      return this.points;
   }

   public void setPoints(List<Vec3> points) {
      this.points.clear();
      this.points.addAll(points);
   }

   public int getColor() {
      return this.color;
   }

   public void setColor(int color) {
      this.color = color;
   }

   public float getWidth() {
      return this.width;
   }

   public void setWidth(float width) {
      this.width = width;
   }

   public void render(PoseStack poseStack, SubmitNodeCollector collector, Vec3 cameraPos) {
      if (this.points.size() >= 2) {
         int argb = this.color;
         float alpha = (argb >> 24 & 0xFF) / 255.0F;
         float red = (argb >> 16 & 0xFF) / 255.0F;
         float green = (argb >> 8 & 0xFF) / 255.0F;
         float blue = (argb & 0xFF) / 255.0F;
         float radius = this.width;
         collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (pose, consumer) -> {
            Vector3f from = new Vector3f();
            Vector3f to = new Vector3f();
            Vector3f direction = new Vector3f();
            Vector3f side = new Vector3f();
            Vector3f up = new Vector3f();

            for (int i = 0; i + 1 < this.points.size(); i++) {
               Vec3 p0 = this.points.get(i);
               Vec3 p1 = this.points.get(i + 1);
               from.set((float)(p0.x - cameraPos.x), (float)(p0.y - cameraPos.y), (float)(p0.z - cameraPos.z));
               to.set((float)(p1.x - cameraPos.x), (float)(p1.y - cameraPos.y), (float)(p1.z - cameraPos.z));
               direction.set(to).sub(from);
               if (!(direction.lengthSquared() < 1.0E-9F)) {
                  direction.normalize();
                  side.set(direction).cross(0.0F, 1.0F, 0.0F);
                  if (side.lengthSquared() < 1.0E-6F) {
                     side.set(direction).cross(1.0F, 0.0F, 0.0F);
                  }

                  side.normalize().mul(radius);
                  up.set(side).cross(direction).normalize().mul(radius);
                  this.emitBox(consumer, pose, from, to, side, up, red, green, blue, alpha);
               }
            }
         });
      }
   }

   protected void emitBox(
      VertexConsumer consumer, Pose pose, Vector3f from, Vector3f to, Vector3f side, Vector3f up, float red, float green, float blue, float alpha
   ) {
      float a0x = from.x + side.x + up.x;
      float a0y = from.y + side.y + up.y;
      float a0z = from.z + side.z + up.z;
      float a1x = from.x + side.x - up.x;
      float a1y = from.y + side.y - up.y;
      float a1z = from.z + side.z - up.z;
      float a2x = from.x - side.x - up.x;
      float a2y = from.y - side.y - up.y;
      float a2z = from.z - side.z - up.z;
      float a3x = from.x - side.x + up.x;
      float a3y = from.y - side.y + up.y;
      float a3z = from.z - side.z + up.z;
      float b0x = to.x + side.x + up.x;
      float b0y = to.y + side.y + up.y;
      float b0z = to.z + side.z + up.z;
      float b1x = to.x + side.x - up.x;
      float b1y = to.y + side.y - up.y;
      float b1z = to.z + side.z - up.z;
      float b2x = to.x - side.x - up.x;
      float b2y = to.y - side.y - up.y;
      float b2z = to.z - side.z - up.z;
      float b3x = to.x - side.x + up.x;
      float b3y = to.y - side.y + up.y;
      float b3z = to.z - side.z + up.z;
      this.emitQuad(consumer, pose, a0x, a0y, a0z, a1x, a1y, a1z, b1x, b1y, b1z, b0x, b0y, b0z, red, green, blue, alpha);
      this.emitQuad(consumer, pose, a1x, a1y, a1z, a2x, a2y, a2z, b2x, b2y, b2z, b1x, b1y, b1z, red, green, blue, alpha);
      this.emitQuad(consumer, pose, a2x, a2y, a2z, a3x, a3y, a3z, b3x, b3y, b3z, b2x, b2y, b2z, red, green, blue, alpha);
      this.emitQuad(consumer, pose, a3x, a3y, a3z, a0x, a0y, a0z, b0x, b0y, b0z, b3x, b3y, b3z, red, green, blue, alpha);
   }

   protected void emitQuad(
      VertexConsumer consumer,
      Pose pose,
      float x0,
      float y0,
      float z0,
      float x1,
      float y1,
      float z1,
      float x2,
      float y2,
      float z2,
      float x3,
      float y3,
      float z3,
      float red,
      float green,
      float blue,
      float alpha
   ) {
      consumer.addVertex(pose, x0, y0, z0).setColor(red, green, blue, alpha);
      consumer.addVertex(pose, x1, y1, z1).setColor(red, green, blue, alpha);
      consumer.addVertex(pose, x2, y2, z2).setColor(red, green, blue, alpha);
      consumer.addVertex(pose, x3, y3, z3).setColor(red, green, blue, alpha);
   }
}
