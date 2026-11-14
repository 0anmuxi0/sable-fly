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
   private void sable$preExplode(CallbackInfoReturnable<List<BlockPos>> cir, @Share("explodedSet") LocalRef<Set<BlockPos>> explodedSet) {
      explodedSet.set(new ObjectOpenHashSet());
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
      CallbackInfoReturnable<List<BlockPos>> cir,
      Set<BlockPos> set,
      int i,
      int j,
      int k,
      int l,
      double d0,
      double d1,
      double d2,
      double d3,
      float f,
      double d4,
      double d6,
      double d8,
      float f1,
      BlockPos blockpos,
      BlockState blockstate,
      FluidState fluidstate,
      @Local(ordinal = 0) LocalFloatRef fReference,
      @Share("explodedSet") LocalRef<Set<BlockPos>> explodedSet
   ) {
      Explosion self = (Explosion)this;
      if (blockstate.isAir()) {
         BoundingBox3d globalBounds = new BoundingBox3d(blockpos);
         Iterable<SubLevel> subLevels = Sable.HELPER.getAllIntersecting(this.level, globalBounds);
         SubLevelContainer container = SubLevelContainer.getContainer(this.level);

         for (SubLevel subLevel : subLevels) {
            Pose3d pose = subLevel.logicalPose();
            Vec3 localRayPosition = pose.transformPositionInverse(new Vec3(d4, d6, d8));
            Vec3 localExplosionPosition = pose.transformPositionInverse(this.center);
            blockpos = BlockPos.containing(localRayPosition);
            blockstate = this.level.getBlockState(blockpos);
            fluidstate = this.level.getFluidState(blockpos);
            boolean canExplodeBefore = f > 0.0;
            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance(self, this.level, blockpos, blockstate, fluidstate);
            if (optional.isPresent()) {
               f -= (optional.get() + 0.3F) * 0.3F;
            }

            if (f > 0.0F && this.damageCalculator.shouldBlockExplode(self, this.level, blockpos, blockstate, f)) {
               set.add(blockpos);
            }

            boolean wind = (this.source instanceof AbstractWindCharge || this.damageCalculator == AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR)
               && !blockstate.isAir();
            if (canExplodeBefore && (f < 0.0F || wind) && ((Set)explodedSet.get()).add(blockpos)) {
               ((Set)explodedSet.get()).add(blockpos);
               if (subLevel instanceof ServerSubLevel serverSubLevel) {
                  SubLevelPhysicsSystem physicsSystem = ((ServerSubLevelContainer)container).physicsSystem();
                  RigidBodyHandle handle = physicsSystem.getPhysicsHandle(serverSubLevel);
                  Vec3 pos = Vec3.atCenterOf(blockpos);
                  Vec3 force = pos.subtract(localExplosionPosition).normalize().scale(5.0);
                  handle.applyImpulseAtPoint(pos, force);
               }
            }
         }

         fReference.set(f);
      }
   }
}
