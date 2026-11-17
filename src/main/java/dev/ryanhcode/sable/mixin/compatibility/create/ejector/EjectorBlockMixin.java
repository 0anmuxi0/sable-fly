/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.zurrtum.create.content.logistics.depot.EjectorBlock
 *  dev.ryanhcode.sable.companion.math.JOMLConversion
 *  net.minecraft.core.Position
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3d
 *  org.joml.Vector3dc
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.ryanhcode.sable.mixin.compatibility.create.ejector;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.logistics.depot.EjectorBlock;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.Position;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={EjectorBlock.class})
public class EjectorBlockMixin {
    /**
     * 1.21.1 的 {@code Block#updateEntityAfterFallOn(BlockGetter, Entity)} 在 Minecraft 26.3
     * 已被 {@code Block#fallOn(Level, BlockState, BlockPos, Entity, double)} 取代，
     * 且 Create 26.3 的 {@code EjectorBlock} 也不再覆盖它。
     * 目标方法彻底不存在，故移除 {@code @Redirect}，仅保留方法体。
     */
    public double distanceTo(Vec3 instance, Vec3 vec, @Local(argsOnly=true) Entity entity) {
        return Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(entity.level(), (Position)instance, (Position)vec));
    }

    /** 同上：{@code updateEntityAfterFallOn} 已不存在，仅保留方法体。 */
    public Vec3 setPos(Vec3 instance, Vec3 vec, @Local(argsOnly=true) Entity entity) {
        Vector3d projected = Sable.HELPER.projectOutOfSubLevel(entity.level(), JOMLConversion.toJOML((Position)instance)).add(vec.x, vec.y, vec.z);
        return JOMLConversion.toMojang((Vector3dc)projected);
    }
}

