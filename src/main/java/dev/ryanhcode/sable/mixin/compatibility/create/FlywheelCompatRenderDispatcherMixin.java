package dev.ryanhcode.sable.mixin.compatibility.create;

import com.zurrtum.create.client.flywheel.api.backend.RenderContext;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Drives all sublevel embedding transform updates at the start of each rendered frame
 * (Flywheel onStartLevelRender HEAD).
 *
 * <p>26.1.2 create-fly structure (verified by javap): {@code VisualizationManagerImpl}
 * owns a private inner class {@code RenderDispatcherImpl implements
 * VisualizationManager.RenderDispatcher} whose {@code onStartLevelRender(RenderContext ctx)}
 * calls the outer {@code beginFrame(ctx)}; the synthetic field {@code this$0} points at the
 * outer {@code VisualizationManagerImpl}.</p>
 */
@Mixin(targets = "com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl$RenderDispatcherImpl", remap = false)
public abstract class FlywheelCompatRenderDispatcherMixin {
    @Shadow(remap = false)
    @Final
    private VisualizationManagerImpl this$0;

    @Inject(method = "onStartLevelRender(Lcom/zurrtum/create/client/flywheel/api/backend/RenderContext;)V", at = @At("HEAD"), remap = false)
    private void sable$preVisualizationFrame(RenderContext ctx, CallbackInfo ci) {
        Map<ClientSubLevel, FlywheelCompat.FlywheelEmbeddingState> states = FlywheelCompat.embeddings();
        if (states.isEmpty() && !FlywheelCompat.hasSubLevels()) {
            return;
        }
        Vec3i parentOrigin = this.this$0.renderOrigin();
        ClientLevel clientLevel = Minecraft.getInstance().level;
        VisualizationManager manager = clientLevel != null ? VisualizationManager.get(clientLevel) : null;
        if (manager == null) {
            return;
        }
        // First run the sublevel BE registration bridge (queueAdd new / queueRemove gone /
        // retry pending).
        if (FlywheelCompat.ENABLED) {
            FlywheelCompat.syncSubLevelBlockEntities(clientLevel, manager);
        }
        float partialTick = ctx.partialTick();
        Iterator<Map.Entry<ClientSubLevel, FlywheelCompat.FlywheelEmbeddingState>> iter = states.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<ClientSubLevel, FlywheelCompat.FlywheelEmbeddingState> entry = iter.next();
            ClientSubLevel subLevel = entry.getKey();
            FlywheelCompat.FlywheelEmbeddingState state = entry.getValue();
            if (subLevel.isRemoved() || !state.hasVisuals() || !isLevelOf(subLevel, clientLevel)) {
                // Sublevel removed (or world switched): remove all of its BEs from the
                // storage and delete the embedding.
                for (BlockEntity be : state.blockEntities()) {
                    manager.blockEntities().queueRemove(be);
                }
                state.embedding().delete();
                iter.remove();
                continue;
            }
            state.updateTransforms(subLevel.renderPose(partialTick), parentOrigin);
        }
    }

    private static boolean isLevelOf(ClientSubLevel subLevel, ClientLevel clientLevel) {
        SubLevelContainer container = SubLevelContainer.getContainer(clientLevel);
        if (container == null) {
            return false;
        }
        for (var existingObj : container.getAllSubLevels()) {
            if (existingObj instanceof ClientSubLevel existing && existing == subLevel) {
                return true;
            }
        }
        return false;
    }
}
