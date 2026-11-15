package dev.ryanhcode.sable.physics.impl.rapier;

import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.physics.PhysicsPipelineProvider;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public final class RapierPhysicsPipelineProvider implements PhysicsPipelineProvider {
   @NotNull
   @Override
   public PhysicsPipeline createPipeline(@NotNull ServerLevel level) {
      return new RapierPhysicsPipeline(level);
   }
}
