package dev.ryanhcode.sable.mixin.dynamic_directional_shading;

import dev.ryanhcode.sable.mixinterface.dynamic_directional_shading.ModelBlockRendererCacheExtension;
import net.minecraft.client.renderer.block.BlockModelLighter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 1.21.1 语义：把 AO 缓存对象（ModelBlockRenderer$AmbientOcclusionRenderData，ThreadLocal
 * field_4179 的载荷）扩展出 sable$onSubLevel 标记，供渲染期判断“当前面片是否属于子关卡”。
 *
 * 26.2 适配：1.21.1 的 ModelBlockRenderer$class_4303 在 26.2 重构为
 * BlockModelLighter$Cache（BlockModelLighter.CACHE ThreadLocal 的载荷）。mixin 目标
 * 相应改为 BlockModelLighter.Cache，扩展接口与标记语义不变。
 */
@Mixin(BlockModelLighter.Cache.class)
public class ModelBlockRendererCacheMixin implements ModelBlockRendererCacheExtension {

    @Unique
    private boolean sable$onSubLevel;

    @Override
    public void sable$setOnSubLevel(boolean onSubLevel) {
        this.sable$onSubLevel = onSubLevel;
    }

    @Override
    public boolean sable$getOnSubLevel() {
        return this.sable$onSubLevel;
    }
}
