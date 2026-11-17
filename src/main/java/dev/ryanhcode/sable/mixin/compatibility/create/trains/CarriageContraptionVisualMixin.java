/*
 * Decompiled with CFR 0.152.
 * 
 * Sable-Fly 26.3 移植说明：
 * Create 26.3 的 CarriageContraptionVisual.animate(float) 已不再使用
 * PoseTransformStack.translate(Vector3fc)Translate，改为直接操作
 * com.mojang.blaze3d.vertex.PoseStack（见字节码 178: PoseStack.translate(DDD)V）。
 * 目标调用点彻底不存在，故移除 @Redirect 注解但保留方法体。
 */
package dev.ryanhcode.sable.mixin.compatibility.create.trains;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.trains.entity.CarriageContraptionEntity;
import com.zurrtum.create.client.content.contraptions.render.ContraptionVisual;
import com.zurrtum.create.client.content.trains.entity.CarriageContraptionVisual;
import com.zurrtum.create.client.flywheel.api.visualization.VisualizationContext;
import com.zurrtum.create.client.flywheel.lib.transform.PoseTransformStack;
import com.zurrtum.create.client.flywheel.lib.transform.Translate;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.compatibility.flywheel.FlywheelCompatNeoForge;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value={CarriageContraptionVisual.class})
public abstract class CarriageContraptionVisualMixin
extends ContraptionVisual<CarriageContraptionEntity> {
    public CarriageContraptionVisualMixin(VisualizationContext ctx, CarriageContraptionEntity entity, float partialTick) {
        super(ctx, entity, partialTick);
    }

    private Translate sable$translate(PoseTransformStack instance, Vector3fc vector3fc, Vector3f visualPosition, float partialTick) {
        int plotZ;
        Vec3 pos = ((CarriageContraptionEntity)this.entity).position();
        SubLevelContainer container = SubLevelContainer.getContainer(((CarriageContraptionEntity)this.entity).level());
        if (container == null) {
            instance.translate(vector3fc);
            return instance;
        }
        ChunkPos chunkPos = ((CarriageContraptionEntity)this.entity).chunkPosition();
        boolean inBounds = container.inBounds(chunkPos);
        if (!inBounds) {
            instance.translate(vector3fc);
            return instance;
        }
        int plotX = (chunkPos.x >> container.getLogPlotSize()) - container.getOrigin().x;
        FlywheelCompatNeoForge.SubLevelFlwRenderState state = FlywheelCompatNeoForge.getInfo(ChunkPos.pack((int)plotX, (int)(plotZ = (chunkPos.z >> container.getLogPlotSize()) - container.getOrigin().y)));
        if (state == null) {
            instance.translate(vector3fc);
            return instance;
        }
        double entityX = Mth.lerp((double)partialTick, (double)((CarriageContraptionEntity)this.entity).xOld, (double)pos.x);
        double entityY = Mth.lerp((double)partialTick, (double)((CarriageContraptionEntity)this.entity).yOld, (double)pos.y);
        double entityZ = Mth.lerp((double)partialTick, (double)((CarriageContraptionEntity)this.entity).zOld, (double)pos.z);
        Vec3i origin = this.renderOrigin();
        Vec3 renderPos = state.renderPose.transformPosition(new Vec3(entityX, entityY, entityZ)).subtract((double)origin.getX(), (double)origin.getY(), (double)origin.getZ());
        instance.translate(renderPos.x, renderPos.y, renderPos.z);
        instance.rotate((Quaternionfc)new Quaternionf((Quaterniondc)state.renderPose.orientation()));
        return instance;
    }
}
