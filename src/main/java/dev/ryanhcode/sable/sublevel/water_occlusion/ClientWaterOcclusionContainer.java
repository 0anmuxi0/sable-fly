package dev.ryanhcode.sable.sublevel.water_occlusion;

import dev.ryanhcode.sable.util.BoundedBitVolume3i;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class ClientWaterOcclusionContainer extends WaterOcclusionContainer<ClientWaterOcclusionContainer.ClientWaterOcclusionRegion> {
   public ClientWaterOcclusionContainer(Level level) {
      super(level);
   }

   public static ClientWaterOcclusionContainer create(Level level) {
      return new ClientWaterOcclusionContainer(level);
   }

   @Override
   public void removeRegion(WaterOcclusionRegion region) {
      this.regions.remove(region);
   }

   public ClientWaterOcclusionContainer.ClientWaterOcclusionRegion addRegion(BoundedBitVolume3i bitSet) {
      ClientWaterOcclusionContainer.ClientWaterOcclusionRegion region = new ClientWaterOcclusionContainer.ClientWaterOcclusionRegion(bitSet);
      this.regions.add(region);
      BoundedBitVolume3i volume = region.getVolume();
      List<BlockPos> blocks = BlockPos.betweenClosedStream(volume.getMinBlockPos(), volume.getMaxBlockPos())
         .map(BlockPos::immutable)
         .filter(x -> volume.getOccupied(x.getX(), x.getY(), x.getZ()))
         .toList();
      return region;
   }

   protected static class ClientWaterOcclusionRegion extends WaterOcclusionRegion {
      public ClientWaterOcclusionRegion(BoundedBitVolume3i bitSet) {
         super(bitSet);
      }
   }
}
