package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.zurrtum.create.client.content.contraptions.ContraptionColliderClient;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.foundation.collision.Matrix3d;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value={ContraptionColliderClient.class}, remap=false)
public abstract class ContraptionColliderClientMixin {
    @WrapOperation(require = 0, method={"collideEntities"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;")})
    private static List<Entity> sable$searchInWorldSpace(Level level, Class<? extends Entity> type, AABB plotQuery, Predicate<? super Entity> filter, Operation<List<Entity>> original, @Local(argsOnly=true) AbstractContraptionEntity contraption) {
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)contraption);
        if (subLevel == null) {
            return (List)original.call(level, type, plotQuery, filter);
        }
        return (List)original.call(level, type, ContraptionColliderClientMixin.sable$projectBox(plotQuery, subLevel.logicalPose()), filter);
    }

    @Redirect(require = 0, method={"collideEntities"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getAnchorVec()Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 sable$anchorToWorld(AbstractContraptionEntity contraption) {
        return ContraptionColliderClientMixin.sable$projectPoint(contraption, contraption.getAnchorVec());
    }

    @WrapOperation(require = 0, method={"collideEntities"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;position()Lnet/minecraft/world/phys/Vec3;"), @At(value="INVOKE", target="Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getPrevPositionVec()Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 sable$contraptionPositionToWorld(AbstractContraptionEntity contraption, Operation<Vec3> original) {
        return ContraptionColliderClientMixin.sable$projectPoint(contraption, (Vec3)original.call(contraption));
    }

    @WrapOperation(require = 0, method={"collideEntities"}, at={@At(value="INVOKE", ordinal=0, target="Lnet/minecraft/world/entity/Entity;getDeltaMovement()Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 sable$entityVelocityToContraptionSpace(Entity entity, Operation<Vec3> original, @Local(argsOnly=true) AbstractContraptionEntity contraption) {
        Vec3 velocity = (Vec3)original.call(entity);
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)contraption);
        return subLevel == null ? velocity : subLevel.logicalPose().transformNormalInverse(velocity);
    }

    @WrapOperation(require = 0, method={"collideEntities"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/content/contraptions/ContraptionCollider;getWorldToLocalTranslation(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lcom/zurrtum/create/foundation/collision/Matrix3d;F)Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 sable$worldToContraptionTranslation(Entity entity, Vec3 worldAnchor, Matrix3d rotation, float yawOffset, Operation<Vec3> original, @Local(argsOnly=true) AbstractContraptionEntity contraption) {
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)contraption);
        if (subLevel == null) {
            return (Vec3)original.call(entity, worldAnchor, rotation, Float.valueOf(yawOffset));
        }
        Vec3 worldTranslation = (Vec3)original.call(entity, worldAnchor, rotation, Float.valueOf(yawOffset));
        Vec3 translation = worldTranslation.subtract(entity.position());
        Vec3 localTranslation = subLevel.logicalPose().transformNormalInverse(translation);
        return entity.position().add(localTranslation);
    }

    @WrapOperation(require = 0, method={"collideEntities"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/foundation/collision/Matrix3d;transform(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 sable$toContraptionDirection(Matrix3d rotation, Vec3 value, Operation<Vec3> original, @Local(argsOnly=true) AbstractContraptionEntity contraption) {
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)contraption);
        return subLevel == null ? (Vec3)original.call(rotation, value) : (Vec3)original.call(rotation, subLevel.logicalPose().transformNormalInverse(value));
    }

    @WrapOperation(require = 0, method={"collideEntities"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/foundation/collision/Matrix3d;transformTransposed(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 sable$fromContraptionDirection(Matrix3d rotation, Vec3 value, Operation<Vec3> original, @Local(argsOnly=true) AbstractContraptionEntity contraption) {
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)contraption);
        Vec3 result = (Vec3)original.call(rotation, value);
        return subLevel == null ? result : subLevel.logicalPose().transformNormal(result);
    }

    @WrapOperation(require = 0, method={"collideEntities"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getContactPointMotion(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;")})
    private static Vec3 sable$contactMotionInWorld(AbstractContraptionEntity contraption, Vec3 worldPosition, Operation<Vec3> original) {
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)contraption);
        if (subLevel == null) {
            return (Vec3)original.call(contraption, worldPosition);
        }
        Pose3d pose = subLevel.logicalPose();
        Vec3 localMotion = (Vec3)original.call(contraption, pose.transformPositionInverse(worldPosition));
        return pose.transformNormal(localMotion);
    }

    @WrapOperation(require = 0, method={"collideEntities", "saveClientPlayerFromClipping", "saveRemotePlayerFromClipping"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;getY()D")})
    private static double sable$contraptionYToWorld(AbstractContraptionEntity contraption, Operation<Double> original) {
        double plotY = (Double)original.call(contraption);
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)contraption);
        if (subLevel == null) {
            return plotY;
        }
        Vector3d world = subLevel.logicalPose().transformPosition(new Vector3d(contraption.getX(), plotY, contraption.getZ()));
        return world.y;
    }

    @Unique
    private static Vec3 sable$projectPoint(AbstractContraptionEntity contraption, Vec3 plotPosition) {
        SubLevel subLevel = Sable.HELPER.getContaining((Entity)contraption);
        return subLevel == null ? plotPosition : subLevel.logicalPose().transformPosition(plotPosition);
    }

    @Unique
    private static AABB sable$projectBox(AABB plotBox, Pose3dc pose) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;
        Vector3d corner = new Vector3d();
        for (int i = 0; i < 8; ++i) {
            corner.set((i & 1) == 0 ? plotBox.minX : plotBox.maxX, (i & 2) == 0 ? plotBox.minY : plotBox.maxY, (i & 4) == 0 ? plotBox.minZ : plotBox.maxZ);
            pose.transformPosition((Vector3dc)corner, corner);
            minX = Math.min(minX, corner.x);
            maxX = Math.max(maxX, corner.x);
            minY = Math.min(minY, corner.y);
            maxY = Math.max(maxY, corner.y);
            minZ = Math.min(minZ, corner.z);
            maxZ = Math.max(maxZ, corner.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }
}