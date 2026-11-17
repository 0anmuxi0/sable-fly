package dev.ryanhcode.sable.api.sublevel.ticket;

import dev.ryanhcode.sable.sublevel.storage.holding.GlobalSavedSubLevelPointer;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SubLevelTicketInfo {
   private final ObjectSet<SubLevelLoadingTicket<?>> tickets = new ObjectArraySet();
   @org.jetbrains.annotations.Nullable
   private GlobalSavedSubLevelPointer pointer = null;

   public SubLevelTicketInfo() {
   }

   public SubLevelTicketInfo(GlobalSavedSubLevelPointer pointer, ObjectSet<SubLevelLoadingTicket<?>> tickets) {
      this.pointer = pointer;
      this.tickets.addAll(tickets);
   }

   @org.jetbrains.annotations.Nullable
   public GlobalSavedSubLevelPointer getPointer() {
      return this.pointer;
   }

   public void setPointer(@org.jetbrains.annotations.Nullable GlobalSavedSubLevelPointer pointer) {
      this.pointer = pointer;
   }

   public ObjectSet<SubLevelLoadingTicket<?>> tickets() {
      return this.tickets;
   }
}
