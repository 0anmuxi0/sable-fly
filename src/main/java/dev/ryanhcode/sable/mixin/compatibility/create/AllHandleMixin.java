package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.AllHandle"}
)
public class AllHandleMixin {
   @WrapOperation(
      method = {"onBlockEntityConfiguration(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lnet/minecraft/core/BlockPos;ILjava/util/function/Predicate;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z"
      )}
   )
   private static boolean sable$poseAwareReachCheck(
      BlockPos pos, Vec3i playerPos, double distance, Operation<Boolean> original, @Local(argsOnly = true) ServerGamePacketListenerImpl listener
   ) {
      if (original.call(pos, playerPos, distance)) {
         return true;
      } else {
         SubLevel subLevel = Sable.HELPER.getContaining(listener.player.level(), pos);
         if (subLevel == null) {
            return false;
         } else {
            Pose3dc pose = subLevel.logicalPose();
            Vector3dc localPlayer = pose.transformPositionInverse(JOMLConversion.toJOML(Vec3.atLowerCornerOf(playerPos)));
            double dx = pos.getX() - localPlayer.x();
            double dy = pos.getY() - localPlayer.y();
            double dz = pos.getZ() - localPlayer.z();
            return dx * dx + dy * dy + dz * dz < distance * distance;
         }
      }
   }

   @WrapOperation(
      method = {"onSuperGlueRemoval(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lcom/zurrtum/create/infrastructure/packet/c2s/SuperGlueRemovalPacket;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerPlayer;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D"
      )}
   )
   private static double sable$poseAwareGlueRemovalReach(ServerPlayer player, Vec3 gluePos, Operation<Double> original) {
      double direct = original.call(player, gluePos);
      return direct <= 1024.0 ? direct : Sable.HELPER.distanceSquaredWithSubLevels(player.level(), player.position(), gluePos);
   }
}
