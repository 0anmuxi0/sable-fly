package dev.ryanhcode.sable.mixin.compatibility.create.entity_falls_on_block;

import com.zurrtum.create.AllBlocks;
import com.zurrtum.create.content.kinetics.belt.BeltBlock;
import com.zurrtum.create.content.kinetics.millstone.MillstoneBlock;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 让实体落到<b>子关卡内</b>的传送带 / 磨石上时，能被正确定位到结构内部的方块位置。
 *
 * <h2>为什么注入 Block 基类</h2>
 * 1.21.1 的做法是在 {@code BeltBlock} / {@code MillstoneBlock} 的
 * {@code updateEntityAfterFallOn(Entity)} 里拦截 {@code Entity.blockPosition()}。
 *
 * <p>Minecraft 26.2 把该方法统一为
 * {@code Block.fallOn(Level, BlockState, BlockPos, Entity, double)}，
 * 且 <b>Create 26.2 的 BeltBlock / MillstoneBlock 都不再覆盖它</b>
 * （已用 javap 确认），因此无法再向这两个子类注入。
 *
 * <p>改为注入 {@code Block.fallOn} 基类，并用 {@code instanceof}
 * 限定只在传送带 / 磨石上生效 —— 其他方块直接返回原位置，行为完全不变。
 */
@Mixin(Block.class)
public class BeltMillstoneBlocksMixin {

    @ModifyVariable(require = 0, method = "fallOn",
            at = @At("HEAD"),
            argsOnly = true,
            index = 3,
            remap = false)
    private BlockPos sable$checkForSubLevels(BlockPos originalPos, Level level, BlockState state,
                                             BlockPos pos, Entity entity, double fallDistance) {
        Block self = (Block) (Object) this;
        if (!(self instanceof BeltBlock) && !(self instanceof MillstoneBlock)) {
            return originalPos;
        }
        Block entry = self instanceof BeltBlock ? AllBlocks.BELT : AllBlocks.MILLSTONE;
        ActiveSableCompanion helper = Sable.HELPER;
        BlockPos gathered = helper.runIncludingSubLevels(level, entity.position(), true, null,
                (subLevel, internalPos) -> {
                    if (level.getBlockState(internalPos).is(entry)) {
                        return internalPos;
                    }
                    if (level.getBlockState(internalPos.below()).is(entry)) {
                        return internalPos.below();
                    }
                    return null;
                });
        return gathered != null ? gathered : originalPos;
    }
}
