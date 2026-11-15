package dev.ryanhcode.sable.mixin.entity.teleport_players;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({ServerPlayer.class})
public abstract class ServerPlayerMixin {
   @Shadow
   public abstract ServerLevel level();

   @WrapMethod(
      method = {"teleportTo(DDD)V"}
   )
   public void sable$teleportTo(double x, double y, double z, Operation<Void> original) {
      Vector3d globalPos = Sable.HELPER.projectOutOfSubLevel(this.level(), new Vector3d(x, y, z));
      original.call(globalPos.x, globalPos.y, globalPos.z);
   }

   @WrapMethod(
      method = {"teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"}
   )
   public boolean sable$teleportTo(
      ServerLevel serverLevel, double x, double y, double z, Set<Relative> set, float g, float h, boolean resetCamera, Operation<Boolean> original
   ) {
      Vector3d globalPos = Sable.HELPER.projectOutOfSubLevel(serverLevel, new Vector3d(x, y, z));
      return original.call(serverLevel, globalPos.x, globalPos.y, globalPos.z, set, g, h, resetCamera);
   }
}
