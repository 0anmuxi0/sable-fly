package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
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
import net.minecraft.world.entity.PositionPath;
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
      PositionMoveRotation change,
      Set<Relative> relatives,
      Entity entity,
      boolean interpolate,
      Operation<Boolean> original,
      @Local(argsOnly = true) ClientboundTeleportEntityPacket packet
   ) {
      PositionMoveRotation newValues = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of(entity), change, relatives);
      boolean actuallyInSubLevel = (Object) packet instanceof PacketActuallyInSubLevelExtension extension && extension.sable$isActuallyInSubLevel();
      return this.sable$lerp(
         entity,
         newValues.position(),
         newValues.yRot(),
         newValues.xRot(),
         3,
         true,
         actuallyInSubLevel,
         () -> original.call(change, relatives, entity, interpolate)
      );
   }

   @WrapOperation(
      method = {"handleMoveEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/entity/PositionPath;FF)V"
      )}
   )
   private void sable$handleMoveEntity(
      Entity instance, PositionPath path, float yRot, float xRot, Operation<Void> original, @Local(argsOnly = true) ClientboundMoveEntityPacket packet
   ) {
      boolean actuallyInSubLevel = (Object) packet instanceof PacketActuallyInSubLevelExtension extension && extension.sable$isActuallyInSubLevel();
      Vec3 target = path.endPosition();
      this.sable$lerp(instance, target, yRot, xRot, 3, false, actuallyInSubLevel, () -> {
         original.call(instance, path, yRot, xRot);
         return false;
      });
   }

   @WrapOperation(
      method = {"handleEntityPositionSync"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/entity/PositionPath;FF)V"
      )}
   )
   private void sable$handleEntityPositionSyncInterpolation(
      Entity instance, PositionPath path, float yRot, float xRot, Operation<Void> original, @Local(argsOnly = true) ClientboundEntityPositionSyncPacket packet
   ) {
      this.sable$handleEntityPositionSync(instance, path.endPosition(), yRot, xRot, () -> {
         original.call(instance, path, yRot, xRot);
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
      Entity instance, Vec3 target, float yRot, float xRot, Operation<Void> original, @Local(argsOnly = true) ClientboundEntityPositionSyncPacket packet
   ) {
      this.sable$handleEntityPositionSync(instance, target, yRot, xRot, () -> {
         original.call(instance, target, yRot, xRot);
         return false;
      }, packet);
   }

   @Unique
   private void sable$handleEntityPositionSync(
      Entity entity, Vec3 target, float yRot, float xRot, BooleanSupplier vanillaCall, ClientboundEntityPositionSyncPacket packet
   ) {
      boolean actuallyInSubLevel = (Object) packet instanceof PacketActuallyInSubLevelExtension extension && extension.sable$isActuallyInSubLevel();
      this.sable$lerp(entity, target, yRot, xRot, 3, true, actuallyInSubLevel, vanillaCall);
   }

   @Unique
   private boolean sable$lerp(
      Entity entity, Vec3 target, float pYRot, float pXRot, int pLerpSteps, boolean pTeleport, boolean actuallyInSubLevel, BooleanSupplier vanillaCall
   ) {
      EntityStickExtension extension = (EntityStickExtension)entity;
      SubLevelContainer container = SubLevelContainer.getContainer(this.level);
      SubLevel subLevel = Sable.HELPER.getContaining(this.level, target);
      Vec3 plotPosition = extension.sable$getPlotPosition();
      if (!actuallyInSubLevel && subLevel == null && container.inBounds(BlockPos.containing(target))) {
         return true;
      } else if (subLevel == null || actuallyInSubLevel) {
         SubLevel existingSubLevel = Sable.HELPER.getContaining(this.level, entity.position());
         if (subLevel != null && actuallyInSubLevel && existingSubLevel != subLevel) {
            entity.setPos(subLevel.logicalPose().transformPositionInverse(entity.position()));
         } else if (existingSubLevel != null && subLevel == null) {
            entity.setPos(existingSubLevel.logicalPose().transformPosition(entity.position()));
         }

         boolean result = vanillaCall.getAsBoolean();
         extension.sable$setPlotPosition(null);
         return result;
      } else if (!(entity instanceof LivingEntity)) {
         Vec3 pos = subLevel.logicalPose().transformPosition(target);
         entity.moveOrInterpolateTo(pos, pYRot, pXRot);
         return true;
      } else {
         if (plotPosition == null) {
            extension.sable$setPlotPosition(subLevel.logicalPose().transformPositionInverse(entity.position()));
         } else {
            SubLevel existingSubLevel = Sable.HELPER.getContaining(this.level, plotPosition);
            if (existingSubLevel != null && subLevel != existingSubLevel) {
               Vec3 globalPlotPos = existingSubLevel.logicalPose().transformPosition(plotPosition);
               extension.sable$setPlotPosition(subLevel.logicalPose().transformPositionInverse(globalPlotPos));
            }
         }

         entity.moveOrInterpolateTo(pYRot, pXRot);
         extension.sable$plotLerpTo(target, pLerpSteps);
         return true;
      }
   }
}
