package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import java.util.Optional;
import net.minecraft.core.Position;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets={"com.zurrtum.create.client.content.contraptions.glue.SuperGlueSelectionHandler"})
public class SuperGlueSelectionHandlerMixin {
    @WrapOperation(method={"tick(Lnet/minecraft/client/Minecraft;)V"}, at={@At(value="INVOKE", target="Lnet/minecraft/world/phys/AABB;clip(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;")})
    private Optional<Vec3> sable$clipGlueInWorldSpace(AABB box, Vec3 from, Vec3 to, Operation<Optional<Vec3>> original) {
        ClientSubLevel subLevel = SubLevelOutlinePose.find((Position)box.getCenter());
        if (subLevel == null) {
            return (Optional)original.call(box, from, to);
        }
        return SubLevelOutlinePose.find((Position)from) != null ? (Optional)original.call(box, from, to) : (Optional)original.call(SubLevelOutlinePose.projectToWorld(subLevel, box), from, to);
    }
}

