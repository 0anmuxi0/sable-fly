/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.zurrtum.create.api.behaviour.movement.MovementBehaviour
 *  com.zurrtum.create.content.contraptions.behaviour.MovementContext
 *  com.zurrtum.create.content.kinetics.base.BlockBreakingMovementBehaviour
 *  dev.ryanhcode.sable.companion.math.JOMLConversion
 *  com.zurrtum.create.client.catnip.math.VecHelper
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.core.Vec3i
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.NbtUtils
 *  net.minecraft.nbt.Tag
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.ryanhcode.sable.mixin.compatibility.create.behaviour_compatibility.block_breaking_behaviour;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import com.zurrtum.create.content.kinetics.base.BlockBreakingMovementBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.mixinhelper.compatibility.create.block_breakers.SubLevelBlockBreakingUtility;
import dev.ryanhcode.sable.sublevel.SubLevel;
import com.zurrtum.create.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlockBreakingMovementBehaviour.class})
public abstract class BlockBreakingMovementBehaviourMixin
extends MovementBehaviour {
    @Shadow
    public abstract boolean canBreak(Level var1, BlockPos var2, BlockState var3);

    @WrapMethod(require = 0, method={"visitNewPosition"})
    public void sable$checkPosition(MovementContext context, BlockPos pos, Operation<Void> original) {
        if (!context.stall) {
            original.call(context, pos);
            if (!context.stall) {
                Vec3 localCenter = Vec3.atCenterOf((Vec3i)context.localPos);
                Vec3 sublevelLocalCenter = context.contraption.entity.toGlobalVector(localCenter, 1.0f);
                Vec3 subLevelLocalDir = (Vec3)context.rotation.apply(this.getActiveAreaOffset(context));
                BlockPos breakingPosWSublevel = SubLevelBlockBreakingUtility.findBreakingPos((blockPos, state) -> this.canBreak(context.world, (BlockPos)blockPos, (BlockState)state), Sable.HELPER.getContaining(context.world, (Vec3i)context.contraption.anchor), context.world, subLevelLocalDir, sublevelLocalCenter, pos);
                original.call(context, breakingPosWSublevel);
            }
        }
        if (context.stall && context.data.contains("BreakingPos")) {
            SubLevel targetSublevel;
            Vector3d checkPos = JOMLConversion.toJOML((Position)context.position);
            SubLevel parentSublevel = Sable.HELPER.getContaining(context.world, (Position)context.position);
            if (parentSublevel != null) {
                parentSublevel.logicalPose().transformPosition(checkPos);
            }
            if ((targetSublevel = Sable.HELPER.getContaining(context.world, (Vec3i)context.data.read("BreakingPos", BlockPos.CODEC).orElseThrow())) != null) {
                targetSublevel.logicalPose().transformPositionInverse(checkPos);
            }
            context.data.put("ProjectedPos", (Tag)VecHelper.writeNBT((Vec3)JOMLConversion.toMojang((Vector3dc)checkPos)));
        }
    }

    @Inject(require = 0, method={"cancelStall"}, at={@At(value="TAIL")})
    public void sable$removeProjected(MovementContext context, CallbackInfo ci) {
        context.data.remove("ProjectedPos");
    }

    @Inject(require = 0, method={"tick"}, at={@At(value="HEAD")}, cancellable=true)
    public void sable$testProjectedPosDist(MovementContext context, CallbackInfo ci) {
        CompoundTag data = context.data;
        if (!context.data.contains("BreakingPos")) {
            data.remove("ProjectedPos");
            return;
        }
        Vec3 sublevelLocalCenter = context.contraption.entity.toGlobalVector(Vec3.atCenterOf((Vec3i)context.localPos), 1.0f);
        if (data.contains("ProjectedPos") && Sable.HELPER.distanceSquaredWithSubLevels(context.world, (Position)VecHelper.readNBT((ListTag)data.getListOrEmpty("ProjectedPos")), (Position)sublevelLocalCenter) > 4.0) {
            BlockPos blockPos = data.read("BreakingPos", BlockPos.CODEC).orElse(null);
            data.remove("Progress");
            data.remove("TicksUntilNextProgress");
            data.remove("BreakingPos");
            data.remove("LastPos");
            data.remove("WaitingTicks");
            data.remove("ProjectedPos");
            context.stall = false;
            if (blockPos != null) {
                context.world.destroyBlockProgress(data.getIntOr("BreakerId", 0), blockPos, -1);
            }
            ci.cancel();
        }
    }
}

