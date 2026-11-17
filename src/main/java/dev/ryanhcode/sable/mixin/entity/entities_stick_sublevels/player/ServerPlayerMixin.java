package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels.player;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin extends Entity {
   @Unique
   private final Vector3d sable$trackedSubLevelPos = new Vector3d();

   public ServerPlayerMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Shadow
   public abstract ServerLevel level();

   @Inject(require = 0, method = {"tick"},
      at = {@At("HEAD")}
   )
   public void tick(CallbackInfo ci) {
      SubLevel trackingSubLevel = Sable.HELPER.getTrackingSubLevel(this);
      if (trackingSubLevel != null && !trackingSubLevel.isRemoved()) {
         Vector3d entityCenter = JOMLConversion.getAABBCenter(this.getBoundingBox(), this.sable$trackedSubLevelPos);
         double entityCenterX = entityCenter.x();
         double entityCenterY = entityCenter.y();
         double entityCenterZ = entityCenter.z();
         Pose3dc pose = trackingSubLevel.logicalPose();
         Pose3dc lastPose = trackingSubLevel.lastPose();
         Vector3d inherited = pose.transformPosition(lastPose.transformPositionInverse(entityCenter, entityCenter));
         Vec3 position = this.position();
         this.setPos(new Vec3(position.x + inherited.x - entityCenterX, position.y + inherited.y - entityCenterY, position.z + inherited.z - entityCenterZ));
      }
   }

   @WrapOperation(require = 0, method = {"setCamera"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"
      )}
   )
   private boolean sable$fixSpectatorSetCamera(
      ServerPlayer instance,
      ServerLevel level,
      double x,
      double y,
      double z,
      Set<Relative> relatives,
      float newYRot,
      float newXRot,
      boolean resetCamera,
      Operation<Boolean> original
   ) {
      SubLevel subLevel = Sable.HELPER.getContaining(level, x, z);
      if (subLevel == null) {
         return original.call(instance, level, x, y, z, relatives, newYRot, newXRot, resetCamera);
      } else {
         Vector3d pos = new Vector3d(x, y, z);
         subLevel.lastPose().transformPosition(pos);
         return original.call(instance, level, pos.x, pos.y, pos.z, relatives, newYRot, newXRot, resetCamera);
      }
   }

   @WrapOperation(require = 0, method = {"tick"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerPlayer;absSnapTo(DDDFF)V"
      )}
   )
   private void sable$fixSpectatorTick(ServerPlayer instance, double x, double y, double z, float yRot, float xRot, Operation<Void> original) {
      SubLevel subLevel = Sable.HELPER.getContaining(instance.level(), x, z);
      if (subLevel == null) {
         original.call(instance, x, y, z, yRot, xRot);
      } else {
         Vector3d pos = new Vector3d(x, y, z);
         subLevel.lastPose().transformPosition(pos);
         original.call(instance, pos.x, pos.y, pos.z, yRot, xRot);
      }
   }
}
