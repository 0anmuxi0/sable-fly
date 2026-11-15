package dev.ryanhcode.sable.fabric.event;

import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface FabricSableSubLevelContainerReadyEvent extends SableSubLevelContainerReadyEvent {
   Event<SableSubLevelContainerReadyEvent> EVENT = EventFactory.createArrayBacked(SableSubLevelContainerReadyEvent.class, events -> (level, container) -> {
      for (SableSubLevelContainerReadyEvent event : events) {
         event.onSubLevelContainerReady(level, container);
      }
   });
}
