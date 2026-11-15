package dev.ryanhcode.sable.mixin.explosion;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin({ServerExplosion.class})
public class ExplosionMixin {
   @Shadow
   @Final
   private ServerLevel level;
   @Shadow
   @Final
   private Vec3 center;
   @Shadow
   @Final
   private ExplosionDamageCalculator damageCalculator;
   @Shadow
   @Final
   @Nullable
   private Entity source;

   @Inject(
      method = {"calculateExplodedPositions"},
      at = {@At("HEAD")}
   )
   private void sable$preExplode(CallbackInfoReturnable<List<BlockPos>> var1, @Share("explodedSet") LocalRef<Set<BlockPos>> var2) {
      var2.set(new ObjectOpenHashSet());
   }

   @Inject(
      method = {"calculateExplodedPositions"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getBlockExplosionResistance(Lnet/minecraft/world/level/Explosion;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Ljava/util/Optional;"
      )},
      locals = LocalCapture.CAPTURE_FAILHARD
   )
   private void sable$redirectBlockExplosionResistance(
      CallbackInfoReturnable<List<BlockPos>> var1,
      Set<BlockPos> var2,
      int var3,
      int var4,
      int var5,
      int var6,
      double var7,
      double var9,
      double var11,
      double var13,
      float var15,
      double var16,
      double var18,
      double var20,
      float var22,
      BlockPos var23,
      BlockState var24,
      FluidState var25,
      @Local(ordinal = 0) LocalFloatRef var26,
      @Share("explodedSet") LocalRef<Set<BlockPos>> var27
   ) {
      Explosion var28 = (Explosion)this;
      if (var24.isAir()) {
         BoundingBox3d var29 = new BoundingBox3d(var23);
         Iterable<SubLevel> var30 = Sable.HELPER.getAllIntersecting(this.level, var29);
         ServerSubLevelContainer var31 = SubLevelContainer.getContainer(this.level);

         for (SubLevel var33 : var30) {
            Pose3d var34 = var33.logicalPose();
            Vec3 var35 = var34.transformPositionInverse(new Vec3(var16, var18, var20));
            Vec3 var36 = var34.transformPositionInverse(this.center);
            var23 = BlockPos.containing(var35);
            var24 = this.level.getBlockState(var23);
            var25 = this.level.getFluidState(var23);
            boolean var37 = var15 > 0.0;
            Optional<Float> var38 = this.damageCalculator.getBlockExplosionResistance(var28, this.level, var23, var24, var25);
            if (var38.isPresent()) {
               var15 -= (var38.get() + 0.3F) * 0.3F;
            }

            if (var15 > 0.0F && this.damageCalculator.shouldBlockExplode(var28, this.level, var23, var24, var15)) {
               var2.add(var23);
            }

            boolean var39 = (this.source instanceof AbstractWindCharge || this.damageCalculator == AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR)
               && !var24.isAir();
            if (var37 && (var15 < 0.0F || var39) && ((Set)var27.get()).add(var23)) {
               ((Set)var27.get()).add(var23);
               if (var33 instanceof ServerSubLevel var40) {
                  SubLevelPhysicsSystem var41 = var31.physicsSystem();
                  RigidBodyHandle var42 = var41.getPhysicsHandle(var40);
                  Vec3 var43 = Vec3.atCenterOf(var23);
                  Vec3 var44 = var43.subtract(var36).normalize().scale(5.0);
                  var42.applyImpulseAtPoint(var43, var44);
               }
            }
         }

         var26.set(var15);
      }
   }
}
