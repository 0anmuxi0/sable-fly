package dev.ryanhcode.sable.mixin.compatibility.create.entity_falls_on_block;

import com.zurrtum.create.content.processing.basin.BasinBlock;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 1.21.1 通过 @Redirect 拦截 {@code Entity.blockPosition()}，让实体落到方块上时改用 {@code getOnPos()}。
 *
 * <p>Minecraft 26.2 将 {@code updateEntityAfterFallOn(Entity)} 改为
 * {@code Block.fallOn(Level, BlockState, BlockPos, Entity, double)}：
 * 方块位置作为 <b>参数</b> 直接传入，不再从实体反查。原注入点已不存在，
 * 该修正不再需要。保留空 Mixin 以对应上游文件结构。
 */
@Mixin(BasinBlock.class)
public class BasinBlockMixin {
}
