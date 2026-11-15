package dev.ryanhcode.sable.mixin.block_decal_render;

import dev.ryanhcode.sable.Sable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.1.2 兼容：26.2 的破坏动画距离计算在 LevelExtractor.extractBlockDestroyAnimation；
 * 26.1.2 没有 LevelExtractor，等价注入 LevelRenderer.extractBlockDestroyAnimation 的同一调用。
 * 作用：sub-level 内方块破坏进度使用"到 sub-level 的投影距离"判定，站在子结构上挖方块时
 * 破坏裂纹（block damage overlay）能正确进入 LevelRenderState 并被渲染。
 */
@Mixin({LevelRenderer.class})
public class LevelExtractorMixin {
   @Redirect(
      method = {"extractBlockDestroyAnimation"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/core/BlockPos;distToCenterSqr(DDD)D"
      )}
   )
   private double sable$blockDamageDistance(BlockPos pos, double cameraX, double cameraY, double cameraZ) {
      return Sable.HELPER.distanceSquaredWithSubLevels(Minecraft.getInstance().level, Vec3.atCenterOf(pos), cameraX, cameraY, cameraZ);
   }
}
