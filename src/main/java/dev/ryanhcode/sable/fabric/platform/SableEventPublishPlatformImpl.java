package dev.ryanhcode.sable.fabric.platform;

import dev.ryanhcode.sable.api.event.SablePostPhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SablePrePhysicsTickEvent;
import dev.ryanhcode.sable.api.event.SableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.fabric.event.FabricSablePostPhysicsTickEvent;
import dev.ryanhcode.sable.fabric.event.FabricSablePrePhysicsTickEvent;
import dev.ryanhcode.sable.fabric.event.FabricSableSubLevelContainerReadyEvent;
import dev.ryanhcode.sable.platform.SableEventPublishPlatform;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;

@Internal
public class SableEventPublishPlatformImpl implements SableEventPublishPlatform {
   @Override
   public void onSubLevelContainerReady(Level level, SubLevelContainer container) {
      ((SableSubLevelContainerReadyEvent)FabricSableSubLevelContainerReadyEvent.EVENT.invoker()).onSubLevelContainerReady(level, container);
   }

   @Override
   public void prePhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      ((SablePrePhysicsTickEvent)FabricSablePrePhysicsTickEvent.EVENT.invoker()).prePhysicsTick(physicsSystem, timeStep);
   }

   @Override
   public void postPhysicsTick(SubLevelPhysicsSystem physicsSystem, double timeStep) {
      ((SablePostPhysicsTickEvent)FabricSablePostPhysicsTickEvent.EVENT.invoker()).postPhysicsTick(physicsSystem, timeStep);
   }
}
