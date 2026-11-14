package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Renders Create's ValueBox (scroll/number input overlays) at the sub-level's world pose.
 *
 * <p>Create submits value boxes with a world-space camera against the main level only. Inside a
 * sub-level the box must follow the moving/rotating/scaled structure, so the whole submit is
 * wrapped and re-projected into the sub-level's render pose.</p>
 */
@Pseudo
@Mixin(targets = {"com.zurrtum.create.client.foundation.blockEntity.behaviour.ValueBox"})
public class ValueBoxMixin {
    @Shadow
    protected BlockPos pos;

    @WrapMethod(method = {"submit(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/phys/Vec3;F)V"})
    private void sable$submitAtWorldPos(Minecraft mc, PoseStack poseStack, SubmitNodeCollector queue, Vec3 camera, float pt, Operation<Void> original) {
        ClientSubLevel subLevel = this.sable$getSubLevel(mc);
        if (subLevel == null) {
            original.call(mc, poseStack, queue, camera, pt);
            return;
        }
        // Same projection pattern as AABBOutlineMixin: multiply the render pose (rotate+scale) into
        // the PoseStack and hand submit() a plot-local camera, so its internal
        // translate(pos - camera) lands in plot space and the whole box (outline + text) follows
        // the sub-level, scale included. Bytecode-verified against the 26.2 pipeline:
        // SubLevelOutlinePose.apply multiplies rotate+scale only (renderPose.position and
        // rotationPoint never enter the matrix), and the rotationPoint translation is cancelled by
        // the inverse-transformed plotCamera inside submit's translate(pos - camera).
        SubLevelOutlinePose.runProjected(poseStack, subLevel, camera, plotCamera ->
            original.call(mc, poseStack, queue, plotCamera, pt));
    }

    private ClientSubLevel sable$getSubLevel(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null) {
            return null;
        }
        SubLevel subLevel = Sable.HELPER.getContaining((Level) level, (Vec3i) this.pos);
        return subLevel instanceof ClientSubLevel clientSubLevel ? clientSubLevel : null;
    }
}
