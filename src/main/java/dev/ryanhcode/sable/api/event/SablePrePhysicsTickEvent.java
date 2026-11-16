package dev.ryanhcode.sable.api.event;

import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;

@FunctionalInterface
public interface SablePrePhysicsTickEvent {
   void prePhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep);
}
