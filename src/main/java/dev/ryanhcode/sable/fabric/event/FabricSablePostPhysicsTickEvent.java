package dev.ryanhcode.sable.fabric.event;

import dev.ryanhcode.sable.api.event.SablePostPhysicsTickEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface FabricSablePostPhysicsTickEvent extends SablePostPhysicsTickEvent {
   Event<SablePostPhysicsTickEvent> EVENT = EventFactory.createArrayBacked(SablePostPhysicsTickEvent.class, events -> (system, timeStep) -> {
      for (SablePostPhysicsTickEvent event : events) {
         event.postPhysicsTick(system, timeStep);
      }
   });
}
