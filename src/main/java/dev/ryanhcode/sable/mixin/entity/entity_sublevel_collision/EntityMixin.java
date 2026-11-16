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

/**
 * 实体在 sub-level（物理结构）内的碰撞与移动。
 *
 * <p><b>关于 sub-level 上的落地 / 弹跳手感（26.1.2）</b>
 *
 * <p>26.1.2 <b>没有</b>任何"按方块 bounciness 弹跳"的机制，javap 逐字节码确认如下：
 *
 * <ol>
 *   <li><b>{@code Entity} 里不存在 bounciness 相关方法</b>。26.2 有
 *       {@code Entity.getBlockBounciness(Block)}（private）、{@code getEntityBounciness()}、
 *       以及 {@code restituteMovementAfterCollisions(BlockState, boolean, boolean, Vec3)}，
 *       26.1.2 全部<b>已删除</b>。26.1.2 的 {@code Entity} 与弹跳相关的成员只剩
 *       {@code getBlockJumpFactor()}（读 {@code Block.getJumpFactor()}）和
 *       {@code isSuppressingBounce()}（= {@code isShiftKeyDown()}）。</li>
 *   <li><b>{@code BlockBehaviour} 里没有任何 bounciness 属性</b>。
 *       26.1.2 的 {@code Block} 只有 {@code getFriction()}、{@code getSpeedFactor()}、
 *       {@code getJumpFactor()}，<b>没有</b> {@code getBounceRestitution()}（26.2 才有），
 *       也没有 {@code BlockTags.SUPPRESSES_BOUNCE} 这类标签。</li>
 *   <li><b>26.1.2 的落地处理走 {@code Block.updateEntityMovementAfterFallOn}</b>，
 *       其基类实现只把 Y 方向速度清零。javap 原文：
 *       <pre>
 *       public void updateEntityMovementAfterFallOn(BlockGetter, Entity);
 *         0: aload_2
 *         1: invokevirtual #  // Method Entity.getDeltaMovement:()Lnet/minecraft/world/phys/Vec3;
 *         5: dconst_1
 *         6: dconst_0
 *         7: dconst_1
 *         8: invokevirtual #  // Method Vec3.multiply(DDD)Lnet/minecraft/world/phys/Vec3;
 *        11: invokevirtual #  // Method Entity.setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V
 *       </pre>
 *       也就是 {@code entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0))}。</li>
 *   <li><b>调用点在 {@code Entity.move(MoverType, Vec3)} 内部</b>，位于 {@code checkFallDamage} 之后：
 *       <pre>
 *       601: iload  (deltaY != savedY)
 *       ...
 *       634: invokevirtual #  // Method canSimulateMovement:()Z
 *       637: ifeq  ...
 *       641: invokevirtual #  // Method net/minecraft/world/level/block/Block.getBlockState(...)
 *       ...
 *       655: invokevirtual #  // Method net/minecraft/world/level/block/Block.updateEntityMovementAfterFallOn:(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V
 *       </pre>
 *       覆盖者只有 {@code Entity}、{@code Block}、{@code BedBlock}、{@code SlimeBlock} 四个。
 *       {@code SlimeBlock} / {@code BedBlock} 用 {@code entity.isSuppressingBounce()}
 *       （按住 Shift）决定是否 {@code bounceUp}，这属于"潜影盒/史莱姆块"的<b>特例</b>，
 *       不是通用的 bounciness 系统。</li>
 * </ol>
 *
 * <p><b>结论与做法</b>：26.1.2 没有等价的 bounciness 机制，所以<b>不能</b>再用 sub-level 内的方块
 * 去覆盖落块处理 —— 那既没有语义等价物，又会引入"临时坐标平移"从而干扰跳跃/移动速度。
 * 因此本 mixin <b>完全不拦截</b> {@code Block.updateEntityMovementAfterFallOn}，
 * 让 sub-level 上的落地走<b>原版</b>语义（Y 速度清零），与主世界手感一致。
 *
 * <p><b>例外</b>：{@code entity_rotations_and_riding.BlockMixin} 的 @Redirect 会对同一方法做
 * <b>旋转校正</b>（仅当实体处于带旋转的 sub-level 时，把 Y 速度清零的方向改为 sub-level 局部 up；
 * {@code getCustomEntityOrientation} 为 null 时原样回落原版语义）。这不属于上述被否定的
 * "sub-level 内方块覆盖落块 / 临时坐标平移"方案，与 1.21.1 时代两 mixin 共存的行为一致。
 *
 * <p>说明：{@link #sable$checkFallDamage} 与 {@link #sable$stepOn} 里的
 * {@code SubLevelHelper.pushEntityLocalIfNeeded} 是 <b>26.2 基线就存在</b>的独立功能
 * （让坠落伤害 / 踩踏效果按 sub-level <i>内</i>的方块计算，而不是父世界坐标处的空气），
 * 与弹跳无关；且 {@code pushEntityLocal} / {@code popEntityLocal} 对速度做的是
 * {@code transformNormalInverse} / {@code transformNormal} 这对<b>互逆</b>变换
 * （见 {@code Pose3dc#transformNormal} / {@code transformNormalInverse}），
 * 因此不改变实体的实际移动速度。
 */
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
   protected abstract Vec3 collide(Vec3 movement);

   @Shadow
   protected abstract boolean isHorizontalCollisionMinor(Vec3 movement);

   @Shadow
   public abstract Level level();

   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;setOnGroundWithMovement(ZZLnet/minecraft/world/phys/Vec3;)V"
      )}
   )
   public void sable$moveInject(Entity instance, boolean onGround, boolean horizontalCollision, Vec3 movement, Operation<Void> operation) {
      this.horizontalCollision = this.sable$collisionInfo.horizontalCollision;
      this.verticalCollision = this.sable$collisionInfo.verticalCollision;
      this.verticalCollisionBelow = this.sable$collisionInfo.verticalCollisionBelow;
      this.minorHorizontalCollision = this.sable$collisionInfo.minorHorizontalCollision;
      operation.call(instance, this.verticalCollisionBelow, this.horizontalCollision, movement);
   }

   /**
    * 让"坠落伤害 / 落地音效"按 <b>sub-level 内</b>脚下的方块计算，
    * 而不是父世界坐标处的空气。
    *
    * <p>26.1.2 的 {@code Entity.checkFallDamage(double, boolean, BlockState, BlockPos)} 字节码：
    * <pre>
    * 38: aload  4        // BlockState
    * 40: invokevirtual # // Method BlockState.getBlock:()Lnet/minecraft/world/level/block/Block;
    * 43: aload_0
    * 44: invokevirtual # // Method Entity.level:()Lnet/minecraft/world/level/Level;
    * 47: aload  4        // BlockState
    * 49: aload  5        // BlockPos
    * 51: aload_0
    * 53: invokevirtual # // Method Block.fallOn:(Level;BlockState;BlockPos;Entity;D)V
    * </pre>
    * 即坠落时用 {@code blockState} 所在位置的方块执行 {@code fallOn}。
    *
    * <p>sub-level 内的实体坐标在父世界坐标系里是"plot 局部坐标"，
    * 直接取父世界的方块状态会取到空气，所以这里把实体临时投影到 sub-level 局部坐标系
    * 再取方块状态。
    *
    * <p>注意：这<b>不是</b>弹跳处理。26.1.2 没有 bounciness 机制（见类注释），
    * 本 mixin 不拦截 {@code Block.updateEntityMovementAfterFallOn}。
    * 另外 {@code pushEntityLocal} / {@code popEntityLocal} 对速度做的是
    * {@code transformNormalInverse} / {@code transformNormal} 这对<b>互逆</b>变换，
    * 所以不改变实体的实际移动速度。
    */
   @WrapOperation(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/entity/Entity;checkFallDamage(DZLnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V"
      )}
   )
   private void sable$checkFallDamage(Entity instance, double y, boolean onGround, BlockState state, BlockPos pos, Operation<Void> operation) {
      SubLevel subLevel = this.sable$trackingSubLevel;
      if (subLevel != null && onGround && this.verticalCollision) {
         boolean pushed = SubLevelHelper.pushEntityLocalIfNeeded(subLevel, instance);

         try {
            BlockPos localBlockPos = BlockPos.containing(instance.getX(), instance.getY() - 0.2, instance.getZ());
            BlockState localBlockState = subLevel.getLevel().getBlockState(localBlockPos);
            operation.call(instance, y, onGround, localBlockState, localBlockPos);
         } finally {
            SubLevelHelper.popEntityLocalIfNeeded(subLevel, instance, pushed);
         }
      } else {
         operation.call(instance, y, onGround, state, pos);
      }
   }

   @WrapOperation(
      method = {"applyEffectsFromBlocks(Ljava/util/List;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"
      )}
   )
   private void sable$stepOn(Block block, Level level, BlockPos pos, BlockState state, Entity entity, Operation<Void> operation) {
      SubLevel subLevel = this.sable$trackingSubLevel;
      if (subLevel == null) {
         operation.call(block, level, pos, state, entity);
      } else {
         boolean pushed = SubLevelHelper.pushEntityLocalIfNeeded(subLevel, entity);

         try {
            BlockPos localBlockPos = BlockPos.containing(entity.getX(), entity.getY() - 0.2, entity.getZ());
            BlockState localBlockState = subLevel.getLevel().getBlockState(localBlockPos);
            operation.call(localBlockState.getBlock(), level, localBlockPos, localBlockState, entity);
         } finally {
            SubLevelHelper.popEntityLocalIfNeeded(subLevel, entity, pushed);
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
   public Vec3 sable$collideRedirect(Entity entity, Vec3 collisionMotion) {
      Entity self = (Entity)(Object)this;
      Vec3 velocity = Vec3.ZERO;
      if (self instanceof LivingEntity livingEntity) {
         velocity = JOMLConversion.toMojang(((LivingEntityMovementExtension)livingEntity).sable$getInheritedVelocity());
      }

      SubLevel preTrackingSubLevel = this.sable$trackingSubLevel;
      Vec3 preDeltaMovement = this.getDeltaMovement();
      this.sable$collisionInfo = SubLevelEntityCollision.collide(entity, collisionMotion, velocity, ((LevelExtension)this.level).sable$getJOMLSink());
      this.sable$collisionInfo.preTrackingSubLevel = preTrackingSubLevel;
      this.sable$collisionInfo.preDeltaMovement = preDeltaMovement;
      if (this.sable$collisionInfo.trackingSubLevel != null) {
         if (this.sable$collisionInfo.verticalCollisionBelow) {
            this.sable$trackingSubLevel = this.sable$collisionInfo.trackingSubLevel;
         }
      } else if (!(entity instanceof ServerPlayer)) {
         this.sable$trackingSubLevel = null;
      }

      Vec3 beforeVanillaCollision = this.sable$collisionInfo.motion;
      Vec3 afterVanillaCollision = this.collide(beforeVanillaCollision);
      boolean xCollision = !Mth.equal(beforeVanillaCollision.x, afterVanillaCollision.x);
      boolean zCollision = !Mth.equal(beforeVanillaCollision.z, afterVanillaCollision.z);
      this.sable$collisionInfo.horizontalCollision |= xCollision || zCollision;
      if (beforeVanillaCollision.y != afterVanillaCollision.y) {
         this.sable$trackingSubLevel = null;
      }

      this.sable$collisionInfo.verticalCollision = this.sable$collisionInfo.verticalCollision | beforeVanillaCollision.y != afterVanillaCollision.y;
      this.sable$collisionInfo.verticalCollisionBelow = this.sable$collisionInfo.verticalCollisionBelow
         | (this.sable$collisionInfo.verticalCollision && collisionMotion.y < 0.0);
      if (this.horizontalCollision) {
         this.sable$collisionInfo.minorHorizontalCollision = this.isHorizontalCollisionMinor(afterVanillaCollision);
      }

      if (this.sable$trackingSubLevel != null && this.sable$trackingSubLevel.isRemoved()) {
         this.sable$trackingSubLevel = null;
      }

      return afterVanillaCollision;
   }

   @Inject(
      method = {"move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"},
      at = {@At("TAIL")}
   )
   public void sable$moveInject(MoverType moverType, Vec3 movement, CallbackInfo ci) {
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
   public abstract void setPos(Vec3 pos);

   @Shadow
   @Nullable
   public abstract Entity getVehicle();

   @Shadow
   public abstract Vec3 getDeltaMovement();

   @Shadow
   public abstract AABB getBoundingBox();

   @Shadow
   public abstract void remove(RemovalReason removalReason);

   @Shadow
   public abstract EntityType<?> getType();

   @Shadow
   public abstract void kill(ServerLevel serverLevel);

   @Inject(
      method = {"tick"},
      at = {@At("TAIL")}
   )
   public void sable$tickInject(CallbackInfo ci) {
      ActiveSableCompanion helper = Sable.HELPER;
      Entity vehicle = this.getVehicle();
      SubLevel containingSubLevel = helper.getContaining((Entity)(Object)this);
      if (containingSubLevel != null) {
         this.sable$trackingSubLevel = null;
      } else if (vehicle != null) {
         SubLevel vehicleSubLevel = helper.getContaining(vehicle);
         if (vehicleSubLevel != null) {
            this.sable$trackingSubLevel = vehicleSubLevel;
         } else {
            this.sable$trackingSubLevel = Sable.HELPER.getTrackingSubLevel(vehicle);
         }
      }

      if (this.sable$trackingSubLevel != null && this.sable$trackingSubLevel.isRemoved()) {
         this.sable$trackingSubLevel = null;
      }

      if (containingSubLevel != null
         && !this.getBoundingBox().intersects(containingSubLevel.getPlot().getBoundingBox().toAABB().inflate(1.0))
         && ((Entity)(Object)this).is(SableTags.DESTROY_WHEN_LEAVING_PLOT)
         && this.level() instanceof ServerLevel serverLevel) {
         this.kill(serverLevel);
      }
   }

   @Override
   public BlockPos sable$getInBlockStatePos() {
      return this.sable$inBlockStatePos;
   }

   @Overwrite
   public BlockState getInBlockState() {
      Level level = this.level();
      if (this.inBlockState == null || this.sable$trackingSubLevel != null) {
         this.inBlockState = level.getBlockState(this.blockPosition);
         this.sable$inBlockStatePos = this.blockPosition;
         Iterable intersecting = Sable.HELPER.getAllIntersecting(this.level, new BoundingBox3d(this.blockPosition));
         Iterator iter = intersecting.iterator();

         while (this.inBlockState.isAir() && iter.hasNext()) {
            SubLevel subLevel = (SubLevel)iter.next();
            BlockPos localBlockPos = BlockPos.containing(subLevel.logicalPose().transformPositionInverse(this.position.add(0.0, 0.001, 0.0)));
            this.inBlockState = level.getBlockState(localBlockPos);
            this.sable$inBlockStatePos = localBlockPos;
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
   public void sable$setTrackingSubLevel(SubLevel subLevel) {
      this.sable$trackingSubLevel = subLevel;
      if (subLevel != null) {
         this.sable$setLastTrackingSubLevelID(subLevel.getUniqueId());
      }
   }

   @Override
   public void sable$setLastTrackingSubLevelID(UUID uuid) {
      this.sable$lastTrackingSubLevelId = uuid;
   }

   @Override
   public SubLevelEntityCollision.CollisionInfo sable$getCollisionInfo() {
      return this.sable$collisionInfo;
   }

   @Override
   public void sable$setPosField(Vec3 newPosition) {
      this.position = newPosition;
   }
}
