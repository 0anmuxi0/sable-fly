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
   private void sable$setPos(
      AbstractArrow arrow, Vec3 position, @Local(argsOnly = true) BlockHitResult blockHitResult, @Share("difference") LocalRef<Vec3> differenceRef
   ) {
      SubLevel subLevel = Sable.HELPER.getContaining(this.level(), blockHitResult.getLocation());
      if (subLevel == null) {
         arrow.setPos(position);
      } else {
         Vec3 localPosition = this.position();
         Vec3 diff = subLevel.logicalPose().transformNormalInverse(this.getDeltaMovement());
         if (!this.level().isClientSide() && !this.isInGround()) {
            RigidBodyHandle.of((ServerSubLevel)subLevel).applyImpulseAtPoint(localPosition, diff);
         }

         differenceRef.set(diff);
         arrow.setPos(localPosition.subtract(diff.normalize().scale(0.05F)));
      }
   }

   @Redirect(
      method = {"onHitBlock"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   private void sable$setDeltaMovement(AbstractArrow arrow, Vec3 movement, @Share("difference") LocalRef<Vec3> differenceRef) {
      Vec3 difference = (Vec3)differenceRef.get();
      if (difference == null) {
         arrow.setDeltaMovement(movement);
      } else {
         arrow.setDeltaMovement(difference);
         Vec3 vec3 = this.getDeltaMovement();
         double d = vec3.horizontalDistance();
         this.setXRot((float)(Mth.atan2(vec3.y, d) * 180.0F / (float)Math.PI));
         this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * 180.0F / (float)Math.PI));
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
