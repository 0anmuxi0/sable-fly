package dev.ryanhcode.sable.mixin.compatibility.create;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.mixinhelpers.compatibility.create.SubLevelOutlinePose;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(
   targets = {"com.zurrtum.create.client.content.kinetics.chainConveyor.ChainConveyorRenderer"}
)
public class ChainConveyorRendererMixin {
   @WrapOperation(
      method = {"getChainsRenderState(Lcom/zurrtum/create/content/kinetics/chainConveyor/ChainConveyorBlockEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;closerThan(Lnet/minecraft/core/Position;D)Z"
      )}
   )
   private boolean sable$compareDistanceInPlotSpace(
      Vec3 instance, Position position, double distance, Operation<Boolean> original, @Local(argsOnly = true) BlockPos tilePos
   ) {
      ClientSubLevel subLevel = SubLevelOutlinePose.find(Vec3.atLowerCornerOf(tilePos));
      return subLevel == null
         ? original.call(instance, position, distance)
         : original.call(SubLevelOutlinePose.plotCamera(subLevel.renderPose(), instance), position, distance);
   }
}
