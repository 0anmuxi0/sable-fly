package dev.ryanhcode.sable.mixin.stop_rain;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.util.LevelAccelerator;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin({WeatherEffectRenderer.class})
public class LevelRenderMixin {
   @Unique
   private MutableBlockPos sable$tempPos;

   @Unique
   private static int sable$getSubLevelHeight(Level level, int pX, int yOffset, int pZ) {
      LevelAccelerator accelerator = new LevelAccelerator(level);
      MutableBlockPos mutableBlockPos = new MutableBlockPos();
      Vector3d checkingPos = new Vector3d();
      Vector3d localUp = new Vector3d(0.0, 1.0, 0.0);
      BoundingBox3dc minMaxBB = new BoundingBox3d(pX, level.getMinY(), pZ, pX + 1, level.getMaxY() + 1, pZ + 1);
      int maxHeight = Integer.MIN_VALUE;

      for (SubLevel subLevel : Sable.HELPER.getAllIntersecting(level, minMaxBB)) {
         subLevel.logicalPose().transformPositionInverse(checkingPos.set(pX + 0.5F, subLevel.boundingBox().maxY(), pZ + 0.5F));
         subLevel.logicalPose().transformNormalInverse(localUp.set(0.0, 1.0, 0.0));
         double checkingDistance = subLevel.boundingBox().maxY() - subLevel.boundingBox().minY();

         for (int i = 0; i < checkingDistance; i++) {
            checkingPos.sub(localUp);
            BlockState gatheredState = accelerator.getBlockState(mutableBlockPos.set(checkingPos.x, checkingPos.y, checkingPos.z));
            if (!gatheredState.getCollisionShape(level, mutableBlockPos).isEmpty() || !gatheredState.getFluidState().isEmpty()) {
               subLevel.logicalPose().transformPosition(checkingPos);
               maxHeight = (int)Math.max((double)maxHeight, checkingPos.y + yOffset);
               break;
            }
         }
      }

      return maxHeight;
   }

   @WrapOperation(
      method = {"extractRenderState"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I"
      )}
   )
   public int sable$preventRainThoughSubLevel(Level instance, Types types, int i, int j, Operation<Integer> original) {
      return Math.max(original.call(instance, types, i, j), sable$getSubLevelHeight(instance, i, 1, j));
   }
}
