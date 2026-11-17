/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.zurrtum.create.client.content.schematics.client.SchematicTransformation
 *  com.zurrtum.create.client.flywheel.lib.transform.PoseTransformStack
 *  com.zurrtum.create.client.flywheel.lib.transform.Translate
 *  com.zurrtum.create.client.catnip.math.VecHelper
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Position
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.ryanhcode.sable.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.schematics.client.SchematicTransformation;
import com.zurrtum.create.client.flywheel.lib.transform.PoseTransformStack;
import com.zurrtum.create.client.flywheel.lib.transform.Translate;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import com.zurrtum.create.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={SchematicTransformation.class})
public abstract class SchematicTransformMixin {
    @Shadow
    private Vec3 prevChasingPos;
    @Shadow
    private Vec3 chasingPos;

    @Shadow
    public abstract BlockPos getAnchor();

    @WrapOperation(require = 0, method={"applyTransformations"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/client/flywheel/lib/transform/PoseTransformStack;translate(Lnet/minecraft/world/phys/Vec3;)Lcom/zurrtum/create/client/flywheel/lib/transform/Translate;", ordinal=0)})
    public Translate<PoseTransformStack> sable$transformFromSublevel(PoseTransformStack instance, Vec3 vec3, Operation<Translate<PoseTransformStack>> original, @Local(argsOnly=true) Vec3 camera, @Local float pt, @Local PoseStack ms) {
        Vec3 center = Vec3.atCenterOf((net.minecraft.core.Vec3i)this.getAnchor());
        SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient((Position)center), ms);
        return instance.translate(VecHelper.lerp((float)pt, (Vec3)this.prevChasingPos, (Vec3)this.chasingPos).subtract(SublevelRenderOffsetHelper.translation(center)).subtract(camera));
    }
}

