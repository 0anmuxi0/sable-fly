package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Set;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin extends Player {
   @Shadow
   public ServerGamePacketListenerImpl connection;

   public ServerPlayerMixin(Level level, GameProfile gameProfile) {
      super(level, gameProfile);
   }

   @WrapOperation(
      method = {"startRiding"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V"
      )}
   )
   private void sable$adjustTeleportPacket(
      ServerGamePacketListenerImpl instance, PositionMoveRotation change, Set<Relative> relatives, Operation<Void> original
   ) {
      Entity vehicle = this.getVehicle();
      if (vehicle == null) {
         original.call(instance, change, relatives);
      } else {
         SubLevel containingSubLevel = Sable.HELPER.getContaining(vehicle);
         if (containingSubLevel == null) {
            original.call(instance, change, relatives);
         } else {
            this.absSnapTo(change.position().x, change.position().y, change.position().z, change.yRot(), change.xRot());
            Vec3 pos = containingSubLevel.logicalPose().transformPositionInverse(this.position());
            this.connection.send(new ClientboundPlayerPositionPacket(-1, new PositionMoveRotation(pos, Vec3.ZERO, change.yRot(), change.xRot()), relatives));
         }
      }
   }
}
