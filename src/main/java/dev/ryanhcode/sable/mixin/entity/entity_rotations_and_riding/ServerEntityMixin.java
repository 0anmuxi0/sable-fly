package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import dev.ryanhcode.sable.Sable;
import java.util.List;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * B9-f: 26.1.2 的 {@code ServerEntity.sendChanges()} 里乘客包发送重构为
 * {@code Synchronizer.sendToTrackingPlayersFiltered(ClientboundSetPassengersPacket, Predicate)}
 * (1.21.1 时代是 {@code broadcast.accept(new ClientboundSetPassengersPacket(...))}),
 * 之后才更新 {@code lastPassengers}。当实体位于子层内时,1.21.1 的 ServerEntityMixin
 * 会在发送前把 {@code lastPassengers} 提前同步为当前乘客列表,使 {@code passengers.equals(lastPassengers)}
 * 判断被强制短路为 false,保证子层内上下车时乘客包一定发出。
 *
 * <p>注入点从 1.21.1 的 {@code Ljava/util/function/Consumer;accept(Ljava/lang/Object;)V}
 * 改为 26.1.2 的
 * {@code Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersFiltered(Lnet/minecraft/network/protocol/Packet;Ljava/util/function/Predicate;)V}
 * (该方法在 {@code sendChanges()} 内唯一, javap 确认偏移 50, 在偏移 57 的
 * {@code putfield lastPassengers} 之前)。
 */
@Mixin({ServerEntity.class})
public abstract class ServerEntityMixin {
   @Shadow
   private List<Entity> lastPassengers;

   @Shadow
   @Final
   private Entity entity;

   @Inject(
      method = {"sendChanges"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/server/level/ServerEntity$Synchronizer;sendToTrackingPlayersFiltered(Lnet/minecraft/network/protocol/Packet;Ljava/util/function/Predicate;)V"
      )}
   )
   private void sable$beforeSendChanges(CallbackInfo ci) {
      List<Entity> passengers = this.entity.getPassengers();
      if (Sable.HELPER.getContaining(this.entity) != null) {
         this.lastPassengers = passengers;
      }
   }
}
