/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.client.flywheel.api.task.Plan
 *  com.zurrtum.create.client.flywheel.api.visual.DynamicVisual$Context
 *  com.zurrtum.create.client.flywheel.api.visualization.VisualManager
 *  com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext
 *  com.zurrtum.create.client.flywheel.impl.visualization.VisualManagerImpl
 *  com.zurrtum.create.client.flywheel.impl.visualization.storage.Storage
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable
 */
package dev.ryanhcode.sable.mixin.compatibility.flywheel;

import com.zurrtum.create.client.flywheel.api.task.Plan;
import com.zurrtum.create.client.flywheel.api.visual.DynamicVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualManager;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualManagerImpl;
import com.zurrtum.create.client.flywheel.impl.visualization.storage.Storage;
import dev.ryanhcode.sable.mixinterface.compatibility.flywheel.BlockEntityStorageExtension;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={VisualManagerImpl.class}, remap=false)
public abstract class VisualManagerImplMixin<T, S extends Storage<T>>
implements VisualManager<T> {
    @Shadow
    @Final
    private S storage;

    @Inject(require = 0, method={"framePlan"}, at={@At(value="HEAD")})
    private void sable$preFramePlan(VisualizationContext visualizationContext, CallbackInfoReturnable<Plan<DynamicVisual.Context>> cir) {
        S s = this.storage;
        if (s instanceof BlockEntityStorageExtension) {
            BlockEntityStorageExtension extension = (BlockEntityStorageExtension)s;
            extension.sable$setPlanVisualizationContext(visualizationContext);
        }
    }
}

