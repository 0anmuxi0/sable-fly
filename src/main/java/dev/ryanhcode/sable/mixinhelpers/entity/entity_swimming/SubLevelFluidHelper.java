package dev.ryanhcode.sable.mixinhelpers.entity.entity_swimming;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;

public final class SubLevelFluidHelper {
   private static final Set<TagKey<Fluid>> FLUIDS_WITH_CURRENT = Set.of(FluidTags.WATER, FluidTags.LAVA);

   private SubLevelFluidHelper() {
   }

   public static void gather(Entity var0, boolean var1, Map<Holder<Fluid>, SubLevelFluidHelper.Found> var2, Map<TagKey<Fluid>, SubLevelFluidHelper.Found> var3) {
      var2.clear();
      var3.clear();
      AABB var4 = var0.getFluidInteractionBox();
      if (var4 != null) {
         Level var5 = var0.level();
         LevelReusedVectors var6 = ((LevelExtension)var5).sable$getJOMLSink();
         BoundingBox3d var7 = new BoundingBox3d(var4);
         BoundingBox3d var8 = new BoundingBox3d();
         MutableBlockPos var9 = new MutableBlockPos();
         Vector3d var10 = new Vector3d();
         Vector3d var11 = new Vector3d();
         Quaterniond var12 = new Quaterniond();
         Vec3 var13 = var0.getEyePosition();

         for (SubLevel var15 : Sable.HELPER.getAllIntersecting(var5, var7)) {
            Pose3dc var16 = var15.lastPose();
            var7.transformInverse(var16, var8);
            Quaterniond var17 = var16.orientation().conjugate(var12);
            var17.rotateY(SubLevelEntityCollision.getHitBoxYaw(var16));
            OrientedBoundingBox3d var18 = new OrientedBoundingBox3d(var16.transformPositionInverse(var7.center(var10)), var7.size(var11), var17, var6);
            OrientedBoundingBox3d var19 = new OrientedBoundingBox3d(new Vector3d(), new Vector3d(1.0, 1.0, 1.0), JOMLConversion.QUAT_IDENTITY, var6);
            Vec3 var20 = var16.transformPositionInverse(var13);
            int var21 = Mth.floor(var20.x);
            int var22 = Mth.floor(var20.z);
            int var23 = Mth.floor(var8.minX);
            int var24 = Mth.ceil(var8.maxX);
            int var25 = Mth.floor(var8.minY);
            int var26 = Mth.ceil(var8.maxY);
            int var27 = Mth.floor(var8.minZ);
            int var28 = Mth.ceil(var8.maxZ);
            double var29 = Double.MAX_VALUE;
            boolean var31 = false;

            for (int var32 = var23; var32 < var24; var32++) {
               for (int var33 = var25; var33 < var26; var33++) {
                  for (int var34 = var27; var34 < var28; var34++) {
                     var9.set(var32, var33, var34);
                     FluidState var37 = var5.getFluidState(var9);
                     if (!var37.isEmpty()) {
                        double var38 = (double)var33 + var37.getHeight(var5, var9);
                        if (!var31) {
                           for (Vector3d var43 : var18.vertices(var6.a)) {
                              var29 = Math.min(var29, var43.y);
                           }

                           var31 = true;
                        }

                        if (!(var38 < var29)) {
                           var19.getPosition().set(var32 + 0.5, var33 + 0.5, var34 + 0.5);
                           if (!(OrientedBoundingBox3d.sat(var18, var19, var6.mtv).lengthSquared() <= 0.0)) {
                              Holder var46 = var37.typeHolder();
                              SubLevelFluidHelper.Found var47 = var2.computeIfAbsent(var46, var0x -> new SubLevelFluidHelper.Found());
                              var47.height = Math.max(var38 - var29, var47.height);
                              if (var32 == var21 && var34 == var22 && var20.y >= var33 && var20.y <= (double)var33 + var37.getHeight(var5, var9)) {
                                 var47.eyesInside = true;
                              }

                              if (!var1) {
                                 for (TagKey var49 : FLUIDS_WITH_CURRENT) {
                                    if (var46.is(var49)) {
                                       SubLevelFluidHelper.Found var44 = var3.computeIfAbsent(var49, var0x -> new SubLevelFluidHelper.Found());
                                       var44.height = Math.max(var47.height, var44.height);
                                       Vec3 var45 = var37.getFlow(var5, var9);
                                       if (var44.height < 0.4) {
                                          var45 = var45.scale(var44.height);
                                       }

                                       var44.current = var44.current.add(var16.transformNormal(var45));
                                       var44.currentBlocks++;
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static FluidState getFluidStateAt(Entity var0, BlockPos var1) {
      Level var2 = var0.level();
      FluidState var3 = var2.getFluidState(var1);
      if (!var3.isEmpty()) {
         return var3;
      } else {
         Vec3 var4 = Vec3.atCenterOf(var1);
         BoundingBox3d var5 = new BoundingBox3d(var1).expand(0.5);

         for (SubLevel var7 : Sable.HELPER.getAllIntersecting(var2, var5)) {
            BlockPos var8 = BlockPos.containing(var7.lastPose().transformPositionInverse(var4));
            FluidState var9 = var2.getFluidState(var8);
            if (!var9.isEmpty()) {
               return var9;
            }
         }

         return var3;
      }
   }

   public static void applyCurrent(Entity var0, SubLevelFluidHelper.Found var1, double var2) {
      if (var1.currentBlocks != 0 && !(var1.current.lengthSqr() < 1.0E-5F)) {
         Vec3 var4 = var0 instanceof Player ? var1.current.scale(1.0 / var1.currentBlocks) : var1.current.normalize();
         Vec3 var5 = var0.getDeltaMovement();
         var4 = var4.scale(var2);
         if (Math.abs(var5.x) < 0.003 && Math.abs(var5.z) < 0.003 && var4.length() < 0.0045000000000000005) {
            var4 = var4.normalize().scale(0.0045000000000000005);
         }

         var0.addDeltaMovement(var4);
      }
   }

   public static final class Found {
      public double height;
      public boolean eyesInside;
      public Vec3 current = Vec3.ZERO;
      public int currentBlocks;
   }
}
