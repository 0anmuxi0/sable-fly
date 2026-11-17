/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.zurrtum.create.content.processing.basin.BasinBlockEntity
 *  com.zurrtum.create.content.processing.burner.BlazeBurnerBlock$HeatLevel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.Nullable
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.At$Shift
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.ryanhcode.sable.mixin.compatibility.create.basin_interactions;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.processing.basin.BasinBlockEntity;
import com.zurrtum.create.content.processing.burner.BlazeBurnerBlock;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={BasinBlockEntity.class})
public class BasinBlockEntityMixin
extends BlockEntity {
    @Shadow
    @org.jetbrains.annotations.Nullable
    private BlazeBurnerBlock.HeatLevel cachedHeatLevel;

    public BasinBlockEntityMixin(BlockEntityType<?> arg, BlockPos arg2, BlockState arg3) {
        super(arg, arg2, arg3);
    }

    @Inject(require = 0, method={"getHeatLevel"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/content/processing/basin/BasinBlockEntity;getHeatLevelOf(Lnet/minecraft/world/level/block/state/BlockState;)Lcom/zurrtum/create/content/processing/burner/BlazeBurnerBlock$HeatLevel;", shift=At.Shift.AFTER)}, cancellable=true)
    private void sable$accountForSubLevels(CallbackInfoReturnable<BlazeBurnerBlock.HeatLevel> cir) {
        BlockPos originalPos;
        if (this.cachedHeatLevel != null && this.cachedHeatLevel != BlazeBurnerBlock.HeatLevel.NONE) {
            return;
        }
        ActiveSableCompanion helper = Sable.HELPER;
        Level level = this.getLevel();
        BlazeBurnerBlock.HeatLevel heatLevel = helper.runIncludingSubLevels(level, Vec3.atCenterOf(originalPos = this.getBlockPos().below()), false, helper.getContaining(level, (Vec3i)originalPos), (subLevel, pos) -> {
            BlazeBurnerBlock.HeatLevel internalHeat = BasinBlockEntity.getHeatLevelOf((BlockState)level.getBlockState(pos));
            if (internalHeat != BlazeBurnerBlock.HeatLevel.NONE) {
                return internalHeat;
            }
            return null;
        });
        if (heatLevel != null) {
            cir.setReturnValue(heatLevel);
        }
    }

    @WrapOperation(require = 0, method={"*"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;")})
    public BlockEntity sable$accountForSubLevels(Level level, BlockPos pos, Operation<BlockEntity> original) {
        ActiveSableCompanion helper = Sable.HELPER;
        return helper.runIncludingSubLevels(level, Vec3.atCenterOf(pos), true, helper.getContaining(level, (Vec3i)pos), (subLevel, internalPos) -> (BlockEntity)original.call(level, internalPos));
    }
}

