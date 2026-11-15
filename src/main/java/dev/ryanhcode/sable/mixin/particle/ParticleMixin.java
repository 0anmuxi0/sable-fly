package dev.ryanhcode.sable.mixin.particle;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.ClientLevelPlot;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Particle.class})
public abstract class ParticleMixin implements ParticleExtension {
   @Unique
   private static final double LIGHT_QUERY_AREA = 8.0;
   @Unique
   private static final BoundingBox3d TEMP_BOX = new BoundingBox3d();
   @Unique
   private final Vector3d sable$inheritedVelocity = new Vector3d();
   @Shadow
   public double x;
   @Shadow
   public double y;
   @Shadow
   public double z;
   @Shadow
   protected double xd;
   @Shadow
   protected double zd;
   @Shadow
   protected double yd;
   @Shadow
   protected double xo;
   @Shadow
   protected double zo;
   @Shadow
   protected double yo;
   @Shadow
   @Final
   protected ClientLevel level;
   @Shadow
   protected boolean onGround;
   @Unique
   private boolean sable$checkedInitialKick = false;
   @Unique
   @Nullable
   private volatile ClientSubLevel sable$trackingSubLevel = null;
   @Unique
   @Nullable
   private Vector3d sable$localTrackingAnchor = null;
   @Unique
   @Nullable
   private volatile List<ClientSubLevel> sable$nearbySubLevels;
   @Shadow
   private boolean stoppedByCollision;

   @Shadow
   public abstract void setPos(double var1, double var3, double var5);

   @Shadow
   public abstract void move(double var1, double var3, double var5);

   @Shadow
   protected abstract void setLocationFromBoundingbox();

   @Shadow
   public abstract AABB getBoundingBox();

   @Shadow
   public abstract void setBoundingBox(AABB var1);

   @Shadow
   public abstract void tick();

   @ModifyConstant(
      method = {"Lnet/minecraft/client/particle/Particle;<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)V"},
      constant = {@Constant(
         doubleValue = 0.1F
      )}
   )
   private double sable$removeUpwardsVelocity(double var1) {
      return 0.0;
   }

   @Inject(
      method = {"Lnet/minecraft/client/particle/Particle;<init>(Lnet/minecraft/client/multiplayer/ClientLevel;DDDDDD)V"},
      at = {@At("TAIL")}
   )
   private void sable$addUpwardsVelocity(ClientLevel var1, double var2, double var4, double var6, double var8, double var10, double var12, CallbackInfo var14) {
      Vec3 var15 = new Vec3(this.x, this.y, this.z);
      ClientSubLevel var16 = Sable.HELPER.getContainingClient(var15);
      if (var16 != null) {
         Vec3 var17 = var16.logicalPose().transformNormalInverse(new Vec3(0.0, 0.1, 0.0));
         this.xd = this.xd + var17.x;
         this.yd = this.yd + var17.y;
         this.zd = this.zd + var17.z;
         this.sable$setTrackingSubLevel(var16, var15);
      }
   }

   @Override
   public void sable$initialKickOut() {
      Vec3 var1 = new Vec3(this.x, this.y, this.z);
      if (!this.sable$checkedInitialKick) {
         ClientSubLevel var2 = Sable.HELPER.getContainingClient(var1);
         if (var2 != null) {
            Pose3d var3 = var2.logicalPose();
            Vec3 var4 = new Vec3(this.xo, this.yo, this.zo);
            Vec3 var5 = var3.transformPosition(var1);
            Vec3 var6 = var3.transformPosition(var4);
            Vec3 var7 = var3.transformNormal(new Vec3(this.xd, this.yd, this.zd));
            this.x = var5.x;
            this.y = var5.y;
            this.z = var5.z;
            this.xo = var6.x;
            this.yo = var6.y;
            this.zo = var6.z;
            this.xd = var7.x;
            this.yd = var7.y;
            this.zd = var7.z;
            this.setPos(this.x, this.y, this.z);
            this.sable$setTrackingSubLevel(var2, var1);
         }

         this.sable$checkedInitialKick = true;
      }
   }

   private void sable$kickFromTracking() {
      Vector3d var1 = this.sable$trackingSubLevel.logicalPose().transformPositionInverse(new Vector3d(this.x, this.y, this.z));
      Sable.HELPER.getVelocity(this.level, var1, this.sable$inheritedVelocity);
      this.sable$inheritedVelocity.mul(0.05);
      this.sable$localTrackingAnchor = null;
      this.sable$trackingSubLevel = null;
   }

   @Override
   public void sable$moveWithInheritedVelocity() {
   }

