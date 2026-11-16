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

   public AbstractArrowMixin(EntityType<?> entityType, Level level) {
      super(entityType, level);
   }

   @Redirect(
      method = {"onHitBlock"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;setPos(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void sable$setPos(AbstractArrow arrow, Vec3 pos, @Local(argsOnly = true) BlockHitResult blockHitResult, @Share("difference") LocalRef<Vec3> differenceRef) {
      SubLevel subLevel = Sable.HELPER.getContaining(this.level(), blockHitResult.getLocation());
      if (subLevel == null) {
         arrow.setPos(pos);
      } else {
         Vec3 currentPos = this.position();
         Vec3 localImpulse = subLevel.logicalPose().transformNormalInverse(this.getDeltaMovement());
         if (!this.level().isClientSide() && !this.isInGround()) {
            RigidBodyHandle.of((ServerSubLevel)subLevel).applyImpulseAtPoint(currentPos, localImpulse);
         }

         differenceRef.set(localImpulse);
         arrow.setPos(currentPos.subtract(localImpulse.normalize().scale(0.05F)));
      }
   }

   @Redirect(
      method = {"onHitBlock"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void sable$setDeltaMovement(AbstractArrow arrow, Vec3 difference, @Share("difference") LocalRef<Vec3> differenceRef) {
      Vec3 sharedDifference = (Vec3)differenceRef.get();
      if (sharedDifference == null) {
         arrow.setDeltaMovement(difference);
      } else {
         arrow.setDeltaMovement(sharedDifference);
         Vec3 deltaMovement = this.getDeltaMovement();
         double horizontalDistance = deltaMovement.horizontalDistance();
         this.setXRot((float)(Mth.atan2(deltaMovement.y, horizontalDistance) * 180.0 / (float) Math.PI));
         this.setYRot((float)(Mth.atan2(deltaMovement.x, deltaMovement.z) * 180.0 / (float) Math.PI));
         this.yRotO = this.getYRot();
         this.xRotO = this.getXRot();
      }
   }

   @Inject(
      method = {"startFalling"},
      at = {@At("TAIL")}
   )
   private void sable$startFalling(CallbackInfo ci) {
      SubLevel subLevel = Sable.HELPER.getContaining(this);
      if (subLevel != null) {
         EntitySubLevelUtil.kickEntity(subLevel, this);
      }
   }

   @Redirect(
      method = {"shouldFall"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/phys/AABB;)Z"
      )}
   )
   private boolean sable$noCollision(Level level, AABB aabb) {
      boolean original = level.noCollision(this, aabb);
      return !original ? false : CanFallAtleastHelper.canFallAtleastWithSubLevels(level, aabb) == null;
   }
}
