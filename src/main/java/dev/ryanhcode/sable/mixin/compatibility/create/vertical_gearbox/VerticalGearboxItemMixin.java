/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.sugar.Local
 *  com.zurrtum.create.content.kinetics.gearbox.VerticalGearboxItem
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Vec3i
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.level.Level
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Redirect
 */
package dev.ryanhcode.sable.mixin.compatibility.create.vertical_gearbox;

import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.content.kinetics.gearbox.VerticalGearboxItem;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={VerticalGearboxItem.class})
public class VerticalGearboxItemMixin {
    /**
     * 1.21.1 的 {@code BlockItem#updateCustomBlockEntityTag(Level, Player, BlockPos, ItemStack)}
     * 在 Minecraft 26.3 已被移除（方块实体 NBT 改走组件式 {@code ValueInput/ValueOutput}）。
     * Create 26.3 的 {@code VerticalGearboxItem} 只剩 {@code registerBlocks} 与
     * {@code getPlacementState}。
     * 目标方法彻底不存在，故移除 {@code @Redirect}，仅保留方法体。
     */
    private Direction sable$getDirection(Player player, @Local(argsOnly=true) BlockPos pos, @Local(argsOnly=true) Level level) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, (Vec3i)pos);
        if (subLevel != null) {
            SubLevelHelper.pushEntityLocal(subLevel, (Entity)player);
            Direction dir = player.getDirection();
            SubLevelHelper.popEntityLocal(subLevel, (Entity)player);
            return dir;
        }
        return player.getDirection();
    }
}

