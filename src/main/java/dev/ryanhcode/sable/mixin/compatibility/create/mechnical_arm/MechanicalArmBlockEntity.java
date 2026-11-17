/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.content.kinetics.mechanicalArm.ArmBlockEntity
 *  com.zurrtum.create.foundation.blockEntity.SmartBlockEntity
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.ryanhcode.sable.mixin.compatibility.create.mechnical_arm;

import com.zurrtum.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ArmBlockEntity.class})
public abstract class MechanicalArmBlockEntity
extends SmartBlockEntity {
    public MechanicalArmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Inject(require = 0, method={"isAreaActuallyLoaded"}, at={@At(value="HEAD")}, cancellable=true)
    private void sable$forceMechArmsLoad(BlockPos center, int range, CallbackInfoReturnable<Boolean> cir) {
        if (Sable.HELPER.getContaining(this.getLevel(), (Vec3i)center) != null) {
            cir.setReturnValue(true);
        }
    }
}

