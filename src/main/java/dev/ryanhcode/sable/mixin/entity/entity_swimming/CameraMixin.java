package dev.ryanhcode.sable.mixin.entity.entity_swimming;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import java.util.Arrays;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera.NearPlane;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Camera.class})
public abstract class CameraMixin {
   @Shadow
   private Vec3 position;
   @Shadow
   private Level level;

   @Shadow
   public abstract NearPlane getNearPlane(float var1);

   @Inject(
      method = {"getFluidInCamera"},
      at = {@At("RETURN")},
      cancellable = true
   )
   public void sable$getFluidInCamera(CallbackInfoReturnable<FogType> var1) {
      if (var1.getReturnValue() == FogType.NONE) {
         BoundingBox3d var2 = new BoundingBox3d(
            this.position.x - 0.5, this.position.y - 0.5, this.position.z - 0.5, this.position.x + 0.5, this.position.y + 0.5, this.position.z + 0.5
         );
         Level var3 = this.level;
         if (var3 == null) {
            return;
         }

         for (SubLevel var5 : Sable.HELPER.getAllIntersecting(var3, var2)) {
            FogType var6 = this.sable$getFluidInCameraAt(((ClientSubLevel)var5).renderPose());
            if (var6 != null) {
               var1.setReturnValue(var6);
               return;
            }
         }
      }
   }

   @Unique
   private FogType sable$getFluidInCameraAt(Pose3dc var1) {
      Vec3 var2 = var1.transformPositionInverse(this.position);
      BlockPos var3 = BlockPos.containing(var2);
      FluidState var4 = this.level.getFluidState(var3);
      if (var4.is(FluidTags.WATER) && var2.y < var3.getY() + var4.getHeight(this.level, var3)) {
         return FogType.WATER;
      } else {
         NearPlane var5 = this.getNearPlane(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true));

         for (Vec3 var7 : Arrays.asList(var5.getPointOnPlane(0.0F, 0.0F), var5.getTopLeft(), var5.getTopRight(), var5.getBottomLeft(), var5.getBottomRight())) {
            Vec3 var8 = var1.transformPositionInverse(this.position.add(var7));
            BlockPos var9 = BlockPos.containing(var8);
            FluidState var10 = this.level.getFluidState(var9);
            if (var10.is(FluidTags.LAVA)) {
               if (var8.y <= var10.getHeight(this.level, var9) + var9.getY()) {
                  return FogType.LAVA;
               }
            } else {
               BlockState var11 = this.level.getBlockState(var9);
               if (var11.is(Blocks.POWDER_SNOW)) {
                  return FogType.POWDER_SNOW;
               }
            }
         }

         return FogType.NONE;
      }
   }
}
