package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets={"com.zurrtum.create.AllHandle"})
public class AllHandleMixin {
    @WrapOperation(method={"onBlockEntityConfiguration(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lnet/minecraft/core/BlockPos;ILjava/util/function/Predicate;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/core/BlockPos;closerThan(Lnet/minecraft/core/Vec3i;D)Z")})
    private static boolean sable$poseAwareReachCheck(BlockPos pos, Vec3i playerPos, double distance, Operation<Boolean> original, @Local(argsOnly=true) ServerGamePacketListenerImpl listener) {
        double dz;
        double dy;
        if (((Boolean)original.call(pos, playerPos, distance)).booleanValue()) {
            return true;
        }
        SubLevel subLevel = Sable.HELPER.getContaining((Level)listener.player.level(), (Vec3i)pos);
        if (subLevel == null) {
            return false;
        }
        Pose3d pose = subLevel.logicalPose();
        Vector3d localPlayer = pose.transformPositionInverse(JOMLConversion.toJOML((Position)Vec3.atLowerCornerOf((Vec3i)playerPos)));
        double dx = (double)pos.getX() - localPlayer.x();
        return dx * dx + (dy = (double)pos.getY() - localPlayer.y()) * dy + (dz = (double)pos.getZ() - localPlayer.z()) * dz < distance * distance;
    }

    @WrapOperation(method={"onSuperGlueRemoval(Lnet/minecraft/server/network/ServerGamePacketListenerImpl;Lcom/zurrtum/create/infrastructure/packet/c2s/SuperGlueRemovalPacket;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/server/level/ServerPlayer;distanceToSqr(Lnet/minecraft/world/phys/Vec3;)D")})
    private static double sable$poseAwareGlueRemovalReach(ServerPlayer player, Vec3 gluePos, Operation<Double> original) {
        double direct = (Double)original.call(player, gluePos);
        return direct <= 1024.0 ? direct : Sable.HELPER.distanceSquaredWithSubLevels((Level)player.level(), (Position)player.position(), (Position)gluePos);
    }
}