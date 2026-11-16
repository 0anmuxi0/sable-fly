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
   public abstract void doAnimateTick(int i, int j, int k, int l, RandomSource randomSource, @Nullable Block block, MutableBlockPos pos);

   private ClientLevelMixin(
      WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i
   ) {
      super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
   }

   @Inject(
      method = {"animateTick"},
      at = {@At("TAIL")}
   )
   public void sable$subLevelAnimateTick(
      int x, int y, int z, CallbackInfo ci, @Local RandomSource randomSource, @Local Block block, @Local MutableBlockPos pos
   ) {
      Iterable<SubLevel> intersectingSubLevels = Sable.HELPER.getAllIntersecting(this, new BoundingBox3d(x - 32, y - 32, z - 32, x + 32, y + 32, z + 32));
      BoundingBox3i tickingBounds = new BoundingBox3i();
      Vector3d playerPos = new Vector3d();

      for (SubLevel subLevel : intersectingSubLevels) {
         Vector3d position = subLevel.logicalPose().transformPositionInverse(playerPos.set(x, y, z));
         tickingBounds.set(Mth.floor(position.x), Mth.floor(position.y), Mth.floor(position.z), Mth.floor(position.x), Mth.floor(position.y), Mth.floor(position.z));
         tickingBounds.expand(16, 16, 16);
         tickingBounds.intersect(subLevel.getPlot().getBoundingBox());
         int randomCount = Mth.floor(667.0F * tickingBounds.volume() / 32768.0F + randomSource.nextFloat());

         for (int i = 0; i < randomCount; i++) {
            int randomX = Mth.randomBetweenInclusive(randomSource, tickingBounds.minX, tickingBounds.maxX);
            int randomY = Mth.randomBetweenInclusive(randomSource, tickingBounds.minY, tickingBounds.maxY);
            int randomZ = Mth.randomBetweenInclusive(randomSource, tickingBounds.minZ, tickingBounds.maxZ);
            this.doAnimateTick(randomX, randomY, randomZ, 1, randomSource, block, pos);
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
   private double sable$particleDistance(Vec3 cameraPos, double x, double y, double z) {
      return Sable.HELPER.distanceSquaredWithSubLevels(this, cameraPos, x, y, z);
   }
}
