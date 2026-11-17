package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets={"com.zurrtum.create.client.catnip.outliner.AABBOutline"})
public class AABBOutlineMixin {
    @WrapMethod(require = 0, method={"submitBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;IIZ)V"})
    private void sable$projectOutlineOntoSubLevel(PoseStack ms, SubmitNodeCollector queue, Vec3 camera, AABB box, int color, int lightmap, boolean disableLineNormals, Operation<Void> original) {
        ClientSubLevel subLevel = SubLevelOutlinePose.find(box);
        if (subLevel == null) {
            original.call(ms, queue, camera, box, color, lightmap, disableLineNormals);
        } else {
            SubLevelOutlinePose.runProjected(ms, subLevel, camera, plotCamera -> original.call(ms, queue, plotCamera, box, color, lightmap, disableLineNormals));
        }
    }
}