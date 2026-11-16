package dev.ryanhcode.sable.network.client;

import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.Sable;
import java.util.Queue;

public class SableClientNetworkEventLoop {
   private final Queue<Runnable> pendingRunnables = Queues.newConcurrentLinkedQueue();

   public void tell(Runnable runnable) {
      this.pendingRunnables.add(runnable);
   }

   public void runAllTasks() {
      while (this.pollTask()) {
      }
   }

   public boolean pollTask() {
      Runnable runnable = this.pendingRunnables.peek();
      if (runnable == null) {
         return false;
      } else {
         this.doRunTask(this.pendingRunnables.remove());
         return true;
      }
   }

   protected void doRunTask(Runnable runnable) {
      try {
         runnable.run();
      } catch (Exception e) {
         Sable.LOGGER.error(LogUtils.FATAL_MARKER, "Error executing packet handle task", e);
      }
   }

   public void clear() {
      this.pendingRunnables.clear();
   }
}
