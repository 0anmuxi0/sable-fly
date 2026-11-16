package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ClientPacketListener.class})
public abstract class ClientPacketListenerMixin {
   @Unique
   private static final int SABLE$DEFAULT_LERP_STEPS = 3;
   @Shadow
   private ClientLevel level;

   @WrapOperation(
      method = {"handleTeleportEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;setValuesFromPositionPacket(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;Lnet/minecraft/world/entity/Entity;Z)Z",
         ordinal = 1
      )}
   )
   private boolean sable$handleTeleportEntity(
      PositionMoveRotation values,
      Set<Relative> relatives,
      Entity entity,
      boolean teleport,
      Operation<Boolean> operation,
      @Local(argsOnly = true) ClientboundTeleportEntityPacket packet
   ) {
      PositionMoveRotation absoluteValues = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of(entity), values, relatives);
      boolean actuallyInSubLevel = sable$isActuallyInSubLevel(packet);
      return this.sable$lerp(entity, absoluteValues.position(), absoluteValues.yRot(), absoluteValues.xRot(), 3, true, actuallyInSubLevel, () -> (Boolean)operation.call(values, relatives, entity, teleport));
   }

   @WrapOperation(
      method = {"handleMoveEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V"
      )}
   )
   private void sable$handleMoveEntity(
      Entity instance, Vec3 pos, float yRot, float xRot, Operation<Void> operation, @Local(argsOnly = true) ClientboundMoveEntityPacket packet
   ) {
      boolean actuallyInSubLevel = sable$isActuallyInSubLevel(packet);
      this.sable$lerp(instance, pos, yRot, xRot, 3, false, actuallyInSubLevel, () -> {
         operation.call(instance, pos, yRot, xRot);
         return false;
      });
   }

   @WrapOperation(
      method = {"handleMoveEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void sable$handleMoveEntityPosOnly(Entity instance, Vec3 pos, Operation<Void> operation, @Local(argsOnly = true) ClientboundMoveEntityPacket packet) {
      boolean actuallyInSubLevel = sable$isActuallyInSubLevel(packet);
      this.sable$lerp(instance, pos, 0.0F, 0.0F, 3, false, actuallyInSubLevel, true, () -> {
         operation.call(instance, pos);
         return false;
      });
   }

   @WrapOperation(
      method = {"handleEntityPositionSync"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V"
      )}
   )
   private void sable$handleEntityPositionSyncInterpolation(
      Entity instance, Vec3 pos, float yRot, float xRot, Operation<Void> operation, @Local(argsOnly = true) ClientboundEntityPositionSyncPacket packet
   ) {
      this.sable$handleEntityPositionSync(instance, pos, yRot, xRot, () -> {
         operation.call(instance, pos, yRot, xRot);
         return false;
      }, packet);
   }

   @WrapOperation(
      method = {"handleEntityPositionSync"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;snapTo(Lnet/minecraft/world/phys/Vec3;FF)V"
      )}
   )
   private void sable$handleEntityPositionSyncSnap(
      Entity instance, Vec3 pos, float yRot, float xRot, Operation<Void> operation, @Local(argsOnly = true) ClientboundEntityPositionSyncPacket packet
   ) {
      this.sable$handleEntityPositionSync(instance, pos, yRot, xRot, () -> {
         operation.call(instance, pos, yRot, xRot);
         return false;
      }, packet);
   }

   @Unique
   private static boolean sable$isActuallyInSubLevel(Object packet) {
      PacketActuallyInSubLevelExtension extension;
      return packet instanceof PacketActuallyInSubLevelExtension && (extension = (PacketActuallyInSubLevelExtension)packet).sable$isActuallyInSubLevel();
   }

   @Unique
   private void sable$handleEntityPositionSync(Entity instance, Vec3 pos, float yRot, float xRot, BooleanSupplier originalCall, ClientboundEntityPositionSyncPacket packet) {
      boolean actuallyInSubLevel = sable$isActuallyInSubLevel(packet);
      this.sable$lerp(instance, pos, yRot, xRot, 3, true, actuallyInSubLevel, originalCall);
   }

   @Unique
   private boolean sable$lerp(Entity entity, Vec3 pos, float yRot, float xRot, int lerpSteps, boolean teleport, boolean actuallyInSubLevel, BooleanSupplier originalCall) {
      return this.sable$lerp(entity, pos, yRot, xRot, lerpSteps, teleport, actuallyInSubLevel, false, originalCall);
   }

   @Unique
   private boolean sable$lerp(Entity entity, Vec3 pos, float yRot, float xRot, int lerpSteps, boolean teleport, boolean actuallyInSubLevel, boolean posOnly, BooleanSupplier originalCall) {
      EntityStickExtension extension = (EntityStickExtension)entity;
      ClientSubLevelContainer container = SubLevelContainer.getContainer(this.level);
      SubLevel subLevel = Sable.HELPER.getContaining(this.level, pos);
      Vec3 plotPosition = extension.sable$getPlotPosition();
      if (!actuallyInSubLevel && subLevel == null && container.inBounds(BlockPos.containing(pos))) {
         return true;
      } else if (subLevel == null || actuallyInSubLevel) {
         SubLevel currentSubLevel = Sable.HELPER.getContaining(this.level, entity.position());
         if (subLevel != null && actuallyInSubLevel && currentSubLevel != subLevel) {
            entity.setPos(subLevel.logicalPose().transformPositionInverse(entity.position()));
         } else if (currentSubLevel != null && subLevel == null) {
            entity.setPos(currentSubLevel.logicalPose().transformPosition(entity.position()));
         }

         boolean originalResult = originalCall.getAsBoolean();
         extension.sable$setPlotPosition(null);
         return originalResult;
      } else if (!(entity instanceof LivingEntity)) {
         Vec3 transformedPos = subLevel.logicalPose().transformPosition(pos);
         if (posOnly) {
            entity.moveOrInterpolateTo(transformedPos);
         } else {
            entity.moveOrInterpolateTo(transformedPos, yRot, xRot);
         }

         return true;
      } else {
         if (plotPosition == null) {
            extension.sable$setPlotPosition(subLevel.logicalPose().transformPositionInverse(entity.position()));
         } else {
            SubLevel plotSubLevel = Sable.HELPER.getContaining(this.level, plotPosition);
            if (plotSubLevel != null && subLevel != plotSubLevel) {
               Vec3 globalPlotPos = plotSubLevel.logicalPose().transformPosition(plotPosition);
               extension.sable$setPlotPosition(subLevel.logicalPose().transformPositionInverse(globalPlotPos));
            }
         }

         entity.moveOrInterpolateTo(yRot, xRot);
         extension.sable$plotLerpTo(pos, lerpSteps);
         return true;
      }
   }
}
