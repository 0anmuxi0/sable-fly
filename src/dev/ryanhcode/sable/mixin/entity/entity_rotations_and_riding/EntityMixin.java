package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.serialization.Codec;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_riding_sub_level_vehicle.EntityRidingSubLevelVehicleHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.tracking_points.SubLevelTrackingPointSavedData;
import java.util.UUID;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Position;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaterniondc;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Entity.class})
public abstract class EntityMixin {
    @Shadow
    private Level level;
    @Shadow
    @Nullable
    private Entity vehicle;
    @Shadow
    private Vec3 position;

    @Shadow
    public abstract void setPos(Vec3 var1);

    @Shadow
    public abstract boolean hasPassenger(Entity var1);

    @Shadow
    public abstract Vec3 position();

    @Shadow
    public abstract double getX();

    @Shadow
    public abstract double getY();

    @Shadow
    public abstract double getZ();

    @Shadow
    public abstract Level level();

    @Shadow
    @Nullable
    public abstract Entity getVehicle();

    @Shadow
    public abstract Vec3 getLookAngle();

    @Shadow
    public abstract void lookAt(EntityAnchorArgument.Anchor var1, Vec3 var2);

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract float getYRot();

    @Shadow
    protected static Vec3 getInputVector(Vec3 vec3, float f, float g) {
        return null;
    }

    @Shadow
    public abstract void setDeltaMovement(Vec3 var1);

    @Shadow
    public abstract Vec3 getDeltaMovement();
// 26.2 folded applyMovementEmissionAndPlaySound back into aiStep and switched the movement
    // threshold to horizontalDistanceSqr (9.0E-6 == 0.003^2); retarget so sub-level-carried entities
    // do not count as walking.
    @WrapOperation(method={"aiStep"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;horizontalDistanceSqr()D")})
    private double sable$fixWalkDistance(Vec3 vec, Operation<Double> original) {
        Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation((Entity)this, 1.0f);
        if (orientation == null) {
            return (Double)original.call(new Object[]{vec});
        }
        return (Double)original.call(new Object[]{JOMLConversion.toMojang((Vector3dc)orientation.transformInverse(JOMLConversion.toJOML((Position)vec)))});
    }

    @Inject(method={"moveRelative"}, at={@At(value="HEAD")}, cancellable=true)
    public void moveRelative(float f, Vec3 vec3, CallbackInfo ci) {
        Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation((Entity)this, 1.0f);
        if (orientation == null) {
            return;
        }
        Vec3 inputVector = EntityMixin.getInputVector(vec3, f, this.getYRot());
        Vec3 impulse = JOMLConversion.toMojang((Vector3dc)orientation.transform(JOMLConversion.toJOML((Position)inputVector)));
        this.setDeltaMovement(this.getDeltaMovement().add(impulse));
        ci.cancel();
    }

    @Inject(method={"rideTick"}, at={@At(value="TAIL")})
    public void sable$onRidingTick(CallbackInfo ci) {
        if (this.vehicle == null) {
            return;
        }
        ActiveSableCompanion helper = Sable.HELPER;
        SubLevel vehicleSubLevel = helper.getContaining(this.vehicle);
        if (vehicleSubLevel == null) {
            return;
        }
        if (helper.getContaining(this.level, (Position)this.position) != vehicleSubLevel) {
            return;
        }
        Vec3 pos = EntityRidingSubLevelVehicleHelper.kickRidingEntity((Entity)this, vehicleSubLevel);
        this.setPos(pos);
    }

    @Inject(method={"positionRider(Lnet/minecraft/world/entity/Entity;)V"}, at={@At(value="TAIL")})
    public void sable$onPositionRider(Entity entity, CallbackInfo ci) {
        if (!this.hasPassenger(entity)) {
            return;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(this.level, (Position)entity.position());
        if (subLevel == null) {
            return;
        }
        Vec3 pos = EntityRidingSubLevelVehicleHelper.kickRidingEntity(entity, subLevel);
        entity.setPos(pos);
    }

    @WrapOperation(method={"saveWithoutId"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/storage/ValueOutput;store(Ljava/lang/String;Lcom/mojang/serialization/Codec;Ljava/lang/Object;)V", ordinal=0)})
    private void sable$fixPassengerSaving(ValueOutput instance, String string, Codec<?> codec, Object value, Operation<Void> original) {
        if (!EntitySubLevelUtil.shouldKick((Entity)this)) {
            original.call(new Object[]{instance, string, codec, value});
            return;
        }
        SubLevel subLevel = Sable.HELPER.getContaining(this.vehicle);
        if (subLevel != null) {
            EntityMixin entityMixin = this;
            if (entityMixin instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)entityMixin;
                SubLevelTrackingPointSavedData data = SubLevelTrackingPointSavedData.getOrLoad((ServerLevel)this.level());
                UUID loginPointUUID = data.generateTrackingPoint(serverPlayer, (ServerSubLevel)subLevel);
                if (loginPointUUID != null) {
                    instance.store("LoginPoint", UUIDUtil.CODEC, (Object)loginPointUUID);
                }
            }
            original.call(new Object[]{instance, string, Vec3.CODEC, this.position()});
            return;
        }
        original.call(new Object[]{instance, string, codec, value});
    }
}

