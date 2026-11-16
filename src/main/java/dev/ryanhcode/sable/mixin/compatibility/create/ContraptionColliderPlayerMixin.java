package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.ContraptionCollider;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让玩家能够<b>站在子关卡上的 Create 动态结构（Contraption）上</b>、被结构推动 / 载着移动。
 *
 * <p><b>根因</b>：{@code ContraptionCollider.collideEntities(contraption)} 第一步用
 * {@code level.getEntitiesOfClass(Entity.class, contraption.getBoundingBox().inflate(2).expandTowards(0,32,0), ...)}
 * 找出与结构接触的实体。子关卡上的 contraption 其 {@code getBoundingBox()} 位于
 * <b>plot 虚拟坐标</b>（约 ±20.48M），而玩家在世界坐标处 —— 两者相距 20M，
 * 玩家永远不在候选列表中，于是原版 Create 的"站上/被推"完全不生效。
 *
 * <p><b>为什么不能简单地"用投影后的包围盒搜索"</b>：Create 后续全部计算
 * （{@code worldToLocalPos}、{@code getWorldToLocalTranslation}、碰撞盒 SAT 等）
 * 都基于 {@code getAnchorVec()}（结构锚点，同样是 plot 坐标）。
 * 如果直接塞进世界坐标的实体，后续换算会得到 20M 之外的相对位置，
 * 碰撞判定依然错误（甚至把玩家甩飞）。
 *
 * <p><b>本 mixin 的做法</b>：
 * <ol>
 *   <li>把结构的搜索包围盒投影到真实世界，用 {@code entity.getBoundingBox().intersects} 找出真实世界的候选实体；</li>
 *   <li>把这些实体的位置<b>临时</b>变换到 plot 坐标系
 *       （{@code subLevel.logicalPose().transformPositionInverse}）；</li>
 *   <li>交给原方法继续处理（此时坐标与结构同一坐标系，原版逻辑完全成立）；</li>
 *   <li>在 {@code collideEntities} 返回前，把实体位置变换回真实世界，
 *       并把速度也按位姿法线变换回来。</li>
 * </ol>
 *
 * <p>主世界 contraption 完全不受影响（不在子关卡时走原路径）。
 */
@Mixin(value = ContraptionCollider.class, remap = false)
public abstract class ContraptionColliderPlayerMixin {

    /** 本次调用中做过的坐标映射：实体 -> 其真实世界位置（用于返回时还原）。 */
    private static final ThreadLocal<List<MappedEntity>> SABLE_MAPPED = ThreadLocal.withInitial(ArrayList::new);

    private record MappedEntity(Entity entity, net.minecraft.world.phys.Vec3 worldPos,
                                net.minecraft.world.phys.Vec3 worldDelta, SubLevel subLevel) {
    }

    @WrapOperation(
            method = "collideEntities",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),
            remap = false)
    private static List<Entity> sable$searchAndRemapEntities(
            Level level, Class<? extends Entity> entityClass, AABB plotBounds,
            java.util.function.Predicate<? super Entity> filter, Operation<List<Entity>> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) AbstractContraptionEntity contraption) {
        List<MappedEntity> mapped = SABLE_MAPPED.get();
        mapped.clear();

        SubLevel subLevel = Sable.HELPER.getContaining(contraption);
        if (subLevel == null) {
            return original.call(level, entityClass, plotBounds, filter);
        }

        // 1. 把搜索包围盒投影到真实世界（8 个角取并集）
        Pose3dc pose = subLevel.logicalPose();
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        Vector3d corner = new Vector3d();
        for (int i = 0; i < 8; i++) {
            corner.set((i & 1) == 0 ? plotBounds.minX : plotBounds.maxX,
                       (i & 2) == 0 ? plotBounds.minY : plotBounds.maxY,
                       (i & 4) == 0 ? plotBounds.minZ : plotBounds.maxZ);
            pose.transformPosition(corner, corner);
            minX = Math.min(minX, corner.x); maxX = Math.max(maxX, corner.x);
            minY = Math.min(minY, corner.y); maxY = Math.max(maxY, corner.y);
            minZ = Math.min(minZ, corner.z); maxZ = Math.max(maxZ, corner.z);
        }
        AABB worldBounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);

        // 2. 在真实世界搜索
        List<Entity> found = original.call(level, entityClass, worldBounds, filter);
        if (found.isEmpty()) {
            return found;
        }

        // 3. 逐个实体：记录真实位置，并把位置临时映射到 plot 坐标系
        List<Entity> result = new ArrayList<>(found.size());
        Pose3dc inversePose = subLevel.logicalPose();
        for (Entity entity : found) {
            net.minecraft.world.phys.Vec3 worldPos = entity.position();
            net.minecraft.world.phys.Vec3 worldDelta = entity.getDeltaMovement();
            mapped.add(new MappedEntity(entity, worldPos, worldDelta, subLevel));

            Vector3d local = new Vector3d(worldPos.x, worldPos.y, worldPos.z);
            inversePose.transformPositionInverse(local, local);
            entity.setPos(local.x, local.y, local.z);

            Vector3d localDelta = new Vector3d(worldDelta.x, worldDelta.y, worldDelta.z);
            inversePose.transformNormalInverse(localDelta, localDelta);
            entity.setDeltaMovement(localDelta.x, localDelta.y, localDelta.z);

            result.add(entity);
        }
        return result;
    }

    /**
     * 在 {@code collideEntities} 返回时，把被映射过的实体还原回真实世界坐标与速度。
     */
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "collideEntities", remap = false)
    private static void sable$restoreAfterCollide(AbstractContraptionEntity contraption,
                                                  com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        List<MappedEntity> mapped = SABLE_MAPPED.get();
        try {
            original.call(contraption);
        } finally {
            if (!mapped.isEmpty()) {
                for (MappedEntity entry : mapped) {
                    Entity entity = entry.entity();
                    if (entity == null) continue;
                    SubLevel subLevel = entry.subLevel();
                    Pose3dc pose = subLevel.logicalPose();

                    // 局部（结构已被 Create 调整过）-> 真实世界
                    net.minecraft.world.phys.Vec3 localPos = entity.position();
                    Vector3d world = new Vector3d(localPos.x, localPos.y, localPos.z);
                    pose.transformPosition(world, world);
                    entity.setPos(world.x, world.y, world.z);

                    net.minecraft.world.phys.Vec3 localDelta = entity.getDeltaMovement();
                    Vector3d worldDelta = new Vector3d(localDelta.x, localDelta.y, localDelta.z);
                    pose.transformNormal(worldDelta, worldDelta);
                    entity.setDeltaMovement(worldDelta.x, worldDelta.y, worldDelta.z);
                }
                mapped.clear();
            }
        }
    }
}