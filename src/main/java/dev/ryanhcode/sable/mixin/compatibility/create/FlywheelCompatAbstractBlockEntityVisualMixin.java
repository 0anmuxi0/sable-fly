package dev.ryanhcode.sable.mixin.compatibility.create;

import com.zurrtum.create.client.flywheel.lib.instance.FlatLit;
import com.zurrtum.create.client.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * CPU-side lighting adapter: when a sublevel BE's Flywheel visual samples light, the
 * plot-virtual position is transformed by the sublevel render pose into real world
 * coordinates first, and the sublevel sky-light scale is applied via scaleLightColor
 * (mirroring official Sable 1.21.1).
 *
 * <p>26.1.2 create-fly {@code AbstractBlockEntityVisual} (verified by javap): 3 of the 6
 * {@code relight} overloads call
 * {@code LevelRenderer.getLightCoords(BlockAndLightGetter, BlockPos)} with the BE's
 * plot-virtual position (near +/-20.48M). The parent-world light engine has no data
 * there, so instances show a constant brightness with no AO. This mixin takes over the
 * three position-based overloads at HEAD: sublevel BEs are re-lit at the transformed
 * world position and the original call is cancelled; parent-world BEs keep the original
 * path.</p>
 *
 * <p>Difference from official Sable 1.21.1's AbstractBlockEntityVisualMixin: that one
 * redirects {@code LevelRenderer.getLightColor}, which later versions replaced; and since
 * we mixin a flywheel class (remap=false), a @Redirect targeting a vanilla method is
 * unreliable without a refmap, so a HEAD cancellable takeover is used instead
 * (semantically equivalent).</p>
 *
 * <p>relight runs on visual creation / section light changes (not per frame), so the
 * extra matrix transform is negligible.</p>
 */
@Mixin(value = AbstractBlockEntityVisual.class, remap = false)
public abstract class FlywheelCompatAbstractBlockEntityVisualMixin {

    /**
     * relight(BlockPos, FlatLit...) - varargs overload.
     */
    @Inject(method = "relight(Lnet/minecraft/core/BlockPos;[Lcom/zurrtum/create/client/flywheel/lib/instance/FlatLit;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sable$relightArray(BlockPos pos, FlatLit[] instances, CallbackInfo ci) {
        Integer light = sable$subLevelLight(pos);
        if (light == null) {
            return;
        }
        FlatLit.relight(light, instances);
        ci.cancel();
    }

    /**
     * relight(BlockPos, Iterator).
     */
    @Inject(method = "relight(Lnet/minecraft/core/BlockPos;Ljava/util/Iterator;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sable$relightIterator(BlockPos pos, Iterator<? extends FlatLit> instances, CallbackInfo ci) {
        Integer light = sable$subLevelLight(pos);
        if (light == null) {
            return;
        }
        FlatLit.relight(light, instances);
        ci.cancel();
    }

    /**
     * relight(BlockPos, Iterable).
     */
    @Inject(method = "relight(Lnet/minecraft/core/BlockPos;Ljava/lang/Iterable;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void sable$relightIterable(BlockPos pos, Iterable<? extends FlatLit> instances, CallbackInfo ci) {
        Integer light = sable$subLevelLight(pos);
        if (light == null) {
            return;
        }
        FlatLit.relight(light, instances);
        ci.cancel();
    }

    /**
     * Computes the real-world packed light for a sublevel BE.
     *
     * @return null for non-sublevel BEs (or when the adapter is disabled) to keep the
     *         original path; otherwise the packed light coords sampled at the pose-
     *         transformed world position with the sky-light scale applied.
     */
    @Unique
    private static Integer sable$subLevelLight(BlockPos pos) {
        if (!FlywheelCompat.ENABLED) {
            return null;
        }
        net.minecraft.client.multiplayer.ClientLevel clientLevel =
                net.minecraft.client.Minecraft.getInstance().level;
        if (clientLevel == null) {
            return null;
        }
        ClientSubLevel subLevel = FlywheelCompat.getContainingSubLevel(clientLevel, pos);
        if (subLevel == null) {
            return null;
        }
        // Plot-virtual position -> real world position (translation/rotation/scale).
        Vector3d worldPos = subLevel.renderPose().transformPosition(
                new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
        BlockPos worldBlockPos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);
        // 26.1.2: LevelRenderer.getLightCoords (26.2 renamed it to LightCoordsUtil).
        int lightCoords = net.minecraft.client.renderer.LevelRenderer.getLightCoords(
                (BlockAndLightGetter) clientLevel, worldBlockPos);
        return subLevel.scaleLightColor(lightCoords);
    }
}
