package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * B10-d: 旋转子层上的实体落地时,原版把落地方向速度清零是沿<b>世界 Y 轴</b>
 * ({@code Vec3.multiply(1.0, 0.0, 1.0)}),在旋转的子层里会导致回弹方向不沿子层局部
 * "上方"而被校正。1.21.1 的 BlockMixin 拦截
 * {@code Block.updateEntityAfterFallOn} 内的 {@code Vec3.multiply(DDD)} 调用,
 * 把清零方向改为子层局部 up 方向(沿局部 up 投影掉速度分量)。
 *
 * <p>26.1.2 适配(javap 确认):方法改名为
 * {@code updateEntityMovementAfterFallOn(BlockGetter, Entity)},基类实现字节码为
 * {@code entity.setDeltaMovement(entity.getDeltaMovement().multiply(1.0, 0.0, 1.0))},
 * 方法体内仍存在唯一的 {@code Vec3.multiply(DDD)Lnet/minecraft/world/phys/Vec3;} 调用,
 * 因此 Redirect target 不变,仅改 {@code method} 名与 descriptor。
 */
@Mixin({Block.class})
public class BlockMixin {

   @Redirect(
      method = {"updateEntityMovementAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;multiply(DDD)Lnet/minecraft/world/phys/Vec3;"
      )}
   )
   private Vec3 sable$rotateWithEntity(Vec3 instance, double x, double y, double z, @Local(argsOnly = true) Entity entity) {
      Quaterniondc orientation = EntitySubLevelUtil.getCustomEntityOrientation(entity, 1.0F);
      if (orientation == null) {
         return instance.multiply(x, y, z);
      }

      Vector3d up = orientation.transform(OrientedBoundingBox3d.UP, new Vector3d());
      double dot = up.dot(instance.x, instance.y, instance.z);
      return instance.subtract(up.x * dot, up.y * dot, up.z * dot);
   }
}
