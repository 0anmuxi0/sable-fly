package dev.ryanhcode.sable.mixin.block_placement;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({BlockPlaceContext.class})
public abstract class BlockPlaceContextMixin extends UseOnContext {
   @Unique
   private final LevelReusedVectors sable$sink = new LevelReusedVectors();
   @Shadow
   protected boolean replaceClicked;

   public BlockPlaceContextMixin(Player var1, InteractionHand var2, BlockHitResult var3) {
      super(var1, var2, var3);
   }

   @Shadow
   @Override
   public abstract BlockPos getClickedPos();

   @Redirect(
      method = {"*"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/Direction;getFacingAxis(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Direction$Axis;)Lnet/minecraft/core/Direction;"
      )}
   )
   private Direction sable$getFacingAxis(Entity var1, Axis var2) {
      SubLevel var3 = Sable.HELPER.getContaining(this.getLevel(), this.getClickedPos());
      if (var3 != null) {
         SubLevelHelper.pushEntityLocal(var3, var1);
         Direction var4 = Direction.getFacingAxis(var1, var2);
         SubLevelHelper.popEntityLocal(var3, var1);
         return var4;
      } else {
         return Direction.getFacingAxis(var1, var2);
      }
   }

   @Redirect(
      method = {"*"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/Direction;orderedByNearest(Lnet/minecraft/world/entity/Entity;)[Lnet/minecraft/core/Direction;"
      )}
   )
   private Direction[] sable$orderedByNearest(Entity var1) {
      SubLevel var2 = Sable.HELPER.getContaining(this.getLevel(), this.getClickedPos());
      if (var2 != null) {
         SubLevelHelper.pushEntityLocal(var2, var1);
         Direction[] var3 = Direction.orderedByNearest(var1);
         SubLevelHelper.popEntityLocal(var2, var1);
         return var3;
      } else {
         return Direction.orderedByNearest(var1);
      }
   }

   @Inject(
      method = {"canPlace"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$canPlace(CallbackInfoReturnable<Boolean> var1) {
      BlockPos var2 = this.getClickedPos();
      SubLevel var3 = Sable.HELPER.getContaining(this.getLevel(), this.getClickedPos());
      BoundingBox3d var4 = new BoundingBox3d(var2);
      Quaterniond var5 = new Quaterniond();
      Vector3d var6 = new Vector3d(var2.getX() + 0.5, var2.getY() + 0.5, var2.getZ() + 0.5);
      if (var3 != null) {
         var3.logicalPose().transformPosition(var6);
         var5.set(var3.logicalPose().orientation());
         var4.transform(var3.logicalPose(), var4);
      }

      for (SubLevel var8 : Sable.HELPER.getAllIntersecting(this.getLevel(), var4)) {
         if (var8 != var3) {
            boolean var9 = this.sable$intersectBlocks(var1, var8, var4, this.sable$sink, var6, var5);
            if (var9) {
               return;
            }
         }
      }

      this.sable$intersectBlocks(var1, null, var4, this.sable$sink, var6, var5);
   }

   @Unique
   private boolean sable$intersectBlocks(
      CallbackInfoReturnable<Boolean> var1, @Nullable SubLevel var2, BoundingBox3dc var3, LevelReusedVectors var4, Vector3d var5, Quaterniond var6
   ) {
      BoundingBox3d var7 = var3.expand(0.36602540380000004, new BoundingBox3d());
      if (var2 != null) {
         var7.transformInverse(var2.logicalPose(), var7);
      }

      for (BlockPos var9 : BlockPos.betweenClosed(
         Mth.floor(var7.minX()), Mth.floor(var7.minY()), Mth.floor(var7.minZ()), Mth.floor(var7.maxX()), Mth.floor(var7.maxY()), Mth.floor(var7.maxZ())
      )) {
         boolean var10 = this.replaceClicked || this.getLevel().getBlockState(var9).canBeReplaced((BlockPlaceContext)(Object)this);
         Vector3d var11 = new Vector3d(var9.getX() + 0.5, var9.getY() + 0.5, var9.getZ() + 0.5);
         Quaterniond var12 = new Quaterniond();
         if (var2 != null) {
            var11 = var2.logicalPose().transformPosition(var11);
            var12.set(var2.logicalPose().orientation());
         }

         OrientedBoundingBox3d var13 = new OrientedBoundingBox3d(var11, new Vector3d(1.0, 1.0, 1.0), var12, var4);
         OrientedBoundingBox3d var14 = new OrientedBoundingBox3d(var5, new Vector3d(1.0, 1.0, 1.0), var6, var4);
         if (!var10 && OrientedBoundingBox3d.sat(var13, var14).lengthSquared() > 0.05) {
            var1.setReturnValue(false);
            return true;
         }
      }

      return false;
   }
}
