package dev.ryanhcode.sable.mixin.particle;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({ClientLevel.class})
public abstract class ClientLevelMixin extends Level {
   @Shadow
   public abstract void doAnimateTick(int var1, int var2, int var3, int var4, RandomSource var5, @Nullable Block var6, MutableBlockPos var7);

   private ClientLevelMixin(
      WritableLevelData var1, ResourceKey<Level> var2, RegistryAccess var3, Holder<DimensionType> var4, boolean var5, boolean var6, long var7, int var9
   ) {
      super(var1, var2, var3, var4, var5, var6, var7, var9);
   }

   @Inject(
      method = {"animateTick"},
      at = {@At("TAIL")}
   )
   public void sable$subLevelAnimateTick(
      int var1, int var2, int var3, CallbackInfo var4, @Local RandomSource var5, @Local Block var6, @Local MutableBlockPos var7
   ) {
      Iterable<SubLevel> var8 = Sable.HELPER.getAllIntersecting(this, new BoundingBox3d(var1 - 32, var2 - 32, var3 - 32, var1 + 32, var2 + 32, var3 + 32));
      BoundingBox3i var9 = new BoundingBox3i();
      Vector3d var10 = new Vector3d();

      for (SubLevel var12 : var8) {
         Vector3d var13 = var12.logicalPose().transformPositionInverse(var10.set(var1, var2, var3));
         var9.set(Mth.floor(var13.x), Mth.floor(var13.y), Mth.floor(var13.z), Mth.floor(var13.x), Mth.floor(var13.y), Mth.floor(var13.z));
         var9.expand(16, 16, 16);
         var9.intersect(var12.getPlot().getBoundingBox());
         int var14 = Mth.floor(667.0F * var9.volume() / 32768.0F + var5.nextFloat());

         for (int var15 = 0; var15 < var14; var15++) {
            int var16 = Mth.randomBetweenInclusive(var5, var9.minX, var9.maxX);
            int var17 = Mth.randomBetweenInclusive(var5, var9.minY, var9.maxY);
            int var18 = Mth.randomBetweenInclusive(var5, var9.minZ, var9.maxZ);
            this.doAnimateTick(var16, var17, var18, 1, var5, var6, var7);
         }
      }
   }

   @Redirect(
      method = {"doAddParticle"},
      at = {@At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/phys/Vec3;distanceToSqr(DDD)D"
      )}
   )
   private double sable$particleDistance(Vec3 var1, double var2, double var4, double var6) {
      return Sable.HELPER.distanceSquaredWithSubLevels(this, var1, var2, var4, var6);
   }
}
