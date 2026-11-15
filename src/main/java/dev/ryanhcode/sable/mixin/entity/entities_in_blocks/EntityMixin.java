package dev.ryanhcode.sable.mixin.entity.entities_in_blocks;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier.StepBasedCollector;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class})
public abstract class EntityMixin {
   @Shadow
   private Level level;
   @Shadow
   @Final
   private StepBasedCollector insideEffectCollector;

   @Shadow
   public abstract boolean isAlive();

   @Shadow
   public abstract AABB getBoundingBox();

   @Shadow
   protected abstract void onInsideBlock(BlockState var1);

   @Inject(
      method = {"checkInsideBlocks(Ljava/util/List;Lnet/minecraft/world/entity/InsideBlockEffectApplier$StepBasedCollector;)V"},
      at = {@At("TAIL")}
   )
   protected void checkInsideBlocks(CallbackInfo var1) {
      AABB var2 = this.getBoundingBox();
      Entity var3 = (Entity)(Object)this;
      BoundingBox3d var4 = new BoundingBox3d(var2);

      for (SubLevel var6 : Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(var2))) {
         var4.set(var2);
         var4.transformInverse(var6.logicalPose(), var4);
         BlockPos var7 = BlockPos.containing(var4.minX + 1.0E-7, var4.minY + 1.0E-7, var4.minZ + 1.0E-7);
         BlockPos var8 = BlockPos.containing(var4.maxX - 1.0E-7, var4.maxY - 1.0E-7, var4.maxZ - 1.0E-7);
         boolean var9 = SubLevelHelper.pushEntityLocalIfNeeded(var6, var3);

         try {
            if (this.level.hasChunksAt(var7, var8)) {
               MutableBlockPos var10 = new MutableBlockPos();

               for (int var11 = var7.getX(); var11 <= var8.getX(); var11++) {
                  for (int var12 = var7.getY(); var12 <= var8.getY(); var12++) {
                     for (int var13 = var7.getZ(); var13 <= var8.getZ(); var13++) {
                        if (!this.isAlive()) {
                           return;
                        }

                        var10.set(var11, var12, var13);
                        BlockState var14 = this.level.getBlockState(var10);

                        try {
                           var14.entityInside(this.level, var10, var3, this.insideEffectCollector, true);
                           this.onInsideBlock(var14);
                        } catch (Throwable var21) {
                           CrashReport var16 = CrashReport.forThrowable(var21, "Colliding entity with block");
                           CrashReportCategory var17 = var16.addCategory("Block being collided with");
                           CrashReportCategory.populateBlockDetails(var17, this.level, var10, var14);
                           throw new ReportedException(var16);
                        }
                     }
                  }
               }
            }
         } finally {
            SubLevelHelper.popEntityLocalIfNeeded(var6, var3, var9);
         }
      }
   }
}
