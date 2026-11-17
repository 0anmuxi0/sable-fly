package dev.ryanhcode.sable.mixin.compatibility.flywheel;

import com.zurrtum.create.client.flywheel.lib.visual.AbstractBlockEntityVisual;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={AbstractBlockEntityVisual.class})
public class AbstractBlockEntityVisualMixin {
    @Redirect(require = 0, method={"relight(Lnet/minecraft/core/BlockPos;[Lcom/zurrtum/create/client/flywheel/lib/instance/FlatLit;)V"}, at=@At(value="INVOKE", target="Lnet/minecraft/util/LightCoordsUtil;getLightCoords(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I"))
    private int sable$getLightColor(BlockAndLightGetter blockAndLightGetter, BlockPos blockPos) {
        ClientSubLevelContainer container = SubLevelContainer.getContainer(Minecraft.getInstance().level);
        assert (container != null);
        ClientSubLevel subLevel = Sable.HELPER.getContainingClient((Vec3i)blockPos);
        if (subLevel instanceof ClientSubLevel) {
            ClientSubLevel clientSubLevel = subLevel;
            int color = LightCoordsUtil.getLightCoords(blockAndLightGetter, blockPos);
            return clientSubLevel.scaleLightColor(color);
        }
        return LightCoordsUtil.getLightCoords(blockAndLightGetter, blockPos);
    }
}
