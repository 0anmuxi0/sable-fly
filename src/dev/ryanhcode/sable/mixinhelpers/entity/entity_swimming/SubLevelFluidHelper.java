package dev.ryanhcode.sable.mixinhelpers.entity.entity_swimming;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.math.LevelReusedVectors;
import dev.ryanhcode.sable.api.math.OrientedBoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.entity.entity_sublevel_collision.LevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;

public final class SubLevelFluidHelper {
    private static final Set<TagKey<Fluid>> FLUIDS_WITH_CURRENT = Set.of(FluidTags.WATER, FluidTags.LAVA);

    private SubLevelFluidHelper() {
    }

    public static void gather(Entity entity, boolean ignoreCurrent, Map<Holder<Fluid>, Found> fluids, Map<TagKey<Fluid>, Found> currents) {
        fluids.clear();
        currents.clear();
        AABB box = entity.getFluidInteractionBox();
        if (box == null) {
            return;
        }
        Level level = entity.level();
        LevelReusedVectors sink = ((LevelExtension)level).sable$getJOMLSink();
        BoundingBox3d globalBounds = new BoundingBox3d(box);
        BoundingBox3d localBounds = new BoundingBox3d();
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        Vector3d boxCenter = new Vector3d();
        Vector3d boxSize = new Vector3d();
        Quaterniond boxOrientation = new Quaterniond();
        Vec3 eye = entity.getEyePosition();
        for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, globalBounds)) {
            Pose3dc pose = subLevel.lastPose();
            globalBounds.transformInverse(pose, localBounds);
            Quaterniond localOrientation = pose.orientation().conjugate(boxOrientation);
            localOrientation.rotateY(SubLevelEntityCollision.getHitBoxYaw(pose));
            OrientedBoundingBox3d entityBox = new OrientedBoundingBox3d((Vector3dc)pose.transformPositionInverse(globalBounds.center(boxCenter)), (Vector3dc)globalBounds.size(boxSize), (Quaterniondc)localOrientation, sink);
            OrientedBoundingBox3d fluidBox = new OrientedBoundingBox3d((Vector3dc)new Vector3d(), (Vector3dc)new Vector3d(1.0, 1.0, 1.0), JOMLConversion.QUAT_IDENTITY, sink);
            Vec3 localEye = pose.transformPositionInverse(eye);
            int eyeLocalX = Mth.floor((double)localEye.x);
            int eyeLocalZ = Mth.floor((double)localEye.z);
            int minX = Mth.floor((double)localBounds.minX);
            int maxX = Mth.ceil((double)localBounds.maxX);
            int minY = Mth.floor((double)localBounds.minY);
            int maxY = Mth.ceil((double)localBounds.maxY);
            int minZ = Mth.floor((double)localBounds.minZ);
            int maxZ = Mth.ceil((double)localBounds.maxZ);
            double minYVertex = Double.MAX_VALUE;
            boolean hasMinYVertex = false;
            for (int x = minX; x < maxX; ++x) {
                for (int y = minY; y < maxY; ++y) {
                    for (int z = minZ; z < maxZ; ++z) {
                        double fluidTopForCamera;
                        mutablePos.set(x, y, z);
                        FluidState fluidState = level.getFluidState((BlockPos)mutablePos);
                        if (fluidState.isEmpty()) continue;
                        double fluidTop = (double)y + (double)fluidState.getHeight((BlockGetter)level, (BlockPos)mutablePos);
                        if (!hasMinYVertex) {
                            for (Vector3d vertex : entityBox.vertices(sink.a)) {
                                minYVertex = Math.min(minYVertex, vertex.y);
                            }
                            hasMinYVertex = true;
                        }
                        if (fluidTop < minYVertex) continue;
                        fluidBox.getPosition().set((double)x + 0.5, (double)y + 0.5, (double)z + 0.5);
                        if (OrientedBoundingBox3d.sat(entityBox, fluidBox, sink.mtv).lengthSquared() <= 0.0) continue;
                        Holder holder = fluidState.typeHolder();
                        Found found = fluids.computeIfAbsent((Holder<Fluid>)holder, h -> new Found());
                        found.height = Math.max(fluidTop - minYVertex, found.height);
                        // 26.2: getHeightForCamera was removed; getHeight is the single fluid-height API
                        if (x == eyeLocalX && z == eyeLocalZ && localEye.y >= (double)y && localEye.y <= (fluidTopForCamera = (double)y + (double)fluidState.getHeight((BlockGetter)level, (BlockPos)mutablePos))) {
                            found.eyesInside = true;
                        }
                        if (ignoreCurrent) continue;
                        for (TagKey<Fluid> tag : FLUIDS_WITH_CURRENT) {
                            if (!holder.is(tag)) continue;
                            Found current = currents.computeIfAbsent(tag, t -> new Found());
                            current.height = Math.max(found.height, current.height);
                            Vec3 flow = fluidState.getFlow((BlockGetter)level, (BlockPos)mutablePos);
                            if (current.height < 0.4) {
                                flow = flow.scale(current.height);
                            }
                            current.current = current.current.add(pose.transformNormal(flow));
                            ++current.currentBlocks;
                        }
                    }
                }
            }
        }
    }

    public static FluidState getFluidStateAt(Entity entity, BlockPos globalPos) {
        Level level = entity.level();
        FluidState worldFluid = level.getFluidState(globalPos);
        if (!worldFluid.isEmpty()) {
            return worldFluid;
        }
        Vec3 center = Vec3.atCenterOf((Vec3i)globalPos);
        BoundingBox3d query = new BoundingBox3d(globalPos).expand(0.5);
        for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, query)) {
            BlockPos localPos = BlockPos.containing((Position)subLevel.lastPose().transformPositionInverse(center));
            FluidState localFluid = level.getFluidState(localPos);
            if (localFluid.isEmpty()) continue;
            return localFluid;
        }
        return worldFluid;
    }

    public static void applyCurrent(Entity entity, Found found, double scale) {
        if (found.currentBlocks == 0 || found.current.lengthSqr() < (double)1.0E-5f) {
            return;
        }
        Vec3 impulse = entity instanceof Player ? found.current.scale(1.0 / (double)found.currentBlocks) : found.current.normalize();
        Vec3 oldMovement = entity.getDeltaMovement();
        impulse = impulse.scale(scale);
        if (Math.abs(oldMovement.x) < 0.003 && Math.abs(oldMovement.z) < 0.003 && impulse.length() < 0.0045000000000000005) {
            impulse = impulse.normalize().scale(0.0045000000000000005);
        }
        entity.addDeltaMovement(impulse);
    }

    public static final class Found {
        public double height;
        public boolean eyesInside;
        public Vec3 current = Vec3.ZERO;
        public int currentBlocks;
    }
}
