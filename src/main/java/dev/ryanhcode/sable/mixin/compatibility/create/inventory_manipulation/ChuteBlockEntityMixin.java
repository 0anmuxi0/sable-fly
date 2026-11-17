/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.zurrtum.create.content.logistics.chute.ChuteBlockEntity
 *  com.zurrtum.create.foundation.blockEntity.SmartBlockEntity
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.level.block.state.BlockState
 *  net.neoforged.neoforge.capabilities.Capabilities$ItemHandler
 *  net.neoforged.neoforge.items.Container
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 *  org.spongepowered.asm.mixin.Mixin
 */
package dev.ryanhcode.sable.mixin.compatibility.create.inventory_manipulation;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.zurrtum.create.content.logistics.chute.ChuteBlockEntity;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={ChuteBlockEntity.class})
public abstract class ChuteBlockEntityMixin
extends SmartBlockEntity {
    public ChuteBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @WrapMethod(method={"grabCapability"})
    public Container sable$grabCap(Direction side, Operation<Container> original) {
        Container handler = (Container)original.call(new Object[]{side});
        if (handler != null) {
            return handler;
        }
        Level level = this.getLevel();
        assert (level != null);
        BlockPos checkPos = this.worldPosition.relative(side);
        Direction opposite = side.getOpposite();
        Vector3d mut = new Vector3d((double)opposite.getStepX(), (double)opposite.getStepY(), (double)opposite.getStepZ());
        ActiveSableCompanion helper = Sable.HELPER;
        SubLevel parentSublevel = helper.getContaining(level, (Vec3i)checkPos);
        if (parentSublevel != null) {
            parentSublevel.logicalPose().transformNormalInverse(mut);
        }
        Vector3d includSublevelDir = new Vector3d((Vector3dc)mut);
        return helper.runIncludingSubLevels(level, Vec3.atCenterOf(checkPos), false, parentSublevel, (sublevel, pos) -> {
            includSublevelDir.set((Vector3dc)mut);
            if (sublevel != null) {
                sublevel.logicalPose().transformNormal(includSublevelDir);
            }
            // 26.3 适配：NeoForge Level.getCapability(Capabilities.ItemHandler.BLOCK) 无 Fabric 对应；
            // 语义等价：26.3 Create 使用 vanilla Container 体系，取该位置 BE 的 Container
            BlockEntity be = level.getBlockEntity(pos);
            return be instanceof Container ? (Container)be : null;
        });
    }
}

