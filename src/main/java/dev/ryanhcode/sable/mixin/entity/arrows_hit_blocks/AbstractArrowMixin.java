package dev.ryanhcode.sable.mixin.entity.arrows_hit_blocks;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.mixinhelpers.CanFallAtleastHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({AbstractArrow.class})
public abstract class AbstractArrowMixin extends Entity {
   @Shadow
   public abstract boolean isInGround();

   public AbstractArrowMixin(EntityType<?> var1, Level var2) {
      super(var1, var2);
   }

   @Redirect(
      method = {"onHitBlock"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;setPos(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void sable$setPos(AbstractArrow var1, Vec3 var2, @Local(argsOnly = true) BlockHitResult var3, @Share("difference") LocalRef<Vec3> var4) {
      SubLevel var5 = Sable.HELPER.getContaining(this.level(), var3.getLocation());
      if (var5 == null) {
         var1.setPos(var2);
      } else {
         Vec3 var6 = this.position();
         Vec3 var7 = var5.logicalPose().transformNormalInverse(this.getDeltaMovement());
         if (!this.level().isClientSide() && !this.isInGround()) {
            RigidBodyHandle.of((ServerSubLevel)var5).applyImpulseAtPoint(var6, var7);
         }

         var4.set(var7);
         var1.setPos(var6.subtract(var7.normalize().scale(0.05F)));
      }
   }

   @Redirect(
      method = {"onHitBlock"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void sable$setDeltaMovement(AbstractArrow var1, Vec3 var2, @Share("difference") LocalRef<Vec3> var3) {
      Vec3 var4 = (Vec3)var3.get();
      if (var4 == null) {
         var1.setDeltaMovement(var2);
      } else {
         var1.setDeltaMovement(var4);
         Vec3 var5 = this.getDeltaMovement();
         double var6 = var5.horizontalDistance();
         this.setXRot((float)(Mth.atan2(var5.y, var6) * 180.0 / (float) Math.PI));
         this.setYRot((float)(Mth.atan2(var5.x, var5.z) * 180.0 / (float) Math.PI));
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }
   }

   @Inject(
      method = {"startFalling"},
      at = {@At("TAIL")}
   )
   private void sable$startFalling(CallbackInfo var1) {
      SubLevel var2 = Sable.HELPER.getContaining(this);
      if (var2 != null) {
         EntitySubLevelUtil.kickEntity(var2, this);
      }
   }

   @Redirect(
      method = {"shouldFall"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/phys/AABB;)Z"
      )}
   )
   private boolean sable$noCollision(Level var1, AABB var2) {
      boolean var3 = var1.noCollision(this, var2);
      return !var3 ? false : CanFallAtleastHelper.canFallAtleastWithSubLevels(var1, var2) == null;
   }
}
