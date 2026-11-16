package dev.ryanhcode.sable.platform;

import dev.ryanhcode.sable.api.event.SablePostPhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SablePrePhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;

public interface SableEventPlatform {
   SableEventPlatform INSTANCE = SablePlatformUtil.load(SableEventPlatform.class);

   void onSubLevelContainerReady(SableSubLevelContainerReadyEvent event);

   void onPhysicsTick(SablePrePhysicsTickEvent event);

   void onPostPhysicsTick(SablePostPhysicsTickEvent event);
}
