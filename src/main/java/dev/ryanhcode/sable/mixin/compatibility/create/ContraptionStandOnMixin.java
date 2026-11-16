package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 让玩家能够<b>站在子关卡上的 Create 动态结构（Contraption）上</b>。
 *
 * <p><b>完整链路（逐字节码确认）</b>：
 * <pre>
 * Entity.move()
 *   └─ ContraptionCollider.collide(movement, entity)          // Create 替换掉的原版碰撞
 *        ├─ level.getEntityCollisions(entity, bbox.expandTowards(movement))   // 收集碰撞体
 *        └─ Entity.collideBoundingBox(entity, motion, bbox, level, shapes)    // 求解
 * </pre>
 *
 * <p>{@code getEntityCollisions}（{@code EntityGetter} 的 default 方法，Mixin 无法注入）
 * 的实现在字节码里是：把包围盒范围内的每个 {@link Entity} 的
 * {@code getBoundingBox()} 通过 {@code Shapes.create(...)} 转成 {@link VoxelShape} 列表。
 *
 * <p>子关卡上的 Contraption 实体位于 <b>plot 虚拟坐标</b>（约 ±20.48M），
 * 玩家在世界坐标 —— 包围盒查询不到它，于是玩家穿透结构、站不住。
 *
 * <p><b>修复位置</b>：拦截 {@code Entity.collideBoundingBox} 的调用，
 * 在传给它的 {@code shapes} 列表上追加「子关卡 Contraption 投影到世界坐标后的包围盒」。
 * 这样原版的求解逻辑（{@code collideWithShapes} / SAT / step-up）完全不做改动，
 * 只是多了一批世界坐标下的碰撞体 —— 玩家就能正常站在结构顶面、被结构顶住。
 *
 * <p>主世界 contraption 不受影响（只为位于子关卡内的结构追加）。
 */
@Mixin(value = Entity.class, remap = false)
public abstract class ContraptionStandOnMixin {

    @WrapOperation(
            method = "collide",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"),
            remap = false)
    private static Vec3 sable$addSubLevelContraptionShapes(
            Entity entity, Vec3 motion, AABB box, Level level, List<VoxelShape> shapes,
            Operation<Vec3> original) {
        if (entity == null || level == null) {
            return original.call(entity, motion, box, level, shapes);
        }
        List<VoxelShape> augmented = sable$augment(level, box.expandTowards(motion), shapes);
        return original.call(entity, motion, box, level, augmented);
    }

    /**
     * 在已有碰撞体列表后追加「位于子关卡内的 Contraption」投影到世界坐标后的包围盒。
     *
     * <p>遍历方式用 {@code level.getEntities().get(queryBox, consumer)} ——
     * 它在客户端与服务端都可用（{@code LevelEntityGetter.get(AABB, Consumer)}），
     * 不像 {@code ServerLevelPlot.getContraptions()} 只在服务端有注册数据。
     */
    private static List<VoxelShape> sable$augment(Level level, AABB queryBox, List<VoxelShape> base) {
        List<VoxelShape> extra = null;
        List<AbstractContraptionEntity> found = new ArrayList<>();
        // 扩大搜索范围：结构包围盒在 plot 坐标，与查询点相差很远，
        // 因此不能按 queryBox 直接查实体 —— 改为遍历全部实体（数量有限，且只在有子关卡时执行）。
        boolean anySubLevel = false;
        for (SubLevel ignored : Sable.HELPER.getAllIntersecting(level,
                new dev.ryanhcode.sable.companion.math.BoundingBox3d(queryBox.inflate(64.0)))) {
            anySubLevel = true;
            break;
        }
        if (!anySubLevel) {
            return base;
        }
        for (Entity candidate : level.getEntities().getAll()) {
            if (candidate instanceof AbstractContraptionEntity contraption) {
                found.add(contraption);
            }
        }
        for (AbstractContraptionEntity contraption : found) {
            SubLevel subLevel = Sable.HELPER.getContaining(contraption);
            if (subLevel == null) {
                continue;
            }
            AABB plotBox = contraption.getBoundingBox();
            if (plotBox == null) {
                continue;
            }
            AABB worldBox = sable$project(plotBox, subLevel.logicalPose());
            if (worldBox == null || !worldBox.intersects(queryBox)) {
                continue;
            }
            if (extra == null) {
                extra = new ArrayList<>();
            }
            extra.add(Shapes.create(worldBox));
        }
        if (extra == null) {
            return base;
        }
        List<VoxelShape> combined = new ArrayList<>(base.size() + extra.size());
        combined.addAll(base);
        combined.addAll(extra);
        return combined;
    }

    /** 把 plot 坐标包围盒按位姿投影到世界坐标（8 角并集）。 */
    private static AABB sable$project(AABB plotBox, Pose3dc pose) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        Vector3d corner = new Vector3d();
        for (int i = 0; i < 8; i++) {
            corner.set((i & 1) == 0 ? plotBox.minX : plotBox.maxX,
                       (i & 2) == 0 ? plotBox.minY : plotBox.maxY,
                       (i & 4) == 0 ? plotBox.minZ : plotBox.maxZ);
            pose.transformPosition(corner, corner);
            minX = Math.min(minX, corner.x); maxX = Math.max(maxX, corner.x);
            minY = Math.min(minY, corner.y); maxY = Math.max(maxY, corner.y);
            minZ = Math.min(minZ, corner.z); maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}