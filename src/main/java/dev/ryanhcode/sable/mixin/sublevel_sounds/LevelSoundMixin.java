package dev.ryanhcode.sable.mixin.sublevel_sounds;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Level.class})
public class LevelSoundMixin {
   @Inject(require = 0, method = {"playSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sable$projectSubLevelSound(Entity except, BlockPos pos, SoundEvent sound, SoundSource source, float volume, float pitch, CallbackInfo ci) {
      Level level = (Level)(Object)this;
      SubLevel subLevel = Sable.HELPER.getContaining(level, pos);
      if (subLevel != null && subLevel.getPlot().contains(pos.getX(), pos.getZ())) {
         if (level.isClientSide()) {
            ci.cancel();
         } else {
            Vec3 world = subLevel.logicalPose().transformPosition(Vec3.atCenterOf(pos));
            level.playSound(null, world.x, world.y, world.z, sound, source, volume, pitch);
            ci.cancel();
         }
      }
   }
}
