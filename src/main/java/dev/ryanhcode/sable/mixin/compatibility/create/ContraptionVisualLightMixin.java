package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.content.contraptions.render.ContraptionVisual;
import dev.ryanhcode.sable.Sable;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 修复子关卡上 Create 动态结构（Contraption）渲染<b>全黑 / 无光照</b>。
 *
 * <p><b>根因</b>：{@code ContraptionVisual} 实现 {@code ShaderLightVisual}，光照走 GPU
 * shader 的 section 采样（{@code LightUpdatedVisualStorage}）。它在
 * {@code checkAndUpdateLightSections()} 里按 {@code entity.getBoundingBox()} 算出需要采样的
 * section，再交给 {@code SectionCollector.sections(LongSet)}。
 *
 * <p>子关卡上的 contraption 其 {@code getBoundingBox()} 是 <b>plot 虚拟坐标</b>
 * （约 ±20.48M）。父世界光照引擎在那些位置**没有任何数据**（世界边界外 / 未加载），
 * 每个 section 的天空光与方块光都是 0 —— 结构因此渲染成全黑。
 *
 * <p><b>修复</b>：拦截 {@code SectionCollector.sections(LongSet)}，把 plot 虚拟坐标的
 * section 集合逐个投影到<b>真实世界</b>对应的 section，再交给 shader 光照。
 * 每个 section 的采样点取 section 中心 + 0.5，经
 * {@code Sable.HELPER.projectOutOfSubLevel(...)} 变换后重新取 section 坐标。
 *
 * <p>主世界 contraption 不受影响：{@code projectOutOfSubLevel} 对不在子关卡内的位置
 * 原样返回，映射结果与原集合一致。
 */
@Mixin(value = ContraptionVisual.class, remap = false)
public abstract class ContraptionVisualLightMixin {

    @Shadow
    protected Entity entity;

    @WrapOperation(
            method = "checkAndUpdateLightSections",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/zurrtum/create/client/flywheel/api/visual/SectionTrackedVisual$SectionCollector;sections(Lit/unimi/dsi/fastutil/longs/LongSet;)V"),
            remap = false)
    private void sable$projectLightSectionsToWorld(
            Object collector, LongSet plotSections, Operation<Void> original) {
        Entity entity = this.entity;
        if (entity == null || plotSections.isEmpty()) {
            original.call(collector, plotSections);
            return;
        }
        Level level = entity.level();
        // 只有位于子关卡内才需要投影
        if (Sable.HELPER.getContaining(level, entity.getBoundingBox().getCenter()) == null) {
            original.call(collector, plotSections);
            return;
        }
        LongSet mapped = new LongOpenHashSet(plotSections.size());
        Vector3d sample = new Vector3d();
        Vector3d projected = new Vector3d();
        for (long packed : plotSections) {
            sample.set(
                    SectionPos.x(packed) * 16 + 8,
                    SectionPos.y(packed) * 16 + 8,
                    SectionPos.z(packed) * 16 + 8);
            Sable.HELPER.projectOutOfSubLevel(level, sample, projected);
            mapped.add(SectionPos.asLong(
                    SectionPos.blockToSectionCoord((int) Math.floor(projected.x)),
                    SectionPos.blockToSectionCoord((int) Math.floor(projected.y)),
                    SectionPos.blockToSectionCoord((int) Math.floor(projected.z))));
        }
        original.call(collector, mapped);
    }
}