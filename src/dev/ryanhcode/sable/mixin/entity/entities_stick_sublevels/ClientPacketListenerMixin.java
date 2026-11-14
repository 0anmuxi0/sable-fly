package dev.ryanhcode.sable.mixin.entity.entities_stick_sublevels;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.ClientSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.EntityStickExtension;
import dev.ryanhcode.sable.mixinterface.entity.entities_stick_sublevels.packet_mixin.PacketActuallyInSubLevelExtension;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Set;
import java.util.function.BooleanSupplier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = {ClientPacketListener.class})
public abstract class ClientPacketListenerMixin {
    @Unique
    private static final int SABLE$DEFAULT_LERP_STEPS = 3;
    @Shadow
    private ClientLevel level;

    @WrapOperation(method={"handleTeleportEntity"}, at={@At(value="INVOKE", target="Lnet/minecraft/client/multiplayer/ClientPacketListener;setValuesFromPositionPacket(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;Lnet/minecraft/world/entity/Entity;Z)Z", ordinal=1)})
    private boolean sable$handleTeleportEntity(PositionMoveRotation positionMoveRotation, Set<Relative> set, Entity entity, boolean bl, Operation<Boolean> operation, @Local(argsOnly=true) ClientboundTeleportEntityPacket clientboundTeleportEntityPacket) {
        PositionMoveRotation positionMoveRotation2 = PositionMoveRotation.calculateAbsolute((PositionMoveRotation)PositionMoveRotation.of((Entity)entity), (PositionMoveRotation)positionMoveRotation, set);
        boolean bl2 = ClientPacketListenerMixin.sable$isActuallyInSubLevel(clientboundTeleportEntityPacket);
        return this.sable$lerp(entity, positionMoveRotation2.position(), positionMoveRotation2.yRot(), positionMoveRotation2.xRot(), 3, true, bl2, () -> (Boolean)operation.call(new Object[]{positionMoveRotation, set, entity, bl}));
    }

