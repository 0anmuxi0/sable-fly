package dev.ryanhcode.sable.mixin.sublevel_render;

import net.minecraft.client.renderer.feature.BlockFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 26.1.2 兼容：26.2 的 MovingBlockFeatureRenderer 在 26.1.2 不存在，
 * 对应的新类是 net.minecraft.client.renderer.feature.BlockFeatureRenderer。
 *
 * 功能等价实现：sub-level 移动方块渲染由
 * dev.ryanhcode.sable.mixin.sublevel_render.impl.vanilla.LevelRendererMixin
 * （注入 submitEntities，逐个 submitMovingBlock）完成。
 *
 * 这里的职责与 26.2 一致：原版在渲染"移动方块"（活塞头、下落方块等）时会把
 * ModelBlockRenderer 的 cull 关掉（传 false），这样单个方块的所有面都会被画出来。
 * 但 sub-level 里的方块是一整个实心结构，若不剔除内部相邻面，就会看到结构内部
 * 的面透出来 —— 视觉上就像整个结构"炸开/穿模"。
 *
 * Sable 的 SubLevelMovingBlockRenderState 覆写了 getBlockState(pos)，
 * 返回 sub-level 内真实的方块状态，因此邻居判定是正确的，可以安全开启剔除。
 */
@Mixin({BlockFeatureRenderer.class})
public abstract class MovingBlockFeatureRendererMixin {
   @ModifyArg(
      method = {"renderMovingBlockSubmits"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/block/ModelBlockRenderer;<init>(ZZLnet/minecraft/client/color/block/BlockColors;)V"
      )},
      index = 1
   )
   private boolean sable$forceFaceCulling(boolean cull) {
      return true;
   }
}
