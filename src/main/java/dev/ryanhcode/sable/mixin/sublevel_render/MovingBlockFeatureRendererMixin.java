package dev.ryanhcode.sable.mixin.sublevel_render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * 26.1.2 兼容占位：26.2 的 MovingBlockFeatureRenderer 在 26.1.2 不存在。
 * 功能等价实现：sub-level 移动方块渲染由
 * dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla.LevelRendererMixin
 * （注入 submitEntities，逐个 submitMovingBlock）完成。
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.feature.MovingBlockFeatureRenderer")
public abstract class MovingBlockFeatureRendererMixin {
}
