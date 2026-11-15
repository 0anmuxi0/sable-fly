package dev.ryanhcode.sable.mixin.plot.serialization;

import dev.ryanhcode.sable.mixinterface.plot.serialization.LevelChunkTicksExtension;
import java.util.List;
import java.util.Set;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import net.minecraft.world.ticks.ScheduledTick;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin({LevelChunkTicks.class})
public class LevelChunkTicksMixin<T> implements LevelChunkTicksExtension<T> {
   @Shadow
   @Nullable
   private List<SavedTick<T>> pendingTicks;
   @Shadow
   @Final
   private Set<ScheduledTick<?>> ticksPerPosition;

   @Override
   public void sable$copy(LevelChunkTicks<T> var1) {
      this.pendingTicks = ((LevelChunkTicksMixin)(Object)var1).pendingTicks;

      for (SavedTick var3 : this.pendingTicks) {
         this.ticksPerPosition.add(ScheduledTick.probe(var3.type(), var3.pos()));
      }
   }
}
