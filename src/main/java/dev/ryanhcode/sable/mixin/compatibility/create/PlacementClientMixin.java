package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create 的放置方向指示器用 {@code VecHelper.projectToPlayerView} 把世界坐标
 * 投到屏幕；目标点位于 sub-level 内时，坐标是子关卡局部系的，直接投影会画在
 * 错误的位置。先用子关卡的渲染位姿把它变换到父世界坐标，再走原投影。
 */
@Pseudo
@Mixin(targets = "com.zurrtum.create.client.catnip.placement.PlacementClient")
public abstract class PlacementClientMixin {

    @WrapOperation(
            method = "drawDirectionIndicator",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/zurrtum/create/client/catnip/math/VecHelper;projectToPlayerView(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"
            )
    )
    private static Vec3 sable$projectPhysicalPlacementTarget(
            final Vec3 target, final float partialTick, final Operation<Vec3> original
    ) {
        final var level = Minecraft.getInstance().level;
        if (level != null && Sable.HELPER.getContaining(level, target) instanceof final ClientSubLevel subLevel) {
            return original.call(subLevel.renderPose(partialTick).transformPosition(target), partialTick);
        }
        return original.call(target, partialTick);
    }
}
