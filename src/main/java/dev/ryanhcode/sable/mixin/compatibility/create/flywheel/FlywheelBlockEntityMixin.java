/*
 * Decompiled with CFR 0.152.
 * 
 * Sable-Fly 26.3 移植说明：
 * Create 26.3 已将 BlockEntity 的 NBT 序列化从 CompoundTag + HolderLookup.Provider
 * 迁移到 ValueOutput / ValueInput（见 KineticBlockEntity.write(ValueOutput, boolean)
 * 与 read(ValueInput, boolean)）。此处 @Inject handler 的形参必须与目标方法完全一致，
 * 否则 require = 0 也无法抑制 InvalidIsjectionException（Invalid descriptor）。
 * 因此 write / read 两个 handler 分别改为 ValueOutput / ValueInput，业务逻辑不变。
 */
package dev.ryanhcode.sable.mixin.compatibility.create.flywheel;

import com.zurrtum.create.content.kinetics.base.IRotate;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;
import com.zurrtum.create.content.kinetics.flywheel.FlywheelBlockEntity;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelReactionWheel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={FlywheelBlockEntity.class})
public abstract class FlywheelBlockEntityMixin
extends KineticBlockEntity
implements BlockEntitySubLevelReactionWheel {
    @Unique
    float sable$smoothedSpeed = 0.0f;

    public FlywheelBlockEntityMixin(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
        super(arg, arg2, arg3);
    }

    @Inject(require = 0, method={"tick"}, at={@At(value="HEAD")})
    public void sable$tick(CallbackInfo ci) {
        this.sable$smoothedSpeed += (this.speed - this.sable$smoothedSpeed) / 32.0f;
    }

    @Inject(require = 0, method={"write"}, at={@At(value="TAIL")})
    public void sable$write(ValueOutput output, boolean clientPacket, CallbackInfo ci) {
        output.putFloat("SmoothedSpeed", this.sable$smoothedSpeed);
    }

    @Inject(require = 0, method={"read"}, at={@At(value="TAIL")})
    public void sable$read(ValueInput input, boolean clientPacket, CallbackInfo ci) {
        this.sable$smoothedSpeed = input.getFloatOr("SmoothedSpeed", 0.0f);
    }

    @Override
    public void sable$getAngularVelocity(Vector3d v) {
        Direction.Axis axis = ((IRotate)this.getBlockState().getBlock()).getRotationAxis(this.getBlockState());
        Direction dir = Direction.get((Direction.AxisDirection)Direction.AxisDirection.NEGATIVE, (Direction.Axis)axis);
        float angularSpeed = this.sable$smoothedSpeed * ((float)Math.PI * 2) / 60.0f;
        v.set((double)dir.getStepX(), (double)dir.getStepY(), (double)dir.getStepZ()).mul((double)angularSpeed);
    }
}