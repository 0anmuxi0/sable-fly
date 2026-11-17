package dev.ryanhcode.sable.mixin.compatibility.create.render_fixes;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import com.zurrtum.create.client.catnip.outliner.AABBOutline;
import com.zurrtum.create.client.catnip.outliner.ChasingAABBOutline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Position;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ChasingAABBOutline.class}, remap=false)
public abstract class ChasingAABBOutlineMixin
extends AABBOutline {
    public ChasingAABBOutlineMixin(AABB bb) {
        super(bb);
    }

    @Inject(require = 0, method={"submit"}, at={@At(value="HEAD")}, remap=false)
    private void sable$pushPose(Minecraft minecraft, PoseStack ms, SubmitNodeCollector collector, Vec3 camera, float pt, CallbackInfo ci) {
        ms.pushPose();
        SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient((Position)this.bb.getCenter()), ms);
    }

    @ModifyArg(require = 0, method={"submit"}, at=@At(value="INVOKE", target="Lcom/zurrtum/create/client/catnip/outliner/ChasingAABBOutline;submitBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;IIZ)V"), index=3)
    private AABB sable$moveBB(AABB box) {
        return box.move(SublevelRenderOffsetHelper.translation(box.getCenter()).scale(-1.0));
    }

    @Inject(require = 0, method={"submit"}, at={@At(value="RETURN")}, remap=false)
    private void sable$popPose(Minecraft minecraft, PoseStack ms, SubmitNodeCollector collector, Vec3 camera, float pt, CallbackInfo ci) {
        ms.popPose();
    }
}
