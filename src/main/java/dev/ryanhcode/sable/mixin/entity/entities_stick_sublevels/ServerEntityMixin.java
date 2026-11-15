package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerEntity.Synchronizer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ServerEntity.class})
public class ServerEntityMixin {
   @Shadow
   @Final
   private Entity entity;
   @Shadow
   @Final
   private ServerLevel level;
   @Unique
   private Vec3 sable$oldPos = null;

   @Inject(
      method = {"sendChanges"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;trackingPosition()Lnet/minecraft/world/phys/Vec3;",
         shift = Shift.BEFORE
      )}
   )
   private void sable$pre(CallbackInfo var1, @Share("actuallyInSubLevel") LocalBooleanRef var2) {
      this.sable$oldPos = null;
      SubLevel var3 = Sable.HELPER.getContaining(this.entity);
      if (var3 != null) {
         var2.set(true);
      }

      SubLevel var4 = Sable.HELPER.getTrackingSubLevel(this.entity);
      Vec3 var5 = this.entity.position();
      if (var4 != null) {
         this.sable$oldPos = new Vec3(var5.x, var5.y, var5.z);
         var5 = var4.lastPose().transformPositionInverse(var5);
         ((EntityMovementExtension)this.entity).sable$setPosField(var5);
      }
   }

   @Inject(
      method = {"sendChanges"},
      at = {@At("RETURN")}
   )
   private void sable$postTransform(CallbackInfo var1) {
      if (this.sable$oldPos != null) {
         ((EntityMovementExtension)this.entity).sable$setPosField(this.sable$oldPos);
         this.sable$oldPos = null;
      }
   }

   @WrapOperation(
      method = {"sendChanges"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayers(Lnet/minecraft/network/protocol/Packet;)V"
      )}
   )
   private void sable$sendToTrackingPlayers(Synchronizer var1, Packet<?> var2, Operation<Void> var3, @Share("actuallyInSubLevel") LocalBooleanRef var4) {
      if (var4.get() && var2 instanceof PacketActuallyInSubLevelExtension var5) {
         var5.sable$setActuallyInSubLevel(true);
      }

      var3.call(var1, var2);
   }

   @WrapOperation(
      method = {"sendChanges"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersAndSelf(Lnet/minecraft/network/protocol/Packet;)V"
      )}
   )
   private void sable$sendToTrackingPlayersAndSelf(Synchronizer var1, Packet<?> var2, Operation<Void> var3, @Share("actuallyInSubLevel") LocalBooleanRef var4) {
      if (var4.get() && var2 instanceof PacketActuallyInSubLevelExtension var5) {
         var5.sable$setActuallyInSubLevel(true);
      }

      var3.call(var1, var2);
   }
}