   @Override
   public void sable$setTrackingSubLevel(ClientSubLevel var1, Vec3 var2) {
      this.sable$trackingSubLevel = var1;
      this.sable$localTrackingAnchor = new Vector3d();
      this.sable$localTrackingAnchor.set(var2.x, var2.y, var2.z);
      this.sable$inheritedVelocity.zero();
   }

   @Override
   public SubLevel sable$getTrackingSubLevel() {
      return this.sable$trackingSubLevel;
   }

   @WrapMethod(
      method = {"move"}
   )
   private void sable$moveWithSubLevels(double var1, double var3, double var5, Operation<Void> var7) {
      AABB var8 = this.getBoundingBox();
      BoundingBox3d var9 = new BoundingBox3d(var8).expand(0.5);
      ObjectOpenHashSet var10 = new ObjectOpenHashSet();
      boolean var11 = this instanceof ParticleSubLevelKickable var12 && !var12.sable$shouldCareAboutIntersectingSubLevels();
      if (!var11) {
         for (SubLevel var13 : Sable.HELPER.getAllIntersecting(this.level, var9)) {
            var10.add(var13);
         }
      }

      if (this.sable$trackingSubLevel != null) {
         var10.add(this.sable$trackingSubLevel);
      }

      if (this.sable$trackingSubLevel != null && this.sable$trackingSubLevel.isRemoved()) {
         this.sable$trackingSubLevel = null;
         this.sable$localTrackingAnchor = null;
      }

      Vector3d var36 = new Vector3d();
      Vector3d var37 = new Vector3d();
      Vector3d var14 = new Vector3d();
      Vector3d var15 = new Vector3d();
      Vector3d var16 = new Vector3d();
      Vector3d var17 = new Vector3d(var1, var3, var5);
      var17.add(this.sable$inheritedVelocity);
      boolean var18 = false;
      ObjectIterator var19 = var10.iterator();

      while (var19.hasNext()) {
         SubLevel var20 = (SubLevel)var19.next();
         Pose3d var21 = var20.logicalPose();
         Pose3dc var22 = var20.lastPose();
         var36.zero();
         if (this.sable$trackingSubLevel == var20) {
            JOMLConversion.getAABBCenter(var8, var14);
            var22.transformPositionInverse(var14, var37);
            Vector3d var23 = var21.transformPosition(var37);
            var36.add(var23).sub(var14);
         } else {
            JOMLConversion.getAABBCenter(var8, var14);
            var22.transformPositionInverse(var14, var15);
            var21.transformPositionInverse(var14, var16);
            ClipContext var39 = new ClipContext(
               JOMLConversion.toMojang(var15), JOMLConversion.toMojang(var16), Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
            );
            ((ClipContextExtension)var39).sable$setDoNotProject(true);
            BlockHitResult var24 = this.level.clip(var39);
            if (var24.getType() == Type.BLOCK) {
               var21.transformPosition(JOMLConversion.toJOML(var24.getLocation(), var36)).sub(var14);
               if (this.sable$trackingSubLevel == null) {
                  this.sable$setTrackingSubLevel((ClientSubLevel)var20, var24.getLocation());
               }
            }
         }

         boolean var40 = !(this.sable$trackingSubLevel == var20 && this instanceof ParticleSubLevelKickable var41)
            || var41.sable$shouldCollideWithTrackingSubLevel();
         if (var40) {
            Vector3d var42 = JOMLConversion.getAABBCenter(var8, var14).add(var36);
            var21.transformPositionInverse(var42, var15);
            var21.transformPositionInverse(var42.add(var17, var16));
            ClipContext var25 = new ClipContext(
               JOMLConversion.toMojang(var15), JOMLConversion.toMojang(var16), Block.COLLIDER, Fluid.NONE, CollisionContext.empty()
            );
            ((ClipContextExtension)var25).sable$setDoNotProject(true);
            BlockHitResult var26 = this.level.clip(var25);
            if (var26 != null && var26.getType() == Type.BLOCK) {
               Vec3 var27 = var21.transformPosition(var26.getLocation()).subtract(var42.x(), var42.y(), var42.z());
               var17.set(var27.x, var27.y, var27.z);
            }
         }

         var17.add(var36);
         if (var40) {
            Vec3 var43 = var21.transformPositionInverse(var8.getCenter().add(var17.x, var17.y, var17.z));
            double var44 = Math.max(var8.getXsize(), Math.max(var8.getYsize(), var8.getZsize())) / 2.0;
            BoundingBox3d var45 = new BoundingBox3d();
            var45.set(var43.x - var44, var43.y - var44, var43.z - var44, var43.x + var44, var43.y + var44, var43.z + var44);
            Vector3d var28 = this.resolveAABBCollision(var45);
            if (var28.lengthSquared() > 0.0) {
               var20.logicalPose().transformNormal(var28);
               Vector3d var29 = var28.normalize(new Vector3d());
               Vector3dc var30 = OrientedBoundingBox3d.UP;
               if (this instanceof ParticleSubLevelKickable var31) {
                  var30 = var31.sable$getUpDirection();
               }

               double var46 = var29.dot(var30);
               if (var46 > 0.6) {
                  var18 = true;
               }

               double var33 = var29.dot(this.xd, this.yd, this.zd);
               this.xd = this.xd - var33 * var29.x;
               this.yd = this.yd - var33 * var29.y;
               this.zd = this.zd - var33 * var29.z;
               if (var46 > 0.6 || var46 < 0.6) {
                  this.xd = var30.x() * this.xd;
                  this.yd = var30.y() * this.yd;
                  this.zd = var30.z() * this.zd;
               }

               var17.add(var28);
               if (this.sable$trackingSubLevel == null) {
                  this.sable$setTrackingSubLevel((ClientSubLevel)var20, var43);
               }
            }
         }
      }

      var7.call(var17.x, var17.y, var17.z);
      this.onGround |= var18;
      if (this.sable$trackingSubLevel != null
         && !(this instanceof ParticleSubLevelKickable var38 && !var38.sable$shouldKickFromTracking())
         && this.sable$trackingSubLevel.logicalPose().transformPosition(this.sable$localTrackingAnchor, new Vector3d()).distanceSquared(this.x, this.y, this.z)
            > 0.25) {
         this.sable$kickFromTracking();
      }
   }

