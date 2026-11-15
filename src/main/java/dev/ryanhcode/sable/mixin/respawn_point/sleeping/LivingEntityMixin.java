package dev.ryanhcode.sable.mixin.respawn_point.sleeping;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin({LivingEntity.class})
public abstract class LivingEntityMixin extends Entity {
   public LivingEntityMixin(EntityType<?> var1, Level var2) {
      super(var1, var2);
   }

   @Overwrite
   private void setPosToBed(BlockPos var1) {
      Vector3d var2 = JOMLConversion.upFromBottomCenterOf(var1, 0.6875);
      this.setPos(JOMLConversion.toMojang(Sable.HELPER.projectOutOfSubLevel(this.level(), var2)));
   }

   @Redirect(
      method = {"stopSleeping"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/LivingEntity;setPos(DDD)V"
      )}
   )
   private void sable$stopSleeping(LivingEntity var1, double var2, double var4, double var6) {
      double var8 = this.getBoundingBox().getYsize() / 2.0;
      Vector3d var10 = new Vector3d(var2, var4 + var8, var6);
      Sable.HELPER.projectOutOfSubLevel(this.level(), var10).sub(0.0, var8, 0.0);
      var1.setPos(JOMLConversion.toMojang(var10));
   }
}
