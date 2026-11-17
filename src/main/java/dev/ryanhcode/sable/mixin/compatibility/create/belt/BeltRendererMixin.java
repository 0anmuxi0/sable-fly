/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.ModifyExpressionValue
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.zurrtum.create.content.kinetics.belt.BeltBlockEntity
 *  com.zurrtum.create.content.kinetics.belt.BeltRenderer
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.ryanhcode.sable.mixin.compatibility.create.belt;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.kinetics.belt.BeltBlockEntity;
import com.zurrtum.create.client.content.kinetics.belt.BeltRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 1.21.1 在 {@code BeltRenderer.renderItem} 内拦截 {@code Camera.getPosition()}，
 * 把相机位置换算到子关卡本地空间。
 *
 * <p>Create 26.3 将渲染流程改为 1.21.9+ 的 RenderState 体系：
 * {@code renderItem(BeltRenderState, BeltItemState, PoseStack, SubmitNodeCollector)}，
 * 经 javap 核对，其方法体内 <b>不再引用</b> {@code Camera.getPosition()}
 * （相机变换已上移到 {@code submit(...)} 的 {@code CameraRenderState} 参数）。
 * 原注入点已不存在，因此移除注解，仅保留方法体备查。
 */
@Mixin(value={BeltRenderer.class})
public class BeltRendererMixin {
    // 原 @ModifyExpressionValue(require = 0, method={"renderItem"},
    //     at=@At(value="INVOKE", target="Lnet/minecraft/client/Camera;getPosition()Lnet/minecraft/world/phys/Vec3;"))
    // 目标表达式在 Create 26.3 的 renderItem 中已不存在（渲染状态化重构）。
    private Vec3 sable$renderViewEntityPosition(Vec3 original, @Local(argsOnly=true) BeltBlockEntity be) {
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient((BlockEntity)be);
        if (subLevel != null) {
            return subLevel.renderPose().transformPositionInverse(original);
        }
        return original;
    }
}