   private Vector3d resolveAABBCollision(BoundingBox3d var1) {
      Vector3d var2 = new Vector3d();
      Vector3d var3 = new Vector3d();
      double[] var4 = new double[]{0.0};
      int var5 = (int)Math.floor(var1.minX());
      int var6 = (int)Math.floor(var1.minY());
      int var7 = (int)Math.floor(var1.minZ());
      int var8 = (int)Math.floor(var1.maxX());
      int var9 = (int)Math.floor(var1.maxY());
      int var10 = (int)Math.floor(var1.maxZ());
      MutableBlockPos var11 = new MutableBlockPos();

      for (int var12 = var5; var12 <= var8; var12++) {
         for (int var13 = var6; var13 <= var9; var13++) {
            for (int var14 = var7; var14 <= var10; var14++) {
               MutableBlockPos var15 = var11.set(var12, var13, var14);
               BlockState var16 = this.level.getBlockState(var15);
               if (!var16.isAir()) {
                  VoxelShape var17 = var16.getCollisionShape(this.level, var15);
                  if (!var17.isEmpty()) {
                     int var18 = var12;
                     int var19 = var13;
                     int var20 = var14;
                     if (var16.isCollisionShapeFullBlock(this.level, var15)) {
                        TEMP_BOX.setUnchecked(0.0 + var18, 0.0 + var19, 0.0 + var20, 1.0 + var18, 1.0 + var19, 1.0 + var20);
                        var3.zero();
                        this.resolveAABBAABBCollision(var1, TEMP_BOX, var3);
                        double var21 = var3.lengthSquared();
                        if (var21 > var4[0]) {
                           var4[0] = var21;
                           var2.set(var3);
                        }
                     } else {
                        var17.forAllBoxes((var8x, var10x, var12x, var14x, var16x, var18x) -> {
                           TEMP_BOX.setUnchecked(var8x + var18, var10x + var19, var12x + var20, var14x + var18, var16x + var19, var18x + var20);
                           var3.zero();
                           this.resolveAABBAABBCollision(var1, TEMP_BOX, var3);
                           double var20x = var3.lengthSquared();
                           if (var20x > var4[0]) {
                              var4[0] = var20x;
                              var2.set(var3);
                           }
                        });
                     }
                  }
               }
            }
         }
      }

      return var2;
   }

