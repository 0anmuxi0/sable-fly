package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.compatibility.create.CreateSupportsVisualizationContext;
import net.minecraft.world.level.LevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets={"com.zurrtum.create.client.flywheel.impl.FlwApiLinkImpl"})
public class FlwApiLinkImplMixin {
    @WrapOperation(require = 0, method={"supportsVisualization"}, at={@At(value="INVOKE", target="Lcom/zurrtum/create/client/flywheel/impl/visualization/VisualizationManagerImpl;supportsVisualization(Lnet/minecraft/world/level/LevelAccessor;)Z")})
    private boolean sable$disableForSubLevel(LevelAccessor level, Operation<Boolean> original) {
        if (CreateSupportsVisualizationContext.isCurrentBlockEntityInSubLevel()) {
            return false;
        }
        return (Boolean)original.call(level);
    }
}