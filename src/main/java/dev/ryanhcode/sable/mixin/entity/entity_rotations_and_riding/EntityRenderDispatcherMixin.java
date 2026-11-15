package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinhelpers.camera.camera_rotation.EntitySubLevelRotationHelper;
import dev.ryanhcode.sable.mixinterface.entity.entity_rotations_and_riding.EntityTransformationExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState.LeashState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderDispatcher.class})
public class EntityRenderDispatcherMixin {
   @Unique
   private final Map<EntityRenderState, EntityRenderDispatcherMixin.SableRenderData> sable$renderData = new IdentityHashMap<>();
   @Unique
   private boolean sable$rotated = false;
   @Unique
   private Quaternionf sable$cameraOrientation;

   @Inject(
      method = {"extractEntity"},
      at = {@At("RETURN")}
   )
   private <E extends Entity> void sable$captureEntity(E entity, float partialTicks, CallbackInfoReturnable<EntityRenderState> cir) {
      EntityRenderState renderState = cir.getReturnValue();
      ClientSubLevel clientSubLevel = Sable.HELPER.getContainingClient(entity);
      EntityTransformationExtension transformation = (EntityTransformationExtension)renderState;
      Quaterniond orientation = EntitySubLevelRotationHelper.getEntityOrientation(
         entity, subLevel -> ((ClientSubLevel)subLevel).renderPose(partialTicks), partialTicks, EntitySubLevelRotationHelper.Type.ENTITY
      );
      if (clientSubLevel != null) {
         Pose3dc pose = clientSubLevel.renderPose(partialTicks);
         Vector3d pos = new Vector3d(
            Mth.lerp((double)partialTicks, entity.xo, entity.getX()),
            Mth.lerp((double)partialTicks, entity.yo, entity.getY()),
            Mth.lerp((double)partialTicks, entity.zo, entity.getZ())
         );
         pose.transformPosition(pos);
         renderState.x = pos.x;
         renderState.y = pos.y;
         renderState.z = pos.z;
         transformation.sable$setSubLevelOrientation(new Quaternionf(pose.orientation()));
         transformation.sable$setSubLevelScale(new Vector3f(pose.scale()));
         transformation.sable$setSubLevelPivot(new Vector3f(0.0F, 0.0F, 0.0F));
      } else if (orientation != null) {
         transformation.sable$setSubLevelOrientation(new Quaternionf(orientation));
         transformation.sable$setSubLevelScale(new Vector3f(1.0F, 1.0F, 1.0F));
         transformation.sable$setSubLevelPivot(new Vector3f(entity.getEyePosition(partialTicks).subtract(entity.position()).toVector3f()));
      } else {
         transformation.sable$setSubLevelOrientation(new Quaternionf());
         transformation.sable$setSubLevelScale(new Vector3f(1.0F, 1.0F, 1.0F));
         transformation.sable$setSubLevelPivot(new Vector3f(0.0F, 0.0F, 0.0F));
      }

      this.sable$adjustLeashes(entity, renderState);
      this.sable$renderData.put(renderState, new EntityRenderDispatcherMixin.SableRenderData(entity, partialTicks));
   }

   @Inject(
      method = {"submit"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V",
         shift = Shift.AFTER,
         ordinal = 0
      )}
   )
   private <S extends EntityRenderState> void sable$rotateEntity(
      S renderState, CameraRenderState camera, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci
   ) {
      EntityRenderDispatcherMixin.SableRenderData renderData = this.sable$renderData.remove(renderState);
      if (renderData != null) {
         Entity entity = renderData.entity();
         if (EntitySubLevelUtil.shouldKick(entity)) {
            float partialTick = renderData.partialTick();
            Quaterniond orientation = EntitySubLevelRotationHelper.getEntityOrientation(
               entity, level -> ((ClientSubLevel)level).renderPose(), partialTick, EntitySubLevelRotationHelper.Type.ENTITY
            );
            if (orientation != null) {
               this.sable$cameraOrientation = new Quaternionf(camera.orientation);
               camera.orientation = new Quaternionf(orientation).conjugate().mul(camera.orientation);
               this.sable$rotated = true;
            }
         }
      }
   }

   @Inject(
      method = {"submit"},
      at = {@At(
         value = "INVOKE",
         target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",
         shift = Shift.BEFORE
      )}
   )
   private <S extends EntityRenderState> void sable$popPose(
      S renderState, CameraRenderState camera, double x, double y, double z, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CallbackInfo ci
   ) {
      if (this.sable$rotated) {
         camera.orientation = this.sable$cameraOrientation;
         this.sable$cameraOrientation = null;
         this.sable$rotated = false;
      }
   }

   @Unique
   private void sable$adjustLeashes(Entity entity, EntityRenderState renderState) {
      if (renderState.leashStates != null) {
         SubLevel entitySubLevel = Sable.HELPER.getContaining(entity);

         for (LeashState leashState : renderState.leashStates) {
            Vector3d end = new Vector3d(leashState.end.x, leashState.end.y, leashState.end.z);
            SubLevel holderSubLevel = Sable.HELPER.getContaining(entity.level(), end);
            if (holderSubLevel != null) {
               holderSubLevel.logicalPose().transformPosition(end);
            }

            if (entitySubLevel != null) {
               entitySubLevel.logicalPose().transformPositionInverse(end);
            }

            leashState.end = new Vec3(end.x, end.y, end.z);
         }
      }
   }

   @Unique
   private record SableRenderData(Entity entity, float partialTick) {
   }
}
