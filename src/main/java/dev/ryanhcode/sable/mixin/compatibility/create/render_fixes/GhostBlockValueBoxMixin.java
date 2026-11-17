/*
 * Decompiled with CFR 0.152.
 * 
 * Sable-Fly 26.3 移植说明：
 * 原 @Redirect 目标是 TransparentGhostBlockRenderer.render 中的 PoseStack.translate(DDD)V。
 * Create 26.3 已把该方法改为直接操作 PoseStack$Pose.translate(FFF)Matrix4f，
 * 目标调用点彻底不存在，故移除 @Redirect 注解但保留方法体。
 */
package dev.ryanhcode.sable.mixin.compatibility.create.render_fixes;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(targets={"com.zurrtum.create.client.catnip.ghostblock.GhostBlockRenderer$TransparentGhostBlockRenderer"})
public abstract class GhostBlockValueBoxMixin {
    public void sable$translate(PoseStack ms, double pX, double pY, double pZ) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.mainCamera().position();
        Vec3 center = new Vec3(pX + camera.x, pY + camera.y, pZ + camera.z);
        SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient((Position)center), ms);
        Vec3 translation = SublevelRenderOffsetHelper.translation(center);
        ms.translate(pX - translation.x, pY - translation.y, pZ - translation.z);
    }
}
