package dev.ryanhcode.sable.mixin.entity.entity_rotations_and_riding;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.mixinhelpers.entity.entity_riding_sub_level_vehicle.EntityRidingSubLevelVehicleHelper;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.21.1 语义：在 EntityType.loadEntityRecursive 的乘客加载 lambda 中，当乘客 newEntity 骑上
 * 载具 entity（startRiding）之前，若载具位于子关卡内则把乘客坐标修正到子关卡位姿（防止载具实体
 * 与乘客的坐标撕裂）。
 *
 * 26.2 适配：乘客加载重构为 EntityType.loadPassengersRecursive(Entity vehicle, ValueInput,
 * Level, EntitySpawnRequest, EntityProcessor)，其内部的 startRiding(Entity,ZZ) 调用点对应同一
 * 时点；LVT 中 Entity 局部序为 entity@0(参数)/passenger@7，@Local(ordinal=1) 仍取 passenger。
 */
@Mixin(EntityType.class)
public class EntityTypeMixin {

    @Inject(method = "loadPassengersRecursive",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z"))
    private static void sable$startRidingEntity(Entity entity, ValueInput input, Level level, EntitySpawnRequest request,
                                                EntityProcessor postLoad, CallbackInfoReturnable<Entity> cir,
                                                @Local(ordinal = 1) Entity newEntity) {
        SubLevel vehicleSubLevel = Sable.HELPER.getContaining(entity);
        if (vehicleSubLevel != null && EntitySubLevelUtil.shouldKick(newEntity)) {
            Vec3 pos = EntityRidingSubLevelVehicleHelper.kickRidingEntity(newEntity, vehicleSubLevel);
            newEntity.setPos(pos);
        }
    }
}
