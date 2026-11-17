package dev.ryanhcode.sable.mixin.compatibility.create.render_fixes;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import com.zurrtum.create.client.catnip.outliner.BlockClusterOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={BlockClusterOutline.class})
public class BlockClusterOutlineMixin {
    @Unique
    private Iterable<BlockPos> sable$collection = null;
    @Unique
    private Vec3 sable$center = Vec3.ZERO;

    @Inject(require = 0, method={"<init>"}, at={@At(value="TAIL")}, remap=false)
    private void sable$gatherSublevel(Iterable<BlockPos> selection, CallbackInfo ci) {
        this.sable$collection = selection;
    }

    @Inject(require = 0, method={"submit"}, at={@At(value="HEAD")}, remap=false)
    private void sable$projectFromSublevel(Minecraft minecraft, PoseStack ms, SubmitNodeCollector collector, Vec3 camera, float pt, CallbackInfo ci) {
        ms.pushPose();
        for (BlockPos pos : this.sable$collection) {
            ClientSubLevel sublevel = Sable.HELPER.getContainingClient((double)pos.getX() + 0.5, (double)pos.getZ() + 0.5);
            if (sublevel == null) continue;
            this.sable$center = Vec3.atCenterOf((Vec3i)pos);
            SublevelRenderOffsetHelper.posePlotToProjected(sublevel, ms);
            break;
        }
    }

    @ModifyVariable(require = 0, method={"submit"}, at=@At(value="HEAD"), remap=false, argsOnly=true)
    private Vec3 sable$modifyCamera(Vec3 camera) {
        if (this.sable$center == null) {
            for (BlockPos pos : this.sable$collection) {
                ClientSubLevel sublevel = Sable.HELPER.getContainingClient((double)pos.getX() + 0.5, (double)pos.getZ() + 0.5);
                if (sublevel == null) continue;
                this.sable$center = Vec3.atCenterOf((Vec3i)pos);
                break;
            }
        }
        return camera.add(SublevelRenderOffsetHelper.translation(this.sable$center));
    }

    @Inject(require = 0, method={"submit"}, at={@At(value="RETURN")}, remap=false)
    private void sable$popPose(Minecraft minecraft, PoseStack ms, SubmitNodeCollector collector, Vec3 camera, float pt, CallbackInfo ci) {
        ms.popPose();
        this.sable$center = Vec3.ZERO;
    }
}
