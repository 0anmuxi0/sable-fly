/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.zurrtum.create.foundation.blockEntity.SmartBlockEntity
 *  com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer
 *  net.minecraft.client.renderer.entity.EntityRenderDispatcher
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  org.joml.Quaterniondc
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.ryanhcode.sable.mixin.compatibility.create.frogports;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.client.foundation.blockEntity.renderer.SmartBlockEntityRenderer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={SmartBlockEntityRenderer.class})
public class SmartBlockEntityRendererMixin<T extends SmartBlockEntity> {
    /**
     * 1.21.1 的 {@code renderNameplateOnHover} 在 Create 26.3 已被移除。
     *
     * <p>渲染管线改成 state/extract/submit 三段式后，名牌朝向改由
     * {@code getNameplateRenderState(...)} 与 {@code submit(...)} 处理，
     * 不再有 {@code EntityRenderDispatcher.cameraOrientation()} 的注入点。
     * 目标方法彻底不存在，故移除 {@code @Redirect}，仅保留方法体。
     */
    private Quaternionf sable$renderNameTag(EntityRenderDispatcher instance, @Local(argsOnly=true) T be) {
        SubLevel subLevel = Sable.HELPER.getContaining((BlockEntity)be);
        if (subLevel == null) {
            return instance.camera.rotation();
        }
        Quaterniondc subLevelOrientation = ((ClientSubLevel)subLevel).renderPose().orientation();
        return instance.camera.rotation().premul((Quaternionfc)new Quaternionf(subLevelOrientation).conjugate());
    }
}

