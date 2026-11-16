package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.content.contraptions.AbstractContraptionEntity;
import com.zurrtum.create.content.contraptions.Contraption;
import com.zurrtum.create.catnip.math.VecHelper;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.api.physics.mass.MassTracker;
import dev.ryanhcode.sable.api.sublevel.KinematicContraption;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.floating_block.FloatingBlockCluster;
import dev.ryanhcode.sable.physics.floating_block.FloatingClusterContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3d;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Create contraption to Sable physics-carrier bridge (26.1.2 port, from Aeronautics/Simulated).
 *
 * <p>Mirrors the 1.21.1 official sable-neoforge-2.0.6 equivalent mixin
 * {@code dev.ryanhcode.sable.neoforge.mixin.compatibility.create.contraptions.AbstractContraptionEntityMixin}.
 * The 26.1.2-ported sable jar lacks this link: after a Create mechanical bearing assembles, the contraption entity is
 * not registered as a KinematicContraption, so the host sub-level's split system tears the bearing-mounted
 * blocks off as unsupported - the bearing head simply drops and cannot rotate.</p>
 *
 * <p>Mechanics: on the contraption's first tick inside a ServerLevel, mass/bounds/buoyancy-cluster/lift data is built and
 * registered with the host plot (addContraption) and the physics pipeline (pipeline.add);
 * it is unregistered when the entity is removed. The sub-level's mass merging (MergedMassTracker), lift
 * calculation (ServerSubLevel force loop), buoyancy following (FloatingBlockController) and the Rapier kinematic
 * collider (RapierPhysicsPipeline) all consume this entity through the official KinematicContraption interface.</p>
 *
 * <p>26.1.2 signature verification (javap against create-fly-26.1.2-6.0.9-4):
 * inside tick() the single {@code this.collidingEntities.entrySet()...removeIf(...)} call is the upstream
 * injection point (first and only Map.entrySet in the method body);
 * {@code getAnchorVec()}/{@code getPrevAnchorVec()}/{@code applyRotation(Vec3,float)}/
 * {@code toLocalVector(Vec3,float)}/{@code moveCollidedEntitiesOnDisassembly(StructureTransform)}
 * keep the same names and shapes; {@code remove(Entity.RemovalReason)} exists non-final.</p>
 */