   private void resolveAABBAABBCollision(BoundingBox3d var1, BoundingBox3dc var2, Vector3d var3) {
      double var4 = var2.maxX() - var1.minX();
      double var6 = var1.maxX() - var2.minX();
      if (!(var4 <= 0.0) && !(var6 <= 0.0)) {
         double var8 = var2.maxY() - var1.minY();
         double var10 = var1.maxY() - var2.minY();
         if (!(var8 <= 0.0) && !(var10 <= 0.0)) {
            double var12 = var2.maxZ() - var1.minZ();
            double var14 = var1.maxZ() - var2.minZ();
            if (!(var12 <= 0.0) && !(var14 <= 0.0)) {
               double var16 = var4;
               var3.set(var4, 0.0, 0.0);
               if (var6 < var4) {
                  var16 = var6;
                  var3.set(-var6, 0.0, 0.0);
               }

               if (var8 < var16) {
                  var16 = var8;
                  var3.set(0.0, var8, 0.0);
               }

               if (var10 < var16) {
                  var16 = var10;
                  var3.set(0.0, -var10, 0.0);
               }

               if (var12 < var16) {
                  var16 = var12;
                  var3.set(0.0, 0.0, var12);
               }

               if (var14 < var16) {
                  var3.set(0.0, 0.0, -var14);
               }
            }
         }
      }
   }

   @Inject(
      method = {"getLightCoords"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$checkSubLevelLightColor(float var1, CallbackInfoReturnable<Integer> var2) {
      double var3 = this.x;
      double var5 = this.y;
      double var7 = this.z;
      BlockPos var9 = BlockPos.containing(var3, var5, var7);
      boolean var10 = this.level.hasChunkAt(var9);
      if (var10) {
         BlockState var11 = this.level.getBlockState(var9);
         if (var11.emissiveRendering()) {
            var2.setReturnValue(15728880);
         } else {
            ClientSubLevel var12 = this.sable$trackingSubLevel;
            int var13;
            int var14;
            if (var12 != null) {
               var14 = this.level.getBrightness(LightLayer.BLOCK, var9);
               var13 = this.level.getBrightness(LightLayer.SKY, var9);
               Vector3d var26 = new Vector3d();
               MutableBlockPos var29 = new MutableBlockPos();
               MutableBlockPos var31 = new MutableBlockPos();
               Pose3d var33 = var12.logicalPose();
               var33.transformPositionInverse(var26.set(var3, var5, var7));
               var29.set(var26.x, var26.y, var26.z);
               var14 = Math.max(var14, var12.getLevel().getBrightness(LightLayer.BLOCK, var29));
               var31.setWithOffset(var29, 0, 1, 0);
               ClientLevelPlot var34 = var12.getPlot();
               boolean var35 = false;

               while (var31.getY() >= var34.getBoundingBox().minY()) {
                  if (!this.level.getBlockState(var31).isAir()) {
                     var35 = true;
                     break;
                  }

                  var31.move(0, -1, 0);
               }

               if (var35) {
                  var13 = Math.min(var13, var12.scaleSkyLight(this.level.getBrightness(LightLayer.SKY, var29)));
               }
            } else {
               List<ClientSubLevel> var15 = this.sable$nearbySubLevels;
               if (var15 == null) {
                  List<ClientSubLevel> var16 = new ObjectArrayList<ClientSubLevel>(6);

                  for (SubLevel var18 : Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(var9).expand(8.0))) {
                     var16.add((ClientSubLevel)var18);
                  }

                  var15 = var16;
                  this.sable$nearbySubLevels = var16;
               }

               if (var15.isEmpty()) {
                  return;
               }

               var14 = this.level.getBrightness(LightLayer.BLOCK, var9);
               var13 = this.level.getBrightness(LightLayer.SKY, var9);
               Vector3d var28 = new Vector3d();
               MutableBlockPos var30 = new MutableBlockPos();
               MutableBlockPos var32 = new MutableBlockPos();
               BoundingBox3d var19 = new BoundingBox3d(var9).expand(0.5);

               for (ClientSubLevel var21 : var15) {
                  if (var21.boundingBox().intersects(var19)) {
                     Pose3d var22 = var21.logicalPose();
                     var22.transformPositionInverse(var28.set(var3, var5, var7));
                     var30.set(var28.x, var28.y, var28.z);
                     var14 = Math.max(var14, var21.getLevel().getBrightness(LightLayer.BLOCK, var30));
                     var32.setWithOffset(var30, 0, 1, 0);
                     ClientLevelPlot var23 = var21.getPlot();
                     boolean var24 = false;

                     while (var32.getY() >= var23.getBoundingBox().minY()) {
                        if (!this.level.getBlockState(var32).isAir()) {
                           var24 = true;
                           break;
                        }

                        var32.move(0, -1, 0);
                     }

                     if (var24) {
                        var13 = Math.min(var13, var21.scaleSkyLight(this.level.getBrightness(LightLayer.SKY, var30)));
                     }
                  }
               }
            }

            int var27 = var11.getLightEmission();
            if (var14 < var27) {
               var14 = var27;
            }

            var2.setReturnValue(LightCoordsUtil.pack(var14, var13));
         }
      }
   }
}
