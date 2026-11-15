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
      PositionMoveRotation var1,
      Set<Relative> var2,
      Entity var3,
      boolean var4,
      Operation<Boolean> var5,
      @Local(argsOnly = true) ClientboundTeleportEntityPacket var6
   ) {
      PositionMoveRotation var7 = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of(var3), var1, var2);
      boolean var8 = sable$isActuallyInSubLevel(var6);
      return this.sable$lerp(var3, var7.position(), var7.yRot(), var7.xRot(), 3, true, var8, () -> (Boolean)var5.call(var1, var2, var3, var4));
   }

   @WrapOperation(
      method = {"handleMoveEntity"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V"
      )}
   )
   private void sable$handleMoveEntity(
      Entity var1, Vec3 var2, float var3, float var4, Operation<Void> var5, @Local(argsOnly = true) ClientboundMoveEntityPacket var6
   ) {
      boolean var7 = sable$isActuallyInSubLevel(var6);
      this.sable$lerp(var1, var2, var3, var4, 3, false, var7, () -> {
         var5.call(var1, var2, var3, var4);
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
   private void sable$handleMoveEntityPosOnly(Entity var1, Vec3 var2, Operation<Void> var3, @Local(argsOnly = true) ClientboundMoveEntityPacket var4) {
      boolean var5 = sable$isActuallyInSubLevel(var4);
      this.sable$lerp(var1, var2, 0.0F, 0.0F, 3, false, var5, true, () -> {
         var3.call(var1, var2);
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
      Entity var1, Vec3 var2, float var3, float var4, Operation<Void> var5, @Local(argsOnly = true) ClientboundEntityPositionSyncPacket var6
   ) {
      this.sable$handleEntityPositionSync(var1, var2, var3, var4, () -> {
         var5.call(var1, var2, var3, var4);
         return false;
      }, var6);
   }

   @WrapOperation(
      method = {"handleEntityPositionSync"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;snapTo(Lnet/minecraft/world/phys/Vec3;FF)V"
      )}
   )
   private void sable$handleEntityPositionSyncSnap(
      Entity var1, Vec3 var2, float var3, float var4, Operation<Void> var5, @Local(argsOnly = true) ClientboundEntityPositionSyncPacket var6
   ) {
      this.sable$handleEntityPositionSync(var1, var2, var3, var4, () -> {
         var5.call(var1, var2, var3, var4);
         return false;
      }, var6);
   }

   @Unique
   private static boolean sable$isActuallyInSubLevel(Object var0) {
      PacketActuallyInSubLevelExtension var1;
      return var0 instanceof PacketActuallyInSubLevelExtension && (var1 = (PacketActuallyInSubLevelExtension)var0).sable$isActuallyInSubLevel();
   }

   @Unique
   private void sable$handleEntityPositionSync(Entity var1, Vec3 var2, float var3, float var4, BooleanSupplier var5, ClientboundEntityPositionSyncPacket var6) {
      boolean var7 = sable$isActuallyInSubLevel(var6);
      this.sable$lerp(var1, var2, var3, var4, 3, true, var7, var5);
   }

   @Unique
   private boolean sable$lerp(Entity var1, Vec3 var2, float var3, float var4, int var5, boolean var6, boolean var7, BooleanSupplier var8) {
      return this.sable$lerp(var1, var2, var3, var4, var5, var6, var7, false, var8);
   }

   @Unique
   private boolean sable$lerp(Entity var1, Vec3 var2, float var3, float var4, int var5, boolean var6, boolean var7, boolean var8, BooleanSupplier var9) {
      EntityStickExtension var10 = (EntityStickExtension)var1;
      ClientSubLevelContainer var11 = SubLevelContainer.getContainer(this.level);
      SubLevel var12 = Sable.HELPER.getContaining(this.level, var2);
      Vec3 var13 = var10.sable$getPlotPosition();
      if (!var7 && var12 == null && var11.inBounds(BlockPos.containing(var2))) {
         return true;
      } else if (var12 == null || var7) {
         SubLevel var17 = Sable.HELPER.getContaining(this.level, var1.position());
         if (var12 != null && var7 && var17 != var12) {
            var1.setPos(var12.logicalPose().transformPositionInverse(var1.position()));
         } else if (var17 != null && var12 == null) {
            var1.setPos(var17.logicalPose().transformPosition(var1.position()));
         }

         boolean var18 = var9.getAsBoolean();
         var10.sable$setPlotPosition(null);
         return var18;
      } else if (!(var1 instanceof LivingEntity)) {
         Vec3 var16 = var12.logicalPose().transformPosition(var2);
         if (var8) {
            var1.moveOrInterpolateTo(var16);
         } else {
            var1.moveOrInterpolateTo(var16, var3, var4);
         }

         return true;
      } else {
         if (var13 == null) {
            var10.sable$setPlotPosition(var12.logicalPose().transformPositionInverse(var1.position()));
         } else {
            SubLevel var14 = Sable.HELPER.getContaining(this.level, var13);
            if (var14 != null && var12 != var14) {
               Vec3 var15 = var14.logicalPose().transformPosition(var13);
               var10.sable$setPlotPosition(var12.logicalPose().transformPositionInverse(var15));
            }
         }

         var1.moveOrInterpolateTo(var3, var4);
         var10.sable$plotLerpTo(var2, var5);
         return true;
      }
   }
}
