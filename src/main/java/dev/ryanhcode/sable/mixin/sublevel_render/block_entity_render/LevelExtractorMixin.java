package dev.ryanhcode.sable.mixin.sublevel_render.block_entity_render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * 26.1.2 兼容占位：26.2 的 LevelExtractor 在 26.1.2 不存在。
 * 功能等价实现：sub-level 方块实体渲染由
 * dev.ryanhcode.sable.mixin.sublevel_render.block_entity_render.LevelRendererMixin（注入 submitBlockEntities）完成。
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.extract.LevelExtractor")
public abstract class LevelExtractorMixin {
}
