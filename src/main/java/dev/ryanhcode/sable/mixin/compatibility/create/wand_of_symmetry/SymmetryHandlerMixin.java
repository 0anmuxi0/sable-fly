/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.zurrtum.create.content.equipment.symmetryWand.SymmetryHandler
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.ryanhcode.sable.mixin.compatibility.create.wand_of_symmetry;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.content.equipment.symmetryWand.SymmetryHandler;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={SymmetryHandler.class})
public class SymmetryHandlerMixin {
    /**
     * 1.21.1 的 {@code SymmetryHandler#onRenderWorld(PoseStack, float, BlockPos)} 在 Create 26.3
     * 已被移除：对称魔杖的渲染改走客户端 {@code SymmetryWandRenderHandler}。
     * Create 26.3 的 {@code SymmetryHandler} 只剩 {@code onBlockPlaced}/{@code onBlockDestroyed}。
     * 目标方法彻底不存在，故移除 {@code @Redirect}，仅保留方法体。
     */
    private static void accountForSublevels(PoseStack instance, double f, double g, double h, @Local(name={"pos"}) BlockPos pos) {
        SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient((Vec3i)pos), instance);
        Vec3 translation = SublevelRenderOffsetHelper.translation(Vec3.atLowerCornerOf((Vec3i)pos));
        instance.translate(f - translation.x, g - translation.y, h - translation.z);
    }
}

