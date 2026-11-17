package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.content.contraptions.ContraptionColliderClient;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让玩家能够站在子关卡上的 Create 动态结构（Contraption）上，并被结构推动。
 *
 * <h2>Create 原版的玩家碰撞在客户端</h2>
 * 服务端 {@code ContraptionCollider.collideEntities} 明确跳过玩家：
 * <pre>
 * if (getPlayerType(entity) == PlayerType.SERVER) continue;   // 服务端不管玩家
 * </pre>
 * 玩家碰撞/预测由客户端 {@code ContraptionColliderClient.collideEntities} 完成。
 *
 * <p>其关键两步：
 * <pre>
 * List&lt;Entity&gt; list = world.getEntitiesOfClass(Entity.class,
 *         bounds.inflate(2).expandTowards(0, 32, 0), ...);   // ① 搜实体
 * Vec3 position = ContraptionCollider.getWorldToLocalTranslation(
 *         e, anchorVec, rotMatrix, yawOffset);               // ② 世界 -> 结构局部
 * </pre>
 * 而 {@code getWorldToLocalTranslation} 内部是
 * {@code worldToLocalPos(pos, anchorVec, ...)}，也就是
 * <b>pos - anchorVec</b> 之后再旋转。
 *
 * <h2>子关卡里为什么失效</h2>
 * 子关卡上的 contraption，其 {@code getBoundingBox()} / {@code getAnchorVec()}
 * 都是 <b>plot 虚拟坐标</b>（约 ±20.48M），玩家在世界坐标：
 * <ul>
 *   <li>① 搜索框离玩家 20M，搜不到实体，循环体不执行；</li>
 *   <li>② {@code 玩家坐标 - plot锚点} 差 20M，得到的局部坐标完全错误。</li>
 * </ul>
 *
 * <h2>本 mixin 的两处修改</h2>
 * <ol>
 *   <li><b>①</b> 把搜索包围盒经子关卡位姿投影到世界坐标 —— 这样才能搜到玩家；</li>
 *   <li><b>②</b> 把 {@code getAnchorVec()} 的返回值换成
 *       <b>该锚点经同一 pose 投影后的世界坐标</b> —— 这样
 *       {@code 玩家世界坐标 - 世界锚点} 的结果与
 *       {@code plot玩家坐标 - plot锚点} 完全等价（只差一个刚体变换，
 *       而后续的 localBB / 方块形状比较都在局部坐标系里进行，因此成立）。</li>
 * </ol>
 *
 * <p><b>注意</b>：锚点必须直接用 {@code getAnchorVec()} 投影，
 * <b>不能</b>用搜索包围盒的中心代替 —— 搜索框是
 * {@code inflate(2).expandTowards(0, 32, 0)} 扩展过的，其中心 Y 比真实锚点高约 16 格，
 * 用它换算会让玩家的局部 Y 变成大负数，碰撞永远判不中。
 *
 * <p>其余逻辑（方块形状提取、SAT 求解、推出玩家、packet 同步）完全沿用原版，
 * 因为玩家位置本来就是世界坐标。主世界 contraption 不受影响。
 */
@Mixin(value = ContraptionColliderClient.class, remap = false)
public abstract class ContraptionColliderClientMixin {

    /**
     * ① 用投影到世界坐标的包围盒搜索实体。
     */
    @WrapOperation(
            method = "collideEntities",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"),
            remap = false)
    private static List<Entity> sable$searchInWorldSpace(
            Level level, Class<? extends Entity> type, AABB plotQuery,
            Predicate<? super Entity> filter, Operation<List<Entity>> original) {
        SubLevel subLevel = Sable.HELPER.getContaining(level, plotQuery.getCenter());
        if (subLevel == null) {
            return original.call(level, type, plotQuery, filter);
        }
        AABB worldQuery = sable$projectBox(plotQuery, subLevel.logicalPose());
        return original.call(level, type, worldQuery, filter);
    }

    /**
     * ② 把 {@code getAnchorVec()} 换成它投影到世界坐标后的值。
     *
     * <p>这样 {@code worldToLocalPos(玩家世界坐标, 世界锚点)} 与
     * {@code worldToLocalPos(玩家plot坐标, plot锚点)} 等价，
     * 后续所有局部坐标计算（localBB、getPotentiallyCollidedShapes、
     * ContinuousOBBCollider）都在正确的局部坐标系里。
     */
    @Redirect(
            method = "collideEntities",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getAnchorVec()Lnet/minecraft/world/phys/Vec3;"),
            remap = false)
    private static Vec3 sable$anchorToWorld(AbstractContraptionEntity instance) {
        Vec3 plotAnchor = instance.getAnchorVec();
        SubLevel subLevel = Sable.HELPER.getContaining(instance);
        if (subLevel == null) {
            return plotAnchor;
        }
        Vector3d in = new Vector3d(plotAnchor.x, plotAnchor.y, plotAnchor.z);
        Vector3d out = new Vector3d();
        subLevel.logicalPose().transformPosition(in, out);
        return new Vec3(out.x, out.y, out.z);
    }

    /** 把 plot 坐标包围盒按位姿投影到世界坐标（8 角并集）。 */
    @Unique
    private static AABB sable$projectBox(AABB plotBox, Pose3dc pose) {
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
