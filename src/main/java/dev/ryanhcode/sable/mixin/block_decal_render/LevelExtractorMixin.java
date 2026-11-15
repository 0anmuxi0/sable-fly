package dev.ryanhcode.sable.mixin.block_decal_render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * 26.1.2 兼容占位：26.2 的 net.minecraft.client.renderer.extract.LevelExtractor 在 26.1.2 不存在。
 * 功能等价实现：sub-level 方块破坏 overlay 的投影由
 * dev.ryanhcode.sable.mixin.block_decal_render.LevelRendererMixin（注入 submitBlockDestroyAnimation）完成。
 * 本 mixin 在 26.1.2 上因目标类缺失自动跳过（@Pseudo）。
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.extract.LevelExtractor")
public abstract class LevelExtractorMixin {
}
