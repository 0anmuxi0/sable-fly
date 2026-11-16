package dev.ryanhcode.sable.mixin.compatibility.create;

import com.zurrtum.create.client.content.contraptions.render.ContraptionVisual;
import com.zurrtum.create.client.flywheel.api.visual.SectionTrackedVisual.SectionCollector;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 修复子关卡上 Create 动态结构（Contraption）渲染<b>全黑 / 无光照</b>。
 *
 * <p><b>根因</b>：{@code ContraptionVisual} 实现 {@code ShaderLightVisual}，光照走 GPU
 * shader 的 section 采样（{@code LightUpdatedVisualStorage}）。
 * {@code checkAndUpdateLightSections()} 按 {@code entity.getBoundingBox()}
 * （子关卡上的 contraption 这里是 <b>plot 虚拟坐标</b>，约 ±20.48M）
 * 算出要采样的 section，再交给 {@code SectionCollector.sections(LongSet)}。
 * 父世界光照引擎在 plot 坐标处没有任何数据 —— 天空光与方块光全是 0，
 * 结构因此渲染成纯黑。
 *
 * <p><b>实现方式</b>：不在 {@code checkAndUpdateLightSections} 内部注入
 * —— 那里既拿不到 {@code entity} 字段（它声明在父类 {@code AbstractEntityVisual}，
 * {@code @Shadow} 会失败），也无法用 {@code @Local}
 * （{@code @WrapOperation} 的参数不允许 MixinExtras 的 {@code @Local}，会报
 * {@code invalid signature}）。
 *
 * <p>改为在 {@code setSectionCollector} 的<b>入参</b>上做文章：用一个包装过的
 * {@code SectionCollector} 替换原 collector。原始 collector 被调用时，
 * 我们先把 plot 坐标的 section 集合投影到真实世界，再转交。
 * 完全不依赖目标类的字段或局部变量。
 *
 * <p><b>防重复包装</b>：包装类实现了标记接口 {@link SableWrappedCollector}，
 * 若入参已经是本 mixin 的包装（例如下层逻辑把我们的包装又传回来），
 * 直接返回，避免坐标被二次投影。
 */
@Mixin(value = ContraptionVisual.class, remap = false)
public abstract class ContraptionVisualLightMixin {

    /** 标记接口：用于识别本 mixin 生成的包装，防止重复投影。 */
    public interface SableWrappedCollector {
    }

    @ModifyVariable(
            method = "setSectionCollector",
            at = @At("HEAD"),
            argsOnly = true,
            remap = false)
    private SectionCollector sable$wrapSectionCollector(SectionCollector original) {
        if (original == null || original instanceof SableWrappedCollector) {
            return original;
        }
        return new SableSectionCollector(original);
    }

    /** 包装类：投影后再转发。 */
    private static final class SableSectionCollector implements SectionCollector, SableWrappedCollector {
        private final SectionCollector delegate;

        private SableSectionCollector(SectionCollector delegate) {
            this.delegate = delegate;
        }

        @Override
        public void sections(LongSet plotSections) {
            this.delegate.sections(sable$projectSections(plotSections));
        }
    }

    /**
     * 把 plot 虚拟坐标的 section 集合投影到真实世界坐标。
     * 不在子关卡内时原样返回（主世界 contraption 不受影响）。
     */
    private static LongSet sable$projectSections(LongSet plotSections) {
        if (plotSections == null || plotSections.isEmpty()) {
            return plotSections;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return plotSections;
        }
        long first = plotSections.iterator().nextLong();
        Vector3d sample = new Vector3d(
                SectionPos.x(first) * 16 + 8,
                SectionPos.y(first) * 16 + 8,
                SectionPos.z(first) * 16 + 8);
        SubLevel subLevel = Sable.HELPER.getContaining(level, sample);
        if (subLevel == null) {
            return plotSections;
        }
        LongSet mapped = new LongOpenHashSet(plotSections.size());
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
        return mapped;
    }
}
