package dev.ryanhcode.sable.mixin.entity.entity_sublevel_collision;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.index.SableTags;
import dev.ryanhcode.sable.mixinterface.EntityExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.EntityMovementExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LivingEntityMovementExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import java.util.Iterator;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {Entity.class},
   priority = 1100
)
public abstract class EntityMixin implements EntityMovementExtension {
   @Shadow
   public boolean horizontalCollision;
   @Shadow
   public boolean verticalCollision;
   @Shadow
   public boolean verticalCollisionBelow;
   @Shadow
   public boolean minorHorizontalCollision;
   @Unique
   private SubLevel sable$trackingSubLevel = null;
   @Unique
   private UUID sable$lastTrackingSubLevelId = null;
   @Shadow
   private Level level;
   @Shadow
   private Vec3 position;
   @Shadow
   @Nullable
   private BlockState inBlockState;
   @Shadow
   private BlockPos blockPosition;
   @Unique
   private SubLevelEntityCollision.CollisionInfo sable$collisionInfo = null;
   @Unique
   private BlockPos sable$inBlockStatePos = BlockPos.ZERO;

   @Shadow
   protected abstract Vec3 collide(Vec3 var1);

   @Shadow
   protected abstract boolean isHorizontalCollisionMinor(Vec3 var1);

