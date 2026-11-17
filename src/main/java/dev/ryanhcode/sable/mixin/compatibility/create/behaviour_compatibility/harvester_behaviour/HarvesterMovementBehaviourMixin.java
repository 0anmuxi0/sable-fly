/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.zurrtum.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour
 *  com.zurrtum.create.content.contraptions.behaviour.MovementContext
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 *  org.spongepowered.asm.mixin.Mixin
 */
package dev.ryanhcode.sable.mixin.compatibility.create.behaviour_compatibility.harvester_behaviour;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.zurrtum.create.content.contraptions.actors.harvester.HarvesterMovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinhelper.compatibility.create.behavior_compatibility.harvester_block_entity.DummyMovementContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={HarvesterMovementBehaviour.class})
public class HarvesterMovementBehaviourMixin {
    @WrapMethod(require = 0, method={"visitNewPosition"})
    public void sable$checkAllPositions(MovementContext context, BlockPos pos, Operation<Void> original) {
        if (context instanceof DummyMovementContext) {
            original.call(context, pos);
            return;
        }
        ActiveSableCompanion helper = Sable.HELPER;
        helper.runIncludingSubLevels(context.world, Vec3.atCenterOf(pos), true, helper.getContaining((Entity)context.contraption.entity), (sublevel, blockPos) -> {
            original.call(context, blockPos);
            return null;
        });
    }
}

