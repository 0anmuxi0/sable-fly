package dev.ryanhcode.sable.mixin.plot.lighting;

import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.plot.SubLevelContainerHolder;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 1.21.1 语义：覆写 BlockAndTintGetter 的两个 default 光照查询，当 receiver 属于子关卡
 * plot 区域时改用该 plot 自带的光照引擎，实现子关卡独立光照。
 *
 * 26.2 适配：1.21.1 的 BlockAndTintGetter（class_1920）在 26.2 拆分为
 * client/renderer/block/BlockAndTintGetter（纯渲染 tint）与 world/level/BlockAndLightGetter
 * （光照查询）。getBrightness(LightLayer,BlockPos) 与 getRawBrightness(BlockPos,int) 两个
 * default 的宿主是 BlockAndLightGetter，@Overwrite 形态保持；
 * 1.21.1 的 getLayerListener/getLightValue/getRawBrightness 在 26.2 LevelLightEngine 上同名存在。
 */
@Mixin(BlockAndLightGetter.class)
public interface BlockAndTintGetterMixin {

    @Shadow
    LevelLightEngine getLightEngine();

    @Overwrite
    default int getBrightness(LightLayer lightLayer, BlockPos blockPos) {
        LevelLightEngine engine = this.getLightEngine();
        if (this instanceof SubLevelContainerHolder holder) {
            SubLevelContainer plotContainer = holder.sable$getPlotContainer();
            if (plotContainer.getLevel() instanceof ServerLevel
                    && plotContainer.getPlot(new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4)) instanceof LevelPlot plot) {
                engine = plot.getLightEngine();
            }
        }
        return engine.getLayerListener(lightLayer).getLightValue(blockPos);
    }

    @Overwrite
    default int getRawBrightness(BlockPos blockPos, int amount) {
        LevelLightEngine engine = this.getLightEngine();
        if (this instanceof SubLevelContainerHolder holder) {
            SubLevelContainer plotContainer = holder.sable$getPlotContainer();
            if (plotContainer.getLevel() instanceof ServerLevel
                    && plotContainer.getPlot(new ChunkPos(blockPos.getX() >> 4, blockPos.getZ() >> 4)) instanceof LevelPlot plot) {
                engine = plot.getLightEngine();
            }
        }
        return engine.getRawBrightness(blockPos, amount);
    }
}
