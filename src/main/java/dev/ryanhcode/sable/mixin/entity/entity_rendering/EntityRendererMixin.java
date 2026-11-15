package dev.ryanhcode.sable.mixin.entity.entity_rendering;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({EntityRenderer.class})
public abstract class EntityRendererMixin {
   @Shadow
   @Final
   protected EntityRenderDispatcher entityRenderDispatcher;

   @Shadow
   protected abstract AABB getBoundingBoxForCulling(Entity var1);

   @Shadow
   protected abstract boolean affectedByCulling(Entity var1);

   @ModifyReturnValue(
      method = {"getPackedLightCoords"},
      at = {@At("RETURN")}
   )
   public int sable$getPackedLightCoords(int var1, Entity var2, float var3) {
      Vec3 var4 = var2.getLightProbePosition(var3).subtract(var2.getEyePosition(var3));
      Vector3d var5 = JOMLConversion.toJOML(Sable.HELPER.getEyePositionInterpolated(var2, var3)).add(var4.x, var4.y, var4.z);
      BlockPos var6 = BlockPos.containing(var5.x, var5.y, var5.z);
      return LightCoordsUtil.pack(LightCoordsUtil.block(var1), sable$getSubLevelAccountedSkyLight(var1, var2.level(), LightLayer.SKY, var6, var5));
   }

   @Redirect(
      method = {"extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/client/renderer/entity/EntityRenderDispatcher;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"
      )}
   )
   private double sable$distanceToCamera(EntityRenderDispatcher var1, Entity var2) {
      return Sable.HELPER.distanceSquaredWithSubLevels(var2.level(), Minecraft.getInstance().gameRenderer.getMainCamera().position(), var2.position());
   }

   @Redirect(
      method = {"getSkyLightLevel"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/level/Level;getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I"
      )}
   )
   private int sable$getSkyLightLevel(Level var1, LightLayer var2, BlockPos var3) {
      return sable$getSubLevelAccountedSkyLight(-1, var1, var2, var3, JOMLConversion.atCenterOf(var3));
   }

   @Unique
   private static int sable$getSubLevelAccountedSkyLight(int var0, Level var1, LightLayer var2, BlockPos var3, Vector3dc var4) {
      Iterable<SubLevel> var5 = Sable.HELPER.getAllIntersecting(var1, new BoundingBox3d(var3));
      int var6 = var0 == -1 ? var1.getBrightness(var2, var3) : LightCoordsUtil.sky(var0);
      MutableBlockPos var7 = new MutableBlockPos();
      MutableBlockPos var8 = new MutableBlockPos();
      Vector3d var9 = new Vector3d();

      for (SubLevel var11 : var5) {
         ClientSubLevel var12 = (ClientSubLevel)var11;
         var12.renderPose().transformPositionInverse(var4, var9);
         var7.set(var9.x, var9.y, var9.z);
         Level var13 = var11.getLevel();
         var8.setWithOffset(var7, Direction.UP);
         LevelPlot var14 = var11.getPlot();
         boolean var15 = false;

         while (var8.getY() >= var14.getBoundingBox().minY()) {
            if (!var13.getBlockState(var8).isAir()) {
               var15 = true;
               break;
            }

            var8.move(Direction.DOWN);
         }

         if (var15) {
            if (var2 == LightLayer.BLOCK) {
               var6 = Math.max(var6, var13.getBrightness(var2, var7));
            } else if (var2 == LightLayer.SKY) {
               int var16 = var12.scaleSkyLight(var13.getBrightness(var2, var7));
               var6 = Math.min(var6, var16);
            }
         }
      }

      return var6;
   }

   @Inject(
      method = {"shouldRender"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private <E extends Entity> void sable$shouldRender(E var1, Frustum var2, double var3, double var5, double var7, CallbackInfoReturnable<Boolean> var9) {
      if (!this.affectedByCulling(var1)) {
         var9.setReturnValue(true);
      } else {
         ClientSubLevel var10 = Sable.HELPER.getContainingClient(var1);
         if (var10 != null) {
            Vec3 var11 = var10.renderPose().transformPosition(var1.position());
            AABB var12 = new AABB(var11.x - 2.0, var11.y - 2.0, var11.z - 2.0, var11.x + 2.0, var11.y + 2.0, var11.z + 2.0);
            var9.setReturnValue(var2.isVisible(var12));
         } else {
            SubLevel var17 = Sable.HELPER.getTrackingSubLevel(var1);
            if (var17 != null) {
               float var18 = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true);
               Vec3 var13 = Sable.HELPER.getEyePositionInterpolated(var1, var18).subtract(0.0, var1.getEyeHeight(), 0.0);
               AABB var14 = this.getBoundingBoxForCulling(var1).inflate(0.5);
               if (var14.hasNaN() || var14.getSize() == 0.0) {
                  var14 = new AABB(var1.getX() - 2.0, var1.getY() - 2.0, var1.getZ() - 2.0, var1.getX() + 2.0, var1.getY() + 2.0, var1.getZ() + 2.0);
               }

               var14 = var14.move(var13.subtract(var1.position()));
               if (var2.isVisible(var14)) {
                  var9.setReturnValue(true);
               } else {
                  if (var1 instanceof Leashable var15) {
                     Entity var16 = var15.getLeashHolder();
                     if (var16 != null) {
                        var9.setReturnValue(var2.isVisible(this.getBoundingBoxForCulling(var16)));
                        return;
                     }
                  }

                  var9.setReturnValue(false);
               }
            }
         }
      }
   }
}
