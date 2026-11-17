/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.mojang.blaze3d.vertex.PoseStack
 *  com.zurrtum.create.client.content.schematics.client.tools.DeployTool
 *  com.zurrtum.create.client.content.schematics.client.tools.SchematicToolBase
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.phys.Vec3
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 */
package dev.ryanhcode.sable.mixin.compatibility.create.schematics;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.client.content.schematics.client.tools.DeployTool;
import com.zurrtum.create.client.content.schematics.client.tools.SchematicToolBase;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.mixinhelper.compatibility.create.renderers.AABBOutlineRenderingOptions;
import dev.ryanhcode.sable.util.SublevelRenderOffsetHelper;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={DeployTool.class})
public abstract class DeployToolMixin
extends SchematicToolBase {
    @WrapOperation(require = 0, method={"renderTool"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V")})
    public void sable$manualTransformBB(PoseStack instance, double x, double y, double z, Operation<Void> original, @Local(ordinal=0) int centerX, @Local(ordinal=1) int centerZ, @Local(argsOnly=true) Vec3 camera) {
        SublevelRenderOffsetHelper.posePlotToProjected(Sable.HELPER.getContainingClient((Vec3i)this.selectedPos), instance);
        Vec3 trans = SublevelRenderOffsetHelper.translation(Vec3.atCenterOf((Vec3i)this.selectedPos));
        original.call(instance, x - trans.x, y - trans.y, z - trans.z);
        ((AABBOutlineRenderingOptions)this.schematicHandler.getOutline()).sable$shouldTransform(false);
    }
}

