/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.zurrtum.create.content.contraptions.actors.harvester.HarvesterBlockEntity
 *  com.zurrtum.create.content.contraptions.actors.harvester.HarvesterRenderer
 *  com.zurrtum.create.client.catnip.math.AngleHelper
 *  com.zurrtum.create.client.catnip.render.SuperByteBuffer
 *  net.minecraft.core.Direction
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.ryanhcode.sable.mixin.compatibility.create.behaviour_compatibility.harvester_block_entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.contraptions.actors.harvester.HarvesterBlockEntity;
import com.zurrtum.create.client.content.contraptions.actors.harvester.HarvesterRenderer;
import dev.ryanhcode.sable.mixinhelper.compatibility.create.harvester.HarvesterLerpedSpeed;
import com.zurrtum.create.catnip.math.AngleHelper;
import com.zurrtum.create.client.catnip.render.SuperByteBuffer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.1 包装 {@code HarvesterRenderer.renderSafe} 内的 {@code HarvesterRenderer.transform(...)} 调用，
 * 在智能收割机静止时改用 Sable 的平滑角速度（{@link HarvesterLerpedSpeed}）。
 *
 * <p>Create 26.3 把渲染器重写为 1.21.9+ 的 RenderState 体系：
 * 原 {@code renderSafe(...)} 与 {@code transform(Level, Direction, SuperByteBuffer, float, Vec3)}
 * 两个方法 <b>都已不存在</b>（现有方法只有 createRenderState / extractRenderState / submit）。
 * 目标方法缺失时 {@code require = 0} 无效，仍会抛 Critical injection failure，
 * 因此移除该 @WrapOperation 注解，仅保留方法体备查。
 */
@Mixin(value={HarvesterRenderer.class})
public class HarvesterRendererMixin {
    // 原 @WrapOperation(require = 0,
    //     method={"renderSafe(Lcom/zurrtum/create/content/contraptions/actors/harvester/HarvesterBlockEntity;F...Lnet/minecraft/client/renderer/MultiBufferSource;II)V"},
    //     at=@At(value="INVOKE", target="L.../HarvesterRenderer;transform(...)V"))
    // renderSafe 与 transform 在 Create 26.3 中均已不存在，注入点消失。
    public void sable$smoothSpeed(Level world, Direction facing, SuperByteBuffer superBuffer, float speed, Vec3 pivot, Operation<Void> original, @Local HarvesterBlockEntity be, @Local float pt) {
        if (be.getAnimatedSpeed() != 0.0f) {
            original.call(world, facing, superBuffer, Float.valueOf(speed), pivot);
        } else {
            float originOffset = 0.0625f;
            Vec3 rotOffset = new Vec3(0.0, pivot.y * 0.0625, pivot.z * 0.0625);
            ((SuperByteBuffer)((SuperByteBuffer)((SuperByteBuffer)superBuffer.rotateCentered(AngleHelper.rad((double)AngleHelper.horizontalAngle((Direction)facing)), Direction.UP)).translate(rotOffset.x, rotOffset.y, rotOffset.z)).rotate(AngleHelper.rad((double)(-((HarvesterLerpedSpeed)be).sable$getLerpedFloat().getValue(pt))), Direction.WEST)).translate(-rotOffset.x, -rotOffset.y, -rotOffset.z);
        }
    }
}

