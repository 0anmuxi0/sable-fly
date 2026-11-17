/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.content.processing.basin.BasinOperatingBlockEntity
 *  com.zurrtum.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.ryanhcode.sable.mixin.compatibility.create.basin_interactions;

import com.zurrtum.create.content.processing.basin.BasinOperatingBlockEntity;
import com.zurrtum.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BasinOperatingBlockEntity.class})
public abstract class BasinOperatingBlockEntityMixin {
    @Shadow
    public DeferralBehaviour basinChecker;
    @Unique
    private int sable$forceUpdateTicks = 0;

    @Inject(require = 0, method={"tick"}, at={@At(value="HEAD")}, remap=false)
    private void sable$forceUpdate(CallbackInfo ci) {
        if (this.sable$forceUpdateTicks == 5) {
            this.basinChecker.scheduleUpdate();
            this.sable$forceUpdateTicks = 0;
        }
        ++this.sable$forceUpdateTicks;
    }

    @Redirect(require = 0, method={"getBasin"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private BlockEntity sable$accountForSubLevels(Level level, BlockPos pos) {
        ActiveSableCompanion helper = Sable.HELPER;
        return helper.runIncludingSubLevels(level, Vec3.atCenterOf(pos), true, helper.getContaining(level, (Vec3i)pos), (subLevel, internalPos) -> level.getBlockEntity(internalPos));
    }
}

