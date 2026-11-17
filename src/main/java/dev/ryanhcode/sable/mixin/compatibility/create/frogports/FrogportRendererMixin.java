/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.zurrtum.create.content.logistics.packagePort.PackagePortBlockEntity
 *  com.zurrtum.create.content.logistics.packagePort.PackagePortTarget
 *  com.zurrtum.create.content.logistics.packagePort.frogport.FrogportRenderer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.LevelAccessor
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.ryanhcode.sable.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.logistics.packagePort.PackagePortBlockEntity;
import com.zurrtum.create.content.logistics.packagePort.PackagePortTarget;
import com.zurrtum.create.client.content.logistics.packagePort.frogport.FrogportRenderer;
import dev.ryanhcode.sable.mixinhelper.compatibility.create.frogports.FrogportMixinHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={FrogportRenderer.class})
public class FrogportRendererMixin {
    /**
     * 1.21.1 的 {@code FrogportRenderer#renderSafe(FrogportBlockEntity, float, PoseStack,
     * MultiBufferSource, int, int)} 在 Create 26.3 已不存在。
     *
     * <p>渲染器改成了 {@code createRenderState/extractRenderState/submit} 三段式，
     * {@code getExactTargetLocation} 的调用被搬进 {@code extractRenderState}。
     * 目标方法彻底不存在，故移除 {@code @WrapOperation}，仅保留方法体。
     * 等价逻辑仍由 {@code FrogportVisualMixin#animate} 覆盖。
     */
    public Vec3 sable$getExactTargetLocation(PackagePortTarget instance, PackagePortBlockEntity packagePortBlockEntity, LevelAccessor levelAccessor, BlockPos blockPos, Operation<Vec3> original) {
        return FrogportMixinHelper.getExactTargetLocation(instance, packagePortBlockEntity, levelAccessor, blockPos, original);
    }
}

