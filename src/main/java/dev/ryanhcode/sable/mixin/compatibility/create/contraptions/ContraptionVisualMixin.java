package dev.ryanhcode.sable.mixin.compatibility.create.contraptions;

import com.mojang.blaze3d.vertex.PoseStack;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.client.content.contraptions.render.ContraptionVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualEmbedding;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.lib.visual.AbstractEntityVisual;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.compatibility.flywheel.FlywheelCompatNeoForge;
import dev.ryanhcode.sable.mixinterface.compatibility.flywheel.EmbeddedEnvironmentExtension;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.joml.Matrix3fc;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 移植自原版 Sable NeoForge（sable-neoforge-1.21.1-2.0.6.jar）ContraptionVisualMixin
// 26.3 适配：mulPose(Quaternionf) -> rotate(Quaternionfc)；ContraptionVisual 移至 com.zurrtum.create.client 包
@Mixin(value = {ContraptionVisual.class})
public abstract class ContraptionVisualMixin extends AbstractEntityVisual<AbstractContraptionEntity> {
    @Shadow
    @Final
    protected VisualEmbedding embedding;
    @Shadow
    @Final
    private PoseStack contraptionMatrix;

    public ContraptionVisualMixin(VisualizationContext ctx, AbstractContraptionEntity entity, float partialTick) {
        super(ctx, entity, partialTick);
    }

    @Inject(method = {"setEmbeddingMatrices"}, at = {@At(value = "TAIL")})
    private void sable$setEmbeddingMatrices(float partialTick, CallbackInfo ci) {
        int plotZ;
        SubLevelContainer container = SubLevelContainer.getContainer(((AbstractContraptionEntity) this.entity).level());
        org.slf4j.LoggerFactory.getLogger("SableDiag").warn("[光照诊断] 注入已执行 container={}", (container == null ? "null" : "非null"));
        if (container == null) {
            return;
        }
        ChunkPos chunkPos = ((AbstractContraptionEntity) this.entity).chunkPosition();
        boolean inBounds = container.inBounds(chunkPos);
        if (!inBounds) {
            return;
        }
        int plotX = (chunkPos.x >> container.getLogPlotSize()) - container.getOrigin().x;
        FlywheelCompatNeoForge.SubLevelFlwRenderState state = FlywheelCompatNeoForge.getInfo(ChunkPos.pack((int) plotX, (int) (plotZ = (chunkPos.z >> container.getLogPlotSize()) - container.getOrigin().y)));
        org.slf4j.LoggerFactory.getLogger("SableDiag").warn("[光照诊断] state={} embedding扩展={}", (state == null ? "null" : "sceneID=" + state.sceneID), (this.embedding instanceof EmbeddedEnvironmentExtension));
        if (state == null) {
            return;
        }
        Vector3d pos = new Vector3d();
        if (((AbstractContraptionEntity) this.entity).isPrevPosInvalid()) {
            pos.x = ((AbstractContraptionEntity) this.entity).getX();
            pos.y = ((AbstractContraptionEntity) this.entity).getY();
            pos.z = ((AbstractContraptionEntity) this.entity).getZ();
        } else {
            pos.x = Mth.lerp((double) partialTick, (double) ((AbstractContraptionEntity) this.entity).xo, (double) ((AbstractContraptionEntity) this.entity).getX());
            pos.y = Mth.lerp((double) partialTick, (double) ((AbstractContraptionEntity) this.entity).yo, (double) ((AbstractContraptionEntity) this.entity).getY());
            pos.z = Mth.lerp((double) partialTick, (double) ((AbstractContraptionEntity) this.entity).zo, (double) ((AbstractContraptionEntity) this.entity).getZ());
        }
        ChunkPos centerChunk = state.centerChunk;
        // 光照场景矩阵语义（对齐 BlockEntityStorageMixin.sable$updateEmbeddingTransforms 参考实现）：
        // embedding 渲染原点相对 sub-level 光照中心的【纯平移】——shader 用它采样 sub-level 光照贴图；
        // 不可叠加装配体旋转/实体位置，否则光照采样错位导致整体发黑（缺陷2）
        Vec3i localOrigin = this.embedding.renderOrigin();
        PoseStack sceneMatrix = new PoseStack();
        sceneMatrix.translate((float) (localOrigin.getX() - centerChunk.getMinBlockX()), (float) localOrigin.getY(), (float) (localOrigin.getZ() - centerChunk.getMinBlockZ()));
        VisualEmbedding visualEmbedding = this.embedding;
        if (visualEmbedding instanceof EmbeddedEnvironmentExtension) {
            EmbeddedEnvironmentExtension extension = (EmbeddedEnvironmentExtension) visualEmbedding;
            extension.sable$setLightingInfo((Matrix4fc) sceneMatrix.last().pose(), state.sceneID, state.latestSkyLightScale / 15.0f);
        }

    }
}
