package dev.ryanhcode.sable.mixin.compatibility.create;

import com.zurrtum.create.client.flywheel.api.backend.RenderContext;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationManager;
import com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets={"com.zurrtum.create.client.flywheel.impl.visualization.VisualizationManagerImpl$RenderDispatcherImpl"}, remap=false)
public abstract class FlywheelCompatRenderDispatcherMixin {
    @Shadow(remap=false)
    @Final
    private VisualizationManagerImpl this$0;

    @Inject(require = 0, method={"onStartLevelRender(Lcom/zurrtum/create/client/flywheel/api/backend/RenderContext;)V"}, at={@At(value="HEAD")}, remap=false)
    private void sable$preVisualizationFrame(RenderContext ctx, CallbackInfo ci) {
        VisualizationManager manager;
        Map<ClientSubLevel, FlywheelCompat.FlywheelEmbeddingState> states = FlywheelCompat.embeddings();
        if (states.isEmpty() && !FlywheelCompat.hasSubLevels()) {
            return;
        }
        Vec3i parentOrigin = this.this$0.renderOrigin();
        ClientLevel clientLevel = Minecraft.getInstance().level;
        VisualizationManager visualizationManager = manager = clientLevel != null ? VisualizationManager.get((LevelAccessor)clientLevel) : null;
        if (manager == null) {
            return;
        }
        if (FlywheelCompat.ENABLED) {
            FlywheelCompat.syncSubLevelBlockEntities(clientLevel, manager);
        }
        float partialTick = ctx.partialTick();
        Iterator<Map.Entry<ClientSubLevel, FlywheelCompat.FlywheelEmbeddingState>> iter = states.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<ClientSubLevel, FlywheelCompat.FlywheelEmbeddingState> entry = iter.next();
            ClientSubLevel subLevel = entry.getKey();
            FlywheelCompat.FlywheelEmbeddingState state = entry.getValue();
            if (subLevel.isRemoved() || !state.hasVisuals() || !FlywheelCompatRenderDispatcherMixin.isLevelOf(subLevel, clientLevel)) {
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
        ClientSubLevelContainer container = SubLevelContainer.getContainer(clientLevel);
        if (container == null) {
            return false;
        }
        for (SubLevel subLevel2 : ((SubLevelContainer)container).getAllSubLevels()) {
            ClientSubLevel existing;
            if (!(subLevel2 instanceof ClientSubLevel) || (existing = (ClientSubLevel)subLevel2) != subLevel) continue;
            return true;
        }
        return false;
    }
}