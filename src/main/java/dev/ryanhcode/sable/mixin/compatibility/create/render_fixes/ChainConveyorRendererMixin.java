/*
 * Decompiled with CFR 0.152.
 * 
 * Sable-Fly 26.3 移植说明：Create 26.3 中 renderChains 已改名为 getChainsRenderState，
 * 其中仍存在 Vec3.closerThan(Position, double) 调用，故仅更新注入方法名。
 */
package dev.ryanhcode.sable.mixin.compatibility.create.render_fixes;

import com.zurrtum.create.client.content.kinetics.chainConveyor.ChainConveyorRenderer;
import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Position;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={ChainConveyorRenderer.class})
public class ChainConveyorRendererMixin {
    @Redirect(require = 0, method={"getChainsRenderState"}, at=@At(value="INVOKE", target="Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"))
    public boolean sable$fixMipDistance(Vec3 instance, Position position, double d) {
        return Sable.HELPER.distanceSquaredWithSubLevels((Level)Minecraft.getInstance().level, (Position)instance, position.x(), position.y(), position.z()) < d * d;
    }
}