    @WrapOperation(method={"handleMoveEntity"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V")})
    private void sable$handleMoveEntity(Entity entity, Vec3 vec3, float f, float f2, Operation<Void> operation, @Local(argsOnly=true) ClientboundMoveEntityPacket clientboundMoveEntityPacket) {
        boolean bl = ClientPacketListenerMixin.sable$isActuallyInSubLevel(clientboundMoveEntityPacket);
        Vec3 vec32 = vec3;
        this.sable$lerp(entity, vec32, f, f2, 3, false, bl, () -> {
            operation.call(new Object[]{entity, vec3, Float.valueOf(f), Float.valueOf(f2)});
            return false;
        });
    }

    // 26.2 parity: handleMoveEntity dispatches over THREE moveOrInterpolateTo overloads. The
    // 1.21.1 original wrapped Entity#lerpTo(DDDFFI), which covered every move packet in one
    // injection point. Wrapping only the (Vec3;FF) overload leaves the position-only (Vec3)V
    // overload -- the one ItemEntity packets actually hit, because items never change yaw and the
    // server therefore sends ClientboundMoveEntityPacket.Pos without rotation -- applying the
    // plot-LOCAL packet coordinates raw, so moveOrInterpolateTo interpolates from the world frame
    // into the plot grid until the next Sync packet pulls it back. Wrap the missing overload with
    // the same conversion, minus the rotation arguments.
    @WrapOperation(method={"handleMoveEntity"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;)V")})
    private void sable$handleMoveEntityPosOnly(Entity entity, Vec3 vec3, Operation<Void> operation, @Local(argsOnly=true) ClientboundMoveEntityPacket clientboundMoveEntityPacket) {
        boolean bl = ClientPacketListenerMixin.sable$isActuallyInSubLevel(clientboundMoveEntityPacket);
        this.sable$lerp(entity, vec3, 0.0f, 0.0f, 3, false, bl, true, () -> {
            operation.call(new Object[]{entity, vec3});
            return false;
        });
    }

    @WrapOperation(method={"handleEntityPositionSync"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;moveOrInterpolateTo(Lnet/minecraft/world/phys/Vec3;FF)V")})
    private void sable$handleEntityPositionSyncInterpolation(Entity entity, Vec3 vec3, float f, float f2, Operation<Void> operation, @Local(argsOnly=true) ClientboundEntityPositionSyncPacket clientboundEntityPositionSyncPacket) {
        this.sable$handleEntityPositionSync(entity, vec3, f, f2, () -> {
            operation.call(new Object[]{entity, vec3, Float.valueOf(f), Float.valueOf(f2)});
            return false;
        }, clientboundEntityPositionSyncPacket);
    }

    @WrapOperation(method={"handleEntityPositionSync"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;snapTo(Lnet/minecraft/world/phys/Vec3;FF)V")})
    private void sable$handleEntityPositionSyncSnap(Entity entity, Vec3 vec3, float f, float f2, Operation<Void> operation, @Local(argsOnly=true) ClientboundEntityPositionSyncPacket clientboundEntityPositionSyncPacket) {
        this.sable$handleEntityPositionSync(entity, vec3, f, f2, () -> {
            operation.call(new Object[]{entity, vec3, Float.valueOf(f), Float.valueOf(f2)});
            return false;
        }, clientboundEntityPositionSyncPacket);
    }

    @Unique
    private static boolean sable$isActuallyInSubLevel(Object object) {
        PacketActuallyInSubLevelExtension packetActuallyInSubLevelExtension;
        return object instanceof PacketActuallyInSubLevelExtension && (packetActuallyInSubLevelExtension = (PacketActuallyInSubLevelExtension)object).sable$isActuallyInSubLevel();
    }

    @Unique
    private void sable$handleEntityPositionSync(Entity entity, Vec3 vec3, float f, float f2, BooleanSupplier booleanSupplier, ClientboundEntityPositionSyncPacket clientboundEntityPositionSyncPacket) {
        boolean bl = ClientPacketListenerMixin.sable$isActuallyInSubLevel(clientboundEntityPositionSyncPacket);
        this.sable$lerp(entity, vec3, f, f2, 3, true, bl, booleanSupplier);
    }

    @Unique
    private boolean sable$lerp(Entity entity, Vec3 vec3, float f, float f2, int n, boolean bl, boolean bl2, BooleanSupplier booleanSupplier) {
        return this.sable$lerp(entity, vec3, f, f2, n, bl, bl2, false, booleanSupplier);
    }

    @Unique
    private boolean sable$lerp(Entity entity, Vec3 vec3, float f, float f2, int n, boolean bl, boolean bl2, boolean posOnly, BooleanSupplier booleanSupplier) {
        EntityStickExtension entityStickExtension = (EntityStickExtension)entity;
        ClientSubLevelContainer clientSubLevelContainer = SubLevelContainer.getContainer(this.level);
        SubLevel subLevel = Sable.HELPER.getContaining((Level)this.level, (Position)vec3);
        Vec3 vec32 = entityStickExtension.sable$getPlotPosition();
        if (!bl2 && subLevel == null && clientSubLevelContainer.inBounds(BlockPos.containing((Position)vec3))) {
            return true;
        }
        if (subLevel == null || bl2) {
            SubLevel subLevel2 = Sable.HELPER.getContaining((Level)this.level, (Position)entity.position());
            if (subLevel != null && bl2 && subLevel2 != subLevel) {
                entity.setPos(subLevel.logicalPose().transformPositionInverse(entity.position()));
            } else if (subLevel2 != null && subLevel == null) {
                entity.setPos(subLevel2.logicalPose().transformPosition(entity.position()));
            }
            boolean bl3 = booleanSupplier.getAsBoolean();
            entityStickExtension.sable$setPlotPosition(null);
            return bl3;
        }
        if (!(entity instanceof LivingEntity)) {
            // 26.3 parity: an UNFLAGGED packet whose target lands on sub-level chunks carries
            // plot-LOCAL coordinates when the sender is a tracked entity (the server rewrote the
            // position field inside its window). Converting them back to world space here --
            // exactly once, on every packet -- keeps the client entity permanently in the world
            // frame, so vanilla physics and the delta codec never flip coordinate systems.
            Vec3 vec33 = subLevel.logicalPose().transformPosition(vec3);
            if (posOnly) {
                // Vanilla (Vec3)V overload: rotation is untouched for position-only move packets.
                entity.moveOrInterpolateTo(vec33);
            } else {
                entity.moveOrInterpolateTo(vec33, f, f2);
            }
            return true;
        }
        if (vec32 == null) {
            entityStickExtension.sable$setPlotPosition(subLevel.logicalPose().transformPositionInverse(entity.position()));
        } else {
            SubLevel subLevel3 = Sable.HELPER.getContaining((Level)this.level, (Position)vec32);
            if (subLevel3 != null && subLevel != subLevel3) {
                Vec3 vec34 = subLevel3.logicalPose().transformPosition(vec32);
                entityStickExtension.sable$setPlotPosition(subLevel.logicalPose().transformPositionInverse(vec34));
            }
        }
        entity.moveOrInterpolateTo(f, f2);
        entityStickExtension.sable$plotLerpTo(vec3, n);
        return true;
    }
}
