/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.zurrtum.create.client.flywheel.api.backend.RenderContext
 *  com.zurrtum.create.client.flywheel.impl.visualization.VisualManagerImpl
 *  com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl
 *  net.minecraft.world.level.Level
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.ryanhcode.sable.mixin.compatibility.flywheel;

import com.zurrtum.create.client.flywheel.api.backend.RenderContext;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualManagerImpl;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.ryanhcode.sable.compatibility.flywheel.FlywheelCompatNeoForge;
import dev.ryanhcode.sable.mixinterface.compatibility.flywheel.BlockEntityStorageExtension;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl$RenderDispatcherImpl"})
public class RenderDispatcherImplMixin {
    @Shadow
    @Final
    private VisualizationManagerImpl this$0;

    @Inject(require = 0, method={"onStartLevelRender"}, at={@At(value="HEAD")})
    private void sable$onStartLevelRender(RenderContext ctx, CallbackInfo ci) {
        FlywheelCompatNeoForge.preVisualizationFrame((Level)Minecraft.getInstance().level, ctx.partialTick());
        ((BlockEntityStorageExtension)((VisualManagerImpl)this.this$0.blockEntities()).getStorage()).sable$preFlywheelFrame();
    }
}

