package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.zurrtum.create.client.flywheel.api.visualization.BlockEntityVisualizer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelCompat;
import dev.ryanhcode.sable.compatibility.create.flywheel.FlywheelWhitelist;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets={"com.zurrtum.create.client.flywheel.lib.visualization.VisualizationHelper"})
public class VisualizationHelperMixin {
    @WrapOperation(require = 0, method={"canVisualize"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;")})
    private static BlockEntityVisualizer<?> sable$gateCanVisualize(BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original) {
        if (Sable.HELPER.getContaining(blockEntity) == null) {
            return (BlockEntityVisualizer)original.call(blockEntity);
        }
        if (!FlywheelCompat.ENABLED || !FlywheelWhitelist.contains(blockEntity.getType())) {
            return null;
        }
        return (BlockEntityVisualizer)original.call(blockEntity);
    }

    @WrapOperation(require = 0, method={"skipVanillaRender"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;")})
    private static BlockEntityVisualizer<?> sable$gateSkipVanillaRender(BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original) {
        if (Sable.HELPER.getContaining(blockEntity) == null) {
            return (BlockEntityVisualizer)original.call(blockEntity);
        }
        if (!FlywheelCompat.ENABLED || !FlywheelWhitelist.contains(blockEntity.getType())) {
            return null;
        }
        return (BlockEntityVisualizer)original.call(blockEntity);
    }

    @WrapOperation(require = 0, method={"tryAddBlockEntity"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/client/flywheel/lib/visualization/VisualizationHelper;getVisualizer(Lnet/minecraft/world/level/block/entity/BlockEntity;)Lcom/zurrtum/create/client/flywheel/api/visualization/BlockEntityVisualizer;")})
    private static BlockEntityVisualizer<?> sable$gateTryAddBlockEntity(BlockEntity blockEntity, Operation<BlockEntityVisualizer<?>> original) {
        if (Sable.HELPER.getContaining(blockEntity) == null) {
            return (BlockEntityVisualizer)original.call(blockEntity);
        }
        return null;
    }
}