   @Shadow
   public abstract Level level();

   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   public void sable$moveInject(Entity var1, boolean var2, boolean var3, Vec3 var4, Operation<Void> var5) {
      this.horizontalCollision = this.sable$collisionInfo.horizontalCollision;
      this.verticalCollision = this.sable$collisionInfo.verticalCollision;
      this.verticalCollisionBelow = this.sable$collisionInfo.verticalCollisionBelow;
      this.minorHorizontalCollision = this.sable$collisionInfo.minorHorizontalCollision;
      var5.call(var1, this.verticalCollisionBelow, this.horizontalCollision, var4);
   }

   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/Block;updateEntityMovementAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"
      )}
   )
   public void sable$subLevelBounciness(Block var1, BlockGetter var2, Entity var3, Operation<Void> var4) {
      if (this.verticalCollision) {
         SubLevel var5 = this.sable$trackingSubLevel;
         if (var5 != null) {
            // 26.1.2 没有 getBlockBounciness，等价语义：用 sub-level 内玩家脚下的方块执行落块处理；
            // 注意：不要临时移动实体坐标（pushEntityLocalIfNeeded），否则会干扰跳跃/移动速度。
            BlockPos var8 = BlockPos.containing(var3.getX(), var3.getY() - 0.2, var3.getZ());
            BlockState var9 = var5.getLevel().getBlockState(var8);
            if (!var9.isAir() && !var9.getCollisionShape(var5.getLevel(), var8).isEmpty()) {
               var9.getBlock().updateEntityMovementAfterFallOn(var5.getLevel(), var3);
               return;
            }
         }
      }

      var4.call(var1, var2, var3);
   }

   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"
      )}
   )
   private void sable$checkFallDamage(Entity var1, double var2, boolean var4, BlockState var5, BlockPos var6, Operation<Void> var7) {
      SubLevel var8 = this.sable$trackingSubLevel;
      if (var8 != null && var4 && this.verticalCollision) {
         boolean var9 = SubLevelHelper.pushEntityLocalIfNeeded(var8, var1);

         try {
            BlockPos var10 = BlockPos.containing(var1.getX(), var1.getY() - 0.2, var1.getZ());
            BlockState var11 = var8.getLevel().getBlockState(var10);
            var7.call(var1, var2, var4, var11, var10);
         } finally {
            SubLevelHelper.popEntityLocalIfNeeded(var8, var1, var9);
         }
      } else {
         var7.call(var1, var2, var4, var5, var6);
      }
   }

   @WrapOperation(
      method = {"applyEffectsFromBlocks(Ljava/util/List;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"
      )}
   )
   private void sable$stepOn(Block var1, Level var2, BlockPos var3, BlockState var4, Entity var5, Operation<Void> var6) {
      SubLevel var7 = this.sable$trackingSubLevel;
      if (var7 == null) {
         var6.call(var1, var2, var3, var4, var5);
      } else {
         boolean var8 = SubLevelHelper.pushEntityLocalIfNeeded(var7, var5);

         try {
            BlockPos var9 = BlockPos.containing(var5.getX(), var5.getY() - 0.2, var5.getZ());
            BlockState var10 = var7.getLevel().getBlockState(var9);
            var6.call(var10.getBlock(), var2, var9, var10, var5);
         } finally {
            SubLevelHelper.popEntityLocalIfNeeded(var7, var5, var8);
         }
      }
   }

   @Redirect(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;collide(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   public Vec3 sable$collideRedirect(Entity var1, Vec3 var2) {
      Entity var3 = (Entity)(Object)this;
      Vec3 var4 = Vec3.ZERO;
      if (var3 instanceof LivingEntity var5) {
         var4 = JOMLConversion.toMojang(((LivingEntityMovementExtension)var5).sable$getInheritedVelocity());
      }

      SubLevel var11 = this.sable$trackingSubLevel;
      Vec3 var6 = this.getDeltaMovement();
      this.sable$collisionInfo = SubLevelEntityCollision.collide(var1, var2, var4, ((LevelExtension)this.level).sable$getJOMLSink());
      this.sable$collisionInfo.preTrackingSubLevel = var11;
      this.sable$collisionInfo.preDeltaMovement = var6;
      if (this.sable$collisionInfo.trackingSubLevel != null) {
         if (this.sable$collisionInfo.verticalCollisionBelow) {
            this.sable$trackingSubLevel = this.sable$collisionInfo.trackingSubLevel;
         }
      } else if (!(var1 instanceof ServerPlayer)) {
         this.sable$trackingSubLevel = null;
      }

      Vec3 var7 = this.sable$collisionInfo.motion;
      Vec3 var8 = this.collide(var7);
      boolean var9 = !Mth.equal(var7.x, var8.x);
      boolean var10 = !Mth.equal(var7.z, var8.z);
      this.sable$collisionInfo.horizontalCollision |= var9 || var10;
      if (var7.y != var8.y) {
         this.sable$trackingSubLevel = null;
      }

      this.sable$collisionInfo.verticalCollision = this.sable$collisionInfo.verticalCollision | var7.y != var8.y;
      this.sable$collisionInfo.verticalCollisionBelow = this.sable$collisionInfo.verticalCollisionBelow
         | (this.sable$collisionInfo.verticalCollision && var2.y < 0.0);
      if (this.horizontalCollision) {
         this.sable$collisionInfo.minorHorizontalCollision = this.isHorizontalCollisionMinor(var8);
      }

      if (this.sable$trackingSubLevel != null && this.sable$trackingSubLevel.isRemoved()) {
         this.sable$trackingSubLevel = null;
      }

      return var8;
   }

   @Inject(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("TAIL")}
   )
   public void sable$moveInject(MoverType var1, Vec3 var2, CallbackInfo var3) {
      if (this.sable$collisionInfo != null) {
         this.horizontalCollision = this.horizontalCollision | this.sable$collisionInfo.subLevelHorizontalCollision;
      }

      if (!((Object)this instanceof LivingEntity)
         && this.sable$collisionInfo != null
         && this.sable$collisionInfo.inheritedMotion != null
         && this.sable$collisionInfo.inheritedMotion.lengthSqr() > Math.pow(1.0E-6, 2.0)) {
         this.setPos(this.position.add(((EntityExtension)this).sable$vanillaCollide(this.sable$collisionInfo.inheritedMotion)));
      }
   }

   @Shadow
   public abstract void setPos(Vec3 var1);

   @Shadow
   @Nullable
   public abstract Entity getVehicle();

   @Shadow
   public abstract Vec3 getDeltaMovement();

   @Shadow
   public abstract AABB getBoundingBox();

   @Shadow
   public abstract void remove(RemovalReason var1);

   @Shadow
   public abstract EntityType<?> getType();

   @Shadow
   public abstract void kill(ServerLevel var1);

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   public void sable$tickInject(CallbackInfo var1) {
      ActiveSableCompanion var2 = Sable.HELPER;
      Entity var3 = this.getVehicle();
      SubLevel var4 = var2.getContaining((Entity)(Object)this);
      if (var4 != null) {
         this.sable$trackingSubLevel = null;
      } else if (var3 != null) {
         SubLevel var5 = var2.getContaining(var3);
         if (var5 != null) {
            this.sable$trackingSubLevel = var5;
         } else {
            this.sable$trackingSubLevel = Sable.HELPER.getTrackingSubLevel(var3);
         }
      }

      if (this.sable$trackingSubLevel != null && this.sable$trackingSubLevel.isRemoved()) {
         this.sable$trackingSubLevel = null;
      }

      if (var4 != null
         && !this.getBoundingBox().intersects(var4.getPlot().getBoundingBox().toAABB().inflate(1.0))
         && ((Entity)(Object)this).is(SableTags.DESTROY_WHEN_LEAVING_PLOT)
         && this.level() instanceof ServerLevel var7) {
         this.kill(var7);
      }
   }

   @Override
   public BlockPos sable$getInBlockStatePos() {
      return this.sable$inBlockStatePos;
   }

   @Overwrite
   public BlockState getInBlockState() {
      Level var1 = this.level();
      if (this.inBlockState == null || this.sable$trackingSubLevel != null) {
         this.inBlockState = var1.getBlockState(this.blockPosition);
         this.sable$inBlockStatePos = this.blockPosition;
         Iterable var2 = Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(this.blockPosition));
         Iterator var3 = var2.iterator();

         while (this.inBlockState.isAir() && var3.hasNext()) {
            SubLevel var4 = (SubLevel)var3.next();
            BlockPos var5 = BlockPos.containing(var4.logicalPose().transformPositionInverse(this.position.add(0.0, 0.001, 0.0)));
            this.inBlockState = var1.getBlockState(var5);
            this.sable$inBlockStatePos = var5;
         }
      }

      return this.inBlockState;
   }

   @Override
   public SubLevel sable$getTrackingSubLevel() {
      return this.sable$trackingSubLevel;
   }

   @Override
   public UUID sable$getLastTrackingSubLevelID() {
      return this.sable$lastTrackingSubLevelId;
   }

   @Override
   public void sable$setTrackingSubLevel(SubLevel var1) {
      this.sable$trackingSubLevel = var1;
      if (var1 != null) {
         this.sable$setLastTrackingSubLevelID(var1.getUniqueId());
      }
   }

   @Override
   public void sable$setLastTrackingSubLevelID(UUID var1) {
      this.sable$lastTrackingSubLevelId = var1;
   }

   @Override
   public SubLevelEntityCollision.CollisionInfo sable$getCollisionInfo() {
      return this.sable$collisionInfo;
   }

   @Override
   public void sable$setPosField(Vec3 var1) {
      this.position = var1;
   }
}
