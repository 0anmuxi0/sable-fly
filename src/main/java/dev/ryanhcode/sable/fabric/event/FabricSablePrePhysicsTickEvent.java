package dev.ryanhcode.sable.fabric.event;

import dev.ryanhcode.sable.api.event.SablePrePhysicsTickEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

@FunctionalInterface
public interface FabricSablePrePhysicsTickEvent extends SablePrePhysicsTickEvent {
   Event<SablePrePhysicsTickEvent> EVENT = EventFactory.createArrayBacked(SablePrePhysicsTickEvent.class, events -> (system, timeStep) -> {
      for (SablePrePhysicsTickEvent event : events) {
         event.prePhysicsTick(system, timeStep);
      }
   });
}
