package dev.ryanhcode.sable.mixin.sign_interaction;

import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让玩家在<b>子关卡</b>内与告示牌交互时，能正确判断自己面对的是正面还是背面。
 *
 * <h2>1.21.1 的做法</h2>
 * 用 {@code @Overwrite} 替换 {@code SignBlockEntity.getSlotPlayerIsFacing(Player)}，
 * 把玩家位置和告示牌法线都投影到同一坐标系后再做点积判断。
 *
 * <h2>26.2 的变化</h2>
 * {@code getSlotPlayerIsFacing} 与 {@code SignTextSlot} 枚举已被移除，
 * 现在用 {@code boolean isFacingFrontText(Player)}：
 * <pre>
 * Vec3 center = signBlock.getSignHitboxCenterPosition(state);
 * double dx = player.getX() - (pos.getX() + center.x);
 * double dz = player.getZ() - (pos.getZ() + center.z);
 * float angle = (float)(Mth.atan2(dz, dx) * 57.2957763671875) - 90.0f;
 * return Mth.degreesDifferenceAbs(signBlock.getYRotationDegrees(state), angle) &lt;= 90.0f;
 * </pre>
 *
 * <p>在子关卡中，{@code player.getX/Z()} 是世界坐标，而
 * {@code pos.getX/Z()} 是 plot 合计坐标，两者相差约 20.48M，
 * 角度计算完全错误，因此玩家永远被判为"背对"。
 *
 * <h2>本 mixin 的修法</h2>
 * 不覆写整个方法（避免与上游实现分叉），而是用 {@code @Redirect} 拦截
 * 方法内对 {@code Player.getX()} / {@code Player.getZ()} 的调用，
 * 在子关卡情形下返回<b>玩家位置反投影到 plot 坐标系</b>后的坐标。
 * 这样原有的角度计算逻辑保持不变，且在两种坐标系下都成立。
 */
@Mixin({SignBlockEntity.class})
public abstract class SignBlockEntityMixin extends BlockEntity {

    public SignBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    /** 玩家位置反投影到告示牌所在坐标系；不在子关卡内时原样返回。 */
    private static Vector3d sable$playerInSignSpace(SignBlockEntity sign, Player player) {
        ActiveSableCompanion helper = Sable.HELPER;
        SubLevel subLevel = helper.getContaining(sign.getLevel(), sign.getBlockPos());
        Vector3d p = JOMLConversion.toJOML(player.position());
        return subLevel == null ? p : subLevel.logicalPose().transformPositionInverse(p);
    }

    @Redirect(
            method = "isFacingFrontText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getX()D"),
            require = 0,
            remap = false)
    private double sable$facingPlayerX(Player player) {
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        return sable$playerInSignSpace(self, player).x;
    }

    @Redirect(
            method = "isFacingFrontText",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getZ()D"),
            require = 0,
            remap = false)
    private double sable$facingPlayerZ(Player player) {
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        return sable$playerInSignSpace(self, player).z;
    }
}