@Mixin(value = AbstractContraptionEntity.class)
public abstract class AbstractContraptionEntityMixin
        extends Entity
        implements KinematicContraption {
    @Unique
    private final Vector3d sable$cachedGlobalPosition = new Vector3d();
    @Unique
    private final Object2ObjectMap<BlockPos, BlockSubLevelLiftProvider.LiftProviderContext> sable$liftProviderContexts =
            new Object2ObjectOpenHashMap<>();
    @Unique
    private final FloatingClusterContainer sable$floatingClusterContainer = new FloatingClusterContainer();
    @Shadow(remap = false)
    protected Contraption contraption;
    @Unique
    private BoundingBox3i sable$localBounds;
    @Unique
    private MassTracker sable$massTracker;
    @Unique
    private boolean sable$initialized = false;

    public AbstractContraptionEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Shadow(remap = false)
    public abstract Vec3 applyRotation(Vec3 localPos, float partialTicks);

    @Shadow(remap = false)
    public abstract Vec3 getPrevAnchorVec();

    @Shadow(remap = false)
    public abstract Vec3 getAnchorVec();

    @Redirect(
            method = "moveCollidedEntitiesOnDisassembly",
            at = @At(value = "INVOKE",
                    target = "Lcom/zurrtum/create/content/contraptions/AbstractContraptionEntity;toLocalVector(Lnet/minecraft/world/phys/Vec3;F)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 sable$applyTransform(AbstractContraptionEntity instance, Vec3 localVec, float partialTicks) {
        SubLevel subLevel = Sable.HELPER.getContaining(instance);
        return instance.toLocalVector(
                subLevel != null ? subLevel.logicalPose().transformPositionInverse(localVec) : localVec,
                partialTicks);
    }

    @WrapOperation(
            method = "moveCollidedEntitiesOnDisassembly",
            at = {@At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setPos(DDD)V"),
                    @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;teleportTo(DDD)V")})
    private void sable$applyTransform(Entity instance, double x, double y, double z,
                                      Operation<Void> original) {
        Vector3d pos = Sable.HELPER.projectOutOfSubLevel(instance.level(), new Vector3d(x, y, z));
        original.call(instance, pos.x, pos.y, pos.z);
    }

    // Upstream injection point: the first (and only) Map.entrySet() call inside tick()
    // (the collidingEntities cleanup line).
    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;entrySet()Ljava/util/Set;"), remap = false)
    private void sable$contraptionInitialize(CallbackInfo ci) {
        Level level;
        if (!this.sable$initialized && (level = this.level()) instanceof ServerLevel serverLevel) {
            Sable.LOGGER.debug("[ctrp-diag] init attempt pos={} anchor={} level={}",
                    this.position(), this.getAnchorVec(), level);
            this.sable$buildProperties();
            // 26.1.2 MassTracker has no isInvalid(); an empty build returns a tracker
            // whose centerOfMass is null.
            if (this.sable$massTracker == null || this.sable$massTracker.getCenterOfMass() == null) {
                this.sable$initialized = true;
                Sable.LOGGER.info("[ctrp-diag] init FAILED (massTracker invalid) pos={} - contraption NOT registered", this.position());
                return;
            }
            this.sable$addToPlot();
            this.sable$addToPipeline(serverLevel);
            this.sable$initialized = true;
            Sable.LOGGER.info("[ctrp-diag] registered mass={} plotHit={} pos={}",
                    this.sable$massTracker.getMass(), Sable.HELPER.getContaining(this) != null, this.position());
        }
    }

    @Override
    public Map<BlockPos, BlockSubLevelLiftProvider.LiftProviderContext> sable$liftProviders() {
        return this.sable$liftProviderContexts;
    }

    @Inject(method = "remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V", at = @At("HEAD"))
    private void sable$onRemove(Entity.RemovalReason removalReason, CallbackInfo ci) {
        Level level = this.level();
        if (level instanceof ServerLevel serverLevel) {
            this.sable$removeFromPlot();
            this.sable$removeFromPipeline(serverLevel);
        }
    }

    @Unique
    private void sable$buildProperties() {
        Sable.LOGGER.debug("[ctrp-diag] buildProperties: blocks={} (contraption local coords)",
                this.contraption.getBlocks().size());
        for (var entry : this.contraption.getBlocks().entrySet()) {
            BlockPos blockPos = entry.getKey();
            BlockState state = entry.getValue().state();
            if (state.isAir()) {
                continue;
            }
            if (this.sable$localBounds == null) {
                this.sable$localBounds = new BoundingBox3i(
                        blockPos.getX(), blockPos.getY(), blockPos.getZ(),
                        blockPos.getX(), blockPos.getY(), blockPos.getZ());
            }
            this.sable$localBounds.expandTo(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            Block block = state.getBlock();
            if (block instanceof BlockSubLevelLiftProvider provider) {
                BlockSubLevelLiftProvider.LiftProviderContext context =
                        new BlockSubLevelLiftProvider.LiftProviderContext(blockPos, state,
                                Vec3.atLowerCornerOf(provider.sable$getNormal(state).getUnitVec3i()));
                this.sable$liftProviderContexts.put(blockPos, context);
            }
            if (PhysicsBlockPropertyHelper.getFloatingMaterial(state) == null) {
                continue;
            }
            this.sable$floatingClusterContainer.addFloatingBlock(state,
                    new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()));
        }
        if (this.sable$localBounds == null) {
            Sable.LOGGER.debug("[ctrp-diag] buildProperties FAIL localBounds=null (empty blocks)");
            this.sable$massTracker = null;
            return;
        }
        this.sable$massTracker = MassTracker.build(this.sable$blockGetter(), this.sable$localBounds);
        if (this.sable$massTracker.getCenterOfMass() == null) {
            Sable.LOGGER.debug("[ctrp-diag] buildProperties FAIL massTracker invalid (bounds={})",
                    this.sable$localBounds);
            this.sable$massTracker = null;
            return;
        }
        Vector3d temp = this.sable$massTracker.getCenterOfMass().negate(new Vector3d()).add(0.5, 0.5, 0.5);
        for (FloatingBlockCluster cluster : this.sable$floatingClusterContainer.clusters) {
            cluster.getBlockData().translateOrigin(temp);
        }
    }

    @Unique
    private void sable$addToPlot() {
        SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            serverSubLevel.getPlot().addContraption(this);
            Sable.LOGGER.debug("[ctrp-diag] addToPlot: plotHit=true subLevel={}",
                    serverSubLevel.getUniqueId());
        } else {
            Sable.LOGGER.debug("[ctrp-diag] addToPlot: plotHit=false (containing subLevel={})",
                    subLevel);
        }
    }

    @Unique
    private void sable$removeFromPlot() {
        SubLevel subLevel = Sable.HELPER.getContaining(this);
        if (subLevel instanceof ServerSubLevel serverSubLevel) {
            serverSubLevel.getPlot().removeContraption(this);
        }
    }

    @Unique
    private void sable$addToPipeline(ServerLevel serverLevel) {
        SubLevelPhysicsSystem.require(serverLevel).getPipeline().add(this);
    }

    @Unique
    private void sable$removeFromPipeline(ServerLevel serverLevel) {
        SubLevelPhysicsSystem.require(serverLevel).getPipeline().remove(this);
    }

    @Override
    public void sable$getLocalBounds(BoundingBox3i bounds) {
        bounds.set(this.sable$localBounds);
    }

    @Override
    public net.minecraft.world.level.BlockGetter sable$blockGetter() {
        return this.contraption.getContraptionWorld();
    }

    @Override
    public MassTracker sable$getMassTracker() {
        return this.sable$massTracker;
    }

    @Override
    public Vector3dc sable$getPosition(double partialTick) {
        Vec3 localVec = JOMLConversion.toMojang(this.sable$massTracker.getCenterOfMass());
        Vec3 anchor = this.getPrevAnchorVec().lerp(this.getAnchorVec(), partialTick);
        Vec3 rotationOffset = VecHelper.getCenterOf(BlockPos.ZERO);
        localVec = localVec.subtract(rotationOffset);
        localVec = this.applyRotation(localVec, (float) partialTick);
        localVec = localVec.add(rotationOffset).add(anchor);
        return JOMLConversion.toJOML((Position) localVec, this.sable$cachedGlobalPosition);
    }

    @Override
    public Quaterniond sable$getOrientation(double partialTick) {
        Matrix3d matrix = new Matrix3d();
        Vector3d tempColumn = new Vector3d();
        for (int i = 0; i < 3; ++i) {
            matrix.getColumn(i, tempColumn);
            Vec3 transformed = this.applyRotation(JOMLConversion.toMojang(tempColumn), (float) partialTick);
            matrix.setColumn(i, transformed.x, transformed.y, transformed.z);
        }
        return matrix.getNormalizedRotation(new Quaterniond());
    }

    @Override
    public boolean sable$isValid() {
        return !this.isRemoved();
    }

    @Override
    public boolean sable$shouldCollide() {
        return true;
    }

    @Override
    public FloatingClusterContainer sable$getFloatingClusterContainer() {
        return this.sable$floatingClusterContainer;
    }
}
