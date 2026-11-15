package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityMixin implements EntityStickExtension {
   @Shadow
   private Level level;
   @Unique
   private Vec3 sable$plotPosition = null;

   @Shadow
   public abstract void setPos(Vec3 var1);

   @Shadow
   public abstract void snapTo(Vec3 var1);

   @Shadow
   public abstract void snapTo(double var1, double var3, double var5);

   @Inject(
      method = {"tick"},
      at = {@At("RETURN")}
   )
   private void sable$updateSubLevelPosition(CallbackInfo var1) {
      Entity var2 = (Entity)(Object)this;
      if (this.sable$plotPosition != null) {
         SubLevel var4 = Sable.HELPER.getContaining(this.level, this.sable$plotPosition);
         if (var4 != null) {
            this.setPos(var4.logicalPose().transformPosition(this.sable$plotPosition));
            ((EntityMovementExtension)this).sable$setTrackingSubLevel(var4);
         } else {
            this.sable$plotPosition = null;
         }
      } else if (this.level.isClientSide() && !(var2 instanceof Player var3 && var3.isLocalPlayer()) && !(var2 instanceof ItemEntity)) {
         ((EntityMovementExtension)this).sable$setTrackingSubLevel(null);
      }
   }

   @Override
   public void sable$plotLerpTo(Vec3 var1, int var2) {
      this.sable$setPlotPosition(var1);
   }

   @Override
   public void sable$setPlotPosition(@Nullable Vec3 var1) {
      this.sable$plotPosition = var1;
   }

   @Nullable
   @Override
   public Vec3 sable$getPlotPosition() {
      return this.sable$plotPosition;
   }

   @Inject(
      method = {"recreateFromPacket"},
      at = {@At("TAIL")}
   )
   public void sable$recreateFromPacket(ClientboundAddEntityPacket var1, CallbackInfo var2) {
      if (EntitySubLevelUtil.shouldKick((Entity)(Object)this)) {
         double var3 = var1.getX();
         double var5 = var1.getY();
         double var7 = var1.getZ();
         SubLevel var9 = Sable.HELPER.getContaining(this.level, var3, var7);
         if (var9 != null) {
            Vector3d var10 = var9.logicalPose().transformPosition(new Vector3d(var3, var5, var7));
            this.snapTo(var10.x, var10.y, var10.z);
         }
      }
   }
}
