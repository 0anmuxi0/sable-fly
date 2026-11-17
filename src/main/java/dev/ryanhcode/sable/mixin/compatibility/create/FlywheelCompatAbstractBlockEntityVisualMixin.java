package dev.ryanhcode.sable.mixin.compatibility.create;

import com.zurrtum.create.client.flywheel.lib.instance.FlatLit;
import com.zurrtum.create.client.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.Iterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={AbstractBlockEntityVisual.class}, remap=false)
public abstract class FlywheelCompatAbstractBlockEntityVisualMixin {
    @Inject(method={"relight(Lnet/minecraft/core/BlockPos;[Lcom/zurrtum/create/client/flywheel/lib/instance/FlatLit;)V"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void sable$relightArray(BlockPos pos, FlatLit[] instances, CallbackInfo ci) {
        Integer light = FlywheelCompatAbstractBlockEntityVisualMixin.sable$subLevelLight(pos);
        if (light == null) {
            return;
        }
        FlatLit.relight((int)light, (FlatLit[])instances);
        ci.cancel();
    }

    @Inject(method={"relight(Lnet/minecraft/core/BlockPos;Ljava/util/Iterator;)V"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void sable$relightIterator(BlockPos pos, Iterator<? extends FlatLit> instances, CallbackInfo ci) {
        Integer light = FlywheelCompatAbstractBlockEntityVisualMixin.sable$subLevelLight(pos);
        if (light == null) {
            return;
        }
        FlatLit.relight((int)light, instances);
        ci.cancel();
    }

    @Inject(method={"relight(Lnet/minecraft/core/BlockPos;Ljava/lang/Iterable;)V"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void sable$relightIterable(BlockPos pos, Iterable<? extends FlatLit> instances, CallbackInfo ci) {
        Integer light = FlywheelCompatAbstractBlockEntityVisualMixin.sable$subLevelLight(pos);
        if (light == null) {
            return;
        }
        FlatLit.relight((int)light, instances);
        ci.cancel();
    }

    @Unique
    private static Integer sable$subLevelLight(BlockPos pos) {
        if (!FlywheelCompat.ENABLED) {
            return null;
        }
        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            return null;
        }
        ClientSubLevel subLevel = FlywheelCompat.getContainingSubLevel(clientLevel, pos);
        if (subLevel == null) {
            return null;
        }
        Vector3d worldPos = subLevel.renderPose().transformPosition(new Vector3d((double)pos.getX() + 0.5, (double)pos.getY() + 0.5, (double)pos.getZ() + 0.5));
        BlockPos worldBlockPos = BlockPos.containing((double)worldPos.x, (double)worldPos.y, (double)worldPos.z);
        int lightCoords = net.minecraft.client.renderer.LevelRenderer.getLightCoords((BlockAndLightGetter)clientLevel, (BlockPos)worldBlockPos);
        return subLevel.scaleLightColor(lightCoords);
    }
}