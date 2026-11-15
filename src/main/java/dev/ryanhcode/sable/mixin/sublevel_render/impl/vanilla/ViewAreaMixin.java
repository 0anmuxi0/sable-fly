package dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * 26.1.2 兼容占位：26.2 的 ViewArea 通过 LevelExtractor 扩展；26.1.2 的 ViewArea 由
 * vanilla 的 createSections/rebuild 流程管理，sub-level section 已由
 * dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla.LevelRendererMixin 的
 * compileSections/prepareChunkRenders 注入覆盖。
 */
@Pseudo
@Mixin(targets = "net.minecraft.client.renderer.extract.LevelExtractor")
public abstract class ViewAreaMixin {
}
