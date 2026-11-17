/*
 * Decompiled with CFR 0.152.
 * 
 * Sable-Fly 26.3 移植说明：
 * 原 @Redirect 目标是 renderOnBlockEntity 中的 Vec3.distanceToSqr(Vec3)D。
 * Create 26.3 的 FilteringRenderer 已重构为 getFilterRenderState(SmartBlockEntity, BlockState,
 * ItemModelResolver, double)，整个类中不再出现任何 distanceToSqr 调用，
 * 目标方法与调用点彻底不存在，故移除 @Redirect 注解但保留方法体。
 */
package dev.ryanhcode.sable.mixin.compatibility.create.render_fixes;

import com.zurrtum.create.client.foundation.blockEntity.behaviour.filtering.FilteringRenderer;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={FilteringRenderer.class})
public class FilteringRendererMixin {
    private static double sable$distanceToSqr(Vec3 instance, Vec3 pVec) {
        return Sable.HELPER.distanceSquaredWithSubLevels((Level)Minecraft.getInstance().level, (Position)instance, (Position)pVec);
    }
}
