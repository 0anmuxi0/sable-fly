package dev.ryanhcode.sable.mixin.entity.entity_pathfinding;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.pathfinder.FlyNodeEvaluator;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.NodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({FlyNodeEvaluator.class})
public abstract class FlyNodeEvaluatorMixin extends NodeEvaluator {
   @Inject(
      method = {"getStart"},
      at = {@At("HEAD")}
   )
   private void sable$init(CallbackInfoReturnable<Node> var1, @Share("mobPosition") LocalRef<Vec3> var2) {
      SubLevel var3 = Sable.HELPER.getTrackingSubLevel(this.mob);
      if (var3 != null) {
         var2.set(var3.logicalPose().transformPositionInverse(this.mob.position()));
      } else {
         var2.set(this.mob.position());
      }
   }

   @Redirect(
      method = {"getStart"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getBlockY()I"
      )}
   )
   private int sable$redirectGetBlockY(Mob var1, @Share("mobPosition") LocalRef<Vec3> var2) {
      return Mth.floor(((Vec3)var2.get()).y);
   }

   @Redirect(
      method = {"getStart"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getX()D"
      )}
   )
   private double sable$redirectGetX(Mob var1, @Share("mobPosition") LocalRef<Vec3> var2) {
      return ((Vec3)var2.get()).x;
   }

   @Redirect(
      method = {"getStart"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getY()D"
      )}
   )
   private double sable$redirectGetY(Mob var1, @Share("mobPosition") LocalRef<Vec3> var2) {
      return ((Vec3)var2.get()).y;
   }

   @Redirect(
      method = {"getStart"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Mob;getZ()D"
      )}
   )
   private double sable$redirectGetZ(Mob var1, @Share("mobPosition") LocalRef<Vec3> var2) {
      return ((Vec3)var2.get()).z;
   }

   @Overwrite
   private Iterable<BlockPos> iteratePathfindingStartNodeCandidatePositions(Mob var1) {
      AABB var2 = var1.getBoundingBox();
      boolean var3 = var2.getSize() < 1.0;
      SubLevel var4 = Sable.HELPER.getTrackingSubLevel(this.mob);
      Vec3 var5 = this.mob.position();
      if (var4 != null) {
         var5 = var4.logicalPose().transformPositionInverse(var5);
      }

      AABB var6 = var1.getBoundingBox().move(var5.subtract(this.mob.position()));
      if (!var3) {
         int var14 = Mth.floor(var5.y);
         return List.of(
            BlockPos.containing(var6.minX, var14, var6.minZ),
            BlockPos.containing(var6.minX, var14, var6.maxZ),
            BlockPos.containing(var6.maxX, var14, var6.minZ),
            BlockPos.containing(var6.maxX, var14, var6.maxZ)
         );
      } else {
         double var7 = Math.max(0.0, 1.1F - var2.getXsize());
         double var9 = Math.max(0.0, 1.1F - var2.getYsize());
         double var11 = Math.max(0.0, 1.1F - var2.getZsize());
         AABB var13 = var6.inflate(var7, var9, var11);
         return BlockPos.randomBetweenClosed(
            var1.getRandom(),
            10,
            Mth.floor(var13.minX),
            Mth.floor(var13.minY),
            Mth.floor(var13.minZ),
            Mth.floor(var13.maxX),
            Mth.floor(var13.maxY),
            Mth.floor(var13.maxZ)
         );
      }
   }
}
