package dev.ryanhcode.sable.sublevel.system.ticket;

import java.util.UUID;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public final class InhabitedChunkTicket {
   private final UUID uuid;
   private long lastInhabitedTick;

   public InhabitedChunkTicket(UUID uuid, long lastInhabitedTick) {
      this.uuid = uuid;
      this.lastInhabitedTick = lastInhabitedTick;
   }

   public long lastInhabitedTick() {
      return this.lastInhabitedTick;
   }

   public void setLastInhabitedTick(long lastInhabitedTick) {
      this.lastInhabitedTick = lastInhabitedTick;
   }

   @Override
   public int hashCode() {
      return this.uuid.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      return obj instanceof InhabitedChunkTicket other ? this.uuid.equals(other.uuid) : false;
   }
}
